package hospital;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/** One live patient tile on the dashboard: header, ECG strip, vitals row. */
public class PatientCard extends Ui.Card {
    public static final int HR = 0, SPO2 = 1, BP = 2, TEMP = 3;

    private final Patient patient;
    private AlertLevel level;          // null = idle / waiting
    private boolean hasData;
    private boolean waiting;           // monitoring started, first reading not in yet
    private final AlertLevel[] levels = new AlertLevel[4];

    private final Header header;
    private final EcgStrip strip = new EcgStrip();
    private final Vitals vitals = new Vitals();

    public PatientCard(Patient p) {
        super(new BorderLayout());
        this.patient = p;
        radius = 8;
        setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        header = new Header(p);
        add(header, BorderLayout.NORTH);
        add(strip, BorderLayout.CENTER);
        add(vitals, BorderLayout.SOUTH);
        setWaiting();
        setIdle();
    }

    public Patient getPatient() { return patient; }
    public boolean isCritical() { return level == AlertLevel.CRITICAL; }
    public AlertLevel getLevel() { return level; }
    public boolean hasLiveData() { return level != null; }

    /** True when that vital (HR, SPO2, BP, TEMP) is currently outside its normal range. */
    public boolean isAbnormal(int param) {
        return level != null && levels[param] != null && levels[param] != AlertLevel.NORMAL;
    }

    /** Lower = more urgent. Used by the "Severity" sort. */
    public int severityRank() {
        if (level == AlertLevel.CRITICAL) return 0;
        if (level == AlertLevel.WARNING) return 1;
        if (level == AlertLevel.NORMAL) return 2;
        return 3;
    }

    /** Changes whenever the alert level or the set of abnormal vitals changes. */
    public int signature() {
        int s = level == null ? 9 : level.ordinal();
        for (int i = 0; i < 4; i++) s = s * 4 + (levels[i] == null ? 3 : levels[i].ordinal());
        return s;
    }

    // ------------------------------------------------------------ card chrome
    @Override protected Color borderColor() {
        if (level == AlertLevel.CRITICAL) return Theme.alpha(Theme.crit, 110 + (int) (Theme.pulse * 145));
        if (level == AlertLevel.WARNING) return Theme.warn;
        if (waiting) return Theme.cyan;
        return Theme.dark ? Theme.alpha(Theme.border, 120) : Theme.border;
    }

