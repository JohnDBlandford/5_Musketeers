package Shop.Merchant;

import Item.Consumable.Consumables;
import Item.ItemEntry;
import Item.Item.ItemRarity;
import Item.Registry.LootRoller;

import java.util.Map;
import java.util.Objects;

// Every tunable number the merchant uses, in one place, so balancing never means hunting through logic.
// ALL of these are PLACEHOLDERS (see the merchant spec, section 6). If one changes, update the matching test in
// Tests/MerchantConfigTests and add a line to the spec's changelog.
public final class MerchantConfig {
    private MerchantConfig() {}

    // shop layout and limits
    private static final int GEAR_SLOTS = 8;
    private static final int FOOD_SLOTS = 1;
    private static final int WATER_SLOTS = 1;

    // most of one item a player can buy in a single purchase
    private static final int MAX_PER_PURCHASE = 5;
    // in-game minutes between restocks (GameClock turns this into frames)
    private static final int RESTOCK_INTERVAL_MINUTES = 30;
    // how much more likely an item from the shop's own world is to be picked than an item from an older world
    // (the number itself lives in LootRoller now, since chests use the same rule)
    private static final int CURRENT_WORLD_WEIGHT = LootRoller.CURRENT_WORLD_WEIGHT;

    // prices
    // gear price = round(base price of the rarity * multiplier of the item's OWN world)
    private static final int COMMON_BASE_PRICE = 40;
    private static final int UNCOMMON_BASE_PRICE = 120;
    private static final int EPIC_BASE_PRICE = 400;
    private static final int LEGENDARY_BASE_PRICE = 1200;

    // index 0 is world 1. World 4 (Final) is a guess, no Final world enemies exist yet to calibrate against
    private static final double[] WORLD_MULTIPLIERS = {1.0, 3.0, 5.0, 8.0};

    // keyed by the consumable's item name. built from the Consumables constants so a rename cannot leave a stale string
    private static final Map<String, Integer> CONSUMABLE_PRICES = Map.of(
            Consumables.HARDTACK_RATION.getItemName(), 10,
            Consumables.REGULAR_RATION.getItemName(), 20,
            Consumables.WATER_BOTTLE.getItemName(), 8);

    // stock per gear slot
    private static final int COMMON_STOCK = 5;
    private static final int UNCOMMON_STOCK = 4;
    private static final int EPIC_STOCK = 2;

    // 1 on purpose, but it means a legendary pair can't be bought in one visit (noted in the spec for the Blacksmith)
    private static final int LEGENDARY_STOCK = 1;

    public static int getGearSlots() { return GEAR_SLOTS; }
    public static int getFoodSlots() { return FOOD_SLOTS; }
    public static int getWaterSlots() { return WATER_SLOTS; }
    public static int getMaxPerPurchase() { return MAX_PER_PURCHASE; }
    public static int getRestockIntervalMins() { return RESTOCK_INTERVAL_MINUTES; }
    public static int getCurrWorldWeight() { return CURRENT_WORLD_WEIGHT; }

    // number of items a single slot starts with
    public static int getStockPerSlot(ItemRarity rarity) {
        return switch (Objects.requireNonNull(rarity, "rarity not assigned")) {
            case COMMON -> COMMON_STOCK;
            case UNCOMMON -> UNCOMMON_STOCK;
            case EPIC -> EPIC_STOCK;
            case LEGENDARY -> LEGENDARY_STOCK;
        };
    }

    // loot weight (out of 100) of a rarity for a shop in the given world, from the design doc. the table lives in LootRoller
    // so the chests share it. player's LCK is deliberately not a parameter: it has no effect on the shop
    public static int getRarityWeight(int world, ItemRarity rarity) {
        return LootRoller.getRarityWeight(world, rarity);
    }

    public static int getBasePrice(ItemRarity rarity) {
        return switch (Objects.requireNonNull(rarity, "rarity price not set")) {
            case COMMON -> COMMON_BASE_PRICE;
            case UNCOMMON -> UNCOMMON_BASE_PRICE;
            case EPIC -> EPIC_BASE_PRICE;
            case LEGENDARY -> LEGENDARY_BASE_PRICE;
        };
    }

    public static double getWorldMultiplier(int world) {
        ItemEntry.requireValidWorld(world);
        return WORLD_MULTIPLIERS[world - ItemEntry.getMinWorld()];
    }

    // price of one gear item. itemWorld is the world the ITEM belongs to, not the world of the shop selling it
    public static int getGearPrice(ItemRarity rarity, int itemWorld) {
        return (int) Math.round(getBasePrice(rarity) * getWorldMultiplier(itemWorld));
    }

    // price of one consumable. an unknown name is an error, so a missing price can never become a free item
    public static int getConsumablePrice(String itemName) {
        Integer price = itemName == null ? null : CONSUMABLE_PRICES.get(itemName);
        if (price == null) throw new IllegalArgumentException("No price set for consumable: " + itemName);
        return price;
    }
}
