package hospital;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AlertsPage extends JPanel {
    private static final int KEEP = 300, SHOWN = 150;

    private final List<AlertEvent> events = new ArrayList<>(); // newest first
    private final Ui.ScrollPanel list = new Ui.ScrollPanel(null);
    private final Ui.Label count = new Ui.Label("0 alerts", Ui.DIM, Font.PLAIN, 13);
    private final JPanel emptyHint;
    private AlertLevel filter = null; // null = all
    private Runnable onClear = () -> { };
    private final Ui.Button fAll = new Ui.Button("All", -1, Ui.Kind.GHOST);
    private final Ui.Button fWarn = new Ui.Button("Warnings", -1, Ui.Kind.GHOST);
    private final Ui.Button fCrit = new Ui.Button("Critical", -1, Ui.Kind.GHOST);

    public AlertsPage() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
        chips.setOpaque(false);
        chips.add(fAll); chips.add(fWarn); chips.add(fCrit);
        chips.add(Box.createHorizontalStrut(6));
        chips.add(count);
        bar.add(chips, BorderLayout.WEST);
        Ui.Button clear = new Ui.Button("Clear", Icons.TRASH, Ui.Kind.GHOST);
        bar.add(clear, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 12));
        add(Ui.scroll(list), BorderLayout.CENTER);

        emptyHint = new Ui.Card(new GridBagLayout());
        emptyHint.setBorder(BorderFactory.createEmptyBorder(36, 20, 36, 20));
        emptyHint.setAlignmentX(LEFT_ALIGNMENT);
        JPanel in = new JPanel();
        in.setOpaque(false);
        in.setLayout(new BoxLayout(in, BoxLayout.Y_AXIS));
        Ui.IconView iv = new Ui.IconView(Icons.BELL, 56, true, () -> Theme.ok);
        iv.setAlignmentX(CENTER_ALIGNMENT);
        in.add(iv);
        in.add(Box.createVerticalStrut(12));
        Ui.Label l1 = new Ui.Label("All clear", Ui.TEXT, Font.BOLD, 17);
        l1.setAlignmentX(CENTER_ALIGNMENT);
        in.add(l1);
        Ui.Label l2 = new Ui.Label("Alerts will appear here the moment a vital goes out of range.", Ui.DIM, Font.PLAIN, 13);
        l2.setAlignmentX(CENTER_ALIGNMENT);
        in.add(Box.createVerticalStrut(4));
        in.add(l2);
        emptyHint.add(in);

        fAll.addActionListener(e -> setFilter(null));
        fWarn.addActionListener(e -> setFilter(AlertLevel.WARNING));
        fCrit.addActionListener(e -> setFilter(AlertLevel.CRITICAL));
        clear.addActionListener(e -> { events.clear(); rebuild(); onClear.run(); });
        setFilter(null);
    }

    /** Called after the user presses Clear (lets the bottom ticker clear too). */
    public void setOnClear(Runnable r) { onClear = r; }

    private void setFilter(AlertLevel f) {
        filter = f;
        fAll.setSelected(f == null);
        fWarn.setSelected(f == AlertLevel.WARNING);
        fCrit.setSelected(f == AlertLevel.CRITICAL);
        rebuild();
    }

    public void add(AlertEvent ev) {
        events.add(0, ev);
        if (events.size() > KEEP) events.remove(events.size() - 1);
        rebuild();
    }

    public void clearAll() { events.clear(); rebuild(); }

    private void rebuild() {
        list.removeAll();
        int shown = 0;
        for (AlertEvent ev : events) {
            if (filter != null && ev.level != filter) continue;
            list.add(new Row(ev));
            list.add(Box.createVerticalStrut(8));
            if (++shown >= SHOWN) break;
        }
        if (shown == 0) list.add(emptyHint);
        count.setText(events.size() + (events.size() == 1 ? " alert" : " alerts"));
        list.revalidate();
        list.repaint();
    }

    private static class Row extends Ui.Card {
        private final Color stripe;

        Row(AlertEvent ev) {
            super(new BorderLayout(14, 0));
            radius = 8;
            stripe = Theme.level(ev.level);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(11, 20, 11, 16));

            JPanel badgeBox = new JPanel(new GridBagLayout());
            badgeBox.setOpaque(false);
            Ui.Badge b = new Ui.Badge();
            b.set(ev.level.name(), Theme.level(ev.level));
            badgeBox.add(b);
            add(badgeBox, BorderLayout.WEST);

            JPanel t = new JPanel();
            t.setOpaque(false);
            t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
            t.add(new Ui.Label(ev.patient.getName() + "  \u00B7  Room " + ev.patient.getRoom(), Ui.TEXT, Font.BOLD, 14));
            t.add(new Ui.Label(ev.message, Ui.DIM, Font.PLAIN, 13));
            add(t, BorderLayout.CENTER);

            add(new Ui.Label(new SimpleDateFormat("HH:mm:ss").format(new Date(ev.time)), Ui.DIM, Font.PLAIN, 12),
                    BorderLayout.EAST);
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Ui.g2(g);
            g2.setColor(stripe);
            g2.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);
            g2.dispose();
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }
}
