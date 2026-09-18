package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class Shield extends Item permits
        WarpedBucklerShield, PatchedWoodShield, ScavengersRoundShield,
        FamineEdgeAegis, WraithboneBulwarkShield {
    protected double blockChance;
    protected double blockAmount;
    protected ItemBuffs buff1;
    protected ItemBuffs buff2;
    protected ItemBuffs buff3;

    Shield(String itemName, Sprite itemLook, ItemRarity itemRarity,
           Map<ItemAbilities, Item> itemAbilities,
           Map<ItemBuffs, Item> itemBuffs, double blockChance,
           double blockAmount, ItemBuffs bonusDef, ItemBuffs defBoost,
           ItemBuffs thorns) {
        super(itemName, itemLook, itemRarity, itemAbilities,
                itemBuffs);
        this.blockChance = blockChance;
        this.blockAmount = blockAmount;
        this.buff1 = bonusDef;
        this.buff2 = defBoost;
        this.buff3 = thorns;
    }

    public abstract double getItemBuffModifiers(ItemBuffs buff);

    public double getBlockChance() { return this.blockChance; }
    public double getBlockAmount() { return this.blockAmount; }
    public ItemBuffs getBonusDef() { return this.buff1; }
    public ItemBuffs getDefBoost() { return this.buff2; }
    public ItemBuffs getThornsDmg() { return this.buff3; }
}
