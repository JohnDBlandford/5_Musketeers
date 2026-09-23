package Item;

import GameObject.Sprite;

import java.util.Map;

public final class FamineEdgeBlade extends Sword {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.CRIT_CHANCE, 0.08,
            ItemBuffs.ATK_BOOST, 0.10
    );
    FamineEdgeBlade(Sprite itemLook, Map<ItemAbilities, AbilityType> itemAbilities) {
        super("FamineEdgeBlade", itemLook, ItemRarity.EPIC, itemAbilities, BUFFS, 60);
    }

    @Override
    public int getPhysicalAtkDmg() { return super.getPhysicalAtkDmg(); }
}
