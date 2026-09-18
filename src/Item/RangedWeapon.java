package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class RangedWeapon extends Item permits
        CrackedHuntingBow, SplinteredSling, ScavengersCrossBow,
        FamineEdgeLongbow, WraithboneRepeater {
    private int rangedDmg;
    private ItemBuffs buff1;
    private ItemBuffs buff2;
    private ItemBuffs buff3;
    private ItemBuffs atkBoost;
    private ItemBuffs critDmg;

    RangedWeapon(String itemName, Sprite itemLook,
                 ItemRarity itemRarity, Map<ItemAbilities, Item> itemAbilities,
                 Map<ItemBuffs, Item> itemBuffs, int rangedDmg,
                 ItemBuffs critChance, ItemBuffs atkBoost, ItemBuffs critDmg,
                 ItemBuffs accuracy, ItemBuffs piercing) {
        super(itemName, itemLook, itemRarity, itemAbilities,
                itemBuffs);
        this.rangedDmg = rangedDmg;
        this.buff2 = critChance;
        this.atkBoost = atkBoost;
        this.critDmg = critDmg;
        this.buff1 = accuracy;
        this.buff3 = piercing;
    }

    public ItemBuffs getWeaponAccuracy() { return this.buff1; }
    public ItemBuffs getCritChance() { return this.buff2; }
    public ItemBuffs getPiercingChance() { return this.buff3; }
    public int getRangedDmg() { return rangedDmg; }
    public ItemBuffs getAtkBoost() { return this.atkBoost; }
    public ItemBuffs getCritDmg() { return critDmg; }
}
