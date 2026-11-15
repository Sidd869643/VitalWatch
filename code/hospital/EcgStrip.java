package hospital;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;

/**
 * Darker band across the middle of a patient card. Shows a live ECG-style trace paced by the
 * patient's heart rate; the chevron on the right flips it to the heart-rate trend graph.
 * When there is nothing to draw it shows a short status message instead.
 */
public class EcgStrip extends JComponent {
    private static final double SPEED = 56;   // px per second
    private static final int HISTORY = 45;

    private final ArrayDeque<Integer> history = new ArrayDeque<>();
    private int hr = 72;
    private boolean live;          // true = readings are streaming in right now
    private boolean hasData;
    private String message = "Not monitoring";
    private java.util.function.Supplier<Color> messageColor; // null = dim
    private boolean trendMode;
    private double phase;          // beat phase (0..1) at the right edge
    private long last = System.nanoTime();

    public EcgStrip() {
        setPreferredSize(new Dimension(10, 40));
        setMinimumSize(new Dimension(10, 40));
        setToolTipText("Show heart-rate trend");
        addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                if (hasData && e.getX() > getWidth() - 40) {
                    trendMode = !trendMode;
                    setToolTipText(trendMode ? "Show ECG trace" : "Show heart-rate trend");
                    repaint();
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                setCursor(hasData && e.getX() > getWidth() - 40
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
    }

    public void push(int heartRate) {
        hr = Math.max(30, Math.min(200, heartRate));
        if (history.size() >= HISTORY) history.removeFirst();
        history.addLast(heartRate);
        hasData = true;
        live = true;
        repaint();
    }

    public void showMessage(String text, java.util.function.Supplier<Color> color, boolean keepData) {
        message = text;
        messageColor = color;
        live = false;
        hasData = keepData && hasData;
        if (!keepData) { history.clear(); trendMode = false; }
        repaint();
    }

    /** Advance the animation. Returns true when something visible changed. */
    public boolean tick() {
        long now = System.nanoTime();
        double dt = Math.min(0.2, (now - last) / 1e9);
        last = now;
        if (!live || trendMode) return false;
        phase = (phase + dt * hr / 60.0) % 1.0;
        repaint();
        return true;
    }

    private static double bump(double x, double mean, double sd) {
        double d = (x - mean) / sd;
        return Math.exp(-0.5 * d * d);
    }

    /** One synthetic PQRST complex; ph runs 0..1 across a beat. */
    private static double beat(double ph) {
        return 0.14 * bump(ph, 0.16, 0.035)    // P
             - 0.13 * bump(ph, 0.282, 0.014)   // Q
             + 1.00 * bump(ph, 0.310, 0.018)   // R
             - 0.26 * bump(ph, 0.342, 0.016)   // S
             + 0.26 * bump(ph, 0.580, 0.055);  // T
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.g2(g);
        int w = getWidth(), h = getHeight();
        g2.setColor(Theme.dark ? Theme.alpha(Color.BLACK, 56) : Theme.alpha(Theme.accent, 22));
        g2.fillRect(0, 0, w, h);

        if (!live && !hasData) {
            drawMessage(g2, w, h);
        } else if (trendMode) {
            drawTrend(g2, w - 44, h);
        } else if (live) {
            drawEcg(g2, w - 44, h);
        } else {
            // monitoring stopped: flat line + note
            g2.setColor(Theme.alpha(Theme.dim, 120));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawLine(12, h / 2, w - 52, h / 2);
            drawMessage(g2, w - 44, h);
        }

        if (hasData) {
            Icons.draw(g2, trendMode ? Icons.CHEV_DOWN : Icons.CHEV_UP, w - 32, (h - 18) / 2f, 18, Theme.alpha(Theme.text, 210));
        }
        g2.dispose();
    }

    private void drawMessage(Graphics2D g2, int w, int h) {
        g2.setFont(Theme.font(Font.PLAIN, 11));
        g2.setColor(messageColor != null ? messageColor.get() : Theme.dim);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(message, (w - fm.stringWidth(message)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
    }

    private void drawEcg(Graphics2D g2, int w, int h) {
        double period = 60.0 / hr;
        double base = h * 0.66, amp = h * 0.52;
        Path2D line = new Path2D.Double();
        boolean first = true;
        for (double x = 8; x < w; x += 0.5) {
            double secondsAgo = (w - x) / SPEED;
            double ph = phase - secondsAgo / period;
            ph -= Math.floor(ph);
            double y = base - beat(ph) * amp;
            if (first) { line.moveTo(x, y); first = false; } else line.lineTo(x, y);
        }
        g2.setColor(Theme.alpha(Theme.vHr, 50));
        g2.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);
        g2.setColor(Theme.vHr);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);
    }

    private void drawTrend(Graphics2D g2, int w, int h) {
        g2.setFont(Theme.font(Font.PLAIN, 10));
        g2.setColor(Theme.dim);
        g2.drawString("HR trend", 10, 12);
        if (history.size() < 2) {
            g2.drawString("Waiting for data...", w / 2 - 36, h / 2 + 4);
            return;
        }
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int v : history) { min = Math.min(min, v); max = Math.max(max, v); }
        double mid = (min + max) / 2.0, range = Math.max(max - min, 20) * 1.25;
        double lo = mid - range / 2;
        int padX = 10, top = 16, bottom = h - 6;
        double step = (w - 2.0 * padX) / (HISTORY - 1);
        int n = history.size(), i = 0;
        double[] xs = new double[n], ys = new double[n];
        for (int v : history) {
            xs[i] = w - padX - (n - 1 - i) * step;
            ys[i] = bottom - (v - lo) / range * (bottom - top);
            i++;
        }
        Path2D line = new Path2D.Double();
        line.moveTo(xs[0], ys[0]);
        for (int k = 1; k < n; k++) {
            double cx = (xs[k - 1] + xs[k]) / 2;
            line.curveTo(cx, ys[k - 1], cx, ys[k], xs[k], ys[k]);
        }
        Path2D fill = new Path2D.Double(line);
        fill.lineTo(xs[n - 1], h);
        fill.lineTo(xs[0], h);
        fill.closePath();
        g2.setPaint(new GradientPaint(0, top, Theme.alpha(Theme.cyan, 90), 0, h, Theme.alpha(Theme.cyan, 0)));
        g2.fill(fill);
        g2.setPaint(null);
        g2.setColor(Theme.cyan);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);
        g2.fill(new java.awt.geom.Ellipse2D.Double(xs[n - 1] - 3, ys[n - 1] - 3, 6, 6));
    }
}
