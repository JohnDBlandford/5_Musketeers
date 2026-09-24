package NPCs;

import Builders.FrameBuilder;
import Engine.GraphicsHandler;
import Engine.ImageLoader;
import Engine.Key;
import Engine.Keyboard;
import GameObject.Frame;
import GameObject.SpriteSheet;
import Level.NPC;
import Level.Player;
import Utils.Point;

import java.util.HashMap;
import java.util.Random;

public abstract class TestDummy extends NPC {

    // Test player stats — Effective ATK hard-locked to 100
    protected static final int PLAYER_LEVEL = 1;
    protected static final float PLAYER_EFFECTIVE_ATK = 100.0f; // Hard-locked at 100
    protected static final float PLAYER_EFFECTIVE_SPD = 25.0f;

    // Default testing weapon values (World 1 starter items)
    protected static final float SWORD_BASE_DAMAGE = 30.0f; // Rusted Shortsword
    protected static final float BOW_BASE_DAMAGE = 25.0f;   // Cracked Hunting Bow
    protected static final float WEAPON_ACCURACY_PCT = 0.0f;
    protected static final float WEAPON_PIERCING_PCT = 0.0f;
    protected static final float WEAPON_CRIT_CHANCE_PCT = 0.0f;
    protected static final float WEAPON_CRIT_DAMAGE_PCT = 0.0f;

    protected String enemyName;
    protected float maxHp;
    protected float currentHp;
    protected float baseAtk;
    protected float baseDef;
    protected float baseSpd;

    protected boolean isHurt = false;
    protected int hurtTimer = 0;
    protected final int HURT_DURATION = 30;
    protected Random random = new Random();

    public TestDummy(int id, Point location, String enemyName, float hp, float atk, float def, float spd) {
        super(id, location.x, location.y, new SpriteSheet(ImageLoader.load("Dummy.png"), 32, 32), "IDLE");
        this.enemyName = enemyName;
        this.maxHp = hp;
        this.currentHp = hp;
        this.baseAtk = atk;
        this.baseDef = def;
        this.baseSpd = spd;
    }

