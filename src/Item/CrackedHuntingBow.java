package Item;

import GameObject.Sprite;

import java.util.Map;

public final class CrackedHuntingBow extends RangedWeapon {
    CrackedHuntingBow(Sprite itemLook, Map<ItemAbilities, Item> itemAbilities) {
        super("CrackedHuntingBow", itemLook, ItemRarity.COMMON, itemAbilities, Map.of(), 25);
    }

    @Override
    public double getItemBuffModifiers(ItemBuffs buff) {
        return 0.0;
    }

    @Override
    public int getRangedDmg() { return super.getRangedDmg(); }
}
