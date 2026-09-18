package Item;

import java.util.Map;

import GameObject.Sprite;

public final class RustedShortSword extends Sword {
    RustedShortSword(Sprite itemLook,
                     Map<ItemAbilities, Item> itemAbilities,
                     Map<ItemBuffs, Item> itemBuffs) {
        super("RustedShortSword", itemLook,
                ItemRarity.COMMON, itemAbilities, itemBuffs,
                30, ItemBuffs.NONE, ItemBuffs.NONE,
                ItemBuffs.NONE);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0.0;
    }
}
