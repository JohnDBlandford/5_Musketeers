package Scripts.World1;

import Level.Script;
import ScriptActions.LockPlayerScriptAction;
import ScriptActions.ScriptAction;
import ScriptActions.TextboxScriptAction;
import ScriptActions.UnlockPlayerScriptAction;

import java.util.ArrayList;

public class DryRiverScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("Welcome to Dry River!");
            addText("A riverbed, cracked white from bank to bank");
            addText("It watered the valley’s fields until the First Reckoning.");
            addText("The Beast dried it to give back what humanity had\ntaken.");
        }});
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}


