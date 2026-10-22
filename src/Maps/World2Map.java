package Maps;

import java.util.ArrayList;

import EnhancedMapTiles.Sign;
import GameObject.Portal;
import Level.EnhancedMapTile;
import Level.Map;
import Level.NPC;
import NPCs.Dummy;
import Scripts.PortalScript;
import Scripts.World1.KeptGroveScript;
import Scripts.World2.*;
import Tilesets.World2Tileset;
import Utils.Point;

public class World2Map extends Map {
    public World2Map() {
        super("world2_map.txt", new World2Tileset());
        this.playerStartPosition = new Point(70 * 48, 48 * 48);
    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();

        Portal portal = new Portal(getMapTile(75, 40).getX(), getMapTile(75, 40).getY(),
                "world1", 3888, 4320, 48, 48);
        portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(portal);

        //Signs with storys that are placed around the map, press enter to interact with them
        //Eldermere
        Sign eldermere = new Sign(getMapTile(70, 47).getLocation(), "Sign.png",32, 32,1.3f);
        eldermere.setInteractScript(new EldermereScript());
        enhancedMapTiles.add(eldermere);

        //Old Wood & Kept Circle
        Sign oldWoodKeptCircle = new Sign(getMapTile(26, 43).getLocation(), "Sign.png",32, 32,1.3f);
        oldWoodKeptCircle.setInteractScript(new OldWoodKeptCircleScript());
        enhancedMapTiles.add(oldWoodKeptCircle);

        //Old Wood & Kept Circle
        Sign woodcuttersClearing = new Sign(getMapTile(43, 43).getLocation(), "Sign.png",32, 32,1.3f);
        woodcuttersClearing.setInteractScript(new WoodcuttersClearingScript());
        enhancedMapTiles.add(woodcuttersClearing);

        //Minster & Scriptorium
        Sign minsterAndScriptorium = new Sign(getMapTile(84, 41).getLocation(), "Sign.png",32, 32,1.3f);
        minsterAndScriptorium.setInteractScript(new MinsterAndScriptoriumScript());
        enhancedMapTiles.add(minsterAndScriptorium);

        //Baron’s Hold
        Sign baronsHold = new Sign(getMapTile(100, 28).getLocation(), "Sign.png",32, 32,1.3f);
        baronsHold.setInteractScript(new BaronsHoldScript());
        enhancedMapTiles.add(baronsHold);

        //Marsh & Drainage
        Sign marshDrainage = new Sign(getMapTile(70, 62).getLocation(), "Sign.png",32, 32,1.3f);
        marshDrainage.setInteractScript(new MarshDrainageScript());
        enhancedMapTiles.add(marshDrainage);

        //The Fracture
        Sign theFracture = new Sign(getMapTile(70, 96).getLocation(), "Sign.png",32, 32,1.3f);
        theFracture.setInteractScript(new TheFractureScript());
        enhancedMapTiles.add(theFracture);

        return enhancedMapTiles;
    }
}