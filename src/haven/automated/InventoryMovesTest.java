package haven.automated;

import haven.Coord;

/**
 * Self-checking tests for InventoryMoves. No JUnit: lib/ has no test jar and
 * build.xml has no test target.
 *
 * Run: make check
 */
public class InventoryMovesTest {
    private static int total = 0;
    private static int failures = 0;

    private static void check(String name, Object expected, Object actual) {
	total++;
	boolean ok = (expected == null) ? (actual == null) : expected.equals(actual);
	if (!ok) failures++;
	System.out.printf("%-58s %s%n", name,
			  ok ? "PASS" : "FAIL (expected " + expected + ", got " + actual + ")");
    }

    private static boolean[][] grid(int w, int h, int... cells) {
	boolean[][] g = new boolean[w][h];
	for (int i = 0; i < cells.length; i += 2)
	    g[cells[i]][cells[i + 1]] = true;
	return g;
    }

    private static Coord[] coords(int... xy) {
	Coord[] s = new Coord[xy.length / 2];
	for (int i = 0; i < s.length; i++)
	    s[i] = new Coord(xy[i * 2], xy[i * 2 + 1]);
	return s;
    }

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

    private static void simTests() {
	Coord isz43 = new Coord(4, 3);
	// two pieces: a 2x1 at (0,0)-(1,0) and a 1x1 at (2,0)
	Coord[] slots = coords(2,1, 1,1);
	Coord[] cur = coords(0,0, 2,0);

	// 1: a take off an empty cursor works, a take with a full one does not
	InventoryMoves.Sim s1 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	check("1a take with an empty cursor", Boolean.TRUE, s1.take(0));
	check("1b take with a full cursor is refused", Boolean.FALSE, s1.take(1));

	// 2: a drop into an empty rect works
	InventoryMoves.Sim s2 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	s2.take(0);
	check("2a drop into an empty rect", Boolean.TRUE, s2.drop(new Coord(0, 1)));
	check("2b the cursor is empty afterwards", -1, s2.hand);
	check("2c the item is where it was dropped", new Coord(0, 1), s2.pos[0]);

	// 3: a drop out of bounds is refused
	InventoryMoves.Sim s3 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	s3.take(0);
	check("3 drop past the right edge is refused", Boolean.FALSE, s3.drop(new Coord(3, 0)));

	// 4: a drop onto a masked cell is refused
	InventoryMoves.Sim s4 = new InventoryMoves.Sim(isz43, grid(4, 3, 0,2), slots, cur);
	s4.take(0);
	check("4 drop onto a masked cell is refused", Boolean.FALSE, s4.drop(new Coord(0, 2)));

	// 5: 1x1 onto 1x1 swaps - the mechanism the chain rides on, and the
	// only swap the planner is allowed to use
	Coord[] sl5 = coords(rep(2, 1, 1));
	Coord[] cur5 = coords(0,0, 1,0);
	InventoryMoves.Sim s5 = new InventoryMoves.Sim(isz43, grid(4, 3), sl5, cur5);
	s5.take(0);
	check("5a 1x1 dropped onto a 1x1 swaps", Boolean.TRUE, s5.drop(new Coord(1, 0)));
	check("5b the swapped item is now on the cursor", 1, s5.hand);
	check("5c the dropped item took its place", new Coord(1, 0), s5.pos[0]);

	// 6: 1x1 onto a multi-tile item does NOT swap. That is the conservative
	// policy: this swap is exactly the accident that leaves the multi-tile
	// item stuck on the cursor today.
	InventoryMoves.Sim s6 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	s6.take(1);
	check("6 1x1 dropped onto a multi-tile item is refused", Boolean.FALSE,
	      s6.drop(new Coord(0, 0)));

	// 7: a multi-tile item onto a single 1x1 does not swap either
	InventoryMoves.Sim s7 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	s7.take(0);
	check("7 a multi-tile item dropped onto a 1x1 is refused", Boolean.FALSE,
	      s7.drop(new Coord(2, 0)));

	// 8: two items underneath is never accepted
	Coord[] sl8 = coords(2,1, 1,1, 1,1);
	Coord[] cur8 = coords(0,2, 0,0, 1,0);
	InventoryMoves.Sim s8 = new InventoryMoves.Sim(isz43, grid(4, 3), sl8, cur8);
	s8.take(0);
	check("8 a drop covering two items is refused", Boolean.FALSE, s8.drop(new Coord(0, 0)));
    }

