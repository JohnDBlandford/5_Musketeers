package Item;

import GameObject.Sprite;

import java.util.Map;

public final class ScavengersHideArmor extends Armor {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_MAX_HP, 0.40
    );
    ScavengersHideArmor(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("ScavengersHideArmor", itemLook, ItemRarity.UNCOMMON, itemAbilities, BUFFS, 14);
    }
}
