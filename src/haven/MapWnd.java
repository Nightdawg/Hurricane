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
import java.util.function.*;
import java.io.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.*;
import haven.MapFile.Marker;
import haven.MapFile.PMarker;
import haven.MapFile.SMarker;
import haven.MiniMap.*;
import haven.BuddyWnd.GroupSelector;
import haven.automated.mapper.MappingClient;

import static haven.MCache.tilesz;
import static haven.MCache.cmaps;
import static haven.Utils.eq;
import javax.swing.JFileChooser;
import javax.swing.filechooser.*;

public class MapWnd extends Window implements Console.Directory {
    public static final Resource markcurs = Resource.local().loadwait("gfx/hud/curs/flag");
    public final MapFile file;
    public final MiniMap view;
    public final MapView mv;
    public final Toolbox tool;
    public final Collection<String> overlays = new java.util.concurrent.CopyOnWriteArraySet<>();
    public MarkerConfig markcfg = MarkerConfig.showall, cmarkers = null;
    private final Locator player;
    private final Widget toolbar;
	private final Widget toolbarTop;
    private final Frame viewf;
    private GroupSelector colsel;
    private Button mremove;
    private Predicate<Marker> mflt = pmarkers;
    private Comparator<ListMarker> mcmp = namecmp;
    private List<ListMarker> markers = Collections.emptyList();
    private int markerseq = -1;
    private boolean domark = false;
    private int olalpha = 64;
    private final Collection<Runnable> deferred = new LinkedList<>();
	private Coord bigmapc = Utils.getprefc("bigmapc", new Coord(0,0));
	private Coord smallmapc = Utils.getprefc("smallmapc", new Coord(0,150));
	private Coord bigmapsz = Utils.getprefc("bigmapsz", new Coord(980,550));
	private Coord smallmapsz = Utils.getprefc("smallmapsz", new Coord(300,300));
	public boolean compact = true;

    private final static Predicate<Marker> pmarkers = (m -> m instanceof PMarker);
    private final static Predicate<Marker> smarkers = (m -> m instanceof SMarker);
    private final static Comparator<ListMarker> namecmp = ((a, b) -> a.mark.nm.compareTo(b.mark.nm));
    private final static Comparator<ListMarker> typecmp = Comparator.comparing((ListMarker lm) -> lm.type).thenComparing(namecmp);

