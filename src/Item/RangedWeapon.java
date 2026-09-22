package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class RangedWeapon extends Item permits
        CrackedHuntingBow, SplinteredSling, ScavengersCrossBow, FamineEdgeLongbow, WraithboneRepeater {
    protected int rangedDmg;

    RangedWeapon(String itemName, Sprite itemLook, ItemRarity itemRarity, Map<ItemAbilities, Item> itemAbilities,
                 Map<ItemBuffs, Double> itemBuffs, int rangedDmg) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs);
        this.rangedDmg = rangedDmg;
    }

    public int getRangedDmg() { return rangedDmg; }
}
