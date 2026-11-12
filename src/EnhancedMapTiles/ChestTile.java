package EnhancedMapTiles;

import Builders.FrameBuilder;
import Chest.ChestId;
import Engine.ImageLoader;
import GameObject.Frame;
import GameObject.GameObject;
import Level.EnhancedMapTile;
import Level.TileType;
import Scripts.ChestScript;
import Utils.Point;

import java.awt.image.BufferedImage;
import java.util.Objects;

// A chest on the map: solid, one map tile big, and pressing ENTER next to it runs ChestScript for its ChestId (the same way
// a Sign runs its script). The chest looks the same before and after it is opened for now (there is no open sprite yet).
// PLACEHOLDER art: the chest cell of the tavern interior tileset (row 4, column 3 of its 16px cells). To change the art, change
// the four constants below. The cells are cut straight from the image like TavernTileset does (SpriteSheet.getSubImage adds a
// 1px gap per cell that this sheet does not have).
public class ChestTile extends EnhancedMapTile {
    private static final String IMAGE_FILE = "tavern_interior_tileset.png";
    private static final int CELL_SIZE = 16;
    private static final int CELL_ROW = 4;
    private static final int CELL_COLUMN = 3;
    private static final int SCALE = 3; // 16px * 3 = 48px, one map tile

    private final ChestId chestId;

    // location is the top left corner of the map tile the chest stands on

    /**
     * @author Derek
     * @param location the location where the chest is on the map
     * @param id the chest id
     */
    public ChestTile(Point location, ChestId id) {
        Objects.requireNonNull(location, "location not set");
        super(location.x, location.y, buildBottomLayer(location), null, TileType.NOT_PASSABLE);
        this.chestId = Objects.requireNonNull(id, "id cannot be found");
        setInteractScript(new ChestScript(id));
    }

    /**
     * @author Derek
     * @return returns a chest id
     */
    public ChestId getChestId() { return chestId; }

    /**
     * @author Derek
     * @param location the location of the bottom layer
     * @return a GameObject with its location and frame set
     */
    private static GameObject buildBottomLayer(Point location) {
        Objects.requireNonNull(location, "location not set");
        BufferedImage sheet = ImageLoader.load(IMAGE_FILE);
        BufferedImage cell = sheet.getSubimage(CELL_COLUMN * CELL_SIZE, CELL_ROW * CELL_SIZE, CELL_SIZE, CELL_SIZE);
        Frame frame = new FrameBuilder(cell).withScale(SCALE).build();
        return new GameObject(location.x, location.y, frame);
    }
}