    public static final KeyBinding kb_home = KeyBinding.get("mapwnd/home", KeyMatch.forcode(KeyEvent.VK_HOME, 0));
    public static final KeyBinding kb_mark = KeyBinding.get("mapwnd/mark", KeyMatch.nil);
    public static final KeyBinding kb_hmark = KeyBinding.get("mapwnd/hmark", KeyMatch.nil);
    public static final KeyBinding kb_compact = KeyBinding.get("mapwnd/compact", KeyMatch.forchar('W', KeyMatch.M));
	public static final KeyBinding kb_claim = KeyBinding.get("mapwnd/pclaim", KeyMatch.forcode(KeyEvent.VK_F9, KeyMatch.C | KeyMatch.S));
	public static final KeyBinding kb_vil = KeyBinding.get("mapwnd/vclaim", KeyMatch.forcode(KeyEvent.VK_F10, KeyMatch.C | KeyMatch.S));
    public static final KeyBinding kb_prov = KeyBinding.get("mapwnd/prov", KeyMatch.forcode(KeyEvent.VK_F11, KeyMatch.C | KeyMatch.S));
    public MapWnd(MapFile file, MapView mv, Coord sz, String title) {
	super(sz, title, true);
	this.file = file;
	this.mv = mv;
	this.player = new MapLocator(mv);
	viewf = add(new ViewFrame());
	view = viewf.add(new View(file));
	recenter();
	toolbarTop = add(new Widget(Coord.z));
	toolbarTop.add(new Img(Resource.loadtex("gfx/hud/mmap/topfgwdg")) {
		public boolean mousedown(MouseDownEvent ev) {
			if((ev.b == 1) && checkhit(ev.c)) {
				MapWnd.this.drag(parentpos(MapWnd.this, ev.c));
				return(true);
			}
			return(super.mousedown(ev));
		}
	}, Coord.z);
	toolbarTop.add(new ICheckBox("gfx/hud/mmap/viewrange", "", "-d", "-h", "-dh") {
		})
		.state(() -> Utils.getprefb("showMapViewRange", true))
		.click(() -> {
			if (!MiniMap.showMapViewRange) {
				MiniMap.showMapViewRange = true;
				Utils.setprefb("showMapViewRange", true);
			} else{
				MiniMap.showMapViewRange = false;
				Utils.setprefb("showMapViewRange", false);
			}
		})
		.settip("Show Sight Range");
	toolbarTop.add(new ICheckBox("gfx/hud/mmap/gridlines", "", "-d", "-h", "-dh") {
		})
		.state(() -> Utils.getprefb("showMapGridLines", false))
		.click(() -> {
			if (!MiniMap.showMapGridLines) {
				MiniMap.showMapGridLines = true;
				Utils.setprefb("showMapGridLines", true);
			} else{
				MiniMap.showMapGridLines = false;
				Utils.setprefb("showMapGridLines", false);
			}
		})
		.settip("Show Grid Lines");
	toolbarTop.add(new ICheckBox("gfx/hud/mmap/maphighlight", "", "-d", "-h", "-dh"), UI.scale(new Coord(25, 0)))
		.state(() -> MiniMap.highlightMapTiles)
		.click(() -> {
			Utils.setprefb("highlightMapTiles", !MiniMap.highlightMapTiles);
			toggleol(TileHighlight.TAG, !MiniMap.highlightMapTiles);
			MiniMap.highlightMapTiles = !MiniMap.highlightMapTiles;
		})
		.rclick(() -> {
			TileHighlight.toggle(ui.gui);
		})
		.settip("Highlight Map Tiles\n\nLeft-click to toggle Tile Highlighting\nRight-click to open Highlight Settings", true);
	toolbarTop.add(new ICheckBox("gfx/hud/mmap/fow", "", "-d", "-h", "-dh"), UI.scale(new Coord(50, 0)))
		.state(() -> MiniMap.showMapFogOfWar)
		.click(() -> {
			Utils.setprefb("showMapFogOfWar", !MiniMap.showMapFogOfWar);
			MiniMap.showMapFogOfWar = !MiniMap.showMapFogOfWar;
		})
		.rclick(() -> {

		})
		.settip("Show Fog of War\n\nThis is just an overlay that shows which map tiles you have seen/explored during this session.\nThe fog resets when you log out.", true);
	toolbarTop.c = new Coord(UI.scale(2), UI.scale(2));
	toolbarTop.pack();
	toolbar = add(new Widget(Coord.z));
	toolbar.add(new Img(Resource.loadtex("gfx/hud/mmap/fgwdg")) {
		public boolean mousedown(MouseDownEvent ev) {
		    if((ev.b == 1) && checkhit(ev.c)) {
			MapWnd.this.drag(parentpos(MapWnd.this, ev.c));
			return(true);
		    }
		    return(super.mousedown(ev));
		}
	    }, Coord.z);
	toolbar.add(new IButton("gfx/hud/mmap/home", "", "-d", "-h") {
		{settip("Recenter on self"); setgkey(kb_home);}
		public void click() {
		    recenter();
		}
	    }, Coord.z);
	toolbar.add(new ICheckBox("gfx/hud/mmap/mark", "", "-d", "-h", "-dh"), Coord.z)
	    .state(() -> domark).set(a -> domark = a)
	    .settip("Add marker").setgkey(kb_mark);
	toolbar.add(new ICheckBox("gfx/hud/mmap/hmark", "", "-d", "-h", "-dh"))
	    .state(() -> Utils.eq(markcfg, MarkerConfig.hideall)).click(() -> {
		    if(Utils.eq(markcfg, MarkerConfig.hideall))
			markcfg = MarkerConfig.showall;
		    else if(Utils.eq(markcfg, MarkerConfig.showall) && (cmarkers != null))
			markcfg = cmarkers;
		    else
			markcfg = MarkerConfig.hideall;
		})
	    .settip("Hide markers").setgkey(kb_hmark);
	if(Utils.getprefb("pclaim-claimMapState", true)) overlays.add("cplot");
	toolbar.add(new ICheckBox("gfx/hud/mmap/pclaim", "", "-d", "-h", "-dh"))
		.state(() -> visol("cplot"))
		.click(() -> {
			if (!visol("cplot")) {
				toggleol("cplot", true);
				Utils.setprefb("pclaim-claimMapState", true);
			} else{
				toggleol("cplot", false);
				Utils.setprefb("pclaim-claimMapState", false);
			}
		})
		.settip("Show Personal Claims on Map").setgkey(kb_claim);
	if(Utils.getprefb("vclaim-claimMapState", true)) overlays.add("vlg");
	toolbar.add(new ICheckBox("gfx/hud/mmap/vclaim", "", "-d", "-h", "-dh"))
		.state(() -> visol("vlg"))
		.click(() -> {
			if (!visol("vlg")) {
				toggleol("vlg", true);
				Utils.setprefb("vclaim-claimMapState", true);
			} else{
				toggleol("vlg", false);
				Utils.setprefb("vclaim-claimMapState", false);
			}
		})
		.settip("Show Village Claims on Map").setgkey(kb_vil);
	toolbar.add(new ICheckBox("gfx/hud/mmap/wnd", "", "-d", "-h", "-dh"))
	    .state(this::compact).set(a -> {
		    fixAndSavePos(!a);
		    compact(a);
		    if (view != null) {
				view.compact = a;
				if (a) {
					view.bigMapZoomLevel = view.zoomlevel;
					view.zoomlevel = view.smallMapZoomLevel;
				} else {
					view.smallMapZoomLevel = view.zoomlevel;
					view.zoomlevel = view.bigMapZoomLevel;
				}
			}
		})
	    .settip("Compact mode").setgkey(kb_compact);
	if(Utils.getprefb("prov-claimMapState", false)) overlays.add("realm");
	toolbar.add(new ICheckBox("gfx/hud/mmap/prov", "", "-d", "-h", "-dh"))
		.state(() -> visol("realm"))
		.click(() -> {
			if (!visol("realm")) {
				toggleol("realm", true);
				Utils.setprefb("prov-claimMapState", true);
			} else{
				toggleol("realm", false);
				Utils.setprefb("prov-claimMapState", false);
			}
		})
		.settip("Show Realm Provinces on Map").setgkey(kb_prov);
	toolbar.pack();
	tool = add(new Toolbox());
	compact(true);
	resize(sz);
    }

