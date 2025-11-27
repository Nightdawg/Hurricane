/* Preprocessed source code */
package haven.res.ui.stackinv;

import haven.*;
import haven.res.ui.tt.q.quality.Quality;

import java.util.*;
import static haven.Inventory.*;

/* >wdg: ItemStack */
@haven.FromResource(name = "ui/stackinv", version = 2)
public class ItemStack extends Widget implements DTarget {
    public final List<GItem> order = new ArrayList<>();
    public final Map<GItem, WItem> wmap = new HashMap<>();
    private boolean dirty;
	public boolean stackQualityNeedsUpdate = false;
	long delayedUpdateTime;

    public static ItemStack mkwidget(UI ui, Object[] args) {
	return(new ItemStack());
    }

    public void tick(double dt) {
	super.tick(dt);
	if(dirty) {
	    int x = 0, y = 0;
	    for(GItem item : order) {
		WItem w = wmap.get(item);
		w.move(Coord.of(x, 0));
		x += w.sz.x;
		y = Math.max(y, w.sz.y);
	    }
	    resize(x, y);
	    dirty = false;
	}
		updateQuality();
    }

    public void addchild(Widget child, Object... args) {
	add(child);
	if(child instanceof GItem) {
	    GItem i = (GItem)child;
	    wmap.put(i, add(new WItem(i)));
	    order.add(i);
	    dirty = true;
		stackQualityNeedsUpdate = true;
		delayedUpdateTime = System.currentTimeMillis();
	}
    }

    public void cdestroy(Widget w) {
	super.cdestroy(w);
	if(w instanceof GItem) {
	    GItem i = (GItem)w;
	    wmap.remove(i).reqdestroy();
	    order.remove(i);
	    dirty = true;
		stackQualityNeedsUpdate = true;
		delayedUpdateTime = System.currentTimeMillis();
	}
    }

    public void cresize(Widget ch) {
	dirty = true;
    }

    public boolean mousewheel(MouseWheelEvent ev) {
	if(ui.modshift) {
	    Inventory minv = getparent(GameUI.class).maininv;
	    if(ev.a < 0)
		wdgmsg("invxf", minv.wdgid(), 1);
	    else if(ev.a > 0)
		minv.wdgmsg("invxf", this.wdgid(), 1);
	}
	return(true);
    }
    
    public boolean drop(Coord cc, Coord ul) {
	wdgmsg("drop");
	return(true);
    }

    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }

	private void updateQuality() {
		if (stackQualityNeedsUpdate) {
			long now = System.currentTimeMillis();
			if ((now - delayedUpdateTime) > 200) { // ND: 200 should be enough, maybe even too much.
				try {
					GItem stackItem = ((GItem.ContentsWindow) this.parent).cont;
					List<ItemInfo> info = stackItem.info();
					if (info != null) {
						List<WItem> ret = new ArrayList<>(wmap.values());
						if (!ret.isEmpty()) {
							int amount = 0;
							double sum = 0;
							for (WItem w : ret) {
								Quality q = ItemInfo.find(Quality.class, w.item.info());
								if (q != null) {
									amount++;
									sum += q.q;
								}
							}
							if (amount > 0) {
								Quality q = ItemInfo.find(Quality.class, info);
								if (q == null) {
									info.add(q = new Quality(stackItem, sum / amount));
									stackItem.stackQualityTex = q.overlay();
									stackQualityNeedsUpdate = false;
								} else {
									q.q = sum / amount;
									stackItem.stackQualityTex = q.overlay();
									stackQualityNeedsUpdate = false;
								}
							}
						}
					}
				} catch (Exception e) {
					stackQualityNeedsUpdate = true;
				}
			}
		}
	}
}
