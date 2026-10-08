package EnhancedMapTiles;

import Builders.FrameBuilder;
import GameObject.GameObject;
import GameObject.SpriteSheet;
import GameObject.Frame;
import Item.Item;
import Level.EnhancedMapTile;
import Level.TileType;
import Utils.Point;

public class ItemMapTile extends EnhancedMapTile {
    private final Item item;

    public ItemMapTile(Point location, Item item) {
        super(location.x, location.y, item.getItemLook(), TileType.PASSABLE);
        this.item = item;
    }

    @Override
    protected GameObject loadBottomLayer(SpriteSheet spriteSheet) {
        Frame frame = new FrameBuilder(spriteSheet.getSubImage(0,0)).withScale(3).build();
        return new GameObject(x, y, frame);
    }
}
