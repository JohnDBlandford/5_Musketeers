package Item;

import GameObject.Sprite;

import java.util.Map;

public final class CrackedHuntingBow extends RangedWeapon {
    CrackedHuntingBow(Sprite itemLook, Map<ItemAbilities, Item> itemAbilities,
                      Map<ItemBuffs, Item> itemBuffs) {
        super("CrackedHuntingBow", itemLook,
                ItemRarity.COMMON, itemAbilities, itemBuffs,
                25, ItemBuffs.NONE, ItemBuffs.NONE,
                ItemBuffs.NONE, ItemBuffs.NONE, ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0.0;
    }
}
