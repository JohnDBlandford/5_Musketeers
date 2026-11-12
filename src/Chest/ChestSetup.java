package Chest;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BiPredicate;

// The part of the map wiring that needs no Map (see MapChests for the part that does), so it can be tested on its own.
//  - walkableGrid builds the [row][column] grid ChestPlacer wants from a (column, row) question
//  - ensurePlaced places a map's chests the first time the map is seen and afterwards just returns the stored ones, so
//    rebuilding a map restores the same chests (and an opened chest stays opened). If the placer throws, nothing is registered,
//    so a later load can try again
public final class ChestSetup {
    private ChestSetup() {}

    /**
     * @author Derek
     * @param width the width of the walkable grid
     * @param height the height of the walkable grid
     * @param isWalkable whether the path is free
     * @return returns a grid equal to the width and height
     */
    public static boolean[][] walkableGrid(int width, int height, BiPredicate<Integer, Integer> isWalkable) {
        Objects.requireNonNull(isWalkable, "isWalkable not set");
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("The grid needs at least one tile, but got " + width + " x " + height);
        }
        boolean[][] grid = new boolean[height][width];
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                grid[row][column] = isWalkable.test(column, row);
            }
        }
        return grid;
    }

    /**
     * @author Derek
     * @param registry the chest registry
     * @param mapName the map name where the chest gets placed
     * @param walkable is the path walkable or not
     * @param start the starting tile
     * @param entities the entities around the chest
     * @param random the random seed for the placement algo
     * @return a list of valid chest ids from the registry for the current world
     */
    public static List<ChestId> ensurePlaced(ChestRegistry registry, String mapName, boolean[][] walkable,
                                             ChestPlacer.Tile start, List<ChestPlacer.Tile> entities, Random random) {
        Objects.requireNonNull(registry, "registry not set");
        Objects.requireNonNull(mapName, "mapName not found");
        Objects.requireNonNull(walkable, "walkable not set");
        Objects.requireNonNull(start, "start not set");
        Objects.requireNonNull(entities, "entities not set");
        Objects.requireNonNull(random, "random not set");

        if (registry.isPlaced(mapName)) return registry.getChests(mapName);
        List<ChestId> chests = ChestPlacer.place(mapName, walkable, start, entities, ChestConfig.CHESTS_PER_MAP, random);
        registry.place(mapName, chests);
        return chests;
    }
}
