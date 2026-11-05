package Story;

import Level.GameListener;
import Level.Script;
import Level.ScriptState;
import ScriptActions.LockPlayerScriptAction;
import ScriptActions.NPCFacePlayerScriptAction;
import ScriptActions.ScriptAction;

import java.util.ArrayList;

// STORY: what happens when the knight presses ENTER next to a story character: the player is locked, the character turns
// to face him, and the conversation opens in the story's dialogue box (PlayLevelScreen, through StoryHost).
// There is deliberately NO UnlockPlayerScriptAction here (same as the shop scripts): the dialogue box frees the player
// when the conversation ends.
public class TalkScript extends Script {
    private final String storyId;

    public TalkScript(String storyId) {
        this.storyId = storyId;
    }

    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> actions = new ArrayList<>();
        actions.add(new LockPlayerScriptAction());
        if (entity instanceof Level.NPC && !isObject()) actions.add(new NPCFacePlayerScriptAction());
        actions.add(new OpenDialogue(storyId));
        return actions;
    }

    // objects (graves, the wall, the mark...) don't turn around
    private boolean isObject() {
        return entity instanceof StoryNPC && ((StoryNPC) entity).look.ordinal() >= NPCArt.Look.GRAVE.ordinal();
    }

    // tells PlayLevelScreen (a map listener that is a StoryHost) to open this character's conversation
    private static final class OpenDialogue extends ScriptAction {
        private final String storyId;

        OpenDialogue(String storyId) { this.storyId = storyId; }

        @Override
        public ScriptState execute() {
            for (GameListener l : map.getListeners())
                if (l instanceof StoryHost) ((StoryHost) l).openStoryDialogue(storyId);
            return ScriptState.COMPLETED;
        }
    }
}