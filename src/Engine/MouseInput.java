package Engine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Mouse support for the screens that need clicks (the waypoint buttons, the world map). The engine only reads the keyboard, so this
// hooks a mouse listener onto the game window's panel the first time it is asked, without changing any engine file.
//   MouseInput.update()      call once per frame (it keeps trying to attach until the window exists)
//   MouseInput.pollClick()   the position of a click since the last call (in game-screen coordinates), or null
//   MouseInput.getX()/getY() where the mouse currently is (for hover highlights), or -1 when it is outside the game area
//   MouseInput.pollWheel()   mouse-wheel notches since the last call (the world map zooms with it)
//
// GamePanel scales the game screen to fit the window and centers it with black bars (see paintComponent), so a mouse
// position in window pixels is turned back into game-screen pixels here with the same scale and offset.
public final class MouseInput {
    private static boolean installed;
    private static volatile Point click;
    private static volatile int mx = -1, my = -1;
    private static volatile int wheel;

    private MouseInput() { }

    public static void update() {
        if (installed) return;
        for (Window w : Window.getWindows()) {
            if (!(w instanceof JFrame) || !w.isShowing()) continue;
            Component panel = ((JFrame) w).getContentPane();
            MouseAdapter listener = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { click = toGame(panel, e.getX(), e.getY()); }
                @Override public void mouseMoved(MouseEvent e) { move(panel, e); }
                @Override public void mouseDragged(MouseEvent e) { move(panel, e); }
                @Override public void mouseExited(MouseEvent e) { mx = -1; my = -1; }
                @Override public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) { wheel += e.getWheelRotation(); }
            };
            panel.addMouseListener(listener);
            panel.addMouseMotionListener(listener);
            panel.addMouseWheelListener(listener);
            installed = true;
            return;
        }
    }

    private static void move(Component panel, MouseEvent e) {
        Point p = toGame(panel, e.getX(), e.getY());
        mx = p == null ? -1 : p.x;
        my = p == null ? -1 : p.y;
    }

    // window pixels -> game-screen pixels (null if the point is on the black bars outside the game)
    private static Point toGame(Component panel, int x, int y) {
        int lw = ScreenManager.getScreenWidth(), lh = ScreenManager.getScreenHeight();
        int pw = panel.getWidth(), ph = panel.getHeight();
        if (lw <= 0 || lh <= 0 || pw <= 0 || ph <= 0) return new Point(x, y);
        double scale = Math.min(pw / (double) lw, ph / (double) lh);
        int offX = (int) Math.round((pw - lw * scale) / 2);
        int offY = (int) Math.round((ph - lh * scale) / 2);
        int gx = (int) Math.floor((x - offX) / scale), gy = (int) Math.floor((y - offY) / scale);
        if (gx < 0 || gy < 0 || gx >= lw || gy >= lh) return null;
        return new Point(gx, gy);
    }

    // the last click (consumed: a second call returns null until the mouse is clicked again)
    public static Point pollClick() {
        Point c = click;
        click = null;
        return c;
    }

    // wheel notches turned since the last call (negative = away from you / up), consumed like pollClick
    public static int pollWheel() {
        int w = wheel;
        wheel = 0;
        return w;
    }

    public static int getX() { return mx; }

    public static int getY() { return my; }
}