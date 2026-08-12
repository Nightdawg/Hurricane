package haven;

import java.awt.Color;
import java.util.Random;

import haven.MapFile.PMarker;

import static haven.MCache.tilesz;

/* The server's prospecting result window, with a Mark button added next to its
 * Dismiss button. The server builds the window in pieces - caption first, then
 * the label and the button, then a pack - so the button starts hidden and only
 * appears once a label proves the find is directly below us. */
public class ProspectingWnd extends Window {
    private static final int GAP = UI.scale(5);
    private final Button mark;
    private String detected = null;
    private Coord2d pc = null;

    public ProspectingWnd(Coord sz, String cap, boolean lg) {
	super(sz, cap, lg);
	mark = add(new Button(UI.scale(100), "Mark", false), UI.scale(105, 25));
	mark.action(this::mark);
	mark.hide();
    }

    protected void attach(UI ui) {
	super.attach(ui);
	/* Where we stood when the result came in - the player can walk off
	 * before clicking, and then the current position is not the find. */
	pc = playerc();
    }

    public <T extends Widget> T add(T child) {
	super.add(child);
	/* The constructor's own add() runs before mark is assigned. */
	if((mark != null) && (child instanceof Label)) {
	    String substance = ProspectingText.detect(((Label)child).texts);
	    if(substance != null) {
		detected = substance;
		mark.show();
	    }
	}
	return(child);
    }

    public void pack() {
	if(mark != null) {
	    for(Button btn : children(Button.class)) {
		if(btn != mark) {
		    layout(btn);
		    break;
		}
	    }
	}
	super.pack();
    }

    /* The server lays the window out for its one button and then packs it to
     * the text, so a long line leaves the button sitting off to the left -
     * with or without ours beside it. Centre the whole row under the text. */
    private void layout(Button dismiss) {
	int width = 0;
	for(Widget wdg = child; wdg != null; wdg = wdg.next) {
	    if((wdg == deco) || !wdg.visible || (wdg instanceof Button))
		continue;
	    width = Math.max(width, wdg.c.x + wdg.sz.x);
	}
	int roww = dismiss.sz.x + (mark.visible ? (GAP + mark.sz.x) : 0);
	int x = Math.max(0, (width - roww) / 2);
	dismiss.c = Coord.of(x, dismiss.c.y);
	/* A large button's box is taller than the frame it draws, which sits
	 * centred in it - so matching box tops would leave ours riding high.
	 * Centre the boxes instead and the frames line up whichever size the
	 * server's button is. */
	mark.c = Coord.of(x + dismiss.sz.x + GAP,
			  dismiss.c.y + ((dismiss.sz.y - mark.sz.y) / 2));
    }

    private Coord2d playerc() {
	if((ui == null) || (ui.gui == null) || (ui.gui.map == null))
	    return(null);
	Gob player = ui.gui.map.player();
	return((player == null) ? null : player.rc);
    }

    private void mark() {
	if(detected == null)
	    return;
	GameUI gui = (ui == null) ? null : ui.gui;
	if(gui == null)
	    return;
	if(pc == null)
	    pc = playerc();
	if(pc == null) {
	    gui.error("Prospecting: cannot tell where you are.");
	    return;
	}
	MapWnd mapfile = gui.mapfile;
	MiniMap.Location loc = (mapfile == null) ? null : mapfile.view.sessloc;
	if(loc == null) {
	    gui.error("Prospecting: the map is not loaded yet.");
	    return;
	}
	MapFile file = mapfile.file;
	Coord tc = loc.tc.add(pc.floor(tilesz));
	String name = ProspectingText.markerName(detected);
	/* onmap = false: no flag planted in the world. Prospecting is done in
	 * bulk, and a flag per find would litter the ground. The marker is
	 * still on the minimap and in the list, and "Display in world" turns
	 * the flag on for the ones worth seeing from afar. */
	file.add(new PMarker(file, loc.seg.id, tc, name,
			     BuddyWnd.gc[new Random().nextInt(BuddyWnd.gc.length)], false));
	/* The failures above all speak, so the success says its piece too -
	 * and it names the marker, which is how you find it on the map. */
	gui.msg("Marked: " + name, Color.WHITE);
	/* One find, one marker. Greyed out rather than hidden: a disabled
	 * button ignores clicks all the same, and hiding it would leave the
	 * hole its own width carved into the window. */
	mark.disable(true);
    }
}
