/* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.ModSprite.*;
import java.util.*;
import java.util.function.Consumer;

@haven.FromResource(name = "lib/vmat", version = 39)
public class VarWrap extends Pipe.Op.Wrapping {
    public final int mid;

    public VarWrap(RenderTree.Node r, Pipe.Op st, int mid) {
	super(r, st, true);
	this.mid = mid;
    }

    public String toString() {
	return(String.format("#<vmat %s %s>", mid, op));
    }

    public static class Applier implements NodeWrap {
	public final Material mat;
	public final int mid;

	public Applier(Material mat, int mid) {
	    this.mat = mat;
	    this.mid = mid;
	}

	public VarWrap apply(RenderTree.Node node) {
	    return(new VarWrap(node, mat, mid));
	}
    }
}
