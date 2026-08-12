package haven.sprites.sky;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import static haven.render.sl.Type.*;
import static haven.MCache.tilesz;

/* Game state -> sky uniforms. No GLSL maths lives here; SkyLib owns that.
 *
 * MapView pushes one of these per tick via basic(SkyPalette.class, ...).
 * equals() lets PView.basic skip rebuilding its ostate when nothing moved.
 * That helps while the player stands still -- Glob.ticklight
 * (Glob.java:164-183) only interpolates the light during a two-second
 * window after each server update -- but not while walking, since the
 * player position is part of the state. That is acceptable: updweather()
 * already rebuilds unconditionally every tick (MapView.java:1386-1390
 * composes a fresh Pipe.Op[], which is never equal), so this is no worse
 * than what the frame already does. */
public class SkyPalette extends State {
    public static final Slot<SkyPalette> slot = new Slot<>(Slot.Type.DRAW, SkyPalette.class);

    /* World-space (Z-up), normalised, pointing at the sun. */
    public final float sx, sy, sz;
    /* The loaded map rectangle in RENDER space: {minx, miny, maxx, maxy}.
     * SkyFog measures how close a fragment is to the nearest edge of THIS,
     * not how far it is from the player.
     *
     * That distinction is the whole point. The cut grid is aligned to the
     * world, not to the player, so the map edge sits anywhere from 550 to
     * 825 units away depending on where inside their centre cut the player
     * happens to stand. Fogging on distance-from-player therefore has to be
     * opaque by 550 to cover the worst case -- which erases roughly 60% of
     * the terrain the client already loaded and drew. Measuring to the edge
     * itself fogs a band of constant width wherever the player stands.
     *
     * Render space is map space with y negated; see from(). */
    public final float rx0, ry0, rx1, ry1;
    /* Night Mode lift already halved -- see Glob.nightVisionBrightness. */
    public final float night;
    /* Fog strength, 0 or 1. Gating this rather than attaching and detaching
     * SkyFog is what keeps cave transitions from recompiling every shader
     * in the scene. */
    public final float fog;

    public SkyPalette(Coord3f sundir, float[] rect, double night, boolean fog) {
	Coord3f n = sundir.norm();
	this.sx = n.x; this.sy = n.y; this.sz = n.z;
	this.rx0 = rect[0]; this.ry0 = rect[1]; this.rx1 = rect[2]; this.ry1 = rect[3];
	this.night = (float)night;
	this.fog = fog ? 1f : 0f;
    }

    /* The loaded cut rectangle, in render space. MapRaster.tick
     * (MapView.java:971-973) computes the same thing for its own use, but it
     * is a field on a private inner class, so this recomputes rather than
     * reaching into it. One cut is MCache.cutsz * MCache.tilesz = 275 units.
     *
     * Map y grows the opposite way from render y, so the negation also swaps
     * which corner is the minimum -- hence the min/max rather than a
     * straight copy. */
    public static float[] maprect(Coord2d plpos, int view) {
	Coord cc = plpos.floor(tilesz).div(MCache.cutsz);
	Coord cs = MCache.cutsz.mul(Coord.of((int)tilesz.x, (int)tilesz.y));
	Coord ul = cc.sub(view, view).mul(cs);
	Coord br = cc.add(view + 1, view + 1).mul(cs);
	return(new float[] {
		Math.min(ul.x, br.x), Math.min(-ul.y, -br.y),
		Math.max(ul.x, br.x), Math.max(-ul.y, -br.y),
	    });
    }

