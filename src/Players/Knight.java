package Players;

import Builders.FrameBuilder;
import Engine.GraphicsHandler;
import Engine.ImageLoader;
import GameObject.Frame;
import GameObject.ImageEffect;
import GameObject.SpriteSheet;
import Level.Player;
import java.util.HashMap;
import java.awt.image.BufferedImage;

// This is the class for the Knight player character
// Defines animations based on your precise 3x4 spritesheet layout configuration
public class Knight extends Player {

    public Knight(float x, float y) {
        // Starts with STAND_RIGHT so the base Player class initializes safely
        super(new SpriteSheet(ImageLoader.load("Spritesheet.png"), 32, 32), x, y, "STAND_RIGHT");
        walkSpeed = 2.3f;
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void draw(GraphicsHandler graphicsHandler) {
        super.draw(graphicsHandler);
    }

    // Explicitly slices the image sub-raster mathematically by Column and Row
    // indices (0-indexed)
    private BufferedImage getSpriteAtCell(SpriteSheet spriteSheet, int col, int row) {
        int x = col * 32;
        int y = row * 32;
        return spriteSheet.getImage().getSubimage(x, y, 32, 32);
    }

    @Override
    public HashMap<String, Frame[]> loadAnimations(SpriteSheet spriteSheet) {
        return new HashMap<String, Frame[]>() {
            {

                // First sprite: facing forward (Row 0, Column 0)
                // We map this to the base engine's expected default keys to prevent the crash
                put("STAND_FORWARD", new Frame[] {
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 0, 0)).withScale(2).withBounds(6, 12, 12, 7)
                                .build()
                });

                put("STAND_RIGHT", new Frame[] {
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 0, 0)).withScale(2).withBounds(6, 12, 12, 7)
                                .build()
                });

                put("STAND_LEFT", new Frame[] {
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 0, 0)).withScale(2)
                                .withImageEffect(ImageEffect.FLIP_HORIZONTAL).withBounds(6, 12, 12, 7).build()
                });

                // Hurt animations: 2 next to forward (Row 0, Col 1 & 2), and 1 below it (Row 1,
                // Col 0)
                put("HURT", new Frame[] {
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 1, 0), 10).withScale(2).withBounds(6, 12, 12, 7)
                                .build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 2, 0), 10).withScale(2).withBounds(6, 12, 12, 7)
                                .build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 0, 1), 10).withScale(2).withBounds(6, 12, 12, 7)
                                .build()
                });

                // Attack Right: 2 remaining on row 1 (Col 1 & 2), and the first 2 on row 2 (Col
                // 0 & 1)
                put("ATTACK_RIGHT", new Frame[] {
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 1, 1), 8).withScale(2).withBounds(6, 12, 12, 7)
                                .build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 2, 1), 8).withScale(2).withBounds(6, 12, 12, 7)
                                .build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 0, 2), 8).withScale(2).withBounds(6, 12, 12, 7)
                                .build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 1, 2), 8).withScale(2).withBounds(6, 12, 12, 7)
                                .build()
                });

                // Attack Left: Same attack frames, flipped horizontally
                put("ATTACK_LEFT", new Frame[] {
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 1, 1), 8).withScale(2)
                                .withImageEffect(ImageEffect.FLIP_HORIZONTAL).withBounds(6, 12, 12, 7).build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 2, 1), 8).withScale(2)
                                .withImageEffect(ImageEffect.FLIP_HORIZONTAL).withBounds(6, 12, 12, 7).build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 0, 2), 8).withScale(2)
                                .withImageEffect(ImageEffect.FLIP_HORIZONTAL).withBounds(6, 12, 12, 7).build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 1, 2), 8).withScale(2)
                                .withImageEffect(ImageEffect.FLIP_HORIZONTAL).withBounds(6, 12, 12, 7).build()
                });

                // Walking right: The remaining sprites (Row 2 Col 2, and all of Row 3)
                put("WALK_RIGHT", new Frame[] {
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 2, 2), 14).withScale(2).withBounds(6, 12, 12, 7)
                                .build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 0, 3), 14).withScale(2).withBounds(6, 12, 12, 7)
                                .build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 1, 3), 14).withScale(2).withBounds(6, 12, 12, 7)
                                .build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 2, 3), 14).withScale(2).withBounds(6, 12, 12, 7)
                                .build()
                });

                // Walking left: Same remaining sprites, flipped horizontally
                put("WALK_LEFT", new Frame[] {
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 2, 2), 14).withScale(2)
                                .withImageEffect(ImageEffect.FLIP_HORIZONTAL).withBounds(6, 12, 12, 7).build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 0, 3), 14).withScale(2)
                                .withImageEffect(ImageEffect.FLIP_HORIZONTAL).withBounds(6, 12, 12, 7).build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 1, 3), 14).withScale(2)
                                .withImageEffect(ImageEffect.FLIP_HORIZONTAL).withBounds(6, 12, 12, 7).build(),
                        new FrameBuilder(getSpriteAtCell(spriteSheet, 2, 3), 14).withScale(2)
                                .withImageEffect(ImageEffect.FLIP_HORIZONTAL).withBounds(6, 12, 12, 7).build()
                });

            }
        };
    }
}