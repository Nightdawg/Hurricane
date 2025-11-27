/* Preprocessed source code */
/* $use: ui/tt/wpn/info */
import haven.*;
import haven.res.ui.tt.wpn.info.*;
import static haven.PUtils.*;
import java.awt.image.BufferedImage;

/* >tt: Weight */
@haven.FromResource(name = "ui/tt/wpn/atkw", version = 6)
public class Weight extends WeaponInfo {
    public final Resource attr;

    public Weight(Owner owner, Resource attr) {
	super(owner);
	this.attr = attr;
    }

    public static Weight mkinfo(Owner owner, Object... args) {
	return(new Weight(owner, owner.context(Resource.Resolver.class).getres((Integer)args[1]).get()));
    }

    public BufferedImage wpntip() {
	BufferedImage line = Text.render("Attack weight: ").img;
	BufferedImage icon = convolvedown(attr.layer(Resource.imgc).img, new Coord(line.getHeight(), line.getHeight()), CharWnd.iconfilter);
	return(catimgsh(0, line, icon));
    }

    public int order() {return(75);}
}
