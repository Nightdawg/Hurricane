package haven;

import java.util.Random;

import haven.MapFile.PMarker;

import static haven.MCache.tilesz;

/* The server's prospecting result window, with a Mark button added next to its
 * Dismiss button. The server builds the window in pieces - caption first, then
 * the label and the button, then a pack - so the button starts hidden and only
 * appears once a label proves the find is directly below us. */
public class ProspectingWnd extends Window {
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
		    /* A large button's box is taller than the frame it draws,
		     * which sits centred in it - so matching box tops would
		     * leave ours riding high. Centre the boxes instead and the
		     * frames line up whichever size the server's button is. */
		    mark.c = Coord.of(mark.c.x, btn.c.y + ((btn.sz.y - mark.sz.y) / 2));
		    break;
		}
	    }
	}
	super.pack();
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
	file.add(new PMarker(file, loc.seg.id, tc, ProspectingText.markerName(detected),
			     BuddyWnd.gc[new Random().nextInt(BuddyWnd.gc.length)], true));
	/* One find, one marker. Greyed out rather than hidden: a disabled
	 * button ignores clicks all the same, and hiding it would leave the
	 * hole its own width carved into the window. */
	mark.disable(true);
    }
}
