package NPCs.World4;

import BattleSystem.BattleEnemy;
import BattleSystem.EnemyMoves.Move;
import Level.Player;
import NPCs.Enemy;
import Utils.Point;

import java.util.Random;

// WORLD 4: the thing that watches. It joins 1 World 4 fight in 8 as a SECOND enemy (a double battle).
//   - it never does damage; its four moves only torment (the battle screen, PlayBattleScreen, carries them out)
//   - its name is unreadable symbols that change every few frames (glitchName), and its level always shows as "???"
//     (corrupt() eats letters out of any text: the CODEX OF BEASTS uses it for everything written about it)
//     (really it is the same level as the enemy it came with)
//   - its moves are shown as binary; each one spells the move's real name in ASCII:
//       VEIL OF STATIC   hides the knight's HP bar under static for the rest of the fight
//       GRIEVANCE        all the knight's stats -50% for 4 turns, with flashbacks of despair; usable once every 4 turns
//       SEVERANCE        tears one of the knight's moves out for the rest of the fight (it becomes a glitched box)
//       BLOOD TITHE      its ally's ATK +999%, but the ally drops to 1 HP: outspeed it and finish it, or be one-shot
//   - it can be killed (it has HP like anything else), and the fight is only won once BOTH enemies are down
// In battle it MIRRORS the enemy it came with (same level, HP, ATK, DEF, SPD, XP, gold: PlayBattleScreen copies them).
// EnemyFactory.glitch(level) still builds it with Balance's GLITCH numbers first; the battle replaces them.
public class GlitchEntity extends Enemy implements BattleEnemy {

    // the hidden name: used for the CODEX OF BEASTS's "seen" list and to recognise it. Never shown; glitchName() is shown instead.
    public static final String ID = "GLITCH ENTITY";

    public enum GlitchMove {
        VEIL_OF_STATIC("VEIL OF STATIC"),
        GRIEVANCE("GRIEVANCE"),
        SEVERANCE("SEVERANCE"),
        BLOOD_TITHE("BLOOD TITHE");

        public final String realName;

        GlitchMove(String realName) { this.realName = realName; }

        // the name as it is shown: 8-bit ASCII binary, one byte per letter ("GRIEVANCE" -> "01000111 01010010 ...")
        public String binary() { return toBinary(realName); }
    }

    public GlitchEntity(int id, Point location) {
        // placeholder numbers; EnemyFactory.glitch(level) always scales it through Balance. spawnRate 0: never a normal spawn.
        super(id, location, ID, 1000f, 1f, 50f, 50f, 0, 0, 0f);
    }

    // ---------------------------------------------------------------- the unreadable name

    private static final String NOISE = "#@%&?!$*/\\|<>=+~^0123456789XZQ";

    // a different jumble every few frames; the same frame always gives the same jumble, so every screen agrees
    public static String glitchName(int frame, int length) {
        Random r = new Random(frame / 4 * 31L + 7);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append(NOISE.charAt(r.nextInt(NOISE.length())));
        return sb.toString();
    }

    // the text with some of its letters eaten by noise; which letters changes every few frames.
    // amount 0 = untouched, 1 = all noise. Spaces stay, so the words keep their shape.
    public static String corrupt(String text, int frame, double amount) {
        StringBuilder sb = new StringBuilder(text.length());
        Random r = new Random(frame / 6 * 131L + text.length());
        for (char c : text.toCharArray()) {
            boolean eat = c != ' ' && r.nextDouble() < amount;
            sb.append(eat ? NOISE.charAt(r.nextInt(NOISE.length())) : c);
        }
        return sb.toString();
    }

    public static String toBinary(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (sb.length() > 0) sb.append(' ');
            String bits = Integer.toBinaryString(c & 0xFF);
            sb.append("00000000".substring(bits.length())).append(bits);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- BattleEnemy
    // The battle screen picks and performs its moves itself (they aren't damage moves). This only exists so the bestiary,
    // which asks every enemy for its moves, lists the four binary names.
    private int listed;

    @Override
    public Move chooseBattleMove(Random rng, Player player) {
        GlitchMove m = GlitchMove.values()[listed++ % GlitchMove.values().length];
        return Move.status(m.binary());
    }

    @Override
    public int battleLevel() { return scaledLevel; }
}