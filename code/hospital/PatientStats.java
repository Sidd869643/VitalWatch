package hospital;

/** Running statistics per patient, used for the final report. Thread-safe via synchronization. */
public class PatientStats {
    private int count;
    private long hrSum, spo2Sum;
    private int hrMin = Integer.MAX_VALUE, hrMax = Integer.MIN_VALUE;
    private int spo2Min = Integer.MAX_VALUE;
    private double tempMax = Double.NEGATIVE_INFINITY;
    private int warnings, criticals;

    public synchronized void update(VitalReading r, AlertLevel level) {
        count++;
        hrSum += r.heartRate;
        spo2Sum += r.spo2;
        hrMin = Math.min(hrMin, r.heartRate);
        hrMax = Math.max(hrMax, r.heartRate);
        spo2Min = Math.min(spo2Min, r.spo2);
        tempMax = Math.max(tempMax, r.temperature);
        if (level == AlertLevel.WARNING) warnings++;
        if (level == AlertLevel.CRITICAL) criticals++;
    }

    public synchronized String summary() {
        if (count == 0) return "no readings";
        return String.format(
            "readings=%d | HR avg=%.1f (min %d, max %d) | SpO2 avg=%.1f (min %d) | max temp=%.1fC | warnings=%d, critical=%d",
            count, hrSum / (double) count, hrMin, hrMax, spo2Sum / (double) count, spo2Min,
            tempMax, warnings, criticals);
    }
}