    /**
     * Replays a plan against a fresh simulator and returns the first thing that
     * went wrong, or null. This is the whole contract in one place: no refusal,
     * one take per item, cursor empty at the end, and every item exactly where
     * the plan said - pinned items included, which have to be untouched rather
     * than merely close.
     */
    private static String replay(boolean[][] mask, Coord isz, Coord[] slots, Coord[] current,
				 InventoryMoves.Plan plan) {
	InventoryMoves.Sim sim = new InventoryMoves.Sim(isz, mask, slots, current);
	boolean[] taken = new boolean[slots.length];
	for (InventoryMoves.Op op : plan.ops) {
	    if (op.kind == InventoryMoves.TAKE) {
		if (taken[op.item]) return "item " + op.item + " taken twice";
		taken[op.item] = true;
		// the widget reference the executor holds was captured before
		// the sort started, and moving an item destroys and recreates
		// its widget - so a take is only ever valid while the item is
		// still on the cell it started from
		if (!current[op.item].equals(sim.pos[op.item]))
		    return "item " + op.item + " taken after it had already moved";
		if (!sim.take(op.item)) return "take " + op.item + " refused";
	    } else {
		if (!sim.drop(op.cell)) return "drop at " + op.cell + " refused";
	    }
	}
	if (sim.hand >= 0) return "cursor still holds item " + sim.hand;
	for (int i = 0; i < slots.length; i++) {
	    Coord want = (plan.targets[i] != null) ? plan.targets[i] : current[i];
	    if (!want.equals(sim.pos[i]))
		return "item " + i + " ended at " + sim.pos[i] + ", wanted " + want;
	}
	return null;
    }

    /** How many items a plan left pinned - i.e. how many null entries in targets. */
    private static int pinnedCount(Coord[] targets) {
	int n = 0;
	for (Coord t : targets) if (t == null) n++;
	return n;
    }

