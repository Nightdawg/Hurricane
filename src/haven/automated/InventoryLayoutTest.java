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

    private static void copyGridTests() {
	boolean[][] src = grid(4, 3, 1, 1);
	boolean[][] copy = InventoryLayout.copyGrid(src, new Coord(4, 3));
	copy[0][0] = true;
	check("13 copyGrid does not alias the source", Boolean.FALSE, src[0][0]);
    }

    private static void findFreeCellTests() {
	Coord isz43 = new Coord(4, 3);
	Coord isz22 = new Coord(2, 2);

	// 1: column 0 full - what a vertical sort leaves behind. 8 cells free.
	check("1 findFreeCell, column 0 full", new Coord(1, 0),
	      InventoryLayout.findFreeCell(isz43, grid(4, 3), grid(4, 3, 0,0, 0,1, 0,2)));

	// 2: row 0 full - what a horizontal sort leaves behind. 8 cells free.
	check("2 findFreeCell, row 0 full", new Coord(0, 1),
	      InventoryLayout.findFreeCell(isz43, grid(4, 3), grid(4, 3, 0,0, 1,0, 2,0, 3,0)));

	// 3: scan stops before the occupied cell
	check("3 findFreeCell, only (1,0) occupied", new Coord(0, 0),
	      InventoryLayout.findFreeCell(isz43, grid(4, 3), grid(4, 3, 1,0)));

	// 4: a masked cell is skipped, not treated as scan-terminating
	check("4 findFreeCell, masked (0,0) and occupied (1,0)", new Coord(2, 0),
	      InventoryLayout.findFreeCell(isz43, grid(4, 3, 0,0), grid(4, 3, 1,0)));

	// 5: genuinely full
	check("5 findFreeCell, 2x2 full", null,
	      InventoryLayout.findFreeCell(isz22, grid(2, 2), grid(2, 2, 0,0, 1,0, 0,1, 1,1)));
    }

    public static void main(String[] args) {
	findFitTests();
	copyGridTests();
	findFreeCellTests();
	System.out.println();
	System.out.println(failures == 0
			   ? (total + " of " + total + " passed")
			   : (failures + " of " + total + " failed"));
	System.exit(failures == 0 ? 0 : 1);
    }
}
