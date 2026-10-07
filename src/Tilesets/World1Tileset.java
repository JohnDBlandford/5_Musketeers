package Tilesets;

import Builders.FrameBuilder;
import Builders.MapTileBuilder;
import Engine.ImageLoader;
import GameObject.Frame;
import Level.TileType;
import Level.Tileset;
import java.util.ArrayList;

public class World1Tileset extends Tileset {

    public World1Tileset() {
        super(ImageLoader.load("World1_ground_tileset.png"), 16, 16, 3);
    }

    // helper: walkable ground tile from a sheet cell
    private MapTileBuilder ground(int row, int col) {
        Frame f = new FrameBuilder(getSubImage(row, col)).withScale(tileScale).build();
        return new MapTileBuilder(f);
    }

    @Override
    public ArrayList<MapTileBuilder> defineTiles() {
        ArrayList<MapTileBuilder> t = new ArrayList<>();

        // rows 0-6: the 14 speckle tiles, 2 per row
        t.add(ground(0, 0)); // ID 0 - brown base ground
        t.add(ground(0, 1)); // ID 1 - brown base ground, variant
        t.add(ground(1, 0)); // ID 2 - lighter grey-brown dirt
        t.add(ground(1, 1)); // ID 3 - lighter grey-brown dirt, variant
        t.add(ground(2, 0)); // ID 4 - tan dirt with dark debris marks
        t.add(ground(2, 1)); // ID 5 - tan dirt with dark debris marks, variant
        t.add(ground(3, 0)); // ID 6 - dark olive with horizontal streaks
        t.add(ground(3, 1)); // ID 7 - dark olive with horizontal streaks, variant
        t.add(ground(4, 0)); // ID 8 - black scorched ground with red embers
        t.add(ground(4, 1)); // ID 9 - black scorched ground, variant
        t.add(ground(5, 0)); // ID 10 - light grey gravel/stone
        t.add(ground(5, 1)); // ID 11 - light grey gravel/stone, variant
        t.add(ground(6, 0)); // ID 12 - dark grey stone
        t.add(ground(6, 1)); // ID 13 - dark grey stone, variant

        // row 7: grass
        t.add(ground(7, 0)); // ID 14 - dark green grass
        t.add(ground(7, 1)); // ID 15 - dark green grass, variant
        t.add(ground(7, 2)); // ID 16 - dark green grass, variant

        // rows 8-9: brown earth with olive-green veins (4x2 block)
        t.add(ground(8, 0)); // ID 17
        t.add(ground(8, 1)); // ID 18
        t.add(ground(8, 2)); // ID 19
        t.add(ground(8, 3)); // ID 20
        t.add(ground(9, 0)); // ID 21
        t.add(ground(9, 1)); // ID 22
        t.add(ground(9, 2)); // ID 23
        t.add(ground(9, 3)); // ID 24

        // ---- building tiles (atlas rows 10+), each block is added row-major so ID =
        // base + y * w + x ----
        addBlock(t, 10, 6, 6); // IDs 25-60 timber house (6x6)
        addBlock(t, 16, 4, 6); // IDs 61-84 small timber house (4x6)
        addBlock(t, 22, 14, 6); // IDs 85-168 Brackenford hall (14x6)
        addBlock(t, 28, 10, 6); // IDs 169-228 Granary Cloister hall (10x6)
        addBlock(t, 34, 6, 4); // IDs 229-252 Dabbah's Hollow gate (6x4)
        addBlock(t, 38, 2, 2); // IDs 253-256 Barrons hut (2x2)

        return t;
    }

    // adds every tile of a w x h block that starts at the given sheet row (column
    // 0), row-major
    private void addBlock(ArrayList<MapTileBuilder> t, int startRow, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                t.add(ground(startRow + y, x).withTileType(TileType.NOT_PASSABLE));
            }
        }
    }
}