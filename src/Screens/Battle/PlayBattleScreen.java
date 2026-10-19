package Screens.Battle;

import BattleSystem.*;

import Engine.CombatResolver;
import Engine.GraphicsHandler;
import Engine.Screen;
import Item.Item;
import Level.Player;
import NPCs.Enemy;
import BattleSystem.Ability.Effect;
import inventory.EquipSlot;
import inventory.Inventory;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
public class PlayBattleScreen extends Screen {
    private enum Phase { INTRO, COMMAND, ABILITY, INFO, CONFIRM_RUN, SCRIPT, END_FADE }

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
    private BattleSprite playerSprite, enemySprite;

    // what the plates show (they glide toward the real numbers, like Gen 1's draining bars)
    private float shownPlayerHP, shownEnemyHP, shownXp, targetXp;
    private int shownLevel;
    private String playerName = "KNIGHT", enemyName = "ENEMY";

    // battle state
    private int healsLeft = 1, runAttempts;
    private boolean playerGuard, playerCounter, enemyStunned;
    private String hint = "";
    private int hintTimer;

    // the step queue
    private static final class Step {
        final String text;      // null = silent step (just runs its action)
        final Runnable action;
        Step(String text, Runnable action) { this.text = text; this.action = action; }
    }
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

    @Override
    public void initialize() {
        BattleInput.reset();
        playerName = player.getClass().getSimpleName().toUpperCase();
        enemyName = enemy.getEnemyName().toUpperCase();
        healsLeft = boss ? 2 : 1;
        runAttempts = 0;
        tick = 0;
        introTimer = 0;
        playerGuard = playerCounter = enemyStunned = false;
        hint = "";
        player.resetStatModifiers();
        enemy.resetStatModifiers();
        if (enemy instanceof BattleEnemy) ((BattleEnemy) enemy).onBattleStart();
        if (enemy instanceof BattleEnemy && ((BattleEnemy) enemy).battleLevel() > 0) enemyLevel = ((BattleEnemy) enemy).battleLevel();

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

        playerSprite = BattleSprites.knight();
        enemySprite = BattleSprites.enemy(enemy.getEnemyName());

        shownPlayerHP = player.getCurrentHP();
        shownEnemyHP = enemy.getCurrentHP();
        shownLevel = player.getLevel();
        shownXp = targetXp = xpFraction();

        status = Battle.BattleStatus.STARTED;
        pendingResult = null;
        current = null;
        script.clear();

        say(enemyName + " blocks your path!", null);
        say("GO! " + playerName + "!", null);
        script.addLast(new Step(null, () -> { status = Battle.BattleStatus.IN_PROGRESS; toCommands(); }));
        phase = Phase.SCRIPT;   // the opening flash is drawn while introTimer counts up, the script waits for it
    }

    // ---------------------------------------------------------------- update

    @Override
    public void update() {
        tick++;
        BattleInput.update();
        if (introTimer < INTRO_FRAMES) introTimer++;
        backgroundScreen.update();
        playerSprite.update();
        enemySprite.update();
        turnHistoryScreen.updateScreen(historyAvailable());
        glideBars();
        if (hintTimer > 0 && --hintTimer == 0) hint = "";
        if (turnHistoryScreen.isExpanded()) return;   // the full move log is open: the battle is paused until it closes

        switch (phase) {
            case COMMAND: optionScreen.update(); break;
            case ABILITY: abilityScreen.update(); break;
            case INFO: abilityInfoScreen.update(); break;
            case CONFIRM_RUN: runAwayScreen.update(); break;
            case SCRIPT: if (introTimer >= INTRO_FRAMES) runScript(); break;
            case END_FADE:
                if (++fade >= FADE_FRAMES) {
                    status = Battle.BattleStatus.COMPLETED;
                    player.resetStatModifiers();
                    enemy.resetStatModifiers();
                    if (listener != null) listener.onBattleEnd(pendingResult);
                }
                break;
            default: break;
        }
    }

