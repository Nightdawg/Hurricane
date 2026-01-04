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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.Color;
import java.io.File;
import java.util.*;

public class IMeter extends LayerMeter {
    public static final Coord off = UI.scale(22, 6); // Meter start position
    public static final Coord fsz = UI.scale(101, 34); // Full(?) size
    public static final Coord msz = UI.scale(75, 14); // Meter Size
    public final Indir<Resource> bg;
	public final Tex bgTex;
	private static final Text.Foundry tipF = new Text.Foundry(Text.sans, 10);
	private Tex tipTex;
	public static String characterCurrentHealth;
	public static double characterSoftHealthPercent;
	private boolean ponyAlarmPlayed = false;
	private boolean energyAlarmTriggered = false;
	private boolean dangerEnergyAlarmTriggered = false;
	public static boolean sparring = false;

    @RName("im")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Indir<Resource> bg = ui.sess.getresv(args[0]);
	    List<Meter> meters = decmeters(args, 1);
	    return(new IMeter(bg, meters));
	}
    }

    public IMeter(Indir<Resource> bg, List<Meter> meters) {
	super(fsz);
	this.bg = bg;
	bgTex = this.bg.get().flayer(Resource.imgc).tex();
	set(meters);
    }

    public void draw(GOut g) {
	try {
//	    Tex bg = this.bg.get().flayer(Resource.imgc).tex();
	    g.chcolor(0, 0, 0, 255);
	    g.frect(off, msz);
	    g.chcolor();
	    for(Meter m : meters) {
		int w = msz.x;
		w = (int)Math.ceil(w * m.a);
		g.chcolor(m.c);
		g.frect(off, new Coord(w, msz.y));
	    }
	    g.chcolor();
	    g.image(bgTex, Coord.z);
		if (tipTex != null) {
			g.chcolor();
			g.image(tipTex, bgTex.sz().div(2).sub(tipTex.sz().div(2)).add(UI.scale(10), 0));
		}
	} catch(Loading l) {
	}
    }

	public void uimsg(String msg, Object... args) {
		if(msg == "set") {
			this.meters = decmeters(args, 0);
			if (!ponyAlarmPlayed) {
				try {
					Resource res = bg.get();
					if (res != null && res.name.equals("gfx/hud/meter/häst")) {
						if (OptWnd.ponyPowerSoundEnabledCheckbox.a && meters.get(0).a <= 0.10) {
							try {
								File file = new File(haven.MainFrame.gameDir + "AlarmSounds/" + OptWnd.ponyPowerSoundFilename.buf.line() + ".wav");
								if (file.exists()) {
									AudioInputStream in = AudioSystem.getAudioInputStream(file);
									AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
									AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
									Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
									((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, OptWnd.ponyPowerSoundVolumeSlider.val / 50.0));
								}
							} catch (Exception ignored) {
							}
							ponyAlarmPlayed = true;
						}
					}
				} catch (Loading e) {
				}
			}
			if (!energyAlarmTriggered) {
				try {
					Resource res = bg.get();
					if (res != null && res.name.equals("gfx/hud/meter/nrj")) {
						if (OptWnd.lowEnergySoundEnabledCheckbox.a && meters.get(0).a < 0.25 && meters.get(0).a > 0.20) {
							try {
								File file = new File(haven.MainFrame.gameDir + "AlarmSounds/" + OptWnd.lowEnergySoundFilename.buf.line() + ".wav");
								if (file.exists()) {
									AudioInputStream in = AudioSystem.getAudioInputStream(file);
									AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
									AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
									Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
									((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, OptWnd.lowEnergySoundVolumeSlider.val / 50.0));
								}
							} catch (Exception ignored) {
							}
							energyAlarmTriggered = true;
						}
					}
				} catch (Loading e) {
				}
			}
			if (!dangerEnergyAlarmTriggered) {
				try {
					Resource res = bg.get();
					if (res != null && res.name.equals("gfx/hud/meter/nrj")) {
						if (OptWnd.lowEnergySoundEnabledCheckbox.a && meters.get(0).a <= 0.20) {
							try {
								File file = new File(haven.MainFrame.gameDir + "AlarmSounds/" + OptWnd.lowEnergySoundFilename.buf.line() + ".wav");
								if (file.exists()) {
									AudioInputStream in = AudioSystem.getAudioInputStream(file);
									AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
									AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
									Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
									((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, OptWnd.lowEnergySoundVolumeSlider.val / 50.0));
								}
							} catch (Exception ignored) {
							}
							dangerEnergyAlarmTriggered = true;
						}
					}
				} catch (Loading e) {
				}
			}
			if (energyAlarmTriggered || dangerEnergyAlarmTriggered) { // ND: If at least one of the alarms has been triggered, check if we refilled the energy bar above 2500, to reset them in case we once again fall below it.
				try {
					Resource res = bg.get();
					if (res != null && res.name.equals("gfx/hud/meter/nrj")) {
						if (meters.get(0).a > 0.25) {
							energyAlarmTriggered = false;
							dangerEnergyAlarmTriggered = false;
						}
					}
				} catch (Loading e) {
				}
			}
		}
		if (msg == "tip") {
			String value = ((String)args[0]).split(":")[1].replaceAll("(\\(.+\\))", "");
			if (value.contains("/")) { // ND: this part removes the HHP, so I only show the SHP and MHP
				String[] hps = value.split("/");
				if (hps.length == 4) { // ND: Sparring adds a 4th value
					sparring = true;
					String SHP = hps[0].trim();
					if (Double.parseDouble(SHP) > 0) {
						String MHP = hps[3].trim();
						characterSoftHealthPercent = (Double.parseDouble(SHP) / ((Double.parseDouble(MHP) / 100)));
					} else {
						characterSoftHealthPercent = 0;
					}
					value = hps[0] + " / " + hps[hps.length - 1]; // ND: hps[0] is SHP, hps[1] is HHP, hps[2] (or hps[hps.length - 1]) is MHP
					characterCurrentHealth = value;
				} else {
					sparring = false;
					String SHP = hps[0].trim();
					if (Double.parseDouble(SHP) > 0) {
						String MHP = hps[2].trim();
						characterSoftHealthPercent = (Double.parseDouble(SHP) / ((Double.parseDouble(MHP) / 100)));
					} else {
						characterSoftHealthPercent = 0;
					}
					value = hps[0] + " / " + hps[hps.length - 1]; // ND: hps[0] is SHP, hps[1] is HHP, hps[2] (or hps[hps.length - 1]) is MHP
					characterCurrentHealth = value;
				}
			}
			tipTex = PUtils.strokeTex(tipF.render(value.trim()));
		}
		super.uimsg(msg, args);
	}

}
