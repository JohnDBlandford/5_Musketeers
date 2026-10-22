package Scripts.World1;

import Level.Script;
import ScriptActions.LockPlayerScriptAction;
import ScriptActions.ScriptAction;
import ScriptActions.TextboxScriptAction;
import ScriptActions.UnlockPlayerScriptAction;

import java.util.ArrayList;

public class BarrowsScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("Welcome to Barrows!");
            addText("Old burial mounds north of the Cloister.");
            addText("The valley buried its dead here for generations\nbefore the Reckoning.");
            addText("Reckoners now camp among the graves.");
        }});
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}