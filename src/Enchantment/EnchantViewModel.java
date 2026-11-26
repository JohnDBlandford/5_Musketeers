package Enchantment;

import Enchantment.ItemEnchants.ApplyResult;
import Item.Item;
import Item.ItemEntry;
import inventory.Inventory;
import inventory.Inventory.BookStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class EnchantViewModel {
    public enum Mode { APPLY, COMBINE }

    private final IEnchantTable table;
    private final Supplier<Inventory> inventory;
    private boolean opened = false;
    private int world;
    private int itemIndex;
    private int bookIndex;
    private Mode mode = Mode.APPLY;
    private String message = "";

    public EnchantViewModel(IEnchantTable table, Supplier<Inventory> inventory) {
        this.table = Objects.requireNonNull(table, "table not set");
        this.inventory = Objects.requireNonNull(inventory, "inventory not set");
    }

    public void open(int world) {
        ItemEntry.requireValidWorld(world);
        this.world = world;
        opened = true;
        itemIndex = 0;
        bookIndex = 0;
        mode = Mode.APPLY;
        message = "";
    }

    public int getWorldCap() { requireOpened(); return world; }
    public List<Item> getItems() { requireOpened(); return inventory.get().getAllItems(); }
    public List<BookStack> getBooks() { requireOpened(); return inventory.get().getAllBooks(); }

    public int getSelectedItemIndex() { return Math.clamp(itemIndex, 0, Math.max(0, getItems().size() - 1)); }
    public int getSelectedBookIndex() { return Math.clamp(bookIndex, 0, Math.max(0, getBooks().size() - 1)); }
    public Item getSelectedItem() { List<Item> l = getItems(); return l.isEmpty() ? null : l.get(getSelectedItemIndex()); }
    public BookStack getSelectedBook() { List<BookStack> l = getBooks(); return l.isEmpty() ? null : l.get(getSelectedBookIndex()); }

    public void selectPreviousItem() { moveItem(getSelectedItemIndex() - 1); }
    public void selectNextItem() { moveItem(getSelectedItemIndex() + 1); }
    public void selectPreviousBook() { moveBook(getSelectedBookIndex() - 1); }
    public void selectNextBook() { moveBook(getSelectedBookIndex() + 1); }

    private void moveItem(int target) {
        int current = getSelectedItemIndex();
        int next = Math.clamp(target, 0, Math.max(0, getItems().size() - 1));
        itemIndex = next;
        if (next != current) message = "";
    }

    private void moveBook(int target) {
        int current = getSelectedBookIndex();
        int next = Math.clamp(target, 0, Math.max(0, getBooks().size() - 1));
        bookIndex = next;
        if (next != current) message = "";
    }

    public Mode getMode() { requireOpened(); return mode; }
    public void toggleMode() { requireOpened(); mode = mode == Mode.APPLY ? Mode.COMBINE : Mode.APPLY; message = ""; }
    public String getMessage() { return message; }

    public EnchantBook getCombineTarget() {
        BookStack stack = getSelectedBook();
        if (stack == null) return null;
        int next = stack.book().getLevel() + 1;
        if (next > Enchantment.getMaxLevel() || next > world) return null;
        return new EnchantBook(stack.book().getEnchantment(), next);
    }

    public ApplyResult applyEnchant() {
        requireOpened();
        Item item = getSelectedItem();
        if (item == null) { message = "You have no gear."; return ApplyResult.NOT_IN_INVENTORY; }
        BookStack book = getSelectedBook();
        if (book == null) { message = "You have no books."; return ApplyResult.NO_BOOK; }
        ApplyResult result = table.apply(item, book.book().getItemName(), inventory.get());
        message = switch (result) {
            case APPLIED -> "Enchantment applied!";
            case REPLACED -> "Enchantment upgraded!";
            case WRONG_ITEM_TYPE -> "That book doesn't fit this item.";
            case NOT_HIGHER -> "The item already has that enchantment at this level or higher.";
            case NOT_IN_INVENTORY -> "Item not found.";
            case NO_BOOK -> "Book not found.";
        };
        return result;
    }

    public CombineBooksResult combineBooks() {
        requireOpened();
        BookStack book = getSelectedBook();
        if (book == null) { message = "You have no books to combine."; return CombineBooksResult.NOT_ENOUGH_BOOKS; }
        CombineBooksResult result = table.combineBooks(book.book().getItemName(), inventory.get(), world);
        message = switch (result) {
            case SUCCESS -> "Books combined!";
            case NOT_ENOUGH_BOOKS -> "You need two of the same book.";
            case AT_WORLD_CAP -> "This world's cap is reached. Higher levels need a later world.";
            case INVENTORY_FULL -> "Your pack is full.";
        };
        return result;
    }

    private void requireOpened() { if (!opened) throw new IllegalStateException("EnchantViewModel.open(world) must be called first"); }
}
