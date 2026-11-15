package hospital;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Glue between the Swing GUI and the worker threads.
 * Starts/stops monitoring and never touches Swing components itself;
 * it only calls the Listener (the GUI hops onto the Event Dispatch Thread).
 */
public class HospitalController {

    public interface Listener {
        void onReading(VitalReading reading);
        void onAlert(AlertEvent event);
        void onMonitoringStopped(String finalReportText);
    }

    private final Map<Integer, Patient> patients = new ConcurrentHashMap<>();
    private final Map<Integer, VitalReading> latestReadings = new ConcurrentHashMap<>();
    private final Map<Integer, PatientStats> stats = new ConcurrentHashMap<>();
    private final BlockingQueue<VitalReading> queue = new LinkedBlockingQueue<>();
    private final AtomicInteger idGen = new AtomicInteger(1);

    private final File alertsLog = new File("alerts.log");
    private final File finalReport = new File("final_report.txt");
    private final AlertManager alertManager = new AlertManager(alertsLog);

    private volatile boolean running = false;   // shutdown signal for sensors
    private volatile boolean active = false;    // true from start until the final report is written
    private ExecutorService executor;
    private long startedAt;
    private Listener listener;

    public void setListener(Listener l) { this.listener = l; }

    // ---------- patients ----------
    public Patient addPatient(String name, int age, String room) {
        Patient p = new Patient(idGen.getAndIncrement(), name, age, room);
        patients.put(p.getId(), p);
        return p;
    }

    public List<Patient> getPatients() {
        return new ArrayList<>(new TreeMap<>(patients).values());
    }

    public boolean hasPatient(int id) { return patients.containsKey(id); }

    /** Removes a patient (allowed even while monitoring: that patient's sensor thread simply exits). */
    public boolean removePatient(int id) {
        boolean existed = patients.remove(id) != null;
        latestReadings.remove(id);
        stats.remove(id);
        return existed;
    }

    public int removeAllPatients() {
        int n = patients.size();
        for (Integer id : new ArrayList<>(patients.keySet())) removePatient(id);
        return n;
    }

    public boolean isActive() { return active; }

    public Map<Integer, VitalReading> getLatestReadings() { return latestReadings; }
    public AlertManager getAlertManager() { return alertManager; }
    public boolean isRunning() { return running; }
    public File getAlertsLog() { return alertsLog; }
    public File getFinalReportFile() { return finalReport; }

    // ---------- monitoring ----------
    public synchronized boolean startMonitoring() {
        if (running || active || patients.isEmpty()) return false;
        latestReadings.clear();
        stats.clear();
        queue.clear();
        alertManager.reset();
        startedAt = System.currentTimeMillis();
        running = true;
        active = true;

        Collection<Patient> list = patients.values();
        executor = Executors.newFixedThreadPool(list.size() + 1);
        CountDownLatch latch = new CountDownLatch(list.size());

        executor.submit(new AnalyzerTask(queue, latestReadings, patients, stats, alertManager, listener));
        for (Patient p : list) {
            executor.submit(new SensorTask(p, queue, latch, this));
        }

        // Watcher: waits for all sensors (CountDownLatch), then stops the analyzer and writes the report.
        Thread watcher = new Thread(() -> {
            try {
                latch.await();
                running = false; // also covers "every patient was removed"
                queue.put(VitalReading.POISON);
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String report = buildReport(true);
            writeReportFile(report);
            active = false;
            if (listener != null) listener.onMonitoringStopped(report);
        }, "shutdown-watcher");
        watcher.setDaemon(true);
        watcher.start();
        return true;
    }

    /** Non-blocking: flips the volatile flag; the sensors exit and the watcher finishes the job. */
    public void stopMonitoring() {
        running = false;
    }

    /** Called when the window closes. */
    public void shutdownNow() {
        running = false;
        if (executor != null) executor.shutdownNow();
    }

    // ---------- reports ----------
    public String buildReport(boolean finalReport) {
        StringBuilder sb = new StringBuilder();
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        sb.append("==============================================\n");
        sb.append(finalReport ? "   HOSPITAL VITALS - FINAL REPORT\n" : "   HOSPITAL VITALS - LIVE SNAPSHOT\n");
        sb.append("==============================================\n");
        sb.append("Generated : ").append(now).append('\n');
        if (startedAt > 0) {
            sb.append("Session   : ").append((System.currentTimeMillis() - startedAt) / 1000).append(" seconds\n");
        }
        sb.append("Patients  : ").append(patients.size()).append('\n');
        sb.append("Warnings  : ").append(alertManager.getWarningCount()).append('\n');
        sb.append("Critical  : ").append(alertManager.getCriticalCount()).append("\n\n");

        for (Patient p : getPatients()) {
            sb.append(String.format("%s (age %d, room %s)%n", p.getName(), p.getAge(), p.getRoom()));
            PatientStats s = stats.get(p.getId());
            sb.append("   ").append(s == null ? "no readings" : s.summary()).append('\n');
            VitalReading r = latestReadings.get(p.getId());
            if (r != null) {
                sb.append(String.format("   latest: HR %d | SpO2 %d%% | BP %d/%d | Temp %.1fC%n",
                        r.heartRate, r.spo2, r.systolic, r.diastolic, r.temperature));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private void writeReportFile(String text) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(finalReport))) {
            pw.print(text);
        } catch (IOException e) {
            System.err.println("Could not write final_report.txt: " + e.getMessage());
        }
    }

    public void exportReport(File dest, String text) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(dest))) {
            pw.print(text);
        }
    }
}