	public void toggleol(String tag, boolean a) {
	if(a)
	    overlays.add(tag);
	else
	    overlays.remove(tag);
    }

	private boolean visol(String tag) {
		if(overlays != null) {
			return overlays.contains(tag);
		}
		return false;
	}

    private class ViewFrame extends Frame {
	Coord sc = Coord.z;

	ViewFrame() {
	    super(Coord.z, true);
	}

	public void resize(Coord sz) {
	    super.resize(sz);
	    sc = sz.sub(box.bisz()).add(box.btloff()).sub(sizer.sz());
	}

	public void draw(GOut g) {
	    super.draw(g);
	    if(compact())
		g.image(sizer, sc);
	}

	private UI.Grab drag;
	private Coord dragc;
	public boolean mousedown(MouseDownEvent ev) {
	    Coord c = ev.c, cc = c.sub(sc);
	    if((ev.b == 1) && compact() && (cc.x < sizer.sz().x) && (cc.y < sizer.sz().y) && (cc.y >= sizer.sz().y - UI.scale(25) + (sizer.sz().x - cc.x))) {
		if(drag == null) {
		    drag = ui.grabmouse(this);
		    dragc = csz().sub(parentpos(MapWnd.this, c));
		    return(true);
		}
	    }
	    if((ev.b == 1) && (checkhit(c) || ui.modshift)) {
		MapWnd.this.drag(parentpos(MapWnd.this, c));
		return(true);
	    }
	    return(super.mousedown(ev));
	}

	public void mousemove(MouseMoveEvent ev) {
	    super.mousemove(ev);
	    if(drag != null) {
		Coord nsz = parentpos(MapWnd.this, ev.c).add(dragc);
		nsz.x = Math.max(nsz.x, UI.scale(230));
		nsz.y = Math.max(nsz.y, UI.scale(230));
		MapWnd.this.resize(nsz);
	    }
	}

	public boolean mouseup(MouseUpEvent ev) {
        fixAndSavePos(compact);
        compact(compact);
	    if((ev.b == 1) && (drag != null)) {
		drag.remove();
		drag = null;
		return(true);
	    }
	    return(super.mouseup(ev));
	}
    }

    private static final int btnw = UI.scale(95);
    public class Toolbox extends Widget {
	public final MarkerList list;
	private final Frame listf;
	private final Button pmbtn, smbtn, nobtn, tobtn, mebtn, mibtn;
	private TextEntry namesel;

	private Toolbox() {
	    super(UI.scale(200, 200));
	    listf = add(new Frame(UI.scale(new Coord(200, 200)), false), 0, 0);
	    list = listf.add(new MarkerList(Coord.of(listf.inner().x, 0)), 0, 0);
	    pmbtn = add(new Button(btnw, "Placed", false) {
		    public void click() {
			mflt = pmarkers;
			markerseq = -1;
		    }
		});
	    smbtn = add(new Button(btnw, "Natural", false) {
		    public void click() {
			mflt = smarkers;
			markerseq = -1;
		    }
		});
	    nobtn = add(new Button(btnw, "By name", false) {
		    public void click() {
			mcmp = namecmp;
			markerseq = -1;
		    }
		});
	    tobtn = add(new Button(btnw, "By type", false) {
		    public void click() {
			mcmp = typecmp;
			markerseq = -1;
		    }
		});
	    mebtn = add(new Button(btnw, "Export...", false) {
		    public void click() {
			exportmap();
		    }
		});
	    mibtn = add(new Button(btnw, "Import...", false) {
		    public void click() {
			importmap();
		    }
		});
	}

	public void resize(int h) {
	    super.resize(new Coord(sz.x, h));
	    listf.resize(listf.sz.x, sz.y - UI.scale(210));
	    listf.c = new Coord(sz.x - listf.sz.x, 0);
	    list.resize(listf.inner());
	    mebtn.c = new Coord(0, sz.y - mebtn.sz.y);
	    mibtn.c = new Coord(sz.x - btnw, sz.y - mibtn.sz.y);
	    nobtn.c = new Coord(0, mebtn.c.y - UI.scale(30) - nobtn.sz.y);
	    tobtn.c = new Coord(sz.x - btnw, mibtn.c.y - UI.scale(30) - tobtn.sz.y);
	    pmbtn.c = new Coord(0, nobtn.c.y - UI.scale(5) - pmbtn.sz.y);
	    smbtn.c = new Coord(sz.x - btnw, tobtn.c.y - UI.scale(5) - smbtn.sz.y);
	    if(namesel != null) {
		namesel.c = listf.c.add(0, listf.sz.y + UI.scale(10));
		mremove.c = pmbtn.c.sub(0, mremove.sz.y + UI.scale(10));
		if(colsel != null) {
		    colsel.c = namesel.c.add(0, namesel.sz.y + UI.scale(10));
		}
	    }
	}
    }

