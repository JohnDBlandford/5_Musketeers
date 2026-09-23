package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WraithboneBulwarkShield extends Shield {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_DEF, 0.22,
            ItemBuffs.DEF_BOOST, 0.09,
            ItemBuffs.THORNS, 0.12
    );
    WraithboneBulwarkShield(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("WraithboneBulwarkShield", itemLook, ItemRarity.LEGENDARY, itemAbilities, BUFFS, 0.25, 0.55);
    }
}