    @Override
    public HashMap<String, Frame[]> loadAnimations(SpriteSheet spriteSheet) {
        return new HashMap<String, Frame[]>() {{
            put("IDLE", new Frame[] {
                    new FrameBuilder(spriteSheet.getSprite(0, 0), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(0, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(0, 2), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(0, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(1, 0), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(1, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(1, 2), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(2, 0), 15).withScale(3).withBounds(4, 4, 16, 20).build()
            });

            put("HURT", new Frame[] {
                    new FrameBuilder(spriteSheet.getSprite(2, 1), 15).withScale(3).withBounds(4, 4, 16, 20).build(),
                    new FrameBuilder(spriteSheet.getSprite(2, 2), 15).withScale(3).withBounds(4, 4, 16, 20).build()
            });
        }};
    }

    @Override
    public void update(Player player) {
        super.update(player);

        if (currentHp > 0) {
            // Press G to trigger Melee / Sword Attack
            if (Keyboard.isKeyDown(Key.G) && !isHurt) {
                executePhysicalAttack();
            }
            // Press H to trigger Ranged / Bow Attack
            else if (Keyboard.isKeyDown(Key.H) && !isHurt) {
                executeRangedAttack();
            }
        }

        if (isHurt) {
            hurtTimer--;
            if (hurtTimer <= 0) {
                isHurt = false;
                this.currentAnimationName = "IDLE";
            }
        }
    }

    protected boolean checkDodge() {
        float safeAtkSpd = Math.max(1.0f, PLAYER_EFFECTIVE_SPD);
        float safeDefSpd = Math.max(1.0f, baseSpd);
        float dodgeChance = (safeDefSpd / (safeDefSpd + safeAtkSpd)) * 50.0f;
        dodgeChance = Math.min(40.0f, Math.max(0.0f, dodgeChance));

        float roll = random.nextFloat() * 100.0f;
        boolean dodged = roll < dodgeChance;
        System.out.printf("   [DODGE CHECK] Defender SPD: %.1f | Dodge Chance: %.2f%% | Roll: %.2f%% -> %s%n",
                baseSpd, dodgeChance, roll, dodged ? "DODGED" : "HIT CONNECTED");
        return dodged;
    }

    protected float calculateCritMultiplier() {
        float critChance = Math.min(50.0f, 5.0f + WEAPON_CRIT_CHANCE_PCT);
        float roll = random.nextFloat() * 100.0f;
        if (roll < critChance) {
            float critMult = 1.5f + WEAPON_CRIT_DAMAGE_PCT;
            System.out.printf("   [CRIT CHECK] Crit Chance: %.2f%% | Roll: %.2f%% -> CRITICAL HIT! (x%.2f multiplier)%n",
                    critChance, roll, critMult);
            return critMult;
        }
        System.out.printf("   [CRIT CHECK] Crit Chance: %.2f%% | Roll: %.2f%% -> Normal Hit%n", critChance, roll);
        return 1.0f;
    }

    protected void executePhysicalAttack() {
        System.out.println("\n==================================================");
        System.out.println(">>> EXECUTE MELEE / SWORD ATTACK [KEY: G] <<<");
        System.out.println("Target: " + enemyName);
        System.out.println("Player ATK (Locked): " + PLAYER_EFFECTIVE_ATK);

        if (checkDodge()) {
            System.out.println("Result: Attack was DODGED by " + enemyName + "!");
            System.out.println("==================================================");
            triggerHurtState();
            return;
        }

        float levelTerm = (2.0f * PLAYER_LEVEL / 5.0f) + 2.0f;
        float statRatio = PLAYER_EFFECTIVE_ATK / Math.max(1.0f, baseDef);
        float unscaledDamage = ((levelTerm * SWORD_BASE_DAMAGE * statRatio) / 50.0f) + 2.0f;

        float randomRoll = 0.85f + (random.nextFloat() * 0.15f);
        float critMult = calculateCritMultiplier();

        float finalDamage = unscaledDamage * randomRoll * critMult;

        System.out.printf("   [CALCULATION] (LvlTerm: %.2f * BaseDmg: %.1f * Ratio: %.2f / 50) + 2 = Unscaled: %.2f%n",
                levelTerm, SWORD_BASE_DAMAGE, statRatio, unscaledDamage);
        System.out.printf("   [VARIANCE] Random Roll: %.4f | Crit Multiplier: %.2f%n", randomRoll, critMult);

        applyDamage(finalDamage, "SWORD / MELEE");
    }

    protected void executeRangedAttack() {
        System.out.println("\n==================================================");
        System.out.println(">>> EXECUTE RANGED / BOW ATTACK [KEY: H] <<<");
        System.out.println("Target: " + enemyName);
        System.out.println("Player ATK (Locked): " + PLAYER_EFFECTIVE_ATK);

        if (checkDodge()) {
            System.out.println("Result: Attack was DODGED by " + enemyName + "!");
            System.out.println("==================================================");
            triggerHurtState();
            return;
        }

        float hitChance = Math.min(100.0f, 90.0f + WEAPON_ACCURACY_PCT);
        float accRoll = random.nextFloat() * 100.0f;
        System.out.printf("   [ACCURACY CHECK] Hit Chance: %.2f%% | Roll: %.2f%% -> %s%n",
                hitChance, accRoll, (accRoll <= hitChance) ? "HIT" : "MISSED");

        if (accRoll > hitChance) {
            System.out.println("Result: Ranged attack MISSED!");
            System.out.println("==================================================");
            return;
        }

        float piercedDef = baseDef * (1.0f - WEAPON_PIERCING_PCT);
        float levelTerm = (2.0f * PLAYER_LEVEL / 5.0f) + 2.0f;
        float statRatio = PLAYER_EFFECTIVE_ATK / Math.max(1.0f, piercedDef);
        float unscaledDamage = ((levelTerm * BOW_BASE_DAMAGE * statRatio) / 50.0f) + 2.0f;

        float randomRoll = 0.85f + (random.nextFloat() * 0.15f);
        float critMult = calculateCritMultiplier();

        float finalDamage = unscaledDamage * randomRoll * critMult;

        System.out.printf("   [CALCULATION] (LvlTerm: %.2f * BaseDmg: %.1f * Ratio: %.2f / 50) + 2 = Unscaled: %.2f%n",
                levelTerm, BOW_BASE_DAMAGE, statRatio, unscaledDamage);
        System.out.printf("   [VARIANCE] Random Roll: %.4f | Crit Multiplier: %.2f%n", randomRoll, critMult);

        applyDamage(finalDamage, "RANGED / BOW");
    }

    protected void applyDamage(float damage, String attackType) {
        currentHp = Math.max(0.0f, currentHp - damage);
        System.out.printf("Result: [%s] took %.2f %s damage.%n", enemyName, damage, attackType);
        System.out.printf("HP Remaining: %.2f / %.2f%n", currentHp, maxHp);

        onHitPassive(damage);

        if (currentHp <= 0) {
            System.out.println("*** [" + enemyName + "] HAS BEEN DEFEATED ***");
        }
        System.out.println("==================================================");
        triggerHurtState();
    }

    protected void onHitPassive(float damageDealt) {
        // Overridden by specific enemies (e.g. Hollow Chorus Zealot heal-on-hit)
    }

    protected void triggerHurtState() {
        isHurt = true;
        hurtTimer = HURT_DURATION;
        this.currentAnimationName = "HURT";
    }

    @Override
    public void draw(GraphicsHandler graphicsHandler) {
        super.draw(graphicsHandler);
    }
}