    private static void planTests() {
	Coord isz43 = new Coord(4, 3);

	// 9: a 4x3 cupboard packed to the last cell, with a 2x1 that has to
	// leave (2,2). This is the user's report: today eviction finds no free
	// cell, warns "inventory too full", and phase 2 leaves the multi-tile
	// item on the cursor. The plan has to come out without a single refusal.
	Coord[] sl9 = coords(cat(rep(1, 2, 1), rep(10, 1, 1)));
	Coord[] cur9 = coords(2,2,
			      0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 3,1, 0,2, 1,2);
	InventoryMoves.Plan p9 = InventoryMoves.plan(grid(4, 3), isz43, sl9, cur9, false);
	check("9 full 4x3 with a 2x1 that has to move", null,
	      replay(grid(4, 3), isz43, sl9, cur9, p9));

	// 10: the same cupboard with one free cell. Today this case fails
	// silently - no message, and an item left on the cursor anyway.
	Coord[] sl10 = coords(cat(rep(1, 2, 1), rep(9, 1, 1)));
	Coord[] cur10 = coords(2,2,
			       0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 0,2, 1,2);
	InventoryMoves.Plan p10 = InventoryMoves.plan(grid(4, 3), isz43, sl10, cur10, false);
	check("10 same cupboard with one free cell", null,
	      replay(grid(4, 3), isz43, sl10, cur10, p10));

	// 11: twelve 1x1 in a full 4x3, in reverse order. A pure cycle: with no
	// free cell at all, the swap chain is the only way out.
	Coord[] sl11 = coords(rep(12, 1, 1));
	Coord[] cur11 = coords(3,2, 2,2, 1,2, 0,2, 3,1, 2,1, 1,1, 0,1, 3,0, 2,0, 1,0, 0,0);
	InventoryMoves.Plan p11 = InventoryMoves.plan(grid(4, 3), isz43, sl11, cur11, false);
	check("11 a full grid of 1x1 in reverse order", null,
	      replay(grid(4, 3), isz43, sl11, cur11, p11));

	// 12: two multi-tile items contending for the same spot. Solving their
	// chained movement is out of scope - but the plan is still required to
	// be executable, pinning whatever cannot be moved.
	Coord[] sl12 = coords(2,1, 2,1);
	Coord[] cur12 = coords(2,0, 0,1);
	InventoryMoves.Plan p12 = InventoryMoves.plan(grid(4, 3), isz43, sl12, cur12, false);
	check("12 two multi-tile items contending for one spot", null,
	      replay(grid(4, 3), isz43, sl12, cur12, p12));

	// 13: nothing to do produces an empty plan, not a pointless round trip
	Coord[] sl13 = coords(rep(2, 1, 1));
	Coord[] cur13 = coords(0,0, 1,0);
	InventoryMoves.Plan p13 = InventoryMoves.plan(grid(4, 3), isz43, sl13, cur13, false);
	check("13a an already sorted inventory plans no moves", 0, p13.ops.size());
	check("13b and reports nothing pinned", Boolean.FALSE, p13.pinnedMulti);

	// 14: vertical mode is held to the same contract
	InventoryMoves.Plan p14 = InventoryMoves.plan(grid(4, 3), isz43, sl9, cur9, true);
	check("14 full 4x3 with a 2x1, vertical fill", null,
	      replay(grid(4, 3), isz43, sl9, cur9, p14));

	// 15: a 2x1 whose target covers its own current cell - exactly the case
	// where single() saw the item itself as the "occupant" and never freed
	// the spot. Ignoring the item that is about to be picked up is what
	// makes the take possible; without that the item stayed pinned forever,
	// even with room to spare.
	Coord[] sl15 = coords(2,1);
	Coord[] cur15 = coords(1,0);
	InventoryMoves.Plan p15 = InventoryMoves.plan(grid(4, 3), isz43, sl15, cur15, false);
	check("15a a target overlapping the item's own current cell is still assigned",
	      Boolean.TRUE, p15.targets[0] != null);
	check("15b and the item actually leaves where it started", Boolean.TRUE,
	      p15.targets[0] != null && !p15.targets[0].equals(cur15[0]));

	// 16: a packed 3x2 grid with one 2x1 and four 1x1. The chain that swaps
	// the 1x1 among themselves runs, halfway through, into a target the 2x1
	// still occupies - and the 2x1 only leaves later, once (never, on a grid
	// this tight) there is room for it. Committing the chain without trying
	// it first, the old code blamed the swap victim for the block and pinned
	// the wrong item; nearly the whole grid came out pinned. Deferring the
	// chain instead, only the 2x1 - which genuinely fits nowhere on a full
	// grid - is left behind.
	Coord isz32 = new Coord(3, 2);
	Coord[] sl16 = coords(2,1, 1,1, 1,1, 1,1, 1,1);
	Coord[] cur16 = coords(1,0, 0,1, 1,1, 2,1, 0,0);
	InventoryMoves.Plan p16 = InventoryMoves.plan(grid(3, 2), isz32, sl16, cur16, false);
	check("16a a chain blocked by an unmoved multi-tile item still sorts", null,
	      replay(grid(3, 2), isz32, sl16, cur16, p16));
	check("16b and only the multi-tile item ends up pinned", 1, pinnedCount(p16.targets));

	// 17: the same packed grid as test 9, but now checking that it actually
	// sorts - not merely that it runs without a refusal. This is the
	// assertion that would have caught the whole defect: before the fixes,
	// plan() passed replay() because pinning an item is never a refusal, and
	// still returned with nearly the whole grid pinned instead of sorted.
	check("17 a packed grid with one multi-tile item pins at most it", Boolean.TRUE,
	      pinnedCount(p9.targets) <= 1);
    }

