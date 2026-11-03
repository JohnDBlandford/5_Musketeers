package Shop;

// Implemented by whatever can show the blacksmith (PlayLevelScreen). OpenBlacksmithScriptAction finds it among the map's
// listeners, so GameListener itself does not have to change. Separate from ShopOpener on purpose: PlayLevelScreen implements
// both, and each script action must open only its own shop.
@FunctionalInterface
public interface BlacksmithOpener {
    void onOpenBlacksmith();
}
