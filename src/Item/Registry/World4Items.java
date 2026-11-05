package Item.Registry;

import GameObject.SpriteSheet;
import Item.Armor.World4.*;
import Item.Item;
import Item.RangedWeapon.World4.*;
import Item.Shield.World4.*;
import Item.Sword.World4.*;

import java.util.function.Function;

final class World4Items {
    private static final int WORLD_LVL = 4;

    private World4Items() {}

    static void registerAll() {
        // Swords
        addItem(CrusadersLongsword::new);
        addItem(BeastSlayerGreatsword::new);
        addItem(MarkedReckonersBlade::new);
        addItem(DabbahsJudgment::new);
        addItem(WitnesssRequiem::new);

        // Ranged Weapons
        addItem(CrusadersCrossbow::new);
        addItem(BeastSlayerLongbow::new);
        addItem(MarkedReckonersBow::new);
        addItem(DabbahsJudgmentBow::new);
        addItem(WitnesssRequiemRepeater::new);

        // Shields
        addItem(CrusadersBulwarkShield::new);
        addItem(BeastSlayerAegis::new);
        addItem(MarkedReckonersWard::new);
        addItem(DabbahsJudgmentBarrier::new);
        addItem(WitnesssRequiemShield::new);

        // Armor
        addItem(CrusadersPlateArmor::new);
        addItem(BeastSlayerWarplate::new);
        addItem(MarkedReckonersRaiment::new);
        addItem(DabbahsJudgmentAegisPlate::new);
        addItem(WitnesssRequiemMailArmor::new);
    }

    private static void addItem(Function<SpriteSheet, Item> constructor) {
        ItemRegistry.register(WORLD_LVL, constructor);
    }
}
