package hospital;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Small custom-painted widget toolkit that gives Swing a modern look. */
public final class Ui {
    private Ui() { }

    public static final int TEXT = 0, DIM = 1;

    public static Graphics2D g2(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    // ------------------------------------------------------------------ Card
    public static class Card extends JPanel {
        protected int radius = 8;
        public boolean alt;

        public Card(LayoutManager lm) { super(lm); setOpaque(false); }
        public Card() { this(new BorderLayout()); }

        protected Color fillColor() { return alt ? Theme.surface2 : Theme.surface; }
        protected Color borderColor() { return Theme.border; }
        protected Stroke borderStroke() { return new BasicStroke(1f); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            int a = radius * 2;
            g2.setColor(fillColor());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), a, a));
            Stroke st = borderStroke();
            float sw = st instanceof BasicStroke ? ((BasicStroke) st).getLineWidth() : 1f;
            g2.setColor(borderColor());
            g2.setStroke(st);
            g2.draw(new RoundRectangle2D.Float(sw / 2f, sw / 2f, getWidth() - sw, getHeight() - sw, a, a));
            g2.dispose();
        }
    }

    // ----------------------------------------------------------------- Label
    public static class Label extends JLabel {
        private final int role;
        private Color fixed;

        public Label(String text, int role, int style, float size) {
            super(text);
            this.role = role;
            setFont(Theme.font(style, size));
        }
        public void setColor(Color c) { fixed = c; repaint(); }
        @Override public Color getForeground() {
            if (fixed != null) return fixed;
            return role == DIM ? Theme.dim : Theme.text;
        }
    }

    // ---------------------------------------------------------------- Button
    public enum Kind { PRIMARY, DANGER, GHOST, DANGER_GHOST }

    public static class Button extends JComponent {
        private String text;
        private int icon;
        private Kind kind;
        private boolean hover, press, selected;
        private final List<ActionListener> listeners = new ArrayList<>();

        public Button(String text, int icon, Kind kind) {
            this.text = text; this.icon = icon; this.kind = kind;
            setFont(Theme.font(Font.BOLD, 13));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; press = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { if (isEnabled()) { press = true; requestFocusInWindow(); repaint(); } }
                @Override public void mouseReleased(MouseEvent e) {
                    boolean was = press; press = false; repaint();
                    if (was && hover && isEnabled()) fire();
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (isEnabled() && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)) fire();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        public void addActionListener(ActionListener l) { listeners.add(l); }
        private void fire() {
            ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, text);
            for (ActionListener l : new ArrayList<>(listeners)) l.actionPerformed(ev);
        }
        public void setText(String t) { text = t; revalidate(); repaint(); }
        public void setIcon(int i) { icon = i; repaint(); }
        public void setKind(Kind k) { kind = k; repaint(); }
        public void setSelected(boolean s) { selected = s; repaint(); }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int w = 32 + fm.stringWidth(text) + (icon >= 0 ? 24 : 0);
            if (text.isEmpty()) w = 40;
            return new Dimension(w, 38);
        }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            int w = getWidth(), h = getHeight();
            boolean en = isEnabled();
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 12, 12);
            Color fg;
            if (kind == Kind.PRIMARY || kind == Kind.DANGER) {
                Color base = kind == Kind.PRIMARY ? Theme.accent : Theme.crit;
                if (press) base = Theme.mix(base, Color.BLACK, .18f);
                else if (hover) base = Theme.mix(base, Color.WHITE, .14f);
                if (!en) base = Theme.alpha(base, 90);
                g2.setColor(base);
                g2.fill(shape);
                fg = en ? Color.WHITE : new Color(255, 255, 255, 150);
            } else {
                Color tone = kind == Kind.GHOST ? Theme.text : Theme.crit;
                Color line = kind == Kind.GHOST ? Theme.border : Theme.alpha(Theme.crit, 110);
                if (selected) { tone = Theme.cyan; line = Theme.cyan; }
                int fillA = press ? 50 : hover ? 28 : selected ? 36 : 0;
                if (fillA > 0) { g2.setColor(Theme.alpha(tone, fillA)); g2.fill(shape); }
                g2.setColor(en ? line : Theme.alpha(line, 80));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(shape);
                fg = en ? tone : Theme.alpha(tone, 110);
            }
            if (isFocusOwner()) {
                g2.setColor(Theme.alpha(Theme.accent, 120));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(2, 2, w - 4f, h - 4f, 9, 9));
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int iw = icon >= 0 ? 16 : 0, gap = icon >= 0 && !text.isEmpty() ? 8 : 0;
            int total = iw + gap + fm.stringWidth(text);
            int x = (w - total) / 2;
            if (icon >= 0) { Icons.draw(g2, icon, x, (h - 16) / 2f, 16, fg); x += iw + gap; }
            g2.setColor(fg);
            g2.drawString(text, x, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ----------------------------------------------------------------- Field
    public static class Field extends JTextField {
        private final String placeholder;

        public Field(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
            setFont(Theme.font(Font.PLAIN, 14));
            setSelectionColor(Theme.alpha(Theme.accent, 120));
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }
        @Override public Color getForeground() { return Theme.text; }
        @Override public Color getCaretColor() { return Theme.text; }
        @Override public Color getSelectedTextColor() { return Theme.text; }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            g2.setColor(Theme.fieldBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
            g2.setColor(isFocusOwner() ? Theme.cyan : Theme.border);
            g2.setStroke(new BasicStroke(isFocusOwner() ? 1.6f : 1f));
            g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, getWidth() - 1.6f, getHeight() - 1.6f, 12, 12));
            g2.dispose();
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D p = g2(g);
                p.setFont(getFont());
                p.setColor(Theme.alpha(Theme.dim, 190));
                FontMetrics fm = p.getFontMetrics();
                p.drawString(placeholder, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                p.dispose();
            }
        }
    }

    // ----------------------------------------------------------- Text area
    public static class TextArea extends JTextArea {
        public TextArea() {
            setOpaque(false);
            setEditable(false);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            setSelectionColor(Theme.alpha(Theme.accent, 120));
        }
        @Override public Color getForeground() { return Theme.text; }
        @Override public Color getSelectedTextColor() { return Theme.text; }
    }

    // ----------------------------------------------------------------- Badge
    public static class Badge extends JComponent {
        private String text = "";
        private Color color = Theme.dim;

        public Badge() { setFont(Theme.font(Font.BOLD, 11)); }
        public void set(String t, Color c) { text = t.toUpperCase(); color = c; revalidate(); repaint(); }
        @Override public Dimension getPreferredSize() {
            return new Dimension(getFontMetrics(getFont()).stringWidth(text) + 26, 24);
        }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            g2.setColor(Theme.alpha(color, 38));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.setColor(color);
            g2.fill(new Ellipse2D.Float(9, getHeight() / 2f - 3, 6, 6));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 20, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------- Select
    /** Dark drop-down used in the filter bar: small caption above, boxed value below. */
    public static class Select extends JComponent {
        private final String caption;
        private final List<String> items = new ArrayList<>();
        private int index;
        private boolean hover;
        private java.util.function.IntConsumer onChange = i -> { };

        public Select(String caption, int width) {
            this.caption = caption;
            setPreferredSize(new Dimension(width, caption == null ? 32 : 52));
            setMinimumSize(new Dimension(Math.min(width, 120), getPreferredSize().height));
            setMaximumSize(getPreferredSize());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); popup(); }
            });
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    int k = e.getKeyCode();
                    if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE || k == KeyEvent.VK_DOWN) popup();
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        public void onChange(java.util.function.IntConsumer c) { onChange = c; }
        public int getSelectedIndex() { return index; }
        public String getSelectedItem() { return items.isEmpty() ? "" : items.get(index); }

        /** Replace the options, keeping the current choice when it still exists. */
        public void setItems(List<String> list) {
            String keep = items.isEmpty() ? null : items.get(index);
            items.clear();
            items.addAll(list);
            index = keep == null ? 0 : Math.max(0, items.indexOf(keep));
            repaint();
        }

        public void setSelectedIndex(int i) {
            if (i < 0 || i >= items.size() || i == index) return;
            index = i;
            repaint();
            onChange.accept(i);
        }

        private int boxTop() { return caption == null ? 0 : 20; }

        private void popup() {
            if (items.isEmpty()) return;
            JPopupMenu pm = new JPopupMenu();
            pm.setBorder(BorderFactory.createLineBorder(Theme.border));
            pm.setBackground(Theme.surface);
            int w = getWidth();
            for (int i = 0; i < items.size(); i++) {
                final int idx = i;
                JComponent row = new JComponent() {
                    boolean over;
                    { addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                        @Override public void mouseExited(MouseEvent e) { over = false; repaint(); }
                        @Override public void mouseReleased(MouseEvent e) { pm.setVisible(false); setSelectedIndex(idx); }
                    }); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
                    @Override public Dimension getPreferredSize() { return new Dimension(Math.max(w, 150), 30); }
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = g2(g);
                        g2.setColor(over ? Theme.alpha(Theme.cyan, 60) : Theme.surface);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        boolean sel = idx == index;
                        g2.setFont(Theme.font(sel ? Font.BOLD : Font.PLAIN, 13));
                        g2.setColor(sel ? Theme.cyan : Theme.text);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(items.get(idx), 12, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                        g2.dispose();
                    }
                };
                pm.add(row);
            }
            pm.show(this, 0, getHeight() + 2);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            int w = getWidth(), top = boxTop(), bh = 32;
            if (caption != null) {
                g2.setFont(Theme.font(Font.PLAIN, 12));
                g2.setColor(Theme.text);
                g2.drawString(caption, 4, 13);
            }
            Color fill = hover ? Theme.mix(Theme.fieldBg, Color.WHITE, .12f) : Theme.fieldBg;
            RoundRectangle2D.Float box = new RoundRectangle2D.Float(0, top, w, bh, 10, 10);
            g2.setColor(fill);
            g2.fill(box);
            if (isFocusOwner()) {
                g2.setColor(Theme.cyan);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.75f, top + 0.75f, w - 1.5f, bh - 1.5f, 10, 10));
            }
            g2.setFont(Theme.font(Font.BOLD, 13));
            g2.setColor(Theme.text);
            FontMetrics fm = g2.getFontMetrics();
            String t = getSelectedItem();
            int maxW = w - 44;
            while (t.length() > 1 && fm.stringWidth(t) > maxW) t = t.substring(0, t.length() - 2) + "\u2026";
            g2.drawString(t, 14, top + (bh - fm.getHeight()) / 2 + fm.getAscent());
            Icons.draw(g2, Icons.CHEV_DOWN, w - 28, top + (bh - 18) / 2f, 18, Theme.text);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------- IconButton
    /** Square icon-only button (view toggles, theme switch). */
    public static class IconButton extends JComponent {
        private int icon;
        private boolean hover, press, selected;
        private final List<ActionListener> listeners = new ArrayList<>();

        public IconButton(int icon, String tip) {
            this.icon = icon;
            setToolTipText(tip);
            setPreferredSize(new Dimension(38, 38));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; press = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { press = true; repaint(); }
                @Override public void mouseReleased(MouseEvent e) {
                    boolean was = press; press = false; repaint();
                    if (was && hover) {
                        ActionEvent ev = new ActionEvent(IconButton.this, ActionEvent.ACTION_PERFORMED, "icon");
                        for (ActionListener l : new ArrayList<>(listeners)) l.actionPerformed(ev);
                    }
                }
            });
        }

        public void addActionListener(ActionListener l) { listeners.add(l); }
        public void setIcon(int i, String tip) { icon = i; setToolTipText(tip); repaint(); }
        public void setSelected(boolean s) { selected = s; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            Color fill = Theme.fieldBg;
            if (selected) fill = Theme.mix(Theme.fieldBg, Theme.text, .22f);
            if (press) fill = Theme.mix(fill, Color.BLACK, .15f);
            else if (hover) fill = Theme.mix(fill, Color.WHITE, .12f);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
            Icons.draw(g2, icon, (getWidth() - 22) / 2f, (getHeight() - 22) / 2f, 22,
                    selected ? Theme.text : Theme.alpha(Theme.text, 190));
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------ Pager
    /** "Prev 1 2 3 4 Next" page control. */
    public static class Pager extends JComponent {
        private int page, pages = 1;
        private java.util.function.IntConsumer onPage = p -> { };
        private final List<Rectangle> hit = new ArrayList<>();
        private final List<Integer> hitPage = new ArrayList<>();

        public Pager() {
            setPreferredSize(new Dimension(300, 30));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseReleased(MouseEvent e) {
                    for (int i = 0; i < hit.size(); i++) {
                        if (hit.get(i).contains(e.getPoint())) {
                            int target = hitPage.get(i);
                            if (target >= 0 && target < pages && target != page) onPage.accept(target);
                            return;
                        }
                    }
                }
            });
        }

        public void onPage(java.util.function.IntConsumer c) { onPage = c; }
        public void set(int page, int pages) { this.page = page; this.pages = Math.max(1, pages); repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            hit.clear(); hitPage.clear();
            int h = getHeight();
            Font f = Theme.font(Font.PLAIN, 13);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();

            // measure everything first so the control hugs the right edge
            int start = Math.max(0, Math.min(page - 2, pages - 5)), end = Math.min(pages - 1, start + 4);
            if (start < 0) start = 0;
            int prevW = fm.stringWidth("Prev") + 28, nextW = fm.stringWidth("Next") + 28;
            int numW = 30, gap = 4;
            int total = prevW + (end - start + 1) * (numW + gap) + nextW;
            int x = Math.max(0, getWidth() - total);

            boolean canPrev = page > 0, canNext = page < pages - 1;
            g2.setColor(canPrev ? Theme.text : Theme.alpha(Theme.text, 90));
            Icons.draw(g2, Icons.CHEV_LEFT, x, (h - 16) / 2f, 16, g2.getColor());
            g2.drawString("Prev", x + 22, ty);
            hit.add(new Rectangle(x, 0, prevW, h)); hitPage.add(page - 1);
            x += prevW;

            for (int p = start; p <= end; p++) {
                Rectangle r = new Rectangle(x, 2, numW, h - 4);
                if (p == page) {
                    g2.setColor(Theme.cyan);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(new RoundRectangle2D.Float(r.x + .6f, r.y + .6f, r.width - 1.2f, r.height - 1.2f, 6, 6));
                    g2.setColor(Theme.text);
                } else {
                    g2.setColor(Theme.alpha(Theme.text, 200));
                }
                String s = String.valueOf(p + 1);
                g2.drawString(s, r.x + (r.width - fm.stringWidth(s)) / 2, ty);
                hit.add(r); hitPage.add(p);
                x += numW + gap;
            }

            g2.setColor(canNext ? Theme.text : Theme.alpha(Theme.text, 90));
            g2.drawString("Next", x + 6, ty);
            Icons.draw(g2, Icons.CHEV_RIGHT, x + 6 + fm.stringWidth("Next") + 4, (h - 16) / 2f, 16, g2.getColor());
            hit.add(new Rectangle(x, 0, nextW, h)); hitPage.add(page + 1);
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------- Avatar
    public static class Avatar extends JComponent {
        private static final Color[] PALETTE = {
            new Color(0x4F8CFF), new Color(0x8B5CF6), new Color(0x14B8A6),
            new Color(0xF59E0B), new Color(0xEC4899), new Color(0x22C55E)};
        private final String initials;
        private final Color color;
        private final int size;

        public Avatar(String name, int size) {
            this.size = size;
            String[] parts = name.trim().split("\\s+");
            String s = parts[0].isEmpty() ? "?" : parts[0].substring(0, 1);
            if (parts.length > 1 && !parts[1].isEmpty()) s += parts[1].substring(0, 1);
            initials = s.toUpperCase();
            color = PALETTE[Math.abs(name.hashCode()) % PALETTE.length];
        }
        @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            g2.setColor(Theme.alpha(color, 45));
            g2.fill(new Ellipse2D.Float(0, 0, size, size));
            g2.setColor(color);
            g2.setFont(Theme.font(Font.BOLD, size * 0.38f));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials, (size - fm.stringWidth(initials)) / 2f, (size - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    // -------------------------------------------------------------- IconView
    public static class IconView extends JComponent {
        private final int icon, size;
        private final Supplier<Color> color;
        private final boolean circle;

        public IconView(int icon, int size, boolean circle, Supplier<Color> color) {
            this.icon = icon; this.size = size; this.circle = circle; this.color = color;
        }
        @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = g2(g);
            Color c = color.get();
            if (circle) {
                g2.setColor(Theme.alpha(c, 40));
                g2.fill(new RoundRectangle2D.Float(0, 0, size, size, size * 0.6f, size * 0.6f));
            }
            float s = circle ? size * 0.5f : size;
            Icons.draw(g2, icon, (size - s) / 2f, (size - s) / 2f, s, c);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------ ScrollPanel
    public static class ScrollPanel extends JPanel implements Scrollable {
        public ScrollPanel(LayoutManager lm) { super(lm); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return (int) (r.height * 0.9); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static class ThinScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        private JButton zero() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d); b.setMinimumSize(d); b.setMaximumSize(d);
            return b;
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = g2(g);
            g2.setColor(Theme.alpha(Theme.dim, isThumbRollover() ? 170 : 110));
            g2.fill(new RoundRectangle2D.Float(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8));
            g2.dispose();
        }
    }

    public static JScrollPane scroll(Component view) { return scroll(view, () -> null); }

    public static JScrollPane scroll(Component view, Supplier<Color> bg) {
        JViewport vp = new JViewport() {
            @Override protected void paintComponent(Graphics g) {
                Color c = bg.get();
                if (c == null) return;
                g.setColor(c);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        vp.setOpaque(false);
        vp.setView(view);
        vp.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        JScrollPane sp = new JScrollPane();
        sp.setViewport(vp);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar sb = sp.getVerticalScrollBar();
        sb.setUI(new ThinScrollBarUI());
        sb.setPreferredSize(new Dimension(10, 0));
        sb.setUnitIncrement(20);
        sb.setOpaque(false);
        return sp;
    }

    /** A panel whose column count follows its width (responsive card grid). */
    public static JPanel responsiveGrid(int minCol, int gap) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        GridLayout gl = new GridLayout(0, 1, gap, gap);
        p.setLayout(gl);
        p.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int cols = Math.max(1, (p.getWidth() + gap) / (minCol + gap));
                if (cols != gl.getColumns()) { gl.setColumns(cols); p.revalidate(); p.repaint(); }
            }
        });
        return p;
    }

    // ---------------------------------------------------------------- Dialog
    public static boolean confirm(Component parent, String title, String message,
                                  String okText, boolean danger, boolean showCancel) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        d.setUndecorated(true);
        boolean[] result = {false};

        Card box = new Card(new BorderLayout(0, 14));
        box.radius = 6;
        box.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));
        box.add(new Label(title, TEXT, Font.BOLD, 17), BorderLayout.NORTH);
        String html = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        box.add(new Label("<html><body style='width:300px'>" + html + "</body></html>", DIM, Font.PLAIN, 14), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        if (showCancel) {
            Button cancel = new Button("Cancel", -1, Kind.GHOST);
            cancel.addActionListener(e -> d.dispose());
            buttons.add(cancel);
        }
        Button ok = new Button(okText, -1, danger ? Kind.DANGER : Kind.PRIMARY);
        ok.addActionListener(e -> { result[0] = true; d.dispose(); });
        buttons.add(ok);
        box.add(buttons, BorderLayout.SOUTH);

        d.setContentPane(box);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.pack();
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
        return result[0];
    }
}
