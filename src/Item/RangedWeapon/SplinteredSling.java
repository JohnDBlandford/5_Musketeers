package Item.RangedWeapon;

import Engine.ImageLoader;
import GameObject.SpriteSheet;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class SplinteredSling extends RangedWeapon {
    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.WINDBORNE_ARROW, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.PIERCING_SHOT, 0.00),
            AbilityType.MULTI_HIT, Map.of(ItemAbilities.VOLLEY, 0.0),
            AbilityType.DEBUFF, Map.of(ItemAbilities.HUNTERS_MARK, 0.00)
    );

    public SplinteredSling(SpriteSheet itemLook) {
        super("SplinteredSling", itemLook, ItemRarity.COMMON, ABILITIES, Map.of(), 30);
    }

    @Override
    public SpriteSheet assignSprite() {
        // load the full item sprite sheet
        BufferedImage mstrSheet = ImageLoader.load("Items.png");

        int col = 1;

        // get the subimage (the actual sprite for each item type)
        BufferedImage croppedIcon = mstrSheet.getSubimage(col * 32, 0, 32, 32);
        return new SpriteSheet(croppedIcon, 32, 32);
    }
}
