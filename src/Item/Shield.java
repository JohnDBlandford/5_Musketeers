package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public sealed abstract class Shield extends Item permits
        WarpedBucklerShield, PatchedWoodShield, ScavengersRoundShield, FamineEdgeAegis, WraithboneBulwarkShield {
    protected double blockChance;
    protected double blockAmount;

    Shield(String itemName, SpriteSheet itemLook, ItemRarity itemRarity, Map<ItemAbilities, AbilityType> itemAbilities,
           Map<ItemBuffs, Double> itemBuffs, double blockChance, double blockAmount) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs);
        this.blockChance = blockChance;
        this.blockAmount = blockAmount;
    }

    public double getBlockChance() { return this.blockChance; }
    public double getBlockAmount() { return this.blockAmount; }
}
