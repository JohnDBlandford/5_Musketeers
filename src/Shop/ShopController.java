package Shop;

import Engine.GraphicsHandler;
import Shop.Merchant.IMerchant;

import java.util.Objects;

// Owns the shop's open/closed state and the GameClock tick, so PlayLevelScreen only needs to delegate to it.
// - tick() is called once per frame from PlayLevelScreen's RUNNING state; it counts time only while the shop is closed.
// - open(world) restocks that world if it is due, then shows the panel.
// - close() runs onClosed (PlayLevelScreen passes player::unlock).
public final class ShopController {
    public static final int MIN_WORLD = 1;
    public static final int MAX_WORLD = 4;

    // What the controller needs from the UI. MerchantScreen implements this.
    public interface Panel {
        void onOpen(int world);
        void update();
        void draw(GraphicsHandler graphicsHandler);
    }

    private final GameClock clock;
    private final IMerchant merchant;
    private final Panel panel;
    private final Runnable onClosed;
    private boolean open = false;

    public ShopController(GameClock clock, IMerchant merchant, Panel panel, Runnable onClosed) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.merchant = Objects.requireNonNull(merchant, "merchant");
        this.panel = Objects.requireNonNull(panel, "panel");
        this.onClosed = Objects.requireNonNull(onClosed, "onClosed");
    }

    public boolean isOpen() { return open; }

    // no time passes inside the shop
    public void tick() {
        if (!open) clock.tick();
    }

    public void open(int world) {
        if (open) return;
        if (world < MIN_WORLD || world > MAX_WORLD) {
            throw new IllegalArgumentException("World must be between " + MIN_WORLD + "-" + MAX_WORLD + ", but got: " + world);
        }
        merchant.restockIfDue(world);
        open = true;
        panel.onOpen(world);
    }

    public void close() {
        if (!open) return;
        open = false;
        onClosed.run();
    }

    public void update() {
        if (open) panel.update();
    }

    public void draw(GraphicsHandler graphicsHandler) {
        if (open) panel.draw(graphicsHandler);
    }
}
