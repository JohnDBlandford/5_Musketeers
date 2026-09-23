package Maps;

import EnhancedMapTiles.ItemMapTile;
import EnhancedMapTiles.PushableRock;
import GameObject.PlaceHolderSpriteFactory;
import GameObject.SpriteSheet;
import Item.*;
import Level.*;
import NPCs.Bug;
import NPCs.Dinosaur;
import NPCs.Walrus;
import Scripts.SimpleTextScript;
import Scripts.TestMap.*;
import Tilesets.CommonTileset;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

// Represents a test map to be used in a level
public class TestMap extends Map {

    public TestMap() {
        super("test_map.txt", new CommonTileset());
        this.playerStartPosition = getMapTile(17, 20).getLocation();
    }

    @Override
    public ArrayList<EnhancedMapTile> loadEnhancedMapTiles() {
        ArrayList<EnhancedMapTile> enhancedMapTiles = new ArrayList<>();

        PushableRock pushableRock = new PushableRock(getMapTile(2, 7).getLocation());
        enhancedMapTiles.add(pushableRock);

        // add one test sword, shield, ranged weapon, and armor piece
        // need to figure out how to get a BufferedImage in here without a real sprite
        SpriteSheet itemSwordLook = PlaceHolderSpriteFactory.drawSolidColor(16, 17, Color.GRAY);
        ItemMapTile testSword = new ItemMapTile(getMapTile(5, 10).getLocation(), ItemTestFactory.createRustedShortSword(itemSwordLook));
        enhancedMapTiles.add(testSword);

        SpriteSheet itemLookShield = PlaceHolderSpriteFactory.drawSolidColor(15, 15, Color.BLUE);
        ItemMapTile testShield = new ItemMapTile(getMapTile(10,20).getLocation(), ItemTestFactory.createPatchedWoodShield(itemLookShield));
        enhancedMapTiles.add(testShield);

        SpriteSheet itemLookRangedWeapon = PlaceHolderSpriteFactory.drawSolidColor(16, 18, Color.GREEN);
        ItemMapTile testRangedWeapon = new ItemMapTile(getMapTile(30, 40).getLocation(), ItemTestFactory.createCrackedHuntingBow(itemLookRangedWeapon));
        enhancedMapTiles.add(testRangedWeapon);

        SpriteSheet itemLookArmor = PlaceHolderSpriteFactory.drawSolidColor(15, 16, Color.ORANGE);
        java.util.Map<Item.ItemAbilities, Item.AbilityType> itemAbilities = new HashMap<>();
        ItemMapTile testArmor = new ItemMapTile(getMapTile(60, 60).getLocation(), ItemTestFactory.createBeggarsWrapsArmor(itemLookArmor, itemAbilities));
        enhancedMapTiles.add(testArmor);

        return enhancedMapTiles;
    }

    @Override
    public ArrayList<NPC> loadNPCs() {
        ArrayList<NPC> npcs = new ArrayList<>();

        Walrus walrus = new Walrus(1, getMapTile(4, 28).getLocation().subtractY(40));
        walrus.setInteractScript(new WalrusScript());
        npcs.add(walrus);

        Dinosaur dinosaur = new Dinosaur(2, getMapTile(13, 4).getLocation());
        dinosaur.setExistenceFlag("hasTalkedToDinosaur");
        dinosaur.setInteractScript(new DinoScript());
        npcs.add(dinosaur);
        
        Bug bug = new Bug(3, getMapTile(7, 12).getLocation().subtractX(20));
        bug.setInteractScript(new BugScript());
        npcs.add(bug);

        return npcs;
    }

    @Override
    public ArrayList<Trigger> loadTriggers() {
        ArrayList<Trigger> triggers = new ArrayList<>();
        triggers.add(new Trigger(790, 1030, 100, 10, new LostBallScript(), "hasLostBall"));
        triggers.add(new Trigger(790, 960, 10, 80, new LostBallScript(), "hasLostBall"));
        triggers.add(new Trigger(890, 960, 10, 80, new LostBallScript(), "hasLostBall"));
        return triggers;
    }

    @Override
    public void loadScripts() {
        getMapTile(21, 19).setInteractScript(new SimpleTextScript("Cat's house"));

        getMapTile(7, 26).setInteractScript(new SimpleTextScript("Walrus's house"));

        getMapTile(20, 4).setInteractScript(new SimpleTextScript("Dino's house"));

        getMapTile(2, 6).setInteractScript(new TreeScript());
    }
}

