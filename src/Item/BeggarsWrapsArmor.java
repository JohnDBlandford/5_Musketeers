package Item;

import GameObject.SpriteSheet;

import java.util.Map;

public final class BeggarsWrapsArmor extends Armor {
    BeggarsWrapsArmor(SpriteSheet itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("BeggarsWrapsArmor", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 8);
    }
}
