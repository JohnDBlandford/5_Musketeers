package Item.RangedWeapon;

import GameObject.SpriteSheet;
import Item.Item;
import inventory.EquipSlot;
import Item.ItemStats;

import java.util.Map;

public sealed abstract class RangedWeapon extends Item permits
        CrackedHuntingBow, SplinteredSling, ScavengersCrossBow, FamineEdgeLongbow, WraithboneRepeater {
    protected int rangedDmg;

    // placeholder constructor
    RangedWeapon(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
                 Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
                 Map<ItemBuffs, Double> itemBuffs, int rangedDmg) {
        this(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, rangedDmg,
                ItemStats.getDefaultsForItemRarity(itemRarity));
    }

    RangedWeapon(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
                 Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
                 Map<ItemBuffs, Double> itemBuffs, int rangedDmg, ItemStats stats) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, stats);
        this.rangedDmg = rangedDmg;
    }

    // this is the base lvl 1 dont use this method to get the dmg, use the method below marked as override
    public int getRangedDmg() { return rangedDmg; }

    @Override
    public int getBaseAttack() { return rangedDmg; }

    @Override
    public EquipSlot getEquipSlot() { return EquipSlot.RANGED_WEAPON; }
}
