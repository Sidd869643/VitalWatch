package hospital;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Monitoring dashboard: filter bar, results/pager row and a paged grid of live patient cards. */
public class DashboardPage extends JPanel {
    private static final int PAGE_SIZE = 9, MIN_COL = 330, GAP = 16;
    private static final String ALL = "Default - All";

    private final HospitalController ctl;
    private final Map<Integer, PatientCard> cards = new LinkedHashMap<>();
    private final Map<Integer, Integer> signatures = new java.util.HashMap<>();

    // filter bar
    private final Ui.Select selRoom = new Ui.Select("Room", 170);
    private final Ui.Select selStatus = new Ui.Select("Status", 170);
    private final Ui.Select selParam = new Ui.Select("Parameter alerts", 190);
    private final Ui.Select selSort = new Ui.Select("Sort by", 170);
    private final Ui.IconButton btnGrid = new Ui.IconButton(Icons.DASH, "Grid view");
    private final Ui.IconButton btnList = new Ui.IconButton(Icons.LIST, "List view");

    // results row
    private final Ui.Label showing = new Ui.Label("", Ui.DIM, Font.PLAIN, 12);
    private final Ui.Pager pager = new Ui.Pager();
    private final StatChip cPatients, cHr, cWarn, cCrit;

    // grid
    private final JPanel grid = new JPanel();
    private final GridLayout gl = new GridLayout(0, 3, GAP, GAP);
    private final JPanel empty, noMatch;
    private int page = 0;
    private boolean listView = false;

    /** Compact stat in the results row: icon, value, caption. */
    static class StatChip extends JComponent {
        private final int icon;
        private final String caption;
        private final Supplier<Color> color;
        private String value = "0";
        private boolean compact;

        StatChip(String caption, int icon, Supplier<Color> color) {
            this.caption = caption; this.icon = icon; this.color = color;
            setToolTipText(caption);
            setFont(Theme.font(Font.BOLD, 15));
        }
        void setValue(String v) { if (!v.equals(value)) { value = v; revalidate(); repaint(); } }
        void setCompact(boolean c) { if (c != compact) { compact = c; revalidate(); repaint(); } }

