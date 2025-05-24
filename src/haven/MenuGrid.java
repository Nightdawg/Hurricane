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

import haven.automated.*;
import haven.render.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import haven.Resource.AButton;
import haven.ItemInfo.AttrCache;

public class MenuGrid extends Widget implements KeyBinding.Bindable {
    public final static Tex bg = Inventory.invsq;
    public final static Coord bgsz = Inventory.sqsz;
    public final static RichText.Foundry ttfnd = new RichText.Foundry(TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, UI.scale(10f));
    private static Coord gsz = new Coord(6, 4);
    public final Set<Pagina> paginae = new HashSet<Pagina>();
    public Pagina cur;
    private final Map<Object, Pagina> pmap = new CacheMap<>(CacheMap.RefType.WEAK);
    private Pagina dragging;
    private Collection<PagButton> curbtns = Collections.emptyList();
    private PagButton pressed, layout[][] = new PagButton[gsz.x][gsz.y];
    private UI.Grab grab;
    private int curoff = 0;
    private boolean recons = true, showkeys = false;
    private double fstart;
	public static ArrayList<String> customButtonPaths = new ArrayList<String>();

    @RName("scm")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new MenuGrid());
	}
    }

    public static class Pagina {
	public final MenuGrid scm;
	public final Object id;
	public Indir<Resource> res;
	public byte[] sdt = null;
	public int anew, tnew;
	public Object[] rawinfo = {};

	public Pagina(MenuGrid scm, Object id, Indir<Resource> res) {
	    this.scm = scm;
	    this.id = id;
	    this.res = res;
	}

	public Resource res() {
	    return(res.get());
	}

	public Message data() {
	    return((sdt == null) ? Message.nil : new MessageBuf(sdt));
	}

	private void invalidate() {
	    button = null;
	}

	private PagButton button = null;
	public PagButton button() {
	    if(button == null) {
		Resource res = res();
		PagButton.Factory f = res.getcode(PagButton.Factory.class, false);
		if(f == null)
		    button = new PagButton(this);
		else
		    button = f.make(this);
	    }
	    return(button);
	}

	public Pagina parent() {
	    return(button().parent());
	}
    }

    public static class Interaction {
	public final int btn, modflags;
	public final Coord2d mc;
	public final ClickData click;

	public Interaction(int btn, int modflags, Coord2d mc, ClickData click) {
	    this.btn = btn;
	    this.modflags = modflags;
	    this.mc = mc;
	    this.click = click;
	}

	public Interaction(int btn, int modflags) {
	    this(btn, modflags, null, null);
	}

	public Interaction() {
	    this(1, 0);
	}
    }

    public static class PagButton implements ItemInfo.Owner, GSprite.Owner, RandomSource {
	public final Pagina pag;
	public final Resource res;
	public final KeyBinding bind;
	private GSprite spr;
	private AButton act;

	public PagButton(Pagina pag) {
	    this.pag = pag;
	    this.res = pag.res();
	    this.bind = binding();
	}

	public AButton act() {
	    if(act == null)
		act = res.flayer(Resource.action);
	    return(act);
	}

	private Pagina parent;
	public Pagina parent() {
	    if(parent == null)
		parent = pag.scm.paginafor(act().parent);
	    return(parent);
	}

	public GSprite spr() {
	    if(spr == null)
		spr = GSprite.create(this, res, Message.nil);
	    return(spr);
	}
	public String name() {return(act().name);}
	public KeyMatch hotkey() {
	    char hk = act().hk;
	    if(hk == 0)
		return(KeyMatch.nil);
	    return(KeyMatch.forchar(Character.toUpperCase(hk), KeyMatch.MODS & ~KeyMatch.S, 0));
	}
	public KeyBinding binding() {
	    return(KeyBinding.get("scm/" + res.name, hotkey()));
	}
	public void use(Interaction iact) {
	    Object[] eact = new Object[] {pag.scm.ui.modflags()};
	    if(iact.mc != null) {
		eact = Utils.extend(eact, iact.mc.floor(OCache.posres));
		if(iact.click != null)
		    eact = Utils.extend(eact, iact.click.clickargs());
	    }
	    if(pag.id instanceof Indir)
		pag.scm.wdgmsg("act", Utils.extend(Utils.extend(new Object[0], act().ad), eact));
	    else
		pag.scm.wdgmsg("use", Utils.extend(new Object[] {pag.id}, eact));
	}
	public void tick(double dt) {
	    if(spr != null)
		spr.tick(dt);
	}

	public BufferedImage img() {
	    GSprite spr = spr();
	    if(spr instanceof GSprite.ImageSprite)
		return(((GSprite.ImageSprite)spr).image());
	    return(null);
	}

	public final AttrCache<Pipe.Op> rstate = new AttrCache<>(this::info, info -> {
		ArrayList<GItem.RStateInfo> ols = new ArrayList<>();
		for(ItemInfo inf : info) {
		    if(inf instanceof GItem.RStateInfo)
			ols.add((GItem.RStateInfo)inf);
		}
		if(ols.size() == 0)
		    return(() -> null);
		if(ols.size() == 1) {
		    Pipe.Op op = ols.get(0).rstate();
		    return(() -> op);
		}
		Pipe.Op[] ops = new Pipe.Op[ols.size()];
		for(int i = 0; i < ops.length; i++)
		    ops[i] = ols.get(0).rstate();
		Pipe.Op cmp = Pipe.Op.compose(ops);
		return(() -> cmp);
	});
	public final AttrCache<GItem.InfoOverlay<?>[]> ols = new AttrCache<>(this::info, info -> {
		ArrayList<GItem.InfoOverlay<?>> buf = new ArrayList<>();
		for(ItemInfo inf : info) {
		    if(inf instanceof GItem.OverlayInfo)
			buf.add(GItem.InfoOverlay.create((GItem.OverlayInfo<?>)inf));
		}
		GItem.InfoOverlay<?>[] ret = buf.toArray(new GItem.InfoOverlay<?>[0]);
		return(() -> ret);
	});
	public final AttrCache<Double> meter = new AttrCache<>(this::info, AttrCache.map1(GItem.MeterInfo.class, minf -> minf::meter));

	public void drawmain(GOut g, GSprite spr) {
	    spr.draw(g);
	}
	public void draw(GOut g, GSprite spr) {
	    if(rstate.get() != null)
		g.usestate(rstate.get());
	    drawmain(g, spr);
	    g.defstate();
	    GItem.InfoOverlay<?>[] ols = this.ols.get();
	    if(ols != null) {
		for(GItem.InfoOverlay<?> ol : ols)
		    ol.draw(g);
	    }
	    Double meter = this.meter.get();
	    if((meter != null) && (meter > 0)) {
		g.chcolor(255, 255, 255, 64);
		Coord half = spr.sz().div(2);
		g.prect(half, half.inv(), half, meter * Math.PI * 2);
		g.chcolor();
	    }
	}

	public String sortkey() {
	    if((act().ad.length == 0) && (pag.id instanceof Indir))
		return("\0" + name());
	    return(name());
	}

	private char bindchr(KeyMatch key) {
	    if(key.modmatch != 0)
		return(0);
	    char vkey = key.chr;
	    if((vkey == 0) && (key.keyname.length() == 1))
		vkey = key.keyname.charAt(0);
	    return(vkey);
	}

	public static final Text.Foundry keyfnd = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 10);
	private Tex keyrend = null;
	private boolean haskeyrend = false;
	public Tex keyrend() {
	    if(!haskeyrend) {
		char vkey = bindchr(bind.key());
		if(vkey != 0)
		    keyrend = new TexI(Utils.outline2(keyfnd.render(Character.toString(vkey), Color.WHITE).img, Color.BLACK));
		else
		    keyrend = null;
		haskeyrend = true;
	    }
	    return(keyrend);
	}

	private List<ItemInfo> info = null;
	public List<ItemInfo> info() {
	    if(info == null) {
		info = ItemInfo.buildinfo(this, pag.rawinfo);
		Resource.Pagina pg = res.layer(Resource.pagina);
		if(pg != null)
		    info.add(new ItemInfo.Pagina(this, pg.text));
	    }
	    return(info);
	}
	private static final OwnerContext.ClassResolver<PagButton> ctxr = new OwnerContext.ClassResolver<PagButton>()
	    .add(PagButton.class, p -> p)
	    .add(MenuGrid.class, p -> p.pag.scm)
	    .add(Glob.class, p -> p.pag.scm.ui.sess.glob)
	    .add(Session.class, p -> p.pag.scm.ui.sess);
	public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}
	public Random mkrandoom() {return(new Random());}
	public Resource getres() {return(res);}

	public BufferedImage rendertt(boolean withpg) {
	    String tt = name();
	    KeyMatch key = bind.key();
	    int pos = -1;
	    char vkey = bindchr(key);
	    if((vkey != 0) && (key.modmatch == 0))
		pos = tt.toUpperCase().indexOf(Character.toUpperCase(vkey));
	    if(pos >= 0)
		tt = tt.substring(0, pos) + "$b{$col[255,128,0]{" + tt.charAt(pos) + "}}" + tt.substring(pos + 1);
	    else if(key != KeyMatch.nil)
		tt += " [$b{$col[255,128,0]{" + key.longname() + "}}]";
	    BufferedImage ret = PUtils.strokeImg(PUtils.strokeImg(ttfnd.render(tt, UI.scale(300)).img));
	    if(withpg) {
		List<ItemInfo> info = info();
		info.removeIf(el -> el instanceof ItemInfo.Name);
		if(!info.isEmpty())
		    ret = ItemInfo.catimgs(0, ret, ItemInfo.longtip(info));
	    }
	    return(ret);
	}

	public static class FactMaker extends Resource.PublishedCode.Instancer.Chain<Factory> {
	    public FactMaker() {
		super(Factory.class);
		add(new Direct<>(Factory.class));
		add(new StaticCall<>(Factory.class, "mkpagina", PagButton.class, new Class<?>[] {Pagina.class},
				     (make) -> (pagina) -> make.apply(new Object[] {pagina})));
		add(new Construct<>(Factory.class, PagButton.class, new Class<?>[] {Pagina.class},
				    (cons) -> (pagina) -> cons.apply(new Object[] {pagina})));
	    }
	}

	@Resource.PublishedCode(name = "pagina", instancer = FactMaker.class)
	public interface Factory {
	    public PagButton make(Pagina info);
	}
    }

    public final PagButton next = new PagButton(new Pagina(this, null, Resource.local().loadwait("gfx/hud/sc-next").indir())) {
	    {pag.button = this;}

	    public void use(Interaction iact) {
		int step = (gsz.x * gsz.y) - 2;
		if((curoff + step) >= curbtns.size())
		    curoff = 0;
		else
		    curoff += step;
		updlayout();
	    }

	    public String name() {return("More...");}

	    public KeyBinding binding() {return(kb_next);}
	};

    public final PagButton bk = new PagButton(new Pagina(this, null, Resource.local().loadwait("gfx/hud/sc-back").indir())) {
	    {pag.button = this;}

	    public void use(Interaction iact) {
		pag.scm.change(pag.scm.cur.parent());
		curoff = 0;
	    }

	    public String name() {return("Back");}

	    public KeyBinding binding() {return(kb_back);}
	};

    public Pagina paginafor(Indir<Resource> res) {
	if(res == null)
	    return(null);
	synchronized(pmap) {
	    Pagina p = pmap.get(res);
	    if(p == null)
		pmap.put(res, p = new Pagina(this, res, res));
	    return(p);
	}
    }

    public Pagina paginafor(Object id, Indir<Resource> res) {
	synchronized(pmap) {
	    Pagina p = pmap.get(id);
	    if((p == null) && (res != null))
		pmap.put(id, p = new Pagina(this, id, res));
	    return(p);
	}
    }

    private boolean cons(Pagina p, Collection<PagButton> buf) {
	Pagina[] cp = new Pagina[0];
	Collection<Pagina> open, close = new HashSet<Pagina>();
	synchronized(pmap) {
	    for(Pagina pag : pmap.values())
		pag.tnew = 0;
	}
	synchronized(paginae) {
	    open = new LinkedList<Pagina>();
	    for(Pagina pag : paginae) {
		open.add(pag);
		if(pag.anew > 0) {
		    try {
			for(Pagina npag = pag; npag != null; npag = npag.parent())
			    npag.tnew = Math.max(npag.tnew, pag.anew);
		    } catch(Loading l) {
		    }
		}
	    }
	}
	boolean ret = true;
	while(!open.isEmpty()) {
	    Iterator<Pagina> iter = open.iterator();
	    Pagina pag = iter.next();
	    iter.remove();
	    try {
		Pagina parent = pag.parent();
		if(parent == p)
		    buf.add(pag.button());
		else if((parent != null) && !close.contains(parent) && !open.contains(parent))
		    open.add(parent);
		close.add(pag);
	    } catch(Loading e) {
		ret = false;
	    }
	}
	return(ret);
    }

    private void announce(Pagina pag) {
	ui.loader.defer(() -> ui.msg("New discovery: " + pag.button().name(), Color.WHITE, null), null);
    }

    public MenuGrid() {
	super(bgsz.mul(gsz).add(1, 1));
	loadCustomActionButtons();
    }

    private void updlayout() {
	synchronized(paginae) {
	    List<PagButton> cur = new ArrayList<>();
	    recons = !cons(this.cur, cur);
	    Collections.sort(cur, Comparator.comparing(PagButton::sortkey));
	    this.curbtns = cur;
	    int i = curoff;
	    for(int y = 0; y < gsz.y; y++) {
		for(int x = 0; x < gsz.x; x++) {
		    PagButton btn = null;
		    if((this.cur != null) && (x == gsz.x - 1) && (y == gsz.y - 1)) {
			btn = bk;
		    } else if((cur.size() > ((gsz.x * gsz.y) - 1)) && (x == gsz.x - 2) && (y == gsz.y - 1)) {
			btn = next;
		    } else if(i < cur.size()) {
			btn = cur.get(i++);
		    }
		    layout[x][y] = btn;
		}
	    }
	    fstart = Utils.rtime();
	}
    }

    public void draw(GOut g) {
	double now = Utils.rtime();
	for(int y = 0; y < gsz.y; y++) {
	    for(int x = 0; x < gsz.x; x++) {
		Coord p = bgsz.mul(new Coord(x, y));
		g.image(bg, p);
		PagButton btn = layout[x][y];
		if(btn != null) {
		    GSprite spr;
		    try {
			spr = btn.spr();
		    } catch(Loading l) {
			continue;
		    }
		    GOut g2 = g.reclip(p.add(1, 1), spr.sz());
		    Pagina info = btn.pag;
		    if(info.tnew != 0) {
			info.anew = 1;
			double a = 0.25;
			if(info.tnew == 2) {
			    double ph = (now - fstart) - (((x + (y * gsz.x)) * 0.15) % 1.0);
			    a = (ph < 1.25) ? (Math.cos(ph * Math.PI * 2) * -0.25) + 0.25 : 0.25;
			}
			g2.usestate(new ColorMask(new FColor(0.125f, 1.0f, 0.125f, (float)a)));
		    }
		    btn.draw(g2, spr);
		    g2.defstate();
		    if(showkeys) {
			Tex ki = btn.keyrend();
			if(ki != null)
			    g2.aimage(ki, Coord.of(bgsz.x - UI.scale(2), UI.scale(1)), 1.0, 0.0);
		    }
		    if(btn == pressed) {
			g.chcolor(new Color(0, 0, 0, 128));
			g.frect(p.add(1, 1), bgsz.sub(1, 1));
			g.chcolor();
		    }
		}
	    }
	}
	super.draw(g);
	if(dragging != null) {
	    GSprite ds = dragging.button().spr();
	    ui.drawafter(new UI.AfterDraw() {
		    public void draw(GOut g) {
			ds.draw(g.reclip(ui.mc.sub(ds.sz().div(2)), ds.sz()));
		    }
		});
	}
    }

    private PagButton curttp = null;
    private boolean curttl = false;
    private Tex curtt = null;
    private double hoverstart;
    public Object tooltip(Coord c, Widget prev) {
	PagButton pag = bhit(c);
	double now = Utils.rtime();
	if(pag != null) {
	    if(prev != this)
		hoverstart = now;
	    boolean ttl = (now - hoverstart) > 0.0;
	    if((pag != curttp) || (ttl != curttl)) {
		BufferedImage ti = pag.rendertt(ttl);
		curtt = (ti == null) ? null : new TexI(ti);
		curttp = pag;
		curttl = ttl;
	    }
	    return(curtt);
	} else {
	    hoverstart = now;
	    return(null);
	}
    }

    private PagButton bhit(Coord c) {
	Coord bc = c.div(bgsz);
	if((bc.x >= 0) && (bc.y >= 0) && (bc.x < gsz.x) && (bc.y < gsz.y))
	    return(layout[bc.x][bc.y]);
	else
	    return(null);
    }

    public boolean mousedown(MouseDownEvent ev) {
	PagButton h = bhit(ev.c);
	if((ev.b == 1) && (h != null)) {
	    pressed = h;
	    grab = ui.grabmouse(this);
	}
	return(true);
    }

    public void mousemove(MouseMoveEvent ev) {
	if((dragging == null) && (pressed != null)) {
	    PagButton h = bhit(ev.c);
	    if(h != pressed)
		dragging = pressed.pag;
	}
    }

    public void change(Pagina dst) {
	this.cur = dst;
	curoff = 0;
	if(dst == null)
	    showkeys = false;
	updlayout();
    }

    public void use(PagButton r, Interaction iact, boolean reset) {
	Collection<PagButton> sub = new ArrayList<>();
	cons(r.pag, sub);
	if(sub.size() > 0) {
	    change(r.pag);
	} else {
		try {
			String[] ad = r.act().ad;
			if (ad[0].equals("@")) {
				useCustom(ad);
			}
			if (ad.length > 0 && (ad[0].equals("craft") || ad[0].equals("bp"))) {
				if((ad[0].equals("craft")))
					ui.gui.makewnd.setLastAction(r.pag);
			}
		} catch (Exception ignored) {
		}
	    r.pag.anew = r.pag.tnew = 0;
	    r.use(iact);
	    if(reset)
		change(null);
	}
    }

	public static boolean loginTogglesNeedUpdate = true;
	public boolean complicatedLoginTogglesNeedUpdate = true;
    public void tick(double dt) {
	if(recons)
	    updlayout();
	if (loginTogglesNeedUpdate) {
		GameUI gui = getparent(GameUI.class);
		if (gui != null) {
			if (OptWnd.toggleTrackingOnLoginCheckBox.a && !GameUI.trackingToggled){
				wdgmsg("act", "tracking");
			}
			if (OptWnd.toggleSwimmingOnLoginCheckBox.a && !GameUI.swimmingToggled){
				wdgmsg("act", "swim");
			}
			if (OptWnd.toggleCriminalActsOnLoginCheckBox.a && !GameUI.crimesToggled){
				wdgmsg("act", "crime");
			}
			loginTogglesNeedUpdate = false;
			if (OptWnd.toggleSiegeEnginesOnLoginCheckBox.a){
				wdgmsg("act", "siegeptr");
			}
		}
	}
	if (complicatedLoginTogglesNeedUpdate) { // ND: Unlike swim/crime/tracking, these are saved serverside. I toggle them automatically here once, then I fix them in GameUI
		wdgmsg("act", "permshare");
		wdgmsg("act", "itemcomb");
		complicatedLoginTogglesNeedUpdate = false;
	}
	for(int y = 0; y < gsz.y; y++) {
	    for(int x = 0; x < gsz.x; x++) {
		if(layout[x][y] != null)
		    layout[x][y].tick(dt);
	    }
	}
    }

    public boolean mouseup(MouseUpEvent ev) {
	PagButton h = bhit(ev.c);
	if((ev.b == 1) && (grab != null)) {
	    if(dragging != null) {
		DropTarget.dropthing(ui.root, ui.mc, dragging);
		pressed = null;
		dragging = null;
	    } else if(pressed != null) {
		if(pressed == h)
		    use(h, new Interaction(), false);
		pressed = null;
	    }
	    grab.remove();
	    grab = null;
	}
	return(true);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "goto") {
	    if(args[0] == null)
		change(null);
	    else
		change(paginafor(ui.sess.getresv(args[0])));
	} else if(msg == "fill") {
	    synchronized(paginae) {
		int a = 0;
		while(a < args.length) {
		    int fl = Utils.iv(args[a++]);
		    Pagina pag;
		    Object id;
		    if((fl & 2) != 0)
			pag = paginafor(id = args[a++], null);
		    else
			id = (pag = paginafor(ui.sess.getres((Integer)args[a++], -2))).res;
		    if((fl & 1) != 0) {
			if((fl & 2) != 0) {
			    Indir<Resource> res = ui.sess.getres((Integer)args[a++], -2);
			    if(pag == null) {
				pag = paginafor(id, res);
			    } else if(pag.res != res) {
				pag.res = res;
				pag.invalidate();
			    }
			}
			byte[] data = ((fl & 4) != 0) ? (byte[])args[a++] : null;
			if(!Arrays.equals(pag.sdt, data)) {
			    pag.sdt = data;
			    pag.invalidate();
			}
			if((fl & 8) != 0) {
			    pag.anew = 2;
			    announce(pag);
			}
			Object[] rawinfo = ((fl & 16) != 0) ? (Object[])args[a++] : new Object[0];
			if(!Arrays.deepEquals(pag.rawinfo, rawinfo)) {
			    pag.rawinfo = rawinfo;
			    pag.invalidate();
			}
			paginae.add(pag);
		    } else {
			paginae.remove(pag);
		    }
		}
		updlayout();
	    }
	} else {
	    super.uimsg(msg, args);
	}
    }

    public static final KeyBinding kb_root = KeyBinding.get("scm-root", KeyMatch.forcode(KeyEvent.VK_ESCAPE, 0));
    public static final KeyBinding kb_back = KeyBinding.get("scm-back", KeyMatch.forcode(KeyEvent.VK_BACK_SPACE, 0));
    public static final KeyBinding kb_next = KeyBinding.get("scm-next", KeyMatch.forchar('N', KeyMatch.S | KeyMatch.C | KeyMatch.M, KeyMatch.S));
    public boolean globtype(GlobKeyEvent ev) {
	if (OptWnd.disableMenuGridHotkeysCheckBox.a || !GameUI.showUI)
		return (false);
	if(kb_root.key().match(ev) && (this.cur != null)) {
	    change(null);
	    return(true);
	} else if(kb_back.key().match(ev) && (this.cur != null)) {
	    use(bk, new Interaction(), false);
	    return(true);
	} else if(kb_next.key().match(ev) && (layout[gsz.x - 2][gsz.y - 1] == next)) {
	    use(next, new Interaction(), false);
	    return(true);
	}
	int cp = -1;
	PagButton pag = null;
	for(PagButton btn : curbtns) {
	    if(btn.bind.key().match(ev)) {
		int prio = btn.bind.set() ? 1 : 0;
		if((pag == null) || (prio > cp)) {
		    pag = btn;
		    cp = prio;
		}
	    }
	}
	if(pag != null) {
	    use(pag, new Interaction(), (ev.mods & KeyMatch.S) == 0);
	    if(this.cur != null)
		showkeys = true;
	    return(true);
	}
	return(super.globtype(ev));
    }

    public KeyBinding getbinding(Coord cc) {
	PagButton h = bhit(cc);
	return((h == null) ? null : h.bind);
    }

	private void makeLocal(String path) {
		customButtonPaths.add(path); // ND: Add the paths to this list, to check against them when we load the action bars in GameUI -> loadLocal().
		Resource.Named res = Resource.local().loadwait(path).indir();
		Pagina pagina = new Pagina(this, null, res);
		synchronized (pmap) { pmap.put(res, pagina); }
		synchronized (paginae) { paginae.add(pagina); }
	}

	private void loadCustomActionButtons() {
		// Category: Toggles
		makeLocal("customclient/menugrid/Toggles/FlavorObjects");
		makeLocal("customclient/menugrid/Toggles/FlatWorld");
		makeLocal("customclient/menugrid/Toggles/TileSmoothing");
		makeLocal("customclient/menugrid/Toggles/TileTransitions");
		makeLocal("customclient/menugrid/Toggles/ItemDroppingAnywhere");
		makeLocal("customclient/menugrid/Toggles/ItemDroppingInWater");
		makeLocal("customclient/menugrid/Toggles/HighlightCliffs");
		makeLocal("customclient/menugrid/Toggles/BeeSkepsRadii");
		makeLocal("customclient/menugrid/Toggles/TroughsRadii");
		makeLocal("customclient/menugrid/Toggles/MoundBedsRadii");
		makeLocal("customclient/menugrid/Toggles/MineSupportRadii");
		makeLocal("customclient/menugrid/Toggles/MineSupportSafeTiles");
		makeLocal("customclient/menugrid/Toggles/MineSweeper");
		makeLocal("customclient/menugrid/Toggles/ClearAllCombatDamage");
		makeLocal("customclient/menugrid/Toggles/AnimalAutoPeace");
		makeLocal("customclient/menugrid/Toggles/AutoDrinking");
		makeLocal("customclient/menugrid/Toggles/TileCentering");
		makeLocal("customclient/menugrid/Toggles/QueuedMovementWindow");
		makeLocal("customclient/menugrid/Toggles/AutoDrop");
		makeLocal("customclient/menugrid/Toggles/BarrelContentsText");
		makeLocal("customclient/menugrid/Toggles/FlowerMenuAutoSelect");

		// Category: Bots
		makeLocal("customclient/menugrid/Bots/OceanScoutBot");
		makeLocal("customclient/menugrid/Bots/TarKilnEmptierBot");
		makeLocal("customclient/menugrid/Bots/TurnipBot");
		makeLocal("customclient/menugrid/Bots/CleanupBot");
		makeLocal("customclient/menugrid/Bots/GetTheBestSeeds");
		makeLocal("customclient/menugrid/Bots/MixSeedsByQuality");
		makeLocal("customclient/menugrid/Bots/FillBarrelWithSeeds");
		makeLocal("customclient/menugrid/Bots/FillInventoryWithSeeds");
		makeLocal("customclient/menugrid/Bots/Autoprepare");
		makeLocal("customclient/menugrid/Bots/Autoshoot");
		makeLocal("customclient/menugrid/Bots/Autoleash");
		makeLocal("customclient/menugrid/Bots/FillTrays");
		makeLocal("customclient/menugrid/Bots/BuilderTools");
		makeLocal("customclient/menugrid/Bots/Farmer");

		// Category: Other Scripts & Tools
		makeLocal("customclient/menugrid/OtherScriptsAndTools/Add9CoalScript");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/Add12CoalScript");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/CloverScript");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/CoracleScript");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/SkisScript");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/RefillWaterContainers");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/CombatDistanceTool");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/RefillCheeseTrays");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/HarvestNearestDreamcatcher");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/DestroyNearestTrellisPlantScript");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/MiningSafetyAssistant");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/QuestgiverTriangulation");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/OreAndStoneCounter");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/GridHeightCalculator");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/CustomAlarmManager");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/AutoDropManager");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/FlowerMenuAutoSelectManager");
		makeLocal("customclient/menugrid/OtherScriptsAndTools/QuestHelper");

		// Category: Quick Switch From Belt
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_B12");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_BoarSpear");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_BronzeSwordWoodenShield");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_Cutblade");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_FyrdsmansSwordWoodenShield");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_HirdsmansSwordWoodenShield");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_MetalShovel");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_Pickaxe");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_Scythe");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_Sledgehammer");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_TinkersShovel");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_TravelersSacks");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_WanderersBindles");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_WoodenShovel");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_HuntersBow");
		makeLocal("customclient/menugrid/QuickSwitchFromBelt/Equip_RangersBow");

		// Category: Combat Decks
		makeLocal("customclient/menugrid/CombatDecks/CombatDeck1");
		makeLocal("customclient/menugrid/CombatDecks/CombatDeck2");
		makeLocal("customclient/menugrid/CombatDecks/CombatDeck3");
		makeLocal("customclient/menugrid/CombatDecks/CombatDeck4");
		makeLocal("customclient/menugrid/CombatDecks/CombatDeck5");

	}

	public void useCustom(String[] ad) {
		GameUI gui = ui.gui;
		if (gui == null)
			return;
		if (ad[1].equals("Toggles")) { // Category: Toggles
			if (ad[2].equals("FlavorObjects")) {
				OptWnd.hideFlavorObjectsCheckBox.set(!OptWnd.hideFlavorObjectsCheckBox.a);
			} else if (ad[2].equals("FlatWorld")) {
				OptWnd.flatWorldCheckBox.set(!OptWnd.flatWorldCheckBox.a);
			} else if (ad[2].equals("TileSmoothing")) {
				OptWnd.disableTileSmoothingCheckBox.set(!OptWnd.disableTileSmoothingCheckBox.a);
			} else if (ad[2].equals("TileTransitions")) {
				OptWnd.disableTileTransitionsCheckBox.set(!OptWnd.disableTileTransitionsCheckBox.a);
			} else if (ad[2].equals("ItemDroppingAnywhere")) {
				OptWnd.noCursorItemDroppingAnywhereCheckBox.set(!OptWnd.noCursorItemDroppingAnywhereCheckBox.a);
			} else if (ad[2].equals("ItemDroppingInWater")) {
				OptWnd.noCursorItemDroppingInWaterCheckBox.set(!OptWnd.noCursorItemDroppingInWaterCheckBox.a);
			} else if (ad[2].equals("HighlightCliffs")) {
				OptWnd.highlightCliffsCheckBox.set(!OptWnd.highlightCliffsCheckBox.a);
			} else if (ad[2].equals("BeeSkepsRadii")) {
				OptWnd.showBeeSkepsRadiiCheckBox.set(!OptWnd.showBeeSkepsRadiiCheckBox.a);
			} else if (ad[2].equals("TroughsRadii")) {
				OptWnd.showFoodTroughsRadiiCheckBox.set(!OptWnd.showFoodTroughsRadiiCheckBox.a);
			} else if (ad[2].equals("MoundBedsRadii")) {
				OptWnd.showMoundBedsRadiiCheckBox.set(!OptWnd.showMoundBedsRadiiCheckBox.a);
			} else if (ad[2].equals("MineSupportRadii")) {
				OptWnd.showMineSupportRadiiCheckBox.set(!OptWnd.showMineSupportRadiiCheckBox.a);
			} else if (ad[2].equals("MineSupportSafeTiles")) {
				OptWnd.showMineSupportSafeTilesCheckBox.set(!OptWnd.showMineSupportSafeTilesCheckBox.a);
			} else if (ad[2].equals("MineSweeper")) {
				OptWnd.enableMineSweeperCheckBox.set(!OptWnd.enableMineSweeperCheckBox.a);
			} else if (ad[2].equals("ClearAllCombatDamage")) {
				OptWnd.damageInfoClearButton.click();
			} else if (ad[2].equals("AnimalAutoPeace")) {
				OptWnd.autoPeaceAnimalsWhenCombatStartsCheckBox.set(!OptWnd.autoPeaceAnimalsWhenCombatStartsCheckBox.a);
			} else if (ad[2].equals("AutoDrinking")) {
				OptWnd.autoDrinkingCheckBox.set(!OptWnd.autoDrinkingCheckBox.a);
			} else if (ad[2].equals("TileCentering")) {
				OptWnd.tileCenteringCheckBox.set(!OptWnd.tileCenteringCheckBox.a);
			} else if (ad[2].equals("QueuedMovementWindow")) {
				OptWnd.enableQueuedMovementCheckBox.set(!OptWnd.enableQueuedMovementCheckBox.a);
			} else if (ad[2].equals("AutoDrop")) {
				AutoDropManagerWindow.autoDropItemsCheckBox.set(!AutoDropManagerWindow.autoDropItemsCheckBox.a);
			} else if (ad[2].equals("BarrelContentsText")) {
				OptWnd.showBarrelContentsTextCheckBox.set(!OptWnd.showBarrelContentsTextCheckBox.a);
			} else if (ad[2].equals("FlowerMenuAutoSelect")) {
				FlowerMenuAutoSelectManagerWindow.flowerMenuAutoSelectCheckBox.set(!FlowerMenuAutoSelectManagerWindow.flowerMenuAutoSelectCheckBox.a);
			}
		} else if (ad[1].equals("Bots")) { // Category: Toggles
			if (ad[2].equals("OceanScoutBot")) {
				if (gui.OceanScoutBot == null && gui.oceanScoutBotThread == null) {
					gui.OceanScoutBot = new OceanScoutBot(gui);
					gui.add(gui.OceanScoutBot, Utils.getprefc("wndc-oceanScoutBotWindow", new Coord(gui.sz.x / 2 - gui.OceanScoutBot.sz.x / 2, gui.sz.y / 2 - gui.OceanScoutBot.sz.y / 2 - 200)));
					gui.oceanScoutBotThread = new Thread(gui.OceanScoutBot, "OceanScoutBot");
					gui.oceanScoutBotThread.start();
				} else {
					if (gui.OceanScoutBot != null) {
						gui.OceanScoutBot.stop = true;
						gui.OceanScoutBot.stop();
						gui.OceanScoutBot.reqdestroy();
						gui.OceanScoutBot = null;
						gui.oceanScoutBotThread = null;
					}
				}
			} else if (ad[2].equals("TarKilnEmptierBot")) {
				if (gui.tarKilnCleanerBot == null && gui.tarKilnCleanerThread == null) {
					gui.tarKilnCleanerBot = new TarKilnCleanerBot(gui);
					gui.add(gui.tarKilnCleanerBot, Utils.getprefc("wndc-tarKilnCleanerBotWindow", new Coord(gui.sz.x/2 - gui.tarKilnCleanerBot.sz.x/2, gui.sz.y/2 - gui.tarKilnCleanerBot.sz.y/2 - 200)));
					gui.tarKilnCleanerThread = new Thread(gui.tarKilnCleanerBot, "TarKilnEmptierBot");
					gui.tarKilnCleanerThread.start();
				} else {
					if (gui.tarKilnCleanerBot != null) {
						gui.tarKilnCleanerBot.stop();
						gui.tarKilnCleanerBot.reqdestroy();
						gui.tarKilnCleanerBot = null;
						gui.tarKilnCleanerThread = null;
					}
				}
			} else if (ad[2].equals("TurnipBot")) {
				if (gui.turnipBot == null && gui.turnipThread == null) {
					gui.turnipBot = new TurnipBot(gui);
					gui.add(gui.turnipBot, Utils.getprefc("wndc-turnipBotWindow", new Coord(gui.sz.x/2 - gui.turnipBot.sz.x/2, gui.sz.y/2 - gui.turnipBot.sz.y/2 - 200)));
					gui.turnipThread = new Thread(gui.turnipBot, "TurnipBot");
					gui.turnipThread.start();
				} else {
					if (gui.turnipBot != null) {
						gui.turnipBot.stop();
						gui.turnipBot.reqdestroy();
						gui.turnipBot = null;
						gui.turnipThread = null;
					}
				}
			} else if (ad[2].equals("CleanupBot")) {
				if (gui.cleanupBot == null && gui.cleanupThread == null) {
					gui.cleanupBot = new CleanupBot(gui);
					gui.add(gui.cleanupBot, Utils.getprefc("wndc-cleanupBotWindow", new Coord(gui.sz.x/2 - gui.cleanupBot.sz.x/2, gui.sz.y/2 - gui.cleanupBot.sz.y/2 - 200)));
					gui.cleanupThread = new Thread(gui.cleanupBot, "CleanupBot");
					gui.cleanupThread.start();
				} else {
					if (gui.cleanupBot != null) {
						gui.cleanupBot.stop();
						gui.cleanupBot.reqdestroy();
						gui.cleanupBot = null;
						gui.cleanupThread = null;
					}
				}
			} else if (ad[2].equals("GetTheBestSeeds")) {
				gui.getTheBestSeedsThread = new Thread(new GetTheBestSeeds(gui), "GetTheBestSeeds");
				gui.runActionThread(gui.getTheBestSeedsThread);
			} else if (ad[2].equals("MixSeedsByQuality")) {
				gui.mixSeedsByQualityThread = new Thread(new MixSeedsByQuality(gui), "MixSeedsByQuality");
				gui.runActionThread(gui.mixSeedsByQualityThread);
			} else if (ad[2].equals("FillBarrelWithSeeds")) {
				gui.fillBarrelWithSeedsThread = new Thread(new FillBarrelWithSeeds(gui), "FillBarrelWithSeeds");
				gui.runActionThread(gui.fillBarrelWithSeedsThread);
			} else if (ad[2].equals("FillInventoryWithSeeds")) {
				gui.fillInventoryWithSeedsThread = new Thread(new FillInventoryWithSeeds(gui), "FillInventoryWithSeeds");
				gui.runActionThread(gui.fillInventoryWithSeedsThread);
			} else if (ad[2].equals("Autoprepare")) {
				gui.autoprepareThread = new Thread(new Autoprepare(gui), "Autoprepare");
				gui.runActionThread(gui.autoprepareThread);
			} else if (ad[2].equals("BuilderTools")) {
				if (gui.builderTools == null && gui.builderToolsThread == null) {
					gui.builderTools = new BuilderTools(gui);
					gui.add(gui.builderTools, Utils.getprefc("wndc-builderToolsWindow", new Coord(gui.sz.x/2 - gui.builderTools.sz.x/2, gui.sz.y/2 - gui.builderTools.sz.y/2 - 200)));
					gui.builderToolsThread = new Thread(gui.builderTools, "BuilderTools");
					gui.builderToolsThread.start();
				} else {
					if (gui.builderTools != null) {
						gui.builderTools.stop();
						gui.builderTools.reqdestroy();
						gui.builderTools = null;
						gui.builderToolsThread = null;
					}
				}
			} else if (ad[2].equals("Farmer")) {
				if (gui.farmerTools == null && gui.farmerToolsThread == null) {
					gui.farmerTools = new Farmer(gui);
					gui.add(gui.farmerTools, Utils.getprefc("wndc-farmerWindow", new Coord(gui.sz.x/2 - gui.farmerTools.sz.x/2, gui.sz.y/2 - gui.farmerTools.sz.y/2 - 200)));
					gui.farmerToolsThread = new Thread(gui.farmerTools, "FarmerTools");
					gui.farmerToolsThread.start();
				} else {
					if (gui.farmerTools != null) {
						gui.farmerTools.stop();
						gui.farmerTools.reqdestroy();
						gui.farmerTools = null;
						gui.farmerToolsThread = null;
					}
				}
			} else if (ad[2].equals("Autoshoot")) {
				gui.runActionThread(new Thread(new Autoshoot(gui), "Autoshoot"));
			} else if (ad[2].equals("Autoleash")) {
				gui.runActionThread(new Thread(new Autoleash(gui), "Autoleash"));
			} else if (ad[2].equals("FillTrays")) {
				gui.runActionThread(new Thread(new FillTrays(gui), "FillTrays"));
			}
		} else if (ad[1].equals("OtherScriptsAndTools")) {
			if (ad[2].equals("Add9CoalScript")) {
				gui.runActionThread(new Thread(new AddCoalToSmelter(gui, 9), "Add9Coal"));
			} else if (ad[2].equals("Add12CoalScript")) {
				gui.runActionThread(new Thread(new AddCoalToSmelter(gui, 12), "Add12Coal"));
			} else if (ad[2].equals("CloverScript")) {
				if (gui.cloverScriptThread == null) {
					gui.cloverScriptThread = new Thread(new CloverScript(gui), "CloverScript");
					gui.cloverScriptThread.start();
				} else {
					gui.cloverScriptThread.interrupt();
					gui.cloverScriptThread = null;
					gui.cloverScriptThread = new Thread(new CloverScript(gui), "CloverScript");
					gui.cloverScriptThread.start();
				}
			} else if (ad[2].equals("CoracleScript")) {
				if (gui.coracleScriptThread == null) {
					gui.coracleScriptThread = new Thread(new CoracleScript(gui), "CoracleScript");
					gui.coracleScriptThread.start();
				} else {
					gui.coracleScriptThread.interrupt();
					gui.coracleScriptThread = null;
					gui.coracleScriptThread = new Thread(new CoracleScript(gui), "CoracleScript");
					gui.coracleScriptThread.start();
				}
			} else if (ad[2].equals("SkisScript")) {
				if (gui.skisScriptThread == null) {
					gui.skisScriptThread = new Thread(new SkisScript(gui), "SkisScript");
					gui.skisScriptThread.start();
				} else {
					gui.skisScriptThread.interrupt();
					gui.skisScriptThread = null;
					gui.skisScriptThread = new Thread(new SkisScript(gui), "SkisScript");
					gui.skisScriptThread.start();
				}
			} else if (ad[2].equals("RefillWaterContainers")) {
				if (gui.refillWaterContainersThread == null) {
					gui.refillWaterContainersThread = new Thread(new RefillWaterContainers(gui), "RefillWaterContainers");
					gui.refillWaterContainersThread.start();
				} else {
					gui.refillWaterContainersThread.interrupt();
					gui.refillWaterContainersThread = null;
					gui.refillWaterContainersThread = new Thread(new RefillWaterContainers(gui), "RefillWaterContainers");
					gui.refillWaterContainersThread.start();
				}
			} else if (ad[2].equals("CombatDistanceTool")) {
				if (gui.combatDistanceTool == null && gui.combatDistanceToolThread == null) {
					gui.combatDistanceTool = new CombatDistanceTool(gui);
					gui.add(gui.combatDistanceTool, Utils.getprefc("wndc-combatDistanceToolWindow", new Coord(gui.sz.x/2 - gui.combatDistanceTool.sz.x/2, gui.sz.y/2 - gui.combatDistanceTool.sz.y/2 - 200)));
					gui.combatDistanceToolThread = new Thread(gui.combatDistanceTool, "CombatDistanceTool");
					gui.combatDistanceToolThread.start();
				} else {
					if (gui.combatDistanceTool != null) {
						gui.combatDistanceTool.stop();
						gui.combatDistanceTool.reqdestroy();
						gui.combatDistanceTool = null;
						gui.combatDistanceToolThread = null;
					}
				}
			} else if (ad[2].equals("RefillCheeseTrays")) {
				gui.runActionThread(new Thread(new FillCheeseTray(gui), "FillCheeseTrays"));
			} else if (ad[2].equals("HarvestNearestDreamcatcher")) {
				if (gui.harvestNearestDreamcatcherThread == null) {
					gui.harvestNearestDreamcatcherThread = new Thread(new HarvestNearestDreamcatcher(gui), "HarvestNearestDreamcatcher");
					gui.harvestNearestDreamcatcherThread.start();
				} else {
					gui.harvestNearestDreamcatcherThread.interrupt();
					gui.harvestNearestDreamcatcherThread = null;
					gui.harvestNearestDreamcatcherThread = new Thread(new HarvestNearestDreamcatcher(gui), "HarvestNearestDreamcatcher");
					gui.harvestNearestDreamcatcherThread.start();
				}
			} else if (ad[2].equals("DestroyNearestTrellisPlantScript")) {
				if (gui.destroyNearestTrellisPlantScriptThread == null) {
					gui.destroyNearestTrellisPlantScriptThread = new Thread(new DestroyNearestTrellisPlantScript(gui), "DestroyNearestTrellisPlantScript");
					gui.destroyNearestTrellisPlantScriptThread.start();
				} else {
					gui.destroyNearestTrellisPlantScriptThread.interrupt();
					gui.destroyNearestTrellisPlantScriptThread = null;
					gui.destroyNearestTrellisPlantScriptThread = new Thread(new DestroyNearestTrellisPlantScript(gui), "DestroyNearestTrellisPlantScript");
					gui.destroyNearestTrellisPlantScriptThread.start();
				}
			} else if (ad[2].equals("MiningSafetyAssistant")) {
				if (gui.miningSafetyAssistantWindow == null && gui.miningSafetyAssistantThread == null) {
					gui.miningSafetyAssistantWindow = new MiningSafetyAssistant(gui);
					gui.miningSafetyAssistantWindow = gui.add(gui.miningSafetyAssistantWindow, Utils.getprefc("wndc-miningSafetyAssistantWindow", new Coord(gui.sz.x/2 - ui.gui.miningSafetyAssistantWindow.sz.x/2, gui.sz.y/2 - gui.miningSafetyAssistantWindow.sz.y/2 - 200)));
					gui.miningSafetyAssistantThread = new Thread(gui.miningSafetyAssistantWindow, "miningSafetyAssistantThread");
					gui.miningSafetyAssistantThread.start();
				} else if (gui.miningSafetyAssistantWindow != null) {
					gui.miningSafetyAssistantThread.interrupt();
					gui.miningSafetyAssistantThread = null;
					gui.miningSafetyAssistantWindow.reqdestroy();
					gui.miningSafetyAssistantWindow = null;
				}
			} else if (ad[2].equals("QuestgiverTriangulation")) {
				if(gui.pointerTriangulation != null){
					gui.pointerTriangulation.reqdestroy();
					gui.pointerTriangulation = null;
				} else {
					gui.pointerTriangulation = new PointerTriangulation(gui);
					gui.add(gui.pointerTriangulation, Utils.getprefc("wndc-pointerTriangulationWindow", new Coord(gui.sz.x/2 - gui.pointerTriangulation.sz.x/2, gui.sz.y/2 - gui.pointerTriangulation.sz.y/2 - 300)));
				}
			} else if (ad[2].equals("OreAndStoneCounter")) {
				if (gui.oreAndStoneCounter == null && gui.oreAndStoneCounterThread == null) {
					gui.oreAndStoneCounter = new OreAndStoneCounter(gui);
					gui.add(gui.oreAndStoneCounter, Utils.getprefc("wndc-oreAndStoneCounterWindow", new Coord(gui.sz.x/2 - gui.oreAndStoneCounter.sz.x/2, gui.sz.y/2 - gui.oreAndStoneCounter.sz.y/2 - 200)));
					gui.oreAndStoneCounterThread = new Thread(gui.oreAndStoneCounter, "OreAndStoneCounter");
					gui.oreAndStoneCounterThread.start();
				} else {
					if (gui.oreAndStoneCounter != null) {
						gui.oreAndStoneCounter.stop();
						gui.oreAndStoneCounter.reqdestroy();
						gui.oreAndStoneCounter = null;
						gui.oreAndStoneCounterThread = null;
					}
				}
			} else if (ad[2].equals("GridHeightCalculator")) {
				AUtils.getGridHeightAvg(gui);
			} else if (ad[2].equals("CustomAlarmManager")) {
				if(gui.opts != null) {
					if(gui.opts.alarmWindow == null) {
						gui.opts.alarmWindow = gui.opts.parent.parent.add(new AlarmWindow());
						gui.opts.alarmWindow.show();
					} else {
						gui.opts.alarmWindow.show(!gui.opts.alarmWindow.visible);
						gui.opts.alarmWindow.bottomNote.settext("NOTE: You can add your own alarm sound files in the \"AlarmSounds\" folder. (The file extension must be .wav)");
						gui.opts.alarmWindow.bottomNote.setcolor(Color.WHITE);
						gui.opts.alarmWindow.bottomNote.c.x = UI.scale(140);
					}
				}
			} else if (ad[2].equals("AutoDropManager")) {
				if(!gui.opts.autoDropManagerWindow.attached) {
					gui.opts.parent.parent.add(gui.opts.autoDropManagerWindow);
					gui.opts.autoDropManagerWindow.show();
				} else {
					gui.opts.autoDropManagerWindow.show(!gui.opts.autoDropManagerWindow.visible);
				}
			} else if (ad[2].equals("FlowerMenuAutoSelectManager")) {
				if(gui.opts.flowerMenuAutoSelectManagerWindow == null) {
					gui.opts.flowerMenuAutoSelectManagerWindow = gui.opts.parent.parent.add(new FlowerMenuAutoSelectManagerWindow());
					gui.opts.flowerMenuAutoSelectManagerWindow.show();
				} else {
					gui.opts.flowerMenuAutoSelectManagerWindow.show(!gui.opts.flowerMenuAutoSelectManagerWindow.visible);
					gui.opts.flowerMenuAutoSelectManagerWindow.refresh();
				}
			} else if (ad[2].equals("QuestHelper")) {
				if(gui.questhelper.visible){
					gui.questhelper.hide();
					gui.questhelper.active = false;
				} else {
					gui.questhelper.show();
					gui.questhelper.active = true;
				}
			}
		} else if (ad[1].equals("QuickSwitchFromBelt")) {
			new Thread(new EquipFromBelt(gui, ad[2]), "EquipFromBelt").start();
		} else if (ad[1].equals("CombatDecks")) {
			gui.changeCombatDeck(Integer.parseInt(ad[2])-1);
		}
	}
}
