/* Preprocessed source code */
/* $use: gfx/fx/mscover */

package haven.res.gfx.fx.msrad;

import java.util.function.*;
import haven.*;
import haven.res.gfx.fx.mscover.*;
import static haven.MCache.*;

/* >objdelta: Radius */
@haven.FromResource(name = "gfx/fx/msradius", version = 1)
public class BuildOl extends Sprite {
    public BuildOl(Owner owner, Resource res, Message sdt) {
	super(owner, res);
	double r = (sdt.float16() * 11) - Radius.ε;
	Gob gob = owner.context(Gob.class);
	try {
	    gob.setattr(new Radius(gob, r, false));
	} catch(NoClassDefFoundError e) {
	}
    }
}
