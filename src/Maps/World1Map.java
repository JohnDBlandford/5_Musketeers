package Maps;

import java.util.ArrayList;

import EnhancedMapTiles.Sign;
import GameObject.Portal;
import Level.EnhancedMapTile;
import Level.Map;
import Scripts.World1.*;
import Scripts.PortalScript;
import Tilesets.World1Tileset;

public class World1Map extends Map {
    public World1Map() {
        super("world1_map.txt", new World1Tileset());
        this.playerStartPosition = getMapTile(81, 90).getLocation(); // Brackenford plaza

    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();

        // The portal is placed at the location of the tile at (60, 90) in the world1
        // map
        // and teleports to (48, 48) in the map
        Portal portal = new Portal(getMapTile(60, 90).getX(), getMapTile(60, 90).getY(),
                "world2_map", 3360, 2340, 48, 48);
        portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(portal);

        //Signs with storys that are placed around the map, press enter to interact with them
        //Intro
        Sign intro = new Sign(getMapTile(80, 88).getLocation(), "Sign.png",32, 32,1.3f);
        intro.setInteractScript(new IntroScript());
        enhancedMapTiles.add(intro);

        //Concord
        Sign concord = new Sign(getMapTile(75, 52).getLocation(), "Sign.png",32, 32,1.3f);
        concord.setInteractScript(new ConcordScript());
        enhancedMapTiles.add(concord);

        //Kept Grove
        Sign keptGrove = new Sign(getMapTile(10, 32).getLocation(), "Sign.png",32, 32,1.3f);
        keptGrove.setInteractScript(new KeptGroveScript());
        enhancedMapTiles.add(keptGrove);

        //Barren Fields
        Sign barrenFields = new Sign(getMapTile(83, 121).getLocation(), "Sign.png",32, 32,1.3f);
        barrenFields.setInteractScript(new BarrenFieldsScript());
        enhancedMapTiles.add(barrenFields);

        //Granary Cloister
        Sign granaryCloister = new Sign(getMapTile(130, 85).getLocation(), "Sign.png",32, 32,1.3f);
        granaryCloister.setInteractScript(new GranaryCloisterScript());
        enhancedMapTiles.add(granaryCloister);

        //Dry River
        Sign dryRiver = new Sign(getMapTile(142, 117).getLocation(), "Sign.png",32, 32,1.3f);
        dryRiver.setInteractScript(new DryRiverScript());
        enhancedMapTiles.add(dryRiver);

        //Barrows
        Sign barrows = new Sign(getMapTile(142, 31).getLocation(), "Sign.png",32, 32,1.3f);
        barrows.setInteractScript(new BarrowsScript());
        enhancedMapTiles.add(barrows);

        return enhancedMapTiles;
    }

}