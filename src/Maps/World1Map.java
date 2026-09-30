package Maps;

import Level.Map;
import Tilesets.World1Tileset;
import Utils.Point;

public class World1Map extends Map {
    public World1Map() {
        super("world1_map.txt", new World1Tileset());
        this.playerStartPosition = new Point(48, 48);
    }
}