package Level;

import java.awt.Color;

import Engine.CombatResolver;
import Engine.GraphicsHandler;
import Engine.Key;
import Engine.KeyLocker;
import Engine.Keyboard;
import GameObject.GameObject;
import GameObject.Rectangle;
import GameObject.SpriteSheet;
import NPCs.Enemy;
import Utils.Direction;
import Game.PlayerData;

import java.util.ArrayList;

import Lighting.BackgroundOpacity;

public abstract class Player extends GameObject {
    // values that affect player movement
    protected float walkSpeed = 0;
    protected int interactionRange = 1;
    protected Direction currentWalkingXDirection;
    protected Direction currentWalkingYDirection;
    protected Direction lastWalkingXDirection;
    protected Direction lastWalkingYDirection;

    // values used to handle player movement
    protected float moveAmountX, moveAmountY;
    protected float lastAmountMovedX, lastAmountMovedY;

    // values used to keep track of player's current state
    protected PlayerState playerState;
    protected PlayerState previousPlayerState;
    protected Direction facingDirection;
    protected Direction lastMovementDirection;

    // define keys
    protected KeyLocker keyLocker = new KeyLocker();
    protected Key MOVE_LEFT_KEY = Key.A;
    protected Key MOVE_RIGHT_KEY = Key.D;
    protected Key MOVE_UP_KEY = Key.W;
    protected Key MOVE_DOWN_KEY = Key.S;
    protected Key INTERACT_KEY = Key.ENTER;

    // Attack keys for combat testing
    protected Key ATTACK_MELEE_KEY = Key.G;
    protected Key ATTACK_RANGED_KEY = Key.H;

    protected boolean isLocked = false;

    // Player Level & Stats (Currency is now handled globally via PlayerData)
    protected int level = 1;
    protected int currentXP = 0;
    protected int xpToNextLevel = 100;

    // Player Combat Stats
    protected float maxHP = 100.0f;
    protected float currentHP = 100.0f;
    protected float baseATK = 15.0f;
    protected float baseDEF = 10.0f;
    protected float baseSPD = 25.0f;

    // Stat Modifiers
    protected float atkModifier = 1.0f;
    protected float defModifier = 1.0f;
    protected float spdModifier = 1.0f;

