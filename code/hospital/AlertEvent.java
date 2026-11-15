package hospital;

/** An alert raised by the analyzer. */
public final class AlertEvent {
    public final long time;
    public final Patient patient;
    public final AlertLevel level;
    public final String message;

    public AlertEvent(Patient patient, AlertLevel level, String message) {
        this.time = System.currentTimeMillis();
        this.patient = patient;
        this.level = level;
        this.message = message;
    }
}
