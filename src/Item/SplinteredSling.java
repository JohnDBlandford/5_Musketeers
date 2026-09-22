package Item;

import GameObject.Sprite;

import java.util.Map;

public final class SplinteredSling extends RangedWeapon {
    SplinteredSling(Sprite itemLook, Map<ItemAbilities, Item> itemAbilities) {
        super("SplinteredSling", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 30);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0.0;
    }

    @Override
    public int getRangedDmg() { return super.getRangedDmg(); }
}