    public Player(SpriteSheet spriteSheet, float x, float y, String startingAnimationName) {
        super(spriteSheet, x, y, startingAnimationName);
        facingDirection = Direction.RIGHT;
        playerState = PlayerState.STANDING;
        previousPlayerState = playerState;
        this.affectedByTriggers = true;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void update() {
        if (!isLocked) {
            moveAmountX = 0;
            moveAmountY = 0;

            do {
                previousPlayerState = playerState;
                handlePlayerState();
            } while (previousPlayerState != playerState);

            handleCombatInputs();

            lastAmountMovedY = super.moveYHandleCollision(moveAmountY);
            lastAmountMovedX = super.moveXHandleCollision(moveAmountX);
        }

        handlePlayerAnimation();
        updateLockedKeys();
        super.update();

    }

    public void draw(GraphicsHandler graphicsHandler) {
        super.draw(graphicsHandler);
        // Center of the scaled sprite on screen
        float centerX = getCalibratedXLocation() + getWidth() / 2f;
        float centerY = getCalibratedYLocation() + getHeight() / 2f;
        new BackgroundOpacity().draw(graphicsHandler, centerX, centerY);
    }

    protected void handlePlayerState() {
        switch (playerState) {
            case STANDING:
                playerStanding();
                break;
            case WALKING:
                playerWalking();
                break;
        }
    }

    protected void playerStanding() {
        if (!keyLocker.isKeyLocked(INTERACT_KEY) && Keyboard.isKeyDown(INTERACT_KEY)) {
            keyLocker.lockKey(INTERACT_KEY);
            map.entityInteract(this);
        }

        if (Keyboard.isKeyDown(MOVE_LEFT_KEY) || Keyboard.isKeyDown(MOVE_RIGHT_KEY) || Keyboard.isKeyDown(MOVE_UP_KEY)
                || Keyboard.isKeyDown(MOVE_DOWN_KEY)) {
            playerState = PlayerState.WALKING;
        }
    }

    protected void playerWalking() {
        if (!keyLocker.isKeyLocked(INTERACT_KEY) && Keyboard.isKeyDown(INTERACT_KEY)) {
            keyLocker.lockKey(INTERACT_KEY);
            map.entityInteract(this);
        }

        if (Keyboard.isKeyDown(MOVE_LEFT_KEY)) {
            moveAmountX -= walkSpeed;
            facingDirection = Direction.LEFT;
            currentWalkingXDirection = Direction.LEFT;
            lastWalkingXDirection = Direction.LEFT;
        } else if (Keyboard.isKeyDown(MOVE_RIGHT_KEY)) {
            moveAmountX += walkSpeed;
            facingDirection = Direction.RIGHT;
            currentWalkingXDirection = Direction.RIGHT;
            lastWalkingXDirection = Direction.RIGHT;
        } else {
            currentWalkingXDirection = Direction.NONE;
        }

        if (Keyboard.isKeyDown(MOVE_UP_KEY)) {
            moveAmountY -= walkSpeed;
            currentWalkingYDirection = Direction.UP;
            lastWalkingYDirection = Direction.UP;
        } else if (Keyboard.isKeyDown(MOVE_DOWN_KEY)) {
            moveAmountY += walkSpeed;
            currentWalkingYDirection = Direction.DOWN;
            lastWalkingYDirection = Direction.DOWN;
        } else {
            currentWalkingYDirection = Direction.NONE;
        }

        if ((currentWalkingXDirection == Direction.RIGHT || currentWalkingXDirection == Direction.LEFT)
                && currentWalkingYDirection == Direction.NONE) {
            lastWalkingYDirection = Direction.NONE;
        }

        if ((currentWalkingYDirection == Direction.UP || currentWalkingYDirection == Direction.DOWN)
                && currentWalkingXDirection == Direction.NONE) {
            lastWalkingXDirection = Direction.NONE;
        }

        if (Keyboard.isKeyUp(MOVE_LEFT_KEY) && Keyboard.isKeyUp(MOVE_RIGHT_KEY) && Keyboard.isKeyUp(MOVE_UP_KEY)
                && Keyboard.isKeyUp(MOVE_DOWN_KEY)) {
            playerState = PlayerState.STANDING;
        }
    }

    protected void handleCombatInputs() {
        if (map == null)
            return;

        // Melee Attack (G Key)
        if (!keyLocker.isKeyLocked(ATTACK_MELEE_KEY) && Keyboard.isKeyDown(ATTACK_MELEE_KEY)) {
            keyLocker.lockKey(ATTACK_MELEE_KEY);
            Enemy target = findNearestEnemy(120.0f);
            if (target != null) {
                CombatResolver.resolvePlayerAttack(this, target, 1.0f);
            }
        }

        // Ranged Attack (H Key)
        if (!keyLocker.isKeyLocked(ATTACK_RANGED_KEY) && Keyboard.isKeyDown(ATTACK_RANGED_KEY)) {
            keyLocker.lockKey(ATTACK_RANGED_KEY);
            Enemy target = findNearestEnemy(300.0f);
            if (target != null) {
                CombatResolver.resolvePlayerAttack(this, target, 0.8f);
            }
        }
    }

    private Enemy findNearestEnemy(float maxDistance) {
        ArrayList<NPC> npcs = map.getNPCs();
        Enemy closestEnemy = null;
        float minDistance = maxDistance;

        for (NPC npc : npcs) {
            if (npc instanceof Enemy) {
                Enemy enemy = (Enemy) npc;
                float distance = (float) Math.hypot(this.x - enemy.getX(), this.y - enemy.getY());
                if (distance < minDistance) {
                    minDistance = distance;
                    closestEnemy = enemy;
                }
            }
        }
        return closestEnemy;
    }

    protected void updateLockedKeys() {
        if (Keyboard.isKeyUp(INTERACT_KEY) && !isLocked) {
            keyLocker.unlockKey(INTERACT_KEY);
        }
        if (Keyboard.isKeyUp(ATTACK_MELEE_KEY) && !isLocked) {
            keyLocker.unlockKey(ATTACK_MELEE_KEY);
        }
        if (Keyboard.isKeyUp(ATTACK_RANGED_KEY) && !isLocked) {
            keyLocker.unlockKey(ATTACK_RANGED_KEY);
        }
    }

    // Progression & Stats Logic

    // Updates global currency
    public void addGold(int amount) {
        PlayerData.addCurrency(amount);
    }

    public void addXP(int amount) {
        if (level >= 100) {
            this.currentXP = 0;
            return;
        }

        this.currentXP += amount;
        while (currentXP >= xpToNextLevel && level < 100) {
            levelUp();
        }

        if (level >= 100) {
            this.currentXP = 0;
        }
    }

    private void levelUp() {
        if (level >= 100)
            return;

        currentXP -= xpToNextLevel;
        level++;

        if (level >= 100) {
            xpToNextLevel = 0;
            currentXP = 0;
        } else {
            xpToNextLevel = level * 100;
        }

        maxHP += 15.0f;
        currentHP = maxHP;
        baseATK += 3.5f;
        baseDEF += 2.0f;
        baseSPD += 1.5f;
    }

    public void takeDamage(float damageAmount) {
        float finalDamage = CombatResolver.calculateDamage(damageAmount, getBaseDEF());
        applyDamage(finalDamage);
    }

    public void applyDamage(float damageAmount) {
        this.currentHP = Math.max(0.0f, this.currentHP - damageAmount);
    }

    // Stat Altering Methods
    public void alterATKModifier(float amount) {
        this.atkModifier = Math.max(0.2f, this.atkModifier + amount);
    }

    public void alterDEFModifier(float amount) {
        this.defModifier = Math.max(0.2f, this.defModifier + amount);
    }

    public void alterSPDModifier(float amount) {
        this.spdModifier = Math.max(0.2f, this.spdModifier + amount);
    }

    public void resetStatModifiers() {
        this.atkModifier = 1.0f;
        this.defModifier = 1.0f;
        this.spdModifier = 1.0f;
    }

    protected void handlePlayerAnimation() {
        if (playerState == PlayerState.STANDING) {
            this.currentAnimationName = facingDirection == Direction.RIGHT ? "STAND_RIGHT" : "STAND_LEFT";
        } else if (playerState == PlayerState.WALKING) {
            this.currentAnimationName = facingDirection == Direction.RIGHT ? "WALK_RIGHT" : "WALK_LEFT";
        }
    }

    @Override
    public void onEndCollisionCheckX(boolean hasCollided, Direction direction, GameObject entityCollidedWith) {
    }

    @Override
    public void onEndCollisionCheckY(boolean hasCollided, Direction direction, GameObject entityCollidedWith) {
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
    }

    public Direction getFacingDirection() {
        return facingDirection;
    }

    public void setFacingDirection(Direction facingDirection) {
        this.facingDirection = facingDirection;
    }

    public Rectangle getInteractionRange() {
        return new Rectangle(
                getBounds().getX1() - interactionRange,
                getBounds().getY1() - interactionRange,
                getBounds().getWidth() + (interactionRange * 2),
                getBounds().getHeight() + (interactionRange * 2));
    }

    public Key getInteractKey() {
        return INTERACT_KEY;
    }

    public Direction getCurrentWalkingXDirection() {
        return currentWalkingXDirection;
    }

    public Direction getCurrentWalkingYDirection() {
        return currentWalkingYDirection;
    }

    public Direction getLastWalkingXDirection() {
        return lastWalkingXDirection;
    }

    public Direction getLastWalkingYDirection() {
        return lastWalkingYDirection;
    }

    // Getters & Setters
    public int getLevel() {
        return level;
    }

    public int getCurrentXP() {
        return currentXP;
    }

    public int getXpToNextLevel() {
        return xpToNextLevel;
    }

    // Reads from global currency
    public int getGold() {
        return PlayerData.getCurrency();
    }

    public float getMaxHP() {
        return maxHP;
    }

    public float getCurrentHP() {
        return currentHP;
    }

    public float getBaseATK() {
        return baseATK * atkModifier;
    }

    public float getBaseDEF() {
        return baseDEF * defModifier;
    }

    public float getBaseSPD() {
        return baseSPD * spdModifier;
    }

    public float getAtkModifier() {
        return atkModifier;
    }

    public float getDefModifier() {
        return defModifier;
    }

    public float getSpdModifier() {
        return spdModifier;
    }

    public void setCurrentHP(float health) {
        this.currentHP = Math.max(0.0f, Math.min(health, maxHP));
    }

    public void lock() {
        isLocked = true;
        playerState = PlayerState.STANDING;
        this.currentAnimationName = facingDirection == Direction.RIGHT ? "STAND_RIGHT" : "STAND_LEFT";
    }

    public void unlock() {
        isLocked = false;
        playerState = PlayerState.STANDING;
        this.currentAnimationName = facingDirection == Direction.RIGHT ? "STAND_RIGHT" : "STAND_LEFT";
    }

    public void stand(Direction direction) {
        playerState = PlayerState.STANDING;
        facingDirection = direction;
        if (direction == Direction.RIGHT) {
            this.currentAnimationName = "STAND_RIGHT";
        } else if (direction == Direction.LEFT) {
            this.currentAnimationName = "STAND_LEFT";
        }
    }

    public void walk(Direction direction, float speed) {
        playerState = PlayerState.WALKING;
        facingDirection = direction;
        if (direction == Direction.RIGHT) {
            this.currentAnimationName = "WALK_RIGHT";
        } else if (direction == Direction.LEFT) {
            this.currentAnimationName = "WALK_LEFT";
        }
        if (direction == Direction.UP) {
            moveY(-speed);
        } else if (direction == Direction.DOWN) {
            moveY(speed);
        } else if (direction == Direction.LEFT) {
            moveX(-speed);
        } else if (direction == Direction.RIGHT) {
            moveX(speed);
        }
    }
}