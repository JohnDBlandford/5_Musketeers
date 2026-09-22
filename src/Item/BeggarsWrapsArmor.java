package Item;

import GameObject.Sprite;

import java.util.Map;

public final class BeggarsWrapsArmor extends Armor {
    BeggarsWrapsArmor(Sprite itemLook, Map<ItemAbilities, Item> itemAbilities) {
        super("BeggarsWrapsArmor", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 8);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0;
    }

    @Override
    public int getBaseDef() { return super.getBaseDef(); }
}
