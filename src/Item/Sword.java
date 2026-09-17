package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class Sword extends Item permits RustedShortSword,
        BentKitchenCleaver, ScavengersFalchion, FamineEdgeBlade,
WraithboneSaber {
    protected int physicalAtkDmg;
    protected ItemBuffs critChance;
    protected ItemBuffs atkBoost;
    protected ItemBuffs critDmg;

    Sword(String itemName, Sprite itemLook,
          ItemRarity itemRarity, Map<ItemAbilities, Item> itemAbilities,
          Map<ItemBuffs, Item> itemBuffs, int physicalAtkDmg,
          ItemBuffs critChance, ItemBuffs atkBoost, ItemBuffs critDmg) {
        super(itemName, itemLook, itemRarity, itemAbilities,
                itemBuffs);
        this.physicalAtkDmg = physicalAtkDmg;
        this.critChance = critChance;
        this.atkBoost = atkBoost;
        this.critDmg = critDmg;
    }

    public int getPhysicalAtkDmg() { return this.physicalAtkDmg; }
    public ItemBuffs getCritChance() { return this.critChance; }
    public ItemBuffs getAtkBoost() { return this.atkBoost; }
    public ItemBuffs getCritDmg() { return this.critDmg; }
}
