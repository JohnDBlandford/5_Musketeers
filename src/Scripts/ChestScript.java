package Scripts;

import Chest.ChestId;
import Level.Script;
import ScriptActions.*;

import java.util.ArrayList;
import java.util.Objects;

// The interact script for a chest: lock the player, open the chest and show what was found, then unlock. Unlike the shops there
// IS an unlock here, because nothing stays open after the textbox is dismissed.
// Attach it with  tile.setInteractScript(new ChestScript(chestId)).
public class ChestScript extends Script {
    private final ChestId chestId;

    public ChestScript(ChestId id) {
        this.chestId = Objects.requireNonNull(id, "id not found");
    }

    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> actions = new ArrayList<>();
        actions.add(new LockPlayerScriptAction());
        actions.add(new OpenChestScriptAction(chestId));
        actions.add(new UnlockPlayerScriptAction());
        return actions;
    }
}
