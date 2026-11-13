package Chest;

import Item.ItemEntry;

// Every tunable number the chests use, in one place, so balancing never means hunting through logic.
// ALL of these are PLACEHOLDERS (see the chest spec, section 1). If one changes, update the matching test in
// Tests/ChestConfigTests and add a line to the spec's changelog.
public final class ChestConfig {
    private ChestConfig() {}

    // chance that a chest holds an item as well as gold
    private static final double ITEM_CHANCE = 0.35;
    // chance that the item is a consumable instead of gear
    private static final double CONSUMABLE_SHARE = 0.25;

    // placement (ChestPlacer): how many chests a map gets, and the gaps in tiles it keeps
    private static final int CHESTS_PER_MAP = 8;
    // between two chests
    private static final int MIN_CHEST_SPACING = 8;
    // from a door, portal, sign or NPC
    private static final int MIN_ENTITY_SPACING = 3;

    // gold a chest pays, per world, about 3 to 5 enemy kills of that world. index 0 is world 1. World 4 (Final) is a guess,
    // no Final world enemies exist yet to calibrate against. every chest pays gold, an item is the bonus
    private static final int[] MIN_GOLD = {60, 180, 300, 600};
    private static final int[] MAX_GOLD = {120, 360, 600, 1200};

    public static double getItemChance() { return ITEM_CHANCE; }
    public static double getConsumableShare() { return CONSUMABLE_SHARE; }
    public static int getChestsPerMap() { return CHESTS_PER_MAP; }
    public static int getMinChestSpacing() { return MIN_CHEST_SPACING; }
    public static int getMinEntitySpacing() { return MIN_ENTITY_SPACING; }

    // lowest gold a chest in the given world can pay, inclusive
    /**
     * @author Derek
     * @param world the world of the chest
     * @return returns the minimum gold for that world
     */
    public static int getMinGold(int world) {
        ItemEntry.requireValidWorld(world);
        return MIN_GOLD[world - ItemEntry.getMinWorld()];
    }

    // highest gold a chest in the given world can pay, inclusive
    /**
     * @author Derek
     * @param world the world of the chest
     * @return returns the maximum gold for that world
     */
    public static int getMaxGold(int world) {
        ItemEntry.requireValidWorld(world);
        return MAX_GOLD[world - ItemEntry.getMinWorld()];
    }
}
