package haven.automated;

import haven.Coord;

/**
 * Self-checking tests for InventoryLayout. No JUnit: lib/ has no test jar
 * and build.xml has no test target.
 *
 * Run: javac -d build/classes -cp build/classes src/haven/automated/InventoryLayout*.java
 *      java -cp build/classes haven.automated.InventoryLayoutTest
 */
public class InventoryLayoutTest {
    private static int total = 0;
    private static int failures = 0;

    private static void check(String name, Object expected, Object actual) {
	total++;
	boolean ok = (expected == null) ? (actual == null) : expected.equals(actual);
	if (!ok) failures++;
	System.out.printf("%-52s %s%n", name,
			  ok ? "PASS" : "FAIL (expected " + expected + ", got " + actual + ")");
    }

    /** Builds a w*h grid with the given (x,y) pairs set to true. */
    private static boolean[][] grid(int w, int h, int... cells) {
	boolean[][] g = new boolean[w][h];
	for (int i = 0; i < cells.length; i += 2)
	    g[cells[i]][cells[i + 1]] = true;
	return g;
    }

    private static void findFitTests() {
	Coord isz43 = new Coord(4, 3);
	Coord one = new Coord(1, 1);

	// 6: horizontal fills across the row
	boolean[][] g6 = grid(4, 3);
	Coord a6 = InventoryLayout.findFit(g6, isz43, one, false);
	check("6a findFit horizontal, first 1x1", new Coord(0, 0), a6);
	InventoryLayout.markGrid(g6, a6, one, true);
	check("6b findFit horizontal, second 1x1", new Coord(1, 0),
	      InventoryLayout.findFit(g6, isz43, one, false));

	// 7: vertical fills down the column
	boolean[][] g7 = grid(4, 3);
	Coord a7 = InventoryLayout.findFit(g7, isz43, one, true);
	check("7a findFit vertical, first 1x1", new Coord(0, 0), a7);
	InventoryLayout.markGrid(g7, a7, one, true);
	check("7b findFit vertical, second 1x1", new Coord(0, 1),
	      InventoryLayout.findFit(g7, isz43, one, true));

	// 7c/7d: the whole point of the feature - walk column 0 to its end, then
	// start column 1. A row-major scan gives (2,0) and (3,0) here instead.
	boolean[][] g7c = grid(4, 3);
	Coord[] seq = new Coord[4];
	for (int i = 0; i < seq.length; i++) {
	    seq[i] = InventoryLayout.findFit(g7c, isz43, one, true);
	    InventoryLayout.markGrid(g7c, seq[i], one, true);
	}
	check("7c findFit vertical, third 1x1 ends column 0", new Coord(0, 2), seq[2]);
	check("7d findFit vertical, fourth 1x1 starts column 1", new Coord(1, 0), seq[3]);

	// 8: a 1x2 in a 3-row grid leaves one dead row in its column
	Coord tall = new Coord(1, 2);
	boolean[][] g8 = grid(4, 3);
	Coord a8 = InventoryLayout.findFit(g8, isz43, tall, true);
	check("8a findFit vertical, first 1x2", new Coord(0, 0), a8);
	InventoryLayout.markGrid(g8, a8, tall, true);
	check("8b findFit vertical, second 1x2", new Coord(1, 0),
	      InventoryLayout.findFit(g8, isz43, tall, true));

	// 9/10: the accepted fragmentation difference between the two modes
	Coord isz24 = new Coord(2, 4);
	Coord wide = new Coord(2, 1);
	boolean[][] g9 = grid(2, 4, 0,0, 0,1, 0,2, 0,3, 1,0);   // five 1x1 packed by column
	check("9  findFit vertical, 2x1 does not fit", null,
	      InventoryLayout.findFit(g9, isz24, wide, true));
	boolean[][] g10 = grid(2, 4, 0,0, 1,0, 0,1, 1,1, 0,2);  // five 1x1 packed by row
	check("10 findFit horizontal, 2x1 fits", new Coord(0, 3),
	      InventoryLayout.findFit(g10, isz24, wide, false));

	// 11: blocked cells are never returned, in either mode
	boolean[][] g11 = grid(4, 3, 0,0, 0,1, 0,2);
	check("11a findFit horizontal respects blocked cells", new Coord(1, 0),
	      InventoryLayout.findFit(g11, isz43, one, false));
	check("11b findFit vertical respects blocked cells", new Coord(1, 0),
	      InventoryLayout.findFit(g11, isz43, one, true));

	// 12: item bigger than the grid - negative bounds, must not throw
	check("12a findFit horizontal, item larger than grid", null,
	      InventoryLayout.findFit(grid(2, 2), new Coord(2, 2), new Coord(3, 3), false));
	check("12b findFit vertical, item larger than grid", null,
	      InventoryLayout.findFit(grid(2, 2), new Coord(2, 2), new Coord(3, 3), true));
    }

