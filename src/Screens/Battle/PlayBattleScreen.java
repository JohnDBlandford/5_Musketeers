package Screens.Battle;

import BattleSystem.*;

import Engine.GraphicsHandler;
import Engine.Key;
import Engine.Screen;
import Item.Item;
import Level.Player;
import NPCs.Enemy;
import NPCs.World1.Dabbah;
import NPCs.World4.GlitchEntity;
import BattleSystem.Ability.Effect;
import inventory.Inventory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

// The whole battle, laid out like the Pokemon HeartGold/SoulSilver battle screen:
// the enemy stands on a platform in the upper right with its name plate at the top left, the knight is big in the lower left
// with his plate (level, HP, XP, stats) on the right, a text box is at the bottom left and the buttons at the bottom right.
//
// Everything that happens is queued as small "steps" (a line of text + something that changes), played one after another.
// Steps are queued per round: [your move] [enemy move] [end of round], and the order of the first two comes from SPD.
//
// Used from PlayLevelScreen:
//     PlayBattleScreen battle = new PlayBattleScreen(player, inventory, enemy, level, BattleWorld.fromMapName(mapName), listener);
//     battle.initialize();   then every frame battle.update() and battle.draw(g)
//     the listener's onBattleEnd(VICTORY / DEFEAT / FLED) is called when the screen has faded out.
//
// BALANCE CHANGES (search for "BALANCE:"):
//   - PP: every knight move and every enemy move has a number of uses per battle (Balance.maxPP / enemyMaxPP). A move at
//     0 PP is refused ("NO PP LEFT FOR ..."). If every move of a category is out, choosing that category makes the knight
//     STRUGGLE (weak, unlimited, 25% recoil). An enemy out of PP for a move uses "Desperate Strike" instead.
//   - RECHARGE: a knight move with power >= Balance.RECHARGE_POWER (HEAVY CLEAVE) greys out MELEE and RANGED on his next
//     turn; pressing them shows "KNIGHT MUST RECHARGE!". SHIELD, HEAL and RUN still work. Enemies don't recharge.
//   - DAMAGE: every hit uses Pokemon's damage formula, Balance.damage(level, power, ATK, DEF) x Balance.roll (85-100%),
//     instead of CombatResolver.calculateDamage(ATK x power, DEF). Crits, FINISHER, blocks, armour reduction and thorns
//     are applied on top exactly as before.
//   - XP: Pokemon's Gen 5 formula (Balance.expGain): less XP from enemies below your level, more from ones above
//   - the enemy's level comes from Balance (enemy.getScaledLevel()) when it was scaled
//   - GUARD MOVES: BULWARK STANCE and BRACE always go first (priority) but can't be used twice in a row. BRACE (no shield
//     equipped) now cuts damage by 30% for 2 enemy turns instead of blocking one hit.
//   - MOVE UNLOCKS: the knight knows move 1 of each type at level 1, move 2 at 10, 3 at 15, 4 at 20
//     (Balance.MOVE_UNLOCK_LEVELS); a level-up that unlocks moves says "KNIGHT learned ...!"
//   - ITEM XP: after a win, every equipped item gains a share of the battle's XP (items never levelled in play before)
//
// GLITCH CHANGES (search for "GLITCH:"): DOUBLE BATTLES. setCompanion() adds a second enemy, the World 4 glitch entity
// (NPCs.World4.GlitchEntity). Laid out like a Pokemon double battle: both enemies stand on the right, two name plates are
// stacked on the left, and a damaging move asks WHICH enemy to hit (LEFT / RIGHT, ENTER). The glitch never does damage; its
// four moves (shown as binary) hide the HP bar, cause GRIEVANCE (-50% stats for 4 turns + flashbacks), sever a move for
// the rest of the fight, and BLOOD TITHE (its ally's ATK +999% but the ally falls to 1 HP: outspeed it or be one-shot). The fight is won when both enemies are down.
// GRIEVANCE's flashbacks are drawn by GriefFlashbacks (11 scenes; two different ones each time, one per slow fade).
// The glitch MIRRORS the enemy it came with (same level, HP, ATK, DEF, SPD, XP and gold). It isn't there at the start:
// after the usual opening, a TAKEOVER cutscene floods the screen with static and leaves it standing next to the enemy.
// Its plate is a black box ringed with live static (white text), and every line it says is in a black, glitching text box.
// Its HP bar never goes DOWN: the more it is hurt, the more the bar breaks: skulls shake, then poke out of the box, then
// fly out at the player growing bigger, the plate starts spinning, and static creeps over the screen (drawGlitchChaos).
// In a glitch battle EVERY box (text box, all three plates) is black with a static border and white text.
// If the enemy falls while the glitch is still standing, the backdrop tears apart and the flashbacks loop behind the rips
// until the glitch is gone (drawTornBackground).
// TEST MODE (setTestMode, used by the T and G keys only): the knight takes no damage and has every move unlocked.
//
// FINAL BOSS CHANGES (search for "FINAL BOSS:"): when the enemy is Dabbah, the battlefield is drawn by FinalBossPresentation
// instead of the backdrop and sprites: a PS1-style 3D Dabbah (black dragon -> burning dragon -> five-headed hydra) over the
// World 4 backdrop, with a Pokemon-style battle camera and an animation for every move. Dabbah's moves, damage, stat drops and
// phases still come from Dabbah.java. The text waits for the animations (holdsScript / consumeImpact).
// CINEMATIC CHANGES (search for "CINEMATIC:"): when a form falls, an anime-style cinematic plays (FinalBossCinematic), then
// the transformation, then the fight goes on; the text is frozen while it plays. FinalBossPresentation.drawOverlay is drawn
// over the HUD (the cinematic, its bars, the Judgement cut-in, rage embers, the danger heartbeat). Critical hits on Dabbah
// get an impact frame. Dabbah's plate shows three pips: its forms left. TEST MODE (the G key): Dabbah's HP is held at 1,
// so every hit ends a form and its cinematic plays (this replaces the K key, which dropped it to 1 HP once).
// CODEX: every form of Dabbah the knight faces is unlocked in the Codex of Beasts (markDabbahForm), win or lose.
// LOSING THE BEAM STRUGGLE kills the knight: the battle ends in a DEFEAT (game over), even in test mode (so it can be tested).
// BALANCE: Dabbah can land CRITICAL HITS (Dabbah.critChance: 15% / 20% / 25% per form, x1.5 damage), each with an impact frame.
public class PlayBattleScreen extends Screen {
    private enum Phase { INTRO, COMMAND, ABILITY, INFO, CONFIRM_RUN, SCRIPT, END_FADE, TARGET }

    private static final int HOLD = 70;          // frames a message stays up if you don't press ENTER
    private static final int FADE_FRAMES = 26;
    private static final int INTRO_FRAMES = 40;

    // the battle
    private Player player;
    private Inventory inventory;
    private Enemy enemy;
    private int enemyLevel = 1;
    private BattleWorld world = BattleWorld.WORLD_1;
    private BattleListener listener;
    private boolean boss;

    private Battle.BattleStatus status = Battle.BattleStatus.STARTED;
    private Phase phase = Phase.INTRO;
    private final Random rng = new Random();
    private int tick, fade, introTimer;
    private Battle.BattleResult pendingResult;

    // the pieces (the skeleton's classes)
    private BattleBackgroundScreen backgroundScreen;
    private TurnHistoryScreen turnHistoryScreen;
    private InventoryDisplayScreen inventoryDisplayScreen;
    private MainMenu optionScreen;
    private AbilityScreen abilityScreen;
    private AbilityInfoScreen abilityInfoScreen;
    private RunAwayScreen runAwayScreen;
    private BattleSprites.BattleSprite playerSprite;
    private BattleSprites.BattleSprite enemySprite;

    // FINAL BOSS: the 3D presentation of the Dabbah fight (null in every other fight)
    private FinalBossPresentation finalBoss;

    // what the plates show (they glide toward the real numbers, like Gen 1's draining bars)
    private float shownPlayerHP, shownEnemyHP, shownXp, targetXp;
    private int shownLevel;
    private String playerName = "KNIGHT", enemyName = "ENEMY";

    // battle state
    private int healsLeft = 1, runAttempts;
    private boolean playerGuard, playerCounter, enemyStunned;
    private String hint = "";
    private int hintTimer;

    // BALANCE: PP used this battle (the knight's by ability, the enemy's by move name) and the recharge turn
    private final IdentityHashMap<Ability, Integer> ppUsed = new IdentityHashMap<>();
    private final HashMap<String, Integer> enemyPpUsed = new HashMap<>();
    private boolean recharging;   // true for the knight's next turn after a recharge move: MELEE and RANGED are greyed out
    private Ability lastPlayerAbility;   // BALANCE: the knight's previous action (guard moves can't be used twice in a row)
    private int braceTurns;              // BALANCE: enemy turns left with BRACE's 30% damage cut

    // GLITCH: the second enemy of a double battle (null in a normal 1-vs-1 fight) and everything its moves do
    private GlitchEntity glitch;
    private int glitchLevel;
    private float shownGlitchHP;
    private boolean glitchStunned, glitchGone, enemyDown, enemyFaintSaid, glitchCollapseSaid;
    private int glitchHurt;                 // frames of the white "hit" flash on the glitch's sprite
    private Enemy target;                   // the enemy the knight's current move hits
    private Ability pendingAbility;         // a damaging move waiting for its target to be picked
    private int targetIndex;                // 0 = the main enemy, 1 = the glitch
    private boolean veiled;                 // VEIL OF STATIC: the knight's HP bar is hidden
    private int griefTurns;                 // GRIEVANCE: rounds left with every knight stat -50%
    private int griefCooldown;              // GRIEVANCE: glitch turns until it can use it again
    private int flashback;                  // GRIEVANCE: frames left of the flashback
    private int[] flashScenes = { 0, 1 };   // GLITCH: the two GriefFlashbacks scenes shown this time (one per fade)
    private int lastFlashScene = -1;        // GLITCH: so the same scene doesn't come straight back
    private boolean tithed;                 // BLOOD TITHE was used (once per fight): the enemy is at 1 HP with +999% ATK
    private boolean titheWait;              // BLOOD TITHE: the enemy holds its swollen strike until the next round
    private final Set<Ability> severed = Collections.newSetFromMap(new IdentityHashMap<>());   // SEVERANCE: moves torn out
    private static final int FLASHBACK_FRAMES = 150;   // 2.5 s: two slow fades (well under 3 flashes a second)

    // the step queue
    private static final class Step {
        final String text;      // null = silent step (just runs its action)
        final Runnable action;
        final boolean glitchy;  // GLITCH: shown in the black, glitching text box
        Step(String text, Runnable action) { this(text, action, false); }
        Step(String text, Runnable action, boolean glitchy) { this.text = text; this.action = action; this.glitchy = glitchy; }
    }

    // GLITCH: a line said by (or about) the glitch: black glitching text box
    private static Step glitchStep(String text, Runnable action) { return new Step(text, action, true); }
    private boolean messageGlitchy;   // GLITCH: the message on screen is a glitch line

    // GLITCH: the TAKEOVER cutscene (static floods the screen, then drains away leaving the glitch on the field)
    private static final int TAKEOVER_FRAMES = 150;
    private int takeover = -1;              // frames into the cutscene, -1 = not playing
    private boolean glitchRevealed;         // the glitch isn't drawn until the takeover has covered the screen

    private boolean takeoverPlaying() { return takeover >= 0 && takeover < TAKEOVER_FRAMES; }

    // GLITCH: the enemy is down but the glitch isn't: the world comes apart
    private int aloneT;                     // frames since the glitch was left alone
    private int aloneSceneBase;             // the first flashback scene shown behind the rips
    private java.awt.image.BufferedImage tornBuf;   // the backdrop, drawn here first so it can be cut into strips
    private boolean alone() { return glitch != null && enemyDown && glitchAlive(); }

    // GLITCH TEST MODE (the T and G keys in PlayLevelScreen): no damage to the knight, every move unlocked. Remove before release.
    private boolean testMode;
    public PlayBattleScreen setTestMode(boolean on) { this.testMode = on; return this; }
    boolean isTestMode() { return testMode; }
    // a slot is usable: learned by level, or test mode
    boolean slotUnlocked(int slot) { return testMode || Balance.moveUnlocked(slot, player.getLevel()); }
    private final ArrayDeque<Step> script = new ArrayDeque<>();
    private Step current;
    private int stepTimer;
    private String message = "";

