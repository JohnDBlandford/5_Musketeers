package Shop.Blacksmith;

import Item.Item;
import inventory.Inventory;

import java.util.List;

public interface IBlacksmith {
    int getRepairFee(Item item);
    List<List<Item>> getCombinableGroups(Inventory inventory);
    Item pickKeeper(Item a, Item b);
    CombineResult combine(Item a, Item b, Inventory inventory);
}
