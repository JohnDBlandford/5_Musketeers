package Maps;

import EnhancedMapTiles.ItemMapTile;
import EnhancedMapTiles.PushableRock;
import GameObject.PlaceHolderSpriteFactory;
import GameObject.Portal;
import GameObject.SpriteSheet;
import Item.Item;
import Item.ItemTestFactory;
import Level.*;
import NPCs.*;
import Scripts.SimpleTextScript;
import Scripts.PortalScript;
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

        // Add a portal to the blank map at (18, 20) that teleports the player to (48, 48) in the blank map
        Portal portal = new Portal(getMapTile(18, 20).getX(), getMapTile(18, 20).getY(),
                "blank", 48, 48, 48, 48);
        portal.setInteractScript(new PortalScript());
        enhancedMapTiles.add(portal);

        // Add one test sword, shield, ranged weapon, and armor piece
        SpriteSheet itemSwordLook = PlaceHolderSpriteFactory.drawSolidColor(16, 17, Color.GRAY);
        ItemMapTile testSword = new ItemMapTile(getMapTile(5, 6).getLocation(), ItemTestFactory.createRustedShortSword(itemSwordLook));
        enhancedMapTiles.add(testSword);

        SpriteSheet itemLookShield = PlaceHolderSpriteFactory.drawSolidColor(15, 15, Color.BLUE);
        ItemMapTile testShield = new ItemMapTile(getMapTile(19,8).getLocation(), ItemTestFactory.createPatchedWoodShield(itemLookShield));
        enhancedMapTiles.add(testShield);

        SpriteSheet itemLookRangedWeapon = PlaceHolderSpriteFactory.drawSolidColor(16, 18, Color.GREEN);
        ItemMapTile testRangedWeapon = new ItemMapTile(getMapTile(3, 17).getLocation(), ItemTestFactory.createCrackedHuntingBow(itemLookRangedWeapon));
        enhancedMapTiles.add(testRangedWeapon);

        SpriteSheet itemLookArmor = PlaceHolderSpriteFactory.drawSolidColor(15, 16, Color.ORANGE);
        java.util.Map<Item.ItemAbilities, Item.AbilityType> itemAbilities = new HashMap<>();
        ItemMapTile testArmor = new ItemMapTile(getMapTile(0, 20).getLocation(), ItemTestFactory.createBeggarsWrapsArmor(itemLookArmor, itemAbilities));
        enhancedMapTiles.add(testArmor);

        return enhancedMapTiles;
    }

    @Override
    public ArrayList<NPC> loadNPCs() {
        ArrayList<NPC> npcs = new ArrayList<>();

        // Original NPCs
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

        Dummy dummy = new Dummy(4, getMapTile(13, 15).getLocation());
        npcs.add(dummy);

        // =========================================================================
        // World 1 Test Combat Dummies (Line up near spawn area for easy testing)
        // =========================================================================

        // 1. Starving Wretch (ID: 10, HP: 70, DEF: 10, SPD: 22)
        StarvingWretchDummy wretch = new StarvingWretchDummy(10, getMapTile(14, 22).getLocation());
        npcs.add(wretch);

        // 2. Reckoner Brute (ID: 11, HP: 150, DEF: 26, SPD: 12)
        ReckonerBruteDummy brute = new ReckonerBruteDummy(11, getMapTile(16, 22).getLocation());
        npcs.add(brute);

        // 3. Ash Wolf (ID: 12, HP: 110, DEF: 9, SPD: 30)
        AshWolfDummy wolf = new AshWolfDummy(12, getMapTile(18, 22).getLocation());
        npcs.add(wolf);

        // 4. Hollow Chorus Zealot (ID: 13, HP: 220, DEF: 24, SPD: 26)
        HollowChorusZealotDummy zealot = new HollowChorusZealotDummy(13, getMapTile(20, 22).getLocation());
        npcs.add(zealot);

        // 5. World 1 Mini-Boss: The Famine Warden (ID: 14, HP: 380, DEF: 32, SPD: 28)
        FamineWardenDummy warden = new FamineWardenDummy(14, getMapTile(22, 22).getLocation());
        npcs.add(warden);

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