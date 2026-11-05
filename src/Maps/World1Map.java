package Maps;

import java.util.ArrayList;

import EnhancedMapTiles.Sign;
import GameObject.Door;
import GameObject.Portal;
import Level.EnhancedMapTile;
import Level.Map;
import Level.NPC;
import Level.Trigger;
import Scripts.World1.*;
import Scripts.DoorScript;
import Story.KeptPortalScript;
import Story.NPCArt.Look;
import Story.StoryNPC;
import Story.StoryState;
import Tilesets.World1Tileset;
import Utils.Point;

// STORY CHANGES (search for "STORY:"): the World 1 story characters and objects (loadNPCs), and the portal west of
// Brackenford is now the Kept's standing stone (KeptPortalScript): it wakes once Old Maud has spoken.
// Each story NPC stands on the nearest walkable tile to the tile given below, so if one looks off, change its two numbers.
public class World1Map extends Map {
    public World1Map() {
        super("world1_map.txt", new World1Tileset());
        this.playerStartPosition = getMapTile(76, 87).getLocation(); // Brackenford plaza
    }

    @Override
    public ArrayList<Trigger> loadTriggers() {
        ArrayList<Trigger> triggers = new ArrayList<>();

        // INTRO MESSAGE SHOWS WHEN PRESS PLAY
        Point location = getMapTile(75, 87).getLocation();
        triggers.add(new Trigger(location.x, location.y, 3 * 48, 100, new IntroScript(), "sawIntro"));

        return triggers;
    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();

        // The portal is placed at the location of the tile at (55, 87) in the world1
        // map
        // and teleports to (48, 48) in the map
        Portal world2portal = new Portal(getMapTile(55, 87).getX(), getMapTile(55, 87).getY(),
                "world2_map", 3360, 2340, 48, 48);
        world2portal.setInteractScript(new KeptPortalScript());   // STORY: was PortalScript; asleep until Old Maud has spoken
        enhancedMapTiles.add(world2portal);

        Door hallDoor = new Door(getMapTile(76, 79).getX(), getMapTile(76, 79).getY(),
                "guild_hall_map", 456, 600, 96, 48);
        hallDoor.setInteractScript(new DoorScript());
        enhancedMapTiles.add(hallDoor);

        //Signs with storys that are placed around the map, press enter to interact with them
        //Intro
        Sign intro = new Sign(getMapTile(80, 88).getLocation(), "Sign.png",32, 32,1.3f);
        intro.setInteractScript(new IntroScript());
        enhancedMapTiles.add(intro);

        //Concord
        Sign concord = new Sign(getMapTile(70, 49).getLocation(), "Sign.png",32, 32,1.3f);
        concord.setInteractScript(new ConcordScript());
        enhancedMapTiles.add(concord);

        //Kept Grove
        Sign keptGrove = new Sign(getMapTile(22, 81).getLocation(), "Sign.png",32, 32,1.3f);
        keptGrove.setInteractScript(new KeptGroveScript());
        enhancedMapTiles.add(keptGrove);

        //Barren Fields
        Sign barrenFields = new Sign(getMapTile(78, 119).getLocation(), "Sign.png",32, 32,1.3f);
        barrenFields.setInteractScript(new BarrenFieldsScript());
        enhancedMapTiles.add(barrenFields);

        //Granary Cloister
        Sign granaryCloister = new Sign(getMapTile(124, 81).getLocation(), "Sign.png",32, 32,1.3f);
        granaryCloister.setInteractScript(new GranaryCloisterScript());
        enhancedMapTiles.add(granaryCloister);

        //Dry River
        Sign dryRiver = new Sign(getMapTile(133, 113).getLocation(), "Sign.png",32, 32,1.3f);
        dryRiver.setInteractScript(new DryRiverScript());
        enhancedMapTiles.add(dryRiver);

        //Barrows
        Sign barrows = new Sign(getMapTile(137, 32).getLocation(), "Sign.png",32, 32,1.3f);
        barrows.setInteractScript(new BarrowsScript());
        enhancedMapTiles.add(barrows);

        return enhancedMapTiles;

    }

    // STORY: the World 1 story characters and objects. Each one shows up only while the story says it is there
    // (Wren until she dies, her grave after; the wounded Reckoner once you have fought at the Barrows; the Warden until he falls).
    //   new StoryNPC(id, storyId, look, map, tileX, tileY, visibleWhen)
    @Override
    public ArrayList<NPC> loadNPCs() {
        ArrayList<NPC> storyNPCs = new ArrayList<>();
        // Brackenford (the plaza is around tile 76, 87)
        storyNPCs.add(new StoryNPC(101, "wren", Look.WREN, this, 73, 89, () -> !StoryState.flag("wren_dead")));
        storyNPCs.add(new StoryNPC(102, "grave", Look.GRAVE, this, 73, 89,
                () -> StoryState.flag("wren_dead") && !StoryState.flag("w1_wren_bread")));
        storyNPCs.add(new StoryNPC(103, "grave", Look.GRAVE_BREAD, this, 73, 89,
                () -> StoryState.flag("wren_dead") && StoryState.flag("w1_wren_bread")));
        storyNPCs.add(new StoryNPC(104, "hester", Look.HESTER, this, 71, 86, null));
        storyNPCs.add(new StoryNPC(105, "ida", Look.IDA, this, 83, 86, null));
        storyNPCs.add(new StoryNPC(106, "mirel", Look.MIREL, this, 79, 84, null));
        storyNPCs.add(new StoryNPC(107, "joss", Look.JOSS, this, 85, 91, null));
        storyNPCs.add(new StoryNPC(108, "vale", Look.VALE, this, 80, 92, null));
        storyNPCs.add(new StoryNPC(109, "bess", Look.BESS, this, 70, 91, null));
        storyNPCs.add(new StoryNPC(110, "wall", Look.WALL, this, 72, 93, null));
        // Barren Fields
        storyNPCs.add(new StoryNPC(111, "elsa", Look.ELSA, this, 74, 122, null));
        // Granary Cloister
        storyNPCs.add(new StoryNPC(112, "ansel", Look.ANSEL, this, 133, 86, null));
        // Dry River
        storyNPCs.add(new StoryNPC(113, "mark", Look.MARK, this, 140, 121, null));
        // Barrows
        storyNPCs.add(new StoryNPC(114, "hale", Look.HALE, this, 133, 27, () -> StoryState.stage("w1_graves") >= 2));
        storyNPCs.add(new StoryNPC(115, "mound", Look.MOUND, this, 139, 23, null));
        // Ashwood
        storyNPCs.add(new StoryNPC(116, "warden", Look.WARDEN, this, 25, 41, () -> !StoryState.done("w1_warden")));
        // Kept Grove
        storyNPCs.add(new StoryNPC(117, "maud", Look.MAUD, this, 18, 82, null));
        storyNPCs.add(new StoryNPC(118, "cache", Look.CACHE, this, 16, 87, null));
        return storyNPCs;
    }
}