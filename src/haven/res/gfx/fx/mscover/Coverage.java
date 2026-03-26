/* Preprocessed source code */
/* $use: ui/pag/toggle */

package haven.res.gfx.fx.mscover;

import haven.*;
import haven.render.*;
import java.util.*;
import java.util.function.*;
import haven.res.ui.pag.toggle.*;
import haven.MenuGrid.Pagina;
import static haven.MCache.*;

@haven.FromResource(name = "gfx/fx/mscover", version = 2)
public abstract class Coverage extends GAttrib {
    public final Global gl;
    public final boolean real;
    public Coord2d cc;
    public double a;
    public int cfl = 0, chseq = 1, useq = 0;
    public boolean removed = false;

    public Coverage(Gob owner, boolean real) {
	super(owner);
	this.gl = Global.get(gob.glob);
	this.real = real;
    }

    public int fl() {
	GobHealth h = gob.getattr(GobHealth.class);
	return((real ? 0 : 1) | (((h != null) && (h.hp < 0.9)) ? 2 : 0));
    }

    public void dispose() {
	super.dispose();
	removed = true;
    }

    public void ctick(double dt) {
	if(!Utils.eq(gob.rc, cc) || (gob.a != a) || (fl() != cfl))
	    gl.update = true;
    }

    public abstract Area extent(Coord2d cc, double a);
    public abstract void cover(Coord2d cc, double a, Area clip, Consumer<Coord> dst);
}
