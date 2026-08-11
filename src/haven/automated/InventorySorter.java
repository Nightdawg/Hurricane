package haven.automated;

import haven.*;
import haven.res.ui.tt.q.quality.Quality;

import java.awt.Color;
import java.util.*;

import static haven.Inventory.sqsz;

public class InventorySorter implements Defer.Callable<Void> {
    private static final String[] EXCLUDE = {
	"Character Sheet", "Study",
	"Chicken Coop", "Belt", "Pouch", "Purse",
	"Cauldron", "Finery Forge", "Fireplace", "Frame",
	"Herbalist Table", "Kiln", "Ore Smelter", "Smith's Smelter",
	"Oven", "Pane mold", "Rack", "Smoke shed",
	"Stack Furnace", "Steelbox", "Tub"
    };

    private static final Comparator<WItem> ITEM_COMPARATOR = Comparator
	.comparing((WItem w) -> w.item.getname())
	.thenComparing(w -> {
	    try { return w.item.res.get().name; } catch (Loading e) { return ""; }
	})
	.thenComparing(w -> {
	    Quality q = ItemInfo.find(Quality.class, w.item.info());
	    return q != null ? q.q : 0.0;
	}, Comparator.reverseOrder());

    private static final Object lock = new Object();
    private static InventorySorter current;
    private Defer.Future<Void> task;
    private final List<Inventory> inventories;
    private final GameUI gui;
    private final boolean vertical;

    private InventorySorter(List<Inventory> inventories, GameUI gui, boolean vertical) {
	this.inventories = inventories;
	this.gui = gui;
	this.vertical = vertical;
    }

    public static void sort(Inventory inv) {
	sort(inv, false);
    }

    public static void sort(Inventory inv, boolean vertical) {
	if (inv.ui.gui.vhand != null) {
	    inv.ui.gui.error("Need empty cursor to sort inventory!");
	    return;
	}
	if (vertical)
	    inv.ui.gui.msg("Sorting into columns", Color.WHITE);
	start(new InventorySorter(Collections.singletonList(inv), inv.ui.gui, vertical));
    }

    public static void sortAll(GameUI gui) {
	if (gui.vhand != null) {
	    gui.error("Need empty cursor to sort inventory!");
	    return;
	}
	List<Inventory> targets = new ArrayList<>();
	for (Inventory inv : gui.ui.root.children(Inventory.class)) {
	    Window wnd = inv.getparent(Window.class);
	    if (wnd != null && isExcluded(wnd.cap)) continue;
	    targets.add(inv);
	}
	if (!targets.isEmpty()) {
	    start(new InventorySorter(targets, gui, false));
	}
    }

    private static boolean isExcluded(String cap) {
	if (cap == null) return false;
	for (String ex : EXCLUDE) {
	    if (ex.equals(cap)) return true;
	}
	return false;
    }

    @Override
    public Void call() throws InterruptedException {
	Inventory last = null;
	for (Inventory inv : inventories) {
	    if (inv.parent == null) return null;
	    // No cursor check here between inventories: widget messages are
	    // reliable and ordered, so the server applies this inventory's first
	    // take strictly after the previous inventory's last drop regardless
	    // of what gui.vhand reads locally right now. What matters is the
	    // cursor being empty server-side when each message is processed, and
	    // ordering already guarantees that - a local read could only tell us
	    // about echoes that have made it back, which is a different question.
	    // A stray item left on the cursor at the very end is still caught by
	    // settle below.
	    doSort(inv);
	    last = inv;
	}
	synchronized (lock) {
	    if (current == this) current = null;
	}
	gui.ui.sfxrl(sfx_done);
	if (last != null) settle(last);
	return null;
    }

    /** How long to let the server catch up before reading the cursor. */
    private static final int SETTLE_MS = 500;

    /**
     * Runs after the completion sound, so the wait costs no time the player
     * sees - the items have already moved on screen by then. The plan is built
     * so the server accepts every move, so this only ever fires when something
     * outside the sort touched the inventory mid-run.
     */
    private void settle(Inventory inv) throws InterruptedException {
	Thread.sleep(SETTLE_MS);
	WItem held = gui.vhand;
	if (held == null) return;
	// inv passed the same parent == null check in call()'s loop before it was
	// sorted, but the wait above gives the window time to close too. A closed
	// inv still has a populated child tree (Widget.remove() unlinks it from
	// its parent but never clears its own children), so freeRect would
	// happily compute a plausible-looking cell and the drop message would
	// silently vanish - reporting "returned to inventory" would then be a
	// lie, worse than the truth that it is still on the cursor.
	if (inv.parent == null) {
	    gui.error("Sort stopped early — item left on cursor");
	    return;
	}
	Coord free = freeRect(inv, held.sz.div(sqsz));
	if (free != null) {
	    inv.wdgmsg("drop", free);
	    gui.error("Sort stopped early — item returned to inventory");
	} else {
	    gui.error("Sort stopped early — item left on cursor");
	}
    }

