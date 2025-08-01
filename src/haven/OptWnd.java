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
import haven.res.gfx.fx.msrad.MSRad;
import haven.res.ui.pag.toggle.Toggle;
import haven.resutil.Ridges;
import haven.sprites.AggroCircleSprite;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.Future;

public class OptWnd extends Window {
    public final Panel main;
	public final Panel advancedSettings;
    public Panel current;
	private static final ScheduledExecutorService simpleUIExecutor = Executors.newSingleThreadScheduledExecutor();
	private static Future<?> simpleUIFuture;
	public static boolean simpleUIChanged = false;
	private static final ScheduledExecutorService skyboxExecutor = Executors.newSingleThreadScheduledExecutor();
	private static Future<?> skyboxFuture;
	public static final Color msgGreen = new Color(8, 211, 0);
	public static final Color msgGray = new Color(145, 145, 145);
	public static final Color msgRed = new Color(197, 0, 0);
	public static final Color msgYellow = new Color(218, 163, 0);
	public static FlowerMenuAutoSelectManagerWindow flowerMenuAutoSelectManagerWindow;
	public static AutoDropManagerWindow autoDropManagerWindow;
	AlarmWindow alarmWindow;
	public static GSettings currentgprefs;
	public static final Map<String, Color> improvedOpeningsImageColor =	new ConcurrentHashMap<>(4);

    public void chpanel(Panel p) {
	if(current != null)
	    current.hide();
	(current = p).show();
	cresize(p);
    }

    public void cresize(Widget ch) {
	if(ch == current) {
	    Coord cc = this.c.add(this.sz.div(2));
	    pack();
	    move(cc.sub(this.sz.div(2)));
	}
    }

    public class PButton extends Button {
	public final Panel tgt;
	public final int key;
	public String newCap; // ND: Used to change the title of the options window

//	public PButton(int w, String title, int key, Panel tgt) {
//	    super(w, title, false);
//	    this.tgt = tgt;
//	    this.key = key;
//	}

	public PButton(int w, String title, int key, Panel tgt, String newCap) {
		super(w, title, false);
		this.tgt = tgt;
		this.key = key;
		this.newCap = newCap;
	}

	public void click() {
	    chpanel(tgt);
		OptWnd.this.cap = newCap;
	}

	public boolean keydown(KeyDownEvent ev) {
	    if((this.key != -1) && (ev.c == this.key)) {
		click();
		return(true);
	    }
	    return(super.keydown(ev));
	}
    }

    public class Panel extends Widget {
	public Panel() {
	    visible = false;
	    c = Coord.z;
	}
    }

    private void error(String msg) {
	GameUI gui = getparent(GameUI.class);
	if(gui != null)
	    gui.error(msg);
    }

    public class VideoPanel extends Panel {
	private final Widget back;
	private CPanel curcf;

	public VideoPanel(Panel prev) {
	    super();
		back = add(new PButton(UI.scale(200), "Back", 27, prev, "Options            "));
		pack(); // ND: Fixes top bar not being fully draggable the first time I open the video panel. Idfk.
	}

	public class CPanel extends Widget {
	    public GSettings prefs;

