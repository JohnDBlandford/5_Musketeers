package Scripts.World2;

import Level.Script;
import ScriptActions.LockPlayerScriptAction;
import ScriptActions.ScriptAction;
import ScriptActions.TextboxScriptAction;
import ScriptActions.UnlockPlayerScriptAction;

import java.util.ArrayList;

public class MarshDrainageScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("Welcome to Marsh & Drainage!");
            addText("A wide marsh, cut through with drainage channels and\nnew wheat.");
            addText("Reeds, birds, and river-wardens lived here in the Age\nof Balance.");
            addText("Now it is drained at scale to make room for more fields.");
        }});
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}

