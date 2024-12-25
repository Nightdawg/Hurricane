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

import haven.render.*;
import haven.sprites.CurrentTargetSprite;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.awt.event.KeyEvent;
import java.util.List;

public class Fightsess extends Widget {
    private static final Coord off = new Coord(UI.scale(32), UI.scale(32));
    public static final Tex cdframe = Resource.loadtex("gfx/hud/combat/cool");
    public static final Tex actframe = Buff.frame;
    public static final Coord actframeo = Buff.imgoff;
    public static final Tex indframe = Resource.loadtex("gfx/hud/combat/indframe");
    public static final Coord indframeo = (indframe.sz().sub(off)).div(2);
    public static final Tex indbframe = Resource.loadtex("gfx/hud/combat/indbframe");
    public static final Coord indbframeo = (indframe.sz().sub(off)).div(2);
    public static final Tex useframe = Resource.loadtex("gfx/hud/combat/lastframe");
    public static final Coord useframeo = (useframe.sz().sub(off)).div(2);
    public static final int actpitch = UI.scale(50);
    public final Action[] actions;
    public int use = -1, useb = -1;
    public Coord pcc;
    public int pho;
    private Fightview fv;
	public static final Text.Foundry ipAdditionalFont = new Text.Foundry(Text.dfont.deriveFont(Font.BOLD), 12);
	public static final Text.Foundry openingAdditionalFont = new Text.Foundry(Text.dfont.deriveFont(Font.BOLD), 10);
	public static final Text.Foundry cleaveAdditionalFont = new Text.Foundry(Text.dfont.deriveFont(Font.BOLD), 10);
	public Map<Fightview.Relation, Coord> relations = new HashMap<>();
	int combatMedColorShift = 0;
	public static final Text.Foundry keybindsFoundry = new Text.Foundry(Text.sans.deriveFont(java.awt.Font.BOLD), 14);
	public static final Text.Foundry damageFoundry = new Text.Foundry(Text.sans.deriveFont(java.awt.Font.BOLD), 11);
	private static final Color coinsInfoBG = new Color(0, 0, 0, 120);
	public static final Color ipInfoColorMe = new Color(0, 201, 4);
	public static final Color ipInfoColorEnemy = new Color(245, 0, 0);
	private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif.deriveFont(Font.BOLD), 22, new Color(0, 201, 4)).aa(true), 1, 1, new Color(0, 0, 0));
	private static final Text.Furnace ipfEnemy = new PUtils.BlurFurn(new Text.Foundry(Text.serif.deriveFont(Font.BOLD), 22, new Color(245, 0, 0)).aa(true), 1, 1, new Color(0, 0, 0));
	private final Text.UText<?> ip = new Text.UText<Integer>(ipf) {
		public String text(Integer v) {return("" + v);} // ND: Removed "IP" text. I only need to see the number, we already know it's the IP/Coins
		public Integer value() {return(fv.current.ip);}
	};
	private final Text.UText<?> oip = new Text.UText<Integer>(ipfEnemy) { // Changed this so I can give the enemy IP a different color
		public String text(Integer v) {return("" + v);} // ND: Removed "IP" text. I only need to see the number, we already know it's the IP/Coins
		public Integer value() {return(fv.current.oip);}
	};
	public static final Color stamBarBlue = new Color(47, 58, 207, 200);
	public static final Color hpBarGreen = new Color(0, 166, 10, 255);
	public static final Color hpBarGray = new Color(113, 113, 113, 255);
	public static final Color hpBarRed = new Color(168, 0, 0, 255);
	public static final Color hpBarYellow = new Color(182, 165, 0, 255);
	private static final Color barFrame = new Color(255, 255, 255, 111);

	private static int[] openingArr = new int[] {0,0,0,0};
	private static int wepdmg = 1;
	private static double ql = 1;
	private static int basedmg = 1;
	private static double str = 1;

	Map<String, Color> openingsColorMap = new HashMap<>() {{
		put("paginae/atk/offbalance", new Color(0, 128, 3));
		put("paginae/atk/dizzy", new Color(39, 82, 191));
		put("paginae/atk/reeling", new Color(217, 177, 20));
		put("paginae/atk/cornered", new Color(192, 28, 28));
	}};
	private boolean combatMedAlphaShiftUp = true;

	private static Coord actc(int i) {
		int rl = OptWnd.singleRowCombatMovesCheckBox.a ? 10 : 5;
		return(new Coord((actpitch * (i % rl)) - (((rl - 1) * actpitch) / 2), UI.scale(125) + ((i / rl) * actpitch)));
	}

    public static class Action {
	public final Indir<Resource> res;
	public double cs, ct;

	public Action(Indir<Resource> res) {
	    this.res = res;
	}
    }

    @RName("fsess")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {

		str = ui.sess.glob.getcattr("str").comp;
		GItem wep = ui.gui.getequipory().getWeapon();
		if(wep!=null) {	//This null-check fixed the soft crash when you login already in combat, another way of handling this is doing this in a second or two, maybe with a new Thread()
			setupWepDmg(ui.gui);
		}
		else {
			new Thread(() -> {	//I did the thing, i hope this doesnt break anything. Thanks chatgpt
				try {
					Thread.sleep(10000); //idk maybe the player has a potato pc? its not like seeing what dmg you will do is a high-prio anyways
					setupWepDmg(ui.gui);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}).start();
		}
		
	    int nact = Utils.iv(args[0]);
		if(OptWnd.combatStartSoundEnabledCheckbox.a) {
			try {
				File file = new File("AlarmSounds/" + OptWnd.combatStartSoundFilename.buf.line() + ".wav");
				if(file.exists()) {
					AudioInputStream in = AudioSystem.getAudioInputStream(file);
					AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
					AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
					Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
					((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, OptWnd.combatStartSoundVolumeSlider.val/50.0));
				}
			} catch(Exception ignored) {
			}
		}
	    return(new Fightsess(nact));
	}
    }

    @SuppressWarnings("unchecked")
    public Fightsess(int nact) {
	pho = -UI.scale(40);
	this.actions = new Action[nact];
    }

    protected void added() {
	fv = parent.getparent(GameUI.class).fv;
	presize();
    }

    public void presize() {
	resize(parent.sz);
	pcc = sz.div(2);
    }

    private void updatepos() {
	MapView map;
	Gob pl;
	if(((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null))
	    return;
	Coord3f raw = pl.placed.getc();
	if(raw == null)
	    return;
	pcc = map.screenxf(raw).round2();
	pho = (int)(map.screenxf(raw.add(0, 0, UI.scale(20))).round2().sub(pcc).y) - UI.scale(20);
    }

    private static class Effect implements RenderTree.Node {
	Sprite spr;
	RenderTree.Slot slot;
	boolean used = true;

	Effect(Sprite spr) {this.spr = spr;}

	public void added(RenderTree.Slot slot) {
	    slot.add(spr);
	}
    }

    private static final Resource tgtfx = Resource.local().loadwait("gfx/hud/combat/trgtarw");
    private final Collection<Effect> curfx = new ArrayList<>();

    private Effect fxon(long gobid, Resource fx, Effect cur) {
	MapView map = getparent(GameUI.class).map;
	Gob gob = ui.sess.glob.oc.getgob(gobid);
	if((map == null) || (gob == null))
	    return(null);
	Pipe.Op place;
	try {
	    place = gob.placed.curplace();
	} catch(Loading l) {
	    return(null);
	}
	if((cur == null) || (cur.slot == null)) {
	    try {
		cur = new Effect(Sprite.create(null, fx, Message.nil));
//		cur = new Effect(new CurrentTargetSprite(null));
		cur.slot = map.basic.add(cur.spr, place);
	    } catch(Loading l) {
		return(null);
	    }
	    curfx.add(cur);
	} else {
	    cur.slot.cstate(place);
	}
	cur.used = true;
	return(cur);
    }

    public void tick(double dt) {
	for(Iterator<Effect> i = curfx.iterator(); i.hasNext();) {
	    Effect fx = i.next();
	    if(!fx.used) {
		if(fx.slot != null) {
		    fx.slot.remove();
		    fx.slot = null;
		}
		i.remove();
	    } else {
		fx.used = false;
		fx.spr.tick(dt);
	    }
	}
    }

    public void destroy() {
	for(Effect fx : curfx) {
	    if(fx.slot != null)
		fx.slot.remove();
	}
	curfx.clear();
	super.destroy();
    }

//    private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 18, new Color(128, 128, 255)).aa(true), 1, 1, new Color(48, 48, 96));
//    private final Indir<Text> ip =  Utils.transform(() -> fv.current.ip , v -> ipf.render("IP: " + v));
//    private final Indir<Text> oip = Utils.transform(() -> fv.current.oip, v -> ipf.render("IP: " + v));

//    private static Coord actc(int i) {
//	int rl = 5;
//	return(new Coord((actpitch * (i % rl)) - (((rl - 1) * actpitch) / 2), UI.scale(125) + ((i / rl) * actpitch)));
//    }

    private static final Coord cmc = UI.scale(new Coord(0, 67));
    private static final Coord usec1 = UI.scale(new Coord(-65, 67));
    private static final Coord usec2 = UI.scale(new Coord(65, 67));
    private Indir<Resource> lastact1 = null, lastact2 = null;
    private Text lastacttip1 = null, lastacttip2 = null;
    private Effect curtgtfx;
	private Effect curtgtfx2;
    public void draw(GOut g) {
//	updatepos();
	relations.clear();
	MapView map = getparent(GameUI.class).map;
	if (map != null){
		for (Fightview.Relation rel : fv.lsrel) {
			try {
				Coord3f rawc = map.glob.oc.getgob(rel.gobid).placed.getc();
				rawc.z = rawc.z + 15;
				if (rawc == null)
					continue;
				relations.put(rel, map.screenxf(rawc).round2());
			} catch (NullPointerException ignore) {}
		}
	}
	int fps = GLPanel.Loop.fps > 0 ? GLPanel.Loop.fps : 1;
	int alphaShiftSpeed = 2400/fps;
	if (combatMedAlphaShiftUp) {
		if (combatMedColorShift + alphaShiftSpeed <= 255) {
			combatMedColorShift += alphaShiftSpeed;
		} else {
			combatMedAlphaShiftUp = false;
			combatMedColorShift = 255;
		}
	} else {
		if (combatMedColorShift - alphaShiftSpeed >= 0){
			combatMedColorShift -= alphaShiftSpeed;
		} else {
			combatMedAlphaShiftUp = true;
			combatMedColorShift = 0;
		}
	}
	if (OptWnd.drawFloatingCombatDataCheckBox.a) {
		if (OptWnd.drawFloatingCombatDataOnCurrentTargetCheckBox.a) {
			try {
				Coord sc = relations.get(fv.current);
				if (sc != null)
					drawCombatData(g, fv.current, sc, true, true);
			} catch (NullPointerException ignored) {}
		}
		if (OptWnd.drawFloatingCombatDataOnOthersCheckBox.a) {
			try {
				for (Map.Entry<Fightview.Relation, Coord> entry : relations.entrySet()) {
					Fightview.Relation otherRelation = entry.getKey();
					Coord sc = entry.getValue();
					if (sc == null || fv.current == otherRelation) {
						continue;
					}
					drawCombatData(g, otherRelation, sc, !OptWnd.onlyShowOpeningsAbovePercentageCombatInfoCheckBox.a, !OptWnd.onlyShowCoinsAbove4CombatInfoCheckBox.a);
				}
			} catch (NullPointerException ignored) {}
		}
	}
	if (OptWnd.drawFloatingCombatOpeningsAboveYourselfCheckBox.a) {
		try {
			drawSelfCombatOpenings(g);
		} catch (Exception ignored) {}
	}

	int x = (int)(ui.gui.sz.x / 2.0);
	int y = (int)(ui.gui.sz.y - ((ui.gui.sz.y / 500.0) * OptWnd.combatUITopPanelHeightSlider.val));
	int bottom = (int)(ui.gui.sz.y - ((ui.gui.sz.y / 500.0) * OptWnd.combatUIBottomPanelHeightSlider.val));

	double now = Utils.rtime();

//	for(Buff buff : fv.buffs.children(Buff.class))
//	    buff.draw(g.reclip(pcc.add(-buff.c.x - Buff.cframe.sz().x - UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
	ArrayList<Buff> myOpenings = new ArrayList<>(fv.buffs.children(Buff.class));
	myOpenings.sort((o2, o1) -> Integer.compare(getOpeningValue(o1), getOpeningValue(o2)));
	Buff myManeuver = null;
	for (Buff buff : myOpenings) {
		try {
			if (buff.res != null && buff.res.get() != null) {
				String name = buff.res.get().name;
				if (Config.maneuvers.contains(name)) {
					myManeuver = buff;
					break;
				}
			}
		} catch (Loading ignored) {
		}
	}
	if (myManeuver != null && myOpenings.size() > 1) {
		myOpenings.remove(myManeuver);
		myOpenings.add(myOpenings.size(), myManeuver);
	}
	int myLocation = - Buff.cframe.sz().x - UI.scale(80);
	for (Buff buff : myOpenings) {
		try {
			Tex img = buff.res.get().flayer(Resource.imgc).tex();
			Coord isz = img.sz();
			g.chcolor(255, 255, 255, 255);
			Double ameter = (buff.ameter >= 0) ? Double.valueOf(buff.ameter / 100.0) : buff.ameteri.get();
			int ameteri = 0; // Added
			if(ameter != null) {
				ameteri = (int) (100*ameter); // Added
				g.image(Buff.cframe, new Coord(x + myLocation - UI.scale(3), y - UI.scale(20) - UI.scale(3)));
				g.chcolor(0, 0, 0, 255);
				g.frect(new Coord(x + myLocation, y - UI.scale(20) + UI.scale(37) - UI.scale(3)), Buff.ametersz);
				g.chcolor(255, 255, 255, 255);
				g.frect(new Coord(x + myLocation, y - UI.scale(20) + UI.scale(37) - UI.scale(3)), new Coord((int)Math.floor(ameter * Buff.ametersz.x), Buff.ametersz.y));
			} else {
				g.image(Buff.frame, new Coord(x + myLocation - UI.scale(3), y - UI.scale(20) - UI.scale(3)));
			}
			if (Buff.improvedOpeningsImageColor.containsKey(buff.res.get().name)) {
				g.chcolor(Buff.improvedOpeningsImageColor.get(buff.res.get().name));
				g.frect(new Coord(x + myLocation, y - UI.scale(20)), isz);
				g.chcolor(Color.WHITE);
				if(ameteri != buff.nmeter) {
					buff.ntext = null;
					buff.nmeter = ameteri;
				}
			} else {
				g.image(img, new Coord(x + myLocation, y - UI.scale(20)));
			}
			if(buff.nmeter >= 0)
				g.aimage(buff.nmeter(), new Coord(x + myLocation, y - UI.scale(20)).add(isz).sub(1, 1), 1, 1);
			myLocation -= UI.scale(40);
		} catch (Loading ignored) {
		}
	}
	if(fv.current != null) {
//	    for(Buff buff : fv.current.buffs.children(Buff.class))
//		buff.draw(g.reclip(pcc.add(buff.c.x + UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
		ArrayList<Buff> enemyOpenings = new ArrayList<>(fv.current.buffs.children(Buff.class));
		setupOpeningArr(enemyOpenings);
		enemyOpenings.sort((o1, o2) -> Integer.compare(getOpeningValue(o2), getOpeningValue(o1)));
		Buff maneuver = null;
		for (Buff buff : enemyOpenings) {
			try {
				if (buff.res != null && buff.res.get() != null) {
					String name = buff.res.get().name;
					if (Config.maneuvers.contains(name)) {
						maneuver = buff;
						break;
					}
				}
			} catch (Loading ignored) {
			}
		}
		if (maneuver != null && enemyOpenings.size() > 1) {
			enemyOpenings.remove(maneuver);
			enemyOpenings.add(enemyOpenings.size(), maneuver);
		}
		int location = UI.scale(80);
		for (Buff buff : enemyOpenings) {
			try {
				String name = buff.res.get().name;
				int meterValue = getOpeningValue(buff);
				Tex img = buff.res.get().flayer(Resource.imgc).tex();
				Coord isz = img.sz();
				g.chcolor(255, 255, 255, 255);
				Double ameter = (buff.ameter >= 0) ? Double.valueOf(buff.ameter / 100.0) : buff.ameteri.get();
				int ameteri = 0; // Added
				if(ameter != null) {
					ameteri = (int) (100*ameter); // Added
					g.image(Buff.cframe, new Coord(x + location - UI.scale(3), y - UI.scale(20) - UI.scale(3)));
					g.chcolor(0, 0, 0, 255);
					g.frect(new Coord(x + location, y - UI.scale(20) + UI.scale(37) - UI.scale(3)), Buff.ametersz);
					g.chcolor(255, 255, 255, 255);
					g.frect(new Coord(x + location, y - UI.scale(20) + UI.scale(37) - UI.scale(3)), new Coord((int)Math.floor(ameter * Buff.ametersz.x), Buff.ametersz.y));
				} else {
					g.image(Buff.frame, new Coord(x + location - UI.scale(3), y - UI.scale(20) - UI.scale(3)));
				}
				if (Buff.improvedOpeningsImageColor.containsKey(name)) {
					g.chcolor(Buff.improvedOpeningsImageColor.get(name));
					g.frect(new Coord(x + location, y - UI.scale(20)), isz);
					g.chcolor(Color.WHITE);
					if(ameteri != buff.nmeter) {
						buff.ntext = null;
						buff.nmeter = ameteri;
					}
				} else {
					if (name.equals("paginae/atk/combmed")){
						if(meterValue > 75){
							g.chcolor(255, 255-combatMedColorShift, 255-combatMedColorShift, 255);
						}
					}
					g.image(img, new Coord(x + location, y - UI.scale(20)));
					g.chcolor(255, 255, 255, 255);
				}
				if(buff.nmeter >= 0)
					g.aimage(buff.nmeter(), new Coord(x + location, y - UI.scale(20)).add(isz).sub(1, 1), 1, 1);
				location += UI.scale(40);
			} catch (Loading ignored) {
			}
		}

	    g.aimage(ip.get().tex(), new Coord(x - UI.scale(40), y - UI.scale(30)), 1, 0.5);
	    g.aimage(oip.get().tex(), new Coord(x + UI.scale(40), y - UI.scale(30)), 0, 0.5);

//	    if(fv.lsrel.size() > 1)
//		curtgtfx = fxon(fv.current.gobid, tgtfx, curtgtfx);
		curtgtfx2 = fxon2(fv.current.gobid, tgtfx, curtgtfx2);
	}

	    Coord cdc = new Coord(x, y);
		Coord cdc2 = new Coord(x, y - UI.scale(40));
		Coord cdc3 = new Coord(x, y + UI.scale(34));
	    if(now < fv.atkct) {
		double a = (now - fv.atkcs) / (fv.atkct - fv.atkcs);
		g.chcolor(225, 0, 0, 220);
		g.fellipse(cdc, UI.scale(new Coord(24, 24)), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
		g.chcolor();
		g.aimage(Text.renderstroked(fmt1DecPlace(fv.atkct - now)).tex(), cdc, 0.5, 0.5);
	    }
	    g.image(cdframe, new Coord(x, y).sub(cdframe.sz().div(2)));

	try {
	    Indir<Resource> lastact = fv.lastact;
	    if(lastact != this.lastact1) {
		this.lastact1 = lastact;
		this.lastacttip1 = null;
	    }
	    double lastuse = fv.lastuse;
	    if(lastact != null) {
		Tex ut = lastact.get().flayer(Resource.imgc).tex();
		Coord useul = new Coord(x - UI.scale(69), y - UI.scale(80));
		g.image(ut, useul);
		g.image(useframe, useul.sub(useframeo));
		double a = now - lastuse;
		if(a < 1) {
		    Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
		    g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
		    g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
		    g.chcolor();
		}
	    }
		if (!fv.currentChanged && lastact1 != null && (lastact1.get() != null && lastact1.get().name != null) && fv.current != null && fv.lastMoveUpdated) {
			Double lastMoveCooldown = fv.lastMoveCooldown;
			String lastMoveName = lastact1.get().name;
			if (Config.unarmedAttackMoves.keySet().stream().anyMatch(lastMoveName::matches)) {
				Double moveDefaultCooldown = Config.unarmedAttackMoves.get(lastMoveName);
				if (Config.attackCooldownNumbers.keySet().stream().anyMatch(moveDefaultCooldown::equals)){
					if (Config.attackCooldownNumbers.get(moveDefaultCooldown).keySet().stream().anyMatch(lastMoveCooldown::equals)){
						HashMap<Double, ArrayList<Double>> cooldowns = Config.attackCooldownNumbers.get(moveDefaultCooldown);
						if (cooldowns != null) {
							Double minAgi = cooldowns.get(lastMoveCooldown).get(0);
							Double maxAgi = cooldowns.get(lastMoveCooldown).get(1);
							if (minAgi != null && maxAgi != null) {
								fv.current.minAgi = Math.max(fv.current.minAgi, minAgi);
								fv.current.maxAgi = Math.min(fv.current.maxAgi, maxAgi);
							}
						}
					}
				}
			} else if (Config.meleeAttackMoves.keySet().stream().anyMatch(lastMoveName::matches)) {
				Double moveDefaultCooldown = Config.meleeAttackMoves.get(lastMoveName);
				Gob gob = ui.sess.glob.oc.getgob(ui.gui.map.plgob);
				boolean b12Equipped = false;
				boolean cutbladeEquipped = false;
				boolean pickaxeEquipped = false;
				if (gob != null) {
					for (GAttrib gobAttrib : gob.attr.values()) {
						if (gobAttrib instanceof Composite) {
							Composite c = (Composite) gobAttrib;
							if (c.comp.cequ.size() > 0) {
								for (Composited.ED item : c.comp.cequ) { // ND: Check for the equipped weapon
									if (item.res.res.get().basename().equals("b12axe")){
										b12Equipped = true;
										break;
									} else if (item.res.res.get().basename().equals("cutblade")){
										cutbladeEquipped = true;
										break;
									} else if (item.res.res.get().basename().equals("pickaxe")){
										pickaxeEquipped = true;
										break;
									}
								}
							}
						}
					}
					if (!b12Equipped && !cutbladeEquipped && !pickaxeEquipped) { // ND: Default cooldowns, weapon has 100% attack speed
						if (Config.attackCooldownNumbers.keySet().stream().anyMatch(moveDefaultCooldown::equals)){
							if (Config.attackCooldownNumbers.get(moveDefaultCooldown).keySet().stream().anyMatch(lastMoveCooldown::equals)){
								HashMap<Double, ArrayList<Double>> cooldowns = Config.attackCooldownNumbers.get(moveDefaultCooldown);
								if (cooldowns != null && cooldowns.get(lastMoveCooldown) != null) {
									Double minAgi = cooldowns.get(lastMoveCooldown).get(0);
									Double maxAgi = cooldowns.get(lastMoveCooldown).get(1);
									if (minAgi != null && maxAgi != null) {
										fv.current.minAgi = Math.max(fv.current.minAgi, minAgi);
										fv.current.maxAgi = Math.min(fv.current.maxAgi, maxAgi);
									}
								}
							}
						}
					} else if (b12Equipped) {
						if (Config.b12AttackCooldownNumbers.keySet().stream().anyMatch(lastMoveName::matches)) {
							HashMap<Double, ArrayList<Double>> cooldowns = Config.b12AttackCooldownNumbers.get(lastMoveName);
							if (cooldowns != null && cooldowns.get(lastMoveCooldown) != null) {
								Double minAgi = cooldowns.get(lastMoveCooldown).get(0);
								Double maxAgi = cooldowns.get(lastMoveCooldown).get(1);
								if (minAgi != null && maxAgi != null) {
									fv.current.minAgi = Math.max(fv.current.minAgi, minAgi);
									fv.current.maxAgi = Math.min(fv.current.maxAgi, maxAgi);
								}
							}
						}
					} else if (cutbladeEquipped) {
						if (Config.cutbladeAttackCooldownNumbers.keySet().stream().anyMatch(lastMoveName::matches)) {
							HashMap<Double, ArrayList<Double>> cooldowns = Config.cutbladeAttackCooldownNumbers.get(lastMoveName);
							if (cooldowns != null && cooldowns.get(lastMoveCooldown) != null) {
								Double minAgi = cooldowns.get(lastMoveCooldown).get(0);
								Double maxAgi = cooldowns.get(lastMoveCooldown).get(1);
								if (minAgi != null && maxAgi != null) {
									fv.current.minAgi = Math.max(fv.current.minAgi, minAgi);
									fv.current.maxAgi = Math.min(fv.current.maxAgi, maxAgi);
								}
							}
						}
					}
				}
			}
			fv.lastMoveUpdated = false;
		}
	} catch(Exception ignored) {
	}
	g.aimage(Text.renderstroked(fmt2DecPlaces(fv.lastMoveCooldownSeconds)).tex(), cdc2, 0.5, 0.5);
	if(fv.current != null) {
		if (OptWnd.showEstimatedAgilityTextCheckBox.a) {
			g.aimage(Text.renderstroked("Est. Agi: ").tex(), cdc3, 1, 0.5);
			if (fv.current.minAgi != 0 && fv.current.maxAgi != 2D) {
				g.aimage(Text.renderstroked("" + fv.current.minAgi + "x - " + fv.current.maxAgi + "x").tex(), cdc3, 0, 0.5);
			} else if (fv.current.minAgi == 0 && fv.current.maxAgi != 2D) {
				g.aimage(Text.renderstroked("<" + fv.current.maxAgi + "x", ipInfoColorMe, Color.BLACK).tex(), cdc3, 0, 0.5);
			} else if (fv.current.minAgi != 0 && fv.current.maxAgi == 2D) {
				g.aimage(Text.renderstroked(">" + fv.current.minAgi + "x", ipInfoColorEnemy, Color.BLACK).tex(), cdc3, 0, 0.5);
			} else {
				g.aimage(Text.renderstroked("Unknown").tex(), cdc3, 0, 0.5);
			}
		}
	    try {
		Indir<Resource> lastact = fv.current.lastact;
		if(lastact != this.lastact2) {
		    this.lastact2 = lastact;
		    this.lastacttip2 = null;
		}
		double lastuse = fv.current.lastuse;
		if(lastact != null) {
		    Tex ut = lastact.get().flayer(Resource.imgc).tex();
		    Coord useul = new Coord(x + UI.scale(69) - ut.sz().x, y - UI.scale(80));
		    g.image(ut, useul);
		    g.image(useframe, useul.sub(useframeo));
		    double a = now - lastuse;
		    if(a < 1) {
			Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
			g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
			g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
			g.chcolor();
		    }
		}
	    } catch(Loading l) {
	    }
	}
	for(int i = 0; i < actions.length; i++) {
	    Coord ca = new Coord(x - 16, bottom - UI.scale(150)).add(actc(i)) ;
	    Action act = actions[i];
	    try {
		if(act != null) {
		    Resource res = act.res.get();
		    Tex img = res.flayer(Resource.imgc).tex();
		    Coord ic = ca.sub(img.sz().div(2));
			Coord hsz = img.sz().div(2);
		    g.image(img, ca);
		    if(now < act.ct) {
			double a = (now - act.cs) / (act.ct - act.cs);
			g.chcolor(0, 0, 0, 132);
			g.prect(ca.add(hsz), hsz.inv(), hsz, (1.0 - a) * Math.PI * 2);
			g.chcolor();
		    }
			int infoY = 0;
			if (OptWnd.showCombatHotkeysUICheckBox.a) {
				String keybindString = kb_acts[i].key().name();

				infoY += 8;
				g.aimage(new TexI(Utils.outline2(keybindsFoundry.render(keybindString).img, Color.BLACK, true)), ca.add((int)(img.sz().x/2), img.sz().y + UI.scale(infoY)), 0.5, 0.5);
			}
			if (OptWnd.showDamagePredictUICheckBox.a) {
				String name = act.res.get().basename();
				if(Config.MapAttInfo.containsKey(name)) {	//Exists?
					Config.AttackInfo attack = Config.MapAttInfo.get(name);
					double openingMul;
					double opening;
					if(attack.getColors().length>1) {
						opening = 1;
						for(Config.Color color : attack.getColors()) {
							opening *= 1.0 - ((double)openingArr[color.getOrder()] / 100);
						}
						opening = 1.0 - opening;
					}
					else {
						opening = ((double)openingArr[attack.getColors()[0].getOrder()] / 100);
					}
					openingMul = (opening*opening);

					if(attack.isMC()) {
						double weaponDamageCalc;
						weaponDamageCalc = basedmg * Math.sqrt( Math.sqrt(ql*str) / 10);
						name = Integer.toString((int)Math.ceil( //I need to cast this into Integer so it doesnt print "0.0", printing "0" is prettier.
						weaponDamageCalc //Full damage
						*attack.getDmgMul()
						*openingMul
					));
					}
					else {
						name = Integer.toString((int)Math.ceil( //I need to cast this into Integer so it doesnt print "0.0", printing "0" is prettier.
							attack.getDmg()*Math.sqrt(str/10) //Full damage
							*openingMul
						));

					}
				}
				else{
					name = "";
				}
				if(name!="") {
					infoY += 12;
					g.aimage(new TexI(Utils.outline2(damageFoundry.render(name,Color.RED).img, Color.BLACK, true)), ca.add((int)(img.sz().x/2), img.sz().y + UI.scale(infoY)), 0.5, 0.5);
				}
			}
		    if(i == use) {
			g.image(indframe, ca.sub(indframeo));
		    } else if(i == useb) {
			g.image(indbframe, ca.sub(indbframeo));
		    } else {
			g.image(actframe, ca.sub(actframeo));
		    }
		}
	    } catch(Loading l) {}
	}
	if (!OptWnd.alwaysShowCombatUIStaminaBarCheckBox.a) { // ND: Check if we're already drawing it in the gui
		IMeter.Meter stam = ui.gui.getmeter("stam", 0);
		if (stam != null) {
			Coord msz = UI.scale(new Coord(234, 22));
			Coord sc = OptWnd.stamBarLocationIsTop ? new Coord(x - msz.x/2,  y + UI.scale(70)) : new Coord(x - msz.x/2,  bottom - UI.scale(68));
			drawStamMeterBar(g, stam, sc, msz);
		}
	}

	if (!OptWnd.alwaysShowCombatUIHealthBarCheckBox.a) { // ND: Check if we're already drawing it in the gui
		IMeter.Meter hp = ui.gui.getmeter("hp", 0);
		if (hp != null) {
			Coord msz = UI.scale(new Coord(234, 22));
			Coord sc = new Coord(x - msz.x / 2, y + UI.scale(44));
			drawHealthMeterBar(g, hp, sc, msz);
		}
	}
    }

    private Widget prevtt = null;
    private Text acttip = null;
    public static final String[] keytips = {"1", "2", "3", "4", "5", "Shift+1", "Shift+2", "Shift+3", "Shift+4", "Shift+5"};
    public Object tooltip(Coord c, Widget prev) {
	int x = (int)(ui.gui.sz.x / 2.0);
	int y = (int)(ui.gui.sz.y - ((ui.gui.sz.y / 500.0) * OptWnd.combatUITopPanelHeightSlider.val));
	int bottom = (int)(ui.gui.sz.y - ((ui.gui.sz.y / 500.0) * OptWnd.combatUIBottomPanelHeightSlider.val));

	for(Buff buff : fv.buffs.children(Buff.class)) {
	    Coord dc = new Coord(x - buff.c.x - Buff.cframe.sz().x - UI.scale(80), y - UI.scale(20));
	    if(c.isect(dc, buff.sz)) {
		Object ret = buff.tooltip(c.sub(dc), prevtt);
		if(ret != null) {
		    prevtt = buff;
		    return(ret);
		}
	    }
	}
	if(fv.current != null) {
	    for(Buff buff : fv.current.buffs.children(Buff.class)) {
		Coord dc = new Coord(x + buff.c.x + UI.scale(80), y - UI.scale(20));
		if(c.isect(dc, buff.sz)) {
		    Object ret = buff.tooltip(c.sub(dc), prevtt);
		    if(ret != null) {
			prevtt = buff;
			return(ret);
		    }
		}
	    }
	}
	final int rl = 5;
	for(int i = 0; i < actions.length; i++) {
	    Coord ca = new Coord(x - 18, bottom - UI.scale(150)).add(actc(i)).add(16, 16);
	    Indir<Resource> act = (actions[i] == null) ? null : actions[i].res;
	    if(act != null) {
		Tex img = act.get().flayer(Resource.imgc).tex();
		ca = ca.sub(img.sz().div(2));
		if(c.isect(ca, img.sz())) {
            String tip = act.get().flayer(Resource.tooltip).t + " ($b{$col[255,128,0]{" + kb_acts[i].key().name() + "}})";
		    if(kb_acts[i].key() != KeyMatch.nil)
			tip += " ($b{$col[255,128,0]{" + kb_acts[i].key().name() + "}})";
		    if((acttip == null) || !acttip.text.equals(tip))
			acttip = RichText.render(tip, -1);
		    return(acttip);
		}
	    }
	}
	{
	    Indir<Resource> lastact = this.lastact1;
	    if(lastact != null) {
		Coord usesz = lastact.get().flayer(Resource.imgc).sz;
		Coord lac = new Coord(x - UI.scale(69), y - UI.scale(80)).add(usesz.div(2));
		if(c.isect(lac.sub(usesz.div(2)), usesz)) {
		    if(lastacttip1 == null)
			lastacttip1 = Text.render(lastact.get().flayer(Resource.tooltip).t);
		    return(lastacttip1);
		}
	    }
	}
	{
	    Indir<Resource> lastact = this.lastact2;
	    if(lastact != null) {
		Coord usesz = lastact.get().flayer(Resource.imgc).sz;
		Coord lac = new Coord(x + UI.scale(69) - usesz.x, y - UI.scale(80)).add(usesz.div(2));
		if(c.isect(lac.sub(usesz.div(2)), usesz)) {
		    if(lastacttip2 == null)
			lastacttip2 = Text.render(lastact.get().flayer(Resource.tooltip).t);
		    return(lastacttip2);
		}
	    }
	}
	return(null);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "act") {
	    int n = Utils.iv(args[0]);
	    if(args.length > 1) {
		Indir<Resource> res = ui.sess.getresv(args[1]);
		actions[n] = new Action(res);
	    } else {
		actions[n] = null;
	    }
	} else if(msg == "acool") {
	    int n = Utils.iv(args[0]);
	    double now = Utils.rtime();
	    actions[n].cs = now;
	    actions[n].ct = now + (Utils.dv(args[1]) * 0.06);
	} else if(msg == "use") {
	    this.use = Utils.iv(args[0]);
	    this.useb = (args.length > 1) ? Utils.iv(args[1]) : -1;
	} else if(msg == "used") {
	} else {
	    super.uimsg(msg, args);
	}
    }

    public static final KeyBinding[] kb_acts = {
	KeyBinding.get("fgt/0", KeyMatch.forcode(KeyEvent.VK_1, 0)),
	KeyBinding.get("fgt/1", KeyMatch.forcode(KeyEvent.VK_2, 0)),
	KeyBinding.get("fgt/2", KeyMatch.forcode(KeyEvent.VK_3, 0)),
	KeyBinding.get("fgt/3", KeyMatch.forchar('R', 0)),
	KeyBinding.get("fgt/4", KeyMatch.forchar('F', 0)),
	KeyBinding.get("fgt/5", KeyMatch.forcode(KeyEvent.VK_1, KeyMatch.S)),
	KeyBinding.get("fgt/6", KeyMatch.forcode(KeyEvent.VK_2, KeyMatch.S)),
	KeyBinding.get("fgt/7", KeyMatch.forcode(KeyEvent.VK_3, KeyMatch.S)),
	KeyBinding.get("fgt/8", KeyMatch.forcode(KeyEvent.VK_F2, 0)),
	KeyBinding.get("fgt/9", KeyMatch.forcode(KeyEvent.VK_F1, 0)),
    };
    public static final KeyBinding kb_relcycle =  KeyBinding.get("fgt-cycle", KeyMatch.forcode(KeyEvent.VK_TAB, 0));
	public static final KeyBinding kb_nearestTarget =  KeyBinding.get("fgt-nearestTarget", KeyMatch.forcode(KeyEvent.VK_SPACE, 0));

    /* XXX: This is a bit ugly, but release message do need to be
     * properly sequenced with use messages in some way. */
    private class Release implements Runnable {
	final int n;

	Release(int n) {
	    this.n = n;
	    Environment env = ui.getenv();
	    Render out = env.render();
	    out.fence(this);
	    env.submit(out);
	}


	public void run() {
	    wdgmsg("rel", n);
	}
    }

    private UI.Grab holdgrab = null;
    private int held = -1;
    public boolean globtype(GlobKeyEvent ev) {
	// ev = new KeyEvent((java.awt.Component)ev.getSource(), ev.getID(), ev.getWhen(), ev.getModifiersEx(), ev.getKeyCode(), ev.getKeyChar(), ev.getKeyLocation());
	{
	    int n = -1;
	    for(int i = 0; i < kb_acts.length; i++) {
		if(kb_acts[i].key().match(ev)) {
		    n = i;
		    break;
		}
	    }
	    int fn = n;
	    if((n >= 0) && (n < actions.length)) {
		MapView map = getparent(GameUI.class).map;
		Coord mvc = map.rootxlate(ui.mc);
		if(held >= 0) {
		    new Release(held);
		    held = -1;
		}
		if(mvc.isect(Coord.z, map.sz)) {
		    map.new Maptest(mvc) {
			    protected void hit(Coord pc, Coord2d mc) {
				wdgmsg("use", fn, 1, ui.modflags(), mc.floor(OCache.posres));
			    }

			    protected void nohit(Coord pc) {
				wdgmsg("use", fn, 1, ui.modflags());
			    }
			}.run();
		}
		if(holdgrab == null)
		    holdgrab = ui.grabkeys(this);
		held = n;
		return(true);
	    }
	}
	if(kb_nearestTarget.key().match(ev)) {
		targetNearestFoe(getparent(GameUI.class));
		return (true);
	}
	if(kb_relcycle.key().match(ev.awt, KeyMatch.S)) {
	    if((ev.mods & KeyMatch.S) == 0) {
		Fightview.Relation cur = fv.current;
		if(cur != null) {
		    fv.lsrel.remove(cur);
		    fv.lsrel.addLast(cur);
		}
	    } else {
		Fightview.Relation last = fv.lsrel.getLast();
		if(last != null) {
		    fv.lsrel.remove(last);
		    fv.lsrel.addFirst(last);
		}
	    }
	    fv.wdgmsg("bump", (int)fv.lsrel.get(0).gobid);
	    return(true);
	}
	return(super.globtype(ev));
    }

    public boolean keydown(KeyDownEvent ev) {
	return(false);
    }

    public boolean keyup(KeyUpEvent ev) {
	if(ev.grabbed && (kb_acts[held].key().match(ev.awt, KeyMatch.MODS))) {
	    MapView map = getparent(GameUI.class).map;
	    new Release(held);
	    holdgrab.remove();
	    holdgrab = null;
	    held = -1;
	    return(true);
	}
	return(false);
    }

	private Effect fxon2(long gobid, Resource fx, Effect cur) {
		MapView map = getparent(GameUI.class).map;
		Gob gob = ui.sess.glob.oc.getgob(gobid);
		if((map == null) || (gob == null))
			return(null);
		Pipe.Op place;
		try {
			place = gob.placed.curplace();
		} catch(Loading l) {
			return(null);
		}
		if((cur == null) || (cur.slot == null)) {
			try {
				cur = new Effect(new CurrentTargetSprite(null));
				cur.slot = map.basic.add(cur.spr, place);
			} catch(Loading l) {
				return(null);
			}
			curfx.add(cur);
		} else {
			cur.slot.cstate(place);
		}
		cur.used = true;
		return(cur);
	}

	private void drawCombatData(GOut g, Fightview.Relation rels, Coord sc, boolean showAllOpenings, boolean alwaysShowCoins) {
		int scaledY = sc.y - UI.scale(86);
		Coord topLeft = new Coord(sc.x - UI.scale(32), scaledY);
		boolean openings;
		boolean cleaveUsed = false;
		long cleaveDuration = 4300;
		//Check if cleave indicator is needed
		if (rels.lastActCleave != null) {
			Gob gob = ui.sess.glob.oc.getgob(rels.gobid);
			for (GAttrib gobAttrib : gob.attr.values()) { // ND: Do the following stuff to figure out the *MINIMUM* cooldown someone can have after cleaving.
				if (gobAttrib instanceof Composite) {
					Composite c = (Composite) gobAttrib;
					if (c.comp.cequ.size() > 0) {
						for (Composited.ED item : c.comp.cequ) { // ND: Check for the equipped weapon
							if (item.res.res.get().basename().equals("b12axe")) { // 5.4
								cleaveDuration = 5400;
								break;
							}
							if (item.res.res.get().basename().equals("cutblade")) { // 5.2
								cleaveDuration = 5200;
								break;
							} else { // 4.3 seconds for anything else
								cleaveDuration = 4300;
							}
						}
					}
				}
			}
			cleaveUsed = System.currentTimeMillis() - rels.lastActCleave < cleaveDuration;
		}
		boolean defenseUsed = false;
		if (rels.lastActDefence != null) {
			defenseUsed = System.currentTimeMillis() - rels.lastActDefence < rels.lastDefenceDuration;
		}

		// ND: Check for openings depending if it's a player or not.
		Gob gob = ui.sess.glob.oc.getgob(rels.gobid);
		if (gob != null && gob.getres() != null) {
			if (gob.getres().name.equals("gfx/borka/body")) { // ND: If it's a player, the first buff is the maneuver, always, so skip it.
				openings = rels.buffs.children(Buff.class).size() > 1;
			} else { // ND: Everything else doesn't have a maneuver.
				openings = rels.buffs.children(Buff.class).size() > 0;
			}
		} else {
			openings = rels.buffs.children(Buff.class).size() > 1;
		}

		topLeft.x -= UI.scale(2) * rels.buffs.children(Buff.class).size();

		// IP / OIP Text
		boolean showCoins = true;
		if (!alwaysShowCoins)
			if (rels.ip < 4 && rels.oip < 4)
				showCoins = false;
		if (showCoins) {
			g.chcolor(0, 0, 0, 120);
			g.frect(new Coord(topLeft.x + UI.scale(3), topLeft.y + UI.scale(9)), UI.scale(new Coord(39, 20)));
			g.chcolor(255, 255, 255, 255);
			int oipOffset = rels.oip < 10 ? 35 : 40;
			g.aimage(Text.renderstroked(Integer.toString(rels.ip), ipInfoColorMe, Color.BLACK, ipAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(20), topLeft.y + UI.scale(19)), 1, 0.5);
			g.aimage(Text.renderstroked("/", Color.WHITE, Color.BLACK, ipAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(25), topLeft.y + UI.scale(19)), 1, 0.5);
			g.aimage(Text.renderstroked(Integer.toString(rels.oip), ipInfoColorEnemy, Color.BLACK, ipAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(oipOffset), topLeft.y + UI.scale(19)), 1, 0.5);
		}

		// Maneuver
		if (OptWnd.showCombatManeuverCombatInfoCheckBox.a) {
			for (Buff buff : rels.buffs.children(Buff.class)) {
				try {
					if (buff.res != null && buff.res.get() != null) {
						String name = buff.res.get().name;
						if (Config.maneuvers.contains(name)) {
							g.chcolor(0, 0, 0, 255);
							g.frect(new Coord(topLeft.x + UI.scale(41), topLeft.y + UI.scale(9)), UI.scale(new Coord(20, 20)));
							g.chcolor(255, 255, 255, 255);
							int meterValue = getOpeningValue(buff);
							Resource.Image img = buff.res.get().flayer(Resource.imgc);
							Tex maneuverTexture = new TexI(PUtils.uiscale(img.scaled, UI.scale(new Coord(18, 18))));
							if (name.equals("paginae/atk/combmed")) {
								if (meterValue > 70) {
									g.chcolor(255, 255 - combatMedColorShift, 255 - combatMedColorShift, 255);
								}
							}
							g.image(maneuverTexture, new Coord(topLeft.x + UI.scale(42), topLeft.y + UI.scale(10)));

							// Meter
							if (meterValue > 1) {
								g.chcolor(0, 0, 0, 255);
								g.frect(new Coord(topLeft.x + UI.scale(61), topLeft.y + UI.scale(9)), UI.scale(new Coord(5, 20)));
								if (meterValue < 30) {
									g.chcolor(255, 255, 255, 255);
								} else {
									g.chcolor(255, (255 - (255 * meterValue) / 100), (255 - (255 * meterValue) / 100), 255);
								}
								g.frect(new Coord(topLeft.x + UI.scale(62), topLeft.y + UI.scale(28) - ((18 * meterValue) / 100)), UI.scale(new Coord(3, (18 * meterValue) / 100)));
							}
						}
					}
				} catch (Loading ignored) {
				}
			}
		}


		// openings, only if it has any
		if (openings) {
			List<TemporaryOpening> openingList = new ArrayList<>();
			for (Buff buff : rels.buffs.children(Buff.class)) {
				try {
					if (buff.res != null && buff.res.get() != null) {
						String name = buff.res.get().name;
						if (openingsColorMap.containsKey(name)) {
							int meterValue = getOpeningValue(buff);
							openingList.add(new TemporaryOpening(meterValue, openingsColorMap.get(name)));
						}
					}
				} catch (Loading ignored) {
				}
			}
			openingList.sort((o1, o2) -> Integer.compare(o2.value, o1.value));
			boolean showOpenings = true;
			if (!showAllOpenings){
				if (!openingList.isEmpty() && !OptWnd.minimumOpeningTextEntry.text().isEmpty()){
					if (openingList.get(0).value < Integer.parseInt(OptWnd.minimumOpeningTextEntry.text())){
						showOpenings = false;
					}
				}
			}
			if (showOpenings) {
				int openingOffsetX = 4;
				for (TemporaryOpening opening : openingList) {
					g.chcolor(0, 0, 0, 255);
					g.frect(new Coord(topLeft.x + UI.scale(openingOffsetX) - UI.scale(1), topLeft.y + UI.scale(30) - UI.scale(1)), UI.scale(new Coord(20, 20)));
					g.chcolor(opening.color);
					g.frect(new Coord(topLeft.x + UI.scale(openingOffsetX), topLeft.y + UI.scale(30)), UI.scale(new Coord(18, 18)));
					g.chcolor(255, 255, 255, 255);

					int valueOffset = opening.value < 10 ? 15 : opening.value< 100 ? 18 : 20;
					g.aimage(Text.renderstroked(String.valueOf(opening.value), openingAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(openingOffsetX) + UI.scale(valueOffset) - UI.scale(1), topLeft.y + UI.scale(39)), 1, 0.5);
					openingOffsetX += 19;
				}
			}
		}

		//add cleave cooldown indicator
		if (cleaveUsed) {
			long timer = ((cleaveDuration - (System.currentTimeMillis() - rels.lastActCleave)));
			g.chcolor(new Color(0, 0, 0, 255));
			g.frect(new Coord(topLeft.x + UI.scale(3), topLeft.y - UI.scale(4)), UI.scale(new Coord((int) ((76 * timer)/cleaveDuration)+2, 13)));
			g.chcolor(new Color(213, 0, 0, 255));
			g.frect(new Coord(topLeft.x + UI.scale(4), topLeft.y - UI.scale(3)), UI.scale(new Coord((int) ((76 * timer)/cleaveDuration), 11)));
			g.chcolor(new Color(255, 255, 255, 255));
			g.aimage(Text.renderstroked(getCooldownTime(timer), cleaveAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(52), topLeft.y + UI.scale(2)), 1, 0.5);
		}

		//add defense cooldown indicator, just like cleave
		if (defenseUsed) {
			long timer = ((rels.lastDefenceDuration - (System.currentTimeMillis() - rels.lastActDefence)));
			g.chcolor(new Color(0, 0, 0, 255));
			g.frect(new Coord(topLeft.x + UI.scale(3), topLeft.y - UI.scale(4)), UI.scale(new Coord((int) ((76 * timer)/rels.lastDefenceDuration)+2, 13)));
			g.chcolor(new Color(227, 136, 0, 255));
			g.frect(new Coord(topLeft.x + UI.scale(4), topLeft.y - UI.scale(3)), UI.scale(new Coord((int) ((76 * timer)/rels.lastDefenceDuration), 11)));
			g.chcolor(new Color(255, 255, 255, 255));
			g.aimage(Text.renderstroked(getCooldownTime(timer), cleaveAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(52), topLeft.y + UI.scale(2)), 1, 0.5);
		}
		g.chcolor(255, 255, 255, 255);
	}

	private void drawSelfCombatOpenings(GOut g) {
		Coord3f rawc = ui.gui.map.player().placed.getc();
		rawc.z += 15;
		Coord sc = getparent(GameUI.class).map.screenxf(rawc).round2();
		int scaledY = sc.y - UI.scale(86);
		Coord topLeft = new Coord(sc.x - UI.scale(32), scaledY);

		ArrayList<Buff> myOpenings = new ArrayList<>(fv.buffs.children(Buff.class));
		myOpenings.sort((o2, o1) -> Integer.compare(getOpeningValue(o1), getOpeningValue(o2)));
		Buff myManeuver = null;
		for (Buff buff : myOpenings) {
			try {
				if (buff.res != null && buff.res.get() != null) {
					String name = buff.res.get().name;
					if (Config.maneuvers.contains(name)) {
						myManeuver = buff;
						break;
					}
				}
			} catch (Loading ignored) {
			}
		}
		if (myManeuver != null && myOpenings.size() > 1) {
			myOpenings.remove(myManeuver);
		}
		topLeft.x -= UI.scale(3) * myOpenings.size();

		List<TemporaryOpening> openingList = new ArrayList<>();
		for (Buff buff : fv.buffs.children(Buff.class)) {
			try {
				if (buff.res != null && buff.res.get() != null) {
					String name = buff.res.get().name;
					if (openingsColorMap.containsKey(name)) {
						int meterValue = getOpeningValue(buff);
						openingList.add(new TemporaryOpening(meterValue, openingsColorMap.get(name)));
					}
				}
			} catch (Loading ignored) {
			}
		}
		openingList.sort((o2, o1) -> Integer.compare(o1.value, o2.value));
		int openingOffsetX = 4;
		for (TemporaryOpening opening : openingList) {
			g.chcolor(0, 0, 0, 255);
			g.frect(new Coord(topLeft.x + UI.scale(openingOffsetX) - UI.scale(1), topLeft.y + UI.scale(30) - UI.scale(1)), UI.scale(new Coord(20, 20)));
			g.chcolor(opening.color);
			g.frect(new Coord(topLeft.x + UI.scale(openingOffsetX), topLeft.y + UI.scale(30)), UI.scale(new Coord(18, 18)));
			g.chcolor(255, 255, 255, 255);

			int valueOffset = opening.value < 10 ? 15 : opening.value< 100 ? 18 : 20;
			g.aimage(Text.renderstroked(String.valueOf(opening.value), openingAdditionalFont).tex(), new Coord(topLeft.x + UI.scale(openingOffsetX) + UI.scale(valueOffset) - UI.scale(1), topLeft.y + UI.scale(39)), 1, 0.5);
			openingOffsetX += 19;
		}
		g.chcolor(255, 255, 255, 255);
	}

	private int getOpeningValue(Buff buff) {
		Double meterDouble = (buff.ameter >= 0) ? Double.valueOf(buff.ameter / 100.0) : buff.ameteri.get();
		if (meterDouble != null) {
			return (int) (100 * meterDouble);
		}
		return 0;
	}

	private static class TemporaryOpening{
		private int value;
		private Color color;

		public TemporaryOpening(int value, Color color) {
			this.value = value;
			this.color = color;
		}
	}

	public String getCooldownTime(long time) {
		double convertedTime = time / 1000.0;
		return String.format("%.1f", convertedTime);
	}

	public static String fmt1DecPlace(double value) {
		double rvalue = (double) Math.round(value * 10) / 10;
		return (rvalue % 1 == 0) ? Integer.toString((int) rvalue) : Double.toString(rvalue);
	}

	public static String fmt2DecPlaces(double value) {
		double rvalue = (double) Math.round(value * 100) / 100;
		return (rvalue % 1 == 0) ? Integer.toString((int) rvalue) : Double.toString(rvalue);
	}

	private void drawHealthMeterBar(GOut g, IMeter.Meter m, Coord sc, Coord msz) {
		int w = msz.x;
		int w1 = (int) Math.ceil(w * m.a);
		int w2 = (int) Math.ceil(w * (IMeter.characterSoftHealthPercent/100));
		if (IMeter.sparring) {
			g.chcolor(Fightsess.hpBarGray);
			g.frect(sc, new Coord(w, msz.y));
			g.chcolor(Fightsess.hpBarGreen);
			g.frect(sc, new Coord(w2, msz.y));
			g.chcolor(Color.BLACK);
			g.line(new Coord(sc.x+w, sc.y), new Coord(sc.x+w, sc.y+msz.y), 2);
			g.rect(sc, new Coord(msz.x, msz.y));

			g.chcolor(Color.WHITE);
			g.aimage(Text.renderstroked((IMeter.characterCurrentHealth), Text.num12boldFnd).tex(), new Coord(sc.x+msz.x/2, sc.y+msz.y/2), 0.5, 0.5);
		} else {
			g.chcolor(Fightsess.hpBarYellow);
			g.frect(sc, new Coord(w1, msz.y));
			g.chcolor(Fightsess.hpBarRed);
			g.frect(sc, new Coord(w2, msz.y));
			g.chcolor(Color.BLACK);
			g.line(new Coord(sc.x+w1, sc.y), new Coord(sc.x+w1, sc.y+msz.y), 2);
			g.rect(sc, new Coord(msz.x, msz.y));

			g.chcolor(Color.WHITE);
			String HHPpercentage = OptWnd.includeHHPTextHealthBarCheckBox.a ? " ("+(Fightsess.fmt1DecPlace((int)(m.a*100))) + "% HHP)" : "";
			g.aimage(Text.renderstroked((IMeter.characterCurrentHealth + HHPpercentage), Text.num12boldFnd).tex(), new Coord(sc.x+msz.x/2, sc.y+msz.y/2), 0.5, 0.5);
		}
	}

	private void drawStamMeterBar(GOut g, IMeter.Meter m, Coord sc, Coord msz) {
		int w = msz.x;
		int w1 = (int) Math.ceil(w * m.a);
		g.chcolor(stamBarBlue);
		g.frect(sc, new Coord(w1, msz.y));
		g.chcolor(Color.BLACK);
		g.line(new Coord(sc.x+w1, sc.y), new Coord(sc.x+w1, sc.y+msz.y), 2);
		g.rect(sc, new Coord(msz.x, msz.y));
		g.chcolor(Color.WHITE);
		String staminaBarText = fmt1DecPlace((int)(m.a*100));
		Gob myself = ui.gui.map.player();
		if (myself != null && myself.imDrinking) {
			g.chcolor(new Color(0, 222, 0));
			staminaBarText = staminaBarText + " (Drinking)";
		}
		g.aimage(Text.renderstroked(staminaBarText, Text.num12boldFnd).tex(), new Coord(sc.x+msz.x/2, sc.y+msz.y/2), 0.5, 0.5);
	}

	private void targetNearestFoe(GameUI gui) {
		//Select nearest enemy
		Fightview.Relation cur = fv.current;
		Fightview.Relation closestRel = fv.current;
		Gob closest = null;
		Gob player = gui.map.player();
		if (player == null) {
			return;
		}

		for (Fightview.Relation rel : fv.lsrel) {
			Gob gob = ui.sess.glob.oc.getgob(rel.gobid);
			if (gob != null && gob.rc != null) {
				if (closest == null) {
					closest = gob;
					closestRel = rel;
				} else if (player.rc.dist(gob.rc) < player.rc.dist(closest.rc)) {
					closest = gob;
					closestRel = rel;
				}
			}
		}

		if (cur != null) {
			fv.lsrel.remove(cur);
			fv.lsrel.addLast(cur);
		}

		if (closestRel != null) {
			fv.wdgmsg("bump", (int) closestRel.gobid);
		}
	}
	private void setupOpeningArr(ArrayList<Buff> buffs)
	{
		for(Buff buff : buffs)
		{
			switch(buff.res.get().name)	{
				case "paginae/atk/offbalance":
					openingArr[0] = getOpeningValue(buff);
					break;
				case "paginae/atk/reeling":
					openingArr[1] = getOpeningValue(buff);
					break;
				case "paginae/atk/cornered":
					openingArr[2] = getOpeningValue(buff);
					break;
				case "paginae/atk/dizzy":
					openingArr[3] = getOpeningValue(buff);
					break;
			}
		}
	}
	private static void setupWepDmg(GameUI gui) {
		GItem wep = gui.getequipory().getWeapon();
		wepdmg = ItemInfo.getDamage(wep.info);
		//ui.gui.msg("wepdmg: "+wepdmg,Color.white);
		ql = gui.getequipory().getWeapon().getQBuff().q;
		//ui.gui.msg("ql: "+ql,Color.white);
		basedmg = (int)(Math.ceil(wepdmg/Math.sqrt(ql/10)));
		//ui.gui.msg("basedmg: "+basedmg,Color.white);
	}
}
