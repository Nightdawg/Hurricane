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
	Astronomy ast;
	synchronized(glob) {
	    lit = (glob.lightamb != null);
	    elev = glob.lightelev;
	    ang = glob.lightang;
	    ast = glob.ast;
	}
	if(!lit)
	    return(new SkyPalette(new Coord3f(0f, 0f, 1f), rect, 0.0, false));
	/* The azimuth still comes from the server's light, so the drawn sun and
	 * the shadows keep agreeing. That half of the vector was never wrong:
	 * aiming the camera down the shadows in game put the disc dead on that
	 * bearing, half a degree above the top edge of the screen. Only the
	 * elevation is replaced.
	 *
	 * Falling back to lightelev when ast is null is deliberate. The astro
	 * blob and the light blob arrive independently (Glob.java:267-300), so
	 * there is a window with light but no clock, and the old behaviour is a
	 * better thing to show there than a sun pinned at dawn. */
	double selev = (ast == null) ? elev : sunelev(ast.dt);
	return(new SkyPalette(Coord3f.o.sadd((float)selev, (float)ang, 1f), rect,
			      Glob.nightVisionBrightness * NIGHT_SHARE, fog));
    }

    /* Day fraction -> the sun's elevation for DRAWING, in radians.
     *
     * The server's lightelev cannot serve here. Measured over 4750 samples it
     * sweeps 23 to 67.5 degrees and never goes negative; through the night it
     * RISES, 0.5625 to 0.5882 rad across dt 0.808 to 0.821 with ast.night set.
     * It is the lighting rig the shadows are built from, not an astronomical
     * sun, and the client is right to keep it that way -- shadows that swing
     * to infinity at dusk would look far worse than a sun that does not set.
     *
     * Two defects followed from reading it as a sun. The disc was drawable
     * only where that sweep happened to land inside the band this projection
     * can show, which tops out at 31.75 degrees: about a third of its range,
     * and then only with the azimuth inside the 53-degree horizontal field,
     * so on the order of 5% of the time. And every night branch in SkyLib
     * keys on the elevation going negative, so the night sky, the dusk band
     * and the stars were all unreachable -- at 19:16 game time with ast.night
     * set, sky_baseA still computed a day factor of 1.583, saturated to full
     * daylight over night-lit terrain.
     *
     * PEAK compresses the arc into the drawable band with room left so the
     * disc is never clipped by the top edge. It is a compression, not a
     * physical elevation: the sky this shader draws is a fiction to begin
     * with, since a camera pitched 45 degrees down with a 15-degree half
     * field never sees real sky at all.
     *
     * Day runs dt 0.25 to 0.75, and both edges are measured. ast.night first
     * went true at dt = 0.75060 and first went false at 0.25161, with the
     * sample before it, 0.24810, still true -- so the morning edge is bracketed
     * around 0.25 to within the probe's five game-minute resolution. The
     * client's own calendar agrees independently: Cal.java:63 puts the sun on
     * the dial at (dt + 0.75) * 2pi, which is the horizontal at dt 0.25 and
     * 0.75 and the top at 0.50. */
    public static final double PEAK = 0.48;

    public static double sunelev(double dt) {
	return(PEAK * Math.sin(Math.PI * (dt - 0.25) / 0.5));
    }

    /* What the compression above costs everything downstream.
     *
     * The drawn arc peaks at PEAK. The sun the server's own lighting describes
     * peaks at NOON -- the maximum of lightelev, measured at 1.1781 rad and
     * reached at dt 0.500 to 0.525, solar noon. (lightelev is not this sine
     * over the rest of the day; it flattens onto a floor at 0.4018. Only the
     * peak is borrowed, because only the peak is being matched.)
     *
     * So DECOMP turns a DRAWN elevation back into a real one, and any
     * PHYSICAL falloff applied to the sun has to undo the compression with it
     * first -- exactly as ADR-0005 makes anything round on screen divide by
     * sky_gain. Skipping it is what kept the twilight glow alive at -38 real
     * degrees, painting dawn over the sky at 03:37 game time. See ADR-0010.
     *
     * Angles only. A drawn elevation reaches the shader as sin(elev) inside
     * the sun vector, so undoing it means asin() first -- which is what
     * SkyLib.sunh does. */
    public static final double NOON = 1.1781;         /* rad, 67.5 degrees */
    public static final double DECOMP = NOON / PEAK;  /* 2.4544 */

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

    /* The elevation stretch in force at this pitch. Anything drawn round on
     * screen -- the sun's disc, the stars -- divides its elevation offsets by
     * it. Same uniform as skyelev, so the two can never disagree. */
    public static Expression skygain(FragmentContext fctx) {
	return(SkyLib.gain.call(u_campitch.ref()));
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
