package EnhancedMapTiles;

import Builders.FrameBuilder;
import Engine.ImageLoader;
import GameObject.Frame;
import GameObject.GameObject;
import GameObject.SpriteSheet;
import Level.EnhancedMapTile;
import Level.TileType;
import Utils.Point;


public class Sign extends EnhancedMapTile {


    // imageFileName: Sign.png

    public Sign(Point location, String imageFileName, int width, int height) {
        this(location, imageFileName, width, height, 1f);
    }

    // Changes Size
    public Sign(Point location, String imageFileName, int width, int height, float tiles) {
        super(location.x, location.y, new SpriteSheet(ImageLoader.load(imageFileName), width, height), TileType.NOT_PASSABLE);
        SpriteSheet spriteSheet = new SpriteSheet(ImageLoader.load(imageFileName), width, height);
        setBottomLayer(buildImage(spriteSheet, tiles));
    }

    @Override
    protected GameObject loadBottomLayer(SpriteSheet spriteSheet) {
        return buildImage(spriteSheet, 1f);
    }


    private GameObject buildImage(SpriteSheet spriteSheet, float tiles) {
        float scale = (48f * tiles) / spriteSheet.getSpriteWidth();
        Frame frame = new FrameBuilder(spriteSheet.getSprite(0, 0)).withScale(scale).build();
        return new GameObject(x, y, frame);
    }
}
