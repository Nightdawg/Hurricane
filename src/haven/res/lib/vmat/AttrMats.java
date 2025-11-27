/* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.ModSprite.*;
import java.util.*;
import java.util.function.Consumer;

@haven.FromResource(name = "lib/vmat", version = 39)
public class AttrMats extends VarMats {
    public final Map<Integer, Material> mats;

    public AttrMats(Gob gob, Map<Integer, Material> mats) {
	super(gob);
	if (gob != null && gob.getres() != null) {
	Resource res = gob.getres();
	if ((OptWnd.disableHerbalistTablesVarMatsCheckBox.a && res.name.equals("gfx/terobjs/htable"))
			|| (OptWnd.disableCupboardsVarMatsCheckBox.a && res.name.equals("gfx/terobjs/cupboard"))
			|| (OptWnd.disableChestsVarMatsCheckBox.a && (res.name.equals("gfx/terobjs/chest") || res.name.equals("gfx/terobjs/stonecasket")))
			|| (OptWnd.disableMetalCabinetsVarMatsCheckBox.a && res.name.equals("gfx/terobjs/metalcabinet"))
			|| (OptWnd.disableTrellisesVarMatsCheckBox.a && res.name.equals("gfx/terobjs/plants/trellis"))
			|| (OptWnd.disableSmokeShedsVarMatsCheckBox.a && res.name.equals("gfx/terobjs/smokeshed"))
			|| (OptWnd.disableAllObjectsVarMatsCheckBox.a)) {
		this.mats = new IntMap<Material>();
	} else
		this.mats = mats;
	} else
	this.mats = mats;
    }

    public Material varmat(int id) {
	return(mats.get(id));
    }

    public static Map<Integer, Material> decode(Resource.Resolver rr, Message sdt) {
	Map<Integer, Material> ret = new IntMap<Material>();
	int idx = 0;
	while(!sdt.eom()) {
	    Indir<Resource> mres = rr.getres(sdt.uint16());
	    int mid = sdt.int8();
	    Material.Res mat;
	    if(mid >= 0)
		mat = mres.get().layer(Material.Res.class, mid);
	    else
		mat = mres.get().layer(Material.Res.class);
	    ret.put(idx++, mat.get());
	}
	return(ret);
    }

    public static void parse(Gob gob, Message dat) {
	gob.setattr(new AttrMats(gob, decode(gob.context(Resource.Resolver.class), dat)));
    }
}

/* >spr: VarSprite */
