/* Preprocessed source code */
/* $use: ui/obj/buddy */

package haven.res.gfx.hud.mmap.plo;

import haven.*;
import haven.res.ui.obj.buddy.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.*;

@haven.FromResource(name = "gfx/hud/mmap/plo", version = 12)
public class DeadPlayer extends GobIcon.ImageIcon {
    public static final Resource img = Resource.local().loadwait("customclient/mapicons/playerDead");
    public static final Object[] id = new Object[] {"dead"};

    public DeadPlayer(OwnerContext owner, Resource res) {
	super(owner, res, GobIcon.Image.get(img));
    }

    public BufferedImage image() {
	return(img.flayer(Resource.imgc).img);
    }

    public Object[] id() {
	return(id);
    }

    public String name() {
	return("Player, dead");
    }
}

/* >mapicon: Factory */
