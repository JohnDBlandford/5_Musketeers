package NPCs;

import Builders.FrameBuilder;
import Engine.CombatResolver;
import Engine.ImageLoader;
import GameObject.Frame;
import GameObject.SpriteSheet;
import Level.NPC;
import Level.Player;
import Utils.Point;

import java.util.HashMap;

// BALANCE CHANGES (search for "BALANCE:"): one new field (scaledLevel) and two new methods (applyScaling, getScaledLevel).
// Nothing else in this class changed. Enemy classes still pick their own moves, passives and phases; Balance now picks
// their numbers (HP / ATK / DEF / SPD / XP / gold) for the level they spawn at.
public abstract class Enemy extends NPC {

    protected String enemyName;
    protected float maxHP;
    protected float currentHP;
    protected float baseATK;
    protected float baseDEF;
    protected float baseSPD;

    // Stat Modifiers
    protected float atkModifier = 1.0f;
    protected float defModifier = 1.0f;
    protected float spdModifier = 1.0f;

    protected int xpReward;
    protected int goldReward;
    protected float spawnRate;

    // BALANCE: the level Balance scaled this enemy to (0 = never scaled: its class's own level and numbers are used)
    protected int scaledLevel = 0;

    protected boolean isHurt = false;
    protected int hurtTimer = 0;
    protected final int HURT_DURATION = 30;

    public Enemy(int id, Point location, String enemyName, float HP, float ATK, float DEF, float SPD, int xpReward, int goldReward, float spawnRate){
        super(id, location.x, location.y, new SpriteSheet(ImageLoader.load("Dummy.png"), 32, 32), "IDLE");
        this.enemyName = enemyName;
        this.maxHP = HP;
        this.currentHP = HP;
        this.baseATK = ATK;
        this.baseDEF = DEF;
        this.baseSPD = Math.max(1.0f, SPD);
        this.xpReward = xpReward;
        this.goldReward = goldReward;
        this.spawnRate = spawnRate;
    }

    // BALANCE: NEW. Called by Balance.scale (through EnemyFactory) right after the enemy is built. Replaces the numbers the
    // class wrote in its constructor with the ones for this level, and heals it to full.
    public void applyScaling(int level, float hp, float atk, float def, float spd, int xp, int gold) {
        this.scaledLevel = level;
        this.maxHP = hp;
        this.currentHP = hp;
        this.baseATK = atk;
        this.baseDEF = def;
        this.baseSPD = Math.max(1.0f, spd);
        this.xpReward = xp;
        this.goldReward = gold;
    }

    // BALANCE: NEW. The battle shows this level when it is above 0 (instead of the class's own battleLevel()).
    public int getScaledLevel() { return scaledLevel; }

    @Override
    public HashMap<String, Frame[]> loadAnimations(SpriteSheet spriteSheet) {
        return new HashMap<String, Frame[]>() {{
            put("IDLE", new Frame[] {
                    new FrameBuilder(spriteSheet.getSprite(0, 0), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(0, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build()
            });

            put("HURT", new Frame[] {
                    new FrameBuilder(spriteSheet.getSprite(2, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(2, 2), 15).withScale(3).withBounds(4, 4, 16, 20).build()
            });
        }};
    }

    @Override
    public void update(Player player){
        super.update(player);

        if(isHurt){
            hurtTimer--;
            if(hurtTimer <= 0){
                isHurt = false;
                this.currentAnimationName = "IDLE";
            }
        }
    }

    public void takeDamage(float damageAmount, Player player){
        if(currentHP <= 0)
            return;

        float finalDamage = CombatResolver.calculateDamage(damageAmount, getBaseDEF());
        applyDamage(finalDamage, player);
    }

    public void applyDamage(float damageAmount, Player player){
        if(currentHP <= 0)
            return;

        currentHP = Math.max(0.0f, currentHP - damageAmount);

        if(currentHP <= 0){
            onDefeated(player);
        }
        else {
            triggerHurtState();
            onTakeDamage(damageAmount);
        }
    }

    protected void onDefeated(Player player){
        if (player != null){
            player.addXP(xpReward);
            player.addGold(goldReward);
        }
        this.setMapEntityStatus(Level.MapEntityStatus.INACTIVE);
    }

    protected void onTakeDamage(float damageAmount){}

    protected void triggerHurtState(){
        isHurt = true;
        hurtTimer = HURT_DURATION;
        this.currentAnimationName = "HURT";
    }

    // Stat Altering Functions (Buffs / Debuffs)
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

    // Getters
    public String getEnemyName() { return enemyName; }
    public float getMaxHP(){ return maxHP; }
    public float getCurrentHP(){ return currentHP; }

    public float getBaseATK() { return baseATK * atkModifier; }
    public float getBaseDEF() { return baseDEF * defModifier; }
    public float getBaseSPD() { return baseSPD * spdModifier; }

    public float getAtkModifier() { return atkModifier; }
    public float getDefModifier() { return defModifier; }
    public float getSpdModifier() { return spdModifier; }

    public int getXpReward() { return xpReward; }
    public int getGoldReward() { return goldReward; }
    public float getSpawnRate(){ return spawnRate; }
}