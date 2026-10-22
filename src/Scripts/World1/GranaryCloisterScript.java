package Scripts.World1;

import Level.Script;
import ScriptActions.LockPlayerScriptAction;
import ScriptActions.ScriptAction;
import ScriptActions.TextboxScriptAction;
import ScriptActions.UnlockPlayerScriptAction;

import java.util.ArrayList;

public class GranaryCloisterScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("Welcome to Granary Cloister!");
            addText("A walled granary that became a monastery.");
            addText("It was built to store the valley’s harvest.\nThe Hollow Chorus took it over.");
            addText("Its cellars hold seed grain saved for the last winter.");
        }});
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}

