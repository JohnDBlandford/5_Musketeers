package Story;

import java.util.ArrayList;
import java.util.List;

// STORY: one conversation. A list of lines said one after another; a line can end in CHOICES, and picking one runs its
// effect and then plays that choice's own lines before the conversation goes on (or ends).
//
//   new Dialogue("HESTER", NPCArt.Look.WIDOW)
//       .say("Is that bread? For me?")
//       .choose("GIVE THE WHOLE RATION", () -> ..., d -> d.say("Bless you."))
//              ("GIVE HALF",            () -> ..., d -> d.say("Half is more than I had."))
//
// A line may also run an action the moment it appears (doThen) or be a GLITCH line: the box flickers with static and
// the letters break up (the first frame of the static that grows through the game).
public final class Dialogue {
    public static final class Line {
        final String speaker, text;
        final NPCArt.Look look;
        final boolean glitch;
        final Runnable onShow;
        final List<Choice> choices = new ArrayList<>();

        Line(String speaker, NPCArt.Look look, String text, boolean glitch, Runnable onShow) {
            this.speaker = speaker;
            this.look = look;
            this.text = text;
            this.glitch = glitch;
            this.onShow = onShow;
        }
    }

    public static final class Choice {
        final String label;
        final Runnable effect;
        final List<Line> then;

        Choice(String label, Runnable effect, List<Line> then) {
            this.label = label;
            this.effect = effect;
            this.then = then;
        }
    }

    // builds the lines that follow a choice
    public interface Branch { void build(Dialogue d); }

    final List<Line> lines = new ArrayList<>();
    private String speaker;
    private NPCArt.Look look;
    Runnable onEnd;   // runs when the conversation closes

    public Dialogue(String speaker, NPCArt.Look look) {
        this.speaker = speaker;
        this.look = look;
    }

    // who says the next lines (the knight's own lines use KNIGHT / null look)
    public Dialogue as(String speaker, NPCArt.Look look) {
        this.speaker = speaker;
        this.look = look;
        return this;
    }

    public Dialogue say(String text) { lines.add(new Line(speaker, look, text, false, null)); return this; }

    public Dialogue sayThen(String text, Runnable onShow) { lines.add(new Line(speaker, look, text, false, onShow)); return this; }

    public Dialogue glitch(String text) { lines.add(new Line(speaker, look, text, true, null)); return this; }

    // a line said by the narrator (no portrait)
    public Dialogue narrate(String text) { lines.add(new Line("", null, text, false, null)); return this; }

    public Dialogue narrateThen(String text, Runnable onShow) { lines.add(new Line("", null, text, false, onShow)); return this; }

    // a silent action between lines
    public Dialogue run(Runnable r) { lines.add(new Line(null, null, null, false, r)); return this; }

    // the choices hang on the LAST line added
    public Dialogue choose(String label, Runnable effect, Branch then) {
        Line last = lines.get(lines.size() - 1);
        Dialogue sub = new Dialogue(speaker, look);
        if (then != null) then.build(sub);
        last.choices.add(new Choice(label, effect, sub.lines));
        return this;
    }

    public Dialogue onEnd(Runnable r) { this.onEnd = r; return this; }
}