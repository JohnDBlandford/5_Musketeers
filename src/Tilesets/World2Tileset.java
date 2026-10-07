package Tilesets;

import Builders.FrameBuilder;
import Builders.MapTileBuilder;
import Engine.ImageLoader;
import GameObject.Frame;
import Level.TileType;
import Level.Tileset;

import java.util.ArrayList;

public class World2Tileset extends Tileset {

    public World2Tileset() {
        super(ImageLoader.load("World2Tileset.png"), 16, 16, 3);
    }

    private MapTileBuilder tile(int row, int column, TileType type) {
        Frame frame = new FrameBuilder(getSubImage(row, column))
                .withScale(tileScale)
                .build();
        return new MapTileBuilder(frame).withTileType(type);
    }

    @Override
    public ArrayList<MapTileBuilder> defineTiles() {
        ArrayList<MapTileBuilder> mapTiles = new ArrayList<>();

        // 0: grass_a
        mapTiles.add(tile(0, 0, TileType.PASSABLE));

        // 1: grass_b
        mapTiles.add(tile(0, 1, TileType.PASSABLE));

        // 2: grass_c
        mapTiles.add(tile(0, 2, TileType.PASSABLE));

        // 3: grass_tufts
        mapTiles.add(tile(0, 3, TileType.PASSABLE));

        // 4: grass_light
        mapTiles.add(tile(0, 4, TileType.PASSABLE));

        // 5: grass_light_flowers
        mapTiles.add(tile(0, 5, TileType.PASSABLE));

        // 6: dirt_a
        mapTiles.add(tile(0, 6, TileType.PASSABLE));

        // 7: dirt_b
        mapTiles.add(tile(0, 7, TileType.PASSABLE));

        // 8: plaza_dirt
        mapTiles.add(tile(0, 8, TileType.PASSABLE));

        // 9: paving
        mapTiles.add(tile(0, 9, TileType.PASSABLE));

        // 10: cracked_dirt
        mapTiles.add(tile(1, 0, TileType.PASSABLE));

        // 11: wheat_a
        mapTiles.add(tile(1, 1, TileType.PASSABLE));

        // 12: wheat_b
        mapTiles.add(tile(1, 2, TileType.PASSABLE));

        // 13: wheat_channel
        mapTiles.add(tile(1, 3, TileType.NOT_PASSABLE));

        // 14: water_a
        mapTiles.add(tile(1, 4, TileType.NOT_PASSABLE));

        // 15: water_b
        mapTiles.add(tile(1, 5, TileType.NOT_PASSABLE));

        // 16: water_crystal
        mapTiles.add(tile(1, 6, TileType.NOT_PASSABLE));

        // 17: reeds_water
        mapTiles.add(tile(1, 7, TileType.NOT_PASSABLE));

        // 18: plank_v
        mapTiles.add(tile(1, 8, TileType.PASSABLE));

        // 19: plank_h
        mapTiles.add(tile(1, 9, TileType.PASSABLE));

        // 20: ash_a
        mapTiles.add(tile(2, 0, TileType.PASSABLE));

        // 21: ash_b
        mapTiles.add(tile(2, 1, TileType.PASSABLE));

        // 22: cracked_earth
        mapTiles.add(tile(2, 2, TileType.PASSABLE));

        // 23: obsidian_a
        mapTiles.add(tile(2, 3, TileType.NOT_PASSABLE));

        // 24: obsidian_b
        mapTiles.add(tile(2, 4, TileType.NOT_PASSABLE));

        // 25: lava_crack_a
        mapTiles.add(tile(2, 5, TileType.NOT_PASSABLE));

        // 26: lava_crack_b
        mapTiles.add(tile(2, 6, TileType.NOT_PASSABLE));

        // 27: lava_glow
        mapTiles.add(tile(2, 7, TileType.NOT_PASSABLE));

        // 28: ember_grate
        mapTiles.add(tile(2, 8, TileType.PASSABLE));

        // 29: rubble
        mapTiles.add(tile(2, 9, TileType.PASSABLE));

        // 30: dead_tree
        mapTiles.add(tile(3, 0, TileType.NOT_PASSABLE));

        // 31: grave_ash
        mapTiles.add(tile(3, 1, TileType.NOT_PASSABLE));

        // 32: mound_ash
        mapTiles.add(tile(3, 2, TileType.NOT_PASSABLE));

        // 33: grave_paving
        mapTiles.add(tile(3, 3, TileType.NOT_PASSABLE));

        // 34: cross_paving
        mapTiles.add(tile(3, 4, TileType.NOT_PASSABLE));

        // 35: fountain_plaza
        mapTiles.add(tile(3, 5, TileType.NOT_PASSABLE));

        // 36: fountain_paving
        mapTiles.add(tile(3, 6, TileType.NOT_PASSABLE));

        // 37: market_stall
        mapTiles.add(tile(3, 7, TileType.NOT_PASSABLE));

        // 38: crates
        mapTiles.add(tile(3, 8, TileType.NOT_PASSABLE));

        // 39: sign
        mapTiles.add(tile(3, 9, TileType.NOT_PASSABLE));

        // 40: tree
        mapTiles.add(tile(4, 0, TileType.NOT_PASSABLE));

        // 41: tree_dark
        mapTiles.add(tile(4, 1, TileType.NOT_PASSABLE));

        // 42: bush
        mapTiles.add(tile(4, 2, TileType.NOT_PASSABLE));

        // 43: stump
        mapTiles.add(tile(4, 3, TileType.NOT_PASSABLE));

        // 44: log_pile
        mapTiles.add(tile(4, 4, TileType.NOT_PASSABLE));

        // 45: boulder
        mapTiles.add(tile(4, 5, TileType.NOT_PASSABLE));

        // 46: standing_stone
        mapTiles.add(tile(4, 6, TileType.NOT_PASSABLE));

        // 47: fence_h
        mapTiles.add(tile(4, 7, TileType.NOT_PASSABLE));

        // 48: fence_v
        mapTiles.add(tile(4, 8, TileType.NOT_PASSABLE));

        // 49: cracked_patch
        mapTiles.add(tile(4, 9, TileType.PASSABLE));

        // 50: roof_tl
        mapTiles.add(tile(5, 0, TileType.NOT_PASSABLE));

        // 51: roof_t
        mapTiles.add(tile(5, 1, TileType.NOT_PASSABLE));

        // 52: roof_tr
        mapTiles.add(tile(5, 2, TileType.NOT_PASSABLE));

        // 53: roof_l
        mapTiles.add(tile(5, 3, TileType.NOT_PASSABLE));

        // 54: roof_m
        mapTiles.add(tile(5, 4, TileType.NOT_PASSABLE));

        // 55: roof_r
        mapTiles.add(tile(5, 5, TileType.NOT_PASSABLE));

        // 56: roof_bl
        mapTiles.add(tile(5, 6, TileType.NOT_PASSABLE));

        // 57: roof_b
        mapTiles.add(tile(5, 7, TileType.NOT_PASSABLE));

        // 58: roof_br
        mapTiles.add(tile(5, 8, TileType.NOT_PASSABLE));

        // 59: wall_brace
        mapTiles.add(tile(5, 9, TileType.NOT_PASSABLE));

        // 60: wall_window
        mapTiles.add(tile(6, 0, TileType.NOT_PASSABLE));

        // 61: wall_door
        mapTiles.add(tile(6, 1, TileType.NOT_PASSABLE));

        // 62: wall_plain
        mapTiles.add(tile(6, 2, TileType.NOT_PASSABLE));

        // 63: hall_plank
        mapTiles.add(tile(6, 3, TileType.NOT_PASSABLE));

        // 64: hall_door
        mapTiles.add(tile(6, 4, TileType.NOT_PASSABLE));

        // 65: palisade_front
        mapTiles.add(tile(6, 5, TileType.NOT_PASSABLE));

        // 66: palisade_side
        mapTiles.add(tile(6, 6, TileType.NOT_PASSABLE));

        return mapTiles;
    }
}
