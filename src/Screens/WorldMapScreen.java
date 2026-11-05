package Screens;

import BattleSystem.BattleUI;
import Engine.*;
import Locations.NamedLocations;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

// The world map (press M while walking). It is the world's REAL tile map shrunk down: every tile of World1Map / World2Map
// becomes 2x2 map pixels coloured from its own sprite, so roads, houses, rivers and forests sit exactly where they are in game.
//   - the colours are graded toward each world's era (famine dust, a green land being cut down, ash and plague)
//   - the border is torn and burnt instead of a straight rectangle edge
//   - everything the knight has not discovered yet is hidden under fog; finding a place clears the fog around it
//   - pins: gray = not discovered yet, gold = discovered (you can travel there), green with a flag = where the knight is now
//   - Dabbah's Hollow (World 4, inside World 1 past the northern gate) is hidden under living shadow clouds
//   - pointing at a place with the mouse shows its world's enemy pool (levels and spawn chance); B opens the bestiary
// ARROWS / WASD pick a pin, ENTER travels there, clicking a pin travels too, Q / E switch world, Z / X or the mouse wheel zoom,
// B opens the bestiary, M or ESC closes.
// Travelling uses PlayLevelScreen's own waypoint move, so level, XP, HP and gear are kept.
public final class WorldMapScreen extends Screen {
    private final PlayLevelScreen playLevelScreen;
    private final KeyLocker keyLocker = new KeyLocker();

    // map sizes in tiles (from world1_map.txt and world2_map.txt) so the pins sit where the places really are.
    // A world that is not listed here (a new World 3 map) uses the bounding box of its places instead.
    private static final java.util.Map<String, int[]> MAP_TILES = new HashMap<>();
    static {
        MAP_TILES.put("world1", new int[] { 148, 136 });
        MAP_TILES.put("world2_map", new int[] { 132, 124 });
    }

    // ---- the story layer (from the sign scripts and the design document) ----
    // what the map says about each place once it is found: { one-line lore, faction or "" }
    private static final java.util.Map<String, String[]> LORE = new HashMap<>();
    static {
        // World 1 - The Reckoning
        LORE.put("BRACKENFORD", new String[] { "A DYING VILLAGE. FAMINE AND THE HOLLOW CHORUS FILL ITS STREETS.", "" });
        LORE.put("ASHWOOD", new String[] { "A WALLED STONE YARD AT THE EDGE OF THE VALLEY.", "" });
        LORE.put("KEPT GROVE", new String[] { "THE KEPT LET THE FIELDS GROW WILD AND REMEMBER THE DAY THE SHEPHERD DIED.", "KEPT GROVE" });
        LORE.put("BARROWS", new String[] { "OLD BURIAL MOUNDS. RECKONERS NOW CAMP AMONG THE GRAVES.", "" });
        LORE.put("GRANARY CLOISTER", new String[] { "A GRANARY TURNED MONASTERY. ITS CELLARS HOLD SEED GRAIN FOR THE LAST WINTER.", "HOLLOW CHORUS" });
        LORE.put("DRY RIVER", new String[] { "CRACKED WHITE FROM BANK TO BANK. THE BEAST DRIED IT TO GIVE BACK WHAT WAS TAKEN.", "" });
        LORE.put("BARREN FIELDS", new String[] { "OVER-FARMED FOR YEARS. WHEN THE RECKONING BEGAN, THE LAST CROPS FAILED.", "" });
        // World 2 - The Long Erosion
        LORE.put("ELDEREMERE", new String[] { "A BUSY VILLAGE. ITS FIELDS GROW WIDER EVERY YEAR, AND THE FOREST SMALLER.", "" });
        LORE.put("OLD WOOD & KEPT CIRCLE", new String[] { "THE OLDEST FOREST. ITS DRUIDS BEGIN CALLING THEMSELVES THE KEPT.", "KEPT GROVE" });
        LORE.put("WOODCUTTERS' CLEARING", new String[] { "CUT FOR FARMLAND AND WAR-TIMBER, ONE TREE AT A TIME.", "" });
        LORE.put("BARON'S HOLD", new String[] { "SEAT OF THE BARON. HIS ORDERS CLEAR THE FORESTS FOR THE KING'S WARSHIPS.", "" });
        LORE.put("MINSTER & SCRIPTORIUM", new String[] { "DOCTRINE IS WRITTEN HERE. IN TIME THIS BODY WILL BECOME THE CONCORD.", "CONCORD" });
        LORE.put("MARSH & DRAINAGE", new String[] { "ONCE HOME TO RIVER-WARDENS. NOW DRAINED AT SCALE FOR MORE FIELDS.", "" });
        LORE.put("THE FRACTURE", new String[] { "BURNED SO LONG THE EARTH ITSELF CRACKED. NOTHING GROWS HERE.", "" });
    }

    private static Color factionColor(String f) {
        if (f.equals("KEPT GROVE")) return new Color(110, 180, 96);
        if (f.equals("HOLLOW CHORUS")) return new Color(176, 146, 196);
        if (f.equals("CONCORD")) return new Color(236, 206, 120);
        return BattleUI.GREY;
    }

    // enemy levels of each world's random encounters
    // BALANCE: read from the world's level band in Balance (was hard-coded 1-5 / 5-12 / 12-20)
    private static String dangerOf(String map) {
        int[] b = BattleSystem.Balance.band(BattleSystem.Balance.worldOf(map));
        return "ENEMIES LV " + b[0] + "-" + b[1];
    }

    // landmarks drawn on the map that are not waypoints (tile positions from World1Map / World2Map and the map files)
    private static final int POI_PORTAL = 0, POI_HOLLOW = 1, POI_CONCORD = 2, POI_STONES = 3;
    private static final class Poi {
        final String map, label;
        final int tx, ty, kind;
        final boolean always; // shown even under the fog
        Poi(String map, String label, int tx, int ty, int kind, boolean always) {
            this.map = map; this.label = label; this.tx = tx; this.ty = ty; this.kind = kind; this.always = always;
        }
    }
    private static final List<Poi> POIS = new ArrayList<>();
    static {
        POIS.add(new Poi("world1", "DABBAH'S HOLLOW - SEALED", 74, 7, POI_HOLLOW, true)); // the gate tiles 229-252, opened for the finale
        POIS.add(new Poi("world1", "CONCORD BANNER", 70, 49, POI_CONCORD, false));
        POIS.add(new Poi("world2_map", "PORTAL: WORLD 1", 75, 40, POI_PORTAL, false));
        POIS.add(new Poi("world2_map", "STANDING STONES", 18, 41, POI_STONES, false)); // the Kept circle's stones
    }

