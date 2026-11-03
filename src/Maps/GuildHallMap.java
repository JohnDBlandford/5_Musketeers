package Maps;

import java.util.ArrayList;

import GameObject.Door;
import Level.Camera;
import Level.EnhancedMapTile;
import Level.Map;
import Scripts.BlacksmithBuildingScript;
import Scripts.DoorScript;
import Scripts.ShopBuildingScript;

import Tilesets.*;
import Utils.Point;

public class GuildHallMap extends Map {

    public GuildHallMap() {
        super("guild_hall_map.txt", new TavernTileset());
        this.playerStartPosition = new Point(0, 0); //

    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();
        Door hallDoor = new Door(getMapTile(9, 14).getX(), getMapTile(9, 14).getY(),
                "world1", 3648, 3792, 96, 48);
        hallDoor.setInteractScript(new DoorScript());
        enhancedMapTiles.add(hallDoor);
        return enhancedMapTiles;
    }

    // SHOP: the merchant's spot is the left side of the hall (the two weapon-rack rows at the top left). Stand next to one of
    // these tiles and press ENTER. {column, row}
    private static final int[][] MERCHANT_TILES = {{1, 1}, {2, 1}, {3, 1}, {1, 3}, {2, 3}, {3, 3}};

    // BLACKSMITH: the blacksmith's spot is the right side of the hall (the weapon-rack row at the right, tile id 39 like the
    // merchant's racks). Stand next to one of these tiles and press ENTER. {column, row}
    private static final int[][] BLACKSMITH_TILES = {{15, 8}, {16, 8}, {17, 8}};

    // TEXTBOXES: the engine's Textbox moves to the TOP of the screen whenever Camera.isAtBottomOfMap() is true (so it doesn't
    // cover the player), which put the greeting over the gold display down by the blacksmith's racks. The hall is the only
    // map that needs this, so it swaps in a camera that never reports the bottom: every textbox here draws at the bottom. Only
    // Textbox reads isAtBottomOfMap(), so nothing else changes.
    @Override
    public void setupMap() {
        super.setupMap();
        this.camera = new Camera(0, 0, tileset.getScaledSpriteWidth(), tileset.getScaledSpriteHeight(), this) {
            @Override
            public boolean isAtBottomOfMap() {
                return false;
            }
        };
    }

    @Override
    protected void loadScripts() {
        for (int[] tile : MERCHANT_TILES) {
            getMapTile(tile[0], tile[1]).setInteractScript(new ShopBuildingScript());
        }
        for (int[] tile : BLACKSMITH_TILES) {
            getMapTile(tile[0], tile[1]).setInteractScript(new BlacksmithBuildingScript());
        }
    }
}
