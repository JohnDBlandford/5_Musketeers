package Scripts.World1;

import Level.Script;
import ScriptActions.*;

import java.util.ArrayList;

public class BarrenFieldsScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("Welcome to Barren Fields!");
            addText("Brackenford’s farmland, south of the town.");
            addText("Years of over-farming wore the soil\nthin long before the Beast came.");
            addText("When the Reckoning began, the last\ncrops failed and never returned.");
        }});
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}

