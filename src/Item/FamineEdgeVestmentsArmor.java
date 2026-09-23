package Item;

import GameObject.Sprite;

import java.util.Map;

public final class FamineEdgeVestmentsArmor extends Armor {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_MAX_HP, 0.60,
            ItemBuffs.SPD_BOOST, 0.06
    );
    FamineEdgeVestmentsArmor(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("FamineEdgeVestmentsArmor", itemLook, ItemRarity.EPIC, itemAbilities, BUFFS, 20);
    }
}