    private class View extends MiniMap implements CursorQuery.Handler {
		private double highlighterDynamicAlpha = 0;
	View(MapFile file) {
	    super(file);
	}

	public void drawgrid(GOut g, Coord ul, DisplayGrid disp) {
	    super.drawgrid(g, ul, disp);
	    for(String tag : overlays) {
		try {
			int alpha = 96;
		    Tex img;
			if(TileHighlight.TAG.equals(tag)) {
				alpha = (int) (100 + 155 * highlighterDynamicAlpha);
				img = disp.tileimg();
			} else {
				img = disp.olimg(tag);
			}
		    if(img != null) {
			g.chcolor(255, 255, 255, alpha);
			g.image(img, ul, UI.scale(img.sz()).mul(dlvl).div(zoomlevel));
		    }
		} catch(Exception ignored) {
		}
	    }
	    g.chcolor();
	}

	public boolean filter(DisplayMarker mark) {
	    return(markcfg.filter(mark.m));
	}

	public boolean clickmarker(DisplayMarker mark, Location loc, int button, boolean press) {
	    if(button == 1) {
		if(!compact() && !press && !domark) {
		    focus(mark.m);
		    return(true);
		}
	    } else if(mark.m instanceof SMarker) {
		Gob gob = MarkerID.find(ui.sess.glob.oc, mark.m);
		if(gob != null)
		    mvclick(mv, null, loc, gob, button);
	    }
	    return(false);
	}

	public boolean clickicon(DisplayIcon icon, Location loc, int button, boolean press) {
	    if(!press && !domark) {
		mvclick(mv, null, loc, icon.gob, button);
		return(true);
	    }
	    return(false);
	}

	public boolean clickloc(Location loc, int button, boolean press) {
	    if(domark && (button == 1) && !press) {
		Marker nm = new PMarker(loc.seg.id, loc.tc, "New marker", BuddyWnd.gc[new Random().nextInt(BuddyWnd.gc.length)]);
		file.add(nm);
		focus(nm);
		domark = false;
		return(true);
	    }
	    if(!press && (sessloc != null) && (loc.seg == sessloc.seg)) {
		mvclick(mv, null, loc, null, button);
		return(true);
	    }
	    return(false);
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if(domark && (ev.b == 3)) {
		domark = false;
		return(true);
	    }
	    super.mousedown(ev);
	    return(true);
	}

	public void draw(GOut g) {
	    g.chcolor(0, 0, 0, 128);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    super.draw(g);
	}

	public boolean getcurs(CursorQuery ev) {
	    if(domark)
		return(ev.set(markcurs));
	    return(false);
	}

		@Override
		public void tick(double dt) {
			super.tick(dt);
			highlighterDynamicAlpha = Math.sin(Math.PI * ((System.currentTimeMillis() % 1000) / 1000.0));
		}

    }

    public void tick(double dt) {
	super.tick(dt);
	synchronized(deferred) {
	    for(Iterator<Runnable> i = deferred.iterator(); i.hasNext();) {
		Runnable task = i.next();
		try {
		    task.run();
		} catch(Loading l) {
		    continue;
		}
		i.remove();
	    }
	}
	view.markobjs();
	if(visible && (markerseq != view.file.markerseq)) {
	    if(view.file.lock.readLock().tryLock()) {
		try {
		    Map<Marker, ListMarker> prev = new HashMap<>();
		    for(ListMarker pm : this.markers)
			prev.put(pm.mark, pm);
		    List<ListMarker> markers = new ArrayList<>();
		    for(Marker mark : view.file.markers) {
			if(!mflt.test(mark))
			    continue;
			ListMarker lm = prev.get(mark);
			if(lm == null)
			    lm = new ListMarker(mark);
			else
			    lm.type = MarkerType.of(lm.mark);
			markers.add(lm);
		    }
		    markers.sort(mcmp);
		    this.markers = markers;
		} finally {
		    view.file.lock.readLock().unlock();
		}
	    }
	}
    }

    public static abstract class MarkerType implements Comparable<MarkerType> {
	public static final int iconsz = UI.scale(20);
	private static final HashedSet<MarkerType> types = new HashedSet<>(Hash.eq);
	public abstract Tex icon();

	public static MarkerType of(Marker mark) {
	    if(mark instanceof PMarker) {
		return(types.intern(new PMarkerType(((PMarker)mark).color)));
	    } else if(mark instanceof SMarker) {
		return(types.intern(new SMarkerType(((SMarker)mark).res)));
	    } else {
		return(null);
	    }
	}

	public int compareTo(MarkerType that) {
	    return(this.getClass().getName().compareTo(that.getClass().getName()));
	}
    }

    public static class PMarkerType extends MarkerType {
	public final Color col;
	private Tex icon = null;

	public PMarkerType(Color col) {
	    this.col = col;
	}

