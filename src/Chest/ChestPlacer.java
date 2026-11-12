package Chest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

// Picks where a map's chests go, once per game (ChestRegistry remembers the answer). A pure function over a grid of walkable
// tiles, so it knows nothing about Map or the engine and can be tested with hand made grids.
//
// A tile can hold a chest only if ALL of these are true:
//  - it can be reached on foot from the player's start (4 way flood fill over walkable tiles)
//  - the tile and all 8 tiles around it are walkable. A chest is solid, so this is what stops it from plugging a doorway or
//    a 1 wide corridor (with the whole ring around it open, the chest can always be walked around), and it keeps chests off
//    the map edge
//  - it is at least MIN_ENTITY_SPACING tiles from the player's start and from every entity (door, portal, sign, NPC).
//    Distances are Chebyshev (the larger of the column and row difference), so a diagonal counts the same as a straight line
// The valid spots are shuffled with the given Random and taken one by one, skipping any that is closer than
// MIN_CHEST_SPACING to a chest already taken, until there are `count` chests or the spots run out. Running out of room
// returns fewer chests.
// If only 1 or 0 valid spots exist (counted before the chest to chest spacing) the map is almost certainly wrong, such as a bad
// grid or a start tile in a sealed room, so it throws instead of quietly giving the map one chest or none.
public final class ChestPlacer {
    private ChestPlacer() {}

    // a tile of the grid. column is the x index and row the y index, like the rest of the map code
    public record Tile(int column, int row) {}

    private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    // walkable is indexed [row][column] and must be rectangular. start is the player's start tile and must be walkable.
    // entities are the tiles of doors, portals, signs and NPCs. Nothing passed in is changed

    /**
     * @author Derek
     * @param mapName the map name
     * @param walkable if the tile is walkable or not
     * @param start the starting tile where the chest is placed
     * @param entities the potential entities around the chest (needed to determine whether the chest can be placed around it or not)
     * @param count the count of the chests in the world (must be at least 1)
     * @param random the random seed for chest world placement
     * @return returns an unmodifiable Collection list of the chests placed in the world
     */
    public static List<ChestId> place(String mapName, boolean[][] walkable, Tile start, List<Tile> entities, int count, Random random) {
        Objects.requireNonNull(walkable, "walkable not set");
        Objects.requireNonNull(start, "start not set");
        Objects.requireNonNull(entities, "entities not set");
        Objects.requireNonNull(random, "random not set");
        if (mapName == null || mapName.isBlank()) throw new IllegalArgumentException("Map name cannot be blank");
        if (count < 1) throw new IllegalArgumentException("Chest count must be at least 1, but got: " + count);
        requireValidGrid(walkable, start);

        boolean[][] reachable = findReachable(walkable, start);
        List<Tile> spots = findValidSpots(walkable, reachable, start, entities);
        if (spots.size() <= 1) {
            throw new IllegalStateException("Only " + spots.size() + " valid chest spot(s) on " + mapName
                    + ", so the grid or the start tile is probably wrong");
        }

        Collections.shuffle(spots, random);
        List<ChestId> chests = new ArrayList<>();
        for (Tile spot : spots) {
            if (chests.size() == count) break;
            if (isFarFromChests(spot, chests)) chests.add(new ChestId(mapName, spot.column(), spot.row()));
        }
        return Collections.unmodifiableList(chests);
    }

    /**
     * @author Derek
     * @param walkable is the tiles in the grid walkable or not
     * @param start the starting tile in the grid
     */
    private static void requireValidGrid(boolean[][] walkable, Tile start) {
        Objects.requireNonNull(walkable, "array not found");
        if (walkable.length == 0 || walkable[0] == null || walkable[0].length == 0) {
            throw new IllegalArgumentException("The grid cannot be empty");
        }
        int columns = walkable[0].length;
        for (boolean[] row : walkable) {
            if (row == null || row.length != columns) throw new IllegalArgumentException("Every row of the grid must be the same length");
        }
        boolean inside = start.row() >= 0 && start.row() < walkable.length && start.column() >= 0 && start.column() < columns;
        if (!inside) throw new IllegalArgumentException("The start " + start + " is outside the grid");
        if (!walkable[start.row()][start.column()]) throw new IllegalArgumentException("The start " + start + " is on a wall");
    }

