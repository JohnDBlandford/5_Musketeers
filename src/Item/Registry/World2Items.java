package Item.Registry;

import GameObject.SpriteSheet;
import Item.Armor.World2.*;
import Item.Item;
import Item.RangedWeapon.World2.*;
import Item.Shield.World2.*;
import Item.Sword.World2.*;
import Item.Consumable.Consumables;

import java.util.function.Function;

final class World2Items {
    private static final int WORLD_LVL = 2;

    private World2Items() {}

    static void registerAll() {
        // Swords
        addItem(MilitiaShortsword::new);
        addItem(WoodsmansCleaver::new);
        addItem(EnforcersLongsword::new);
        addItem(BaronsBroadsword::new);
        addItem(ClearedWoodZweihander::new);

        // Ranged Weapons
        addItem(MilitiaShortbow::new);
        addItem(WoodsmansCrossbow::new);
        addItem(EnforcersLongbow::new);
        addItem(BaronsRecurve::new);
        addItem(ClearedWoodArbalest::new);

        // Shields
        addItem(MilitiaBuckler::new);
        addItem(WoodsmansTarge::new);
        addItem(EnforcersKiteShield::new);
        addItem(BaronsHeaterShield::new);
        addItem(ClearedWoodBulwarkShield::new);

        // Armor
        addItem(MilitiaLeathers::new);
        addItem(WoodmansHide::new);
        addItem(EnforcersChainmailArmor::new);
        addItem(BaronsPlateArmor::new);
        addItem(ClearedWoodWarplateArmor::new);

        ItemRegistry.registerConsumable(WORLD_LVL, Consumables.REGULAR_RATION);
    }

    private static void addItem(Function<SpriteSheet, Item> constructor) {
        ItemRegistry.register(WORLD_LVL, constructor);
    }
}