	public Tex icon() {
	    if(icon == null) {
		Resource.Image fg = MiniMap.DisplayMarker.flagfg, bg = MiniMap.DisplayMarker.flagbg;
		Coord tsz = Coord.of(Math.max(fg.tsz.x, bg.tsz.x), Math.max(fg.tsz.y, bg.tsz.y));
		Coord bsz = Coord.of(Math.max(tsz.x, tsz.y));
		Coord o = bsz.sub(tsz);
		WritableRaster buf = PUtils.imgraster(bsz);
		PUtils.blit(buf, PUtils.coercergba(fg.img).getRaster(), fg.o.add(o));
		PUtils.colmul(buf, col);
		PUtils.alphablit(buf, PUtils.coercergba(bg.img).getRaster(), bg.o.add(o));
		icon = new TexI(PUtils.uiscale(PUtils.rasterimg(buf), new Coord(iconsz, iconsz)));
	    }
	    return(icon);
	}

	public boolean equals(PMarkerType that) {
	    return(Utils.eq(this.col, that.col));
	}
	public boolean equals(Object that) {
	    return((that instanceof PMarkerType) && equals((PMarkerType)that));
	}

	public int hashCode() {
	    return(col.hashCode());
	}

	public int compareTo(PMarkerType that) {
	    int a = Utils.index(BuddyWnd.gc, this.col), b = Utils.index(BuddyWnd.gc, that.col);
	    if((a >= 0) && (b >= 0))
		return(a - b);
	    if((a < 0) && (b >= 0))
		return(1);
	    if((a >= 0) && (b < 0))
		return(-1);
	    return(Utils.idcmp.compare(this.col, that.col));
	}
	public int compareTo(MarkerType that) {
	    if(that instanceof PMarkerType)
		return(compareTo((PMarkerType)that));
	    return(super.compareTo(that));
	}
    }

    public static class SMarkerType extends MarkerType {
	private Resource.Saved spec;
	private Tex icon = null;

	public SMarkerType(Resource.Saved spec) {
	    this.spec = spec;
	}

	public Tex icon() {
	    if(icon == null) {
		BufferedImage img = spec.get().flayer(Resource.imgc).img;
		icon = new TexI(PUtils.uiscale(img, new Coord((iconsz * img.getWidth())/ img.getHeight(), iconsz)));
	    }
	    return(icon);
	}

	public boolean equals(SMarkerType that) {
	    if(Utils.eq(this.spec.name, that.spec.name)) {
		if(that.spec.ver > this.spec.ver) {
		    this.spec = that.spec;
		    this.icon = null;
		}
		return(true);
	    }
	    return(false);
	}
	public boolean equals(Object that) {
	    return((that instanceof SMarkerType) && equals((SMarkerType)that));
	}

	public int hashCode() {
	    return(spec.name.hashCode());
	}

	public int compareTo(SMarkerType that) {
	    return(this.spec.name.compareTo(that.spec.name));
	}
	public int compareTo(MarkerType that) {
	    if(that instanceof SMarkerType)
		return(compareTo((SMarkerType)that));
	    return(super.compareTo(that));
	}
    }

    public static class MarkerConfig {
	public static final MarkerConfig showall = new MarkerConfig();
	public static final MarkerConfig hideall = new MarkerConfig().showsel(true);
	public Set<MarkerType> sel = Collections.emptySet();
	public boolean showsel = false;

	public MarkerConfig() {
	}

	public MarkerConfig(MarkerConfig from) {
	    this.sel = from.sel;
	    this.showsel = from.showsel;
	}

	public MarkerConfig showsel(boolean showsel) {
	    MarkerConfig ret = new MarkerConfig(this);
	    ret.showsel = showsel;
	    return(ret);
	}

	public MarkerConfig add(MarkerType type) {
	    MarkerConfig ret = new MarkerConfig(this);
	    ret.sel = new HashSet<>(ret.sel);
	    ret.sel.add(type);
	    return(ret);
	}

	public MarkerConfig remove(MarkerType type) {
	    MarkerConfig ret = new MarkerConfig(this);
	    ret.sel = new HashSet<>(ret.sel);
	    ret.sel.remove(type);
	    return(ret);
	}

	public MarkerConfig toggle(MarkerType type) {
	    if(sel.contains(type))
		return(remove(type));
	    else
		return(add(type));
	}

	public boolean filter(MarkerType type) {
	    return(sel.contains(type) != showsel);
	}

	public boolean filter(Marker mark) {
	    return(sel.isEmpty() ? showsel : filter(MarkerType.of(mark)));
	}

	public boolean equals(MarkerConfig that) {
	    return(Utils.eq(this.sel, that.sel) && (this.showsel == that.showsel));
	}
	public boolean equals(Object that) {
	    return((that instanceof MarkerConfig) && equals((MarkerConfig)that));
	}
    }

    public static class ListMarker {
	public final Marker mark;
	public MarkerType type;

	public ListMarker(Marker mark) {
	    this.mark = mark;
	    type = MarkerType.of(mark);
	}
    }

    public class MarkerList extends SSearchBox<ListMarker, Widget> {
	public MarkerList(Coord sz) {
	    super(sz, MarkerType.iconsz);
	}

