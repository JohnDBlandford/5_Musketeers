package Item.Sword;

import GameObject.SpriteSheet;
import Item.Item;
import inventory.EquipSlot;

import java.util.Map;

public sealed abstract class Sword extends Item permits RustedShortSword,
        BentKitchenCleaver, ScavengersFalchion, FamineEdgeBlade, WraithboneSaber {
    protected int physicalAtkDmg;

    Sword(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
          Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
          Map<ItemBuffs, Double> itemBuffs, int physicalAtkDmg) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs);
        this.physicalAtkDmg = physicalAtkDmg;
    }

    public int getPhysicalAtkDmg() { return this.physicalAtkDmg; }

    @Override
    public EquipSlot getEquipSlot() { return EquipSlot.MELEE; }
}
