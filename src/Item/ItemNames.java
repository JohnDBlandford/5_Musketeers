package Item;

import java.util.Objects;

// Item ids in the code are written like "WraithboneSaber"; the player should see "Wraithbone Saber". A space goes between a
// lower case letter and an upper case letter that follows it, and nothing else changes (the same rule BattleUI.prettyName and
// InventoryScreen use). Pure, so chest messages, shops and screens can share it without depending on any drawing code.
public final class ItemNames {
    private ItemNames() {}

    public static String prettyName(String name) {
        Objects.requireNonNull(name, "name is not pretty enough");
        return name.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
    }
}