    // every tile that can be walked to from the start. the 4 way flood fill is what keeps chests out of sealed rooms and out
    // of areas on the other side of water or walls

    /**
     * @author Derek
     * @param walkable are all of the tiles walkable or not
     * @param start the starting tile
     * @return returns true if the walkable length is all clear
     */
    private static boolean[][] findReachable(boolean[][] walkable, Tile start) {
        Objects.requireNonNull(walkable, "array not found");
        Objects.requireNonNull(start, "tile not found");
        boolean[][] reachable = new boolean[walkable.length][walkable[0].length];
        ArrayDeque<Tile> queue = new ArrayDeque<>();
        reachable[start.row()][start.column()] = true;
        queue.add(start);
        while (!queue.isEmpty()) {
            Tile at = queue.poll();
            for (int[] step : STEPS) {
                int column = at.column() + step[0];
                int row = at.row() + step[1];
                if (row < 0 || row >= walkable.length || column < 0 || column >= walkable[0].length) continue;
                if (!walkable[row][column] || reachable[row][column]) continue;
                reachable[row][column] = true;
                queue.add(new Tile(column, row));
            }
        }
        return reachable;
    }

    // row 0 and the last row (and the same for columns) are skipped: a tile on the edge has no full 3x3 block

    /**
     * @author Derek
     * @param walkable is the spot walkable
     * @param reachable is the spot reachable
     * @param start the starting tile
     * @param entities the list of entities in the area
     * @return returns all of the valid tile spots where a chest can be placed
     */
    private static List<Tile> findValidSpots(boolean[][] walkable, boolean[][] reachable, Tile start, List<Tile> entities) {
        Objects.requireNonNull(walkable, "array not found");
        List<Tile> spots = new ArrayList<>();
        for (int row = 1; row < walkable.length - 1; row++) {
            for (int column = 1; column < walkable[0].length - 1; column++) {
                if (!reachable[row][column] || !isBlockWalkable(walkable, column, row)) continue;
                Tile spot = new Tile(column, row);
                if (distance(spot, start) < ChestConfig.MIN_ENTITY_SPACING) continue;
                if (isNearAny(spot, entities)) continue;
                spots.add(spot);
            }
        }
        return spots;
    }

    // returns true if the block is walkable

    /**
     * @author Derek
     * @param walkable is the block walkable
     * @param column the column of the block
     * @param row the row of the block
     * @return returns true if the entire block is walkable
     */
    private static boolean isBlockWalkable(boolean[][] walkable, int column, int row) {
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                if (!walkable[row + rowOffset][column + columnOffset]) return false;
            }
        }
        return true;
    }

    // checks if the chests are near any other entities

    /**
     * @author Derek
     * @param spot the tile spot of the placed chest
     * @param entities the entities around the chest
     * @return returns false if the chest is too close to other entities
     */
    private static boolean isNearAny(Tile spot, List<Tile> entities) {
        Objects.requireNonNull(entities, "entities list not found");
        for (Tile entity : entities) {
            if (distance(spot, entity) < ChestConfig.MIN_ENTITY_SPACING) return true;
        }
        return false;
    }

    /**
     * @author Derek
     * @param spot the tile spot for the other entity
     * @param chests the list of where the chests are
     * @return returns false if the min distance between chests and entities is less than the value
     */
    private static boolean isFarFromChests(Tile spot, List<ChestId> chests) {
        Objects.requireNonNull(chests, "chest list not found");
        for (ChestId chest : chests) {
            if (distance(spot, new Tile(chest.column(), chest.row())) < ChestConfig.MIN_CHEST_SPACING) return false;
        }
        return true;
    }

    // Chebyshev distance: a diagonal step counts as 1, the same as a straight one

    /**
     * @author Derek
     * @param a the first tile
     * @param b the second tile
     * @return returns the distance between two tiles, diagonal tiles still count as one tile apart
     */
    private static int distance(Tile a, Tile b) {
        Objects.requireNonNull(a, "first tile cannot be found");
        Objects.requireNonNull(b, "second tile cannot be found");
        return Math.max(Math.abs(a.column() - b.column()), Math.abs(a.row() - b.row()));
    }
}
