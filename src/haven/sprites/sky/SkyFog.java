package haven.sprites.sky;

import haven.*;
import haven.render.*;
import haven.render.sl.*;
import static haven.render.sl.Cons.*;
import static haven.render.sl.Type.*;

/* Fades everything under MapView's basic slot -- terrain and gobs both --
 * into the sky's own horizon colour, so the draw-distance edge stops being
 * a hard cut.
 *
 * Distances: MapView.view is 2 CUTS (MapView.java:969-970 divides by
 * MCache.cutsz), cutsz is 25 tiles and tilesz is 11 units, so one cut is 275
 * units and the rendered area is 5x5 cuts. The player sits inside the centre
 * cut, so the nearest terrain edge is between 550 and 825 units away. Fog
 * must be fully opaque by 550 or the cut shows.
 *
 * Those are distances from the PLAYER, and the fog measures them as such:
 * horizontal distance in map space, from a player-position uniform. Do not
 * "simplify" this to length(eyev). Eye distance is a different quantity --
 * FreeCam sits 400 units back by default and can pull out to 3000
 * (MapView.java:285,304) -- so eye-space fog would haze the player's own
 * feet and would turn the whole world one flat colour when zoomed out.
 * Horizontal rather than 3D distance is also deliberate: terrain height
 * should not decide how fogged a tile is. */
public class SkyFog extends State {
    public static final Slot<SkyFog> slot = new Slot<>(Slot.Type.DRAW, SkyFog.class);

    /* Tuned in Task 12. END must stay <= 550. */
    public static final double START = 300.0;
    public static final double END = 540.0;

    public static final SkyFog quality = new SkyFog(true);
    public static final SkyFog cheap = new SkyFog(false);

    /* Galaxy has no analytic horizon and no mip chain to blur, so it borrows
     * the cheap gradient -- smooth and desaturated, least likely to clash. */
    public static SkyFog current() {
	if(SkyPalette.style == 1)
	    return(cheap);
	return(SkyPalette.hq ? quality : cheap);
    }

    public final boolean hq;
    private final ShaderMacro shader;

    private SkyFog(boolean hq) {
	this.hq = hq;
	Function hor = hq ? SkyLib.horB : SkyLib.horA;
	this.shader = prog -> {
	    /* This state rides on MapView's basic slot, so it reaches
	     * everything under it -- including screen-quad post-effects that
	     * have no Homo3D at all (Outlines, MapView.java:605, uses Ortho2D
	     * in the vxf slot instead). Asking for fragmapv there would
	     * construct a Homo3D inside a locked value-block and reference a
	     * vertex attribute the quad does not supply. ShadowMap.java:311-313
	     * guards the same way. */
	    if(prog.getmod(Homo3D.class) == null)
		return;
	    /* Load-bearing: creates the fragedir value before the mod lambda
	     * runs inside a locked ValBlock. See Task 7. */
	    Homo3D.fragedir(prog.fctx);
	    /* Order 5000: after Phong writes lighting at 500 (Phong.java:185),
	     * so fog covers lit colour rather than being lit itself. The only
	     * mod above this anywhere in the tree is Lighting.java:490 at 50000. */
	    FragColor.fragcol(prog.fctx).mod(in -> {
		    Expression dist = length(sub(pick(Homo3D.fragmapv.ref(), "xy"),
						 SkyPalette.u_plpos.ref()));
		    Expression f = mul(smoothstep(l(START), l(END), dist),
				       SkyPalette.u_fogstr.ref());
		    Expression col = hor.call(SkyPalette.viewdir(prog.fctx),
					      SkyPalette.u_sundir.ref(),
					      SkyPalette.u_night.ref());
		    return(vec4(mix(pick(in, "rgb"), col, f), pick(in, "a")));
		}, 5000);
	};
    }

    public ShaderMacro shader() {return(shader);}
    public void apply(Pipe p) {p.put(slot, this);}

    public String toString() {return(String.format("#<skyfog hq=%b>", hq));}
}
