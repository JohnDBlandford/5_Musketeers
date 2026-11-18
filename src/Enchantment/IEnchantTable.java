package Enchantment;

import Item.Item;
import inventory.Inventory;

import static Enchantment.ItemEnchants.ApplyResult;

public interface IEnchantTable {
    ApplyResult apply(Item item, String bookId, Inventory inv);
    CombineBooksResult combineBooks(String bookId, Inventory inv, int worldCap);
}
