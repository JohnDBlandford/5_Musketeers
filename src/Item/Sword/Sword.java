package Item.Sword;

import GameObject.SpriteSheet;
import Item.Item;
import Item.ItemStats;
import inventory.Inventory;

import java.util.Map;

public abstract class Sword extends Item {
    protected int physicalAtkDmg;

    // placeholder constructor
    protected Sword(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
          Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
          Map<ItemBuffs, Double> itemBuffs, int physicalAtkDmg) {
        this(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, physicalAtkDmg,
                ItemStats.getDefaultsForItemRarity(itemRarity));
    }

    protected Sword(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
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
    public Inventory.EquipSlot getEquipSlot() { return Inventory.EquipSlot.MELEE; }
}
