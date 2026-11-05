package Item.Consumable;

import static Item.Item.ItemRarity.COMMON;

// Every consumable in the game, defined once. The registrars register these constants and test code references them directly,
// so no consumable is ever looked up by a string. BasicConsumable is immutable, so one shared instance per consumable is safe.
// PLACEHOLDER restore amounts until the hunger/hydration numbers are balanced.
public final class Consumables {
    private Consumables() {}

    public static final BasicConsumable HARDTACK_RATION = BasicConsumable.food("HardtackRation", COMMON, 30);
    public static final BasicConsumable REGULAR_RATION = BasicConsumable.food("RegularRation", COMMON, 30);
    public static final BasicConsumable WATER_BOTTLE = BasicConsumable.water("WaterBottle", COMMON, 40);
}
