package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class Sword extends Item permits RustedShortSword,
        BentKitchenCleaver, ScavengersFalchion, FamineEdgeBlade, WraithboneSaber {
    protected int physicalAtkDmg;

    Sword(String itemName, Sprite itemLook, ItemRarity itemRarity, Map<ItemAbilities, Item> itemAbilities,
          Map<ItemBuffs, Double> itemBuffs, int physicalAtkDmg) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs);
        this.physicalAtkDmg = physicalAtkDmg;
    }

    public int getPhysicalAtkDmg() { return this.physicalAtkDmg; }
}
