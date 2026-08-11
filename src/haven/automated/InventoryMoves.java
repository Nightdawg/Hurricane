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
     * concerned. pos[i] == null means item i is on the cursor.
     */
    static class Sim {
	final Coord isz;
	final boolean[][] mask;
	final Coord[] slots;
	final Coord[] pos;
	int hand = -1;

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
}
