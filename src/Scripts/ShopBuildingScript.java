package Scripts;

import java.util.ArrayList;
import Level.Script;
import ScriptActions.*;

// The interact script for a world's shop building: the player is locked, the shopkeeper greets them, then the shop opens.
// There is deliberately NO UnlockPlayerScriptAction here: the shop stays open after this script ends, and
// ShopController's onClosed callback (PlayLevelScreen passes player::unlock) frees the player when the shop closes.
// Attach it with  mapTile.setInteractScript(new ShopBuildingScript())  (or on an entity / portal-style tile).
public class ShopBuildingScript extends Script {
    // PLACEHOLDER greeting, to be replaced with the merchant's real lines
    // each string is one textbox page; the engine Textbox is 30pt Arial in a 750px box, so keep a page under about 52 characters
    public static final String[] DEFAULT_GREETING = {
            "Hm. A customer. Don't see many of those anymore.",
            "Take a look. Everything here is honestly priced...",
            "...mostly."
    };

    private final String[] greeting;

    public ShopBuildingScript() {
        this(DEFAULT_GREETING);
    }

    public ShopBuildingScript(String[] greeting) {
        this.greeting = greeting.clone();
    }

    @Override
    public ArrayList<ScriptAction> loadScriptActions() {
        ArrayList<ScriptAction> actions = new ArrayList<>();
        actions.add(new LockPlayerScriptAction());
        actions.add(new TextboxScriptAction(greeting));
        actions.add(new OpenShopScriptAction());
        return actions;
    }
}