    // used by BattleBackgroundScreen so building the backdrop does not build a whole new battle
    protected PlayBattleScreen() {}

    public PlayBattleScreen(Player player, Inventory inventory, Enemy enemy, int enemyLevel, BattleWorld world, BattleListener listener) {
        this.player = player;
        this.inventory = inventory;
        this.enemy = enemy;
        this.enemyLevel = Math.max(1, enemyLevel);
        this.world = world == null ? BattleWorld.WORLD_1 : world;
        this.listener = listener;
    }

    // bosses cannot be run from and the heal can be used twice
    public PlayBattleScreen setBoss(boolean boss) {
        this.boss = boss;
        return this;
    }

    // FINAL BOSS: the fight against Dabbah (a 1-vs-1 fight; always a boss, always in World 4)
    private boolean isFinalFight() {
        return enemy instanceof Dabbah && glitch == null;
    }

    // FINAL BOSS: the opening lasts longer in the Dabbah fight (it rises out of the earth first)
    private int introFrames() {
        return finalBoss != null ? FinalBossPresentation.INTRO_FRAMES : INTRO_FRAMES;
    }

    // GLITCH: makes this a double battle with the glitch entity as the second enemy (call before initialize). null = 1-vs-1.
    public PlayBattleScreen setCompanion(Enemy companion) {
        this.glitch = companion instanceof GlitchEntity ? (GlitchEntity) companion : null;
        return this;
    }

    @Override
    public void initialize() {
        BattleInput.reset();
        if (isFinalFight()) {   // FINAL BOSS
            world = BattleWorld.WORLD_4;
            boss = true;
        }
        playerName = player.getClass().getSimpleName().toUpperCase();
        enemyName = enemy.getEnemyName().toUpperCase();
        healsLeft = boss ? 2 : 1;
        runAttempts = 0;
        tick = 0;
        introTimer = 0;
        playerGuard = playerCounter = enemyStunned = false;
        hint = "";
        ppUsed.clear();          // BALANCE: every move starts the battle with full PP
        enemyPpUsed.clear();
        recharging = false;
        lastPlayerAbility = null;   // BALANCE
        braceTurns = 0;
        // GLITCH: a fresh double battle
        glitchStunned = glitchGone = enemyDown = enemyFaintSaid = glitchCollapseSaid = veiled = tithed = titheWait = false;
        griefTurns = griefCooldown = flashback = glitchHurt = 0;
        severed.clear();
        target = enemy;
        pendingAbility = null;
        player.resetStatModifiers();
        enemy.resetStatModifiers();
        if (enemy instanceof BattleEnemy) ((BattleEnemy) enemy).onBattleStart();
        if (enemy.getScaledLevel() > 0) enemyLevel = enemy.getScaledLevel();   // BALANCE: the level Balance scaled it to
        else if (enemy instanceof BattleEnemy && ((BattleEnemy) enemy).battleLevel() > 0) enemyLevel = ((BattleEnemy) enemy).battleLevel();
        takeover = -1;
        aloneT = 0;
        aloneSceneBase = rng.nextInt(GriefFlashbacks.COUNT);
        glitchRevealed = glitch == null;
        messageGlitchy = false;
        if (glitch != null) {
            // GLITCH: it MIRRORS the enemy it came with: same level, HP, ATK, DEF, SPD, XP and gold
            glitch.resetStatModifiers();
            glitch.applyScaling(enemyLevel, enemy.getMaxHP(), enemy.getBaseATK(), enemy.getBaseDEF(), enemy.getBaseSPD(),
                    enemy.getXpReward(), enemy.getGoldReward());
            glitchLevel = enemyLevel;
            shownGlitchHP = glitch.getCurrentHP();
        }

        backgroundScreen = new BattleBackgroundScreen(world);
        backgroundScreen.initialize();
        turnHistoryScreen = new TurnHistoryScreen();
        turnHistoryScreen.initialize();
        inventoryDisplayScreen = new InventoryDisplayScreen(inventory, this);
        inventoryDisplayScreen.initialize();
        optionScreen = new MainMenu(this);
        abilityScreen = new AbilityScreen(this);
        abilityInfoScreen = new AbilityInfoScreen(this);
        runAwayScreen = new RunAwayScreen(this);
        optionScreen.initialize();
        abilityScreen.initialize();
        abilityInfoScreen.initialize();
        runAwayScreen.initialize();

        playerSprite = BattleSprites.BattleSprite.knight();
        enemySprite = BattleSprites.BattleSprite.enemy(enemy.getEnemyName());

        // FINAL BOSS: the 3D battlefield (it uses the World 4 backdrop and draws the knight's own sprite into the scene)
        finalBoss = null;
        if (isFinalFight()) {
            finalBoss = new FinalBossPresentation(backgroundScreen, playerSprite, enemySprite, (Dabbah) enemy);
            finalBoss.initialize();
            enemyName = "DABBAH";   // the plate shows the full title of the current form
        }

        shownPlayerHP = player.getCurrentHP();
        shownEnemyHP = enemy.getCurrentHP();
        shownLevel = player.getLevel();
        shownXp = targetXp = xpFraction();

        status = Battle.BattleStatus.STARTED;
        pendingResult = null;
        current = null;
        script.clear();

        if (finalBoss != null) {   // FINAL BOSS
            say("THE EARTH SPLITS OPEN BENEATH DABBAH'S HOLLOW...", null);
            say("DABBAH, BEAST OF THE EARTH, EMERGES!", null);
        } else {
            say(enemyName + " blocks your path!", null);
        }
        if (glitch != null) {   // GLITCH: the takeover cutscene, then it speaks
            script.addLast(new Step(null, () -> takeover = 0));
            script.addLast(glitchStep("...SOMETHING ELSE IS WATCHING.", null));
        }
        say("GO! " + playerName + "!", null);
        script.addLast(new Step(null, () -> { status = Battle.BattleStatus.IN_PROGRESS; toCommands(); }));
        phase = Phase.SCRIPT;   // the opening flash is drawn while introTimer counts up, the script waits for it
    }

    // ---------------------------------------------------------------- update

    @Override
    public void update() {
        tick++;
        BattleInput.update();
        // CINEMATIC: TEST MODE ONLY (the G key): Dabbah's HP is held at 1, so every hit ends a form and its cinematic plays
        // (replaces the K key, which dropped it to 1 HP once)
        if (finalBoss != null && testMode && enemy.getCurrentHP() > 1) {
            enemy.applyDamage(enemy.getCurrentHP() - 1, null);
            shownEnemyHP = enemy.getCurrentHP();
        }
        // FINAL BOSS: the opening (Dabbah rising out of the earth) can be skipped with ENTER
        if (finalBoss != null && introTimer > 20 && introTimer < introFrames() - 10 && BattleInput.confirm()) introTimer = introFrames() - 10;
        if (introTimer < introFrames()) introTimer++;   // FINAL BOSS: introFrames() (was INTRO_FRAMES)
        backgroundScreen.update();
        playerSprite.update();
        enemySprite.update();
        if (finalBoss != null) {   // FINAL BOSS
            finalBoss.setKnightHp(player.getCurrentHP() / Math.max(1f, player.getMaxHP()));   // CINEMATIC: the danger heartbeat
            finalBoss.update(introTimer, introFrames(), phase == Phase.END_FADE);
            // CINEMATIC: the knight lost the beam struggle and was torn apart: straight to the game over (even in test mode)
            if (finalBoss.consumeKnightSlain() && pendingResult == null) {
                script.clear();
                current = null;
                player.applyDamage(player.getCurrentHP());
                playerSprite.setPose(BattleSprites.BattleSprite.Pose.FAINT);
                finish(Battle.BattleResult.DEFEAT);
            }
            // CODEX: the Codex of Beasts shows each of Dabbah's forms once the knight has faced it (a form counts once its
            // cinematic is over and it rises, so a knight killed in the beam struggle hasn't seen the next one)
            if (pendingResult == null && !finalBoss.cinematicPlaying())
                Screens.CodexOfBeastsScreen.markDabbahForm(((Dabbah) enemy).getPhase());
        }
        turnHistoryScreen.updateScreen(historyAvailable());
        glideBars();
        if (flashback > 0) flashback--;   // GLITCH
        if (alone()) aloneT++;             // GLITCH: the world tearing apart
        if (takeoverPlaying()) {           // GLITCH: the takeover cutscene
            takeover++;
            if (takeover >= TAKEOVER_FRAMES / 2) glitchRevealed = true;   // it is there when the static drains away
        }
        if (glitchHurt > 0) glitchHurt--;
        if (hintTimer > 0 && --hintTimer == 0) hint = "";
        if (turnHistoryScreen.isExpanded()) return;   // the full move log is open: the battle is paused until it closes

        switch (phase) {
            case COMMAND: optionScreen.update(); break;
            case ABILITY: abilityScreen.update(); break;
            case INFO: abilityInfoScreen.update(); break;
            case CONFIRM_RUN: runAwayScreen.update(); break;
            case SCRIPT: if (introTimer >= introFrames() && !takeoverPlaying()) runScript(); break;   // FINAL BOSS: introFrames()   // GLITCH: waits for the cutscene
            case TARGET: updateTarget(); break;   // GLITCH
            case END_FADE:
                if (++fade >= FADE_FRAMES) {
                    status = Battle.BattleStatus.COMPLETED;
                    player.resetStatModifiers();
                    enemy.resetStatModifiers();
                    if (glitch != null) glitch.resetStatModifiers();   // GLITCH
                    if (listener != null) listener.onBattleEnd(pendingResult);
                }
                break;
            default: break;
        }
    }

    private void glideBars() {
        shownPlayerHP = glide(shownPlayerHP, player.getCurrentHP(), 0.55f);
        shownEnemyHP = glide(shownEnemyHP, enemy.getCurrentHP(), 0.55f);
        if (glitch != null) shownGlitchHP = glide(shownGlitchHP, glitch.getCurrentHP(), 0.55f);   // GLITCH
        shownXp = glide(shownXp, targetXp, 0.02f);
    }

    // HP glides in points (faster when the gap is big), XP in fractions
    private static float glide(float shown, float target, float step) {
        if (target > 2f || shown > 2f) step = Math.max(step, Math.abs(target - shown) * 0.06f);
        if (shown < target) return Math.min(target, shown + step);
        return Math.max(target, shown - step);
    }

    // ---------------------------------------------------------------- the step queue

    private void say(String text, Runnable action) {
        script.addLast(new Step(text, action));
    }

    // puts steps at the FRONT of the queue (used when a step decides what happens next)
    private void insert(List<Step> steps) {
        for (int i = steps.size() - 1; i >= 0; i--) script.addFirst(steps.get(i));
    }

    private void runScript() {
        // CINEMATIC: while a cinematic plays the text is frozen: no next line, no time-out, ENTER (mashing it) skips nothing
        if (finalBoss != null && finalBoss.cinematicPlaying()) return;
        // FINAL BOSS: the moment one of Dabbah's attacks lands, its damage text comes up right away...
        if (finalBoss != null && current != null && finalBoss.consumeImpact()) current = null;
        while (current == null) {
            // FINAL BOSS: ...and the next step waits while Dabbah winds up an attack or changes form
            if (finalBoss != null && finalBoss.holdsScript()) return;
            current = script.pollFirst();
            if (current == null) { scriptFinished(); return; }
            if (current.action != null) current.action.run();
            if (current.text == null) {
                current = null;
                if (takeoverPlaying()) { message = ""; return; }   // GLITCH: a step started the cutscene: wait for it
            }
            else { message = current.text; messageGlitchy = current.glitchy; stepTimer = 0; }
        }
        stepTimer++;
        if (stepTimer >= HOLD || (stepTimer >= 14 && BattleInput.confirm())) current = null;
    }

    private void scriptFinished() {
        message = "";
        if (phase == Phase.SCRIPT) toCommands();
    }

    // ---------------------------------------------------------------- phases (the option screens call these)

    private void toCommands() {
        phase = Phase.COMMAND;
        optionScreen.open();
        message = "";
    }

    void backToCommands() {
        phase = Phase.COMMAND;
        int keep = optionScreen.getCursor();
        optionScreen.open();
        optionScreen.setCursor(keep);
    }