	public List<ListMarker> allitems() {return(markers);}
	public boolean searchmatch(ListMarker lm, String txt) {return(lm.mark.nm.toLowerCase().indexOf(txt.toLowerCase()) >= 0);}

	public class Item extends IconText {
	    public final ListMarker lm;

	    public Item(Coord sz, ListMarker lm) {
		super(sz);
		this.lm = lm;
	    }

	    protected BufferedImage img() {throw(new RuntimeException());}
	    protected String text() {return(lm.mark.nm);}
	    protected boolean valid(String text) {return(Utils.eq(text, text()));}

	    protected void drawicon(GOut g) {
		try {
		    Tex icon = lm.type.icon();
		    if(markcfg.filter(lm.type))
			g.chcolor(255, 255, 255, 128);
		    g.aimage(icon, Coord.of(sz.y / 2), 0.5, 0.5);
		    g.chcolor();
		} catch(Loading l) {
		}
	    }

	    public boolean mousedown(MouseDownEvent ev) {
		if(ev.c.x < sz.y) {
		    toggletype(lm.type);
		    return(true);
		}
		return(super.mousedown(ev));
	    }
	}

	public Widget makeitem(ListMarker lm, int idx, Coord sz) {
	    Widget ret = new ItemWidget<ListMarker>(this, sz, lm);
	    ret.add(new Item(sz, lm), Coord.z);
	    return(ret);
	}

	private void toggletype(MarkerType type) {
	    MarkerConfig nc = markcfg.toggle(type);
	    markcfg = nc;
	    cmarkers = nc.sel.isEmpty() ? null : nc;
	}

	public void change(ListMarker lm) {
	    change2(lm);
	    if(lm != null)
		view.center(new SpecLocator(lm.mark.seg, lm.mark.tc));
	}

	public void change2(ListMarker lm) {
	    this.sel = lm;

	    if(tool.namesel != null) {
		ui.destroy(tool.namesel);
		tool.namesel = null;
		ui.destroy(mremove);
		mremove = null;
		if(colsel != null) {
		    ui.destroy(colsel);
		    colsel = null;
		}
	    }

	    if(lm != null) {
		Marker mark = lm.mark;
		if(tool.namesel == null) {
		    tool.namesel = tool.add(new TextEntry(UI.scale(200), "") {
			    {dshow = true;}
			    public void activate(String text) {
				mark.nm = text;
				view.file.update(mark);
				commit();
				change2(null);
			    }
			});
		}
		tool.namesel.settext(mark.nm);
		tool.namesel.buf.point(mark.nm.length());
		tool.namesel.commit();
		if(mark instanceof PMarker) {
		    PMarker pm = (PMarker)mark;
		    colsel = tool.add(new GroupSelector(Math.max(0, Utils.index(BuddyWnd.gc, pm.color))) {
			    public void changed(int group) {
				pm.color = BuddyWnd.gc[group];
				view.file.update(mark);
			    }
			});
		}
		mremove = tool.add(new Button(UI.scale(200), "Remove", false) {
			public void click() {
			    view.file.remove(mark);
			    change2(null);
			}
		    });
		MapWnd.this.resize(csz());
	    }
	}
    }

    public void resize(Coord sz) {
	sz = sz.max(compact() ? UI.scale(230, 230) : UI.scale(440, 230));
	super.resize(sz);
	tool.resize(sz.y);
	if(!compact()) {
	    tool.c = new Coord(sz.x - tool.sz.x, 0);
	    viewf.resize(tool.pos("bl").subs(22, 0));
	} else {
	    viewf.resize(sz);
	    tool.c = viewf.pos("ur").adds(22, 0);
	}
	view.resize(viewf.inner());
	toolbar.c = viewf.c.add(0, viewf.sz.y - toolbar.sz.y).add(UI.scale(2), UI.scale(-2));
    }

    private boolean compact() {
	return(deco == null);
    }

    public void compact(boolean a) {
	tool.show(!a);
	if(a)
	    delfocusable(tool);
	else
	    newfocusable(tool);
	chdeco(a ? null : makedeco());
	if (a) {
		this.c = smallmapc;
		resize(smallmapsz);
	} else {
		this.c = bigmapc;
		resize(bigmapsz);
	}
	pack();
	this.compact = a;
	if (view != null) view.compact = a;
	if (ui != null && ui.gui != null)
		fixAndSavePos(a); // ND: Do this again to fix the window size when we resize the game window after switching the compact mode and we go back to the other mode. It's for my OCD, ok?
    }