    /* The sun as the world lighting sees it. Identical expression to
     * MapView.java:1308, so the drawn sun and the shadows agree.
     *
     * plpos arrives from MapView.getcc() in MAP space. Homo3D.fragmapv,
     * which SkyFog compares against, is in RENDER space -- which is map
     * space with y negated. Every producer of u_wxf does that negation:
     * Gob.java:1123-1125 (rc.y = -rc.y) for gobs, MapView.java:940
     * (Location.xlate(pc.x, -pc.y, 0)) for terrain cuts, MapMesh.java:125
     * for the vertices themselves. Six other call sites convert with
     * getcc().invy() (MapView.java:196, 251, 307, 397, 486, 1257). Get this
     * wrong and the fog origin lands at 2*|y| off -- and H&H map coordinates
     * are hundreds of thousands of units, so the whole world saturates to
     * flat horizon colour rather than looking subtly mirrored.
     *
     * Never returns null: before the server sends light data there is
     * nothing to draw a sky from, but detaching the state would churn the
     * shader set, so it reports fog = false and a default sun instead. */
    public static SkyPalette from(Glob glob, float[] rect, boolean fog) {
	double elev, ang;
	boolean lit;
	synchronized(glob) {
	    lit = (glob.lightamb != null);
	    elev = glob.lightelev;
	    ang = glob.lightang;
	}
	if(!lit)
	    return(new SkyPalette(new Coord3f(0f, 0f, 1f), rect, 0.0, false));
	probe(glob, elev);
	return(new SkyPalette(Coord3f.o.sadd((float)elev, (float)ang, 1f), rect,
			      Glob.nightVisionBrightness * NIGHT_SHARE, fog));
    }

    /* TEMPORARY -- Task 10 measurement, delete once the range is known.
     *
     * Everything night-related in SkyLib is gated on the sun's elevation
     * going negative, and nothing in the client establishes that the
     * server's lightelev ever does. This answers that.
     *
     * from() runs on every MapView.tick, so this reports only when an
     * extreme or the night flag actually moves -- an unthrottled print
     * floods the log and skews the timing being measured.
     *
     * It writes to ~/skyprobe.log as well as stderr because the client is
     * normally launched through Steam, which swallows stderr; nothing in
     * this tree redirects it to a file. */
    private static final java.io.File plog =
	new java.io.File(System.getProperty("user.home"), "skyprobe.log");
    private static double pmin = Double.POSITIVE_INFINITY, pmax = Double.NEGATIVE_INFINITY;
    private static Boolean pnight = null;
    private static void probe(Glob glob, double elev) {
	Astronomy ast = glob.ast;
	Boolean night = (ast == null) ? null : ast.night;
	boolean chg = false;
	if(elev < pmin) {pmin = elev; chg = true;}
	if(elev > pmax) {pmax = elev; chg = true;}
	if(!Utils.eq(night, pnight)) {pnight = night; chg = true;}
	if(!chg)
	    return;
	String ln = String.format("skyprobe elev=%.4f min=%.4f max=%.4f night=%s dt=%.4f",
				  elev, pmin, pmax, night, (ast == null) ? -1.0 : ast.dt);
	System.err.println(ln);
	try(java.io.FileWriter w = new java.io.FileWriter(plog, true)) {
	    w.write(ln + "\n");
	} catch(java.io.IOException e) {
	    /* The measurement is not worth breaking a frame over. */
	}
    }

    /* The sky takes half the lift the terrain takes. Full strength washes
     * the night sky to flat grey and drowns the stars. */
    public static final double NIGHT_SHARE = 0.5;

    /* Cached because SkyFog.current() and SkyboxShader.current() would
     * otherwise read java.util.prefs on every tick. OptWnd calls reload()
     * when the user changes either. volatile because OptWnd writes on the
     * UI thread while the render tree reads during slot construction --
     * the same unsynchronised-static defect this work removes from
     * OptWnd.skyboxFuture. */
    public static volatile int style = Utils.getprefi("skyboxStyle", 0);
    public static volatile boolean hq = Utils.getprefb("skyboxQuality", false);

    public static void reload() {
	style = Utils.getprefi("skyboxStyle", 0);
	hq = Utils.getprefb("skyboxQuality", false);
    }

