/* Preprocessed source code */
package haven.res.ui.grainslot;

import haven.*;
import haven.automated.helpers.FarmingStatic;

import java.util.*;
import static haven.Inventory.invsq;

/* >wdg: Grainslot */
@haven.FromResource(name = "ui/grainslot", version = 21)
public class Grainslot extends Widget implements DTarget, ItemInfo.Owner {
    public final Label lbl;
    public final Button tbtn, pbtn, ebtn, ybtn;
    public Indir<Resource> icon;
    private ItemInfo.Raw rawinfo = null;
    private Tex iconc;
	public boolean autoTake = false;

    public Grainslot() {
	super(UI.scale(390, 40));
	lbl = adda(new Label(""), sz.y, sz.y / 2, 0, 0.5);
	int w = UI.scale(45), m = UI.scale(5);
	ebtn = adda(new Button(w, "Empty", () -> wdgmsg("empty")), sz.x-UI.scale(5), sz.y / 2, 1.0, 0.5);
	tbtn = adda(new Button(w, "Take", () -> wdgmsg("take")), ebtn.c.x - m, sz.y / 2, 1.0, 0.5);
	pbtn = adda(new Button(w, "Put", this::putUpgraded), tbtn.c.x - m, sz.y / 2, 1.0, 0.5);
	ybtn = adda(new Button(w, "Auto", this::takeFromHere), pbtn.c.x - m, sz.y / 2, 1, 0.5);
	ebtn.hide(); tbtn.hide(); pbtn.hide();
    }

    public static Widget mkwidget(UI ui, Object... args) {
	return(new Grainslot());
    }

    public void draw(GOut g) {
	int ic = (sz.y - invsq.sz().y) / 2;
	g.image(invsq, new Coord(ic, ic));
	if(icon != null) {
	    try {
		if(iconc == null)
		    iconc = icon.get().layer(Resource.imgc).tex();
		g.image(iconc, new Coord(ic + 1, ic + 1));
	    } catch(Loading l) {
	    }
	}
	super.draw(g);
    }

    private static final OwnerContext.ClassResolver<Widget> ctxr = new OwnerContext.ClassResolver<Widget>()
	.add(Glob.class, wdg -> wdg.ui.sess.glob)
	.add(Session.class, wdg -> wdg.ui.sess);
    public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

    private List<ItemInfo> info = Collections.emptyList();
    public List<ItemInfo> info() {
	if(info == null)
	    info = ItemInfo.buildinfo(this, rawinfo);
	return(info);
    }

    private double hoverstart;
    private Tex shorttip, longtip;
    public Object tooltip(Coord c, Widget prev) {
	int ic = (sz.y - invsq.sz().y) / 2;
	if((rawinfo != null) && c.isect(new Coord(ic, ic), invsq.sz())) {
	    double now = Utils.rtime();
	    if(prev != this)
		hoverstart = now;
	    try {
		if(now - hoverstart < 1.0) {
		    if(shorttip == null)
			shorttip = new TexI(ItemInfo.shorttip(info()));
		    return(shorttip);
		} else {
		    if(longtip == null)
			longtip = new TexI(ItemInfo.longtip(info()));
		    return(longtip);
		}
	    } catch(Loading l) {
		return("...");
	    }
	}
	return(super.tooltip(c, prev));
    }

    public boolean mousedown(MouseDownEvent ev) {
	int ic = (sz.y - invsq.sz().y) / 2;
	if(ev.c.isect(new Coord(ic, ic), invsq.sz())) {
	    wdgmsg("click", ev.b, ui.modflags());
	    return(true);
	}
	return(super.mousedown(ev));
    }

    public boolean drop(Coord cc, Coord ul) {
	wdgmsg("drop", ui.modflags());
	return(true);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
	wdgmsg("itemiact", ui.modflags());
	return(true);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "upd") {
	    lbl.settext((String)args[0]);
	    icon = (args[1] == null) ? null : ui.sess.getres((Integer)args[1]);
	    iconc = null;
	    int bfl = (Integer)args[2];
	    tbtn.show((bfl & 1) != 0);
		if((bfl & 1) == 0){
			disableTake();
		}
	    pbtn.show((bfl & 2) != 0);
		ybtn.show((bfl & 2) != 0);
	    ebtn.show((bfl & 4) != 0);
	    rawinfo = (args.length > 3) ? new ItemInfo.Raw((Object[])args[3]) : null;
	    info = null;
	    shorttip = longtip = null;
	} else {
	    super.uimsg(msg, args);
	}
    }

	public ItemInfo.Raw getRawinfo() {
		return rawinfo;
	}

	public void takeFromHere(){
		if(rawinfo != null) {
			autoTake = !autoTake;
			if (autoTake) {
				ybtn.change("Stop");
			} else {
				ybtn.change("Auto");
			}
		}
	}

	public void disableTake(){
		autoTake = false;
		ybtn.change("Auto");
	}

	public void putUpgraded(){
		for(Grainslot grainslot : FarmingStatic.grainSlots){
			if(grainslot.autoTake){
				grainslot.wdgmsg("take");
				break;
			}
		}
		wdgmsg("put");
	}

	@Override
	public void remove() {
		FarmingStatic.grainSlots.remove(this);
		super.remove();

	}

}