	public void fixAndSavePos(boolean compact) { // ND: Just like preventDraggingOutside() in Window.java, it's spaghetti, but it works on all interface scales guaranteed.
		if (compact) {
			// ND: This prevents us from resizing it larger than the game window size
			if(this.csz().x > ui.gui.sz.x) this.resize(ui.gui.sz.x, this.csz().y);
			if(this.csz().y > ui.gui.sz.y) this.resize(this.csz().x, ui.gui.sz.y);
			// ND: This prevents us from dragging it outside at all
			if (this.c.x < 0) this.c.x = 0;
			if (this.c.y < 0) this.c.y = 0;
			if (this.c.x > (ui.gui.sz.x - this.csz().x)) this.c.x = ui.gui.sz.x - this.csz().x;
			if (this.c.y > (ui.gui.sz.y - this.csz().y)) this.c.y = ui.gui.sz.y - this.csz().y;
			smallmapc = this.c;
			smallmapsz = this.csz();
			Utils.setprefc("smallmapc", smallmapc);
			Utils.setprefc("smallmapsz", smallmapsz);
		} else {
			// ND: This prevents us from resizing it larger than the game window size
			if(this.csz().x > ui.gui.sz.x - dlmrgn.x * 2 - dsmrgn.x) this.resize(ui.gui.sz.x - dlmrgn.x * 2 - dsmrgn.x, this.csz().y);
			if(this.csz().y > ui.gui.sz.y - (dlmrgn.y + dsmrgn.y) * 2) this.resize(this.csz().x, ui.gui.sz.y - (dlmrgn.y+dsmrgn.y) * 2);
			// ND: This prevents us from dragging it outside at all
			if (this.c.x < -dsmrgn.x) this.c.x = -dsmrgn.x;
			if (this.c.y < -dsmrgn.y) this.c.y = -dsmrgn.y;
			if (this.c.x > (ui.gui.sz.x - this.csz().x - (dlmrgn.x + dsmrgn.x) * 2)) this.c.x = ui.gui.sz.x - this.csz().x - (dlmrgn.x + dsmrgn.x) * 2;
			if (this.c.y > (ui.gui.sz.y - this.csz().y - (dlmrgn.y + dsmrgn.y) * 2) - dsmrgn.y) this.c.y = ui.gui.sz.y - this.csz().y - (dlmrgn.y + dsmrgn.y) * 2 - dsmrgn.y;
			bigmapc = this.c;
			bigmapsz = this.csz();
			Utils.setprefc("bigmapc", bigmapc);
			Utils.setprefc("bigmapsz", bigmapsz);
		}
	}

    public void recenter() {
	view.follow(player);
    }

    public void focus(Marker m) {
	for(ListMarker lm : markers) {
	    if(lm.mark == m) {
		tool.list.change2(lm);
		tool.list.display(lm);
		break;
	    }
	}
    }

    protected Deco makedeco() {
	return(new DefaultDeco(true){
		@Override
		public boolean mouseup(MouseUpEvent ev) {
			fixAndSavePos(compact);
			compact(compact);
			if((ev.b == 1) && (szdrag != null)) {
				szdrag.remove();
				szdrag = null;
				return(true);
			}
			return(super.mouseup(ev));
		}
	}.dragsize(true));
    }

	public boolean mouseup(MouseUpEvent ev) {
		fixAndSavePos(compact);
		compact(compact);
		return(super.mouseup(ev));
	}

    public void markobj(long gobid, long oid, Indir<Resource> resid, String nm) {
	synchronized(deferred) {
	    deferred.add(new Runnable() {
		    double f = 0;
		    public void run() {
			Resource res = resid.get();
			String rnm = nm;
			if(rnm == null) {
			    Resource.Tooltip tt = res.layer(Resource.tooltip);
			    if(tt == null)
				return;
			    rnm = tt.t;
			}
			double now = Utils.rtime();
			if(f == 0)
			    f = now;
			Gob gob = ui.sess.glob.oc.getgob(gobid);
			if(gob == null) {
			    if(now - f < 1.0)
				throw(new Loading());
			    return;
			}
			Coord tc = gob.rc.floor(tilesz);
			MCache.Grid obg = ui.sess.glob.map.getgrid(tc.div(cmaps));
			SMarker mark;
			if(!view.file.lock.writeLock().tryLock())
			    throw(new Loading());
			try {
			    MapFile.GridInfo info = view.file.gridinfo.get(obg.id);
			    if(info == null)
				throw(new Loading());
			    Coord sc = tc.add(info.sc.sub(obg.gc).mul(cmaps));
			    SMarker prev = view.file.smarker(res.name, info.seg, sc);
			    if(prev == null) {
				mark = new SMarker(info.seg, sc, rnm, oid, new Resource.Saved(Resource.remote(), res.name, res.ver));
				view.file.add(mark);
			    } else {
				mark = prev;
				if((prev.seg != info.seg) || !eq(prev.tc, sc) || !eq(prev.nm, rnm)) {
				    prev.seg = info.seg;
				    prev.tc = sc;
				    prev.nm = rnm;
				    view.file.update(prev);
				}
			    }
				if (MappingClient.getInstance() != null && OptWnd.uploadMapTilesCheckBox.a) {
					MappingClient.getInstance().uploadSMarker(gob, mark);
				}
			} finally {
			    view.file.lock.writeLock().unlock();
			}
			synchronized(gob) {
			    gob.setattr(new MarkerID(gob, mark));
			}
		    }
		});
	}
    }

    public static class ExportWindow extends Window implements MapFile.ExportStatus {
	private Thread th;
	private volatile String prog = "Exporting map...";

	public ExportWindow() {
	    super(UI.scale(new Coord(300, 65)), "Exporting map...", true);
	    adda(new Button(UI.scale(100), "Cancel", false, this::cancel), csz().x / 2, UI.scale(40), 0.5, 0.0);
	}

