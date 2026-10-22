package Scripts.World1;

import Level.Script;
import ScriptActions.*;

import java.util.ArrayList;

public class ConcordScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("Welcome to The Concord!");
            addText("The Beast is a curse, to be ended by force,\nnot understood."); //Belief
            addText("Holy war, Crusading armies, Beast-slayer\nknights, and martyrs made saints."); //Method
            addText("Each crusade only made the Beast burn\nhotter. Famine became fire."); //Flaw
        }});
        scriptActions.add(new ChangeFlagScriptAction("sawConcord", true)); // makes it play only once
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}

