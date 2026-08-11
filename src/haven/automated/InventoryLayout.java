package haven.automated;

import haven.Coord;

import java.util.Arrays;

/**
 * Pure grid placement logic for InventorySorter.
 *
 * Depends on nothing but haven.Coord on purpose: InventorySorter's static
 * initialiser loads a sound from the resource server, so anything that needs
 * to be testable has to live outside that class.
 *
 * Grids are boolean[x][y], true meaning unavailable.
 */
class InventoryLayout {
    /**
     * First position where an item of `slots` size fits, or null.
     * Horizontal scans rows first, vertical scans columns first.
     */
    static Coord findFit(boolean[][] grid, Coord isz, Coord slots, boolean vertical) {
	if (vertical) {
	    for (int x = 0; x <= isz.x - slots.x; x++)
		for (int y = 0; y <= isz.y - slots.y; y++)
		    if (fits(grid, x, y, slots)) return new Coord(x, y);
	} else {
	    for (int y = 0; y <= isz.y - slots.y; y++)
		for (int x = 0; x <= isz.x - slots.x; x++)
		    if (fits(grid, x, y, slots)) return new Coord(x, y);
	}
	return null;
    }

    /**
     * Order to pack items in: largest first, ties broken along the fill
     * direction, then by display order. Placing the big items while the grid is
     * still whole is what keeps first-fit from splitting the free space into
     * pieces too small for them - packing in display order does not, and can
     * fail to re-home an item set that demonstrably fits.
     *
     * Returns indices into slots. Stable, so items of one size keep their
     * display order relative to each other.
     */
    static int[] packOrder(Coord[] slots, boolean vertical) {
	Integer[] order = new Integer[slots.length];
	for (int i = 0; i < order.length; i++)
	    order[i] = i;
	Arrays.sort(order, (a, b) -> {
	    Coord sa = slots[a], sb = slots[b];
	    int c = (sb.x * sb.y) - (sa.x * sa.y);
	    if (c != 0) return c;
	    // along the fill direction first: a vertical sort cares about height
	    c = vertical ? (sb.y - sa.y) : (sb.x - sa.x);
	    if (c != 0) return c;
	    c = vertical ? (sb.x - sa.x) : (sb.y - sa.y);
	    if (c != 0) return c;
	    return a - b;
	});
	int[] out = new int[order.length];
	for (int i = 0; i < out.length; i++)
	    out[i] = order[i];
	return out;
    }

    /**
     * Target position for every item, packed largest-first but returned aligned
     * to the caller's display order. Null means the item did not fit anywhere
     * and is pinned where it already is.
     *
     * An item that does not fit is pinned rather than skipped: its current rect
     * is reserved and the pass restarts, so no other item is ever assigned a
     * target on top of it. That terminates - each restart pins one more item,
     * and the all-pinned state is the original layout, which fits by
     * construction.
     */
    static Coord[] assignTargets(boolean[][] mask, Coord isz, Coord[] slots,
				 Coord[] current, boolean vertical) {
	Coord[] targets = new Coord[slots.length];
	boolean[] pinned = new boolean[slots.length];
	int[] order = packOrder(slots, vertical);
	for (int attempt = 0; attempt <= slots.length; attempt++) {
	    boolean[][] grid = copyGrid(mask, isz);
	    for (int i = 0; i < slots.length; i++)
		if (pinned[i])
		    markOccupied(grid, isz, current[i], slots[i], true);
	    Arrays.fill(targets, null);
	    int failed = -1;
	    for (int idx : order) {
		if (pinned[idx]) continue;
		Coord pos = findFit(grid, isz, slots[idx], vertical);
		if (pos == null) { failed = idx; break; }
		targets[idx] = pos;
		markGrid(grid, pos, slots[idx], true);
	    }
	    if (failed < 0) return targets;
	    pinned[failed] = true;
	}
	return targets;
    }

    static boolean fits(boolean[][] grid, int ox, int oy, Coord slots) {
	for (int x = 0; x < slots.x; x++)
	    for (int y = 0; y < slots.y; y++)
		if (grid[ox + x][oy + y]) return false;
	return true;
    }

    static boolean[][] copyGrid(boolean[][] src, Coord sz) {
	boolean[][] copy = new boolean[sz.x][sz.y];
	for (int x = 0; x < sz.x; x++)
	    copy[x] = Arrays.copyOf(src[x], sz.y);
	return copy;
    }

    /** Marks an item's rect. The caller guarantees it is in bounds (a findFit result). */
    static void markGrid(boolean[][] grid, Coord pos, Coord slots, boolean val) {
	for (int x = 0; x < slots.x; x++)
	    for (int y = 0; y < slots.y; y++)
		grid[pos.x + x][pos.y + y] = val;
    }

    /**
     * Marks an item's rect, ignoring cells outside isz. For positions that are not
     * findFit results - widget geometry or server echoes - which markGrid's
     * unchecked indexing cannot accept. The caller is responsible for isz matching
     * the grid's actual dimensions.
     */
    static void markOccupied(boolean[][] grid, Coord isz, Coord pos, Coord slots, boolean val) {
	for (int x = pos.x; x < pos.x + slots.x; x++)
	    for (int y = pos.y; y < pos.y + slots.y; y++)
		if (x >= 0 && x < isz.x && y >= 0 && y < isz.y)
		    grid[x][y] = val;
    }

    /** First free, unmasked cell in row-major scan order, or null. */
    static Coord findFreeCell(Coord isz, boolean[][] mask, boolean[][] occupied) {
	for (int y = 0; y < isz.y; y++)
	    for (int x = 0; x < isz.x; x++) {
		if (mask[x][y]) continue;
		if (occupied[x][y]) continue;
		return new Coord(x, y);
	    }
	return null;
    }
}
