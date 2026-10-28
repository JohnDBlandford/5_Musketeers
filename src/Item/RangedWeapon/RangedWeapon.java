package Item.RangedWeapon;

import GameObject.SpriteSheet;
import Item.Item;
import Item.ItemStats;
import inventory.Inventory;

import java.util.Map;

public abstract class RangedWeapon extends Item {
    protected int rangedDmg;

    // placeholder constructor
    RangedWeapon(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
                 Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
                 Map<ItemBuffs, Double> itemBuffs, int rangedDmg) {
        this(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, rangedDmg,
                ItemStats.getDefaultsForItemRarity(itemRarity));
    }

    protected RangedWeapon(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
                 Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
                 Map<ItemBuffs, Double> itemBuffs, int rangedDmg, ItemStats stats) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, stats);
        this.rangedDmg = rangedDmg;
    }

    // this is the base lvl 1 dont use this method to get the dmg, use the method below marked as override
    public int getRangedDmg() { return rangedDmg; }

    @Override
    public int getBaseAttack() { return rangedDmg; }

    @Override
    public Inventory.EquipSlot getEquipSlot() { return Inventory.EquipSlot.RANGED_WEAPON; }
}
