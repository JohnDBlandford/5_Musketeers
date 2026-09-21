package Item;

import GameObject.Sprite;

import java.util.Map;

public final class FamineEdgeLongbow extends RangedWeapon {
    FamineEdgeLongbow(Sprite itemLook,
                      Map<ItemAbilities, Item> itemAbilities,
                      Map<ItemBuffs, Item> itemBuffs) {
        super("FamineEdgeLongbow", itemLook, ItemRarity.EPIC,
                itemAbilities, itemBuffs, 55,
                ItemBuffs.CRIT_CHANCE, ItemBuffs.NONE, ItemBuffs.NONE,
                ItemBuffs.ACCURACY, ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return switch (buff) {
            case ACCURACY -> 0.08;
            case CRIT_CHANCE -> 0.06;
            default -> 0.0;
        };
    }
}
