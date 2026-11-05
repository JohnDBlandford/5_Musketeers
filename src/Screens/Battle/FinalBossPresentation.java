package Screens.Battle;

import BattleSystem.BattleInput;
import BattleSystem.BattleSprites;
import BattleSystem.BattleUI;
import Engine.GraphicsHandler;
import NPCs.World1.Dabbah;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

// The FINAL fight against Dabbah: a fake-3D, turn-based boss battle with PS1-style graphics. PlayBattleScreen runs the fight;
// this class draws the battlefield and animates everything.
//
// DABBAH has the three forms of the title screen (MenuScreen):
//   1 Beast of the Earth   a pitch-black dragon: horns, jaw spikes, wings, a long tail, red eyes
//   2 Scourge of the Ash   the same dragon engulfed in flames, with a dark purple aura tracing its outline
//   3 The Unbound Ruin     a hydra: a black, cracked body with five heads, each firing a different dark beam
//
// PS1 LOOK: a low-poly mesh built in code, smooth (Gouraud) shading, 32x32 textures mapped without perspective correction,
// rendered at half resolution in 15-bit colour with ordered dithering, then scaled up without smoothing.
//
// MOVE ANIMATIONS (Pokemon-style: wind-up, projectile or strike, impact star, target flash), different for the dragon and the hydra:
//   Earthshaking Slam   dragon: leaps on its wings and crashes down          hydra: all five heads rise and slam the ground
//                       -> shockwave, rocks bursting out of the ground under the knight
//   Wrath of the Beast  dragon: roars, charges and rakes with its claws     hydra: three heads bite in turn
//   Infernal Rupture    dragon: stomps; a burning crack runs to the knight  hydra: two heads spit fireballs
//                       -> every hit erupts in fire under the knight
//   Judgement of Dabbah the sky darkens, energy gathers in the mouth(s), then a beam (the hydra: five coloured beams) hits
// plus idle (breathing, wings, tail, head turns, smoke), flinching, emerging from the earth, changing form, collapsing.
// The KNIGHT is the existing battle sprite (BattleSprites.knight(): its own attack, hurt, guard and heal animations), drawn into
// the scene unchanged. Dabbah's moves, damage, stat drops and phases all come from Dabbah.java through PlayBattleScreen;
// this class only shows them.
//
// DEFEAT: when a form falls, cracks of light spread over it, it flashes white and bursts into embers; the next form rises.
// THE WORLD BREAKS a little more with each defeat:
//   form 1 -> 2   the sky and the ground of World 4 crack open, purple light leaks through, rocks float up off the ground
//   form 2 -> 3   a cutscene: the hydra rises, fires its five beams at the knight, reality cracks like glass around him and
//                 SHATTERS; the shards fly away and the fight goes on in the purple 4th dimension (a swirling void with a
//                 glowing grid floor and pieces of the old world drifting in it)
//   final defeat  the dimension shatters in turn and World 4 comes back, mended
//
// THE CAMERA works like a 3D Pokemon battle (Black/White, X/Y): a wide shot of both fighters that sways slowly, and for each
// action it swings in to frame whoever is acting, then cuts to the one being hit:
//   WIDE            the default: the knight in the left foreground, Dabbah to the right, both in view
//   BOSS            Dabbah winds up an attack, changes form or collapses: the camera swings toward it and zooms in
//   KNIGHT_ATTACK   the knight attacks: an over-the-shoulder shot past him toward Dabbah
//   KNIGHT_HIT      Dabbah's attack lands: a closer shot of the knight taking the hit
// Shots blend smoothly into each other; the camera only turns and zooms a little and moves a short way, so the painted
// World 4 backdrop (panned and zoomed to match) still lines up with the 3D scene.
//
// CINEMATIC CHANGES (search for "CINEMATIC:"):
//   - every time a form falls, an anime-style CINEMATIC plays first (FinalBossCinematic: impact frames, a split-screen
//     face-off, both fighters charging, a MASH-ENTER beam struggle, an explosion and Dabbah's last words). When it ends,
//     the 3D scene takes over with the form-change above (the old form breaking, the new one rising): the TRANSFORMATION is
//     the last part of the cinematic, then the battle goes on. After the last form the cinematic leads into the final collapse.
//   - cinematic BARS slide in over the HUD during the transformation and the final collapse
//   - IMPACT FRAMES on the knight's critical hits (the fighters as white shapes on black, then black on white)
//   - SPEED LINES behind the knight's attacks
//   - JUDGEMENT CUT-IN: when Dabbah calls down Judgement of Dabbah, a slanted panel with its eye and the move's name sweeps
//     across the screen, the rest of the screen dimmed (like a special-move cut-in)
//   - RAGE: below 25% HP the current form throws off embers that rise across the whole screen, in its colour
//   - DANGER: below 30% HP a slow red heartbeat pulses at the edges of the screen
//   - the beam struggle can be LOST: the knight is torn apart (FinalBossCinematic's SLAIN beat), the screen stays black and
//     PlayBattleScreen ends the battle in a defeat (consumeKnightSlain)
//   - IMPACT FRAMES on Dabbah's critical hits too (PlayBattleScreen calls critImpact for both)
//   FinalBossCinematic.REDUCED_FLASH = true turns every impact frame into a dark red still.
// CODEX CHANGES (search for "CODEX:"): the class is public now, for formPortrait(form): a picture of one of Dabbah's three
// forms, rendered by this same 3D renderer, for the Codex of Beasts (Dabbah has no 2D sprite of its own).
public final class FinalBossPresentation {
    static final int INTRO_FRAMES = 150;

    private static final float BOSS_X = 3.0f, BOSS_Z = 9.4f, KNIGHT_X = -2.2f, KNIGHT_Z = 4.5f;
    private static final float FOCAL = 430f;
    private static final int S = 2;
    // how long each form change and the final defeat take (the battle's text waits for them)
    private static final int DEFEAT_FRAMES = 70, REBIRTH2_FRAMES = 150, REBIRTH3_FRAMES = 345, FAINT_FRAMES = 165;

    // materials: textures 0..9, solid colours 20+, eyes 30+ (one colour per head)
    private static final int T_SCALE = 0, T_BELLY = 1, T_HORN = 2, T_WING = 3, T_HYDRA = 4, T_MOUTH = 5, T_CLAW = 6, M_VOID = 20, M_EYE = 30;
    private static final int TS = 32, GLOW = 0x01000000;
    // the hydra's five beams, from the title screen: dried blood, bruise, drowned blue, rot, rust
    private static final int[] BEAM = {0xaa0019, 0x6e00af, 0x0037b4, 0x007d2d, 0xb95500};
    private static final int DRAGON_BEAM = 0xd0182a;
    private static final int FLAME_BEAM = 0xff8a20;   // CINEMATIC: form 2's colour

    private enum Anim { NONE, SLAM, WRATH, RUPTURE, JUDGEMENT }

    private final BattleBackgroundScreen backdrop;
    private final BattleSprites.BattleSprite knight, bossSprite;
    private final Dabbah boss;
    private final Random rng = new Random();

    private int W, H, horizon, LW, LH;
    private BufferedImage frame, subj;
    private int[] framePx, subjPx, lowPx, srcX, dist;
    private boolean[] meshMask;
    private float[] zbuf;
    private Graphics2D frameG, subjG;
    private GraphicsHandler subjGh;
    private final int[][] tex = new int[7][];

    private int t, introTimer, introFrames = INTRO_FRAMES, introDoneAt = -1, lastPhase;
    private boolean introDone;
    private BattleSprites.BattleSprite.Pose lastBossPose, lastKnightPose;

    private Anim anim = Anim.NONE;
    private int animT, animDur, impactAt;
    private boolean impactPending;
    private int hurtT = -1, faintT = -1, rebirthT = -1, rebirthDur = REBIRTH2_FRAMES;
    private boolean cutsceneBeam;   // the Judgement fired in the form 3 cutscene is not a real attack
    private float shake, bgDim;

    // CINEMATIC: the cinematic that plays when a form falls
    private volatile FinalBossCinematic.Kind cineRequest;   // asked for by update(); built by the next drawWorld() (it needs a frame)
    private volatile FinalBossCinematic cine;
    private boolean pendingRebirth, pendingFaint;            // what plays after it: the next form rising, or the final collapse
    private int critT;                                       // frames left of a critical hit's impact frame
    private float letterbox;                                 // 0..1: how far the cinematic bars have slid in
    private float knightHp = 1f;                             // the knight's HP fraction (PlayBattleScreen passes it in)
    private volatile boolean knightSlain, slainHold;         // CINEMATIC: the struggle was lost (slainHold keeps the screen black)

    // ---- the camera: where it is (eye), what it looks at (target) and its zoom (focal length), blended toward the current shot
    private enum Shot { WIDE, BOSS, KNIGHT_ATTACK, KNIGHT_HIT }
    private Shot shot = Shot.WIDE;
    private int shotT;                                   // frames the current shot still holds (then back to WIDE)
    private final float[] eye = new float[3], target = new float[3];
    private float f = FOCAL, yaw, pitch, shakeX, shakeY;
    private float cy, sy, cp, sp;                       // cos/sin of yaw and pitch, worked out once per frame

    // points of the animated model (world space) that effects start from
    private final float[][] mouths = new float[5][];
    private int mouthCount;
    private float[] frontFeet = new float[3], bodyCenter = new float[]{BOSS_X, 2.2f, BOSS_Z};
    private float bossScale = 1f, curYaw;

    private static final int FX_SHOCK = 0, FX_ERUPT = 1, FX_DUST = 2, FX_SMOKE = 3, FX_ROCKS = 4, FX_SLASH = 5, FX_BITE = 6, FX_STAR = 7,
            FX_ROAR = 8, FX_FIREBALL = 9, FX_CRACK = 10;
    private static final class Fx {
        final int kind, dur; final float[] a, b; int t, color;
        Fx(int kind, float[] a, float[] b, int dur, int color) { this.kind = kind; this.a = a; this.b = b; this.dur = dur; this.color = color; }
    }
    // effects, shards and debris are changed by update() and read by drawWorld(), which the game can run on different threads
    // (update in the game loop, drawing on Swing's paint thread), so these lists are copy-on-write: safe to read while changed
    private final List<Fx> fx = new CopyOnWriteArrayList<>();

    FinalBossPresentation(BattleBackgroundScreen backdrop, BattleSprites.BattleSprite knight, BattleSprites.BattleSprite bossSprite, Dabbah boss) {
        this.backdrop = backdrop;
        this.knight = knight;
        this.bossSprite = bossSprite;
        this.boss = boss;
    }

    void initialize() {
        W = BattleUI.W();
        H = BattleUI.H();
        horizon = 200;
        LW = W / S;
        LH = H / S + 1;
        frame = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        subj = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        framePx = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();
        subjPx = ((DataBufferInt) subj.getRaster().getDataBuffer()).getData();
        lowPx = new int[LW * LH];
        dist = new int[LW * LH];
        srcX = new int[W];
        zbuf = new float[LW * LH];
        meshMask = new boolean[LW * LH];
        frameG = frame.createGraphics();
        subjG = subj.createGraphics();
        subjG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        subjGh = new GraphicsHandler();
        subjGh.setGraphics(subjG);
        buildTextures();
        t = 0;
        introDone = false;
        lastPhase = boss.getPhase();
        lastBossPose = bossSprite.getPose();
        lastKnightPose = knight.getPose();
        shot = Shot.WIDE;
        shotT = 0;
        initWorldBreak();
        shotPose(Shot.WIDE, 0, eye, target);
        f = FOCAL;
        anim = Anim.NONE;
        fx.clear();
        cineRequest = null;   // CINEMATIC
        cine = null;
        pendingRebirth = pendingFaint = false;
        critT = 0;
        letterbox = 0;
        knightHp = 1f;
        knightSlain = slainHold = false;
    }

    // ================================================================= the battle's hooks

    // Dabbah used a move (matched by name)
    void playMove(String moveName) {
        String n = moveName.toLowerCase();
        if (n.contains("slam")) start(Anim.SLAM, 84, 50);
        else if (n.contains("wrath")) start(Anim.WRATH, 72, 34);
        else if (n.contains("rupture")) start(Anim.RUPTURE, 64, 26);
        else if (n.contains("judg")) start(Anim.JUDGEMENT, 112, 70);
        else start(Anim.WRATH, 72, 34);
    }

    private void start(Anim a, int dur, int impact) {
        anim = a;
        animT = 0;
        animDur = dur;
        impactAt = impact;
        impactPending = false;
    }

    // the text waits while an attack winds up, a cinematic plays or Dabbah changes form
    boolean holdsScript() {
        return (anim != Anim.NONE && animT < impactAt && !cutsceneBeam) || (rebirthT >= 0 && rebirthT < rebirthDur)
                || (faintT >= 0 && faintT < FAINT_FRAMES)
                || cineRequest != null || cine != null || boss.getPhase() != lastPhase;   // CINEMATIC
    }

    // CINEMATIC: true while a cinematic is about to play or playing: the battle's text is frozen (it neither shows the next
    // line nor times out), so the lines Dabbah says when it changes form come up after the transformation, not under it
    boolean cinematicPlaying() {
        return cineRequest != null || cine != null || boss.getPhase() != lastPhase;
    }

    // true once, the moment an attack lands (the battle then shows its damage right away)
    boolean consumeImpact() {
        if (!impactPending) return false;
        impactPending = false;
        return true;
    }

    // CINEMATIC: true once, after the knight lost a beam struggle and was killed (the battle then ends in a defeat)
    boolean consumeKnightSlain() {
        if (!knightSlain) return false;
        knightSlain = false;
        return true;
    }

    // CINEMATIC: a critical hit (the knight's on Dabbah, or Dabbah's on the knight): an impact frame
    void critImpact() {
        if (cine == null && cineRequest == null) critT = 6;
    }

    // CINEMATIC: the knight's HP fraction (for the danger heartbeat)
    void setKnightHp(float fraction) {
        knightHp = fraction;
    }

    // CODEX: a picture of Dabbah in one of its forms (1, 2 or 3), cut out of a frame rendered off screen by this renderer
    // (the 3D model, its flames in form 2). The Codex of Beasts uses these as Dabbah's portraits. null if it can't be made.
    public static BufferedImage formPortrait(int form) {
        try {
            Dabbah d = new Dabbah(0, new Utils.Point(0, 0));
            d.onBattleStart();
            for (int i = 1; i < form; i++) d.onBattleDefeat();   // walk it to that form
            BattleBackgroundScreen bg = new BattleBackgroundScreen(BattleSystem.BattleWorld.WORLD_4);
            bg.initialize();
            FinalBossPresentation p = new FinalBossPresentation(bg, BattleSprites.BattleSprite.knight(),
                    BattleSprites.BattleSprite.enemy(d.getEnemyName()), d);
            p.initialize();
            for (int i = 0; i < 160; i++) p.update(INTRO_FRAMES, INTRO_FRAMES, false);   // risen, the camera settled on it
            BufferedImage screen = new BufferedImage(p.W, p.H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = screen.createGraphics();
            GraphicsHandler gh = new GraphicsHandler();
            gh.setGraphics(g);
            p.drawWorld(gh, 0);
            g.dispose();
            // everything drawn at low resolution is Dabbah (and its flames): crop to it
            int x0 = p.LW, y0 = p.LH, x1 = -1, y1 = -1;
            for (int y = 0; y < p.LH; y++) for (int x = 0; x < p.LW; x++)
                if ((p.lowPx[y * p.LW + x] >>> 24) != 0) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
            if (x1 < 0) return null;
            BufferedImage out = new BufferedImage(x1 - x0 + 1, y1 - y0 + 1, BufferedImage.TYPE_INT_ARGB);
            for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
                int c = p.lowPx[y * p.LW + x];
                if ((c >>> 24) != 0) out.setRGB(x - x0, y - y0, c);
            }
            return out;
        } catch (RuntimeException ex) {
            System.out.println("Codex: could not draw Dabbah's form " + form + " (" + ex + ")");
            return null;
        }
    }

