package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class FamineEdgeAegis extends Shield {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_DEF, 0.16,
            ItemBuffs.DEF_BOOST, 0.06
    );

    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.BULWARK_STANCE, AbilityType.BUFF,
            ItemAbilities.SHIELD_BASH, AbilityType.CONTROL,
            ItemAbilities.SHIELD_THROW, AbilityType.CONTROL,
            ItemAbilities.RETALIATE, AbilityType.COUNTER
    );

    FamineEdgeAegis(SpriteSheet itemLook) {
        super("FamineEdgeAegis", itemLook, ItemRarity.EPIC, ABILITIES, BUFFS, 0.20, 0.45);
    }
}
