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

import haven.res.ui.tt.wear.Wear;
import haven.res.ui.tt.armor.Armor;

import java.awt.*;
import java.util.*;
import static haven.Inventory.invsq;

public class Equipory extends Widget implements DTarget {
    private static final Resource.Image bgi = Resource.loadrimg("gfx/hud/equip/bg");
    private static final int yo = Inventory.sqsz.y, sh = 10;
    private static final Tex bg = new TexI(PUtils.uiscale(bgi.img, Coord.of((sh * yo * bgi.sz.x) / bgi.sz.y, sh * yo)));
    private static final int rx = invsq.sz().x + bg.sz().x;
    public static final Coord bgc = new Coord(invsq.sz().x, 0);
    public static final Coord ecoords[] = {
	new Coord( 0, 0 * yo),
	new Coord( 0, 1 * yo),
	new Coord( 0, 2 * yo),
	new Coord(rx, 2 * yo),
	new Coord( 0, 3 * yo),
	new Coord(rx, 3 * yo),
	new Coord( 0, 4 * yo),
	new Coord(rx, 4 * yo),
	new Coord( 0, 5 * yo),
	new Coord(rx, 5 * yo),
	new Coord( 0, 6 * yo),
	new Coord(rx, 6 * yo),
	new Coord( 0, 8 * yo),
	new Coord(rx, 8 * yo),
	new Coord( 0, 9 * yo),
	new Coord(rx, 9 * yo),
	new Coord(invsq.sz().x, 0 * yo),
	new Coord(rx, 0 * yo),
	new Coord(rx, 1 * yo),
	new Coord( 0, 7 * yo),
	new Coord(rx, 7 * yo),
    };
    public static final Tex[] ebgs = new Tex[ecoords.length];
    public static final Text[] etts = new Text[ecoords.length];
    static Coord isz;
    static {
	isz = new Coord();
	for(Coord ec : ecoords) {
	    if(ec.x + invsq.sz().x > isz.x)
		isz.x = ec.x + invsq.sz().x;
	    if(ec.y + invsq.sz().y > isz.y)
		isz.y = ec.y + invsq.sz().y;
	}
	for(int i = 0; i < ebgs.length; i++) {
	    Resource bgres = Resource.local().loadwait("gfx/hud/equip/ep" + i);
	    Resource.Image img = bgres.layer(Resource.imgc);
	    if(img != null) {
		ebgs[i] = img.tex();
		etts[i] = Text.render(bgres.flayer(Resource.tooltip).t);
	    }
	}
    }
    Map<GItem, Collection<WItem>> wmap = new HashMap<>();
    private final Avaview ava;
	public WItem[] slots = new WItem[ecoords.length];
	private static final Text.Foundry acf = new Text.Foundry(Text.sans, 12);
	public boolean updateBottomText = false;
	long delayedUpdateTime;
	private Tex Detection = null;
	private Tex Subtlety = null;
	private Tex ArmorClass = null;
	AttrBonusesWdg bonuses;
	private boolean showEquipmentBonuses = Utils.getprefb("showEquipmentBonuses", false);
	private Button expandButton = null;
	public boolean myOwnEquipory = false;
	public static CheckBox autoDropLeechesCheckBox;
	public static CheckBox autoDropTicksCheckBox;
	public static CheckBox autoEquipBunnySlippersPlateBootsCheckBox;
	boolean checkForLeeches = false;
	boolean checkForTicks = false;
	GItem lweap,rweap;

