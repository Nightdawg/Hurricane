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
public class Data {
    public final Area area;
    public final short[] cc, vc, dc;

    public Data(Area area) {
	this.area = area;
	cc = new short[area.rsz()];
	vc = new short[area.rsz()];
	dc = new short[area.rsz()];
    }

    public void mod(Coord2d cc, double a, Coverage cov, short[] buf, int m) {
	cov.cover(cc, a, area, tc -> buf[area.ridx(tc)] += m);
    }

    public void mod(Coord2d oc, double a, Coverage cov, int fl, int m) {
	mod(oc, a, cov, cc, m);
	if((fl & 1) != 0)
	    mod(oc,a , cov, vc, m);
	if((fl & 2) != 0)
	    mod(oc, a, cov, dc, m);
    }
}
