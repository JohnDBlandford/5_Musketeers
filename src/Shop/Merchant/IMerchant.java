package Shop.Merchant;

import Item.Item;
import Item.ItemEntry;
import Shop.PurchaseResult;
import inventory.Inventory;

import java.util.List;

public interface IMerchant {
    void restockIfDue(int world);
    List<SlotView> getSlots(int world);
    PurchaseResult purchase(int world, int slotIndex, int quantity, Inventory inventory);

    public record SlotView(String id, ItemEntry.ItemKind kind, Item.ItemRarity rarity, int itemWorld, int unitPrice, int remainingStock,
                           boolean unlimited) {
        public boolean isSoldOut() {
            return !unlimited && remainingStock <= 0;
        }
    }
}
