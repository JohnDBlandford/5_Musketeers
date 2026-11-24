package Chest;

import Enchantment.EnchantmentConfig;
import Item.ItemEntry;
import Item.ItemEntry.ItemKind;
import Item.Registry.LootRoller;

import java.util.List;
import java.util.Objects;
import java.util.Random;

// Rolls what one chest holds (chest spec, section 1). A pure function: the pools are passed in and all randomness comes from
// `random`, so a seeded Random gives a repeatable chest and tests can use tiny hand made pools. Nothing is built or changed.
//  - gold: always paid, picked evenly between the world's minimum and maximum (both inclusive)
//  - item: only with probability ITEM_CHANCE. It is a consumable with probability CONSUMABLE_SHARE, a book with probability
//    EnchantmentConfig.getBookShare() and gear with whatever is left, all decided by one draw
//  - gear goes through LootRoller.rollOne, so it follows the doc's rarity table, the player's luck and the current world
//    weighting. Consumables and books are picked evenly from their pool: the caller passes exactly the books of the chest's
//    world, so a book's level is the chest's world and it never goes through the rarity table
//  - luck only changes which gear shows up. It never changes the gold, the chance of an item or the split between kinds
//  - if the kind that was rolled has an empty pool, the chest holds no item (gold only) instead of switching kind
public final class ChestLoot {
    private ChestLoot() {}

    /**
     * @author Derek
     * @param world the world the contents belong to
     * @param luck the players luck value which increases the chance of an item to also be in the chest
     * @param gearPool the gear pool for the world
     * @param bookPool the enchantment books of the world (exactly that world's level)
     * @param consumablePool the consumable pool for the world
     * @param random the random chance for items to also be in chests
     * @return returns a ChestContents object that sets all of the values
     */
    public static ChestContents rollContents(int world, int luck, List<ItemEntry> gearPool, List<ItemEntry> bookPool,
                                             List<ItemEntry> consumablePool, Random random) {
        Objects.requireNonNull(gearPool, "gearPool not set");
        Objects.requireNonNull(bookPool, "bookPool not set");
        Objects.requireNonNull(consumablePool, "consumablePool not set");
        Objects.requireNonNull(random, "random not set");
        ItemEntry.requireValidWorld(world);
        if (luck < 0) throw new IllegalArgumentException("Luck cannot be negative, but got: " + luck);
        for (ItemEntry entry : gearPool) {
            if (entry.itemKind() != ItemKind.GEAR) throw new IllegalArgumentException(entry.id() + " is not gear");
        }
        for (ItemEntry entry : bookPool) {
            if (entry.itemKind() != ItemKind.BOOK) throw new IllegalArgumentException(entry.id() + " is not a book");
        }
        for (ItemEntry entry : consumablePool) {
            if (entry.itemKind() != ItemKind.CONSUMABLE) throw new IllegalArgumentException(entry.id() + " is not a consumable");
        }

        int gold = rollGold(world, random);
        return new ChestContents(gold, rollItem(world, luck, gearPool, bookPool, consumablePool, random));
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
     * @param bookPool the enchantment books of the world
     * @param consumablePool the consumable pool for the world
     * @param random the random seed to determine what item gets picked
     * @return returns one item with the params required
     */
    private static ItemEntry rollItem(int world, int luck, List<ItemEntry> gearPool, List<ItemEntry> bookPool,
                                      List<ItemEntry> consumablePool, Random random) {
        Objects.requireNonNull(random, "random not set");
        if (random.nextDouble() >= ChestConfig.getItemChance()) return null;
        // one draw picks the kind: consumables first, then books, and gear gets what is left
        double kind = random.nextDouble();
        if (kind < ChestConfig.getConsumableShare()) {
            if (consumablePool.isEmpty()) return null;
            return consumablePool.get(random.nextInt(consumablePool.size()));
        }
        if (kind < ChestConfig.getConsumableShare() + EnchantmentConfig.getBookShare()) {
            if (bookPool.isEmpty()) return null;
            return bookPool.get(random.nextInt(bookPool.size())); // uniform, never through the rarity table
        }
        return LootRoller.rollOne(world, gearPool, luck, random); // null for an empty gear pool
    }
}
