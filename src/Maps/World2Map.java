package Maps;

import java.util.ArrayList;

import GameObject.Portal;
import Level.EnhancedMapTile;
import Level.Map;
import Scripts.PortalScript;
import Tilesets.World2Tileset;
import Utils.Point;

public class World2Map extends Map {
    public World2Map() {
        super("world2_map.txt", new World2Tileset());
        this.playerStartPosition = new Point(70 * 48, 48 * 48);
    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();

        Portal portal = new Portal(getMapTile(75, 40).getX(), getMapTile(75, 40).getY(),
                "world1", 3888, 4320, 48, 48);
        portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(portal);

        return enhancedMapTiles;
    }

}
