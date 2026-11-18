package Enchantment;

import Item.Item;
import inventory.Inventory;

import java.util.Objects;

// The logic behind the enchant table: applying a book to a piece of gear, and combining two books into one of the next level.
// Stateless, so one instance can be shared. Gold is never involved (the book price is the whole cost), and every check runs
// before anything changes, so a rejected call leaves the pack exactly as it was.
public class EnchantTable implements IEnchantTable {
    /// Checks run in this order: item in the pack, book held, slot fits, level higher. Only APPLIED and REPLACED use up a book.
    /// @param item the item to apply the enchant to
    /// @param bookId the id of the enchanted book used to enchant the item
    /// @param inv the pack that holds the item and the book. The book is used up from it on success
    /// @return what happened: NOT_IN_INVENTORY, NO_BOOK, WRONG_ITEM_TYPE, NOT_HIGHER, REPLACED or APPLIED
    @Override
    public ItemEnchants.ApplyResult apply(Item item, String bookId, Inventory inv) {
        Objects.requireNonNull(item, "item not set");
        Objects.requireNonNull(bookId, "bookId not set");
        Objects.requireNonNull(inv, "inventory not set");

        if (!inv.getAllItems().contains(item)) return ItemEnchants.ApplyResult.NOT_IN_INVENTORY;
        // an unknown id, a gear id or a book of another level simply is not held
        EnchantBook book = findBook(inv, bookId);
        if (book == null) return ItemEnchants.ApplyResult.NO_BOOK;

        // the slot and level rules live in ItemEnchants.apply, which changes nothing unless it returns APPLIED or REPLACED
        ItemEnchants.ApplyResult result = item.getEnchants().apply(book.getEnchantment(), book.getLevel(), item.getEquipSlot());
        if (result == ItemEnchants.ApplyResult.APPLIED || result == ItemEnchants.ApplyResult.REPLACED) {
            if (inv.useBook(bookId) == null) throw new IllegalStateException("Book " + bookId + " vanished during an apply");
        }
        return result;
    }

    /// Checks run in this order: cap is valid (throws), two books held, level + 1 within the cap, room for the new book.
    /// @param bookId the id of the two books being combined, one id for both because they must be the same enchantment and level
    /// @param inv the pack that holds the books. Two are used up and one of the next level is added
    /// @param worldCap the highest book level this world allows (1 to Enchantment.getMaxLevel()). Throws IllegalArgumentException outside that
    /// @return what happened: NOT_ENOUGH_BOOKS, AT_WORLD_CAP, INVENTORY_FULL or SUCCESS
    @Override
    public CombineBooksResult combineBooks(String bookId, Inventory inv, int worldCap) {
        Objects.requireNonNull(bookId, "bookId not set");
        Objects.requireNonNull(inv, "inventory not set");
        Enchantment.requireValidLevel(worldCap);

        if (inv.getBookCount(bookId) < 2) return CombineBooksResult.NOT_ENOUGH_BOOKS;
        EnchantBook book = findBook(inv, bookId);
        if (book == null) throw new IllegalStateException("Book " + bookId + " is counted but has no stack");
        if (book.getLevel() + 1 > worldCap) return CombineBooksResult.AT_WORLD_CAP;

        // whether the new book fits depends on which stacks the two books leave, so try it and put them back if it does not.
        // freeing a slot always makes room for putting the two books back, so the undo cannot fail
        EnchantBook next = new EnchantBook(book.getEnchantment(), book.getLevel() + 1);
        inv.useBook(bookId);
        inv.useBook(bookId);
        if (!inv.addBook(next)) {
            if (!inv.addBook(book) || !inv.addBook(book)) throw new IllegalStateException("Could not put " + bookId + " back");
            return CombineBooksResult.INVENTORY_FULL;
        }
        return CombineBooksResult.SUCCESS;
    }

    // the book with this id from the pack's stacks, or null if none is held
    private static EnchantBook findBook(Inventory inv, String bookId) {
        Objects.requireNonNull(inv, "inventory cannot be null");
        for (Inventory.BookStack stack : inv.getAllBooks()) {
            if (stack.book().getItemName().equals(bookId)) return stack.book();
        }
        return null;
    }
}
