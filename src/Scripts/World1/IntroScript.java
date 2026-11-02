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
            addText("Use WASD or ARROW Keys to move around and\npress ENTER to interact with signs and to continue.");
            addText("Use I to open Inventory and ESC Key to close\nInventory.");
            addText("Pressing P will pause/unpause the game.\nThe pause menu has a list of controls.");
        }});
        scriptActions.add(new ChangeFlagScriptAction("sawIntro", true)); // makes it play only once
        scriptActions.add(new UnlockPlayerScriptAction());
        return scriptActions;
    }
}