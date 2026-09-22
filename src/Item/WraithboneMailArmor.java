package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WraithboneMailArmor extends Armor {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_MAX_HP, 0.85,
            ItemBuffs.SPD_BOOST, 0.09,
            ItemBuffs.DMG_REDUCTION, 0.05
    );
    WraithboneMailArmor(Sprite itemLook,
                        Map<ItemAbilities, Item> itemAbilities) {
        super("WraithboneMailArmor", itemLook,
                ItemRarity.LEGENDARY, itemAbilities, BUFFS, 28);
    }
}
