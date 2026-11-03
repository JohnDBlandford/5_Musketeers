package Shop.Merchant;

import Shop.Merchant.IMerchant.SlotView;
import Shop.PurchaseResult;
import inventory.Inventory;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

// Everything MerchantScreen decides that is not drawing: which slot is selected, the quantity stepper (1..MAX_PER_PURCHASE,
// capped by remaining stock), buying, and the status message. The screen maps keys to these methods and draws what they report.
// Slots are always read live from the IMerchant, never cached, so a purchase or restock shows up immediately and the
// selection / quantity are re-clamped against whatever is there now.
public final class MerchantViewModel {
    private final IMerchant merchant;
    private final Supplier<Inventory> inventory;   // a supplier because PlayLevelScreen can replace its Inventory object
    private boolean opened = false;
    private int world;
    private int selectedIndex;
    private int quantity;
    private String message = "";

    public MerchantViewModel(IMerchant merchant, Supplier<Inventory> inventory) {
        this.merchant = Objects.requireNonNull(merchant, "merchant");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    // call every time the shop opens: back to the first slot, quantity 1, no message
    public void open(int world) {
        this.world = world;
        this.opened = true;
        this.selectedIndex = 0;
        this.quantity = 1;
        this.message = "";
    }

    public List<SlotView> getSlots() {
        requireOpened();
        return merchant.getSlots(world);
    }

    // clamped to the current slot list (0 when the list is empty)
    public int getSelectedIndex() {
        int size = getSlots().size();
        return Math.clamp(selectedIndex, 0, Math.max(0, size - 1));
    }

    // null when there are no slots
    public SlotView getSelected() {
        List<SlotView> slots = getSlots();
        return slots.isEmpty() ? null : slots.get(getSelectedIndex());
    }

    public void selectPrevious() { selectIndex(getSelectedIndex() - 1); }

    public void selectNext() { selectIndex(getSelectedIndex() + 1); }

    private void selectIndex(int target) {
        int size = getSlots().size();
        int current = getSelectedIndex();
        int next = Math.clamp(target, 0, Math.max(0, size - 1));
        selectedIndex = next;
        if (next != current) {
            quantity = 1;
            message = "";
        }
    }

    // the most the stepper may reach: 1..MAX_PER_PURCHASE, capped by stock; never below 1 so a sold-out slot stays valid
    public int getMaxQuantity() {
        SlotView selected = getSelected();
        if (selected == null || selected.unlimited()) return MerchantConfig.MAX_PER_PURCHASE;
        return Math.clamp(selected.remainingStock(), 1, MerchantConfig.MAX_PER_PURCHASE);
    }

    public int getQuantity() {
        return Math.clamp(quantity, 1, getMaxQuantity());
    }

    public void increaseQuantity() {
        int current = getQuantity();
        if (current < getMaxQuantity()) {
            quantity = current + 1;
            message = "";
        }
    }

    public void decreaseQuantity() {
        int current = getQuantity();
        if (current > 1) {
            quantity = current - 1;
            message = "";
        }
    }

    public int getTotalPrice() {
        SlotView selected = getSelected();
        return selected == null ? 0 : selected.unitPrice() * getQuantity();
    }

    public String getMessage() { return message; }

    public PurchaseResult buy() {
        requireOpened();
        if (getSelected() == null) {
            message = messageFor(PurchaseResult.SOLD_OUT);
            return PurchaseResult.SOLD_OUT;
        }
        PurchaseResult result = merchant.purchase(world, getSelectedIndex(), getQuantity(), inventory.get());
        message = messageFor(result);
        quantity = getQuantity();   // stock may have dropped below the chosen quantity
        return result;
    }

    // PLACEHOLDER wording
    private static String messageFor(PurchaseResult result) {
        return switch (result) {
            case SUCCESS -> "Purchased!";
            case SOLD_OUT -> "Sold out.";
            case NOT_ENOUGH_GOLD -> "Not enough gold.";
            case INVENTORY_FULL -> "Your pack is full.";
            case OVER_LIMIT -> "You can't buy that many.";
        };
    }

    private void requireOpened() {
        if (!opened) throw new IllegalStateException("MerchantViewModel.open(world) must be called first");
    }
}
