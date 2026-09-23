package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public sealed abstract class RangedWeapon extends Item permits
        CrackedHuntingBow, SplinteredSling, ScavengersCrossBow, FamineEdgeLongbow, WraithboneRepeater {
    protected int rangedDmg;

    RangedWeapon(String itemName, SpriteSheet itemLook, ItemRarity itemRarity, Map<ItemAbilities, AbilityType> itemAbilities,
                 Map<ItemBuffs, Double> itemBuffs, int rangedDmg) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs);
        this.rangedDmg = rangedDmg;
    }

    public int getRangedDmg() { return rangedDmg; }
}
