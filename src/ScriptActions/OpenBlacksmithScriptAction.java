package ScriptActions;

import Level.ScriptState;
import Shop.Blacksmith.BlacksmithOpener;

import java.util.List;
import java.util.Objects;

// A script action that asks the screen to open the blacksmith. The screen is found among the map's listeners (anything that
// implements BlacksmithOpener, i.e. PlayLevelScreen), so GameListener does not need a new method. Finishes immediately.
// Only BlacksmithOpeners are told: the merchant has its own OpenShopScriptAction.
public class OpenBlacksmithScriptAction extends ScriptAction {

    @Override
    public ScriptState execute() {
        if (notifyOpeners(map.getListeners()) == 0) {
            System.err.println("OpenBlacksmithScriptAction: no listener implements BlacksmithOpener, so the blacksmith was not opened.");
            // BlacksmithBuildingScript has no unlock of its own (the shop controller's onClosed does it), so free the player here or they stay locked forever
            if (player != null) player.unlock();
        }
        return ScriptState.COMPLETED;
    }

    // tells every BlacksmithOpener in the list to open the blacksmith and returns how many were told; anything else is skipped
    public static int notifyOpeners(List<?> listeners) {
        Objects.requireNonNull(listeners, "listeners");
        int told = 0;
        for (Object listener : listeners) {
            if (listener instanceof BlacksmithOpener opener) {
                opener.onOpenBlacksmith();
                told++;
            }
        }
        return told;
    }
}
