package Shop;

import Engine.GraphicsHandler;
import Shop.Merchant.IMerchant;

import java.util.Objects;

// Owns the shop's open/closed state and the GameClock tick, so PlayLevelScreen only needs to delegate to it.
// One controller serves both the merchant and the blacksmith, so there is one "a shop is open" flag for PlayLevelScreen to check.
// - tick() is called once per frame from PlayLevelScreen's RUNNING state; it counts time only while a shop is closed.
// - open(world) restocks that world if it is due, then shows the merchant panel.
// - openBlacksmith() shows the blacksmith panel. It needs no world (an item's world comes from the registry) and restocks nothing.
// - update() and draw() reach only the panel that was opened, and close() runs onClosed (PlayLevelScreen passes player::unlock).
public final class ShopController {
    private static final int MIN_WORLD = 1;
    private static final int MAX_WORLD = 4;

    // What the controller needs from the UI. MerchantScreen and BlacksmithScreen implement this.
    // the blacksmith is always told world 0, which its panel ignores
    public interface Panel {
        void onOpen(int world);
        void update();
        void draw(GraphicsHandler graphicsHandler);
    }

    private final GameClock clock;
    private final IMerchant merchant;
    private final Panel merchantPanel;
    private final Panel blacksmithPanel;   // null when built without a blacksmith
    private final Runnable onClosed;
    private Panel activePanel = null;      // the panel that was opened, null while closed
    private boolean open = false;

    // a controller for the merchant only
    public ShopController(GameClock clock, IMerchant merchant, Panel merchantPanel, Runnable onClosed) {
        this.clock = Objects.requireNonNull(clock, "clock not set");
        this.merchant = Objects.requireNonNull(merchant, "merchant not set");
        this.merchantPanel = Objects.requireNonNull(merchantPanel, "panel not set");
        this.blacksmithPanel = null;
        this.onClosed = Objects.requireNonNull(onClosed, "onClosed not set. Attach a Runnable.");
    }

    // a controller for the merchant and the blacksmith
    public ShopController(GameClock clock, IMerchant merchant, Panel merchantPanel, Panel blacksmithPanel, Runnable onClosed) {
        this.clock = Objects.requireNonNull(clock, "clock not set");
        this.merchant = Objects.requireNonNull(merchant, "merchant not set");
        this.merchantPanel = Objects.requireNonNull(merchantPanel, "merchant panel not set");
        this.blacksmithPanel = Objects.requireNonNull(blacksmithPanel, "blacksmith panel not set");
        this.onClosed = Objects.requireNonNull(onClosed, "onClosed not set. Attach a Runnable. ");
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
        activePanel = merchantPanel;
        merchantPanel.onOpen(world);
    }

    // throws IllegalStateException if this controller was built without a blacksmith panel, even if a shop is already open
    public void openBlacksmith() {
        if (blacksmithPanel == null) throw new IllegalStateException("This ShopController was built without a blacksmith panel");
        if (open) return;
        open = true;
        activePanel = blacksmithPanel;
        blacksmithPanel.onOpen(0);
    }

    public void close() {
        if (!open) return;
        open = false;
        activePanel = null;
        onClosed.run();
    }

    public void update() {
        if (open) activePanel.update();
    }

    public void draw(GraphicsHandler graphicsHandler) {
        if (open) activePanel.draw(graphicsHandler);
    }
}
