/* Preprocessed source code */
/* $use: gfx/fx/mscover */

package haven.res.gfx.fx.msrad;

import java.util.function.*;
import haven.*;
import haven.res.gfx.fx.mscover.*;
import static haven.MCache.*;

/* >objdelta: Radius */
@haven.FromResource(name = "gfx/fx/msradius", version = 1)
public class Radius extends Coverage {
    public static final double ε = 0.01 * 11;
    public final double r;

    public Radius(Gob owner, double r, boolean real) {
	super(owner, real);
	this.r = r;
	gl.add(this);
    }

    public Area extent(Coord2d cc, double ang) {
	return(Area.corn(cc.sub(r, r).floor(tilesz),
			 cc.add(r, r).ceil(tilesz)));
    }

    public void cover(Coord2d cc, double ang, Area clip, Consumer<Coord> dst) {
	Area a = Area.corn(cc.sub(r, r).floor(tilesz),
			   cc.add(r, r).ceil(tilesz));
	if((clip != null) && ((a = a.overlap(clip)) == null))
	    return;
	for(Coord tc : a) {
	    if(Coord2d.of(tc).add(0.5, 0.5).mul(tilesz).dist(cc) <= r)
		dst.accept(tc);
	}
    }

    public static void parse(Gob gob, Message dat) {
	try {
        double rad = (dat.float16() * 11) - ε;
        gob.setattr(new Radius(gob, rad, true));
        gob.msRadSize = (float) rad;
	} catch(NoClassDefFoundError e) {
	}
    }
}

/* Only used for placement info */
/* >spr: BuildOl */