    void openAbilities(MoveCategory category) {
        // BALANCE: every move of this category is out of PP -> the knight struggles instead of opening the window
        if (outOfPP(category)) {
            optionScreen.close();
            say("NO PP LEFT! " + playerName + " STRUGGLES!", null);
            useAbility(Balance.STRUGGLE);
            return;
        }
        phase = Phase.ABILITY;
        abilityScreen.open(category, inventory);
    }

    void openAbilityInfo(Ability ability) {
        phase = Phase.INFO;
        abilityInfoScreen.open(ability);
    }

    void closeAbilityInfo() {
        phase = Phase.ABILITY;
        abilityScreen.resume();
    }

    void openRunConfirm() {
        phase = Phase.CONFIRM_RUN;
        runAwayScreen.open();
    }

    void showHint(String text) {
        hint = text;
        hintTimer = 90;
    }

    // ---------------------------------------------------------------- BALANCE: PP and recharge (the option screens read these)

    // uses left of a move this battle (-1 = unlimited)
    int ppLeft(Ability a) {
        int max = Balance.maxPP(a);
        if (max < 0) return -1;
        return Math.max(0, max - ppUsed.getOrDefault(a, 0));
    }

    int ppMax(Ability a) { return Balance.maxPP(a); }

    boolean isRecharging() { return recharging; }

    // true when no UNLOCKED move of this category (on the equipped item) has a use left
    private boolean outOfPP(MoveCategory category) {
        Ability[] slots = AbilityLibrary.slotsFor(category, inventory.getEquippedItem(category.getSlot()));
        for (int i = 0; i < slots.length; i++)
            if (slots[i] != null && slotUnlocked(i) && !isSevered(slots[i]) && ppLeft(slots[i]) != 0) return false;   // BALANCE + GLITCH
        return true;
    }

    int getPlayerLevel() { return player.getLevel(); }   // BALANCE: AbilityScreen locks moves by level

    // ---------------------------------------------------------------- the player's moves

    // BALANCE: BULWARK STANCE and BRACE (the GUARD moves) always act first, and can't be used twice in a row
    static boolean isGuardMove(Ability a) { return a != null && a.getEffect() == Effect.GUARD; }

    boolean blockedAsRepeat(Ability a) { return isGuardMove(a) && a == lastPlayerAbility; }

    // BALANCE: BRACE is the shield move you get with no shield equipped
    static boolean isBrace(Ability a) { return a != null && a == AbilityLibrary.defaultFor(MoveCategory.SHIELD); }

    // an ability was chosen in the ability window (or from the info window)
    void useAbility(Ability ability) {
        if (ppLeft(ability) == 0) {   // BALANCE: refused at 0 PP (AbilityScreen checks first; this is a safety net)
            showHint("NO PP LEFT FOR " + ability.getName() + "!");
            backToCommands();
            return;
        }
        if (blockedAsRepeat(ability)) {   // BALANCE: no two BULWARK STANCEs / BRACEs in a row (AbilityScreen checks first)
            showHint("CAN'T USE " + ability.getName() + " TWICE IN A ROW!");
            backToCommands();
            return;
        }
        if (isSevered(ability)) {   // GLITCH: torn out by SEVERANCE (AbilityScreen checks first)
            showHint("THAT MOVE WAS SEVERED.");
            backToCommands();
            return;
        }
        // GLITCH: a damaging move with two enemies standing asks which one to hit
        if (needsTarget(ability) && enemyAlive() && glitchAlive()) {
            pendingAbility = ability;
            targetIndex = 0;
            optionScreen.close();
            abilityScreen.close();
            abilityInfoScreen.close();
            phase = Phase.TARGET;
            return;
        }
        commitAbility(ability, enemyAlive() ? enemy : glitch);
    }

    // spends the PP and starts the round with this move aimed at this enemy
    private void commitAbility(Ability ability, Enemy at) {
        if (Balance.maxPP(ability) > 0) ppUsed.merge(ability, 1, Integer::sum);   // BALANCE: spend one PP
        lastPlayerAbility = ability;
        target = at;   // GLITCH
        startRound(() -> playerAbilityBlock(ability), isGuardMove(ability));   // BALANCE: guard moves have priority
    }

    // ---------------------------------------------------------------- GLITCH: double battle helpers

    private static boolean needsTarget(Ability a) { return a.getEffect() != Effect.GUARD && a.getEffect() != Effect.COUNTER; }

    private boolean enemyAlive() { return !enemyDown && enemy.getCurrentHP() > 0; }

    private boolean glitchAlive() { return glitch != null && !glitchGone && glitch.getCurrentHP() > 0; }

    boolean isSevered(Ability a) { return a != null && severed.contains(a); }

    String nameOf(Enemy e) { return e == glitch ? GlitchEntity.glitchName(tick, 8) : enemyName; }

    // LEFT / RIGHT picks the enemy, ENTER confirms, B goes back to the menu
    private void updateTarget() {
        if (BattleInput.left() || BattleInput.right() || BattleInput.up() || BattleInput.down()) targetIndex = 1 - targetIndex;
        if (BattleInput.back()) { pendingAbility = null; backToCommands(); return; }
        if (BattleInput.confirm() && pendingAbility != null) {
            Ability a = pendingAbility;
            pendingAbility = null;
            commitAbility(a, targetIndex == 0 ? enemy : glitch);
        }
    }

    // the knight's stats while GRIEVANCE is on
    private float grief() { return griefTurns > 0 ? Balance.GRIEF_FACTOR : 1f; }

    void useHeal() {
        if (healsLeft <= 0) { showHint("HEAL ALREADY USED THIS BATTLE!"); return; }
        healsLeft--;
        lastPlayerAbility = null;   // BALANCE: anything in between lets a guard move be used again
        startRound(this::playerHealBlock, true);
    }

    void attemptRun() {
        lastPlayerAbility = null;   // BALANCE
        startRound(this::playerRunBlock, true);
    }

    // queue one round: both fighters act (SPD decides who is first, heal/run always go first), then the round ends
    private void startRound(Runnable playerBlock, boolean priority) {
        recharging = false;   // BALANCE: any action spends the recharge turn (a recharge move used now sets it again)
        turnHistoryScreen.nxtTurn();
        phase = Phase.SCRIPT;
        optionScreen.close();
        abilityScreen.close();
        abilityInfoScreen.close();
        runAwayScreen.close();
        // GLITCH: up to three actors (knight, enemy, glitch), fastest first; heal / run / guard moves always go first
        List<Object[]> actors = new ArrayList<>();
        actors.add(new Object[] { playerBlock, priority ? Float.MAX_VALUE : BattleMath.initiative(playerSpd(), rng) });
        if (enemyAlive()) actors.add(new Object[] { (Runnable) this::enemyTurnBlock, BattleMath.initiative(enemy.getBaseSPD(), rng) });
        if (glitchAlive()) actors.add(new Object[] { (Runnable) this::glitchTurnBlock, BattleMath.initiative(glitch.getBaseSPD(), rng) });
        actors.sort((a, b) -> Float.compare((Float) b[1], (Float) a[1]));
        for (Object[] a : actors) {
            script.addLast(new Step(null, (Runnable) a[0]));
            script.addLast(new Step(null, this::checkEnd));
        }
        script.addLast(new Step(null, this::endOfRound));
    }

    // GLITCH: GRIEVANCE counts down once per round
    private void endOfRound() {
        titheWait = false;   // GLITCH: BLOOD TITHE: from the next round on, the swollen enemy attacks in speed order
        if (griefTurns > 0 && --griefTurns == 0 && pendingResult == null) {
            List<Step> steps = new ArrayList<>();
            steps.add(new Step("THE GRIEVANCE LIFTS... FOR NOW.", null));
            insert(steps);
        }
    }

    private float playerSpd() {
        return BattleMath.effectiveSpd(player, inventory) * grief();   // GLITCH: GRIEVANCE halves it
    }