	public void run(Thread th) {
	    (this.th = th).start();
	}

	public void cdraw(GOut g) {
	    g.text(prog, UI.scale(new Coord(10, 10)));
	}

	public void cancel() {
	    th.interrupt();
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if(!th.isAlive())
		destroy();
	}

	public void grid(int cs, int ns, int cg, int ng) {
	    this.prog = String.format("Exporting map cut %,d/%,d in segment %,d/%,d", cg, ng, cs, ns);
	}

	public void mark(int cm, int nm) {
	    this.prog = String.format("Exporting marker", cm, nm);
	}
    }

    public static class ImportWindow extends Window {
	private Thread th;
	private volatile String prog = "Initializing";
	private double sprog = -1;

	public ImportWindow() {
	    super(UI.scale(new Coord(300, 65)), "Importing map...", true);
	    adda(new Button(UI.scale(100), "Cancel", false, this::cancel), csz().x / 2, UI.scale(40), 0.5, 0.0);
	}

	public void run(Thread th) {
	    (this.th = th).start();
	}

	public void cdraw(GOut g) {
	    String prog = this.prog;
	    if(sprog >= 0)
		prog = String.format("%s: %d%%", prog, (int)Math.floor(sprog * 100));
	    else
		prog = prog + "...";
	    g.text(prog, UI.scale(new Coord(10, 10)));
	}

	public void cancel() {
	    th.interrupt();
	}

	public void tick(double dt) {
	    super.tick(dt);
	    if(!th.isAlive())
		destroy();
	}

	public void prog(String prog) {
	    this.prog = prog;
	    this.sprog = -1;
	}

	public void sprog(double sprog) {
	    this.sprog = sprog;
	}
    }

    public void exportmap(Path path) {
	GameUI gui = getparent(GameUI.class);
	ExportWindow prog = new ExportWindow();
	Thread th = new HackThread(() -> {
		boolean complete = false;
		try {
		    try {
			try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(path))) {
			    file.export(out, MapFile.ExportFilter.all, prog);
			}
			complete = true;
		    } finally {
			if(!complete)
			    Files.deleteIfExists(path);
		    }
		} catch(IOException e) {
		    e.printStackTrace(Debug.log);
		    gui.error("Unexpected error occurred when exporting map.");
		} catch(InterruptedException e) {
		}
	}, "Mapfile exporter");
	prog.run(th);
	gui.adda(prog, gui.sz.div(2), 0.5, 1.0);
    }

    public void importmap(Path path) {
	GameUI gui = getparent(GameUI.class);
	ImportWindow prog = new ImportWindow();
	Thread th = new HackThread(() -> {
		try {
		    try(SeekableByteChannel fp = Files.newByteChannel(path)) {
			long size = fp.size();
			class Updater extends CountingInputStream {
			    Updater(InputStream bk) {super(bk);}

			    protected void update(long val) {
				super.update(val);
				prog.sprog((double)pos / (double)size);
			    }
			}
			prog.prog("Validating map data");
			file.reimport(new Updater(new BufferedInputStream(Channels.newInputStream(fp))), MapFile.ImportFilter.readonly);
			prog.prog("Importing map data");
			fp.position(0);
			file.reimport(new Updater(new BufferedInputStream(Channels.newInputStream(fp))), MapFile.ImportFilter.all);
		    }
		} catch(InterruptedException e) {
		} catch(Exception e) {
		    e.printStackTrace(Debug.log);
		    gui.error("Could not import map: " + e.getMessage());
		}
	}, "Mapfile importer");
	prog.run(th);
	gui.adda(prog, gui.sz.div(2), 0.5, 1.0);
    }

    public void exportmap() {
	java.awt.EventQueue.invokeLater(() -> {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter("Exported Haven map data", "hmap"));
		if(fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
		    return;
		Path path = fc.getSelectedFile().toPath();
		if(path.getFileName().toString().indexOf('.') < 0)
		    path = path.resolveSibling(path.getFileName() + ".hmap");
		exportmap(path);
	    });
    }

    public void importmap() {
	java.awt.EventQueue.invokeLater(() -> {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter("Exported Haven map data", "hmap"));
		if(fc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
		    return;
		importmap(fc.getSelectedFile().toPath());
	    });
    }

    private Map<String, Console.Command> cmdmap = new TreeMap<String, Console.Command>();
    {
	cmdmap.put("exportmap", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args.length > 1)
			exportmap(Utils.path(args[1]));
		    else
			exportmap();
		}
	    });
	cmdmap.put("importmap", new Console.Command() {
		public void run(Console cons, String[] args) {
		    if(args.length > 1)
			importmap(Utils.path(args[1]));
		    else
			importmap();
		}
	    });
    }
    public Map<String, Console.Command> findcmds() {
	return(cmdmap);
    }

	@Override
	public boolean keydown(KeyDownEvent ev) { // ND: do this to override the escape key being able to close the window
		if(key_esc.match(ev)) {
			return(false);
		}
		if(super.keydown(ev))
			return(true);
		return(false);
	}
}
