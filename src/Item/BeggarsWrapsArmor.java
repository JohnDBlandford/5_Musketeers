package Item;

import GameObject.Sprite;

import java.util.Map;

public final class BeggarsWrapsArmor extends Armor {
    BeggarsWrapsArmor(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("BeggarsWrapsArmor", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 8);
    }
}