    /** Coord list from flat (x,y) pairs - used for both sizes and positions. */
    private static Coord[] coords(int... xy) {
	Coord[] s = new Coord[xy.length / 2];
	for (int i = 0; i < s.length; i++)
	    s[i] = new Coord(xy[i * 2], xy[i * 2 + 1]);
	return s;
    }

    /** n items of w*h, appended to the list being built. */
    private static int[] rep(int n, int w, int h) {
	int[] out = new int[n * 2];
	for (int i = 0; i < n; i++) { out[i * 2] = w; out[i * 2 + 1] = h; }
	return out;
    }

    private static int[] cat(int[]... parts) {
	int len = 0;
	for (int[] p : parts) len += p.length;
	int[] out = new int[len];
	int o = 0;
	for (int[] p : parts) { System.arraycopy(p, 0, out, o, p.length); o += p.length; }
	return out;
    }

    /** Every item got a target. */
    private static boolean allPlaced(Coord[] targets) {
	for (Coord t : targets)
	    if (t == null) return false;
	return true;
    }

    private static int placed(Coord[] targets) {
	int n = 0;
	for (Coord t : targets)
	    if (t != null) n++;
	return n;
    }

    /**
     * The invariant every sort must hold: with unplaced items staying where
     * they are, no two item rects may share a cell. A violation means the
     * sorter will drop an item onto a cell another item still occupies.
     */
    private static boolean noOverlap(Coord[] targets, Coord[] current, Coord[] slots, Coord isz) {
	int[][] count = new int[isz.x][isz.y];
	for (int i = 0; i < slots.length; i++) {
	    Coord at = targets[i] != null ? targets[i] : current[i];
	    for (int x = at.x; x < at.x + slots[i].x; x++)
		for (int y = at.y; y < at.y + slots[i].y; y++) {
		    if (x < 0 || x >= isz.x || y < 0 || y >= isz.y) return false;
		    if (++count[x][y] > 1) return false;
		}
	}
	return true;
    }

