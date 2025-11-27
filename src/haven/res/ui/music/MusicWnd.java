/* Preprocessed source code */
package haven.res.ui.music;

import haven.*;
import haven.render.*;
import java.awt.Color;
import java.util.*;
import java.io.*;
import java.awt.event.KeyEvent;
import haven.Audio.CS;

import javax.sound.midi.*;

/* >wdg: MusicWnd */
@haven.FromResource(name = "ui/music", version = 35)
public class MusicWnd extends Window {
    public Tex[] tips;
    public Map<Integer, Integer> keys;
    public int[] nti, shi;
    public int[] ntp, shp;
    public Tex[] ikeys;
    public boolean[] cur;
    public final int[] act;
    public final double start;
    public double latcomp = 0.15;
    public int actn;

	public boolean originalLayout;
	public double tempo = 1;
	public Label tempoLabel = new Label("Tempo Factor: " + tempo);
	public HafenMidiplayer hafenMidiplayer = null;
	Thread midiThread;
	public static final int NOTE_ON = 0x90;
	public static final int NOTE_OFF = 0x80;

    public MusicWnd(String name, int maxpoly) {
	super(Coord.z, name, true);
	setMusicWndLayout(!OptWnd.improvedInstrumentMusicWindowCheckBox.a);
	this.act = new int[maxpoly];
	this.start = System.currentTimeMillis() / 1000.0;
    }

    public static Widget mkwidget(UI ui, Object[] args) {
	String nm = (String)args[0];
	int maxpoly = (Integer)args[1];
	return(new MusicWnd(nm, maxpoly));
    }

    protected void added() {
	super.added();
	ui.grabkeys(this);
    }

    public void cdraw(GOut g) {
	if (originalLayout) {
	boolean[] cact = new boolean[cur.length];
	for(int i = 0; i < actn; i++)
	    cact[act[i]] = true;
	int base = 12;
	if(ui.modshift) base += 12;
	if(ui.modctrl)  base -= 12;
	for(int i = 0; i < nti.length; i++) {
	    Coord c = new Coord(ikeys[0].sz().x * ntp[i], 0);
	    boolean a = cact[nti[i] + base];
	    g.image(ikeys[a?1:0], c);
	    g.image(tips[nti[i]], c.add((ikeys[0].sz().x - tips[nti[i]].sz().x) / 2, ikeys[0].sz().y - tips[nti[i]].sz().y - (a?9:12)));
	}
	int sho = ikeys[0].sz().x - (ikeys[2].sz().x / 2);
	for(int i = 0; i < shi.length; i++) {
	    Coord c = new Coord(ikeys[0].sz().x * shp[i] + sho, 0);
	    boolean a = cact[shi[i] + base];
	    g.image(ikeys[a?3:2], c);
	    g.image(tips[shi[i]], c.add((ikeys[2].sz().x - tips[shi[i]].sz().x) / 2, ikeys[2].sz().y - tips[shi[i]].sz().y - (a?9:12)));
	}
	} else {
		final boolean[] array = new boolean[this.cur.length];
		for (int i = 0; i < this.actn; ++i) {
			try {
				array[this.act[i]] = true;
			} catch (ArrayIndexOutOfBoundsException e) {}
		}
		int n = 0;
		for (int j = 0; j < nti.length; ++j) {
			final Coord coord = new Coord(ikeys[0].sz().x * ntp[j], 0);
			final int n2 = array[nti[j] + n] ? 1 : 0;
			g.image(ikeys[n2], coord);
			g.image(tips[nti[j]], coord.add((ikeys[0].sz().x - tips[nti[j]].sz().x) / 2, ikeys[0].sz().y - tips[nti[j]].sz().y - ((n2 != 0) ? UI.scale(9) : UI.scale(12))));
		}
		final int n3 = ikeys[0].sz().x - ikeys[2].sz().x / 2;
		for (int k = 0; k < shi.length; ++k) {
			final Coord coord2 = new Coord(ikeys[0].sz().x * shp[k] + n3, 0);
			final boolean b = array[shi[k] + n];
			g.image(ikeys[b ? 3 : 2], coord2);
			g.image(tips[shi[k]], coord2.add((ikeys[2].sz().x - tips[shi[k]].sz().x) / 2, ikeys[2].sz().y - tips[shi[k]].sz().y - (b ? UI.scale(9) : UI.scale(12))));
		}
	}
    }

