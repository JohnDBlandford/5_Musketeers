package Tilesets;

import Builders.FrameBuilder;
import Builders.MapTileBuilder;
import Engine.ImageLoader;
import GameObject.Frame;
import Level.TileType;
import Level.Tileset;
import java.util.ArrayList;

public class TavernTileset extends Tileset {

    public TavernTileset() {
        super(ImageLoader.load("tavern_interior_tileset.png"), 16, 16, 3);
    }

    // helper: walkable ground tile from a sheet cell
    private MapTileBuilder ground(int row, int col) {
        Frame f = new FrameBuilder(getImage().getSubimage(
                col * getSpriteWidth(),
                row * getSpriteHeight(),
                getSpriteWidth(),
                getSpriteHeight())).withScale(tileScale).build();
        return new MapTileBuilder(f);
    }

    @Override
    public ArrayList<MapTileBuilder> defineTiles() {
        ArrayList<MapTileBuilder> t = new ArrayList<>();

        // row 1
        t.add(ground(0, 0));
        t.add(ground(0, 1));
        t.add(ground(0, 2));
        t.add(ground(0, 3));
        t.add(ground(0, 4));
        t.add(ground(0, 5).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(0, 6).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(0, 7).withTileType(TileType.NOT_PASSABLE));

        // row 2
        t.add(ground(1, 0));
        t.add(ground(1, 1));
        t.add(ground(1, 2));
        t.add(ground(1, 3));
        t.add(ground(1, 4).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(1, 5));
        t.add(ground(1, 6));
        t.add(ground(1, 7));

        // row 3
        t.add(ground(2, 0));
        t.add(ground(2, 1));
        t.add(ground(2, 2));
        t.add(ground(2, 3));
        t.add(ground(2, 4));
        t.add(ground(2, 5));
        t.add(ground(2, 6).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(2, 7).withTileType(TileType.NOT_PASSABLE));
        // row 4
        t.add(ground(3, 0).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(3, 1).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(3, 2).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(3, 3).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(3, 4).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(3, 5).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(3, 6).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(3, 7).withTileType(TileType.NOT_PASSABLE));
        // row 5
        t.add(ground(4, 0).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(4, 1).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(4, 2).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(4, 3).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(4, 4).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(4, 5).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(4, 6).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(4, 7).withTileType(TileType.NOT_PASSABLE));

        // row 6
        t.add(ground(5, 0).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(5, 1).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(5, 2).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(5, 3).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(5, 4).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(5, 5).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(5, 6).withTileType(TileType.NOT_PASSABLE));
        t.add(ground(5, 7).withTileType(TileType.NOT_PASSABLE));

        return t;
    }

}