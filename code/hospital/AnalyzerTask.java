package hospital;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

/** Single consumer: takes readings from the queue, checks thresholds, updates latest vitals. */
public class AnalyzerTask implements Runnable {
    private final BlockingQueue<VitalReading> queue;
    private final Map<Integer, VitalReading> latest;
    private final Map<Integer, Patient> patients;
    private final Map<Integer, PatientStats> stats;
    private final AlertManager alertManager;
    private final HospitalController.Listener listener;

    public AnalyzerTask(BlockingQueue<VitalReading> queue, Map<Integer, VitalReading> latest,
                        Map<Integer, Patient> patients, Map<Integer, PatientStats> stats,
                        AlertManager alertManager, HospitalController.Listener listener) {
        this.queue = queue;
        this.latest = latest;
        this.patients = patients;
        this.stats = stats;
        this.alertManager = alertManager;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            while (true) {
                VitalReading r = queue.take();
                if (r == VitalReading.POISON) break; // graceful shutdown after draining
                Patient p = patients.get(r.patientId);
                if (p == null) continue;

                latest.put(r.patientId, r);

                StringBuilder msg = new StringBuilder();
                AlertLevel level = AlertManager.classify(r, msg);
                stats.computeIfAbsent(r.patientId, k -> new PatientStats()).update(r, level);

                AlertEvent ev = alertManager.process(p, level, msg.toString());
                if (listener != null) {
                    listener.onReading(r);
                    if (ev != null) listener.onAlert(ev);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
