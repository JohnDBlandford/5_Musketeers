package Maps;

import java.util.ArrayList;

import GameObject.Door;

import Level.EnhancedMapTile;
import Level.Map;

import Tilesets.CommonTileset;
import Tilesets.TavernTileset;
import Utils.Point;

public class GuildHallMap extends Map {

    public GuildHallMap() {
        super("guild_hall_map.txt", new TavernTileset());
        this.playerStartPosition = new Point(0, 0); //

    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();
        return enhancedMapTiles;
    }
}