    /**
     * Builds a layout with `nmulti` multi-tile items and 1x1 filling the rest
     * except `freeCells`. `mask` cells are treated as already occupied, so
     * neither the multi-tile items nor the 1x1 filler ever land on one -
     * exactly what a real container with permanently blocked squares
     * (Inventory.sqmask) does. Returns {slots, current} or null if the
     * multi-tile items would not fit - the caller just skips those draws.
     */
    private static Coord[][] randomLayout(Coord isz, int nmulti, int freeCells,
					  boolean[][] mask, java.util.Random rnd) {
	Coord[] shapes = coords(2,1, 1,2, 2,2, 3,1);
	java.util.List<Coord> sl = new java.util.ArrayList<>();
	java.util.List<Coord> at = new java.util.ArrayList<>();
	boolean[][] used = new boolean[isz.x][isz.y];
	for (int x = 0; x < isz.x; x++)
	    used[x] = java.util.Arrays.copyOf(mask[x], isz.y);
	for (int i = 0; i < nmulti; i++) {
	    Coord sz = shapes[rnd.nextInt(shapes.length)];
	    boolean placed = false;
	    for (int tries = 0; tries < 60 && !placed; tries++) {
		int x = rnd.nextInt(Math.max(1, isz.x - sz.x + 1));
		int y = rnd.nextInt(Math.max(1, isz.y - sz.y + 1));
		if (x + sz.x > isz.x || y + sz.y > isz.y) continue;
		boolean ok = true;
		for (int dx = 0; dx < sz.x && ok; dx++)
		    for (int dy = 0; dy < sz.y && ok; dy++)
			if (used[x + dx][y + dy]) ok = false;
		if (!ok) continue;
		for (int dx = 0; dx < sz.x; dx++)
		    for (int dy = 0; dy < sz.y; dy++)
			used[x + dx][y + dy] = true;
		sl.add(sz);
		at.add(new Coord(x, y));
		placed = true;
	    }
	    if (!placed) return null;
	}
	java.util.List<Coord> free = new java.util.ArrayList<>();
	for (int y = 0; y < isz.y; y++)
	    for (int x = 0; x < isz.x; x++)
		if (!used[x][y]) free.add(new Coord(x, y));
	if (free.size() < freeCells) return null;
	java.util.Collections.shuffle(free, rnd);
	for (int i = freeCells; i < free.size(); i++) {
	    sl.add(new Coord(1, 1));
	    at.add(free.get(i));
	}
	return new Coord[][] {sl.toArray(new Coord[0]), at.toArray(new Coord[0])};
    }

    /**
     * One sweep configuration; reports the first layout that fails, if any
     * does. "Fails" covers two things, not one: a refusal (replay != null),
     * and a plan that is fine in shape but wrong in substance - pinning more
     * items than there are multi-tile items in the layout. That second check
     * is the reason the sweep exists: Task 3 already shipped a planner that
     * passed replay() every time, because a plan that moves nothing is never
     * refused, and still pinned 21 of 22 items in a packed cupboard. Only
     * counting pinned items caught that; a sweep calling replay() alone would
     * have let it through.
     */
    private static void sweep(String name, Coord isz, int nmulti, int freeCells,
			      boolean vertical, long seed) {
	// an invariant measured empirically over these same configurations
	// (the observed maximum never exceeded nmulti, over 2000 layouts each):
	// only a multi-tile item is left behind, never a 1x1. For nmulti == 0
	// this becomes "nothing is pinned", which is the pure-permutation case.
	sweep(name, isz, nmulti, freeCells, vertical, seed, grid(isz.x, isz.y), nmulti);
    }