    private static void assignTargetsTests() {
	Coord isz43 = new Coord(4, 3);
	Coord isz24 = new Coord(2, 4);

	// 17: sanity - twelve 1x1 fill a 4x3 exactly, in both modes
	Coord[] sl17 = coords(rep(12, 1, 1));
	Coord[] cur17 = coords(0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 3,1, 0,2, 1,2, 2,2, 3,2);
	check("17a assignTargets horizontal, twelve 1x1 in 4x3", Boolean.TRUE,
	      allPlaced(InventoryLayout.assignTargets(grid(4, 3), isz43, sl17, cur17, false)));
	check("17b assignTargets vertical, twelve 1x1 in 4x3", Boolean.TRUE,
	      allPlaced(InventoryLayout.assignTargets(grid(4, 3), isz43, sl17, cur17, true)));

	// 18: a full 4x3 holding ten 1x1 and one 1x2. The pre-sort layout is proof
	// the set fits; sorting must not turn that into "no room". The 1x2 sorts
	// last by name, so packing in display order scatters the 1x1 across both
	// free cells first and leaves the 1x2 homeless.
	Coord[] sl18 = coords(cat(rep(10, 1, 1), rep(1, 1, 2)));
	Coord[] cur18 = coords(0,0, 1,0, 2,0, 0,1, 1,1, 2,1, 0,2, 1,2, 2,2, 3,2,
			       3,0);   // the 1x2 spans (3,0)-(3,1)
	Coord[] t18 = InventoryLayout.assignTargets(grid(4, 3), isz43, sl18, cur18, false);
	check("18a assignTargets horizontal, 1x2 after ten 1x1 in a full 4x3", Boolean.TRUE,
	      allPlaced(t18));
	check("18b assignTargets horizontal, the full 4x3 stays consistent", Boolean.TRUE,
	      noOverlap(t18, cur18, sl18, isz43));

	// 19: the vertical counterpart - a 2x1 after five 1x1 in a 2x4
	Coord[] sl19 = coords(cat(rep(5, 1, 1), rep(1, 2, 1)));
	Coord[] cur19 = coords(0,0, 1,0, 0,1, 1,1, 0,2,
			       0,3);   // the 2x1 spans (0,3)-(1,3)
	Coord[] t19 = InventoryLayout.assignTargets(grid(2, 4), isz24, sl19, cur19, true);
	check("19a assignTargets vertical, 2x1 after five 1x1 in a 2x4", Boolean.TRUE,
	      allPlaced(t19));
	check("19b assignTargets vertical, the 2x4 stays consistent", Boolean.TRUE,
	      noOverlap(t19, cur19, sl19, isz24));

	// 20: an item that genuinely cannot be re-homed. sqmask comes from the
	// server, so it need not match where the items already sit: row 1 is
	// masked while a 1x3 still spans (0,0)-(0,2). No column has three free
	// rows, so the 1x3 has to stay put - and that must cost nothing else.
	// It sits mid-list on purpose: at the end, aborting would cost nothing
	// and the assertion below would pass for the wrong reason.
	boolean[][] m20 = grid(4, 3, 0,1, 1,1, 2,1, 3,1);
	Coord[] sl20 = coords(cat(rep(3, 1, 1), rep(1, 1, 3), rep(3, 1, 1)));
	Coord[] cur20 = coords(1,0, 2,0, 3,0,
			       0,0,               // the 1x3 spans (0,0)-(0,2)
			       1,2, 2,2, 3,2);
	Coord[] t20 = InventoryLayout.assignTargets(m20, isz43, sl20, cur20, false);
	check("20a assignTargets pins the item that cannot be re-homed", null, t20[3]);
	check("20b assignTargets, a misfit does not strand the items after it", 6,
	      placed(t20));
	check("20c assignTargets, no target lands on the pinned item", Boolean.TRUE,
	      noOverlap(t20, cur20, sl20, isz43));

	// 21: no two rects may overlap once pinned items are left in place
	Coord[] sl21 = coords(cat(rep(10, 1, 1), rep(1, 1, 2)));
	Coord[] cur21 = coords(0,0, 1,0, 2,0, 0,1, 1,1, 2,1, 0,2, 1,2, 2,2, 3,2, 3,0);
	Coord[] t21 = InventoryLayout.assignTargets(grid(4, 3), isz43, sl21, cur21, false);
	check("21 assignTargets, targets never overlap a pinned item", Boolean.TRUE,
	      noOverlap(t21, cur21, sl21, isz43));

	// 22: masked cells are honoured - a 4x3 with column 0 masked holds a 1x2
	boolean[][] m22 = grid(4, 3, 0,0, 0,1, 0,2);
	Coord[] sl22 = coords(cat(rep(4, 1, 1), rep(1, 1, 2)));
	Coord[] cur22 = coords(1,0, 2,0, 3,0, 1,1,
			       2,1);   // the 1x2 spans (2,1)-(2,2)
	Coord[] t22 = InventoryLayout.assignTargets(m22, isz43, sl22, cur22, false);
	check("22a assignTargets respects the mask", Boolean.TRUE, allPlaced(t22));
	for (Coord t : t22)
	    if (t != null && t.x == 0)
		check("22b assignTargets placed an item in the masked column",
		      "no target in column 0", t);

	// 23: packing largest-first must not reshuffle items of the same size.
	// Two 1x2 in display order A then B: A takes the earlier scan position.
	Coord[] sl23 = coords(cat(rep(2, 1, 2), rep(8, 1, 1)));
	Coord[] cur23 = coords(0,0, 1,0, 2,0, 3,0, 2,1, 3,1, 0,2, 1,2, 2,2, 3,2);
	Coord[] t23 = InventoryLayout.assignTargets(grid(4, 3), isz43, sl23, cur23, false);
	check("23a assignTargets, same-size items keep display order (first)", new Coord(0, 0),
	      t23[0]);
	check("23b assignTargets, same-size items keep display order (second)", new Coord(1, 0),
	      t23[1]);
	check("23c assignTargets, two 1x2 and eight 1x1 all fit a 4x3", Boolean.TRUE,
	      allPlaced(t23));

	// 24: um item fixado de fora fica onde está, e nada é mandado por cima
	// dele. É o gancho que o planejador de movimentos usa quando descobre
	// que um item até cabe no alvo, mas não há como levá-lo até lá.
	Coord[] sl24 = coords(cat(rep(1, 2, 1), rep(10, 1, 1)));
	Coord[] cur24 = coords(2,2,                                  // o 2x1 ocupa (2,2)-(3,2)
			       0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 3,1, 0,2, 1,2);
	boolean[] pin24 = new boolean[sl24.length];
	pin24[0] = true;
	Coord[] t24 = InventoryLayout.assignTargets(grid(4, 3), isz43, sl24, cur24, false, pin24);
	check("24a assignTargets keeps a caller-pinned item put", null, t24[0]);
	check("24b assignTargets places everything else around it", 10, placed(t24));
	check("24c assignTargets, no target lands on the caller-pinned item", Boolean.TRUE,
	      noOverlap(t24, cur24, sl24, isz43));
    }

