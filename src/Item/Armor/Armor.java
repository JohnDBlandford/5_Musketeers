package Item.Armor;

import GameObject.SpriteSheet;
import Item.Item;
import inventory.EquipSlot;

import java.util.Map;

public sealed abstract class Armor extends Item permits BeggarsWrapsArmor, FamineEdgeVestmentsArmor,
        PatchedLeatherArmor, ScavengersHideArmor, WraithboneMailArmor{
    protected int baseDef;
    Armor(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
          Map<AbilityType, Map<ItemAbilities, Double>> itemAbilities, Map<ItemBuffs, Double> itemBuffs, int baseDef) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs);
        this.baseDef = baseDef;
    }

    public int getBaseDef() { return this.baseDef; }

    @Override
    public EquipSlot getEquipSlot() { return EquipSlot.ARMOR; }
}
