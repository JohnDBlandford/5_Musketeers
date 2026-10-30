package Item.Shield.World3;

import Engine.ImageLoader;
import GameObject.SpriteSheet;
import Item.ItemStats;
import Item.Shield.Shield;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class BeastbaneBarrier extends Shield {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.BULWARK_STANCE, 0.00),
            AbilityType.CONTROL, Map.of(ItemAbilities.SHIELD_BASH, 0.00),
            AbilityType.FINISHER, Map.of(ItemAbilities.SHIELD_THROW, 0.0),
            AbilityType.COUNTER, Map.of(ItemAbilities.RETALIATE, 0.00)
    );

    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_DEF, 0.95,
            ItemBuffs.DEF_BOOST, 0.16,
            ItemBuffs.THORNS, 0.42
    );

    public BeastbaneBarrier(SpriteSheet itemLook) {
        super("BeastbaneBarrier", itemLook, ItemRarity.LEGENDARY, ABILITIES, BUFFS, 0.40,
                0.74, 95, ItemStats.getDefaultsForItemRarity(ItemRarity.LEGENDARY));
    }

    @Override
    public SpriteSheet assignSprite() {
        // load the full item sprite sheet
        BufferedImage mstrSheet = ImageLoader.load("Items.png");

        int row = 1;

        // get the subimage (the actual sprite for each item type)
        BufferedImage croppedIcon = mstrSheet.getSubimage(0, row * 32, 32, 32);
        return new SpriteSheet(croppedIcon, 32, 32);
    }
}
