package Item;

import java.util.Map;

import GameObject.Sprite;

public final class RustedShortSword extends Sword {
    RustedShortSword(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("RustedShortSword", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 30);
    }
}
