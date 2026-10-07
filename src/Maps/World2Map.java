package Maps;

import Level.Map;
import Tilesets.World2Tileset;
import Utils.Point;

public class World2Map extends Map {
    public World2Map() {
        super("world2_map.txt", new World2Tileset());
        this.playerStartPosition = new Point(70 * 48, 48 * 48);
    }
}