    public boolean keydown(KeyDownEvent ev) {
	if (originalLayout){
	double now = (ev.awt.getWhen() / 1000.0) + latcomp;
	Integer keyp = keys.get(ev.awt.getKeyCode());
	if(keyp != null) {
	    int key = keyp + 12;
	    if((ev.awt.getModifiersEx() & KeyMatch.S) != 0) key += 12;
	    if((ev.awt.getModifiersEx() & KeyMatch.C) != 0)  key -= 12;
	    if(!cur[key]) {
		if(actn >= act.length) {
		    wdgmsg("stop", act[0], (float)(now - start));
		    for(int i = 1; i < actn; i++)
			act[i - 1] = act[i];
		    actn--;
		}
		wdgmsg("play", key, (float)(now - start));
		cur[key] = true;
		act[actn++] = key;
	    }
	    return(true);
	}
	} else {
		final double n = ev.awt.getWhen() / 1000.0 + this.latcomp;
		final Integer n2 = keys.get(ev.awt.getKeyCode());
		if (n2 != null) {
			int n3 = n2;
			if (!this.cur[n3]) {
				if (this.actn >= this.act.length) {
					this.wdgmsg("stop", new Object[] { this.act[0], (float)(n - this.start) });
					for (int i = 1; i < this.actn; ++i) {
						this.act[i - 1] = this.act[i];
					}
					--this.actn;
				}
				this.wdgmsg("play", new Object[] { n3, (float)(n - this.start) });
				this.cur[n3] = true;
				this.act[this.actn++] = n3;
			}
		}
	}
	super.keydown(ev);
	return(true);
    }

    private void stopnote(double now, int key) {
	if(cur[key]) {
	    outer: for(int i = 0; i < actn; i++) {
		if(act[i] == key) {
		    wdgmsg("stop", key, (float)(now - start));
		    for(actn--; i < actn; i++)
			act[i] = act[i + 1];
		    break outer;
		}
	    }
	    cur[key] = false;
	}
    }

    public boolean keyup(KeyUpEvent ev) {
	if (originalLayout){
	double now = (ev.awt.getWhen() / 1000.0) + latcomp;
	Integer keyp = keys.get(ev.awt.getKeyCode());
	if(keyp != null) {
	    int key = keyp;
	    stopnote(now, key);
	    stopnote(now, key + 12);
	    stopnote(now, key + 24);
	    return(true);
	}
	} else {
		final double n = ev.awt.getWhen() / 1000.0 + this.latcomp;
		final Integer n2 = keys.get(ev.awt.getKeyCode());
		if (n2 != null) {
			final int intValue = n2;
			this.stopnote(n, intValue);
			return true;
		}
	}
	return(super.keyup(ev));
    }

