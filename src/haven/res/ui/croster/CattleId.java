/* Preprocessed source code */
package haven.res.ui.croster;

import haven.*;
import haven.render.*;
import java.util.*;
import java.util.function.*;
import haven.MenuGrid.Pagina;
import java.awt.Color;
import java.awt.image.BufferedImage;

import static haven.PUtils.blurmask2;
import static haven.PUtils.rasterimg;

@haven.FromResource(name = "ui/croster", version = 77)
public class CattleId extends GAttrib implements RenderTree.Node, PView.Render2D {
    public final UID id;
	public static final Text.Foundry fnd = new Text.Foundry(Text.dfont, 12);
	public static final Tex selectionMark = Resource.loadtex("customclient/cattleroster/selectionMark");

    public CattleId(Gob gob, UID id) {
	super(gob);
	this.id = id;
    }

    public static void parse(Gob gob, Message dat) {
	UID id = UID.of(dat.int64());
	gob.setattr(new CattleId(gob, id));
    }

    private int rmseq = 0, entryseq = 0;
    private RosterWindow wnd = null;
    private CattleRoster<?> roster = null;
    private Entry entry = null;
    public Entry entry() {
	if((entry == null) || ((roster != null) && (roster.entryseq != entryseq))) {
	    if(rmseq != RosterWindow.rmseq) {
		synchronized(RosterWindow.rosters) {
		    RosterWindow wnd = RosterWindow.rosters.get(gob.glob);
		    if(wnd != null) {
			for(CattleRoster<?> ch : wnd.children(CattleRoster.class)) {
			    if(ch.entries.get(this.id) != null) {
				this.wnd = wnd;
				this.roster = ch;
				this.rmseq = RosterWindow.rmseq;
				break;
			    }
			}
		    }
		}
	    }
	    if(roster != null)
		this.entry = roster.entries.get(this.id);
	}
	return(entry);
    }

    private String lnm;
    private int lgrp;
    private Tex rnm;
    public void draw(GOut g, Pipe state) {
	Coord sc = Homo3D.obj2view(new Coord3f(0, 0, 15), state, Area.sized(g.sz())).round2();
	if(sc.isect(Coord.z, g.sz())) {
	    Entry entry = entry();
	    int grp = (entry != null) ? entry.grp : 0;
	    String name = (entry != null) ? entry.name : null;
	    if((name != null) && ((rnm == null) || !name.equals(lnm) || (grp != lgrp))) {
		Color col = BuddyWnd.gc[grp];
		rnm = new TexI(rasterimg(blurmask2(Utils.outline2(fnd.render(name, col).img, Color.BLACK, true).getRaster(), UI.rscale(1.0), UI.rscale(1.0), Color.BLACK)));
		lnm = name;
		lgrp = grp;
	    }
	    if((rnm != null) && (wnd != null) && wnd.visible) {
		Coord nmc = sc.sub(rnm.sz().x / 2, 0);
		g.aimage(rnm, sc, 0.5, 1.0);
		if((entry != null) && entry.mark.a)
		    g.aimage(selectionMark, nmc.sub(selectionMark.sz().x, 0), 0.1, 1.3);
	    }
	}
    }
}
