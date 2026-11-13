package Chest;

import Item.ItemEntry;
import Item.ItemEntry.ItemKind;
import Item.Registry.LootRoller;

import java.util.List;
import java.util.Objects;
import java.util.Random;

// Rolls what one chest holds (chest spec, section 1). A pure function: the pools are passed in and all randomness comes from
// `random`, so a seeded Random gives a repeatable chest and tests can use tiny hand made pools. Nothing is built or changed.
//  - gold: always paid, picked evenly between the world's minimum and maximum (both inclusive)
//  - item: only with probability ITEM_CHANCE. It is a consumable with probability CONSUMABLE_SHARE and gear otherwise
//  - gear goes through LootRoller.rollOne, so it follows the doc's rarity table, the player's luck and the current world
//    weighting. Consumables are picked evenly from the consumable pool
//  - luck only changes which gear shows up. It never changes the gold or the chance of an item
//  - if the kind that was rolled has an empty pool, the chest holds no item (gold only) instead of switching kind
public final class ChestLoot {
    private ChestLoot() {}

    /**
     * @author Derek
     * @param world the world the contents belong to
     * @param luck the players luck value which increases the chance of an item to also be in the chest
     * @param gearPool the gear pool for the world
     * @param consumablePool the consumable pool for the world
     * @param random the random chance for items to also be in chests
     * @return returns a ChestContents object that sets all of the values
     */
    public static ChestContents rollContents(int world, int luck, List<ItemEntry> gearPool, List<ItemEntry> consumablePool,
                                             Random random) {
        Objects.requireNonNull(gearPool, "gearPool not set");
        Objects.requireNonNull(consumablePool, "consumablePool not set");
        Objects.requireNonNull(random, "random not set");
        ItemEntry.requireValidWorld(world);
        if (luck < 0) throw new IllegalArgumentException("Luck cannot be negative, but got: " + luck);
        for (ItemEntry entry : gearPool) {
            if (entry.itemKind() != ItemKind.GEAR) throw new IllegalArgumentException(entry.id() + " is not gear");
        }
        for (ItemEntry entry : consumablePool) {
            if (entry.itemKind() != ItemKind.CONSUMABLE) throw new IllegalArgumentException(entry.id() + " is not a consumable");
        }

        int gold = rollGold(world, random);
        return new ChestContents(gold, rollItem(world, luck, gearPool, consumablePool, random));
    }

    // inclusive at both ends

    /**
     * @author Derek
     * @param world the world that the gold is in
     * @param random the random value to roll
     * @return returns a random integer from min to max - min + 1
     */
    private static int rollGold(int world, Random random) {
        Objects.requireNonNull(random, "random not set");
        int min = ChestConfig.getMinGold(world);
        int max = ChestConfig.getMaxGold(world);
        return min + random.nextInt(max - min + 1);
    }

    // null when the chest holds no item

    /**
     * @author Derek
     * @param world the world which the item was rolled in
     * @param luck the players luck value, gives a higher chance of an item dropping
     * @param gearPool the gear pool for the world
     * @param consumablePool the consumable pool for the world
     * @param random the random seed to determine what item gets picked
     * @return returns one item with the params required
     */
    private static ItemEntry rollItem(int world, int luck, List<ItemEntry> gearPool, List<ItemEntry> consumablePool,
                                      Random random) {
        Objects.requireNonNull(random, "random not set");
        if (random.nextDouble() >= ChestConfig.getItemChance()) return null;
        if (random.nextDouble() < ChestConfig.getConsumableShare()) {
            if (consumablePool.isEmpty()) return null;
            return consumablePool.get(random.nextInt(consumablePool.size()));
        }
        return LootRoller.rollOne(world, gearPool, luck, random); // null for an empty gear pool
    }
}
