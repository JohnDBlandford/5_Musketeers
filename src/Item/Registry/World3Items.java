package Item.Registry;

import GameObject.SpriteSheet;
import Item.Armor.World3.*;
import Item.Item;
import static Item.Item.ItemRarity.*;

import Item.RangedWeapon.World3.*;
import Item.Shield.World3.*;
import Item.Sword.World3.*;

import java.util.function.Function;

final class World3Items {
    private static final int WORLD_LVL = 3;

    private World3Items() {}

    static void registerAll() {
        // Swords
        addItem("InquisitorsBlade", EPIC, InquisitorsBlade::new);
        addItem("ZealotsFalchion", EPIC, ZealotsFalchion::new);
        addItem("HuntCaptainsLongsword", EPIC, HuntCaptainsLongsword::new);
        addItem("ShepherdsVigil", LEGENDARY, ShepherdsVigil::new);
        addItem("BeastbaneGreatsword", LEGENDARY, BeastbaneGreatsword::new);

        // Ranged Weapons
        addItem("InquisitorsCrossbow", EPIC, InquisitorsCrossbow::new);
        addItem("ZealotsLongbow", EPIC, ZealotsLongbow::new);
        addItem("HuntCaptainsArbalest", EPIC, HuntCaptainsArbalest::new);
        addItem("ShepherdsVigilBow", LEGENDARY, ShepherdsVigilBow::new);
        addItem("BeastbaneBallista", LEGENDARY, BeastbaneBallista::new);

        // Shields
        addItem("InquisitorsKiteShield", EPIC, InquisitorsKiteShield::new);
        addItem("ZealotsAegis", EPIC, ZealotsAegis::new);
        addItem("HuntCaptainsBulwark", EPIC, HuntCaptainsBulwark::new);
        addItem("ShepherdsVigilWard", LEGENDARY, ShepherdsVigilWard::new);
        addItem("BeastbaneBarrier", LEGENDARY, BeastbaneBarrier::new);

        // Armor
        addItem("InquisitorsVestmentsArmor", EPIC, InquisitorsVestmentsArmor::new);
        addItem("ZealotsPlateArmor", EPIC, ZealotsPlateArmor::new);
        addItem("HuntCaptainsWarplate", EPIC, HuntCaptainsWarplate::new);
        addItem("ShepherdsVigilRaimentArmor", LEGENDARY, ShepherdsVigilRaimentArmor::new);
        addItem("BeastbaneAegisPlate", LEGENDARY, BeastbaneAegisPlate::new);

    }

    private static void addItem(String id, Item.ItemRarity rarity, Function<SpriteSheet, Item> constructor) {
        ItemRegistry.register(id, WORLD_LVL, rarity, () -> constructor.apply(null));
    }
}