    private void glideBars() {
        shownPlayerHP = glide(shownPlayerHP, player.getCurrentHP(), 0.55f);
        shownEnemyHP = glide(shownEnemyHP, enemy.getCurrentHP(), 0.55f);
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
        while (current == null) {
            current = script.pollFirst();
            if (current == null) { scriptFinished(); return; }
            if (current.action != null) current.action.run();
            if (current.text == null) current = null;
            else { message = current.text; stepTimer = 0; }
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

    // ---------------------------------------------------------------- the player's moves

    // an ability was chosen in the ability window (or from the info window)
    void useAbility(Ability ability) {
        startRound(() -> playerAbilityBlock(ability), false);
    }

    void useHeal() {
        if (healsLeft <= 0) { showHint("HEAL ALREADY USED THIS BATTLE!"); return; }
        healsLeft--;
        startRound(this::playerHealBlock, true);
    }

    void attemptRun() {
        startRound(this::playerRunBlock, true);
    }

    // queue one round: both fighters act (SPD decides who is first, heal/run always go first), then the round ends
    private void startRound(Runnable playerBlock, boolean priority) {
        turnHistoryScreen.nxtTurn();
        phase = Phase.SCRIPT;
        optionScreen.close();
        abilityScreen.close();
        abilityInfoScreen.close();
        runAwayScreen.close();
        boolean playerFirst = priority || BattleMath.initiative(playerSpd(), rng) >= BattleMath.initiative(enemy.getBaseSPD(), rng);
        Runnable p = playerBlock, e = this::enemyTurnBlock;
        script.addLast(new Step(null, playerFirst ? p : e));
        script.addLast(new Step(null, this::checkEnd));
        script.addLast(new Step(null, playerFirst ? e : p));
        script.addLast(new Step(null, this::checkEnd));
    }

    private float playerSpd() {
        return BattleMath.effectiveSpd(player, inventory);
    }

    private void playerAbilityBlock(Ability a) {
        if (isOver()) return;
        Item weapon = inventory.getEquippedItem(a.getCategory().getSlot());   // sword / bow / shield (null = bare hands)
        boolean stance = a.getEffect() == Effect.GUARD || a.getEffect() == Effect.COUNTER;
        TurnHistoryScreen.Entry log = turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER, a.getName(),
                stance ? TurnHistoryScreen.N_TRGT : enemy.getCurrentHP());
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(playerName + " used " + a.getName() + "!", null));

        // moves that don't hit: GUARD and COUNTER just set a stance
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
        float atk = BattleMath.effectiveAtk(player, inventory, weapon);
        double critChance = BattleMath.critChance(weapon);
        for (int i = 0; i < hits; i++) {
            // the move's base accuracy (plus the weapon's ACCURACY buff) decides if it lands
            boolean accurate = rng.nextDouble() < BattleMath.accuracy(a, weapon);
            boolean dodged = rng.nextFloat() < BattleMath.dodgeChance(enemy.getBaseSPD(), playerSpd());
            if (!accurate || dodged) {
                steps.add(new Step(accurate ? enemyName + " dodged!" : playerName + " missed!", null));
                continue;
            }
            float base = atk;
            if (a.getEffect() == Effect.THROW) base = atk * (float) (1 + BattleMath.blockAmount(inventory));   // better shields hit harder
            double raw = base * a.getPower();
            boolean crit = rng.nextDouble() < critChance;
            if (crit) raw *= BattleMath.critMultiplier(weapon);
            if (a.getEffect() == Effect.FINISHER && enemy.getCurrentHP() < enemy.getMaxHP() * 0.3f) raw *= 2;
            // the weapon's PIERCING buff and the PIERCE move both let hits ignore part of the enemy's DEF
            float def = enemy.getBaseDEF() * (float) (1 - BattleMath.piercing(weapon)) * (a.getEffect() == Effect.PIERCE ? 0.5f : 1f);
            int dmg = Math.max(1, Math.round(CombatResolver.calculateDamage((float) raw, def)));
            final boolean c = crit;
            steps.add(new Step(c ? "A critical hit! " + dmg + " damage!" : (hits > 1 ? "HIT! " + dmg + " damage!" : "It hit for " + dmg + " damage!"),
                    () -> dealToEnemy(dmg, log)));
            if (a.getEffect() == Effect.LIFESTEAL) {
                int heal = Math.max(1, dmg / 2);
                steps.add(new Step(playerName + " drained " + heal + " HP!", () -> {
                    player.setCurrentHP(player.getCurrentHP() + heal);
                    // no damage, but the HP column shows the knight's HP after the drain (same as HEAL)
                    turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER, "DRAIN", player.getCurrentHP())
                            .hit(0, player.getCurrentHP());
                    }));
            }
        }
        // stat effects only happen if the enemy is still standing after the hits land
        steps.add(new Step(null, this::checkEnd));
        switch (a.getEffect()) {
            case ATK_UP: statChange(steps, true, 0, +1); break;
            case SPD_UP: statChange(steps, true, 2, +1); break;
            case DEF_DOWN: statChange(steps, false, 1, -1); break;
            case STUN:
                if (rng.nextFloat() < 0.4f) steps.add(new Step(enemyName + " is stunned!", () -> enemyStunned = true));
                break;
            default: break;
        }
        insert(steps);
    }

    // ---------------------------------------------------------------- stat changes (shared by your moves and the enemy's)

    // Like Pokemon, each stat can go up or down by "stages" (here 4 at most each way). One stage changes the stat by 15%.
    private static final int MAX_STAGE = 4;
    private static final float STAGE_SIZE = 0.15f;
    private static final String[] STAT_NAMES = {"ATK", "DEF", "SPD"};
    private final int[] stages = new int[6];   // 0-2: your ATK, DEF, SPD   3-5: the enemy's ATK, DEF, SPD

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
        int idx = (onPlayer ? 0 : 3) + stat;
        String who = onPlayer ? playerName : enemyName;
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
                if (stat == 0) enemy.alterATKModifier(amount); else if (stat == 1) enemy.alterDEFModifier(amount); else enemy.alterSPDModifier(amount);
                enemySprite.playStatFx(up, statColor(stat));
            }
        }));
    }

    private void playerHealBlock() {
        TurnHistoryScreen.Entry log = turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER, "HEAL", player.getCurrentHP());
        List<Step> steps = new ArrayList<>();
        steps.add(new Step(playerName + " used HEAL!", null));
        steps.add(new Step(playerName + "'s HP was fully restored!", () -> {
            player.setCurrentHP(player.getMaxHP());
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
            steps.add(new Step(playerName + " has fled!", () -> playerSprite.setPose(BattleSprite.Pose.FLEE)));
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
        if (isOver()) return;
        List<Step> steps = new ArrayList<>();
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
        TurnHistoryScreen.Entry log = turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.ENEMY, move.name,
                move.damages && !move.flees ? player.getCurrentHP() : TurnHistoryScreen.N_TRGT);
        String note = brain != null ? brain.pollMessage() : null;
        if (note != null) steps.add(new Step(note, null));

        // the enemy runs away (Starving Wretch): the fight ends with no rewards
        if (move.flees) {
            steps.add(new Step(enemyName + " ran away!", () -> enemySprite.setPose(BattleSprite.Pose.FLEE)));
            steps.add(new Step(null, () -> {
                script.clear();
                finish(Battle.BattleResult.FLED);
            }));
            insert(steps);
            return;
        }
        steps.add(new Step(enemyName + " used " + move.name + "!", move.damages ? () -> enemySprite.setPose(BattleSprite.Pose.ATTACK) : null));

        // status moves do no damage: they just change stats (your guard and counter stay ready)
        if (!move.damages) {
            enemyEffects(steps, move, true);
            insert(steps);
            return;
        }
        if (playerGuard) {
            steps.add(new Step(playerName + " blocked the attack!", () -> {
                playerGuard = false;
                playerSprite.setPose(BattleSprite.Pose.GUARD);
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
        if (rng.nextFloat() < BattleMath.dodgeChance(playerSpd(), enemy.getBaseSPD())) {
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
            float dmgF = CombatResolver.calculateDamage(enemy.getBaseATK() * move.multiplier(), BattleMath.effectiveDef(player, inventory));
            dmgF *= (float) (1 - BattleMath.damageReduction(inventory));
            boolean blocked = rng.nextDouble() < BattleMath.blockChance(inventory);
            if (blocked) dmgF *= (float) (1 - BattleMath.blockAmount(inventory));
            final int hit = Math.max(1, Math.round(dmgF));
            dmg = hit;
            steps.add(new Step(enemyName + " hit for " + hit + " damage!", () -> {
                if (player.getCurrentHP() <= 0) return;
                dealt[0] += hit;
                player.applyDamage(hit);
                log.hit(hit, player.getCurrentHP());
                playerSprite.setPose(BattleSprite.Pose.HURT);
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
                dealToEnemy(thorns, thornsLog);
                }));
        }
        if (playerCounter && player.getCurrentHP() - dmg > 0) {
            playerCounter = false;
            steps.add(new Step(playerName + " counter-attacks!", () -> playerSprite.setPose(BattleSprite.Pose.ATTACK)));
            float atk = BattleMath.effectiveAtk(player, inventory, inventory.getEquippedItem(EquipSlot.MELEE));
            int counter = Math.max(1, Math.round(CombatResolver.calculateDamage(atk * 1.2f, enemy.getBaseDEF())));
            steps.add(new Step("It hit for " + counter + " damage!", () -> {
                TurnHistoryScreen.Entry counterLog = turnHistoryScreen.beginBattleLog(TurnHistoryScreen.Actor.PLAYER, "RETAL", enemy.getCurrentHP());
                dealToEnemy(counter, counterLog);
            }));
        }
        enemyEffects(steps, move, true);
        insert(steps);
    }

    // ---------------------------------------------------------------- results

    private void dealToEnemy(int dmg, TurnHistoryScreen.Entry log) {
        // null player on purpose: rewards are handed out by the victory steps so the XP bar can animate
        enemy.applyDamage(dmg, null);
        log.hit(dmg, enemy.getCurrentHP());
        enemySprite.setPose(BattleSprite.Pose.HURT);
    }

    private boolean isOver() {
        return enemy.getCurrentHP() <= 0 || player.getCurrentHP() <= 0 || pendingResult != null;
    }

    private void checkEnd() {
        if (pendingResult != null) return;
        if (enemy.getCurrentHP() <= 0) {
            // a boss with phases (BattleEnemy.onBattleDefeat) can come back instead of falling
            String[] again = enemy instanceof BattleEnemy ? ((BattleEnemy) enemy).onBattleDefeat() : null;
            if (again != null) {
                script.clear();
                enemyStunned = false;
                for (String line : again) say(line, null);
                return;
            }
            script.clear();
            victory();
        } else if (player.getCurrentHP() <= 0) {
            script.clear();
            say(playerName + " fainted!", () -> playerSprite.setPose(BattleSprite.Pose.FAINT));
            say("YOU WERE DEFEATED...", null);
            script.addLast(new Step(null, () -> finish(Battle.BattleResult.DEFEAT)));
        }
    }

    private void victory() {
        int xp = enemy.getXpReward(), gold = enemy.getGoldReward();
        say(enemyName + " fainted!", () -> enemySprite.setPose(BattleSprite.Pose.FAINT));
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
                insert(up);
            } else {
                targetXp = xpFraction();
            }
        });
        if (gold > 0) say("FOUND " + gold + " GOLD!", null);
        script.addLast(new Step(null, () -> finish(Battle.BattleResult.VICTORY)));
    }

    private void finish(Battle.BattleResult result) {
        pendingResult = result;
        phase = Phase.END_FADE;
        fade = 0;
        message = "";
    }

    private float xpFraction() {
        if (player.getLevel() >= 100 || player.getXpToNextLevel() <= 0) return 1f;
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
        return introTimer >= INTRO_FRAMES && phase != Phase.INTRO && phase != Phase.END_FADE;
    }

    @Override
    // order matters here: backdrop first, then the fighters, then the plates, then the bottom bar on top
    public void draw(GraphicsHandler gh) {
        Graphics2D g = gh.getGraphics();
        int w = BattleUI.W(), h = BattleUI.H();
        backgroundScreen.draw(gh);

        // platforms and fighters (the enemy is behind and smaller, the knight in front)
        drawShadow(g, enemyFootX(), enemyFootY(), 150, 22);
        enemySprite.draw(gh, enemyFootX(), enemyFootY(), tick + 40);
        drawShadow(g, playerFootX(), playerFootY(), 190, 28);
        playerSprite.draw(gh, playerFootX(), playerFootY(), tick);

        drawEnemyPlate(gh);
        drawPlayerPlate(gh);
        drawTextBox(gh);
        inventoryDisplayScreen.draw(gh);
        optionScreen.draw(gh);
        abilityScreen.draw(gh);
        abilityInfoScreen.draw(gh);
        runAwayScreen.draw(gh);
        turnHistoryScreen.draw(gh);   // last of the HUD, so the expanded log (and its dim overlay) covers the menus too

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
        BattleUI.text(gh, name, x, scale == 2 ? y : y + 4, scale, BattleUI.INK);
        BattleUI.textRight(gh, level, rightEdge, y, 2, BattleUI.INK);
    }

    private void drawEnemyPlate(GraphicsHandler gh) {
        int x = 0, y = 24, w = 330, h = 78;
        BattleUI.plate(gh, x, y, w, h, true);
        nameAndLevel(gh, enemyName, "LV" + enemyLevel, x + 22, y + 12, x + w - 44);
        float frac = shownEnemyHP / Math.max(1f, enemy.getMaxHP());
        BattleUI.hpTag(gh, x + 22, y + 34);
        BattleUI.bar(gh, x + 58, y + 34, w - 112, 16, frac, BattleUI.hpColor(frac));
        BattleUI.textRight(gh, Math.round(shownEnemyHP) + "/" + Math.round(enemy.getMaxHP()), x + w - 44, y + 58, 1, BattleUI.INK_SOFT);
    }

    private void drawPlayerPlate(GraphicsHandler gh) {
        int x = BattleUI.W() - 392, y = BattleUI.barTop() - 112, w = 392, h = 100;
        BattleUI.plate(gh, x, y, w, h, false);
        int left = x + 46, right = x + w - 18;
        nameAndLevel(gh, playerName, "LV" + shownLevel, left, y + 12, right);
        float frac = shownPlayerHP / Math.max(1f, player.getMaxHP());
        BattleUI.hpTag(gh, left, y + 34);
        BattleUI.bar(gh, left + 36, y + 34, right - left - 36, 16, frac, BattleUI.hpColor(frac));
        BattleUI.textRight(gh, Math.round(shownPlayerHP) + "/" + Math.round(player.getMaxHP()), right, y + 56, 2, BattleUI.INK);

        BattleUI.text(gh, "EXP", left, y + 80, 1, BattleUI.INK_SOFT);
        BattleUI.bar(gh, left + 36, y + 76, right - left - 36, 10, shownXp, BattleUI.BLUE);

    }

    // the narration box: half width while a menu is open, full width while text is playing
    private void drawTextBox(GraphicsHandler gh) {
        int y = BattleUI.barTop(), h = 96;   // a compact box; the space under it is left empty so the backdrop shows
        boolean menuOpen = phase == Phase.COMMAND || phase == Phase.ABILITY || phase == Phase.INFO || phase == Phase.CONFIRM_RUN;
        int x = BattleUI.boxX(), w = menuOpen ? BattleUI.boxW() : BattleUI.W() - 2 * x;
        BattleUI.box(gh, x, y, w, h);
        int tx = x + 24, ty = y + 18;
        int maxChars = (w - 48) / 12;
        if (phase == Phase.ABILITY || phase == Phase.INFO) {
            MoveCategory cat = abilityScreen.getCategory();
            BattleUI.text(gh, cat.getLabel(), tx, ty - 4, 2, AbilityScreen.colorFor(cat).darker());
            String nm = abilityScreen.getItemName();
            if (nm.length() <= maxChars) BattleUI.text(gh, nm, tx, ty + 20, 2, BattleUI.INK);
            else {
                List<String> item = BattleUI.wrap(nm, (w - 48) / 6);
                for (int i = 0; i < item.size() && i < 2; i++) BattleUI.text(gh, item.get(i), tx, ty + 20 + i * 11, 1, BattleUI.INK);
            }
            BattleUI.text(gh, "ENTER USE   Y INFO", tx, y + h - 30, 1, BattleUI.INK_SOFT);
            BattleUI.text(gh, "1-4 JUMP   B BACK", tx, y + h - 18, 1, BattleUI.INK_SOFT);
            return;
        }
        String text;
        switch (phase) {
            case COMMAND: text = hint.isEmpty() ? "WHAT WILL " + playerName + " DO?" : hint; break;
            case CONFIRM_RUN: text = "RUN FROM THE FIGHT?"; break;
            default: text = message;
        }
        List<String> lines = BattleUI.wrap(text, maxChars);
        for (int i = 0; i < lines.size() && i < 3; i++) BattleUI.text(gh, lines.get(i), tx, ty + i * 24, 2, BattleUI.INK);
        // the little "press ENTER" arrow, blinking, while a message is waiting
        if (!menuOpen && current != null && (tick / 20) % 2 == 0 && phase == Phase.SCRIPT) {
            BattleUI.arrow(gh, x + w - 36, y + h - 34, 7, BattleUI.BOX_FRAME);
        }
    }

    // ---------------------------------------------------------------- getters used by the option screens

    BattleSprite getPlayerSprite() { return playerSprite; }
    Inventory getInventory() { return inventory; }
    int getHealsLeft() { return healsLeft; }
    boolean isBoss() { return boss; }
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
        protected BattleSprite.Pose poseFor(int index) {
            return index == 3 ? BattleSprite.Pose.HEAL : BattleSprite.Pose.IDLE;   // the others only open another window
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

        @Override
        public void update() {
            if (!open) return;
            navigate(3);
            if (BattleInput.confirm()) {
                // a heal that is used up is refused BEFORE the sprite changes
                if (cursor == 3 && battle.getHealsLeft() <= 0) battle.showHint("HEAL ALREADY USED THIS BATTLE!");
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
                boolean dead = i == 3 && battle.getHealsLeft() <= 0;
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
            return switch (i) {
                case 0, 1, 2 -> "";
                case 3 -> battle.getHealsLeft() > 0 ? "Full HP  x" + battle.getHealsLeft() : "Used up";
                default -> "Flee the fight";
            };
        }
    }
}