	public void setMusicWndLayout (boolean originalLayout) {
		if (originalLayout) {
			nti = new int[] {0, 2, 4, 5, 7, 9, 11};
			shi = new int[] {1, 3, 6, 8, 10};
			ntp = new int[] {0, 1, 2, 3, 4, 5,  6};
			shp = new int[] {0, 1, 3, 4,  5};

			cur = new boolean[12 * 3];

			Map<Integer, Integer> km = new HashMap<Integer, Integer>();
			km.put(KeyEvent.VK_Z,  0);
			km.put(KeyEvent.VK_S,  1);
			km.put(KeyEvent.VK_X,  2);
			km.put(KeyEvent.VK_D,  3);
			km.put(KeyEvent.VK_C,  4);
			km.put(KeyEvent.VK_V,  5);
			km.put(KeyEvent.VK_G,  6);
			km.put(KeyEvent.VK_B,  7);
			km.put(KeyEvent.VK_H,  8);
			km.put(KeyEvent.VK_N,  9);
			km.put(KeyEvent.VK_J, 10);
			km.put(KeyEvent.VK_M, 11);
			Tex[] il = new Tex[4];
			for(int i = 0; i < 4; i++) {
				il[i] = Resource.classres(MusicWnd.class).layer(Resource.imgc, i).tex();
			}
			String tc = "ZSXDCVGBHNJM";
			Text.Foundry fnd = new Text.Foundry(Text.fraktur.deriveFont(java.awt.Font.BOLD, 16)).aa(true);
			Tex[] tl = new Tex[tc.length()];
			for(int i = 0; i < nti.length; i++) {
				int ki = nti[i];
				tl[ki] = fnd.render(tc.substring(ki, ki + 1), new Color(0, 0, 0)).tex();
			}
			for(int i = 0; i < shi.length; i++) {
				int ki = shi[i];
				tl[ki] = fnd.render(tc.substring(ki, ki + 1), new Color(255, 255, 255)).tex();
			}
			keys = km;
			ikeys = il;
			tips = tl;
			this.resize(ikeys[0].sz().mul(nti.length, 1));
		} else {
			nti = new int[] {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24, 26, 28, 29, 31, 33, 35};
			shi = new int[] {1, 3, 6, 8, 10, 13, 15, 18, 20, 22, 25, 27, 30, 32, 34};
			ntp = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
			shp = new int[] {0, 1, 3, 4, 5, 7, 8, 10, 11, 12, 14, 15, 17, 18, 19};
			cur = new boolean[36];

			Map<Integer, Integer> keys2 = new HashMap<Integer, Integer>();
			keys2.put(49, 0);
			keys2.put(50, 1);
			keys2.put(51, 2);
			keys2.put(52, 3);
			keys2.put(53, 4);
			keys2.put(54, 5);
			keys2.put(55, 6);
			keys2.put(56, 7);
			keys2.put(57, 8);
			keys2.put(48, 9);
			keys2.put(81, 10);
			keys2.put(87, 11);
			keys2.put(69, 12);
			keys2.put(82, 13);
			keys2.put(84, 14);
			keys2.put(89, 15);
			keys2.put(85, 16);
			keys2.put(73, 17);
			keys2.put(79, 18);
			keys2.put(80, 19);
			keys2.put(65, 20);
			keys2.put(83, 21);
			keys2.put(68, 22);
			keys2.put(70, 23);
			keys2.put(71, 24);
			keys2.put(72, 25);
			keys2.put(74, 26);
			keys2.put(75, 27);
			keys2.put(76, 28);
			keys2.put(90, 29);
			keys2.put(88, 30);
			keys2.put(67, 31);
			keys2.put(86, 32);
			keys2.put(66, 33);
			keys2.put(78, 34);
			keys2.put(77, 35);
			final Tex[] ikeys2 = new Tex[4];
			for (int i = 0; i < 4; i++) {
				ikeys2[i] = Resource.remote().loadwait("ui/music").layer(Resource.imgc,i).tex();
			}
			final String s =    "1234567890QW"+"ERTYUIOPASDF"+"GHJKLZXCVBNM";
			final Text.Foundry aa = new Text.Foundry(Text.fraktur.deriveFont(1, 16.0f)).aa(true);
			final Tex[] tips2 = new Tex[s.length()];
			for (int j = 0; j < nti.length; ++j) {
				final int n = nti[j];
				tips2[n] = aa.render(s.substring(n, n + 1), new Color(0, 0, 0)).tex();
			}
			for (int k = 0; k < shi.length; ++k) {
				final int n2 = shi[k];
				tips2[n2] = aa.render(s.substring(n2, n2 + 1), new Color(255, 255, 255)).tex();
			}
			keys = keys2;
			ikeys = ikeys2;
			tips = tips2;
			this.resize(ikeys[0].sz().x*(nti.length), UI.scale(200));

			hafenMidiplayer = new HafenMidiplayer(this);
			midiThread = new Thread(hafenMidiplayer, "HafenMidiPlayer");
			midiThread.start();
			TextEntry midiEntry = new TextEntry(UI.scale(200),"midiFiles/example.mid");
			add(midiEntry,UI.scale(10),sz.y-UI.scale(140));
			midiEntry.focusctl = false;
			midiEntry.hasfocus = false;

			Button startButton = new Button(UI.scale(100), "Play") {
				@Override
				public void click() {
					try {
						hafenMidiplayer.startPlaying(haven.MainFrame.gameDir + midiEntry.buf.line());
						parent.setfocus(this);
					} catch (Exception e) {
//					System.out.println(e);
					}
				}
			};
			add(startButton, new Coord(UI.scale(10), sz.y-UI.scale(120)));

			Button stopButton = new Button(UI.scale(100), "Stop") {
				@Override
				public void click() {
					try {
						hafenMidiplayer.stopPlaying();
						parent.setfocus(this);
					} catch (Exception e) {
//					System.out.println(e);
					}
				}
			};
			add(stopButton, new Coord(UI.scale(110), sz.y-UI.scale(120)));
			setfocus(stopButton);

			Button partyButton = new Button(UI.scale(100), "Party playing") {
				public void click() {
					for (Widget w = ui.gui.chat.lchild; w != null; w = w.prev) {
						if (w instanceof ChatUI.MultiChat) {
							ChatUI.MultiChat chat = (ChatUI.MultiChat) w;
							if (chat.name().equals("Party")) {
								String timetoplay = ""+(System.currentTimeMillis()+1000);
								chat.send("HFMPL@@@"+timetoplay+"|"+midiEntry.buf.line());
								break;
							}
						}
					}
				}


			};
			add(partyButton, new Coord(UI.scale(440), sz.y-UI.scale(120)) );

			HSlider tempoHSlider = new HSlider(UI.scale(200), 0, 20, 0) {
				protected void attach(UI ui) {
					super.attach(ui);
					val = (int) (tempo * 10);
				}

				public void changed() {
					tempo = val / 10.0;
					hafenMidiplayer.setTempo((float)tempo);
					tempoLabel.settext(("Tempo Factor " + tempo));
					//System.out.println(tempo);
				}};
			add ( tempoLabel , new Coord(UI.scale(230), sz.y-UI.scale(130)));
			add(tempoHSlider , new Coord(UI.scale(230), sz.y-UI.scale(110)));
		}
		this.originalLayout = originalLayout;
	}

