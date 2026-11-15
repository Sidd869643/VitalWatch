package hospital;

/** Demo data helper. */
public final class SamplePatients {
    private SamplePatients() { }

    public static void addTo(HospitalController c) {
        c.addPatient("Asha Rao", 58, "101");
        c.addPatient("Ravi Kumar", 72, "102");
        c.addPatient("Meena Iyer", 45, "103");
        c.addPatient("John Paul", 66, "104");
    }
}