    private static final Key[] KEYS = { Key.M, Key.ESC, Key.Q, Key.E, Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT,
            Key.W, Key.A, Key.S, Key.D, Key.ENTER, Key.SPACE, Key.Z, Key.X, Key.B };

    private static final int PX = 3;            // size of one map pixel on screen (bigger = chunkier)
    private static final double COAST = 0.22;   // land / sea threshold of the height field

    private static final Color DIM = new Color(0, 0, 0, 190);
    private static final Color PANEL = new Color(14, 12, 14);
    private static final Color FRAME = new Color(214, 178, 110);
    private static final Color IRON = new Color(52, 46, 44);
    private static final Color BLOOD = new Color(110, 18, 18);
    private static final Color PIN_FOUND = new Color(230, 190, 60);
    private static final Color PIN_HERE = new Color(80, 190, 100);
    private static final Color PIN_LOCKED = new Color(125, 120, 115);
    private static final Color NAME_FOUND = new Color(255, 240, 200);
    private static final Color NAME_HERE = new Color(190, 240, 190);
    private static final Color NAME_LOCKED = new Color(150, 150, 150);
    private static final Color LABEL_BG = new Color(0, 0, 0, 180);

    private final List<String> worlds = new ArrayList<>(); // map names, in the order NamedLocations lists them
    private final java.util.Map<String, Terrain> terrainCache = new HashMap<>();
    private BufferedImage fog;          // fog of war for the shown world (rebuilt on open and on world switch)
    private BufferedImage vignette;
    private int worldIndex;
    private int selected; // index into placesOf(current world)
    private int frame;
    private boolean confirmArmed; // ENTER / SPACE travel on release, so the held key does not reach the knight
    private String note = "";
    private int noteTimer;
    private int lastMx = -1, lastMy = -1;

    // zoom (Z / X / mouse wheel): 1 = the whole world fits; below 1 is zoomed out further, above 1 follows the selected place
    private static final double ZOOM_MIN = 0.7, ZOOM_MAX = 3.0, ZOOM_START = 0.9;
    private double zoom = ZOOM_START;
    private double camX, camY; // the tile the view is centred on when zoomed in (glides toward the selected place)

    // the bestiary, opened over the map with B
    private CodexOfBeastsScreen bestiary;
    private boolean showingBestiary;

    // Dabbah's Hollow (World 4 lies inside World 1, past the gate in the north): it stays under living shadow clouds
    private static final String HOLLOW_MAP = "world1";
    private static final double HOLLOW_X = 74, HOLLOW_Y = 8, HOLLOW_RX = 36, HOLLOW_RY = 24; // in tiles
    private BufferedImage hollowClouds;
    private int hollowFrame = -1;

    public WorldMapScreen(PlayLevelScreen playLevelScreen) {
        this.playLevelScreen = playLevelScreen;
        for (NamedLocations.Place p : NamedLocations.all())
            if (!worlds.contains(p.map)) worlds.add(p.map);
    }

    // called every time the map is opened: starts on the knight's world, with his current place selected
    @Override
    public void initialize() {
        frame = 0;
        note = "";
        noteTimer = 0;
        confirmArmed = false;
        lastMx = MouseInput.getX();
        lastMy = MouseInput.getY();
        for (Key k : KEYS) keyLocker.lockKey(k); // the M that opened the map must not close it again
        int w = worlds.indexOf(playLevelScreen.getCurrentMapName());
        worldIndex = Math.max(0, w);
        selectCurrentOrFirst();
        terrain(world()); // the first time a world is shown its real map is read once, then kept
        fog = buildFog(world());
        showingBestiary = false;
        MouseInput.pollWheel(); // wheel turns made while walking don't count
        snapCamera();
    }

    // centres the view on the selected place at once (on open and on a world switch)
    private void snapCamera() {
        List<NamedLocations.Place> ps = placesOf(world());
        if (selected < ps.size()) { camX = ps.get(selected).tileX; camY = ps.get(selected).tileY; }
    }

    private String world() {
        return worlds.isEmpty() ? "" : worlds.get(worldIndex);
    }

    private List<NamedLocations.Place> placesOf(String map) {
        List<NamedLocations.Place> out = new ArrayList<>();
        for (NamedLocations.Place p : NamedLocations.all())
            if (p.map.equals(map)) out.add(p);
        return out;
    }

    private void selectCurrentOrFirst() {
        List<NamedLocations.Place> ps = placesOf(world());
        int i = ps.indexOf(playLevelScreen.getCurrentPlace());
        selected = Math.max(0, i);
    }

    // ================================================================== update

    // true once per key press (not while held)
    private boolean tap(Key key) {
        if (Keyboard.isKeyUp(key)) keyLocker.unlockKey(key);
        if (Keyboard.isKeyDown(key) && !keyLocker.isKeyLocked(key)) {
            keyLocker.lockKey(key);
            return true;
        }
        return false;
    }

    @Override
    public void update() {
        update(null);
    }

