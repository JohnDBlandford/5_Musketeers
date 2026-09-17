package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class RangedWeapon extends Item permits
        CrackedHuntingBow, SplinteredSling, ScavengersCrossBow,
        FamineEdgeLongbow, WraithboneRepeater {
    private int rangedDmg;
    private ItemBuffs accuracy;
    private ItemBuffs critChance;
    private ItemBuffs piercing;
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
        this.critChance = critChance;
        this.atkBoost = atkBoost;
        this.critDmg = critDmg;
        this.accuracy = accuracy;
        this.piercing = piercing;
    }

    public ItemBuffs getWeaponAccuracy() { return this.accuracy; }
    public ItemBuffs getCritChance() { return this.critChance; }
    public ItemBuffs getPiercing() { return this.piercing; }
    public int getRangedDmg() { return rangedDmg; }
    public ItemBuffs getAtkBoost() { return this.atkBoost; }
    public ItemBuffs getCritDmg() { return critDmg; }
}
