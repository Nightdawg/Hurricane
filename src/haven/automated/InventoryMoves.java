package haven.automated;

import haven.Coord;

import java.util.*;

/**
 * Turns "where every item should end up" into "which messages to send".
 *
 * Widget messages are reliable and ordered (Connection.java:340,795 on the way
 * out, 494-505 on the way in), so the server applies take/drop in the order
 * they were sent. The client therefore never has to wait for a reply - it has
 * to send a sequence the server will accept. That is what this class produces:
 * every move is checked against a model of the server's own rules before it is
 * emitted, so a refusal is impossible rather than merely unlikely.
 *
 * Depends on nothing but haven.Coord, like InventoryLayout, so it stays
 * testable without a resource server.
 */
class InventoryMoves {
    static final int TAKE = 0, DROP = 1;

    /** One message. TAKE carries an item index, DROP a cell. */
    static class Op {
	final int kind;
	final int item;
	final Coord cell;

	private Op(int kind, int item, Coord cell) {
	    this.kind = kind;
	    this.item = item;
	    this.cell = cell;
	}

	static Op take(int item) { return new Op(TAKE, item, null); }
	static Op drop(Coord cell) { return new Op(DROP, -1, cell); }

	public String toString() {
	    return (kind == TAKE) ? ("take " + item) : ("drop " + cell);
	}
    }

    /**
     * The server's view of one inventory, as far as take and drop are
     * concerned. The cursor starts empty; pos[i] == null thereafter arises
     * only through take, so current must be fully populated - a Sim built
     * from a state where an item is already mid-drag would think the cursor
     * is empty and allow a second take, reaching a state a single int hand
     * cannot represent.
     */
    static class Sim {
	final Coord isz;
	final boolean[][] mask;
	final Coord[] slots;
	final Coord[] pos;
	int hand = -1;

	/**
	 * isz, mask, slots and the Coord objects inside them (including
	 * current) are held, not copied - only the pos array itself is a
	 * fresh copy. That is safe because nothing in this class ever writes
	 * a Coord's x or y field; every move here is reference reassignment
	 * (pos[i] = ...), never mutation. Callers must not mutate a Coord (or
	 * mask, or slots) they have handed to a live Sim: haven.Coord is
	 * mutable and in-place mutation is an idiom elsewhere in this
	 * codebase, but InventorySorter's coords come from sub/div, which
	 * always return fresh objects, so nothing here actually does that.
	 */
	Sim(Coord isz, boolean[][] mask, Coord[] slots, Coord[] current) {
	    this.isz = isz;
	    this.mask = mask;
	    this.slots = slots;
	    this.pos = Arrays.copyOf(current, current.length);
	}

	static boolean one(Coord s) { return s.x == 1 && s.y == 1; }

	/** Index of the item covering a cell, or -1. */
	int at(int x, int y) {
	    for (int i = 0; i < pos.length; i++) {
		if (pos[i] == null) continue;
		if (x >= pos[i].x && x < pos[i].x + slots[i].x
		    && y >= pos[i].y && y < pos[i].y + slots[i].y)
		    return i;
	    }
	    return -1;
	}

	/** Distinct items under a rect, or null if it leaves the grid or hits the mask. */
	private Set<Integer> under(Coord p, Coord sz) {
	    if (p.x < 0 || p.y < 0 || p.x + sz.x > isz.x || p.y + sz.y > isz.y)
		return null;
	    Set<Integer> out = new LinkedHashSet<>();
	    for (int x = p.x; x < p.x + sz.x; x++)
		for (int y = p.y; y < p.y + sz.y; y++) {
		    if (mask[x][y]) return null;
		    int o = at(x, y);
		    if (o >= 0) out.add(o);
		}
	    return out;
	}

	// An out-of-range item is a caller bug, not a refused move, so this
	// throws rather than returning false like drop does - collapsing the
	// two would let a planner indexing error masquerade as "that move
	// wasn't possible" and quietly yield a worse plan instead of failing
	// loudly in the tests.
	boolean take(int item) {
	    if (hand >= 0 || pos[item] == null) return false;
	    hand = item;
	    pos[item] = null;
	    return true;
	}

	/**
	 * Drops what is held. An empty rect always accepts. A rect covering
	 * exactly one item swaps, but only between two 1x1 items - the case the
	 * sorter's chain has always relied on and which demonstrably works.
	 * Anything involving a multi-tile item is refused: whether the server
	 * swaps in that direction has never been exercised, and guessing wrong
	 * is what leaves a large item stuck on the cursor.
	 */
	boolean drop(Coord to) {
	    if (hand < 0) return false;
	    Set<Integer> u = under(to, slots[hand]);
	    if (u == null) return false;
	    if (u.isEmpty()) {
		pos[hand] = to;
		hand = -1;
		return true;
	    }
	    if (u.size() != 1) return false;
	    int other = u.iterator().next();
	    if (!one(slots[hand]) || !one(slots[other])) return false;
	    int held = hand;
	    pos[other] = null;
	    pos[held] = to;
	    hand = other;
	    return true;
	}
    }

