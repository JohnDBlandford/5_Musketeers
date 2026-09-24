package NPCs;

import Builders.FrameBuilder;
import Engine.GraphicsHandler;
import Engine.ImageLoader;
import GameObject.Frame;
import GameObject.SpriteSheet;
import Level.NPC;
import Level.Player;
import Utils.Point;
import Engine.Keyboard;
import Engine.Key;
import java.util.HashMap;

public class Dummy extends NPC {

    protected boolean isHurt = false;
    protected int hurtTimer = 0;
    protected final int HURT_DURATION = 30; // Shorter duration to match the compact frame count

    public Dummy(int id, Point location) {
        super(id, location.x, location.y, new SpriteSheet(ImageLoader.load("Dummy.png"), 32, 32), "IDLE");
    }

    @Override
    public HashMap<String, Frame[]> loadAnimations(SpriteSheet spriteSheet) {
        return new HashMap<String, Frame[]>() {{
            put("IDLE", new Frame[] {
                // Row 0: Safe Columns (0, 1, 2)
                new FrameBuilder(spriteSheet.getSprite(0, 0), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                new FrameBuilder(spriteSheet.getSprite(0, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                new FrameBuilder(spriteSheet.getSprite(0, 2), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                new FrameBuilder(spriteSheet.getSprite(0, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                
                // Row 1: Safe Columns (0, 1, 2)
                new FrameBuilder(spriteSheet.getSprite(1, 0), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                new FrameBuilder(spriteSheet.getSprite(1, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                new FrameBuilder(spriteSheet.getSprite(1, 2), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                
                // Row 2: Safe Column (0)
                new FrameBuilder(spriteSheet.getSprite(2, 0), 15).withScale(3).withBounds(4, 4, 16, 20).build()
            });

            put("HURT", new Frame[] {
                // Row 2: Safe Columns (1, 2)
                new FrameBuilder(spriteSheet.getSprite(2, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                new FrameBuilder(spriteSheet.getSprite(2, 2), 15).withScale(3).withBounds(4, 4, 16, 20).build()
            });
        }};
    }

    @Override
    public void update(Player player) {
        super.update(player);

        if (Keyboard.isKeyDown(Key.SPACE) && !isHurt) {
            isHurt = true;
            hurtTimer = HURT_DURATION;
            this.currentAnimationName = "HURT";
        }

        if (isHurt) {
            hurtTimer--;
            if (hurtTimer <= 0) {
                isHurt = false;
                this.currentAnimationName = "IDLE";
            }
        }
    }

    @Override
    public void draw(GraphicsHandler graphicsHandler) {
        super.draw(graphicsHandler);
    }
}