    private void playerAbilityBlock(Ability a) {
        if (isOver()) return;
        // GLITCH: the chosen enemy; if it already fell this round, the other one
        Enemy t = target;
        if (t == null || t.getCurrentHP() <= 0 || (t == glitch && glitchGone) || (t == enemy && enemyDown)) t = enemyAlive() ? enemy : glitch;
        final Enemy foe = t;
        String foeName = nameOf(foe);
        Item weapon = inventory.getEquippedItem(a.getCategory().getSlot());   // sword / bow / shield (null = bare hands)
        boolean stance = a.getEffect() == Effect.GUARD || a.getEffect() == Effect.COUNTER;
        TurnHistoryScreen.Entry log = turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER, a.getName(),
                stance ? TurnHistoryScreen.N_TRGT : foe.getCurrentHP());
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(playerName + " used " + a.getName() + "!", null));

        // moves that don't hit: GUARD and COUNTER just set a stance
        if (isBrace(a)) {   // BALANCE: BRACE now cuts damage by 30% for 2 enemy turns (it used to block the next hit, like BULWARK)
            steps.add(new Step(playerName + " braces! Damage taken is cut by " + Math.round(Balance.BRACE_REDUCTION * 100)
                    + "% for " + Balance.BRACE_TURNS + " turns.", () -> braceTurns = Balance.BRACE_TURNS));
            insert(steps);
            return;
        }
        if (a.getEffect() == Effect.GUARD) {
            steps.add(new Step(playerName + " braces behind the shield!", () -> playerGuard = true));
            insert(steps);
            return;
        }
        if (a.getEffect() == Effect.COUNTER) {
            steps.add(new Step(playerName + " readies a counter!", () -> playerCounter = true));
            insert(steps);
            return;
        }

        int hits = a.getEffect() == Effect.MULTI_HIT ? 3 : 1;
        float atk = BattleMath.effectiveAtk(player, inventory, weapon) * grief();   // GLITCH: GRIEVANCE halves it
        double critChance = BattleMath.critChance(weapon);
        for (int i = 0; i < hits; i++) {
            // the move's base accuracy (plus the weapon's ACCURACY buff) decides if it lands
            double acc = BattleMath.accuracy(a, weapon);
            boolean accurate = rng.nextDouble() < acc;
            // BALANCE: DODGE FIX: a 100% move can't be dodged; otherwise only a faster enemy dodges, at most 10%
            boolean dodged = accurate && rng.nextFloat() < BattleMath.dodgeChance(foe.getBaseSPD(), playerSpd(), acc);
            if (!accurate || dodged) {
                steps.add(new Step(accurate ? foeName + " dodged!" : playerName + " missed!", null));
                continue;
            }
            float base = atk;
            if (a.getEffect() == Effect.THROW) base = atk * (float) (1 + BattleMath.blockAmount(inventory));   // better shields hit harder
            // the weapon's PIERCING buff and the PIERCE move both let hits ignore part of the enemy's DEF
            float def = foe.getBaseDEF() * (float) (1 - BattleMath.piercing(weapon)) * (a.getEffect() == Effect.PIERCE ? 0.5f : 1f);
            // BALANCE: Pokemon's damage formula (level, move power, your ATK / its DEF) x the 85-100% random roll
            double raw = Balance.damage(player.getLevel(), a.getPower(), base, def) * Balance.roll(rng);
            boolean crit = rng.nextDouble() < critChance;
            if (crit) raw *= BattleMath.critMultiplier(weapon);
            if (a.getEffect() == Effect.FINISHER && foe.getCurrentHP() < foe.getMaxHP() * 0.3f) raw *= 2;
            int dmg = Math.max(1, (int) Math.round(raw));
            final boolean c = crit;
            steps.add(new Step(c ? "A critical hit! " + dmg + " damage!" : (hits > 1 ? "HIT! " + dmg + " damage!" : "It hit for " + dmg + " damage!"),
                    () -> {
                        dealTo(foe, dmg, log);
                        if (c && finalBoss != null) finalBoss.critImpact();   // CINEMATIC: a critical hit on Dabbah: an impact frame
                    }));
            if (a.getEffect() == Effect.LIFESTEAL) {
                int heal = Math.max(1, dmg / 2);
                steps.add(new Step(playerName + " drained " + heal + " HP!", () -> {
                    player.setCurrentHP(player.getCurrentHP() + heal);
                    // no damage, but the HP column shows the knight's HP after the drain (same as HEAL)
                    turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER, "DRAIN", player.getCurrentHP())
                            .hit(0, player.getCurrentHP());
                }));
            }
            if (a == Balance.STRUGGLE) {   // BALANCE: struggling hurts the knight too
                int recoil = Math.max(1, (int) Math.round(dmg * Balance.STRUGGLE_RECOIL));
                steps.add(new Step(playerName + " is hurt by recoil! (" + recoil + ")", () -> {
                    if (!testMode) player.applyDamage(recoil);   // GLITCH TEST MODE
                    playerSprite.setPose(BattleSprites.BattleSprite.Pose.HURT);
                }));
            }
        }
        // stat effects only happen if the enemy is still standing after the hits land
        steps.add(new Step(null, this::checkEnd));
        switch (a.getEffect()) {
            case ATK_UP: statChange(steps, true, 0, +1); break;
            case SPD_UP: statChange(steps, true, 2, +1); break;
            case DEF_DOWN: statChange(steps, foe, 1, -1, -STAGE_SIZE); break;   // GLITCH: on the enemy that was hit
            case STUN:
                if (rng.nextFloat() < 0.4f) steps.add(new Step(foeName + " is stunned!", () -> {
                    if (foe == glitch) glitchStunned = true; else enemyStunned = true;
                }));
                break;
            default: break;
        }
        // BALANCE: a heavy move (HEAVY CLEAVE) leaves the knight recharging: no MELEE or RANGED on his next turn
        if (Balance.recharges(a)) steps.add(new Step(playerName + " must recharge next turn!", () -> recharging = true));
        insert(steps);
    }

    // ---------------------------------------------------------------- stat changes (shared by your moves and the enemy's)

    // Like Pokemon, each stat can go up or down by "stages" (here 4 at most each way). One stage changes the stat by 15%.
    private static final int MAX_STAGE = 4;
    private static final float STAGE_SIZE = 0.15f;
    private static final String[] STAT_NAMES = {"ATK", "DEF", "SPD"};
    private final int[] stages = new int[9];   // 0-2: your ATK, DEF, SPD   3-5: the enemy's   6-8: the glitch's (GLITCH)

    private static Color statColor(int stat) {
        return stat == 0 ? BattleUI.BTN_MELEE : (stat == 1 ? BattleUI.BTN_SHIELD : BattleUI.BTN_HEAL);
    }

    // adds the steps for one stat change: the arrow animation and the "ATK rose!" / "DEF fell!" text (or "won't go any higher!" at the limit)
    private void statChange(List<Step> steps, boolean onPlayer, int stat, int delta) {
        statChange(steps, onPlayer, stat, delta, delta * STAGE_SIZE);
    }

    // same, but the stat modifier changes by an exact amount (enemy moves: -0.10 = -10%); the stage count (for the cap and the
    // "sharply / harshly" wording) is the amount in 15% steps, at least one
    private void statChangeAmount(List<Step> steps, boolean onPlayer, int stat, float amount) {
        int stages = Math.max(1, Math.round(Math.abs(amount) / STAGE_SIZE));
        statChange(steps, onPlayer, stat, amount < 0 ? -stages : stages, amount);
    }

    private void statChange(List<Step> steps, boolean onPlayer, int stat, int delta, float amount) {
        statChange(steps, onPlayer ? null : enemy, stat, delta, amount);
    }

    // GLITCH: foe = the enemy whose stat changes (null = the knight)
    private void statChange(List<Step> steps, Enemy foe, int stat, int delta, float amount) {
        boolean onPlayer = foe == null;
        int idx = (onPlayer ? 0 : (foe == glitch ? 6 : 3)) + stat;
        String who = onPlayer ? playerName : nameOf(foe);
        String name = STAT_NAMES[stat];
        boolean up = delta > 0;
        int next = stages[idx] + delta;
        if (next > MAX_STAGE || next < -MAX_STAGE) {
            steps.add(new Step(who + "'s " + name + " won't go any " + (up ? "higher" : "lower") + "!", null));
            return;
        }
        steps.add(new Step(who + "'s " + name + (Math.abs(delta) > 1 ? (up ? " rose sharply!" : " fell harshly!") : (up ? " rose!" : " fell!")), () -> {
            stages[idx] += delta;
            if (onPlayer) {
                if (stat == 0) player.alterATKModifier(amount); else if (stat == 1) player.alterDEFModifier(amount); else player.alterSPDModifier(amount);
                playerSprite.playStatFx(up, statColor(stat));
            } else {
                if (stat == 0) foe.alterATKModifier(amount); else if (stat == 1) foe.alterDEFModifier(amount); else foe.alterSPDModifier(amount);
                if (foe == enemy) enemySprite.playStatFx(up, statColor(stat));
            }
        }));
    }

    private void playerHealBlock() {
        TurnHistoryScreen.Entry log = turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER, "HEAL", player.getCurrentHP());
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(playerName + " used HEAL!", null));
        steps.add(new Step(playerName + "'s HP was fully restored!", () -> {
            player.setCurrentHP(player.getMaxHP());   // BALANCE: now really fills gear HP too (Player.setCurrentHP fix)
            log.hit(0, player.getCurrentHP());   // no damage, but the HP column shows the HP after the heal
        }));
        insert(steps);
    }

    private void playerRunBlock() {
        turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER, "RUN", TurnHistoryScreen.N_TRGT);
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(playerName + " tries to run away...", null));
        if (boss) {
            steps.add(new Step("CAN'T ESCAPE!", null));
            insert(steps);
            return;
        }
        runAttempts++;
        boolean escaped = BattleMath.runEscapes(Math.round(playerSpd()), Math.round(enemy.getBaseSPD()), runAttempts, rng);
        if (escaped) {
            steps.add(new Step(playerName + " has fled!", () -> playerSprite.setPose(BattleSprites.BattleSprite.Pose.FLEE)));
            steps.add(new Step(null, () -> {
                script.clear();
                finish(Battle.BattleResult.FLED);
            }));
        } else {
            steps.add(new Step("CAN'T ESCAPE!", null));
        }
        insert(steps);
    }

    // ---------------------------------------------------------------- the enemy's move

    // The enemy hits with its ATK exactly like CombatResolver.resolveEnemyAttack (raw = ATK, then your DEF), and then your gear applies:
    // armor DEF (in effectiveDef), the armor's damage reduction, a chance for the shield to block part of it, and shield thorns.
    private EnemyMoves.Move lastEnemyMove;   // so the enemy does not use the same move twice in a row

    // the stat changes a move causes. The enemy's own changes always happen; changes to the knight only if the attack landed
    // (and, for attacks, by the move's chance and only while the knight is still standing).
    private void enemyEffects(List<Step> steps, EnemyMoves.Move move, boolean landed) {
        boolean toPlayer = false;
        for (EnemyMoves.Fx fx : move.effects()) if (fx.onPlayer) toPlayer = true;
        boolean playerFx = toPlayer && landed && (!move.damages || rng.nextFloat() < move.effectChance());
        if (playerFx && move.damages) steps.add(new Step(null, this::checkEnd));
        for (EnemyMoves.Fx fx : move.effects()) {
            if (fx.onPlayer && !playerFx) continue;
            statChangeAmount(steps, fx.onPlayer, fx.stat, fx.amount);
        }
    }

    private void enemyTurnBlock() {
        if (isOver() || !enemyAlive()) return;   // GLITCH: the main enemy may already be down while the glitch fights on
        if (titheWait) {   // GLITCH: BLOOD TITHE: it was tithed this round, so it holds its strike until the next one
            List<Step> wait = new ArrayList<>();
            wait.add(glitchStep(enemyName + " TREMBLES, GATHERING THE STOLEN STRENGTH...", null));
            insert(wait);
            return;
        }
        List<Step> steps = new ArrayList<>();
        // BALANCE: every enemy turn uses up one turn of BRACE (hit or not)
        boolean braced = braceTurns > 0;
        if (braced) braceTurns--;
        if (braced && braceTurns == 0) script.addFirst(new Step(playerName + "'s brace wore off.", null));   // shown after this turn's steps
        if (enemyStunned) {
            turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.ENEMY, "STUNNED", TurnHistoryScreen.N_TRGT);
            steps.add(new Step(enemyName + " is stunned and can't move!", () -> enemyStunned = false));
            insert(steps);
            return;
        }
        // an enemy class that implements BattleEnemy picks its own move (its own pattern, passives, boss phases);
        // the rest use the generic lists in EnemyMoves
        BattleEnemy brain = enemy instanceof BattleEnemy ? (BattleEnemy) enemy : null;
        EnemyMoves.Move move = brain != null ? brain.chooseBattleMove(rng, player) : null;
        if (move == null) move = EnemyMoves.pick(enemyName, rng, lastEnemyMove);
        lastEnemyMove = move;
        String note = brain != null ? brain.pollMessage() : null;
        if (note != null) steps.add(new Step(note, null));

        // BALANCE: enemy PP. A move used up this battle is replaced by a plain Desperate Strike.
        if (!move.flees) {
            int used = enemyPpUsed.getOrDefault(move.name, 0);
            if (used >= Balance.enemyMaxPP(move, boss)) {
                steps.add(new Step(enemyName + " is out of PP for " + move.name + "!", null));
                move = EnemyMoves.Move.attack(Balance.ENEMY_FALLBACK_NAME, Balance.ENEMY_FALLBACK_POWER);
            } else {
                enemyPpUsed.put(move.name, used + 1);
            }
        }
        TurnHistoryScreen.Entry log = turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.ENEMY, move.name,
                move.damages && !move.flees ? player.getCurrentHP() : TurnHistoryScreen.N_TRGT);

        // the enemy runs away (Starving Wretch): the fight ends with no rewards
        if (move.flees) {
            steps.add(new Step(enemyName + " ran away!", () -> enemySprite.setPose(BattleSprites.BattleSprite.Pose.FLEE)));
            steps.add(new Step(null, () -> {
                script.clear();
                finish(Battle.BattleResult.FLED);
            }));
            insert(steps);
            return;
        }
        final EnemyMoves.Move usedMove = move;
        steps.add(new Step(enemyName + " used " + move.name + "!", () -> {
            if (usedMove.damages) enemySprite.setPose(BattleSprites.BattleSprite.Pose.ATTACK);
            if (finalBoss != null) finalBoss.playMove(usedMove.name);   // FINAL BOSS: Dabbah's animation for this move
        }));

        // status moves do no damage: they just change stats (your guard and counter stay ready)
        if (!move.damages) {
            enemyEffects(steps, move, true);
            insert(steps);
            return;
        }
        if (playerGuard) {
            steps.add(new Step(playerName + " blocked the attack!", () -> {
                playerGuard = false;
                playerSprite.setPose(BattleSprites.BattleSprite.Pose.GUARD);
            }));
            enemyEffects(steps, move, false);
            insert(steps);
            return;
        }
        if (move.accuracy() < 1f && rng.nextFloat() >= move.accuracy()) {
            steps.add(new Step(enemyName + "'s " + move.name + " missed!", null));
            enemyEffects(steps, move, false);
            insert(steps);
            return;
        }
        if (rng.nextFloat() < BattleMath.dodgeChance(playerSpd(), enemy.getBaseSPD(), move.accuracy())) {   // BALANCE: DODGE FIX
            steps.add(new Step(playerName + " dodged!", null));
            enemyEffects(steps, move, false);
            insert(steps);
            return;
        }
        int dmg = 0;
        final int[] dealt = {0};
        for (int h = 0; h < move.hits(); h++) {
            if (h > 0) steps.add(new Step(null, this::checkEnd));   // stop the combo if the knight already fell
            // the move's multiplier scales the enemy's ATK (same as CombatResolver.resolveEnemyAttack(enemy, player, multiplier))
            // BALANCE: Pokemon's damage formula with the enemy's level, the move's power, its ATK / your DEF, x the random roll
            float dmgF = Balance.damage(enemyLevel, move.multiplier(), enemy.getBaseATK(), BattleMath.effectiveDef(player, inventory) * grief()) * Balance.roll(rng);   // GLITCH: GRIEVANCE halves DEF
            dmgF *= (float) (1 - BattleMath.damageReduction(inventory));
            if (braced) dmgF *= (float) (1 - Balance.BRACE_REDUCTION);   // BALANCE: BRACE
            // BALANCE: Dabbah lands critical hits (15% / 20% / 25% by form, x1.5); CINEMATIC: each one gets an impact frame
            final boolean bossCrit = finalBoss != null && rng.nextFloat() < ((Dabbah) enemy).critChance();
            if (bossCrit) dmgF *= Dabbah.CRIT_MULTIPLIER;
            boolean blocked = rng.nextDouble() < BattleMath.blockChance(inventory);
            if (blocked) dmgF *= (float) (1 - BattleMath.blockAmount(inventory));
            final int hit = Math.max(1, Math.round(dmgF));
            dmg = hit;
            steps.add(new Step((bossCrit ? "A critical hit! " : "") + enemyName + " hit for " + hit + " damage!", () -> {
                if (player.getCurrentHP() <= 0) return;
                if (bossCrit) finalBoss.critImpact();   // CINEMATIC
                dealt[0] += hit;
                if (!testMode) player.applyDamage(hit);   // GLITCH TEST MODE: the knight can't be hurt
                log.hit(hit, player.getCurrentHP());
                playerSprite.setPose(BattleSprites.BattleSprite.Pose.HURT);
            }));
            if (blocked) steps.add(new Step("The shield took some of the blow!", null));
        }
        // passives that react to the damage dealt (Hollow Chorus Zealot drains health)
        if (brain != null) {
            steps.add(new Step(null, () -> {
                String t = dealt[0] > 0 ? brain.onDealtDamage(dealt[0]) : null;
                if (t != null) {
                    List<Step> extra = new ArrayList<>();
                    extra.add(new Step(t, null));
                    insert(extra);
                }
            }));
        }

        int thorns = (int) Math.round(dmg * BattleMath.thorns(inventory));
        if (thorns > 0) {
            steps.add(new Step(enemyName + " was hurt by thorns for " + thorns + "!", () -> {
                TurnHistoryScreen.Entry thornsLog = turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER,
                        "THORNS", enemy.getCurrentHP());
                dealTo(enemy, thorns, thornsLog);
            }));
        }
        if (playerCounter && player.getCurrentHP() - dmg > 0) {
            playerCounter = false;
            steps.add(new Step(playerName + " counter-attacks!", () -> playerSprite.setPose(BattleSprites.BattleSprite.Pose.ATTACK)));
            float atk = BattleMath.effectiveAtk(player, inventory, inventory.getEquippedItem(Inventory.EquipSlot.MELEE)) * grief();
            int counter = Math.max(1, Math.round(Balance.damage(player.getLevel(), 1.2, atk, enemy.getBaseDEF()) * Balance.roll(rng)));   // BALANCE
            steps.add(new Step("It hit for " + counter + " damage!", () -> {
                TurnHistoryScreen.Entry counterLog = turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER, "RETAL", enemy.getCurrentHP());
                dealTo(enemy, counter, counterLog);
            }));
        }
        enemyEffects(steps, move, true);
        insert(steps);
    }

    // ---------------------------------------------------------------- GLITCH: the glitch entity's turn (it never does damage)

    // the moves SEVERANCE can still tear out: every learned, not-yet-severed move of the equipped items
    private List<Ability> severable() {
        List<Ability> out = new ArrayList<>();
        for (MoveCategory c : MoveCategory.values()) {
            Ability[] slots = AbilityLibrary.slotsFor(c, inventory.getEquippedItem(c.getSlot()));
            for (int i = 0; i < slots.length; i++)
                if (slots[i] != null && slotUnlocked(i) && !isSevered(slots[i])) out.add(slots[i]);
        }
        return out;
    }

    private void glitchTurnBlock() {
        if (isOver() || !glitchAlive()) return;
        List<Step> steps = new ArrayList<>();
        String gName = nameOf(glitch);
        if (griefCooldown > 0) griefCooldown--;
        if (glitchStunned) {
            turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.ENEMY, "STUNNED", TurnHistoryScreen.N_TRGT);
            steps.add(glitchStep(gName + " flickers and can't move!", () -> glitchStunned = false));
            insert(steps);
            return;
        }
        // it picks at random among the moves that would still do something
        List<GlitchEntity.GlitchMove> options = new ArrayList<>();
        if (!veiled) options.add(GlitchEntity.GlitchMove.VEIL_OF_STATIC);
        if (griefCooldown == 0 && griefTurns == 0) options.add(GlitchEntity.GlitchMove.GRIEVANCE);
        if (!severable().isEmpty()) options.add(GlitchEntity.GlitchMove.SEVERANCE);
        if (!tithed && enemyAlive()) options.add(GlitchEntity.GlitchMove.BLOOD_TITHE);   // needs its ally standing
        if (options.isEmpty()) {
            steps.add(glitchStep(gName + " IS WATCHING YOU.", null));
            insert(steps);
            return;
        }
        GlitchEntity.GlitchMove m = options.get(rng.nextInt(options.size()));
        turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.ENEMY, m.binary().substring(0, 8), TurnHistoryScreen.N_TRGT);
        steps.add(glitchStep(gName + " used " + m.binary() + "!", null));   // the name is shown only as binary
        switch (m) {
            case VEIL_OF_STATIC:
                steps.add(glitchStep("STATIC SWALLOWS YOUR HEALTH. YOU CAN'T SEE IT ANYMORE.", () -> veiled = true));
                break;
            case GRIEVANCE:
                // +1: the round it is used in counts down too, so the knight's next 4 turns are all affected
                steps.add(glitchStep("GRIEVANCE. EVERYTHING YOU ARE FALLS BY HALF.", () -> {
                    flashScenes = GriefFlashbacks.pickPair(rng, lastFlashScene);   // GLITCH: two different scenes
                    lastFlashScene = flashScenes[1];
                    flashback = FLASHBACK_FRAMES;
                    griefTurns = Balance.GRIEF_TURNS + 1;
                    griefCooldown = Balance.GRIEF_COOLDOWN;
                }));
                break;
            case SEVERANCE: {
                List<Ability> pool = severable();
                Ability cut = pool.get(rng.nextInt(pool.size()));
                steps.add(glitchStep(cut.getName() + " WAS TORN OUT OF YOU.", () -> severed.add(cut)));
                break;
            }
            default:
                // BLOOD TITHE: the ally is bled down to 1 HP and its ATK swells by 999%. It waits for the next round to
                // strike, so the knight gets one chance: be faster and finish it, or take a hit that will almost surely kill.
                steps.add(glitchStep(enemyName + " IS BLED DRY... AND SWELLS WITH STOLEN STRENGTH.", () -> {
                    tithed = true;
                    titheWait = true;
                    enemy.applyDamage(Math.max(0f, enemy.getCurrentHP() - 1f), null);   // down to exactly 1 HP
                    enemy.alterATKModifier(9.99f);                                       // +999% ATK
                    enemySprite.playStatFx(true, new Color(200, 20, 30));
                }));
                steps.add(glitchStep(enemyName + "'S ATK ROSE BY 999%! STRIKE FIRST.", null));
                break;
        }
        insert(steps);
    }

    // ---------------------------------------------------------------- results

    // GLITCH: damage to either enemy (was dealToEnemy, main enemy only)
    private void dealTo(Enemy foe, int dmg, TurnHistoryScreen.Entry log) {
        // null player on purpose: rewards are handed out by the victory steps so the XP bar can animate
        foe.applyDamage(dmg, null);
        log.hit(dmg, foe.getCurrentHP());
        if (foe == glitch) glitchHurt = 12; else enemySprite.setPose(BattleSprites.BattleSprite.Pose.HURT);
    }

    // GLITCH: the battle is over when the knight falls, or when every enemy is down
    private boolean isOver() {
        return player.getCurrentHP() <= 0 || pendingResult != null || (!enemyAlive() && !glitchAlive());
    }

    private void checkEnd() {
        if (pendingResult != null) return;
        // GLITCH: the glitch falls (the main enemy may still be standing)
        if (glitch != null && !glitchGone && glitch.getCurrentHP() <= 0) {
            glitchGone = true;
            if (enemyAlive()) {
                glitchCollapseSaid = true;
                List<Step> steps = new ArrayList<>();
                steps.add(glitchStep("THE STATIC COLLAPSES. IT IS GONE... FOR NOW.", null));
                insert(steps);
            }
        }
        if (!enemyDown && enemy.getCurrentHP() <= 0) {
            // a boss with phases (BattleEnemy.onBattleDefeat) can come back instead of falling
            String[] again = enemy instanceof BattleEnemy ? ((BattleEnemy) enemy).onBattleDefeat() : null;
            if (again != null) {
                script.clear();
                enemyStunned = false;
                for (String line : again) say(line, null);
                return;
            }
            enemyDown = true;
            if (glitchAlive()) {   // GLITCH: the glitch fights on alone
                enemyFaintSaid = true;
                List<Step> steps = new ArrayList<>();
                steps.add(new Step(enemyName + " fainted!", () -> enemySprite.setPose(BattleSprites.BattleSprite.Pose.FAINT)));
                insert(steps);
                return;
            }
        }
        if (enemyDown && !glitchAlive()) {
            script.clear();
            victory();
        } else if (player.getCurrentHP() <= 0) {
            script.clear();
            say(playerName + " fainted!", () -> playerSprite.setPose(BattleSprites.BattleSprite.Pose.FAINT));
            say("YOU WERE DEFEATED...", null);
            script.addLast(new Step(null, () -> finish(Battle.BattleResult.DEFEAT)));
        }
    }

    private void victory() {
        // BALANCE: a scaled enemy's xpReward is its XP YIELD; Pokemon's Gen 5 formula turns it into XP using both levels
        int xpSum = enemy.getScaledLevel() > 0 ? Balance.expGain(enemy.getXpReward(), enemyLevel, player.getLevel()) : enemy.getXpReward();
        int goldSum = enemy.getGoldReward();
        if (glitch != null) {   // GLITCH: killing it is worth its own XP and gold too
            xpSum += Balance.expGain(glitch.getXpReward(), glitchLevel, player.getLevel());
            goldSum += glitch.getGoldReward();
        }
        final int xp = xpSum, gold = goldSum;   // GLITCH: final copies, the lambdas below can only use final values
        // GLITCH: each enemy's "down" line is said once, whichever fell first
        if (finalBoss != null) say("DABBAH COLLAPSES AND SINKS BACK INTO THE EARTH...", () -> enemySprite.setPose(BattleSprites.BattleSprite.Pose.FAINT));   // FINAL BOSS
        else if (!enemyFaintSaid) say(enemyName + " fainted!", () -> enemySprite.setPose(BattleSprites.BattleSprite.Pose.FAINT));
        if (glitch != null && !glitchCollapseSaid) script.addLast(glitchStep("THE STATIC COLLAPSES.", null));
        say(playerName + " gained " + xp + " EXP!", () -> {
            int before = player.getLevel();
            player.addXP(xp);        // Player.addXP does the leveling (and the full heal on level up)
            player.addGold(gold);
            if (player.getLevel() > before) {
                targetXp = 1f;   // fill the bar, then the level-up message resets it
                List<Step> up = new ArrayList<>();
                up.add(new Step(playerName + " grew to LV " + player.getLevel() + "!", () -> {
                    shownLevel = player.getLevel();
                    shownXp = 0f;
                    targetXp = xpFraction();
                }));
                // BALANCE: announce the moves this level-up unlocked (one per type for each slot crossed)
                for (int slot = 1; slot < Balance.MOVE_UNLOCK_LEVELS.length; slot++) {
                    int at = Balance.moveUnlockLevel(slot);
                    if (before < at && player.getLevel() >= at) {
                        up.add(new Step(playerName + " learned " + AbilityLibrary.forCategory(MoveCategory.MELEE).get(slot).getName() + ", "
                                + AbilityLibrary.forCategory(MoveCategory.RANGED).get(slot).getName() + " and "
                                + AbilityLibrary.forCategory(MoveCategory.SHIELD).get(slot).getName() + "!", null));
                    }
                }
                insert(up);
            } else {
                targetXp = xpFraction();
            }
        });
        if (gold > 0) say("FOUND " + gold + " GOLD!", null);
        giveItemXp(xp);   // BALANCE
        script.addLast(new Step(null, () -> finish(Battle.BattleResult.VICTORY)));
    }

    // BALANCE: NEW. Every equipped item gains Balance.ITEM_XP_SHARE of the battle's XP; a line for each item that levels up.
    private void giveItemXp(int battleXp) {
        long share = Math.round(battleXp * Balance.ITEM_XP_SHARE);
        if (share <= 0) return;
        List<String> grew = new ArrayList<>();
        for (Inventory.EquipSlot slot : Inventory.EquipSlot.values()) {
            Item item = inventory.getEquippedItem(slot);
            if (item == null || item.getItemStats().isMaxLvl()) continue;
            if (item.getItemStats().addXp(share) > 0)
                grew.add(BattleUI.prettyName(item.getItemName()).toUpperCase() + " grew to LV " + item.getItemStats().getLvl() + "!");
        }
        say("YOUR GEAR GAINED " + share + " EXP!", null);
        for (String line : grew) say(line, null);
    }

    private void finish(Battle.BattleResult result) {
        pendingResult = result;
        phase = Phase.END_FADE;
        fade = 0;
        message = "";
    }

    private float xpFraction() {
        if (player.getLevel() >= Balance.MAX_LEVEL || player.getXpToNextLevel() <= 0) return 1f;   // BALANCE: cap read from Balance (100)
        return Math.min(1f, player.getCurrentXP() / (float) player.getXpToNextLevel());
    }

    // ---------------------------------------------------------------- draw

    // where the fighters stand (also used by the sprites' platforms)
    private int enemyFootX() { return BattleUI.W() - 190; }
    private int enemyFootY() { return 236; }
    private int playerFootX() { return 165; }
    private int playerFootY() { return BattleUI.barTop() - 8; }

    // the move log may only be opened once the fight has really started and before it is fading out
    private boolean historyAvailable() {
        return introTimer >= introFrames() && phase != Phase.INTRO && phase != Phase.END_FADE;   // FINAL BOSS: introFrames()
    }

    @Override
    // order matters here: backdrop first, then the fighters, then the plates, then the bottom bar on top
    public void draw(GraphicsHandler gh) {
        Graphics2D g = gh.getGraphics();
        int w = BattleUI.W(), h = BattleUI.H();
        AffineTransform saved = g.getTransform();
        // GLITCH: in a double battle the main enemy steps left and the glitch stands to its right
        int ex = glitch != null && glitchRevealed ? enemyFootX() - 80 : enemyFootX();   // it steps aside when the glitch appears
        if (finalBoss != null) {
            finalBoss.drawWorld(gh, tick);   // FINAL BOSS: the 3D battlefield with Dabbah and the knight
        } else {
            if (alone()) drawTornBackground(gh, w, h);   // GLITCH: the backdrop rips, the flashbacks show through
            else backgroundScreen.draw(gh);

            // platforms and fighters (the enemy is behind and smaller, the knight in front)
            drawShadow(g, ex, enemyFootY(), 150, 22);
            enemySprite.draw(gh, ex, enemyFootY(), tick + 40);
            if (glitch != null && !glitchGone && glitchRevealed) {
                drawShadow(g, glitchFootX(), enemyFootY(), 90, 16);
                drawGlitchSprite(gh, glitchFootX(), enemyFootY());
            }
            drawShadow(g, playerFootX(), playerFootY(), 190, 28);
            playerSprite.draw(gh, playerFootX(), playerFootY(), tick);
        }

        drawEnemyPlate(gh);
        if (glitch != null && !glitchGone && glitchRevealed) drawGlitchPlate(gh);   // GLITCH: stacked under the enemy's, like a Pokemon double battle
        drawPlayerPlate(gh);
        if (glitch != null && !glitchGone && glitchRevealed) drawGlitchChaos(gh, w, h);   // GLITCH: its "HP" spilling out
        if (phase == Phase.TARGET && (tick / 12) % 2 == 0) {      // GLITCH: a blinking arrow over the enemy being aimed at
            int ax = targetIndex == 0 ? ex : glitchFootX(), ay = targetIndex == 0 ? enemyFootY() - 165 : enemyFootY() - 150;
            g.setColor(BattleUI.GOLD);
            g.fillPolygon(new int[] { ax - 10, ax + 10, ax }, new int[] { ay, ay, ay + 14 }, 3);
        }
        drawTextBox(gh);
        inventoryDisplayScreen.draw(gh);
        optionScreen.draw(gh);
        abilityScreen.draw(gh);
        abilityInfoScreen.draw(gh);
        runAwayScreen.draw(gh);
        turnHistoryScreen.draw(gh);   // last of the HUD, so the expanded log (and its dim overlay) covers the menus too
        drawFlashback(gh, w, h);      // GLITCH: GRIEVANCE's flashback covers everything
        if (alone()) {                // GLITCH: a faint flashback over everything too, while it is alone
            Composite oc = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.10f + 0.05f * (float) Math.sin(tick * 0.03)));
            GriefFlashbacks.draw(g, aloneScene(), w, h, tick);
            g.setComposite(oc);
        }
        g.setTransform(saved);
        drawTakeover(gh, w, h);       // GLITCH: the takeover cutscene
        if (finalBoss != null) finalBoss.drawOverlay(gh, tick);   // CINEMATIC: the cinematic, its bars, the cut-in, over the HUD

        // the opening flash, then the fade to black when the battle is over
        if (introTimer < INTRO_FRAMES && (introTimer / 5) % 2 == 0) {
            g.setColor(new Color(255, 255, 255, 235 - introTimer * 5));
            g.fillRect(0, 0, w, h);
        }
        if (phase == Phase.END_FADE) {
            g.setColor(new Color(0, 0, 0, Math.min(255, fade * 255 / FADE_FRAMES)));
            g.fillRect(0, 0, w, h);
        }
        g.setStroke(new BasicStroke(1));   // the engine's drawRectangle leaves its stroke width behind, so put it back
    }

    // a soft dark shadow under a fighter so it does not float
    private void drawShadow(Graphics2D g, int cx, int cy, int w, int h) {
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(cx - w / 2, cy - h / 2, w, h);
    }

    // the name and level on the first row of a plate; long names drop to the smaller text size so they never run into the level
    private void nameAndLevel(GraphicsHandler gh, String name, String level, int x, int y, int rightEdge) {
        int room = rightEdge - x;
        int scale = BattleUI.textW(name, 2) + BattleUI.textW(level, 2) + 24 > room ? 1 : 2;
        BattleUI.text(gh, name, x, scale == 2 ? y : y + 4, scale, ink());
        BattleUI.textRight(gh, level, rightEdge, y, 2, ink());
    }

    // GLITCH: in a glitch battle every box is black, ringed with static, with white text
    private boolean dark() { return glitch != null; }
    private static final Color DARK_TEXT = new Color(240, 236, 245), DARK_SOFT = new Color(185, 175, 200);
    private Color ink() { return dark() ? DARK_TEXT : BattleUI.INK; }
    private Color inkSoft() { return dark() ? DARK_SOFT : BattleUI.INK_SOFT; }

    private void darkBox(GraphicsHandler gh, int x, int y, int w, int h, long seed) {
        gh.drawFilledRectangle(x, y, w, h, Color.BLACK);
        staticFrame(gh, x, y, w, h, 4, 8, seed);
    }

    private void drawEnemyPlate(GraphicsHandler gh) {
        int x = 0, y = 24, w = 330, h = 78;
        if (dark()) darkBox(gh, x, y, w, h, 3); else BattleUI.plate(gh, x, y, w, h, true);   // GLITCH
        // FINAL BOSS: Dabbah's plate shows the title of its current form (Beast of the Earth, Scourge of the Ash, The Unbound Ruin)
        String plateName = finalBoss != null ? ((Dabbah) enemy).getFormTitle().toUpperCase() : enemyName;
        nameAndLevel(gh, plateName, "LV" + enemyLevel, x + 22, y + 12, x + w - 44);
        float frac = shownEnemyHP / Math.max(1f, enemy.getMaxHP());
        BattleUI.hpTag(gh, x + 22, y + 34);
        BattleUI.bar(gh, x + 58, y + 34, w - 112, 16, frac, BattleUI.hpColor(frac));
        BattleUI.textRight(gh, Math.round(shownEnemyHP) + "/" + Math.round(enemy.getMaxHP()), x + w - 44, y + 58, 1, inkSoft());
        if (finalBoss != null) drawFormPips(gh, x + 22, y + 58);   // CINEMATIC
    }

    // CINEMATIC: three small dragon eyes under Dabbah's HP bar: the forms it has left (the current one glows and pulses)
    private void drawFormPips(GraphicsHandler gh, int x, int y) {
        Graphics2D g = gh.getGraphics();
        int form = ((Dabbah) enemy).getPhase();
        for (int i = 0; i < 3; i++) {
            int cx = x + 8 + i * 22, cy = y + 6;
            boolean gone = i < form - 1, now = i == form - 1;
            int a = now ? 160 + (int) (95 * Math.abs(Math.sin(tick * 0.06))) : 255;
            g.setColor(gone ? new Color(70, 64, 60) : (now ? new Color(230, 30, 30, a) : new Color(120, 20, 20)));
            g.fillPolygon(new int[]{cx - 9, cx, cx + 9, cx}, new int[]{cy, cy - 5, cy, cy + 5}, 4);
            g.setColor(gone ? new Color(40, 36, 34) : Color.BLACK);
            g.fillRect(cx - 1, cy - 4, 2, 8);   // the slit
        }
    }

    private void drawPlayerPlate(GraphicsHandler gh) {
        int x = BattleUI.W() - 392, y = BattleUI.barTop() - 112, w = 392, h = 100;
        if (dark()) darkBox(gh, x, y, w, h, 7); else BattleUI.plate(gh, x, y, w, h, false);   // GLITCH
        int left = x + 46, right = x + w - 18;
        nameAndLevel(gh, playerName, "LV" + shownLevel, left, y + 12, right);
        float frac = shownPlayerHP / Math.max(1f, player.getMaxHP());
        BattleUI.hpTag(gh, left, y + 34);
        BattleUI.bar(gh, left + 36, y + 34, right - left - 36, 16, frac, BattleUI.hpColor(frac));
        BattleUI.textRight(gh, Math.round(shownPlayerHP) + "/" + Math.round(player.getMaxHP()), right, y + 56, 2, ink());
        if (veiled) drawStatic(gh, left - 4, y + 30, right - left + 8, 44);   // GLITCH: VEIL OF STATIC hides the HP bar and numbers

        BattleUI.text(gh, "EXP", left, y + 80, 1, inkSoft());
        BattleUI.bar(gh, left + 36, y + 76, right - left - 36, 10, shownXp, BattleUI.BLUE);

    }

    // the narration box: half width while a menu is open, full width while text is playing
    private void drawTextBox(GraphicsHandler gh) {
        int y = BattleUI.barTop(), h = 96;   // a compact box; the space under it is left empty so the backdrop shows
        boolean menuOpen = phase == Phase.COMMAND || phase == Phase.ABILITY || phase == Phase.INFO || phase == Phase.CONFIRM_RUN;
        int x = BattleUI.boxX(), w = menuOpen ? BattleUI.boxW() : BattleUI.W() - 2 * x;
        if (phase == Phase.SCRIPT && messageGlitchy && !message.isEmpty()) { drawGlitchTextBox(gh, x, y, w, h); return; }   // GLITCH
        if (dark()) darkBox(gh, x, y, w, h, 11); else BattleUI.box(gh, x, y, w, h);   // GLITCH: black in a glitch battle
        int tx = x + 24, ty = y + 18;
        int maxChars = (w - 48) / 12;
        if ((phase == Phase.ABILITY || phase == Phase.INFO) && hint.isEmpty()) {   // BALANCE: a hint ("NO PP LEFT ...") replaces this
            MoveCategory cat = abilityScreen.getCategory();
            BattleUI.text(gh, cat.getLabel(), tx, ty - 4, 2, dark() ? AbilityScreen.colorFor(cat).brighter() : AbilityScreen.colorFor(cat).darker());
            String nm = abilityScreen.getItemName();
            if (nm.length() <= maxChars) BattleUI.text(gh, nm, tx, ty + 20, 2, ink());
            else {
                List<String> item = BattleUI.wrap(nm, (w - 48) / 6);
                for (int i = 0; i < item.size() && i < 2; i++) BattleUI.text(gh, item.get(i), tx, ty + 20 + i * 11, 1, ink());
            }
            BattleUI.text(gh, "ENTER USE   Y INFO", tx, y + h - 30, 1, inkSoft());
            BattleUI.text(gh, "1-4 JUMP   B BACK", tx, y + h - 18, 1, inkSoft());
            return;
        }
        String text;
        switch (phase) {
            case COMMAND: text = hint.isEmpty() ? "WHAT WILL " + playerName + " DO?" : hint; break;
            case ABILITY: case INFO: text = hint; break;
            case CONFIRM_RUN: text = "RUN FROM THE FIGHT?"; break;
            case TARGET: text = "HIT WHICH ONE?  LEFT / RIGHT, THEN ENTER.  B: BACK"; break;   // GLITCH
            default: text = message;
        }
        List<String> lines = BattleUI.wrap(text, maxChars);
        for (int i = 0; i < lines.size() && i < 3; i++) BattleUI.text(gh, lines.get(i), tx, ty + i * 24, 2, ink());
        // the little "press ENTER" arrow, blinking, while a message is waiting
        if (!menuOpen && current != null && (tick / 20) % 2 == 0 && phase == Phase.SCRIPT) {
            BattleUI.arrow(gh, x + w - 36, y + h - 34, 7, dark() ? DARK_TEXT : BattleUI.BOX_FRAME);
        }
    }

    // ---------------------------------------------------------------- GLITCH: drawing

    private int glitchFootX() { return enemyFootX() + 90; }

    private static final String[] SKULL = { ".###.", "#####", "#.#.#", ".###.", ".#.#." };

    private void drawSkull(GraphicsHandler gh, int x, int y, int px, Color c) {
        for (int r = 0; r < SKULL.length; r++)
            for (int k = 0; k < SKULL[r].length(); k++)
                if (SKULL[r].charAt(k) == '#') gh.drawFilledRectangle(x + k * px, y + r * px, px, px, c);
    }

    private static final Color[] STATIC = { new Color(10, 6, 16), new Color(42, 15, 61), new Color(90, 30, 138),
            new Color(138, 16, 32), new Color(20, 10, 26) };

    // the glitch's body: a tall tearing column of static, sliced sideways, with tiny skulls flickering inside
    private void drawGlitchSprite(GraphicsHandler gh, int cx, int footY) {
        Random r = new Random(tick / 3 * 13L);
        int h = 124, y0 = footY - h, cell = 4;
        for (int y = 0; y < h; y += cell) {
            int shift = r.nextInt(10) < 2 ? r.nextInt(21) - 10 : 0;   // the "tear": some rows jump sideways
            int bw = (int) (78 * (0.28 + 0.5 * Math.sin(Math.PI * Math.min(1.0, (y + 10) / (double) h))));
            for (int x = 0; x < bw; x += cell) {
                Color c = r.nextInt(40) == 0 ? new Color(232, 224, 240) : STATIC[r.nextInt(STATIC.length)];
                gh.drawFilledRectangle(cx - bw / 2 + x + shift, y0 + y, cell, cell, c);
            }
        }
        // red and blue ghosts of the outline, slightly offset (the colour-split of a broken signal)
        if (r.nextInt(3) == 0) gh.drawFilledRectangle(cx - 30 + r.nextInt(6), y0 + r.nextInt(h - 8), 60, 2, new Color(200, 20, 40, 160));
        if (r.nextInt(3) == 0) gh.drawFilledRectangle(cx - 30 - r.nextInt(6), y0 + r.nextInt(h - 8), 60, 2, new Color(60, 80, 220, 140));
        for (int k = 0; k < 4; k++)
            if (r.nextInt(3) == 0) drawSkull(gh, cx - 18 + r.nextInt(30), y0 + 18 + r.nextInt(h - 40), 2, new Color(232, 224, 240));
        if ((tick / 20) % 9 != 0) {   // two red eyes, blinking now and then
            gh.drawFilledRectangle(cx - 9, y0 + 22, 4, 2, new Color(230, 30, 20));
            gh.drawFilledRectangle(cx + 5, y0 + 22, 4, 2, new Color(230, 30, 20));
        }
        if (glitchHurt > 0 && glitchHurt % 4 < 2) gh.drawFilledRectangle(cx - 40, y0, 80, h, new Color(255, 255, 255, 150));
    }

    // a frame of live static, thick cells, around a rectangle (the glitch's plate and its text box)
    private void staticFrame(GraphicsHandler gh, int x, int y, int w, int h, int cell, int thick, long seed) {
        Random r = new Random(tick / 3 * 31L + seed);
        for (int yy = 0; yy < h; yy += cell)
            for (int xx = 0; xx < w; xx += cell) {
                boolean edge = xx < thick || yy < thick || xx >= w - thick || yy >= h - thick;
                // the inner edge frays: a few cells reach in by one more cell
                boolean fray = !edge && (xx < thick + cell || yy < thick + cell || xx >= w - thick - cell || yy >= h - thick - cell) && r.nextInt(4) == 0;
                if (edge || fray) gh.drawFilledRectangle(x + xx, y + yy, Math.min(cell, w - xx), Math.min(cell, h - yy),
                        r.nextInt(30) == 0 ? new Color(232, 224, 240) : STATIC[r.nextInt(STATIC.length)]);
            }
        if (r.nextInt(4) == 0) drawSkull(gh, x + r.nextInt(Math.max(1, w - 12)), y + (r.nextBoolean() ? 1 : h - 11), 2, new Color(232, 224, 240));
    }

    // how hurt the glitch is: 0 = full HP, 1 = nearly dead. Drives everything that goes wrong with its plate.
    private float glitchDistortion() {
        if (glitch == null) return 0f;
        return 1f - Math.max(0f, Math.min(1f, shownGlitchHP / Math.max(1f, glitch.getMaxHP())));
    }

    // its plate: a black box ringed with live static around the HP bar; unreadable name, LV???, a bar of tiny skulls,
    // all its text in white. The bar never empties. Instead, the more it is hurt (glitchDistortion):
    //   any damage   the skulls shake, more and more
    //   past 25%     skulls break out of the bar and poke out above and below the box
    //   past 60%     the whole plate starts to spin, faster as it gets closer to dying (upside down and round again)
    private void drawGlitchPlate(GraphicsHandler gh) {
        int x = 0, y = 108, w = 330, h = 78;
        float d = glitchDistortion();
        Graphics2D g = gh.getGraphics();
        AffineTransform before = g.getTransform();
        if (d > 0.6f) {   // spinning around its own middle
            double speed = (d - 0.6) / 0.4;   // 0..1
            double angle = Math.sin(tick * 0.015 * (1 + speed * 3)) * Math.PI * speed + (d > 0.85f ? tick * 0.03 * speed : 0);
            g.rotate(angle, x + w / 2.0, y + h / 2.0);
        }
        gh.drawFilledRectangle(x, y, w, h, Color.BLACK);
        staticFrame(gh, x, y, w, h, 4, 8 + Math.round(d * 6), 5);
        Color white = new Color(240, 236, 245);
        int shake = (tick / 3) % 11 == 0 ? 2 : 0;   // now and then the text jumps sideways
        BattleUI.text(gh, GlitchEntity.glitchName(tick, 8), x + 22 + shake, y + 14, 2, white);
        BattleUI.textRight(gh, "LV???", x + w - 22 - shake, y + 14, 2, white);
        BattleUI.text(gh, "HP", x + 22, y + 38, 1, white);
        int bx = x + 48, by = y + 34, bw = w - 70, bh = 16;
        gh.drawFilledRectangle(bx, by, bw, bh, new Color(20, 12, 28));
        gh.drawFilledRectangle(bx + 2, by + 2, bw - 4, bh - 4, new Color(60 + Math.round(d * 80), 14, 70));   // always full
        Random r = new Random(tick / 2 * 41L);
        for (int k = 0, sx = bx + 4; sx + 10 <= bx + bw - 2; sx += 13, k++) {
            int jx = d > 0 ? Math.round((r.nextFloat() - 0.5f) * d * 12) : 0, jy = d > 0 ? Math.round((r.nextFloat() - 0.5f) * d * 10) : 0;
            // past 25% some skulls break out of the bar, up or down, beyond the edge of the box
            if (d > 0.25f && r.nextFloat() < (d - 0.25f) * 0.9f)
                jy += (r.nextBoolean() ? -1 : 1) * (20 + r.nextInt(Math.round(10 + d * 50)));
            Color c = (k + tick / 8) % 7 == 0 || r.nextFloat() < d * 0.3f ? new Color(200, 30, 40) : new Color(220, 210, 230);
            drawSkull(gh, sx + jx, by + 3 + jy, d > 0.5f && r.nextInt(4) == 0 ? 3 : 2, c);
        }
        gh.drawRectangle(bx, by, bw - 1, bh - 1, white, 2);
        BattleUI.textRight(gh, d > 0.5f ? GlitchEntity.glitchName(tick + 3, 7) : "???/???", x + w - 22, y + 58, 1, white);
        g.setTransform(before);
    }

    // GLITCH: the part of the glitch's "HP" that leaves its box (drawn over the field, under the text box and menus,
    // so the fight stays playable):
    //   past 40%   skulls tear off the bar and fly out toward the player, growing as they come at the screen
    //   past 75%   static creeps over the whole screen, thicker the closer it is to dying
    //   past 90%   a huge faint skull fills the middle of the screen
    private void drawGlitchChaos(GraphicsHandler gh, int w, int h) {
        float d = glitchDistortion();
        if (d <= 0.4f) return;
        int fromX = 165, fromY = 150;   // the middle of its HP bar
        int flying = Math.round((d - 0.4f) / 0.6f * 16);
        for (int k = 0; k < flying; k++) {
            float p = ((tick + k * 37) % 80) / 80f;           // each skull's trip from the bar to the viewer
            Random r = new Random(k * 977L + (tick + k * 37) / 80);
            int tx = r.nextInt(w), ty = h / 4 + r.nextInt(h / 2);  // where it is heading
            int px = Math.round(fromX + (tx - fromX) * p), py = Math.round(fromY + (ty - fromY) * p);
            int size = 2 + Math.round(p * p * (4 + d * 14));       // grows as it comes "toward" you
            int alpha = Math.round(255 * (p < 0.8f ? 1f : (1f - p) / 0.2f));
            Color c = k % 3 == 0 ? new Color(200, 30, 40, alpha) : new Color(232, 224, 240, alpha);
            drawSkull(gh, px - size * 5 / 2, py - size * 5 / 2, size, c);
        }
        if (d > 0.75f) {   // static creeping over the screen
            Random r = new Random(tick / 3 * 59L);
            float density = Math.min(0.35f, (d - 0.75f) * 1.4f);
            for (int yy = 0; yy < h; yy += 8)
                for (int xx = 0; xx < w; xx += 8)
                    if (r.nextFloat() < density) {
                        Color c = STATIC[r.nextInt(STATIC.length)];
                        gh.drawFilledRectangle(xx, yy, 8, 8, new Color(c.getRed(), c.getGreen(), c.getBlue(), 190));
                    }
        }
        if (d > 0.9f && (tick / 30) % 3 != 0) {   // a huge faint skull in the middle
            int px = 22;
            drawSkull(gh, w / 2 - px * 5 / 2, h / 2 - px * 5 / 2 - 40, px, new Color(232, 224, 240, 40));
        }
    }

    // GLITCH: the text box while the glitch speaks: solid black, ringed with static, white text that loses letters and jitters
    private void drawGlitchTextBox(GraphicsHandler gh, int x, int y, int w, int h) {
        gh.drawFilledRectangle(x, y, w, h, Color.BLACK);
        staticFrame(gh, x, y, w, h, 4, 8, 11);
        Random r = new Random(tick / 4 * 17L);
        int tx = x + 24, ty = y + 18, maxChars = (w - 48) / 12;
        List<String> lines = BattleUI.wrap(message, maxChars);
        for (int i = 0; i < lines.size() && i < 3; i++) {
            int jx = r.nextInt(8) == 0 ? r.nextInt(7) - 3 : 0;
            String line = GlitchEntity.corrupt(lines.get(i), tick + i * 7, 0.06);
            if (r.nextInt(5) == 0) BattleUI.text(gh, line, tx + jx + 2, ty + i * 24, 2, new Color(200, 20, 40, 170));   // red ghost
            BattleUI.text(gh, line, tx + jx, ty + i * 24, 2, new Color(240, 236, 245));
        }
        if (current != null && (tick / 20) % 2 == 0) BattleUI.arrow(gh, x + w - 36, y + h - 34, 7, new Color(240, 236, 245));
    }

    // GLITCH: the TAKEOVER. Static eats the screen from scattered points (about 1 s), holds with the glitch's name and binary
    // pouring down the middle, then drains away (about 1 s) leaving the glitch standing beside the enemy.
    private void drawTakeover(GraphicsHandler gh, int w, int h) {
        if (!takeoverPlaying()) return;
        int t = takeover, in = TAKEOVER_FRAMES * 2 / 5, hold = TAKEOVER_FRAMES / 5;
        double cover = t < in ? t / (double) in : t < in + hold ? 1.0 : 1.0 - (t - in - hold) / (double) (TAKEOVER_FRAMES - in - hold);
        cover = Math.max(0, Math.min(1, cover));
        int cell = 8;
        Random seedField = new Random(99);   // each cell's fixed "turn" to be eaten, so the static spreads instead of flickering
        Random r = new Random(tick / 2 * 7L);
        for (int yy = 0; yy < h; yy += cell)
            for (int xx = 0; xx < w; xx += cell) {
                // cells near a few seed points go first, so it spreads out from them
                double order = seedField.nextDouble() * 0.35 + 0.65 * nearSeed(xx, yy, w, h);
                if (order < cover) gh.drawFilledRectangle(xx, yy, cell, cell, r.nextInt(40) == 0 ? new Color(232, 224, 240) : STATIC[r.nextInt(STATIC.length)]);
            }
        // tears: whole rows of static slide sideways
        for (int k = 0; k < 3; k++)
            if (r.nextInt(2) == 0) gh.drawFilledRectangle(0, r.nextInt(h), w, 2 + r.nextInt(5), new Color(0, 0, 0, (int) (200 * cover)));
        if (cover > 0.85) {   // fully covered: skulls, binary rain, its name
            for (int k = 0; k < 10; k++) drawSkull(gh, r.nextInt(w - 20), r.nextInt(h - 20), 3, new Color(232, 224, 240));
            String bits = GlitchEntity.toBinary("I SEE YOU");
            for (int col = 0; col < 6; col++) {
                int cx = 60 + col * (w - 120) / 5;
                for (int row = 0; row < 14; row++) {
                    int idx = (row + t / 2 + col * 5) % bits.length();
                    BattleUI.text(gh, String.valueOf(bits.charAt(idx)), cx, (row * 40 + t * 4 + col * 23) % h, 2, new Color(150, 90, 210));
                }
            }
            String name = GlitchEntity.glitchName(tick, 9);
            gh.drawFilledRectangle(w / 2 - BattleUI.textW(name, 4) / 2 - 16, h / 2 - 34, BattleUI.textW(name, 4) + 32, 60, Color.BLACK);
            BattleUI.textCenter(gh, name, w / 2 + (r.nextInt(5) == 0 ? 3 : 0), h / 2 - 20, 4, new Color(240, 236, 245));
        }
    }

    // GLITCH: which flashback is behind the rips right now (a new one every 4 seconds, looping through all of them)
    private int aloneScene() { return (aloneSceneBase + aloneT / 240) % GriefFlashbacks.COUNT; }

    // GLITCH: the backdrop tearing apart. The flashbacks loop underneath; the backdrop is drawn over them in horizontal
    // strips that slide apart, two jagged vertical rips open wider and wider, and some strips drop out for a moment.
    // It gets worse for about 4 seconds, then stays that broken until the glitch is gone.
    private void drawTornBackground(GraphicsHandler gh, int w, int h) {
        Graphics2D g = gh.getGraphics();
        GriefFlashbacks.draw(g, aloneScene(), w, h, aloneT);   // what is underneath
        if (tornBuf == null || tornBuf.getWidth() != w || tornBuf.getHeight() != h)
            tornBuf = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = tornBuf.createGraphics();
        GraphicsHandler bgh = new GraphicsHandler();
        bgh.setGraphics(bg);
        backgroundScreen.draw(bgh);
        bg.dispose();

        float rip = Math.min(1f, aloneT / 240f);
        int[] ripX = { w * 35 / 100, w * 70 / 100 };
        Random slow = new Random(aloneT / 20 * 7919L);   // changes 3 times a second: which strips drop out
        Random jolt = new Random(tick / 3 * 104729L);     // sudden jumps
        int y = 0, band = 0;
        while (y < h) {
            Random fixed = new Random(band * 31L + 5);    // each strip's own height and rip shape, so the tears hold still
            int bh = 6 + fixed.nextInt(18);
            int shift = Math.round((float) Math.sin(tick * 0.02 + band) * rip * 24) + (jolt.nextInt(12) == 0 ? (jolt.nextInt(41) - 20) : 0);
            boolean dropped = slow.nextFloat() < rip * 0.22f;   // the strip is gone: the flashback shows through
            if (!dropped) {
                // the strip is cut at the two rips; each piece moves outward from its rip as the rip opens
                int prev = 0, pieceShift = shift - Math.round(rip * 30);
                for (int k = 0; k <= ripX.length; k++) {
                    int cut = k < ripX.length ? ripX[k] + fixed.nextInt(25) - 12 : w;
                    int gap = k < ripX.length ? Math.round(rip * (20 + fixed.nextInt(40))) : 0;
                    int sx1 = prev, sx2 = Math.max(prev, cut - gap / 2);
                    if (sx2 > sx1) g.drawImage(tornBuf, sx1 + pieceShift, y, sx2 + pieceShift, y + bh, sx1, y, sx2, y + bh, null);
                    // a fringe of static along the torn edge
                    if (k < ripX.length && rip > 0.05f)
                        for (int yy = y; yy < y + bh; yy += 4)
                            gh.drawFilledRectangle(sx2 + pieceShift, yy, 4, 4, STATIC[jolt.nextInt(STATIC.length)]);
                    prev = Math.min(w, cut + gap / 2);
                    pieceShift = shift + Math.round(rip * 30 * (k + 1) / ripX.length);
                }
            }
            y += bh;
            band++;
        }
        // a few strips in the wrong colours, like a broken signal
        if (jolt.nextInt(3) == 0) gh.drawFilledRectangle(0, jolt.nextInt(h), w, 3, new Color(200, 20, 40, 120));
        if (jolt.nextInt(3) == 0) gh.drawFilledRectangle(0, jolt.nextInt(h), w, 2, new Color(60, 80, 220, 110));
    }

    // 0 at one of the seed points, up to 1 far from all of them
    private static double nearSeed(int x, int y, int w, int h) {
        int[][] seeds = { { w * 3 / 4, h / 4 }, { w / 5, h / 3 }, { w / 2, h * 3 / 4 }, { w - 40, h - 60 } };
        double best = 1;
        for (int[] s : seeds) best = Math.min(best, Math.hypot(x - s[0], y - s[1]) / Math.hypot(w, h) * 1.8);
        return Math.min(1, best);
    }

    // VEIL OF STATIC: a block of dark static with a skull or two in it
    private void drawStatic(GraphicsHandler gh, int x, int y, int w, int h) {
        Random r = new Random(tick / 3 * 7L);
        for (int yy = 0; yy < h; yy += 3)
            for (int xx = 0; xx < w; xx += 3)
                gh.drawFilledRectangle(x + xx, y + yy, 3, 3, STATIC[r.nextInt(STATIC.length)]);
        if (r.nextInt(2) == 0) drawSkull(gh, x + 6 + r.nextInt(Math.max(1, w - 20)), y + 6 + r.nextInt(Math.max(1, h - 16)), 2, new Color(220, 210, 230));
    }

    // GRIEVANCE's flashback: two of the 11 scenes in GriefFlashbacks (calvary, graveyard, famine dead, burning minster,
    // the rite, the shield, the wood, the ossuary, the weeping window, the drowned, the mourner), one per slow fade,
    // each with its line from the story. Two smooth fades over 2.5 s (well under 3 flashes a second).
    private void drawFlashback(GraphicsHandler gh, int w, int h) {
        if (flashback <= 0) return;
        int t = FLASHBACK_FRAMES - flashback;
        float a = (float) Math.abs(Math.sin(Math.PI * t / (FLASHBACK_FRAMES / 2.0)));   // two smooth pulses
        if (a < 0.02f) return;
        int scene = flashScenes[t < FLASHBACK_FRAMES / 2 ? 0 : 1];
        Graphics2D g = gh.getGraphics();
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, a)));
        GriefFlashbacks.draw(g, scene, w, h, t);
        BattleUI.textCenter(gh, GriefFlashbacks.line(scene), w / 2, h - 40, 2, new Color(220, 40, 40));
        g.setComposite(old);
    }

    // ---------------------------------------------------------------- getters used by the option screens

    BattleSprites.BattleSprite getPlayerSprite() { return playerSprite; }
    Inventory getInventory() { return inventory; }
    int getHealsLeft() { return healsLeft; }
    boolean isBoss() { return boss; }
    String getPlayerName() { return playerName; }
    public Battle.BattleStatus getStatus() { return status; }
    public boolean isAwaitingCommand() { return phase == Phase.COMMAND; }
    public BattleWorld getWorld() { return world; }

    // ---------------------------------------------------------------- the main menu: MELEE / SHIELD / RANGED / HEAL / RUN

    private static final class MainMenu extends OptionScreen {
        private static final String[] LABELS = {"MELEE", "SHIELD", "RANGED", "HEAL", "RUN"};
        private static final Color[] COLORS = {BattleUI.BTN_MELEE, BattleUI.BTN_SHIELD, BattleUI.BTN_RANGED, BattleUI.BTN_HEAL, BattleUI.BTN_RUN};

        MainMenu(PlayBattleScreen battle) {
            super(battle);
        }

        void setCursor(int c) { cursor = c; }

        @Override
        protected int optionCount() { return LABELS.length; }

        @Override
        protected BattleSprites.BattleSprite.Pose poseFor(int index) {
            return index == 3 ? BattleSprites.BattleSprite.Pose.HEAL : BattleSprites.BattleSprite.Pose.IDLE;   // the others only open another window
        }

        @Override
        protected void perform(int index) {
            switch (index) {
                case 0: close(); battle.openAbilities(MoveCategory.MELEE); break;
                case 1: close(); battle.openAbilities(MoveCategory.SHIELD); break;
                case 2: close(); battle.openAbilities(MoveCategory.RANGED); break;
                case 3: battle.useHeal(); break;
                default: close(); battle.openRunConfirm(); break;
            }
        }

        // BALANCE: MELEE (0) and RANGED (2) are greyed out while the knight recharges
        private boolean lockedByRecharge(int i) {
            return (i == 0 || i == 2) && battle.isRecharging();
        }

        @Override
        public void update() {
            if (!open) return;
            navigate(3);
            if (BattleInput.confirm()) {
                // a heal that is used up is refused BEFORE the sprite changes
                if (cursor == 3 && battle.getHealsLeft() <= 0) battle.showHint("HEAL ALREADY USED THIS BATTLE!");
                else if (lockedByRecharge(cursor)) battle.showHint(battle.getPlayerName() + " MUST RECHARGE!");   // BALANCE
                else interact(cursor);
            }
        }

        @Override
        public void draw(GraphicsHandler gh) {
            if (!open) return;
            int x = BattleUI.btnX(), y = BattleUI.barTop() + 2, w = BattleUI.btnW(), gap = 8;
            int topH = 68, botH = 66;
            for (int i = 0; i < LABELS.length; i++) {
                int bx, by, bw, bh;
                if (i < 3) { bw = (w - 2 * gap) / 3; bx = x + i * (bw + gap); by = y; bh = topH; }
                else { bw = (w - gap) / 2; bx = x + (i - 3) * (bw + gap); by = y + topH + gap; bh = botH; }
                boolean hover = i == cursor;
                boolean dead = (i == 3 && battle.getHealsLeft() <= 0) || lockedByRecharge(i);   // BALANCE: recharge greys it
                BattleUI.button(gh, bx, by, bw, bh, COLORS[i], hover, dead);
                boolean hasSub = !subtitle(i).isEmpty();   // buttons without a subtitle get their label centered
                int ly = hasSub ? by + 11 : by + bh / 2 - 7;
                BattleUI.textCenter(gh, LABELS[i], bx + bw / 2 + 1, ly + 1, 2, BattleUI.SHADOW);
                BattleUI.textCenter(gh, LABELS[i], bx + bw / 2, ly, 2, BattleUI.WHITE);
                List<String> sub = BattleUI.wrap(subtitle(i), Math.max(8, (bw - 20) / 6));
                for (int k = 0; k < sub.size() && k < 2; k++) BattleUI.shadowText(gh, sub.get(k), bx + bw / 2 - BattleUI.textW(sub.get(k), 1) / 2, by + 36 + k * 10, 1, BattleUI.WHITE);
            }
        }

        private String subtitle(int i) {
            if (lockedByRecharge(i)) return "Recharging";   // BALANCE
            return switch (i) {
                case 0, 1, 2 -> "";
                case 3 -> battle.getHealsLeft() > 0 ? "Full HP  x" + battle.getHealsLeft() : "Used up";
                default -> "Flee the fight";
            };
        }
    }
}