    /**
     * As above, but with a mask - the same grid is threaded through both
     * plan() and replay(), which is the whole point: passing an unmasked grid
     * to either one would make a masked case pass for free instead of
     * actually exercising the mask. `pinnedBound` is a separate parameter
     * rather than always `nmulti` because a masked bound has to be measured,
     * not assumed - see sweepTests() for the measurements behind the calls
     * below.
     */
    private static void sweep(String name, Coord isz, int nmulti, int freeCells,
			      boolean vertical, long seed, boolean[][] mask, int pinnedBound) {
	java.util.Random rnd = new java.util.Random(seed);
	int runs = 2000, drawn = 0;
	String first = null;
	for (int r = 0; r < runs && first == null; r++) {
	    Coord[][] layout = randomLayout(isz, nmulti, freeCells, mask, rnd);
	    if (layout == null) continue;
	    drawn++;
	    Coord[] slots = layout[0], cur = layout[1];
	    InventoryMoves.Plan p = InventoryMoves.plan(mask, isz, slots, cur, vertical);
	    String bad = replay(mask, isz, slots, cur, p);
	    if (bad == null) {
		int pinned = pinnedCount(p.targets);
		if (pinned > pinnedBound)
		    bad = "pinned " + pinned + " items, more than the bound of " + pinnedBound;
	    }
	    if (bad != null) first = "layout " + r + ": " + bad;
	}
	// only report "too few usable layouts" if nothing more specific has
	// already failed - a real defect stops the loop within the first few
	// draws, and that low count must not erase the index and the cause that
	// were already found
	if (first == null && drawn < runs / 2)
	    first = "only " + drawn + " of " + runs + " layouts were usable";
	check(name, null, first);
    }

    private static void sweepTests() {
	Coord isz56 = new Coord(5, 6);
	Coord isz43 = new Coord(4, 3);
	for (int free : new int[] {0, 1, 3, 8}) {
	    sweep("18." + free + "h sweep 5x6, 2 multi, " + free + " free, horizontal",
		  isz56, 2, free, false, 42 + free);
	    sweep("18." + free + "v sweep 5x6, 2 multi, " + free + " free, vertical",
		  isz56, 2, free, true, 42 + free);
	}
	sweep("19a sweep 5x6, no multi, packed", isz56, 0, 0, false, 7);
	sweep("19b sweep 4x3, 1 multi, packed", isz43, 1, 0, false, 9);
	sweep("19c sweep 4x3, 1 multi, packed, vertical", isz43, 1, 0, true, 9);

	// 20.x/21.x: the same 2000 draws as before, but now with masked cells -
	// the only mask coverage that goes through plan() instead of poking
	// Sim.drop() directly (see simTests case 4). The mask is the same one in
	// randomLayout (via used) and in plan/replay below: neither of them sees
	// a freer grid than the other.
	//
	// The pinned-item bound was measured before it became an assertion, the
	// same way the unmasked bound was: by instrumenting a throwaway copy of
	// these same configurations for 2000 draws each. In all of them the
	// observed maximum was exactly nmulti (never more), so the bound below
	// is the usual one, only now verified under a mask instead of assumed.
	boolean[][] mask56 = grid(5, 6, 2,2, 4,0, 0,5, 1,3, 3,1, 4,5);
	sweep("20a masked sweep 5x6, 2 multi, 3 free, horizontal",
	      isz56, 2, 3, false, 55, mask56, 2);
	sweep("20b masked sweep 5x6, 2 multi, 3 free, vertical",
	      isz56, 2, 3, true, 55, mask56, 2);
	// packed to the last free cell - the case that squeezes hardest on the
	// interaction between assignTargets (which picks targets seeing the
	// mask) and Sim (which refuses moves seeing that same mask)
	sweep("20c masked sweep 5x6, 2 multi, 0 free, horizontal",
	      isz56, 2, 0, false, 55, mask56, 2);
	sweep("20d masked sweep 5x6, 2 multi, 0 free, vertical",
	      isz56, 2, 0, true, 55, mask56, 2);

	boolean[][] mask43 = grid(4, 3, 2,1, 0,2);
	sweep("21a masked sweep 4x3, 1 multi, packed, horizontal",
	      isz43, 1, 0, false, 77, mask43, 1);
	sweep("21b masked sweep 4x3, 1 multi, packed, vertical",
	      isz43, 1, 0, true, 77, mask43, 1);
    }

    public static void main(String[] args) {
	simTests();
	System.out.println();
	planTests();
	System.out.println();
	sweepTests();
	System.out.println();
	System.out.println(failures == 0
			   ? (total + " of " + total + " passed")
			   : (failures + " of " + total + " failed"));
	System.exit(failures == 0 ? 0 : 1);
    }
}
