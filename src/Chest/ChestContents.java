package Chest;

import Item.ItemEntry;

// What one chest holds: the gold it pays, and the item it also holds, if any. item is the registry ENTRY (id, kind, rarity,
// world) and not a built Item, so rolling a chest never creates anything. ChestController builds the real item with
// entry.create() only at the moment it puts it in the pack. item is null for a chest that only holds gold.
public record ChestContents(int gold, ItemEntry item) {
    public ChestContents {
        if (gold < 0) throw new IllegalArgumentException("Gold cannot be negative, but got: " + gold);
    }

    public boolean hasItem() {
        return item != null;
    }
}
