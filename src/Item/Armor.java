package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class Armor extends Item permits
        BeggarsWrapsArmor, PatchedLeatherArmor, ScavengersHideArmor,
        FamineEdgeVestmentsArmor, WraithboneMailArmor {
    protected int baseDef;
    protected ItemBuffs bonusMaxHP;
    protected ItemBuffs spdBoost;
    protected ItemBuffs dmgReduction;

    Armor(String itemName, Sprite itemLook, ItemRarity itemRarity,
          Map<ItemAbilities, Item> itemAbilities,
          Map<ItemBuffs, Item> itemBuffs, int baseDef,
          ItemBuffs bonusMaxHP, ItemBuffs spdBoost,
          ItemBuffs dmgReduction) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs);
        this.baseDef = baseDef;
        this.bonusMaxHP = bonusMaxHP;
        this.spdBoost = spdBoost;
        this.dmgReduction = dmgReduction;
    }

    public abstract double getItemBuffModifiers(ItemBuffs buff);

    public int getBaseDef() { return this.baseDef; }
    public ItemBuffs getBonusMaxHP() { return this.bonusMaxHP; }
    public ItemBuffs getSpdBoost() { return this.spdBoost; }
    public ItemBuffs getDmgReduction() { return this.dmgReduction; }
}
