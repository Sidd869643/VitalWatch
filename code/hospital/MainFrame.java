package hospital;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Application window: top navigation bar + pages + alert ticker. */
public class MainFrame extends JFrame implements HospitalController.Listener {
    private static final String[] NAMES = {"Dashboard", "Patients", "Live Alerts", "Reports"};
    private static final String[] SUBS = {
        "Live overview of every monitored patient",
        "Add or remove patients on the ward",
        "Real-time warnings and critical events",
        "Snapshots and the final session report"};

    private final HospitalController ctl;
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = new JPanel(cards);
    private final NavBar nav;
    private final AlertTicker ticker;
    private final Ui.Label title = new Ui.Label("", Ui.TEXT, Font.BOLD, 22);
    private final Ui.Label subtitle = new Ui.Label("", Ui.DIM, Font.PLAIN, 13);
    private final JPanel titles = new JPanel();
    private final Ui.Button startStop = new Ui.Button("Start monitoring", Icons.PLAY, Ui.Kind.PRIMARY);
    private final Ui.IconButton themeBtn = new Ui.IconButton(Icons.SUN, "Switch to light mode");
    private final Live live = new Live();

    private final DashboardPage dashboard;
    private final PatientsPage patientsPage;
    private final AlertsPage alertsPage = new AlertsPage();
    private final ReportsPage reportsPage;

    private int current = 0;
    private int unread = 0;
    private boolean stopping = false;

    /** Animated LIVE / IDLE pill in the top bar. */
    private class Live extends JComponent {
        Live() { setPreferredSize(new Dimension(86, 30)); setFont(Theme.font(Font.BOLD, 11)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.g2(g);
            boolean on = ctl.isRunning();
            Color c = on ? Theme.ok : Theme.dim;
            g2.setColor(Theme.alpha(c, 34));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 30, 30));
            if (on) {
                float r = 4 + Theme.pulse * 4;
                g2.setColor(Theme.alpha(c, (int) (110 * (1 - Theme.pulse))));
                g2.fill(new Ellipse2D.Float(16 - r, 15 - r, r * 2, r * 2));
            }
            g2.setColor(c);
            g2.fill(new Ellipse2D.Float(12, 11, 8, 8));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(on ? "LIVE" : "IDLE", 30, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    public MainFrame(HospitalController controller) {
        super("VitalWatch - Hospital Vitals Monitoring");
        this.ctl = controller;
        controller.setListener(this);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                ctl.shutdownNow();
                dispose();
                System.exit(0);
            }
        });

        dashboard = new DashboardPage(ctl, () -> navigate(1));
        patientsPage = new PatientsPage(ctl, this::patientsChanged);
        reportsPage = new ReportsPage(ctl);
        ticker = new AlertTicker(() -> navigate(2));
        alertsPage.setOnClear(ticker::clear);

        pages.setOpaque(false);
        pages.add(dashboard, "0");
        pages.add(patientsPage, "1");
        pages.add(alertsPage, "2");
        pages.add(reportsPage, "3");

        // right-hand controls in the top bar
        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.weighty = 1; gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(0, 0, 0, 12);
        actions.add(live, gc);
        actions.add(startStop, gc);
        gc.insets = new Insets(0, 0, 0, 0);
        actions.add(themeBtn, gc);

        nav = new NavBar(NAMES, SUBS, this::navigate, actions);

        // page heading (shown on every page except the dashboard)
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(subtitle);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(14, 28, 10, 16));
        center.add(titles, BorderLayout.NORTH);
        center.add(pages, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, Theme.bg, getWidth(), getHeight(), Theme.bg2));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.add(nav, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(ticker, BorderLayout.SOUTH);
        setContentPane(root);

        startStop.addActionListener(e -> onStartStop());
        themeBtn.addActionListener(e -> toggleTheme());

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { nav.setCompact(getWidth() < 1120); }
        });

        // one animation clock for pulsing elements and the ECG traces
        new javax.swing.Timer(60, e -> {
            Theme.pulse = (float) (0.5 + 0.5 * Math.sin(System.nanoTime() / 1e9 * 4.0));
            live.repaint();
            dashboard.tick();
        }).start();

        navigate(0);
        setMinimumSize(new Dimension(900, 620));
        setSize(1280, 820);
        setLocationRelativeTo(null);
    }

    private void toggleTheme() {
        Theme.apply(!Theme.dark);
        themeBtn.setIcon(Theme.dark ? Icons.SUN : Icons.MOON, Theme.dark ? "Switch to light mode" : "Switch to dark mode");
        getContentPane().repaint();
    }

    // ----------------------------------------------------------- navigation
    private void navigate(int i) {
        current = i;
        cards.show(pages, String.valueOf(i));
        title.setText(NAMES[i]);
        subtitle.setText(SUBS[i]);
        titles.setVisible(i != 0);
        nav.select(i);
        if (i == 2) { unread = 0; nav.setBadge(2, 0); }
    }

    private void patientsChanged() {
        dashboard.rebuild();
        patientsPage.refresh();
    }

    // ------------------------------------------------------ start / stop
    private void onStartStop() {
        if (stopping) return;
        if (ctl.isRunning()) {
            stopping = true;
            ctl.stopMonitoring();
            startStop.setText("Stopping...");
            startStop.setEnabled(false);
            return;
        }
        if (ctl.getPatients().isEmpty()) {
            Ui.confirm(this, "No patients", "Add at least one patient before starting monitoring.", "Go to Patients", false, true);
            navigate(1);
            return;
        }
        alertsPage.clearAll();
        ticker.clear();
        unread = 0;
        nav.setBadge(2, 0);
        if (ctl.startMonitoring()) {
            dashboard.monitoringStarted();
            setRunningUi(true);
        }
    }

    private void setRunningUi(boolean running) {
        startStop.setEnabled(true);
        if (running) {
            startStop.setText("Stop monitoring");
            startStop.setIcon(Icons.STOP);
            startStop.setKind(Ui.Kind.DANGER);
        } else {
            startStop.setText("Start monitoring");
            startStop.setIcon(Icons.PLAY);
            startStop.setKind(Ui.Kind.PRIMARY);
        }
        live.repaint();
    }

    // ------------------------------------- Listener (called from worker threads)
    @Override public void onReading(VitalReading r) {
        SwingUtilities.invokeLater(() -> dashboard.onReading(r));
    }

    @Override public void onAlert(AlertEvent ev) {
        SwingUtilities.invokeLater(() -> {
            alertsPage.add(ev);
            ticker.add(ev);
            if (current != 2) { unread++; nav.setBadge(2, unread); }
            dashboard.refreshStats();
        });
    }

    @Override public void onMonitoringStopped(String report) {
        SwingUtilities.invokeLater(() -> {
            stopping = false;
            setRunningUi(false);
            dashboard.monitoringStopped();
            reportsPage.setReport(report);
            if (Ui.confirm(this, "Monitoring stopped",
                    "The final report was saved to final_report.txt.", "View report", false, true)) {
                navigate(3);
            }
        });
    }
}
