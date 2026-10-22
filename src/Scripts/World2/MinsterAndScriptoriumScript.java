package Scripts.World2;

import Level.Script;
import ScriptActions.LockPlayerScriptAction;
import ScriptActions.ScriptAction;
import ScriptActions.TextboxScriptAction;
import ScriptActions.UnlockPlayerScriptAction;

import java.util.ArrayList;

public class MinsterAndScriptoriumScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("Welcome to Minster & Scriptorium!");
            addText("A stone church with a hall for copying books.");
            addText("The kingdoms’ religious authorities write their\nshared doctrine here.");
            addText("They keep records, not swords. In time, this body will\nbecome the Concord.");
        }});
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}
