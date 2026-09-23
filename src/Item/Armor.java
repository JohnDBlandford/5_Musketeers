package Item;

import GameObject.Sprite;
import GameObject.SpriteSheet;

import java.util.Map;

public sealed abstract class Armor extends Item permits
        BeggarsWrapsArmor, PatchedLeatherArmor, ScavengersHideArmor, FamineEdgeVestmentsArmor, WraithboneMailArmor {
    protected int baseDef;
    Armor(String itemName, SpriteSheet itemLook, ItemRarity itemRarity,
          Map<ItemAbilities, AbilityType> itemAbilities, Map<ItemBuffs, Double> itemBuffs, int baseDef) {
        super(itemName, itemLook, itemRarity, itemAbilities, itemBuffs);
        this.baseDef = baseDef;
    }

    public int getBaseDef() { return this.baseDef; }
}
