package Shop;

// Implemented by whatever can show the shop (PlayLevelScreen). OpenShopScriptAction finds it among the map's listeners,
// so GameListener itself does not have to change.
@FunctionalInterface
public interface ShopOpener {
    void onOpenShop();
}
