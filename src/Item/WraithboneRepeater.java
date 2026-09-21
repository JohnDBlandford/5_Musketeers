package Item;

import GameObject.Sprite;

import java.util.Map;

public final class WraithboneRepeater extends RangedWeapon {
    WraithboneRepeater(Sprite itemLooks,
                       Map<ItemAbilities, Item> itemAbilities,
                       Map<ItemBuffs, Item> itemBuffs) {
        super("WraithboneRepeater", itemLooks,
                ItemRarity.LEGENDARY, itemAbilities, itemBuffs,
                75, ItemBuffs.CRIT_CHANCE, ItemBuffs.NONE,
                ItemBuffs.NONE, ItemBuffs.ACCURACY, ItemBuffs.PIERCING);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return switch (buff) {
            case ACCURACY -> 0.10;
            case CRIT_CHANCE, PIERCING -> 0.08;
            default -> 0.0;
        };
    }
}
