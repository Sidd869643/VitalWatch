package hospital;

/** Immutable snapshot of one patient's vitals produced by a sensor thread. */
public final class VitalReading {
    /** Poison pill used to tell the analyzer thread to shut down. */
    public static final VitalReading POISON = new VitalReading(-1, 0, 0, 0, 0, 0);

    public final int patientId;
    public final int heartRate;      // bpm
    public final int spo2;           // %
    public final int systolic;       // mmHg
    public final int diastolic;      // mmHg
    public final double temperature; // Celsius
    public final long timestamp;

    public VitalReading(int patientId, int heartRate, int spo2,
                        int systolic, int diastolic, double temperature) {
        this.patientId = patientId;
        this.heartRate = heartRate;
        this.spo2 = spo2;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.temperature = temperature;
        this.timestamp = System.currentTimeMillis();
    }
}
