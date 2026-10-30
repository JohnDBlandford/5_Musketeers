package Item.Registry;

import GameObject.SpriteSheet;
import Item.Armor.World1.*;
import Item.BasicConsumable;
import Item.Item;
import Item.RangedWeapon.World1.*;
import Item.Shield.World1.*;
import Item.Sword.World1.*;

import java.util.function.Function;

import static Item.Item.ItemRarity.*;

// Every world 1 item. Sprites are passed as null because Item.getItemLook() loads the sprite lazily through assignSprite().
// NOTE: all 20 existing items are registered as world 1. Move any that belong to a later world once that is decided.
final class World1Items {
    private static final int WORLD_LVL = 1;

    private World1Items() {}

    static void registerAll() {
        // Swords
        addItem("RustedShortSword", COMMON, RustedShortSword::new);
        addItem("BentKitchenCleaver", COMMON, BentKitchenCleaver::new);
        addItem("ScavengersFalchion", UNCOMMON, ScavengersFalchion::new);
        addItem("FamineEdgeBlade", EPIC, FamineEdgeBlade::new);
        addItem("WraithboneSaber", LEGENDARY, WraithboneSaber::new);

        // Ranged Weapons
        addItem("CrackedHuntingBow", COMMON, CrackedHuntingBow::new);
        addItem("SplinteredSling", COMMON, SplinteredSling::new);
        addItem("ScavengersCrossBow", UNCOMMON, ScavengersCrossBow::new);
        addItem("FamineEdgeLongbow", EPIC, FamineEdgeLongbow::new);
        addItem("WraithboneRepeater", LEGENDARY, WraithboneRepeater::new);

        // Shields
        addItem("WarpedBucklerShield", COMMON, WarpedBucklerShield::new);
        addItem("PatchedWoodShield", COMMON, PatchedWoodShield::new);
        addItem("ScavengersRoundShield", UNCOMMON, ScavengersRoundShield::new);
        addItem("FamineEdgeAegis", EPIC, FamineEdgeAegis::new);
        addItem("WraithboneBulwarkShield", LEGENDARY, WraithboneBulwarkShield::new);

        // Armor
        addItem("BeggarsWrapsArmor", COMMON, BeggarsWrapsArmor::new);
        addItem("PatchedLeatherArmor", COMMON, PatchedLeatherArmor::new);
        addItem("ScavengersHideArmor", UNCOMMON, ScavengersHideArmor::new);
        addItem("FamineEdgeVestmentsArmor", EPIC, FamineEdgeVestmentsArmor::new);
        addItem("WraithboneMailArmor", LEGENDARY, WraithboneMailArmor::new);

        // Consumables. PLACEHOLDER names and restore amounts until the hunger/hydration numbers are balanced
        ItemRegistry.registerConsumable("HardtackRation", WORLD_LVL, COMMON, () -> BasicConsumable.food("HardtackRation", COMMON, 30));
        ItemRegistry.registerConsumable("WaterBottle", WORLD_LVL, COMMON, () -> BasicConsumable.water("WaterBottle", COMMON, 40));
    }

    private static void addItem(String id, Item.ItemRarity rarity, Function<SpriteSheet, Item> constructor) {
        ItemRegistry.register(id, WORLD_LVL, rarity, () -> constructor.apply(null));
    }
}
