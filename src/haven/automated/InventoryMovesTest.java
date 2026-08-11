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
	// duas peças: um 2x1 em (0,0)-(1,0) e um 1x1 em (2,0)
	Coord[] slots = coords(2,1, 1,1);
	Coord[] cur = coords(0,0, 2,0);

	// 1: take de mão vazia funciona, take com a mão ocupada não
	InventoryMoves.Sim s1 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	check("1a take with an empty cursor", Boolean.TRUE, s1.take(0));
	check("1b take with a full cursor is refused", Boolean.FALSE, s1.take(1));

	// 2: drop em retângulo vazio funciona
	InventoryMoves.Sim s2 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	s2.take(0);
	check("2a drop into an empty rect", Boolean.TRUE, s2.drop(new Coord(0, 1)));
	check("2b the cursor is empty afterwards", -1, s2.hand);
	check("2c the item is where it was dropped", new Coord(0, 1), s2.pos[0]);

	// 3: drop fora dos limites é recusado
	InventoryMoves.Sim s3 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	s3.take(0);
	check("3 drop past the right edge is refused", Boolean.FALSE, s3.drop(new Coord(3, 0)));

	// 4: drop em célula mascarada é recusado
	InventoryMoves.Sim s4 = new InventoryMoves.Sim(isz43, grid(4, 3, 0,2), slots, cur);
	s4.take(0);
	check("4 drop onto a masked cell is refused", Boolean.FALSE, s4.drop(new Coord(0, 2)));

	// 5: 1x1 sobre 1x1 troca - o mecanismo da corrente, o único caso de
	// troca que o planejador tem permissão de usar
	Coord[] sl5 = coords(rep(2, 1, 1));
	Coord[] cur5 = coords(0,0, 1,0);
	InventoryMoves.Sim s5 = new InventoryMoves.Sim(isz43, grid(4, 3), sl5, cur5);
	s5.take(0);
	check("5a 1x1 dropped onto a 1x1 swaps", Boolean.TRUE, s5.drop(new Coord(1, 0)));
	check("5b the swapped item is now on the cursor", 1, s5.hand);
	check("5c the dropped item took its place", new Coord(1, 0), s5.pos[0]);

	// 6: 1x1 sobre item grande NÃO troca. É a política conservadora: essa
	// troca é justamente o acidente que hoje deixa o item grande na mão.
	InventoryMoves.Sim s6 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	s6.take(1);
	check("6 1x1 dropped onto a multi-tile item is refused", Boolean.FALSE,
	      s6.drop(new Coord(0, 0)));

	// 7: item grande sobre um único 1x1 também não troca
	InventoryMoves.Sim s7 = new InventoryMoves.Sim(isz43, grid(4, 3), slots, cur);
	s7.take(0);
	check("7 a multi-tile item dropped onto a 1x1 is refused", Boolean.FALSE,
	      s7.drop(new Coord(2, 0)));

	// 8: dois itens embaixo nunca aceita
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

    private static void planTests() {
	Coord isz43 = new Coord(4, 3);

	// 9: um armário 4x3 cheio até a última célula, com um 2x1 que precisa
	// sair de (2,2). É o relato do usuário: hoje a evicção não encontra
	// célula livre, avisa "inventory too full" e a fase 2 deixa o item
	// grande na mão. O plano tem de sair sem uma única recusa.
	Coord[] sl9 = coords(cat(rep(1, 2, 1), rep(10, 1, 1)));
	Coord[] cur9 = coords(2,2,
			      0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 3,1, 0,2, 1,2);
	InventoryMoves.Plan p9 = InventoryMoves.plan(grid(4, 3), isz43, sl9, cur9, false);
	check("9 full 4x3 with a 2x1 that has to move", null,
	      replay(grid(4, 3), isz43, sl9, cur9, p9));

	// 10: o mesmo armário com uma célula livre. Hoje esse caso falha em
	// silêncio - nenhuma mensagem, e mesmo assim um item no cursor.
	Coord[] sl10 = coords(cat(rep(1, 2, 1), rep(9, 1, 1)));
	Coord[] cur10 = coords(2,2,
			       0,0, 1,0, 2,0, 3,0, 0,1, 1,1, 2,1, 0,2, 1,2);
	InventoryMoves.Plan p10 = InventoryMoves.plan(grid(4, 3), isz43, sl10, cur10, false);
	check("10 same cupboard with one free cell", null,
	      replay(grid(4, 3), isz43, sl10, cur10, p10));

	// 11: doze 1x1 num 4x3 cheio, em ordem invertida. Puro ciclo: sem
	// nenhuma célula livre, a única saída é a corrente de trocas.
	Coord[] sl11 = coords(rep(12, 1, 1));
	Coord[] cur11 = coords(3,2, 2,2, 1,2, 0,2, 3,1, 2,1, 1,1, 0,1, 3,0, 2,0, 1,0, 0,0);
	InventoryMoves.Plan p11 = InventoryMoves.plan(grid(4, 3), isz43, sl11, cur11, false);
	check("11 a full grid of 1x1 in reverse order", null,
	      replay(grid(4, 3), isz43, sl11, cur11, p11));

	// 12: dois itens grandes disputando lugar. Fora de escopo resolver o
	// movimento em cadeia deles - mas o plano continua obrigado a ser
	// executável, fixando o que não dá para mover.
	Coord[] sl12 = coords(2,1, 2,1);
	Coord[] cur12 = coords(2,0, 0,1);
	InventoryMoves.Plan p12 = InventoryMoves.plan(grid(4, 3), isz43, sl12, cur12, false);
	check("12 two multi-tile items contending for one spot", null,
	      replay(grid(4, 3), isz43, sl12, cur12, p12));

	// 13: nada a fazer produz plano vazio, não uma volta inútil
	Coord[] sl13 = coords(rep(2, 1, 1));
	Coord[] cur13 = coords(0,0, 1,0);
	InventoryMoves.Plan p13 = InventoryMoves.plan(grid(4, 3), isz43, sl13, cur13, false);
	check("13a an already sorted inventory plans no moves", 0, p13.ops.size());
	check("13b and reports nothing pinned", Boolean.FALSE, p13.pinnedMulti);

	// 14: modo vertical passa pelo mesmo contrato
	InventoryMoves.Plan p14 = InventoryMoves.plan(grid(4, 3), isz43, sl9, cur9, true);
	check("14 full 4x3 with a 2x1, vertical fill", null,
	      replay(grid(4, 3), isz43, sl9, cur9, p14));
    }

    public static void main(String[] args) {
	simTests();
	System.out.println();
	planTests();
	System.out.println();
	System.out.println(failures == 0
			   ? (total + " of " + total + " passed")
			   : (failures + " of " + total + " failed"));
	System.exit(failures == 0 ? 0 : 1);
    }
}