    // click = a mouse click this frame (PlayLevelScreen polls it), or null
    public void update(java.awt.Point click) {
        frame++;
        if (noteTimer > 0) noteTimer--;
        if (showingBestiary) {
            bestiary.update(click);
            MouseInput.pollWheel();
            return;
        }
        if (tap(Key.B)) {
            if (bestiary == null) bestiary = new CodexOfBeastsScreen(() -> {
                showingBestiary = false;
                for (Key k : KEYS) keyLocker.lockKey(k); // the key that closed the bestiary must not also close the map
            });
            bestiary.initialize();
            showingBestiary = true;
            return;
        }

        // zoom: Z in, X out, or the mouse wheel
        int wheel = MouseInput.pollWheel();
        if (tap(Key.Z)) wheel -= 1;
        if (tap(Key.X)) wheel += 1;
        if (wheel != 0) zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * Math.pow(1.2, -wheel)));
        // the view glides toward the selected place (only matters when zoomed in past the whole map)
        List<NamedLocations.Place> camPs = placesOf(world());
        if (selected < camPs.size()) {
            camX += (camPs.get(selected).tileX - camX) * 0.18;
            camY += (camPs.get(selected).tileY - camY) * 0.18;
        }

        if (tap(Key.M) | tap(Key.ESC)) {
            playLevelScreen.closeWorldMap();
            return;
        }
        if (tap(Key.Q)) switchWorld(-1);
        if (tap(Key.E)) switchWorld(1);

        if (tap(Key.UP) | tap(Key.W)) move(0, -1);
        if (tap(Key.DOWN) | tap(Key.S)) move(0, 1);
        if (tap(Key.LEFT) | tap(Key.A)) move(-1, 0);
        if (tap(Key.RIGHT) | tap(Key.D)) move(1, 0);

        if (tap(Key.ENTER) | tap(Key.SPACE)) confirmArmed = true;
        if (confirmArmed && Keyboard.isKeyUp(Key.ENTER) && Keyboard.isKeyUp(Key.SPACE)) {
            confirmArmed = false;
            List<NamedLocations.Place> ps = placesOf(world());
            if (selected < ps.size()) tryTravel(ps.get(selected));
            return;
        }

        // the mouse: pointing at a pin selects it, clicking it travels; clicking the arrows switches world
        int mx = MouseInput.getX(), my = MouseInput.getY();
        boolean moved = mx != lastMx || my != lastMy;
        lastMx = mx;
        lastMy = my;
        if (moved) {
            int hit = pinAt(mx, my);
            if (hit >= 0) selected = hit;
        }
        if (click != null) {
            if (arrowLeft().contains(click)) { switchWorld(-1); return; }
            if (arrowRight().contains(click)) { switchWorld(1); return; }
            int hit = pinAt(click.x, click.y);
            if (hit >= 0) {
                selected = hit;
                tryTravel(placesOf(world()).get(hit));
            }
        }
    }

    private void switchWorld(int dir) {
        if (worlds.size() < 2) return;
        worldIndex = (worldIndex + dir + worlds.size()) % worlds.size();
        selectCurrentOrFirst();
        noteTimer = 0;
        fog = buildFog(world());
        snapCamera();
    }

    private void tryTravel(NamedLocations.Place p) {
        if (p == playLevelScreen.getCurrentPlace()) {
            showNote("YOU ARE ALREADY HERE");
        } else if (!playLevelScreen.getDiscoveredPlaces().contains(p)) {
            showNote("NOT DISCOVERED YET");
        } else {
            playLevelScreen.travelTo(p);
        }
    }

    private void showNote(String s) {
        note = s;
        noteTimer = 120;
    }

    // moves the selection to the nearest pin in that direction (straight lines are preferred over diagonals)
    private void move(int dx, int dy) {
        List<NamedLocations.Place> ps = placesOf(world());
        if (ps.size() < 2) return;
        java.awt.Rectangle r = mapRect(world());
        java.awt.Point from = pin(ps.get(selected), r);
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < ps.size(); i++) {
            if (i == selected) continue;
            java.awt.Point p = pin(ps.get(i), r);
            double vx = p.x - from.x, vy = p.y - from.y;
            double along = vx * dx + vy * dy;
            if (along <= 0) continue;
            double across = Math.abs(vx * dy - vy * dx);
            double score = along + across * 2;
            if (score < bestScore) {
                bestScore = score;
                best = i;
            }
        }
        if (best >= 0) selected = best;
    }

    // ================================================================== layout

    private static int W() { return ScreenManager.getScreenWidth(); }
    private static int H() { return ScreenManager.getScreenHeight(); }
    private static int areaX() { return 46; }
    private static int areaY() { return 92; }
    private static int areaW() { return W() - 92; }
    private static int areaH() { return H() - 244; }
    private static int lowW() { return areaW() / PX; }
    private static int lowH() { return areaH() / PX; }
    private static int infoY() { return areaY() + lowH() * PX + 12; }

    // the world's size in tiles, read from its real map
    private int[] tiles(String map) {
        Terrain t = terrain(map);
        return new int[] { t.tilesW, t.tilesH };
    }

    // where the world's tiles sit on screen, as big as fits while keeping the map's shape (the land is drawn inside it)
    private java.awt.Rectangle mapRect(String map) {
        int[] t = tiles(map);
        int aw = lowW() * PX - 8, ah = lowH() * PX - 8, vw = lowW() * PX, vh = lowH() * PX;
        double s = Math.min(aw / (double) t[0], ah / (double) t[1]) * zoom;
        int w = (int) (t[0] * s), h = (int) (t[1] * s);
        int x = areaX() + (vw - w) / 2, y = areaY() + (vh - h) / 2;
        if (w > vw) x = Math.max(areaX() + vw - w, Math.min(areaX(), (int) (areaX() + vw / 2 - (camX + 0.5) * s)));
        if (h > vh) y = Math.max(areaY() + vh - h, Math.min(areaY(), (int) (areaY() + vh / 2 - (camY + 0.5) * s)));
        return new java.awt.Rectangle(x, y, w, h);
    }

    private java.awt.Rectangle viewRect() { return new java.awt.Rectangle(areaX(), areaY(), lowW() * PX, lowH() * PX); }

    private java.awt.Point pin(NamedLocations.Place p, java.awt.Rectangle r) {
        int[] t = tiles(p.map);
        int tx = p.tileX < 0 ? t[0] / 2 : p.tileX, ty = p.tileY < 0 ? t[1] / 2 : p.tileY;
        return new java.awt.Point(r.x + (int) ((tx + 0.5) * r.width / t[0]), r.y + (int) ((ty + 0.5) * r.height / t[1]));
    }

    // index of the pin (or its name label) under this point, or -1
    private int pinAt(int x, int y) {
        if (x < 0 || !viewRect().contains(x, y)) return -1;
        List<NamedLocations.Place> ps = placesOf(world());
        java.awt.Rectangle r = mapRect(world());
        java.awt.Rectangle[] labels = labelRects(world());
        for (int i = 0; i < ps.size(); i++) {
            java.awt.Point c = pin(ps.get(i), r);
            if (Math.hypot(x - c.x, y - c.y) <= 12 || labels[i].contains(x, y)) return i;
        }
        return -1;
    }

    // the name labels beside the pins: right of the pin if there is room, otherwise left, above or below,
    // whichever does not run off the map or cover a label that is already placed
    private java.awt.Rectangle[] labelRects(String map) {
        List<NamedLocations.Place> ps = placesOf(map);
        java.awt.Rectangle r = mapRect(map);
        java.awt.Rectangle[] out = new java.awt.Rectangle[ps.size()];
        int left = areaX() + 4, right = areaX() + lowW() * PX - 4, top = areaY() + 4, bottom = areaY() + lowH() * PX - 4;
        for (int i = 0; i < ps.size(); i++) {
            java.awt.Point c = pin(ps.get(i), r);
            int tw = BattleUI.font().getTextWidth(ps.get(i).name.toUpperCase(), 1, 1) + 10;
            java.awt.Rectangle[] tries = {
                    new java.awt.Rectangle(c.x + 12, c.y - 8, tw, 16), new java.awt.Rectangle(c.x - 12 - tw, c.y - 8, tw, 16),
                    new java.awt.Rectangle(c.x - tw / 2, c.y + 12, tw, 16), new java.awt.Rectangle(c.x - tw / 2, c.y - 30, tw, 16) };
            out[i] = tries[0];
            for (java.awt.Rectangle t : tries) {
                if (t.x < left || t.x + t.width > right || t.y < top || t.y + t.height > bottom) continue;
                boolean clash = false;
                for (int k = 0; k < i; k++) if (out[k].intersects(t)) clash = true;
                if (!clash) { out[i] = t; break; }
            }
        }
        return out;
    }

    private java.awt.Rectangle arrowLeft() { return new java.awt.Rectangle(W() - 330, 38, 40, 36); }
    private java.awt.Rectangle arrowRight() { return new java.awt.Rectangle(W() - 84, 38, 40, 36); }

    // ================================================================== draw

    @Override
    public void draw(GraphicsHandler gh) {
        if (showingBestiary) {
            bestiary.draw(gh);
            return;
        }
        int w = W(), h = H();
        Graphics2D g = gh.getGraphics();
        gh.drawFilledRectangle(0, 0, w, h, DIM);
        gh.drawFilledRectangleWithBorder(30, 30, w - 60, h - 60, PANEL, FRAME, 3);
        gh.drawRectangle(36, 36, w - 73, h - 73, BLOOD, 1);

        BattleUI.shadowText(gh, "WORLD MAP", 50, 46, 3, NAME_FOUND);
        drawWorldSwitcher(gh);

        String world = world();
        java.awt.Rectangle r = mapRect(world);
        Terrain t = terrain(world);
        int ax = areaX(), ay = areaY(), aw = lowW() * PX, ah = lowH() * PX;

        Object interp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gh.drawFilledRectangle(ax, ay, aw, ah, new Color(10, 8, 10));
        Shape oldClip = g.getClip();
        g.clipRect(ax, ay, aw, ah); // when zoomed in, nothing spills out of the map window
        g.drawImage(t.image, r.x, r.y, r.width, r.height, null);
        if (fog != null) g.drawImage(fog, r.x, r.y, r.width, r.height, null);
        drawHollowClouds(gh, world, r);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp == null ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : interp);
        if (vignette == null || vignette.getWidth() != aw || vignette.getHeight() != ah) vignette = makeVignette(aw, ah);
        g.drawImage(vignette, ax, ay, null);

        List<NamedLocations.Place> ps = placesOf(world);
        Set<NamedLocations.Place> found = playLevelScreen.getDiscoveredPlaces();
        NamedLocations.Place here = playLevelScreen.getCurrentPlace();
        drawPois(gh, world, r, labelRects(world));
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < ps.size(); i++) {
            NamedLocations.Place p = ps.get(i);
            java.awt.Point c = pin(p, r);
            boolean isHere = p == here, isFound = found.contains(p);
            if (isFound) { // a faint torch glow under discovered places
                int gr = 16 + (int) Math.round(Math.sin(frame * 0.08 + i) * 2);
                g.setColor(new Color(255, 170, 60, 38));
                g.fillOval(c.x - gr, c.y - gr, gr * 2, gr * 2);
            }
            if (i == selected) { // pulsing gold ring around the selected pin
                int rr = 11 + (int) Math.round(Math.sin(frame * 0.15) * 2);
                g.setStroke(new BasicStroke(3));
                g.setColor(BattleUI.GOLD);
                g.drawOval(c.x - rr, c.y - rr, rr * 2, rr * 2);
                g.setStroke(new BasicStroke(1));
            }
            g.setColor(new Color(20, 14, 10));
            g.fillOval(c.x - 8, c.y - 8, 16, 16);
            g.setColor(isHere ? PIN_HERE : (isFound ? PIN_FOUND : PIN_LOCKED));
            g.fillOval(c.x - 6, c.y - 6, 12, 12);
            if (isHere) { // a little flag over the knight's place
                g.setColor(new Color(20, 14, 10));
                g.fillRect(c.x - 1, c.y - 22, 2, 16);
                g.setColor(PIN_HERE);
                g.fillPolygon(new int[] { c.x + 1, c.x + 12, c.x + 1 }, new int[] { c.y - 22, c.y - 18, c.y - 14 }, 3);
            }
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : aa);

        java.awt.Rectangle[] labels = labelRects(world);
        for (int i = 0; i < ps.size(); i++) { // names last, so no pin covers them
            NamedLocations.Place p = ps.get(i);
            java.awt.Rectangle lr = labels[i];
            gh.drawFilledRectangle(lr.x, lr.y, lr.width, lr.height, i == selected ? new Color(70, 52, 24, 225) : LABEL_BG);
            Color c = p == here ? NAME_HERE : (found.contains(p) ? NAME_FOUND : NAME_LOCKED);
            BattleUI.font().draw(gh, p.name.toUpperCase(), lr.x + 5, lr.y + (lr.height - BattleUI.font().getCapHeight(1)) / 2, 1, 1, c);
        }
        g.setClip(oldClip);
        drawMapFrame(gh, ax, ay, aw, ah);
        drawCompass(gh, ax + aw - 34, ay + 30);
        BattleUI.text(gh, "ZOOM " + Math.round(zoom * 100) + "%", ax + 8, ay + 8, 1, new Color(214, 178, 110, 200));

        drawInfo(gh, ps, found, here);
        String help = "ARROWS: PICK   ENTER/CLICK: TRAVEL   Q/E: WORLD   Z/X/WHEEL: ZOOM   B: BESTIARY   M: CLOSE";
        BattleUI.font().draw(gh, help, (w - BattleUI.font().getTextWidth(help, 1, 1)) / 2, h - 60, 1, 1, BattleUI.GREY);

        int hover = pinAt(MouseInput.getX(), MouseInput.getY()); // pointing at a place shows its world's enemy pool
        if (hover >= 0) drawPoolTooltip(gh, ps.get(hover), MouseInput.getX(), MouseInput.getY());
    }

    // ---- the enemy pool of a place (its world's random encounters), shown while the mouse points at it
    private void drawPoolTooltip(GraphicsHandler gh, NamedLocations.Place p, int mx, int my) {
        List<String[]> rows = new ArrayList<>();
        if (p.safe) rows.add(new String[] { "SAFE - NO ENEMIES", "", "" });
        else for (CodexOfBeastsScreen.Entry e : CodexOfBeastsScreen.pool(p.map))
            rows.add(new String[] { CodexOfBeastsScreen.isSeen(e) ? e.name : "???", CodexOfBeastsScreen.levels(e),
                    e.boss ? "BOSS" : Math.round(e.spawnShare) + "%" });
        int nameW = 0;
        for (String[] r : rows) nameW = Math.max(nameW, BattleUI.font().getTextWidth(r[0], 1, 1));
        int tw = Math.max(170, nameW + 120), th = 26 + rows.size() * 14 + (p.safe ? 0 : 12);
        int x = Math.min(mx + 16, W() - tw - 34), y = Math.min(my + 14, H() - th - 34);
        if (x < 34) x = 34;
        gh.drawFilledRectangleWithBorder(x, y, tw, th, new Color(16, 12, 14, 240), new Color(214, 178, 110), 2);
        BattleUI.text(gh, "ENEMY POOL", x + 8, y + 8, 1, new Color(200, 80, 70));
        int ry = y + 24;
        for (String[] r : rows) {
            boolean unknown = r[0].equals("???");
            BattleUI.font().draw(gh, r[0], x + 8, ry, 1, 1, unknown ? NAME_LOCKED : (r[2].equals("BOSS") ? BattleUI.GOLD : NAME_FOUND));
            BattleUI.text(gh, r[1], x + tw - 96, ry, 1, NAME_FOUND);
            BattleUI.textRight(gh, r[2], x + tw - 8, ry, 1, r[2].equals("BOSS") ? BattleUI.GOLD : BattleUI.GREY);
            ry += 14;
        }
        if (!p.safe) BattleUI.text(gh, "B: BESTIARY FOR MOVES AND PASSIVES", x + 8, ry + 2, 1, BattleUI.GREY);
    }

    // ---- living shadow over Dabbah's Hollow: two layers of dark cloud drifting against each other, with red light inside
    private void drawHollowClouds(GraphicsHandler gh, String map, java.awt.Rectangle r) {
        if (!map.equals(HOLLOW_MAP)) return;
        int[] t = tiles(map);
        int sub = 2; // cloud cells per tile
        int x0 = (int) Math.floor(HOLLOW_X - HOLLOW_RX), y0 = (int) Math.max(0, Math.floor(HOLLOW_Y - HOLLOW_RY));
        int cw = (int) (HOLLOW_RX * 2 * sub), ch = (int) ((HOLLOW_Y + HOLLOW_RY - y0) * sub);
        if (hollowClouds == null || hollowClouds.getWidth() != cw || hollowClouds.getHeight() != ch)
            hollowClouds = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
        if (hollowFrame != frame / 2) { // redrawn every other frame
            hollowFrame = frame / 2;
            double time = frame * 0.02;
            int[] px = new int[cw * ch];
            for (int j = 0; j < ch; j++)
                for (int i = 0; i < cw; i++) {
                    double tu = x0 + (i + 0.5) / sub, tv = y0 + (j + 0.5) / sub;
                    double dx = (tu - HOLLOW_X) / HOLLOW_RX, dy = (tv - HOLLOW_Y) / HOLLOW_RY;
                    double edge = 1 - Math.sqrt(dx * dx + dy * dy);
                    if (edge <= 0) continue;
                    double a = fbm(tu / 7 + time, tv / 5 - time * 0.4, 4242);
                    double b = fbm(tu / 5 - time * 0.7, tv / 6 + time * 0.3, 977);
                    double dens = Math.min(1, edge * 1.8) * (0.55 + 0.6 * (a * 0.6 + b * 0.4));
                    int level = (int) Math.floor(Math.min(1, dens) * 4 + bayer(i, j) - 0.3);
                    if (level <= 0) continue;
                    int alpha = new int[] { 0, 110, 170, 215, 240 }[Math.min(4, level)];
                    // smoky violet, darker in the thick of it, lit red from inside where the two layers meet
                    int rgb = level >= 4 ? 0x1c1024 : (level == 3 ? 0x2e1c3a : (level == 2 ? 0x46305a : 0x5e4670));
                    double glow = a * b;
                    if (level >= 3 && glow > 0.40) rgb = glow > 0.48 ? 0xb4141e : 0x6a0e1e;
                    px[j * cw + i] = (alpha << 24) | rgb;
                }
            hollowClouds.setRGB(0, 0, cw, ch, px, 0, cw);
        }
        int sx = r.x + (int) (x0 * r.width / (double) t[0]), sy = r.y + (int) (y0 * r.height / (double) t[1]);
        int sw = (int) (cw / (double) sub * r.width / t[0]), sh = (int) (ch / (double) sub * r.height / t[1]);
        gh.getGraphics().drawImage(hollowClouds, sx, sy, sw, sh, null);
    }

    // ---- landmarks: portals swirl, the sealed Hollow watches, the Concord banner and the Kept stones appear once revealed
    private boolean revealed(String map, double tx, double ty) {
        for (NamedLocations.Place p : placesOf(map))
            if (playLevelScreen.getDiscoveredPlaces().contains(p) && Math.hypot(tx - p.tileX, ty - p.tileY) < p.radius + 18) return true;
        return false;
    }

    private void drawPois(GraphicsHandler gh, String map, java.awt.Rectangle r, java.awt.Rectangle[] placeLabels) {
        Graphics2D g = gh.getGraphics();
        int[] t = tiles(map);
        List<java.awt.Rectangle> used = new ArrayList<>(java.util.Arrays.asList(placeLabels));
        for (Poi poi : POIS) {
            if (!poi.map.equals(map) || (!poi.always && !revealed(map, poi.tx, poi.ty))) continue;
            int x = r.x + (int) ((poi.tx + 0.5) * r.width / t[0]), y = r.y + (int) ((poi.ty + 0.5) * r.height / t[1]);
            Color labelColor;
            switch (poi.kind) {
                case POI_PORTAL: { // a violet ring that turns
                    Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(150, 80, 220, 70));
                    g.fillOval(x - 9, y - 9, 18, 18);
                    g.setStroke(new BasicStroke(2));
                    int a = (frame * 6) % 360;
                    g.setColor(new Color(200, 140, 255));
                    g.drawArc(x - 7, y - 7, 14, 14, a, 220);
                    g.setColor(new Color(110, 50, 170));
                    g.drawArc(x - 4, y - 4, 8, 8, -a * 2, 200);
                    g.setStroke(new BasicStroke(1));
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : aa);
                    labelColor = new Color(200, 160, 240);
                    break;
                }
                case POI_HOLLOW: { // Dabbah's slit eye, the same one as the title-screen cursor, pulsing red and blinking
                    String[] eye = frame % 200 < 7 ? new String[] { ".......", ".......", "#######", ".......", "......." }
                            : new String[] { "..###..", ".##.##.", "###.###", ".##.##.", "..###.." };
                    int glow = 60 + (int) (40 * Math.sin(frame * 0.06));
                    g.setColor(new Color(160, 0, 0, glow));
                    g.fillOval(x - 16, y - 12, 32, 24);
                    for (int row = 0; row < 5; row++)
                        for (int col = 0; col < 7; col++)
                            if (eye[row].charAt(col) == '#') gh.drawFilledRectangle(x - 10 + col * 3, y - 7 + row * 3, 3, 3, new Color(230, 30, 20));
                    labelColor = new Color(230, 70, 60);
                    break;
                }
                case POI_CONCORD: { // a gold banner on a pole
                    gh.drawFilledRectangle(x - 1, y - 10, 2, 16, new Color(40, 30, 20));
                    gh.drawFilledRectangle(x + 1, y - 10, 8, 6, new Color(220, 190, 100));
                    gh.drawFilledRectangle(x + 3, y - 9, 2, 4, new Color(150, 30, 30)); // the red cross
                    gh.drawFilledRectangle(x + 2, y - 8, 4, 1, new Color(150, 30, 30));
                    labelColor = factionColor("CONCORD");
                    break;
                }
                default: { // three grey standing stones
                    for (int k = -1; k <= 1; k++) gh.drawFilledRectangle(x + k * 5 - 1, y - 6 + Math.abs(k) * 2, 3, 8 - Math.abs(k) * 2, new Color(150, 150, 140));
                    labelColor = factionColor("KEPT GROVE");
                    break;
                }
            }
            // a small label below, above, right or left of it: the first spot that covers no other name (none fits: no label)
            int tw = BattleUI.font().getTextWidth(poi.label, 1, 1) + 8;
            java.awt.Rectangle[] tries = { new java.awt.Rectangle(x - tw / 2, y + 12, tw, 13), new java.awt.Rectangle(x - tw / 2, y - 28, tw, 13),
                    new java.awt.Rectangle(x + 14, y - 6, tw, 13), new java.awt.Rectangle(x - 14 - tw, y - 6, tw, 13) };
            java.awt.Rectangle lr = null;
            for (java.awt.Rectangle tr : tries) {
                boolean clash = false;
                for (java.awt.Rectangle u : used) if (u.intersects(tr)) { clash = true; break; }
                if (!clash) { lr = tr; break; }
            }
            if (lr == null) continue;
            used.add(lr);
            gh.drawFilledRectangle(lr.x, lr.y, lr.width, lr.height, new Color(0, 0, 0, 150));
            BattleUI.font().draw(gh, poi.label, lr.x + 4, lr.y + 3, 1, 1, labelColor);
        }
    }

    private void drawWorldSwitcher(GraphicsHandler gh) {
        String w = world();
        String label = w.contains("2") ? "WORLD 2" : (w.contains("3") ? "WORLD 3" : "WORLD 1");
        String sub = theme(w).subtitle;
        java.awt.Rectangle l = arrowLeft(), rr = arrowRight();
        int mid = (l.x + l.width + rr.x) / 2;
        BattleUI.shadowText(gh, label, mid - BattleUI.textW(label, 3) / 2, 42, 3, BattleUI.WHITE);
        BattleUI.textCenter(gh, sub, mid, 70, 1, new Color(200, 80, 70));
        boolean many = worlds.size() > 1;
        drawArrowButton(gh, l, "Q", many, l.contains(MouseInput.getX(), MouseInput.getY()));
        drawArrowButton(gh, rr, "E", many, rr.contains(MouseInput.getX(), MouseInput.getY()));
    }

    private void drawArrowButton(GraphicsHandler gh, java.awt.Rectangle b, String key, boolean enabled, boolean hover) {
        gh.drawFilledRectangleWithBorder(b.x, b.y, b.width, b.height, hover && enabled ? new Color(90, 72, 40) : new Color(34, 28, 26),
                enabled ? FRAME : PIN_LOCKED, 2);
        BattleUI.textCenter(gh, key, b.x + b.width / 2, b.y + (b.height - BattleUI.font().getCapHeight(2)) / 2, 2,
                enabled ? NAME_FOUND : PIN_LOCKED);
    }

    // an iron frame with gold corner studs around the map
    private void drawMapFrame(GraphicsHandler gh, int x, int y, int w, int h) {
        gh.drawRectangle(x - 3, y - 3, w + 5, h + 5, IRON, 4);
        gh.drawRectangle(x - 6, y - 6, w + 11, h + 11, new Color(24, 20, 20), 2);
        int[][] corners = { { x - 8, y - 8 }, { x + w - 2, y - 8 }, { x - 8, y + h - 2 }, { x + w - 2, y + h - 2 } };
        for (int[] c : corners) {
            gh.drawFilledRectangleWithBorder(c[0], c[1], 10, 10, IRON, FRAME, 2);
            gh.drawFilledRectangle(c[0] + 4, c[1] + 4, 2, 2, FRAME);
        }
        for (int k = 1; k < 4; k++) { // studs along the long edges
            int sx = x + w * k / 4 - 2;
            gh.drawFilledRectangle(sx, y - 5, 4, 4, FRAME);
            gh.drawFilledRectangle(sx, y + h + 1, 4, 4, FRAME);
        }
    }

    // a small pixel compass rose
    private void drawCompass(GraphicsHandler gh, int cx, int cy) {
        Graphics2D g = gh.getGraphics();
        Color light = new Color(214, 178, 110, 200), dark = new Color(110, 84, 50, 200);
        g.setColor(new Color(0, 0, 0, 120));
        g.fillOval(cx - 15, cy - 15, 30, 30);
        g.setColor(light);
        g.fillPolygon(new int[] { cx, cx + 4, cx - 4 }, new int[] { cy - 14, cy, cy }, 3);
        g.setColor(dark);
        g.fillPolygon(new int[] { cx, cx + 4, cx - 4 }, new int[] { cy + 14, cy, cy }, 3);
        g.fillPolygon(new int[] { cx - 14, cx, cx }, new int[] { cy, cy - 4, cy + 4 }, 3);
        g.fillPolygon(new int[] { cx + 14, cx, cx }, new int[] { cy, cy - 4, cy + 4 }, 3);
        BattleUI.textCenter(gh, "N", cx, cy - 26, 1, light);
    }

    private BufferedImage makeVignette(int w, int h) {
        BufferedImage v = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = v.createGraphics();
        float r = (float) Math.hypot(w, h) * 0.55f;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h / 2f), r, new float[] { 0.55f, 1f },
                new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 170) }));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return v;
    }

    // the strip under the map: the selected place, what ENTER would do, its story line, faction and danger
    private void drawInfo(GraphicsHandler gh, List<NamedLocations.Place> ps, Set<NamedLocations.Place> found, NamedLocations.Place here) {
        int x = areaX(), y = infoY(), iw = lowW() * PX, ih = 66;
        gh.drawFilledRectangleWithBorder(x, y, iw, ih, new Color(24, 20, 20), IRON, 2);
        int count = 0;
        for (NamedLocations.Place p : ps) if (found.contains(p)) count++;
        String tally = "FOUND " + count + "/" + ps.size();
        BattleUI.textRight(gh, tally, x + iw - 14, y + 10, 2, BattleUI.GREY);
        if (ps.isEmpty() || selected >= ps.size()) return;

        NamedLocations.Place p = ps.get(selected);
        boolean isFound = found.contains(p), isHere = p == here;
        String[] lore = LORE.getOrDefault(p.name.toUpperCase(), new String[] { "", "" });
        BattleUI.shadowText(gh, p.name, x + 14, y + 9, 2, isHere ? NAME_HERE : (isFound ? NAME_FOUND : NAME_LOCKED));
        int nameW = BattleUI.textW(p.name, 2);
        if (isFound && !lore[1].isEmpty()) { // faction tag beside the name
            String tag = "[" + lore[1] + "]";
            BattleUI.text(gh, tag, x + 14 + nameW + 12, y + 13, 1, factionColor(lore[1]));
        }

        String status;
        Color sc;
        if (noteTimer > 0) { status = note; sc = BattleUI.RED; }
        else if (isHere) { status = "YOU ARE HERE"; sc = NAME_HERE; }
        else if (isFound) { status = "PRESS ENTER OR CLICK TO TRAVEL"; sc = BattleUI.GOLD; }
        else { status = "UNDISCOVERED - WALK THERE TO UNLOCK"; sc = NAME_LOCKED; }
        BattleUI.text(gh, status, x + 14, y + 33, 1, sc);
        String danger = p.safe ? "SAFE - NO ENEMIES" : dangerOf(p.map);
        BattleUI.textRight(gh, danger, x + iw - 14, y + 33, 1, p.safe ? NAME_HERE : new Color(200, 90, 80));

        String story = isFound ? lore[0] : "???";
        int maxChars = (iw - 28) / 7;
        if (story.length() > maxChars) story = story.substring(0, maxChars - 3) + "...";
        BattleUI.text(gh, story, x + 14, y + 49, 1, new Color(190, 176, 150));
    }

    // ================================================================== the real map, shrunk

    // one world's colour grading, picked from its era (see the design doc): famine dust, a green land being cut down, ash and plague
    private static final class Theme {
        int tint;          // colour the whole map is pulled toward
        double tintAmount; // how far (0..1)
        double darken;     // 1 = as is, lower = darker
        double desat;      // 0 = full colour, 1 = grey
        String subtitle;
    }

    private static Theme theme(String map) {
        Theme t = new Theme();
        if (map.contains("2")) { t.tint = 0x1c2a22; t.tintAmount = 0.18; t.darken = 0.82; t.desat = 0.20; t.subtitle = "THE LONG EROSION"; }
        else if (map.contains("3")) { t.tint = 0x2a2a34; t.tintAmount = 0.25; t.darken = 0.78; t.desat = 0.40; t.subtitle = "THE AGE OF WATCHING"; }
        else { t.tint = 0x3a2614; t.tintAmount = 0.22; t.darken = 0.80; t.desat = 0.35; t.subtitle = "THE RECKONING"; }
        return t;
    }

    private static final int SUB = 2; // map pixels per tile, each way (2 = every tile keeps a little of its own detail)

    private static final class Terrain {
        BufferedImage image; // SUB x SUB pixels per tile, with a torn, burnt edge cut out of it
        int tilesW, tilesH;
    }

    private Terrain terrain(String map) {
        Terrain t = terrainCache.get(map);
        if (t == null) {
            t = buildTerrain(map);
            terrainCache.put(map, t);
        }
        return t;
    }

    // reads every tile of the world's real map and shrinks its sprite (bottom layer + top layer) to SUB x SUB pixels
    private Terrain buildTerrain(String mapName) {
        Terrain tr = new Terrain();
        Level.Map m = null;
        try {
            m = playLevelScreen.getMapForOverview(mapName);
        } catch (RuntimeException e) {
            System.out.println("World map: could not load " + mapName + " (" + e + ")");
        }
        int[] fallback = MAP_TILES.getOrDefault(mapName, new int[] { 100, 100 });
        tr.tilesW = m != null ? m.getWidth() : fallback[0];
        tr.tilesH = m != null ? m.getHeight() : fallback[1];
        int w = tr.tilesW * SUB, h = tr.tilesH * SUB;
        int[] px = new int[w * h];
        Theme th = theme(mapName);
        java.util.Map<BufferedImage, int[]> cache = new java.util.IdentityHashMap<>();
        java.util.Map<Long, int[]> pairCache = new HashMap<>();
        for (int ty = 0; ty < tr.tilesH; ty++)
            for (int tx = 0; tx < tr.tilesW; tx++) {
                Level.MapTile tile = m != null ? m.getMapTile(tx, ty) : null;
                int[] cells = tile == null ? null : tileColours(tile, cache, pairCache);
                for (int sy = 0; sy < SUB; sy++)
                    for (int sx = 0; sx < SUB; sx++) {
                        int c = cells == null ? 0x0c0a0c : cells[sy * SUB + sx];
                        px[(ty * SUB + sy) * w + tx * SUB + sx] = 0xff000000 | grade(c, th);
                    }
            }
        tearEdges(px, w, h, mapName.hashCode());
        tr.image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        tr.image.setRGB(0, 0, w, h, px, 0, w);
        return tr;
    }

    // the SUB x SUB average colours of one tile (its top layer painted over its bottom layer)
    private int[] tileColours(Level.MapTile tile, java.util.Map<BufferedImage, int[]> cache, java.util.Map<Long, int[]> pairCache) {
        BufferedImage bottom = frameImage(tile.getBottomLayer()), top = frameImage(tile.getTopLayer());
        if (bottom == null) return null;
        long key = ((long) System.identityHashCode(bottom) << 32) ^ (top == null ? 0 : System.identityHashCode(top));
        int[] hit = pairCache.get(key);
        if (hit != null) return hit;
        int[] b = averages(bottom, cache), t = top == null ? null : averages(top, cache);
        int[] out = new int[SUB * SUB];
        for (int k = 0; k < out.length; k++) {
            if (t == null || (t[k] >>> 24) == 0) { out[k] = b[k] & 0xffffff; continue; }
            double a = (t[k] >>> 24) / 255.0; // how much of that cell the top layer covers
            out[k] = mix(b[k] & 0xffffff, t[k] & 0xffffff, a);
        }
        pairCache.put(key, out);
        return out;
    }

    private static BufferedImage frameImage(GameObject.GameObject o) {
        if (o == null) return null;
        try {
            GameObject.Frame[] anim = o.getCurrentAnimation();
            return anim == null || anim.length == 0 ? null : anim[0].getImage();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // average colour of each SUB x SUB cell of an image, skipping see-through and magenta (transparent-key) pixels.
    // The alpha byte of each result says how much of the cell was solid.
    private static int[] averages(BufferedImage img, java.util.Map<BufferedImage, int[]> cache) {
        int[] c = cache.get(img);
        if (c != null) return c;
        c = new int[SUB * SUB];
        int iw = img.getWidth(), ih = img.getHeight();
        for (int cy = 0; cy < SUB; cy++)
            for (int cx = 0; cx < SUB; cx++) {
                long r = 0, g = 0, b = 0;
                int n = 0, total = 0;
                for (int y = cy * ih / SUB; y < (cy + 1) * ih / SUB; y++)
                    for (int x = cx * iw / SUB; x < (cx + 1) * iw / SUB; x++) {
                        total++;
                        int p = img.getRGB(x, y);
                        if ((p >>> 24) < 128 || (p & 0xffffff) == 0xff00ff) continue;
                        r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255; n++;
                    }
                c[cy * SUB + cx] = n == 0 ? 0 : ((n * 255 / Math.max(1, total)) << 24) | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
            }
        cache.put(img, c);
        return c;
    }

    // pulls a colour toward the world's era: less colour, a tint, darker
    private static int grade(int rgb, Theme th) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        int grey = (r * 30 + g * 59 + b * 11) / 100;
        r += (int) ((grey - r) * th.desat); g += (int) ((grey - g) * th.desat); b += (int) ((grey - b) * th.desat);
        int c = mix((r << 16) | (g << 8) | b, th.tint, th.tintAmount);
        return shade(c, th.darken - 1);
    }

    // the map's border is torn and burnt instead of a straight edge: some pixels are cut away, the rim smoulders
    private static void tearEdges(int[] px, int w, int h, int seed) {
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int d = Math.min(Math.min(x, w - 1 - x), Math.min(y, h - 1 - y));
                if (d > 22) continue;
                double edge = 3 + fbm(x / 9.0, y / 9.0, seed) * 16 + fbm(x / 3.0, y / 3.0, seed + 7) * 4;
                int k = y * w + x;
                if (d < edge) px[k] = 0;                                                     // torn away
                else if (d < edge + 1.5) px[k] = hash(x, y, seed) > 0.75 ? 0xffb4501e : 0xff3a1a10; // embers on the rim
                else if (d < edge + 4) px[k] = 0xff000000 | shade(px[k] & 0xffffff, -0.45);   // scorched
            }
    }

    private static int mix(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // fog of war, one pixel per tile: dark cloud everywhere except around the places the knight has found
    private BufferedImage buildFog(String map) {
        int[] t = tiles(map);
        int tw = t[0], th = t[1];
        List<NamedLocations.Place> found = new ArrayList<>();
        for (NamedLocations.Place p : placesOf(map)) if (playLevelScreen.getDiscoveredPlaces().contains(p)) found.add(p);
        int seed = map.hashCode() + 909;
        int[] px = new int[tw * th];
        int[] alphas = { 0, 60, 115, 160, 195 }; // the deepest fog still lets the coastline show through
        for (int j = 0; j < th; j++)
            for (int i = 0; i < tw; i++) {
                double tu = i + 0.5, tv = j + 0.5;
                double reveal = 0;
                for (NamedLocations.Place p : found) {
                    double d = Math.hypot(tu - p.tileX, tv - p.tileY), inner = p.radius + 10;
                    reveal = Math.max(reveal, Math.min(1, Math.max(0, (inner + 16 - d) / 16)));
                }
                double f = (1 - reveal) * (0.7 + 0.45 * fbm(tu / 9, tv / 9, seed));
                int level = (int) Math.floor(Math.min(1, f) * 4 + bayer(i, j) - 0.25);
                level = Math.max(0, Math.min(4, level));
                px[j * tw + i] = (alphas[level] << 24) | 0x0c0a10;
            }
        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, tw, th, px, 0, tw);
        return img;
    }

    // ================================================================== noise helpers

    private static final double[] BAYER = { 0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5 };

    // ordered dither value 0..1 for a pixel (gives the hand-pixelled, checkered blend between two colors)
    private static double bayer(int x, int y) {
        return BAYER[(y & 3) * 4 + (x & 3)] / 16.0;
    }

    private static double hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0x7fffffff) / (double) 0x7fffffff;
    }

    private static double noise(double x, double y, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        double u = fx * fx * (3 - 2 * fx), v = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi, seed), b = hash(xi + 1, yi, seed), c = hash(xi, yi + 1, seed), d = hash(xi + 1, yi + 1, seed);
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
    }

    private static double fbm(double x, double y, int seed) {
        double s = 0, amp = 0.5, norm = 0;
        for (int o = 0; o < 4; o++) {
            s += amp * noise(x, y, seed + o * 31);
            norm += amp;
            x *= 2.03;
            y *= 2.03;
            amp *= 0.5;
        }
        return s / norm;
    }

    private static int shade(int rgb, double f) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        double m = 1 + f;
        r = (int) Math.max(0, Math.min(255, r * m));
        g = (int) Math.max(0, Math.min(255, g * m));
        b = (int) Math.max(0, Math.min(255, b * m));
        return (r << 16) | (g << 8) | b;
    }
}