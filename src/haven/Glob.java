/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.util.*;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import haven.render.*;
import haven.render.sl.*;

public class Glob {
    public final OCache oc = new OCache(this);
    public final MCache map;
    public final Session sess;
    public final Loader loader = new Loader();
    public double gtime, sgtime, epoch = Utils.rtime();
    public Astronomy ast;
    public Party party;
    public Color lightamb = null, lightdif = null, lightspc = null;
    public Color olightamb = null, olightdif = null, olightspc = null;
    public Color tlightamb = null, tlightdif = null, tlightspc = null;
    public double lightang = 0.0, lightelev = 0.0;
    public double olightang = 0.0, olightelev = 0.0;
    public double tlightang = 0.0, tlightelev = 0.0;
    public double lchange = -1;
    public Indir<Resource> sky1 = null, sky2 = null;
    public double skyblend = 0.0;
    private final Map<String, CAttr> cattr = new HashMap<String, CAttr>();
    private Map<Indir<Resource>, Object> wmap = new HashMap<Indir<Resource>, Object>();
	public String mservertime;
	public String lservertime;
	public String rservertime;
	public String bservertime;
	public final AtomicReference<Pair<String, Tex>> mservertimetex = new AtomicReference<>(new Pair<>(null, null));
	public final AtomicReference<Pair<String, Tex>> lservertimetex = new AtomicReference<>(new Pair<>(null, null));
	public final AtomicReference<Pair<String, Tex>> rservertimetex = new AtomicReference<>(new Pair<>(null, null));
	public final AtomicReference<Pair<String, Tex>> bservertimetex = new AtomicReference<>(new Pair<>(null, null));
	private static final long secinday = 60 * 60 * 24;
	private static final long dewyladysmantletimemin = 4 * 60 * 60 + 45 * 60;
	private static final long dewyladysmantletimemax = 7 * 60 * 60 + 15 * 60;
	private static final String[] seasonNames = {"Spring", "Summer", "Autumn", "Winter"};
	private static final String[] mPhaseNames = {
			"New",
			"Waxing Crescent",
			"First Quarter",
			"Waxing Gibbous",
			"Full",
			"Waning Gibbous",
			"Last Quarter",
			"Waning Crescent"
	};

    public Glob(Session sess) {
	this.sess = sess;
	map = new MCache(sess);
	party = new Party(this);
    }

    public static interface Weather {
	public Pipe.Op state();
	public void update(Object... args);
	public boolean tick(double dt);

	public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
	    public FactMaker() {super(Factory.class);}
	    {
		add(new Direct<>(Factory.class));
		add(new StaticCall<>(Factory.class, "mkweather", Weather.class, new Class<?>[] {Object[].class},
				     (make) -> (args) -> make.apply(new Object[]{args})));
		add(new Construct<>(Factory.class, Weather.class, new Class<?>[] {Object[].class},
				    (cons) -> (args) -> cons.apply(new Object[]{args})));
	    }
	}

