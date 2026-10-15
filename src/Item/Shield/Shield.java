package Item.Shield;

import GameObject.SpriteSheet;
import Item.Item;
import inventory.EquipSlot;
import Item.ItemStats;

import java.util.Map;

public sealed abstract class Shield extends Item permits
        WarpedBucklerShield, PatchedWoodShield, ScavengersRoundShield, FamineEdgeAegis, WraithboneBulwarkShield {
    protected double blockChance;
    protected double blockAmount;
    protected int baseDef;

    // this constructor is for placeholder stats
    Shield(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
           Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
           Map<ItemBuffs, Double> itemBuffs, double blockChance, double blockAmount) {
        this(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, blockChance, blockAmount,
                0, ItemStats.getDefaultsForItemRarity(itemRarity));
    }

    Shield(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
           Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities,
           Map<ItemBuffs, Double> itemBuffs, double blockChance, double blockAmount, int baseDef, ItemStats stats) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs, stats);
        this.blockChance = blockChance;
        this.blockAmount = blockAmount;
        this.baseDef = baseDef;
    }

    public double getBlockChance() { return this.blockChance; }
    public double getBlockAmount() { return this.blockAmount; }

    @Override
    public EquipSlot getEquipSlot() { return EquipSlot.SHIELD; }

    @Override
    public int getBaseDefense() { return this.baseDef; }
}
