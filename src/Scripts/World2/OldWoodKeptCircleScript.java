package Scripts.World2;

import Level.Script;
import ScriptActions.LockPlayerScriptAction;
import ScriptActions.ScriptAction;
import ScriptActions.TextboxScriptAction;
import ScriptActions.UnlockPlayerScriptAction;

import java.util.ArrayList;

public class OldWoodKeptCircleScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("Welcome to Old Wood & Kept Circle!");
            addText("The valley’s oldest forest, and a ring of druid huts\nwithin it.");
            addText("Here the old gods and the First Beasts once kept the\ncompact with humankind.");
            addText("As their neighbors forget the compact, the druids begin\ncalling themselves the Kept.");
        }});
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}

