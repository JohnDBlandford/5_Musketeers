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
        t.add(ground(0, 0)); // ID 0 - brown base ground
        t.add(ground(0, 1)); // ID 1 - brown base ground, variant
        t.add(ground(0, 2)); // ID 2 - lighter grey-brown dirt
        t.add(ground(0, 3)); // ID 3 - lighter grey-brown dirt, variant
        t.add(ground(0, 4)); // ID 4 - tan dirt with dark debris marks
        t.add(ground(0, 5).withTileType(TileType.NOT_PASSABLE)); // ID 5 - tan dirt with dark debris marks, variant
        t.add(ground(0, 6));
        t.add(ground(0, 7));
        t.add(ground(0, 8));

        // rows 8-9: brown earth with olive-green veins (4x2 block)
        t.add(ground(8, 0)); // ID 17
        t.add(ground(8, 1)); // ID 18
        t.add(ground(8, 2)); // ID 19
        t.add(ground(8, 3)); // ID 20
        t.add(ground(9, 0)); // ID 21
        t.add(ground(9, 1)); // ID 22
        t.add(ground(9, 2)); // ID 23
        t.add(ground(9, 3)); // ID 24

        return t;
    }

}