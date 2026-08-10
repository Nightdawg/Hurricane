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
     * Marks an item's rect, ignoring cells outside the grid. Used for positions
     * reported by the server, which markGrid's unchecked indexing cannot accept.
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