	public class CustomReceiver implements Receiver {

		public CustomReceiver() {

		}

		@Override
		public void send(MidiMessage message, long timeStamp) {
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage) message;
				//System.out.print("Channel: " + sm.getChannel() + " ");
				//SKIP PERCUSSION CHANNELS, MAYBE MAKE THIS TOGGLABLE?
				if (sm.getChannel() == 10 || sm.getChannel() == 11) {
					return;
				}
				if (sm.getCommand() == NOTE_ON) {
					int velocity = sm.getData2();
					if (velocity > 0) {
						keydown2(getHafenKey(sm),System.currentTimeMillis());
					} else {
						keyup2(getHafenKey(sm),System.currentTimeMillis());
					}

				} else if (sm.getCommand() == NOTE_OFF) {
					keyup2(getHafenKey(sm),System.currentTimeMillis());
				} else {
					//System.out.println("Command:" + sm.getCommand());
				}
			} else {
				//System.out.println("Other message: " + message.getClass());
			}
		}

		public int getHafenKey(ShortMessage sm) {
			int key = sm.getData1();
			int octave = (key / 12)-1;
			int note = key % 12;
			while (octave > 5) {
				octave--;
			}
			while (octave < 3) {
				octave++;
			}
			//System.out.println(note);
			return note+(octave-3)*12;
		}

		@Override
		public void close() {

		}
	}

	public class HafenMidiplayer implements Runnable{
		MusicWnd musicWnd;
		public Receiver synthRcvr = new CustomReceiver();
		public Transmitter seqTrans;
		public Sequencer sequencer;
		public Sequence sequence;
		public boolean active = true;
		public boolean start = false;
		public boolean stop = false;
		public boolean kill = false;
		public boolean changedTempo = false;
		public boolean synchPlay = false;
		public long timeToPlay = 0;
		public String midiFile = "";
		public float tempo = 1f;
		public HafenMidiplayer (MusicWnd musicWnd) {
			this.musicWnd = musicWnd;
		}

		@Override
		public void run() {
			while (active) {
				if (start) {
					try {
						//System.out.println("Starting sequence..");
						sequence = MidiSystem.getSequence(new File(midiFile));
						sequencer = MidiSystem.getSequencer(false);
						seqTrans = sequencer.getTransmitter();
						seqTrans.setReceiver(synthRcvr);

						sequencer.open();
						sequencer.setSequence(sequence);
						sequencer.start();
						start = false;
					} catch (Exception e) {}
				}

				if (changedTempo && sequencer != null) {
					try {
						sequencer.setTempoFactor((float)tempo);
						changedTempo = false;
					} catch (Exception e) {
					}
				}
				if (stop) {
					try {
						sequencer.stop();
						stop = false;
					} catch (Exception e) {
					}
				}
				if (kill) {
					try {
						sequencer.stop();
						return;
					} catch (Exception e) {
					}
				}
				if (synchPlay) {
					try {
						System.out.println("SYNC PLAY " + midiFile + " in " + (timeToPlay-System.currentTimeMillis()) + " milliseconds");
						if (timeToPlay-System.currentTimeMillis() < 100 || timeToPlay-System.currentTimeMillis() > 1000) {
							ui.gui.error("Your clock is out of synch, go to windows clock internet time and update it with time.windows.com ");
							continue;
						}
						sequence = MidiSystem.getSequence(new File(midiFile));
						sequencer = MidiSystem.getSequencer(false);
						seqTrans = sequencer.getTransmitter();
						seqTrans.setReceiver(synthRcvr);

						sequencer.open();
						sequencer.setSequence(sequence);
						//wait until we should start
						Thread.sleep(timeToPlay-System.currentTimeMillis());
						sequencer.start();
						synchPlay = false;
					} catch (Exception e) {
					}
				}

				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
//					e.printStackTrace();
				}
			}
		}


		public void startPlaying(String text) {
			midiFile = text;
			start = true;
		}
		public void stopPlaying() {
			stop = true;
		}

		public void kill() {kill = true;}

		public void setTempo(float tempo) {
			this.tempo = tempo;
			changedTempo = true;
		}

		public void synchPlay(long timeToPlay, String track) {
			this.midiFile = track;
			this.timeToPlay = timeToPlay;
			synchPlay = true;
		}

	}

	public boolean keydown2(int hafenkey, long time) {
		final double n = time / 1000.0 + this.latcomp;
		final Integer n2 = hafenkey;
		if (n2 != null) {
			int n3 = n2;
			if (!this.cur[n3]) {
				if (this.actn >= this.act.length) {
					this.wdgmsg("stop", new Object[] { this.act[0], (float)(n - this.start) });
					for (int i = 1; i < this.actn; ++i) {
						this.act[i - 1] = this.act[i];
					}
					--this.actn;
				}
				this.wdgmsg("play", new Object[] { n3, (float)(n - this.start) });
				this.cur[n3] = true;
				this.act[this.actn++] = n3;
			}
			return true;
		}
		return true;
	}

	public boolean keyup2(int hafenkey, long time) {
		final double n = time / 1000.0 + this.latcomp;
		final Integer n2 = hafenkey;
		if (n2 != null) {
			final int intValue = n2;
			this.stopnote(n, intValue);
			return true;
		}
		return true;
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if ((sender == this) && (msg == "close")) {
			hafenMidiplayer.kill();
			this.reqdestroy();
			midiThread.interrupt();
			super.wdgmsg(sender, msg, args);
		}
		super.wdgmsg(sender, msg, args);
	}

}
