package Locations;

import Level.Map;
import Level.MapTile;
import Level.TileType;

import java.util.ArrayList;
import java.util.List;

// The named places of the game. PlayLevelScreen uses this for:
//   1. remembering the last named place the knight walked into (the game-over screen sends him back there),
//   2. DISCOVERY: walking into a place for the first time unlocks it as a waypoint; press M to fast-travel between discovered places,
//   3. the place-name banner,
//   4. random encounters: a SAFE place (Brackenford) has none.
//
// Every place has a FIXED tile: that is where the knight wakes up and where a waypoint warp puts him (if that exact tile is
// blocked he is put on the nearest walkable one). Brackenford's tile is real; the others are estimated from the world-map picture,
// so check them: stand on the spot you want and read the tile from the knight's position (pixels / 48), then edit the numbers.
//   place(name, map, tileX, tileY, radiusInTiles, safe)   radius = how close counts as "in" the place (nearest centre wins)
public final class NamedLocations {
    public static final int TILE_PX = 48;   // 16px tiles x scale 3

    public static final class Place {
        public final String name, map;
        public final int tileX, tileY;     // the fixed spawn / waypoint tile (-1 = the map's normal start position)
        public final int radius;           // tiles
        public final boolean safe;         // no random encounters
        Place(String name, String map, int tileX, int tileY, int radius, boolean safe) {
            this.name = name; this.map = map; this.tileX = tileX; this.tileY = tileY; this.radius = radius; this.safe = safe;
        }
        public boolean wholeMap() { return radius == Integer.MAX_VALUE; }
        public String world() { return map.contains("2") ? "WORLD 2" : (map.contains("3") ? "WORLD 3" : "WORLD 1"); }
    }

    private static final List<Place> PLACES = new ArrayList<>();

    static {
        // Read from your real map files (world1_map.txt 160x146, world2_map.txt 132x124): each tile is a walkable tile at the
        // middle of the place, on its road / plaza / clearing, found by scanning the map's tile ids.
        // ---- World 1 ----
        place("Brackenford", "world1", 81, 90, 14, true);               // the town square: SAFE
        place("Ashwood", "world1", 30, 46, 12, false);                  // the walled stone yard, top left
        place("Kept Grove", "world1", 24, 87, 12, false);               // the cottage on the west road
        place("Barrows", "world1", 140, 28, 14, false);                 // the scattered huts, top right
        place("Granary Cloister", "world1", 140, 88, 14, false);        // the hall at the east end of the road
        place("Dry River", "world1", 143, 126, 12, false);              // the sand bank, bottom right
        place("Barren Fields", "world1", 76, 128, 24, false);           // the blighted field south of the plaza
        // ---- World 2 ---- (names from the design-doc picture)
        place("Elderemere", "world2_map", 70, 46, 14, false);           // the village plaza (where you arrive)
        place("Old Wood & Kept Circle", "world2_map", 22, 40, 22, false);
        place("Woodcutters' Clearing", "world2_map", 50, 40, 10, false);
        place("Baron's Hold", "world2_map", 103, 23, 22, false);        // the moated compound, top right
        place("Minster & Scriptorium", "world2_map", 98, 50, 14, false); // the white paved cloister with the crosses
        place("Marsh & Drainage", "world2_map", 90, 72, 30, false);      // the flooded lake and the wheat channels
        place("The Fracture", "world2_map", 72, 110, 16, false);         // the scorched ruin at the bottom
    }

    // ---- no-encounter zones: tile rectangles {x1, y1, x2, y2}. Everything else (roads included) can have random encounters.
    private static final java.util.Map<String, int[][]> SAFE_ZONES = new java.util.HashMap<>();
    static {
        // Brackenford's plaza only: the buildings and the square around the knight's start (x 69-91, y 76-93 in your map)
        SAFE_ZONES.put("world1", new int[][]{{68, 77, 92, 94}});
    }

    // true if this pixel position is inside a no-encounter zone
    public static boolean isSafe(String mapName, float px, float py) {
        int[][] zones = SAFE_ZONES.get(mapName);
        if (zones == null) return false;
        double tx = px / TILE_PX, ty = py / TILE_PX;
        for (int[] z : zones) if (tx >= z[0] && tx <= z[2] + 1 && ty >= z[1] && ty <= z[3] + 1) return true;
        return false;
    }

    private static void place(String n, String m, int tx, int ty, int r, boolean safe) { PLACES.add(new Place(n, m, tx, ty, r, safe)); }

    private NamedLocations() { }

    public static List<Place> all() { return PLACES; }

    // the named place the given pixel position is in (null if none)
    public static Place find(String mapName, float px, float py) {
        Place best = null;
        double bestD = Double.MAX_VALUE;
        for (Place p : PLACES) {
            if (!p.map.equals(mapName)) continue;
            double d;
            if (p.wholeMap()) d = 1e9;
            else {
                double dx = px / TILE_PX - p.tileX, dy = py / TILE_PX - p.tileY;
                d = Math.sqrt(dx * dx + dy * dy);
                if (d > p.radius) continue;
            }
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    // a walkable tile at or next to the place's tile, as pixel coordinates {x, y}
    public static float[] spawnPixels(Place p, Map map) {
        if (p.wholeMap()) return new float[]{map.getPlayerStartPosition().x, map.getPlayerStartPosition().y};
        for (int r = 0; r <= 8; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    MapTile t = map.getMapTile(p.tileX + dx, p.tileY + dy);
                    if (t != null && t.getTileType() != TileType.NOT_PASSABLE)
                        return new float[]{t.getX(), t.getY()};
                }
        return new float[]{p.tileX * TILE_PX, p.tileY * TILE_PX};
    }
}