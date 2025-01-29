/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.res.ui.stackinv.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class Inventory extends Widget implements DTarget {
    public static final Coord sqsz = UI.scale(new Coord(32, 32)).add(1, 1);
    public static final Tex invsq = Resource.loadtex("gfx/hud/invsq");
    public boolean dropul = true;
    public Coord isz;
    public boolean[] sqmask = null;
    public Map<GItem, WItem> wmap = new HashMap<GItem, WItem>();
	public static Set<String> PLAYER_INVENTORY_NAMES = new HashSet<>(Arrays.asList("Inventory", "Belt", "Equipment", "Character Sheet", "Study"));

	public static final Comparator<WItem> ITEM_COMPARATOR_ASC = new Comparator<WItem>() {
		@Override
		public int compare(WItem o1, WItem o2) {

			double q1 = o1.item.getQBuff() != null ? o1.item.getQBuff().q : 0;
			double q2 = o2.item.getQBuff() != null ? o2.item.getQBuff().q : 0;

			return Double.compare(q1, q2);
		}
	};
	public static final Comparator<WItem> ITEM_COMPARATOR_DESC = new Comparator<WItem>() {
		@Override
		public int compare(WItem o1, WItem o2) {
			return ITEM_COMPARATOR_ASC.compare(o2, o1);
		}
	};

	// ND: WHY is this happening when there's literally a texture resource for this?
	// ND: This affects the menugrid slots color, I'm basically replacing it with the inventory square texture
//    static {
//	Coord sz = sqsz.add(1, 1);
//	WritableRaster buf = PUtils.imgraster(sz);
//	for(int i = 1, y = sz.y - 1; i < sz.x - 1; i++) {
//	    buf.setSample(i, 0, 0, 20); buf.setSample(i, 0, 1, 28); buf.setSample(i, 0, 2, 21); buf.setSample(i, 0, 3, 167);
//	    buf.setSample(i, y, 0, 20); buf.setSample(i, y, 1, 28); buf.setSample(i, y, 2, 21); buf.setSample(i, y, 3, 167);
//	}
//	for(int i = 1, x = sz.x - 1; i < sz.y - 1; i++) {
//	    buf.setSample(0, i, 0, 20); buf.setSample(0, i, 1, 28); buf.setSample(0, i, 2, 21); buf.setSample(0, i, 3, 167);
//	    buf.setSample(x, i, 0, 20); buf.setSample(x, i, 1, 28); buf.setSample(x, i, 2, 21); buf.setSample(x, i, 3, 167);
//	}
//	for(int y = 1; y < sz.y - 1; y++) {
//	    for(int x = 1; x < sz.x - 1; x++) {
//		buf.setSample(x, y, 0, 36); buf.setSample(x, y, 1, 52); buf.setSample(x, y, 2, 38); buf.setSample(x, y, 3, 125);
//	    }
//	}
//	invsq = new TexI(PUtils.rasterimg(buf));
//    }

    @RName("inv")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Inventory((Coord)args[0]));
	}
    }

    public void draw(GOut g) {
	Coord c = new Coord();
	int mo = 0;
	for(c.y = 0; c.y < isz.y; c.y++) {
	    for(c.x = 0; c.x < isz.x; c.x++) {
		if((sqmask != null) && sqmask[mo++]) {
		    g.chcolor(64, 64, 64, 255);
		    g.image(invsq, c.mul(sqsz));
		    g.chcolor();
		} else {
		    g.image(invsq, c.mul(sqsz));
		}
	    }
	}
	super.draw(g);
    }
	
    public Inventory(Coord sz) {
	super(sqsz.mul(sz).add(1, 1));
	isz = sz;
    }
    
    public boolean mousewheel(MouseWheelEvent ev) {
	if(ui.modshift) {
	    Inventory minv = getparent(GameUI.class).maininv;
	    if(minv != this) {
		if(ev.a < 0)
		    wdgmsg("invxf", minv.wdgid(), 1);
		else if(ev.a > 0)
		    minv.wdgmsg("invxf", this.wdgid(), 1);
	    }
	}
	return(true);
    }
    
    public void addchild(Widget child, Object... args) {
	add(child);
	Coord c = (Coord)args[0];
	if(child instanceof GItem) {
	    GItem i = (GItem)child;
	    wmap.put(i, add(new WItem(i), c.mul(sqsz).add(1, 1)));
	}
    }
    
    public void cdestroy(Widget w) {
	super.cdestroy(w);
	if(w instanceof GItem) {
	    GItem i = (GItem)w;
	    ui.destroy(wmap.remove(i));
	}
    }
    
    public boolean drop(Coord cc, Coord ul) {
	Coord dc;
	if(dropul)
	    dc = ul.add(sqsz.div(2)).div(sqsz);
	else
	    dc = cc.div(sqsz);
	wdgmsg("drop", dc);
	return(true);
    }
	
    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }
	
    public void uimsg(String msg, Object... args) {
	if(msg == "sz") {
	    isz = (Coord)args[0];
	    resize(invsq.sz().add(UI.scale(new Coord(-1, -1))).mul(isz).add(UI.scale(new Coord(1, 1))));
	    sqmask = null;
	} else if(msg == "mask") {
	    boolean[] nmask;
	    if(args[0] == null) {
		nmask = null;
	    } else {
		nmask = new boolean[isz.x * isz.y];
		byte[] raw = (byte[])args[0];
		for(int i = 0; i < isz.x * isz.y; i++)
		    nmask[i] = (raw[i >> 3] & (1 << (i & 7))) != 0;
	    }
	    this.sqmask = nmask;
	} else if(msg == "mode") {
	    dropul = !Utils.bv(args[0]);
	} else {
	    super.uimsg(msg, args);
	}
    }

	public List<WItem> getAllItems() {
		List<WItem> items = new ArrayList<WItem>();
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				items.add((WItem) wdg);
			}
		}
		return items;
	}

	public List<WItem> getItemsExact(String... names) {
		List<WItem> items = new ArrayList<WItem>();
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				String wdgname = ((WItem)wdg).item.getname();
				for (String name : names) {
					if (wdgname.equals(name)) {
						items.add((WItem) wdg);
						break;
					}
				}
			}
		}
		return items;
	}

	public WItem getItemPrecise(String name) {
		if (name == null)
			return null;
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				String wdgname = ((WItem)wdg).item.getname();
				if (wdgname.equals(name))
					return (WItem) wdg;
			}
		}
		return null;
	}

	public WItem getItemPartial(String name) {
		if (name == null)
			return null;
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				String wdgname = ((WItem)wdg).item.getname();
				if (wdgname.contains(name))
					return (WItem) wdg;
			}
		}
		return null;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if(msg.equals("transfer-ordered")){
			processTransfer(getSame((GItem) args[0], (Boolean) args[1]));
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	private static boolean isInPlayerInventory(WItem item) {
		Window window = item.getparent(Window.class);
		return window != null && Objects.equals("Inventory", window.cap);
	}

	private static List<Integer> getExternalInventoryIds(UI ui) {
		List<Inventory> inventories = ui.gui.getAllWindows()
				.stream()
				.flatMap(w -> w.children().stream())
				.filter(child -> child instanceof Inventory)
				.map(i -> (Inventory) i)
				.collect(Collectors.toList());

		List<Integer> externalInventoryIds = inventories
				.stream()
				.filter(i -> {
					Window window = i.getparent(Window.class);
					return window != null && !PLAYER_INVENTORY_NAMES.contains(window.cap);
				}).map(i -> i.wdgid())
				.collect(Collectors.toList());

		List<Integer> stockpileIds = ui.gui.getAllWindows()
				.stream()
				.map(i -> i.getchild(ISBox.class))
				.filter(Objects::nonNull)
				.map(Widget::wdgid)
				.collect(Collectors.toList());

		externalInventoryIds.addAll(stockpileIds);
		return externalInventoryIds;
	}

	private static void attemptTransferSplittingStack(List<Integer> externalInventoryIds, ItemStack stack) {
		for (Integer externalInventoryId : externalInventoryIds) {
			Object[] invxf2Args = new Object[3];
			invxf2Args[0] = 0;
			invxf2Args[1] = stack.order.size();
			invxf2Args[2] = externalInventoryId;

			stack.order.get(0).wdgmsg("invxf2", invxf2Args);
		}
	}

	private void processTransfer(List<WItem> items) {
		List<Integer> externalInventoryIds = getExternalInventoryIds(ui);
		for (WItem item : items){
			item.item.wdgmsg("transfer", Coord.z);

			Widget contents = item.item.contents;
			if (contents instanceof ItemStack && isInPlayerInventory(item)) {
				attemptTransferSplittingStack(externalInventoryIds, (ItemStack) contents);
			}
		}
	}

	private List<WItem> getSame(GItem item, Boolean ascending) {
		List<WItem> items = new ArrayList<>();
		try {
			String name = item.res.get().name;
			GSprite spr = item.spr();
			for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
				if(wdg.visible && wdg instanceof WItem) {
					WItem wItem = (WItem) wdg;
					GItem child = wItem.item;
					try {
						if(child.res.get().name.equals(name) && ((spr == child.spr()) || (spr != null && spr.same(child.spr())))) {
							items.add(wItem);
						}
					} catch (Loading e) {}
				}
			}
			Collections.sort(items, ascending ? ITEM_COMPARATOR_ASC : ITEM_COMPARATOR_DESC);
		} catch (Loading e) { }
		return items;
	}

	public Coord isRoom(int x, int y) {
		//check if there is a space for an x times y item, return coordinate where.
		Coord freespot = null;
		boolean[][] occumap = new boolean[isz.x][isz.y];
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				for (int i = 0; i < wdg.sz.x; i++) {
					for (int j = 0; j < wdg.sz.y; j++) {
						occumap[(wdg.c.x/sqsz.x+i/sqsz.x)][(wdg.c.y/sqsz.y+j/sqsz.y)] = true;
					}
				}
			}
		}
		//(NICE LOOPS)
		//Iterate through all spots in inventory
		superloop:
		for (int i = 0; i < isz.x; i++) {
			for (int j = 0; j < isz.y; j++) {
				boolean itsclear = true;
				//Check if there is X times Y free slots
				try {
					for (int k = 0; k < x; k++) {
						for (int l = 0; l < y; l++) {
							if (occumap[i+k][j+l] == true) {
								itsclear = false;
							}
						}
					}
				} catch (IndexOutOfBoundsException e) {
					itsclear = false;
				}

				if (itsclear) {
					freespot = new Coord(i,j);
					break superloop;
				}
			}
		}

		return freespot;
	}

	public int getFreeSpace() {
		int feespace = isz.x * isz.y;
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem)
				feespace -= (wdg.sz.x * wdg.sz.y) / (sqsz.x * sqsz.y);
		}
		return feespace;
	}

	public List<WItem> getItemsPartial(String... names) {
		List<WItem> items = new ArrayList<WItem>();
		for (Widget wdg = child; wdg != null; wdg = wdg.next) {
			if (wdg instanceof WItem) {
				String wdgname = ((WItem)wdg).item.getname();
				for (String name : names) {
					if (name == null)
						continue;
					if (wdgname.contains(name)) {
						items.add((WItem) wdg);
						break;
					}
				}
			}
		}
		return items;
	}
}
