package Engine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Mouse support for the screens that need clicks (the waypoint buttons). The engine only reads the keyboard, so this hooks a mouse
// listener onto the game window's panel the first time it is asked, without changing any engine file.
//   MouseInput.update()      call once per frame (it keeps trying to attach until the window exists)
//   MouseInput.pollClick()   the position of a click since the last call (in game-screen coordinates), or null
//   MouseInput.getX()/getY() where the mouse currently is (for hover highlights)
public final class MouseInput {
    private static boolean installed;
    private static volatile Point click;
    private static volatile int mx = -1, my = -1;

    private MouseInput() { }

    public static void update() {
        if (installed) return;
        for (Window w : Window.getWindows()) {
            if (!(w instanceof JFrame) || !w.isShowing()) continue;
            Component panel = ((JFrame) w).getContentPane();
            MouseAdapter listener = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { click = e.getPoint(); }
                @Override public void mouseMoved(MouseEvent e) { mx = e.getX(); my = e.getY(); }
                @Override public void mouseDragged(MouseEvent e) { mx = e.getX(); my = e.getY(); }
                @Override public void mouseExited(MouseEvent e) { mx = -1; my = -1; }
            };
            panel.addMouseListener(listener);
            panel.addMouseMotionListener(listener);
            installed = true;
            return;
        }
    }

    // the last click (consumed: a second call returns null until the mouse is clicked again)
    public static Point pollClick() {
        Point c = click;
        click = null;
        return c;
    }

    public static int getX() { return mx; }

    public static int getY() { return my; }
}