	    public CPanel(GSettings gprefs) {
		this.prefs = gprefs;
		Widget prev;
		int marg = UI.scale(5);
		prev = add(new CheckBox("Render shadows") {
			{a = prefs.lshadow.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.lshadow, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    }, Coord.z);
		prev = add(new Label("Render scale"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label("");
		    final int steps = 4;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), -2 * steps, 1 * steps, (int)Math.round(steps * Math.log(prefs.rscale.val) / Math.log(2.0f))) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(String.format("%.2f\u00d7", Math.pow(2, this.val / (double)steps)));
			       }
			       public void changed() {
				   try {
				       float val = (float)Math.pow(2, this.val / (double)steps);
				       ui.setgprefs(prefs = prefs.update(null, prefs.rscale, val));
					   if(ui.gui != null && ui.gui.map != null) {ui.gui.map.updateGridMat();}
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new CheckBox("Vertical sync") {
			{a = prefs.vsync.val;}

			public void set(boolean val) {
			    try {
				GSettings np = prefs.update(null, prefs.vsync, val);
				ui.setgprefs(prefs = np);
			    } catch(GSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			}
		    }, prev.pos("bl").adds(0, 5));
		prev = add(new Label("Framerate limit (active window)"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label("");
		    final int max = 250;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 20, max, (prefs.hz.val == Float.POSITIVE_INFINITY) ? max : prefs.hz.val.intValue()) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   if(this.val == max)
				       dpy.settext("None");
				   else
				       dpy.settext(Integer.toString(this.val));
			       }
			       public void changed() {
				   try {
				       if(this.val > 10)
					   this.val = (this.val / 2) * 2;
				       float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				       ui.setgprefs(prefs = prefs.update(null, prefs.hz, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Framerate limit (background window)"), prev.pos("bl").adds(0, 5));
		{
		    Label dpy = new Label("");
		    final int max = 250;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 20, max, (prefs.bghz.val == Float.POSITIVE_INFINITY) ? max : prefs.bghz.val.intValue()) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   if(this.val == max)
				       dpy.settext("None");
				   else
				       dpy.settext(Integer.toString(this.val));
			       }
			       public void changed() {
				   try {
				       if(this.val > 10)
					   this.val = (this.val / 2) * 2;
				       float val = (this.val == max) ? Float.POSITIVE_INFINITY : this.val;
				       ui.setgprefs(prefs = prefs.update(null, prefs.bghz, val));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Lighting mode"), prev.pos("bl").adds(0, 5));
		{
		    boolean[] done = {false};
		    RadioGroup grp = new RadioGroup(this) {
			    public void changed(int btn, String lbl) {
				if(!done[0])
				    return;
				try {
				    ui.setgprefs(prefs = prefs
						 .update(null, prefs.lightmode, GSettings.LightMode.values()[btn])
						 .update(null, prefs.maxlights, 0));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				resetcf();
			    }
			};
		    prev = grp.add("Global", prev.pos("bl").adds(5, 2));
		    prev.settip("Global lighting supports fewer light sources, and scales worse in " +
				"performance per additional light source, than zoned lighting, but " +
				"has lower baseline performance requirements.", true);
		    prev = grp.add("Zoned", prev.pos("bl").adds(0, 2));
		    prev.settip("Zoned lighting supports far more light sources than global " +
				"lighting with better performance, but may have higher performance " +
				"requirements in cases with few light sources, and may also have " +
				"issues on old graphics hardware.", true);
		    grp.check(prefs.lightmode.val.ordinal());
		    done[0] = true;
		}
		prev = add(new Label("Light-source limit"), prev.pos("bl").adds(0, 5).x(0));
		{
		    Label dpy = new Label("");
		    int val = prefs.maxlights.val, max = 32;
		    if(val == 0) {    /* XXX: This is just ugly. */
			if(prefs.lightmode.val == GSettings.LightMode.ZONED)
			    val = Lighting.LightGrid.defmax;
			else
			    val = Lighting.SimpleLights.defmax;
		    }
		    if(prefs.lightmode.val == GSettings.LightMode.SIMPLE)
			max = 4;
		    addhlp(prev.pos("bl").adds(0, 2), UI.scale(5),
			   prev = new HSlider(UI.scale(160), 1, max, val / 4) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(Integer.toString(this.val * 4));
			       }
			       public void changed() {dpy();}
			       public void fchanged() {
				   try {
				       ui.setgprefs(prefs = prefs.update(null, prefs.maxlights, this.val * 4));
				   } catch(GSettings.SettingException e) {
				       error(e.getMessage());
				       return;
				   }
				   dpy();
			       }
			       {
				   settip("The light-source limit means different things depending on the " +
					  "selected lighting mode. For Global lighting, it limits the total "+
					  "number of light-sources globally. For Zoned lighting, it limits the " +
					  "total number of overlapping light-sources at any point in space.",
					  true);
			       }
			   },
			   dpy);
		}
		prev = add(new Label("Frame sync mode"), prev.pos("bl").adds(0, 5).x(0));
		{
		    boolean[] done = {false};
		    RadioGroup grp = new RadioGroup(this) {
			    public void changed(int btn, String lbl) {
				if(!done[0])
				    return;
				try {
				    ui.setgprefs(prefs = prefs.update(null, prefs.syncmode, JOGLPanel.SyncMode.values()[btn]));
				} catch(GSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
			    }
			};
		    prev = add(new Label("\u2191 Better performance, worse latency"), prev.pos("bl").adds(5, 2));
		    prev = grp.add("One-frame overlap", prev.pos("bl").adds(0, 2));
		    prev = grp.add("Tick overlap", prev.pos("bl").adds(0, 2));
		    prev = grp.add("CPU-sequential", prev.pos("bl").adds(0, 2));
		    prev = grp.add("GPU-sequential", prev.pos("bl").adds(0, 2));
		    prev = add(new Label("\u2193 Worse performance, better latency"), prev.pos("bl").adds(0, 2));
		    grp.check(prefs.syncmode.val.ordinal());
		    done[0] = true;
		}
		/* XXXRENDER
		composer.add(new CheckBox("Antialiasing") {
			{a = cf.fsaa.val;}

			public void set(boolean val) {
			    try {
				cf.fsaa.set(val);
			    } catch(GLSettings.SettingException e) {
				error(e.getMessage());
				return;
			    }
			    a = val;
			    cf.dirty = true;
			}
		    });
		composer.add(new Label("Anisotropic filtering"));
		if(cf.anisotex.max() <= 1) {
		    composer.add(new Label("(Not supported)"));
		} else {
		    final Label dpy = new Label("");
		    composer.addRow(
			    new HSlider(UI.scale(160), (int)(cf.anisotex.min() * 2), (int)(cf.anisotex.max() * 2), (int)(cf.anisotex.val * 2)) {
			    protected void added() {
				dpy();
			    }
			    void dpy() {
				if(val < 2)
				    dpy.settext("Off");
				else
				    dpy.settext(String.format("%.1f\u00d7", (val / 2.0)));
			    }
			    public void changed() {
				try {
				    cf.anisotex.set(val / 2.0f);
				} catch(GLSettings.SettingException e) {
				    error(e.getMessage());
				    return;
				}
				dpy();
				cf.dirty = true;
			    }
			},
			dpy
		    );
		}
		*/
		add(new Button(UI.scale(200), "Reset to defaults", false).action(() -> {
			    ui.setgprefs(GSettings.defaults());
			    curcf.destroy();
			    curcf = null;
		}), prev.pos("bl").adds(-5, 5));
		pack();
	    }
	}

	public void draw(GOut g) {
	    if((curcf == null) || (ui.gprefs != curcf.prefs))
		resetcf();
	    super.draw(g);
	}

	private void resetcf() {
	    if(curcf != null)
		curcf.destroy();
	    curcf = add(new CPanel(ui.gprefs), 0, 0);
	    back.move(curcf.pos("bl").adds(0, 15));
	    pack();
	}
    }

	public static HSlider instrumentsSoundVolumeSlider;
	public static HSlider clapSoundVolumeSlider;
	public static HSlider quernSoundVolumeSlider;
	public static HSlider cauldronSoundVolumeSlider;
	public static HSlider squeakSoundVolumeSlider;
	public static HSlider butcherSoundVolumeSlider;
	public static HSlider whiteDuckCapSoundVolumeSlider;
	private final int audioSliderWidth = 220;
	public static HSlider themeSongVolumeSlider;

    public class AudioPanel extends Panel {
	public AudioPanel(Panel back) {
		Widget leftColumn, rightColumn;
		leftColumn = add(new Label("Master audio volume"), 179, 0);
		leftColumn = add(new HSlider(UI.scale(460), 0, 1000, (int)(Audio.volume * 1000)) {
		    public void changed() {
			Audio.setvolume(val / 1000.0);
		    }
		}, leftColumn.pos("bl").adds(0, 2).x(0));

		leftColumn = add(new Label("In-game event volume (Sound FX)"), leftColumn.pos("bl").adds(0, 15));
		leftColumn = add(new HSlider(UI.scale(audioSliderWidth), 0, 1000, 0) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = (int)(ui.audio.pos.volume * 1000);
			}
			public void changed() {
				ui.audio.pos.setvolume(val / 1000.0);
			}
		}, leftColumn.pos("bl").adds(0, 2));


		leftColumn = add(new Label("Background Music Volume (Custom Client)"), leftColumn.pos("bl").adds(0, 5));
		leftColumn = add(themeSongVolumeSlider = new HSlider(UI.scale(220), 0, 100, Utils.getprefi("themeSongVolume", 40)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() { // ND: I hate hardcoding stuff but OH WELL
				if (LoginScreen.mainThemeClip != null) ((Audio.VolAdjust) LoginScreen.mainThemeClip).vol = val/100d;
				if (LoginScreen.charSelectThemeClip != null) ((Audio.VolAdjust) LoginScreen.charSelectThemeClip).vol = val/100d;
				if (GameUI.cabinThemeClip != null) ((Audio.VolAdjust) GameUI.cabinThemeClip).vol = val/100d;
				if (GameUI.caveThemeClip != null) ((Audio.VolAdjust) GameUI.caveThemeClip).vol = val/100d;
				if (GameUI.fishingThemeClip != null) ((Audio.VolAdjust) GameUI.fishingThemeClip).vol = val/100d;
				if (GameUI.hookahThemeClip != null) ((Audio.VolAdjust) GameUI.hookahThemeClip).vol = val/100d;
				if (GameUI.feastingThemeClip != null) ((Audio.VolAdjust) GameUI.feastingThemeClip).vol = val/100d;

				if (LoginScreen.themeSongVolumeSlider != null) LoginScreen.themeSongVolumeSlider.val = val;
				if (Charlist.themeSongVolumeSlider != null) Charlist.themeSongVolumeSlider.val = val;
				Utils.setprefi("themeSongVolume", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		leftColumn = add(new Label("Background Music Theme:"), leftColumn.pos("bl").adds(0, 6).x(0));
		List<String> musicThemes = Arrays.asList("Hurricane  ", "Legacy");
		add(new OldDropBox<String>(musicThemes.size(), musicThemes) {
			{
				super.change(musicThemes.get(Utils.getprefi("backgroundMusicTheme", 0)));
			}
			@Override
			protected String listitem(int i) {
				return musicThemes.get(i);
			}
			@Override
			protected int listitems() {
				return musicThemes.size();
			}
			@Override
			protected void drawitem(GOut g, String item, int i) {
				g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
			}
			@Override
			public void change(String item) {
				super.change(item);
				for (int i = 0; i < musicThemes.size(); i++){
					if (item.equals(musicThemes.get(i))){
						Utils.setprefi("backgroundMusicTheme", i);
					}
				}
				GameUI.settingStopAllThemes();
			}
		}, leftColumn.pos("ur").adds(0, 1));

		rightColumn = add(new Label("Ambient volume"), UI.scale(240, 51));
		rightColumn = add(new HSlider(UI.scale(audioSliderWidth), 0, 1000, 0) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = (int)(ui.audio.amb.volume * 1000);
			}
			public void changed() {
				ui.audio.amb.setvolume(val / 1000.0);
			}
		}, rightColumn.pos("bl").adds(0, 2));


		rightColumn = add(new Label("Interface sound volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = add(new HSlider(UI.scale(audioSliderWidth), 0, 1000, 0) {
			protected void attach(UI ui) {
				super.attach(ui);
				val = (int)(ui.audio.aui.volume * 1000);
			}
			public void changed() {
				ui.audio.aui.setvolume(val / 1000.0);
			}
		}, rightColumn.pos("bl").adds(0, 2));

	    leftColumn = add(new Label("Audio latency"), leftColumn.pos("bl").adds(195, 15));
		leftColumn.tooltip = audioLatencyTooltip;
	    {
		Label dpy = new Label("");
		addhlp(leftColumn.pos("bl").adds(0, 2).x(0), UI.scale(5),
			leftColumn = new HSlider(UI.scale(460-40), Math.round(Audio.fmt.getSampleRate() * 0.05f), Math.round(Audio.fmt.getSampleRate() / 4), Audio.bufsize()) {
			   protected void added() {
			   dpy();
			   }
			   void dpy() {
			   dpy.settext(Math.round((this.val * 1000) / Audio.fmt.getSampleRate()) + " ms");
			   }
			   public void changed() {
			   Audio.bufsize(val, true);
			   dpy();
			   }
		   	}, dpy);
			leftColumn.tooltip = audioLatencyTooltip;
	    }

		leftColumn = add(new Label("Other Sound Settings"), leftColumn.pos("bl").adds(177, 20));

		leftColumn = add(new Label("Boiling Cauldron Volume (Requires Reload)"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = add(cauldronSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("cauldronSoundVolume", 25)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("cauldronSoundVolume", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(new Label("Squeak Sound Volume (Roasting Spit, etc.)"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = add(squeakSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("squeakSoundVolume", 25)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("squeakSoundVolume", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(new Label("Butchering Sound Volume"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = add(butcherSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("butcherSoundVolume", 75)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("butcherSoundVolume", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(new Label("Quern Sound Effect Volume"), leftColumn.pos("bl").adds(0, 5).x(0));
		leftColumn = add(quernSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("quernSoundVolume", 10)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("quernSoundVolume", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		rightColumn = add(new Label("Music Instruments Volume"), rightColumn.pos("bl").adds(0, 119));
		rightColumn = add(instrumentsSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("instrumentsSoundVolume", 70)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("instrumentsSoundVolume", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = add(new Label("Clap Sound Effect Volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = add(clapSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("clapSoundVolume", 10)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("clapSoundVolume", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));

		rightColumn = add(new Label("White Duck Cap Sound Volume"), rightColumn.pos("bl").adds(0, 5));
		rightColumn = add(whiteDuckCapSoundVolumeSlider = new HSlider(UI.scale(audioSliderWidth), 0, 100, Utils.getprefi("whiteDuckCapSoundVolume", 75)) {
			protected void attach(UI ui) {
				super.attach(ui);
			}
			public void changed() {
				Utils.setprefi("whiteDuckCapSoundVolume", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));

	    add(new PButton(UI.scale(200), "Back", 27, back, "Options            "), leftColumn.pos("bl").adds(0, 30));
	    pack();
	}
    }

	public static CheckBox simplifiedUIThemeCheckBox;
	public static CheckBox extendedMouseoverInfoCheckBox;
	public static CheckBox disableMenuGridHotkeysCheckBox;
	public static CheckBox alwaysOpenBeltOnLoginCheckBox;
	public static CheckBox showMapMarkerNamesCheckBox;
	public static CheckBox verticalContainerIndicatorsCheckBox;
	public static boolean expWindowLocationIsTop = Utils.getprefb("expWindowLocationIsTop", true);
	private static CheckBox showFramerateCheckBox;
	public static CheckBox snapWindowsBackInsideCheckBox;
	public static CheckBox dragWindowsInWhenResizingCheckBox;
	public static CheckBox showHoverInventoriesWhenHoldingShiftCheckBox;
	private CheckBox showQuickSlotsCheckBox;
	public static CheckBox showStudyReportHistoryCheckBox;
	public static CheckBox lockStudyReportCheckBox;
	public static CheckBox soundAlertForFinishedCuriositiesCheckBox;
	public static CheckBox alwaysShowCombatUIStaminaBarCheckBox;
	public static CheckBox alwaysShowCombatUIHealthBarCheckBox;
	public static CheckBox transparentQuestsObjectivesWindowCheckBox;
	public static HSlider mapZoomSpeedSlider;
	public static CheckBox alwaysOpenMiniStudyOnLoginCheckBox;
	public static HSlider mapIconsSizeSlider;

    public class InterfaceSettingsPanel extends Panel {
	public InterfaceSettingsPanel(Panel back) {
	    Widget leftColumn = add(new Label("Interface scale (requires restart)"), 0, 0);
		leftColumn.tooltip = interfaceScaleTooltip;
	    {
		Label dpy = new Label("");
		final double gran = 0.05;
		final double smin = 1, smax = Math.floor(UI.maxscale() / gran) * gran;
		final int steps = (int)Math.round((smax - smin) / gran);
		addhlp(leftColumn.pos("bl").adds(0, 4), UI.scale(5),
		       leftColumn = new HSlider(UI.scale(160), 0, steps, (int)Math.round(steps * (Utils.getprefd("uiscale", 1.0) - smin) / (smax - smin))) {
			       protected void added() {
				   dpy();
			       }
			       void dpy() {
				   dpy.settext(String.format("%.2f\u00d7", smin + (((double)this.val / steps) * (smax - smin))));
			       }
			       public void changed() {
				   double val = smin + (((double)this.val / steps) * (smax - smin));
				   Utils.setprefd("uiscale", val);
				   dpy();
			       }
			   },
		       dpy);
		leftColumn.tooltip = interfaceScaleTooltip;
	    }
		leftColumn = add(showFramerateCheckBox = new CheckBox("Show Framerate"){
			{a = (Utils.getprefb("showFramerate", true));}
			public void changed(boolean val) {
				GLPanel.Loop.showFramerate = val;
				Utils.setprefb("showFramerate", val);
			}
		}, leftColumn.pos("bl").adds(0, 18));
		showFramerateCheckBox.tooltip = showFramerateTooltip;
		leftColumn = add(snapWindowsBackInsideCheckBox = new CheckBox("Snap windows back when dragged out"){
			{a = (Utils.getprefb("snapWindowsBackInside", true));}
			public void changed(boolean val) {
				Utils.setprefb("snapWindowsBackInside", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		snapWindowsBackInsideCheckBox.tooltip = snapWindowsBackInsideTooltip;
		leftColumn = add(dragWindowsInWhenResizingCheckBox = new CheckBox("Drag windows in when resizing game"){
			{a = (Utils.getprefb("dragWindowsInWhenResizing", false));}
			public void changed(boolean val) {
				Utils.setprefb("dragWindowsInWhenResizing", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		dragWindowsInWhenResizingCheckBox.tooltip = dragWindowsInWhenResizingTooltip;
		leftColumn = add(showHoverInventoriesWhenHoldingShiftCheckBox = new CheckBox("Show Hover-Inventories (Stacks, Belt, etc.) only when holding Shift"){
			{a = (Utils.getprefb("showHoverInventoriesWhenHoldingShift", true));}
			public void changed(boolean val) {
				Utils.setprefb("showHoverInventoriesWhenHoldingShift", val);
			}
		}, leftColumn.pos("bl").adds(0, 12));
		leftColumn = add(showQuickSlotsCheckBox = new CheckBox("Enable Quick Slots Widget"){
			{a = (Utils.getprefb("showQuickSlotsBar", true));}
			public void changed(boolean val) {
				Utils.setprefb("showQuickSlotsBar", val);
				if (ui != null && ui.gui != null && ui.gui.quickslots != null){
					ui.gui.quickslots.show(val);
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		showQuickSlotsCheckBox.tooltip = showQuickSlotsTooltip;
		leftColumn = add(showStudyReportHistoryCheckBox = new CheckBox("Show Study Report History"){
			{a = (Utils.getprefb("showStudyReportHistory", true));}
			public void set(boolean val) {
				SAttrWnd.showStudyReportHistoryCheckBox.a = val;
				Utils.setprefb("showStudyReportHistory", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 12));
		showStudyReportHistoryCheckBox.tooltip = showStudyReportHistoryTooltip;
		leftColumn = add(lockStudyReportCheckBox = new CheckBox("Lock Study Report"){
			{a = (Utils.getprefb("lockStudyReport", false));}
			public void set(boolean val) {
				SAttrWnd.lockStudyReportCheckBox.a = val;
				Utils.setprefb("lockStudyReport", val);
				a = val;
			}
		}, leftColumn.pos("bl").adds(0, 2));
		lockStudyReportCheckBox.tooltip = lockStudyReportTooltip;
		leftColumn = add(soundAlertForFinishedCuriositiesCheckBox = new CheckBox("Sound Alert for Finished Curiosities"){
			{a = (Utils.getprefb("soundAlertForFinishedCuriosities", false));}
			public void set(boolean val) {
				SAttrWnd.soundAlertForFinishedCuriositiesCheckBox.a = val;
				Utils.setprefb("soundAlertForFinishedCuriosities", val);
				a = val;
				if (val) {
					try {
						File file = new File(haven.MainFrame.gameDir + "res/customclient/sfx/CurioFinished.wav");
						if (file.exists()) {
							AudioInputStream in = AudioSystem.getAudioInputStream(file);
							AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
							AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
							Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
							((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 0.8));
						}
					} catch (Exception e) {
					}
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		soundAlertForFinishedCuriositiesCheckBox.tooltip = soundAlertForFinishedCuriositiesTooltip;

		leftColumn = add(alwaysOpenMiniStudyOnLoginCheckBox = new CheckBox("Always Open Mini-Study on Login"){
			{a = (Utils.getprefb("alwaysOpenMiniStudyOnLogin", false));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysOpenMiniStudyOnLogin", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));

		leftColumn = add(alwaysShowCombatUIStaminaBarCheckBox = new CheckBox("Always Show Combat UI Stamina Bar"){
			{a = (Utils.getprefb("alwaysShowCombatUIStaminaBar", false));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysShowCombatUIStaminaBar", val);
			}
		}, leftColumn.pos("bl").adds(0, 12));
		alwaysShowCombatUIStaminaBarCheckBox.tooltip = alwaysShowCombatUiBarTooltip;
		leftColumn = add(alwaysShowCombatUIHealthBarCheckBox = new CheckBox("Always Show Combat UI Health Bar"){
			{a = (Utils.getprefb("alwaysShowCombatUIHealthBar", false));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysShowCombatUIHealthBar", val);
			}
		}, leftColumn.pos("bl").adds(0, 2));
		alwaysShowCombatUIHealthBarCheckBox.tooltip = alwaysShowCombatUiBarTooltip;

		leftColumn = add(transparentQuestsObjectivesWindowCheckBox = new CheckBox("Transparent Quests Objectives Window"){
			{a = (Utils.getprefb("transparentQuestsObjectivesWindow", false));}
			public void changed(boolean val) {
				Utils.setprefb("transparentQuestsObjectivesWindow", val);
				if (ui != null && ui.gui != null && ui.gui.questObjectivesWindow != null && ui.gui.questObjectivesWindow.visible()) {
					ui.gui.questObjectivesWindow.resetDeco();
				}
			}
		}, leftColumn.pos("bl").adds(0, 2));
		transparentQuestsObjectivesWindowCheckBox.tooltip = transparentQuestsObjectivesWindowTooltip;

		Widget rightColumn;
		rightColumn = add(simplifiedUIThemeCheckBox = new CheckBox("Simplified UI Theme"){
			{a = (Utils.getprefb("simplifiedUITheme", false));}
			public void changed(boolean val) {
				Utils.setprefb("simplifiedUITheme", val);
				Window.bg = (!val ? Resource.loadtex("gfx/hud/wnd/lg/bg") : Resource.loadtex("customclient/simplifiedUI/wnd/bg"));
				Window.cl =  (!val ? Resource.loadtex("gfx/hud/wnd/lg/cl") : Resource.loadtex("customclient/simplifiedUI/wnd/cl"));
				Window.br = (!val ? Resource.loadtex("gfx/hud/wnd/lg/br") : Resource.loadtex("customclient/simplifiedUI/wnd/br"));
				Button.bl = (!val ? Resource.loadsimg("gfx/hud/buttons/tbtn/left") : Resource.loadsimg("customclient/simplifiedUI/buttons/tbtn/left"));
				Button.br = (!val ? Resource.loadsimg("gfx/hud/buttons/tbtn/right") : Resource.loadsimg("customclient/simplifiedUI/buttons/tbtn/right"));
				Button.bt = (!val ? Resource.loadsimg("gfx/hud/buttons/tbtn/top") : Resource.loadsimg("customclient/simplifiedUI/buttons/tbtn/top"));
				Button.bb = (!val ? Resource.loadsimg("gfx/hud/buttons/tbtn/bottom") : Resource.loadsimg("customclient/simplifiedUI/buttons/tbtn/bottom"));
				Button.dt = (!val ? Resource.loadsimg("gfx/hud/buttons/tbtn/dtex") : Resource.loadsimg("customclient/simplifiedUI/buttons/tbtn/dtex"));
				Button.ut = (!val ? Resource.loadsimg("gfx/hud/buttons/tbtn/utex") : Resource.loadsimg("customclient/simplifiedUI/buttons/tbtn/utex"));
				Button.bm = (!val ? Resource.loadsimg("gfx/hud/buttons/tbtn/mid") : Resource.loadsimg("customclient/simplifiedUI/buttons/tbtn/mid"));
				if (simpleUIFuture != null)
					simpleUIFuture.cancel(true);
				simpleUIChanged = true;
				simpleUIFuture = simpleUIExecutor.scheduleWithFixedDelay(OptWnd.this::resetSimpleUIChanged, 2, 3, TimeUnit.SECONDS);
			}
		}, UI.scale(230, 2));
		simplifiedUIThemeCheckBox.tooltip = simplifiedUIThemeCheckBoxTooltip;
		rightColumn = add(extendedMouseoverInfoCheckBox = new CheckBox("Extended Mouseover Info (Dev)"){
			{a = (Utils.getprefb("extendedMouseoverInfo", false));}
			public void changed(boolean val) {
				Utils.setprefb("extendedMouseoverInfo", val);
			}
		}, rightColumn.pos("bl").adds(0, 4));
		extendedMouseoverInfoCheckBox.tooltip = extendedMouseoverInfoTooltip;
		rightColumn = add(disableMenuGridHotkeysCheckBox = new CheckBox("Disable All Menu Grid Hotkeys"){
			{a = (Utils.getprefb("disableMenuGridHotkeys", false));}
			public void changed(boolean val) {
				Utils.setprefb("disableMenuGridHotkeys", val);
			}
		}, rightColumn.pos("bl").adds(0, 15));
		disableMenuGridHotkeysCheckBox.tooltip = disableMenuGridHotkeysTooltip;
		rightColumn = add(alwaysOpenBeltOnLoginCheckBox = new CheckBox("Always Open Belt on Login"){
			{a = (Utils.getprefb("alwaysOpenBeltOnLogin", true));}
			public void changed(boolean val) {
				Utils.setprefb("alwaysOpenBeltOnLogin", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		alwaysOpenBeltOnLoginCheckBox.tooltip = alwaysOpenBeltOnLoginTooltip;
		rightColumn = add(showMapMarkerNamesCheckBox = new CheckBox("Show Map Marker Names"){
			{a = (Utils.getprefb("showMapMarkerNames", true));}
			public void changed(boolean val) {
				Utils.setprefb("showMapMarkerNames", val);
			}
		}, rightColumn.pos("bl").adds(0, 2));
		showMapMarkerNamesCheckBox.tooltip = showMapMarkerNamesTooltip;
		rightColumn = add(verticalContainerIndicatorsCheckBox = new CheckBox("Vertical Container Indicators"){
			{a = (Utils.getprefb("verticalContainerIndicators", true));}
			public void changed(boolean val) {
				Utils.setprefb("verticalContainerIndicators", val);
			}
		}, rightColumn.pos("bl").adds(0, 32));
		verticalContainerIndicatorsCheckBox.tooltip = verticalContainerIndicatorsTooltip;
		Label expWindowLocationLabel;
		rightColumn = add(expWindowLocationLabel = new Label("Experience Event Window Location:"), rightColumn.pos("bl").adds(0, 11));{
			RadioGroup expWindowGrp = new RadioGroup(this) {
				public void changed(int btn, String lbl) {
					try {
						if(btn==0) {
							Utils.setprefb("expWindowLocationIsTop", true);
							expWindowLocationIsTop = true;
						}
						if(btn==1) {
							Utils.setprefb("expWindowLocationIsTop", false);
							expWindowLocationIsTop = false;
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			rightColumn = expWindowGrp.add("Top", rightColumn.pos("bl").adds(26, 3));
			rightColumn = expWindowGrp.add("Bottom", rightColumn.pos("ur").adds(30, 0));
			if (Utils.getprefb("expWindowLocationIsTop", true)){
				expWindowGrp.check(0);
			} else {
				expWindowGrp.check(1);
			}
		}
		expWindowLocationLabel.tooltip = experienceWindowLocationTooltip;

		rightColumn = add(new Label("Map Window Zoom Speed:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(230)));
		rightColumn = add(mapZoomSpeedSlider = new HSlider(UI.scale(110), 10, 30, Utils.getprefi("mapZoomSpeed", 15)) {
			public void changed() {
				Utils.setprefi("mapZoomSpeed", val);
			}
		}, rightColumn.pos("bl").adds(0, 4));
		add(new Button(UI.scale(60), "Reset", false).action(() -> {
			mapZoomSpeedSlider.val = 15;
			Utils.setprefi("mapZoomSpeed", 15);
		}), rightColumn.pos("ur").adds(6, -4)).tooltip = resetButtonTooltip;

		rightColumn = add(new Label("Map Icons Size:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(230)));
		rightColumn = add(mapIconsSizeSlider = new HSlider(UI.scale(110), 16, 40, Utils.getprefi("mapIconsSize", 20)) {
			public void changed() {
				Utils.setprefi("mapIconsSize", val);
				GobIcon.size = UI.scale(val);
				synchronized(GobIcon.Image.cache) {
					GobIcon.Image.cache.clear();
				}
				BufferedImage buf = MiniMap.plpImg.img;
				buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
				Coord tsz;
				if(buf.getWidth() > buf.getHeight())
					tsz = new Coord(GobIcon.size, (GobIcon.size * buf.getHeight()) / buf.getWidth());
				else
					tsz = new Coord((GobIcon.size * buf.getWidth()) / buf.getHeight(), GobIcon.size);
				buf = PUtils.convolve(buf, tsz, GobIcon.filter);
				MiniMap.plp = new TexI(buf);
			}
		}, rightColumn.pos("bl").adds(0, 4));
		add(new Button(UI.scale(60), "Reset", false).action(() -> {
			mapIconsSizeSlider.val = 20;
			GobIcon.size = UI.scale(20);
			synchronized(GobIcon.Image.cache) {
				GobIcon.Image.cache.clear();
			}
			Utils.setprefi("mapIconsSize", 20);
			BufferedImage buf = MiniMap.plpImg.img;
			buf = PUtils.rasterimg(PUtils.blurmask2(buf.getRaster(), 1, 1, Color.BLACK));
			Coord tsz;
			if(buf.getWidth() > buf.getHeight())
				tsz = new Coord(GobIcon.size, (GobIcon.size * buf.getHeight()) / buf.getWidth());
			else
				tsz = new Coord((GobIcon.size * buf.getWidth()) / buf.getHeight(), GobIcon.size);
			buf = PUtils.convolve(buf, tsz, GobIcon.filter);
			MiniMap.plp = new TexI(buf);
		}), rightColumn.pos("ur").adds(6, -4)).tooltip = resetButtonTooltip;

		Widget backButton;
		add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 30).x(0));
	    pack();
		centerBackButton(backButton, this);
	}
    }

	public static CheckBox holdCTRLtoRemoveActionButtonsCheckBox;

	public class ActionBarsSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public ActionBarsSettingsPanel(Panel back) {
			Widget prev;
			prev = add(new Label("Enabled Action Bars:"), 0, 0);
			add(new Label("Action Bar Orientation:"), prev.pos("ur").adds(42, 0));
			prev = add(new CheckBox("Action Bar 1"){
				{a = Utils.getprefb("showActionBar1", true);}
				public void changed(boolean val) {
					Utils.setprefb("showActionBar1", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar1 != null){
						ui.gui.actionBar1.show(val);
					}
				}
			}, prev.pos("bl").adds(12, 6));
			addOrientationRadio(prev, "actionBar1Horizontal", 1);
			prev = add(new CheckBox("Action Bar 2"){
				{a = Utils.getprefb("showActionBar2", false);}
				public void changed(boolean val) {
					Utils.setprefb("showActionBar2", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar2 != null){
						ui.gui.actionBar2.show(val);
					}
				}
			}, prev.pos("bl").adds(0, 2));
			addOrientationRadio(prev, "actionBar2Horizontal", 2);
			prev = add(new CheckBox("Action Bar 3"){
				{a = Utils.getprefb("showActionBar3", false);}
				public void changed(boolean val) {
					Utils.setprefb("showActionBar3", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar3 != null){
						ui.gui.actionBar3.show(val);
					}
				}
			}, prev.pos("bl").adds(0, 2));
			addOrientationRadio(prev, "actionBar3Horizontal", 3);
			prev = add(new CheckBox("Action Bar 4"){
				{a = Utils.getprefb("showActionBar4", false);}
				public void changed(boolean val) {
					Utils.setprefb("showActionBar4", val);
					if (ui != null && ui.gui != null && ui.gui.actionBar4 != null){
						ui.gui.actionBar4.show(val);
					}
				}
			}, prev.pos("bl").adds(0, 2));
			addOrientationRadio(prev, "actionBar4Horizontal", 4);

			prev = add(holdCTRLtoRemoveActionButtonsCheckBox = new CheckBox("Hold CTRL when right-clicking to remove action buttons"){
				{a = (Utils.getprefb("holdCTRLtoRemoveActionButtons", false));}
				public void changed(boolean val) {
					Utils.setprefb("holdCTRLtoRemoveActionButtons", val);
				}
			}, prev.pos("bl").adds(0, 12).x(0));

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(290, 380))), prev.pos("bl").adds(0,10).x(0));
			Widget cont = scroll.cont;
			int y = 0;
			y = cont.adda(new Label("Action Bar 1 Keybinds"), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar1.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar1[i], y);
			y = cont.adda(new Label("Action Bar 2 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar2.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar2[i], y);
			y = cont.adda(new Label("Action Bar 3 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar3.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar3[i], y);
			y = cont.adda(new Label("Action Bar 4 Keybinds"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
			for (int i = 0; i < GameUI.kb_actbar4.length; i++)
				y = addbtn(cont, String.format("Button - %d", i + 1), GameUI.kb_actbar4[i], y);
			adda(new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), scroll.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
			pack();
		}

		private void addOrientationRadio(Widget prev, String prefName, int actionBarNumber){
			RadioGroup radioGroup = new RadioGroup(this) {
				public void changed(int btn, String lbl) {
					try {
						if(btn==0) {
							Utils.setprefb(prefName, true);
							if (ui != null && ui.gui != null){
								GameUI.ActionBar actionBar = ui.gui.getActionBar(actionBarNumber);
								actionBar.setActionBarHorizontal(true);
							}
						}
						if(btn==1) {
							Utils.setprefb(prefName, false);
							if (ui != null && ui.gui != null){
								GameUI.ActionBar actionBar = ui.gui.getActionBar(actionBarNumber);
								actionBar.setActionBarHorizontal(false);
							}
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			Widget prevOption = radioGroup.add("Horizontal", prev.pos("ur").adds(40, 0));
			radioGroup.add("Vertical", prevOption.pos("ur").adds(10, 0));
			if (Utils.getprefb(prefName, true)){
				radioGroup.check(0);
			} else {
				radioGroup.check(1);
			}
		}
	}

	public static CheckBox showCombatHotkeysUICheckBox;
	public static CheckBox showDamagePredictUICheckBox;
	public static CheckBox singleRowCombatMovesCheckBox;
	public static CheckBox includeHHPTextHealthBarCheckBox;
	public static ColorOptionWidget greenCombatColorOptionWidget;
	public static String[] greenCombatColorSetting = Utils.getprefsa("greenCombat" + "_colorSetting", new String[]{"0", "128", "3", "255"});
	public static ColorOptionWidget blueCombatColorOptionWidget;
	public static String[] blueCombatColorSetting = Utils.getprefsa("blueCombat" + "_colorSetting", new String[]{"39", "82", "191", "255"});
	public static ColorOptionWidget yellowCombatColorOptionWidget;
	public static String[] yellowCombatColorSetting = Utils.getprefsa("yellowCombat" + "_colorSetting", new String[]{"217", "177", "20", "255"});
	public static ColorOptionWidget redCombatColorOptionWidget;
	public static String[] redCombatColorSetting = Utils.getprefsa("redCombat" + "_colorSetting", new String[]{"192", "28", "28", "255"});
	public static CheckBox showCombatOpeningsAsLettersCheckBox;
	public static ColorOptionWidget myIPCombatColorOptionWidget;
	public static String[] myIPCombatColorSetting = Utils.getprefsa("myIPCombat" + "_colorSetting", new String[]{"0", "201", "4", "255"});
	public static ColorOptionWidget enemyIPCombatColorOptionWidget;
	public static String[] enemyIPCombatColorSetting = Utils.getprefsa("enemyIPCombat" + "_colorSetting", new String[]{"245", "0", "0", "255"});
	public static CheckBox showEstimatedAgilityTextCheckBox;
	public static CheckBox drawFloatingCombatDataCheckBox;
	public static CheckBox drawFloatingCombatDataOnCurrentTargetCheckBox;
	public static CheckBox drawFloatingCombatDataOnOthersCheckBox;
	public static CheckBox showCombatManeuverCombatInfoCheckBox;
	public static CheckBox onlyShowOpeningsAbovePercentageCombatInfoCheckBox;
	public static CheckBox onlyShowCoinsAbove4CombatInfoCheckBox;
	public static CheckBox drawFloatingCombatOpeningsAboveYourselfCheckBox;
	public static TextEntry minimumOpeningTextEntry;
	public static HSlider combatUITopPanelHeightSlider;
	public static HSlider combatUIBottomPanelHeightSlider;
	public static CheckBox toggleGobDamageInfoCheckBox;
	public static CheckBox toggleGobDamageWoundInfoCheckBox;
	public static CheckBox toggleGobDamageArmorInfoCheckBox;
	public static Button damageInfoClearButton;
	public static CheckBox yourselfDamageInfoCheckBox;
	public static CheckBox partyMembersDamageInfoCheckBox;
	public static boolean stamBarLocationIsTop = Utils.getprefb("stamBarLocationIsTop", true);
	public static CheckBox highlightPartyMembersCheckBox;
	public static CheckBox showCirclesUnderPartyMembersCheckBox;
	public static ColorOptionWidget yourselfPartyColorOptionWidget;
	public static String[] yourselfPartyColorSetting = Utils.getprefsa("yourselfParty" + "_colorSetting", new String[]{"255", "255", "255", "128"});
	public static ColorOptionWidget leaderPartyColorOptionWidget;
	public static String[] leaderPartyColorSetting = Utils.getprefsa("leaderParty" + "_colorSetting", new String[]{"0", "74", "208", "164"});
	public static ColorOptionWidget memberPartyColorOptionWidget;
	public static String[] memberPartyColorSetting = Utils.getprefsa("memberParty" + "_colorSetting", new String[]{"0", "160", "0", "164"});
	public static CheckBox highlightCombatFoesCheckBox;
	public static CheckBox showCirclesUnderCombatFoesCheckBox;
	public static ColorOptionWidget combatFoeColorOptionWidget;
	public static String[] combatFoeColorSetting = Utils.getprefsa("combatFoe" + "_colorSetting", new String[]{"180", "0", "0", "196"});
	public static boolean refreshCurrentTargetSpriteColor = false;
	public static HSlider targetSpriteSizeSlider;
	public static CheckBox drawChaseVectorsCheckBox;
	public static CheckBox drawYourCurrentPathCheckBox;
	public static CheckBox showYourCombatRangeCirclesCheckBox;
	public static boolean refreshMyUnarmedRange = false;
	public static boolean refreshMyWeaponRange = false;
	public static ColorOptionWidget unarmedCombatRangeColorOptionWidget;
	public static String[] unarmedCombatRangeColorSetting = Utils.getprefsa("unarmedCombatRange" + "_colorSetting", new String[]{"0", "160", "0", "255"});
	public static ColorOptionWidget weaponCombatRangeColorOptionWidget;
	public static String[] weaponCombatRangeColorSetting = Utils.getprefsa("weaponCombatRange" + "_colorSetting", new String[]{"130", "0", "172", "255"});

	public class CombatSettingsPanel extends Panel {
		public CombatSettingsPanel(Panel back) {
			Widget leftColumn, rightColumn;

			leftColumn = add(new Label("Top panel height:"), 0, 0);
			leftColumn = add(combatUITopPanelHeightSlider = new HSlider(UI.scale(200), 36, 480, Utils.getprefi("combatTopPanelHeight", 400)) {
				public void changed() {
					Utils.setprefi("combatTopPanelHeight", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				combatUITopPanelHeightSlider.val = 400;
				Utils.setprefi("combatTopPanelHeight", 400);
			}), leftColumn.pos("bl").adds(210, -20)).tooltip = resetButtonTooltip;
			leftColumn = add(new Label("Bottom panel height:"), leftColumn.pos("bl").adds(0, 10));
			leftColumn = add(combatUIBottomPanelHeightSlider = new HSlider(UI.scale(200), 10, 480, Utils.getprefi("combatBottomPanelHeight", 100)) {
				public void changed() {
					Utils.setprefi("combatBottomPanelHeight", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				combatUIBottomPanelHeightSlider.val = 100;
				Utils.setprefi("combatBottomPanelHeight", 100);
			}), leftColumn.pos("bl").adds(210, -20)).tooltip = resetButtonTooltip;

			leftColumn = add(showEstimatedAgilityTextCheckBox = new CheckBox("Show Target Estimated Agility (Top Panel)"){
				{a = Utils.getprefb("showEstimatedAgility", true);}
				public void changed(boolean val) {
					Utils.setprefb("showEstimatedAgility", val);
				}
			}, leftColumn.pos("bl").adds(0, 12).x(0));

			leftColumn = add(includeHHPTextHealthBarCheckBox = new CheckBox("Include HHP% text in Health Bar (Top Panel)"){
				{a = Utils.getprefb("includeHHPTextHealthBar", false);}
				public void changed(boolean val) {
					Utils.setprefb("includeHHPTextHealthBar", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));

			leftColumn = add(new Label("Stamina Bar Location:"), leftColumn.pos("bl").adds(0, 10));{
				RadioGroup expWindowGrp = new RadioGroup(this) {
					public void changed(int btn, String lbl) {
						try {
							if(btn==0) {
								Utils.setprefb("stamBarLocationIsTop", true);
								stamBarLocationIsTop = true;
							}
							if(btn==1) {
								Utils.setprefb("stamBarLocationIsTop", false);
								stamBarLocationIsTop = false;
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
				leftColumn = expWindowGrp.add("Top Panel", leftColumn.pos("bl").adds(20, 3));
				leftColumn = expWindowGrp.add("Bottom Panel", leftColumn.pos("ur").adds(30, 0));
				if (Utils.getprefb("stamBarLocationIsTop", true)){
					expWindowGrp.check(0);
				} else {
					expWindowGrp.check(1);
				}
			}

			leftColumn = add(showCombatHotkeysUICheckBox = new CheckBox("Show Combat Move Hotkeys (Bottom Panel)"){
				{a = Utils.getprefb("showCombatHotkeysUI", true);}
				public void changed(boolean val) {
					Utils.setprefb("showCombatHotkeysUI", val);
				}
			}, leftColumn.pos("bl").adds(0, 12).xs(0));
			leftColumn = add(singleRowCombatMovesCheckBox = new CheckBox("Single Row for Combat Moves (Bottom Panel)"){
				{a = Utils.getprefb("singleRowCombatMoves", false);}
				public void set(boolean val) {
					Utils.setprefb("singleRowCombatMoves", val);
					a = val;
				}
			}, leftColumn.pos("bl").adds(0, 2));
			singleRowCombatMovesCheckBox.tooltip = singleRowCombatMovesTooltip;
			leftColumn = add(showDamagePredictUICheckBox = new CheckBox("Show Combat Damage Prediction (Bottom Panel)"){
				{a = Utils.getprefb("showDamagePredictUI", true);}
				public void changed(boolean val) {
					Utils.setprefb("showDamagePredictUI", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			showDamagePredictUICheckBox.tooltip = showDamagePredictUITooltip;

			leftColumn = add(drawFloatingCombatOpeningsAboveYourselfCheckBox = new CheckBox("Display Combat Openings above Yourself"){
				{a = Utils.getprefb("drawFloatingCombatDataAboveYourself", true);}
				public void changed(boolean val) {
					Utils.setprefb("drawFloatingCombatDataAboveYourself", val);
				}
			}, leftColumn.pos("bl").adds(0, 18).x(0));

			leftColumn = add(drawFloatingCombatDataCheckBox = new CheckBox("Display Combat Data above Combat Foes:"){
				{a = Utils.getprefb("drawFloatingCombatData", true);}
				public void changed(boolean val) {
					Utils.setprefb("drawFloatingCombatData", val);
				}
			}, leftColumn.pos("bl").adds(0, 4));
			add(new Label(" >"), leftColumn.pos("bl").adds(0, 4).xs(0));
			leftColumn = add(drawFloatingCombatDataOnCurrentTargetCheckBox = new CheckBox("Show on Current Target"){
				{a = Utils.getprefb("drawFloatingCombatDataOnCurrentTarget", true);}
				public void changed(boolean val) {
					Utils.setprefb("drawFloatingCombatDataOnCurrentTarget", val);
				}
			}, leftColumn.pos("bl").adds(20, 4));
			add(new Label(" >"), leftColumn.pos("bl").adds(0, 3).xs(0));
			leftColumn = add(drawFloatingCombatDataOnOthersCheckBox = new CheckBox("Show on other Combat Foes"){
				{a = Utils.getprefb("drawFloatingCombatDataOnOthers", true);}
				public void changed(boolean val) {
					Utils.setprefb("drawFloatingCombatDataOnOthers", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			add(new Label(" >"), leftColumn.pos("bl").adds(0, 3).xs(0));
			leftColumn = add(showCombatManeuverCombatInfoCheckBox = new CheckBox("Show Combat Stance/Maneuver"){
				{a = Utils.getprefb("showCombatManeuverCombatInfo", true);}
				public void changed(boolean val) {
					Utils.setprefb("showCombatManeuverCombatInfo", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			add(new Label(" >"), leftColumn.pos("bl").adds(0, 3).xs(0));
			leftColumn = add(onlyShowOpeningsAbovePercentageCombatInfoCheckBox = new CheckBox("Only show openings when higher than:"){
				{a = Utils.getprefb("onlyShowOpeningsAbovePercentage", false);}
				public void changed(boolean val) {
					Utils.setprefb("onlyShowOpeningsAbovePercentage", val);
				}
			}, leftColumn.pos("bl").adds(0, 4));
			onlyShowOpeningsAbovePercentageCombatInfoCheckBox.tooltip = onlyShowOpeningsAbovePercentageCombatInfoTooltip;
			add(minimumOpeningTextEntry = new TextEntry(UI.scale(40), Utils.getpref("minimumOpening", "30")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", "")); // Only numbers
					this.settext(this.text().replaceAll("(?<=^.{2}).*", "")); // No more than 2 digits
					Utils.setpref("minimumOpening", this.buf.line());
					super.changed();
				}
			}, leftColumn.pos("ur").adds(10, 0));
			add(new Label(" >"), leftColumn.pos("bl").adds(0, 2).xs(0));

			leftColumn = add(onlyShowCoinsAbove4CombatInfoCheckBox = new CheckBox("Only show coins when higher than 4"){
				{a = Utils.getprefb("onlyShowCoinsAbove4", false);}
				public void changed(boolean val) {
					Utils.setprefb("onlyShowCoinsAbove4", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));

			leftColumn = add(toggleGobDamageInfoCheckBox = new CheckBox("Display Damage Info:"){
				{a = Utils.getprefb("GobDamageInfoToggled", true);}
				public void changed(boolean val) {
					Utils.setprefb("GobDamageInfoToggled", val);
				}
			}, leftColumn.pos("bl").adds(0, 10).xs(0));
			leftColumn = add(new Label("> Include:"), leftColumn.pos("bl").adds(0, 1).xs(0));
			leftColumn = add(toggleGobDamageWoundInfoCheckBox = new CheckBox("Wounds"){
				{a = Utils.getprefb("GobDamageInfoWoundsToggled", true);}
				public void changed(boolean val) {
					Utils.setprefb("GobDamageInfoWoundsToggled", val);
				}
			}, leftColumn.pos("bl").adds(56, -17));
			toggleGobDamageWoundInfoCheckBox.lbl = Text.create("Wounds", PUtils.strokeImg(Text.std.render("Wounds", new Color(255, 232, 0, 255))));
			leftColumn = add(toggleGobDamageArmorInfoCheckBox = new CheckBox("Armor"){
				{a = Utils.getprefb("GobDamageInfoArmorToggled", true);}
				public void changed(boolean val) {
					Utils.setprefb("GobDamageInfoArmorToggled", val);
				}
			}, leftColumn.pos("bl").adds(66, -18));
			toggleGobDamageArmorInfoCheckBox.lbl = Text.create("Armor", PUtils.strokeImg(Text.std.render("Armor", new Color(50, 255, 92, 255))));
			add(damageInfoClearButton = new Button(UI.scale(70), "Clear", false).action(() -> {
				GobDamageInfo.clearAllDamage(ui.gui);
				if (ui != null && ui.gui != null) {
					ui.gui.optionInfoMsg("All Combat Damage Info has been CLEARED!", msgYellow, Audio.resclip(Toggle.sfxoff));
				}
			}), leftColumn.pos("ur").adds(37, -16));
			damageInfoClearButton.tooltip = damageInfoClearTooltip;
			leftColumn = add(new Label("> Also show on:"), leftColumn.pos("bl").adds(0, 2).xs(0));
			leftColumn = add(yourselfDamageInfoCheckBox = new CheckBox("Yourself"){
				{a = Utils.getprefb("yourselfDamageInfo", true);}
				public void changed(boolean val) {
					Utils.setprefb("yourselfDamageInfo", val);
				}
			}, leftColumn.pos("bl").adds(80, -17));
			leftColumn = add(partyMembersDamageInfoCheckBox = new CheckBox("Party Members"){
				{a = Utils.getprefb("(partyMembersDamageInfo", true);}
				public void changed(boolean val) {
					Utils.setprefb("(partyMembersDamageInfo", val);
				}
			}, leftColumn.pos("ur").adds(6, 0));

			rightColumn = add(showCombatOpeningsAsLettersCheckBox = new CheckBox("Show Combat Openings as Colored Letters"){
				{a = Utils.getprefb("showCombatOpeningsAsLetters", false);}
				public void changed(boolean val) {
					Utils.setprefb("showCombatOpeningsAsLetters", val);
				}
			}, UI.scale(320, 0));
			showCombatOpeningsAsLettersCheckBox.tooltip = showCombatOpeningsAsLettersTooltip;

			rightColumn = add(new Label("Combat Openings Colors:"), rightColumn.pos("bl").adds(0, 4));
			rightColumn = add(new Label("Green"), rightColumn.pos("bl").adds(2, 1));
			add(greenCombatColorOptionWidget = new ColorOptionWidget("", "greenCombat", 0,
					Integer.parseInt(greenCombatColorSetting[0]), Integer.parseInt(greenCombatColorSetting[1]), Integer.parseInt(greenCombatColorSetting[2]), Integer.parseInt(greenCombatColorSetting[3]), (Color col) -> {
				improvedOpeningsImageColor.put("paginae/atk/offbalance", OptWnd.greenCombatColorOptionWidget.currentColor);
			}){}, rightColumn.pos("bl").adds(6, 0));
			rightColumn = add(new Label("Blue"), rightColumn.pos("ur").adds(8, 0));
			add(blueCombatColorOptionWidget = new ColorOptionWidget("", "blueCombat", 0,
					Integer.parseInt(blueCombatColorSetting[0]), Integer.parseInt(blueCombatColorSetting[1]), Integer.parseInt(blueCombatColorSetting[2]), Integer.parseInt(blueCombatColorSetting[3]), (Color col) -> {
				improvedOpeningsImageColor.put("paginae/atk/dizzy", OptWnd.blueCombatColorOptionWidget.currentColor);
			}){}, rightColumn.pos("bl").adds(2, 0));
			rightColumn = add(new Label("Yellow"), rightColumn.pos("ur").adds(6, 0));
			add(yellowCombatColorOptionWidget = new ColorOptionWidget("", "yellowCombat", 0,
					Integer.parseInt(yellowCombatColorSetting[0]), Integer.parseInt(yellowCombatColorSetting[1]), Integer.parseInt(yellowCombatColorSetting[2]), Integer.parseInt(yellowCombatColorSetting[3]), (Color col) -> {
				improvedOpeningsImageColor.put("paginae/atk/reeling", OptWnd.yellowCombatColorOptionWidget.currentColor);
			}){}, rightColumn.pos("bl").adds(8, 0));
			rightColumn = add(new Label("Red"), rightColumn.pos("ur").adds(8, 0));
			rightColumn = add(redCombatColorOptionWidget = new ColorOptionWidget("", "redCombat", 0,
					Integer.parseInt(redCombatColorSetting[0]), Integer.parseInt(redCombatColorSetting[1]), Integer.parseInt(redCombatColorSetting[2]), Integer.parseInt(redCombatColorSetting[3]), (Color col) -> {
				improvedOpeningsImageColor.put("paginae/atk/cornered", OptWnd.redCombatColorOptionWidget.currentColor);
			}){}, rightColumn.pos("bl").adds(1, 0));
			rightColumn = add(new Button(UI.scale(70), "Reset All", false).action(() -> {
				Utils.setprefsa("greenCombat" + "_colorSetting", new String[]{"0", "128", "3", "255"});
				Utils.setprefsa("blueCombat" + "_colorSetting", new String[]{"39", "82", "191", "255"});
				Utils.setprefsa("yellowCombat" + "_colorSetting", new String[]{"217", "177", "20", "255"});
				Utils.setprefsa("redCombat" + "_colorSetting", new String[]{"192", "28", "28", "255"});
				greenCombatColorOptionWidget.cb.colorChooser.setColor(greenCombatColorOptionWidget.currentColor = new Color(0, 128, 3, 255));
				blueCombatColorOptionWidget.cb.colorChooser.setColor(blueCombatColorOptionWidget.currentColor = new Color(39, 82, 191, 255));
				yellowCombatColorOptionWidget.cb.colorChooser.setColor(yellowCombatColorOptionWidget.currentColor = new Color(217, 177, 20, 255));
				redCombatColorOptionWidget.cb.colorChooser.setColor(redCombatColorOptionWidget.currentColor = new Color(192, 28, 28, 255));
				improvedOpeningsImageColor.put("paginae/atk/offbalance", OptWnd.greenCombatColorOptionWidget.currentColor);
				improvedOpeningsImageColor.put("paginae/atk/dizzy", OptWnd.blueCombatColorOptionWidget.currentColor);
				improvedOpeningsImageColor.put("paginae/atk/reeling", OptWnd.yellowCombatColorOptionWidget.currentColor);
				improvedOpeningsImageColor.put("paginae/atk/cornered", OptWnd.redCombatColorOptionWidget.currentColor);
			}), rightColumn.pos("ur").adds(21, -2));
			improvedOpeningsImageColor.put("paginae/atk/offbalance", OptWnd.greenCombatColorOptionWidget.currentColor);
			improvedOpeningsImageColor.put("paginae/atk/dizzy", OptWnd.blueCombatColorOptionWidget.currentColor);
			improvedOpeningsImageColor.put("paginae/atk/reeling", OptWnd.yellowCombatColorOptionWidget.currentColor);
			improvedOpeningsImageColor.put("paginae/atk/cornered", OptWnd.redCombatColorOptionWidget.currentColor);

			rightColumn = add(new Label("Combat IP (Coins) Colors:"), rightColumn.pos("bl").adds(0, 10).xs(320));
			rightColumn = add(new Label("Your IP"), rightColumn.pos("bl").adds(20, 1));
			add(myIPCombatColorOptionWidget = new ColorOptionWidget("", "myIPCombat", 0,
					Integer.parseInt(myIPCombatColorSetting[0]), Integer.parseInt(myIPCombatColorSetting[1]), Integer.parseInt(myIPCombatColorSetting[2]), Integer.parseInt(myIPCombatColorSetting[3]), (Color col) -> {
			}){}, rightColumn.pos("bl").adds(8, 0));
			rightColumn = add(new Label("Enemy IP"), rightColumn.pos("ur").adds(24, 0));
			rightColumn = add(enemyIPCombatColorOptionWidget = new ColorOptionWidget("", "enemyIPCombat", 0,
					Integer.parseInt(enemyIPCombatColorSetting[0]), Integer.parseInt(enemyIPCombatColorSetting[1]), Integer.parseInt(enemyIPCombatColorSetting[2]), Integer.parseInt(enemyIPCombatColorSetting[3]), (Color col) -> {
			}){}, rightColumn.pos("bl").adds(12, 0));

			rightColumn = add(new Button(UI.scale(70), "Reset All", false).action(() -> {
				Utils.setprefsa("myIPCombat" + "_colorSetting", new String[]{"0", "201", "4", "255"});
				Utils.setprefsa("enemyIPCombat" + "_colorSetting", new String[]{"245", "0", "0", "255"});
				myIPCombatColorOptionWidget.cb.colorChooser.setColor(myIPCombatColorOptionWidget.currentColor = new Color(0, 201, 4, 255));
				enemyIPCombatColorOptionWidget.cb.colorChooser.setColor(enemyIPCombatColorOptionWidget.currentColor = new Color(245, 0, 0, 255));
			}), rightColumn.pos("ur").adds(46, -2));

			rightColumn = add(highlightPartyMembersCheckBox = new CheckBox("Highlight Party Members"){
				{a = Utils.getprefb("highlightPartyMembers", false);}
				public void changed(boolean val) {
					Utils.setprefb("highlightPartyMembers", val);
					if (ui != null && ui.gui != null && ui.gui.map != null && ui.gui.map.partyHighlight != null)
						ui.gui.map.partyHighlight.update();
				}
			}, rightColumn.pos("bl").adds(0, 16).xs(320));
			highlightPartyMembersCheckBox.tooltip = highlightPartyMembersTooltip;
			rightColumn = add(showCirclesUnderPartyMembersCheckBox = new CheckBox("Show Circles under Party Members"){
				{a = Utils.getprefb("showCirclesUnderPartyMembers", true);}
				public void changed(boolean val) {
					Utils.setprefb("showCirclesUnderPartyMembers", val);
					if (ui != null && ui.gui != null && ui.gui.map != null && ui.gui.map.partyCircles != null)
						ui.gui.map.partyCircles.update();
				}
			}, rightColumn.pos("bl").adds(0, 2));
			showCirclesUnderPartyMembersCheckBox.tooltip = showCirclesUnderPartyMembersTooltip;

			rightColumn = add(yourselfPartyColorOptionWidget = new ColorOptionWidget("Yourself (Party Color):", "yourselfParty", 120, Integer.parseInt(yourselfPartyColorSetting[0]), Integer.parseInt(yourselfPartyColorSetting[1]), Integer.parseInt(yourselfPartyColorSetting[2]), Integer.parseInt(yourselfPartyColorSetting[3]), (Color col) -> {
				PartyHighlight.YOURSELF_OL_COLOR = col;
				PartyCircles.YOURSELF_OL_COLOR = col;
			}){}, rightColumn.pos("bl").adds(6, 2));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("yourselfParty" + "_colorSetting", new String[]{"255", "255", "255", "128"});
				yourselfPartyColorOptionWidget.cb.colorChooser.setColor(yourselfPartyColorOptionWidget.currentColor = new Color(255, 255, 255, 128));
				PartyHighlight.YOURSELF_OL_COLOR = yourselfPartyColorOptionWidget.currentColor;
				PartyCircles.YOURSELF_OL_COLOR = yourselfPartyColorOptionWidget.currentColor;
			}), yourselfPartyColorOptionWidget.pos("ur").adds(16, 0)).tooltip = resetButtonTooltip;
			rightColumn = add(leaderPartyColorOptionWidget = new ColorOptionWidget("Leader (Party Color):", "leaderParty", 120, Integer.parseInt(leaderPartyColorSetting[0]), Integer.parseInt(leaderPartyColorSetting[1]), Integer.parseInt(leaderPartyColorSetting[2]), Integer.parseInt(leaderPartyColorSetting[3]), (Color col) -> {
				PartyHighlight.LEADER_OL_COLOR = col;
				PartyCircles.LEADER_OL_COLOR = col;
			}){}, rightColumn.pos("bl").adds(0, 4));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("leaderParty" + "_colorSetting", new String[]{"0", "74", "208", "164"});
				leaderPartyColorOptionWidget.cb.colorChooser.setColor(leaderPartyColorOptionWidget.currentColor = new Color(0, 74, 208, 164));
				PartyHighlight.LEADER_OL_COLOR = leaderPartyColorOptionWidget.currentColor;
				PartyCircles.LEADER_OL_COLOR = leaderPartyColorOptionWidget.currentColor;
			}), leaderPartyColorOptionWidget.pos("ur").adds(16, 0)).tooltip = resetButtonTooltip;
			rightColumn = add(memberPartyColorOptionWidget = new ColorOptionWidget("Member (Party Color):", "memberParty", 120, Integer.parseInt(memberPartyColorSetting[0]), Integer.parseInt(memberPartyColorSetting[1]), Integer.parseInt(memberPartyColorSetting[2]), Integer.parseInt(memberPartyColorSetting[3]), (Color col) -> {
				PartyHighlight.MEMBER_OL_COLOR = col;
				PartyCircles.MEMBER_OL_COLOR = col;
			}){}, rightColumn.pos("bl").adds(0, 4));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("memberParty" + "_colorSetting", new String[]{"0", "160", "0", "164"});
				memberPartyColorOptionWidget.cb.colorChooser.setColor(memberPartyColorOptionWidget.currentColor = new Color(0, 160, 0, 164));
				PartyHighlight.MEMBER_OL_COLOR = memberPartyColorOptionWidget.currentColor;
				PartyCircles.MEMBER_OL_COLOR = memberPartyColorOptionWidget.currentColor;
			}), memberPartyColorOptionWidget.pos("ur").adds(16, 0)).tooltip = resetButtonTooltip;

			rightColumn = add(highlightCombatFoesCheckBox = new CheckBox("Highlight Combat Foes"){
				{a = Utils.getprefb("highlightCombatFoes", false);}
				public void changed(boolean val) {
					Utils.setprefb("highlightCombatFoes", val);
				}
			}, rightColumn.pos("bl").adds(0, 12).xs(320));
			highlightCombatFoesCheckBox.tooltip = highlightCombatFoesTooltip;
			rightColumn = add(showCirclesUnderCombatFoesCheckBox = new CheckBox("Show Circles under Combat Foes"){
				{a = Utils.getprefb("showCirclesUnderCombatFoes", true);}
				public void changed(boolean val) {
					Utils.setprefb("showCirclesUnderCombatFoes", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			showCirclesUnderCombatFoesCheckBox.tooltip = showCirclesUnderCombatFoesTooltip;

			rightColumn = add(combatFoeColorOptionWidget = new ColorOptionWidget("Combat Foes:", "combatFoes", 120, Integer.parseInt(combatFoeColorSetting[0]), Integer.parseInt(combatFoeColorSetting[1]), Integer.parseInt(combatFoeColorSetting[2]), Integer.parseInt(combatFoeColorSetting[3]), (Color col) -> {
				GobCombatHighlight.COMBAT_FOE_MIXCOLOR = new MixColor(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
				AggroCircleSprite.COMBAT_FOE_COLOR = col;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::removeCombatFoeCircleOverlay);
					ui.sess.glob.oc.gobAction(Gob::removeCombatFoeHighlight);
				}
				haven.sprites.CurrentAggroSprite.col = new BaseColor(col);
				refreshCurrentTargetSpriteColor = true;
			}){}, rightColumn.pos("bl").adds(6, 2));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("combatFoes" + "_colorSetting", new String[]{"180", "0", "0", "196"});
				combatFoeColorOptionWidget.cb.colorChooser.setColor(combatFoeColorOptionWidget.currentColor = new Color(180, 0, 0, 196));
				GobCombatHighlight.COMBAT_FOE_MIXCOLOR = new MixColor(combatFoeColorOptionWidget.currentColor.getRed(), combatFoeColorOptionWidget.currentColor.getGreen(), combatFoeColorOptionWidget.currentColor.getBlue(), combatFoeColorOptionWidget.currentColor.getAlpha());
				AggroCircleSprite.COMBAT_FOE_COLOR = combatFoeColorOptionWidget.currentColor;
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::removeCombatFoeCircleOverlay);
					ui.sess.glob.oc.gobAction(Gob::removeCombatFoeHighlight);
				}
				haven.sprites.CurrentAggroSprite.col = new BaseColor(combatFoeColorOptionWidget.currentColor);
				refreshCurrentTargetSpriteColor = true;
			}), combatFoeColorOptionWidget.pos("ur").adds(16, 0)).tooltip = resetButtonTooltip;

			rightColumn = add(new Label("Target Sprite Size:"), rightColumn.pos("bl").adds(0, 4).xs(325));
			rightColumn.tooltip = targetSpriteTooltip;
			rightColumn = add(targetSpriteSizeSlider = new HSlider(UI.scale(110), 3, 7, Utils.getprefi("targetSpriteSize", 5)) {
				public void changed() {
					Utils.setprefi("targetSpriteSize", val);
					haven.sprites.CurrentAggroSprite.size = val;
					refreshCurrentTargetSpriteColor = true;
				}
			}, rightColumn.pos("ur").adds(26, 4));
			targetSpriteSizeSlider.tooltip = targetSpriteTooltip;

			rightColumn = add(drawChaseVectorsCheckBox = new CheckBox("Draw Chase Vectors"){
				{a = Utils.getprefb("drawChaseVectors", true);}
				public void changed(boolean val) {
					Utils.setprefb("drawChaseVectors", val);
				}
			}, rightColumn.pos("bl").adds(0, 12).xs(320));
			drawChaseVectorsCheckBox.tooltip = drawChaseVectorsTooltip;
			rightColumn = add(drawYourCurrentPathCheckBox = new CheckBox("Draw Your Current Path"){
				{a = Utils.getprefb("drawYourCurrentPath", false);}
				public void changed(boolean val) {
					Utils.setprefb("drawYourCurrentPath", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			drawYourCurrentPathCheckBox.tooltip = drawYourCurrentPathTooltip;
			rightColumn = add(showYourCombatRangeCirclesCheckBox = new CheckBox("Show Your Combat Range Circles"){
				{a = Utils.getprefb("showYourCombatRangeCircles", false);}
				public void changed(boolean val) {
					Utils.setprefb("showYourCombatRangeCircles", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			showYourCombatRangeCirclesCheckBox.tooltip = showYourCombatRangeCirclesTooltip;
			rightColumn = add(new Label("Unarmed"), rightColumn.pos("bl").adds(16, 1));
			add(unarmedCombatRangeColorOptionWidget = new ColorOptionWidget("", "unarmedCombatRange", 0, Integer.parseInt(unarmedCombatRangeColorSetting[0]), Integer.parseInt(unarmedCombatRangeColorSetting[1]), Integer.parseInt(unarmedCombatRangeColorSetting[2]), Integer.parseInt(unarmedCombatRangeColorSetting[3]), (Color col) -> {
				refreshMyUnarmedRange = true;
			}){}, rightColumn.pos("bl").adds(12, 0));
			rightColumn = add(new Label("Weapon"), rightColumn.pos("ur").adds(20, 0));
			rightColumn = add(weaponCombatRangeColorOptionWidget = new ColorOptionWidget("", "weaponCombatRange", 0, Integer.parseInt(weaponCombatRangeColorSetting[0]), Integer.parseInt(weaponCombatRangeColorSetting[1]), Integer.parseInt(weaponCombatRangeColorSetting[2]), Integer.parseInt(weaponCombatRangeColorSetting[3]), (Color col) -> {
				refreshMyWeaponRange = true;
			}){}, rightColumn.pos("bl").adds(10, 0));
			rightColumn = add(new Button(UI.scale(70), "Reset All", false).action(() -> {
				Utils.setprefsa("unarmedCombatRange" + "_colorSetting", new String[]{"0", "160", "0", "255"});
				Utils.setprefsa("weaponCombatRange" + "_colorSetting", new String[]{"130", "0", "172", "255"});
				unarmedCombatRangeColorOptionWidget.cb.colorChooser.setColor(unarmedCombatRangeColorOptionWidget.currentColor = new Color(0, 160, 0, 255));
				weaponCombatRangeColorOptionWidget.cb.colorChooser.setColor(weaponCombatRangeColorOptionWidget.currentColor = new Color(130, 0, 172, 255));
				refreshMyUnarmedRange = true;
				refreshMyWeaponRange = true;
			}), rightColumn.pos("ur").adds(46, -2));
			

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}
	}


	public static CheckBox excludeGreenBuddyFromAggroCheckBox;
	public static CheckBox excludeRedBuddyFromAggroCheckBox;
	public static CheckBox excludeBlueBuddyFromAggroCheckBox;
	public static CheckBox excludeTealBuddyFromAggroCheckBox;
	public static CheckBox excludeYellowBuddyFromAggroCheckBox;
	public static CheckBox excludePurpleBuddyFromAggroCheckBox;
	public static CheckBox excludeOrangeBuddyFromAggroCheckBox;
	public static CheckBox excludeAllVillageOrRealmMembersFromAggroCheckBox;

	public class AggroExclusionSettingsPanel extends Panel {
		public AggroExclusionSettingsPanel(Panel back) {
			Widget prev;
			prev = add(new Label("Manually attacking will still work, regardless of these settings!"), 0, 0);
			prev = add(new Label("Select which Players should be excluded from Aggro Keybinds:"), prev.pos("bl").adds(0, 4));

			prev = add(excludeGreenBuddyFromAggroCheckBox = new CheckBox("Green Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeGreenBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeGreenBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 12));
			excludeGreenBuddyFromAggroCheckBox.lbl = Text.create("Green Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Green Memorised / Kinned Players", BuddyWnd.gc[1])));
			prev = add(excludeRedBuddyFromAggroCheckBox = new CheckBox("Red Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeRedBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeRedBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeRedBuddyFromAggroCheckBox.lbl = Text.create("Red Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Red Memorised / Kinned Players", BuddyWnd.gc[2])));
			prev = add(excludeBlueBuddyFromAggroCheckBox = new CheckBox("Blue Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeBlueBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeBlueBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeBlueBuddyFromAggroCheckBox.lbl = Text.create("Blue Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Blue Memorised / Kinned Players", BuddyWnd.gc[3])));
			prev = add(excludeTealBuddyFromAggroCheckBox = new CheckBox("Teal Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeTealBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeTealBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeTealBuddyFromAggroCheckBox.lbl = Text.create("Teal Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Teal Memorised / Kinned Players", BuddyWnd.gc[4])));
			prev = add(excludeYellowBuddyFromAggroCheckBox = new CheckBox("Yellow Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeYellowBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeYellowBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeYellowBuddyFromAggroCheckBox.lbl = Text.create("Yellow Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Yellow Memorised / Kinned Players", BuddyWnd.gc[5])));
			prev = add(excludePurpleBuddyFromAggroCheckBox = new CheckBox("Purple Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludePurpleBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludePurpleBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludePurpleBuddyFromAggroCheckBox.lbl = Text.create("Purple Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Purple Memorised / Kinned Players", BuddyWnd.gc[6])));
			prev = add(excludeOrangeBuddyFromAggroCheckBox = new CheckBox("Orange Memorised / Kinned Players"){
				{a = (Utils.getprefb("excludeOrangeBuddyFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeOrangeBuddyFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 4));
			excludeOrangeBuddyFromAggroCheckBox.lbl = Text.create("Orange Memorised / Kinned Players", PUtils.strokeImg(Text.std.render("Orange Memorised / Kinned Players", BuddyWnd.gc[7])));

			prev = add(excludeAllVillageOrRealmMembersFromAggroCheckBox = new CheckBox("ALL Village & Realm Members (Regardless of Memo/Kin)"){
				{a = (Utils.getprefb("excludeAllVillageOrRealmMembersFromAggro", false));}
				public void changed(boolean val) {
					Utils.setprefb("excludeAllVillageOrRealmMembersFromAggro", val);
				}
			}, prev.pos("bl").adds(0, 20));
			excludeAllVillageOrRealmMembersFromAggroCheckBox.lbl = Text.create("ALL Village & Realm Members (Regardless of Memo/Kin)", PUtils.strokeImg(Text.std.render("ALL Village & Realm Members (Regardless of Memo/Kin)", new Color(151, 17, 17, 255))));

			prev = add(new Label("PARTY MEMBERS ARE ALWAYS EXCLUDED!"), prev.pos("bl").adds(0, 20));

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}
	}

	public static CheckBox chatAlertSoundsCheckBox;
	public static CheckBox areaChatAlertSoundsCheckBox;
	public static CheckBox partyChatAlertSoundsCheckBox;
	public static CheckBox privateChatAlertSoundsCheckBox;

	public static TextEntry villageNameTextEntry;
	public static CheckBox villageChatAlertSoundsCheckBox;
	public static CheckBox autoSelectNewChatCheckBox;
	public static CheckBox removeRealmChatCheckBox;
	public static CheckBox showKinStatusChangeMessages;

	public static HSlider systemMessagesListSizeSlider;
	public static HSlider systemMessagesDurationSlider;

	public class ChatSettingsPanel extends Panel {
		public ChatSettingsPanel(Panel back) {
			Widget prev;

			prev = add(chatAlertSoundsCheckBox = new CheckBox("Enable chat message notification sounds"){
				{a = (Utils.getprefb("chatAlertSounds", true));}
				public void changed(boolean val) {
					Utils.setprefb("chatAlertSounds", val);
				}
			}, 0, 0);

			prev = add(areaChatAlertSoundsCheckBox = new CheckBox("Area Chat Sound"){
				{a = (Utils.getprefb("areaChatAlertSounds", true));}
				public void changed(boolean val) {
					Utils.setprefb("areaChatAlertSounds", val);
				}
			}, prev.pos("bl").adds(20, 4));

			prev = add(privateChatAlertSoundsCheckBox = new CheckBox("Private Messages Sound"){
				{a = (Utils.getprefb("privateChatAlertSounds", true));}
				public void changed(boolean val) {
					Utils.setprefb("privateChatAlertSounds", val);
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(partyChatAlertSoundsCheckBox = new CheckBox("Party Chat Sound"){
				{a = (Utils.getprefb("partyChatAlertSounds", true));}
				public void changed(boolean val) {
					Utils.setprefb("partyChatAlertSounds", val);
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(villageChatAlertSoundsCheckBox = new CheckBox("Village Chat Sound"){
				{
					a = (Utils.getprefb("villageChatAlertSounds", true));
					tooltip = RichText.render("You must set a Village Name below, for this setting to properly work." +
							"\n" +
							"\nIf you don't set a village name, the sound alert will always trigger if chat message notification sounds are enabled.", UI.scale(300));
				}
				public void changed(boolean val) {
					Utils.setprefb("villageChatAlertSounds", val);
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(new Label("Village Name:"), prev.pos("bl").adds(20, 4));
			add(villageNameTextEntry = new TextEntry(UI.scale(100), Utils.getpref("villageNameForChatAlerts", "")){
				protected void changed() {
					Utils.setpref("villageNameForChatAlerts", this.buf.line());
					super.changed();
				}}, prev.pos("ur").adds(6, 0));

			prev = add(autoSelectNewChatCheckBox = new CheckBox("Auto-select new chats"){
				{a = (Utils.getprefb("autoSelectNewChat", true));}
				public void changed(boolean val) {
					Utils.setprefb("autoSelectNewChat", val);
				}
			}, prev.pos("bl").adds(0, 4).x(0));

			prev = add(removeRealmChatCheckBox = new CheckBox("Remove public realm chat (requires relog)"){
				{a = (Utils.getprefb("removeRealmChat", false));}
				public void changed(boolean val) {
					Utils.setprefb("removeRealmChat", val);
				}
			}, prev.pos("bl").adds(0, 4));

			prev = add(showKinStatusChangeMessages = new CheckBox("Show kin status system messages"){
				{a = (Utils.getprefb("showKinStatusChangeMessages", true));}
				public void changed(boolean val) {
					Utils.setprefb("showKinStatusChangeMessages", val);
				}
			}, prev.pos("bl").adds(0, 4));
			prev = add(new Label("System Messages List Size: "), prev.pos("bl").adds(0, 5));
			Label systemMessagesListSizeLabel = new Label(Utils.getprefi("systemMessagesListSize", 5) + " rows");
			add(systemMessagesListSizeLabel, prev.pos("ur").adds(0, 0));
			prev = add(systemMessagesListSizeSlider = new HSlider(UI.scale(230), 1, 10, Utils.getprefi("systemMessagesListSize", 5)) {
				public void changed() {
					Utils.setprefi("systemMessagesListSize", val);
					systemMessagesListSizeLabel.settext(val + " rows");
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(new Label("System Messages Duration: "), prev.pos("bl").adds(0, 5));
			Label systemMessagesDurationLabel = new Label(Utils.getprefi("systemMessagesDuration", 4) + (Utils.getprefi("systemMessagesDuration", 5) > 1 ? " seconds" : " second"));
			add(systemMessagesDurationLabel, prev.pos("ur").adds(0, 0));
			prev = add(systemMessagesDurationSlider = new HSlider(UI.scale(230), 3, 10, Utils.getprefi("systemMessagesDuration", 4)) {
				public void changed() {
					Utils.setprefi("systemMessagesDuration", val);
					systemMessagesDurationLabel.settext(val + (val > 1 ? " seconds" : " second"));
				}
			}, prev.pos("bl").adds(0, 2));

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}
	}


	public static CheckBox showObjectCollisionBoxesCheckBox;
	public static ColorOptionWidget collisionBoxColorOptionWidget;
	public static String[] collisionBoxColorSetting = Utils.getprefsa("collisionBox" + "_colorSetting", new String[]{"255", "255", "255", "210"});
	public static CheckBox displayObjectDurabilityPercentageCheckBox;
	public static CheckBox displayObjectQualityOnInspectionCheckBox;
	public static CheckBox displayGrowthInfoCheckBox;
	public static CheckBox alsoShowOversizedTreesAbovePercentageCheckBox;
	public static TextEntry oversizedTreesPercentageTextEntry;
	public static CheckBox showCritterAurasCheckBox;
	public static ColorOptionWidget rabbitAuraColorOptionWidget;
	public static String[] rabbitAuraColorSetting = Utils.getprefsa("rabbitAura" + "_colorSetting", new String[]{"88", "255", "0", "140"});
	public static ColorOptionWidget genericCritterAuraColorOptionWidget;
	public static String[] genericCritterAuraColorSetting = Utils.getprefsa("genericCritterAura" + "_colorSetting", new String[]{"193", "0", "255", "140"});
	public static CheckBox showSpeedBuffAurasCheckBox;
	public static ColorOptionWidget speedBuffAuraColorOptionWidget;
	public static String[] speedBuffAuraColorSetting = Utils.getprefsa("speedBuffAura" + "_colorSetting", new String[]{"255", "255", "255", "140"});
	public static CheckBox showMidgesCircleAurasCheckBox;
	public static CheckBox showDangerousBeastRadiiCheckBox;
	public static CheckBox showBeeSkepsRadiiCheckBox;
	public static CheckBox showFoodTroughsRadiiCheckBox;
	public static CheckBox showMoundBedsRadiiCheckBox;
	public static CheckBox showBarrelContentsTextCheckBox;
	public static CheckBox showIconSignTextCheckBox;
	public static CheckBox showCheeseRacksTierTextCheckBox;
	public static CheckBox highlightCliffsCheckBox;
	public static ColorOptionWidget highlightCliffsColorOptionWidget;
	public static String[] highlightCliffsColorSetting = Utils.getprefsa("highlightCliffs" + "_colorSetting", new String[]{"255", "0", "0", "200"});
	public static CheckBox showContainerFullnessCheckBox;
	public static CheckBox showContainerFullnessFullCheckBox;
	public static ColorOptionWidget showContainerFullnessFullColorOptionWidget;
	public static String[] containerFullnessFullColorSetting = Utils.getprefsa("containerFullnessFull" + "_colorSetting", new String[]{"170", "0", "0", "170"});
	public static CheckBox showContainerFullnessPartialCheckBox;
	public static ColorOptionWidget showContainerFullnessPartialColorOptionWidget;
	public static String[] containerFullnessPartialColorSetting = Utils.getprefsa("containerFullnessPartial" + "_colorSetting", new String[]{"194", "155", "2", "140"});
	public static CheckBox showContainerFullnessEmptyCheckBox;
	public static ColorOptionWidget showContainerFullnessEmptyColorOptionWidget;
	public static String[] containerFullnessEmptyColorSetting = Utils.getprefsa("containerFullnessEmpty" + "_colorSetting", new String[]{"0", "120", "0", "180"});
	public static CheckBox showWorkstationProgressCheckBox;
	public static CheckBox showWorkstationProgressFinishedCheckBox;
	public static ColorOptionWidget showWorkstationProgressFinishedColorOptionWidget;
	public static String[] workstationProgressFinishedColorSetting = Utils.getprefsa("workstationProgressFinished" + "_colorSetting", new String[]{"170", "0", "0", "170"});
	public static CheckBox showWorkstationProgressInProgressCheckBox;
	public static ColorOptionWidget showWorkstationProgressInProgressColorOptionWidget;
	public static String[] workstationProgressInProgressColorSetting = Utils.getprefsa("workstationProgressInProgress" + "_colorSetting", new String[]{"194", "155", "2", "140"});
	public static CheckBox showWorkstationProgressReadyForUseCheckBox;
	public static ColorOptionWidget showWorkstationProgressReadyForUseColorOptionWidget;
	public static String[] workstationProgressReadyForUseColorSetting = Utils.getprefsa("workstationProgressReadyForUse" + "_colorSetting", new String[]{"0", "120", "0", "180"});
	public static CheckBox showWorkstationProgressUnpreparedCheckBox;
	public static ColorOptionWidget showWorkstationProgressUnpreparedColorOptionWidget;
	public static String[] workstationProgressUnpreparedColorSetting = Utils.getprefsa("workstationProgressUnprepared" + "_colorSetting", new String[]{"20", "20", "20", "180"});
	public static CheckBox showMineSupportRadiiCheckBox;
	public static CheckBox showMineSupportSafeTilesCheckBox;
	public static CheckBox enableMineSweeperCheckBox;
	public static OldDropBox<Integer> sweeperDurationDropbox;
	public static final List<Integer> sweeperDurations = Arrays.asList(5, 10, 15, 30, 45, 60, 120);
	public static int sweeperSetDuration = Utils.getprefi("sweeperSetDuration", 1);

	public static ColorOptionWidget areaChatPingColorOptionWidget;
	public static String[] areaChatPingColorSetting = Utils.getprefsa("areaChatPing" + "_colorSetting", new String[]{"255", "183", "0", "255"});
	public static ColorOptionWidget partyChatPingColorOptionWidget;
	public static String[] partyChatPingColorSetting = Utils.getprefsa("partyChatPing" + "_colorSetting", new String[]{"243", "0", "0", "255"});
	public static CheckBox showObjectsSpeedCheckBox;
	public static CheckBox showTreesBushesHarvestIconsCheckBox;
	public static CheckBox showLowFoodWaterIconsCheckBox;
	public static CheckBox showBeeSkepsHarvestIconsCheckBox;

	public static CheckBox objectPermanentHighlightingCheckBox;

	public class DisplaySettingsPanel extends Panel {
		public DisplaySettingsPanel(Panel back) {
			Widget leftColumn;
			Widget middleColumn;
			Widget rightColumn;
			leftColumn = add(new Label("Object fine-placement granularity"), 0, 0);
			{
				Label pos = add(new Label("Position"), leftColumn.pos("bl").adds(5, 4));
				pos.tooltip = granularityPositionTooltip;
				Label ang = add(new Label("Angle"), pos.pos("bl").adds(0, 4));
				ang.tooltip = granularityAngleTooltip;
				int x = Math.max(pos.pos("ur").x, ang.pos("ur").x);
				{
					Label dpy = new Label("");
					final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
					final int steps = (int)Math.round((smax - smin) / 0.25);
					int ival = (int)Math.round(MapView.plobpgran);
					addhlp(Coord.of(x + UI.scale(5), pos.c.y), UI.scale(5),
							leftColumn = new HSlider(UI.scale(155) - x, 2, 65, (ival == 0) ? 65 : ival) {
								protected void added() {
									dpy();
								}
								void dpy() {
									dpy.settext((this.val == 65) ? "\u221e" : Integer.toString(this.val));
								}
								public void changed() {
									Utils.setprefd("plobpgran", MapView.plobpgran = ((this.val == 65) ? 0 : this.val));
									dpy();
								}
							},
							dpy);
					leftColumn.tooltip = granularityPositionTooltip;
				}
				{
					Label dpy = new Label("");
					final double smin = 1, smax = Math.floor(UI.maxscale() / 0.25) * 0.25;
					final int steps = (int)Math.round((smax - smin) / 0.25);
					int[] vals = {4, 5, 6, 8, 9, 10, 12, 15, 18, 20, 24, 30, 36, 40, 45, 60, 72, 90, 120, 180, 360};
					int ival = 0;
					for(int i = 0; i < vals.length; i++) {
						if(Math.abs((MapView.plobagran * 2) - vals[i]) < Math.abs((MapView.plobagran * 2) - vals[ival]))
							ival = i;
					}
					addhlp(Coord.of(x + UI.scale(5), ang.c.y), UI.scale(5),
							leftColumn = new HSlider(UI.scale(155) - x, 0, vals.length - 1, ival) {
								protected void added() {
									dpy();
								}
								void dpy() {
									dpy.settext(String.format("%d\u00b0", 360 / vals[this.val]));
								}
								public void changed() {
									Utils.setprefd("plobagran", MapView.plobagran = (vals[this.val] / 2.0));
									dpy();
								}
							},
							dpy);
					leftColumn.tooltip = granularityAngleTooltip;
				}
			}
			leftColumn = add(highlightCliffsCheckBox = new CheckBox("Highlight Cliffs (Color Overlay)"){
				{a = (Utils.getprefb("highlightCliffs", false));}
				public void set(boolean val) {
					Utils.setprefb("highlightCliffs", val);
					a = val;
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Highlight Cliffs is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, leftColumn.pos("bl").adds(0, 12).x(0));
			highlightCliffsCheckBox.tooltip = highlightCliffsTooltip;
			leftColumn = add(highlightCliffsColorOptionWidget = new ColorOptionWidget("Highlight Cliffs Color:", "highlightCliffs", 115, Integer.parseInt(highlightCliffsColorSetting[0]), Integer.parseInt(highlightCliffsColorSetting[1]), Integer.parseInt(highlightCliffsColorSetting[2]), Integer.parseInt(highlightCliffsColorSetting[3]), (Color col) -> {
				Ridges.setCliffHighlightMat();
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
			}){}, leftColumn.pos("bl").adds(0, 1).x(0));

			leftColumn = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("highlightCliffs" + "_colorSetting", new String[]{"255", "0", "0", "200"});
				highlightCliffsColorOptionWidget.cb.colorChooser.setColor(highlightCliffsColorOptionWidget.currentColor = new Color(255, 0, 0, 200));
				Ridges.setCliffHighlightMat();
				if (ui.sess != null)
					ui.sess.glob.map.invalidateAll();
			}), leftColumn.pos("ur").adds(10, 0));
			leftColumn.tooltip = resetButtonTooltip;

			leftColumn = add(showContainerFullnessCheckBox = new CheckBox("Highlight Container Fullness:"){
				{a = (Utils.getprefb("showContainerFullness", true));}
				public void changed(boolean val) {
					Utils.setprefb("showContainerFullness", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
						ui.gui.map.updatePlobContainerHighlight();
					}
				}
			}, leftColumn.pos("bl").adds(0, 12).x(0));
			showContainerFullnessCheckBox.tooltip = showContainerFullnessTooltip;
			leftColumn = add(showContainerFullnessFullCheckBox = new CheckBox("Full"){
				{a = (Utils.getprefb("showContainerFullnessFull", true));}
				public void changed(boolean val) {
					Utils.setprefb("showContainerFullnessFull", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
						ui.gui.map.updatePlobContainerHighlight();
					}
				}
			}, leftColumn.pos("bl").adds(20, 4));
			add(showContainerFullnessFullColorOptionWidget = new ColorOptionWidget("", "containerFullnessFull", 0, Integer.parseInt(containerFullnessFullColorSetting[0]), Integer.parseInt(containerFullnessFullColorSetting[1]), Integer.parseInt(containerFullnessFullColorSetting[2]), Integer.parseInt(containerFullnessFullColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("containerFullnessFull" + "_colorSetting", new String[]{"170", "0", "0", "170"});
				showContainerFullnessFullColorOptionWidget.cb.colorChooser.setColor(showContainerFullnessFullColorOptionWidget.currentColor = new Color(170, 0, 0, 170));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}), showContainerFullnessFullColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			leftColumn = add(showContainerFullnessPartialCheckBox = new CheckBox("Partial"){
				{a = (Utils.getprefb("showContainerFullnessPartial", true));}
				public void changed(boolean val) {
					Utils.setprefb("showContainerFullnessPartial", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
						ui.gui.map.updatePlobContainerHighlight();
					}
				}
			}, leftColumn.pos("bl").adds(0, 8));
			add(showContainerFullnessPartialColorOptionWidget = new ColorOptionWidget("", "containerFullnessPartial", 0, Integer.parseInt(containerFullnessPartialColorSetting[0]), Integer.parseInt(containerFullnessPartialColorSetting[1]), Integer.parseInt(containerFullnessPartialColorSetting[2]), Integer.parseInt(containerFullnessPartialColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("containerFullnessPartial" + "_colorSetting", new String[]{"194", "155", "2", "140"});
				showContainerFullnessPartialColorOptionWidget.cb.colorChooser.setColor(showContainerFullnessPartialColorOptionWidget.currentColor = new Color(194, 155, 2, 140));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}), showContainerFullnessPartialColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			leftColumn = add(showContainerFullnessEmptyCheckBox = new CheckBox("Empty"){
				{a = (Utils.getprefb("showContainerFullnessEmpty", true));}
				public void changed(boolean val) {
					Utils.setprefb("showContainerFullnessEmpty", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
						ui.gui.map.updatePlobContainerHighlight();
					}
				}
			}, leftColumn.pos("bl").adds(0, 8));
			add(showContainerFullnessEmptyColorOptionWidget = new ColorOptionWidget("", "containerFullnessEmpty", 0, Integer.parseInt(containerFullnessEmptyColorSetting[0]), Integer.parseInt(containerFullnessEmptyColorSetting[1]), Integer.parseInt(containerFullnessEmptyColorSetting[2]), Integer.parseInt(containerFullnessEmptyColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));

			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("containerFullnessEmpty" + "_colorSetting", new String[]{"0", "120", "0", "180"});
				showContainerFullnessEmptyColorOptionWidget.cb.colorChooser.setColor(showContainerFullnessEmptyColorOptionWidget.currentColor = new Color(0, 120, 0, 180));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateContainerFullnessHighlight);
					ui.gui.map.updatePlobContainerHighlight();
				}
			}), showContainerFullnessEmptyColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			leftColumn = add(showWorkstationProgressCheckBox = new CheckBox("Highlight Workstation Progress:"){
				{a = (Utils.getprefb("showWorkstationProgress", true));}
				public void changed(boolean val) {
					Utils.setprefb("showWorkstationProgress", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
						ui.gui.map.updatePlobWorkstationProgressHighlight();
					}
				}
			}, leftColumn.pos("bl").adds(0, 12).x(0));
			showWorkstationProgressCheckBox.tooltip = showWorkstationProgressTooltip;
			leftColumn = add(showWorkstationProgressFinishedCheckBox = new CheckBox("Finished"){
				{a = (Utils.getprefb("showWorkstationProgressFinished", true));}
				public void changed(boolean val) {
					Utils.setprefb("showWorkstationProgressFinished", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
						ui.gui.map.updatePlobWorkstationProgressHighlight();
					}
				}
			}, leftColumn.pos("bl").adds(20, 4));
			add(showWorkstationProgressFinishedColorOptionWidget = new ColorOptionWidget("", "workstationProgressFinished", 0, Integer.parseInt(workstationProgressFinishedColorSetting[0]), Integer.parseInt(workstationProgressFinishedColorSetting[1]), Integer.parseInt(workstationProgressFinishedColorSetting[2]), Integer.parseInt(workstationProgressFinishedColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("workstationProgressFinished" + "_colorSetting", new String[]{"170", "0", "0", "170"});
				showWorkstationProgressFinishedColorOptionWidget.cb.colorChooser.setColor(showWorkstationProgressFinishedColorOptionWidget.currentColor = new Color(170, 0, 0, 170));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}), showWorkstationProgressFinishedColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			leftColumn = add(showWorkstationProgressInProgressCheckBox = new CheckBox("In progress"){
				{a = (Utils.getprefb("showWorkstationProgressInProgress", true));}
				public void changed(boolean val) {
					Utils.setprefb("showWorkstationProgressInProgress", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
						ui.gui.map.updatePlobWorkstationProgressHighlight();
					}
				}
			}, leftColumn.pos("bl").adds(0, 8));
			add(showWorkstationProgressInProgressColorOptionWidget = new ColorOptionWidget("", "workstationProgressInProgress", 0, Integer.parseInt(workstationProgressInProgressColorSetting[0]), Integer.parseInt(workstationProgressInProgressColorSetting[1]), Integer.parseInt(workstationProgressInProgressColorSetting[2]), Integer.parseInt(workstationProgressInProgressColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("workstationProgressInProgress" + "_colorSetting", new String[]{"194", "155", "2", "140"});
				showWorkstationProgressInProgressColorOptionWidget.cb.colorChooser.setColor(showWorkstationProgressInProgressColorOptionWidget.currentColor = new Color(194, 155, 2, 140));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}), showWorkstationProgressInProgressColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			leftColumn = add(showWorkstationProgressReadyForUseCheckBox = new CheckBox("Ready for use"){
				{a = (Utils.getprefb("showWorkstationProgressReadyForUse", true));}
				public void changed(boolean val) {
					Utils.setprefb("showWorkstationProgressReadyForUse", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
						ui.gui.map.updatePlobWorkstationProgressHighlight();
					}
				}
			}, leftColumn.pos("bl").adds(0, 8));
			add(showWorkstationProgressReadyForUseColorOptionWidget = new ColorOptionWidget("", "workstationProgressReadyForUse", 0, Integer.parseInt(workstationProgressReadyForUseColorSetting[0]), Integer.parseInt(workstationProgressReadyForUseColorSetting[1]), Integer.parseInt(workstationProgressReadyForUseColorSetting[2]), Integer.parseInt(workstationProgressReadyForUseColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("workstationProgressReadyForUse" + "_colorSetting", new String[]{"0", "120", "0", "180"});
				showWorkstationProgressReadyForUseColorOptionWidget.cb.colorChooser.setColor(showWorkstationProgressReadyForUseColorOptionWidget.currentColor = new Color(0, 120, 0, 180));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}), showWorkstationProgressReadyForUseColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			leftColumn = add(showWorkstationProgressUnpreparedCheckBox = new CheckBox("Unprepared"){
				{a = (Utils.getprefb("showWorkstationProgressUnprepared", true));}
				public void changed(boolean val) {
					Utils.setprefb("showWorkstationProgressUnprepared", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
						ui.gui.map.updatePlobWorkstationProgressHighlight();
					}
				}
			}, leftColumn.pos("bl").adds(0, 8));
			add(showWorkstationProgressUnpreparedColorOptionWidget = new ColorOptionWidget("", "workstationProgressUnprepared", 0, Integer.parseInt(workstationProgressUnpreparedColorSetting[0]), Integer.parseInt(workstationProgressUnpreparedColorSetting[1]), Integer.parseInt(workstationProgressUnpreparedColorSetting[2]), Integer.parseInt(workstationProgressUnpreparedColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}){}, leftColumn.pos("ur").adds(0, -3).x(UI.scale(115)));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("workstationProgressUnprepared" + "_colorSetting", new String[]{"20", "20", "20", "180"});
				showWorkstationProgressUnpreparedColorOptionWidget.cb.colorChooser.setColor(showWorkstationProgressUnpreparedColorOptionWidget.currentColor = new Color(20, 20, 20, 180));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateWorkstationProgressHighlight);
					ui.gui.map.updatePlobWorkstationProgressHighlight();
				}
			}), showWorkstationProgressUnpreparedColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			leftColumn = add(showMineSupportRadiiCheckBox = new CheckBox("Show Mine Support Radii"){
				{a = (Utils.getprefb("showMineSupportRadii", false));}
				public void set(boolean val) {
					Utils.setprefb("showMineSupportRadii", val);
					a = val;
					MSRad.show(val);
					if (ui != null && ui.gui != null){
						ui.sess.glob.oc.gobAction(Gob::updateMineLadderRadius);
						ui.gui.optionInfoMsg("Mine Support Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, leftColumn.pos("bl").adds(0, 12).x(0));
			showMineSupportRadiiCheckBox.tooltip = showMineSupportRadiiTooltip;
			leftColumn = add(showMineSupportSafeTilesCheckBox = new CheckBox("Show Mine Support Safe Tiles"){
				{a = (Utils.getprefb("showMineSupportTiles", false));}
				public void set(boolean val) {
					Utils.setprefb("showMineSupportTiles", val);
					a = val;
					if (ui != null && ui.gui != null){
						ui.sess.glob.oc.gobAction(Gob::updateSupportOverlays);
						ui.gui.optionInfoMsg("Mine Support Safe Tiles are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, leftColumn.pos("bl").adds(0, 2));
			showMineSupportSafeTilesCheckBox.tooltip = showMineSupportSafeTilesTooltip;
			leftColumn = add(enableMineSweeperCheckBox = new CheckBox("Enable Mine Sweeper"){
				{a = (Utils.getprefb("enableMineSweeper", true));}
				public void set(boolean val) {
					Utils.setprefb("enableMineSweeper", val);
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Mine Sweeper numbers are now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
						if (ui != null && ui.gui != null && ui.gui.miningSafetyAssistantWindow != null)
							ui.gui.miningSafetyAssistantWindow.enableMineSweeperCheckBox.a = val;
					}
					a = val;
				}
			}, leftColumn.pos("bl").adds(0, 12));
			enableMineSweeperCheckBox.tooltip = enableMineSweeperTooltip;
			leftColumn = add(new Label("Sweeper Display Duration (Min):"), leftColumn.pos("bl").adds(0, 2));
			leftColumn.tooltip = RichText.render("Use this to set how long you want the numbers to be displayed on the ground, in minutes. The numbers will be visible as long as the dust particle effect stays on the tile." +
					"\n" +
					"\n$col[218,163,0]{Note:} $col[185,185,185]{Changing this option will only affect the duration of newly spawned cave dust tiles. The duration is set once the wall tile is mined and the cave dust spawns in.}", UI.scale(300));
			add(sweeperDurationDropbox = new OldDropBox<Integer>(UI.scale(40), sweeperDurations.size(), UI.scale(17)) {
				{
					super.change(sweeperDurations.get(sweeperSetDuration));
				}
				@Override
				protected Integer listitem(int i) {
					return sweeperDurations.get(i);
				}
				@Override
				protected int listitems() {
					return sweeperDurations.size();
				}
				@Override
				protected void drawitem(GOut g, Integer item, int i) {
					g.aimage(Text.renderstroked(item.toString()).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
				}
				@Override
				public void change(Integer item) {
					super.change(item);
					sweeperSetDuration = sweeperDurations.indexOf(item);
					System.out.println(sweeperSetDuration);
					Utils.setprefi("sweeperSetDuration", sweeperDurations.indexOf(item));
					if (ui != null && ui.gui != null && ui.gui.miningSafetyAssistantWindow != null)
						ui.gui.miningSafetyAssistantWindow.sweeperDurationDropbox.change2(item);
				}
			}, leftColumn.pos("ul").adds(160, 2));

			middleColumn = add(showObjectCollisionBoxesCheckBox = new CheckBox("Show Object Collision Boxes"){
				{a = (Utils.getprefb("showObjectCollisionBoxes", false));}
				public void set(boolean val) {
					Utils.setprefb("showObjectCollisionBoxes", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
						ui.gui.map.updatePlobCollisionBox();
					}
				}
			}, UI.scale(240, 0));
			showObjectCollisionBoxesCheckBox.tooltip = showObjectCollisionBoxesTooltip;
			middleColumn = add(collisionBoxColorOptionWidget = new ColorOptionWidget("Collision Box Color:", "collisionBox", 115, Integer.parseInt(collisionBoxColorSetting[0]), Integer.parseInt(collisionBoxColorSetting[1]), Integer.parseInt(collisionBoxColorSetting[2]), Integer.parseInt(collisionBoxColorSetting[3]), (Color col) -> {
				CollisionBox.SOLID_HOLLOW = Pipe.Op.compose(new ColorMask(col), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
					ui.gui.map.updatePlobCollisionBox();
				}
			}){}, middleColumn.pos("bl").adds(1, 0));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("collisionBox" + "_colorSetting", new String[]{"255", "255", "255", "210"});
				collisionBoxColorOptionWidget.cb.colorChooser.setColor(collisionBoxColorOptionWidget.currentColor = new Color(255, 255, 255, 210));
				CollisionBox.SOLID_HOLLOW = Pipe.Op.compose(new ColorMask(OptWnd.collisionBoxColorOptionWidget.currentColor), new States.LineWidth(CollisionBox.WIDTH), CollisionBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
					ui.gui.map.updatePlobCollisionBox();
				}
			}), collisionBoxColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(230, 40))), middleColumn.pos("bl").adds(0, 8).xs(240));
			middleColumn = scroll;
			Widget cont = scroll.cont;
			addbtn(cont, "Show Collision Boxes Hotkey:", GameUI.kb_toggleCollisionBoxes, 0);

			middleColumn = add(displayObjectDurabilityPercentageCheckBox = new CheckBox("Display Object Durability Percentage"){
				{a = (Utils.getprefb("displayObjectHealthPercentage", true));}
				public void changed(boolean val) {
					Utils.setprefb("displayObjectHealthPercentage", val);
				}
			}, middleColumn.pos("bl").adds(0, -6).x(UI.scale(240)));
			displayObjectDurabilityPercentageCheckBox.tooltip = displayObjectDurabilityPercentageTooltip;
			middleColumn = add(displayObjectQualityOnInspectionCheckBox = new CheckBox("Display Object Quality on Inspection"){
				{a = (Utils.getprefb("displayObjectQualityOnInspection", true));}
				public void changed(boolean val) {
					Utils.setprefb("displayObjectQualityOnInspection", val);
				}
			}, middleColumn.pos("bl").adds(0, 2));
			displayObjectQualityOnInspectionCheckBox.tooltip = displayObjectQualityOnInspectionTooltip;

			middleColumn = add(showCritterAurasCheckBox = new CheckBox("Show Critter Circle Auras (Clickable)"){
				{a = (Utils.getprefb("showCritterAuras", true));}
				public void changed(boolean val) {
					Utils.setprefb("showCritterAuras", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
					}
				}
			}, middleColumn.pos("bl").adds(0, 17));
			showCritterAurasCheckBox.tooltip = showCritterAurasTooltip;
			middleColumn = add(rabbitAuraColorOptionWidget = new ColorOptionWidget("Rabbit Aura:", "rabbitAura", 115, Integer.parseInt(rabbitAuraColorSetting[0]), Integer.parseInt(rabbitAuraColorSetting[1]), Integer.parseInt(rabbitAuraColorSetting[2]), Integer.parseInt(rabbitAuraColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
				}
			}){}, middleColumn.pos("bl").adds(1, 1));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("rabbitAura" + "_colorSetting", new String[]{"88", "255", "0", "140"});
				rabbitAuraColorOptionWidget.cb.colorChooser.setColor(rabbitAuraColorOptionWidget.currentColor = new Color(88, 255, 0, 140));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
				}
			}), rabbitAuraColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			middleColumn = add(genericCritterAuraColorOptionWidget = new ColorOptionWidget("Generic Critter Aura:", "genericCritterAura", 115, Integer.parseInt(genericCritterAuraColorSetting[0]), Integer.parseInt(genericCritterAuraColorSetting[1]), Integer.parseInt(genericCritterAuraColorSetting[2]), Integer.parseInt(genericCritterAuraColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
				}
			}){}, middleColumn.pos("bl").adds(0, 4));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("genericCritterAura" + "_colorSetting", new String[]{"193", "0", "255", "140"});
				genericCritterAuraColorOptionWidget.cb.colorChooser.setColor(genericCritterAuraColorOptionWidget.currentColor = new Color(193, 0, 255, 140));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateCritterAuras);
				}
			}), genericCritterAuraColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;

			middleColumn = add(showSpeedBuffAurasCheckBox = new CheckBox("Show Speed Buff Circle Auras"){
				{a = (Utils.getprefb("showSpeedBuffAuras", true));}
				public void set(boolean val) {
					Utils.setprefb("showSpeedBuffAuras", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateSpeedBuffAuras);
					}
				}
			}, middleColumn.pos("bl").adds(0, 18).x(UI.scale(240)));
			showSpeedBuffAurasCheckBox.tooltip = showSpeedBuffAurasTooltip;
			middleColumn = add(speedBuffAuraColorOptionWidget = new ColorOptionWidget("Speed Buff Aura:", "speedBuffAura", 115, Integer.parseInt(speedBuffAuraColorSetting[0]), Integer.parseInt(speedBuffAuraColorSetting[1]), Integer.parseInt(speedBuffAuraColorSetting[2]), Integer.parseInt(speedBuffAuraColorSetting[3]), (Color col) -> {
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateSpeedBuffAuras);
				}
			}){}, middleColumn.pos("bl").adds(1, 1));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("speedBuffAura" + "_colorSetting", new String[]{"255", "255", "255", "140"});
				speedBuffAuraColorOptionWidget.cb.colorChooser.setColor(speedBuffAuraColorOptionWidget.currentColor = new Color(255, 255, 255, 140));
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateSpeedBuffAuras);
				}
			}), speedBuffAuraColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;

			middleColumn = add(showMidgesCircleAurasCheckBox = new CheckBox("Show Midges Circle Auras"){
				{a = (Utils.getprefb("showMidgesCircleAuras", true));}
				public void changed(boolean val) {
					Utils.setprefb("showMidgesCircleAuras", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateMidgesAuras);
					}
				}
			}, middleColumn.pos("bl").adds(0, 18).x(UI.scale(240)));
			showMidgesCircleAurasCheckBox.tooltip = showMidgesCircleAurasTooltip;

			middleColumn = add(showDangerousBeastRadiiCheckBox = new CheckBox("Show Dangerous Beast Radii"){
				{a = (Utils.getprefb("showDangerousBeastRadii", true));}
				public void changed(boolean val) {
					Utils.setprefb("showDangerousBeastRadii", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateDangerousBeastRadii);
					}
				}
			}, middleColumn.pos("bl").adds(0, 2));
			showDangerousBeastRadiiCheckBox.tooltip = showDangerousBeastRadiiTooltip;

			middleColumn = add(showBeeSkepsRadiiCheckBox = new CheckBox("Show Bee Skep Radii"){
				{a = (Utils.getprefb("showBeeSkepsRadii", false));}
				public void set(boolean val) {
					Utils.setprefb("showBeeSkepsRadii", val);
					a = val;
					if (ui != null && ui.gui != null){
						ui.sess.glob.oc.gobAction(Gob::updateBeeSkepRadius);
						ui.gui.optionInfoMsg("Bee Skep Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, middleColumn.pos("bl").adds(0, 2));
			showBeeSkepsRadiiCheckBox.tooltip = showBeeSkepsRadiiTooltip;
			middleColumn = add(showFoodTroughsRadiiCheckBox = new CheckBox("Show Food Trough Radii"){
				{a = (Utils.getprefb("showFoodTroughsRadii", false));}
				public void set(boolean val) {
					Utils.setprefb("showFoodTroughsRadii", val);
					a = val;
					if (ui != null && ui.gui != null){
						ui.sess.glob.oc.gobAction(Gob::updateTroughsRadius);
						ui.gui.optionInfoMsg("Food Trough Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, middleColumn.pos("bl").adds(0, 2));
			showFoodTroughsRadiiCheckBox.tooltip = showFoodThroughsRadiiTooltip;
			middleColumn = add(showMoundBedsRadiiCheckBox = new CheckBox("Show Mound Beds Radii"){
				{a = (Utils.getprefb("showMoundBedsRadii", false));}
				public void set(boolean val) {
					Utils.setprefb("showMoundBedsRadii", val);
					a = val;
					if (ui != null && ui.gui != null){
						ui.sess.glob.oc.gobAction(Gob::updateMoundBedsRadius);
						ui.gui.optionInfoMsg("Mound Beds Radii are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, middleColumn.pos("bl").adds(0, 2));
			showMoundBedsRadiiCheckBox.tooltip = showMoundBedsRadiiTooltip;
			middleColumn = add(objectPermanentHighlightingCheckBox = new CheckBox(""){
				{a = (Utils.getprefb("objectPermanentHighlighting", false));}
				public void changed(boolean val) {
					Utils.setprefb("objectPermanentHighlighting", val);
					if (!val) {
						if (ui != null && ui.gui != null)
							ui.sess.glob.oc.gobAction(Gob::removePermanentHighlightOverlay);
						Gob.permanentHighlightList.clear();
					}
				}
			}, middleColumn.pos("bl").adds(0, 20));
			objectPermanentHighlightingCheckBox.tooltip = objectPermanentHighlightingTooltip;
			// ND: Doing funny workaround with 2 separate labels to split the checkbox on 2 rows haha
			add(new Label("Permanently Highlight Objects with"){
				@Override
				public boolean mousedown(MouseDownEvent ev) {
					if(ev.b == 1) {
						objectPermanentHighlightingCheckBox.click();
						return(true);
					}
					return(super.mousedown(ev));
				}
			}, middleColumn.pos("ur").adds(6, -10)).tooltip = objectPermanentHighlightingTooltip;;
			add(new Label("Alt + Middle Click (Mouse Scroll Click)"){
				@Override
				public boolean mousedown(MouseDownEvent ev) {
					if(ev.b == 1) {
						objectPermanentHighlightingCheckBox.click();
						return(true);
					}
					return(super.mousedown(ev));
				}
			}, middleColumn.pos("bl").adds(21, -6)).tooltip = objectPermanentHighlightingTooltip;;

			rightColumn = add(new Label("Object Pinging Colors:"), UI.scale(480, 0));
			rightColumn = add(areaChatPingColorOptionWidget = new ColorOptionWidget("Area Chat (Alt+LClick):", "areaChatPing", 115, Integer.parseInt(areaChatPingColorSetting[0]), Integer.parseInt(areaChatPingColorSetting[1]), Integer.parseInt(areaChatPingColorSetting[2]), Integer.parseInt(areaChatPingColorSetting[3]), (Color col) -> {
			}){}, rightColumn.pos("bl").adds(1, 1));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("areaChatPing" + "_colorSetting", new String[]{"255", "183", "0", "255"});
				areaChatPingColorOptionWidget.cb.colorChooser.setColor(areaChatPingColorOptionWidget.currentColor = new Color(255, 183, 0, 255));
			}), areaChatPingColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			rightColumn = add(partyChatPingColorOptionWidget = new ColorOptionWidget("Party Chat (Alt+RClick):", "partyChatPing", 115, Integer.parseInt(partyChatPingColorSetting[0]), Integer.parseInt(partyChatPingColorSetting[1]), Integer.parseInt(partyChatPingColorSetting[2]), Integer.parseInt(partyChatPingColorSetting[3]), (Color col) -> {
			}){}, rightColumn.pos("bl").adds(0, 4));
			add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("partyChatPing" + "_colorSetting", new String[]{"243", "0", "0", "255"});
				partyChatPingColorOptionWidget.cb.colorChooser.setColor(partyChatPingColorOptionWidget.currentColor = new Color(243, 0, 0, 255));
			}), partyChatPingColorOptionWidget.pos("ur").adds(10, 0)).tooltip = resetButtonTooltip;
			rightColumn = add(showObjectsSpeedCheckBox = new CheckBox("Show Objects Speed"){
				{a = Utils.getprefb("showObjectsSpeed", false);}
				public void changed(boolean val) {
					Utils.setprefb("showObjectsSpeed", val);
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Objects Speed is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, rightColumn.pos("bl").adds(0, 12).x(UI.scale(480)));
			showObjectsSpeedCheckBox.tooltip = showObjectsSpeedTooltip;
			rightColumn = add(displayGrowthInfoCheckBox = new CheckBox("Display Growth Info on Plants and Trees"){
				{a = (Utils.getprefb("displayGrowthInfo", false));}
				public void changed(boolean val) {
					Utils.setprefb("displayGrowthInfo", val);
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Growth Info on Plants and Trees is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, rightColumn.pos("bl").adds(0, 17));
			displayGrowthInfoCheckBox.tooltip = displayGrowthInfoTooltip;
			rightColumn = add(alsoShowOversizedTreesAbovePercentageCheckBox = new CheckBox("Also Show Trees Above %:"){
				{a = (Utils.getprefb("alsoShowOversizedTreesAbovePercentage", true));}
				public void changed(boolean val) {
					Utils.setprefb("alsoShowOversizedTreesAbovePercentage", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::refreshGrowthInfo);
					}
				}
			}, rightColumn.pos("bl").adds(12, 2));
			add(oversizedTreesPercentageTextEntry = new TextEntry(UI.scale(36), Utils.getpref("oversizedTreesPercentage", "150")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", "")); // Only numbers
					this.settext(this.text().replaceAll("(?<=^.{3}).*", "")); // No more than 3 digits
					Utils.setpref("oversizedTreesPercentage", this.buf.line());
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::refreshGrowthInfo);
					}
					super.changed();
				}
			}, alsoShowOversizedTreesAbovePercentageCheckBox.pos("ur").adds(4, 0));
			rightColumn = add(showTreesBushesHarvestIconsCheckBox = new CheckBox("Show Trees & Bushes Harvest Icons"){
				{a = Utils.getprefb("showTreesBushesHarvestIcons", false);}
				public void changed(boolean val) {
					Utils.setprefb("showTreesBushesHarvestIcons", val);
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Trees & Bushes Harvest Icons are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, rightColumn.pos("bl").adds(0, 12).x(UI.scale(480)));
			showTreesBushesHarvestIconsCheckBox.tooltip = showTreesBushesHarvestIconsTooltip;
			rightColumn = add(showLowFoodWaterIconsCheckBox = new CheckBox("Show Low Food & Water Icons"){
				{a = Utils.getprefb("showLowFoodWaterIcons", false);}
				public void changed(boolean val) {
					Utils.setprefb("showLowFoodWaterIcons", val);
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Low Food & Water Icons are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, rightColumn.pos("bl").adds(0, 2).x(UI.scale(480)));
			showLowFoodWaterIconsCheckBox.tooltip = showLowFoodWaterIconsTooltip;
			rightColumn = add(showBeeSkepsHarvestIconsCheckBox = new CheckBox("Show Bee Skep Harvest Icons"){
				{a = Utils.getprefb("showBeeSkepsHarvestIcons", false);}
				public void changed(boolean val) {
					Utils.setprefb("showBeeSkepsHarvestIcons", val);
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Bee Skep Harvest Icons are now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, rightColumn.pos("bl").adds(0, 2).x(UI.scale(480)));
			showBeeSkepsHarvestIconsCheckBox.tooltip = showBeeSkepsHarvestIconsTooltip;

			rightColumn = add(showBarrelContentsTextCheckBox = new CheckBox("Show Barrel Contents Text"){
				{a = (Utils.getprefb("showBarrelContentsText", true));}
				public void changed(boolean val) {
					Utils.setprefb("showBarrelContentsText", val);
					if (ui != null && ui.gui != null){
						ui.gui.optionInfoMsg("Barrel Contents Text is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, rightColumn.pos("bl").adds(0, 13));
			showBarrelContentsTextCheckBox.tooltip = showBarrelContentsTextTooltip;

			rightColumn = add(showIconSignTextCheckBox = new CheckBox("Show Icon Sign Text"){
				{a = (Utils.getprefb("showIconSignText", true));}
				public void changed(boolean val) {
					Utils.setprefb("showIconSignText", val);
					if (ui != null && ui.gui != null){
						ui.gui.optionInfoMsg("Icon Sign Text is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, rightColumn.pos("bl").adds(0, 2));
			showIconSignTextCheckBox.tooltip = showIconSignTextTooltip;
			rightColumn = add(showCheeseRacksTierTextCheckBox = new CheckBox("Show Cheese Racks Tier Text"){
				{a = (Utils.getprefb("showCheeseRacksTierText", false));}
				public void changed(boolean val) {
					Utils.setprefb("showCheeseRacksTierText", val);
					if (ui != null && ui.gui != null){
						ui.gui.optionInfoMsg("Cheese Racks Tier Text is now " + (val ? "SHOWN" : "HIDDEN") + "!", (val ? msgGreen : msgGray), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, rightColumn.pos("bl").adds(0, 2));
			showCheeseRacksTierTextCheckBox.tooltip = showCheeseRacksTierTextTooltip;

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}

		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(70), cmd))
					+ UI.scale(2));
		}

	}


	public static CheckBox showQualityDisplayCheckBox;
	public static CheckBox roundedQualityCheckBox;
	public static CheckBox customQualityColorsCheckBox;

	public static TextEntry q7ColorTextEntry, q6ColorTextEntry, q5ColorTextEntry, q4ColorTextEntry, q3ColorTextEntry, q2ColorTextEntry, q1ColorTextEntry;
	public static ColorOptionWidget q7ColorOptionWidget, q6ColorOptionWidget, q5ColorOptionWidget, q4ColorOptionWidget, q3ColorOptionWidget, q2ColorOptionWidget, q1ColorOptionWidget;
	public static String[] q7ColorSetting = Utils.getprefsa("q7ColorSetting_colorSetting", new String[]{"255","0","0","255"});
	public static String[] q6ColorSetting = Utils.getprefsa("q6ColorSetting_colorSetting", new String[]{"255","114","0","255"});
	public static String[] q5ColorSetting = Utils.getprefsa("q5ColorSetting_colorSetting", new String[]{"165","0","255","255"});
	public static String[] q4ColorSetting = Utils.getprefsa("q4ColorSetting_colorSetting", new String[]{"0","131","255","255"});
	public static String[] q3ColorSetting = Utils.getprefsa("q3ColorSetting_colorSetting", new String[]{"0","214","10","255"});
	public static String[] q2ColorSetting = Utils.getprefsa("q2ColorSetting_colorSetting", new String[]{"255","255","255","255"});
	public static String[] q1ColorSetting = Utils.getprefsa("q1ColorSetting_colorSetting", new String[]{"180","180","180","255"});

	public class QualityDisplaySettingsPanel extends Panel {

		public QualityDisplaySettingsPanel(Panel back) {
			Widget prev;
			prev = add(showQualityDisplayCheckBox = new CheckBox("Display Quality on Inventory Items"){
				{a = (Utils.getprefb("qtoggle", true));}
				public void set(boolean val) {
					Utils.setprefb("qtoggle", val);
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					a = val;
				}
			}, 0, 6);
			prev = add(roundedQualityCheckBox = new CheckBox("Rounded Quality Number"){
				{a = (Utils.getprefb("roundedQuality", true));}
				public void changed(boolean val) {
					Utils.setprefb("roundedQuality", val);
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(customQualityColorsCheckBox = new CheckBox("Enable Custom Quality Colors:"){
				{a = (Utils.getprefb("enableCustomQualityColors", false));}
				public void changed(boolean val) {
					Utils.setprefb("enableCustomQualityColors", val);
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
				}
			}, prev.pos("bl").adds(0, 12));
			prev.tooltip = customQualityColorsTooltip;

			prev = add(q7ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q7ColorTextEntry", "400")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q7ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10));
			prev = add(q7ColorOptionWidget = new ColorOptionWidget(" Godlike Quality:", "q7ColorSetting", 120, Integer.parseInt(q7ColorSetting[0]), Integer.parseInt(q7ColorSetting[1]), Integer.parseInt(q7ColorSetting[2]), Integer.parseInt(q7ColorSetting[3]), (Color col) -> {
				q7ColorOptionWidget.cb.colorChooser.setColor(q7ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q7ColorSetting_colorSetting", new String[]{"255","0","0","255"});
				q7ColorOptionWidget.cb.colorChooser.setColor(q7ColorOptionWidget.currentColor = new Color(255, 0, 0, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = resetButtonTooltip;

			prev = add(q6ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q6ColorTextEntry", "300")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q6ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q6ColorOptionWidget = new ColorOptionWidget("  Legendary Quality:", "q6ColorSetting", 120, Integer.parseInt(q6ColorSetting[0]), Integer.parseInt(q6ColorSetting[1]), Integer.parseInt(q6ColorSetting[2]), Integer.parseInt(q6ColorSetting[3]), (Color col) -> {
				q6ColorOptionWidget.cb.colorChooser.setColor(q6ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q6ColorSetting_colorSetting", new String[]{"255","114","0","255"});
				q6ColorOptionWidget.cb.colorChooser.setColor(q6ColorOptionWidget.currentColor = new Color(255, 114, 0, 255));
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = resetButtonTooltip;

			prev = add(q5ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q5ColorTextEntry", "200")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q5ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q5ColorOptionWidget = new ColorOptionWidget("  Epic Quality:", "q5ColorSetting", 120, Integer.parseInt(q5ColorSetting[0]), Integer.parseInt(q5ColorSetting[1]), Integer.parseInt(q5ColorSetting[2]), Integer.parseInt(q5ColorSetting[3]), (Color col) -> {
				q5ColorOptionWidget.cb.colorChooser.setColor(q5ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q5ColorSetting_colorSetting", new String[]{"165","0","255","255"});
				q5ColorOptionWidget.cb.colorChooser.setColor(q5ColorOptionWidget.currentColor = new Color(165, 0, 255, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = resetButtonTooltip;

			prev = add(q4ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q4ColorTextEntry", "100")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q4ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q4ColorOptionWidget = new ColorOptionWidget("  Rare Quality:", "q4ColorSetting", 120, Integer.parseInt(q4ColorSetting[0]), Integer.parseInt(q4ColorSetting[1]), Integer.parseInt(q4ColorSetting[2]), Integer.parseInt(q4ColorSetting[3]), (Color col) -> {
				q4ColorOptionWidget.cb.colorChooser.setColor(q4ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q4ColorSetting_colorSetting", new String[]{"0","131","255","255"});
				q4ColorOptionWidget.cb.colorChooser.setColor(q4ColorOptionWidget.currentColor = new Color(0, 131, 255, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = resetButtonTooltip;

			prev = add(q3ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q3ColorTextEntry", "50")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q3ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q3ColorOptionWidget = new ColorOptionWidget("  Uncommon Quality:", "q3ColorSetting", 120, Integer.parseInt(q3ColorSetting[0]), Integer.parseInt(q3ColorSetting[1]), Integer.parseInt(q3ColorSetting[2]), Integer.parseInt(q3ColorSetting[3]), (Color col) -> {
				q3ColorOptionWidget.cb.colorChooser.setColor(q3ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q3ColorSetting_colorSetting", new String[]{"0","214","10","255"});
				q3ColorOptionWidget.cb.colorChooser.setColor(q3ColorOptionWidget.currentColor = new Color(0, 214, 10, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = resetButtonTooltip;

			prev = add(q2ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q2ColorTextEntry", "10")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q2ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q2ColorOptionWidget = new ColorOptionWidget("  Common Quality:", "q2ColorSetting", 120, Integer.parseInt(q2ColorSetting[0]), Integer.parseInt(q2ColorSetting[1]), Integer.parseInt(q2ColorSetting[2]), Integer.parseInt(q2ColorSetting[3]), (Color col) -> {
				q2ColorOptionWidget.cb.colorChooser.setColor(q2ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q2ColorSetting_colorSetting", new String[]{"255","255","255","255"});
				q2ColorOptionWidget.cb.colorChooser.setColor(q2ColorOptionWidget.currentColor = new Color(255, 255, 255, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = resetButtonTooltip;

			prev = add(q1ColorTextEntry = new TextEntry(UI.scale(60), Utils.getpref("q1ColorTextEntry", "1")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", ""));
					Utils.setpref("q1ColorTextEntry", this.buf.line());
					if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
					super.changed();
				}
			}, prev.pos("bl").adds(0, 10).x(UI.scale(0)));
			prev = add(q1ColorOptionWidget = new ColorOptionWidget("  Junk Quality:", "q1ColorSetting", 120, Integer.parseInt(q1ColorSetting[0]), Integer.parseInt(q1ColorSetting[1]), Integer.parseInt(q1ColorSetting[2]), Integer.parseInt(q1ColorSetting[3]), (Color col) -> {
				q1ColorOptionWidget.cb.colorChooser.setColor(q1ColorOptionWidget.currentColor = col);
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}){}, prev.pos("ur").adds(5, -2));
			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("q1ColorSetting_colorSetting", new String[]{"180","180","180","255"});
				q1ColorOptionWidget.cb.colorChooser.setColor(q1ColorOptionWidget.currentColor = new Color(180, 180, 180, 255));
				if (ui != null && ui.gui != null) ui.gui.reloadAllItemOverlays();
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = resetButtonTooltip;

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}
	}


    private static final Text kbtt = RichText.render("$col[255,200,0]{Escape}: Cancel input\n" +
						     "$col[255,200,0]{Backspace}: Revert to default\n" +
						     "$col[255,200,0]{Delete}: Disable keybinding", 0);
    public class BindingPanel extends Panel {
	private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
	    return(cont.addhl(new Coord(0, y), cont.sz.x,
			      new Label(nm), new SetButton(UI.scale(140), cmd))
		   + UI.scale(2));
	}

		private int addbtnImproved(Widget cont, String nm, String tooltip, Color color, KeyBinding cmd, int y) {
			Label theLabel = new Label(nm);
			if (tooltip != null && !tooltip.equals(""))
				theLabel.tooltip = RichText.render(tooltip, UI.scale(300));
			theLabel.setcolor(color);
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					theLabel, new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

	public BindingPanel(Panel back) {
	    super();
		int y = 5;
		Label topNote = new Label("Don't use the same keys on multiple Keybinds!");
		topNote.setcolor(Color.RED);
		y = adda(topNote, UI.scale(155), y, 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = adda(new Label("If you do that, only one of them will work. God knows which."), 310 / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
		Scrollport scroll = add(new Scrollport(UI.scale(new Coord(310, 360))), 0, 60);
	    Widget cont = scroll.cont;
	    Widget prev;
	    y = 0;
	    y = cont.adda(new Label("Main menu"), cont.sz.x / 2, y, 0.5, 0.0).pos("bl").adds(0, 5).y;
	    y = addbtn(cont, "Inventory", GameUI.kb_inv, y);
	    y = addbtn(cont, "Equipment", GameUI.kb_equ, y);
	    y = addbtn(cont, "Character sheet", GameUI.kb_chr, y);
	    y = addbtn(cont, "Map window", GameUI.kb_map, y);
	    y = addbtn(cont, "Kith & Kin", GameUI.kb_bud, y);
	    y = addbtn(cont, "Options", GameUI.kb_opt, y);
	    y = addbtn(cont, "Search actions", GameUI.kb_srch, y);
	    y = addbtn(cont, "Focus chat window", GameUI.kb_chat, y);
//	    y = addbtn(cont, "Quick chat", ChatUI.kb_quick, y);
//	    y = addbtn(cont, "Take screenshot", GameUI.kb_shoot, y);
	    y = addbtn(cont, "Minimap icons", GameUI.kb_ico, y);
	    y = addbtn(cont, "Toggle UI", GameUI.kb_hide, y);
		y = addbtn(cont, "Toggle Mini-Study Window", GameUI.kb_miniStudy, y);
	    y = addbtn(cont, "Log out", GameUI.kb_logout, y);
	    y = addbtn(cont, "Switch character", GameUI.kb_switchchr, y);

	    y = cont.adda(new Label("Map buttons"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtn(cont, "Reset view", MapWnd.kb_home, y);
		y = addbtn(cont, "Compact mode", MapWnd.kb_compact, y);
		y = addbtn(cont, "Hide markers", MapWnd.kb_hmark, y);
		y = addbtn(cont, "Add marker", MapWnd.kb_mark, y);

		y = cont.adda(new Label("Game World Toggles"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
	    y = addbtn(cont, "Display Personal Claims", GameUI.kb_claim, y);
	    y = addbtn(cont, "Display Village Claims", GameUI.kb_vil, y);
	    y = addbtn(cont, "Display Realm Provinces", GameUI.kb_rlm, y);
	    y = addbtn(cont, "Display Tile Grid", MapView.kb_grid, y);

	    y = cont.adda(new Label("Camera control"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtn(cont, "Snap North", MapView.kb_camSnapNorth, y);
		y = addbtn(cont, "Snap South", MapView.kb_camSnapSouth, y);
		y = addbtn(cont, "Snap East", MapView.kb_camSnapEast, y);
		y = addbtn(cont, "Snap West", MapView.kb_camSnapWest, y);


	    y = cont.adda(new Label("Walking speed"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
	    y = addbtn(cont, "Increase speed", Speedget.kb_speedup, y);
	    y = addbtn(cont, "Decrease speed", Speedget.kb_speeddn, y);
	    for(int i = 0; i < 4; i++)
		y = addbtn(cont, String.format("Set speed %d", i + 1), Speedget.kb_speeds[i], y);

	    y = cont.adda(new Label("Combat actions"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
	    for(int i = 0; i < Fightsess.kb_acts.length; i++)
		y = addbtn(cont, String.format("Combat action %d", i + 1), Fightsess.kb_acts[i], y);
		y = addbtnImproved(cont, "Cycle through targets", "This only cycles through the targets you are currently engaged in combat with.", Color.WHITE, Fightsess.kb_relcycle, y);
		y = addbtnImproved(cont, "Switch to nearest target", "This only switches to the nearest target you are currently engaged in combat with.", Color.WHITE, Fightsess.kb_nearestTarget, y);
		y = addbtnImproved(cont, "Aggro Nearest Player/Animal", "Selects the nearest non-friendly Player or Animal to attack, based on your situation:" +
				"\n\n$col[218,163,0]{Case 1:} $col[185,185,185]{If you are in combat with Players, it will only attack other not-already-aggroed non-friendly players.}" +
				"\n$col[218,163,0]{Case 2:} $col[185,185,185]{If you are in combat with Animals, it will try to attack the closest not-already-aggroed player. If none is found, try to attack the closest animal. Once this happens, you're back to Case 1.}" +
				"\n\n$col[185,185,185]{Party members will never be attacked by this button. You can exclude other specific player groups from being attacked in the Aggro Exclusion Settings.}", new Color(255, 0, 0,255), GameUI.kb_aggroNearestTargetButton, y);
		y = addbtnImproved(cont, "Aggro/Target Nearest Cursor", "Tries to attack/target the closest player/animal it can find near the cursor." +
				"\n\n$col[185,185,185]{Party members will never be attacked by this button. You can exclude other specific player groups from being attacked in the Aggro Exclusion Settings.}", new Color(255, 0, 0,255), GameUI.kb_aggroOrTargetNearestCursor, y);
		y = addbtnImproved(cont, "Aggro Nearest Player", "Selects the nearest non-aggroed Player to attack." +
				"\n\n$col[185,185,185]{This only attacks players.}" +
				"\n\n$col[185,185,185]{Party members will never be attacked by this button. You can exclude other specific player groups from being attacked in the Aggro Exclusion Settings.}", new Color(255, 0, 0,255), GameUI.kb_aggroNearestPlayerButton, y);
		y = addbtnImproved(cont, "Aggro all Non-Friendly Players", "Tries to attack everyone in range. " +
				"\n\n$col[185,185,185]{Party members will never be attacked by this button. You can exclude other specific player groups from being attacked in the Aggro Exclusion Settings.}", new Color(255, 0, 0,255), GameUI.kb_aggroAllNonFriendlyPlayers, y);
		y = addbtnImproved(cont, "Re-Aggro Animal (Cheese)", "Use this to cheese animals and re-aggro them quickly when they flee." +
				"\n$col[185,185,185]{This is useful when you use animal auto-peace. Also, it only works when you're fighting one single animal.}", new Color(255, 68, 0,255), GameUI.kb_aggroLastTarget, y);
		y = addbtnImproved(cont, "Peace Current Target", "", new Color(0, 255, 34,255), GameUI.kb_peaceCurrentTarget, y);

		y = cont.adda(new Label("Other Custom features"), cont.sz.x / 2, y + UI.scale(10), 0.5, 0.0).pos("bl").adds(0, 5).y;
		y = addbtnImproved(cont, "Drink Button", "", new Color(0, 140, 255, 255), GameUI.kb_drinkButton, y+6);
		y = addbtn(cont, "Left Hand (Quick-Switch)", GameUI.kb_leftQuickSlotButton, y);
		y = addbtn(cont, "Right Hand (Quick-Switch)", GameUI.kb_rightQuickSlotButton, y);
		y = addbtnImproved(cont, "Night Vision / Brighter World", "This will simulate daytime lighting during the night. \n$col[185,185,185]{It slightly affects the light levels during the day too.}" +
				"\n\n$col[218,163,0]{Note:} $col[185,185,185]{This keybind just switches the value of Night Vision / Brighter World between minimum and maximum. This can also be set more precisely using the slider in the World Graphics Settings.}", Color.WHITE, GameUI.kb_nightVision, y);

		y+=UI.scale(12);
		y = addbtn(cont, "Inventory search", GameUI.kb_searchInventoriesButton, y);
		y = addbtn(cont, "Object search", GameUI.kb_searchObjectsButton, y);

		y+=UI.scale(20);
		y = addbtnImproved(cont, "Click Nearest Object (Cursor)","When this button is pressed, you will instantly click the nearest object to your cursor, selected from below." +
				"\n$col[218,163,0]{Range:} $col[185,185,185]{12 tiles (approximately)}", new Color(255, 191, 0,255), GameUI.kb_clickNearestCursorObject, y);
		y = addbtnImproved(cont, "Click Nearest Object (You)","When this button is pressed, you will instantly click the nearest object to you, selected from below." +
				"\n$col[218,163,0]{Range:} $col[185,185,185]{12 tiles (approximately)}", new Color(255, 191, 0,255), GameUI.kb_clickNearestObject, y);
		Widget objectsLeft, objectsRight;
		y = cont.adda(objectsLeft = new Label("Objects to Click:"), UI.scale(20), y + UI.scale(2), 0, 0.0).pos("bl").adds(0, 5).y;
		objectsLeft = cont.add(new CheckBox("Forageables"){{a = Utils.getprefb("clickNearestObject_Forageables", true);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_Forageables", val);}}, objectsLeft.pos("ur").adds(30, 0)).settip("Pick the nearest Forageable.");
		objectsRight = cont.add(new CheckBox("Critters"){{a = Utils.getprefb("clickNearestObject_Critters", true);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_Critters", val);}}, objectsLeft.pos("ur").adds(26, 0)).settip("Chase the nearest Critter.");
		objectsLeft = cont.add(new CheckBox("Non-Visitor Gates"){{a = Utils.getprefb("clickNearestObject_NonVisitorGates", true);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_NonVisitorGates", val);}}, objectsLeft.pos("bl").adds(0, 4)).settip("Open/Close the nearest Non-Visitor Gate.");
		objectsRight = cont.add(new CheckBox("Caves"){{a = Utils.getprefb("clickNearestObject_Caves", false);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_Caves", val);}}, objectsRight.pos("bl").adds(0, 4)).settip("Go through the nearest Cave Entrance/Exit.");
		objectsLeft = cont.add(new CheckBox("Mineholes & Ladders"){{a = Utils.getprefb("clickNearestObject_MineholesAndLadders", false);}
			public void changed(boolean val) {Utils.setprefb("clickNearestObject_MineholesAndLadders", val);}}, objectsLeft.pos("bl").adds(0, 4)).settip("Hop down the nearest Minehole, or Climb up the nearest Ladder.");
		y+=UI.scale(60);
		y = addbtnImproved(cont, "Hop on Nearest Vehicle", "When this button is pressed, your character will run towards the nearest mountable Vehicle/Animal, and try to mount it." +
				"\n\n$col[185,185,185]{If the closest vehicle to you is full, or unmountable (like a rowboat on land), it will keep looking for the next closest mountable vehicle.}" +
				"\n\n$col[218,163,0]{Works with:} Knarr, Snekkja, Rowboat, Dugout, Kicksled, Coracle, Wagon, Wilderness Skis, Tamed Horse" +
				"\n\n$col[218,163,0]{Range:} $col[185,185,185]{36 tiles (approximately)}", new Color(255, 191, 0,255), GameUI.kb_enterNearestVehicle, y);
		y+=UI.scale(20);
		y = addbtnImproved(cont, "Lift Nearest into Wagon/Cart", "When pressed the nearest supported liftable object will be stored in the nearest Wagon/Cart" +
				"\n\n$col[185,185,185]{If you are riding a Wagon it will try to exit the wagon, store the object and enter the wagon again.}", new Color(255, 191, 0,255), GameUI.kb_wagonNearestLiftable, y);
		Widget objectsLiftActionLeft, objectsLiftActionRight;
		y = cont.adda(objectsLiftActionLeft = new Label("Objects to Lift:"), UI.scale(20), y + UI.scale(2), 0, 0.0).pos("bl").adds(0, 5).y;
		objectsLiftActionLeft = cont.add(new CheckBox("Dead Animals"){{a = Utils.getprefb("wagonNearestLiftable_animalcarcass", true);}
			public void changed(boolean val) {Utils.setprefb("wagonNearestLiftable_animalcarcass", val);}}, objectsLiftActionLeft.pos("ur").adds(39, 0)).settip("Lift the nearest animal carcass into Wagon/Cart.");
		objectsLiftActionRight = cont.add(new CheckBox("Containers"){{a = Utils.getprefb("wagonNearestLiftable_container", true);}
			public void changed(boolean val) {Utils.setprefb("wagonNearestLiftable_container", val);}}, objectsLiftActionLeft.pos("ur").adds(4, 0)).settip("Lift the nearest storage container into Wagon/Cart.");
		objectsLiftActionLeft = cont.add(new CheckBox("Tree Logs"){{a = Utils.getprefb("wagonNearestLiftable_log", true);}
			public void changed(boolean val) {Utils.setprefb("wagonNearestLiftable_log", val);}}, objectsLiftActionLeft.pos("bl").adds(0, 4)).settip("Lift nearest log into Wagon/Cart.");

		y+=UI.scale(40);
		y = addbtn(cont, "Toggle Collision Boxes", GameUI.kb_toggleCollisionBoxes, y);
		y = addbtn(cont, "Toggle Object Hiding", GameUI.kb_toggleHidingBoxes, y);
		y = addbtn(cont, "Display Growth Info on Plants", GameUI.kb_toggleGrowthInfo, y);
		y = addbtn(cont, "Show Tree/Bush Harvest Icons", GameUI.kb_toggleHarvestIcons, y);
		y = addbtn(cont, "Show Low Food/Water Icons", GameUI.kb_toggleLowFoodWaterIcons, y);
		y = addbtn(cont, "Show Bee Skep Harvest Icons", GameUI.kb_toggleBeeSkepIcons, y);
		y = addbtn(cont, "Show Objects Speed", GameUI.kb_toggleSpeedInfo, y);
		y = addbtn(cont, "Hide/Show Cursor Item", GameUI.kb_toggleCursorItem, y);
		y+=UI.scale(20);
		y = addbtn(cont, "Loot Nearest Knocked Player", GameUI.kb_lootNearestKnockedPlayer, y);
		y+=UI.scale(20);
		y = addbtn(cont, "Instant Log Out", GameUI.kb_instantLogout, y);

		prev = adda(new PointBind(UI.scale(200)), scroll.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
	    prev = adda(new PButton(UI.scale(200), "Back", 27, back, "Options            "), prev.pos("bl").adds(0, 10).x(scroll.sz.x / 2), 0.5, 0.0);
	    pack();
	}
	}

	public class SetButton extends KeyMatch.Capture {
	    public final KeyBinding cmd;

	    public SetButton(int w, KeyBinding cmd) {
		super(w, cmd.key());
		this.cmd = cmd;
	    }

	    public void set(KeyMatch key) {
		super.set(key);
		cmd.set(key);
	    }

	    public void draw(GOut g) {
		if(cmd.key() != key)
		    super.set(cmd.key());
		super.draw(g);
	    }

	    protected KeyMatch mkmatch(KeyEvent ev) {
		return(KeyMatch.forevent(ev, ~cmd.modign));
	    }

	    protected boolean handle(KeyEvent ev) {
		if(ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
		    cmd.set(null);
		    super.set(cmd.key());
		    return(true);
		}
		return(super.handle(ev));
	    }

	    public Object tooltip(Coord c, Widget prev) {
		return(kbtt.tex());
	    }
	}

	public static CheckBox toggleTrackingOnLoginCheckBox;
	public static CheckBox toggleSwimmingOnLoginCheckBox;
	public static CheckBox toggleCriminalActsOnLoginCheckBox;
	public static CheckBox toggleSiegeEnginesOnLoginCheckBox;
	public static CheckBox togglePartyPermissionsOnLoginCheckBox;
	public static CheckBox toggleItemStackingOnLoginCheckBox;
	public static CheckBox autoSelect1stFlowerMenuCheckBox;
	public static CheckBox alsoUseContainersWithRepeaterCheckBox;
	public static CheckBox autoRepeatFlowerMenuCheckBox;
	public static CheckBox autoReloadCuriositiesFromInventoryCheckBox;
	public static CheckBox preventTablewareFromBreakingCheckBox = null;
	public static CheckBox autoDropLeechesCheckBox;
	public static CheckBox autoEquipBunnySlippersPlateBootsCheckBox;
	public static CheckBox autoDropTicksCheckBox;
	public static CheckBox autoPeaceAnimalsWhenCombatStartsCheckBox;
	public static CheckBox autoDrinkingCheckBox;
	public static TextEntry autoDrinkingThresholdTextEntry;
	public static CheckBox enableQueuedMovementCheckBox;

	public class GameplayAutomationSettingsPanel extends Panel {

		public GameplayAutomationSettingsPanel(Panel back) {
			Widget prev;
			Widget rightColumn;

			Widget toggleLabel = add(new Label("Toggle on Login:"), 0, 0);
			prev = add(toggleTrackingOnLoginCheckBox = new CheckBox("Tracking"){
				{a = Utils.getprefb("toggleTrackingOnLogin", true);}
				public void changed(boolean val) {
					Utils.setprefb("toggleTrackingOnLogin", val);
				}
			}, toggleLabel.pos("bl").adds(0, 6).x(UI.scale(0)));
			prev = add(toggleSwimmingOnLoginCheckBox = new CheckBox("Swimming"){
				{a = Utils.getprefb("toggleSwimmingOnLogin", true);}
				public void changed(boolean val) {
					Utils.setprefb("toggleSwimmingOnLogin", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(toggleCriminalActsOnLoginCheckBox = new CheckBox("Criminal Acts"){
				{a = Utils.getprefb("toggleCriminalActsOnLogin", true);}
				public void changed(boolean val) {
					Utils.setprefb("toggleCriminalActsOnLogin", val);
				}
			}, prev.pos("bl").adds(0, 2));

			rightColumn = add(toggleSiegeEnginesOnLoginCheckBox = new CheckBox("Check for Siege Engines"){
				{a = Utils.getprefb("toggleSiegeEnginesOnLogin", true);}
				public void changed(boolean val) {
					Utils.setprefb("toggleSiegeEnginesOnLogin", val);
				}
			}, toggleLabel.pos("bl").adds(110, 6));
			rightColumn = add(togglePartyPermissionsOnLoginCheckBox = new CheckBox("Party Permissions"){
				{a = Utils.getprefb("togglePartyPermissionsOnLogin", false);}
				public void changed(boolean val) {
					Utils.setprefb("togglePartyPermissionsOnLogin", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			rightColumn = add(toggleItemStackingOnLoginCheckBox = new CheckBox("Automatic Item Stacking"){
				{a = Utils.getprefb("toggleItemStackingOnLogin", false);}
				public void changed(boolean val) {
					Utils.setprefb("toggleItemStackingOnLogin", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));

			prev = add(new Label("Default Speed on Login:"), prev.pos("bl").adds(0, 16).x(0));
			List<String> runSpeeds = Arrays.asList("Crawl", "Walk", "Run", "Sprint");
			add(new OldDropBox<String>(runSpeeds.size(), runSpeeds) {
				{
					super.change(runSpeeds.get(Utils.getprefi("defaultSetSpeed", 2)));
				}
				@Override
				protected String listitem(int i) {
					return runSpeeds.get(i);
				}
				@Override
				protected int listitems() {
					return runSpeeds.size();
				}
				@Override
				protected void drawitem(GOut g, String item, int i) {
					g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
				}
				@Override
				public void change(String item) {
					super.change(item);
					for (int i = 0; i < runSpeeds.size(); i++){
						if (item.equals(runSpeeds.get(i))){
							Utils.setprefi("defaultSetSpeed", i);
						}
					}
				}
			}, prev.pos("bl").adds(130, -16));

			prev = add(new Label("Other gameplay automations:"), prev.pos("bl").adds(0, 14).x(0));
			prev = add(autoSelect1stFlowerMenuCheckBox = new CheckBox("Auto-Select 1st Flower-Menu Option (hold Ctrl)"){
				{a = Utils.getprefb("autoSelect1stFlowerMenu", true);}
				public void changed(boolean val) {
					Utils.setprefb("autoSelect1stFlowerMenu", val);
				}
			}, prev.pos("bl").adds(0, 6));
			autoSelect1stFlowerMenuCheckBox.tooltip = autoSelect1stFlowerMenuTooltip;
			prev = add(autoRepeatFlowerMenuCheckBox = new CheckBox("Auto-Repeat Flower-Menu (hold Ctrl+Shift)"){
				{a = Utils.getprefb("autoRepeatFlowerMenu", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoRepeatFlowerMenu", val);
				}
			}, prev.pos("bl").adds(0, 2));
			autoRepeatFlowerMenuCheckBox.tooltip = autoRepeatFlowerMenuTooltip;
			prev = add(alsoUseContainersWithRepeaterCheckBox = new CheckBox("Also use containers with Auto-Repeat"){
				{a = Utils.getprefb("alsoUseContainersWithRepeater", false);}
				public void changed(boolean val) {
					Utils.setprefb("alsoUseContainersWithRepeater", val);
				}
			}, prev.pos("bl").adds(16, 2));
			alsoUseContainersWithRepeaterCheckBox.tooltip = alsoUseContainersWithRepeaterTooltip;
			prev = add(new Button(UI.scale(250), "Flower-Menu Auto-Select Manager", false, () -> {
				if(!flowerMenuAutoSelectManagerWindow.attached) {
					this.parent.parent.add(flowerMenuAutoSelectManagerWindow); // ND: this.parent.parent is root widget in login screen or gui in game.
					flowerMenuAutoSelectManagerWindow.show();
				} else {
					flowerMenuAutoSelectManagerWindow.show(!flowerMenuAutoSelectManagerWindow.visible);
				}
			}),prev.pos("bl").adds(0, 4).x(0));
			prev.tooltip = flowerMenuAutoSelectManagerTooltip;
			prev = add(autoReloadCuriositiesFromInventoryCheckBox = new CheckBox("Auto-Reload Curiosities from Inventory"){
				{a = Utils.getprefb("autoStudyFromInventory", false);}
				public void set(boolean val) {
					SAttrWnd.autoReloadCuriositiesFromInventoryCheckBox.a = val;
					Utils.setprefb("autoStudyFromInventory", val);
					a = val;
				}
			}, prev.pos("bl").adds(0, 12).x(0));
			autoReloadCuriositiesFromInventoryCheckBox.tooltip = autoReloadCuriositiesFromInventoryTooltip;
			prev = add(preventTablewareFromBreakingCheckBox = new CheckBox("Prevent Tableware from Breaking"){
				{a = Utils.getprefb("preventTablewareFromBreaking", true);}
				public void set(boolean val) {
					Utils.setprefb("preventTablewareFromBreaking", val);
					a = val;
					TableInfo.preventTablewareFromBreakingCheckBox.a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			preventTablewareFromBreakingCheckBox.tooltip = preventTablewareFromBreakingTooltip;
			prev = add(autoDropLeechesCheckBox = new CheckBox("Auto-Drop Leeches"){
				{a = Utils.getprefb("autoDropLeeches", true);}
				public void set(boolean val) {
					Utils.setprefb("autoDropLeeches", val);
					a = val;
					Equipory.autoDropLeechesCheckBox.a = val;
					if (ui != null && ui.gui != null) {
						Equipory eq = ui.gui.getequipory();
						if (eq != null && eq.myOwnEquipory) {
							eq.checkForLeeches = true;
						}
					}
				}
			}, prev.pos("bl").adds(0, 12));
			prev = add(autoDropTicksCheckBox = new CheckBox("Auto-Drop Ticks"){
				{a = Utils.getprefb("autoDropTicks", true);}
				public void set(boolean val) {
					Utils.setprefb("autoDropTicks", val);
					a = val;
					Equipory.autoDropTicksCheckBox.a = val;
					if (ui != null && ui.gui != null) {
						Equipory eq = ui.gui.getequipory();
						if (eq != null && eq.myOwnEquipory) {
							eq.checkForTicks = true;
						}
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoEquipBunnySlippersPlateBootsCheckBox = new CheckBox("Auto-Equip Bunny Slippers/Plate Boots"){
				{a = Utils.getprefb("autoEquipBunnySlippersPlateBoots", true);}
				public void set(boolean val) {
					Utils.setprefb("autoEquipBunnySlippersPlateBoots", val);
					if (Equipory.autoEquipBunnySlippersPlateBootsCheckBox != null)
						Equipory.autoEquipBunnySlippersPlateBootsCheckBox.a = val;
					a = val;
				}
			}, prev.pos("bl").adds(0, 2));
			autoEquipBunnySlippersPlateBootsCheckBox.tooltip = autoEquipBunnySlippersPlateBootsTooltip;
			prev = add(new Button(UI.scale(250), "Auto-Drop Manager", false, () -> {
				if(!autoDropManagerWindow.attached) {
					this.parent.parent.add(autoDropManagerWindow); // ND: this.parent.parent is root widget in login screen or gui in game.
					autoDropManagerWindow.show();
				} else {
					autoDropManagerWindow.show(!autoDropManagerWindow.visible);
				}
			}),prev.pos("bl").adds(0, 12).x(0));

			prev = add(autoPeaceAnimalsWhenCombatStartsCheckBox = new CheckBox("Auto-Peace Animals when Combat Starts"){
				{a = Utils.getprefb("autoPeaceAnimalsWhenCombatStarts", false);}
				public void set(boolean val) {
					Utils.setprefb("autoPeaceAnimalsWhenCombatStarts", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Autopeace Animals when Combat Starts is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, prev.pos("bl").adds(0, 12));
			autoPeaceAnimalsWhenCombatStartsCheckBox.tooltip = autoPeaceAnimalsWhenCombatStartsTooltip;
			prev = add(autoDrinkingCheckBox = new CheckBox("Auto-Drink Water below threshold:"){
				{a = Utils.getprefb("autoDrinkTeaOrWater", false);}
				public void set(boolean val) {
					Utils.setprefb("autoDrinkTeaOrWater", val);
					a = val;
					if (ui != null && ui.gui != null) {
						String threshold = "75";
						if (!autoDrinkingThresholdTextEntry.text().isEmpty()) threshold = autoDrinkingThresholdTextEntry.text();
						ui.gui.optionInfoMsg("Auto-Drinking Water is now " + (val ? "ENABLED, with a " + threshold + "% treshhold!" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, prev.pos("bl").adds(0, 12));
			autoDrinkingCheckBox.tooltip = autoDrinkingTooltip;
			add(autoDrinkingThresholdTextEntry = new TextEntry(UI.scale(40), Utils.getpref("autoDrinkingThreshold", "75")){
				protected void changed() {
					this.settext(this.text().replaceAll("[^\\d]", "")); // Only numbers
					this.settext(this.text().replaceAll("(?<=^.{2}).*", "")); // No more than 2 digits
					Utils.setpref("autoDrinkingThreshold", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(10, 0));

			prev = add(enableQueuedMovementCheckBox = new CheckBox("Enable Queued Movement Window (Alt+Click)"){
				{a = Utils.getprefb("enableQueuedMovement", true);}
				public void set(boolean val) {
					Utils.setprefb("enableQueuedMovement", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Queued Movement - Checkpoint Route Window is now " + (val ? "ENABLED" : "DISABLED") + ".", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
						if (!val && ui.gui.map.checkpointManager != null)
							ui.gui.map.checkpointManager.wdgmsg("close");
					}
				}
			}, prev.pos("bl").adds(0, 12));
			enableQueuedMovementCheckBox.tooltip = enableQueuedMovementTooltip;

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18));
			pack();
			centerBackButton(backButton, this);
		}
	}


	public static CheckBox overrideCursorItemWhenHoldingAltCheckBox;
	public static CheckBox noCursorItemDroppingAnywhereCheckBox;
	public static CheckBox noCursorItemDroppingInWaterCheckBox;
	public static CheckBox useOGControlsForBuildingAndPlacingCheckBox;
	public static CheckBox useImprovedInventoryTransferControlsCheckBox;
	public static CheckBox tileCenteringCheckBox;
	public static CheckBox clickThroughContainerDecalCheckBox;

	public class AlteredGameplaySettingsPanel extends Panel {

		public AlteredGameplaySettingsPanel(Panel back) {
			Widget prev;

			prev = add(overrideCursorItemWhenHoldingAltCheckBox = new CheckBox("Override Cursor Item and prevent dropping it (hold Alt)"){
				{a = Utils.getprefb("overrideCursorItemWhenHoldingAlt", true);}
				public void set(boolean val) {
					Utils.setprefb("overrideCursorItemWhenHoldingAlt", val);
					a = val;
					if (val) {
						if (noCursorItemDroppingAnywhereCheckBox.a) {// ND: Set it like this so it doesn't do the optionInfoMsg
							noCursorItemDroppingAnywhereCheckBox.a = false;
							Utils.setprefb("noCursorItemDroppingAnywhere", false);
						}
						if (noCursorItemDroppingInWaterCheckBox.a) { // ND: Set it like this so it doesn't do the optionInfoMsg
							noCursorItemDroppingInWaterCheckBox.a = false;
							Utils.setprefb("noCursorItemDroppingInWater", false);
						}
					}
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Override Cursor Item and prevent dropping it (hold Alt) is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, UI.scale(0, 2));
			overrideCursorItemWhenHoldingAltCheckBox.tooltip = overrideCursorItemWhenHoldingAltTooltip;
			prev = add(noCursorItemDroppingAnywhereCheckBox = new CheckBox("No Cursor Item Dropping (Anywhere)"){
				{a = Utils.getprefb("noCursorItemDroppingAnywhere", false);}
				public void set(boolean val) {
					Utils.setprefb("noCursorItemDroppingAnywhere", val);
					a = val;
					if (val) {
						if (overrideCursorItemWhenHoldingAltCheckBox.a) { // ND: Set it like this so it doesn't do the optionInfoMsg
							overrideCursorItemWhenHoldingAltCheckBox.a = false;
							Utils.setprefb("overrideCursorItemWhenHoldingAlt", false);
						}
					}
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("No Item Dropping (Anywhere) is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, prev.pos("bl").adds(0, 12));
			noCursorItemDroppingAnywhereCheckBox.tooltip = noCursorItemDroppingAnywhereTooltip;
			prev = add(noCursorItemDroppingInWaterCheckBox = new CheckBox("No Cursor Item Dropping (Water Only)"){
				{a = Utils.getprefb("noCursorItemDroppingInWater", false);}
				public void set(boolean val) {
					Utils.setprefb("noCursorItemDroppingInWater", val);
					a = val;
					if (val) {
						if (overrideCursorItemWhenHoldingAltCheckBox.a) overrideCursorItemWhenHoldingAltCheckBox.set(false);
					}
					if (ui != null && ui.gui != null) {
						if (!noCursorItemDroppingAnywhereCheckBox.a) {
							ui.gui.optionInfoMsg("No Item Dropping (in Water) is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
						} else {
							ui.gui.optionInfoMsg("No Item Dropping (in Water) is now " + (val ? "ENABLED" : "DISABLED") + "!" + (val ? "" : " (WARNING!!!: No Item Dropping (Anywhere) IS STILL ENABLED, and it overwrites this option!)"), (val ? msgGreen : msgYellow), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
						}
					}
				}
			}, prev.pos("bl").adds(0, 2));
			noCursorItemDroppingInWaterCheckBox.tooltip = noCursorItemDroppingInWaterTooltip;

			prev = add(useOGControlsForBuildingAndPlacingCheckBox = new CheckBox("Use OG controls for Building and Placing"){
				{a = Utils.getprefb("useOGControlsForBuildingAndPlacing", true);}
				public void changed(boolean val) {
					Utils.setprefb("useOGControlsForBuildingAndPlacing", val);
				}
			}, prev.pos("bl").adds(0, 12));
			useOGControlsForBuildingAndPlacingCheckBox.tooltip = useOGControlsForBuildingAndPlacingTooltip;
			prev = add(useImprovedInventoryTransferControlsCheckBox = new CheckBox("Use improved Inventory Transfer controls (hold Alt)"){
				{a = Utils.getprefb("useImprovedInventoryTransferControls", true);}
				public void changed(boolean val) {
					Utils.setprefb("useImprovedInventoryTransferControls", val);
				}
			}, prev.pos("bl").adds(0, 2));
			useImprovedInventoryTransferControlsCheckBox.tooltip = useImprovedInventoryTransferControlsTooltip;

			prev = add(tileCenteringCheckBox = new CheckBox("Tile Centering"){
				{a = Utils.getprefb("tileCentering", false);}
				public void set(boolean val) {
					Utils.setprefb("tileCentering", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Tile Centering is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, prev.pos("bl").adds(0, 12));
			tileCenteringCheckBox.tooltip = tileCenteringTooltip;

			prev = add(clickThroughContainerDecalCheckBox = new CheckBox("Click through Container Decal (Hold Ctrl to pick)"){
				{a = Utils.getprefb("clickThroughContainerDecal", true);}
				public void changed(boolean val) {
					Utils.setprefb("clickThroughContainerDecal", val);
				}
			}, prev.pos("bl").adds(0, 12));

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18));
			pack();
			centerBackButton(backButton, this);
		}
	}


	public static CheckBox allowMouse4CamDragCheckBox;
	public static CheckBox allowMouse5CamDragCheckBox;
	private Label freeCamZoomSpeedLabel;
	public static HSlider freeCamZoomSpeedSlider;
	private Button freeCamZoomSpeedResetButton;
	private Label freeCamRotationSensitivityLabel;
	public static HSlider freeCamRotationSensitivitySlider;
	private Button freeCamRotationSensitivityResetButton;
	private Label freeCamHeightLabel;
	public static HSlider freeCamHeightSlider;
	private Button freeCamHeightResetButton;
	public static CheckBox unlockedOrthoCamCheckBox;
	private Label orthoCamZoomSpeedLabel;
	public static HSlider orthoCamZoomSpeedSlider;
	private Button orthoCamZoomSpeedResetButton;
	private Label orthoCamRotationSensitivityLabel;
	public static HSlider orthoCamRotationSensitivitySlider;
	private Button orthoCamRotationSensitivityResetButton;
	public static CheckBox reverseOrthoCameraAxesCheckBox;
	public static CheckBox reverseFreeCamXAxisCheckBox;
	public static CheckBox reverseFreeCamYAxisCheckBox;
	public static CheckBox lockVerticalAngleAt45DegreesCheckBox;
	public static CheckBox allowLowerFreeCamTiltCheckBox;

	public class CameraSettingsPanel extends Panel {

		public CameraSettingsPanel(Panel back) {
			add(new Label(""), 278, 0); // ND: added this so the window's width does not change when switching camera type and closing/reopening the panel
			Widget TopPrev; // ND: these are always visible at the top, with either camera settings
			Widget FreePrev; // ND: used to calculate the positions for the Free camera settings
			Widget OrthoPrev; // ND: used to calculate the positions for the Ortho camera settings

			TopPrev = add(new Label("Selected Camera Type:"), 0, 0);{
				RadioGroup camGrp = new RadioGroup(this) {
					public void changed(int btn, String lbl) {
						try {
							if(btn==0) {
								Utils.setpref("defcam", "Free");
								setFreeCameraSettingsVisibility(true);
								setOrthoCameraSettingsVisibility(false);
								MapView.currentCamera = 1;
								if (ui != null && ui.gui != null && ui.gui.map != null) {
									ui.gui.map.setcam("Free");
								}
							}
							if(btn==1) {
								Utils.setpref("defcam", "Ortho");
								setFreeCameraSettingsVisibility(false);
								setOrthoCameraSettingsVisibility(true);
								MapView.currentCamera = 2;
								if (ui != null && ui.gui != null && ui.gui.map != null) {
									ui.gui.map.setcam("Ortho");
								}
							}
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				};
			TopPrev = camGrp.add("Free Camera", TopPrev.pos("bl").adds(16, 2));
			TopPrev = camGrp.add("Ortho Camera", TopPrev.pos("bl").adds(0, 1));
			TopPrev = add(new Label("Camera Dragging:"), TopPrev.pos("bl").adds(0, 6).x(0));
				TopPrev = add(allowMouse4CamDragCheckBox = new CheckBox("Also allow Mouse 4 Button to drag the Camera"){
					{a = (Utils.getprefb("allowMouse4CamDrag", false));}
					public void changed(boolean val) {
						Utils.setprefb("allowMouse4CamDrag", val);
					};
				}, TopPrev.pos("bl").adds(12, 2));
				TopPrev = add(allowMouse5CamDragCheckBox = new CheckBox("Also allow Mouse 5 Button to drag the Camera"){
					{a = Utils.getprefb("allowMouse5CamDrag", false);}
					public void changed(boolean val) {
						Utils.setprefb("allowMouse5CamDrag", val);
					}
				}, TopPrev.pos("bl").adds(0, 2));
			TopPrev = add(new Label("Selected Camera Settings:"), TopPrev.pos("bl").adds(0, 6).x(0));
			// ND: The Ortho Camera Settings
			OrthoPrev = add(reverseOrthoCameraAxesCheckBox = new CheckBox("Reverse Ortho Look Axis"){
				{a = (Utils.getprefb("reverseOrthoCamAxis", true));}
				public void changed(boolean val) {
					Utils.setprefb("reverseOrthoCamAxis", val);
				};
			}, TopPrev.pos("bl").adds(12, 2));
			reverseOrthoCameraAxesCheckBox.tooltip = reverseOrthoCameraAxesTooltip;
			OrthoPrev = add(unlockedOrthoCamCheckBox = new CheckBox("Unlocked Ortho Camera"){
				{a = Utils.getprefb("unlockedOrthoCam", true);}
				public void changed(boolean val) {
					Utils.setprefb("unlockedOrthoCam", val);
				}
			}, OrthoPrev.pos("bl").adds(0, 2));
			unlockedOrthoCamCheckBox.tooltip = unlockedOrthoCamTooltip;
			OrthoPrev = add(orthoCamZoomSpeedLabel = new Label("Ortho Camera Zoom Speed:"), OrthoPrev.pos("bl").adds(0, 10).x(0));
			OrthoPrev = add(orthoCamZoomSpeedSlider = new HSlider(UI.scale(200), 2, 40, Utils.getprefi("orthoCamZoomSpeed", 10)) {
				public void changed() {
					Utils.setprefi("orthoCamZoomSpeed", val);
				}
			}, OrthoPrev.pos("bl").adds(0, 4));
			add(orthoCamZoomSpeedResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				orthoCamZoomSpeedSlider.val = 10;
				Utils.setprefi("orthoCamZoomSpeed", 10);
			}), OrthoPrev.pos("bl").adds(210, -20));
			orthoCamZoomSpeedResetButton.tooltip = resetButtonTooltip;
				OrthoPrev = add(orthoCamRotationSensitivityLabel = new Label("Ortho Camera Rotation Sensitivity:"), OrthoPrev.pos("bl").adds(0, 10).x(0));
				OrthoPrev = add(orthoCamRotationSensitivitySlider = new HSlider(UI.scale(200), 100, 1000, Utils.getprefi("orthoCamRotationSensitivity", 1000)) {
					public void changed() {
						Utils.setprefi("orthoCamRotationSensitivity", val);
					}
				}, OrthoPrev.pos("bl").adds(0, 4));
				add(orthoCamRotationSensitivityResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
					orthoCamRotationSensitivitySlider.val = 1000;
					Utils.setprefi("orthoCamRotationSensitivity", 1000);
				}), OrthoPrev.pos("bl").adds(210, -20));
				orthoCamRotationSensitivityResetButton.tooltip = resetButtonTooltip;

			// ND: The Free Camera Settings
			FreePrev = add(reverseFreeCamXAxisCheckBox = new CheckBox("Reverse X Axis"){
				{a = (Utils.getprefb("reverseFreeCamXAxis", true));}
				public void changed(boolean val) {
					Utils.setprefb("reverseFreeCamXAxis", val);
				}
			}, TopPrev.pos("bl").adds(12, 2));
			add(reverseFreeCamYAxisCheckBox = new CheckBox("Reverse Y Axis"){
				{a = (Utils.getprefb("reverseFreeCamYAxis", true));}
				public void changed(boolean val) {
					Utils.setprefb("reverseFreeCamYAxis", val);
				}
			}, FreePrev.pos("ul").adds(110, 0));
			FreePrev = add(lockVerticalAngleAt45DegreesCheckBox = new CheckBox("Lock Vertical Angle at 45°"){
				{a = (Utils.getprefb("lockVerticalAngleAt45Degrees", false));}
				public void changed(boolean val) {
					Utils.setprefb("lockVerticalAngleAt45Degrees", val);
					if (ui.gui.map != null)
						if (ui.gui.map.camera instanceof MapView.FreeCam)
							((MapView.FreeCam)ui.gui.map.camera).telev = (float)Math.PI / 4.0f;
				}
			}, FreePrev.pos("bl").adds(0, 2));
			FreePrev = add(allowLowerFreeCamTiltCheckBox = new CheckBox("Enable Lower Tilting Angle", Color.RED){
				{a = (Utils.getprefb("allowLowerTiltBool", false));}
				public void changed(boolean val) {
					Utils.setprefb("allowLowerTiltBool", val);
				}
			}, FreePrev.pos("bl").adds(0, 2));
			allowLowerFreeCamTiltCheckBox.tooltip = allowLowerFreeCamTiltTooltip;
			allowLowerFreeCamTiltCheckBox.lbl = Text.create("Enable Lower Tilting Angle", PUtils.strokeImg(Text.std.render("Enable Lower Tilting Angle", new Color(185,0,0,255))));
			FreePrev = add(freeCamZoomSpeedLabel = new Label("Free Camera Zoom Speed:"), FreePrev.pos("bl").adds(0, 10).x(0));
			FreePrev = add(freeCamZoomSpeedSlider = new HSlider(UI.scale(200), 4, 40, Utils.getprefi("freeCamZoomSpeed", 25)) {
				public void changed() {
					Utils.setprefi("freeCamZoomSpeed", val);
				}
			}, FreePrev.pos("bl").adds(0, 4));
			add(freeCamZoomSpeedResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				freeCamZoomSpeedSlider.val = 25;
				Utils.setprefi("freeCamZoomSpeed", 25);
			}), FreePrev.pos("bl").adds(210, -20));
			freeCamZoomSpeedResetButton.tooltip = resetButtonTooltip;
			FreePrev = add(freeCamRotationSensitivityLabel = new Label("Free Camera Rotation Sensitivity:"), FreePrev.pos("bl").adds(0, 10).x(0));
			FreePrev = add(freeCamRotationSensitivitySlider = new HSlider(UI.scale(200), 100, 1000, Utils.getprefi("freeCamRotationSensitivity", 1000)) {
				public void changed() {
					Utils.setprefi("freeCamRotationSensitivity", val);
				}
			}, FreePrev.pos("bl").adds(0, 4));
			add(freeCamRotationSensitivityResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				freeCamRotationSensitivitySlider.val = 1000;
				Utils.setprefi("freeCamRotationSensitivity", 1000);
			}), FreePrev.pos("bl").adds(210, -20));
			freeCamRotationSensitivityResetButton.tooltip = resetButtonTooltip;
			FreePrev = add(freeCamHeightLabel = new Label("Free Camera Height:"), FreePrev.pos("bl").adds(0, 10));
			freeCamHeightLabel.tooltip = freeCamHeightTooltip;
			FreePrev = add(freeCamHeightSlider = new HSlider(UI.scale(200), 10, 300, (Math.round((float) Utils.getprefd("cameraHeightDistance", 15f)))*10) {
				public void changed() {
					Utils.setprefd("cameraHeightDistance", (float) (val/10));
				}
			}, FreePrev.pos("bl").adds(0, 4));
			freeCamHeightSlider.tooltip = freeCamHeightTooltip;
			add(freeCamHeightResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				freeCamHeightSlider.val = 150;
				Utils.setprefd("cameraHeightDistance", 15f);
			}), FreePrev.pos("bl").adds(210, -20));
			freeCamHeightResetButton.tooltip = resetButtonTooltip;

			// ND: Finally, check which camera is selected and set the right options to be visible
			String startupSelectedCamera = Utils.getpref("defcam", "Free");
			if (startupSelectedCamera.equals("Free") || startupSelectedCamera.equals("worse") || startupSelectedCamera.equals("follow")){
				camGrp.check(0);
				Utils.setpref("defcam", "Free");
				setFreeCameraSettingsVisibility(true);
				setOrthoCameraSettingsVisibility(false);
				MapView.currentCamera = 1;
			}
			else {
				camGrp.check(1);
				Utils.setpref("defcam", "Ortho");
				setFreeCameraSettingsVisibility(false);
				setOrthoCameraSettingsVisibility(true);
				MapView.currentCamera = 2;
			}
			}

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), FreePrev.pos("bl").adds(0, 18));
			pack();
			centerBackButton(backButton, this);
		}
		private void setFreeCameraSettingsVisibility(boolean bool){
			freeCamZoomSpeedLabel.visible = bool;
			freeCamZoomSpeedSlider.visible = bool;
			freeCamZoomSpeedResetButton.visible = bool;
			freeCamRotationSensitivityLabel.visible = bool;
			freeCamRotationSensitivitySlider.visible = bool;
			freeCamRotationSensitivityResetButton.visible = bool;
			freeCamHeightLabel.visible = bool;
			freeCamHeightSlider.visible = bool;
			freeCamHeightResetButton.visible = bool;
			lockVerticalAngleAt45DegreesCheckBox.visible = bool;
			allowLowerFreeCamTiltCheckBox.visible = bool;
			reverseFreeCamXAxisCheckBox.visible = bool;
			reverseFreeCamYAxisCheckBox.visible = bool;
		}
		private void setOrthoCameraSettingsVisibility(boolean bool){
			unlockedOrthoCamCheckBox.visible = bool;
			orthoCamZoomSpeedLabel.visible = bool;
			orthoCamZoomSpeedSlider.visible = bool;
			orthoCamZoomSpeedResetButton.visible = bool;
			orthoCamRotationSensitivityLabel.visible = bool;
			orthoCamRotationSensitivitySlider.visible = bool;
			orthoCamRotationSensitivityResetButton.visible = bool;
			reverseOrthoCameraAxesCheckBox.visible = bool;
		}
	}

	private Label nightVisionLabel;
	public static HSlider nightVisionSlider;
	private Button nightVisionResetButton;
	public static CheckBox simplifiedCropsCheckBox;
	public static CheckBox simplifiedForageablesCheckBox;
	public static CheckBox hideFlavorObjectsCheckBox;
	public static CheckBox flatWorldCheckBox;
	public static CheckBox disableTileSmoothingCheckBox;
	public static CheckBox disableTileTransitionsCheckBox;
	public static CheckBox flatCaveWallsCheckBox;
	public static CheckBox straightCliffEdgesCheckBox;
	public static CheckBox disableSeasonalGroundColorsCheckBox;
	public static CheckBox disableGroundCloudShadowsCheckBox;
	public static CheckBox disableRainCheckBox;
	public static CheckBox disableWetGroundOverlayCheckBox;
	public static CheckBox disableSnowingCheckBox;
	public static HSlider treeAndBushScaleSlider;
	private Button treeAndBushScaleResetButton;
	public static CheckBox disableTreeAndBushSwayingCheckBox;
	public static CheckBox disableIndustrialSmokeCheckBox;
	public static CheckBox disableScentSmokeCheckBox;
	public static CheckBox flatCupboardsCheckBox;
	public static CheckBox disableHerbalistTablesVarMatsCheckBox;
	public static CheckBox disableCupboardsVarMatsCheckBox;
	public static CheckBox disableChestsVarMatsCheckBox;
	public static CheckBox disableMetalCabinetsVarMatsCheckBox;
	public static CheckBox disableTrellisesVarMatsCheckBox;
	public static CheckBox disableSmokeShedsVarMatsCheckBox;
	public static CheckBox disableAllObjectsVarMatsCheckBox;
	public static CheckBox disableValhallaFilterCheckBox;
	public static CheckBox disableScreenShakingCheckBox;
	public static CheckBox disableHempHighCheckBox;
	public static CheckBox disableOpiumHighCheckBox;
	public static CheckBox disableLibertyCapsHighCheckBox;
	public static CheckBox disableDrunkennessDistortionCheckBox;
	public static HSlider palisadesAndBrickWallsScaleSlider;
	private Button palisadesAndBrickWallsScaleResetButton;
	public static CheckBox enableSkyboxCheckBox;

	public class WorldGraphicsSettingsPanel extends Panel {

		public WorldGraphicsSettingsPanel(Panel back) {
			Widget leftColumn;
			Widget rightColumn;
			add(new Label(""), 278, 0); // To fix window width

			leftColumn = add(nightVisionLabel = new Label("Night Vision / Brighter World:"), 0, 0);
			nightVisionLabel.tooltip = nightVisionTooltip;
			Glob.nightVisionBrightness = Utils.getprefd("nightVisionSetting", 0.0);
			leftColumn = add(nightVisionSlider = new HSlider(UI.scale(200), 0, 650, (int)(Glob.nightVisionBrightness*1000)) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = (int)(Glob.nightVisionBrightness*1000);
				}
				public void changed() {
					Glob.nightVisionBrightness = val/1000.0;
					Utils.setprefd("nightVisionSetting", val/1000.0);
					if(ui.sess != null && ui.sess.glob != null) {
						ui.sess.glob.brighten();
					}
				}
			}, leftColumn.pos("bl").adds(0, 6));
			nightVisionSlider.tooltip = nightVisionTooltip;
			add(nightVisionResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				Glob.nightVisionBrightness = 0.0;
				nightVisionSlider.val = 0;
				Utils.setprefd("nightVisionSetting", 0.0);
				if(ui.sess != null && ui.sess.glob != null) {
					ui.sess.glob.brighten();
				}
			}), leftColumn.pos("bl").adds(210, -20));
			nightVisionResetButton.tooltip = resetButtonTooltip;
			leftColumn = add(flatWorldCheckBox = new CheckBox("Flat World"){
				{a = Utils.getprefb("flatWorld", false);}
				public void changed(boolean val) {
					Utils.setprefb("flatWorld", val);
					if (ui.sess != null)
						ui.sess.glob.map.resetMap();
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Flat World is now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
					}
				}
			}, leftColumn.pos("bl").adds(12, 8));
			flatWorldCheckBox.tooltip = flatWorldTooltip;
			leftColumn = add(disableTileSmoothingCheckBox = new CheckBox("Disable Tile Smoothing"){
				{a = Utils.getprefb("disableTileSmoothing", false);}
				public void changed(boolean val) {
					Utils.setprefb("disableTileSmoothing", val);
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Tile Smoothing is now " + (val ? "DISABLED" : "ENABLED") + "!", (val ? msgRed : msgGreen), Audio.resclip(val ? Toggle.sfxoff : Toggle.sfxon));
					}
				}
			}, leftColumn.pos("bl").adds(0, 2));
			disableTileSmoothingCheckBox.tooltip = disableTileSmoothingTooltip;
			leftColumn = add(disableTileTransitionsCheckBox = new CheckBox("Disable Tile Transitions"){
				{a = Utils.getprefb("disableTileTransitions", false);}
				public void changed(boolean val) {
					Utils.setprefb("disableTileTransitions", val);
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Tile Transitions are now " + (val ? "DISABLED" : "ENABLED") + "!", (val ? msgRed : msgGreen), Audio.resclip(val ? Toggle.sfxoff : Toggle.sfxon));
					}
				}
			}, leftColumn.pos("bl").adds(0, 2));
			disableTileTransitionsCheckBox.tooltip = disableTileTransitionsTooltip;
			leftColumn = add(hideFlavorObjectsCheckBox = new CheckBox("Hide Flavor Objects"){
				{a = Utils.getprefb("hideFlavorObjects", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideFlavorObjects", val);
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					if (ui != null && ui.gui != null) {
						ui.gui.optionInfoMsg("Flavor Objects are now " + (val ? "HIDDEN" : "SHOWN") + "!", (val ? msgGray : msgGreen), Audio.resclip(val ? Toggle.sfxoff : Toggle.sfxon));
					}
				}
			}, leftColumn.pos("bl").adds(0, 12));
			hideFlavorObjectsCheckBox.tooltip = hideFlavorObjectsTooltip;
			leftColumn = add(simplifiedCropsCheckBox = new CheckBox("Simplified Crops (Requires Reload)"){
				{a = Utils.getprefb("simplifiedCrops", false);}
				public void changed(boolean val) {
					Utils.setprefb("simplifiedCrops", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			simplifiedCropsCheckBox.tooltip = simplifiedCropsTooltip;
			leftColumn = add(simplifiedForageablesCheckBox = new CheckBox("Simplified Forageables (Requires Reload)"){
				{a = Utils.getprefb("simplifiedForageables", false);}
				public void changed(boolean val) {
					Utils.setprefb("simplifiedForageables", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			simplifiedForageablesCheckBox.tooltip = simplifiedForageablesTooltip;
			leftColumn = add(flatCaveWallsCheckBox = new CheckBox("Flat Cave Walls"){
				{a = Utils.getprefb("flatCaveWalls", false);}
				public void changed(boolean val) {
					Utils.setprefb("flatCaveWalls", val);
					if (ui.sess != null)
						ui.sess.glob.map.invalidateAll();
					if (ui != null && ui.gui != null)
						ui.gui.optionInfoMsg("Flat Cave Walls are now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}, leftColumn.pos("bl").adds(0, 12));
			flatCaveWallsCheckBox.tooltip = flatCaveWallsTooltip;
			leftColumn = add(straightCliffEdgesCheckBox = new CheckBox("Straight Cliff Edges"){
				{a = Utils.getprefb("straightCliffEdges", false);}
				public void changed(boolean val) {
					Utils.setprefb("straightCliffEdges", val);
					if (ui.sess != null)
						ui.sess.glob.map.resetMap();
					if (ui != null && ui.gui != null)
						ui.gui.optionInfoMsg("Straight Cliff Edges are now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? msgGreen : msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
				}
			}, leftColumn.pos("bl").adds(0, 2));
			straightCliffEdgesCheckBox.tooltip = straightCliffEdgesTooltip;
			leftColumn = add(new Label("Other Altered Objects:"), leftColumn.pos("bl").adds(0, 10).x(UI.scale(0)));
			leftColumn = add(flatCupboardsCheckBox = new CheckBox("Flat Cupboards"){
				{a = (Utils.getprefb("flatCupboards", true));}
				public void set(boolean val) {
					Utils.setprefb("flatCupboards", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateCustomSizeAndRotation);
						ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
						ui.gui.map.updatePlobCustomSizeAndRotation();
						ui.gui.map.updatePlobCollisionBox();
					}
				}
			}, leftColumn.pos("bl").adds(12, 8));
			flatCupboardsCheckBox.tooltip = flatCupboardsTooltip;

			// TODO: ND: Would be nice if this was a scrollable list with selectable items, rather than individual checkboxes
			leftColumn = add(new Label("Disable Variable Materials for Objects:"), leftColumn.pos("bl").adds(0, 10).x(UI.scale(0)));
			leftColumn = add(disableHerbalistTablesVarMatsCheckBox = new CheckBox("Herbalist Tables Variable Materials"){
				{a = (Utils.getprefb("disableHerbalistTablesVarMats", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableHerbalistTablesVarMats", val);
				}
			}, leftColumn.pos("bl").adds(12, 8));
			leftColumn = add(disableCupboardsVarMatsCheckBox = new CheckBox("Cupboards Variable Materials"){
				{a = (Utils.getprefb("disableCupboardsVarMats", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableCupboardsVarMats", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			leftColumn = add(disableChestsVarMatsCheckBox = new CheckBox("Chests Variable Materials"){
				{a = (Utils.getprefb("disableChestsVarMats", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableChestsVarMats", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			leftColumn = add(disableMetalCabinetsVarMatsCheckBox = new CheckBox("Metal Cabinets Variable Materials"){
				{a = (Utils.getprefb("disableMetalCabinetsVarMats", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableMetalCabinetsVarMats", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			leftColumn = add(disableTrellisesVarMatsCheckBox = new CheckBox("Trellises Variable Materials"){
				{a = (Utils.getprefb("disableTrellisesVarMats", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableTrellisesVarMats", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));
			leftColumn = add(disableSmokeShedsVarMatsCheckBox = new CheckBox("Smoke Sheds Variable Materials"){
				{a = (Utils.getprefb("disableSmokeShedsVarMats", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableSmokeShedsVarMats", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));

			leftColumn = add(disableAllObjectsVarMatsCheckBox = new CheckBox("ALL OBJECTS Variable Materials (YES, ALL!)"){
				{a = (Utils.getprefb("disableAllObjectsVarMats", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableAllObjectsVarMats", val);
				}
			}, leftColumn.pos("bl").adds(0, 2));

			leftColumn = add(new Label("Palisades & Brick Walls Scale:"), leftColumn.pos("bl").adds(0, 10).x(0));
			leftColumn.tooltip = palisadesAndBrickWallsScaleTooltip;
			leftColumn = add(palisadesAndBrickWallsScaleSlider = new HSlider(UI.scale(200), 40, 100, Utils.getprefi("palisadesAndBrickWallsScale", 100)) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = Utils.getprefi("palisadesAndBrickWallsScale", 100);
				}
				public void changed() {
					Utils.setprefi("palisadesAndBrickWallsScale", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::reloadPalisadeScale);
					}
				}
			}, leftColumn.pos("bl").adds(0, 6));
			palisadesAndBrickWallsScaleSlider.tooltip = palisadesAndBrickWallsScaleTooltip;
			add(palisadesAndBrickWallsScaleResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				palisadesAndBrickWallsScaleSlider.val = 100;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::reloadPalisadeScale);
				Utils.setprefi("palisadesAndBrickWallsScale", 100);
			}), leftColumn.pos("bl").adds(210, -20));
			palisadesAndBrickWallsScaleResetButton.tooltip = resetButtonTooltip;

			rightColumn = add(enableSkyboxCheckBox = new CheckBox("Enable Skybox"){
				{a = (Utils.getprefb("enableSkybox", false));}
				public void changed(boolean val) {
					Utils.setprefb("enableSkybox", val);
				}
			}, UI.scale(302, 0));
			enableSkyboxCheckBox.tooltip = enableSkyboxTooltip;

			rightColumn = add(new Label("Skybox Style:"), rightColumn.pos("bl").adds(0, 4));
			List<String> skyboxStyles = Arrays.asList("Clouds", "Galaxy");
			add(new OldDropBox<String>(skyboxStyles.size(), skyboxStyles) {
				{
					super.change(skyboxStyles.get(Utils.getprefi("skyboxStyle", 0)));
				}
				@Override
				protected String listitem(int i) {
					return skyboxStyles.get(i);
				}
				@Override
				protected int listitems() {
					return skyboxStyles.size();
				}
				@Override
				protected void drawitem(GOut g, String item, int i) {
					g.aimage(Text.renderstroked(item).tex(), Coord.of(UI.scale(3), g.sz().y / 2), 0.0, 0.5);
				}
				@Override
				public void change(String item) {
					super.change(item);
					for (int i = 0; i < skyboxStyles.size(); i++){
						if (item.equals(skyboxStyles.get(i))){
							Utils.setprefi("skyboxStyle", i);
							if (enableSkyboxCheckBox.a) { // ND: It's easier to just reset the checkbox to load the new skybox, haha...
								enableSkyboxCheckBox.set(false);
								if (skyboxFuture != null)
									skyboxFuture.cancel(true);
								skyboxFuture = skyboxExecutor.scheduleWithFixedDelay(OptWnd.this::resetSkyboxCheckbox, 200, 3000, TimeUnit.MILLISECONDS);
							}
						}
					}
				}
			}, rightColumn.pos("ur").adds(4, 0));

			rightColumn = add(new Label("Trees & Bushes Scale:"), rightColumn.pos("bl").adds(0, 14).xs(290));
			rightColumn = add(treeAndBushScaleSlider = new HSlider(UI.scale(200), 30, 100, Utils.getprefi("treeAndBushScale", 100)) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = Utils.getprefi("treeAndBushScale", 100);
				}
				public void changed() {
					Utils.setprefi("treeAndBushScale", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::reloadTreeScale);
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.sess.glob.oc.gobAction(Gob::updateCollisionBoxes);
					}
				}
			}, rightColumn.pos("bl").adds(0, 6));
			add(treeAndBushScaleResetButton = new Button(UI.scale(70), "Reset", false).action(() -> {
				treeAndBushScaleSlider.val = 100;
				if (ui != null && ui.gui != null)
					ui.sess.glob.oc.gobAction(Gob::reloadTreeScale);
				Utils.setprefi("treeAndBushScale", 100);
			}), rightColumn.pos("bl").adds(210, -20));
			treeAndBushScaleResetButton.tooltip = resetButtonTooltip;
			rightColumn = add(disableTreeAndBushSwayingCheckBox = new CheckBox("Disable Tree & Bush Swaying"){
				{a = Utils.getprefb("disableTreeAndBushSwaying", false);}
				public void changed(boolean val) {
					Utils.setprefb("disableTreeAndBushSwaying", val);
					if (ui != null && ui.gui != null)
						ui.sess.glob.oc.gobAction(Gob::reloadTreeSwaying);
				}
			}, rightColumn.pos("bl").adds(12, 14));
			disableTreeAndBushSwayingCheckBox.tooltip = disableTreeAndBushSwayingTooltip;
			rightColumn = add(disableIndustrialSmokeCheckBox = new CheckBox("Disable Industrial Smoke (Requires Reload)"){
				{a = (Utils.getprefb("disableIndustrialSmoke", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableIndustrialSmoke", val);
					if (val) synchronized (ui.sess.glob.oc){
						for(Gob gob : ui.sess.glob.oc){
							if(gob.getres() != null && !gob.getres().name.equals("gfx/terobjs/clue")){
								synchronized (gob.ols){
									for(Gob.Overlay ol : gob.ols){
										if(ol.spr!= null && ol.spr.res != null && ol.spr.res.name.contains("ismoke")){
											gob.removeOl(ol);
										}
									}
								}
								gob.ols.clear();
							}
						}
					}
				}
			}, rightColumn.pos("bl").adds(0, 2));
			disableIndustrialSmokeCheckBox.tooltip = disableIndustrialSmokeTooltip;
			rightColumn = add(disableScentSmokeCheckBox = new CheckBox("Disable Scent Smoke (Requires Reload)"){
				{a = (Utils.getprefb("disableScentSmoke", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableScentSmoke", val);
					if (val) synchronized (ui.sess.glob.oc){
						synchronized (ui.sess.glob.oc){
							for(Gob gob : ui.sess.glob.oc){
								if(gob.getres() != null && gob.getres().name.equals("gfx/terobjs/clue")){
									synchronized (gob.ols){
										for(Gob.Overlay ol : gob.ols){
											gob.removeOl(ol);
										}
									}
									gob.ols.clear();
								}
							}
						}
					}
				}
			}, rightColumn.pos("bl").adds(0, 2));
			disableScentSmokeCheckBox.tooltip = disableScentSmokeTooltip;

			rightColumn = add(new Label("World Effects:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(290)));
			rightColumn = add(disableSeasonalGroundColorsCheckBox = new CheckBox("Disable Seasonal Ground Colors"){
				{a = (Utils.getprefb("disableSeasonalGroundColors", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableSeasonalGroundColors", val);
				}
			}, rightColumn.pos("bl").adds(12, 8));
			disableSeasonalGroundColorsCheckBox.tooltip = disableSeasonalGroundColorsTooltip;

			rightColumn = add(disableGroundCloudShadowsCheckBox = new CheckBox("Disable Ground Cloud Shadows"){
				{a = (Utils.getprefb("disableGroundCloudShadows", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableGroundCloudShadows", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			disableGroundCloudShadowsCheckBox.tooltip = disableGroundCloudShadowsTooltip;

			rightColumn = add(disableRainCheckBox = new CheckBox("Disable Raining Particles"){
				{a = (Utils.getprefb("disableRain", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableRain", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));

			rightColumn = add(disableWetGroundOverlayCheckBox = new CheckBox("Disable Wet Ground Overlay"){
				{a = (Utils.getprefb("disableWetGroundOverlay", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableWetGroundOverlay", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			disableWetGroundOverlayCheckBox.tooltip = disableWetGroundOverlayTooltip;

			rightColumn = add(disableSnowingCheckBox = new CheckBox("Disable Snowing Particles"){
				{a = (Utils.getprefb("disableSnowing", false));}
				public void changed(boolean val) {
					Utils.setprefb("disableSnowing", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));

			rightColumn = add(new Label("Screen Effects:"), rightColumn.pos("bl").adds(0, 10).x(UI.scale(290)));
			rightColumn = add(disableValhallaFilterCheckBox = new CheckBox("Disable Valhalla Desaturation Filter"){
				{a = (Utils.getprefb("disableValhallaFilter", true));}
				public void changed(boolean val) {
					Utils.setprefb("disableValhallaFilter", val);
				}
			}, rightColumn.pos("bl").adds(12, 8));
			disableValhallaFilterCheckBox.tooltip = disableValhallaFilterTooltip;

			rightColumn = add(disableScreenShakingCheckBox = new CheckBox("Disable Screen Shaking"){
				{a = (Utils.getprefb("disableScreenShaking", true));}
				public void changed(boolean val) {
					Utils.setprefb("disableScreenShaking", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			disableScreenShakingCheckBox.tooltip = disableScreenShakingTooltip;

			rightColumn = add(disableHempHighCheckBox = new CheckBox("Disable Hemp High"){
				{a = (Utils.getprefb("disableHempHigh", true));}
				public void changed(boolean val) {
					Utils.setprefb("disableHempHigh", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			rightColumn = add(disableOpiumHighCheckBox = new CheckBox("Disable Opium High"){
				{a = (Utils.getprefb("disableOpiumHigh", true));}
				public void changed(boolean val) {
					Utils.setprefb("disableOpiumHigh", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			rightColumn = add(disableLibertyCapsHighCheckBox = new CheckBox("Disable Liberty Caps High"){
				{a = (Utils.getprefb("disableLibertyCapsHigh", true));}
				public void changed(boolean val) {
					Utils.setprefb("disableLibertyCapsHigh", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));
			disableLibertyCapsHighCheckBox.setTextColor(Color.red);
			disableLibertyCapsHighCheckBox.tooltip = disableLibertyCapsHighTooltip;
			rightColumn = add(disableDrunkennessDistortionCheckBox = new CheckBox("Disable Drunkenness Distortion"){
				{a = (Utils.getprefb("disableDrunkennessDistortion", true));}
				public void changed(boolean val) {
					Utils.setprefb("disableDrunkennessDistortion", val);
				}
			}, rightColumn.pos("bl").adds(0, 2));

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), leftColumn.pos("bl").adds(0, 38));
			pack();
			centerBackButton(backButton, this);
		}
	}

	public static CheckBox toggleGobHidingCheckBox;
	public static CheckBox alsoFillTheHidingBoxesCheckBox;
	public static CheckBox dontHideObjectsThatHaveTheirMapIconEnabledCheckBox;
	public static CheckBox hideTreesCheckbox;
	public static CheckBox hideBushesCheckbox;
	public static CheckBox hideBouldersCheckbox;
	public static CheckBox hideTreeLogsCheckbox;
	public static CheckBox hideWallsCheckbox;
	public static CheckBox hideHousesCheckbox;
	public static CheckBox hideCropsCheckbox;
	public static CheckBox hideTrellisCheckbox;
	public static CheckBox hideStockpilesCheckbox;
	public static ColorOptionWidget hiddenObjectsColorOptionWidget;
	public static String[] hiddenObjectsColorSetting = Utils.getprefsa("hidingBox" + "_colorSetting", new String[]{"0", "225", "255", "170"});

	public class HidingSettingsPanel extends Panel {
		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public HidingSettingsPanel(Panel back) {
			Widget prev;
			Widget prev2;
			prev = add(toggleGobHidingCheckBox = new CheckBox("Hide Objects"){
				{a = (Utils.getprefb("hideObjects", false));}
				public void set(boolean val) {
					Utils.setprefb("hideObjects", val);
					a = val;
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, 0, 10);
			toggleGobHidingCheckBox.tooltip = genericHasKeybindTooltip;

			add(alsoFillTheHidingBoxesCheckBox = new CheckBox("Also fill the Hiding Boxes"){
				{a = (Utils.getprefb("alsoFillTheHidingBoxes", true));}
				public void changed(boolean val) {
					Utils.setprefb("alsoFillTheHidingBoxes", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("ur").adds(50, 0));
			alsoFillTheHidingBoxesCheckBox.tooltip = RichText.render("Fills in the boxes. Only the outer lines will remain visible through other objects (like cliffs).");

			prev = add(dontHideObjectsThatHaveTheirMapIconEnabledCheckBox = new CheckBox("Don't hide Objects that have their Map Icon Enabled"){
				{a = (Utils.getprefb("dontHideObjectsThatHaveTheirMapIconEnabled", true));}
				public void changed(boolean val) {
					Utils.setprefb("dontHideObjectsThatHaveTheirMapIconEnabled", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(300, 40))), prev.pos("bl").adds(14, 16));
			Widget cont = scroll.cont;
			addbtn(cont, "Toggle object hiding hotkey:", GameUI.kb_toggleHidingBoxes, 0);

			prev = add(hiddenObjectsColorOptionWidget = new ColorOptionWidget("Hidden Objects Box Color:", "hidingBox", 170, Integer.parseInt(hiddenObjectsColorSetting[0]), Integer.parseInt(hiddenObjectsColorSetting[1]), Integer.parseInt(hiddenObjectsColorSetting[2]), Integer.parseInt(hiddenObjectsColorSetting[3]), (Color col) -> {
				HidingBox.SOLID_FILLED = Pipe.Op.compose(new BaseColor(col), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				HidingBox.SOLID_HOLLOW = Pipe.Op.compose(new BaseColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 153)), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}){}, scroll.pos("bl").adds(1, -2));

			prev = add(new Button(UI.scale(70), "Reset", false).action(() -> {
				Utils.setprefsa("hidingBox" + "_colorSetting", new String[]{"0", "225", "255", "170"});
				hiddenObjectsColorOptionWidget.cb.colorChooser.setColor(hiddenObjectsColorOptionWidget.currentColor = new Color(0, 225, 255, 170));
				HidingBox.SOLID_FILLED = Pipe.Op.compose(new BaseColor(hiddenObjectsColorOptionWidget.currentColor), new States.Facecull(States.Facecull.Mode.NONE), Rendered.last);
				HidingBox.SOLID_HOLLOW = Pipe.Op.compose(new BaseColor(new Color(hiddenObjectsColorOptionWidget.currentColor.getRed(), hiddenObjectsColorOptionWidget.currentColor.getGreen(), hiddenObjectsColorOptionWidget.currentColor.getBlue(), 153)), new States.LineWidth(HidingBox.WIDTH), HidingBox.TOP);
				if (ui != null && ui.gui != null) {
					ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
					ui.gui.map.updatePlobHidingBox();
				}
			}), prev.pos("ur").adds(30, 0));
			prev.tooltip = resetButtonTooltip;

			prev = add(new Label("Objects that will be hidden:"), prev.pos("bl").adds(0, 20).x(0));

			prev2 = add(hideTreesCheckbox = new CheckBox("Trees"){
				{a = Utils.getprefb("hideTrees", true);}
				public void changed(boolean val) {
					Utils.setprefb("hideTrees", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(16, 10));

			prev = add(hideBushesCheckbox = new CheckBox("Bushes"){
				{a = Utils.getprefb("hideBushes", true);}
				public void changed(boolean val) {
					Utils.setprefb("hideBushes", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev2.pos("bl").adds(0, 2));

			prev = add(hideBouldersCheckbox = new CheckBox("Boulders"){
				{a = Utils.getprefb("hideBoulders", true);}
				public void changed(boolean val) {
					Utils.setprefb("hideBoulders", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(hideTreeLogsCheckbox = new CheckBox("Tree Logs"){
				{a = Utils.getprefb("hideTreeLogs", true);}
				public void changed(boolean val) {
					Utils.setprefb("hideTreeLogs", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));

			prev = add(hideWallsCheckbox = new CheckBox("Palisades and Brick Walls"){
				{a = Utils.getprefb("hideWalls", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideWalls", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev2.pos("ur").adds(90, 0));
			prev.tooltip = RichText.render("Only wall sections, NOT gates!");

			// TODO ND: Gotta figure out a way to not hide the doors... somehow
			prev = add(hideHousesCheckbox = new CheckBox("Houses"){
				{a = Utils.getprefb("hideHouses", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideHouses", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(hideStockpilesCheckbox = new CheckBox("Stockpiles"){
				{a = Utils.getprefb("hideStockpiles", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideStockpiles", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(hideCropsCheckbox = new CheckBox("Crops"){
				{a = Utils.getprefb("hideCrops", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideCrops", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev.tooltip = RichText.render("These won't show a hiding box, cause there's no collision.", UI.scale(300));

			prev = add(hideTrellisCheckbox = new CheckBox("Trellises"){
				{a = Utils.getprefb("hideTrellis", false);}
				public void changed(boolean val) {
					Utils.setprefb("hideTrellis", val);
					if (ui != null && ui.gui != null) {
						ui.sess.glob.oc.gobAction(Gob::updateHidingBoxes);
						ui.gui.map.updatePlobHidingBox();
					}
				}
			}, prev.pos("bl").adds(0, 2));
			prev.tooltip = RichText.render("This only hides the trellises, and not the crops growing on them.", UI.scale(300));

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}
	}

	public static CheckBox whitePlayerAlarmEnabledCheckbox, whiteVillageOrRealmPlayerAlarmEnabledCheckbox, greenPlayerAlarmEnabledCheckbox,
			redPlayerAlarmEnabledCheckbox, bluePlayerAlarmEnabledCheckbox, tealPlayerAlarmEnabledCheckbox, yellowPlayerAlarmEnabledCheckbox,
			purplePlayerAlarmEnabledCheckbox, orangePlayerAlarmEnabledCheckbox;
	public static TextEntry whitePlayerAlarmFilename, whiteVillageOrRealmPlayerAlarmFilename, greenPlayerAlarmFilename,
			redPlayerAlarmFilename, bluePlayerAlarmFilename, tealPlayerAlarmFilename, yellowPlayerAlarmFilename,
			purplePlayerAlarmFilename, orangePlayerAlarmFilename;
	public static HSlider whitePlayerAlarmVolumeSlider, whiteVillageOrRealmPlayerAlarmVolumeSlider, greenPlayerAlarmVolumeSlider,
			redPlayerAlarmVolumeSlider, bluePlayerAlarmVolumeSlider, tealPlayerAlarmVolumeSlider, yellowPlayerAlarmVolumeSlider,
			purplePlayerAlarmVolumeSlider, orangePlayerAlarmVolumeSlider;

	public static CheckBox combatStartSoundEnabledCheckbox, cleaveSoundEnabledCheckbox, opkSoundEnabledCheckbox,
			ponyPowerSoundEnabledCheckbox, lowEnergySoundEnabledCheckbox;
	public static TextEntry combatStartSoundFilename, cleaveSoundFilename, opkSoundFilename,
			ponyPowerSoundFilename, lowEnergySoundFilename;
	public static HSlider combatStartSoundVolumeSlider, cleaveSoundVolumeSlider, opkSoundVolumeSlider,
			ponyPowerSoundVolumeSlider, lowEnergySoundVolumeSlider;

	Button CustomAlarmManagerButton;

	public class AlarmsAndSoundsSettingsPanel extends Panel {

		public AlarmsAndSoundsSettingsPanel(Panel back) {
			Widget prev;
			AlarmWidgetComponents comps;

			add(new Label("You can add your own alarm sound files in the \"AlarmSounds\" folder.", new Text.Foundry(Text.sans, 12)), 0, 0);
			add(new Label("(The file extension must be .wav)", new Text.Foundry(Text.sans, 12)), UI.scale(0, 16));
			prev = add(new Label("Enabled Player Alarms:"), UI.scale(0, 50));
			prev = add(new Label("Sound File"), prev.pos("ur").add(70, 0));
			prev = add(new Label("Volume"), prev.pos("ur").add(78, 0));

			comps = addAlarmWidget("whitePlayerAlarm", "White OR Unknown:", "ND_YoHeadsUp", true,null, prev);
			{whitePlayerAlarmEnabledCheckbox = comps.checkbox; whitePlayerAlarmFilename = comps.filename; whitePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("whiteVillageOrRealmPlayerAlarm", "Village/Realm Member:", "ND_HelloFriend",true,null, prev);
			{whiteVillageOrRealmPlayerAlarmEnabledCheckbox = comps.checkbox; whiteVillageOrRealmPlayerAlarmFilename = comps.filename; whiteVillageOrRealmPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("greenPlayerAlarm", "Green:", "ND_FlyingTheFriendlySkies", false, BuddyWnd.gc[1], prev);
			{greenPlayerAlarmEnabledCheckbox = comps.checkbox; greenPlayerAlarmFilename = comps.filename; greenPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("redPlayerAlarm", "Red:", "ND_EnemySighted", true, BuddyWnd.gc[2], prev);
			{redPlayerAlarmEnabledCheckbox = comps.checkbox; redPlayerAlarmFilename = comps.filename; redPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("bluePlayerAlarm", "Blue:", "", false, BuddyWnd.gc[3], prev);
			{bluePlayerAlarmEnabledCheckbox = comps.checkbox; bluePlayerAlarmFilename = comps.filename; bluePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("tealPlayerAlarm", "Teal:", "", false, BuddyWnd.gc[4], prev);
			{tealPlayerAlarmEnabledCheckbox = comps.checkbox; tealPlayerAlarmFilename = comps.filename; tealPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("yellowPlayerAlarm", "Yellow:", "", false, BuddyWnd.gc[5], prev);
			{yellowPlayerAlarmEnabledCheckbox = comps.checkbox; yellowPlayerAlarmFilename = comps.filename; yellowPlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("purplePlayerAlarm", "Purple:", "", false, BuddyWnd.gc[6], prev);
			{purplePlayerAlarmEnabledCheckbox = comps.checkbox; purplePlayerAlarmFilename = comps.filename; purplePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("orangePlayerAlarm", "Orange:", "", false, BuddyWnd.gc[7], prev);
			{orangePlayerAlarmEnabledCheckbox = comps.checkbox; orangePlayerAlarmFilename = comps.filename; orangePlayerAlarmVolumeSlider = comps.volume; prev = comps.lastWidget;}

			prev = add(new Label("Enabled Sounds & Alerts:"), prev.pos("bl").add(0, 20).x(0));
			prev = add(new Label("Sound File"), prev.pos("ur").add(69, 0));
			prev = add(new Label("Volume"), prev.pos("ur").add(78, 0));

			comps = addAlarmWidget("combatStartSound", "Combat Started Alert:", "ND_HitAndRun", false, null, prev);
			{combatStartSoundEnabledCheckbox = comps.checkbox; combatStartSoundFilename = comps.filename; combatStartSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("cleaveSound", "Cleave Sound Effect:", "ND_Cleave", true, null, prev);
			{cleaveSoundEnabledCheckbox = comps.checkbox; cleaveSoundFilename = comps.filename; cleaveSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("opkSound", "Oppknock Sound Effect:", "ND_Opk", true, null, prev);
			{opkSoundEnabledCheckbox = comps.checkbox; opkSoundFilename = comps.filename; opkSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("ponyPowerSound", "Pony Power <10% Alert:", "ND_HorseEnergy", true, null, prev);
			{ponyPowerSoundEnabledCheckbox = comps.checkbox; ponyPowerSoundFilename = comps.filename; ponyPowerSoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			comps = addAlarmWidget("lowEnergySound", "Energy <2500% Alert:", "ND_NotEnoughEnergy", true, null, prev);
			{lowEnergySoundEnabledCheckbox = comps.checkbox; lowEnergySoundFilename = comps.filename; lowEnergySoundVolumeSlider = comps.volume; prev = comps.lastWidget;}

			prev = add(CustomAlarmManagerButton = new Button(UI.scale(360), ">>> Other Alarms (Custom Alarm Manager) <<<", () -> {
				if(alarmWindow == null) {
					alarmWindow = this.parent.parent.add(new AlarmWindow());
					alarmWindow.show();
				} else {
					alarmWindow.show(!alarmWindow.visible);
					alarmWindow.bottomNote.settext("NOTE: You can add your own alarm sound files in the \"AlarmSounds\" folder. (The file extension must be .wav)");
					alarmWindow.bottomNote.setcolor(Color.WHITE);
					alarmWindow.bottomNote.c.x = UI.scale(140);
				}
			}),prev.pos("bl").adds(0, 18).x(UI.scale(51)));


			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}

		private AlarmWidgetComponents addAlarmWidget(String prefPrefix, String label, String defaultFilename, boolean defaultEnabled, Color labelColor, Widget prev) {
			AlarmWidgetComponents out = new AlarmWidgetComponents();

			out.checkbox = new CheckBox(label) {
				{ a = Utils.getprefb(prefPrefix + "Enabled", defaultEnabled); }
				public void set(boolean val) {
					Utils.setprefb(prefPrefix + "Enabled", val);
					a = val;
				}
			};
			prev = add(out.checkbox, prev.pos("bl").adds(0, 7).x(0));

			if (labelColor != null) {
				out.checkbox.lbl = Text.create(label, PUtils.strokeImg(Text.std.render(label, labelColor)));
			}

			out.filename = new TextEntry(UI.scale(140), Utils.getpref(prefPrefix + "Filename", defaultFilename)) {
				protected void changed() {
					this.settext(this.text().replaceAll(" ", ""));
					Utils.setpref(prefPrefix + "Filename", this.buf.line());
					super.changed();
				}
			};
			prev = add(out.filename, prev.pos("ur").adds(0, -2).x(UI.scale(143)));

			out.volume = new HSlider(UI.scale(100), 0, 100, Utils.getprefi(prefPrefix + "Volume", 50)) {
				@Override
				public void changed() {
					Utils.setprefi(prefPrefix + "Volume", val);
					super.changed();
				}
			};
			prev = add(out.volume, prev.pos("ur").adds(6, 3));

			TextEntry finalFilename = out.filename;
			HSlider finalVolume = out.volume;
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(MouseDownEvent ev) {
					if (ev.b != 1)
						return true;
					File file = new File(haven.MainFrame.gameDir + "AlarmSounds/" + finalFilename.buf.line() + ".wav");
					if (!file.exists() || file.isDirectory()) {
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!", Color.WHITE);
						return super.mousedown(ev);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS clip = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(clip, finalVolume.val / 50.0));
					} catch (UnsupportedAudioFileException | IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(ev);
				}
			}, prev.pos("ur").adds(6, -5));

			out.lastWidget = prev;
			return out;
		}

		public class AlarmWidgetComponents { // ND: I can't set the static components from within the addAlarmWidget method without using this. It's some Java limitation, idk.
			public CheckBox checkbox;
			public TextEntry filename;
			public HSlider volume;
			public Widget lastWidget;
		}

	}

	public static TextEntry webmapEndpointTextEntry;
	public static CheckBox uploadMapTilesCheckBox;
	public static CheckBox sendLiveLocationCheckBox;
	public static TextEntry liveLocationNameTextEntry;
//	public static Map<Color, Boolean> colorCheckboxesMap = new HashMap<>();
//	static {
//		for (Color color : BuddyWnd.gc) {
//			colorCheckboxesMap.put(color, Utils.getprefb("enableMarkerUpload" + color.getRGB(), false));
//		}
//	}

	public class ServerIntegrationSettingsPanel extends Panel {

		public ServerIntegrationSettingsPanel(Panel back) {
			Widget prev;
			prev = add(new Label("Web Map Integration"), 110, 8);
			prev = add(new Label("Web Map Endpoint:"), prev.pos("bl").adds(0, 16).x(0));
			prev = add(webmapEndpointTextEntry = new TextEntry(UI.scale(220), Utils.getpref("webMapEndpoint", "")){
				protected void changed() {
					Utils.setpref("webMapEndpoint", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(6, 0));
			prev = add(uploadMapTilesCheckBox = new CheckBox("Upload Map Tiles to your Web Map Server"){
				{a = Utils.getprefb("uploadMapTiles", false);}
				public void changed(boolean val) {
					Utils.setprefb("uploadMapTiles", val);
				}
			}, prev.pos("bl").adds(0, 8).x(12));
			uploadMapTilesCheckBox.tooltip = uploadMapTilesTooltip;

			prev = add(sendLiveLocationCheckBox = new CheckBox("Send Live Location to your Web Map Server"){
				{a = Utils.getprefb("enableLocationTracking", false);}
				public void changed(boolean val) {
					Utils.setprefb("enableLocationTracking", val);
				}
			}, prev.pos("bl").adds(0, 12));
			sendLiveLocationCheckBox.tooltip = sendLiveLocationTooltip;

			prev = add(new Label("Your Live Location Name (Req. Relog):"), prev.pos("bl").adds(20, 4));
			prev.tooltip = liveLocationNameTooltip;
			prev = add(liveLocationNameTextEntry = new TextEntry(UI.scale(96), Utils.getpref("liveLocationName", "")){
				protected void changed() {
					Utils.setpref("liveLocationName", this.buf.line());
					super.changed();
				}
			}, prev.pos("ur").adds(6, 0));
			liveLocationNameTextEntry.tooltip = liveLocationNameTooltip;

//			prev = add(new Label("Markers to upload:"), prev.pos("bl").adds(0, 20).x(0));
//
//			for (Map.Entry<Color, Boolean> entry : colorCheckboxesMap.entrySet()) {
//				Color color = entry.getKey();
//				boolean isChecked = entry.getValue();
//
//				CheckBox colorCheckbox = new CheckBox(""){
//					{a = isChecked;}
//					@Override
//					public void draw(GOut g) {
//						g.chcolor(color);
//						g.frect(Coord.z.add(0, (sz.y - box.sz().y) / 2), box.sz());
//						g.chcolor();
//						if(state())
//							g.image(mark, Coord.z.add(0, (sz.y - mark.sz().y) / 2));
//					}
//
//					public void set(boolean val) {
//						Utils.setprefb("enableMarkerUpload" + color.getRGB(), val);
//						colorCheckboxesMap.put(color, val);
//						a = val;
//					}
//				};
//				prev = add(colorCheckbox, prev.pos("ur").adds(10, 0));
//			}

			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}

	}

	public static CheckBox autoLootRingsCheckBox;
	public static CheckBox autoLootNecklaceCheckBox;
	public static CheckBox autoLootWeaponCheckBox;
	public static CheckBox autoLootHelmetCheckBox;
	public static CheckBox autoLootChestArmorCheckBox;
	public static CheckBox autoLootLegArmorCheckBox;
	public static CheckBox autoLootCloakRobeCheckBox;
	public static CheckBox autoLootShirtCheckBox;
	public static CheckBox autoLootPantsCheckBox;
	public static CheckBox autoLootGlovesCheckBox;
	public static CheckBox autoLootBootsCheckBox;
	public static CheckBox autoLootEyewearCheckBox;
	public static CheckBox autoLootMouthCheckBox;
	public static CheckBox autoLootCapeCheckBox;

	public class AutoLootSettingsPanel extends Panel {

		private int addbtn(Widget cont, String nm, KeyBinding cmd, int y) {
			return (cont.addhl(new Coord(0, y), cont.sz.x,
					new Label(nm), new SetButton(UI.scale(140), cmd))
					+ UI.scale(2));
		}

		public AutoLootSettingsPanel(Panel back) {
			Widget prev;

			Scrollport scroll = add(new Scrollport(UI.scale(new Coord(350, 40))), 0, 0);
			prev = scroll.cont;
			addbtn(prev, "Loot Nearest Knocked Player hotkey:", GameUI.kb_lootNearestKnockedPlayer, 0);

			prev = add(new Label("Auto-Loot the following gear from enemies (when stealing):"), prev.pos("bl").adds(0, 6));
			prev = add(autoLootRingsCheckBox = new CheckBox("Rings"){
				{a = Utils.getprefb("autoLootRings", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootRings", val);
				}
			}, prev.pos("bl").adds(0, 4).x(12));
			prev = add(autoLootNecklaceCheckBox = new CheckBox("Necklace"){
				{a = Utils.getprefb("autoLootNecklace", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootNecklace", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootWeaponCheckBox = new CheckBox("Weapon (Try to put in Belt first, then Inventory)"){
				{a = Utils.getprefb("autoLootWeapon", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootWeapon", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootHelmetCheckBox = new CheckBox("Helmet"){
				{a = Utils.getprefb("autoLootHelmet", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootHelmet", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootChestArmorCheckBox = new CheckBox("Chest Armor"){
				{a = Utils.getprefb("autoLootChestArmor", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootChestArmor", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootLegArmorCheckBox = new CheckBox("Leg Armor"){
				{a = Utils.getprefb("autoLootLegArmor", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootLegArmor", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootCloakRobeCheckBox = new CheckBox("Cloak/Robe"){
				{a = Utils.getprefb("autoLootCloakRobe", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootCloakRobe", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootShirtCheckBox = new CheckBox("Shirt"){
				{a = Utils.getprefb("autoLootShirt", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootShirt", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootPantsCheckBox = new CheckBox("Pants"){
				{a = Utils.getprefb("autoLootPants", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootPants", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootGlovesCheckBox = new CheckBox("Gloves"){
				{a = Utils.getprefb("autoLootGloves", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootGloves", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootBootsCheckBox = new CheckBox("Boots"){
				{a = Utils.getprefb("autoLootBoots", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootBoots", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootEyewearCheckBox = new CheckBox("Eyewear"){
				{a = Utils.getprefb("autoLootEyewear", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootEyewear", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootMouthCheckBox = new CheckBox("Mouth"){
				{a = Utils.getprefb("autoLootMouth", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootMouth", val);
				}
			}, prev.pos("bl").adds(0, 2));
			prev = add(autoLootCapeCheckBox = new CheckBox("Cape"){
				{a = Utils.getprefb("autoLootCape", false);}
				public void changed(boolean val) {
					Utils.setprefb("autoLootCape", val);
				}
			}, prev.pos("bl").adds(0, 2));



			Widget backButton;
			add(backButton = new PButton(UI.scale(200), "Back", 27, back, "Advanced Settings"), prev.pos("bl").adds(0, 18).x(0));
			pack();
			centerBackButton(backButton, this);
		}

	}


    public static class PointBind extends Button implements CursorQuery.Handler {
	public static final String msg = "Bind other elements...";
	public static final Resource curs = Resource.local().loadwait("gfx/hud/curs/wrench");
	private UI.Grab mg, kg;
	private KeyBinding cmd;

	public PointBind(int w) {
	    super(w, msg, false);
	    tooltip = RichText.render("Bind a key to an element not listed above, such as an action-menu " +
				      "button. Click the element to bind, and then press the key to bind to it. " +
				      "Right-click to stop rebinding.",
				      300);
	}

	public void click() {
	    if(mg == null) {
		change("Click element...");
		mg = ui.grabmouse(this);
	    } else if(kg != null) {
		kg.remove();
		kg = null;
		change(msg);
	    }
	}

	private boolean handle(KeyEvent ev) {
	    switch(ev.getKeyCode()) {
	    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
	    case KeyEvent.VK_META: case KeyEvent.VK_WINDOWS:
		return(false);
	    }
	    int code = ev.getKeyCode();
	    if(code == KeyEvent.VK_ESCAPE) {
		return(true);
	    }
	    if(code == KeyEvent.VK_BACK_SPACE) {
		cmd.set(null);
		return(true);
	    }
	    if(code == KeyEvent.VK_DELETE) {
		cmd.set(KeyMatch.nil);
		return(true);
	    }
	    KeyMatch key = KeyMatch.forevent(ev, ~cmd.modign);
	    if(key != null)
		cmd.set(key);
	    return(true);
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if(!ev.grabbed)
		return(super.mousedown(ev));
	    Coord gc = ui.mc;
	    if(ev.b == 1) {
		this.cmd = KeyBinding.Bindable.getbinding(ui.root, gc);
		return(true);
	    }
	    if(ev.b == 3) {
		mg.remove();
		mg = null;
		change(msg);
		return(true);
	    }
	    return(false);
	}

	public boolean mouseup(MouseUpEvent ev) {
	    if(mg == null)
		return(super.mouseup(ev));
	    Coord gc = ui.mc;
	    if(ev.b == 1) {
		if((this.cmd != null) && (KeyBinding.Bindable.getbinding(ui.root, gc) == this.cmd)) {
		    mg.remove();
		    mg = null;
		    kg = ui.grabkeys(this);
		    change("Press key...");
		} else {
		    this.cmd = null;
		}
		return(true);
	    }
	    if(ev.b == 3)
		return(true);
	    return(false);
	}

	public boolean getcurs(CursorQuery ev) {
	    return(ev.grabbed ? ev.set(curs) : false);
	}

	public boolean keydown(KeyDownEvent ev) {
	    if(!ev.grabbed)
		return(super.keydown(ev));
	    if(handle(ev.awt)) {
		kg.remove();
		kg = null;
		cmd = null;
		change("Click another element...");
		mg = ui.grabmouse(this);
	    }
	    return(true);
	}
    }

    public OptWnd(boolean gopts) {
	super(Coord.z, "Options            ", true); // ND: Added a bunch of spaces to the caption(title) in order avoid text cutoff when changing it
	autoDropManagerWindow = new AutoDropManagerWindow();
	flowerMenuAutoSelectManagerWindow = new FlowerMenuAutoSelectManagerWindow();
	if (simpleUIFuture != null)
		simpleUIFuture.cancel(true);
	main = add(new Panel());
	Panel video = add(new VideoPanel(main));
	Panel audio = add(new AudioPanel(main));
	Panel keybind = add(new BindingPanel(main));

	int y = UI.scale(6);
	Widget prev;
	y = main.add(new PButton(UI.scale(200), "Video Settings", -1, video, "Video Settings"), 0, y).pos("bl").adds(0, 5).y;
	y = main.add(new PButton(UI.scale(200), "Audio Settings", -1, audio, "Audio Settings"), 0, y).pos("bl").adds(0, 5).y;
	y = main.add(new PButton(UI.scale(200), "Keybindings (Hotkeys)", -1, keybind, "Keybindings (Hotkeys)"), 0, y).pos("bl").adds(0, 5).y;
	y += UI.scale(20);

	advancedSettings = add(new Panel());
	// ND: Add the sub-panel buttons for the advanced settings here
		Panel interfacesettings = add(new InterfaceSettingsPanel(advancedSettings));
		Panel actionbarssettings =  add(new ActionBarsSettingsPanel(advancedSettings));
		Panel chatsettings =  add(new ChatSettingsPanel(advancedSettings));
		Panel displaysettings = add(new DisplaySettingsPanel(advancedSettings));
		Panel qualitydisplaysettings = add(new QualityDisplaySettingsPanel(advancedSettings));
		Panel gameplayautomationsettings = add(new GameplayAutomationSettingsPanel(advancedSettings));
		Panel alteredgameplaysettings =  add(new AlteredGameplaySettingsPanel(advancedSettings));
		Panel camsettings = add(new CameraSettingsPanel(advancedSettings));
		Panel worldgraphicssettings = add(new WorldGraphicsSettingsPanel(advancedSettings));
		Panel hidingsettings = add(new HidingSettingsPanel(advancedSettings));
		Panel alarmsettings = add(new AlarmsAndSoundsSettingsPanel(advancedSettings));
		Panel combatsettings = add(new CombatSettingsPanel(advancedSettings));
		Panel combataggrosettings = add(new AggroExclusionSettingsPanel(advancedSettings));
		Panel serverintegrationsettings = add(new ServerIntegrationSettingsPanel(advancedSettings));
		Panel autolootsettings = add(new AutoLootSettingsPanel(advancedSettings));

		int leftY = UI.scale(6);
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Interface Settings", -1, interfacesettings, "Interface Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Action Bars Settings", -1, actionbarssettings, "Action Bars Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Combat Settings", -1, combatsettings, "Combat Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Quality Display Settings", -1, qualitydisplaysettings, "Quality Display Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Chat Settings", -1, chatsettings, "Chat Settings"), 0, leftY).pos("bl").adds(0, 5).y;

		leftY += UI.scale(20);
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Altered Gameplay Settings", -1, alteredgameplaysettings, "Altered Gameplay Settings"), 0, leftY).pos("bl").adds(0, 5).y;
		leftY = advancedSettings.add(new PButton(UI.scale(200), "Aggro Exclusion Settings", -1, combataggrosettings, "Aggro Exclusion Settings"), 0, leftY).pos("bl").adds(0, 5).y;

		int rightX = UI.scale(220);
		int rightY = UI.scale(6);
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Display Settings", -1, displaysettings, "Display Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Camera Settings", -1, camsettings, "Camera Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "World Graphics Settings", -1, worldgraphicssettings, "World Graphics Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Hiding Settings", -1, hidingsettings, "Hiding Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Alarms & Sounds Settings", -1, alarmsettings, "Alarms & Sounds Settings"), rightX, rightY).pos("bl").adds(0, 5).y;

		rightY += UI.scale(20);
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Gameplay Automation Settings", -1, gameplayautomationsettings, "Gameplay Automation Settings"), rightX, rightY).pos("bl").adds(0, 5).y;
		rightY = advancedSettings.add(new PButton(UI.scale(200), "Auto-Loot Settings", -1, autolootsettings, "Auto-Loot Settings"), rightX, rightY).pos("bl").adds(0, 5).y;


		int middleX = UI.scale(110);
		int middleY = leftY + UI.scale(20);
		middleY = advancedSettings.add(new PButton(UI.scale(200), "Server Integration Settings", -1, serverintegrationsettings, "Server Integration Settings"), middleX, middleY).pos("bl").adds(0, 5).y;
		middleY += UI.scale(20);
		middleY = advancedSettings.add(new PButton(UI.scale(200), "Back", 27, main, "Options            "), middleX, middleY).pos("bl").adds(0, 5).y;
	this.advancedSettings.pack();

	// Now back to the main panel, we add the advanced settings button and continue with everything else
	y = main.add(new PButton(UI.scale(200), "Advanced Settings", -1, advancedSettings, "Advanced Settings"), 0, y).pos("bl").adds(0, 5).y;
	y += UI.scale(20);
	if(gopts) {
	    if((SteamStore.steamsvc.get() != null) && (Steam.get() != null)) {
		y = main.add(new Button(UI.scale(200), "Visit store", false).action(() -> {
			    SteamStore.launch(ui.sess);
		}), 0, y).pos("bl").adds(0, 5).y;
	    }
	    y = main.add(new Button(UI.scale(200), "Switch character", false).action(() -> {
			getparent(GameUI.class).act("lo", "cs");
	    }), 0, y).pos("bl").adds(0, 5).y;
	    y = main.add(new Button(UI.scale(200), "Log out", false).action(() -> {
			getparent(GameUI.class).act("lo");
	    }), 0, y).pos("bl").adds(0, 5).y;
	}
	y = main.add(new Button(UI.scale(200), "Close", false).action(() -> {
		    OptWnd.this.hide();
	}), 0, y).pos("bl").adds(0, 5).y;
	this.main.pack();

	chpanel(this.main);
    }

    public OptWnd() {
	this(true);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && (msg == "close")) {
	    hide();
		cap = "Options            ";
	} else {
	    super.wdgmsg(sender, msg, args);
	}
    }

    public void show() {
	chpanel(main);
	super.show();
    }

	private void centerBackButton(Widget backButton, Widget parent){ // ND: Should only be used at the very end after the panel was already packed once.
		backButton.move(new Coord(parent.sz.x/2-backButton.sz.x/2, backButton.c.y));
		pack();
	}

	private void resetSimpleUIChanged(){
		simpleUIChanged = false;
		simpleUIFuture.cancel(true);
	}

	private void resetSkyboxCheckbox(){
		enableSkyboxCheckBox.set(true);
		skyboxFuture.cancel(true);
	}

	// ND: Setting Tooltips
	// Interface Settings Tooltips
	private static final Object interfaceScaleTooltip = RichText.render("$col[218,163,0]{Warning:} This setting is by no means perfect, and it can mess up many UI related things." +
			"\nSome windows might just break when this is set above 1.00x." +
			"\n" +
			"\n$col[185,185,185]{I really try my best to support this setting, but I can't guarantee everything will work." +
			"\nUnless you're on a 4K or 8K display, I'd keep this at 1.00x.}", UI.scale(300));
	private static final Object simplifiedUIThemeCheckBoxTooltip = RichText.render("$col[185,185,185]{A more boring theme for the UI...}", UI.scale(300));
	private static final Object extendedMouseoverInfoTooltip = RichText.render("Holding Ctrl+Shift shows the Resource Path when mousing over Objects or Tiles. " +
			"\nThis setting will add a lot of additional information on top of that." +
			"\n" +
			"\n$col[185,185,185]{Unless you're a client dev, you don't really need to enable this setting, like ever.}", UI.scale(300));
	private static final Object disableMenuGridHotkeysTooltip = RichText.render("This completely disables the hotkeys for the action buttons & categories in the bottom right corner menu (aka the menu grid)." +
			"\n" +
			"\n$col[185,185,185]{Your action bar keybinds are NOT affected by this setting.}", UI.scale(300));

	private static final Object alwaysOpenBeltOnLoginTooltip = RichText.render("This will cause your belt window to always open when you log in." +
			"\n" +
			"\n$col[185,185,185]{By default, Loftar saves the status of the belt at logout. So if you don't enable this setting, but leave the belt window open when you log out/exit the game, it will still open on login.}", UI.scale(300));
	private static final Object showMapMarkerNamesTooltip = RichText.render("$col[185,185,185]{The marker names are NOT visible in compact mode.}", UI.scale(320));
	private static final Object verticalContainerIndicatorsTooltip = RichText.render("Orientation for inventory container indicators." +
			"\n" +
			"\n$col[185,185,185]{For example, the amount of water in waterskins, seeds in a bucket, etc.}", UI.scale(230));
	private static final Object experienceWindowLocationTooltip = RichText.render("Select where you want the experience event pop-up window to show up." +
			"\n" +
			"\n$col[185,185,185]{The default client pops it up in the middle of your screen, which can be annoying.}", UI.scale(300));

	private static final Object showFramerateTooltip = RichText.render("Shows the current FPS in the top-right corner of the game window.", UI.scale(300));
	private static final Object snapWindowsBackInsideTooltip = RichText.render("This will cause most windows, that are not too large, to be fully snapped back into your game's window." +
			"\nBy default, when you try to drag a window outside of your game window, it will only pop 25% of it back in." +
			"\n" +
			"\n$col[185,185,185]{Very large windows are not affected by this setting. Only the 25% rule applies to them." +
			"\nThe map window is always fully snapped back.}", UI.scale(300));
	private static final Object dragWindowsInWhenResizingTooltip = RichText.render("This will force ALL windows to be dragged back inside the game window, whenever you resize it." +
			"\n" +
			"\n$col[185,185,185]{Without this setting, windows remain in the same spot when you resize your game window, even if they end up outside of it. They will only come back if closed and reopened (for example, via keybinds)", UI.scale(300));
	private static final Object showQuickSlotsTooltip = RichText.render("Just a small interactable widget that shows your hands, belt, backpack and cape slots, so you don't have to open your equipment window." +
			"\nTo drag this widget to a new position: hold down Shift, click and drag." +
			"\n" +
			"\n$col[185,185,185]{Your quick-switch keybinds ('Right Hand' and 'Left Hand') are NOT affected by this setting.}", UI.scale(300));
	public static final Object showStudyReportHistoryTooltip = RichText.render("Shows what curiosity was formerly placed in each slot. " +
			"\nThe history is saved separately for every character, on every account." +
			"\n" +
			"\n$col[185,185,185]{It doesn't work with Gems. Don't ask me why.}", UI.scale(300));
	static final Object lockStudyReportTooltip = RichText.render("This will prevent grabbing or dropping items from the Study Report", UI.scale(300));
	public static final Object soundAlertForFinishedCuriositiesTooltip = RichText.render("A violin sound will be played every time a curiosity is finished." +
			"\n" +
			"\n$col[218,163,0]{Preview:}$col[185,185,185]{Enable this to hear the sound!", UI.scale(300));
	private static final Object alwaysShowCombatUiBarTooltip = RichText.render("For more options for this bar, check the Combat Settings.", UI.scale(320));
	private static final Object transparentQuestsObjectivesWindowTooltip = RichText.render("This makes the Quest Objectives window background transparent, like in the default client." +
			"\n" +
			"\n$col[185,185,185]{You can still drag the window around, regardless.", UI.scale(300));

	// Combat Settings Tooltips
	private static final Object singleRowCombatMovesTooltip = RichText.render("This makes the Bottom Panel show the combat moves in one row, rather than two.", UI.scale(300));
	private static final Object showDamagePredictUITooltip = RichText.render("This makes the Combat Moves that can deal damage show how much damage they can potentially do, when used." +
			"\n" +
			"\nThis is calculated depending on the following:" +
			"\n$col[185,185,185]{- How high your current target's $col[218,163,0]{Openings} are (depending on the openings the combat move applies to)" +
			"\n- How much total $col[218,163,0]{Strength} your character has" +
			"\n- How much $col[218,163,0]{Damage} your currently equipped $col[218,163,0]{Weapon} has (if the move uses the weapon)}", UI.scale(320));
	private static final Object damageInfoClearTooltip = RichText.render("Clears all damage info." +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object onlyShowOpeningsAbovePercentageCombatInfoTooltip = RichText.render("Only show the combat info openings if at least one of them is above the set number. If one of them is above that, show all of them." +
			"\n" +
			"\nThis does NOT apply to your current target, only other combat foes.}", UI.scale(320));
	private static final Object showCombatOpeningsAsLettersTooltip = RichText.render("This will change the openings from full squares into colored letters. For example, the red square will become a colored R, blue will become B, etc." +
			"\n" +
			"\n$col[185,185,185]{The color settings from below still apply to the letters.}" +
			"\n$col[185,185,185]{I only added this as an extra aid for colorblind people, but I doubt anybody will use it...}", UI.scale(300));
	private static final Object highlightPartyMembersTooltip = RichText.render("This will put a color highlight over all party members." +
			"\n" +
			"\n$col[185,185,185]{If you are the party leader, your color highlight will always be the $col[218,163,0]{Leader's Color}, regardless of what you set $col[218,163,0]{Your Color} to.}", UI.scale(310));
	private static final Object showCirclesUnderPartyMembersTooltip = RichText.render("This will put a colored circle under all party members." +
			"\n" +
			"\n$col[185,185,185]{If you are the party leader, your circle's color will always be the $col[218,163,0]{Leader's Color}, regardless of what you set $col[218,163,0]{Your Color} to.}", UI.scale(300));
	private static final Object highlightCombatFoesTooltip = RichText.render("This will put a color highlight over all enemies that you are currently in combat with.", UI.scale(310));
	private static final Object showCirclesUnderCombatFoesTooltip = RichText.render("This will put a colored circle under all enemies that you are currently in combat with.", UI.scale(300));
	private static final Object targetSpriteTooltip = RichText.render("The target sprite uses the same color you set for Combat Foes.", UI.scale(300));
	private static final Object drawChaseVectorsTooltip = RichText.render("If this setting is enabled, colored lines will be drawn between chasers and chased targets." +
			"\n=====================" +
			"\n$col[255,255,255]{White: }You are the chaser." +
			"\n$col[0,160,0]{Green: }A party member is the chaser." +
			"\n$col[185,0,0]{Red: }A player is chasing you or a party member." +
			"\n$col[165,165,165]{Gray: }An animal is the chaser, OR random (non-party) players are chasing each other." +
			"\n=====================" +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{Chase vectors include queuing attacks, clicking a critter to pick up, or simply following someone.}" +
			"\n$col[218,163,0]{Disclaimer:} $col[185,185,185]{Chase vectors sometimes don't show when chasing a critter that is standing still. The client treats this as something else for some reason and I can't fix it.}", UI.scale(430));
	private static final Object drawYourCurrentPathTooltip = RichText.render("When this is enabled, a straight line will be drawn between your character and wherever you clicked" +
			"\n" +
			"\n$col[185,185,185]{You can use this to make sure you won't run into a tree or something, I guess.}", UI.scale(300));
	private static final Object showYourCombatRangeCirclesTooltip = RichText.render("This will display two circles under your character, that show your unarmed range, and currently equipped weapon range (if you have a weapon equipped).", UI.scale(300));

	// Display Settings Tooltips
	private static final Object granularityPositionTooltip = RichText.render("This works like the :placegrid console command. " +
			"\nIt allows you to have more freedom when placing constructions/objects.", UI.scale(300));
	private static final Object granularityAngleTooltip = RichText.render("This works like the :placeangle console command. " +
			"\nIt allows you to have more freedom when rotating constructions/objects before placement.", UI.scale(300));
	private static final Object displayGrowthInfoTooltip = RichText.render("This will show the following growth information:" +
			"\n" +
			"\n> Trees and Bushes will display their growth percentage (below 100%) and extra size percentage, if you enable the \"Also Show Trees Above %\" setting." +
			"\n$col[185,185,185]{If a Tree or Bush is not showing a percentage below 100%, that means it reached full growth.}" +
			"\n" +
			"\n> Crops will generally display their growth stage as \"Current\", and a red dot when they reached the final stage." +
			"\n$col[185,185,185]{Crops with a seeds stage (carrots, turnips, leeks, etc.) will also display a blue dot during the seeds stage.}" +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(330));
	private static final Object highlightCliffsTooltip = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object showContainerFullnessTooltip = RichText.render("Colors containers (Cupboards, Chests, Crates, etc.), depending on how much stuff is in them." +
			"\n" +
			"\n$col[185,185,185]{Select from below what states you want to be highlighted, and what colors you want each of them to show.}", UI.scale(330));
	private static final Object showWorkstationProgressTooltip = RichText.render("Colors workstations (Drying Racks, Tanning Tubs, Cheese Racks, Flower Pots), depending on their current progress." +
			"\n" +
			"\n$col[185,185,185]{Select from below what states you want to be highlighted, and what colors you want each of them to show.}", UI.scale(330));
	private static final Object showObjectCollisionBoxesTooltip = RichText.render("This shows the collision boundaries of objects in the world by outlining each edge with a line." +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	private static final Object displayObjectDurabilityPercentageTooltip = RichText.render("This makes objects that took decay hits also show a percentage number, on top of the cracked texture overlay.", UI.scale(300));
	private static final Object displayObjectQualityOnInspectionTooltip = RichText.render("This makes objects that have been inspected show their quality number on top, until unload them.", UI.scale(300));
	private static final Object showCritterAurasTooltip = RichText.render("This will draw clickable circles under all critters, which makes it easier to spot them, and right-click to chase them." +
			"\n" +
			"\n$col[185,185,185]{This can be very nice during combat, due to the $col[218,163,0]{Speed Boost provided by the Forager Credo} when chasing critters. " +
			"\nIt can also just make your life easier when foraging in general.}", UI.scale(300));
	private static final Object showSpeedBuffAurasTooltip = RichText.render("This will draw a circle under speed buffs that spawn in the world, to make it easier to spot them." +
			"\n" +
			"\n$col[185,185,185]{This circle is not clickable, but it shows you where exactly the speed buff is on the ground plane.}" , UI.scale(300));
	private static final Object showMidgesCircleAurasTooltip = RichText.render("This will draw a red circle under midges, to make it easier to spot and avoid them.", UI.scale(300));
	private static final Object showDangerousBeastRadiiTooltip = RichText.render("This will draw a large red radius around dangerous animals to make it easier to spot them." +
			"\n" +
			"\n$col[200,0,0]{WARNING: This doesn't show you how close you can get to it!}" +
			"\n" +
			"\n$col[185,185,185]{If you don't know how dangerous an animal is and how close you can get to it, just stay as far as possible.}", UI.scale(300));
	private static final Object showBeeSkepsRadiiTooltip = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object showFoodThroughsRadiiTooltip = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object showMoundBedsRadiiTooltip = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object objectPermanentHighlightingTooltip = RichText.render("This allows you to set a purple highlight on objects that persists even if you reload the objects, switch character, log out, etc." +
			"\n" +
			"\n$col[185,185,185]{For example, you can use this to keep track of which cows you milked already, or whatever.}" +
			"\n" +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{Disabling this setting will also reset the current list of highlighted objects.}", UI.scale(300));
	private static final Object showBarrelContentsTextTooltip = RichText.render("This adds text on top of barrels, to make it easier to determine what’s inside of them." +
			"\n" +
			"\n$col[185,185,185]{Empty barrels won't show any text.}", UI.scale(300));
	private static final Object showIconSignTextTooltip = RichText.render("This adds text on top of icon signs, that shows the name of the currently displayed icon." +
			"\n" +
			"\n$col[185,185,185]{Icon signs that don't have any icon set won't show any text.}", UI.scale(300));
	private static final Object showCheeseRacksTierTextTooltip = RichText.render("This adds text on top of each cheese tray inside cheese racks, that shows the current tier of the cheese present in the tray." +
			"\n" +
			"\n$col[185,185,185]{Unfortunately, the server only sends the tier info, so the client can't tell which exact cheese is in the trays.}", UI.scale(300));
	private static final Object showMineSupportRadiiTooltip = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object showMineSupportSafeTilesTooltip = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object enableMineSweeperTooltip = RichText.render("This will cause cave dust tiles to show the number of potential cave-ins surrounding them, just like in Minesweeper." +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{If a cave-in has been mined out, the tiles surrounding it will still drop cave dust, and they will still show a number on the ground. The cave dust tiles are pre-generated with the world. That's just how Loftar coded it.}" +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{You can still pick up the cave dust item off the ground. The numbers are affected only by the duration of the falling dust particles effect (aka dust rain), which can be set below}" +
			"\n" +
			"\n$col[200,0,0]{NOTE:} $col[185,185,185]{There's a bug with the falling dust particles, that we can't really \"fix\". If you mine them out on a level, the same particles can also show up on different levels or the overworld. If you want them to vanish, you can just relog, but they will despawn from their original location too.}" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object showObjectsSpeedTooltip = RichText.render("This will show the speed of moving objects (Players, Mobs, Vehicles, etc.) below them." +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	private static final Object showTreesBushesHarvestIconsTooltip = RichText.render("This will show the icons of seeds and leaves that can be collected on fully grown trees and bushes." +
			"\n" +
			"\n$col[185,185,185]{It won't show the icons on trees/bushes that are not 100% grown, even if they can already be harvested.}" +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	private static final Object showLowFoodWaterIconsTooltip = RichText.render("This will show warning icons for low food and low water over Chicken Coops and Rabbit Hutches" +
			"\n" +
			"\n$col[185,185,185]{For Chicken Coops, the icons show up when they go below 50% Food/Water. " +
			"\nFor Rabbit Hutches, the icons show up when they go below 33% for Food and below 50% for Water." +
			"\nI can't change this because the game does not differentiate between any other values for them.}" +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));
	private static final Object showBeeSkepsHarvestIconsTooltip = RichText.render("This will show icons for Wax and Honey when they can be harvested from Bee Skeps." +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));


	// Quality Display Settings Tooltips
	private static final Object customQualityColorsTooltip = RichText.render("These numbers and colors are completely arbitrary, and you can change them to whatever you like." +
			"\n" +
			"\n$col[218,163,0]{Note:} $col[185,185,185]{The quality color for container liquids is not affected by this setting.}", UI.scale(300));

	// Audio Settings Tooltips
	private static final Object audioLatencyTooltip = RichText.render("Sets the size of the audio buffer." +
			"\n" +
			"\n$col[185,185,185]{Loftar claims that smaller sizes are better, but anything below 50ms always seems to stutter, so I limited it to that." +
			"\nIncrease this if your audio is still stuttering.}", UI.scale(300));

	// Gameplay Automation Settings Tooltips
	static final Object autoReloadCuriositiesFromInventoryTooltip = RichText.render("If enabled, curiosities will be automatically reloaded into the Study Report once they finish being studied." +
			"\nThis picks items only from your Inventory and currently open Cupboards. No other containers." +
			"\n" +
			"\n$col[185,185,185]{It only reloads curiosities that are currently being studied. It can't add new curiosities.}", UI.scale(300));
	private static final Object preventTablewareFromBreakingTooltip = RichText.render("Prevents eating when the table contains Tableware with only 1 durability left." +
			"\n" +
			"\n$col[185,185,185]{A system warning message will be shown, to let you know which Tableware is at 1 durability.}", UI.scale(300));
    private static final Object autoSelect1stFlowerMenuTooltip = RichText.render("Holding Ctrl before right clicking an item or object will auto-select the first available option from the flower menu." +
			"\n" +
			"\n$col[185,185,185]{Except for the Head of Lettuce. It will select the 2nd option there, so you split it rather than eat it.}", UI.scale(300));
	private static final Object autoRepeatFlowerMenuTooltip = RichText.render("This will trigger the Auto-Repeat Flower-Menu Script to run when you Right Click an item while holding Ctrl + Shift." +
			"\n\n$col[185,185,185]{You have} $col[218,163,0]{2 seconds} $col[185,185,185]{to select a Flower Menu option, after which the script will automatically click the selected option for ALL items that have the same name in your inventory.}" +
			"\n$col[200,0,0]{If you don't select an option within} $col[218,163,0]{2 seconds}$col[200,0,0]{, the script won't run.}" +
			"\n\nYou can stop the script before it finishes by pressing ESC." +
			"\n\n$col[218,163,0]{Example:} You have 10 Oak Blocks in your inventory. You hold Ctrl + Shift and right click one of the Oak Blocks and select \"Split\" in the flower menu. The script starts running and it splits all 10 Oak Blocks." +
			"\n\n$col[218,163,0]{Note:} $col[185,185,185]{This script only runs on items that have the same name inside your inventory. It does not take into consideration items of the same \"type\" (for example, if you run the script on Oak Blocks, it won't also run on Spruce Blocks).} ", UI.scale(310));
	private static final Object alsoUseContainersWithRepeaterTooltip = RichText.render("Allow the Auto-Repeat Flower-Menu Script to run through all inventories, such as open cupboards, chests, crates, or any other containers.", UI.scale(300));
	private static final Object flowerMenuAutoSelectManagerTooltip = RichText.render("An advanced menu to automatically select specific flower menu options all the time. New options are added to the list as you discover them." +
			"\n" +
			"\n$col[185,185,185]{I don't recommend using this, but nevertheless it exists due to popular demand.}", UI.scale(300));
	private static final Object autoEquipBunnySlippersPlateBootsTooltip = RichText.render("Switches your currently equipped shoes to Bunny Slippers when you right click to chase a rabbit, or Plate Boots if you click on anything else." +
			"\n" +
			"\n$col[185,185,185]{I suggest always using this setting in PVP.}", UI.scale(300));
	private static final Object autoPeaceAnimalsWhenCombatStartsTooltip = RichText.render("This will automatically set your status to 'Peace' when combat is initiated with a new target (animals only). " +
			"\nToggling this on, while in combat, will also autopeace all animals you are currently fighting." +
			"\n\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object autoDrinkingTooltip = RichText.render("When your Stamina Bar goes below the set threshold, try to drink Water. If the threshold box is empty, it defaults to 75%." +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object enableQueuedMovementTooltip = RichText.render("$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(300));

	// Altered Gameplay Settings Tooltips
	private static final Object overrideCursorItemWhenHoldingAltTooltip = RichText.render("Holding Alt while having an item on your cursor will allow you to left click to walk, or right click to interact with objects, rather than drop it on the ground." +
			"\n" +
			"\n$col[185,185,185]{Left click ignores the UI when you do this, so don't try to click on the map to walk while holding an item.}" +
			"\n" +
			"\n$col[200,0,0]{SETTING OVERRIDE:} This doesn't work with the \"No Cursor Dropping\" settings, and it will toggle them off when this is enabled.", UI.scale(320));
	private static final Object noCursorItemDroppingAnywhereTooltip = RichText.render("This will allow you to have an item on your cursor and still be able to left click to walk." +
			"\n" +
			"\n$col[185,185,185]{You can drop the item from your cursor if you hold Alt.}" +
			"\n" +
			"\n$col[200,0,0]{WARNING: If you're holding something on your cursor, you're NOT ABLE to enter Deep Water to Swim. The game prevents you from doing it.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}" +
			"\n" +
			"\n$col[200,0,0]{SETTING OVERRIDE:} This doesn't work with the \"Override Cursor Item\" setting, and it will toggle it off when this is enabled.", UI.scale(320));
	private static final Object noCursorItemDroppingInWaterTooltip =  RichText.render("This will allow you to have an item on your cursor and still be able to left click to walk, while you are in water. " +
			"\nIf the previous setting is Enabled, this one will be ignored." +
			"\n" +
			"\n$col[185,185,185]{You can drop the item from your cursor if you hold Alt.}" +
			"\n" +
			"\n$col[200,0,0]{WARNING: If you're holding something on your cursor, you're NOT ABLE to enter Deep Water to Swim. The game prevents you from doing it.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}" +
			"\n" +
			"\n$col[200,0,0]{SETTING OVERRIDE:} This doesn't work with the \"Override Cursor Item\" setting, and it will toggle it off when this is enabled.", UI.scale(320));
	private static final Object useOGControlsForBuildingAndPlacingTooltip = RichText.render("Hold Ctrl to smoothly place, and Ctrl+Shift to also smoothly rotate. To walk to the place you click (rather than build/place the object) hold Alt." +
			"\n" +
			"\n$col[185,185,185]{Idk why Loftar changed them when he did, but some of you might be used to the new controls rather than the OG ones, so you have the option to disable this.}", UI.scale(320));
	private static final Object useImprovedInventoryTransferControlsTooltip = RichText.render("Alt+Left Click for descending order, and Alt+Right click for ascending order.", UI.scale(320));
	private static final Object tileCenteringTooltip = RichText.render("This forces your left and right clicks in the world to go to the center of the tile you clicked. So you will always walk to the center of the tile, or place items down on the center." +
			"\n" +
			"\n$col[185,185,185]{It doesn't affect the manual precise placement of objects, just the quick right-click one.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));

	// Camera Settings Tooltips
	private static final Object reverseOrthoCameraAxesTooltip = RichText.render("This will reverse the Horizontal axis when dragging the camera to look around." +
			"\n" +
			"\n$col[185,185,185]{I don't know why Loftar inverts it in the first place...}", UI.scale(280));
	private static final Object unlockedOrthoCamTooltip = RichText.render("This allows you to rotate the Ortho camera freely, without locking it to only 4 view angles.", UI.scale(280));
	private static final Object allowLowerFreeCamTiltTooltip = RichText.render("This will allow you to tilt the camera below the character (and under the ground), to look upwards." +
			"\n" +
			"\n$col[200,0,0]{WARNING: Be careful when using this setting, especially in combat! You're NOT able to click on the ground when looking at the world from below.}" +
			"\n" +
			"\n$col[185,185,185]{Honestly just enable this when you need to take a screenshot or something, and keep it disabled the rest of the time. I added this setting for fun.}", UI.scale(300));
	private static final Object freeCamHeightTooltip = RichText.render("This affects the height of the point at which the free camera is pointed. By default, it is pointed right above the player's head." +
			"\n" +
			"\n$col[185,185,185]{This doesn't really affect gameplay that much, if at all. With this setting, you can make the camera point at the feet, torso, head, slightly above you, or whatever's in between.}", UI.scale(300));

	// World Graphics Settings Tooltips
	private static final Object nightVisionTooltip = RichText.render("Increasing this will simulate daytime lighting during the night." +
			"\n" +
			"\n$col[185,185,185]{It can slightly affect the light levels during the day too, but it is barely noticeable.}" +
			"\n" +
			"\n$col[218,163,0]{Keybind:} $col[185,185,185]{This slider can also be switched between minimum and maximum by using the 'Night Vision' keybind.}", UI.scale(300));
	private static final Object hideFlavorObjectsTooltip = RichText.render("This hides the random objects that appear in the world, like random weeds on the ground and whatnot, which you cannot interact with." +
			"\n" +
			"\n$col[185,185,185]{Players usually disable flavor objects to improve visibility, especially in combat.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object flatWorldTooltip = RichText.render("This will make the entire game world terrain flat, except for cliffs." +
			"\n" +
			"\n$col[185,185,185]{Cliffs will still be visible, but with their relative height scaled down.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object disableTileSmoothingTooltip = RichText.render("This will cause all biome tiles to look the same, with no variations." +
			"\n" +
			"\n$col[185,185,185]{I guess some people think this makes it easier to differentiate between terrain types, or maybe it's just preference.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object disableTileTransitionsTooltip = RichText.render("This will turn all tiles into squares, so you can determine where one biome ends and another one starts, or where the shoreline meets the water." +
			"\n" +
			"\n$col[185,185,185]{It can be useful in some niche cases I won't bother listing, but it's preference.}" +
			"\n" +
			"\n$col[218,163,0]{Action Button:} $col[185,185,185]{This setting can also be turned on/off using an action button from the menu grid (Custom Client Extras → Toggles).}", UI.scale(320));
	private static final Object simplifiedCropsTooltip = RichText.render("This reduces each crop tile to a single plant for a cleaner, more minimal look." +
			"\n" +
			"\n$col[185,185,185]{No more random clumps. Just a single sprout in the center of the tile, for each crop type.}", UI.scale(300));
	private static final Object simplifiedForageablesTooltip = RichText.render("This makes forageable spawns appear as a single item per tile instead of a cluster, for simplified visuals." +
			"\n" +
			"\n$col[185,185,185]{I never liked how they look like 10 things in a cluster, but you only pick one up... lol.}", UI.scale(300));
	private static final Object flatCaveWallsTooltip = RichText.render("This lowers the height of cave walls significantly, and also displays their texture on the ground." +
			"\n" +
			"\n$col[185,185,185]{It makes it easier to see what you're looking at, from all angles.}", UI.scale(300));
	private static final Object straightCliffEdgesTooltip = RichText.render("This disables the variations for cliffs, making it a bit easier to distinguish their angles." +
			"\n" +
			"\n$col[185,185,185]{The difference is very small, honestly.}", UI.scale(300));
	private static final Object flatCupboardsTooltip = RichText.render("This turns cupboards into short boxes that open towards the ceiling." +
			"\n" +
			"\n$col[185,185,185]{Parchements that are placed on the cupboards are also moved to the top.}", UI.scale(300));
	private static final Object palisadesAndBrickWallsScaleTooltip = RichText.render("This changes how tall Palisades and Brick Walls are." +
			"\n" +
			"\n$col[185,185,185]{Only wall sections, NOT gates!}", UI.scale(300));
	private static final Object enableSkyboxTooltip = RichText.render("Adds a skybox to the game world." +
			"\n" +
			"\n$col[185,185,185]{Summon the sky above the hearthlands, and banish the endless void!}", UI.scale(190));
	private static final Object disableTreeAndBushSwayingTooltip = RichText.render("Trees and bushes will no longer move as if the wind is blowing them around." +
			"\n" +
			"\n$col[185,185,185]{Disabling swaying can improve your framerate.}", UI.scale(300));
	private static final Object disableIndustrialSmokeTooltip = RichText.render("Completely removes the smoke particles from things like smelters and kilns." +
			"\n" +
			"\n$col[185,185,185]{It *might* improve your framerate around smelters and stuff, but I'm not sure though.}", UI.scale(300));
	private static final Object disableScentSmokeTooltip = RichText.render("Completely removes the smoke particles from crime scents." +
			"\n" +
			"\n$col[185,185,185]{It can significantly improve your framerate during big battles, but I still recommend disabling scents completely when you're participating in big fights.}", UI.scale(300));
	private static final Object disableSeasonalGroundColorsTooltip = RichText.render("This makes all biomes keep their summer colors, during any season." +
			"\n" +
			"\n$col[185,185,185]{Have you noticed how all biome colors shift their hue when the season changes?}", UI.scale(300));
	private static final Object disableGroundCloudShadowsTooltip = RichText.render("$col[185,185,185]{They look kinda nice to be honest.}", UI.scale(300));
	private static final Object disableWetGroundOverlayTooltip = RichText.render("$col[185,185,185]{Honestly just keep it disabled. It looks VERY UGLY, lmao.}", UI.scale(300));
	private static final Object disableValhallaFilterTooltip = RichText.render("This makes Valhalla look the same as the normal world." +
			"\n" +
			"\n$col[185,185,185]{I hate how it makes Valhalla look more gray, as if it's some weird purgatory.}", UI.scale(300));
	private static final Object disableScreenShakingTooltip = RichText.render("$col[185,185,185]{This usually happens when a dungeon is about to collapse, after you've defeated the boss.}", UI.scale(300));
	private static final Object disableLibertyCapsHighTooltip = RichText.render("$col[200,0,0]{WARNING:} This is the only screen effect in the game that displays intense flashing lights and has sharp sounds." +
			"\n" +
			"\nIf you have epilepsy or are sensitive to these kinds of effects, I BEG YOU to keep it DISABLED if you are at risk." +
			"\n" +
			"\n$col[185,185,185]{I have no idea why this disgusting effect exists at all. " +
			"\nThe vanilla client does not warn you about it in any way, shape or form.}", UI.scale(280));

	// Server Integration Settings Tooltips
	private static final Object uploadMapTilesTooltip = RichText.render("Enable this to upload your map tiles to your web map server.", UI.scale(300));
	private static final Object sendLiveLocationTooltip = RichText.render("Enable this to show your current location on your web map server.", UI.scale(320));
	private static final Object liveLocationNameTooltip = RichText.render("If you send your location to the server, your name will appear as whatever you set in this text entry + your current character name." +
			"\n" +
			"\n$col[218,163,0]{For example:} Nightdawg (VillageCrafter)$col[185,185,185]{, where }\"Nightdawg\" $col[185,185,185]{is the name I set in this text entry, and} \"VillageCrafter\" $col[185,185,185]{is the character's original name." +
			"\nThe character's original name is the one you see in the character selection screen, NOT the presentation name.}", UI.scale(320));

	// Misc/Other
	private static final Object resetButtonTooltip = RichText.render("Reset to default value.", UI.scale(300));
	private static final Object genericHasKeybindTooltip = RichText.render("$col[218,163,0]{Keybind:} $col[185,185,185]{This can also be toggled using a keybind.}", UI.scale(300));

	@Override
	protected void attached() {
		super.attached();
		if (ui != null)
			currentgprefs = ui.gprefs;
		if (ui.gui != null) {
			ui.gui.add(autoDropManagerWindow); // ND: this.parent.parent is root widget in login screen or gui in game.
			autoDropManagerWindow.hide();
			ui.gui.add(flowerMenuAutoSelectManagerWindow);
			flowerMenuAutoSelectManagerWindow.hide();
		}
	}
}
