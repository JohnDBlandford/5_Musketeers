package Item;

import GameObject.Sprite;

import java.util.Map;

public sealed abstract class Armor extends Item permits
        BeggarsWrapsArmor, PatchedLeatherArmor, ScavengersHideArmor,
        FamineEdgeVestmentsArmor, WraithboneMailArmor {
    private ItemBuffs bonusDef;
    private ItemBuffs bonusMaxHP;
    private ItemBuffs spdBoost;
    private ItemBuffs dmgReduction;

    Armor(String itemName, Sprite itemLook,
          ItemRarity itemRarity, Map<ItemAbilities, Item> itemAbilities,
          Map<ItemBuffs, Item> itemBuffs, ItemBuffs bonusDef,
          ItemBuffs bonusMaxHP, ItemBuffs spdBoost,
          ItemBuffs dmgReduction) {
        super(itemName, itemLook, itemRarity, itemAbilities,
                itemBuffs);
        this.bonusDef = bonusDef;
        this.bonusMaxHP = bonusMaxHP;
        this.spdBoost = spdBoost;
        this.dmgReduction = dmgReduction;
    }

    public ItemBuffs getBonusDef() { return this.bonusDef; }
    public ItemBuffs getBonusMaxHP() { return this.bonusMaxHP; }
    public ItemBuffs getSpdBoost() { return this.spdBoost; }
    public ItemBuffs getDmgReduction() { return this.dmgReduction; }
}
