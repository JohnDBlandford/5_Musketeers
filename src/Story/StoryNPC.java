package Story;

import Builders.FrameBuilder;
import Engine.GraphicsHandler;
import GameObject.Frame;
import GameObject.ImageEffect;
import Level.Map;
import Level.MapTile;
import Level.NPC;
import Level.TileType;
import BattleSystem.BattleUI;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.function.BooleanSupplier;

// STORY: a World 1 story character or object on the map. It is drawn from NPCArt (no image files), stands on the nearest
// walkable tile to where it is placed, shows up only while its condition holds (exists() asks the story, so a character
// can appear, move on or die as the story goes), and talking to it opens its conversation (TalkScript -> StoryDirector).
// A "!" floats over it when it has a new quest, a "?" when it is part of the quest you are on: RED for a main quest,
// BLUE for a side quest.
public class StoryNPC extends NPC {
    // quest marker colours: RED = main quest, BLUE = side quest ("!" = new quest, "?" = part of a quest you are on)
    public static final Color MAIN_COLOR = new Color(235, 60, 50), SIDE_COLOR = new Color(80, 150, 245);

    public final String storyId;
    public final NPCArt.Look look;
    private final BooleanSupplier visible;
    private int tick;

    // tileX / tileY: where it should stand (it moves to the nearest walkable tile). visible: null = always there
    public StoryNPC(int id, String storyId, NPCArt.Look look, Map map, int tileX, int tileY, BooleanSupplier visible) {
        super(id, spot(map, tileX, tileY, look)[0], spot(map, tileX, tileY, look)[1], animations(look), "STAND_LEFT");
        this.storyId = storyId;
        this.look = look;
        this.visible = visible;
        setInteractScript(new TalkScript(storyId));
    }

    @Override
    public boolean exists() {
        return visible == null || visible.getAsBoolean();
    }

    @Override
    public void update(Level.Player player) {
        tick++;
        super.update(player);
    }

    @Override
    public void draw(GraphicsHandler gh) {
        super.draw(gh);
        // the quest marker, bobbing over its head
        String m = StoryDirector.markerFor(storyId);
        if (m == null) return;
        char marker = m.charAt(0);
        Quest q = Quest.get(m.substring(1));
        boolean main = q == null || q.main;   // the Kept cache's tablet piece counts as main story
        int x = Math.round(getCalibratedXLocation()) + getWidth() / 2, y = Math.round(getCalibratedYLocation()) - 24;
        y += (int) Math.round(Math.sin(tick * 0.08) * 3);
        Color c = main ? MAIN_COLOR : SIDE_COLOR;
        gh.drawFilledRectangle(x - 11, y - 5, 22, 24, new Color(10, 8, 10, 190));
        gh.drawRectangle(x - 11, y - 5, 22, 24, c, 2);
        BattleUI.textCenter(gh, String.valueOf(marker), x + 1, y + 1, 2, new Color(20, 14, 10));
        BattleUI.textCenter(gh, String.valueOf(marker), x, y, 2, c);
    }

    // ---------------------------------------------------------------- building it

    // the nearest walkable tile to (tx, ty): the sprite's top-left in pixels, its feet on that tile, centred on it
    private static float[] spot(Map map, int tx, int ty, NPCArt.Look look) {
        BufferedImage img = NPCArt.portrait(look);
        float ox = 24 - img.getWidth() * 3 / 2f, oy = 48 - img.getHeight() * 3;
        for (int r = 0; r <= 6; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    MapTile t = map.getMapTile(tx + dx, ty + dy);
                    if (t != null && t.getTileType() != TileType.NOT_PASSABLE) return new float[]{ t.getX() + ox, t.getY() + oy };
                }
        return new float[]{ tx * 48f + ox, ty * 48f + oy };
    }

    // STAND_LEFT / STAND_RIGHT (the engine's facePlayer uses these), each a slow two-frame breathing loop
    private static HashMap<String, Frame[]> animations(NPCArt.Look look) {
        BufferedImage[] f = NPCArt.frames(look);
        int w = f[0].getWidth(), h = f[0].getHeight();
        // collision box: the feet (bottom third), so the knight can walk right up to them
        int bx = 2, bw = w - 4, bh = Math.max(4, h / 3), by = h - bh;
        HashMap<String, Frame[]> map = new HashMap<>();
        map.put("STAND_RIGHT", new Frame[]{
                new FrameBuilder(f[0], 40).withScale(3).withBounds(bx, by, bw, bh).build(),
                new FrameBuilder(f[1], 40).withScale(3).withBounds(bx, by, bw, bh).build() });
        map.put("STAND_LEFT", new Frame[]{
                new FrameBuilder(f[0], 40).withScale(3).withBounds(bx, by, bw, bh).withImageEffect(ImageEffect.FLIP_HORIZONTAL).build(),
                new FrameBuilder(f[1], 40).withScale(3).withBounds(bx, by, bw, bh).withImageEffect(ImageEffect.FLIP_HORIZONTAL).build() });
        return map;
    }
}