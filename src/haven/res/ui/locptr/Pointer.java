/* Preprocessed source code */
package haven.res.ui.locptr;

import haven.*;
import haven.automated.PointerTriangulation;
import haven.render.*;
import java.awt.Color;
import static java.lang.Math.*;

/* >wdg: Pointer */
@haven.FromResource(name = "ui/locptr", version = 21)
public class Pointer extends Widget {
    public static final BaseColor col = new BaseColor(new Color(241, 227, 157, 255));
    public Indir<Resource> icon;
    public Coord2d tc;
    public Coord lc;
    public long gobid = -1;
    public boolean click;
    private Tex licon;
	private Text.Line tt = null;
	private int dist;

    public Pointer(Indir<Resource> icon) {
	super(Coord.z);
	this.icon = icon;
    }

    public static Widget mkwidget(UI ui, Object... args) {
	int iconid = (Integer)args[0];
	Indir<Resource> icon = (iconid < 0) ? null : ui.sess.getres(iconid);
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

	private void drawarrow(GOut gOut, double a) {
		Coord hsz = sz.div(2);
		double ca = -Coord.z.angle(hsz);
		Coord ac;
		if((a > ca) && (a < -ca)) {
			ac = new Coord(sz.x, hsz.y - (int)(Math.tan(a) * hsz.x));
		} else if((a > -ca) && (a < Math.PI + ca)) {
			ac = new Coord(hsz.x - (int)(Math.tan(a - Math.PI / 2) * hsz.y), 0);
		} else if((a > -Math.PI - ca) && (a < ca)) {
			ac = new Coord(hsz.x + (int)(Math.tan(a + Math.PI / 2) * hsz.y), sz.y);
		} else {
			ac = new Coord(0, hsz.y + (int)(Math.tan(a) * hsz.x));
		}
		Coord bc = ac.add(Coord.sc(a, 0));

		Coord coord1 = bc.add(Coord.sc(a + Math.PI / 12, -35));
		Coord coord2 = bc.add(Coord.sc(a - Math.PI / 12, -35));

//		gOut.usestate(Pointer.col);
		gOut.drawp(Model.Mode.TRIANGLES, new float[] {
				bc.x, bc.y,
				coord1.x, coord1.y,
				coord2.x, coord2.y,});

		if (this.icon != null) {
			try {
				if (this.licon == null) {
					this.licon = ((this.icon.get()).layer(Resource.imgc)).tex();
				}
				Coord bcc = bc.add(Coord.sc(a, -UI.scale(30)));
				gOut.aimage(this.licon, bcc, 0.5, 0.5);
				gOut.aimage(Text.renderstroked(dist + "", Color.WHITE, Color.BLACK, Text.num12boldFnd).tex(), bcc, 0.5, 0.5);
			}
			catch (Loading e) {
			}
		}
		this.lc = bc.add(Coord.sc(a, -30));
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

	final Coord norm = sc.sub(tc).norm(UI.scale(30.0));
	final Coord add = sc.add(hsz);

	// gl.glEnable(GL2.GL_POLYGON_SMOOTH); XXXRENDER
//		g.usestate(col);
	g.drawp(Model.Mode.TRIANGLES, new float[] {
		add.x, add.y, add.x + norm.x - norm.y / 3, add.y + norm.y + norm.x / 3, add.x + norm.x + norm.y / 3, add.y + norm.y - norm.x / 3
	    });

	if(icon != null) {
	    try {
		if(licon == null)
		    licon = icon.get().layer(Resource.imgc).tex();
		Coord bcc = add.add(norm);
		g.aimage(licon, bcc, 0.5, 0.5);
		g.aimage(Text.renderstroked(dist + "", Color.WHITE, Color.BLACK, Text.num12boldFnd).tex(), bcc, 0.5, 0.5);
	    } catch(Loading l) {
	    }
	}
	this.lc = add.add(norm);
    }

    public void draw(GOut g) {
	this.lc = null;
	if(tc == null || ui.gui == null || ui.gui.map == null)
	    return;
	Gob gob = (gobid < 0) ? null : ui.sess.glob.oc.getgob(gobid);
	Coord3f sl;
	Coord2d gobrc;
	if(gob != null) {
	    try {
			sl = ui.gui.map.screenxf(gob.getc());
			gobrc = gob.rc;
	    } catch(Loading l) {
		return;
	    }
	} else {
		sl = ui.gui.map.screenxf(tc);
		gobrc = tc;
	}
	if (sl != null) {
		final Double angle = ui.gui.map.screenangle(gobrc, true);
		Gob me = this.ui.gui.map.player();
		if (me != null) {
			int cdist = (int) (Math.ceil(me.rc.dist(tc) / 11.0));
			if (cdist != dist) {
				dist = cdist;
		}
		}
		if(!angle.equals(Double.NaN)) {
			drawarrow(g, ui.gui.map.screenangle(gobrc, true));
		} else {
	    drawarrow(g, new Coord(sl));
    }
	}
	}

    public void update(Coord2d tc, long gobid) {
	this.tc = tc;
	this.gobid = gobid;
    }

    public boolean mousedown(MouseDownEvent ev) {
	if(click && (lc != null)) {
	    if(lc.dist(ev.c) < 20) {
		wdgmsg("click", ev.b, ui.modflags());
		return(true);
	    }
	}
	return(super.mousedown(ev));
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
		gobid = Utils.uint32((Integer)args[1]);
	} else if(name == "icon") {
	    int iconid = (Integer)args[0];
	    Indir<Resource> icon = (iconid < 0) ? null : ui.sess.getres(iconid);
	    this.icon = icon;
	    licon = null;
	} else if(name == "cl") {
	    click = ((Integer)args[0]) != 0;
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
				} catch (NullPointerException e) {
				}
			}
			return (tooltip);
		}
	return(null);
    }
}