    /** First rect of `slots` free in the inventory as it stands right now. */
    private static Coord freeRect(Inventory inv, Coord slots) {
	boolean[][] grid = maskGrid(inv);
	for (Widget wdg = inv.lchild; wdg != null; wdg = wdg.prev) {
	    if (!wdg.visible || !(wdg instanceof WItem)) continue;
	    WItem w = (WItem) wdg;
	    InventoryLayout.markOccupied(grid, inv.isz, w.c.sub(1, 1).div(sqsz),
					 w.sz.div(sqsz), true);
	}
	return InventoryLayout.findFit(grid, inv.isz, slots, false);
    }

    private static class Entry {
	final WItem w;
	final Coord slots;
	final Coord current;

	Entry(WItem w, Coord slots, Coord current) {
	    this.w = w;
	    this.slots = slots;
	    this.current = current;
	}
    }

    /** The permanently blocked cells, as the server last described them. */
    private static boolean[][] maskGrid(Inventory inv) {
	boolean[][] mask = new boolean[inv.isz.x][inv.isz.y];
	if (inv.sqmask != null) {
	    int mo = 0;
	    for (int y = 0; y < inv.isz.y; y++)
		for (int x = 0; x < inv.isz.x; x++)
		    mask[x][y] = inv.sqmask[mo++];
	}
	return mask;
    }

    private void doSort(Inventory inv) throws InterruptedException {
	// Collect all items, skip those with unloaded sprites
	List<Entry> entries = new ArrayList<>();
	for (Widget wdg = inv.lchild; wdg != null; wdg = wdg.prev) {
	    if (!wdg.visible || !(wdg instanceof WItem)) continue;
	    WItem w = (WItem) wdg;
	    if (w.item.spr() == null) continue;
	    entries.add(new Entry(w, w.sz.div(sqsz), w.c.sub(1, 1).div(sqsz)));
	}
	entries.sort(Comparator.comparing(e -> e.w, ITEM_COMPARATOR));

	Coord[] slots = new Coord[entries.size()];
	Coord[] current = new Coord[entries.size()];
	for (int i = 0; i < slots.length; i++) {
	    slots[i] = entries.get(i).slots;
	    current[i] = entries.get(i).current;
	}

	// The plan is validated against the server's own rules before a single
	// message goes out, so nothing below can be refused. Messages are
	// delivered in order, so no step waits for the one before it.
	InventoryMoves.Plan plan =
	    InventoryMoves.plan(maskGrid(inv), inv.isz, slots, current, vertical);

	for (InventoryMoves.Op op : plan.ops) {
	    if (op.kind == InventoryMoves.TAKE) {
		entries.get(op.item).w.item.wdgmsg("take", Coord.z);
		// throttle only, not synchronisation: one pause per take is
		// what the chain already cost, and half what a large item cost
		Thread.sleep(10);
	    } else {
		inv.wdgmsg("drop", op.cell);
	    }
	}

	// A pinned large item is normal, not newsworthy: under the conservative
	// swap policy a multi-tile item can only move into wholly empty space,
	// so in a packed cupboard it stays put on nearly every sort, and there
	// is nothing the player could do about it anyway. What is worth a toast
	// is the sort achieving nothing at all - the one outcome that would
	// otherwise look like silent failure.
	if (plan.ops.isEmpty() && plan.pinnedMulti)
	    gui.error("No room to move anything — inventory too tightly packed");
    }

    public static void cancel() {
	synchronized (lock) {
	    if (current != null) {
		current.task.cancel();
		current = null;
	    }
	}
    }

    private static final Audio.Clip sfx_done = Audio.resclip(Resource.remote().loadwait("sfx/hud/on"));

    private static void start(InventorySorter sorter) {
	cancel();
	synchronized (lock) { current = sorter; }
	sorter.task = Defer.later(sorter);
    }
}
