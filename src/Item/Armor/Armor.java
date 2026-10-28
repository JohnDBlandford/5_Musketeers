package Item.Armor;

import GameObject.SpriteSheet;
import Item.Item;
import Item.ItemStats;
import inventory.Inventory;

import java.util.Map;

public abstract class Armor extends Item {
    protected int baseDef;

    // placeholder constructor
    Armor(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
          Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities, Map<ItemBuffs, Double> itemBuffs, int baseDef) {
        this(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, baseDef, ItemStats.getDefaultsForItemRarity(itemRarity));
    }

    protected Armor(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
          Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities, Map<ItemBuffs, Double> itemBuffs, int baseDef,
          ItemStats stats) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, stats);
        this.baseDef = baseDef;
    }

    // this is the lvl 1 val, if you need to get the base def, use the method below instead (the one with the override)
    public int getBaseDef() { return this.baseDef; }

    @Override
    public int getBaseDefense() { return this.baseDef; }

    @Override
    public Inventory.EquipSlot getEquipSlot() { return Inventory.EquipSlot.ARMOR; }
}
