package Maps;

import java.util.ArrayList;

import GameObject.Door;
import Level.EnhancedMapTile;
import Level.Map;
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
    // these tiles and press ENTER. The blacksmith's spot (right side) is not built yet. {column, row}
    private static final int[][] MERCHANT_TILES = {{1, 1}, {2, 1}, {3, 1}, {1, 3}, {2, 3}, {3, 3}};

    @Override
    protected void loadScripts() {
        for (int[] tile : MERCHANT_TILES) {
            getMapTile(tile[0], tile[1]).setInteractScript(new ShopBuildingScript());
        }
    }
}
