package hospital;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Central colour palette + fonts. Every custom component reads from here at paint time, so toggling is instant. */
public final class Theme {
    private Theme() { }

    public static boolean dark = true;
    public static Color bg, bg2, header, sidebar, surface, surface2, border, text, dim, accent, ok, warn, crit, fieldBg;
    /** Cyan used for secondary labels, plus the signature colour of each vital sign. */
    public static Color cyan, vHr, vSpo2, vBp, vTemp;
    public static float pulse = 1f; // animated 0..1, driven by a timer in MainFrame
    public static final String FAMILY = pickFamily();

    static { apply(true); }

    public static void apply(boolean d) {
        dark = d;
        if (d) {
            // deep-blue monitoring-wall look: blue gradient, navy cards
            bg = new Color(0x0B4DAA);      bg2 = new Color(0x083A86);
            header = new Color(0x0A3A8E);  sidebar = header;
            surface = new Color(0x052B5E); surface2 = new Color(0x0C3F86);
            border = new Color(0x1D58B0);  fieldBg = new Color(0x2459B0);
            text = new Color(0xFFFFFF);    dim = new Color(0x9DC0EE);
            accent = new Color(0x1B8FE3);  cyan = new Color(0x22D3EE);
            ok = new Color(0x3DDC84);      warn = new Color(0xFFE600);
            crit = new Color(0xFF4D4F);
            vHr = new Color(0x3DDC84);     vSpo2 = new Color(0x22D3EE);
            vBp = new Color(0xFFE600);     vTemp = new Color(0xFFFFFF);
        } else {
            bg = new Color(0xE9F1FC);      bg2 = new Color(0xD5E4F8);
            header = new Color(0xFFFFFF);  sidebar = header;
            surface = new Color(0xFFFFFF); surface2 = new Color(0xEAF1FB);
            border = new Color(0xC6D6EE);  fieldBg = new Color(0xDDE8F8);
            text = new Color(0x0B2A5B);    dim = new Color(0x5A7199);
            accent = new Color(0x1769D6);  cyan = new Color(0x0A8FB8);
            ok = new Color(0x0E9F5B);      warn = new Color(0xC79200);
            crit = new Color(0xD92D3A);
            vHr = new Color(0x0E9F5B);     vSpo2 = new Color(0x0A8FB8);
            vBp = new Color(0xB98900);     vTemp = new Color(0x0B2A5B);
        }
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    public static Color mix(Color a, Color b, float t) {
        return new Color(
            Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
            Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    public static Color level(AlertLevel l) {
        if (l == AlertLevel.CRITICAL) return crit;
        if (l == AlertLevel.WARNING) return warn;
        return ok;
    }

    public static Font font(int style, float size) {
        return new Font(FAMILY, style, 12).deriveFont(style, size);
    }

    private static String pickFamily() {
        Set<String> names = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Roboto", "Ubuntu", "Noto Sans"}) {
            if (names.contains(f)) return f;
        }
        return Font.SANS_SERIF;
    }
}
