package Scripts;

import java.util.ArrayList;

import GameObject.Portal;
import Level.Script;
import ScriptActions.*;

// Teleports the player immediately when they step on a door. No prompt.
public class DoorScript extends Script {
    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        Portal door = (Portal) getEntity();
        ArrayList<ScriptAction> actions = new ArrayList<>();
        actions.add(new LockPlayerScriptAction());
        actions.add(new ChangeMapScriptAction(door.getTargetMap(), door.getTargetX(), door.getTargetY()));
        actions.add(new UnlockPlayerScriptAction());
        return actions;
    }
}