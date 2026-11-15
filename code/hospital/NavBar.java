package hospital;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.IntConsumer;

/** Top bar: logo on the left, page tabs in the middle, clock + controls on the right. */
public class NavBar extends JPanel {
    private final String[] labels;
    private final String[] tips;
    private final int[] badge;
    private int selected = 0, hover = -1;
    private boolean compact;
    private final IntConsumer onNavigate;
    private final Tabs tabs = new Tabs();
    private final Logo logo = new Logo();
    private final Clock clock = new Clock();

    public NavBar(String[] labels, String[] tips, IntConsumer onNavigate, JComponent actions) {
        this.labels = labels;
        this.tips = tips;
        this.badge = new int[labels.length];
        this.onNavigate = onNavigate;
        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 20));
        add(logo, BorderLayout.WEST);
        add(tabs, BorderLayout.CENTER);

        JPanel east = new JPanel(new GridBagLayout());
        east.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.weighty = 1; gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(0, 0, 0, 16);
        east.add(clock, gc);
        gc.insets = new Insets(0, 0, 0, 0);
        east.add(actions, gc);
        add(east, BorderLayout.EAST);

        new javax.swing.Timer(1000, e -> clock.repaint()).start();
    }

    public void select(int i) { selected = i; repaint(); }
    public void setBadge(int i, int n) { badge[i] = n; repaint(); }

    public void setCompact(boolean c) {
        if (c == compact) return;
        compact = c;
        clock.setVisible(!c);
        logo.revalidate();
        revalidate();
        repaint();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(10, 66); }

    @Override protected void paintComponent(Graphics g) {
        g.setColor(Theme.header);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.alpha(Theme.border, 160));
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
    }

    // ------------------------------------------------------------------ logo
    private class Logo extends JComponent {
        @Override public Dimension getPreferredSize() { return new Dimension(compact ? 44 : 196, 66); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.g2(g);
            g2.setColor(Theme.accent);
            g2.fill(new RoundRectangle2D.Float(0, 13, 40, 40, 12, 12));
            Icons.draw(g2, Icons.PULSE, 8, 21, 24, Color.WHITE);
            if (!compact) {
                g2.setColor(Theme.text);
                g2.setFont(Theme.font(Font.BOLD, 18));
                g2.drawString("VitalWatch", 52, 31);
                g2.setColor(Theme.dim);
                g2.setFont(Theme.font(Font.PLAIN, 11));
                g2.drawString("Hospital monitor", 52, 47);
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ tabs
    private class Tabs extends JComponent {
        Tabs() {
            addMouseListener(new MouseAdapter() {
                @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
                @Override public void mouseReleased(MouseEvent e) {
                    int i = indexAt(e.getX());
                    if (i >= 0) onNavigate.accept(i);
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int i = indexAt(e.getX());
                    if (i != hover) { hover = i; repaint(); }
                    setCursor(i >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    setToolTipText(i >= 0 ? tips[i] : null);
                }
            });
        }

        private int tabW() { return Math.max(70, Math.min(150, getWidth() / labels.length)); }
        private int startX() { return (getWidth() - tabW() * labels.length) / 2; }
        private int indexAt(int x) {
            int i = (x - startX()) / tabW();
            return x >= startX() && i >= 0 && i < labels.length ? i : -1;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.g2(g);
            int tw = tabW(), x0 = startX(), h = getHeight();
            for (int i = 0; i < labels.length; i++) {
                boolean sel = i == selected;
                g2.setFont(Theme.font(sel ? Font.BOLD : Font.PLAIN, 15));
                FontMetrics fm = g2.getFontMetrics();
                String s = labels[i];
                int textW = fm.stringWidth(s);
                int cx = x0 + i * tw + tw / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(sel ? Theme.text : i == hover ? Theme.text : Theme.alpha(Theme.text, 175));
                g2.drawString(s, cx - textW / 2, ty);
                if (sel) {
                    g2.setColor(Theme.cyan);
                    g2.fill(new RoundRectangle2D.Float(cx - textW / 2f, h - 9, textW, 3, 3, 3));
                }
                if (badge[i] > 0) {
                    String b = badge[i] > 99 ? "99+" : String.valueOf(badge[i]);
                    g2.setFont(Theme.font(Font.BOLD, 10));
                    FontMetrics bf = g2.getFontMetrics();
                    int bw = Math.max(18, bf.stringWidth(b) + 10);
                    float bx = cx + textW / 2f + 6, by = ty - 14;
                    g2.setColor(Theme.crit);
                    g2.fill(new RoundRectangle2D.Float(bx, by, bw, 18, 18, 18));
                    g2.setColor(Color.WHITE);
                    g2.drawString(b, bx + (bw - bf.stringWidth(b)) / 2f, by + 13);
                }
            }
            g2.dispose();
        }
    }

    // ----------------------------------------------------------------- clock
    private static class Clock extends JComponent {
        Clock() { setPreferredSize(new Dimension(96, 66)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.g2(g);
            Date now = new Date();
            String d = new SimpleDateFormat("dd/MMM/yyyy", Locale.ENGLISH).format(now).toUpperCase(Locale.ENGLISH);
            String t = new SimpleDateFormat("HH:mm").format(now);
            g2.setFont(Theme.font(Font.PLAIN, 12));
            g2.setColor(Theme.text);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(d, getWidth() - fm.stringWidth(d), 29);
            g2.setColor(Theme.dim);
            g2.drawString(t, getWidth() - fm.stringWidth(t), 45);
            g2.dispose();
        }
    }
}
