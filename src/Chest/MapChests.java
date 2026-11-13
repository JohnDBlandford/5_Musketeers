package Chest;

import EnhancedMapTiles.ChestTile;
import Level.Map;
import Level.MapEntity;
import Level.MapTile;
import Level.TileType;
import Utils.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// The part of the chest wiring that needs a real Map (ChestSetup is the part that does not). Call install() on a freshly built
// map BEFORE map.preloadScripts(), so the chests' scripts get initialized with everything else.
//  - maps with no world in their name (the Guild Hall, test and blank maps) get no chests
//  - walkable = the tile's type is PASSABLE. The flood fill starts at the map's own player start tile, so a fast travel into
//    a sealed pocket on the first visit cannot decide where the chests go for the whole game
//  - doors, portals, signs, NPCs and triggers are the entities the chests keep their distance from
//  - the first visit places the chests and stores their spots in the registry, later visits rebuild the same ones. A chest that
//    was opened is still drawn (there is no open sprite yet) and says "The chest is empty."
//  - if placing fails (it should never happen on a real map) the problem is printed to System.err and the map gets no chests
//    this time. Nothing is stored, so the next visit tries again
public final class MapChests {
    private MapChests() {}

    /**
     * @author Derek
     * @param map the map object that the chest is associated with
     * @param mapName the map name that the chest is in
     * @param registry the chest registry
     * @param random the random seed for placement
     */
    public static void install(Map map, String mapName, ChestRegistry registry, Random random) {
        if (!ChestWorlds.hasWorld(mapName)) return;

        Point startIndex = map.getTileIndexByPosition(map.getPlayerStartPosition().x, map.getPlayerStartPosition().y);
        ChestPlacer.Tile start = new ChestPlacer.Tile((int) startIndex.x, (int) startIndex.y);
        boolean[][] walkable = ChestSetup.walkableGrid(map.getWidth(), map.getHeight(),
                (column, row) -> map.getMapTile(column, row).getTileType() == TileType.PASSABLE);

        List<MapEntity> entities = new ArrayList<>();
        entities.addAll(map.getEnhancedMapTiles());
        entities.addAll(map.getNPCs());
        entities.addAll(map.getTriggers());
        List<ChestPlacer.Tile> entityTiles = new ArrayList<>();
        for (MapEntity entity : entities) {
            Point index = map.getTileIndexByPosition(entity.getX(), entity.getY());
            entityTiles.add(new ChestPlacer.Tile((int) index.x, (int) index.y));
        }

        List<ChestId> chests;
        try {
            chests = ChestSetup.ensurePlaced(registry, mapName, walkable, start, entityTiles, random);
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.err.println("MapChests: no chests on " + mapName + " this time: " + e.getMessage());
            return;
        }

        for (ChestId id : chests) {
            MapTile tile = map.getMapTile(id.column(), id.row());
            map.addEnhancedMapTile(new ChestTile(tile.getLocation(), id));
        }
    }
}
