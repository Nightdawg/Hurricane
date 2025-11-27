/* Preprocessed source code */
package haven.res.ui.tt.wpn.info;

import haven.*;
import java.util.*;
import java.awt.image.BufferedImage;

@haven.FromResource(name = "ui/tt/wpn/info", version = 5)
public abstract class WeaponInfo extends ItemInfo.Tip {
    public WeaponInfo(Owner owner) {
	super(owner);
    }

    public static class Subtip extends Tip {
	final List<WeaponInfo> ls = new ArrayList<>();

	Subtip(Owner owner) {super(owner);}

	public void layout(Layout l) {
	    Collections.sort(ls, Comparator.comparing(WeaponInfo::order));
	    CompImage img = new CompImage();
	    for(WeaponInfo inf : ls)
		inf.add(img);
	    l.cmp.add(img, new Coord(UI.scale(2), l.cmp.sz.y + UI.scale(4)));
	}
    }

    public static final Layout.TipID<Subtip> sid = Subtip::new;

    public void add(CompImage img) {
	img.add(CompImage.mk(wpntip()), new Coord(0, img.sz.y));
    }

    public BufferedImage wpntip() {
	return(RichText.render(wpntips(), 0).img);
    }

    public String wpntips() {
	throw(new UnsupportedOperationException());
    }

    public void prepare(Layout l) {
	l.intern(sid).ls.add(this);
    }
}
