package Chest;

import Item.ItemEntry;

import java.util.Locale;
import java.util.Objects;

// Which world (1-4) a chest on this map belongs to. Map names are the ones PlayLevelScreen.createMap uses ("world1",
// "world2_map", ...). The world is found by the "world1" .. "world4" text in the name, ignoring case (the same convention as
// BattleWorld.fromMapName). Unlike BattleWorld, a name with no world in it (the Guild Hall, a typo) throws instead of quietly
// becoming world 1, because a wrong guess would pay the wrong gold and roll the wrong loot.
public final class ChestWorlds {
    private ChestWorlds() {}

    public static int worldOf(String worldName) {
        Objects.requireNonNull(worldName, "worldName");
        String name = worldName.toLowerCase(Locale.ROOT);
        for (int world = ItemEntry.getMinWorld(); world <= ItemEntry.getMaxWorld(); world++) {
            if (name.contains("world" + world)) return world;
        }
        throw new IllegalArgumentException("No world in the map name: '" + worldName + "'");
    }
}