    /** A move list, the layout it realises, and whether anything had to stay put. */
    static class Plan {
	final List<Op> ops;
	final Coord[] targets;      // null at i: item i is pinned where it is
	final boolean pinnedMulti;  // a multi-tile item had to stay put

	Plan(List<Op> ops, Coord[] targets, boolean pinnedMulti) {
	    this.ops = ops;
	    this.targets = targets;
	    this.pinnedMulti = pinnedMulti;
	}
    }

    /**
     * The move list that takes the inventory from `current` to the layout
     * InventoryLayout picks, or as close to it as can actually be carried out.
     *
     * An item can fit its target and still be impossible to carry there - a
     * large item whose destination is blocked by another large item, for
     * instance. When that happens the item is pinned and the layout is
     * recomputed around it, which is the same pin-and-restart loop
     * assignTargets already runs for items that do not fit at all. Each restart
     * pins one more item and the all-pinned state is the original layout, so
     * this terminates in at most n passes.
     */
    static Plan plan(boolean[][] mask, Coord isz, Coord[] slots,
		     Coord[] current, boolean vertical) {
	boolean[] pinned = new boolean[slots.length];
	for (int attempt = 0; attempt <= slots.length; attempt++) {
	    Coord[] targets = InventoryLayout.assignTargets(mask, isz, slots, current,
							    vertical, pinned);
	    List<Op> ops = new ArrayList<>();
	    int stuck = sequence(mask, isz, slots, current, targets, ops);
	    if (stuck < 0) {
		boolean multi = false;
		for (int i = 0; i < slots.length; i++)
		    if ((targets[i] == null) && !Sim.one(slots[i]))
			multi = true;
		return new Plan(ops, targets, multi);
	    }
	    pinned[stuck] = true;
	}
	throw new AssertionError("pin-and-restart did not converge");
    }

    /**
     * Emits the moves realising `targets` into `ops`. Returns -1 on success, or
     * the index of an item that cannot be carried to its target.
     *
     * Two move shapes, and no others:
     *
     *  - direct: the target rect is empty, so take and drop land cleanly. This
     *    is the only shape a multi-tile item ever gets.
     *  - chain: a 1x1 whose target holds exactly one other 1x1. Dropping swaps,
     *    the displaced item comes up on the cursor, and it is dropped on its own
     *    target in turn. The cycle closes on the hole the first take opened.
     *
     * Preferring direct moves is what removes the old eviction step entirely:
     * an item moved to its own target is never in the way again, whereas the
     * old code shoved items into whatever cell was free - including cells
     * inside the very rect it was trying to clear.
     */
    private static int sequence(boolean[][] mask, Coord isz, Coord[] slots,
				Coord[] current, Coord[] targets, List<Op> ops) {
	Sim sim = new Sim(isz, mask, slots, current);
	boolean[] done = new boolean[slots.length];
	for (int i = 0; i < slots.length; i++)
	    done[i] = (targets[i] == null) || targets[i].equals(current[i]);

	for (;;) {
	    int direct = -1, chain = -1;
	    for (int i = 0; i < slots.length; i++) {
		if (done[i]) continue;
		int u = single(sim, targets[i], slots[i]);
		if (u == EMPTY) {
		    // a multi-tile item only ever gets this shape, so give it
		    // the free rect before a 1x1 can settle into it
		    if (!Sim.one(slots[i])) { direct = i; break; }
		    if (direct < 0) direct = i;
		} else if (u >= 0 && Sim.one(slots[i]) && Sim.one(slots[u]) && chain < 0) {
		    chain = i;
		}
	    }

	    int start = (direct >= 0) ? direct : chain;
	    if (start < 0) break;

	    if (!sim.take(start)) return start;
	    ops.add(Op.take(start));
	    while (sim.hand >= 0) {
		int held = sim.hand;
		if (targets[held] == null || !sim.drop(targets[held])) return held;
		ops.add(Op.drop(targets[held]));
		done[held] = true;
	    }
	}

	for (int i = 0; i < slots.length; i++)
	    if (!done[i]) {
		// prefer pinning a multi-tile item: it is the one whose rect
		// blocks everything else, and pinning a 1x1 would not unstick it
		if (!Sim.one(slots[i])) return i;
	    }
	for (int i = 0; i < slots.length; i++)
	    if (!done[i]) return i;
	return -1;
    }

    private static final int EMPTY = -1, MANY = -2;

    /**
     * The single item under `at`, EMPTY if the rect is clear, or MANY if it is
     * unusable - more than one item, off the grid, or masked.
     */
    private static int single(Sim sim, Coord at, Coord sz) {
	if (at == null) return MANY;
	if (at.x < 0 || at.y < 0 || at.x + sz.x > sim.isz.x || at.y + sz.y > sim.isz.y)
	    return MANY;
	int found = EMPTY;
	for (int x = at.x; x < at.x + sz.x; x++)
	    for (int y = at.y; y < at.y + sz.y; y++) {
		if (sim.mask[x][y]) return MANY;
		int o = sim.at(x, y);
		if (o < 0) continue;
		if (found != EMPTY && found != o) return MANY;
		found = o;
	    }
	return found;
    }
}
