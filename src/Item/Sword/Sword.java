package Item.Sword;

import GameObject.SpriteSheet;
import Item.Item;
import inventory.EquipSlot;
import Item.ItemStats;

import java.util.Map;

public sealed abstract class Sword extends Item permits RustedShortSword,
        BentKitchenCleaver, ScavengersFalchion, FamineEdgeBlade, WraithboneSaber {
    protected int physicalAtkDmg;

    // placeholder constructor
    Sword(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
          Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
          Map<ItemBuffs, Double> itemBuffs, int physicalAtkDmg) {
        this(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, physicalAtkDmg,
                ItemStats.getDefaultsForItemRarity(itemRarity));
    }

    Sword(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
          Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
          Map<ItemBuffs, Double> itemBuffs, int physicalAtkDmg, ItemStats stats) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, stats);
        this.physicalAtkDmg = physicalAtkDmg;
    }

    // dont use this method for getting the base dmg use the one under this marked as override
    public int getPhysicalAtkDmg() { return this.physicalAtkDmg; }

    @Override
    public int getBaseAttack() { return this.physicalAtkDmg; }

    @Override
    public EquipSlot getEquipSlot() { return EquipSlot.MELEE; }
}
