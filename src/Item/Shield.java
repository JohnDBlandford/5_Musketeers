package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class Shield extends Item permits
        WarpedBucklerShield, PatchedWoodShield, ScavengersRoundShield,
        FamineEdgeAegis, WraithboneBulwarkShield {
    private double blockChance;
    private double blockAmount;
    private ItemBuffs bonusDef;
    private ItemBuffs defBoost;
    private ItemBuffs thorns;

    Shield(String itemName, ItemBuffs buffName, Sprite itemLook,
           ItemRarity itemRarity, Map<ItemAbilities, Item> itemAbilities,
           Map<ItemBuffs, Item> itemBuffs, double blockChance,
           double blockAmount, ItemBuffs bonusDef, ItemBuffs defBoost,
           ItemBuffs thorns) {
        super(itemName, buffName, itemLook, itemRarity, itemAbilities,
                itemBuffs);
        this.blockChance = blockChance;
        this.blockAmount = blockAmount;
        this.bonusDef = bonusDef;
        this.defBoost = defBoost;
        this.thorns = thorns;
    }

    public double getBlockChance() { return this.blockChance; }
    public double getBlockAmount() { return this.blockAmount; }
    public ItemBuffs getBonusDef() { return this.bonusDef; }
    public ItemBuffs getDefBoost() { return this.defBoost; }
    public ItemBuffs getThorns() { return this.thorns; }
}
