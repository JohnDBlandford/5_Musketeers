package Scripts.World1;

import Level.Script;
import ScriptActions.*;

import java.util.ArrayList;

public class IntroScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> scriptActions = new ArrayList<>();
        scriptActions.add(new LockPlayerScriptAction());
        scriptActions.add(new TextboxScriptAction() {{
            addText("Use WASD to move around and press ENTER to\ninteract with signs and to continue.");
        }});
        scriptActions.add(new ChangeFlagScriptAction("sawIntro", true)); // makes it play only once
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}