        @Override public Dimension getPreferredSize() {
            FontMetrics vf = getFontMetrics(Theme.font(Font.BOLD, 15));
            FontMetrics cf = getFontMetrics(Theme.font(Font.PLAIN, 12));
            int w = 26 + vf.stringWidth(value) + (compact ? 0 : 6 + cf.stringWidth(caption));
            return new Dimension(w + 14, 28);
        }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.g2(g);
            Icons.draw(g2, icon, 0, (getHeight() - 18) / 2f, 18, color.get());
            g2.setFont(Theme.font(Font.BOLD, 15));
            FontMetrics vf = g2.getFontMetrics();
            int y = (getHeight() - vf.getHeight()) / 2 + vf.getAscent();
            g2.setColor(Theme.text);
            g2.drawString(value, 26, y);
            if (!compact) {
                g2.setFont(Theme.font(Font.PLAIN, 12));
                g2.setColor(Theme.dim);
                g2.drawString(caption, 26 + vf.stringWidth(value) + 6, y);
            }
            g2.dispose();
        }
    }

    public DashboardPage(HospitalController ctl, Runnable goPatients) {
        this.ctl = ctl;
        setOpaque(false);
        setLayout(new BorderLayout(0, 6));

        // ---------------------------------------------------------- filter bar
        selRoom.setItems(Arrays.asList(ALL));
        selStatus.setItems(Arrays.asList(ALL, "Critical", "Warning", "Stable", "Waiting for data"));
        selParam.setItems(Arrays.asList(ALL, "Heart rate", "SpO\u2082", "Blood pressure", "Temperature"));
        selSort.setItems(Arrays.asList("Default", "Name (A-Z)", "Room", "Severity"));
        for (Ui.Select s : new Ui.Select[]{selRoom, selStatus, selParam, selSort}) s.onChange(i -> { page = 0; refreshView(); });
        btnGrid.setSelected(true);
        btnGrid.addActionListener(e -> setListView(false));
        btnList.addActionListener(e -> setListView(true));

        // GridBag so the drop-downs shrink on narrow windows instead of wrapping out of sight
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setOpaque(false);
        Ui.Label fl = new Ui.Label("Filter", Ui.TEXT, Font.PLAIN, 13);
        fl.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.anchor = GridBagConstraints.NORTHWEST; gc.insets = new Insets(0, 0, 0, 14);
        filters.add(fl, gc);
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 0.01;
        for (Ui.Select s : new Ui.Select[]{selRoom, selStatus, selParam, selSort}) filters.add(s, gc);
        gc.weightx = 1; gc.fill = GridBagConstraints.NONE;
        filters.add(Box.createGlue(), gc);

        JPanel views = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        views.setOpaque(false);
        views.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        views.add(btnGrid); views.add(btnList);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(filters, BorderLayout.CENTER);
        bar.add(views, BorderLayout.EAST);

        // ----------------------------------------------------------- results row
        cPatients = new StatChip("patients", Icons.USERS, () -> Theme.cyan);
        cHr = new StatChip("avg heart rate", Icons.HEART, () -> Theme.crit);
        cWarn = new StatChip("warnings", Icons.BELL, () -> Theme.warn);
        cCrit = new StatChip("critical", Icons.PULSE, () -> Theme.crit);
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        stats.setOpaque(false);
        stats.add(cPatients); stats.add(cHr); stats.add(cWarn); stats.add(cCrit);

        pager.onPage(p -> { page = p; refreshView(); });
        JPanel pageBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        pageBox.setOpaque(false);
        pageBox.add(showing);
        pager.setPreferredSize(new Dimension(280, 30));
        pageBox.add(pager);

        JPanel results = new JPanel(new BorderLayout());
        results.setOpaque(false);
        results.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        results.add(stats, BorderLayout.WEST);
        results.add(pageBox, BorderLayout.EAST);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        bar.setAlignmentX(LEFT_ALIGNMENT);
        results.setAlignmentX(LEFT_ALIGNMENT);
        top.add(bar);
        top.add(Box.createVerticalStrut(6));
        top.add(results);
        top.add(Box.createVerticalStrut(4));
        add(top, BorderLayout.NORTH);

        // ------------------------------------------------------------------ grid
        grid.setOpaque(false);
        grid.setLayout(gl);
        grid.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { relayoutColumns(); }
        });

        Ui.ScrollPanel content = new Ui.ScrollPanel(null);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 12));
        grid.setAlignmentX(LEFT_ALIGNMENT);
        content.add(grid);

        empty = buildEmpty(goPatients);
        empty.setAlignmentX(LEFT_ALIGNMENT);
        content.add(empty);
        noMatch = buildNoMatch();
        noMatch.setAlignmentX(LEFT_ALIGNMENT);
        content.add(noMatch);

        add(Ui.scroll(content), BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                boolean compact = getWidth() < 1060;
                for (StatChip c : new StatChip[]{cPatients, cHr, cWarn, cCrit}) c.setCompact(compact);
                showing.setVisible(getWidth() >= 900);
            }
        });
        rebuild();
    }

    private JPanel buildEmpty(Runnable goPatients) {
        Ui.Card ec = new Ui.Card(new GridBagLayout());
        ec.setBorder(BorderFactory.createEmptyBorder(40, 24, 40, 24));
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        Ui.IconView ic = new Ui.IconView(Icons.PULSE, 64, true, () -> Theme.cyan);
        ic.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(ic);
        inner.add(Box.createVerticalStrut(16));
        Ui.Label t1 = new Ui.Label("No patients yet", Ui.TEXT, Font.BOLD, 18);
        t1.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(t1);
        inner.add(Box.createVerticalStrut(6));
        Ui.Label t2 = new Ui.Label("Add patients to start streaming their vitals in real time.", Ui.DIM, Font.PLAIN, 13);
        t2.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(t2);
        inner.add(Box.createVerticalStrut(18));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.setOpaque(false);
        Ui.Button sample = new Ui.Button("Add sample patients", Icons.PLUS, Ui.Kind.PRIMARY);
        Ui.Button manual = new Ui.Button("Add manually", -1, Ui.Kind.GHOST);
        sample.addActionListener(e -> { SamplePatients.addTo(ctl); rebuild(); });
        manual.addActionListener(e -> goPatients.run());
        row.add(sample); row.add(manual);
        row.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(row);
        ec.add(inner);
        return ec;
    }

    private JPanel buildNoMatch() {
        Ui.Card c = new Ui.Card(new GridBagLayout());
        c.setBorder(BorderFactory.createEmptyBorder(30, 24, 30, 24));
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        Ui.Label t1 = new Ui.Label("No patients match these filters", Ui.TEXT, Font.BOLD, 16);
        t1.setAlignmentX(CENTER_ALIGNMENT);
        inner.add(t1);
        inner.add(Box.createVerticalStrut(14));
        Ui.Button reset = new Ui.Button("Clear filters", -1, Ui.Kind.GHOST);
        reset.setAlignmentX(CENTER_ALIGNMENT);
        reset.addActionListener(e -> {
            selRoom.setSelectedIndex(0); selStatus.setSelectedIndex(0); selParam.setSelectedIndex(0);
        });
        inner.add(reset);
        c.add(inner);
        return c;
    }

    private void setListView(boolean list) {
        listView = list;
        btnGrid.setSelected(!list);
        btnList.setSelected(list);
        relayoutColumns();
    }

    private void relayoutColumns() {
        int cols = listView ? 1 : Math.max(1, (grid.getWidth() + GAP) / (MIN_COL + GAP));
        if (cols != gl.getColumns()) { gl.setColumns(cols); grid.revalidate(); grid.repaint(); }
    }

    // ------------------------------------------------------------ view model
    private void refreshRoomChoices() {
        List<String> rooms = new ArrayList<>();
        rooms.add(ALL);
        for (Patient p : ctl.getPatients()) if (!rooms.contains(p.getRoom())) rooms.add(p.getRoom());
        selRoom.setItems(rooms);
    }

    private boolean passes(PatientCard c) {
        Patient p = c.getPatient();
        if (selRoom.getSelectedIndex() > 0 && !p.getRoom().equals(selRoom.getSelectedItem())) return false;
        switch (selStatus.getSelectedIndex()) {
            case 1: if (c.getLevel() != AlertLevel.CRITICAL) return false; break;
            case 2: if (c.getLevel() != AlertLevel.WARNING) return false; break;
            case 3: if (c.getLevel() != AlertLevel.NORMAL) return false; break;
            case 4: if (c.hasLiveData()) return false; break;
            default:
        }
        int prm = selParam.getSelectedIndex();
        return prm == 0 || c.isAbnormal(prm - 1);
    }

    private List<PatientCard> visibleCards() {
        List<PatientCard> list = new ArrayList<>();
        for (Patient p : ctl.getPatients()) {
            PatientCard c = cards.get(p.getId());
            if (c != null && passes(c)) list.add(c);
        }
        Comparator<PatientCard> byId = Comparator.comparingInt(c -> c.getPatient().getId());
        switch (selSort.getSelectedIndex()) {
            case 1: list.sort(Comparator.comparing((PatientCard c) -> c.getPatient().getName().toLowerCase()).thenComparing(byId)); break;
            case 2: list.sort(Comparator.comparing((PatientCard c) -> roomKey(c.getPatient().getRoom())).thenComparing(byId)); break;
            case 3: list.sort(Comparator.comparingInt(PatientCard::severityRank).thenComparing(byId)); break;
            default: list.sort(byId);
        }
        return list;
    }

    /** Rooms like "9" and "101" sort numerically; anything else falls back to text. */
    private static String roomKey(String room) {
        try { return String.format("%09d", Integer.parseInt(room.trim())); }
        catch (NumberFormatException e) { return "~" + room.toLowerCase(); }
    }

    /** Re-apply filters, sort and paging to the card grid. */
    private void refreshView() {
        List<PatientCard> all = visibleCards();
        int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * PAGE_SIZE, to = Math.min(all.size(), from + PAGE_SIZE);

        grid.removeAll();
        for (PatientCard c : all.subList(from, to)) grid.add(c);

        boolean any = !cards.isEmpty();
        empty.setVisible(!any);
        noMatch.setVisible(any && all.isEmpty());
        grid.setVisible(!all.isEmpty());
        showing.setText("Showing " + (to - from) + " out of " + all.size() + " results");
        pager.set(page, pages);
        relayoutColumns();
        grid.revalidate();
        grid.repaint();
    }

    /** Sync cards with the controller's patient list. */
    public void rebuild() {
        cards.keySet().removeIf(id -> !ctl.hasPatient(id));
        signatures.keySet().removeIf(id -> !ctl.hasPatient(id));
        for (Patient p : ctl.getPatients()) {
            if (cards.containsKey(p.getId())) continue;
            PatientCard c = new PatientCard(p);
            VitalReading r = ctl.getLatestReadings().get(p.getId());
            if (r != null && ctl.isRunning()) c.update(r, AlertManager.classify(r, new StringBuilder()));
            else if (ctl.isRunning()) c.setWaiting();
            cards.put(p.getId(), c);
            signatures.put(p.getId(), c.signature());
        }
        refreshRoomChoices();
        refreshView();
        refreshStats();
    }

    public void onReading(VitalReading r) {
        PatientCard c = cards.get(r.patientId);
        if (c != null) {
            c.update(r, AlertManager.classify(r, new StringBuilder()));
            int sig = c.signature();
            Integer old = signatures.put(r.patientId, sig);
            boolean dependsOnState = selStatus.getSelectedIndex() > 0 || selParam.getSelectedIndex() > 0
                    || selSort.getSelectedIndex() == 3;
            if (dependsOnState && (old == null || old != sig)) refreshView();
        }
        refreshStats();
    }

    public void refreshStats() {
        cPatients.setValue(String.valueOf(ctl.getPatients().size()));
        int sum = 0, n = 0;
        for (VitalReading r : ctl.getLatestReadings().values()) { sum += r.heartRate; n++; }
        cHr.setValue(n == 0 ? "--" : (sum / n) + " bpm");
        cWarn.setValue(String.valueOf(ctl.getAlertManager().getWarningCount()));
        cCrit.setValue(String.valueOf(ctl.getAlertManager().getCriticalCount()));
    }

    public void monitoringStarted() {
        for (PatientCard c : cards.values()) { c.setWaiting(); signatures.put(c.getPatient().getId(), c.signature()); }
        refreshView();
        refreshStats();
    }

    public void monitoringStopped() {
        for (PatientCard c : cards.values()) { c.setIdle(); signatures.put(c.getPatient().getId(), c.signature()); }
        refreshView();
        refreshStats();
    }

    /** Shared animation clock: moves the ECG traces and pulses critical borders. */
    public void tick() { for (PatientCard c : cards.values()) c.tick(); }
}