    // ================================================================= update

    void update(int introTimer, int introFrames, boolean ending) {
        t++;
        this.introTimer = introTimer;
        this.introFrames = introFrames;
        if (!introDone && introTimer >= introFrames) { introDone = true; introDoneAt = t; }

        BattleSprites.BattleSprite.Pose bp = bossSprite.getPose(), kp = knight.getPose();
        if (bp != lastBossPose) {
            if (bp == BattleSprites.BattleSprite.Pose.HURT) { hurtT = 0; fx.add(new Fx(FX_STAR, bossChest(), null, 14, 0xffffff)); }
            // CINEMATIC: the last form fell: the cinematic first, then the final collapse (was: faintT = 0 right away)
            if (bp == BattleSprites.BattleSprite.Pose.FAINT && faintT < 0 && cine == null && cineRequest == null) {
                pendingFaint = true;
                cineRequest = FinalBossCinematic.Kind.FINAL_FORM_FALLS;
                anim = Anim.NONE;
            }
        }
        if (kp != lastKnightPose) {
            if (kp == BattleSprites.BattleSprite.Pose.HURT) {
                shake = Math.max(shake, 8);
                fx.add(new Fx(FX_STAR, knightChest(), null, 14, 0xffffff));
                // the hit marks only appear when the attack really lands (not on a dodge, miss or block)
                if (anim == Anim.WRATH) fx.add(new Fx(hydra() ? FX_BITE : FX_SLASH, knightChest(), null, 18, 0xff3030));
                if (anim == Anim.RUPTURE) {                      // every hit of Infernal Rupture erupts under the knight
                    fx.add(new Fx(FX_ERUPT, v3(KNIGHT_X, 0, KNIGHT_Z), null, 44, 0));
                    if (animT > 32) animT = 12;                   // and Dabbah attacks again
                }
            }
            if (kp == BattleSprites.BattleSprite.Pose.ATTACK) cut(Shot.KNIGHT_ATTACK, 60);   // the knight's own attack animation
        }
        lastBossPose = bp;
        lastKnightPose = kp;
        if (boss.getPhase() != lastPhase) {   // a form fell: the cinematic, then the defeat animation and the next form
            lastPhase = boss.getPhase();
            // CINEMATIC: the form change waits for the cinematic (was: rebirthT = 0 right away)
            pendingRebirth = true;
            cineRequest = lastPhase >= 3 ? FinalBossCinematic.Kind.SECOND_FORM_FALLS : FinalBossCinematic.Kind.FIRST_FORM_FALLS;
            anim = Anim.NONE;
            cutsceneBeam = false;
            hurtT = -1;
        }
        // CINEMATIC: play it; when it is over, the transformation (or the final collapse) starts behind a white fade
        FinalBossCinematic c = cine;
        if (c != null) {
            c.update(BattleInput.confirm());
            if (c.done()) finishCinematic(c.lost());
        }
        if (critT > 0) critT--;
        float lbTarget = cine == null && cineRequest == null && ((rebirthT >= 0 && rebirthT < rebirthDur) || (faintT >= 0 && faintT < FAINT_FRAMES)) ? 1f : 0f;
        letterbox += (lbTarget - letterbox) * 0.1f;

        if (anim != Anim.NONE) {
            animT++;
            moveEvents();
            if (animT == impactAt) {
                impact();
                if (!cutsceneBeam) { impactPending = true; cut(Shot.KNIGHT_HIT, anim == Anim.JUDGEMENT ? 50 : 40); }
            }
            if (animT > animDur) { anim = Anim.NONE; cutsceneBeam = false; }
        }
        // the sky darkens for Judgement (like a special move in Pokemon)
        float dimTarget = anim == Anim.JUDGEMENT && animT < 96 ? 0.55f : 0f;
        bgDim += (dimTarget - bgDim) * 0.12f;
        if (hurtT >= 0 && ++hurtT > 26) hurtT = -1;
        if (faintT >= 0) faintT++;
        if (rebirthT >= 0) {
            rebirthT++;
            if (rebirthT == DEFEAT_FRAMES) fx.add(new Fx(FX_DUST, v3(BOSS_X, 0, BOSS_Z), null, 50, 0));
            if (rebirthT > rebirthDur) rebirthT = -1;
        }
        updateDefeat();
        updateWorldBreak();
        if (!introDone && introTimer == 24) fx.add(new Fx(FX_DUST, v3(BOSS_X, 0, BOSS_Z), null, 50, 0));

        // idle smoke from the nostrils
        if (anim == Anim.NONE && rebirthT < 0 && faintT < 0 && t % 200 == 90)
            for (int i = 0; i < mouthCount; i++) fx.add(new Fx(FX_SMOKE, mouths[i], null, 40, 0));
        for (Fx e : fx) e.t++;
        fx.removeIf(e -> e.t > e.dur);
        shake *= 0.86f;
        updateCamera();
    }

    // CINEMATIC: the cinematic is over: the next form rises (or the last one collapses), out of a white flash
    private void finishCinematic(boolean lost) {
        cine = null;
        if (lost) {   // CINEMATIC: the knight was killed in the struggle: no transformation, the screen stays black
            pendingRebirth = pendingFaint = false;
            slainHold = true;
            knightSlain = true;
            return;
        }
        flashT = 9;
        if (pendingRebirth) {
            pendingRebirth = false;
            rebirthT = 0;
            rebirthDur = lastPhase >= 3 ? REBIRTH3_FRAMES : REBIRTH2_FRAMES;
        }
        if (pendingFaint) {
            pendingFaint = false;
            faintT = 0;
        }
    }

    private boolean hydra() { return visPhase() >= 3; }

    // effects spawned partway through a move
    private void moveEvents() {
        int p = animT;
        float[] k = knightChest();
        if (anim == Anim.WRATH && !hydra() && (p == 4 || p == 10) && mouthCount > 0) fx.add(new Fx(FX_ROAR, mouths[0], null, 24, 0xffffff));
        if (anim == Anim.RUPTURE && !hydra() && p == 18) fx.add(new Fx(FX_CRACK, frontFeet, v3(KNIGHT_X, 0, KNIGHT_Z), 30, 0xff7a20));
        if (anim == Anim.RUPTURE && hydra() && (p == 6 || p == 12) && mouthCount == 5)
            fx.add(new Fx(FX_FIREBALL, mouths[p == 6 ? 0 : 4], k, 26 - p + 2, 0xff8a2a));
    }

    private void impact() {
        float[] kg = v3(KNIGHT_X, 0, KNIGHT_Z);
        switch (anim) {
            case SLAM:
                fx.add(new Fx(FX_SHOCK, hydra() ? v3(BOSS_X - 1.5f, 0, BOSS_Z - 2.5f) : frontFeet, null, 34, 0));
                fx.add(new Fx(FX_ROCKS, kg, null, 40, 0));
                fx.add(new Fx(FX_DUST, kg, null, 50, 0));
                shake = 16;
                break;
            case WRATH: shake = 9; break;
            case RUPTURE: shake = 6; break;
            case JUDGEMENT: shake = cutsceneBeam ? 20 : 12; break;
            default: break;
        }
    }

    // ================================================================= the camera

    // switch to a shot for this many frames (it then drifts back to WIDE)
    private void cut(Shot next, int frames) {
        shot = next;
        shotT = frames;
    }

    // where each shot puts the camera (world units; the knight stands at x -2.2 z 4.5, Dabbah at x 3 z 9.4)
    private void shotPose(Shot s, int t, float[] e, float[] tg) {
        float sway = (float) Math.sin(t * 0.006), sway2 = (float) Math.sin(t * 0.0041 + 1.3);
        float bh = 2.6f * bossScale + (hydra() ? 0.8f : 0f);   // Dabbah's chest height (the hydra's heads are higher)
        switch (s) {
            case BOSS:            // swing toward Dabbah and look up at it
                set(e, 0.9f + 0.15f * sway, 2.3f, 1.4f);
                set(tg, bodyCenter[0] - 0.3f, bh + 1.0f, bodyCenter[2]);
                break;
            case KNIGHT_ATTACK:   // low, over the knight's shoulder toward Dabbah (the knight big in the left foreground)
                set(e, -2.7f, 2.2f, 1.5f);
                set(tg, bodyCenter[0] - 0.8f, bh + 0.6f, bodyCenter[2]);
                break;
            case KNIGHT_HIT:      // a closer shot of the knight taking the hit, Dabbah still at the edge of the frame
                set(e, -0.2f, 2.0f, 0.7f);
                set(tg, KNIGHT_X + 0.6f, 2.0f, KNIGHT_Z);
                break;
            default:              // WIDE: both fighters, swaying slowly
                set(e, 0.25f + 0.35f * sway, 2.4f + 0.1f * sway2, -1.0f);
                set(tg, 0.5f + 0.25f * sway2, 3.0f, 8f);
                break;
        }
    }

    private static float shotFocal(Shot s) {
        switch (s) {
            case BOSS: return FOCAL * 1.06f;
            case KNIGHT_ATTACK: return FOCAL;
            case KNIGHT_HIT: return FOCAL * 1.12f;
            default: return FOCAL;
        }
    }

    private static void set(float[] v, float x, float y, float z) { v[0] = x; v[1] = y; v[2] = z; }

    private void updateCamera() {
        // what Dabbah is doing decides the shot
        if (rebirthT >= 0) {
            // the form change: on Dabbah while it falls, rises and charges; wide for the beams and the shattering
            if (lastPhase >= 3 && rebirthT >= 200) { shot = Shot.WIDE; shotT = 0; }
            else cut(Shot.BOSS, 2);
        } else if (faintT >= 0) {
            if (faintT < DEFEAT_FRAMES) cut(Shot.BOSS, 2);
            else { shot = Shot.WIDE; shotT = 0; }
        } else if (anim != Anim.NONE && animT < impactAt) cut(Shot.BOSS, 20);
        if (shotT > 0) shotT--;
        else shot = Shot.WIDE;

        float[] e = new float[3], tg = new float[3];
        shotPose(shot, t, e, tg);
        // the opening: the camera starts high and far back and swoops down into the wide shot as Dabbah rises
        float swoop = introDone ? 1f : Math.min(1f, introTimer / (float) introFrames) * 0.35f;
        if (introDone && t - introDoneAt < 110) swoop = 0.35f + 0.65f * easeOut((t - introDoneAt) / 110f);
        float s1 = 1 - swoop;
        e[1] += 2.2f * s1;
        e[2] -= 4.0f * s1;
        e[0] -= 1.2f * s1;
        // ease toward the shot (fast enough to feel like a camera move, slow enough to read)
        float k = introDone && t - introDoneAt >= 110 ? 0.085f : 1f;
        for (int i = 0; i < 3; i++) { eye[i] += (e[i] - eye[i]) * k; target[i] += (tg[i] - target[i]) * k; }
        f += (shotFocal(shot) - f) * k;

        float dx = target[0] - eye[0], dy = target[1] - eye[1], dz = target[2] - eye[2];
        yaw = (float) Math.atan2(dx, dz);
        pitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        cy = (float) Math.cos(yaw); sy = (float) Math.sin(yaw);
        cp = (float) Math.cos(pitch); sp = (float) Math.sin(pitch);
        shakeX = shake * (float) (Math.sin(t * 2.3) + 0.5 * Math.sin(t * 5.9));
        shakeY = shake * 0.7f * (float) (Math.cos(t * 2.9) + 0.5 * Math.cos(t * 4.7));
    }

    private float[] knightChest() { return v3(KNIGHT_X, 1.3f, KNIGHT_Z); }
    private float[] bossChest() { return v3(bodyCenter[0], 2.4f * bossScale, bodyCenter[2]); }

    // ================================================================= drawing

    void drawWorld(GraphicsHandler screen, int tick) {
        {
            if (dimension) drawDimension(); else { drawBackdrop(); drawBrokenWorld(); }   // the world behind the fight
            Arrays.fill(subjPx, 0);
            Arrays.fill(lowPx, 0);
            Arrays.fill(zbuf, 0f);
            Arrays.fill(meshMask, false);
            int d = defeatT();
            if (!bossGone()) {
                drawGroundMarks();
                buildBoss();
                rasterBoss();
                if (visPhase() <= 2 && (d < 0 || d < 46)) fireAura(visPhase() == 2);
                if (d >= 0) defeatPass(d);
            }
            for (int y = 0; y < LH; y++) for (int x = 0; x < LW; x++) {
                int c = lowPx[y * LW + x];
                if ((c >>> 24) == 0) continue;
                int o = y * S * W + x * S;
                for (int j = 0; j < S && y * S + j < H; j++, o += W) for (int i = 0; i < S; i++) subjPx[o + i] = c;
            }
            drawSmallFx();
            drawKnight(tick);
            drawBigFx();
            if (d >= 0) drawDefeatFx(d);
            frameG.drawImage(subj, 0, 0, null);
            drawShatter();   // the cracks spreading over the world, then the flying shards
            drawSpeedLines();                                                   // CINEMATIC
            if (cineRequest != null && cine == null) captureCinematic(cineRequest);   // CINEMATIC: built from this very frame
            if (critT > 2 && !FinalBossCinematic.REDUCED_FLASH) impactFrame(critT <= 4);   // CINEMATIC
        }
        Graphics2D sg = screen.getGraphics();
        sg.drawImage(frame, 0, 0, null);
        if (flashT > 0) {   // the white flash of a defeat or a shattering
            sg.setColor(new Color(255, 255, 255, Math.min(255, flashT * 26)));
            sg.fillRect(0, 0, W, H);
        }
    }

    // ================================================================= CINEMATIC: everything drawn over the HUD

