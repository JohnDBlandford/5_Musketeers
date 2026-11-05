package Item.Registry;

import GameObject.SpriteSheet;
import Item.Armor.World1.*;
import Item.Consumable.Consumables;
import Item.Item;
import Item.RangedWeapon.World1.*;
import Item.Shield.World1.*;
import Item.Sword.World1.*;

import java.util.function.Function;

// Every world 1 item. Sprites are passed as null because Item.getItemLook() loads the sprite lazily through assignSprite().
// NOTE: all 20 existing items are registered as world 1
final class World1Items {
    private static final int WORLD_LVL = 1;

    private World1Items() {}

    static void registerAll() {
        // Swords
        addItem(RustedShortSword::new);
        addItem(BentKitchenCleaver::new);
        addItem(ScavengersFalchion::new);
        addItem(FamineEdgeBlade::new);
        addItem(WraithboneSaber::new);

        // Ranged Weapons
        addItem(CrackedHuntingBow::new);
        addItem(SplinteredSling::new);
        addItem(ScavengersCrossBow::new);
        addItem(FamineEdgeLongbow::new);
        addItem(WraithboneRepeater::new);

        // Shields
        addItem(WarpedBucklerShield::new);
        addItem(PatchedWoodShield::new);
        addItem(ScavengersRoundShield::new);
        addItem(FamineEdgeAegis::new);
        addItem(WraithboneBulwarkShield::new);

        // Armor
        addItem(BeggarsWrapsArmor::new);
        addItem(PatchedLeatherArmor::new);
        addItem(ScavengersHideArmor::new);
        addItem(FamineEdgeVestmentsArmor::new);
        addItem(WraithboneMailArmor::new);

        // Consumables
        ItemRegistry.registerConsumable(WORLD_LVL, Consumables.HARDTACK_RATION);
        ItemRegistry.registerConsumable(WORLD_LVL, Consumables.WATER_BOTTLE);
    }

    private static void addItem(Function<SpriteSheet, Item> constructor) {
        ItemRegistry.register(WORLD_LVL, constructor);
    }
}
