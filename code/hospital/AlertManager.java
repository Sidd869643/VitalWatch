package hospital;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Classifies readings, keeps alert counters (AtomicInteger) and writes alerts.log
 * under a ReentrantLock so concurrent writers never interleave.
 */
public class AlertManager {
    private final AtomicInteger warningCount = new AtomicInteger();
    private final AtomicInteger criticalCount = new AtomicInteger();
    private final ReentrantLock fileLock = new ReentrantLock();
    private final File logFile;

    // Throttle: remember last alert per patient so we don't spam every second
    private final Map<Integer, AlertLevel> lastLevel = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastTime = new ConcurrentHashMap<>();
    private static final long REPEAT_MS = 6000;

    public AlertManager(File logFile) {
        this.logFile = logFile;
    }

    public static AlertLevel hrLevel(int hr) {
        return hr < 40 || hr > 130 ? AlertLevel.CRITICAL
             : hr < 55 || hr > 110 ? AlertLevel.WARNING : AlertLevel.NORMAL;
    }
    public static AlertLevel spo2Level(int spo2) {
        return spo2 < 90 ? AlertLevel.CRITICAL : spo2 < 94 ? AlertLevel.WARNING : AlertLevel.NORMAL;
    }
    public static AlertLevel bpLevel(int sys, int dia) {
        return sys > 180 || sys < 80 || dia > 120 ? AlertLevel.CRITICAL
             : sys > 140 || sys < 90 || dia > 90 ? AlertLevel.WARNING : AlertLevel.NORMAL;
    }
    public static AlertLevel tempLevel(double t) {
        return t > 39.5 || t < 35.0 ? AlertLevel.CRITICAL
             : t > 37.8 || t < 36.0 ? AlertLevel.WARNING : AlertLevel.NORMAL;
    }

    /**
     * Simple early-warning score shown on the dashboard cards: every vital contributes
     * 0 (normal), 2 (warning) or 3 (critical) using the same thresholds as the alerts.
     * Not the clinical NEWS2 scale; it only summarises how many vitals are off and how badly.
     */
    public static int ews(VitalReading r) {
        return points(hrLevel(r.heartRate)) + points(spo2Level(r.spo2))
             + points(bpLevel(r.systolic, r.diastolic)) + points(tempLevel(r.temperature));
    }

    private static int points(AlertLevel l) {
        return l == AlertLevel.CRITICAL ? 3 : l == AlertLevel.WARNING ? 2 : 0;
    }

    /** Returns the worst level and fills a message describing every abnormal vital. */
    public static AlertLevel classify(VitalReading r, StringBuilder msg) {
        AlertLevel worst = AlertLevel.NORMAL;
        worst = worse(worst, check(msg, "Heart rate " + r.heartRate + " bpm", hrLevel(r.heartRate)));
        worst = worse(worst, check(msg, "SpO2 " + r.spo2 + "%", spo2Level(r.spo2)));
        worst = worse(worst, check(msg, "BP " + r.systolic + "/" + r.diastolic + " mmHg", bpLevel(r.systolic, r.diastolic)));
        worst = worse(worst, check(msg, String.format("Temp %.1fC", r.temperature), tempLevel(r.temperature)));
        return worst;
    }

    private static AlertLevel check(StringBuilder msg, String text, AlertLevel level) {
        if (level != AlertLevel.NORMAL) {
            if (msg.length() > 0) msg.append("; ");
            msg.append(text);
        }
        return level;
    }

    private static AlertLevel worse(AlertLevel a, AlertLevel b) {
        return b.ordinal() > a.ordinal() ? b : a;
    }

    /** Decide if this alert should be emitted (throttling), and if so count + log it. */
    public AlertEvent process(Patient p, AlertLevel level, String message) {
        if (level == AlertLevel.NORMAL) {
            lastLevel.put(p.getId(), AlertLevel.NORMAL);
            return null;
        }
        long now = System.currentTimeMillis();
        AlertLevel prev = lastLevel.get(p.getId());
        Long prevT = lastTime.get(p.getId());
        boolean changed = prev != level;
        boolean stale = prevT == null || now - prevT > REPEAT_MS;
        if (!changed && !stale) return null;

        lastLevel.put(p.getId(), level);
        lastTime.put(p.getId(), now);

        if (level == AlertLevel.CRITICAL) criticalCount.incrementAndGet();
        else warningCount.incrementAndGet();

        AlertEvent ev = new AlertEvent(p, level, message);
        writeLog(ev);
        return ev;
    }

    private void writeLog(AlertEvent ev) {
        fileLock.lock();
        try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ev.time));
            pw.printf("[%s] %-8s %s (Room %s): %s%n", ts, ev.level, ev.patient.getName(),
                    ev.patient.getRoom(), ev.message);
        } catch (IOException e) {
            System.err.println("Could not write alerts.log: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    public int getWarningCount() { return warningCount.get(); }
    public int getCriticalCount() { return criticalCount.get(); }

    public void reset() {
        warningCount.set(0);
        criticalCount.set(0);
        lastLevel.clear();
        lastTime.clear();
    }
}