    @Override protected Stroke borderStroke() {
        if (level == AlertLevel.CRITICAL || level == AlertLevel.WARNING) {
            return new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f);
        }
        if (waiting) return new BasicStroke(2f);
        return new BasicStroke(1f);
    }

    @Override public Dimension getPreferredSize() {
        return new Dimension(330, 174);
    }

    // ----------------------------------------------------------------- state
    public void update(VitalReading r, AlertLevel overall) {
        hasData = true;
        waiting = false;
        level = overall;
        levels[HR] = AlertManager.hrLevel(r.heartRate);
        levels[SPO2] = AlertManager.spo2Level(r.spo2);
        levels[BP] = AlertManager.bpLevel(r.systolic, r.diastolic);
        levels[TEMP] = AlertManager.tempLevel(r.temperature);

        vitals.set(0, String.valueOf(r.heartRate), levels[HR]);
        vitals.set(1, String.valueOf(r.spo2), levels[SPO2]);
        vitals.set(2, r.systolic + "/" + r.diastolic, levels[BP]);
        vitals.set(3, String.format("%.1f", r.temperature), levels[TEMP]);
        String name = overall == AlertLevel.CRITICAL ? "Critical" : overall == AlertLevel.WARNING ? "Warning" : "Stable";
        vitals.setStatus(name, () -> Theme.level(overall));

        header.set("Updated " + new SimpleDateFormat("HH:mm:ss").format(new Date(r.timestamp)), AlertManager.ews(r));
        strip.push(r.heartRate);
        repaint();
    }

    public void setWaiting() {
        hasData = false;
        waiting = true;
        level = null;
        java.util.Arrays.fill(levels, null);
        vitals.reset();
        vitals.setStatus("Waiting", () -> Theme.cyan);
        header.set("Waiting for sensor", -1);
        strip.showMessage("Waiting for sensor...", () -> Theme.cyan, false);
        repaint();
    }

    public void setIdle() {
        waiting = false;
        level = null;
        vitals.setStatus("Idle", null);
        header.setUpdated(hasData ? header.updated : "Not monitoring");
        strip.showMessage(hasData ? "Monitoring stopped" : "Not monitoring", null, hasData);
        repaint();
    }

    /** Called from the shared animation timer. */
    public void tick() {
        strip.tick();
        if (level == AlertLevel.CRITICAL) repaint(); // pulsing border
    }

    // ---------------------------------------------------------------- header
    private static class Header extends JComponent {
        private final Patient p;
        private String updated = "Not monitoring";
        private int ews = -1;

        Header(Patient p) {
            this.p = p;
            setPreferredSize(new Dimension(10, 58));
            setToolTipText("EWS = early warning score. Each vital adds 0 (normal), 2 (warning) or 3 (critical).");
        }

        void set(String updated, int ews) { this.updated = updated; this.ews = ews; repaint(); }
        void setUpdated(String u) { updated = u; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.g2(g);
            int w = getWidth(), h = getHeight();

            // sensor id + last update
            Icons.draw(g2, Icons.SENSOR, 16, 4, 22, Theme.text);
            g2.setFont(Theme.font(Font.BOLD, 11));
            g2.setColor(Theme.text);
            g2.drawString(String.format("ID %03d", p.getId()), 44, 20);
            g2.setFont(Theme.font(Font.ITALIC, 9.5f));
            g2.setColor(Theme.dim);
            g2.drawString(updated, 16, 46);

            // divider
            g2.setColor(Theme.alpha(Theme.text, 60));
            g2.fillRect(108, 8, 1, h - 16);

            // EWS block (right)
            int rightEdge = w - 16;
            String num = ews < 0 ? "--" : String.valueOf(ews);
            Color ec = ews < 0 ? Theme.dim : ews >= 5 ? Theme.crit : ews >= 1 ? Theme.warn : Theme.ok;
            g2.setFont(Theme.font(Font.BOLD, 13));
            FontMetrics f1 = g2.getFontMetrics();
            g2.setColor(ews < 0 ? Theme.dim : Theme.warn);
            g2.drawString("EWS", rightEdge - f1.stringWidth("EWS"), 20);
            g2.setFont(Theme.font(Font.BOLD, 26));
            FontMetrics f2 = g2.getFontMetrics();
            g2.setColor(ec);
            g2.drawString(num, rightEdge - f2.stringWidth(num), 49);
            int ewsW = Math.max(f1.stringWidth("EWS"), f2.stringWidth(num));

            // name / room / age
            int nx = 120, maxW = Math.max(40, rightEdge - ewsW - 12 - nx);
            g2.setFont(Theme.font(Font.BOLD, 15));
            g2.setColor(Theme.text);
            g2.drawString(fit(g2, p.getName(), maxW), nx, 21);
            g2.setFont(Theme.font(Font.PLAIN, 12));
            g2.setColor(Theme.alpha(Theme.text, 225));
            g2.drawString(fit(g2, "Room " + p.getRoom(), maxW), nx, 38);
            g2.setFont(Theme.font(Font.PLAIN, 10.5f));
            g2.setColor(Theme.cyan);
            g2.drawString("Age " + p.getAge(), nx, 52);
            g2.dispose();
        }

        private static String fit(Graphics2D g2, String s, int maxW) {
            FontMetrics fm = g2.getFontMetrics();
            while (s.length() > 1 && fm.stringWidth(s) > maxW) s = s.substring(0, s.length() - 2) + "\u2026";
            return s;
        }
    }

    // ---------------------------------------------------------------- vitals
    private static class Vitals extends JComponent {
        private static final String[] CAPTION = {"HR", "SpO\u2082", "BP", "TEMP"};
        private static final String[] UNIT = {"bpm", "%", "mmHg", "\u00B0C"};
        private static final int[] ICON = {Icons.HEART, Icons.DROP, Icons.GAUGE, Icons.THERMO};

        private final String[] value = new String[4];
        private final AlertLevel[] lvl = new AlertLevel[4];
        private String status = "Idle";
        private java.util.function.Supplier<Color> statusColor;

        Vitals() { setPreferredSize(new Dimension(10, 66)); }

        void set(int i, String v, AlertLevel l) { value[i] = v; lvl[i] = l; repaint(); }
        void setStatus(String s, java.util.function.Supplier<Color> c) { status = s; statusColor = c; repaint(); }
        void reset() { java.util.Arrays.fill(value, null); java.util.Arrays.fill(lvl, null); repaint(); }

        private static Color signature(int i) {
            switch (i) {
                case 0: return Theme.vHr;
                case 1: return Theme.vSpo2;
                case 2: return Theme.vBp;
                default: return Theme.vTemp;
            }
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.g2(g);
            int pad = 12, w = getWidth() - pad * 2;
            double unit = w / 4.8;               // four vitals + a slightly narrower status column
            for (int i = 0; i < 4; i++) {
                double cx = pad + unit * i + unit / 2;
                boolean has = value[i] != null;
                AlertLevel l = lvl[i];
                boolean bad = has && l != AlertLevel.NORMAL;
                Color base = signature(i);
                Color vc = !has ? Theme.alpha(Theme.cyan, 200) : l == AlertLevel.CRITICAL ? Theme.crit : l == AlertLevel.WARNING ? Theme.warn : base;
                Color ic = bad ? vc : base;

                // caption row: icon + label (+ warning triangle)
                g2.setFont(Theme.font(Font.BOLD, 10.5f));
                FontMetrics cf = g2.getFontMetrics();
                int cw = 15 + 3 + cf.stringWidth(CAPTION[i]) + (bad ? 14 : 0);
                float x0 = (float) (cx - cw / 2.0);
                Icons.draw(g2, ICON[i], x0, 2, 15, ic);
                g2.setColor(ic);
                g2.drawString(CAPTION[i], x0 + 18, 14);
                if (bad) Icons.draw(g2, Icons.WARN, x0 + 18 + cf.stringWidth(CAPTION[i]) + 3, 4, 11, vc);

                // value, shrunk to fit its column
                String v = has ? value[i] : "--";
                float size = 26f;
                Font f = Theme.font(Font.BOLD, size);
                FontMetrics vf = g2.getFontMetrics(f);
                while (size > 13f && vf.stringWidth(v) > unit - 6) {
                    size -= 1f;
                    f = Theme.font(Font.BOLD, size);
                    vf = g2.getFontMetrics(f);
                }
                g2.setFont(f);
                g2.setColor(vc);
                g2.drawString(v, (float) (cx - vf.stringWidth(v) / 2.0), 44);

                g2.setFont(Theme.font(Font.PLAIN, 10));
                g2.setColor(Theme.dim);
                FontMetrics uf = g2.getFontMetrics();
                g2.drawString(UNIT[i], (float) (cx - uf.stringWidth(UNIT[i]) / 2.0), 58);
            }

            // status column
            double sx = pad + unit * 4 + unit * 0.42;
            g2.setFont(Theme.font(Font.PLAIN, 10));
            g2.setColor(Theme.dim);
            FontMetrics sf = g2.getFontMetrics();
            g2.drawString("Status", (float) (sx - sf.stringWidth("Status") / 2.0), 14);
            Color sc = statusColor != null ? statusColor.get() : Theme.dim;
            float size = 12.5f;
            Font stf = Theme.font(Font.BOLD, size);
            FontMetrics bf = g2.getFontMetrics(stf);
            while (size > 9f && bf.stringWidth(status) > unit * 0.95) {
                size -= 0.5f;
                stf = Theme.font(Font.BOLD, size);
                bf = g2.getFontMetrics(stf);
            }
            g2.setFont(stf);
            g2.setColor(sc);
            g2.drawString(status, (float) (sx - bf.stringWidth(status) / 2.0), 42);
            // small coloured bar under the status text
            g2.setColor(Theme.alpha(sc, 150));
            g2.fill(new java.awt.geom.RoundRectangle2D.Float((float) (sx - 10), 49, 20, 3, 3, 3));
            g2.dispose();
        }
    }
}
