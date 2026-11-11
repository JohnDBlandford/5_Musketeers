package Shop.Blacksmith;

import Item.Item;
import inventory.Inventory;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

// Everything BlacksmithScreen decides that is not drawing: which group of matching items is selected, which two copies of it
// are picked, the preview (who is kept and what it costs), combining, and the status message. The screen maps keys to these
// methods and draws what they report.
// Groups are always read live from the IBlacksmith, never cached, so a combine or a change in the pack shows up immediately and
// the selection and the two copy cursors are re-clamped against whatever is there now. The two cursors always sit on two
// DIFFERENT copies of the selected group.
public final class BlacksmithViewModel {
    private final IBlacksmith blacksmith;
    private final Supplier<Inventory> inventory;   // a supplier because PlayLevelScreen can replace its Inventory object
    private boolean opened = false;
    private int groupIndex;
    private int firstIndex;
    private int secondIndex;
    private String message = "";

    // one consistent look at the groups: the selected group (null when there are none) and the cursors clamped to it
    private record View(int groupIndex, List<Item> group, int first, int second) {}

    public BlacksmithViewModel(IBlacksmith iBlacksmith, Supplier<Inventory> inventorySupplier) {
        this.blacksmith = Objects.requireNonNull(iBlacksmith, "blacksmith not set");
        this.inventory = Objects.requireNonNull(inventorySupplier, "inventory not supplied");
    }

    // call every time the blacksmith opens: back to the first group, copies 0 and 1, no message
    public void open() {
        opened = true;
        groupIndex = 0;
        firstIndex = 0;
        secondIndex = 1;
        message = "";
    }

    public List<List<Item>> getGroups() {
        requireOpened();
        return blacksmith.getCombinableGroups(inventory.get());
    }

    // clamped to the current group list (0 when there are none)
    public int getSelectedGroupIndex() { return view().groupIndex(); }

    // null when there is nothing to combine
    public List<Item> getSelectedGroup() { return view().group(); }

    public void selectPreviousGroup() { selectGroup(view().groupIndex() - 1); }

    public void selectNextGroup() { selectGroup(view().groupIndex() + 1); }

    private void selectGroup(int target) {
        View current = view();
        int size = getGroups().size();
        int next = Math.clamp(target, 0, Math.max(0, size - 1));
        groupIndex = next;
        if (next != current.groupIndex()) {
            firstIndex = 0;
            secondIndex = 1;
            message = "";
        }
    }

    public int getFirstIndex() { return view().first(); }

    public int getSecondIndex() { return view().second(); }

    // the two picked copies, null when there is nothing to combine
    public Item getFirst() {
        View view = view();
        return view.group() == null ? null : view.group().get(view.first());
    }

    public Item getSecond() {
        View view = view();
        return view.group() == null ? null : view.group().get(view.second());
    }

    // moves the first cursor to the next copy the second cursor is not on, wrapping. a group of 2 has nothing to move to
    public void cycleFirst() {
        View view = view();
        int next = nextFree(view, view.first(), view.second());
        if (next != view.first()) {
            firstIndex = next;
            secondIndex = view.second();   // keep the other cursor where the player sees it, even if it was only clamped
            message = "";
        }
    }

    public void cycleSecond() {
        View view = view();
        int next = nextFree(view, view.second(), view.first());
        if (next != view.second()) {
            secondIndex = next;
            firstIndex = view.first();
            message = "";
        }
    }

    // who combine() would keep, who would be used up, and what it costs. null / 0 when nothing is selected
    public Item getKeeper() {
        Item first = getFirst();
        return first == null ? null : blacksmith.pickKeeper(first, getSecond());
    }

    public Item getFodder() {
        Item first = getFirst();
        if (first == null) return null;
        Item second = getSecond();
        return blacksmith.pickKeeper(first, second) == first ? second : first;
    }

    public int getFee() {
        Item first = getFirst();
        return first == null ? 0 : blacksmith.getRepairFee(first);
    }

    public String getMessage() { return this.message; }

    // with nothing to combine this never reaches the blacksmith. CombineResult has no "nothing to combine" value, so it reports
    // NOT_IN_INVENTORY with its own message
    public CombineResult combine() {
        View view = view();
        if (view.group() == null) {
            message = NOTHING_TO_COMBINE;
            return CombineResult.NOT_IN_INVENTORY;
        }
        CombineResult result = blacksmith.combine(view.group().get(view.first()), view.group().get(view.second()), inventory.get());
        message = messageFor(result);
        return result;
    }

    // the next index after current that is not taken, wrapping. current itself when the group is too small to move in
    private static int nextFree(View view, int current, int taken) {
        if (view.group() == null || view.group().size() < 2) return current;
        int size = view.group().size();
        int next = (current + 1) % size;
        if (next == taken) next = (next + 1) % size;
        return next;
    }

    // reads the groups once and clamps everything against them
    private View view() {
        List<List<Item>> groups = getGroups();
        if (groups.isEmpty()) return new View(0, null, 0, 0);
        int index = Math.clamp(groupIndex, 0, groups.size() - 1);
        List<Item> group = groups.get(index);
        int last = group.size() - 1;
        int first = Math.clamp(firstIndex, 0, last);
        int second = Math.clamp(secondIndex, 0, last);
        if (first == second && last >= 1) second = (first == 0) ? 1 : 0;   // the group shrank under the cursors
        return new View(index, group, first, second);
    }

    // PLACEHOLDER wording
    private static final String NOTHING_TO_COMBINE = "Nothing to combine.";

    private static String messageFor(CombineResult result) {
        return switch (result) {
            case SUCCESS -> "Combined and repaired!";
            case NOT_IN_INVENTORY -> "Item not found.";
            case SAME_COPY -> "Pick two different copies.";
            case NOT_SAME_ITEM -> "Those items don't match.";
            case BROKEN -> "A broken item can't be combined.";
            case NOT_ENOUGH_GOLD -> "Not enough gold.";
        };
    }

    private void requireOpened() {
        if (!opened) throw new IllegalStateException("BlacksmithViewModel.open() must be called first");
    }
}