    @RName("epry")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    long gobid;
	    if(args.length < 1)
		gobid = -2;
	    else if(args[0] == null)
		gobid = -1;
	    else
		gobid = Utils.uiv(args[0]);
	    return(new Equipory(gobid));
	}
    }

    protected void added() {
	if(ava.avagob == -2)
	    ava.avagob = getparent(GameUI.class).plid;
	super.added();
    }

    public Equipory(long gobid) {
	super(isz);
	if (gobid == GameUI.playerId)
		myOwnEquipory = true;
	ava = add(new Avaview(bg.sz(), gobid, "equcam") {
		public boolean mousedown(MouseDownEvent ev) {
		    return(false);
		}

		public void draw(GOut g) {
		    g.image(bg, Coord.z);
		    super.draw(g);
		}

		{
		    basic.add(new Outlines(false));
		}

		final FColor cc = new FColor(0, 0, 0, 0);
		protected FColor clearcolor() {return(cc);}
	    }, bgc);
	ava.drawv = false;
	bonuses = add(new AttrBonusesWdg(isz.y), isz.x + UI.scale(20), 0);
	bonuses.show(showEquipmentBonuses);
	add(expandButton = new Button(UI.scale(24), showEquipmentBonuses ? "←" : "→", false).action(this::expandAttributes), isz.x + UI.scale(10), 0);
	if (myOwnEquipory){
		Widget prev;
		prev = add(autoDropLeechesCheckBox = new CheckBox("Auto-Drop Leeches"){
			{a = Utils.getprefb("autoDropLeeches", true);}
			public void set(boolean val) {
				if (OptWnd.autoDropLeechesCheckBox != null)
					OptWnd.autoDropLeechesCheckBox.set(val);
				a = val;
			}
		}, UI.scale(10), isz.y + UI.scale(12));
		prev = add(autoEquipBunnySlippersPlateBootsCheckBox = new CheckBox("Auto-Equip Bunny Slippers/Plate Boots"){
			{a = OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.a;}
			public void set(boolean val) {
				if (OptWnd.autoEquipBunnySlippersPlateBootsCheckBox != null)
					OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.set(val);
				a = val;
			}
		}, prev.pos("ur").adds(10, 0));
		autoEquipBunnySlippersPlateBootsCheckBox.tooltip = OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.tooltip;
		prev = add(autoDropTicksCheckBox = new CheckBox("Auto-Drop Ticks"){
			{a = Utils.getprefb("autoDropTicks", true);}
			public void set(boolean val) {
				if (OptWnd.autoDropTicksCheckBox != null)
					OptWnd.autoDropTicksCheckBox.set(val);
				a = val;
			}
		}, prev.pos("bl").adds(0, 2).x(UI.scale(10)));
	}
	pack();
    }

	public void expandAttributes(){
		showEquipmentBonuses = !showEquipmentBonuses;
		bonuses.show(showEquipmentBonuses);
		Utils.setprefb("showEquipmentBonuses", showEquipmentBonuses);
		expandButton.change(showEquipmentBonuses ? "←" : "→");
		// ND: I don't know why, but I need to pack both for this to work, lol
		this.pack();
		parent.pack();
	}

    public static interface SlotInfo {
	public int slots();
    }

    public void addchild(Widget child, Object... args) {
	if(child instanceof GItem) {
	    add(child);
	    GItem g = (GItem)child;
	    ArrayList<WItem> v = new ArrayList<>();
	    for(int i = 0; i < args.length; i++) {
		int ep = Utils.iv(args[i]);
		switch(ep)
		{
			case 6:

			lweap = g;

			break;
			case 7:

			rweap = g;
			
			break;
		}
		if(ep < ecoords.length)
			 v.add(slots[ep] = add(new WItem(g), ecoords[ep].add(1, 1)));
	    }
		g.sendttupdate = true;
	    v.trimToSize();
	    wmap.put(g, v);
		updateBottomText = true;
		delayedUpdateTime = System.currentTimeMillis();
		checkForLeeches = true;
		checkForTicks = true;
		try {
			if (g.resource() != null && g.resource().name.equals("gfx/invobjs/batcape")) {
				Gob.batWingCapeEquipped = true;
				ui.sess.glob.oc.gobAction(Gob::updateBeastDangerRadii);
			}
		} catch (Exception ignored){}
	} else {
	    super.addchild(child, args);
	}
    }

    public void cdestroy(Widget w) {
	super.cdestroy(w);
	if(w instanceof GItem) {
	    GItem i = (GItem)w;
	    for(WItem v : wmap.remove(i)) {
		ui.destroy(v);
		for (int s = 0; s < slots.length; ++s) {
			if (slots[s] == v)
				slots[s] = null;
		}
		}
	bonuses.update(slots);
	updateBottomText = true;
	delayedUpdateTime = System.currentTimeMillis();
	checkForLeeches = true;
	checkForTicks = true;
		try {
			if (i.resource() != null && i.resource().name.equals("gfx/invobjs/batcape")) {
				Gob.batWingCapeEquipped = false;
				ui.sess.glob.oc.gobAction(Gob::updateBeastDangerRadii);
			}
		} catch (Exception ignored){}
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "pop") {
	    ava.avadesc = Composited.Desc.decode(ui.sess, args);
	} else {
	    super.uimsg(msg, args);
	}
    }

    public int epat(Coord c) {
	for(int i = 0; i < ecoords.length; i++) {
	    if(c.isect(ecoords[i], invsq.sz()))
		return(i);
	}
	return(-1);
    }

    public boolean drop(Coord cc, Coord ul) {
	wdgmsg("drop", epat(cc));
	return(true);
    }

    public void drawslots(GOut g) {
	int slots = 0;
	GameUI gui = getparent(GameUI.class);
	if((gui != null) && (gui.vhand != null)) {
	    try {
		SlotInfo si = ItemInfo.find(SlotInfo.class, gui.vhand.item.info());
		if(si != null)
		    slots = si.slots();
	    } catch(Loading l) {
	    }
	}
	for(int i = 0; i < ecoords.length; i++) {
	    if((slots & (1 << i)) != 0) {
		g.chcolor(0, 200, 0, 96);
		g.frect(ecoords[i].add(1, 1), invsq.sz().sub(2, 2));
		g.chcolor();
	    }
	    g.image(invsq, ecoords[i]);
	    if(ebgs[i] != null)
		g.image(ebgs[i], ecoords[i]);
	}
    }

    public Object tooltip(Coord c, Widget prev) {
	int sl = epat(c);
	if(sl >= 0)
	    return(etts[sl]);
	return(null);
    }

    public void draw(GOut g) {
	drawslots(g);
	super.draw(g);
		GameUI gui = ui.gui;
		if (updateBottomText) {
			long now = System.currentTimeMillis();
			if ((now - delayedUpdateTime) > 200){ // ND: 100ms was not enough, bumped to 200ms
				// ND: I genuinely don't know any other workaround to this crap not updating when you add a new item. For some reason this doesn't happen in Ardennes' (old render)
				//     In Ardennes', it looks like the UI freezes for a second when you try to add the new item sometimes. Maybe these weird hiccups are different in new render? For now, I have no clue.
				int prc = 0, exp = 0, det, intl = 0, ste = 0, snk, aHard = 0, aSoft = 0;
				CharWnd chrwdg = null;
				try {
					chrwdg = gui.chrwdg;
					for (BAttrWnd.Attr attr : chrwdg.battr.attrs) {
						if (attr.attr.nm.contains("prc"))
							prc = attr.attr.comp;
						if (attr.attr.nm.contains("int"))
							intl = attr.attr.comp;
					}
					for (SAttrWnd.SAttr attr : chrwdg.sattr.attrs) {
						if (attr.attr.nm.contains("exp"))
							exp = attr.attr.comp;
						if (attr.attr.nm.contains("ste"))
							ste = attr.attr.comp;
					}
					for (int i = 0; i < slots.length; i++) {
						WItem itm = slots[i];
						boolean isBroken = false;
						if (itm != null) {
							for (ItemInfo info : itm.item.info()) {
								if (info instanceof Wear) {
									if (((Wear) info).m-((Wear) info).d == 0)
										isBroken = true;
									break;
								}
							}

							for (ItemInfo info : itm.item.info()) {
								if (info instanceof Armor) {
									if (!isBroken){
										aHard += ((Armor) info).hard;
										aSoft += ((Armor) info).soft;
									}
									break;
								}
							}
						}
					}
					det = prc * exp;
					snk = intl * ste;
					String DetectionString = String.format("%,d", det).replace(',', '.');
					String SubtletyString = String.format("%,d", snk).replace(',', '.');
					if (myOwnEquipory) {
						Detection = PUtils.strokeTex(Text.renderstroked2("Detection (Prc*Exp):  " + DetectionString, Color.WHITE, Color.BLACK, acf));
						Subtlety = PUtils.strokeTex(Text.renderstroked2("Subtlety (Int*Ste):  " + SubtletyString, Color.WHITE, Color.BLACK, acf));
					}
					ArmorClass = PUtils.strokeTex(Text.renderstroked2("Armor Class:  " + (aHard + aSoft) + " (" + aHard + " + " + aSoft + ")", Color.WHITE, Color.BLACK, acf));
					updateBottomText = false;
				} catch (Exception e) {
				}
			}
		}
		if (Detection != null)
			g.image(Detection, new Coord(( invsq.sz().x + bg.sz().x / 2 ) - Detection.sz().x / 2, bg.sz().y - UI.scale(56)));
		if (Subtlety != null)
			g.image(Subtlety, new Coord(( invsq.sz().x + bg.sz().x / 2 ) - Subtlety.sz().x / 2, bg.sz().y - UI.scale(40)));
		if (ArmorClass != null)
			g.image(ArmorClass, new Coord(( invsq.sz().x + bg.sz().x / 2 ) - ArmorClass.sz().x / 2, bg.sz().y - UI.scale(20)));
    }

    public boolean iteminteract(Coord cc, Coord ul) {
	return(false);
    }

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (sender instanceof GItem && wmap.containsKey(sender) && msg.equals("ttupdate")) {
			bonuses.update(slots);
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	public void tick(double dt) {
		super.tick(dt);
		if (OptWnd.autoDropLeechesCheckBox.a && myOwnEquipory && checkForLeeches) {
			long now = System.currentTimeMillis();
			if ((now - delayedUpdateTime) > 300){
				for (WItem equippedItem : slots) {
					if (equippedItem != null && equippedItem.item != null && equippedItem.item.getname() != null && equippedItem.item.getname().contains("Leech")){
						equippedItem.item.wdgmsg("drop", new Coord(equippedItem.sz.x / 2, equippedItem.sz.y / 2));
					}
				}
				checkForLeeches = false;
			}
		}
		if (OptWnd.autoDropTicksCheckBox.a && myOwnEquipory && checkForTicks) {
			long now = System.currentTimeMillis();
			if ((now - delayedUpdateTime) > 300){
				for (WItem equippedItem : slots) {
					if (equippedItem != null && equippedItem.item != null && equippedItem.item.getname() != null && equippedItem.item.getname().contains("Tick")){
						equippedItem.item.wdgmsg("drop", new Coord(equippedItem.sz.x / 2, equippedItem.sz.y / 2));
					}
				}
				checkForTicks = false;
			}
		}
		
	}
	public GItem getWeapon() {

        if (lweap != null && ItemInfo.hasInfo(lweap.info,"Damage")) {
            return lweap;
        } else if (rweap != null && ItemInfo.hasInfo(rweap.info,"Damage")) {
            return rweap;
        } else {
            return null;
        }
    }

}