    // PlayBattleScreen calls this after the plates, the text box and the menus: the cinematic itself, the bars, the
    // Judgement cut-in, the rage embers and the danger heartbeat
    void drawOverlay(GraphicsHandler gh, int tick) {
        if (slainHold) { gh.drawFilledRectangle(0, 0, W, H, Color.BLACK); return; }   // CINEMATIC: the knight is dead
        FinalBossCinematic c = cine;
        if (c != null) { c.draw(gh); return; }
        if (cineRequest != null) {   // the frame it is being built from: hold black so the HUD doesn't blink through
            gh.drawFilledRectangle(0, 0, W, H, Color.BLACK);
            return;
        }
        Graphics2D g = gh.getGraphics();
        drawRage(g);
        drawDanger(g);
        if (anim == Anim.JUDGEMENT && !cutsceneBeam && animT < 62) drawCutIn(gh, animT);
        if (letterbox > 0.01f) {
            int bar = Math.round(H * 0.12f * letterbox);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, W, bar);
            g.fillRect(0, H - bar, W, bar);
        }
    }

    // builds the cinematic from what is on screen right now: the whole scene, the fighters' shapes, Dabbah, the knight
    private void captureCinematic(FinalBossCinematic.Kind kind) {
        BufferedImage scene = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        System.arraycopy(framePx, 0, ((DataBufferInt) scene.getRaster().getDataBuffer()).getData(), 0, framePx.length);
        BufferedImage subjects = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        System.arraycopy(subjPx, 0, ((DataBufferInt) subjects.getRaster().getDataBuffer()).getData(), 0, subjPx.length);
        // Dabbah: its mesh's pixels (at the low resolution it is rendered in), cropped to its outline
        int x0 = LW, y0 = LH, x1 = -1, y1 = -1;
        for (int y = 0; y < LH; y++) for (int x = 0; x < LW; x++)
            if (meshMask[y * LW + x]) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
        BufferedImage bossImg;
        int hitX = Math.round(W * 0.68f), hitY = Math.round(H * 0.4f);
        if (x1 < 0) bossImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        else {
            bossImg = new BufferedImage(x1 - x0 + 1, y1 - y0 + 1, BufferedImage.TYPE_INT_ARGB);
            for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++)
                if (meshMask[y * LW + x]) bossImg.setRGB(x - x0, y - y0, 0xFF000000 | lowPx[y * LW + x]);
            hitX = (x0 + x1) / 2 * S;
            hitY = (y0 + y1) / 2 * S;
        }
        // the knight: his battle sprite drawn on its own, cropped
        BufferedImage tmp = new BufferedImage(420, 420, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = tmp.createGraphics();
        GraphicsHandler th = new GraphicsHandler();
        th.setGraphics(tg);
        tg.translate(210, 410);
        knight.draw(th, 0, 0, t);
        tg.dispose();
        BufferedImage knightImg = crop(tmp);
        int[] colors = kind == FinalBossCinematic.Kind.FINAL_FORM_FALLS ? BEAM
                : (kind == FinalBossCinematic.Kind.SECOND_FORM_FALLS ? new int[]{FLAME_BEAM} : new int[]{DRAGON_BEAM});
        critT = 0;
        cine = new FinalBossCinematic(kind, W, H, scene, subjects, knightImg, bossImg, hitX, hitY, colors);
        cineRequest = null;
    }

    private static BufferedImage crop(BufferedImage img) {
        int x0 = img.getWidth(), y0 = img.getHeight(), x1 = -1, y1 = -1;
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++)
            if ((img.getRGB(x, y) >>> 24) > 40) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
        if (x1 < 0) return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        return img.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    }

    // a critical hit's impact frame: the fighters as white shapes on black, then black on white
    private void impactFrame(boolean inverted) {
        int ink = inverted ? 0x000000 : 0xffffff, paper = inverted ? 0xffffff : 0x000000;
        for (int i = 0; i < framePx.length; i++) framePx[i] = (subjPx[i] >>> 24) > 40 ? ink : paper;
    }

    // speed lines behind the knight's attacks (the first moments of the over-the-shoulder shot)
    private void drawSpeedLines() {
        if (shot != Shot.KNIGHT_ATTACK || shotT < 42 || cine != null) return;
        float[] p = new float[3];
        if (!project(bossChest(), p)) return;
        FinalBossCinematic.radialLines(frameG, p[0], p[1], 50, new Color(255, 255, 255, 70 + (shotT - 42) * 6), 160, W, H, t / 2);
    }

    // the Judgement cut-in: a slanted band sweeps in with Dabbah's eye and the move's name, holds, and sweeps out
    private void drawCutIn(GraphicsHandler gh, int a) {
        Graphics2D g = (Graphics2D) gh.getGraphics().create();
        float in = a < 10 ? easeOut(a / 10f) : 1f, out = a > 52 ? easeIn((a - 52) / 10f) : 0f;
        float off = W * (1 - in) - W * out;
        g.setColor(new Color(0, 0, 0, Math.round(120 * (1 - out))));
        g.fillRect(0, 0, W, H);
        int col = hydra() ? BEAM[(t / 8) % 5] : (visPhase() == 2 ? FLAME_BEAM : DRAGON_BEAM);
        float midY = H * 0.42f, half = 78, skew = 60;
        Polygon band = new Polygon(new int[]{Math.round(off - skew), Math.round(off + W + skew), Math.round(off + W), Math.round(off)},
                new int[]{Math.round(midY - half), Math.round(midY - half), Math.round(midY + half), Math.round(midY + half)}, 4);
        g.setColor(new Color(36, 4, 8));
        g.fill(band);
        Graphics2D b = (Graphics2D) g.create();
        b.clip(band);
        Random r = new Random(t / 2 * 31L);
        b.setColor(FinalBossCinematic.rgba(col, 120));
        for (int i = 0; i < 40; i++) b.fillRect(r.nextInt(W), Math.round(midY - half) + r.nextInt(Math.round(half * 2)), 60 + r.nextInt(200), 2);
        FinalBossCinematic.drawEye(b, off + W * 0.24f, midY + 6, 52, col, 0.4f, t);
        b.dispose();
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5));
        g.drawLine(band.xpoints[0], band.ypoints[0], band.xpoints[1], band.ypoints[1]);
        g.drawLine(band.xpoints[3], band.ypoints[3], band.xpoints[2], band.ypoints[2]);
        g.setStroke(new BasicStroke(1));
        g.dispose();
        int tx = Math.round(off + W * 0.64f);
        BattleUI.textCenter(gh, "JUDGEMENT", tx + 3, Math.round(midY - 44) + 3, 5, FinalBossCinematic.rgb(col));
        BattleUI.textCenter(gh, "JUDGEMENT", tx, Math.round(midY - 44), 5, Color.WHITE);
        BattleUI.textCenter(gh, "OF DABBAH", tx + 2, Math.round(midY + 18) + 2, 3, new Color(60, 0, 0));
        BattleUI.textCenter(gh, "OF DABBAH", tx, Math.round(midY + 18), 3, new Color(255, 220, 200));
    }

    // RAGE: a form below 25% HP throws off embers that rise across the whole screen, and the screen's edges glow its colour
    private void drawRage(Graphics2D g) {
        float frac = boss.getCurrentHP() / Math.max(1f, boss.getMaxHP());
        if (frac >= 0.25f || rebirthT >= 0 || faintT >= 0 || boss.getCurrentHP() <= 0) return;
        float k = 1 - frac / 0.25f;
        int col = hydra() ? BEAM[0] : (visPhase() == 2 ? FLAME_BEAM : DRAGON_BEAM);
        for (int i = 0; i < 46; i++) {
            float sp = 0.8f + hash(i, 1, 91) * 2f;
            float x = hash(i, 2, 91) * W + (float) Math.sin(t * 0.03 + i) * 12;
            float y = H - ((t * sp + hash(i, 3, 91) * H) % (H + 20));
            g.setColor(FinalBossCinematic.rgba(i % 4 == 0 ? 0xffe080 : col, Math.round(200 * k)));
            g.fillRect(Math.round(x) & ~1, Math.round(y) & ~1, 2 + (i % 3) * 2, 2 + (i % 3) * 2);
        }
        int a = Math.round(70 * k * (0.7f + 0.3f * (float) Math.sin(t * 0.08)));
        Paint old = g.getPaint();
        g.setPaint(new GradientPaint(0, H, FinalBossCinematic.rgba(col, a), 0, H * 0.7f, FinalBossCinematic.rgba(col, 0)));
        g.fillRect(0, Math.round(H * 0.7f), W, H);
        g.setPaint(old);
    }

    // DANGER: the knight below 30% HP: a slow red heartbeat at the edges of the screen
    private void drawDanger(Graphics2D g) {
        if (knightHp >= 0.3f || knightHp <= 0) return;
        double beat = (t % 70) / 70.0;   // two soft thumps a beat, a bit under one beat a second
        float pulse = (float) (Math.max(0, Math.sin(beat * Math.PI * 4)) * (beat < 0.5 ? 1 : 0));
        int a = Math.round((90 + 90 * pulse) * (1 - knightHp / 0.3f) + 40);
        Paint old = g.getPaint();
        g.setPaint(new RadialGradientPaint(W / 2f, H / 2f, Math.max(W, H) * 0.75f, new float[]{0.55f, 1f},
                new Color[]{new Color(140, 0, 0, 0), new Color(140, 0, 0, Math.min(255, a))}));
        g.fillRect(0, 0, W, H);
        g.setPaint(old);
    }

    // the World 4 backdrop: zoomed in a little and panned as the camera turns and tilts, zoomed further as the camera zooms
    private void drawBackdrop() {
        BufferedImage bg = backdrop.drawSmall();
        int[] src = ((DataBufferInt) bg.getRaster().getDataBuffer()).getData();
        int sw = bg.getWidth(), sh = bg.getHeight();
        float zoom = 1.28f * (1 + (f / FOCAL - 1) * 0.6f), scale = zoom * W / sw;
        float margin = Math.max(0f, (zoom - 1) * W / 2f - 4), marginY = Math.max(0f, (zoom - 1) * H / 2f - 4);
        float pan = clampf(-f * (float) Math.tan(yaw - 0.03f) * 0.5f, -margin, margin);
        float tilt = clampf(f * (float) Math.tan(pitch) * 0.5f, -marginY, marginY);
        float dx = W / 2f - sw * scale / 2f + pan + shakeX * 0.5f, dy = horizon - 96 * scale + tilt + shakeY * 0.4f;
        for (int x = 0; x < W; x++) srcX[x] = Math.max(0, Math.min(sw - 1, (int) Math.floor((x - dx) / scale)));
        bgPanX = pan;
        bgTiltY = tilt;
        int keep = (int) (256 * (1 - bgDim) * (1 - 0.3f * crackAmt));
        for (int y = 0; y < H; y++) {
            int srow = Math.max(0, Math.min(sh - 1, (int) Math.floor((y - dy) / scale))) * sw, row = y * W;
            for (int x = 0; x < W; x++) {
                int c = src[srow + srcX[x]];
                framePx[row + x] = ((((c & 0xFF00FF) * keep) >>> 8) & 0xFF00FF) | ((((c & 0x00FF00) * keep) >>> 8) & 0x00FF00);
            }
        }
    }

    private void drawGroundMarks() {
        float[] p = new float[3];
        float sink = sinkDepth();
        Polygon shadow = new Polygon();
        float rx = (sink > 0.05f ? 2.8f : 2.3f) * bossScale, rz = (sink > 0.05f ? 2.0f : 1.6f) * bossScale;
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            if (project(bodyCenter[0] + (float) Math.cos(a) * rx, 0, bodyCenter[2] + (float) Math.sin(a) * rz, p)) shadow.addPoint((int) p[0], (int) p[1]);
        }
        frameG.setColor(sink > 0.05f ? new Color(14, 6, 5, 230) : new Color(0, 0, 0, 120));
        frameG.fillPolygon(shadow);
        if (visPhase() >= 3) {   // the ruin splits the ground: glowing cracks
            frameG.setColor(new Color(220, 30, 20));
            float[] q = new float[3];
            for (int k = 0; k < 9; k++) {
                double a = k * 0.7 + 0.3;
                float r1 = 3.4f + (k % 3) * 0.4f;
                if (project(bodyCenter[0] + (float) Math.cos(a) * 1.6f, 0, bodyCenter[2] + (float) Math.sin(a) * 1.3f, p)
                        && project(bodyCenter[0] + (float) Math.cos(a + 0.2) * r1, 0, bodyCenter[2] + (float) Math.sin(a + 0.2) * r1 * 0.8f, q))
                    pixelLine(frameG, p[0], p[1], q[0], q[1], 2);
            }
        }
    }

    private void pixelLine(Graphics2D g, float x0, float y0, float x1, float y1, int size) {
        int n = (int) Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) / 2 + 1;
        for (int i = 0; i <= n; i++) {
            float k = i / (float) n;
            g.fillRect(((int) (x0 + (x1 - x0) * k)) & ~1, ((int) (y0 + (y1 - y0) * k)) & ~1, size, size);
        }
    }

    private void drawKnight(int tick) {
        float[] p = new float[3];
        if (!project(KNIGHT_X, 0, KNIGHT_Z, p)) return;
        float sc = (f / p[2]) / (FOCAL / KNIGHT_Z) * 0.85f;   // the knight's sprite grows as the camera gets closer
        subjG.setColor(new Color(0, 0, 0, 110));
        subjG.fillOval(Math.round(p[0] - 90 * sc), Math.round(p[1] - 13 * sc), Math.round(180 * sc), Math.round(26 * sc));
        AffineTransform old = subjG.getTransform();
        subjG.translate(p[0], p[1]);
        subjG.scale(sc, sc);
        knight.draw(subjGh, 0, 0, tick);
        subjG.setTransform(old);
    }

    // ================================================================= DEFEAT
    // When a form falls (and at the very end): it staggers while cracks of light spread over it, flashes white, shafts of light
    // break out of it and it bursts into embers.

    // frames into the defeat animation, or -1
    private int defeatT() {
        if (rebirthT >= 0 && rebirthT < DEFEAT_FRAMES) return rebirthT;
        if (faintT >= 0 && faintT < DEFEAT_FRAMES) return faintT;
        return -1;
    }

    // after the final defeat Dabbah is gone
    private boolean bossGone() { return faintT >= DEFEAT_FRAMES; }

    private void updateDefeat() {
        int d = defeatT();
        if (d < 0) return;
        if (d < 44) shake = Math.max(shake, 2 + d * 0.12f);
        if (d == 44) { shake = 20; flashT = 9; }
    }

    // on the boss's pixels: glowing cracks that widen, the white flash, then the pixels breaking away
    private void defeatPass(int d) {
        float crack = clamp01f((d - 4) / 38f), flash = d >= 42 && d < 52 ? 1 - (d - 42) / 10f : 0, gone = clamp01f((d - 46) / 22f);
        int core = (t / 3) % 2 == 0 ? 0xfff8e0 : 0xffd060, edge = visPhase() >= 3 ? 0xd050ff : 0xff7a20;
        for (int y = 0; y < LH; y++) for (int x = 0; x < LW; x++) {
            int i = y * LW + x;
            if ((lowPx[i] >>> 24) == 0) continue;
            if (gone > 0 && hash(x, y, 909) < gone) { lowPx[i] = 0; continue; }
            if (!meshMask[i]) continue;
            double c = Math.abs(vn(x * 0.11, y * 0.11, 77) - 0.5);
            if (c < 0.06 * crack) lowPx[i] = 0xFF000000 | core;
            else if (c < 0.1 * crack) lowPx[i] = 0xFF000000 | edge;
            if (flash > 0) lowPx[i] = 0xFF000000 | lerpRGB(lowPx[i] & 0xFFFFFF, 0xffffff, flash);
        }
    }

    // shafts of light breaking out of it, then the burst of embers
    private void drawDefeatFx(int d) {
        float[] p = new float[3];
        if (!project(bossChest(), p)) return;
        if (d >= 26 && d < 62) {
            float a = d < 44 ? (d - 26) / 18f : 1 - (d - 44) / 18f;
            for (int k = 0; k < 9; k++) {
                double ang = k * Math.PI * 2 / 9 + t * 0.02 + hash(k, 0, 5) * 0.4;
                float len = 260 + 160 * hash(k, 1, 5), wd = 0.06f + 0.05f * hash(k, 2, 5);
                int[] xs = {(int) p[0], (int) (p[0] + Math.cos(ang - wd) * len), (int) (p[0] + Math.cos(ang + wd) * len)};
                int[] ys = {(int) p[1], (int) (p[1] + Math.sin(ang - wd) * len), (int) (p[1] + Math.sin(ang + wd) * len)};
                subjG.setColor(rgba(0xfff0c0, (int) (140 * a)));
                subjG.fillPolygon(xs, ys, 3);
            }
        }
        if (d >= 44) {
            float k = (d - 44) / (float) (DEFEAT_FRAMES - 44);
            int accent = visPhase() >= 3 ? 0xc040ff : (visPhase() == 2 ? 0xff8a20 : 0xff3020);
            for (int i = 0; i < 90; i++) {
                double ang = hash(i, 3, 6) * Math.PI * 2;
                float dist = (4 + 9 * hash(i, 4, 6)) * (d - 44);
                float x = p[0] + (float) Math.cos(ang) * dist, y = p[1] + (float) Math.sin(ang) * dist * 0.8f - 60 * k * k;
                subjG.setColor(rgba(i % 3 == 0 ? 0xffffff : (i % 3 == 1 ? 0xffd060 : accent), (int) (255 * (1 - k))));
                block(x, y, 4 + (i % 3) * 2);
            }
        }
    }

    // ================================================================= THE WORLD BREAKING

    private float crackAmt;          // 0..1: how far World 4's sky and ground have cracked open (form 2 onward)
    private boolean dimension;       // the fight has moved into the purple 4th dimension (form 3)
    private int flashT;              // frames left of a white flash
    private float bgPanX, bgTiltY;   // how far the backdrop is panned / tilted this frame (the cracks move with it)
    private final List<float[]> worldCracks = new ArrayList<>();
    private final float[][] rocks = new float[14][];
    private static final int NEB = 256;
    private float[] nebula;
    private final int[] dimPal = new int[256];
    private final float[][] stars = new float[70][];

    private void initWorldBreak() {
        int ph = Math.max(1, boss.getPhase());
        crackAmt = ph >= 2 ? 1 : 0;
        dimension = ph >= 3;
        flashT = 0;
        shatterT = -1;
        shards.clear();
        debris.clear();
        if (nebula == null) buildDimensionArt();
        worldCracks.clear();
        Random r = new Random(41);
        for (int k = 0; k < 9; k++) {   // jagged cracks across the sky (and two over the ground), each with a branch
            boolean ground = k >= 7;
            float x = 50 + r.nextFloat() * (W - 100), y = ground ? 270 + r.nextFloat() * 110 : 10 + r.nextFloat() * 150;
            double ang = r.nextDouble() * Math.PI * 2;
            int n = 10 + r.nextInt(7);
            float[] pts = new float[n * 2];
            for (int i = 0; i < n; i++) {
                pts[i * 2] = x;
                pts[i * 2 + 1] = y;
                ang += (r.nextDouble() - 0.5) * 1.4;
                float step = 14 + r.nextFloat() * 24;
                x += (float) Math.cos(ang) * step;
                y += (float) Math.sin(ang) * step * (ground ? 0.35f : 1f);
            }
            worldCracks.add(pts);
            int from = n / 2, m = 4 + r.nextInt(4);
            float[] br = new float[m * 2];
            float bx = pts[from * 2], by = pts[from * 2 + 1];
            double ba = ang + (r.nextBoolean() ? 1.2 : -1.2);
            for (int i = 0; i < m; i++) {
                br[i * 2] = bx;
                br[i * 2 + 1] = by;
                ba += (r.nextDouble() - 0.5) * 1.2;
                bx += (float) Math.cos(ba) * 15;
                by += (float) Math.sin(ba) * 15 * (ground ? 0.35f : 1f);
            }
            worldCracks.add(br);
        }
        for (int i = 0; i < rocks.length; i++)   // x, z, size, seed, height
            rocks[i] = new float[]{-9 + r.nextFloat() * 22, 10 + r.nextFloat() * 12, 0.25f + r.nextFloat() * 0.7f, r.nextFloat() * 6.28f, 0.8f + r.nextFloat() * 3.2f};
    }

    private void updateWorldBreak() {
        if (flashT > 0) flashT--;
        // form 1 -> 2: the cracks spread as the dragon bursts
        if (lastPhase == 2 && rebirthT >= 0) crackAmt = Math.max(crackAmt, clamp01f((rebirthT - 44) / 55f));
        // form 2 -> 3: the cutscene. The hydra fires its five beams at the knight; reality cracks around him and shatters.
        if (lastPhase >= 3 && rebirthT >= 0) {
            if (rebirthT == 132) { start(Anim.JUDGEMENT, 112, 70); cutsceneBeam = true; }
            if (rebirthT == 204) {
                float[] p = new float[3];
                if (project(knightChest(), p)) startShatter(p[0], p[1], 46, true);
                fx.add(new Fx(FX_STAR, knightChest(), null, 14, 0xffffff));
            }
        }
        // the final defeat: the dimension shatters in turn and World 4 comes back, mended
        if (faintT == DEFEAT_FRAMES + 4) {
            float[] p = new float[3];
            if (project(bossChest(), p)) startShatter(p[0], p[1], 34, false);
        }
        if (shatterT >= 0) {
            shatterT++;
            if (shatterT < crackLen) shake = Math.max(shake, 1 + 6f * shatterT / crackLen);
            if (shatterT == crackLen) breakNow();
            if (shatterT > crackLen) for (Shard sh : shards) sh.step();
            if (shatterT > crackLen + 60) endShatter();
        }
        for (Shard sh : debris) sh.drift(t);
    }

    // form 2: World 4 cracked open: rocks floating up off the ground, glowing purple cracks across the sky and the ground
    private void drawBrokenWorld() {
        if (crackAmt <= 0.001f) return;
        float[] p = new float[3];
        for (float[] rk : rocks) {
            float h = crackAmt * (rk[4] + 0.35f * (float) Math.sin(t * 0.013 + rk[3]));
            if (!project(rk[0], h, rk[1], p)) continue;
            float sz = f * rk[2] / p[2];
            int n = 7;
            int[] xs = new int[n], ys = new int[n];
            double rot = rk[3] + t * 0.003;
            for (int i = 0; i < n; i++) {
                double a = rot + i * Math.PI * 2 / n;
                float rr = sz * (0.7f + 0.5f * hash(i, (int) (rk[3] * 100), 13));
                xs[i] = ((int) (p[0] + Math.cos(a) * rr)) & ~1;
                ys[i] = ((int) (p[1] + Math.sin(a) * rr * 0.75f)) & ~1;
            }
            frameG.setColor(new Color(170, 60, 230, 120));   // purple light leaking from underneath
            frameG.fillRect((int) (p[0] - sz * 0.6f), (int) (p[1] + sz * 0.45f), (int) (sz * 1.2f), 3);
            frameG.setColor(new Color(36, 24, 22));
            frameG.fillPolygon(xs, ys, n);
            frameG.setColor(new Color(92, 62, 50));
            frameG.drawPolygon(xs, ys, n);
        }
        float pulse = 0.85f + 0.15f * (float) Math.sin(t * 0.08);
        for (float[] c : worldCracks) drawCrackLine(c, crackAmt, pulse);
    }

    private void drawCrackLine(float[] pts, float amount, float pulse) {
        int n = pts.length / 2;
        float reveal = amount * (n - 1), ox = bgPanX + shakeX * 0.5f, oy = bgTiltY + shakeY * 0.4f;
        Color[] cols = {new Color(110, 20, 170, 80), new Color(200, 80, 255, (int) (210 * pulse)), new Color(255, 235, 255)};
        int[] sizes = {8, 4, 2};
        for (int pass = 0; pass < 3; pass++) {
            frameG.setColor(cols[pass]);
            for (int i = 0; i + 1 < n && i < reveal; i++) {
                float k = Math.min(1f, reveal - i);
                float x0 = pts[i * 2] + ox, y0 = pts[i * 2 + 1] + oy;
                float x1 = x0 + (pts[i * 2 + 2] - pts[i * 2]) * k, y1 = y0 + (pts[i * 2 + 3] - pts[i * 2 + 1]) * k;
                pixelLine(frameG, x0, y0, x1, y1, sizes[pass]);
            }
        }
    }

    // ---- the purple 4th dimension

    private void buildDimensionArt() {
        nebula = new float[NEB * NEB];
        for (int y = 0; y < NEB; y++) for (int x = 0; x < NEB; x++) {
            float v = 0, amp = 0.5f;
            for (int o = 0; o < 5; o++) {
                int per = 4 << o;
                v += amp * vnp(x * per / (double) NEB, y * per / (double) NEB, per, 300 + o);
                amp *= 0.5f;
            }
            nebula[y * NEB + x] = v / 0.97f;
        }
        float[] at = {0f, 0.3f, 0.5f, 0.65f, 0.78f, 0.9f, 1f};
        int[] col = {0x040108, 0x150528, 0x3a0c66, 0x6a1aa8, 0xa438e0, 0xe08cff, 0xfff0ff};
        for (int i = 0; i < 256; i++) {
            float v = i / 255f;
            int k = 0;
            while (k < at.length - 2 && v > at[k + 1]) k++;
            dimPal[i] = lerpRGB(col[k], col[k + 1], (v - at[k]) / (at[k + 1] - at[k]));
        }
        Random r = new Random(9);
        for (int i = 0; i < stars.length; i++) stars[i] = new float[]{r.nextFloat() * 1400 - 300, r.nextFloat() * 700 - 50, r.nextFloat() * 6.28f};
    }

    // tileable value noise (wraps every `period` cells)
    private static double vnp(double x, double y, int period, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        int x0 = Math.floorMod(xi, period), x1 = Math.floorMod(xi + 1, period), y0 = Math.floorMod(yi, period), y1 = Math.floorMod(yi + 1, period);
        double a = hash(x0, y0, seed), b = hash(x1, y0, seed), c = hash(x0, y1, seed), d = hash(x1, y1, seed);
        double top = a + (b - a) * fx, bot = c + (d - c) * fx;
        return top + (bot - top) * fy;
    }

    // a swirling purple void (two layers of drifting nebula, in 15-bit colour with dithering like the rest), stars, lightning,
    // a glowing grid floor that the fighters stand on, and pieces of the shattered world drifting in it
    private void drawDimension() {
        float ox = t * 0.25f - yaw * 300, oy = pitch * 300 - t * 0.08f;
        float keep = 1 - bgDim * 0.6f;
        for (int y = 0; y < LH; y++) {
            float warp = (float) Math.sin((y + t * 0.6f) * 0.045f) * 10, grad = y / (float) LH;
            int v1 = ((int) (y + oy)) & (NEB - 1), v2 = ((int) (y * 0.5f + t * 0.15f + pitch * 120)) & (NEB - 1);
            for (int x = 0; x < LW; x++) {
                int u1 = ((int) (x + ox + warp)) & (NEB - 1), u2 = ((int) (x * 0.5f - t * 0.4f - yaw * 120)) & (NEB - 1);
                float n = nebula[v1 * NEB + u1] * 0.65f + nebula[v2 * NEB + u2] * 0.55f;
                n += 0.1f * (float) Math.sin(x * 0.02f + y * 0.035f - t * 0.03f);   // a band of energy sweeping through
                n -= 0.12f * grad;
                int idx = (int) ((n - 0.33f) / 0.6f * 255 * keep);
                int c = ps1(dimPal[Math.max(0, Math.min(255, idx))], x, y);
                int o = y * S * W + x * S;
                for (int j = 0; j < S && y * S + j < H; j++, o += W) { framePx[o] = c; framePx[o + 1] = c; }
            }
        }
        // stars, twinkling, with a little parallax
        for (int i = 0; i < stars.length; i++) {
            float[] st = stars[i];
            float x = st[0] - yaw * 160 + (t * 0.05f * (1 + i % 3)) % 1400, y = st[1] + pitch * 160;
            x = ((x % 1400) + 1400) % 1400 - 300;
            if ((t / 6 + i) % 9 == 0) continue;
            frameG.setColor(i % 4 == 0 ? new Color(255, 190, 255) : new Color(230, 220, 255));
            frameG.fillRect(((int) x) & ~1, ((int) y) & ~1, i % 5 == 0 ? 4 : 2, i % 5 == 0 ? 4 : 2);
        }
        // lightning crackling across the void now and then
        if ((t / 6) % 15 == 0) {
            Random r = new Random(t / 90 * 7 + 3);
            float x = -20, y = 40 + r.nextFloat() * 160;
            for (int pass = 0; pass < 2; pass++) {
                Random rr = new Random(t / 90 * 7 + 3);
                float lx = x, ly = y;
                frameG.setColor(pass == 0 ? new Color(190, 90, 255, 120) : new Color(250, 230, 255));
                while (lx < W + 20) {
                    float nx = lx + 20 + rr.nextFloat() * 40, ny = ly + (rr.nextFloat() - 0.5f) * 50;
                    pixelLine(frameG, lx, ly, nx, ny, pass == 0 ? 6 : 2);
                    lx = nx;
                    ly = ny;
                }
            }
        }
        for (Shard sh : debris) drawShard(sh, true);   // pieces of the shattered world drifting far away
        // the grid floor
        float[] a = new float[3], b = new float[3];
        for (int line = 0; line < 2; line++) {
            for (float g = -16; g <= 20.01f; g += 2) {
                for (int sgm = 0; sgm < 14; sgm++) {
                    float t0 = sgm / 14f, t1 = (sgm + 1) / 14f;
                    boolean ok = line == 0
                            ? project(-16 + 36 * t0, 0, 1 + g + 17, a) && project(-16 + 36 * t1, 0, 1 + g + 17, b)
                            : project(g, 0, 1 + 33 * t0, a) && project(g, 0, 1 + 33 * t1, b);
                    if (!ok) continue;
                    float fade = clamp01f(1 - (a[2] + b[2]) / 2 / 34f);
                    frameG.setColor(rgba(0xd060ff, (int) (200 * fade * fade)));
                    pixelLine(frameG, a[0], a[1], b[0], b[1], 2);
                }
            }
        }
        // pools of light under the fighters
        if (project(KNIGHT_X, 0, KNIGHT_Z, a)) { frameG.setColor(new Color(200, 110, 255, 60)); frameG.fillOval((int) a[0] - 90, (int) a[1] - 14, 180, 28); }
        if (project(bodyCenter[0], 0, bodyCenter[2], a)) { frameG.setColor(new Color(200, 110, 255, 50)); frameG.fillOval((int) a[0] - 150, (int) a[1] - 20, 300, 40); }
    }

    // ---- reality cracking like glass and shattering (the shards are cut from a snapshot of the screen)

    private static final class Shard {
        final Polygon poly;
        final float cx, cy, seed;
        float ox, oy, vx, vy, rot, vr, sc = 1, vs, alpha = 1;
        int age;
        Shard(Polygon poly, float cx, float cy, float seed) { this.poly = poly; this.cx = cx; this.cy = cy; this.seed = seed; }
        void step() {
            age++;
            ox += vx;
            oy += vy;
            vy += 0.12f;
            rot += vr;
            sc = Math.max(0.05f, sc + vs);
            alpha = Math.max(0f, 1 - Math.max(0, age - 34) / 26f);
        }
        void drift(int t) {   // a piece of the old world floating in the void
            ox += vx;
            oy += vy + 0.15f * (float) Math.sin(t * 0.02 + seed);
            rot += vr;
        }
    }

    private volatile int shatterT = -1;
    private int crackLen;
    private boolean shatterToDimension;
    private float shCx, shCy;
    private volatile float[][] ringX, ringY;
    private final List<Shard> shards = new CopyOnWriteArrayList<>(), debris = new CopyOnWriteArrayList<>();
    private BufferedImage snap;
    private static final float[] RINGS = {0, 22, 62, 122, 205, 315, 465, 680, 1000};
    private static final int SECTORS = 14;

    // cracks start spreading from (cx, cy); after `len` frames the screen breaks
    private void startShatter(float cx, float cy, int len, boolean toDimension) {
        crackLen = len;
        shatterToDimension = toDimension;
        shCx = cx;
        shCy = cy;
        Random r = new Random(t * 31L + 7);
        // built in local arrays first, so a frame being drawn at the same moment never sees half-filled cracks
        float[][] rx = new float[RINGS.length][SECTORS], ry = new float[RINGS.length][SECTORS];
        for (int k = 0; k < RINGS.length; k++) for (int j = 0; j < SECTORS; j++) {
            double ang = j * Math.PI * 2 / SECTORS + (r.nextFloat() - 0.5f) * 0.3f;
            float rad = RINGS[k] * (0.82f + r.nextFloat() * 0.36f);
            rx[k][j] = cx + (float) Math.cos(ang) * rad;
            ry[k][j] = cy + (float) Math.sin(ang) * rad;
        }
        shards.clear();
        ringX = rx;
        ringY = ry;
        shatterT = 0;
    }

    // the moment it breaks: the screen as it was becomes shards flying away; behind them is the new world
    private void breakNow() {
        if (snap == null) snap = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        System.arraycopy(framePx, 0, ((DataBufferInt) snap.getRaster().getDataBuffer()).getData(), 0, framePx.length);
        flashT = 8;
        shake = 22;
        Random r = new Random(t * 17L + 3);
        for (int k = 0; k + 1 < RINGS.length; k++) for (int j = 0; j < SECTORS; j++) {
            int j2 = (j + 1) % SECTORS;
            Polygon poly = new Polygon(
                    new int[]{(int) ringX[k][j], (int) ringX[k][j2], (int) ringX[k + 1][j2], (int) ringX[k + 1][j]},
                    new int[]{(int) ringY[k][j], (int) ringY[k][j2], (int) ringY[k + 1][j2], (int) ringY[k + 1][j]}, 4);
            float cx = (ringX[k][j] + ringX[k][j2] + ringX[k + 1][j2] + ringX[k + 1][j]) / 4, cy = (ringY[k][j] + ringY[k][j2] + ringY[k + 1][j2] + ringY[k + 1][j]) / 4;
            Shard sh = new Shard(poly, cx, cy, r.nextFloat() * 6.28f);
            float dx = cx - shCx, dy = cy - shCy, len = (float) Math.max(1, Math.sqrt(dx * dx + dy * dy));
            float sp = 2 + k * 1.3f + r.nextFloat() * 3;
            sh.vx = dx / len * sp;
            sh.vy = dy / len * sp - 2.5f;
            sh.vr = (r.nextFloat() - 0.5f) * 0.09f;
            sh.vs = r.nextFloat() < 0.55f ? 0.02f : -0.013f;   // some fly at the camera, some fall away into the void
            shards.add(sh);
        }
        dimension = shatterToDimension;
        crackAmt = 0;
        debris.clear();
    }

    private void endShatter() {
        if (shatterToDimension) {   // a few pieces stay, drifting in the void
            Random r = new Random(5);
            for (Shard sh : shards) {
                if (sh.vs >= 0 || debris.size() >= 12) continue;
                float x = sh.cx + sh.ox, y = sh.cy + sh.oy;
                if (x < -40 || x > W + 40 || y < -40 || y > H * 0.65f) continue;
                sh.alpha = 0.9f;
                sh.sc = Math.max(sh.sc, 0.3f);
                sh.vx = (r.nextFloat() - 0.5f) * 0.25f;
                sh.vy = -0.05f;
                sh.vr = (r.nextFloat() - 0.5f) * 0.006f;
                debris.add(sh);
            }
        }
        shards.clear();
        shatterT = -1;
    }

    private void drawShatter() {
        if (shatterT < 0) return;
        if (shatterT < crackLen) drawCracks(shatterT / (float) crackLen);
        else for (Shard sh : shards) drawShard(sh, false);
    }

    // white cracks with a purple glow spreading out from the impact, along the edges the shards will break on
    private void drawCracks(float p) {
        float[][] rx = ringX, ry = ringY;   // read once (update() may replace them)
        if (rx == null || ry == null) return;
        float reach = easeOut(p) * 760;
        for (int pass = 0; pass < 2; pass++) {
            frameG.setColor(pass == 0 ? new Color(170, 60, 255, 110) : new Color(255, 245, 255));
            int size = pass == 0 ? 6 : 2;
            for (int j = 0; j < SECTORS; j++) {
                for (int k = 0; k + 1 < RINGS.length; k++) {   // the radial cracks
                    float d0 = dist2(rx[k][j], ry[k][j]), d1 = dist2(rx[k + 1][j], ry[k + 1][j]);
                    if (d0 >= reach) break;
                    float q = Math.min(1f, (reach - d0) / Math.max(1f, d1 - d0));
                    pixelLine(frameG, rx[k][j], ry[k][j], rx[k][j] + (rx[k + 1][j] - rx[k][j]) * q,
                            ry[k][j] + (ry[k + 1][j] - ry[k][j]) * q, size);
                }
                for (int k = 1; k < RINGS.length; k++) {        // the rings joining them
                    int j2 = (j + 1) % SECTORS;
                    if (dist2(rx[k][j], ry[k][j]) < reach * 0.85f && dist2(rx[k][j2], ry[k][j2]) < reach * 0.85f)
                        pixelLine(frameG, rx[k][j], ry[k][j], rx[k][j2], ry[k][j2], size);
                }
            }
        }
        frameG.setColor(new Color(255, 255, 255, 200));   // the point of impact glows
        int r = (int) (10 + 20 * p);
        frameG.fillOval((int) shCx - r, (int) shCy - r, r * 2, r * 2);
    }

    private float dist2(float x, float y) {
        float dx = x - shCx, dy = y - shCy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void drawShard(Shard sh, boolean far) {
        if (sh.alpha <= 0.01f || snap == null) return;
        Graphics2D g2 = (Graphics2D) frameG.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (sh.alpha < 1) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sh.alpha));
        g2.translate(sh.cx + sh.ox, sh.cy + sh.oy);
        g2.rotate(sh.rot);
        g2.scale(sh.sc, sh.sc);
        g2.translate(-sh.cx, -sh.cy);
        g2.clip(sh.poly);
        g2.drawImage(snap, 0, 0, null);
        if (far) {   // tinted by the void
            g2.setColor(new Color(90, 20, 150, 110));
            g2.fill(sh.poly);
        }
        g2.setClip(null);
        g2.setStroke(new BasicStroke(Math.max(1f, 2f / sh.sc)));
        g2.setColor(far ? new Color(220, 150, 255, 160) : new Color(245, 225, 255, 220));
        g2.draw(sh.poly);
        g2.dispose();
    }

    // ================================================================= projection

    // world -> camera space: move to the eye, turn by the camera's yaw, then tilt by its pitch (+z is straight ahead)
    private void view(float x, float y, float z, float[] o) {
        float px = x - eye[0], py = y - eye[1], pz = z - eye[2];
        float rx = px * cy - pz * sy, rz = px * sy + pz * cy;     // yaw
        o[0] = rx;
        o[1] = py * cp - rz * sp;                                 // pitch
        o[2] = py * sp + rz * cp;
    }

    private boolean project(float x, float y, float z, float[] o) {
        view(x, y, z, o);
        if (o[2] < 0.25f) return false;
        float vx = o[0], vy = o[1], vz = o[2];
        o[0] = W / 2f + shakeX + f * vx / vz;
        o[1] = horizon + shakeY - f * vy / vz;
        o[2] = vz;
        return true;
    }

    private boolean project(float[] w, float[] o) { return project(w[0], w[1], w[2], o); }

    // ================================================================= the model

    private static final class Mat {
        final float[] m;
        Mat(float... m) { this.m = m; }
        static Mat id() { return new Mat(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0); }
        Mat mul(Mat o) {
            float[] a = m, b = o.m, r = new float[12];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) r[i * 4 + j] = a[i * 4] * b[j] + a[i * 4 + 1] * b[4 + j] + a[i * 4 + 2] * b[8 + j];
                r[i * 4 + 3] = a[i * 4] * b[3] + a[i * 4 + 1] * b[7] + a[i * 4 + 2] * b[11] + a[i * 4 + 3];
            }
            return new Mat(r);
        }
        Mat t(float x, float y, float z) { return mul(new Mat(1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z)); }
        Mat t(float[] p) { return t(p[0], p[1], p[2]); }
        Mat s(float k) { return mul(new Mat(k, 0, 0, 0, 0, k, 0, 0, 0, 0, k, 0)); }
        Mat rx(float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return mul(new Mat(1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0)); }
        Mat ry(float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return mul(new Mat(c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0)); }
        Mat rz(float a) { float c = (float) Math.cos(a), s = (float) Math.sin(a); return mul(new Mat(c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0)); }
        float[] ap(float x, float y, float z) {
            return new float[]{m[0] * x + m[1] * y + m[2] * z + m[3], m[4] * x + m[5] * y + m[6] * z + m[7], m[8] * x + m[9] * y + m[10] * z + m[11]};
        }
        float[] ap(float[] p) { return ap(p[0], p[1], p[2]); }
        float[] dir(float x, float y, float z) {
            float a = m[0] * x + m[1] * y + m[2] * z, b = m[4] * x + m[5] * y + m[6] * z, c = m[8] * x + m[9] * y + m[10] * z;
            float l = (float) Math.sqrt(a * a + b * b + c * c);
            return l < 1e-9f ? new float[]{0, 1, 0} : new float[]{a / l, b / l, c / l};
        }
        // a frame at a whose +y axis points at b (used for limbs between two points)
        static Mat align(float[] a, float[] b) {
            float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-5f) return id().t(a);
            dx /= len; dy /= len; dz /= len;
            float hx = Math.abs(dy) < 0.9f ? 0 : 1, hy = Math.abs(dy) < 0.9f ? 1 : 0;
            float xx = hy * dz, xy = -hx * dz, xz = hx * dy - hy * dx;
            float xl = (float) Math.sqrt(xx * xx + xy * xy + xz * xz);
            xx /= xl; xy /= xl; xz /= xl;
            float zx = xy * dz - xz * dy, zy = xz * dx - xx * dz, zz = xx * dy - xy * dx;
            return new Mat(xx, dx, zx, a[0], xy, dy, zy, a[1], xz, dz, zz, a[2]);
        }
    }

    private static float distance(float[] a, float[] b) {
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    private static float[] lerp3(float[] a, float[] b, float k) { return new float[]{a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k, a[2] + (b[2] - a[2]) * k}; }
    private static float[] v3(float x, float y, float z) { return new float[]{x, y, z}; }
    private static float[] add(float[] a, float x, float y, float z) { return new float[]{a[0] + x, a[1] + y, a[2] + z}; }
    private static float[] bezier(float[] a, float[] b, float[] c, float[] d, float k) {
        float u = 1 - k, w0 = u * u * u, w1 = 3 * u * u * k, w2 = 3 * u * k * k, w3 = k * k * k;
        return new float[]{a[0] * w0 + b[0] * w1 + c[0] * w2 + d[0] * w3, a[1] * w0 + b[1] * w1 + c[1] * w2 + d[1] * w3, a[2] * w0 + b[2] * w1 + c[2] * w2 + d[2] * w3};
    }

    // ---- the vertex list: per triangle 3 vertices of (x, y, z, nx, ny, nz, u, v) in world space
    private static final int MAX_TRIS = 7000, VS = 8;
    private final float[] tv = new float[MAX_TRIS * 3 * VS];
    private final int[] tmat = new int[MAX_TRIS];
    private int tris;

    private float[] vert(Mat m, float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        float[] p = m.ap(x, y, z), n = m.dir(nx, ny, nz);
        return new float[]{p[0], p[1], p[2], n[0], n[1], n[2], u, v};
    }

    private void tri(float[] a, float[] b, float[] c, int mat) {
        if (tris >= MAX_TRIS) return;
        int o = tris * 3 * VS;
        System.arraycopy(a, 0, tv, o, VS);
        System.arraycopy(b, 0, tv, o + VS, VS);
        System.arraycopy(c, 0, tv, o + 2 * VS, VS);
        tmat[tris++] = mat;
    }

    private void quad(float[] a, float[] b, float[] c, float[] d, int mat) { tri(a, b, c, mat); tri(a, c, d, mat); }

    private void ellipsoid(Mat m, float rx, float ry, float rz, int lat, int lon, int mat, float uRep, float vRep) {
        float[][][] p = new float[lat + 1][lon + 1][];
        for (int i = 0; i <= lat; i++) {
            double a = -Math.PI / 2 + i / (double) lat * Math.PI;
            float y = (float) Math.sin(a), r = (float) Math.cos(a);
            for (int j = 0; j <= lon; j++) {
                double b = j * Math.PI * 2 / lon;
                float x = (float) Math.cos(b) * r, z = (float) Math.sin(b) * r;
                p[i][j] = vert(m, x * rx, y * ry, z * rz, x / rx, y / ry, z / rz, j / (float) lon * uRep, i / (float) lat * vRep);
            }
        }
        for (int i = 0; i < lat; i++) for (int j = 0; j < lon; j++) quad(p[i][j], p[i][j + 1], p[i + 1][j + 1], p[i + 1][j], mat);
    }

    // an ellipsoid lying along the local z axis (bodies, snouts)
    private void ellipsoidZ(Mat m, float rx, float ry, float rz, int lat, int lon, int mat, float uRep, float vRep) {
        ellipsoid(m.rx((float) Math.PI / 2), rx, rz, ry, lat, lon, mat, uRep, vRep);
    }

    private void tube(Mat m, int n, float r0, float r1, float len, int mat, float vRep, boolean cap) {
        float[][] lo = new float[n + 1][], hi = new float[n + 1][];
        float sl = (r0 - r1) / Math.max(0.01f, len);
        for (int i = 0; i <= n; i++) {
            float a = (float) (i * Math.PI * 2 / n), c = (float) Math.cos(a), s = (float) Math.sin(a);
            lo[i] = vert(m, c * r0, 0, s * r0, c, sl, s, i / (float) n, 0);
            hi[i] = vert(m, c * r1, len, s * r1, c, sl, s, i / (float) n, vRep);
        }
        for (int i = 0; i < n; i++) quad(lo[i], lo[i + 1], hi[i + 1], hi[i], mat);
        if (cap && r1 > 0) {
            float[] c1 = vert(m, 0, len, 0, 0, 1, 0, 0.5f, vRep);
            for (int i = 0; i < n; i++) tri(c1, hi[i], hi[i + 1], mat);
        }
    }

    private void limb(Mat root, float[] a, float[] b, float r0, float r1, int sides, int mat) {
        float len = distance(a, b);
        tube(root.mul(Mat.align(a, b)), sides, r0, r1, len, mat, len * 1.2f, r1 > 0.02f);
    }

    // a smooth tube along a list of points (necks, tails, horns)
    private void curve(Mat root, float[][] pts, float r0, float r1, int sides, int mat) {
        for (int i = 0; i + 1 < pts.length; i++) {
            float k0 = i / (float) (pts.length - 1), k1 = (i + 1) / (float) (pts.length - 1);
            float ra = r0 + (r1 - r0) * k0, rb = r0 + (r1 - r0) * k1;
            limb(root, pts[i], pts[i + 1], ra, rb, sides, mat);
            if (i + 2 < pts.length) ellipsoid(root.t(pts[i + 1]), rb, rb, rb, 3, sides, mat, 1, 0.3f);
        }
    }

    // a flat polygon fan in the local x/y plane (wing membranes, the spade of the tail); lit from both sides
    private void fan(Mat m, float[][] outline, int mat) {
        float[] c = vert(m, 0, 0, 0, 0, 0, -1, 0.5f, 0.5f);
        for (int i = 0; i + 1 < outline.length; i++) {
            float[] a = vert(m, outline[i][0], outline[i][1], 0, 0, 0, -1, 0.5f + outline[i][0] * 0.3f, 0.5f + outline[i][1] * 0.3f);
            float[] b = vert(m, outline[i + 1][0], outline[i + 1][1], 0, 0, 0, -1, 0.5f + outline[i + 1][0] * 0.3f, 0.5f + outline[i + 1][1] * 0.3f);
            tri(c, a, b, mat);
        }
    }

    // a membrane stretched between world points (the wings): triangles from a hub to each pair of neighbouring points
    private void membrane(float[] hub, float[][] rim, int mat) {
        for (int i = 0; i + 1 < rim.length; i++) {
            float[] a = rim[i], b = rim[i + 1];
            tri(new float[]{hub[0], hub[1], hub[2], 0, 0, -1, 0.5f, 0}, new float[]{a[0], a[1], a[2], 0, 0, -1, i / 4f, 1},
                    new float[]{b[0], b[1], b[2], 0, 0, -1, (i + 1) / 4f, 1}, mat);
        }
    }

    private void diamond(Mat m, float w, float h, int mat) {
        quad(vert(m, 0, h, 0, 0, 0, -1, 0, 0), vert(m, w, 0, 0, 0, 0, -1, 0, 0), vert(m, 0, -h, 0, 0, 0, -1, 0, 0), vert(m, -w, 0, 0, 0, 0, -1, 0, 0), mat);
    }

    // ---- poses

    private int visPhase() {
        int phase = Math.max(1, Math.min(3, boss.getPhase()));
        if (rebirthT >= 0 && rebirthT < DEFEAT_FRAMES) phase = Math.max(1, phase - 1);   // the old form falls before the new one rises
        if (pendingRebirth) phase = Math.max(1, phase - 1);   // CINEMATIC: the old form stays until the cinematic is over
        return phase;
    }

    private float sinkDepth() {
        float s = 0;
        if (!introDone) s = 1 - smooth(clamp01f((introTimer - 20) / 100f));
        if (rebirthT >= 0) s = rebirthT < DEFEAT_FRAMES ? 0 : 1 - smooth(clamp01f((rebirthT - DEFEAT_FRAMES) / 65f));   // the new form rises
        if (faintT >= 0) s = 0;
        return s;
    }

    private static float bell(float p, float a, float b) { return p <= a || p >= b ? 0 : (float) Math.sin(Math.PI * (p - a) / (b - a)); }
    private static float ramp(float p, float a, float b) { return clamp01f((p - a) / (b - a)); }

    private void buildBoss() {
        tris = 0;
        mouthCount = 0;
        int phase = visPhase();
        bossScale = phase == 1 ? 0.9f : (phase == 2 ? 0.95f : 1.0f);
        float sinkY = -sinkDepth() * 6.5f * bossScale;
        float collapse = 0;
        float recoil = hurtT >= 0 ? (float) Math.sin(Math.PI * Math.min(1f, hurtT / 20f)) : 0;
        int d = defeatT();
        if (d >= 0) {   // the defeat: it staggers and buckles, shaking, as the light breaks out of it
            recoil = 0.55f + 0.45f * (float) Math.sin(t * 0.9);
            collapse = 0.5f * smooth(clamp01f(d / 45f));
        }
        curYaw = 1.15f;
        if (phase < 3) buildDragon(sinkY, collapse, recoil);
        else buildHydra(sinkY, collapse, recoil);
    }

    // ---------------------------------------------------------------- forms 1 and 2: the dragon

    private void buildDragon(float sinkY, float collapse, float recoil) {
        float p = animT, breath = (float) Math.sin(t * 0.045);
        float crouch = 0, hop = 0, flap = 0, roar = 0, charge = 0, claw = 0, stomp = 0, headBack = 0, jaw = 0.1f, wingSpread = 0.35f;
        switch (anim) {
            case SLAM:   // crouch, leap on its wings, crash down
                crouch = bell(p, 0, 16) + 0.5f * bell(p, 50, 64);
                hop = p < 14 ? 0 : (p < 34 ? 0.6f * easeOut(ramp(p, 14, 34)) : (p < 42 ? 0.6f : (p < 50 ? 0.6f * (1 - easeIn(ramp(p, 42, 50))) : 0)));
                flap = p >= 12 && p < 46 ? (float) Math.sin(p * 0.6) : 0;
                wingSpread = 0.35f + 0.65f * Math.max(hop / 0.6f, bell(p, 8, 50));
                break;
            case WRATH:  // roar, charge, rake with the claws
                roar = bell(p, 0, 20);
                jaw = 0.1f + 0.9f * roar;
                charge = p < 14 ? 0 : (p < 32 ? easeIn(ramp(p, 14, 32)) * 0.38f : 0.38f * (1 - smooth(ramp(p, 38, 72))));
                claw = p < 20 ? 0 : (p < 30 ? easeOut(ramp(p, 20, 30)) : (p < 36 ? 1 - ramp(p, 30, 36) * 1.6f : 0));
                wingSpread = 0.35f + 0.4f * roar;
                break;
            case RUPTURE: // rear and stomp; a burning crack runs to the knight
                crouch = -0.6f * bell(p, 0, 18);
                stomp = p < 12 ? easeOut(ramp(p, 0, 12)) : (p < 18 ? 1 - easeIn(ramp(p, 12, 18)) : 0);
                break;
            case JUDGEMENT: // head pulls back, energy gathers in the mouth, the beam
                headBack = bell(p, 0, 70) * 0.8f;
                jaw = 0.1f + 0.9f * ramp(p, 26, 40) * (p < 96 ? 1 : 0);
                wingSpread = 0.35f + 0.65f * bell(p, 10, 100);
                break;
            default: break;
        }
        float tx = KNIGHT_X - BOSS_X, tz = KNIGHT_Z - BOSS_Z;
        float rx = BOSS_X + tx * charge - tx * 0.05f * recoil, rz = BOSS_Z + tz * charge - tz * 0.05f * recoil;
        Mat root = Mat.id().t(rx, sinkY - collapse * 1.0f + 2.4f * hop, rz).ry(curYaw).s(bossScale);
        float[] hip = v3(0, 2.0f, 1.0f);
        float pitch = 0.5f * stomp + 0.08f * crouch * -1f - 0.1f * recoil - 0.12f * collapse;
        Mat body = root.mul(Mat.id().t(hip).rx(pitch).t(-hip[0], -hip[1] - 0.5f * Math.max(0, crouch) - 0.5f * collapse, -hip[2]));

        // body: a deep chest and lean hips, black scales, a plated belly
        float bs = 1 + 0.02f * breath;
        ellipsoidZ(body.t(0, 2.3f, -0.55f), 0.82f * bs, 0.85f * bs, 1.2f, 7, 10, T_SCALE, 3, 2);
        ellipsoidZ(body.t(0, 2.05f, 0.8f), 0.66f, 0.7f, 1.0f, 6, 10, T_SCALE, 3, 2);
        ellipsoidZ(body.t(0, 1.85f, -0.35f), 0.6f, 0.45f, 1.1f, 5, 8, T_BELLY, 1, 3);
        for (int k = 0; k < 7; k++) {   // spikes along the spine
            float z = -1.2f + k * 0.42f;
            limb(body, v3(0, 3.05f - k * 0.08f, z), v3(0, 3.5f - k * 0.1f, z + 0.25f), 0.12f, 0f, 4, T_HORN);
        }

        // legs: front legs reach forward, hind legs crouched, three claws each
        for (int side = -1; side <= 1; side += 2) {
            float lift = side < 0 ? Math.max(stomp, claw) : stomp * 0.6f;
            float[] sh = v3(side * 0.62f, 2.0f, -1.0f), el = v3(side * 0.78f, 1.15f, -0.85f + 0.4f * lift * 0f), wr = v3(side * 0.72f, 0.2f, -1.25f);
            el = add(el, 0, 0.6f * lift, -0.5f * lift);
            wr = add(wr, 0, 1.3f * lift + 0.25f * hop, -0.9f * lift);
            if (side < 0 && claw > 0) wr = add(wr, 0, 0.3f * claw, -0.6f * claw);
            float[] shW = body.ap(sh), elW = body.ap(el), wrW = body.ap(wr);
            if (collapse > 0) wrW = lerp3(wrW, add(wrW, 0, -0.2f, -0.6f), collapse);
            if (hop <= 0.05f && lift <= 0.05f) wrW[1] = Math.max(0.12f * bossScale + sinkY, wrW[1]);
            leg(shW, elW, wrW, 0.32f, 0.2f, curYaw);
            if (side < 0) frontFeet = v3(wrW[0], 0, wrW[2]);
            float[] hp = v3(side * 0.6f, 2.0f, 1.0f), kn = v3(side * 0.78f, 1.35f, 0.45f), hk = v3(side * 0.7f, 0.7f, 1.25f), ft = v3(side * 0.7f, 0.1f, 0.95f);
            Mat legRoot = root.mul(Mat.id().t(0, -0.45f * Math.max(0, crouch) + 0.5f * hop, 0));
            float[] hpW = body.ap(hp), knW = legRoot.ap(kn), hkW = legRoot.ap(hk), ftW = legRoot.ap(add(ft, 0, 0.3f * hop, 0));
            limb(Mat.id(), hpW, knW, 0.4f * bossScale, 0.26f * bossScale, 7, T_SCALE);
            limb(Mat.id(), knW, hkW, 0.24f * bossScale, 0.15f * bossScale, 6, T_SCALE);
            leg(hkW, hkW, ftW, 0.15f, 0.12f, curYaw);
        }
        bodyCenter = body.ap(v3(0, 0, -0.1f));
        bodyCenter[1] = 0;

        // the tail: long, sweeping back and curling, with a spade tip
        float sw = (float) Math.sin(t * 0.03) * 0.4f + 0.6f * recoil + 0.8f * bell(p, 12, 30) * (anim == Anim.RUPTURE ? 1 : 0);
        float[][] tail = {v3(0, 2.15f, 1.65f), v3(0.1f * sw, 1.6f, 2.6f), v3(0.6f * sw + 0.2f, 0.7f, 3.5f), v3(1.2f * sw + 0.9f, 0.35f, 4.1f), v3(1.6f * sw + 1.8f, 0.4f, 4.3f)};
        curve(body, tail, 0.5f, 0.1f, 7, T_SCALE);
        float[] tip = body.ap(tail[4]);
        fan(Mat.id().t(tip).ry(curYaw + 1.2f).s(bossScale), new float[][]{{0, 0}, {0.35f, 0.2f}, {0.5f, 0}, {0.35f, -0.2f}, {0, 0}}, T_HORN);

        // wings: arm bones and four fingers with dark membranes between them
        for (int side = -1; side <= 1; side += 2) {
            float up = wingSpread + 0.35f * flap + 0.04f * breath;
            float[] a = v3(side * 0.55f, 2.95f, -0.6f);
            float[] e = v3(side * (1.4f + 0.6f * up), 3.4f + 1.1f * up, 0.1f - 0.2f * up);
            float[] w = v3(side * (1.9f + 1.4f * up), 4.0f + 1.7f * up, 0.6f);
            float[][] tips = new float[4][];
            for (int k = 0; k < 4; k++) {
                float kk = k / 3f;
                tips[k] = add(w, side * (0.6f + 0.6f * up) * (1 - kk) - side * 0.2f, -1.2f - 1.6f * kk * (1.2f - 0.4f * up), 0.4f + 1.4f * kk);
            }
            float[] aW = body.ap(a), eW = body.ap(e), wW = body.ap(w);
            limb(Mat.id(), aW, eW, 0.17f * bossScale, 0.12f * bossScale, 5, T_SCALE);
            limb(Mat.id(), eW, wW, 0.12f * bossScale, 0.08f * bossScale, 5, T_SCALE);
            float[][] rim = new float[6][];
            rim[0] = wW;
            for (int k = 0; k < 4; k++) {
                float[] tW = body.ap(tips[k]);
                limb(Mat.id(), wW, tW, 0.06f * bossScale, 0.02f * bossScale, 4, T_HORN);
                rim[k + 1] = tW;
            }
            rim[5] = body.ap(v3(side * 0.6f, 2.6f, 0.9f));
            membrane(eW, rim, T_WING);
        }

        // neck and head
        float[] n0 = v3(0, 2.75f, -1.4f);
        float[] n1 = v3(0, 3.5f + 0.4f * headBack, -1.9f + 0.6f * headBack);
        float[] n2 = v3(0, 4.15f + 0.6f * headBack - 0.3f * roar, -1.75f + 0.8f * headBack - 0.3f * roar);
        float[] n3 = v3(0, 4.45f + 0.5f * headBack + 0.04f * breath - 0.4f * claw, -2.1f + 0.7f * headBack - 0.5f * roar - 0.4f * claw);
        float[][] neck = new float[7][];
        for (int i = 0; i < 7; i++) neck[i] = bezier(n0, n1, n2, n3, i / 6f);
        curve(body, neck, 0.48f, 0.28f, 8, T_SCALE);
        for (int i = 1; i < 6; i++) limb(body, add(neck[i], 0, 0.3f, 0.05f), add(neck[i], 0, 0.62f, 0.25f), 0.09f, 0f, 4, T_HORN);
        float look = 0.18f * (float) Math.sin(t * 0.011) - 0.2f;
        float headPitch = -0.1f - 0.6f * roar + 0.5f * headBack * (animT < 60 ? -1 : 1) * (anim == Anim.JUDGEMENT ? 1 : 0) + 0.3f * claw;
        Mat head = body.t(n3).ry(look).rx(headPitch).rz(0.12f * recoil).s(1.2f);
        dragonHead(head, jaw, 0, visPhase() == 2 ? 0xffa030 : 0xff2020);
    }

    // a dragon's head: skull, snout, an opening jaw, horns sweeping back, jaw spikes, glowing eyes
    private void dragonHead(Mat head, float jaw, int eyeIndex, int eyeColor) {
        eyeColors[eyeIndex] = eyeColor;
        ellipsoidZ(head.t(0, 0.08f, 0), 0.36f, 0.32f, 0.5f, 5, 9, T_SCALE, 2, 2);
        ellipsoidZ(head.t(0, 0.02f, -0.55f), 0.24f, 0.18f, 0.48f, 4, 8, T_SCALE, 2, 2);
        Mat lower = head.t(0, -0.12f, -0.15f).rx(0.55f * jaw);
        ellipsoidZ(lower.t(0, -0.04f, -0.42f), 0.21f, 0.09f, 0.45f, 3, 8, T_SCALE, 2, 1);
        if (jaw > 0.25f) ellipsoidZ(head.t(0, -0.1f, -0.45f), 0.16f, 0.06f, 0.4f, 3, 6, T_MOUTH, 1, 1);
        for (int side = -1; side <= 1; side += 2) {
            curve(head, new float[][]{v3(side * 0.2f, 0.28f, 0.1f), v3(side * 0.42f, 0.55f, 0.45f), v3(side * 0.5f, 0.75f, 0.95f), v3(side * 0.42f, 0.95f, 1.35f)}, 0.1f, 0.02f, 5, T_HORN);
            limb(head, v3(side * 0.3f, -0.05f, -0.1f), v3(side * 0.52f, -0.2f, 0.15f), 0.07f, 0f, 4, T_HORN);       // jaw spikes
            limb(lower, v3(side * 0.18f, -0.06f, -0.3f), v3(side * 0.34f, -0.18f, -0.15f), 0.05f, 0f, 4, T_HORN);
            diamond(head.t(side * 0.29f, 0.17f, -0.22f).ry(-side * 0.95f), 0.08f, 0.04f, M_EYE + eyeIndex);
        }
        diamond(head.t(-0.08f, 0.08f, -1.02f), 0.035f, 0.025f, M_VOID);
        diamond(head.t(0.08f, 0.08f, -1.02f), 0.035f, 0.025f, M_VOID);
        mouths[mouthCount++] = head.ap(0, -0.08f, -1.05f);
    }

    private final int[] eyeColors = new int[5];

    private void leg(float[] a, float[] b, float[] c, float r0, float r1, float yaw) {
        float s = bossScale;
        if (a != b) { limb(Mat.id(), a, b, r0 * 1.25f * s, r0 * s, 7, T_SCALE); ellipsoid(Mat.id().t(b), r0 * s, r0 * s, r0 * s, 3, 6, T_SCALE, 1, 1); }
        limb(Mat.id(), b, c, r0 * s, r1 * s, 6, T_SCALE);
        Mat foot = Mat.id().t(c).ry(yaw).s(s);
        ellipsoidZ(foot.t(0, -0.02f, -0.1f), 0.22f, 0.1f, 0.3f, 3, 7, T_SCALE, 1, 1);
        for (int k = -1; k <= 1; k++) limb(foot, v3(k * 0.12f, 0, -0.3f), v3(k * 0.16f, -0.08f, -0.55f), 0.05f, 0f, 4, T_CLAW);
    }

    // ---------------------------------------------------------------- form 3: the hydra

    private void buildHydra(float sinkY, float collapse, float recoil) {
        float p = animT, breath = (float) Math.sin(t * 0.04);
        // the hydra turns more toward the camera so its five heads fan out across the screen
        float hyaw = 0.6f;
        Mat root = Mat.id().t(BOSS_X - BOSS_X * 0.05f * recoil, sinkY - collapse * 1.2f, BOSS_Z + 0.6f).ry(hyaw).s(bossScale);
        float bs = 1 + 0.025f * breath;
        // the ruin: a huge low black body split by glowing red cracks, short heavy legs, a thick tail
        ellipsoidZ(root.t(0, 1.55f, 0.2f), 1.5f * bs, 1.2f * bs, 2.0f, 8, 12, T_HYDRA, 3, 3);
        ellipsoidZ(root.t(0, 2.1f, -1.0f), 1.25f, 0.95f, 1.0f, 6, 10, T_HYDRA, 2, 2);
        for (int side = -1; side <= 1; side += 2) for (int fb = -1; fb <= 1; fb += 2) {
            float[] top = root.ap(side * 1.05f, 1.2f, fb * 1.0f - 0.2f), knee = root.ap(side * 1.45f, 0.6f, fb * 1.05f - 0.3f), foot = root.ap(side * 1.4f, 0.1f, fb * 1.0f - 0.45f);
            if (collapse > 0) { knee[1] -= 0.4f * collapse; foot = lerp3(foot, add(foot, side * 0.4f, 0, 0), collapse); }
            limb(Mat.id(), top, knee, 0.5f * bossScale, 0.35f * bossScale, 7, T_HYDRA);
            leg(knee, knee, foot, 0.32f, 0.26f, hyaw);
        }
        float sw = (float) Math.sin(t * 0.025) * 0.5f;
        curve(root, new float[][]{v3(0, 1.4f, 2.0f), v3(0.3f * sw, 0.9f, 3.0f), v3(0.8f * sw + 0.3f, 0.45f, 3.9f), v3(1.3f * sw + 1.0f, 0.3f, 4.5f)}, 0.75f, 0.12f, 8, T_HYDRA);
        bodyCenter = root.ap(v3(0, 0, 0));
        bodyCenter[1] = 0;

        // five necks and heads; each head's eyes glow in the colour of its beam
        float[] knightW = knightChest();
        for (int i = 0; i < 5; i++) {
            float c = i - 2, ac = Math.abs(c);
            float swy = (float) Math.sin(t * 0.03 + i * 2.1) * 0.25f, swx = (float) Math.cos(t * 0.025 + i) * 0.2f;
            float[] base = root.ap(c * 0.48f, 2.6f, -1.55f + 0.15f * ac);
            float[] rest = root.ap(c * 1.5f + swx, 5.0f - 0.5f * ac + swy, -2.3f + 0.45f * ac);
            float rise = 0, slam = 0, bite = 0, rear = 0, jaw = 0.1f;
            switch (anim) {
                case SLAM:
                    rise = bell(p, 0, 44);
                    slam = bell(p, 34 + i * 3, 56 + i * 3);
                    break;
                case WRATH:
                    if (i >= 1 && i <= 3) { float peak = 22 + (i - 1) * 6; bite = bell(p, peak - 12, peak + 6); jaw = 0.1f + 0.9f * bell(p, peak - 12, peak - 1); }
                    else rear = 0.4f * bell(p, 0, 50);
                    break;
                case RUPTURE:
                    if (i == 0 || i == 4) { float sp = i == 0 ? 6 : 12; rear = bell(p, sp - 6, sp + 4); jaw = 0.1f + 0.9f * bell(p, sp - 4, sp + 6); }
                    break;
                case JUDGEMENT:
                    rear = bell(p, 0, 100) * 0.8f;
                    jaw = 0.1f + 0.9f * ramp(p, 24, 38) * (p < 96 ? 1 : 0);
                    break;
                default: break;
            }
            float[] head = add(rest, 0, 1.2f * rise + 0.7f * rear, 0);
            if (slam > 0) head = lerp3(head, root.ap(c * 0.9f, 0.5f, -3.4f), slam);
            if (bite > 0) head = lerp3(head, add(knightW, 0.2f, 0.4f, 0.6f), bite * 0.85f);
            if (collapse > 0) head = lerp3(head, add(base, c * 0.6f, -1.6f, -1.0f), collapse);
            float[] c1 = add(base, 0, 1.0f, -0.4f), c2 = add(lerp3(base, head, 0.6f), 0, 0.6f, 0.5f);
            float[][] neck = new float[7][];
            for (int k = 0; k < 7; k++) neck[k] = bezier(base, c1, c2, head, k / 6f);
            curve(Mat.id(), neck, 0.44f * bossScale, 0.26f * bossScale, 7, T_HYDRA);
            // face the knight (heads turn toward him; the slam aims at the ground)
            float dx = knightW[0] - head[0], dz = knightW[2] - head[2];
            float hy = (float) Math.atan2(-dx, -dz) + (c * 0.18f) * (1 - bite);
            float pitch = 0.6f * slam - 0.4f * rear + 0.2f * bite;
            dragonHead(Mat.id().t(head).ry(hy).rx(pitch).s(1.0f * bossScale), jaw, i, brighten(BEAM[i]));
        }
    }

    private static int brighten(int c) { return mulRGB(c, 2.0f) | 0x202020; }

    // ================================================================= the PS1-style rasterizer

    private void rasterBoss() {
        float[] va = new float[3];
        float lx = -0.45f, ly = 0.8f, lz = -0.4f, ll = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ll; ly /= ll; lz /= ll;
        float qx = 0.3f, qy = 0.35f, qz = 0.88f, ql = (float) Math.sqrt(qx * qx + qy * qy + qz * qz);
        qx /= ql; qy /= ql; qz /= ql;
        boolean flash = hurtT >= 0 && hurtT < 8 && (hurtT & 2) == 0;   // the Pokemon-style hit blink
        float[] lit = new float[3], rim = new float[3], sx = new float[3], sy = new float[3], iz = new float[3], uu = new float[3], vv = new float[3], wy = new float[3];
        for (int i = 0; i < tris; i++) {
            int o = i * 3 * VS, mat = tmat[i];
            boolean twoSided = mat == T_WING || mat == T_HORN;
            boolean ok = true;
            for (int k = 0; k < 3; k++) {
                int b = o + k * VS;
                view(tv[b], tv[b + 1], tv[b + 2], va);
                if (va[2] < 0.3f) { ok = false; break; }
                float d = va[2];
                sx[k] = Math.round((W / 2f + shakeX + f * va[0] / d) / S);    // snapped to whole pixels, like the PS1
                sy[k] = Math.round((horizon + shakeY - f * va[1] / d) / S);
                iz[k] = 1 / va[2];
                float nx = tv[b + 3], ny = tv[b + 4], nz = tv[b + 5];
                float dl = nx * lx + ny * ly + nz * lz;
                if (twoSided) dl = Math.abs(dl);
                lit[k] = 0.42f + 0.8f * Math.max(0f, dl);
                rim[k] = Math.max(0f, nx * qx + ny * qy + nz * qz);
                uu[k] = tv[b + 6];
                vv[k] = tv[b + 7];
                wy[k] = tv[b + 1];
            }
            if (ok) rasterTri(sx, sy, iz, lit, rim, uu, vv, wy, mat, flash);
        }
    }

    private void rasterTri(float[] x, float[] y, float[] iz, float[] lit, float[] rim, float[] u, float[] v, float[] wy, int mat, boolean flash) {
        float area = (x[1] - x[0]) * (y[2] - y[0]) - (x[2] - x[0]) * (y[1] - y[0]);
        if (Math.abs(area) < 0.01f) return;
        int minX = Math.max(0, (int) Math.floor(Math.min(x[0], Math.min(x[1], x[2])))), maxX = Math.min(LW - 1, (int) Math.ceil(Math.max(x[0], Math.max(x[1], x[2]))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y[0], Math.min(y[1], y[2])))), maxY = Math.min(LH - 1, (int) Math.ceil(Math.max(y[0], Math.max(y[1], y[2]))));
        if (minX > maxX || minY > maxY) return;
        int[] tx = mat < tex.length ? tex[mat] : null;
        boolean glowEye = anim == Anim.JUDGEMENT && animT > 20 && animT < 96;
        for (int py = minY; py <= maxY; py++) {
            float fy = py + 0.5f;
            for (int px = minX; px <= maxX; px++) {
                float fx = px + 0.5f;
                float w0 = ((x[1] - fx) * (y[2] - fy) - (x[2] - fx) * (y[1] - fy)) / area;
                float w1 = ((fx - x[0]) * (y[2] - y[0]) - (x[2] - x[0]) * (fy - y[0])) / area;
                float w2 = 1 - w0 - w1;
                if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                int idx = py * LW + px;
                float z = w0 * iz[0] + w1 * iz[1] + w2 * iz[2];
                if (z <= zbuf[idx]) continue;
                if (w0 * wy[0] + w1 * wy[1] + w2 * wy[2] < -0.02f) continue;   // below the ground (while it is in the earth)
                int col;
                if (mat == M_VOID) col = 0x080606;
                else if (mat >= M_EYE) col = glowEye ? 0xfff0d0 : eyeColors[mat - M_EYE];
                else {
                    // affine texture mapping and nearest texels: the PS1 look
                    float tu = w0 * u[0] + w1 * u[1] + w2 * u[2], tvv = w0 * v[0] + w1 * v[1] + w2 * v[2];
                    int texel = tx[(((int) Math.floor(tvv * TS)) & (TS - 1)) * TS + (((int) Math.floor(tu * TS)) & (TS - 1))];
                    if ((texel & GLOW) != 0) col = texel & 0xFFFFFF;
                    else {
                        float l = w0 * lit[0] + w1 * lit[1] + w2 * lit[2], rm = w0 * rim[0] + w1 * rim[1] + w2 * rim[2];
                        rm = rm * rm * 0.8f;
                        int r = (int) (((texel >> 16) & 255) * l + 200 * rm), g = (int) (((texel >> 8) & 255) * l + 40 * rm), b = (int) ((texel & 255) * l + 50 * rm);
                        col = (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
                    }
                }
                if (flash) col = lerpRGB(col, 0xffffff, 0.7f);
                zbuf[idx] = z;
                meshMask[idx] = true;
                lowPx[idx] = 0xFF000000 | ps1(col, px, py);
            }
        }
    }

    // 15-bit colour with 4x4 ordered dithering, like the PS1
    private static int ps1(int c, int x, int y) {
        int d = (BAYER[y & 3][x & 3] - 8) / 2;
        int r = Math.max(0, Math.min(255, ((c >> 16) & 255) + d)), g = Math.max(0, Math.min(255, ((c >> 8) & 255) + d)), b = Math.max(0, Math.min(255, (c & 255) + d));
        return ((r & 0xF8) << 16) | ((g & 0xF8) << 8) | (b & 0xF8);
    }

    // form 1: a dark red haze around the black dragon so it stands out (as on the title screen)
    // form 2: a flickering fire outline hugging the dragon and a wider dark purple aura around it
    private void fireAura(boolean burning) {
        final int INF = 99;
        for (int i = 0; i < dist.length; i++) dist[i] = meshMask[i] ? 0 : INF;
        for (int y = 0; y < LH; y++) for (int x = 0; x < LW; x++) {   // two-pass distance to the silhouette
            int i = y * LW + x;
            if (x > 0) dist[i] = Math.min(dist[i], dist[i - 1] + 1);
            if (y > 0) dist[i] = Math.min(dist[i], dist[i - LW] + 1);
        }
        for (int y = LH - 1; y >= 0; y--) for (int x = LW - 1; x >= 0; x--) {
            int i = y * LW + x;
            if (x < LW - 1) dist[i] = Math.min(dist[i], dist[i + 1] + 1);
            if (y < LH - 1) dist[i] = Math.min(dist[i], dist[i + LW] + 1);
        }
        int step = t / 4;
        if (!burning) {
            for (int i = 0; i < dist.length; i++)
                if (!meshMask[i] && dist[i] <= 6) lowPx[i] = ((int) (120 - dist[i] * 18 + 15 * Math.sin(t * 0.04)) << 24) | 0x5a0808;
            return;
        }
        for (int y = 0; y < LH; y++) for (int x = 0; x < LW; x++) {
            int i = y * LW + x;
            if (meshMask[i]) continue;
            // flames lick upward: look a few pixels below for the silhouette
            int below = Math.min(LH - 1, y + 1 + (int) (hash(x / 2, step, 9) * 3));
            int d = Math.min(dist[i], dist[below * LW + x] + 1);
            float n = hash(x, y + step * 3, 11);
            if (d <= 1 || (d <= 3 && n > 0.35f)) lowPx[i] = 0xFF000000 | (d <= 1 ? (n > 0.5f ? 0xffe050 : 0xff8a10) : 0xd02a00);
            else if (dist[i] <= 9) lowPx[i] = ((int) (215 - dist[i] * 19 + 25 * Math.sin(t * 0.05 + dist[i] * 0.7)) << 24) | (dist[i] < 5 ? 0x6a00a0 : 0x4b0073);
        }
    }

    // ================================================================= effects

    // effects behind the knight: shockwaves, dust, rocks, smoke, burning cracks, embers, roar rings
    private void drawSmallFx() {
        float[] p = new float[3], q = new float[3];
        for (Fx e : fx) {
            if (e.t < 0) continue;
            float k = e.t / (float) e.dur;
            switch (e.kind) {
                case FX_SHOCK:
                    for (int ring = 0; ring < 2; ring++) {
                        float r = 0.4f + k * 7f - ring * 0.8f;
                        if (r <= 0) continue;
                        subjG.setColor(ring == 0 ? new Color(255, 214, 140, (int) (230 * (1 - k))) : new Color(200, 120, 60, (int) (200 * (1 - k))));
                        for (int i = 0; i < 64; i++) {
                            double a = i * Math.PI * 2 / 64;
                            if (project(e.a[0] + (float) Math.cos(a) * r, 0.05f, e.a[2] + (float) Math.sin(a) * r * 0.85f, p)) block(p[0], p[1], 4);
                        }
                    }
                    break;
                case FX_DUST:
                    subjG.setColor(new Color(120, 92, 70, (int) (180 * (1 - k))));
                    for (int i = 0; i < 26; i++) {
                        double a = hash(i, 1, 2) * Math.PI * 2;
                        float r = 0.5f + k * (1.5f + hash(i, 2, 3) * 2.5f), h = (float) Math.sin(Math.PI * Math.min(1f, k * 1.4f)) * (0.3f + hash(i, 4, 5) * 1.2f);
                        if (project(e.a[0] + (float) Math.cos(a) * r, h, e.a[2] + (float) Math.sin(a) * r * 0.8f, p)) block(p[0], p[1], 4);
                    }
                    break;
                case FX_SMOKE:
                    subjG.setColor(new Color(90, 84, 92, (int) (150 * (1 - k))));
                    for (int i = 0; i < 5; i++) {
                        float d = k * 1.3f + i * 0.08f;
                        if (project(e.a[0] - 0.4f * d, e.a[1] + 0.5f * d, e.a[2] - 0.2f * d, p)) block(p[0] + (i % 2) * 2, p[1], 3 + (int) (k * 5));
                    }
                    break;
                case FX_CRACK: {   // a burning crack racing along the ground to the knight
                    float reach = Math.min(1f, k * 1.6f);
                    subjG.setColor(new Color(255, 120, 30, (int) (255 * (1 - Math.max(0, k - 0.6f) * 2.5f))));
                    float[] prev = null;
                    for (int s = 0; s <= 20; s++) {
                        float kk = s / 20f * reach;
                        float[] w = lerp3(e.a, e.b, kk);
                        w[0] += (hash(s, 0, 31) - 0.5f) * 0.5f;
                        if (project(w, p)) { if (prev != null) pixelLine(subjG, prev[0], prev[1], p[0], p[1], 3); prev = new float[]{p[0], p[1]}; }
                    }
                    break;
                }
                case FX_ROAR:   // expanding sound rings from the mouth (like Roar)
                    if (project(e.a, p)) {
                        int r = (int) (10 + k * 120);
                        subjG.setColor(new Color(255, 255, 255, (int) (200 * (1 - k))));
                        subjG.setStroke(new BasicStroke(4));
                        subjG.drawArc((int) p[0] - r, (int) p[1] - r, r * 2, r * 2, 120, 120);
                        subjG.setStroke(new BasicStroke(1));
                    }
                    break;
                default: break;
            }
        }
        int phase = visPhase();
        if (phase >= 2 && sinkDepth() < 0.5f) {   // embers rising off the burning dragon, sparks off the cracked ruin
            subjG.setColor(phase == 2 ? new Color(255, 140, 40) : new Color(230, 40, 30));
            for (int i = 0; i < 30; i++) {
                float h = (float) ((hash(i, 0, 61) * 5 + t * (0.012 + hash(i, 1, 61) * 0.01)) % 5.0);
                float x = bodyCenter[0] + (hash(i, 2, 61) - 0.5f) * 4f + (float) Math.sin(t * 0.03 + i) * 0.2f, z = bodyCenter[2] + (hash(i, 3, 61) - 0.5f) * 3f;
                if (project(x, 1.2f + h, z, p)) block(p[0], p[1], 2);
            }
        }
    }

    // effects in front of the knight: eruptions, rocks, the charge-up and the beams, slashes, bites, impact stars, projectiles
    private void drawBigFx() {
        float[] p = new float[3], q = new float[3];
        for (Fx e : fx) {
            if (e.t < 0) continue;
            float k = e.t / (float) e.dur;
            switch (e.kind) {
                case FX_ERUPT: {
                    float rise = Math.min(1f, k * 3f), fade = 1 - Math.max(0f, (k - 0.5f) * 2f);
                    subjG.setColor(new Color(40, 12, 8, (int) (200 * fade)));
                    if (project(e.a, p)) subjG.fillOval((int) p[0] - 55, (int) p[1] - 9, 110, 18);
                    for (int i = 0; i < 80; i++) {
                        float hh = hash(i, 0, 71 + e.t / 3) * 3.6f * rise, r = 0.55f * (1 - hh / 3.8f) * (0.4f + hash(i, 1, 71) * 0.8f);
                        double a = hash(i, 2, 71 + e.t / 2) * Math.PI * 2;
                        if (!project(e.a[0] + (float) Math.cos(a) * r, hh, e.a[2] + (float) Math.sin(a) * r, p)) continue;
                        float heat = hh / 3.6f;
                        int c = heat < 0.3f ? 0xffec8c : (heat < 0.65f ? 0xff8c28 : 0xc83218);
                        subjG.setColor(rgba(c, (int) (255 * fade)));
                        block(p[0], p[1], 4 + (int) (5 * (1 - heat)));
                    }
                    break;
                }
                case FX_ROCKS:   // rocks burst out of the ground and fall back (like Earthquake / Rock Slide)
                    for (int i = 0; i < 12; i++) {
                        float ang = hash(i, 0, 81) * 6.28f, r = 0.4f + hash(i, 1, 81) * 1.2f, vy = 5 + hash(i, 2, 81) * 4;
                        float tt = k * 1.6f, h = Math.max(0, vy * tt - 9 * tt * tt);
                        if (!project(e.a[0] + (float) Math.cos(ang) * r * (1 + k), h, e.a[2] + (float) Math.sin(ang) * r * 0.6f, p)) continue;
                        int s = 8 + (i % 3) * 4;
                        subjG.setColor(new Color(74, 56, 42));
                        subjG.fillRect(((int) p[0] - s / 2) & ~1, ((int) p[1] - s / 2) & ~1, s, s);
                        subjG.setColor(new Color(120, 96, 70));
                        subjG.fillRect(((int) p[0] - s / 2) & ~1, ((int) p[1] - s / 2) & ~1, s, 2);
                    }
                    break;
                case FX_SLASH:   // three claw streaks across the target (like Slash)
                    if (project(e.a, p)) {
                        float a = 1 - Math.max(0, k - 0.5f) * 2;
                        for (int s = 0; s < 3; s++) {
                            float reach = Math.min(1f, k * 3f - s * 0.25f);
                            if (reach <= 0) continue;
                            int ox = (int) p[0] - 50 + s * 28, oy = (int) p[1] - 60;
                            subjG.setColor(rgba(e.color, (int) (255 * a)));
                            pixelLine(subjG, ox, oy, ox + 60 * reach, oy + 110 * reach, 6);
                            subjG.setColor(rgba(0xffffff, (int) (255 * a)));
                            pixelLine(subjG, ox + 2, oy, ox + 2 + 60 * reach, oy + 110 * reach, 2);
                        }
                    }
                    break;
                case FX_BITE:   // two rows of fangs snapping shut on the target (like Bite / Crunch)
                    if (project(e.a, p)) {
                        float close = Math.min(1f, k * 2.5f), a = 1 - Math.max(0, k - 0.6f) * 2.5f;
                        int gap = (int) (70 * (1 - close)), cx = (int) p[0], cy = (int) p[1];
                        subjG.setColor(rgba(0xffffff, (int) (255 * a)));
                        for (int s = -2; s <= 2; s++) {
                            int fx0 = cx + s * 22;
                            subjG.fillPolygon(new int[]{fx0 - 10, fx0 + 10, fx0}, new int[]{cy - gap - 40, cy - gap - 40, cy - gap - 4}, 3);
                            subjG.fillPolygon(new int[]{fx0 - 10, fx0 + 10, fx0}, new int[]{cy + gap + 40, cy + gap + 40, cy + gap + 4}, 3);
                        }
                    }
                    break;
                case FX_STAR:   // the impact star on whoever was hit
                    if (project(e.a, p)) {
                        float s = 18 + 34 * easeOut(Math.min(1f, k * 2.5f)), a = 1 - k;
                        star(p[0], p[1], s, s * 0.4f, rgba(0xfff4a0, (int) (255 * a)));
                        star(p[0], p[1], s * 0.55f, s * 0.22f, rgba(0xffffff, (int) (255 * a)));
                    }
                    break;
                case FX_FIREBALL: {   // the hydra's fireballs arc from a mouth to the knight
                    float kk = Math.min(1f, e.t / (float) Math.max(1, e.dur));
                    float arc = 1.5f * (float) Math.sin(Math.PI * kk);
                    for (int s = 0; s < 6; s++) {
                        float[] tr = add(lerp3(e.a, e.b, Math.max(0, kk - s * 0.03f)), 0, arc, 0);
                        if (project(tr, q)) { subjG.setColor(rgba(s < 2 ? 0xffe070 : 0xff6a1a, 255 - s * 35)); block(q[0], q[1], 26 - s * 3); }
                    }
                    break;
                }
                default: break;
            }
        }
        drawJudgement(p, q);
    }

    // Judgement of Dabbah: energy gathers into an orb at each mouth, then a beam (five coloured beams for the hydra)
    private void drawJudgement(float[] p, float[] q) {
        if (anim != Anim.JUDGEMENT || mouthCount == 0) return;
        float a = animT;
        float[] kc = knightChest();
        for (int i = 0; i < mouthCount; i++) {
            int col = hydra() ? BEAM[i] : DRAGON_BEAM;
            if (!project(mouths[i], p)) continue;
            if (a >= 24 && a < 66) {   // charging: particles stream in, the orb grows
                float k = ramp(a, 24, 62);
                for (int s = 0; s < 10; s++) {
                    float ang = hash(s, i, 7) * 6.28f, d = 90 * (1 - ((a * 0.06f + hash(s, i, 8)) % 1f));
                    subjG.setColor(rgba(brighten(col), 220));
                    block(p[0] + (float) Math.cos(ang) * d, p[1] + (float) Math.sin(ang) * d, 4);
                }
                int r = (int) (4 + 16 * k * (hydra() ? 0.7f : 1f) + 2 * Math.sin(a * 0.8));
                subjG.setColor(rgba(col, 230));
                subjG.fillOval((int) p[0] - r, (int) p[1] - r, r * 2, r * 2);
                subjG.setColor(rgba(0x101010, 255));
                subjG.fillOval((int) p[0] - r / 2, (int) p[1] - r / 2, r, r);   // a dark core, like the title screen's beams
            }
            if (a >= 62 && a < 96 && project(kc, q)) {   // the beam
                float reach = Math.min(1f, (a - 62) / 8f), fade = a > 86 ? 1 - (a - 86) / 10f : 1;
                float ex = p[0] + (q[0] - p[0]) * reach, ey = p[1] + (q[1] - p[1]) * reach;
                int wdt = (int) ((hydra() ? 14 : 22) * (0.85f + 0.15f * Math.sin(a * 0.9)));
                subjG.setStroke(new BasicStroke(wdt, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                subjG.setColor(rgba(col, (int) (170 * fade)));
                subjG.drawLine((int) p[0], (int) p[1], (int) ex, (int) ey);
                subjG.setStroke(new BasicStroke(wdt * 0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                subjG.setColor(rgba(brighten(col), (int) (255 * fade)));
                subjG.drawLine((int) p[0], (int) p[1], (int) ex, (int) ey);
                subjG.setStroke(new BasicStroke(Math.max(2, wdt * 0.18f)));
                subjG.setColor(rgba(0x0a0a0a, (int) (255 * fade)));
                subjG.drawLine((int) p[0], (int) p[1], (int) ex, (int) ey);
                subjG.setStroke(new BasicStroke(1));
                if (reach >= 1) star(q[0], q[1], 40 + 8 * (float) Math.sin(a), 16, rgba(brighten(col), (int) (230 * fade)));
            }
        }
    }

    private void star(float cx, float cy, float outer, float inner, Color c) {
        int n = 16;
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI / 8 + 0.2;
            float r = (i % 2 == 0) ? outer : inner;
            xs[i] = (int) (cx + Math.cos(a) * r);
            ys[i] = (int) (cy + Math.sin(a) * r);
        }
        subjG.setColor(c);
        subjG.fillPolygon(xs, ys, n);
    }

    private void block(float x, float y, int size) {
        subjG.fillRect(((int) x - size / 2) & ~1, ((int) y - size / 2) & ~1, size, size);
    }

    private static Color rgba(int rgb, int a) {
        return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.max(0, Math.min(255, a)));
    }

    // ================================================================= textures (32x32, made in code)

    private interface TexFn { int at(int x, int y, float n); }

    private int[] makeTex(int seed, TexFn fn) {
        int[] tx = new int[TS * TS];
        for (int y = 0; y < TS; y++) for (int x = 0; x < TS; x++) tx[y * TS + x] = fn.at(x, y, (float) (0.6 * vn(x * 0.25, y * 0.25, seed) + 0.4 * hash(x, y, seed)));
        return tx;
    }

    private void buildTextures() {
        // pitch-black scales: rows of overlapping rounded scales, each with a faint sheen along its top edge
        tex[T_SCALE] = makeTex(11, (x, y, n) -> {
            int row = y / 6, sx = (x + (row & 1) * 4) % 8, sy = y % 6;
            double d = Math.hypot(sx - 3.5, sy * 1.2);
            if (d > 4.2) return 0x100e14;
            if (sy == 0 || d > 3.6) return 0x343040;
            return lerpRGB(0x16141c, 0x2c2836, n);
        });
        tex[T_BELLY] = makeTex(21, (x, y, n) -> y % 6 == 0 ? 0x120e10 : lerpRGB(0x2a2024, 0x463840, n));
        tex[T_HORN] = makeTex(31, (x, y, n) -> lerpRGB(0x4a443e, 0x9a9284, (float) (n * 0.5 + 0.5 * hash(x, 0, 32))));
        tex[T_WING] = makeTex(41, (x, y, n) -> (x % 10 == 0 || (x + y) % 23 == 0) ? 0x14080c : lerpRGB(0x2a0c14, 0x521a26, n));
        tex[T_HYDRA] = makeTex(51, (x, y, n) -> {   // black hide split by glowing red cracks
            double c = Math.abs(vn(x * 0.21, y * 0.21, 52) - 0.5);
            if (c < 0.016) return GLOW | 0xff5a2a;
            if (c < 0.03) return GLOW | 0xb8160e;
            return lerpRGB(0x121014, 0x262228, n);
        });
        tex[T_MOUTH] = makeTex(61, (x, y, n) -> lerpRGB(0x4a0a0e, 0x8a1c1a, n));
        tex[T_CLAW] = makeTex(71, (x, y, n) -> lerpRGB(0x1a1816, 0x5a5450, n));
    }

    // ================================================================= helpers

    private static float hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    private static double vn(double x, double y, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash(xi, yi, seed), b = hash(xi + 1, yi, seed), c = hash(xi, yi + 1, seed), d = hash(xi + 1, yi + 1, seed);
        double top = a + (b - a) * fx, bot = c + (d - c) * fx;
        return top + (bot - top) * fy;
    }

    private static final int[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};

    private static int lerpRGB(int a, int b, float k) {
        k = k < 0 ? 0 : (k > 1 ? 1 : k);
        int r = (int) (((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k), g = (int) (((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }

    private static int mulRGB(int c, float f) {
        int r = (int) Math.min(255, ((c >> 16) & 255) * f), g = (int) Math.min(255, ((c >> 8) & 255) * f), b = (int) Math.min(255, (c & 255) * f);
        return (r << 16) | (g << 8) | b;
    }

    private static float clamp01f(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static float clampf(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }
    private static float easeOut(float p) { p = clamp01f(p); return 1 - (1 - p) * (1 - p) * (1 - p); }
    private static float easeIn(float p) { p = clamp01f(p); return p * p * p; }
    private static float smooth(float p) { p = clamp01f(p); return p * p * (3 - 2 * p); }
}