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

import haven.render.*;
import java.util.*;
import java.util.function.*;
import java.awt.Color;
import haven.MapFile.Segment;
import haven.MapFile.DataGrid;
import haven.MapFile.GridInfo;
import haven.MapFile.Marker;
import haven.MapFile.PMarker;
import haven.MapFile.SMarker;
import haven.sprites.MapSprite;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import static haven.OCache.posres;

public class MiniMap extends Widget {
    public static final Tex bg = Resource.loadtex("gfx/hud/mmap/ptex");
    public static final Tex nomap = Resource.loadtex("gfx/hud/mmap/nomap");
    public static final Tex plp = ((TexI)Resource.loadtex("gfx/hud/mmap/plp")).filter(Texture.Filter.LINEAR);
    public final MapFile file;
    public Location curloc;
    public Location sessloc;
    public GobIcon.Settings iconconf;
    public List<DisplayIcon> icons = Collections.emptyList();
    protected Locator setloc;
    protected boolean follow;
    protected float zoomlevel = 1;
	public float smallMapZoomLevel = 1;
	public float bigMapZoomLevel = 1;
	public float zoomMomentum = 0;
	private boolean allowZooming = false;
    protected DisplayGrid[] display;
    protected Area dgext, dtext;
    protected Segment dseg;
    protected int dlvl;
    protected Location dloc;
	public boolean compact;
	private static final Color BIOME_BG = new Color(0, 0, 0, 110);
	private String biome;
	private Tex biometex;
	public static boolean showMapViewRange = Utils.getprefb("showMapViewRange", true);
	public static boolean showMapGridLines = Utils.getprefb("showMapGridLines", false);
	public static boolean highlightMapTiles = Utils.getprefb("highlightMapTiles", false);
	private final List<MapSprite> mapSprites = new LinkedList<>();

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

    public static class Location {
	public Segment seg;
	public final Coord tc;

