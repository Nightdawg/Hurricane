package haven.automated;

import haven.*;
import haven.res.ui.tt.q.quality.Quality;

import java.awt.Color;
import java.util.*;
import java.util.stream.Collectors;

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
	    inv.ui.gui.msg("Sorting vertically", Color.WHITE);
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
	for (Inventory inv : inventories) {
	    if (inv.parent == null) return null;
	    if (!doSort(inv)) return null;
	}
	synchronized (lock) {
	    if (current == this) current = null;
	}
	gui.ui.sfxrl(sfx_done);
	return null;
    }

    private static class Entry {
	final WItem w;
	final Coord slots;
	Coord current;
	Coord target;

	Entry(WItem w, Coord slots, Coord current) {
	    this.w = w;
	    this.slots = slots;
	    this.current = current;
	    this.target = current;
	}
    }

    /**
     * Moves an entry to a new cell, keeping the occupancy grid in step. Clears the
     * old rect before reassigning current, so callers cannot get the order wrong.
     */
    private static void relocate(Entry e, Coord to, boolean[][] occupied, Coord isz) {
	InventoryLayout.markOccupied(occupied, isz, e.current, e.slots, false);
	e.current = to;
	InventoryLayout.markOccupied(occupied, isz, e.current, e.slots, true);
    }

    private boolean doSort(Inventory inv) throws InterruptedException {
	// Build mask grid (permanently blocked cells)
	boolean[][] maskGrid = new boolean[inv.isz.x][inv.isz.y];
	if (inv.sqmask != null) {
	    int mo = 0;
	    for (int y = 0; y < inv.isz.y; y++)
		for (int x = 0; x < inv.isz.x; x++)
		    maskGrid[x][y] = inv.sqmask[mo++];
	}

	// Collect all items, skip those with unloaded sprites
	List<Entry> entries = new ArrayList<>();
	for (Widget wdg = inv.lchild; wdg != null; wdg = wdg.prev) {
	    if (!wdg.visible || !(wdg instanceof WItem)) continue;
	    WItem w = (WItem) wdg;
	    if (w.item.spr() == null) continue;
	    Coord slots = w.sz.div(sqsz);
	    Coord current = w.c.sub(1, 1).div(sqsz);
	    entries.add(new Entry(w, slots, current));
	}

	// Live occupancy, kept in step with every move made below
	boolean[][] occupiedGrid = new boolean[inv.isz.x][inv.isz.y];
	for (Entry e : entries)
	    InventoryLayout.markOccupied(occupiedGrid, inv.isz, e.current, e.slots, true);

	// Sort all items together
	entries.sort(Comparator.comparing(e -> e.w, ITEM_COMPARATOR));

	// Assign target positions in scan order, respecting each item's size
	boolean[][] assignGrid = InventoryLayout.copyGrid(maskGrid, inv.isz);
	for (Entry e : entries) {
	    Coord pos = InventoryLayout.findFit(assignGrid, inv.isz, e.slots, vertical);
	    if (pos == null) break;
	    e.target = pos;
	    InventoryLayout.markGrid(assignGrid, pos, e.slots, true);
	}

	List<Entry> singles = entries.stream().filter(e -> e.slots.x * e.slots.y == 1).collect(Collectors.toList());
	List<Entry> multis  = entries.stream().filter(e -> e.slots.x * e.slots.y > 1).collect(Collectors.toList());

	// Phase 1: place multi-tile items
	// For each, first evict any 1x1 items from its target cells, then take+drop it
	boolean anyMultiSkipped = false;
	for (Entry me : multis) {
	    if (me.current.equals(me.target)) continue;
	    boolean blocked = false;
	    for (int tx = me.target.x; tx < me.target.x + me.slots.x && !blocked; tx++) {
		for (int ty = me.target.y; ty < me.target.y + me.slots.y && !blocked; ty++) {
		    Coord cell = new Coord(tx, ty);
		    for (Entry se : singles) {
			if (se.current.equals(cell)) {
			    Coord free = InventoryLayout.findFreeCell(inv.isz, maskGrid, occupiedGrid);
			    if (free == null) { blocked = true; break; }
			    se.w.item.wdgmsg("take", Coord.z);
			    Thread.sleep(10);
			    inv.wdgmsg("drop", free);
			    Thread.sleep(10);
			    relocate(se, free, occupiedGrid, inv.isz);
			    break;
			}
		    }
		}
	    }
	    if (blocked) { anyMultiSkipped = true; continue; }
	    me.w.item.wdgmsg("take", Coord.z);
	    Thread.sleep(10);
	    inv.wdgmsg("drop", me.target);
	    Thread.sleep(10);
	    relocate(me, me.target, occupiedGrid, inv.isz);
	}
	if (anyMultiSkipped)
	    gui.error("Could not move all large items — inventory too full");

	// Phase 2: sort 1x1 items using chain/swap algorithm
	for (Entry se : singles) {
	    if (se.current.equals(se.target)) continue;
	    se.w.item.wdgmsg("take", Coord.z);
	    Entry handu = se;
	    // a legitimate cycle visits each single at most once, so n+1 is one spare
	    int guard = singles.size() + 1;
	    while (handu != null) {
		if (--guard < 0) {
		    gui.error("Sort incomplete — placement chain too long, check your cursor");
		    return false;
		}
		inv.wdgmsg("drop", handu.target);
		Entry next = null;
		for (Entry x : singles) {
		    if (x != handu && x.current.equals(handu.target)) { next = x; break; }
		}
		handu.current = handu.target;
		handu = next;
	    }
	    Thread.sleep(10);
	}
	return true;
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
