package Item.Shield.World4;

import Engine.ImageLoader;
import GameObject.SpriteSheet;
import Item.ItemStats;
import Item.Shield.Shield;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class BeastSlayerAegis extends Shield {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.BULWARK_STANCE, 0.00),
            AbilityType.CONTROL, Map.of(ItemAbilities.SHIELD_BASH, 0.00),
            AbilityType.FINISHER, Map.of(ItemAbilities.SHIELD_THROW, 0.0),
            AbilityType.COUNTER, Map.of(ItemAbilities.RETALIATE, 0.00)
    );

    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.BONUS_DEF, 1.25,
            ItemBuffs.DEF_BOOST, 0.14
    );

    public BeastSlayerAegis(SpriteSheet itemLook) {
        super("BeastSlayerAegis", itemLook, ItemRarity.EPIC, ABILITIES, BUFFS, 0.34,
                0.65, 125, ItemStats.getDefaultsForItemRarity(ItemRarity.EPIC));
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