	public Location(Segment seg, Coord tc) {
	    Objects.requireNonNull(seg);
	    Objects.requireNonNull(tc);
	    this.seg = seg; this.tc = tc;
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
		    if(map.grids.get(lastgrid.gc) == lastgrid)
			return(lastloc);
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
	public double stime;
	public boolean notify;
	private Consumer<UI> snotify;
	private boolean markchecked;

	public DisplayIcon(GobIcon attr, GobIcon.Setting conf) {
	    this.attr = attr;
	    this.gob = attr.gob;
	    this.icon = attr.icon();
	    this.z = icon.z();
	    this.stime = Utils.rtime();
	    this.conf = conf;
	    if(this.notify = conf.notify)
		this.snotify = conf.notification();
	}

	public void update(Coord2d rc, double ang) {
	    this.rc = rc;
	    this.ang = ang;
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
		double t = (Utils.rtime() - stime) * 1.0;
		if(t > 1) {
		    notify = false;
		} else {
		    double f = 1.0 + (Math.pow(Math.sin(t * Math.PI * 1.5), 2) * 1.0);
		    double a = (t < 0.5) ? 0.5 : (0.5 - (t - 0.5));
		    g.usestate(new ColorMask(notifcol));
		    g.usestate(new Scale2D(sc.add(g.tx), (float)f));
		    g.chcolor(255, 255, 255, (int)Math.round(255 * a));
		    icon.draw(g, sc);
		    g.defstate();
		}
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

    public static class DisplayMarker {
	public static final Resource.Image flagbg, flagfg;
	public static final Coord flagcc;
	public final Marker m;
	public final Text tip;
	public Area hit;
	private Resource.Image img;
	private Coord imgsz;
	private Coord cc;
	public static HashMap<String, Tex> titleTexMap = new HashMap<String, Tex>();

	static {
	    Resource flag = Resource.local().loadwait("gfx/hud/mmap/flag");
	    flagbg = flag.layer(Resource.imgc, 1);
	    flagfg = flag.layer(Resource.imgc, 0);
	    flagcc = UI.scale(flag.layer(Resource.negc).cc);
	}

	public DisplayMarker(Marker marker) {
	    this.m = marker;
	    this.tip = Text.render(m.nm);
		if (!titleTexMap.containsKey(tip.text))
			titleTexMap.put(tip.text, Text.renderstroked(tip.text, Color.white, Color.BLACK, Text.num12boldFnd).tex());
	    if(marker instanceof PMarker)
		this.hit = Area.sized(flagcc.inv(), UI.scale(flagbg.sz));
	}

	public void draw(GOut g, Coord c) {
	    if(m instanceof PMarker) {
		Coord ul = c.sub(flagcc);
		g.chcolor(((PMarker)m).color);
		g.image(flagfg, ul);
		g.chcolor();
		g.image(flagbg, ul);
	    } else if(m instanceof SMarker) {
		SMarker sm = (SMarker)m;
		try {
		    if(cc == null) {
			Resource res = sm.res.get();
			img = res.flayer(Resource.imgc);
			Resource.Neg neg = res.layer(Resource.negc);
			cc = (neg != null) ? neg.cc : img.ssz.div(2);
			if(hit == null)
			    hit = Area.sized(cc.inv(), img.ssz);
		    }
		} catch(Loading l) {
		} catch(Exception e) {
		    cc = Coord.z;
		}
		if(img != null)
		    g.image(img, c.sub(cc));
	    }
	}
    }

    public static class DisplayGrid {
	public final MapFile file;
	public final Segment seg;
	public final Coord sc;
	public final Area mapext;
	public final Indir<? extends DataGrid> gref;
	private DataGrid cgrid = null;
	private Tex img = null;
	private Defer.Future<Tex> nextimg = null;

	public DisplayGrid(Segment seg, Coord sc, int lvl, Indir<? extends DataGrid> gref) {
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

	private Collection<DisplayMarker> markers = Collections.emptyList();
	private int markerseq = -1;
	public Collection<DisplayMarker> markers(boolean remark) {
	    if(remark && (markerseq != file.markerseq)) {
		if(file.lock.readLock().tryLock()) {
		    try {
			ArrayList<DisplayMarker> marks = new ArrayList<>();
			for(Marker mark : file.markers) {
			    if((mark.seg == this.seg.id) && mapext.contains(mark.tc))
				marks.add(new DisplayMarker(mark));
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
			display[dgext.ri(c)] = new DisplayGrid(dloc.seg, c, lvl, dloc.seg.grid(lvl, c.mul(dlvl)));
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
		mark.draw(g, mark.m.tc.sub(dloc.tc).div(scalef()).add(hsz));
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
			    disp.update(gob.rc, gob.a);
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
		g.chcolor(m.col.getRed(), m.col.getGreen(), m.col.getBlue(), 255);
		g.rotimage(plp, p2c(ppc), plp.sz().div(2), -m.geta() - (Math.PI / 2));
		g.chcolor();
	    } catch(Loading l) {}
	}
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
	if (dloc.seg != sessloc.seg){ // ND: Attempts to fix the bug where the segments desync, idk why they desync and it's annoying me alot
		sessloc.seg = dloc.seg;
	}
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
		if((mark.hit != null) && mark.hit.contains(tc.sub(mark.m.tc).div(scalef())) && !filter(mark))
		    return(mark);
	    }
	}
	return(null);
    }

    public void markobjs() {
	for(DisplayIcon icon : icons) {
	    try {
		if(icon.markchecked)
		    continue;
		GobIcon.Icon micon = icon.icon;
		if(!icon.conf.getmarkablep() || !(micon instanceof GobIcon.ImageIcon)) {
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
		    SMarker prev = file.smarker(micon.res.name, info.seg, sc);
		    if(prev == null) {
			if(icon.conf.getmarkp()) {
			    Resource.Tooltip tt = micon.res.flayer(Resource.tooltip);
			    mid = new SMarker(info.seg, sc, tt.t, 0, new Resource.Saved(Resource.remote(), micon.res.name, micon.res.ver));
			    file.add(mid);
			} else {
			    mid = null;
			}
		    } else {
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
    public boolean mousedown(Coord c, int button) {
	dsloc = xlate(c);
	if(dsloc != null) {
	    dsicon = iconat(c);
	    dsmark = markerat(dsloc.tc);
	    if((dsicon != null) && clickicon(dsicon, dsloc, button, true))
		return(true);
	    if((dsmark != null) && clickmarker(dsmark, dsloc, button, true))
		return(true);
	    if(clickloc(dsloc, button, true))
		return(true);
	} else {
	    dsloc = null;
	    dsicon = null;
	    dsmark = null;
	}
	if(dragp(button)) {
	    Location loc = curloc;
	    if((drag == null) && (loc != null)) {
		drag = ui.grabmouse(this);
		dsc = c;
		dmc = loc.tc;
		dragging = false;
	    }
	    return(true);
	}
	return(super.mousedown(c, button));
    }

    public void mousemove(Coord c) {
	if(drag != null) {
	    if(dragging) {
		setloc = null;
		follow = false;
		curloc = new Location(curloc.seg, dmc.add(dsc.sub(c).mul(scalef())));
	    } else if(c.dist(dsc) > 5) {
		dragging = true;
	    }
	}
	super.mousemove(c);
    }

    public boolean mouseup(Coord c, int button) {
	if((drag != null) && (button == 1)) {
	    drag.remove();
	    drag = null;
	}
	release: if(!dragging && (dsloc != null)) {
	    if((dsicon != null) && clickicon(dsicon, dsloc, button, false))
		break release;
	    if((dsmark != null) && clickmarker(dsmark, dsloc, button, false))
		break release;
	    if(clickloc(dsloc, button, false))
		break release;
	}
	dsloc = null;
	dsicon = null;
	dsmark = null;
	dragging = false;
	return(super.mouseup(c, button));
    }

    public boolean mousewheel(Coord c, int amount) {
	if (allowZooming){
		zoomMomentum += (float) (1.5*Math.signum(amount));
		allowZooming = false;
	}
	return(true);
    }

    public Object tooltip(Coord c, Widget prev) {
	if(dloc != null) {
	    Coord tc = c.sub(sz.div(2)).mul(scalef()).add(dloc.tc);
	    DisplayMarker mark = markerat(tc);
	    if(mark != null) {
		return(mark.tip);
	    }
	}
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
			} else if (ui.modmeta && button == 1){
				mv.addCheckpoint(loc.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)));
			}
			if (OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.a) {
				ui.gui.map.switchToPlateBoots();
			}
			if(mv.checkpointManager != null && mv.checkpointManagerThread != null && button == 1){
				if (!ui.modmeta)
					mv.checkpointManager.pauseIt();
			}
		mv.wdgmsg("click", mc, loc.tc.sub(sessloc.tc).mul(tilesz).add(tilesz.div(2)).floor(posres),	button, ui.modflags());
		} else {
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
				biometex = Text.renderstroked(prettybiome(biome)).tex();
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

	private static final Color gridColor = new Color(180, 0, 0);
	void drawgridlines(GOut g) {
		Coord2d zmaps = new Coord2d(cmaps).div(scalef());
		Coord2d offset = new Coord2d(sz.div(2)).sub(new Coord2d(dloc.tc).div(scalef())).mod(zmaps);
		double width = UI.scale(2f/zoomlevel);
		double width2 = UI.scale((2f/zoomlevel) + 2f);
		Color col = g.getcolor();
		Coord gridlines = sz.div(zmaps);
		Coord2d ulgrid = dgext.ul.mul(zmaps).mod(zmaps);
		g.chcolor(Color.BLACK);
		for (int x = -1; x < gridlines.x+1; x++) {
			Coord up = new Coord2d((zmaps.x*x+ulgrid.x+offset.x), 0).floor();
			Coord dn = new Coord2d((zmaps.x*x+ulgrid.x+offset.x), sz.y).floor();
			if(up.x >= 0 && up.x <= sz.x) {
				g.line(up, dn, width2);
			}
		}
		for (int y = -1; y < gridlines.y+1; y++) {
			Coord le = new Coord2d(0, (zmaps.y*y+ulgrid.y+offset.y)).floor();
			Coord ri = new Coord2d(sz.x, (zmaps.y*y+ulgrid.y+offset.y)).floor();
			if(le.y >= 0 && le.y <= sz.y) {
				g.line(le, ri, width2);
			}
		}
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

}
