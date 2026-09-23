package Item;

import GameObject.Sprite;

import java.util.Map;

public final class FamineEdgeBlade extends Sword {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.CRIT_CHANCE, 0.08,
            ItemBuffs.ATK_BOOST, 0.10
    );

    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.GALE_SLASH, AbilityType.BUFF,
            ItemAbilities.HEAVY_CLEAVE, AbilityType.BURST,
            ItemAbilities.BLOODFANG_STRIKE, AbilityType.SUSTAIN,
            ItemAbilities.RECKONING_BLOW, AbilityType.FINISHER
    );

    FamineEdgeBlade(Sprite itemLook) {
        super("FamineEdgeBlade", itemLook, ItemRarity.EPIC, ABILITIES, BUFFS, 60);
    }
}
