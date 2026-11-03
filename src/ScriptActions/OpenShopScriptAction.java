package ScriptActions;

import Level.ScriptState;
import Shop.ShopOpener;

import java.util.List;
import java.util.Objects;

// A script action that asks the screen to open the shop. The screen is found among the map's listeners (anything that
// implements ShopOpener, i.e. PlayLevelScreen), so GameListener does not need a new method. Finishes immediately.
public class OpenShopScriptAction extends ScriptAction {

    @Override
    public ScriptState execute() {
        if (notifyOpeners(map.getListeners()) == 0) {
            System.err.println("OpenShopScriptAction: no listener implements ShopOpener, so the shop was not opened.");
            // ShopBuildingScript has no unlock of its own (the shop's onClosed does it), so free the player here or they stay locked forever
            if (player != null) player.unlock();
        }
        return ScriptState.COMPLETED;
    }

    // tells every ShopOpener in the list to open the shop and returns how many were told; anything else is skipped
    public static int notifyOpeners(List<?> listeners) {
        Objects.requireNonNull(listeners, "listeners");
        int told = 0;
        for (Object listener : listeners) {
            if (listener instanceof ShopOpener opener) {
                opener.onOpenShop();
                told++;
            }
        }
        return told;
    }
}
