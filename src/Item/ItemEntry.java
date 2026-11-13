package Item;

import java.util.function.Supplier;

// One row of the item registry. The registry stores how to build an item, never a built item, because every Item owns
// its own ItemStats (level, xp, durability). A shared instance would mean every copy of a sword shares one durability.
// id is the same string as Stockable.getItemName() so the id also works as the save key and the blacksmith's "same name" check
public record ItemEntry(String id, int world, Item.ItemRarity rarity, ItemKind itemKind, Supplier<Stockable> factory) {
    private static final int MIN_WORLD = 1;
    private static final int MAX_WORLD = 4;

    public static int getMinWorld() { return MIN_WORLD; }
    public static int getMaxWorld() { return MAX_WORLD; }

    public static boolean isValidWorld(final int world) { return world >= MIN_WORLD && world <= MAX_WORLD; }

    // the one place the world range is enforced, so anything that takes a world number can validate it the same way
    public static void requireValidWorld(final int world) {
        if (!isValidWorld(world)) {
            throw new IllegalArgumentException("World must be between " + MIN_WORLD + "-" + MAX_WORLD + ", but got: " + world);
        }
    }

    // GEAR entries build an Item, CONSUMABLE entries build a Consumable
    public enum ItemKind { GEAR, CONSUMABLE }

    public ItemEntry {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Item id cannot be blank");
        if (!isValidWorld(world)) {
            throw new IllegalArgumentException("World must be between " + MIN_WORLD + "-" + MAX_WORLD + " for " + id + ", but got: "
                    + world);
        }
        if (rarity == null) throw new IllegalArgumentException("Rarity cannot be null for " + id);
        if (itemKind == null) throw new IllegalArgumentException("Kind cannot be null for " + id);
        if (factory == null) throw new IllegalArgumentException("Factory cannot be null for " + id);
    }

    // always a brand new instance
    public Stockable create() { return factory.get(); }
}