    public static final Uniform u_sundir = new Uniform(VEC3, "skysun", p -> {
	    SkyPalette s = p.get(slot);
	    return((s == null) ? new float[] {0f, 0f, 1f} : new float[] {s.sx, s.sy, s.sz});
	}, slot);
    public static final Uniform u_night = new Uniform(FLOAT, "skynight", p -> {
	    SkyPalette s = p.get(slot);
	    return((s == null) ? 0f : s.night);
	}, slot);
    public static final Uniform u_maprect = new Uniform(VEC4, "skymaprect", p -> {
	    SkyPalette s = p.get(slot);
	    return((s == null) ? new float[] {0f, 0f, 0f, 0f}
		   : new float[] {s.rx0, s.ry0, s.rx1, s.ry1});
	}, slot);
    public static final Uniform u_fogstr = new Uniform(FLOAT, "skyfogstr", p -> {
	    SkyPalette s = p.get(slot);
	    return((s == null) ? 0f : s.fog);
	}, slot);

    /* Eye space -> world space rotation, for turning a fragment's view
     * direction back into a world direction. Same expression the old
     * skybox shader used. */
    public static final Uniform u_icam = new Uniform(MAT3, p -> Homo3D.camxf(p).transpose(), Homo3D.cam);

    /* World-space view direction of the current fragment (Z-up).
     * fragedir points at the eye, so negate it to point into the scene. */
    public static Expression viewdir(FragmentContext fctx) {
	return(Cons.mul(u_icam.ref(), Cons.neg(Homo3D.fragedir(fctx).depref())));
    }

    /* This fragment's sky elevation, in radians. SkyLib.elev owns the maths
     * and the reasoning; this only feeds it the eye-space y and z and the
     * camera's pitch.
     *
     * frageyev is the fragment's position in eye space, so atan(y, -z) is its
     * angle above the camera axis. It is an AutoVarying, which resolves on the
     * vertex context, so referencing it from inside a mod lambda is safe even
     * though the fragment value-block is locked by then. */
    public static Expression skyelev(FragmentContext fctx) {
	return(SkyLib.elev.call(Cons.pick(Homo3D.frageyev.ref(), "y"),
				Cons.pick(Homo3D.frageyev.ref(), "z"),
				u_campitch.ref()));
    }

    /* How far the camera is looking down, in radians: 0 level, pi/2 straight
     * down. FreeCam defaults to pi/4 and the drag can take it anywhere in
     * between (MapView.java:287, 328-337).
     *
     * camxf is the world-to-eye rotation, so the camera's world forward is
     * minus its third row, and m[10] -- column-major, so element (2,2) -- is
     * the negated z of that. asin of it is the pitch directly, for any
     * azimuth; verified against Camera.makepointed across four elevations and
     * three azimuths. */
    public static final Uniform u_campitch =
	new Uniform(FLOAT, "skycampitch",
		    p -> (float)Math.asin(Utils.clip(Homo3D.camxf(p).m[10], -1f, 1f)),
		    Homo3D.cam);

    public ShaderMacro shader() {return(null);}
    public void apply(Pipe p) {p.put(slot, this);}

    public boolean equals(Object o) {
	if(!(o instanceof SkyPalette))
	    return(false);
	SkyPalette t = (SkyPalette)o;
	return((sx == t.sx) && (sy == t.sy) && (sz == t.sz)
	       && (rx0 == t.rx0) && (ry0 == t.ry0) && (rx1 == t.rx1) && (ry1 == t.ry1)
	       && (night == t.night) && (fog == t.fog));
    }

    public int hashCode() {
	return(Float.hashCode(sx) ^ Float.hashCode(sy) ^ Float.hashCode(sz)
	       ^ Float.hashCode(rx0) ^ Float.hashCode(ry0)
	       ^ Float.hashCode(rx1) ^ Float.hashCode(ry1)
	       ^ Float.hashCode(night) ^ Float.hashCode(fog));
    }

    public String toString() {
	return(String.format("#<skypalette sun=(%f, %f, %f) rect=(%f, %f, %f, %f) night=%f fog=%f>",
			     sx, sy, sz, rx0, ry0, rx1, ry1, night, fog));
    }
}
