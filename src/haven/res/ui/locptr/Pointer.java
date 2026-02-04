/* Preprocessed source code */
package haven.res.ui.locptr;

import haven.*;
import haven.automated.PointerTriangulation;
import haven.render.*;
import java.awt.Color;
import static java.lang.Math.*;

/* >wdg: Pointer */
@haven.FromResource(name = "ui/locptr", version = 23)
public class Pointer extends Widget {
    public static final BaseColor col = new BaseColor(new Color(241, 227, 157, 255));
    public Indir<Resource> icon;
    public Coord2d tc;
    public Coord lc;
    public long gobid = -1;
    private Tex licon;
	private Text.Line tt = null;
	private int dist;

	private Tex cachedDistText = null;
	private int cachedDist = -1;

    public Pointer(Indir<Resource> icon) {
	super(Coord.z);
	this.icon = icon;
    }

    public static Widget mkwidget(UI ui, Object... args) {
	Indir<Resource> icon = ui.sess.getresv(args[0]);
	return(new Pointer(icon));
    }

    public void presize() {
	resize(parent.sz);
    }

    protected void added() {
	presize();
	super.added();
    }

    private int signum(int a) {
	if(a < 0) return(-1);
	if(a > 0) return(1);
	return(0);
    }

    private void drawarrow(GOut g, Coord tc) {
	Coord hsz = sz.div(2);
	tc = tc.sub(hsz);
	if(tc.equals(Coord.z))
	    tc = new Coord(1, 1);
	double d = Coord.z.dist(tc);
	Coord sc = tc.mul((d - 25.0) / d);
	float ak = ((float)hsz.y) / ((float)hsz.x);
	if((abs(sc.x) > hsz.x) || (abs(sc.y) > hsz.y)) {
	    if(abs(sc.x) * ak < abs(sc.y)) {
		sc = new Coord((sc.x * hsz.y) / sc.y, hsz.y).mul(signum(sc.y));
	    } else {
		sc = new Coord(hsz.x, (sc.y * hsz.x) / sc.x).mul(signum(sc.x));
	    }
	}
	Coord ad = sc.sub(tc).norm(UI.scale(30.0));
	sc = sc.add(hsz);

	// gl.glEnable(GL2.GL_POLYGON_SMOOTH); XXXRENDER
//	g.usestate(col);
	g.drawp(Model.Mode.TRIANGLES, new float[] {
		sc.x, sc.y,
		sc.x + ad.x - (ad.y / 3), sc.y + ad.y + (ad.x / 3),
		sc.x + ad.x + (ad.y / 3), sc.y + ad.y - (ad.x / 3),
	    });

	if(icon != null) {
	    try {
		if(licon == null)
		    licon = icon.get().layer(Resource.imgc).tex();
		g.aimage(licon, sc.add(ad), 0.5, 0.5);
		if (cachedDist != dist) {
		    if (cachedDistText != null) {
		        cachedDistText.dispose();
		    }
		    cachedDistText = Text.renderstroked(dist + "", Color.WHITE, Color.BLACK, Text.num12boldFnd).tex();
		    cachedDist = dist;
		}

		if (cachedDistText != null) {
		    g.aimage(cachedDistText, sc.add(ad), 0.5, 0.5);
		}
	    } catch(Loading l) {
	    }
	}
	this.lc = sc.add(ad);
    }

    public void draw(GOut g) {
	this.lc = null;
	if(tc == null)
	    return;
	Gob gob = (gobid < 0) ? null : ui.sess.glob.oc.getgob(gobid);
	MapView mv = getparent(GameUI.class).map;
	HomoCoord4f sl;
	if(gob != null) {
	    try {
		sl = mv.clipxf(gob.getc(), true);
	    } catch(Loading l) {
		return;
	    }
	} else {
	    try {
		sl = mv.clipxf(Coord3f.of((float)tc.x, (float)tc.y, mv.getcc().z), true);
	    } catch(Loading l) {
		return;
	    }
	}
	Gob me = getparent(GameUI.class).map.player();
	if (me != null) {
		int cdist = (int) (Math.ceil(me.rc.dist(tc) / 11.0));
		if (cdist != dist)
			dist = cdist;
	}
	drawarrow(g, new Coord(sl.toview(Area.sized(mv.sz))));
    }

    public void update(Coord2d tc, long gobid) {
	this.tc = tc;
	this.gobid = gobid;
    }

    public boolean checkhit(Coord c) {
	return((lc != null) && (lc.dist(c) < 20));
    }

    public void uimsg(String name, Object... args) {
	if(name == "upd") {
	    if(args[0] == null)
		tc = null;
	    else
		tc = ((Coord)args[0]).mul(OCache.posres);
	    if(args[1] == null)
		gobid = -1;
	    else
		gobid = Utils.uiv(args[1]);
	} else if(name == "icon") {
	    Indir<Resource> icon = ui.sess.getresv(args[0]);
	    this.icon = icon;
	    licon = null;
	} else {
	    super.uimsg(name, args);
	}
    }

    public Object tooltip(Coord c, Widget prev) {
	if ((lc != null) && (lc.dist(c) < 20) && this.ui.gui.map.player() != null) {
		if (tooltip instanceof Widget.KeyboundTip) {
			try {
				try {
					Coord2d playerCoord = ui.gui.map.player().rc;
					Coord2d targetCoord = tc;
					double dx = targetCoord.x - playerCoord.x;
					double dy = playerCoord.y - targetCoord.y;
					PointerTriangulation.pointerAngle = Math.atan2(dy, dx);
					PointerTriangulation.pointerChecked = true;
				} catch (Exception ignored){}
				if (tt != null && tt.tex() != null)
					tt.tex().dispose();
				if (dist > 990) {
					return tt = Text.render("> " + ((Widget.KeyboundTip) tooltip).base + " <" + " | Distance: Over " + 1000 + " tiles");
				} else {
					return tt = Text.render("> " + ((Widget.KeyboundTip) tooltip).base + " <" + " | Distance: " + dist + " tiles");
				}
			} catch (NullPointerException ignored) {
			}
		}
		return (tooltip);
	}
	return(null);
    }

    @Override
    public void dispose() {
	if (cachedDistText != null) {
	    cachedDistText.dispose();
	    cachedDistText = null;
	}
	super.dispose();
    }
}
