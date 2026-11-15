package hospital;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class PatientsPage extends JPanel {
    private final HospitalController ctl;
    private final Runnable onChanged;

    private final Ui.Field tfName = new Ui.Field("e.g. Priya Sharma");
    private final Ui.Field tfAge = new Ui.Field("e.g. 54");
    private final Ui.Field tfRoom = new Ui.Field("e.g. 105");
    private final Ui.Label error = new Ui.Label(" ", Ui.DIM, Font.PLAIN, 12);
    private final Ui.Label listTitle = new Ui.Label("Patients", Ui.TEXT, Font.BOLD, 17);
    private final Ui.ScrollPanel list = new Ui.ScrollPanel(null);
    private final JPanel emptyHint;
    private Ui.Button removeAll;

    private boolean wide = true, laidOut = false;
    private final JPanel holder;
    private final JPanel right = new JPanel(new BorderLayout(0, 12));

    public PatientsPage(HospitalController ctl, Runnable onChanged) {
        this.ctl = ctl;
        this.onChanged = onChanged;
        setOpaque(false);
        setLayout(new BorderLayout(20, 16));

        // ---------- form card
        Ui.Card form = new Ui.Card();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        form.add(left(new Ui.Label("Add patient", Ui.TEXT, Font.BOLD, 17)));
        form.add(left(new Ui.Label("Enter the details to register a patient.", Ui.DIM, Font.PLAIN, 12)));
        form.add(Box.createVerticalStrut(18));
        form.add(left(caption("FULL NAME")));
        form.add(Box.createVerticalStrut(5));
        form.add(left(tfName));
        form.add(Box.createVerticalStrut(12));
        JPanel two = new JPanel(new GridLayout(1, 2, 10, 0));
        two.setOpaque(false);
        JPanel a = stack(caption("AGE"), tfAge);
        JPanel b = stack(caption("ROOM"), tfRoom);
        two.add(a); two.add(b);
        form.add(left(two));
        form.add(Box.createVerticalStrut(8));
        error.setColor(Theme.crit);
        form.add(left(error));
        form.add(Box.createVerticalStrut(8));
        Ui.Button add = new Ui.Button("Add patient", Icons.PLUS, Ui.Kind.PRIMARY);
        form.add(left(add));
        form.add(Box.createVerticalStrut(18));
        JSeparator sep = new JSeparator() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.border);
                g.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
        };
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        form.add(left(sep));
        form.add(Box.createVerticalStrut(16));
        Ui.Button sample = new Ui.Button("Add 4 sample patients", -1, Ui.Kind.GHOST);
        form.add(left(sample));

        holder = new JPanel(new BorderLayout()) {
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return wide ? new Dimension(330, d.height) : d;
            }
        };
        holder.setOpaque(false);
        holder.add(form, BorderLayout.NORTH);

        // ---------- list
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        head.add(listTitle, BorderLayout.WEST);
        removeAll = new Ui.Button("Remove all", Icons.TRASH, Ui.Kind.DANGER_GHOST);
        head.add(removeAll, BorderLayout.EAST);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 12));
        right.setOpaque(false);
        right.add(head, BorderLayout.NORTH);
        right.add(Ui.scroll(list), BorderLayout.CENTER);

        emptyHint = new JPanel(new FlowLayout(FlowLayout.CENTER));
        emptyHint.setOpaque(false);
        emptyHint.add(new Ui.Label("No patients registered yet.", Ui.DIM, Font.PLAIN, 14));

        add.addActionListener(e -> addPatient());
        tfRoom.addActionListener(e -> addPatient());
        sample.addActionListener(e -> {
            if (!canEdit()) return;
            SamplePatients.addTo(ctl);
            changed();
        });
        removeAll.addActionListener(e -> {
            int n = ctl.getPatients().size();
            if (n == 0) return;
            String extra = ctl.isRunning() ? "\nTheir live sensors will be stopped." : "";
            if (Ui.confirm(this, "Remove all patients?", "This removes all " + n + " patients." + extra, "Remove all", true, true)) {
                ctl.removeAllPatients();
                changed();
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { relayout(); }
        });
        relayout();
        refresh();
    }

    private boolean canEdit() {
        if (ctl.isRunning() || ctl.isActive()) {
            error.setText("Stop monitoring before adding patients.");
            return false;
        }
        return true;
    }

    private void addPatient() {
        if (!canEdit()) return;
        String name = tfName.getText().trim(), room = tfRoom.getText().trim();
        int age;
        try {
            age = Integer.parseInt(tfAge.getText().trim());
            if (age < 0 || age > 130) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            error.setText("Enter a valid age between 0 and 130.");
            return;
        }
        if (name.isEmpty() || room.isEmpty()) { error.setText("Name and room are required."); return; }
        ctl.addPatient(name, age, room);
        tfName.setText(""); tfAge.setText(""); tfRoom.setText("");
        error.setText(" ");
        tfName.requestFocusInWindow();
        changed();
    }

    private void changed() { refresh(); onChanged.run(); }

    private void relayout() {
        boolean nowWide = getWidth() == 0 || getWidth() >= 860;
        if (laidOut && nowWide == wide) return;
        wide = nowWide;
        laidOut = true;
        remove(holder);
        remove(right);
        add(holder, wide ? BorderLayout.WEST : BorderLayout.NORTH);
        add(right, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void refresh() {
        list.removeAll();
        java.util.List<Patient> ps = ctl.getPatients();
        listTitle.setText("Patients (" + ps.size() + ")");
        removeAll.setVisible(!ps.isEmpty());
        if (ps.isEmpty()) {
            list.add(emptyHint);
        } else {
            for (Patient p : ps) {
                list.add(new Row(p));
                list.add(Box.createVerticalStrut(10));
            }
        }
        list.revalidate();
        list.repaint();
    }

    private class Row extends Ui.Card {
        Row(Patient p) {
            super(new BorderLayout(14, 0));
            radius = 8;
            setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            setAlignmentX(LEFT_ALIGNMENT);
            add(new Ui.Avatar(p.getName(), 42), BorderLayout.WEST);
            JPanel t = new JPanel();
            t.setOpaque(false);
            t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
            t.add(Box.createVerticalGlue());
            t.add(new Ui.Label(p.getName(), Ui.TEXT, Font.BOLD, 15));
            t.add(new Ui.Label("ID " + p.getId() + "  \u00B7  Room " + p.getRoom() + "  \u00B7  Age " + p.getAge(),
                    Ui.DIM, Font.PLAIN, 12));
            t.add(Box.createVerticalGlue());
            add(t, BorderLayout.CENTER);
            JPanel bw = new JPanel(new GridBagLayout());
            bw.setOpaque(false);
            Ui.Button rm = new Ui.Button("Remove", Icons.TRASH, Ui.Kind.DANGER_GHOST);
            rm.addActionListener(e -> {
                String extra = ctl.isRunning() ? "\nIts live sensor will stop immediately." : "";
                if (Ui.confirm(PatientsPage.this, "Remove patient?",
                        "Remove " + p.getName() + " (Room " + p.getRoom() + ")?" + extra, "Remove", true, true)) {
                    ctl.removePatient(p.getId());
                    changed();
                }
            });
            bw.add(rm);
            add(bw, BorderLayout.EAST);
        }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }

    private static Ui.Label caption(String s) { return new Ui.Label(s, Ui.DIM, Font.BOLD, 11); }

    private static JPanel stack(JComponent... cs) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (int i = 0; i < cs.length; i++) {
            if (i > 0) p.add(Box.createVerticalStrut(5));
            cs[i].setAlignmentX(LEFT_ALIGNMENT);
            p.add(cs[i]);
        }
        return p;
    }

    private static <T extends JComponent> T left(T c) { c.setAlignmentX(LEFT_ALIGNMENT); return c; }
}
