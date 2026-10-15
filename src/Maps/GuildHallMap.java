package Maps;

import java.util.ArrayList;

import GameObject.Door;

import Level.EnhancedMapTile;
import Level.Map;

import Tilesets.CommonTileset;
import Utils.Point;

public class GuildHallMap extends Map {

    public GuildHallMap() {
        super("guild_hall_map.txt", new CommonTileset());
        this.playerStartPosition = new Point(3360, 2340); // Brackenford plaza

    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();
        return enhancedMapTiles;
    }
}
