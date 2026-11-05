package Item.Shield.World2;

import Engine.ImageLoader;
import GameObject.SpriteSheet;
import Item.ItemStats;
import Item.Shield.Shield;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class MilitiaBuckler extends Shield {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.BULWARK_STANCE, 0.00),
            AbilityType.CONTROL, Map.of(ItemAbilities.SHIELD_BASH, 0.00),
            AbilityType.FINISHER, Map.of(ItemAbilities.SHIELD_THROW, 0.0),
            AbilityType.COUNTER, Map.of(ItemAbilities.RETALIATE, 0.00)
    );

    public MilitiaBuckler(SpriteSheet itemLook) {
        super("MilitiaBuckler", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 0.14,
                0.34, 0, ItemStats.getDefaultsForItemRarity(ItemRarity.COMMON));
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
