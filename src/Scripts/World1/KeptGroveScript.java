package Scripts.World1;

import Level.Script;
import ScriptActions.*;

import java.util.ArrayList;

public class KeptGroveScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("The Kept Grove");
            addText("Violence repeats the fear-driven crime that created it."); //Belief
            addText("Let the fields grow wild,give back what was taken,\nand remember the day the shepherd died."); //Method
            addText("Few in number, slow to act, and dismissed\nas dreamers by Concord and Chorus alike."); //Flaw
        }});
        scriptActions.add(new ChangeFlagScriptAction("sawKeptGrove", true)); // makes it play only once
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}