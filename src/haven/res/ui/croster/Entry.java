/* Preprocessed source code */
package haven.res.ui.croster;

import haven.*;
import haven.render.*;
import java.util.*;
import java.util.function.*;
import haven.MenuGrid.Pagina;
import java.awt.Color;
import java.awt.image.BufferedImage;

@haven.FromResource(name = "ui/croster", version = 76)
public class Entry extends Widget {
    public static final int WIDTH = CattleRoster.WIDTH;
    public static final int HEIGHT = UI.scale(20);
    public static final Coord SIZE = new Coord(WIDTH, HEIGHT);
    public static final Color every = new Color(255, 255, 255, 16), other = new Color(255, 255, 255, 32);
    public static final Function<Integer, String> percent = v -> String.format("%d%%", v);
    public static final Function<Number, String> quality = v -> Long.toString(Math.round(v.doubleValue()));
    public static final Function<Entry, Tex> namerend = e -> {
	return(CharWnd.attrf.render(e.name, BuddyWnd.gc[e.grp]).tex());
    };
    public static final Tex male   = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/male", 2)::get).layer(Resource.imgc).tex();
    public static final Tex female = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/female", 2)::get).layer(Resource.imgc).tex();
    public static final Function<Boolean, Tex> sex = v -> (v ? male : female);
    public static final Tex adult = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/adult", 2)::get).layer(Resource.imgc).tex();
    public static final Tex child = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/child", 2)::get).layer(Resource.imgc).tex();
    public static final Function<Boolean, Tex> growth = v -> (v ? child : adult);
    public static final Tex dead  = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/dead", 2)::get).layer(Resource.imgc).tex();
    public static final Tex alive = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/alive", 2)::get).layer(Resource.imgc).tex();
    public static final Function<Boolean, Tex> deadrend = v -> (v ? dead : alive);
    public static final Tex pregy = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/pregnant-y", 2)::get).layer(Resource.imgc).tex();
    public static final Tex pregn = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/pregnant-n", 2)::get).layer(Resource.imgc).tex();
    public static final Function<Boolean, Tex> pregrend = v -> (v ? pregy : pregn);
    public static final Tex lacty = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/lactate-y", 1)::get).layer(Resource.imgc).tex();
    public static final Tex lactn = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/lactate-n", 1)::get).layer(Resource.imgc).tex();
    public static final Function<Boolean, Tex> lactrend = v -> (v ? lacty : lactn);
    public static final Tex ownedn = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/owned-n", 1)::get).layer(Resource.imgc).tex();
    public static final Tex ownedo = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/owned-o", 1)::get).layer(Resource.imgc).tex();
    public static final Tex ownedm = Loading.waitfor(Resource.classres(Entry.class).pool.load("gfx/hud/rosters/owned-m", 1)::get).layer(Resource.imgc).tex();
    public static final Function<Integer, Tex> ownrend = v -> ((v == 3) ? ownedm : ((v == 1) ? ownedo : ownedn));
    public final UID id;
    public String name;
    public int grp;
    public double q;
    public int idx;
    public CheckBox mark;

    public Entry(Coord sz, UID id, String name) {
	super(sz);
	this.id = id;
	this.name = name;
	this.mark = adda(new CheckBox(""), UI.scale(5), sz.y / 2, 0, 0.5);
    }

    protected void drawbg(GOut g) {
	g.chcolor(((idx & 1) == 0) ? every : other);
	g.frect(Coord.z, sz);
	g.chcolor();
    }

    private Tex[] rend = {};
    private Object[] rendv = {};
    public <V> void drawcol(GOut g, Column<?> col, double a, V val, Function<? super V, ?> fmt, int idx) {
	if(fmt == null) fmt = Function.identity();
	if(rend.length <= idx) {
	    rend = Arrays.copyOf(rend, idx + 1);
	    rendv = Arrays.copyOf(rendv, idx + 1);
	}
	if(!Utils.eq(rendv[idx], val)) {
	    if(rend[idx] != null)
		rend[idx].dispose();
	    Object rval = fmt.apply(val);
	    if(rval instanceof Tex)
		rend[idx] = (Tex)rval;
	    else
		rend[idx] = CharWnd.attrf.render(String.valueOf(rval)).tex();
	    rendv[idx] = val;
	}
	Coord sz = rend[idx].sz();
	g.image(rend[idx], new Coord(col.x + (int)Math.round((col.w - sz.x) * a), (this.sz.y - sz.y) / 2));
    }

    public boolean mousedown(MouseDownEvent ev) {
	if(ev.propagate(this) || super.mousedown(ev))
	    return(true);
	getparent(CattleRoster.class).wdgmsg("click", id, ev.b, ui.modflags(), ui.mc);
	return(true);
    }

    public <T extends Entry> void markall(Class<T> type, Predicate<? super T> p) {
	for(T ent : getparent(CattleRoster.class).children(type)) {
	    if(p.test(ent))
		ent.mark.click();
	}
    }
}
