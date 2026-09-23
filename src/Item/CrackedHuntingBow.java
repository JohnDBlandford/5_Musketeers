package Item;

import GameObject.Sprite;

import java.util.Map;

public final class CrackedHuntingBow extends RangedWeapon {
    private static final Map<ItemAbilities, AbilityType> ABILITIES = Map.of(
            ItemAbilities.WINDBORNE_ARROW, AbilityType.BUFF,
            ItemAbilities.PIERCING_SHOT, AbilityType.BURST,
            ItemAbilities.VOLLEY, AbilityType.MULTI_HIT,
            ItemAbilities.HUNTERS_MARK, AbilityType.DEBUFF
    );

    CrackedHuntingBow(Sprite itemLook) {
        super("CrackedHuntingBow", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 25);
    }
}
