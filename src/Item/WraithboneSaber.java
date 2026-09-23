package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class WraithboneSaber extends Sword {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.CRIT_CHANCE, 0.10,
            ItemBuffs.ATK_BOOST, 0.14,
            ItemBuffs.CRIT_DMG, 0.25
    );

    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.GALE_SLASH, AbilityType.BUFF,
            ItemAbilities.HEAVY_CLEAVE, AbilityType.BURST,
            ItemAbilities.BLOODFANG_STRIKE, AbilityType.SUSTAIN,
            ItemAbilities.RECKONING_BLOW, AbilityType.FINISHER
    );

    public WraithboneSaber(SpriteSheet itemLook) {
        super("WraithboneSaber", itemLook, ItemRarity.LEGENDARY, ABILITIES, BUFFS, 80);
    }
}
