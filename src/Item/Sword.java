package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class Sword extends Item permits RustedShortSword,
        BentKitchenCleaver, ScavengersFalchion, FamineEdgeBlade,
WraithboneSaber {
    protected int physicalAtkDmg;
    protected ItemBuffs buff1;
    protected ItemBuffs buff2;
    protected ItemBuffs buff3;

    Sword(String itemName, Sprite itemLook,
          ItemRarity itemRarity, Map<ItemAbilities, Item> itemAbilities,
          Map<ItemBuffs, Item> itemBuffs, int physicalAtkDmg,
          ItemBuffs critChance, ItemBuffs atkBoost, ItemBuffs critDmg) {
        super(itemName, itemLook, itemRarity, itemAbilities,
                itemBuffs);
        this.physicalAtkDmg = physicalAtkDmg;
        this.buff1 = critChance;
        this.buff2 = atkBoost;
        this.buff3 = critDmg;
    }

    public int getPhysicalAtkDmg() { return this.physicalAtkDmg; }
    public ItemBuffs getCritChance() { return this.buff1; }
    public ItemBuffs getAtkBoost() { return this.buff2; }
    public ItemBuffs getCritDmg() { return this.buff3; }
}
