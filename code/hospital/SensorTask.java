package hospital;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

/** One sensor per patient: a Runnable that simulates vitals and pushes them to the queue. */
public class SensorTask implements Runnable {
    private final Patient patient;
    private final BlockingQueue<VitalReading> queue;
    private final CountDownLatch latch;
    private final HospitalController controller; // reads the volatile running flag
    private final Random rnd = new Random();

    private double hr = 72 + rnd.nextInt(14);
    private double spo2 = 97 + rnd.nextInt(3);
    private double sys = 115 + rnd.nextInt(15);
    private double dia = 72 + rnd.nextInt(10);
    private double temp = 36.6 + rnd.nextDouble() * 0.5;

    public SensorTask(Patient patient, BlockingQueue<VitalReading> queue,
                      CountDownLatch latch, HospitalController controller) {
        this.patient = patient;
        this.queue = queue;
        this.latch = latch;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            while (controller.isRunning() && controller.hasPatient(patient.getId())) {
                step();
                queue.put(new VitalReading(patient.getId(), (int) Math.round(hr),
                        (int) Math.round(spo2), (int) Math.round(sys),
                        (int) Math.round(dia), Math.round(temp * 10) / 10.0));
                Thread.sleep(900 + rnd.nextInt(400));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown();
        }
    }

    /** Random walk with occasional "events" so alerts actually show up in the demo. */
    private void step() {
        hr += rnd.nextGaussian() * 2.5 + (72 - hr) * 0.05;
        spo2 += rnd.nextGaussian() * 0.6 + (97.5 - spo2) * 0.1;
        sys += rnd.nextGaussian() * 3 + (120 - sys) * 0.05;
        dia += rnd.nextGaussian() * 2 + (78 - dia) * 0.05;
        temp += rnd.nextGaussian() * 0.05 + (36.8 - temp) * 0.05;

        int event = rnd.nextInt(100);
        if (event < 3) { hr += 35 + rnd.nextInt(25); sys += 30; }          // tachycardia / hypertension
        else if (event < 5) { spo2 -= 6 + rnd.nextInt(5); }                // oxygen dip
        else if (event < 7) { temp += 1.2 + rnd.nextDouble(); }            // fever spike
        else if (event < 8) { hr -= 25; sys -= 25; }                       // bradycardia / hypotension

        hr = clamp(hr, 30, 180);
        spo2 = clamp(spo2, 78, 100);
        sys = clamp(sys, 70, 210);
        dia = clamp(dia, 40, 130);
        temp = clamp(temp, 34, 41.5);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
