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
			  ok ? "PASS" : "FAIL (esperado " + expected + ", veio " + actual + ")");
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
	check("6a findFit horizontal, primeiro 1x1", new Coord(0, 0), a6);
	InventoryLayout.markGrid(g6, a6, one, true);
	check("6b findFit horizontal, segundo 1x1", new Coord(1, 0),
	      InventoryLayout.findFit(g6, isz43, one, false));

	// 7: vertical fills down the column
	boolean[][] g7 = grid(4, 3);
	Coord a7 = InventoryLayout.findFit(g7, isz43, one, true);
	check("7a findFit vertical, primeiro 1x1", new Coord(0, 0), a7);
	InventoryLayout.markGrid(g7, a7, one, true);
	check("7b findFit vertical, segundo 1x1", new Coord(0, 1),
	      InventoryLayout.findFit(g7, isz43, one, true));

	// 8: a 1x2 in a 3-row grid leaves one dead row in its column
	Coord tall = new Coord(1, 2);
	boolean[][] g8 = grid(4, 3);
	Coord a8 = InventoryLayout.findFit(g8, isz43, tall, true);
	check("8a findFit vertical, primeiro 1x2", new Coord(0, 0), a8);
	InventoryLayout.markGrid(g8, a8, tall, true);
	check("8b findFit vertical, segundo 1x2", new Coord(1, 0),
	      InventoryLayout.findFit(g8, isz43, tall, true));

	// 9/10: the accepted fragmentation difference between the two modes
	Coord isz24 = new Coord(2, 4);
	Coord wide = new Coord(2, 1);
	boolean[][] g9 = grid(2, 4, 0,0, 0,1, 0,2, 0,3, 1,0);   // cinco 1x1 por coluna
	check("9  findFit vertical, 2x1 nao cabe", null,
	      InventoryLayout.findFit(g9, isz24, wide, true));
	boolean[][] g10 = grid(2, 4, 0,0, 1,0, 0,1, 1,1, 0,2);  // cinco 1x1 por linha
	check("10 findFit horizontal, 2x1 cabe", new Coord(0, 3),
	      InventoryLayout.findFit(g10, isz24, wide, false));

	// 11: blocked cells are never returned, in either mode
	boolean[][] g11 = grid(4, 3, 0,0, 0,1, 0,2);
	check("11a findFit horizontal respeita bloqueio", new Coord(1, 0),
	      InventoryLayout.findFit(g11, isz43, one, false));
	check("11b findFit vertical respeita bloqueio", new Coord(1, 0),
	      InventoryLayout.findFit(g11, isz43, one, true));

	// 12: item bigger than the grid - negative bounds, must not throw
	check("12a findFit horizontal, item maior que a grade", null,
	      InventoryLayout.findFit(grid(2, 2), new Coord(2, 2), new Coord(3, 3), false));
	check("12b findFit vertical, item maior que a grade", null,
	      InventoryLayout.findFit(grid(2, 2), new Coord(2, 2), new Coord(3, 3), true));
    }

    public static void main(String[] args) {
	findFitTests();
	System.out.println();
	System.out.println(failures == 0
			   ? (total + " de " + total + " passaram")
			   : (failures + " de " + total + " falharam"));
	System.exit(failures == 0 ? 0 : 1);
    }
}
