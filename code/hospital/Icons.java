package hospital;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Vector icons drawn with Java2D, so no image files are needed. */
public final class Icons {
    private Icons() { }

    public static final int DASH = 0, USERS = 1, BELL = 2, REPORT = 3, HEART = 4, TRASH = 5, PLUS = 6,
            SUN = 7, MOON = 8, PLAY = 9, STOP = 10, DOWNLOAD = 11, PULSE = 12,
            THERMO = 13, DROP = 14, GAUGE = 15, WARN = 16, SENSOR = 17, LIST = 18,
            CHEV_UP = 19, CHEV_DOWN = 20, CHEV_LEFT = 21, CHEV_RIGHT = 22;

    public static void draw(Graphics2D g0, int type, float x, float y, float s, Color c) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setColor(c);
        g.setStroke(new BasicStroke(Math.max(1.6f, s / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case DASH: {
                float q = s * 0.36f, gap = s * 0.12f, o = (s - 2 * q - gap) / 2f;
                for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
                    g.fill(new RoundRectangle2D.Float(x + o + i * (q + gap), y + o + j * (q + gap), q, q, 5, 5));
                }
                break;
            }
            case USERS:
                g.draw(new Ellipse2D.Float(x + s * .33f, y + s * .14f, s * .34f, s * .34f));
                g.draw(new Arc2D.Float(x + s * .16f, y + s * .56f, s * .68f, s * .64f, 0, 180, Arc2D.OPEN));
                break;
            case BELL: {
                g.draw(new Arc2D.Float(x + s * .22f, y + s * .1f, s * .56f, s * .56f, 0, 180, Arc2D.OPEN));
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * .22f, y + s * .38f);
                p.lineTo(x + s * .14f, y + s * .72f);
                p.lineTo(x + s * .86f, y + s * .72f);
                p.lineTo(x + s * .78f, y + s * .38f);
                g.draw(p);
                g.fill(new Ellipse2D.Float(x + s * .42f, y + s * .8f, s * .16f, s * .12f));
                break;
            }
            case REPORT:
                g.draw(new RoundRectangle2D.Float(x + s * .2f, y + s * .08f, s * .6f, s * .84f, s * .12f, s * .12f));
                g.draw(new Line2D.Float(x + s * .33f, y + s * .36f, x + s * .67f, y + s * .36f));
                g.draw(new Line2D.Float(x + s * .33f, y + s * .52f, x + s * .67f, y + s * .52f));
                g.draw(new Line2D.Float(x + s * .33f, y + s * .68f, x + s * .55f, y + s * .68f));
                break;
            case HEART: {
                Area a = new Area(new Ellipse2D.Float(x + s * .1f, y + s * .14f, s * .42f, s * .42f));
                a.add(new Area(new Ellipse2D.Float(x + s * .48f, y + s * .14f, s * .42f, s * .42f)));
                Path2D t = new Path2D.Float();
                t.moveTo(x + s * .13f, y + s * .46f);
                t.lineTo(x + s * .87f, y + s * .46f);
                t.lineTo(x + s * .5f, y + s * .9f);
                t.closePath();
                a.add(new Area(t));
                g.fill(a);
                break;
            }
            case PULSE: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * .06f, y + s * .55f);
                p.lineTo(x + s * .30f, y + s * .55f);
                p.lineTo(x + s * .40f, y + s * .25f);
                p.lineTo(x + s * .56f, y + s * .85f);
                p.lineTo(x + s * .66f, y + s * .55f);
                p.lineTo(x + s * .94f, y + s * .55f);
                g.draw(p);
                break;
            }
            case TRASH: {
                g.draw(new Line2D.Float(x + s * .15f, y + s * .26f, x + s * .85f, y + s * .26f));
                Path2D h = new Path2D.Float();
                h.moveTo(x + s * .4f, y + s * .26f); h.lineTo(x + s * .4f, y + s * .12f);
                h.lineTo(x + s * .6f, y + s * .12f); h.lineTo(x + s * .6f, y + s * .26f);
                g.draw(h);
                Path2D b = new Path2D.Float();
                b.moveTo(x + s * .22f, y + s * .32f); b.lineTo(x + s * .28f, y + s * .88f);
                b.lineTo(x + s * .72f, y + s * .88f); b.lineTo(x + s * .78f, y + s * .32f);
                g.draw(b);
                break;
            }
            case PLUS:
                g.draw(new Line2D.Float(x + s * .5f, y + s * .2f, x + s * .5f, y + s * .8f));
                g.draw(new Line2D.Float(x + s * .2f, y + s * .5f, x + s * .8f, y + s * .5f));
                break;
            case SUN: {
                g.draw(new Ellipse2D.Float(x + s * .32f, y + s * .32f, s * .36f, s * .36f));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    float cx = x + s / 2f, cy = y + s / 2f;
                    g.draw(new Line2D.Double(cx + Math.cos(a) * s * .32, cy + Math.sin(a) * s * .32,
                                             cx + Math.cos(a) * s * .44, cy + Math.sin(a) * s * .44));
                }
                break;
            }
            case MOON: {
                Area a = new Area(new Ellipse2D.Float(x + s * .16f, y + s * .14f, s * .66f, s * .66f));
                a.subtract(new Area(new Ellipse2D.Float(x + s * .4f, y + s * .04f, s * .6f, s * .6f)));
                g.fill(a);
                break;
            }
            case PLAY: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * .3f, y + s * .18f);
                p.lineTo(x + s * .3f, y + s * .82f);
                p.lineTo(x + s * .82f, y + s * .5f);
                p.closePath();
                g.fill(p);
                break;
            }
            case STOP:
                g.fill(new RoundRectangle2D.Float(x + s * .24f, y + s * .24f, s * .52f, s * .52f, 4, 4));
                break;
            case DOWNLOAD: {
                g.draw(new Line2D.Float(x + s * .5f, y + s * .12f, x + s * .5f, y + s * .62f));
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * .3f, y + s * .44f); p.lineTo(x + s * .5f, y + s * .66f); p.lineTo(x + s * .7f, y + s * .44f);
                g.draw(p);
                g.draw(new Line2D.Float(x + s * .18f, y + s * .82f, x + s * .82f, y + s * .82f));
                break;
            }
            case THERMO: {
                g.draw(new RoundRectangle2D.Float(x + s * .4f, y + s * .08f, s * .2f, s * .55f, s * .2f, s * .2f));
                g.fill(new Ellipse2D.Float(x + s * .3f, y + s * .56f, s * .4f, s * .4f));
                g.draw(new Line2D.Float(x + s * .5f, y + s * .3f, x + s * .5f, y + s * .66f));
                break;
            }
            case DROP: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * .5f, y + s * .08f);
                p.curveTo(x + s * .5f, y + s * .08f, x + s * .2f, y + s * .46f, x + s * .2f, y + s * .62f);
                p.curveTo(x + s * .2f, y + s * .8f, x + s * .34f, y + s * .92f, x + s * .5f, y + s * .92f);
                p.curveTo(x + s * .66f, y + s * .92f, x + s * .8f, y + s * .8f, x + s * .8f, y + s * .62f);
                p.curveTo(x + s * .8f, y + s * .46f, x + s * .5f, y + s * .08f, x + s * .5f, y + s * .08f);
                p.closePath();
                g.draw(p);
                break;
            }
            case GAUGE: {
                g.draw(new Arc2D.Float(x + s * .1f, y + s * .16f, s * .8f, s * .8f, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Float(x + s * .1f, y + s * .56f, x + s * .9f, y + s * .56f));
                g.draw(new Line2D.Float(x + s * .5f, y + s * .56f, x + s * .68f, y + s * .3f));
                g.fill(new Ellipse2D.Float(x + s * .44f, y + s * .5f, s * .12f, s * .12f));
                break;
            }
            case WARN: {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * .5f, y + s * .08f);
                p.lineTo(x + s * .94f, y + s * .88f);
                p.lineTo(x + s * .06f, y + s * .88f);
                p.closePath();
                g.fill(p);
                g.setColor(new Color(0, 0, 0, 170));
                g.setStroke(new BasicStroke(Math.max(1f, s / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(x + s * .5f, y + s * .36f, x + s * .5f, y + s * .62f));
                g.fill(new Ellipse2D.Float(x + s * .455f, y + s * .71f, s * .09f, s * .09f));
                break;
            }
            case SENSOR: {
                float cx = x + s / 2f, cy = y + s / 2f, r = s * .17f, o = s * .34f;
                g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                for (int i = -1; i <= 1; i += 2) for (int j = -1; j <= 1; j += 2) {
                    g.draw(new Line2D.Float(cx + i * r * .72f, cy + j * r * .72f, cx + i * o * .86f, cy + j * o * .86f));
                    g.draw(new Ellipse2D.Float(cx + i * o - s * .1f, cy + j * o - s * .1f, s * .2f, s * .2f));
                }
                break;
            }
            case LIST:
                g.fill(new RoundRectangle2D.Float(x + s * .14f, y + s * .18f, s * .72f, s * .27f, 4, 4));
                g.fill(new RoundRectangle2D.Float(x + s * .14f, y + s * .55f, s * .72f, s * .27f, 4, 4));
                break;
            case CHEV_UP: case CHEV_DOWN: case CHEV_LEFT: case CHEV_RIGHT: {
                Path2D p = new Path2D.Float();
                if (type == CHEV_UP) { p.moveTo(x + s * .22f, y + s * .62f); p.lineTo(x + s * .5f, y + s * .36f); p.lineTo(x + s * .78f, y + s * .62f); }
                else if (type == CHEV_DOWN) { p.moveTo(x + s * .22f, y + s * .38f); p.lineTo(x + s * .5f, y + s * .64f); p.lineTo(x + s * .78f, y + s * .38f); }
                else if (type == CHEV_LEFT) { p.moveTo(x + s * .62f, y + s * .22f); p.lineTo(x + s * .36f, y + s * .5f); p.lineTo(x + s * .62f, y + s * .78f); }
                else { p.moveTo(x + s * .38f, y + s * .22f); p.lineTo(x + s * .64f, y + s * .5f); p.lineTo(x + s * .38f, y + s * .78f); }
                g.setStroke(new BasicStroke(Math.max(1.8f, s / 8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(p);
                break;
            }
            default:
        }
        g.dispose();
    }
}
