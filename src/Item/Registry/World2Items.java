package Item.Registry;

import GameObject.SpriteSheet;
import Item.Armor.World2.*;
import Item.Item;
import Item.RangedWeapon.World2.*;
import Item.Shield.World2.*;
import Item.Sword.World2.*;
import Item.Consumable.BasicConsumable;

import java.util.function.Function;

import static Item.Item.ItemRarity.*;

final class World2Items {
    private static final int WORLD_LVL = 2;

    private World2Items() {}

    static void registerAll() {
        // Swords
        addItem("MilitiaShortsword", COMMON, MilitiaShortsword::new);
        addItem("WoodsmansCleaver", COMMON, WoodsmansCleaver::new);
        addItem("EnforcersLongsword", UNCOMMON, EnforcersLongsword::new);
        addItem("BaronsBroadsword", EPIC, BaronsBroadsword::new);
        addItem("ClearedWoodZweihander", LEGENDARY, ClearedWoodZweihander::new);

        // Ranged Weapons
        addItem("MilitiaShortbow", COMMON, MilitiaShortbow::new);
        addItem("WoodsmansCrossbow", COMMON, WoodsmansCrossbow::new);
        addItem("EnforcersLongbow", UNCOMMON, EnforcersLongbow::new);
        addItem("BaronsRecurve", EPIC, BaronsRecurve::new);
        addItem("ClearedWoodArbalest", LEGENDARY, ClearedWoodArbalest::new);

        // Shields
        addItem("MilitiaBuckler", COMMON, MilitiaBuckler::new);
        addItem("WoodsmansTarge", COMMON, WoodsmansTarge::new);
        addItem("EnforcersKiteShield", UNCOMMON, EnforcersKiteShield::new);
        addItem("BaronsHeaterShield", EPIC, BaronsHeaterShield::new);
        addItem("ClearedWoodBulwarkShield", LEGENDARY, ClearedWoodBulwarkShield::new);

        // Armor
        addItem("MilitiaLeathers", COMMON, MilitiaLeathers::new);
        addItem("WoodmansHide", COMMON, WoodmansHide::new);
        addItem("EnforcersChainmailArmor", UNCOMMON, EnforcersChainmailArmor::new);
        addItem("BaronsPlateArmor", EPIC, BaronsPlateArmor::new);
        addItem("ClearedWoodWarplateArmor", LEGENDARY, ClearedWoodWarplateArmor::new);

        ItemRegistry.registerConsumable("RegularRation", WORLD_LVL, COMMON, () -> BasicConsumable.food("RegularRation", COMMON, 30));
    }

    private static void addItem(String id, Item.ItemRarity rarity, Function<SpriteSheet, Item> constructor) {
        ItemRegistry.register(id, WORLD_LVL, rarity, () -> constructor.apply(null));
    }
}
