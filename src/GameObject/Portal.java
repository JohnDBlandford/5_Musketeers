package GameObject;

import Level.EnhancedMapTile;
import Level.Player;
import Level.TileType;

// A map entity that can be used to move the player to another map.
//

public class Portal extends EnhancedMapTile {
    private final String targetMap;
    private final float targetX;
    private final float targetY;
    private boolean interactionArmed = true;

    public Portal(float x, float y, String targetMap, float targetX, float targetY, int width, int height) {
        super(x, y, new GameObject(x, y), null, TileType.PASSABLE);
        this.targetMap = targetMap;
        this.targetX = targetX;
        this.targetY = targetY;
        this.setIsUncollidable(true);
        this.getBottomLayer().setWidth(width);
        this.getBottomLayer().setHeight(height);
    }

    public Portal(float x, float y, SpriteSheet spriteSheet, String startingAnimation, String targetMap,
            float targetX, float targetY) {
        this(x, y, targetMap, targetX, targetY, 48, 48);
    }

    public String getTargetMap() {
        return targetMap;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public boolean isPlayerInPortal(Player player) {
        return intersects(player.getBounds());
    }

    @Override
    public void update(Player player) {
        super.update(player);
        if (!isPlayerInPortal(player)) {
            interactionArmed = true;
        } else if (interactionArmed && map.getActiveScript() == null && getInteractScript() != null) {
            interactionArmed = false;
            map.setActiveScript(getInteractScript());
        }
    }
}
