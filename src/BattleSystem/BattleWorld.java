package BattleSystem;

// Which of the 4 animated battle backgrounds to use. The game picks one from the map the player is standing in.
// Each one is drawn from the story of that era, and its ground is built from real tiles of the world's tileset:
//   WORLD_1  The Reckoning (opening, 910 CE)      famine, dried fields, ruined village, crows           (World1_ground_tileset.png)
//   WORLD_2  The Long Erosion (5th-9th century)   a still-green world being cleared, a crack in the sky   (World2Tileset.png)
//   WORLD_3  The Age of Watching & the Betrayal   festival warmth that keeps turning into plague gloom    (World2Tileset.png, until World 3 has its own)
//   WORLD_4  The Reckoning, Full (World 1 again)  the same land burnt black and red under Dabbah's sky    (World1_ground_tileset.png)
public enum BattleWorld {
    WORLD_1("World1_ground_tileset.png"),
    WORLD_2("World2Tileset.png"),
    WORLD_3("World2Tileset.png"),
    WORLD_4("World1_ground_tileset.png");

    private final String tilesetFile;

    BattleWorld(String tilesetFile) {
        this.tilesetFile = tilesetFile;
    }

    public String getTilesetFile() { return tilesetFile; }

    // PlayLevelScreen's createMap() names: "world1", "world2_map", ...
    // World 1 is also the final world, so the final visit needs its own map name containing "world4" (or call startBattle with BattleWorld.WORLD_4 directly).
    public static BattleWorld fromMapName(String mapName) {
        if (mapName == null) return WORLD_1;
        String n = mapName.toLowerCase();
        if (n.contains("world2")) return WORLD_2;
        if (n.contains("world3")) return WORLD_3;
        if (n.contains("world4")) return WORLD_4;
        return WORLD_1;
    }
}