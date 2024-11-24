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

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Future;
import java.util.function.*;
import java.util.stream.Collectors;

import haven.Fuzzy;
import haven.automated.mapper.MappingClient;
import haven.render.*;
import haven.render.gl.GLObject;
import haven.res.lib.svaj.GobSvaj;
import haven.res.lib.tree.TreeScale;
import haven.res.ui.obj.buddy.Buddy;
import haven.res.ui.obj.buddy_v.Vilmate;
import haven.sprites.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Gob implements RenderTree.Node, Sprite.Owner, Skeleton.ModOwner, EquipTarget, RandomSource {
    public Coord2d rc;
    public double a;
    public boolean virtual = false;
    int clprio = 0;
    public long id;
    public boolean removed = false;
    public final Glob glob;
	public ConcurrentHashMap<Class<? extends GAttrib>, GAttrib> attr = new ConcurrentHashMap<>(); // ND: Make this ConcurrentHashMap to prevent concurrent modification exceptions. It doesn't seem to affect performance
    public final Collection<Overlay> ols = new ArrayList<Overlay>();
    public final Collection<RenderTree.Slot> slots = new CopyOnWriteArrayList<>(); // ND: Make this COW to prevent concurrent modification exceptions. It doesn't seem to affect performance
    public int updateseq = 0;
    private final Collection<SetupMod> setupmods = new ArrayList<>();
    private final LinkedList<Runnable> deferred = new LinkedList<>();
    private Loader.Future<?> deferral = null;
	public Boolean isComposite = false;
	private final Set<String> animatedGobsToDisable = new HashSet<>(Arrays.asList("dreca", "pow", "kiln", "cauldron", "beehive", "stockpile-trash"));
	private boolean thisGobAnimationsCanBeDisabled = false;
	public Boolean isMe = null;
	public Boolean isMannequin = null;
	public Boolean isSkeleton = null;
	private boolean isLoftar = false;
	public boolean playerNameChecked = false;
	public final ArrayList<Gob> occupants = new ArrayList<Gob>(); // ND: The "passengers" of this gob
	public Long occupiedGobID = null; // ND: The id of the "vehicle" this gob is currently in
	private HitBoxGobSprite<HidingBox> hidingBoxHollow = null;
	private HitBoxGobSprite<HidingBox> hidingBoxFilled = null;
	public HitBoxGobSprite<CollisionBox> collisionBox = null;
	private final GobQualityInfo qualityInfo;
	GobGrowthInfo growthInfo;
	public boolean isHidden;
	private final GobCustomSizeAndRotation customSizeAndRotation = new GobCustomSizeAndRotation();
	public double gobSpeed = 0;
	private Overlay customSearchOverlay;
	public Boolean knocked = null;  // knocked will be null if pose update request hasn't been received yet
	private Overlay customAuraOverlay;
	private Overlay customRadiusOverlay;
	public static Boolean batWingCapeEquipped = false; // ND: Check for Bat Wing Cape
	public static Boolean nightQueenDefeated = false; // ND: Check for Bat Dungeon Experience (Defeated Bat Queen)
	public String playerGender = "unknown";
	public Boolean isDeadPlayer = false;
	public int playerPoseUpdatedCounter = 0;
	private long lastKnockedOutSoundtime = 0;
	public static boolean somethingJustDied = false;
	public static final ScheduledExecutorService gobDeathExecutor = Executors.newSingleThreadScheduledExecutor();
	private static Future<?> gobDeathFuture;
	private Overlay gobChaseVector = null;
	public static final HashSet<Long> alarmPlayed = new HashSet<Long>();
	private Overlay miningSafeTilesOverlay = null;
	public Overlay combatFoeCircleOverlay = null;
	public static Set<Long> permanentHighlightList = new HashSet<>();
	private GobDamageInfo damage;
	public Boolean imDrinking = false;
	public Boolean imInCoracle = false;
	public Boolean imOnSkis = false;
	List<String> onWaterAnimations = List.of("coracleidle", "coraclerowan", "dugout", "rowboat", "rowing", "snekkja", "knarr");
	private Overlay archeryVector;
	private Overlay archeryRadius;
	BarrelContentsGobInfo barrelContentsGobInfo;

    public static class Overlay implements RenderTree.Node {
	public final int id;
	public final Gob gob;
	public final Sprite.Mill<?> sm;
	public Sprite spr;
	public boolean delign = false, old = false;
	private Collection<RenderTree.Slot> slots = null;
	private boolean added = false;

	public Overlay(Gob gob, int id, Sprite.Mill<?> sm) {
	    this.gob = gob;
	    this.id = id;
	    this.sm = sm;
	    this.spr = null;
	}

	public Overlay(Gob gob, Sprite.Mill<?> sm) {
	    this(gob, -1, sm);
	}

	public Overlay(Gob gob, int id, Indir<Resource> res, Message sdt) {
	    this(gob, id, owner -> Sprite.create(owner, res.get(), sdt));
	}

	public Overlay(Gob gob, Sprite spr) {
	    this.gob = gob;
	    this.id = -1;
	    this.sm = null;
	    this.spr = spr;
	}

	private void init() {
	    if(spr == null) {
		spr = sm.create(gob);
		if(old)
		    spr.age();
		if(added && (spr instanceof SetupMod))
		    gob.setupmods.add((SetupMod)spr);
	    }
	    if(slots == null)
		RUtils.multiadd(gob.slots, this);
	}

	private void add0() {
	    if(added)
		throw(new IllegalStateException());
	    if(spr instanceof SetupMod)
		gob.setupmods.add((SetupMod)spr);
	    added = true;
	}

	private void remove0() {
	    if(!added)
		throw(new IllegalStateException());
	    if(slots != null) {
		RUtils.multirem(new ArrayList<>(slots));
		slots = null;
	    }
	    if(spr instanceof SetupMod)
		gob.setupmods.remove(spr);
	    added = false;
	}

	public void remove(boolean async) {
	    if(async) {
		gob.defer(() -> remove(false));
		return;
	    }
	    remove0();
	    gob.ols.remove(this);
	    removed();
	}

	public void remove() {
	    remove(true);
	}

	protected void removed() {
	}

	public boolean tick(double dt) {
	    if(spr == null)
		return(false);
	    return(spr.tick(dt));
	}

	public void added(RenderTree.Slot slot) {
	    slot.add(spr);
	    if(slots == null)
		slots = new ArrayList<>(1);
	    slots.add(slot);
		if (this.spr != null && this.spr.res != null && this.spr.res.name.contains("decal")){
			if (OptWnd.flatCupboardsCheckBox.a && this.spr.owner.getres().name.equals("gfx/terobjs/cupboard"))
				slot.cstate(Pipe.Op.compose(Location.scale(1, 1, 1.6f), Location.xlate(new Coord3f(0, 0, -5.4f))));
			slot.ostate(new MixColor(new Color(255, 255, 255, 0)));
		}
	}

	public void removed(RenderTree.Slot slot) {
	    if(slots != null)
		slots.remove(slot);
	}

	public String getSprResName() {
		Sprite spr = this.spr;
		if(spr != null) {
			Resource res = spr.res;
			if(res != null) {
				return res.name;
			}
		}
		return "";
	}
    }

    public static interface SetupMod {
	public default Pipe.Op gobstate() {return(null);}
	public default Pipe.Op placestate() {return(null);}
    }

    public static interface Placer {
	/* XXX: *Quite* arguably, the distinction between getc and
	 * getr should be abolished and a single transform matrix
	 * should be used instead, but that requires first abolishing
	 * the distinction between the gob/gobx location IDs. */
	public Coord3f getc(Coord2d rc, double ra);
	public Matrix4f getr(Coord2d rc, double ra);
    }

    public static interface Placing {
	public Placer placer();
    }

    public static class DefaultPlace implements Placer {
	public final MCache map;
	public final MCache.SurfaceID surf;

	public DefaultPlace(MCache map, MCache.SurfaceID surf) {
	    this.map = map;
	    this.surf = surf;
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    return(map.getzp(surf, rc));
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    return(Transform.makerot(new Matrix4f(), Coord3f.zu, -(float)ra));
	}
    }

    public static class InclinePlace extends DefaultPlace {
	public InclinePlace(MCache map, MCache.SurfaceID surf) {
	    super(map, surf);
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    Matrix4f ret = super.getr(rc, ra);
	    Coord3f norm = map.getnorm(surf, rc);
	    norm.y = -norm.y;
	    Coord3f rot = Coord3f.zu.cmul(norm);
	    float sin = rot.abs();
	    if(sin > 0) {
		Matrix4f incl = Transform.makerot(new Matrix4f(), rot.mul(1 / sin), sin, (float)Math.sqrt(1 - (sin * sin)));
		ret = incl.mul(ret);
	    }
	    return(ret);
	}
    }

    public static class BasePlace extends DefaultPlace {
	public final Coord2d[][] obst;
	private Coord2d cc;
	private double ca;
	private int seq = -1;
	private float z;

	public BasePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] obst) {
	    super(map, surf);
	    this.obst = obst;
	}

	public BasePlace(MCache map, MCache.SurfaceID surf, Resource res, String id) {
	    this(map, surf, res.flayer(Resource.obst, id).p);
	}

	public BasePlace(MCache map, MCache.SurfaceID surf, Resource res) {
	    this(map, surf, res, "");
	}

	private float getz(Coord2d rc, double ra) {
	    Coord2d[][] no = this.obst, ro = new Coord2d[no.length][];
	    {
		double s = Math.sin(ra), c = Math.cos(ra);
		for(int i = 0; i < no.length; i++) {
		    ro[i] = new Coord2d[no[i].length];
		    for(int o = 0; o < ro[i].length; o++)
			ro[i][o] = Coord2d.of((no[i][o].x * c) - (no[i][o].y * s), (no[i][o].y * c) + (no[i][o].x * s)).add(rc);
		}
	    }
	    float ret = Float.NaN;
	    for(int i = 0; i < no.length; i++) {
		for(int o = 0; o < ro[i].length; o++) {
		    Coord2d a = ro[i][o], b = ro[i][(o + 1) % ro[i].length];
		    for(Coord2d c : new Line2d.GridIsect(a, b, MCache.tilesz, false)) {
			double z = map.getz(surf, c);
			if(Float.isNaN(ret) || (z < ret))
			    ret = (float)z;
		    }
		}
	    }
	    return(ret);
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    int mseq = map.chseq;
	    if((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
		this.z = getz(rc, ra);
		this.seq = mseq;
		this.cc = rc;
		this.ca = ra;
	    }
	    return(Coord3f.of((float)rc.x, (float)rc.y, this.z));
	}
    }

    public static class LinePlace extends DefaultPlace {
	public final double max, min;
	public final Coord2d k;
	private Coord3f c;
	private Matrix4f r = Matrix4f.id;
	private int seq = -1;
	private Coord2d cc;
	private double ca;

	public LinePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] points, Coord2d k) {
	    super(map, surf);
	    Line2d l = Line2d.from(Coord2d.z, k);
	    double max = 0, min = 0;
	    for(int i = 0; i < points.length; i++) {
		for(int o = 0; o < points[i].length; o++) {
		    int p = (o + 1) % points[i].length;
		    Line2d edge = Line2d.twixt(points[i][o], points[i][p]);
		    Coord2d t = l.cross(edge);
		    if((t.y >= 0) && (t.y <= 1)) {
			max = Math.max(t.x, max);
			min = Math.min(t.x, min);
		    }
		}
	    }
	    if((max == 0) || (min == 0))
		throw(new RuntimeException("illegal bounds for LinePlace"));
	    this.k = k;
	    this.max = max;
	    this.min = min;
	}

	public LinePlace(MCache map, MCache.SurfaceID surf, Resource res, String id, Coord2d k) {
	    this(map, surf, res.flayer(Resource.obst, id).p, k);
	}

	public LinePlace(MCache map, MCache.SurfaceID surf, Resource res, Coord2d k) {
	    this(map, surf, res, "", k);
	}

	private void recalc(Coord2d rc, double ra) {
	    Coord2d rk = k.rot(ra);
	    double maxz = map.getz(surf, rc.add(rk.mul(max)));
	    double minz = map.getz(surf, rc.add(rk.mul(min)));
	    Coord3f rax = Coord3f.of((float)-rk.y, (float)-rk.x, 0);
	    float dz = (float)(maxz - minz);
	    float dx = (float)(max - min);
	    float hyp = (float)Math.sqrt((dx * dx) + (dz * dz));
	    float sin = dz / hyp, cos = dx / hyp;
	    c = Coord3f.of((float)rc.x, (float)rc.y, (float)minz + (dz * (float)(-min / (max - min))));
	    r = Transform.makerot(new Matrix4f(), rax, sin, cos).mul(super.getr(rc, ra));
	}

	private void check(Coord2d rc, double ra) {
	    int mseq = map.chseq;
	    if((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
		recalc(rc, ra);
		this.seq = mseq;
		this.cc = rc;
		this.ca = ra;
	    }
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    check(rc, ra);
	    return(c);
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    check(rc, ra);
	    return(r);
	}
    }

    public static class PlanePlace extends DefaultPlace {
	public final Coord2d[] points;
	private Coord3f c;
	private Matrix4f r = Matrix4f.id;
	private int seq = -1;
	private Coord2d cc;
	private double ca;

	public static Coord2d[] flatten(Coord2d[][] points) {
	    int n = 0;
	    for(int i = 0; i < points.length; i++)
		n += points[i].length;
	    Coord2d[] ret = new Coord2d[n];
	    for(int i = 0, o = 0; i < points.length; o += points[i++].length)
		System.arraycopy(points[i], 0, ret, o, points[i].length);
	    return(ret);
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Coord2d[] points) {
	    super(map, surf);
	    this.points = points;
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Coord2d[][] points) {
	    this(map, surf, flatten(points));
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Resource res, String id) {
	    this(map, surf, res.flayer(Resource.obst, id).p);
	}

	public PlanePlace(MCache map, MCache.SurfaceID surf, Resource res) {
	    this(map, surf, res, "");
	}

	private void recalc(Coord2d rc, double ra) {
	    double s = Math.sin(ra), c = Math.cos(ra);
	    Coord3f[] pp = new Coord3f[points.length];
	    for(int i = 0; i < pp.length; i++) {
		Coord2d rv = Coord2d.of((points[i].x * c) - (points[i].y * s), (points[i].y * c) + (points[i].x * s));
		pp[i] = map.getzp(surf, rv.add(rc));
	    }
	    int I = 0, O = 1, U = 2;
	    Coord3f mn = Coord3f.zu;
	    double ma = 0;
	    for(int i = 0; i < pp.length - 2; i++) {
		for(int o = i + 1; o < pp.length - 1; o++) {
		    plane: for(int u = o + 1; u < pp.length; u++) {
			Coord3f n = pp[o].sub(pp[i]).cmul(pp[u].sub(pp[i])).norm();
			for(int p = 0; p < pp.length; p++) {
			    if((p == i) || (p == o) || (p == u))
				continue;
			    float pz = (((n.x * (pp[i].x - pp[p].x)) + (n.y * (pp[i].y - pp[p].y))) / n.z) + pp[i].z;
			    if(pz < pp[p].z - 0.01)
				continue plane;
			}
			double a = n.cmul(Coord3f.zu).abs();
			if(a > ma) {
			    mn = n;
			    ma = a;
			    I = i; O = o; U = u;
			}
		    }
		}
	    }
	    this.c = Coord3f.of((float)rc.x, (float)rc.y, (((mn.x * (pp[I].x - (float)rc.x)) + (mn.y * (pp[I].y - (float)rc.y))) / mn.z) + pp[I].z);
	    this.r = Transform.makerot(new Matrix4f(), Coord3f.zu, -(float)ra);
	    mn.y = -mn.y;
	    Coord3f rot = Coord3f.zu.cmul(mn);
	    float sin = rot.abs();
	    if(sin > 0) {
		Matrix4f incl = Transform.makerot(new Matrix4f(), rot.mul(1 / sin), sin, (float)Math.sqrt(1 - (sin * sin)));
		this.r = incl.mul(this.r);
	    }
	}

	private void check(Coord2d rc, double ra) {
	    int mseq = map.chseq;
	    if((mseq != this.seq) || !Utils.eq(rc, cc) || (ra != ca)) {
		recalc(rc, ra);
		this.seq = mseq;
		this.cc = rc;
		this.ca = ra;
	    }
	}

	public Coord3f getc(Coord2d rc, double ra) {
	    check(rc, ra);
	    return(this.c);
	}

	public Matrix4f getr(Coord2d rc, double ra) {
	    return(this.r);
	}
    }

    public Gob(Glob glob, Coord2d c, long id) {
	this.glob = glob;
	this.rc = c;
	this.id = id;
	if(id < 0)
	    virtual = true;
	if(GobDamageInfo.has(this)) {
		addDmg();
	}
	setupmods.add(customSizeAndRotation);
	qualityInfo = new GobQualityInfo(this);
	setattr(GobQualityInfo.class, qualityInfo);
	growthInfo = new GobGrowthInfo(this);
	setattr(GobGrowthInfo.class, growthInfo);
	barrelContentsGobInfo = new BarrelContentsGobInfo(this);
	setattr(BarrelContentsGobInfo.class, barrelContentsGobInfo);
	updwait(this::updateDrawableStuff, waiting -> {});
    }

    public Gob(Glob glob, Coord2d c) {
	this(glob, c, -1);
    }

    public void ctick(double dt) {
	Map<Class<? extends GAttrib>, GAttrib> attr = cloneattrs();
	for(GAttrib a : attr.values()){
		if(a instanceof ResDrawable){
			if(!(OptWnd.disableObjectAnimationsCheckBox.a && thisGobAnimationsCanBeDisabled)){
				a.ctick(dt);
			}
		}
		else
	    a.ctick(dt);
		}
	for(Iterator<Overlay> i = ols.iterator(); i.hasNext();) {
	    Overlay ol = i.next();
	    if(ol.slots == null) {
		try {
		    ol.init();
		} catch(Loading e) {}
	    } else {
		boolean done = ol.tick(dt);
		if((!ol.delign || (ol.spr instanceof Sprite.CDel)) && done) {
		    ol.remove0();
		    i.remove();
		}
	    }
	}
	updstate();
	if(virtual && ols.isEmpty() && (getattr(Drawable.class) == null))
	    glob.oc.remove(this);
	if (isMe == null) {
		isMe = isItMe();
	}
	if (isMe != null) {
		setCustomPlayerName();
		playPlayerAlarm();
	}
	if (getattr(Moving.class) instanceof Following){
		Following following = (Following) getattr(Moving.class);
		occupiedGobID = following.tgt;
		if (occupiedGobID != null) {
			Gob OccupiedGob = glob.oc.getgob(occupiedGobID);
			if (OccupiedGob != null) {
				synchronized (OccupiedGob.occupants) {
					if (!OccupiedGob.occupants.contains(this)) {
						OccupiedGob.occupants.add(this);
					}
				}
			}
		}
	}
    }

    public void gtick(Render g) {
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    d.gtick(g);
	List<Overlay> olsSnapshot = new ArrayList<>(ols); // ND: Idk if this will break anything, but THIS PIECE OF SHIT SEEMS TO BE THE ROOT CAUSE OF ALL OLS CRASHES
	for(Overlay ol : olsSnapshot) {
	    if(ol.spr != null)
		ol.spr.gtick(g);
	}
    }

    void removed() {
	removed = true;
    }

    private void deferred() {
	while(true) {
	    Runnable task;
	    synchronized(deferred) {
		task = deferred.peek();
		if(task == null) {
		    deferral = null;
		    return;
		}
	    }
	    synchronized(this) {
		if(!removed)
		    task.run();
	    }
	    if(task instanceof Disposable)
		((Disposable)task).dispose();
	    synchronized(deferred) {
		if(deferred.poll() != task)
		    throw(new RuntimeException());
	    }
	}
    }

    public void defer(Runnable task) {
	synchronized(deferred) {
	    deferred.add(task);
	    if(deferral == null)
		deferral = glob.loader.defer(this::deferred, null);
	}
    }

    public void addol(Overlay ol, boolean async) {
	if (getres() != null) {
		if(OptWnd.disableIndustrialSmokeCheckBox.a && !getres().name.equals("gfx/terobjs/clue")) {
				if (ol.spr != null && ol.spr.res != null && ol.spr.res.name.contains("ismoke")) {
					return;
			}
		}
		if(OptWnd.disableScentSmokeCheckBox.a && getres().name.equals("gfx/terobjs/clue")) {
			if (ol.spr != null && ol.spr.res != null && ol.spr.res.name.contains("ismoke")) {
				return;
			}
		}
	}
	if(async) {
	    defer(() -> addol(ol, false));
	    return;
	}
	ol.init();
	ol.add0();
	ols.add(ol);
	try {
		Sprite spr = ol.spr;
		if(spr != null) {
			Resource res = spr.res;
			if(res != null) {
				MessageBuf sdt = spr.sdt;
				if(sdt != null && res.name.equals("gfx/fx/floatimg")) {
					processDmg(sdt.clone());
				}
			}
		}
	} catch (Loading ignored) {}
	// ND: Update these here as well, just to make sure they don't bug out with the collision box overlay when initialised (idfk why it breaks)
	updateContainerFullnessHighlight();
	updateWorkstationProgressHighlight();
    }
    public void addol(Overlay ol) {
	addol(ol, true);
    }
    public void addol(Sprite ol) {
	addol(new Overlay(this, ol));
    }
    public void addol(Indir<Resource> res, Message sdt) {
	addol(new Overlay(this, -1, res, sdt));
    }
    public void addol(Sprite.Mill<?> ol) {
	addol(new Overlay(this, ol));
    }
    public <S extends Sprite> S addolsync(Sprite.Mill<S> sm) {
	Overlay ol = new Overlay(this, sm);
	addol(ol, false);
	@SuppressWarnings("unchecked") S ret = (S)ol.spr;
	return(ret);
    }

    public Overlay findol(int id) {
	List<Overlay> tempOls;
	synchronized (ols) {
		tempOls = new ArrayList<>(ols);
	}
	for(Overlay ol : tempOls) {
	    if(ol.id == id)
		return(ol);
	}
	return(null);
    }

    public void dispose() {
	Map<Class<? extends GAttrib>, GAttrib> attr = cloneattrs();
	for(GAttrib a : attr.values())
	    a.dispose();
    }

    public void move(Coord2d c, double a) {
	Moving m = getattr(Moving.class);
	if(m != null)
		m.move(c);
	this.gobSpeed = m != null ? m.getv() : 0;
		if(isMe != null && isMe && MappingClient.getInstance() != null) {
			if (OptWnd.uploadMapTilesCheckBox.a)
				MappingClient.getInstance().CheckGridCoord(c);
			if (OptWnd.sendLiveLocationCheckBox.a)
				MappingClient.getInstance().Track(id, c);
		}
	this.rc = c;
	this.a = a;
    }

    public Placer placer() {
	Drawable d = getattr(Drawable.class);
	if(d != null) {
	    Placer ret = d.placer();
	    if(ret != null)
		return(ret);
	}
	return(glob.map.mapplace);
    }

    public Coord3f getc() {
	Moving m = getattr(Moving.class);
	Coord3f ret = (m != null) ? m.getc() : getrc();
	DrawOffset df = getattr(DrawOffset.class);
	if(df != null)
	    ret = ret.add(df.off);
	return(ret);
    }

    public Coord3f getrc() {
	return(placer().getc(rc, a));
    }

    protected Pipe.Op getmapstate(Coord3f pc) {
	Tiler tile = glob.map.tiler(glob.map.gettile(new Coord2d(pc).floor(MCache.tilesz)));
	return(tile.drawstate(glob, pc));
    }

    private Class<? extends GAttrib> attrclass(Class<? extends GAttrib> cl) {
	while(true) {
	    Class<?> p = cl.getSuperclass();
	    if(p == GAttrib.class)
		return(cl);
	    cl = p.asSubclass(GAttrib.class);
	}
    }

    public <C extends GAttrib> C getattr(Class<C> c) {
	GAttrib attr = this.attr.get(attrclass(c));
	if(!c.isInstance(attr))
	    return(null);
	return(c.cast(attr));
    }

    private void setattr(Class<? extends GAttrib> ac, GAttrib a) {
	GAttrib prev = attr.remove(ac);
	if(prev != null) {
	    if((prev instanceof RenderTree.Node) && (prev.slots != null))
		RUtils.multirem(new ArrayList<>(prev.slots));
	    if(prev instanceof SetupMod)
		setupmods.remove(prev);
	}
	if(a != null) {
	    if(a instanceof RenderTree.Node && !a.skipRender) {
		try {
		    RUtils.multiadd(this.slots, (RenderTree.Node)a);
		} catch(Loading l) {
		    if(prev instanceof RenderTree.Node && !prev.skipRender) {
			RUtils.multiadd(this.slots, (RenderTree.Node)prev);
			attr.put(ac, prev);
		    }
		    if(prev instanceof SetupMod)
			setupmods.add((SetupMod)prev);
		    throw(l);
		}
	    }
	    if(a instanceof SetupMod)
		setupmods.add((SetupMod)a);
	    attr.put(ac, a);
	}
	if (ac == Drawable.class) {
		if (a != prev) {
			updateDrawableStuff();
		}
	}
	if(prev != null)
	    prev.dispose();

	if(ac == Moving.class && a == null) {
		if (occupiedGobID != null){
			Gob OccupiedGob = glob.oc.getgob(occupiedGobID);
			if (OccupiedGob != null){
				synchronized (OccupiedGob.occupants) {
					OccupiedGob.occupants.remove(this);
				}
				occupiedGobID = null;
			}
		}
		if (isMe != null && isMe)
			glob.sess.ui.gui.map.gobPathLastClick = null;
	}
	if (a instanceof Moving) {
		if (gobChaseVector != null) {
			gobChaseVector.remove();
			gobChaseVector = null;
		}
	}
	if (a instanceof Homing) {
		Homing homing = (Homing) a;
		if (gobChaseVector == null && homing != null) {
			gobChaseVector = new Overlay(this, new ChaseVectorSprite(this, homing));
			synchronized (ols) {
				addol(gobChaseVector);
			}
		} else if (gobChaseVector != null && homing != null) {
			gobChaseVector.remove();
			gobChaseVector = new Overlay(this, new ChaseVectorSprite(this, homing));
			synchronized (ols) {
				addol(gobChaseVector);
			}
		} else if (gobChaseVector != null) {
			gobChaseVector.remove();
			gobChaseVector = null;
		}
	}
    }

    public void setattr(GAttrib a) {
	setattr(attrclass(a.getClass()), a);
    }

    public void delattr(Class<? extends GAttrib> c) {
	setattr(attrclass(c), null);
    }

    public Supplier<? extends Pipe.Op> eqpoint(String nm, Message dat) {
	for(GAttrib attr : this.attr.values()) {
	    if(attr instanceof EquipTarget) {
		Supplier<? extends Pipe.Op> ret = ((EquipTarget)attr).eqpoint(nm, dat);
		if(ret != null)
		    return(ret);
	    }
	}
	return(null);
    }

    public static class GobClick extends Clickable {
	public final Gob gob;

	public GobClick(Gob gob) {
	    this.gob = gob;
	}

	public Object[] clickargs(ClickData cd) {
	    Object[] ret = {0, (int)gob.id, gob.rc.floor(OCache.posres), 0, -1};
	    for(Object node : cd.array()) {
		if(node instanceof Gob.Overlay) {
		    ret[0] = 1;
		    ret[3] = ((Gob.Overlay)node).id;
		}
		if(node instanceof FastMesh.ResourceMesh)
		    ret[4] = ((FastMesh.ResourceMesh)node).id;
	    }
	    return(ret);
	}

	public String toString() {
	    return(String.format("#<gob-click %s>", gob));
	}
    }

    protected void obstate(Pipe buf) {
    }

    private class GobState implements Pipe.Op {
	final Pipe.Op mods;

	private GobState() {
	    if(setupmods.isEmpty()) {
		this.mods = null;
	    } else {
		Pipe.Op[] mods = new Pipe.Op[setupmods.size()];
		int n = 0;
		for(SetupMod mod : setupmods) {
		    if((mods[n] = mod.gobstate()) != null)
			n++;
		}
		this.mods = (n > 0) ? Pipe.Op.compose(mods) : null;
	    }
	}

	public void apply(Pipe buf) {
	    if(!virtual)
		buf.prep(new GobClick(Gob.this));
	    buf.prep(new TickList.Monitor(Gob.this));
	    obstate(buf);
	    if(mods != null)
		buf.prep(mods);
	}

	public boolean equals(GobState that) {
	    return(Utils.eq(this.mods, that.mods));
	}
	public boolean equals(Object o) {
	    return((o instanceof GobState) && equals((GobState)o));
	}
    }
    private GobState curstate = null;
    private GobState curstate() {
	if(curstate == null)
	    curstate = new GobState();
	return(curstate);
    }

    private void updstate() {
	GobState nst;
	try {
	    nst = new GobState();
	} catch(Loading l) {
	    return;
	}
	if(!Utils.eq(nst, curstate)) {
	    try {
		for(RenderTree.Slot slot : slots)
		    slot.ostate(nst);
		this.curstate = nst;
	    } catch(Loading l) {
	    }
	}
    }

    public void added(RenderTree.Slot slot) {
	slot.ostate(curstate());
	for(Overlay ol : ols) {
	    if(ol.slots != null)
		slot.add(ol);
	}
	Map<Class<? extends GAttrib>, GAttrib> attr = cloneattrs();
	for(GAttrib a : attr.values()) {
	    if(a instanceof RenderTree.Node && !a.skipRender)
			try {
				slot.add((RenderTree.Node) a);
			} catch (GLObject.UseAfterFreeException ignored) {
			}
	}
	slots.add(slot);
    }

    public void removed(RenderTree.Slot slot) {
	slots.remove(slot);
    }

    private Waitable.Queue updwait = null;
    void updated() {
	synchronized(this) {
	    updateseq++;
	    if(updwait != null)
		updwait.wnotify();
	}
    }

    public void updwait(Runnable callback, Consumer<Waitable.Waiting> reg) {
	/* Caller should probably synchronize on this already for a
	 * call like this to even be meaningful, but just in case. */
	synchronized(this) {
	    if(updwait == null)
		updwait = new Waitable.Queue();
	    reg.accept(updwait.add(callback));
	}
    }

    public static class DataLoading extends Loading {
	public final transient Gob gob;
	public final int updseq;

	/* It would be assumed that the caller has synchronized on gob
	 * while creating this exception. */
	public DataLoading(Gob gob, String message) {
	    super(message);
	    this.gob = gob;
	    this.updseq = gob.updateseq;
	}

	public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
	    synchronized(gob) {
		if(gob.updateseq != this.updseq) {
		    reg.accept(Waitable.Waiting.dummy);
		    callback.run();
		} else {
		    gob.updwait(callback, reg);
		}
	    }
	}
    }

    public Random mkrandoom() {
	return(Utils.mkrandoom(id));
    }

    @Deprecated
    public Resource getres() {
	Drawable d = getattr(Drawable.class);
	if(d != null)
	    return(d.getres());
	return(null);
    }

    private static final ClassResolver<Gob> ctxr = new ClassResolver<Gob>()
	.add(Gob.class, g -> g)
	.add(Glob.class, g -> g.glob)
	.add(Session.class, g -> g.glob.sess);
    public <T> T context(Class<T> cl) {return(ctxr.context(cl, this));}

    /* Because generic functions are too nice a thing for Java. */
    public double getv() {
	Moving m = getattr(Moving.class);
	if(m == null)
	    return(0);
	return(m.getv());
    }

    public Collection<Location.Chain> getloc() {
	Collection<Location.Chain> ret = new ArrayList<>(slots.size());
	for(RenderTree.Slot slot : slots) {
	    Location.Chain loc = slot.state().get(Homo3D.loc);
	    if(loc != null)
		ret.add(loc);
	}
	return(ret);
    }

    public class Placed implements RenderTree.Node, TickList.Ticking, TickList.TickNode {
	/* XXX: Using a COW list is far from an ideal solution. It
	 * should work for the specific case of flavobjs (which are
	 * added asynchronously in a way that makes it difficult to
	 * lock on each individually), but it's certainly not a
	 * general solution, and it would be nice with something that
	 * is in fact more general. */
	private final Collection<RenderTree.Slot> slots = new java.util.concurrent.CopyOnWriteArrayList<>();
	private Placement cur;

	private Placed() {}

	private class Placement implements Pipe.Op {
	    final Pipe.Op flw, tilestate, mods;
	    final Coord3f oc, rc;
	    final Matrix4f rot;

	    Placement() {
		try {
		    Following flw = Gob.this.getattr(Following.class);
		    Pipe.Op flwxf = (flw == null) ? null : flw.xf();
		    Pipe.Op tilestate = null;
		    if(flwxf == null) {
			Coord3f oc = Gob.this.getc();
			Coord3f rc = new Coord3f(oc);
			rc.y = -rc.y;
			this.flw = null;
			this.oc = oc;
			this.rc = rc;
			this.rot = Gob.this.placer().getr(Coord2d.of(oc), Gob.this.a);
			tilestate = Gob.this.getmapstate(oc);
		    } else {
			this.flw = flwxf;
			this.oc = this.rc = null;
			this.rot = null;
		    }
		    this.tilestate = tilestate;
		    if(setupmods.isEmpty()) {
			this.mods = null;
		    } else {
			Pipe.Op[] mods = new Pipe.Op[setupmods.size()];
			int n = 0;
			for(SetupMod mod : setupmods) {
			    if((mods[n] = mod.placestate()) != null)
				n++;
			}
			this.mods = (n > 0) ? Pipe.Op.compose(mods) : null;
		    }
		} catch(Loading bl) {
		    throw(new Loading(bl) {
			    public String getMessage() {return(bl.getMessage());}

			    public void waitfor(Runnable callback, Consumer<Waitable.Waiting> reg) {
				Waitable.or(callback, reg, bl, Gob.this::updwait);
			    }
			});
		}
	    }

	    public boolean equals(Placement that) {
		if(this.flw != null) {
		    if(!Utils.eq(this.flw, that.flw))
			return(false);
		} else {
		    if(!(Utils.eq(this.oc, that.oc) && Utils.eq(this.rot, that.rot)))
			return(false);
		}
		if(!Utils.eq(this.tilestate, that.tilestate))
		    return(false);
		if(!Utils.eq(this.mods, that.mods))
		    return(false);
		return(true);
	    }

	    public boolean equals(Object o) {
		return((o instanceof Placement) && equals((Placement)o));
	    }

	    Pipe.Op gndst = null;
	    public void apply(Pipe buf) {
		if(this.flw != null) {
		    this.flw.apply(buf);
		} else {
		    if(gndst == null)
			gndst = Pipe.Op.compose(new Location(Transform.makexlate(new Matrix4f(), this.rc), "gobx"),
						new Location(rot, "gob"));
		    gndst.apply(buf);
		}
		if(tilestate != null)
		    tilestate.apply(buf);
		if(mods != null)
		    mods.apply(buf);
	    }
	}

	public Pipe.Op placement() {
	    return(new Placement());
	}

	public void autotick(double dt) {
	    synchronized(Gob.this) {
		Placement np;
		try {
		    np = new Placement();
		} catch(Loading l) {
		    return;
		}
		if(!Utils.eq(this.cur, np))
		    update(np);
	    }
	}

	private void update(Placement np) {
	    for(RenderTree.Slot slot : slots)
		slot.ostate(np);
	    this.cur = np;
	}

	public void added(RenderTree.Slot slot) {
	    slot.ostate(curplace());
	    slot.add(Gob.this);
	    slots.add(slot);
	}

	public void removed(RenderTree.Slot slot) {
	    slots.remove(slot);
	}

	public Pipe.Op curplace() {
	    if(cur == null)
		cur = new Placement();
	    return(cur);
	}

	public Coord3f getc() {
	    return((this.cur != null) ? this.cur.oc : null);
	}

	public TickList.Ticking ticker() {return(this);}
    }
    public final Placed placed = new Placed();

    public String toString() {
	return(String.format("#<ob %d %s>", id, getattr(Drawable.class)));
    }

	public void init(boolean throwLoading) {
		Resource res = getres();
		if (res != null) {
			String resBaseName = res.basename();
			for (String substring : animatedGobsToDisable) {
				if (resBaseName.contains(substring)) {
					thisGobAnimationsCanBeDisabled = true;
					break;
				}
			}
			if (getattr(Drawable.class) instanceof Composite) {
				try {
					initComp((Composite)getattr(Drawable.class));
					isComposite = true;
					if(!alarmPlayed.contains(id)) {
						if(AlarmManager.play(res.name, Gob.this)){
							alarmPlayed.add(id);
						}
					}
				} catch (Loading e) {
					if (!throwLoading) {
						glob.loader.syncdefer(() -> this.init(true), null, this);
					} else {
						throw e;
					}
				}
			} else {
				if(!alarmPlayed.contains(id)) {
					if(AlarmManager.play(res.name, Gob.this))
						alarmPlayed.add(id);
				}
			}
		}
		updateCustomIcons();
		updateCritterAuras();
		updateSpeedBuffAuras();
		updateMidgesAuras();
		updateBeastDangerRadii();
		updateTroughsRadius();
		updateBeeSkepRadius();
		updateMineLadderRadius();
		updateSupportOverlays();
		initPermanentHighlightOverlay();
	}

	public void updPose(HashSet<String> poses) {
		isComposite = true;
		knocked = (poses.contains("knock") || poses.contains("dead") || poses.contains("waterdead") || poses.contains("chicken-knock"));
		if (this.getres().name.equals("gfx/borka/body")) {
			isMannequin = (poses.contains("mannequinlift"));
			isSkeleton = (poses.contains("deadskeletonpose"));
		}
		updateCritterAuras();
		updateBeastDangerRadii();
		if (this.getres().name.equals("gfx/borka/body") && isMannequin != null && !isMannequin && isSkeleton != null && !isSkeleton){
			setPlayerGender();
			if  (!isDeadPlayer){
				checkIfPlayerIsDead(poses);
				if (playerPoseUpdatedCounter >= 2) { // ND: Do this to prevent the sounds from being played if you load in an already knocked/killed hearthling.
					knockedOrDeadPlayerSoundEfect(poses);
				}
				playerPoseUpdatedCounter = playerPoseUpdatedCounter + 1;
			}
			imDrinking = (poses.contains("drinkan"));
			imInCoracle = (poses.contains("coracleidle") || poses.contains("coraclerowan"));
			imOnSkis = (poses.contains("skian-idle") || poses.contains("skian-walk") || poses.contains("skian-run"));
			boolean imOnWater = onWaterAnimations.stream().anyMatch(target -> poses.stream().anyMatch(s -> s.contains(target)));
			if (poses.contains("spear-ready")) {
				archeryIndicator(155, !imOnWater);
			} else if (poses.contains("sling-aim")) {
				archeryIndicator(155, !imOnWater);
			} else if (poses.contains("drawbow")) {
				for (GAttrib g : this.attr.values()) {
					if (g instanceof Drawable) {
						if (g instanceof Composite) {
							Composite c = (Composite) g;
							if (c.comp.cequ.size() > 0) {
								for (Composited.ED item : c.comp.cequ) {
									if (item.res.res.get().basename().equals("huntersbow"))
										archeryIndicator(195, !imOnWater);
									else if (item.res.res.get().basename().equals("rangersbow"))
										archeryIndicator(252, !imOnWater);
								}
							}
						}
					}
				}
			} else {
				removeOl(archeryVector);
				archeryVector = null;
				removeOl(archeryRadius);
				archeryRadius = null;
			}
		}
	}

	public void initComp(Composite c) {
		c.cmpinit(this);
	}

	public void reloadTreeScale(){
		TreeScale treeScale = null;
		if (getres() != null) {
			if ((getres().name.startsWith("gfx/terobjs/trees") && !getres().name.endsWith("log") && !getres().name.endsWith("oldtrunk")) || getres().name.startsWith("gfx/terobjs/bushes")) {
				treeScale = getattr(TreeScale.class);
				if (treeScale != null) {
					float scale = treeScale.originalScale;
					delattr(TreeScale.class);
					setattr(new TreeScale(this, (OptWnd.treeAndBushScaleSlider.val/100f) * scale, scale));
				}
			}
		}
	}

	public void reloadTreeSwaying(){
		GobSvaj gobSvaj = null;
		if (getres() != null) {
			if ((getres().name.startsWith("gfx/terobjs/trees") && !getres().name.endsWith("log") && !getres().name.endsWith("oldtrunk") && !getres().name.endsWith("trombonechantrelle") && !getres().name.endsWith("towercap")) || getres().name.startsWith("gfx/terobjs/bushes")) {
				gobSvaj = getattr(GobSvaj.class);
				if (gobSvaj != null && (OptWnd.disableTreeAndBushSwayingCheckBox.a)) {
					delattr(GobSvaj.class);
				} else if (!OptWnd.disableTreeAndBushSwayingCheckBox.a) {
					setattr(new GobSvaj(this));
				}
			}
		}
	}

	public void removeOl(Overlay ol) {
		if (ol != null) {
			synchronized (ols) {
				ol.remove();
			}
		}
	}

	public void updateCustomIcons() {
		if(getattr(GobIcon.class) == null) {
			GobIcon icon = GobIconsCustom.getIcon(this);
			if(icon != null) {
				setattr(icon);
			}
		}
	}

	public Boolean isItMe() {
		if(isMe == null) {
			if(glob.sess.ui.gui == null || glob.sess.ui.gui.map == null || glob.sess.ui.gui.map.plgob < 0) {
				return null;
			} else {
				return id == glob.sess.ui.gui.map.plgob;
			}
		}
		return isMe;
	}

	public void setCustomPlayerName() {
		if (!playerNameChecked) {
			if (getattr(Buddy.class) == null && getattr(haven.res.ui.obj.buddy_n.Named.class) == null && isMannequin != null && !isMannequin && isSkeleton != null && !isSkeleton && glob.sess.ui.gui != null && glob.sess.ui.gui.map != null) {
				if (getres() != null) {
					if (getres().name.equals("gfx/borka/body")) {
						long plgobid = glob.sess.ui.gui.map.plgob;
						if (plgobid != -1 && plgobid != id) {
							if (isLoftar)
								setattr(new Buddy(this, -1, "Loftar", Color.WHITE));
							else if ((getattr(Vilmate.class) != null))
								setattr(new Buddy(this, -1, "Village/Realm Member", Color.WHITE));
							else {
								setattr(new Buddy(this, -1, "Unknown", Color.GRAY));
							}
						}
					}
				}
			}
			playerNameChecked = true;
		}
	}

	public void isItLoftar(List<Composited.MD> mod, List<Composited.ED> equ) {
		if (getres() != null) {
			if (getres().name.equals("gfx/borka/body")) {
				if (mod != null && equ != null) {
					if (!mod.isEmpty() && !equ.isEmpty()) {
						boolean isMale = false;
						for (Composited.MD item : mod) {
							if (item.mod.get().basename().equals("male")) {
								isMale = true;
								break;
							}
						}
						if (isMale){
							boolean isgandalfhat = false;
							boolean isravens = false;
							for (Composited.ED item : equ) {
								if (item.res.res.get().basename().equals("gandalfhat")){
									isgandalfhat = true;
								} else if (item.res.res.get().basename().equals("ravens")){
									isravens = true;
								}
								if (isgandalfhat && isravens){
									if (getattr(Buddy.class) != null)
										delattr(Buddy.class);
									isLoftar = true;
									playerNameChecked = false;
									break;
								}
							}
						}
					}

				}
			}
		}
	}

	private Map<Class<? extends GAttrib>, GAttrib> cloneattrs() { // ND: To prevent concurrent modification exceptions
		synchronized (this.attr) {
			return new HashMap<>(this.attr);
		}
	}

	public void updateHidingBoxes() {
		if (updateseq == 0) {
			return;
		}
		boolean doHide = false;
		boolean doShowHidingBox = false;
		Resource res = Gob.this.getres();
		if (res != null) {
			String resName = res.name;
			if (OptWnd.hideTreesCheckbox.a && resName.startsWith("gfx/terobjs/trees") && !resName.endsWith("log") && !resName.endsWith("oldtrunk")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideBushesCheckbox.a && resName.startsWith("gfx/terobjs/bushes")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideBouldersCheckbox.a && resName.startsWith("gfx/terobjs/bumlings")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideTreeLogsCheckbox.a && resName.startsWith("gfx/terobjs/trees") && (resName.endsWith("log") || resName.endsWith("oldtrunk"))) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideWallsCheckbox.a && (resName.startsWith("gfx/terobjs/arch/palisade") || resName.startsWith("gfx/terobjs/arch/brickwall")) && !resName.endsWith("gate")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideHousesCheckbox.a && Arrays.asList(Config.housesResPaths).contains(resName)) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideStockpilesCheckbox.a && resName.startsWith("gfx/terobjs/stockpile")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = true;
			} else if (OptWnd.hideCropsCheckbox.a && resName.startsWith("gfx/terobjs/plants") && !resName.endsWith("trellis")) {
				doHide = OptWnd.toggleGobHidingCheckBox.a;
				doShowHidingBox = false; // ND: You can walk through them anyway, so it doesn't matter. Their resource doesn't have an actual hitbox layer and we'll have an endless lag loop of trying to draw one.
			}
			isHidden = doHide;
			Drawable d = getattr(Drawable.class);
			if (d != null && d.skipRender != doHide) {
				d.skipRender = doHide;
				if (doHide) {
					if (d.slots != null) {
						ArrayList<RenderTree.Slot> tmpSlots = new ArrayList<>(d.slots);
						try {
							glob.loader.defer(() -> {
								synchronized(Gob.this) {
									RUtils.multiremSafe(tmpSlots);
								}
							}, null);
						} catch (Exception ignored) {
						}
					}
				} else {
					ArrayList<RenderTree.Slot> tmpSlots = new ArrayList<>(slots);
					try {
						glob.loader.defer(() -> {
							synchronized(Gob.this) {
								RUtils.multiadd(tmpSlots, d);
							}
						}, null);
					} catch (Exception ignored) {
					}
				}
			}
			if ((OptWnd.toggleGobHidingCheckBox.a && doShowHidingBox)) {
				if (hidingBoxHollow != null) {
					if (!hidingBoxHollow.show(true)) {
						hidingBoxHollow.fx.updateState();
					}
				} else if (!virtual || this instanceof MapView.Plob) {
					HidingBox hidingBoxHollow = HidingBox.forGob(this, false);
					if (hidingBoxHollow != null) {
						this.hidingBoxHollow = new HitBoxGobSprite<>(this, hidingBoxHollow);
						synchronized (ols) {
							addol(this.hidingBoxHollow);
						}
					}
				}
			} else if (hidingBoxHollow != null) {
				hidingBoxHollow.show(false);
			}

			if ((OptWnd.toggleGobHidingCheckBox.a && OptWnd.alsoFillTheHidingBoxesCheckBox.a && doShowHidingBox)) {
				if (hidingBoxFilled != null) {
					if (!hidingBoxFilled.show(true)) {
						hidingBoxFilled.fx.updateState();
					}
				} else if (!virtual || this instanceof MapView.Plob) {
					HidingBox hidingBoxFilled = HidingBox.forGob(this, true);
					if (hidingBoxFilled != null) {
						this.hidingBoxFilled = new HitBoxGobSprite<>(this, hidingBoxFilled);
						synchronized (ols) {
							addol(this.hidingBoxFilled);
						}
					}
				}
			} else if(hidingBoxFilled != null) {
				hidingBoxFilled.show(false);
			}
		}
	}

	public void updateCollisionBoxes() {
		if (updateseq == 0) {
			return;
		}
		Resource res = Gob.this.getres();
		if (res != null) {
			if ((OptWnd.toggleGobCollisionBoxesCheckBox.a)) {
				if (collisionBox != null) {
					if (!collisionBox.show(true)) {
						collisionBox.fx.updateState();
					}
				} else if (!virtual || this instanceof MapView.Plob) {
					CollisionBox collisionBox = CollisionBox.forGob(this);
					if (collisionBox != null) {
						this.collisionBox = new HitBoxGobSprite<>(this, collisionBox);
						synchronized (ols) {
							addol(this.collisionBox);
						}
					}
				}
			} else if (collisionBox != null) {
				collisionBox.show(false);
			}
		}
	}

	public void setQualityInfo(int quality){
		qualityInfo.clear();
		qualityInfo.setQ(quality);
	}


	public void updateContainerFullnessHighlight() {
		if (getres() != null) {
			String resName = getres().name;
			if (Arrays.stream(Config.containersResPaths).anyMatch(resName::matches)) {
				Drawable dr = getattr(Drawable.class);
				ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
				if (d != null) {
					setContainerHighlight(resName, d.sdt);
				}
			}
		}
	}

	private void setContainerHighlight(String resName, MessageBuf sdt){
		int peekrbuf = sdt.checkrbuf(0);
		if (OptWnd.showContainerFullnessCheckBox.a) {
			switch (resName) {
				case "gfx/terobjs/cupboard":
				case "gfx/terobjs/chest":
				case "gfx/terobjs/exquisitechest":
				case "gfx/terobjs/map/stonekist":
					if (peekrbuf == 30 || peekrbuf == 29) {
						if (OptWnd.showContainerFullnessFullCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessFullColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 2 || peekrbuf == 1) {
						if (OptWnd.showContainerFullnessEmptyCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessEmptyColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessPartialCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessPartialColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/crate":
				case "gfx/terobjs/linencrate":
					if (peekrbuf == 16) {
						if (OptWnd.showContainerFullnessFullCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessFullColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 0) {
						if (OptWnd.showContainerFullnessEmptyCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessEmptyColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessPartialCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessPartialColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/leatherbasket":
				case "gfx/terobjs/thatchbasket":
					if (peekrbuf == 4) {
						if (OptWnd.showContainerFullnessFullCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessFullColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 0) {
						if (OptWnd.showContainerFullnessEmptyCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessEmptyColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessPartialCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessPartialColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/woodbox":
					if (peekrbuf == 8) {
						if (OptWnd.showContainerFullnessFullCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessFullColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 0) {
						if (OptWnd.showContainerFullnessEmptyCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessEmptyColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessPartialCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessPartialColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/largechest":
				case "gfx/terobjs/birchbasket":
				case "gfx/terobjs/stonecasket":
				case "gfx/terobjs/bonechest":
					if (peekrbuf == 17 || peekrbuf == 18) {
						if (OptWnd.showContainerFullnessFullCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessFullColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 2 || peekrbuf == 1) {
						if (OptWnd.showContainerFullnessEmptyCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessEmptyColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessPartialCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessPartialColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/coffer":
				case "gfx/terobjs/metalcabinet":
					if (peekrbuf == 65 || peekrbuf == 66) {
						if (OptWnd.showContainerFullnessFullCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessFullColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 2 || peekrbuf == 1) {
						if (OptWnd.showContainerFullnessEmptyCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessEmptyColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessPartialCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessPartialColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/map/jotunclam":
					if ((peekrbuf == 113) || (peekrbuf == 114)){
						if (OptWnd.showContainerFullnessFullCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessFullColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else if (peekrbuf == 2 || peekrbuf == 1) {
						if (OptWnd.showContainerFullnessEmptyCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessEmptyColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else {
						if (OptWnd.showContainerFullnessPartialCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessPartialColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					}
					break;
				case "gfx/terobjs/barrel":
					int olsSize = ols.size();
					if(collisionBox != null)
						olsSize = olsSize - 1;
					if (olsSize < 1) {
						if (OptWnd.showContainerFullnessEmptyCheckBox.a) setGobStateHighlight(OptWnd.showContainerFullnessEmptyColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else delattr(GobStateHighlight.class);
					break;
				default:
					break;
			}
		} else {
			delattr(GobStateHighlight.class);
		}
	}

	private void setGobStateHighlight(Color color) {
		GobStateHighlight current = getattr(GobStateHighlight.class);
		if (current != null) {
			current.color = color;
		} else  {
			setattr(new GobStateHighlight(this, color));
		}
	}

	public void updateCustomSizeAndRotation(){
		customSizeAndRotation.update(this);
		if (getres() != null && getres().name.equals("gfx/terobjs/cupboard")) {
			CollisionBox.MODEL_CACHE.remove("gfx/terobjs/cupboard");
			if (collisionBox != null) {
				collisionBox.fx.updateState();
			}
			synchronized (ols) {
				for (Overlay ol : ols) {
					if (ol.spr != null && ol.spr.res != null && ol.spr.res.name.equals("gfx/terobjs/items/parchment-decal") && ol.slots != null) {
						synchronized (ol.slots) {
							for (RenderTree.Slot slot : ol.slots) {
								if (OptWnd.flatCupboardsCheckBox.a) {
									slot.cstate(Pipe.Op.compose(Location.scale(1, 1, 1.6f), Location.xlate(new Coord3f(0, 0, -5.4f))));
								} else {
									slot.cstate(Pipe.Op.compose(Location.scale(1, 1, 1), Location.xlate(new Coord3f(0, 0, 0))));
								}
							}
						}
					}
				}
			}
		}
	}

	public void setWorkstationProgressHighlight(String resName) {
		if (OptWnd.showWorkstationProgressCheckBox.a) {

			// ND: Workstations that depend on the rbuf
			Drawable drawable = getattr(Drawable.class);
			ResDrawable resDrawable = (drawable instanceof ResDrawable) ? (ResDrawable) drawable : null;
			int rbuf = (resDrawable != null) ? resDrawable.sdt.checkrbuf(0) : -1; // ND: Just also remember to check if resDrawable is not null wherever we use rbuf
			if (resName.equals("gfx/terobjs/ttub") && resDrawable != null) {
				if (rbuf == 0 || rbuf == 1 || rbuf == 4 || rbuf == 5) {
					if (OptWnd.showWorkstationProgressUnpreparedCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressUnpreparedColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				} else if (rbuf == 10 || rbuf == 9 || rbuf == 8) {
					if (OptWnd.showWorkstationProgressFinishedCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressFinishedColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				} else if (rbuf != 6) {
					if (OptWnd.showWorkstationProgressReadyForUseCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressReadyForUseColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				} else {
					if (OptWnd.showWorkstationProgressInProgressCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressInProgressColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				}
			}

			// ND: Workstations that depend on overlays
			int olsSize = ols.size(); // ND: For some workstations we count the amount of overlays to determine progress.
			if(collisionBox != null) // ND: The collisionBox overlay might not always exist, but when it does, remove it from the counter.
				olsSize = olsSize - 1;
			if (resName.equals("gfx/terobjs/dframe")) {
				boolean done = true;
				boolean empty = true;
				for (Overlay ol : ols) {
					try {
						Resource olres = ol.spr.res;
						if (olres != null) {
							empty = false;
							if (olres.name.endsWith("-blood") || olres.name.endsWith("-windweed") || olres.name.endsWith("-fishraw")) {
								done = false;
								break;
							}
						}
					} catch (Loading l) {
					}
				}
				if (OptWnd.showWorkstationProgressInProgressCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressInProgressColorOptionWidget.currentColor);
				else delattr(GobStateHighlight.class);
				if (done && !empty) {
					if (OptWnd.showWorkstationProgressFinishedCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressFinishedColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				} else if (empty) {
					if (OptWnd.showWorkstationProgressReadyForUseCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressReadyForUseColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				}
			}
			if (resName.equals("gfx/terobjs/cheeserack")) {
				if (olsSize == 3) {
					if (OptWnd.showWorkstationProgressFinishedCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressFinishedColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				} else if (olsSize == 0) {
					if (OptWnd.showWorkstationProgressReadyForUseCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressReadyForUseColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				} else {
					if (OptWnd.showWorkstationProgressInProgressCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressInProgressColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				}
			}

			// ND: Workstations that depend on both rbuf and overlays
			if (resName.equals("gfx/terobjs/gardenpot")) {
				if (olsSize == 2) {
					if (OptWnd.showWorkstationProgressFinishedCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressFinishedColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				} else if (olsSize == 1) {
					if (OptWnd.showWorkstationProgressInProgressCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressInProgressColorOptionWidget.currentColor);
					else delattr(GobStateHighlight.class);
				} else if (olsSize == 0) {
					if (rbuf == 3) {
						if (OptWnd.showWorkstationProgressReadyForUseCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressReadyForUseColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					} else { // (rbuf == 0 || peekrbuf == 1 || peekrbuf == 2)
						if (OptWnd.showWorkstationProgressUnpreparedCheckBox.a) setGobStateHighlight(OptWnd.showWorkstationProgressUnpreparedColorOptionWidget.currentColor);
						else delattr(GobStateHighlight.class);
					}
				} else {
					delattr(GobStateHighlight.class);
				}
			}
		} else {
			delattr(GobStateHighlight.class);
		}
	}

	public void updateWorkstationProgressHighlight() {
		if (getres() != null) {
			String resName = getres().name;
			if (Arrays.stream(Config.workstationsResPaths).anyMatch(resName::matches)) {
				setWorkstationProgressHighlight(resName);
			}
		}
	}

	public void setGobSearchOverlay() {
		if (getres() == null) return;
		String resourceName = getres().basename().toLowerCase().replace("stockpile", "");
		String searchKeyword = ObjectSearchWindow.objectSearchString.toLowerCase();
		boolean result = searchKeyword.length() > 1 && Fuzzy.fuzzyContains(resourceName, searchKeyword);
		String barterStandOverlays = null;
		if (resourceName.contains("barter")) {
			try {
				barterStandOverlays = this.ols.stream()
						.map(ol -> {
							if(Reflect.is(ol.spr, "haven.res.gfx.fx.eq.Equed")) {
							Sprite espr = Reflect.getFieldValue(ol.spr, "espr", Sprite.class);
								try {
									if (espr.res != null)
										return espr.res.basename();
									else return null;
								} catch (IndexOutOfBoundsException e) {
									return "N/A";
								}
							}
							return "N/A";
						})
						.filter(Objects::nonNull)
						.collect(Collectors.joining(", "));
			} catch (Exception ignores) {
			}
		}
		if (barterStandOverlays != null) {
			if (searchKeyword.startsWith("@") && Fuzzy.fuzzyContains(barterStandOverlays, searchKeyword.replaceAll("@", "")) && searchKeyword.length() > 2) {
				result = true;
			}
		}

		setSearchOl(result);
	}

	private void setSearchOl(boolean on) {
		if (on) {
			if (customSearchOverlay != null) {
				removeOl(customSearchOverlay);
				customSearchOverlay = null;
			}
			customSearchOverlay = new Overlay(this, new haven.sprites.GobSearchHighlight(this, null));
			synchronized (ols) {
				addol(customSearchOverlay);
			}
		} else if (customSearchOverlay != null) {
			removeOl(customSearchOverlay);
			customSearchOverlay = null;
		}
	}

	public void updateCritterAuras() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (knocked != null && !knocked) {
				if (Arrays.stream(Config.critterResPaths).anyMatch(resourceName::matches)) {
					setAuraCircleOverlay(OptWnd.showCritterAurasCheckBox.a, OptWnd.genericCritterAuraColorOptionWidget.currentColor);
				} else if (resourceName.matches(".*(rabbit|bunny)$")) {
					setAuraCircleOverlay(OptWnd.showCritterAurasCheckBox.a, OptWnd.rabbitAuraColorOptionWidget.currentColor);
				}
			} else if (knocked != null && knocked) {
				if (Arrays.stream(Config.critterResPaths).anyMatch(resourceName::matches)) {
					setAuraCircleOverlay(false, OptWnd.genericCritterAuraColorOptionWidget.currentColor);
				} else if (resourceName.matches(".*(rabbit|bunny)$")) {
					setAuraCircleOverlay(false, OptWnd.rabbitAuraColorOptionWidget.currentColor);
				}
			} else if (!isComposite) { // ND: For critters that can't have a knocked status, like insects.
				if (Arrays.stream(Config.critterResPaths).anyMatch(resourceName::matches)) {
					setAuraCircleOverlay(OptWnd.showCritterAurasCheckBox.a, OptWnd.genericCritterAuraColorOptionWidget.currentColor);
				}
			}
		}
	}

	private void setAuraCircleOverlay(boolean enabled, Color col, float size) {
		if (enabled) {
			if (customAuraOverlay != null) {
				removeOl(customAuraOverlay);
				customAuraOverlay = null;
			}
			customAuraOverlay = new Overlay(this, new AuraCircleSprite(this, col, size));
			synchronized (ols) {
				addol(customAuraOverlay);
			}
		} else if (customAuraOverlay != null) {
			removeOl(customAuraOverlay);
			customAuraOverlay = null;
		}
	}

	private void setAuraCircleOverlay(boolean enabled, Color col) {
		setAuraCircleOverlay(enabled, col, 10f);
	}

	public void updateSpeedBuffAuras() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.equals("gfx/terobjs/boostspeed"))
				setAuraCircleOverlay(OptWnd.showSpeedBuffAurasCheckBox.a, OptWnd.speedBuffAuraColorOptionWidget.currentColor, 6f);
		}
	}

	public void updateMidgesAuras() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.equals("gfx/kritter/midgeswarm/midgeswarm"))
				setAuraCircleOverlay(OptWnd.showMidgesCircleAurasCheckBox.a, new Color(192, 0, 0, 140), 6f);
		}
	}

	public void updateBeastDangerRadii() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (knocked != null && knocked == false) {
				if (Arrays.stream(Config.beastResPaths).anyMatch(resourceName::endsWith)) {
					if (resourceName.endsWith("/bat")) {
						if (nightQueenDefeated || batWingCapeEquipped) {
							setRadiusOverlay(false, null, 0f);
						} else {
							setRadiusOverlay(OptWnd.showBeastDangerRadiiCheckBox.a, new Color(192, 0, 0, 140), 120F);
						}
					} else {
						setRadiusOverlay(OptWnd.showBeastDangerRadiiCheckBox.a, new Color(192, 0, 0, 140), 120F);
					}
				}
			} else if (knocked != null && knocked == true) {
				if (Arrays.stream(Config.beastResPaths).anyMatch(resourceName::endsWith)) {
					setRadiusOverlay(false, null, 0f);
				}
			}
			else if (isComposite && knocked == null) { // ND: Workaround. Some of these animals have no animation when standing still, so knocked stays null. I think they have no poses to load or something. Didn't look too much into it.
				if (Arrays.stream(Config.beastResPaths).anyMatch(resourceName::endsWith)) {
					if (resourceName.endsWith("/bat")) {
						if (nightQueenDefeated || batWingCapeEquipped) {
							setRadiusOverlay(false, null, 0f);
						} else {
							setRadiusOverlay(OptWnd.showBeastDangerRadiiCheckBox.a, new Color(192, 0, 0, 140), 120F);
						}
					} else {
						setRadiusOverlay(OptWnd.showBeastDangerRadiiCheckBox.a, new Color(192, 0, 0, 140), 120F);
					}
				}
			}
		}
	}



	private void setRadiusOverlay(boolean enabled, Color col, float radius) {
		if (enabled) {
			if (customRadiusOverlay != null) {
				removeOl(customRadiusOverlay);
				customRadiusOverlay = null;
			}
			customRadiusOverlay = new Overlay(this, new RangeRadiusSprite(this, null, radius, col));
			synchronized (ols) {
				addol(customRadiusOverlay);
			}
		} else if (customRadiusOverlay != null) {
			removeOl(customRadiusOverlay);
			customRadiusOverlay = null;
		}
	}

	public void updateTroughsRadius() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.equals("gfx/terobjs/trough")){
				setRadiusOverlay(OptWnd.showFoodTroughsRadiiCheckBox.a, new Color(255, 136, 0, 128), 200f);
			}
		}
	}

	public void updateBeeSkepRadius() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.equals("gfx/terobjs/beehive")){
				setRadiusOverlay(OptWnd.showBeeSkepsRadiiCheckBox.a, new Color(255, 242, 0, 128), 150f);
			}
		}
	}


	private void setPlayerGender(){
		try {
			if (getres() != null) {
				for (GAttrib g : attr.values()) {
					if (g instanceof Drawable) {
						if (g instanceof Composite) {
							Composite c = (Composite) g;
							if (c.comp.cmod.size() > 0) {
								for (Composited.MD item : c.comp.cmod) {
									if (item.mod.get().basename().equals("male")) {
										playerGender = "male";
									} else if (item.mod.get().basename().equals("female")) {
										playerGender = "female";
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception ignored){}
	}

	public void checkIfPlayerIsDead(HashSet<String> poses){
		Gob hearthling = this;
		final Timer timer = new Timer(); // ND: Need to do this with a timer cause the knocked out birds get loaded a few miliseconds later. I hope 100 is enough to prevent any issues.
		timer.schedule(new TimerTask(){
			@Override
			public void run() {
				if (poses.contains("rigormortis")) {
					isDeadPlayer = true;
					return;
				}
				if (poses.contains("knock") || poses.contains("drowned")) {
					isDeadPlayer = true;
					for (GAttrib g : hearthling.attr.values()) {
						if (g instanceof Drawable) {
							if (g instanceof Composite) {
								Composite c = (Composite) g;
								if (c.comp.cequ.size() > 0) {
									for (Composited.ED item : c.comp.cequ) {
										if (item.res.res.get().basename().equals("knockchirp")) {
											isDeadPlayer = false;
											break;
										}
									}
								}
							}
						}
					}
				}
				timer.cancel();
			}
		}, 100);
	}

	public void knockedOrDeadPlayerSoundEfect(HashSet<String> poses){
		Gob hearthling = this;
		final Timer timer = new Timer(); // ND: Need to do this with a timer cause the knocked out birds get loaded a few miliseconds later. I hope 100 is enough to prevent any issues.
		timer.schedule(new TimerTask(){
			@Override
			public void run(){
				long now = System.currentTimeMillis();
				// ND: Should only allow this sound to play again after 45 seconds. If you loot someone, their body sometimes does the KO animation again.
				// So check if at least 45 seconds passed. Tt takes about 50ish seconds for a hearthling to get up after being knocked anyway. They can port or log out after 25-30ish seconds.
				if ((now - lastKnockedOutSoundtime) > 45000) {
					boolean imDead = true;
					ArrayList<Map.Entry<Class<? extends GAttrib>, GAttrib>> gAttribs = new ArrayList<>(hearthling.attr.entrySet());
					for (int i = 0; i < gAttribs.size(); i++) {
						Map.Entry<Class<? extends GAttrib>, GAttrib> entry = gAttribs.get(i);
						GAttrib g = entry.getValue();
						if (g instanceof Drawable) {
							if (g instanceof Composite) {
								Composite c = (Composite) g;
								if (c.comp.cequ.size() > 0) {
									for (Composited.ED item : c.comp.cequ) {
										if (item.res.res.get().basename().equals("knockchirp")) {
											imDead = false;
											break;
										}
									}
								}
							}
						}
					}
					if (poses.contains("knock") || poses.contains("drowned")) {
						if (!imDead) {
							File file = new File("res/customclient/sfx/PlayerKnockedOut.wav");
							if (file.exists()) {
								try {
									AudioInputStream in = AudioSystem.getAudioInputStream(file);
									AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
									AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
									Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
									((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 1));
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						} else {
							isDeadPlayer = true;
							File file = null;
							if (playerGender.equals("male")) {
								file = new File("res/customclient/sfx/MalePlayerKilled.wav");
							} else if (playerGender.equals("female")) {
								file = new File("res/customclient/sfx/FemalePlayerKilled.wav");
							}
							if (file != null && file.exists() && somethingJustDied) {
								try {
									AudioInputStream in = AudioSystem.getAudioInputStream(file);
									AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
									AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
									Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
									((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 1));
								} catch (UnsupportedAudioFileException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						lastKnockedOutSoundtime = now;
					}
				}
				timer.cancel();
			}
		}, 100);
	}

	public void checkIfObjectJustDied(){
		if (virtual){
			for (int i = 0; i < ols.size(); i++) {
				Overlay ol = (Overlay) ols.toArray()[i];
				if (ol.spr.res != null && ol.spr.res.name.equals("gfx/fx/death")){
					setSomethingJustDiedStatus();
				}
			}
		}
	}

	public static void setSomethingJustDiedStatus(){
		if (gobDeathFuture != null)
			gobDeathFuture.cancel(true);
		somethingJustDied = true;
		gobDeathFuture = gobDeathExecutor.scheduleWithFixedDelay(Gob::resetSomethingJustDiedStatus, 1, 5, TimeUnit.SECONDS);
	}
	public static void resetSomethingJustDiedStatus() {
		somethingJustDied = false;
		gobDeathFuture.cancel(true);
	}

	public void highlight(Color c) {
		GobPingHighlight h = getattr(GobPingHighlight.class);
		if (h != null) {
			delattr(h.getClass());
		}
		h = new GobPingHighlight(this, c);
		setattr(h);
		h.start();
	}

	public Set<String> getPoses() {
		Set<String> poses = new HashSet<>();
		if (this.isComposite) {
			try {
				if (this.getattr(Drawable.class) != null) {
					poses = new HashSet<>(((Composite) this.getattr(Drawable.class)).poses);

				}
			} catch (Exception ignored) { }
		}
		return poses;
	}

	public boolean isPartyMember() {
		synchronized (glob.party.memb) {
			for (Party.Member m : glob.party.memb.values()) {
				if (m.gobid == id)
					return true;
			}
		}
		return false;
	}

	public void playPlayerAlarm() {
		if (!alarmPlayed.contains(id)){
			if (getres() != null) {
				if (isMannequin != null && !isMannequin && isSkeleton != null && !isSkeleton){
					if (getres().name.equals("gfx/borka/body")) {
						Buddy buddyInfo = getattr(Buddy.class);
						boolean isVillager = getattr(Vilmate.class) != null;
						haven.res.ui.obj.buddy_n.Named namedInfo = getattr(haven.res.ui.obj.buddy_n.Named.class);
						if (!isMe) {
							if (buddyInfo != null) {
								if ((buddyInfo.customName != null && buddyInfo.customName.equals("Unknown"))) {
									playPlayerColorAlarm(OptWnd.whitePlayerAlarmEnabledCheckbox.a, OptWnd.whitePlayerAlarmFilename.buf.line(), OptWnd.whitePlayerAlarmVolumeSlider.val);
								} else if ((buddyInfo.customName != null && buddyInfo.customName.equals("Village/Realm Member") && isVillager)) {
									playPlayerColorAlarm(OptWnd.whiteVillageOrRealmPlayerAlarmEnabledCheckbox.a, OptWnd.whiteVillageOrRealmPlayerAlarmFilename.buf.line(), OptWnd.whiteVillageOrRealmPlayerAlarmVolumeSlider.val);
								} else if (buddyInfo.rgrp == 1) {
									playPlayerColorAlarm(OptWnd.greenPlayerAlarmEnabledCheckbox.a, OptWnd.greenPlayerAlarmFilename.buf.line(), OptWnd.greenPlayerAlarmVolumeSlider.val);
								} else if (buddyInfo.rgrp == 2) {
									playPlayerColorAlarm(OptWnd.redPlayerAlarmEnabledCheckbox.a, OptWnd.redPlayerAlarmFilename.buf.line(), OptWnd.redPlayerAlarmVolumeSlider.val);
								} else if (buddyInfo.rgrp == 3) {
									playPlayerColorAlarm(OptWnd.bluePlayerAlarmEnabledCheckbox.a, OptWnd.bluePlayerAlarmFilename.buf.line(), OptWnd.bluePlayerAlarmVolumeSlider.val);
								} else if (buddyInfo.rgrp == 4) {
									playPlayerColorAlarm(OptWnd.tealPlayerAlarmEnabledCheckbox.a, OptWnd.tealPlayerAlarmFilename.buf.line(), OptWnd.tealPlayerAlarmVolumeSlider.val);
								} else if (buddyInfo.rgrp == 5) {
									playPlayerColorAlarm(OptWnd.yellowPlayerAlarmEnabledCheckbox.a, OptWnd.yellowPlayerAlarmFilename.buf.line(), OptWnd.yellowPlayerAlarmVolumeSlider.val);
								} else if (buddyInfo.rgrp == 6) {
									playPlayerColorAlarm(OptWnd.purplePlayerAlarmEnabledCheckbox.a, OptWnd.purplePlayerAlarmFilename.buf.line(), OptWnd.purplePlayerAlarmVolumeSlider.val);
								} else if (buddyInfo.rgrp == 7) {
									playPlayerColorAlarm(OptWnd.orangePlayerAlarmEnabledCheckbox.a, OptWnd.orangePlayerAlarmFilename.buf.line(), OptWnd.orangePlayerAlarmVolumeSlider.val);
								}
							}
							if (namedInfo != null) {
								playPlayerColorAlarm(OptWnd.whitePlayerAlarmEnabledCheckbox.a, OptWnd.whitePlayerAlarmFilename.buf.line(), OptWnd.whitePlayerAlarmVolumeSlider.val);
							}
						}
					}
				}
			}
		}
	}

	private void playPlayerColorAlarm(Boolean enabled, String line, int val) {
		if (enabled) {
			try {
				File file = new File("AlarmSounds/" + line + ".wav");
				if (file.exists()) {
					AudioInputStream in = AudioSystem.getAudioInputStream(file);
					AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
					AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
					Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
					((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, val / 50.0));
					alarmPlayed.add(id);
				}
			} catch (Exception ignored) {
			}
		}
	}

	public void updateMineLadderRadius() {
		if (getres() != null) {
			String resourceName = getres().name;
			if (resourceName.equals("gfx/terobjs/ladder")){
				setRadiusOverlay(OptWnd.showMineSupportRadiiCheckBox.a, new Color(0, 121, 12, 128), 100F);
			}
		}
	}

	public void updateSupportOverlays(){
		if (getres() != null) {
			if (getres().name.equals("gfx/terobjs/map/naturalminesupport") ){
				setMiningSafeTilesOverlay(OptWnd.showMineSupportSafeTilesCheckBox.a, (float) a, 0);
			} else if (getres().name.equals("gfx/terobjs/ladder") || getres().name.equals("gfx/terobjs/minesupport") ){
				setMiningSafeTilesOverlay(OptWnd.showMineSupportSafeTilesCheckBox.a, (float) a, 1);
			} else if (getres().name.equals("gfx/terobjs/column")){
				setMiningSafeTilesOverlay(OptWnd.showMineSupportSafeTilesCheckBox.a, (float) a, 2);
			} else if (getres().name.equals("gfx/terobjs/minebeam")){
				setMiningSafeTilesOverlay(OptWnd.showMineSupportSafeTilesCheckBox.a, (float) a, 3);
			}
		}
	}

	public void setMiningSafeTilesOverlay(boolean enabled, float angle, int size) {
		if (enabled) {
			for (Overlay ol : ols) {
				if (ol.spr instanceof MiningSafeTilesSprite) {
					return;
				}
			}
			miningSafeTilesOverlay = new Overlay(this, new MiningSafeTilesSprite(this, angle, size));
			synchronized (ols) {
				addol(miningSafeTilesOverlay);
			}
		} else if (miningSafeTilesOverlay != null) {
			removeOl(miningSafeTilesOverlay);
			miningSafeTilesOverlay = null;
		}
	}


	public void setCombatFoeCircleOverlay() {
		if (OptWnd.showCirclesUnderCombatFoesCheckBox.a && combatFoeCircleOverlay == null) {
			combatFoeCircleOverlay = new Overlay(this, new AggroCircleSprite(this));
			synchronized (ols) {
				addol(combatFoeCircleOverlay);
			}
		} else if (!OptWnd.showCirclesUnderCombatFoesCheckBox.a && combatFoeCircleOverlay != null) {
			removeOl(combatFoeCircleOverlay);
			combatFoeCircleOverlay = null;
		}
	}

	public void removeCombatFoeCircleOverlay(){
		if (combatFoeCircleOverlay != null) {
			removeOl(combatFoeCircleOverlay);
			combatFoeCircleOverlay = null;
		}
	}

	public void initPermanentHighlightOverlay(){
		if (permanentHighlightList.contains(id)) {
			setattr(new GobPermanentHighlight(this));
		}
	}

	public void removePermanentHighlightOverlay(){
		if (Gob.permanentHighlightList.contains(id)) {
			Gob.permanentHighlightList.remove(id);
			delattr(GobPermanentHighlight.class);
		}
	}

	private void processDmg(MessageBuf msg) {
		try {
			msg.rewind();
			int v = msg.int32();
			msg.uint8();
			int c = msg.uint16();

			if(damage == null) {
				addDmg();
			}
			damage.update(c, v);
		} catch (Exception ignored) {
			ignored.printStackTrace();
		}
	}

	private void addDmg() {
		damage = new GobDamageInfo(this);
		setattr(GobDamageInfo.class, damage);
	}

	public void clearDmg() {
		setattr(GobDamageInfo.class, null);
		damage = null;
	}

	public boolean isFriend() {
		synchronized (glob.party.memb) {
			for (Party.Member m : glob.party.memb.values()) {
				if (m.gobid == id)
					return true;
			}
		}
		Buddy buddyInfo = getattr(Buddy.class);
		if (buddyInfo != null) {
			if (buddyInfo.customName != null && buddyInfo.customName.equals("Unknown")) return false;
			if (buddyInfo.rgrp == 1 && OptWnd.excludeGreenBuddyFromAggroCheckBox.a) return true;
			if (buddyInfo.rgrp == 2 && OptWnd.excludeRedBuddyFromAggroCheckBox.a) return true;
			if (buddyInfo.rgrp == 3 && OptWnd.excludeBlueBuddyFromAggroCheckBox.a) return true;
			if (buddyInfo.rgrp == 4 && OptWnd.excludeTealBuddyFromAggroCheckBox.a) return true;
			if (buddyInfo.rgrp == 5 && OptWnd.excludeYellowBuddyFromAggroCheckBox.a) return true;
			if (buddyInfo.rgrp == 6 && OptWnd.excludePurpleBuddyFromAggroCheckBox.a) return true;
			if (buddyInfo.rgrp == 7 && OptWnd.excludeOrangeBuddyFromAggroCheckBox.a) return true;
		}
		Vilmate vilmateInfo = getattr(Vilmate.class);
		if (vilmateInfo != null && OptWnd.excludeAllVillageOrRealmMembersFromAggroCheckBox.a) return true;

		return false;
	}

	public boolean isPlgob(GameUI gui) {
		try {
			return gui.map.plgob == id;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isMoving() {
		return (getattr(Moving.class) != null);
	}

	public LinMove getLinMove() {
		LinMove lm = getattr(LinMove.class);
		if (lm != null)
			return lm;

		Following follow = getattr(Following.class);
		if (follow != null)
			return follow.tgt().getattr(LinMove.class);

		return null;
	}

	private void archeryIndicator(int range, boolean imOnLand) {
		if (imOnLand){
			if (this.archeryVector == null) {
				archeryVector = new Overlay(this, new ArcheryVectorSprite(this, 45));
				synchronized (ols) {
					addol(archeryVector);
				}
			}
		}
		if (this.archeryRadius == null) {
			archeryRadius = new Overlay(this, new ArcheryRadiusSprite(this, range));
			synchronized (ols) {
				addol(archeryRadius);
			}
		}
	}

	public void updateDrawableStuff(){
		updateHidingBoxes();
		updateCollisionBoxes();
		updateContainerFullnessHighlight();
		updateCustomSizeAndRotation();
		updateWorkstationProgressHighlight();
		checkIfObjectJustDied();
		growthInfo.clear();
		barrelContentsGobInfo.clear();
		setGobSearchOverlay();
	}

	public void refreshGrowthInfo(){
		growthInfo.clear();
	}

}
