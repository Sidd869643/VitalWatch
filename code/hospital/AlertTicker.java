package hospital;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Bottom strip of the newest alerts as "Patient | reading" chips, with arrows to page through them. */
public class AlertTicker extends JComponent {
    private static final int KEEP = 40;

    private final List<AlertEvent> events = new ArrayList<>(); // newest first
    private int offset = 0;
    private final Runnable onOpen;

    private final Rectangle leftBtn = new Rectangle(), rightBtn = new Rectangle();
    private final List<Rectangle> chipRects = new ArrayList<>();

    public AlertTicker(Runnable onOpen) {
        this.onOpen = onOpen;
        setPreferredSize(new Dimension(10, 60));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                if (leftBtn.contains(p)) { offset = Math.max(0, offset - 1); repaint(); return; }
                if (rightBtn.contains(p)) { offset = Math.min(Math.max(0, events.size() - 1), offset + 1); repaint(); return; }
                for (Rectangle r : chipRects) if (r.contains(p)) { onOpen.run(); return; }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                boolean hand = leftBtn.contains(p) || rightBtn.contains(p);
                for (Rectangle r : chipRects) hand |= r.contains(p);
                setCursor(hand ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    public void add(AlertEvent ev) {
        events.add(0, ev);
        if (events.size() > KEEP) events.remove(events.size() - 1);
        offset = 0; // always show the newest first
        repaint();
    }

    public void clear() { events.clear(); offset = 0; repaint(); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.g2(g);
        int w = getWidth(), h = getHeight();
        g2.setColor(Theme.header);
        g2.fillRect(0, 0, w, h);
        g2.setColor(Theme.alpha(Theme.border, 160));
        g2.fillRect(0, 0, w, 1);

        int arrow = 30, pad = 18;
        leftBtn.setBounds(pad, (h - arrow) / 2, arrow, arrow);
        rightBtn.setBounds(w - pad - arrow, (h - arrow) / 2, arrow, arrow);
        boolean canL = offset > 0, canR = offset < events.size() - 1;
        Icons.draw(g2, Icons.CHEV_LEFT, leftBtn.x, leftBtn.y, arrow, canL ? Theme.text : Theme.alpha(Theme.text, 80));
        Icons.draw(g2, Icons.CHEV_RIGHT, rightBtn.x, rightBtn.y, arrow, canR ? Theme.text : Theme.alpha(Theme.text, 80));

        chipRects.clear();
        int left = leftBtn.x + arrow + 14, right = rightBtn.x - 14;
        if (events.isEmpty()) {
            g2.setFont(Theme.font(Font.PLAIN, 13));
            g2.setColor(Theme.dim);
            FontMetrics fm = g2.getFontMetrics();
            String s = "No alerts yet. New warnings and critical events will appear here.";
            g2.drawString(s, left, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
            return;
        }

        g2.setClip(left, 0, Math.max(0, right - left), h);
        int x = left, ch = 32, y = (h - ch) / 2;
        for (int i = offset; i < events.size(); i++) {
            AlertEvent ev = events.get(i);
            Color lc = Theme.level(ev.level);
            String name = ev.patient.getName();
            String[] parts = ev.message.split("; ");
            String value = parts[0] + (parts.length > 1 ? "  +" + (parts.length - 1) : "");

            g2.setFont(Theme.font(Font.PLAIN, 13));
            int nameW = g2.getFontMetrics().stringWidth(name) + 28;
            g2.setFont(Theme.font(Font.BOLD, 13));
            int valW = g2.getFontMetrics().stringWidth(value) + 28 + 16;
            int cw = nameW + valW;

            RoundRectangle2D.Float full = new RoundRectangle2D.Float(x, y, cw, ch, 8, 8);
            g2.setColor(Theme.dark ? Theme.alpha(Color.BLACK, 70) : Theme.surface2);
            g2.fill(full);
            Shape oldClip = g2.getClip();
            g2.clip(new Rectangle(x + nameW, y, valW, ch));
            g2.setColor(Theme.alpha(lc, Theme.dark ? 60 : 45));
            g2.fill(full);
            g2.setClip(oldClip);
            g2.setColor(Theme.alpha(lc, 150));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(x + .5f, y + .5f, cw - 1, ch - 1, 8, 8));

            FontMetrics fm;
            g2.setFont(Theme.font(Font.PLAIN, 13));
            fm = g2.getFontMetrics();
            int ty = y + (ch - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(Theme.text);
            g2.drawString(name, x + 14, ty);

            Icons.draw(g2, Icons.WARN, x + nameW + 12, y + (ch - 14) / 2f, 14, lc);
            g2.setFont(Theme.font(Font.BOLD, 13));
            g2.setColor(ev.level == AlertLevel.WARNING && !Theme.dark ? Theme.warn : lc);
            g2.drawString(value, x + nameW + 32, ty);

            chipRects.add(new Rectangle(x, y, cw, ch));
            x += cw + 16;
            if (x > right) break;
        }
        g2.dispose();
    }
}
