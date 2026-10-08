package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class ItemTestFactory {
    private ItemTestFactory() {}

    // Swords
    public static Item createRustedShortSword(SpriteSheet itemLook) { return new RustedShortSword(itemLook); }
    public static Item createBentKitchenCleaver(SpriteSheet itemLook) { return new BentKitchenCleaver(itemLook); }
    public static Item createScavengersFalchion(SpriteSheet itemLook) { return new ScavengersFalchion(itemLook); }
    public static Item createFamineEdgeBlade(SpriteSheet itemLook) { return new FamineEdgeBlade(itemLook); }
    public static Item createWraithboneSaber(SpriteSheet itemLook) { return new WraithboneSaber(itemLook); }

    // Ranged Weapons
    public static Item createCrackedHuntingBow(SpriteSheet itemLook) { return new CrackedHuntingBow(itemLook); }
    public static Item createSplinteredSling(SpriteSheet itemLook) { return new SplinteredSling(itemLook); }
    public static Item createScavengersCrossBow(SpriteSheet itemLook) { return new ScavengersCrossBow(itemLook); }
    public static Item createFamineEdgeLongbow(SpriteSheet itemLook) { return new FamineEdgeLongbow(itemLook); }
    public static Item createWraithboneRepeater(SpriteSheet itemLook) { return new WraithboneRepeater(itemLook); }

    // Shields
    public static Item createWarpedBucklerShield(SpriteSheet itemLook) { return new WarpedBucklerShield(itemLook); }
    public static Item createPatchedWoodShield(SpriteSheet itemLook) { return new PatchedWoodShield(itemLook); }
    public static Item createScavengersRoundShield(SpriteSheet itemLook) { return new ScavengersRoundShield(itemLook); }
    public static Item createFamineEdgeAegis(SpriteSheet itemLook) { return new FamineEdgeAegis(itemLook); }
    public static Item createWraithboneBulwarkShield(SpriteSheet itemLook) { return new WraithboneBulwarkShield(itemLook); }

    // Armor
    public static Item createBeggarsWrapsArmor(SpriteSheet itemLook, Map<Item.ItemAbilities, Item.AbilityType> itemAbilities) { return new BeggarsWrapsArmor(itemLook, itemAbilities); }
    public static Item createPatchedLeatherArmor(SpriteSheet itemLook, Map<Item.ItemAbilities, Item.AbilityType> itemAbilities) { return new PatchedLeatherArmor(itemLook, itemAbilities); }
    public static Item createScavengersHideArmor(SpriteSheet itemLook, Map<Item.ItemAbilities, Item.AbilityType> itemAbilities) { return new ScavengersHideArmor(itemLook, itemAbilities); }
    public static Item createFamineEdgeVestmentsArmor(SpriteSheet itemLook, Map<Item.ItemAbilities, Item.AbilityType> itemAbilities) { return new FamineEdgeVestmentsArmor(itemLook, itemAbilities); }
    public static Item createWraithboneMailArmor(SpriteSheet itemLook, Map<Item.ItemAbilities, Item.AbilityType> itemAbilities) { return new WraithboneMailArmor(itemLook, itemAbilities); }
}