    private static void copyGridTests() {
	boolean[][] src = grid(4, 3, 1, 1);
	boolean[][] copy = InventoryLayout.copyGrid(src, new Coord(4, 3));
	copy[0][0] = true;
	check("13 copyGrid does not alias the source", Boolean.FALSE, src[0][0]);
    }

    private static void markOccupiedTests() {
	Coord isz43 = new Coord(4, 3);

	// 14: a 2x2 item anchored at (3,2) runs off both edges - must not throw
	boolean[][] g14 = grid(4, 3);
	InventoryLayout.markOccupied(g14, isz43, new Coord(3, 2), new Coord(2, 2), true);
	check("14a markOccupied marks the in-bounds cell", Boolean.TRUE, g14[3][2]);
	check("14b markOccupied leaves the neighbour alone", Boolean.FALSE, g14[2][2]);

	// 14c: a negative anchor clips on the low edge - without the >= 0 half of
	// the guard this throws, and cases 14a/14b/15/16 would not notice
	boolean[][] g14c = grid(4, 3);
	InventoryLayout.markOccupied(g14c, isz43, new Coord(-1, -1), new Coord(2, 2), true);
	check("14c markOccupied clips a negative anchor", Boolean.TRUE, g14c[0][0]);

	// 15: clearing is symmetric
	boolean[][] g15 = grid(4, 3, 1,1, 2,1);
	InventoryLayout.markOccupied(g15, isz43, new Coord(1, 1), new Coord(2, 1), false);
	check("15a markOccupied clears (1,1)", Boolean.FALSE, g15[1][1]);
	check("15b markOccupied clears (2,1)", Boolean.FALSE, g15[2][1]);

	// 16: a zero-sized item marks nothing (sprites smaller than sqsz)
	boolean[][] g16 = grid(4, 3);
	InventoryLayout.markOccupied(g16, isz43, new Coord(0, 0), new Coord(0, 1), true);
	check("16 markOccupied with a zero slot marks nothing", Boolean.FALSE, g16[0][0]);
    }

    public static void main(String[] args) {
	findFitTests();
	assignTargetsTests();
	copyGridTests();
	markOccupiedTests();
	System.out.println();
	System.out.println(failures == 0
			   ? (total + " of " + total + " passed")
			   : (failures + " of " + total + " failed"));
	System.exit(failures == 0 ? 0 : 1);
    }
}
