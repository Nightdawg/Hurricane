package haven.sprites.sky;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import static haven.render.sl.Type.*;

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
    /* Player position in RENDER space, horizontal only -- the origin SkyFog
     * measures its distances from. Render space is map space with y negated;
     * see from(). */
    public final float px, py;
    /* Night Mode lift already halved -- see Glob.nightVisionBrightness. */
    public final float night;
    /* Fog strength, 0 or 1. Gating this rather than attaching and detaching
     * SkyFog is what keeps cave transitions from recompiling every shader
     * in the scene. */
    public final float fog;

    public SkyPalette(Coord3f sundir, Coord3f plpos, double night, boolean fog) {
	Coord3f n = sundir.norm();
	this.sx = n.x; this.sy = n.y; this.sz = n.z;
	this.px = plpos.x; this.py = plpos.y;
	this.night = (float)night;
	this.fog = fog ? 1f : 0f;
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
    public static SkyPalette from(Glob glob, Coord3f plpos, boolean fog) {
	Coord3f rc = plpos.invy();
	double elev, ang;
	boolean lit;
	synchronized(glob) {
	    lit = (glob.lightamb != null);
	    elev = glob.lightelev;
	    ang = glob.lightang;
	}
	if(!lit)
	    return(new SkyPalette(new Coord3f(0f, 0f, 1f), rc, 0.0, false));
	return(new SkyPalette(Coord3f.o.sadd((float)elev, (float)ang, 1f), rc,
			      Glob.nightVisionBrightness * NIGHT_SHARE, fog));
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
    public static final Uniform u_plpos = new Uniform(VEC2, "skyplpos", p -> {
	    SkyPalette s = p.get(slot);
	    return((s == null) ? new float[] {0f, 0f} : new float[] {s.px, s.py});
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

    public ShaderMacro shader() {return(null);}
    public void apply(Pipe p) {p.put(slot, this);}

    public boolean equals(Object o) {
	if(!(o instanceof SkyPalette))
	    return(false);
	SkyPalette t = (SkyPalette)o;
	return((sx == t.sx) && (sy == t.sy) && (sz == t.sz)
	       && (px == t.px) && (py == t.py)
	       && (night == t.night) && (fog == t.fog));
    }

    public int hashCode() {
	return(Float.hashCode(sx) ^ Float.hashCode(sy) ^ Float.hashCode(sz)
	       ^ Float.hashCode(px) ^ Float.hashCode(py)
	       ^ Float.hashCode(night) ^ Float.hashCode(fog));
    }

    public String toString() {
	return(String.format("#<skypalette sun=(%f, %f, %f) pl=(%f, %f) night=%f fog=%f>",
			     sx, sy, sz, px, py, night, fog));
    }
}