	@Resource.PublishedCode(name = "wtr", instancer = FactMaker.class)
	public static interface Factory {
	    public Weather weather(Object... args);
	}
    }

    public static class CAttr {
	public final Glob glob;
	public static final Text.Foundry fnd = new Text.Foundry(Text.sans, 12);
	public final String nm;
	public int base, comp;
	public ItemInfo.Raw info;
	private Text.Line compLine = null;

	public CAttr(Glob glob, String nm, int base, int comp, ItemInfo.Raw info) {
	    this.glob = glob;
	    this.nm = nm.intern();
	    this.base = base;
	    this.comp = comp;
	    this.info = info;
		compLine = null;
	}
	
	public void update(int base, int comp, ItemInfo.Raw info) {
	    this.base = base;
	    this.comp = comp;
	    this.info = info;
        compLine = null;
        Makewindow.invalidate(nm);
	}

	public Indir<Resource> res() {
	    return(Resource.local().load("gfx/hud/chr/" + nm));
	}

    public Text.Line compline() {
        if(compLine == null) {
            Color c = Color.WHITE;
            if(comp > base) {
                c = CharWnd.buff;
            } else if(comp < base) {
                c = CharWnd.debuff;
            }
            compLine = Text.renderstroked(Integer.toString(comp), c, Color.BLACK, fnd);
        }
        return compLine;
    }
    }
    
    private static Color colstep(Color o, Color t, double a) {
	int or = o.getRed(), og = o.getGreen(), ob = o.getBlue(), oa = o.getAlpha();
	int tr = t.getRed(), tg = t.getGreen(), tb = t.getBlue(), ta = t.getAlpha();
	return(new Color(or + (int)((tr - or) * a),
			 og + (int)((tg - og) * a),
			 ob + (int)((tb - ob) * a),
			 oa + (int)((ta - oa) * a)));
    }

    private void ticklight(double dt) {
	if(lchange >= 0) {
	    lchange += dt;
	    if(lchange > 2.0) {
		lchange = -1;
		lightamb = tlightamb;
		lightdif = tlightdif;
		lightspc = tlightspc;
		lightang = tlightang;
		lightelev = tlightelev;
	    } else {
		double a = lchange / 2.0;
		lightamb = colstep(olightamb, tlightamb, a);
		lightdif = colstep(olightdif, tlightdif, a);
		lightspc = colstep(olightspc, tlightspc, a);
		lightang = olightang + a * Utils.cangle(tlightang - olightang);
		lightelev = olightelev + a * Utils.cangle(tlightelev - olightelev);
	    }
		brighten();
	}
    }

    private double lastctick = 0;
    public void ctick() {
	double now = Utils.rtime();
	double dt;
	if(lastctick == 0)
	    dt = 0;
	else
	    dt = Math.max(now - lastctick, 0.0);

	synchronized(this) {
	    ticklight(dt);
	    for(Object o : wmap.values()) {
		if(o instanceof Weather)
		    ((Weather)o).tick(dt);
	    }
	}

	tickgtime(now, dt);
	oc.ctick(dt);
	map.ctick(dt);

	servertimecalc();

	lastctick = now;
    }

    public void gtick(Render g) {
	oc.gtick(g);
	map.gtick(g);
    }

    private static final double itimefac = 3.0;
    private double stimefac = itimefac, ctimefac = itimefac;
    private void tickgtime(double now, double dt) {
	double sgtime = this.sgtime + ((now - epoch) * stimefac);
	gtime += dt * ctimefac;
	if((sgtime > gtime) && (ctimefac / stimefac < 1.1))
	    ctimefac += Math.min((sgtime - gtime) * 0.001, 0.02) * dt;
	else if((sgtime < gtime) && (stimefac / ctimefac < 1.1))
	    ctimefac -= Math.min((gtime - sgtime) * 0.001, 0.02) * dt;
	ctimefac += Math.signum(stimefac - ctimefac) *0.002 * dt;
    }

    private void updgtime(double sgtime, boolean inc) {
	double now = Utils.rtime();
	double delta = now - epoch;
	epoch = now;
	if((this.sgtime == 0) || !inc || (Math.abs(sgtime - this.sgtime) > 500)) {
	    this.gtime = this.sgtime = sgtime;
	    return;
	}
	if((sgtime - this.sgtime) > 1) {
	    double utimefac = (sgtime - this.sgtime) / delta;
	    double f = Math.min(delta * 0.01, 0.5);
	    stimefac = (stimefac * (1 - f)) + (utimefac * f);
	}
	this.sgtime = sgtime;
    }

    public String gtimestats() {
	double sgtime = this.sgtime + ((Utils.rtime() - epoch) * stimefac);
	return(String.format("%.2f %.2f %.2f %.2f %.2f %.2f %.2f", gtime, this.sgtime, epoch, sgtime, sgtime - gtime, ctimefac, stimefac));
    }

    public double globtime() {
	return(gtime);
    }

    public void blob(Message msg) {
	boolean inc = msg.uint8() != 0;
	while(!msg.eom()) {
	    String t = msg.string().intern();
	    Object[] a = msg.list(sess.resmapper);
	    int n = 0;
	    if(t == "tm") {
		updgtime(Utils.dv(a[n++]), inc);
	    } else if(t == "astro") {
		double dt = Utils.dv(a[n++]);
		double mp = Utils.dv(a[n++]);
		double yt = Utils.dv(a[n++]);
		boolean night = Utils.bv(a[n++]);
		Color mc = (Color)a[n++];
		int is = (n < a.length) ? Utils.iv(a[n++]) : 1;
		double sp = (n < a.length) ? Utils.dv(a[n++]) : 0.5;
		double sd = (n < a.length) ? Utils.dv(a[n++]) : 0.5;
		double years = (n < a.length) ? Utils.dv(a[n++]) : 0.5;
		double ym = (n < a.length) ? Utils.dv(a[n++]) : 0.5;
		double md = (n < a.length) ? Utils.dv(a[n++]) : 0.5;
		ast = new Astronomy(dt, mp, yt, night, mc, is, sp, sd, years, ym, md);
	    } else if(t == "light") {
		synchronized(this) {
		    tlightamb = (Color)a[n++];
		    tlightdif = (Color)a[n++];
		    tlightspc = (Color)a[n++];
		    tlightang = Utils.dv(a[n++]);
		    tlightelev = Utils.dv(a[n++]);
		    if(inc) {
			olightamb = lightamb;
			olightdif = lightdif;
			olightspc = lightspc;
			olightang = lightang;
			olightelev = lightelev;
			lchange = 0;
		    } else {
			lightamb = tlightamb;
			lightdif = tlightdif;
			lightspc = tlightspc;
			lightang = tlightang;
			lightelev = tlightelev;
			lchange = -1;
			brighten();
		    }
		}
	    } else if(t == "sky") {
		synchronized(this) {
		    if(a.length < 1) {
			sky1 = sky2 = null;
			skyblend = 0.0;
		    } else {
			sky1 = sess.getresv(a[n++]);
			if(a.length < 2) {
			    sky2 = null;
			    skyblend = 0.0;
			} else {
			    sky2 = sess.getresv(a[n++]);
			    skyblend = Utils.dv(a[n++]);
			}
		    }
		}
	    } else if(t == "wth") {
		synchronized(this) {
		    if(!inc)
			wmap.clear();
		    Collection<Object> old = new LinkedList<Object>(wmap.keySet());
		    while(n < a.length) {
			Indir<Resource> res = sess.getresv(a[n++]);
			Object[] args = (Object[])a[n++];
			Object curv = wmap.get(res);
			if(curv instanceof Weather) {
			    Weather cur = (Weather)curv;
			    cur.update(args);
			} else {
			    wmap.put(res, args);
			}
			old.remove(res);
		    }
		    for(Object p : old)
			wmap.remove(p);
		}
	    } else {
		System.err.println("Unknown globlob type: " + t);
	    }
	}
    }

    public Collection<Weather> weather() {
	synchronized(this) {
	    ArrayList<Weather> ret = new ArrayList<>(wmap.size());
	    for(Map.Entry<Indir<Resource>, Object> cur : wmap.entrySet()) {
		try {
			String resName = cur.getKey().get().name;
			if ((OptWnd.disableSeasonalGroundColorsCheckBox.a && resName.equals("gfx/fx/seasonmap"))
			|| (OptWnd.disableRainCheckBox.a && resName.equals("gfx/fx/rain"))
			|| (OptWnd.disableGroundCloudShadowsCheckBox.a && resName.equals("gfx/fx/clouds"))
			|| (OptWnd.disableWetGroundOverlayCheckBox.a && resName.equals("gfx/fx/wet"))
			|| (OptWnd.disableSnowingCheckBox.a && resName.equals("gfx/fx/snow"))
			|| (OptWnd.disableValhallaFilterCheckBox.a && resName.equals("gfx/fx/desat"))
			|| (OptWnd.disableScreenShakingCheckBox.a && resName.equals("gfx/fx/quake"))
			|| (OptWnd.disableHempHighCheckBox.a && resName.equals("gfx/fx/lucy"))
			|| (OptWnd.disableOpiumHighCheckBox.a && resName.equals("gfx/fx/dragon"))
			|| (OptWnd.disableLibertyCapsHighCheckBox.a && resName.equals("gfx/fx/shroomed"))
			|| (OptWnd.disableDrunkennessDistortionCheckBox.a && resName.equals("gfx/fx/bottle"))) {
				continue;
			}
		} catch (Loading ignored){}
		Object val = cur.getValue();
		if(val instanceof Weather) {
		    ret.add((Weather)val);
		} else {
		    try {
			Weather.Factory f = cur.getKey().get().flayer(Resource.CodeEntry.class).get(Weather.Factory.class);
			Weather w = f.weather((Object[])val);
			cur.setValue(w);
			ret.add(w);
		    } catch(Loading l) {
		    }
		}
	    }
	    return(ret);
	}
    }

    /* XXX: This is actually quite ugly and there should be a better
     * way, but until I can think of such a way, have this as a known
     * entry-point to be forwards-compatible with compiled
     * resources. */
    public static DirLight amblight(Pipe st) {
	return(((MapView)((PView.WidgetContext)st.get(RenderContext.slot)).widget()).amblight);
    }

    public CAttr getcattr(String nm) {
	synchronized(cattr) {
	    CAttr a = cattr.get(nm);
	    if(a == null) {
		a = new CAttr(this, nm, 0, 0, ItemInfo.Raw.nil);
		cattr.put(nm, a);
	    }
	    return(a);
	}
    }

    public void cattr(String nm, int base, int comp, ItemInfo.Raw info) {
	synchronized(cattr) {
	    CAttr a = cattr.get(nm);
	    if(a == null) {
		a = new CAttr(this, nm, base, comp, info);
		cattr.put(nm, a);
	    } else {
		a.update(base, comp, info);
	    }
	}
    }

    public static class FrameInfo extends State {
	public static final Slot<FrameInfo> slot = new Slot<>(Slot.Type.SYS, FrameInfo.class);
	public static final Uniform u_globtime = new Uniform(Type.FLOAT, "globtime", p -> {
		FrameInfo inf = p.get(slot);
		return((inf == null) ? 0.0f : (float)(inf.globtime % 10000.0));
	    }, slot);
	public final double globtime;

	public FrameInfo(Glob glob) {
	    this.globtime = glob.globtime();
	}

	public ShaderMacro shader() {return(null);}
	public void apply(Pipe p) {p.put(slot, this);}

	public static Expression globtime() {
	    return(u_globtime.ref());
	}

	public String toString() {return(String.format("#<globinfo @%fs>", globtime));}
    }

	// ND: This is used for the Night Mode slider
	private final Object brightsync = new Object();
	public Color blightamb = null, blightdif = null, blightspc = null;
	public static double nightVisionBrightness = Utils.getprefd("nightVisionSetting", 0.0);
	public void brighten(){
		synchronized(brightsync) {
			double bright = nightVisionBrightness;
			if(lightamb != null) {
				blightamb = Utils.blendcol(lightamb, Color.WHITE, bright);
			}
			if(lightdif != null) {
				blightdif = Utils.blendcol(lightdif, Color.WHITE, bright);
			}
			if(lightspc != null) {
				blightspc = Utils.blendcol(lightspc, Color.WHITE, bright);
			}
		}
	}

	private void servertimecalc() {
		long secs = (long) (globtime());
		long day = secs / secinday;
		long secintoday = secs % secinday;
		long hours = secintoday / 3600;
		long mins = (secintoday % 3600) / 60;
		long seconds = secintoday % 60;

		String dayOfMonth = "";
		String phaseOfMoon = " ";
		if (ast != null) {
			int nextseason = (int) Math.ceil((1 - ast.sp) * (ast.is == 1 ? 35 : ast.is == 3 ? 5 : 10));

			int sdt = (ast.is == 1 ? 105 : ast.is == 3 ? 15 : 30); //days of season total
			int sdp = (int) (ast.sp * (sdt)); //days of season passed
			int sdl = (int) Math.floor((1 - ast.sp) * (sdt));
			if (sdl >= 1)
				dayOfMonth = seasonNames[ast.is] + String.format(" %d (%d ", (sdp + 1), sdl) + "left" + String.format(" (%d RL))", nextseason);
			else
				dayOfMonth = String.format("Last day of %s", seasonNames[ast.is]);
			int mp = (int) Math.round(ast.mp * mPhaseNames.length) % mPhaseNames.length;
			phaseOfMoon = mPhaseNames[mp] + " Moon";
		}

		mservertime = "Day" + String.format(" %d, %02d:%02d:%02d", day, hours, mins, seconds);
		lservertime = String.format("%s", dayOfMonth);
		rservertime = phaseOfMoon;
		if (secintoday >= dewyladysmantletimemin && secintoday <= dewyladysmantletimemax)
			bservertime = "(Dewy Lady's Mantle)";
		else
			bservertime = "";
		infoUpdate(mservertimetex, mservertime);
		infoUpdate(lservertimetex, lservertime);
		infoUpdate(rservertimetex, rservertime);
		infoUpdate(bservertimetex, bservertime);
	}

	private void infoUpdate(AtomicReference<Pair<String, Tex>> t, String text) {
		infoUpdate(t, text, () -> Text.renderstroked(text).tex());
	}

	private void infoUpdate(AtomicReference<Pair<String, Tex>> t, String text, Supplier<Tex> getter) {
		if (text != null && (t.get().a == null || !t.get().a.equals(text))) t.set(new Pair<>(text, text.isEmpty() ? null : getter.get()));
	}
}
