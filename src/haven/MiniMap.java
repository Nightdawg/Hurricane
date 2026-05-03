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

import haven.automated.pathfinder.Pathfinder;
import haven.render.*;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.*;
import java.awt.image.*;
import java.awt.Color;
import haven.MapFile.Segment;
import haven.MapFile.DataGrid;
import haven.MapFile.Grid;
import haven.MapFile.GridInfo;
import haven.MapFile.Marker;
import haven.MapFile.PMarker;
import haven.MapFile.SMarker;
import haven.res.ui.obj.buddy.Buddy;
import haven.sprites.MapSprite;

import haven.MapFile.TileInfo;
import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class MiniMap extends Widget {
    public static final Tex bg = Resource.loadtex("gfx/hud/mmap/ptex");
    public static final Tex nomap = Resource.loadtex("gfx/hud/mmap/nomap");
    public static Tex plp;
	public static final Resource.Image plpImg = Resource.local().loadwait("gfx/hud/mmap/plp").layer(Resource.imgc);
	static {
		BufferedImage buf = MiniMap.plpImg.img;
		buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
		Coord tsz;
		if(buf.getWidth() > buf.getHeight())
			tsz = new Coord(GobIcon.size, (GobIcon.size * buf.getHeight()) / buf.getWidth());
		else
			tsz = new Coord((GobIcon.size * buf.getWidth()) / buf.getHeight(), GobIcon.size);
		buf = PUtils.convolve(buf, tsz, GobIcon.filter);
		plp = new TexI(buf);
	}
    public final MapFile file;
    public Markers markers = new Markers(this);
    public Location curloc;
    public Location sessloc;
    public GobIcon.Settings iconconf;
    public List<DisplayIcon> icons = Collections.emptyList();
    protected Locator setloc;
    protected boolean follow;
    protected float zoomlevel = 1, maglevel = 1 << Utils.clip((int)Math.round(Math.log(UI.scale(1.0)) / Math.log(2)), 0, 3);
	public float smallMapZoomLevel = 1;
	public float bigMapZoomLevel = 1;
	public float zoomMomentum = 0;
	private boolean allowZooming = false;
    protected DisplayGrid[] display = {};
    protected Area dgext, dtext;
    protected Segment dseg;
    protected int dlvl, dmag;
    protected Location dloc;
	public boolean compact;
	private static final Color BIOME_BG = new Color(0, 0, 0, 110);
	private String biome;
	private Tex biometex;
    public static boolean showMapViewRange = Utils.getprefb("showMapViewRange", true);
	public static boolean showMapGridLines = Utils.getprefb("showMapGridLines", false);
	private final List<MapSprite> mapSprites = new LinkedList<>();
	private Coord2d lastMineSupportUpdatePos = null;
	private static final double MINE_SUPPORT_UPDATE_THRESHOLD = 11.0 * 5; // 5 tiles, same as fog of war

	// Track mine support gobs for overlay updates
	private final Set<Long> mineSupportGobIds = Collections.synchronizedSet(new HashSet<>());
	private volatile boolean pendingMineSupportUpdate = false;
	private long lastMineSupportUpdateTime = 0;
	private static final long MIN_UPDATE_INTERVAL_MS = 100; // Minimum time between updates (debouncing)
	private OCache.ChangeCallback mineSupportCallback = null;

    public MiniMap(Coord sz, MapFile file) {
	super(sz);
	this.file = file;
    }

    public MiniMap(MapFile file) {
	this(Coord.z, file);
    }

    protected void attached() {
	if(iconconf == null) {
	    GameUI gui = getparent(GameUI.class);
	    if(gui != null)
		iconconf = gui.iconconf;
	}
	super.attached();
    }

    public void destroy() {
	cleanupMineSupportCallback();
	super.destroy();
    }

    public static class Location {
	public final Segment seg;
	public final Coord tc;

	public Location(Segment seg, Coord tc) {
	    Objects.requireNonNull(seg);
	    Objects.requireNonNull(tc);
	    this.seg = seg; this.tc = tc;
	}

	public String toString() {
	    return(String.format("(%d, %d) @ %s", tc.x, tc.y, Long.toUnsignedString(seg.id, 16)));
	}
    }

    public interface Locator {
	Location locate(MapFile file) throws Loading;
    }

    public static class SessionLocator implements Locator {
	public final Session sess;
	private MCache.Grid lastgrid = null;
	private Location lastloc;

	public SessionLocator(Session sess) {this.sess = sess;}

	public Location locate(MapFile file) {
	    MCache map = sess.glob.map;
	    if(lastgrid != null) {
		synchronized(map.grids) {
		    if(map.grids.get(lastgrid.gc) == lastgrid) {
			GridInfo info = file.gridinfo.get(lastgrid.id);
			if((info != null) && (info.seg == lastloc.seg.id))
			    return(lastloc);
		    }
		}
		lastgrid = null;
		lastloc = null;
	    }
	    Collection<MCache.Grid> grids = new ArrayList<>();
	    synchronized(map.grids) {
		grids.addAll(map.grids.values());
	    }
	    for(MCache.Grid grid : grids) {
		GridInfo info = file.gridinfo.get(grid.id);
		if(info == null)
		    continue;
		Segment seg = file.segments.get(info.seg);
		if(seg != null) {
		    Location ret = new Location(seg, info.sc.sub(grid.gc).mul(cmaps));
		    lastgrid = grid;
		    lastloc = ret;
		    return(ret);
		}
	    }
	    throw(new Loading("No mapped grids found."));
	}
    }

    public static class MapLocator implements Locator {
	public final MapView mv;

	public MapLocator(MapView mv) {this.mv = mv;}

	public Location locate(MapFile file) {
	    Coord mc = new Coord2d(mv.getcc()).floor(MCache.tilesz);
	    if(mc == null)
		throw(new Loading("Waiting for initial location"));
	    MCache.Grid plg = mv.ui.sess.glob.map.getgrid(mc.div(cmaps));
	    GridInfo info = file.gridinfo.get(plg.id);
	    if(info == null)
		throw(new Loading("No grid info, probably coming soon"));
	    Segment seg = file.segments.get(info.seg);
	    if(seg == null)
		throw(new Loading("No segment info, probably coming soon"));
	    return(new Location(seg, info.sc.mul(cmaps).add(mc.sub(plg.ul))));
	}
    }

    public static class SpecLocator implements Locator {
	public final long seg;
	public final Coord tc;

	public SpecLocator(long seg, Coord tc) {this.seg = seg; this.tc = tc;}

	public Location locate(MapFile file) {
	    Segment seg = file.segments.get(this.seg);
	    if(seg == null)
		return(null);
	    return(new Location(seg, tc));
	}
    }

    public static class MarkerIcon implements ItemInfo.Owner, ItemInfo.Name.Dynamic {
	public final Markers o;
	public final Marker m;
	private final Loader loader;
	private Loader.Future<GobIcon.Icon> load;
	private GobIcon.Icon icon;
	private int lseq, iseq;

	public MarkerIcon(Markers o, Marker m) {
	    this.o = o;
	    this.m = m;
	    this.loader = o.mm.ui.loader;
	}

	private static final OwnerContext.ClassResolver<MarkerIcon> ctxr = new OwnerContext.ClassResolver<MarkerIcon>()
	    .add(Marker.class, i -> i.m)
	    .add(MiniMap.class, i -> i.o.mm)
	    .add(UI.class, i -> i.o.mm.ui)
	    .add(Glob.class, i -> i.o.mm.ui.sess.glob)
	    .add(Session.class, i -> i.o.mm.ui.sess);
	public <T> T context(Class<T> cl) {
	    return(ctxr.context(cl, this));
	}

	private GobIcon.Icon create() {
	    if(m instanceof PMarker) {
		return(new Flag(this, ((PMarker)m).color, m.nm));
	    } else {
		SMarker sm = (SMarker)m;
		Resource res = sm.res.get();
		return(GobIcon.getfac(res).create(this, res, new MessageBuf(sm.data)));
	    }
	}

	private void ckload() {
	    /* XXX: Arguably, the loader task should do this part itself. */
	    if(load.done()) {
		icon = load.get();
		iseq = lseq;
		load = null;
		info = null;
		o.seq++;
	    }
	}

	private void update() {
	    int nseq = m.seq;
	    boolean reload = false;
	    if(load == null) {
		reload = (nseq != this.iseq);
	    } else {
		if(nseq != this.lseq)
		    reload = true;
		else
		    ckload();
	    }
	    if(reload) {
		if(load != null)
		    load.cancel();
		load = loader.defer(this::create);
		lseq = nseq;
	    }
	}

	public GobIcon.Icon icon() {
	    synchronized(o) {
		if((load == null) && (icon == null)) {
		    load = loader.defer(this::create);
		    lseq = o.mseq;
		    o.loading = true;
		}
		if(load != null)
		    ckload();
		if(icon == null)
		    throw(new Loading());
		return(icon);
	    }
	}

	public String name() {
	    return(m.nm);
	}

	private List<ItemInfo> info = null;
	public List<ItemInfo> info() {
	    if(info == null) {
		Object[] raw = icon().info(this);
		info = ItemInfo.buildinfo(this, raw);
	    }
	    return(info);
	}
    }

    public static class Markers {
	public final MiniMap mm;
	public int seq;
	private final Map<Marker, MarkerIcon> icons = new HashMap<>();
	private volatile int mseq = -1;
	private volatile Future<?> updater = null;
	private boolean loading;

	private Markers(MiniMap mm) {
	    this.mm = mm;
	}

	private void update0() {
	    boolean loading = false;
	    try(Locked lk = new Locked(mm.file.lock.readLock())) {
		int nseq = mm.file.markerseq;
		Set<Marker> current = new HashSet<>(mm.file.markers);
		synchronized(this) {
		    for(Iterator<Map.Entry<Marker, MarkerIcon>> i = icons.entrySet().iterator(); i.hasNext();) {
			Map.Entry<Marker, MarkerIcon> ent = i.next();
			Marker m = ent.getKey();
			MarkerIcon st = ent.getValue();
			if(current.contains(m)) {
			    current.remove(m);
			    st.update();
			    if(st.load != null)
				loading = true;
			} else {
			    i.remove();
			}
		    }
		    boolean ch = false;
		    for(Marker m : current) {
			MarkerIcon st = new MarkerIcon(this, m);
			icons.put(m, st);
			st.update();
			if(st.load != null)
			    loading = true;
			ch = true;
		    }
		    mseq = nseq;
		    if(ch)
			seq++;
		}
	    } finally {
		this.loading = loading;
		updater = null;
	    }
	}

	private void update() {
	    if((mseq != mm.file.markerseq) || loading) {
		if(updater == null)
		    updater = Defer.later(this::update0, null);
	    }
	}

	public MarkerIcon get(Marker m) {
	    synchronized(this) {
		update();
		return(icons.computeIfAbsent(m, k -> new MarkerIcon(this, k)));
	    }
	}

	public Collection<? extends MarkerIcon> known() {
	    return(icons.values());
	}
    }

    public void center(Location loc) {
	curloc = loc;
    }

    public Location resolve(Locator loc) {
	if(!file.lock.readLock().tryLock())
	    throw(new Loading("Map file is busy"));
	try {
	    return(loc.locate(file));
	} finally {
	    file.lock.readLock().unlock();
	}
    }

    public Coord xlate(Location loc) {
	Location dloc = this.dloc;
	if((dloc == null) || (dloc.seg != loc.seg))
	    return(null);
	return(loc.tc.sub(dloc.tc).div(scalef()).add(sz.div(2)));
    }

    public Location xlate(Coord sc) {
	Location dloc = this.dloc;
	if(dloc == null)
	    return(null);
	Coord tc = sc.sub(sz.div(2)).mul(scalef()).add(dloc.tc);
	return(new Location(dloc.seg, tc));
    }

    private Locator sesslocator;
    public void tick(double dt) {
	if(setloc != null) {
	    try {
		Location loc = resolve(setloc);
		center(loc);
		if(!follow)
		    setloc = null;
	    } catch(Loading l) {
	    }
	}
	if((sesslocator == null) && (ui != null) && (ui.sess != null))
	    sesslocator = new SessionLocator(ui.sess);
	if(sesslocator != null) {
	    try {
		sessloc = resolve(sesslocator);
	    } catch(Loading l) {
	    }
	}
	icons = findicons(icons);

		if (GLPanel.Loop.bgmode) {
			zoomMomentum = 0.0f;
		} else if (Math.abs(zoomMomentum) > 0.15) {
			double delta = dt*zoomMomentum*(zoomlevel/6f);
			int nextdlvl = Math.max(Integer.highestOneBit((int)(zoomlevel+delta)),1);
			if (zoomMomentum > 0 && nextdlvl > dlvl && !allowzoomout()) {
				//zoomlevel = zoomlevel*0.98f; // ND: I wonder why matias did it like this, I don't think this is necessary
				zoomMomentum = 0;
			} else {
				zoomlevel += delta;
				zoomMomentum *= 1-(5*dt);
			}
		}

		if (zoomlevel <= 0.1f) { // ND: I had to change this from 0. I don't remember it bugging out in matias' client, but I could zoom in infinitely in mine, like it never reached 0, ever. 0.1 seems perfect
			zoomlevel = 0.1f;
			zoomMomentum = 0;
		}

		Coord mc = rootxlate(ui.mc);
		if(mc.isect(Coord.z, sz)) {
			setBiome(xlate(mc));
		} else {
			setBiome(null);
		}
		allowZooming = true;
		ticksprites(dt);
    }

    public void center(Locator loc) {
	setloc = loc;
	follow = false;
    }

    public void follow(Locator loc) {
	setloc = loc;
	follow = true;
    }

    public static class Scale2D implements Pipe.Op {
	public final Coord cc;
	public final float f;

	public Scale2D(Coord cc, float f) {
	    this.cc = cc;
	    this.f = f;
	}

	public void apply(Pipe buf) {
	    Ortho2D st = (Ortho2D)buf.get(States.vxf);
	    float w = st.r - st.l, h = st.b - st.u;
	    buf.prep(new Ortho2D(cc.x + ((st.l - cc.x) / f), cc.y + ((st.u - cc.y) / f),
				 cc.x + ((st.r - cc.x) / f), cc.y + ((st.b - cc.y) / f)));
	}
    }

    public static final Color notifcol = new Color(255, 128, 0, 255);
    public class DisplayIcon {
	public final GobIcon attr;
	public final Gob gob;
	public final GobIcon.Icon icon;
	public final GobIcon.Setting conf;
	public Coord2d rc = null;
	public Coord sc = null;
	public double ang = 0.0;
	public int z;
	public double stime, ntime;
	public boolean notify;
	private Consumer<UI> snotify;
	private boolean markchecked;

	public DisplayIcon(GobIcon attr, GobIcon.Setting conf) {
	    this.attr = attr;
	    this.gob = attr.gob;
	    this.icon = attr.icon();
	    this.z = icon.z();
	    this.stime = ui.lasttick;
	    this.conf = conf;
	    if(this.notify = conf.notify)
		this.snotify = conf.notification();
	}

	public void update(Coord2d rc, double ang) {
	    this.rc = rc;
	    this.ang = ang;
	    if(notify) {
		if((ntime = (ui.lasttick - stime) * 0.5) > 1.0) {
		    notify = false;
		    snotify = null;
		}
	    }
	}

	public void dispupdate() {
	    if((this.rc == null) || (sessloc == null) || (dloc == null) || (dloc.seg != sessloc.seg))
		this.sc = null;
	    else
		this.sc = p2c(this.rc);
	}

	public void draw(GOut g) {
	    icon.draw(g, sc);
	    if(notify) {
		double f = 1.0 + (Math.pow(Math.sin(ntime * Math.PI * 1.5), 2) * 1.0);
		double a = (ntime < 0.5) ? 0.5 : (0.5 - (ntime - 0.5));
		g.usestate(new ColorMask(notifcol));
		g.usestate(new Scale2D(sc.add(g.tx), (float)f));
		g.chcolor(255, 255, 255, (int)Math.round(255 * a));
		icon.draw(g, sc);
		g.defstate();
	    }
	    if(snotify != null) {
		snotify.accept(ui);
		snotify = null;
	    }
	}

	public boolean force() {
	    if(notify)
		return(true);
	    return(false);
	}

		public Object tooltip() {
			return icon.tooltip();
		}
    }

    public static class MarkerID extends GAttrib {
	public final Marker mark;

	public MarkerID(Gob gob, Marker mark) {
	    super(gob);
	    this.mark = mark;
	}

	public static Gob find(OCache oc, Marker mark) {
	    synchronized(oc) {
		for(Gob gob : oc) {
		    MarkerID iattr = gob.getattr(MarkerID.class);
		    if((iattr != null) && (iattr.mark == mark))
			return(gob);
		}
	    }
	    return(null);
	}
    }

    public static class Flag extends GobIcon.Icon {
	public static final Resource res = Resource.local().loadwait("gfx/hud/mmap/flag");
	public static final Resource.Image fg = res.flayer(Resource.imgc, 0);
	public static final Resource.Image bg = res.flayer(Resource.imgc, 1);
	public static final Coord cc = UI.scale(res.flayer(Resource.negc).cc);
	public final Color col;
	public final String name;

	public Flag(OwnerContext owner, Color col, String name) {
	    super(owner, res);
	    this.col = col;
	    this.name = name;
	}

	public String name() {
	    return(name);
	}

	public BufferedImage image() {
	    WritableRaster buf = PUtils.imgraster(bg.sz);
	    PUtils.colmul(PUtils.blit(buf, fg.img.getRaster(), fg.o), col);
	    PUtils.alphablit(buf, bg.img.getRaster(), bg.o);
	    return(PUtils.rasterimg(buf));
	}

	public void draw(GOut g, Coord c) {
	    Coord ul = c.sub(cc);
	    g.chcolor(col);
	    g.image(fg, ul);
	    g.chcolor();
	    g.image(bg, ul);
	}

	public boolean checkhit(Coord c) {
	    return(c.isect(cc.inv(), bg.ssz));
	}

	public Object[] id() {
	    return(new Object[] {col});
	}
    }

    public static class DisplayMarker {
	public final MiniMap mm;
	public final Marker m;
    public final Text tip;
    public static HashMap<String, Tex> titleTexMap = new HashMap<String, Tex>();
	public Coord sc = null;

	public DisplayMarker(MiniMap mm, Marker marker) {
	    this.mm = mm;
	    this.m = marker;
        this.tip = Text.render(m.nm);
        if (!titleTexMap.containsKey(tip.text))
            titleTexMap.put(tip.text, Text.renderstroked(tip.text, Color.white, Color.BLACK, Text.num12boldFnd).tex());
	}

	public GobIcon.Icon icon() {
	    return(mm.markers.get(m).icon());
	}

	public void draw(GOut g, Coord c) {
	    try {
		icon().draw(g, c);
	    } catch(Loading l) {}
	}

	private int tseq = -1;
	private BufferedImage tooltip = null;
	public BufferedImage tooltip() {
	    MarkerIcon minf = mm.markers.get(m);
	    if((tooltip == null) || (minf.iseq != tseq)) {
		tooltip = ItemInfo.longtip(minf.info());
		tseq = minf.iseq;
	    }
	    return(tooltip);
	}
    }

    public static class DisplayGrid {
	public final MiniMap mm;
	public final MapFile file;
	public final Segment seg;
	public final Coord sc;
	public final Area mapext;
	public final Indir<? extends DataGrid> gref;
	public Coord dc;
	private Tex img = null;
	private Defer.Future<Tex> nextimg = null;

	public DisplayGrid(MiniMap mm, Segment seg, Coord sc, int lvl, Indir<? extends DataGrid> gref) {
	    this.mm = mm;
	    this.file = seg.file();
	    this.seg = seg;
	    this.sc = sc;
	    this.gref = gref;
	    mapext = Area.sized(sc.mul(cmaps.mul(1 << lvl)), cmaps.mul(1 << lvl));
	}

	class CachedImage {
	    final Function<DataGrid, Defer.Future<Tex>> src;
	    DataGrid cgrid;
	    Defer.Future<Tex> next;
	    Tex img;

	    CachedImage(Function<DataGrid, Defer.Future<Tex>> src) {
		this.src = src;
	    }

	    public Tex get() {
		DataGrid grid = gref.get();
		if(grid != cgrid || !valid()) {
		    if(next != null)
			next.cancel();
			next = getNext(grid);
		    cgrid = grid;
		}
		if(next != null) {
		    try {
			img = next.get();
		    } catch(Loading l) {}
		}
		return(img);
	    }

		protected Defer.Future<Tex> getNext(DataGrid grid) {
			return src.apply(grid);
		}

		protected boolean valid() {return true;}
	}

		class CachedTileOverlay extends MiniMap.DisplayGrid.CachedImage {
			private long seq = 0;
			CachedTileOverlay(Function<MapFile.DataGrid, Defer.Future<Tex>> src) {
				super(src);
			}

			@Override
			protected boolean valid() {
				return this.seq == TileHighlight.seq;
			}

			@Override
			protected Defer.Future<Tex> getNext(DataGrid grid) {
				this.seq = TileHighlight.seq;
				return super.getNext(grid);
			}
		}

	private CachedImage img_c;
	public Tex img() {
	    if(img_c == null) {
		img_c = new CachedImage(grid -> {
			if(grid instanceof MapFile.ZoomGrid) {
			    return(Defer.later(() -> new TexI(grid.render(sc.mul(cmaps)))));
			} else {
			    return(Defer.later(new Defer.Callable<Tex>() {
				    MapFile.View view = new MapFile.View(seg);

				    public TexI call() {
					try(Locked lk = new Locked(file.lock.readLock())) {
					    for(int y = -1; y <= 1; y++) {
						for(int x = -1; x <= 1; x++) {
						    view.addgrid(sc.add(x, y));
						}
					    }
					    view.fin();
					    return(new TexI(MapSource.drawmap(view, Area.sized(sc.mul(cmaps), cmaps))));
					}
				    }
				}));
			}
		});
	    }
	    return(img_c.get());
	}

	private final Map<String, CachedImage> olimg_c = new HashMap<>();
	public Tex olimg(String tag) {
	    CachedImage ret;
	    synchronized(olimg_c) {
		if((ret = olimg_c.get(tag)) == null)
		    olimg_c.put(tag, ret = new CachedImage(grid -> Defer.later(() -> new TexI(grid.olrender(sc.mul(cmaps), tag)))));
	    }
	    return(ret.get());
	}
	public Tex tileimg() {
		CachedImage ret;
		synchronized(olimg_c) {
			if((ret = olimg_c.get(TileHighlight.TAG)) == null)
				olimg_c.put(TileHighlight.TAG, ret = new CachedTileOverlay(grid -> Defer.later(() -> new TexI(TileHighlight.olrender(grid)))));
		}
		return(ret.get());
	}

	public void clearCache() {
		img_c = null;
		synchronized(olimg_c) {
			olimg_c.clear();
		}
	}

	private Collection<DisplayMarker> markers = Collections.emptyList();
	private int markerseq = -1;
	public Collection<DisplayMarker> markers(boolean remark) {
	    if(remark && (markerseq != file.markerseq)) {
		if(file.lock.readLock().tryLock()) {
		    try {
			ArrayList<DisplayMarker> marks = new ArrayList<>();
			for(Marker mark : file.markers) {
			    if((mark.seg == this.seg.id) && mapext.contains(mark.tc))
				marks.add(new DisplayMarker(mm, mark));
			}
			marks.trimToSize();
			markers = (marks.size() == 0) ? Collections.emptyList() : marks;
			markerseq = file.markerseq;
		    } finally {
			file.lock.readLock().unlock();
		    }
		}
	    }
	    return(markers);
	}
    }

    private float scalef() {
	return(UI.unscale((zoomlevel)));
    }

    private Coord l2dscale(Coord c) {
	return(c.mul(dmag).div(1 << dlvl));
    }

    private Coord scalec(Coord c) {
        int f = dlvl - dmag;
        if(f < 0)
            return(c.div(1 << -f));
        else
            return(c.mul(1 << f));
    }

    private Coord d2lscale(Coord c) {
        return(c.mul(1 << dlvl).div(dmag));
    }

    public Coord st2c(Coord tc) {
	return(UI.scale(tc.add(sessloc.tc).sub(dloc.tc).div(zoomlevel)).add(sz.div(2)));
    }

    public Coord p2c(Coord2d pc) {
	return(st2c(pc.floor(tilesz)));
    }

	public int calcDrawLevel() {
	return Math.max(Integer.highestOneBit((int)zoomlevel), 1);
	}

    private void redisplay(Location loc) {
	Coord hsz = sz.div(2);
	int safezoom = calcDrawLevel();
	Coord zmaps = cmaps.mul(safezoom);
	Area next = Area.sized(loc.tc.sub(hsz.mul(UI.unscale((safezoom)))).div(zmaps).sub(2, 2),
	    UI.unscale(sz).div(cmaps).add(6, 6));
	if((display == null) || (loc.seg != dseg) || (dlvl != calcDrawLevel()) || !next.equals(dgext)) {
	    DisplayGrid[] nd = new DisplayGrid[next.rsz()];
	    if((display != null) && (loc.seg == dseg) && (dlvl == calcDrawLevel())) {
		for(Coord c : dgext) {
		    if(next.contains(c))
			nd[next.ri(c)] = display[dgext.ri(c)];
		}
	    }
	    display = nd;
	    dseg = loc.seg;
	    dlvl = calcDrawLevel();
	    dgext = next;
	    dtext = Area.sized(next.ul.mul(zmaps), next.sz().mul(zmaps));
		zoomMomentum = 0;
	}
	dloc = loc;
	if(file.lock.readLock().tryLock()) {
	    try {
			// the level here specifies which sized saved maps we should load
			// if you love bitwise operations like loftar you would probably not need to read that
			// 31-NOLZ finds a dirty reverse power of 2, I.E turns 32 -> 5, 16 -> 4, 8 -> 3, 4 -> 2, 2 -> 1, 1 -> 0
			int lvl = dlvl < 1f ? 0 : 31-Integer.numberOfLeadingZeros(dlvl);
		for(Coord c : dgext) {
		    if(display[dgext.ri(c)] == null)
			display[dgext.ri(c)] = new DisplayGrid(this, dloc.seg, c, lvl, dloc.seg.grid(lvl, c.mul(dlvl)));
		}
	    } finally {
		file.lock.readLock().unlock();
	    }
	}
	for(DisplayIcon icon : icons)
	    icon.dispupdate();
    }

    public void drawgrid(GOut g, Coord ul, DisplayGrid disp) {
	try {
	    disp.dc = ul;
	    Tex img = disp.img();
	    if(img != null)
		g.image(img, ul, UI.scale(img.sz().mul(dlvl).divUpFloor(zoomlevel)));
	} catch(Loading l) {
	}
    }

    public void drawmap(GOut g) {
	Coord hsz = sz.div(2);
	for(Coord c : dgext) {
	    Coord ul = UI.scale(c.mul(cmaps)).mul(dlvl).div(zoomlevel).sub(dloc.tc.div(scalef())).add(hsz);
	    DisplayGrid disp = display[dgext.ri(c)];
	    if(disp == null)
		continue;
	    drawgrid(g, ul, disp);
	}
    }

    public void drawmarkers(GOut g) {
	Coord hsz = sz.div(2);
	for(Coord c : dgext) {
	    DisplayGrid dgrid = display[dgext.ri(c)];
	    if(dgrid == null)
		continue;
	    for(DisplayMarker mark : dgrid.markers(true)) {
		if(filter(mark))
		    continue;
        mark.sc = mark.m.tc.sub(dloc.tc).div(scalef()).add(hsz);
		mark.draw(g, mark.sc);
		if (!compact) {
			if (OptWnd.showMapMarkerNamesCheckBox.a)
			g.image(DisplayMarker.titleTexMap.get(mark.tip.text), mark.m.tc.sub(dloc.tc).div(scalef()).add(hsz).add(-mark.tip.text.length()*3,-30));
		}
	    }
	}
    }

    public List<DisplayIcon> findicons(Collection<? extends DisplayIcon> prev) {
	if((ui.sess == null) || (iconconf == null))
	    return(Collections.emptyList());
	Map<GobIcon, DisplayIcon> pmap = Collections.emptyMap();
	if(prev != null) {
	    pmap = new HashMap<>();
	    for(DisplayIcon disp : prev)
		pmap.put(disp.attr, disp);
	}
	List<DisplayIcon> ret = new ArrayList<>();
	OCache oc = ui.sess.glob.oc;
	synchronized(oc) {
	    for(Gob gob : oc) {
		try {
		    GobIcon icon = gob.getattr(GobIcon.class);
		    if(icon != null) {
			GobIcon.Setting conf = iconconf.get(icon.icon());
			if((conf != null) && conf.show) {
			    DisplayIcon disp = pmap.remove(icon);
			    if(disp == null)
				disp = new DisplayIcon(icon, conf);
			    ret.add(disp);
			}
		    }
		} catch(Loading l) {}
	    }
	}
	for(DisplayIcon disp : pmap.values()) {
	    if(disp.force())
		ret.add(disp);
	}
	for(DisplayIcon disp : ret)
	    disp.update(disp.gob.rc, disp.gob.a);
	Collections.sort(ret, (a, b) -> a.z - b.z);
	if(ret.size() == 0)
	    return(Collections.emptyList());
	return(ret);
    }

    public void drawicons(GOut g) {
	if((sessloc == null) || (dloc.seg != sessloc.seg))
	    return;
	for(DisplayIcon disp : icons) {
	    if((disp.sc == null) || filter(disp))
		continue;
	    disp.draw(g);
	}
	g.chcolor();
    }

    public void remparty() {
	Map<Long, Party.Member> memb = ui.sess.glob.party.memb;
	if(memb.isEmpty()) {
	    /* XXX: This is a bit of a hack to avoid unknown-player
	     * notifications only before initial party information has
	     * been received. Not sure if there's a better
	     * solution. */
	    icons.clear();
	    return;
	}
	for(Iterator<DisplayIcon> it = icons.iterator(); it.hasNext();) {
	    DisplayIcon icon = it.next();
	    if(memb.containsKey(icon.gob.id))
		it.remove();
	}
    }

    public void drawparty(GOut g) {
	for(Party.Member m : ui.sess.glob.party.memb.values()) {
	    try {
		Coord2d ppc = m.getc();
		if(ppc == null)
		    continue;
		Coord p2cppc = p2c(ppc);
		g.chcolor(m.col.getRed(), m.col.getGreen(), m.col.getBlue(), 255);
		g.rotimage(plp, p2c(ppc), plp.sz().div(2), -m.geta() - (Math.PI / 2));
		g.chcolor();
			if (!compact) {
				String name;
				if (GameUI.gobIdToKinName.containsKey(m.gobid)) {
					name = GameUI.gobIdToKinName.get(m.gobid);
					g.image(Text.renderstroked(name, Color.white, Color.BLACK, Text.num12boldFnd).tex(),p2cppc.add(-name.length()*4,-30));
				} else if (m.getgob() != null) {
					Buddy buddyInfo = m.getgob().getattr(Buddy.class);
					if (buddyInfo != null) {
						name = buddyInfo.rnm;
						if (name == null && buddyInfo.customName!= null)
							name = buddyInfo.customName;
						if (!GameUI.gobIdToKinName.containsKey(m.gobid) && name != null) {
							GameUI.gobIdToKinName.put(m.gobid, name);
						}
					}
				}
			}
	    } catch(Loading l) {}
	}
    }

    private void updateMineSupportOverlays() {
        try {
            if (OptWnd.showMineSupportCoverageCheckBox == null || !OptWnd.showMineSupportCoverageCheckBox.a) {
                return;
            }
            GameUI gui = getparent(GameUI.class);
            if (gui == null || gui.map == null || ui == null || ui.sess == null || ui.sess.glob == null) {
                return;
            }

            Gob player = gui.map.player();
            if (player == null) {
                return;
            }

            Coord2d playerPos = player.rc;

            if (lastMineSupportUpdatePos != null) {
                double dist = playerPos.dist(lastMineSupportUpdatePos);
                if (dist < MINE_SUPPORT_UPDATE_THRESHOLD) {
                    return;
                }
            }

            lastMineSupportUpdatePos = playerPos;
            performMineSupportUpdate();
        } catch (Exception ignored) {}
    }

    private static boolean isMineSupport(Gob gob) {
        return GroundSupportOverlay.supportsMineCoverage(gob);
    }

    private void performMineSupportUpdate() {
        if (ui == null || ui.sess == null || ui.sess.glob == null) {
            return;
        }

        if (OptWnd.showMineSupportCoverageCheckBox == null || !OptWnd.showMineSupportCoverageCheckBox.a) {
            return;
        }

        GroundSupportOverlay overlay = GroundSupportOverlay.getInstance();
        overlay.setMap(ui.sess.glob.map);
        overlay.clear();
        mineSupportGobIds.clear();

        ui.sess.glob.oc.gobAction(gob -> {
            if (isMineSupport(gob)) {
                overlay.addGobCoverage(gob);
                mineSupportGobIds.add(gob.id);
            }
        });
    }

    private void requestMineSupportUpdate() {
        pendingMineSupportUpdate = true;
    }

    private void processMineSupportUpdates() {
        if (!pendingMineSupportUpdate) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMineSupportUpdateTime < MIN_UPDATE_INTERVAL_MS) {
            return;
        }

        pendingMineSupportUpdate = false;
        lastMineSupportUpdateTime = currentTime;
        performMineSupportUpdate();
    }

    private void setupMineSupportCallback() {
        if (mineSupportCallback != null || ui == null || ui.sess == null || ui.sess.glob == null) {
            return;
        }

        mineSupportCallback = new OCache.ChangeCallback() {
            @Override
            public void added(Gob gob) {
                if (OptWnd.showMineSupportCoverageCheckBox != null && OptWnd.showMineSupportCoverageCheckBox.a) {
                    requestMineSupportUpdate();
                }
            }

            @Override
            public void removed(Gob gob) {
                if (OptWnd.showMineSupportCoverageCheckBox != null && OptWnd.showMineSupportCoverageCheckBox.a && mineSupportGobIds.remove(gob.id)) {
                    requestMineSupportUpdate();
                }
            }
        };

        ui.sess.glob.oc.callback(mineSupportCallback);
    }

    private void cleanupMineSupportCallback() {
        if (mineSupportCallback != null && ui != null && ui.sess != null && ui.sess.glob != null) {
            ui.sess.glob.oc.uncallback(mineSupportCallback);
            mineSupportCallback = null;
        }
        mineSupportGobIds.clear();
    }

    public void handleMineSupportOverlays() {
        setupMineSupportCallback();
        processMineSupportUpdates();
        updateMineSupportOverlays();
    }

    public void drawparts(GOut g){
	drawmap(g);
	drawmarkers(g);
	drawmovequeue(g);
	if(showMapViewRange) {drawview(g);}
	if(showMapGridLines && dlvl <= 6) {drawgridlines(g);}
	if(dlvl <= 3)
	    drawicons(g);
	drawparty(g);
	drawbiome(g);
	drawsprites(g);
    }

    public void draw(GOut g) {
	Location loc = this.curloc;
	if(loc == null)
	    return;
	redisplay(loc);
	remparty();
	drawparts(g);
    }

    private static boolean hascomplete(DisplayGrid[] disp, Area dext, Coord c) {
	DisplayGrid dg = disp[dext.ri(c)];
	if(dg == null)
	    return(false);
	return(dg.gref.get() != null);
    }

    protected boolean allowzoomout() {
	DisplayGrid[] disp = this.display;
	Area dext = this.dgext;
	if(dext == null)
	    return(false);
	try {
	    for(int x = dext.ul.x; x < dext.br.x; x++) {
		if(hascomplete(disp, dext, new Coord(x, dext.ul.y)) ||
		   hascomplete(disp, dext, new Coord(x, dext.br.y - 1)))
		    return(true);
	    }
	    for(int y = dext.ul.y; y < dext.br.y; y++) {
		if(hascomplete(disp, dext, new Coord(dext.ul.x, y)) ||
		   hascomplete(disp, dext, new Coord(dext.br.x - 1, y)))
		    return(true);
	    }
	} catch(Loading l) {
	    return(false);
	}
	return(false);
    }

    public DisplayIcon iconat(Coord c) {
	for(ListIterator<DisplayIcon> it = icons.listIterator(icons.size()); it.hasPrevious();) {
	    DisplayIcon disp = it.previous();
	    if((disp.sc != null) && disp.icon.checkhit(c.sub(disp.sc)) && !filter(disp))
		return(disp);
	}
	return(null);
    }

    public DisplayGrid gridat(Coord sc) {
	if((dloc == null) || (dgext == null))
	    return(null);
	Coord hsz = sz.div(2);
	Coord gc = dloc.tc.add(scalec(sc.sub(hsz))).div(cmaps.mul(1 << dlvl));
	if(!dgext.contains(gc))
	    return(null);
	return(display[dgext.ri(gc)]);
    }

    public DisplayMarker findmarker(Marker rm) {
	for(DisplayGrid dgrid : display) {
	    if(dgrid == null)
		continue;
	    for(DisplayMarker mark : dgrid.markers(false)) {
		if(mark.m == rm)
		    return(mark);
	    }
	}
	return(null);
    }

    public DisplayMarker markerat(Coord tc) {
	for(DisplayGrid dgrid : display) {
	    if(dgrid == null)
		continue;
	    for(DisplayMarker mark : dgrid.markers(false)) {
        try {
		if(mark.icon().checkhit(tc.sub(mark.m.tc).div(scalef())) && !filter(mark))
		    return(mark);
	    } catch(Loading l){}
        }
	}
	return(null);
    }

    public void markobjs() {
	for(DisplayIcon icon : icons) {
	    try {
		if(icon.markchecked)
		    continue;
		GobIcon aicon = icon.attr;
		Resource res = aicon.res.get();
		GobIcon.Icon micon = icon.icon;
		if(!icon.conf.getmarkablep()) {
		    icon.markchecked = true;
		    continue;
		}
		Coord tc = icon.gob.rc.floor(tilesz);
		MCache.Grid obg = ui.sess.glob.map.getgrid(tc.div(cmaps));
		if(!file.lock.writeLock().tryLock())
		    continue;
		SMarker mid = null;
		try {
		    MapFile.GridInfo info = file.gridinfo.get(obg.id);
		    if(info == null)
			continue;
		    Coord sc = tc.add(info.sc.sub(obg.gc).mul(cmaps));
		    SMarker prev = file.smarker(res.name, info.seg, sc);
		    if(prev == null) {
			if(icon.conf.getmarkp()) {
			    mid = new SMarker(file, info.seg, sc, micon.name(), UID.nil, new Resource.Saved(Resource.remote(), res.name, res.ver), aicon.sdt);
			    file.add(mid);
			} else {
			    mid = null;
			}
		    } else {
			if(!Arrays.equals(prev.data, aicon.sdt)) {
			    prev.data = aicon.sdt;
			    file.update(prev);
			}
			mid = prev;
		    }
		} finally {
		    file.lock.writeLock().unlock();
		}
		if(mid != null) {
		    synchronized(icon.gob) {
			icon.gob.setattr(new MarkerID(icon.gob, mid));
		    }
		}
		icon.markchecked = true;
	    } catch(Loading l) {
		continue;
	    }
	}
    }

    public boolean filter(DisplayIcon icon) {
	MarkerID iattr = icon.gob.getattr(MarkerID.class);
	if((iattr != null) && (findmarker(iattr.mark) != null))
	    return(true);
	return(false);
    }

    public boolean filter(DisplayMarker marker) {
	return(false);
    }

    public boolean clickloc(Location loc, int button, boolean press) {
	return(false);
    }

    public boolean clickicon(DisplayIcon icon, Location loc, int button, boolean press) {
	return(false);
    }

    public boolean clickmarker(DisplayMarker mark, Location loc, int button, boolean press) {
	return(false);
    }

    private UI.Grab drag;
    private boolean dragging;
    private Coord dsc, dmc;
    public boolean dragp(int button) {
	return(button == 1);
    }

    private Location dsloc;
    private DisplayIcon dsicon;
    private DisplayMarker dsmark;
    public boolean mousedown(MouseDownEvent ev) {
	dsloc = xlate(ev.c);
	if(dsloc != null) {
		if (ui.modmeta && ev.b == 1){
			ui.gui.map.addCheckpoint(dsloc.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)));
		}
	    dsicon = iconat(ev.c);
	    dsmark = markerat(dsloc.tc);
	    if((dsicon != null) && clickicon(dsicon, dsloc, ev.b, true))
		return(true);
	    if((dsmark != null) && clickmarker(dsmark, dsloc, ev.b, true))
		return(true);
	    if(clickloc(dsloc, ev.b, true))
		return(true);
	} else {
	    dsloc = null;
	    dsicon = null;
	    dsmark = null;
	}
	if(dragp(ev.b)) {
		if (OptWnd.enableQueuedMovementCheckBox.a && ui.modmeta) // ND: Prevent dragging the map by mistake when we're trying to add a checkpoint for queued movement
			return false;
	    Location loc = curloc;
	    if((drag == null) && (loc != null)) {
		drag = ui.grabmouse(this);
		dsc = ev.c;
		dmc = loc.tc;
		dragging = false;
	    }
	    return(true);
	}
	return(super.mousedown(ev));
    }

    public void mousemove(MouseMoveEvent ev) {
	if(drag != null) {
	    if(dragging) {
		setloc = null;
		follow = false;
		curloc = new Location(curloc.seg, dmc.add(dsc.sub(ev.c).mul(scalef())));
	    } else if(ev.c.dist(dsc) > 5) {
		dragging = true;
	    }
	}
	super.mousemove(ev);
    }

    public boolean mouseup(MouseUpEvent ev) {
	if((drag != null) && (ev.b == 1)) {
	    drag.remove();
	    drag = null;
	}
	release: if(!dragging && (dsloc != null)) {
	    if((dsicon != null) && clickicon(dsicon, dsloc, ev.b, false))
		break release;
	    if((dsmark != null) && clickmarker(dsmark, dsloc, ev.b, false))
		break release;
	    if(clickloc(dsloc, ev.b, false))
		break release;
	}
	dsloc = null;
	dsicon = null;
	dsmark = null;
	dragging = false;
	return(super.mouseup(ev));
    }

    public boolean mousewheel(MouseWheelEvent ev) {
	if (allowZooming){
		zoomMomentum += (OptWnd.mapZoomSpeedSlider.val/10f*Math.signum(ev.a));
		allowZooming = false;
	}
	return(true);
    }

    public boolean mousehover(MouseHoverEvent ev, boolean hovering) {
	boolean ret = false;
	if(hovering) {
	    for(ListIterator<DisplayIcon> it = icons.listIterator(icons.size()); it.hasPrevious();) {
		DisplayIcon disp = it.previous();
		if(disp.sc == null)
		    continue;
		Coord ic = ev.c.sub(disp.sc);
		if(disp.icon.hover(ic, hovering && disp.icon.checkhit(ic) && !filter(disp))) {
		    hovering = false;
		    ret = true;
		}
	    }
	    for(DisplayGrid dgrid : display) {
		if(dgrid == null)
		    continue;
		for(DisplayMarker mark : dgrid.markers(false)) {
		    if(mark.sc == null)
			continue;
		    try {
			GobIcon.Icon icon = mark.icon();
			Coord ic = ev.c.sub(mark.sc);
			if(icon.hover(ic, hovering && icon.checkhit(ic) && !filter(mark))) {
			    hovering = false;
			    ret = true;
			}
		    } catch(Loading l) {}
		}
	    }
	}
	return(ret);
    }

    private String lasttname = null;
    private Object lastobjid = null;
    private Tex lasttip = null;
    public Object tooltip(Coord c, Widget prev) {
	DisplayGrid grid = gridat(c);
	String tname = null;
	Object objid = null;
	Supplier<BufferedImage> objtip = null;
	try {
	    if((grid != null) && (grid.dc != null)) {
		DataGrid dgrid = grid.gref.get();
		if(dgrid != null) {
            Coord gc = c.sub(grid.dc).div(1 << dmag);
		    gc = Area.sized(cmaps).closest(gc); /* XXX: This should not be necessary. */
		    TileInfo tile = dgrid.tilesets[dgrid.gettile(gc)];
		    if(tile != null) {
			Resource tres = tile.res.get();
			Resource.Tooltip tt = tres.layer(Resource.tooltip);
			if(tt != null)
			    tname = tt.t;
		    }
		}
	    }
	} catch(Loading l) {
	    tname = "...";
	}
	Location mloc = xlate(c);
	if(mloc != null) {
	    DisplayIcon icon = iconat(c);
	    DisplayMarker mark = markerat(mloc.tc);
	    if(icon != null) {
		if(icon.icon != null) {
		    objid = icon.icon;
		    objtip = () -> Text.render(icon.icon.name()).img;
		}
	    } else if(mark != null) {
		objid = mark;
		objtip = mark::tooltip;
	    }
	}
	if((tname != null) || (objid != null)) {
	    if((tname != lasttname) || (objid != lastobjid)) {
		BufferedImage tip = ItemInfo.catimgs(0,
		    (objid == null) ? null : objtip.get(),
		    (tname == null) ? null : RichText.render("Terrain: $col[255,255,128]{" + RichText.Parser.quote(tname) + "}", 0).img);
		lasttip = new TexI(tip);
		lasttname = tname; lastobjid = objid;
	    }
	} else {
	    lasttip = null;
	}
	if(lasttip != null)
	    return(lasttip);
	return(super.tooltip(c, prev));
    }

    public void mvclick(MapView mv, Coord mc, Location loc, Gob gob, int button) {
	if(mc == null) mc = ui.mc;
	if((sessloc != null) && (sessloc.seg == loc.seg)) {
	    if(gob == null) {
			if(ui.modmeta && button == 3){
				Gob player = ui.gui.map.player();
				if (player != null && player.rc != null) {
					Map<String, ChatUI.MultiChat> chats = ui.gui.chat.getMultiChannels();
					Coord2d clickloc = loc.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2));
					ChatUI.MultiChat chat = chats.get("Party");
					if (chat != null) {
						chat.send("LOC@" + (int)(clickloc.x-player.rc.x) + "x" + (int)(clickloc.y-player.rc.y));
					}
				}
			}
			if (OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.a) {
				ui.gui.map.switchToPlateBoots();
			}
			if(mv.checkpointManager != null && mv.checkpointManagerThread != null && button == 1){
				if (!ui.modmeta)
					mv.checkpointManager.pauseIt();
			}
            synchronized (Pathfinder.class) {
                if (ui.gui.map.pf != null && button == 1) {
                    ui.gui.map.pf.terminate = true;
                    ui.gui.map.pfthread.interrupt();
                }
            }
		mv.wdgmsg("click", mc, loc.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)).floor(posres),	button, ui.modflags());
		} else {
            if(mv.checkpointManager != null && mv.checkpointManagerThread != null && button == 3){
                mv.checkpointManager.pauseIt();
            }
            synchronized (Pathfinder.class) {
                if (ui.gui.map.pf != null && button == 3) {
                    ui.gui.map.pf.terminate = true;
                    ui.gui.map.pfthread.interrupt();
                }
            }
			if (OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.a) {
				if (button == 3)
					ui.gui.map.switchBunnySlippersAndPlateBoots(gob);
				if (button == 1)
					ui.gui.map.switchToPlateBoots();
			}
		Object[] args = {mc, loc.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)).floor(posres), button, ui.modflags(), 0, (int) gob.id, gob.rc.floor(posres), 0, -1};
			if (button == 3 && OptWnd.autoSelect1stFlowerMenuCheckBox.a) {
				mv.wdgmsg("click", args);
				if (ui.modctrl) {
					ui.rcvr.rcvmsg(ui.lastWidgetID + 1, "cl", 0, ui.modflags());
				}
				return;
			}
		mv.wdgmsg("click", args);
		}
        if (OptWnd.walkWithPathFinderCheckBox.a && button == 1 && ui.modctrl && ui.modshift && !ui.modmeta && !ui.modsuper) {
            mv.pfLeftClick(dsloc.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)).floor(), null);
        }
	}
    }

	void drawbiome(GOut g) {
		if(biometex != null) {
			Coord mid = new Coord(g.sz().x / 2, 0);
			Coord tsz = biometex.sz();
			g.chcolor(BIOME_BG);
			g.frect(mid.sub(2 + tsz.x /2, 0), tsz.add(4, 2));
			g.chcolor();
			g.aimage(biometex, mid, 0.5f, 0);
		}
	}

	private void setBiome(Location loc) {
		try {
			Resource res = null;
			String newbiome = biome;
			if(loc == null) {
				Gob player = ui.gui.map.player();
				MCache mCache = ui.sess.glob.map;
				if (player != null) { // ND: Do this to avoid Nullpointer crash when switching maps? (Like going from character creation zone to valhalla or the real world)
					int tile = mCache.gettile(player.rc.div(tilesz).floor());
					res = mCache.tilesetr(tile);
				}
				if(res != null) {
					newbiome = res.name;
				}
			} else {
				MapFile map = loc.seg.file();
				if(map.lock.readLock().tryLock()) {
					try {
						MapFile.Grid grid = loc.seg.grid(loc.tc.div(cmaps)).get();
						if(grid != null) {
							int tile = grid.gettile(loc.tc.mod(cmaps));
							newbiome = grid.tilesets[tile].res.name;
						}
					} finally {
						map.lock.readLock().unlock();
					}
				}
			}
			if(newbiome != null && !newbiome.equals(biome)) {
				biome = newbiome;
				String key = prettybiome(biome).toLowerCase();
				String biomeText;
				if (Config.ORE_FULL_NAMES.containsKey(key)) {
					biomeText = Config.ORE_FULL_NAMES.get(key);
				} else if (Config.STONE_FULL_NAMES.containsKey(key)) {
					biomeText = Config.STONE_FULL_NAMES.get(key);
				} else {
					biomeText = prettybiome(biome);
				}
				biometex = Text.renderstroked(biomeText).tex();
			}
		} catch (Loading ignored) {
		}
	}

	private static final Map<String, String> improvedTileNames = new HashMap<String, String>(){{
		put("Water", "Shallow Water");
		put("Deep", "Deep Water");
		put("Owater", "Shallow Ocean");
		put("Odeep", "Deep Ocean");
		put("Odeeper", "Very Deep Ocean");
	}};
	private static String prettybiome(String biome) {
		int k = biome.lastIndexOf("/");
		biome = biome.substring(k + 1);
		biome = biome.substring(0, 1).toUpperCase() + biome.substring(1);
		if(improvedTileNames.containsKey(biome)) {
			return improvedTileNames.get(biome);
		}
		return biome;
	}

	public static final Coord sgridsz = new Coord(100, 100);
	public static final Coord VIEW_SZ = UI.scale(sgridsz.mul(9).div(tilesz.floor()));// view radius is 9x9 "server" grids
	public static final Color VIEW_BG_COLOR = new Color(255, 255, 255, 60);
	public static final Color VIEW_BORDER_COLOR = new Color(0, 0, 0, 128);
	void drawview(GOut g) {
		Coord2d sgridsz = new Coord2d(MiniMap.sgridsz);
		Gob player = ui.gui.map.player();
		if(player != null) {
			Coord rc = p2c(player.rc.floor(sgridsz).sub(4, 4).mul(sgridsz));
			Coord viewsz = VIEW_SZ.div(zoomlevel);
			g.chcolor(VIEW_BG_COLOR);
			g.frect(rc, viewsz);
			if (zoomlevel >= 0.4 && follow) {
				g.chcolor(VIEW_BORDER_COLOR);
				g.rect(rc, viewsz);
			}
			g.chcolor();
		}
	}

	private static final Color gridColor = new Color(180, 0, 0, 220);
	void drawgridlines(GOut g) {
		Coord2d zmaps = new Coord2d(cmaps).div(scalef());
		Coord2d offset = new Coord2d(sz.div(2)).sub(new Coord2d(dloc.tc).div(scalef())).mod(zmaps);
		double width = UI.scale(1f/zoomlevel);
		Color col = g.getcolor();
		Coord gridlines = sz.div(zmaps);
		Coord2d ulgrid = dgext.ul.mul(zmaps).mod(zmaps);
		g.chcolor(gridColor);
		for (int x = -1; x < gridlines.x+1; x++) {
			Coord up = new Coord2d((zmaps.x*x+ulgrid.x+offset.x), 0).floor();
			Coord dn = new Coord2d((zmaps.x*x+ulgrid.x+offset.x), sz.y).floor();
			if(up.x >= 0 && up.x <= sz.x) {
				g.line(up, dn, width);
			}
		}
		for (int y = -1; y < gridlines.y+1; y++) {
			Coord le = new Coord2d(0, (zmaps.y*y+ulgrid.y+offset.y)).floor();
			Coord ri = new Coord2d(sz.x, (zmaps.y*y+ulgrid.y+offset.y)).floor();
			if(le.y >= 0 && le.y <= sz.y) {
				g.line(le, ri, width);
			}
		}
		g.chcolor(col);
	}

	private void drawsprites(GOut g) {

		synchronized (mapSprites) {
			for (MapSprite mapSprite : mapSprites) {
				mapSprite.draw(g, p2c(mapSprite.rc), zoomlevel);
			}
		}
	}

	private void ticksprites(double dt) {
		synchronized (mapSprites) {
			ListIterator<MapSprite> iter = mapSprites.listIterator();
			while (iter.hasNext()) {
				MapSprite mapSprite = iter.next();
				boolean done = mapSprite.tick(dt);
				if (done) {
					iter.remove();
				}
			}
		}
	}

	public void addSprite(MapSprite mapSprite) {
		synchronized (mapSprites) {
			mapSprites.add(mapSprite);
		}
	}

	private void drawmovequeue(GOut g) {
		MapView mv = ui.gui.map;
		if (mv == null){
			return;
		}
		if (mv.checkpointManager != null && mv.checkpointManagerThread != null) {
			if(mv.checkpointManager.checkpointList.listitems() > 0){
				List<Coord2d> coords = mv.getCheckPointList();
				Gob player = mv.player();
				if (player == null) return;
				final Coord2d movingto = coords.get(0);
				final Iterator<Coord2d> queue = coords.iterator();
				Coord last;
				if (movingto != null && player.rc != null) {
					//Make the line first
					g.chcolor(Color.WHITE);
					Coord cloc = p2c(player.rc);
					last = p2c(mv.getCheckPointList().get(0));
					if (last != null && cloc != null) {
						g.dottedline(cloc, last, 2);
						if (queue.hasNext()) {
							while (queue.hasNext()) {
								final Coord next = p2c(queue.next());
								if (next != null) {
									g.dottedline(last, next, 2);
									last = next;
								} else {
									break;
								}
							}
						}
					}
				} else if (mv.player().rc != null && player.rc != null) {
					Coord cloc = p2c(player.rc);
					last = p2c(mv.player().rc);
					if (last != null && cloc != null) {
						g.dottedline(cloc, last, 1);
					}
				}
			}
		}
	}

	public void refreshMapCache() {
		if (display != null) {
			for (DisplayGrid dg : display) {
				if (dg != null) {
					dg.clearCache();
				}
			}
		}
	}
}
