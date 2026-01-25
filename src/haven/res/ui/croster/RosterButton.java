/* Preprocessed source code */
package haven.res.ui.croster;

import haven.*;
import haven.render.*;
import java.util.*;
import java.util.function.*;
import haven.MenuGrid.Pagina;
import java.awt.Color;
import java.awt.image.BufferedImage;

@haven.FromResource(name = "ui/croster", version = 77)
public class RosterButton extends MenuGrid.PagButton {
    public final GameUI gui;
    public RosterWindow wnd;

    public RosterButton(Pagina pag) {
	super(pag);
	gui = pag.scm.getparent(GameUI.class);
    }

    public static class Fac implements Factory {
	public MenuGrid.PagButton make(Pagina pag) {
	    return(new RosterButton(pag));
	}
    }

    public void add(CattleRoster rost) {
	if(wnd == null) {
	    wnd = new RosterWindow();
	    wnd.addroster(rost);
		gui.addchild(wnd, "misc", new Coord(0, 0), new Object[] {"id", "croster"});
	    synchronized(RosterWindow.rosters) {
		RosterWindow.rosters.put(pag.scm.ui.sess.glob, wnd);
	    }
	} else {
	    wnd.addroster(rost);
	}
    }

    public void use(MenuGrid.Interaction iact) {
	if(pag.scm.ui.modshift) {
	    pag.scm.wdgmsg("act", "croster", "a");
	} else if(wnd == null) {
	    pag.scm.wdgmsg("act", "croster");
	} else {
	    if(wnd.show(!wnd.visible)) {
		wnd.raise();
		gui.setfocus(wnd);
	    }
	}
    }
}

/* >objdelta: CattleId */
