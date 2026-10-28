package Item;

// Anything the registry can hand out to the merchant or a chest: gear (Item) or a Consumable.
// Item already has both of these methods, so it just declares that it implements this.
public interface Stockable {
    // doubles as the registry id and the inventory stack key
    String getItemName();
    Item.ItemRarity getItemRarity();
}
