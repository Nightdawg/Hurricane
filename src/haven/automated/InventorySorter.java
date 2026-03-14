package haven.automated;

import haven.*;
import haven.res.ui.tt.q.quality.Quality;

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

    private InventorySorter(List<Inventory> inventories) {
	this.inventories = inventories;
    }

    public static void sort(Inventory inv) {
	if (inv.ui.gui.vhand != null) {
	    inv.ui.gui.error("Need empty cursor to sort inventory!");
	    return;
	}
	start(new InventorySorter(Collections.singletonList(inv)), inv.ui.gui);
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
	    start(new InventorySorter(targets), gui);
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
	    if (inv.disposed()) {
		cancel();
		break;
	    }
	    doSort(inv);
	}
	synchronized (lock) {
	    if (current == this) current = null;
	}
	return null;
    }

    private void doSort(Inventory inv) throws InterruptedException {
	boolean[][] grid = new boolean[inv.isz.x][inv.isz.y];
	boolean[] mask = inv.sqmask;
	if (mask != null) {
	    int mo = 0;
	    for (int y = 0; y < inv.isz.y; y++)
		for (int x = 0; x < inv.isz.x; x++)
		    grid[x][y] = mask[mo++];
	}

	List<WItem> items = new ArrayList<>();
	for (Widget wdg = inv.lchild; wdg != null; wdg = wdg.prev) {
	    if (wdg.visible && wdg instanceof WItem) {
		WItem w = (WItem) wdg;
		Coord slots = w.sz.div(sqsz);
		if (slots.x * slots.y == 1) {
		    items.add(w);
		} else {
		    Coord loc = w.c.sub(1, 1).div(sqsz);
		    for (int x = 0; x < slots.x; x++)
			for (int y = 0; y < slots.y; y++)
			    grid[loc.x + x][loc.y + y] = true;
		}
	    }
	}

	// [WItem, currentPos, targetPos]
	List<Object[]> sorted = items.stream()
	    .sorted(ITEM_COMPARATOR)
	    .map(w -> new Object[]{w, w.c.sub(1, 1).div(sqsz), new Coord(0, 0)})
	    .collect(Collectors.toList());

	int cx = -1, cy = 0;
	for (Object[] a : sorted) {
	    while (true) {
		if (++cx == inv.isz.x) { cx = 0; cy++; }
		if (cy == inv.isz.y) break;
		if (!grid[cx][cy]) { a[2] = new Coord(cx, cy); break; }
	    }
	    if (cy == inv.isz.y) break;
	}

	for (Object[] a : sorted) {
	    if (a[1].equals(a[2])) continue;
	    ((WItem) a[0]).item.wdgmsg("take", Coord.z);
	    Object[] handu = a;
	    while (handu != null) {
		inv.wdgmsg("drop", handu[2]);
		Object[] b = null;
		for (Object[] x : sorted) {
		    if (x[1].equals(handu[2])) { b = x; break; }
		}
		handu[1] = handu[2];
		handu = b;
	    }
	    Thread.sleep(10);
	}
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

    private static void start(InventorySorter sorter, GameUI gui) {
	cancel();
	synchronized (lock) { current = sorter; }
	sorter.task = Defer.later(sorter);
	sorter.task.callback(() -> {
	    if (!sorter.task.cancelled()) {
		gui.ui.sfxrl(sfx_done);
	    }
	});
    }
}
