package Item.Registry;

import GameObject.SpriteSheet;
import Item.Armor.World3.*;
import Item.Item;

import Item.RangedWeapon.World3.*;
import Item.Shield.World3.*;
import Item.Sword.World3.*;

import java.util.function.Function;

// NOTE: These are only the Epics and Lengendaries from each world
// it also has no consumables to register
final class World3Items {
    private static final int WORLD_LVL = 3;

    private World3Items() {}

    static void registerAll() {
        // Swords
        addItem(InquisitorsBlade::new);
        addItem(ZealotsFalchion::new);
        addItem(HuntCaptainsLongsword::new);
        addItem(ShepherdsVigil::new);
        addItem(BeastbaneGreatsword::new);

        // Ranged Weapons
        addItem(InquisitorsCrossbow::new);
        addItem(ZealotsLongbow::new);
        addItem(HuntCaptainsArbalest::new);
        addItem(ShepherdsVigilBow::new);
        addItem(BeastbaneBallista::new);

        // Shields
        addItem(InquisitorsKiteShield::new);
        addItem(ZealotsAegis::new);
        addItem(HuntCaptainsBulwark::new);
        addItem(ShepherdsVigilWard::new);
        addItem(BeastbaneBarrier::new);

        // Armor
        addItem(InquisitorsVestmentsArmor::new);
        addItem(ZealotsPlateArmor::new);
        addItem(HuntCaptainsWarplate::new);
        addItem(ShepherdsVigilRaimentArmor::new);
        addItem(BeastbaneAegisPlate::new);

    }

    private static void addItem(Function<SpriteSheet, Item> constructor) {
        ItemRegistry.register(WORLD_LVL, constructor);
    }
}
