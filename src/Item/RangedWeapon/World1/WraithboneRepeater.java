package Item.RangedWeapon.World1;

import Engine.ImageLoader;
import GameObject.SpriteSheet;
import Item.ItemStats;
import Item.RangedWeapon.RangedWeapon;

import java.awt.image.BufferedImage;
import java.util.Map;

public final class WraithboneRepeater extends RangedWeapon {
    private static final Map<ItemBuffs, Double> BUFFS = Map.of(
            ItemBuffs.ACCURACY, 0.10,
            ItemBuffs.CRIT_CHANCE, 0.08,
            ItemBuffs.PIERCING, 0.08
    );

    private static final Map<AbilityType, Map<ItemAbilities, Double>> ABILITIES = Map.of(
            AbilityType.BUFF, Map.of(ItemAbilities.WINDBORNE_ARROW, 0.00),
            AbilityType.BURST, Map.of(ItemAbilities.PIERCING_SHOT, 0.00),
            AbilityType.MULTI_HIT, Map.of(ItemAbilities.VOLLEY, 0.0),
            AbilityType.DEBUFF, Map.of(ItemAbilities.HUNTERS_MARK, 0.00)
    );

    public WraithboneRepeater(SpriteSheet itemLooks) {
        super("WraithboneRepeater", itemLooks, ItemRarity.LEGENDARY, ABILITIES, BUFFS, 75,
                ItemStats.getDefaultsForItemRarity(ItemRarity.LEGENDARY));
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
