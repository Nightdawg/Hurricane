/* Preprocessed source code */
package haven.res.ui.obj.buddy;

import haven.*;
import haven.render.*;
import java.util.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import static haven.PUtils.*;

@haven.FromResource(name = "ui/obj/buddy", version = 4)
public class Buddy extends GAttrib implements InfoPart {
    public final int id;
    public final Info info;
    private int bseq = -1;
    private BuddyWnd bw = null;
    public BuddyWnd.Buddy b = null;
    public int rgrp = -1;
    public String rnm;
	public String customName = null;
	private Color customNameColor = Color.WHITE;

    public Buddy(Gob gob, int id) {
	super(gob);
	this.id = id;
	info = Info.add(gob, this);
    }

	public Buddy(Gob gob, int id, String customName, Color customNameColor, int rgrp) {
		super(gob);
		this.id = id;
		info = Info.add(gob, this);
		this.customName = customName;
		this.customNameColor = customNameColor;
        this.rgrp = rgrp;
	}

    public static void parse(Gob gob, Message dat) {
	int fl = dat.uint8();
	if((fl & 1) != 0)
	    gob.setattr(new Buddy(gob, dat.int32()));
	else {
		gob.delattr(Buddy.class);
	}
    }

    public void dispose() {
	super.dispose();
	info.remove(this);
    }

    public BuddyWnd.Buddy buddy() {
	return(b);
    }

    public void draw(CompImage cmp, RenderContext ctx) {
	if (customName != null) {
		cmp.add(InfoPart.rendertext(customName, customNameColor), Coord.z);
	} else {
	BuddyWnd.Buddy b = null;
	if(bw == null) {
	    if(ctx instanceof PView.WidgetContext) {
		GameUI gui = ((PView.WidgetContext)ctx).widget().getparent(GameUI.class);
		if(gui != null) {
		    if(gui.buddies == null)
			throw(new Loading());
		    bw = gui.buddies;
		}
	    }
	}
	if(bw != null)
	    b = bw.find(id);
	if(b != null) {
	    Color col = BuddyWnd.gc[rgrp = b.group];
	    cmp.add(InfoPart.rendertext(rnm = b.name, col), Coord.z);
		GameUI.gobIdToKinName.put(gob.id, rnm);
	}
	this.b = b;
	}
    }

    public void ctick(double dt) {
	super.ctick(dt);
	if((bw != null) && (bw.serial != bseq)) {
	    bseq = bw.serial;
	    if((bw.find(id) != b) || ((b != null) && (!(rnm.equals(b.name)) || (rgrp != b.group))))
		info.dirty();
	}
    }

    public boolean auto() {return(true);}
    public int order() {return(-10);}
}
