package haven;

import java.awt.Color;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import haven.MapFile.PMarker;

import static haven.MCache.tilesz;

public class ProspectingWnd extends Window {
    private static final int GAP = UI.scale(5);
    private static final Pattern DIRECT_FIND =
	Pattern.compile("There appears to be (.*) directly below\\.");
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
	/* Preserve the find position if the player moves before marking it. */
	pc = playerc();
    }

    public <T extends Widget> T add(T child) {
	super.add(child);
	if((mark != null) && (child instanceof Label)) {
	    String substance = directFind(((Label)child).texts);
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
	mark.c = Coord.of(x + dismiss.sz.x + GAP,
			  dismiss.c.y + ((dismiss.sz.y - mark.sz.y) / 2));
    }

    private static String directFind(String text) {
	if(text == null)
	    return(null);
	Matcher m = DIRECT_FIND.matcher(text.trim());
	if(!m.matches())
	    return(null);
	String substance = m.group(1).trim();
	return(substance.isEmpty() ? null : substance);
    }

    private static String markerName(String substance) {
	StringBuilder buf = new StringBuilder();
	for(String word : substance.split(" ")) {
	    if(word.isEmpty())
		continue;
	    if(buf.length() > 0)
		buf.append(' ');
	    buf.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
	}
	return(buf.toString() + " (below)");
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
	String name = markerName(detected);
	/* Keep bulk prospecting markers off the ground. */
	file.add(new PMarker(file, loc.seg.id, tc, name,
			     BuddyWnd.gc[new Random().nextInt(BuddyWnd.gc.length)], false));
	gui.msg("Marked: " + name, Color.WHITE);
	mark.disable(true);
    }
}
