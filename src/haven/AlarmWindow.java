package haven;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlarmWindow extends Window {

	private static AlarmList al;
	private Label enabledLabel;
	private Label alarmNameLabel;
	private Label resPathLabel;
	private Label soundFileLabel;
	private Label dontTriggerCheckboxLabel;
	private Label dontTriggerCheckboxLabel2;
	private final Text.Foundry warningFoundry = new Text.Foundry(Text.sans, 14);
	static Label bottomNote;
	static Label defaultsReloadedText;
	private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static Future<?> future;
	private static Future<?> future2;


	public AlarmWindow() {
		super(UI.scale(825, 400), "Custom Alarm Manager");
		Widget prev;
		prev = add(enabledLabel = new Label("Enabled"), UI.scale(38,10));
		enabledLabel.tooltip = RichText.render("This checkbox determines whether the alarm will be triggered, or not.\n$col[185,185,185]{I added this checkbox so you don't have to delete the entire alarm if you just want to turn it off for a while.}", UI.scale(300));
		prev = add(alarmNameLabel = new Label("Alarm Name"), prev.pos("ul").adds(73, 0));

		alarmNameLabel.tooltip = RichText.render("You can set this to whatever you want, or even leave empty. It's here just to help you find your alarms easier in the list.", UI.scale(300));
		prev = add(resPathLabel = new Label("Resource Path"), prev.pos("ul").adds(170, 0));
		resPathLabel.tooltip = RichText.render("This is the resource path of the entity type for which you want to throw an alarm.\n$col[200,0,0]{Note: This field can not be changed after the alarm has been added!}", UI.scale(300));
		prev = add(soundFileLabel = new Label("Sound File"), prev.pos("ul").adds(180, 0));
		soundFileLabel.tooltip = RichText.render("This is the name of the .wav sound file that will be played when the alarm is triggered.\nThe file must be present in the \"AlarmSounds\" folder.\n$col[185,185,185]{You don't need to include the file extension in this box, but it must always be a .wav file!}", UI.scale(300));
		prev = add(new Label("Volume"), prev.pos("ul").adds(120, 0));
		prev = add(dontTriggerCheckboxLabel = new Label("Also trigger for"), prev.pos("ul").adds(73, -6));
		prev = add(dontTriggerCheckboxLabel2 = new Label("KO'd or Dead"), prev.pos("ul").adds(2, 14));
		dontTriggerCheckboxLabel.tooltip = dontTriggerCheckboxLabel2.tooltip = RichText.render("This checkbox only affects entities that can be knocked out or dead (for example, animals).", UI.scale(300));


		al = new AlarmList(825, 10);
		for(AlarmItem ai : AlarmManager.getAlarmItems()) {
			al.addItem(ai);
		}
		add(al, UI.scale(25, 35));

		prev = add(new HRuler(UI.scale(850)), UI.scale(0, 350));
		add(new Label("Create new alarm:", warningFoundry), prev.pos("ul").adds(0, 12).x(UI.scale(370)));


		Label enabledLabel;
		prev = add(enabledLabel = new Label("Enabled"), prev.pos("ul").adds(0, 43).x(UI.scale(38)));
		Label alarmNameLabel2;
		prev = add(alarmNameLabel2 = new Label("Alarm Name"), prev.pos("ul").adds(73, 0));
		alarmNameLabel2.tooltip = RichText.render("You can set this to whatever you want, or even leave empty. It's here just to help you find your alarms easier in the list.", UI.scale(300));
		Label resPathLabel2;
		prev = add(resPathLabel2 = new Label("Resource Path"), prev.pos("ul").adds(170, 0));
		resPathLabel2.tooltip = RichText.render("This is the resource path of the entity type for which you want to throw an alarm.\n$col[200,0,0]{Note: This field can not be changed after the alarm has been added!}", UI.scale(300));
		Label soundFileLabel2;
		prev = add(soundFileLabel2 = new Label("Sound File"), prev.pos("ul").adds(180, 0));
		soundFileLabel2.tooltip = RichText.render("This is the name of the .wav sound file that will be played when the alarm is triggered.\nThe file must be present in the \"AlarmSounds\" folder.\n$col[185,185,185]{You don't need to include the file extension in this box, but it must always be a .wav file!}", UI.scale(300));
		prev = add(new Label("Volume"), prev.pos("ul").adds(120, 0));
		Label dontTriggerCheckboxLabel0;
		prev = add(dontTriggerCheckboxLabel0 = new Label("Also trigger for"), prev.pos("ul").adds(73, -6));
		Label dontTriggerCheckboxLabel02;
		prev = add(dontTriggerCheckboxLabel02 = new Label("KO'd or Dead"), prev.pos("ul").adds(2, 14));
		dontTriggerCheckboxLabel0.tooltip = dontTriggerCheckboxLabel02.tooltip = RichText.render("This checkbox only affects entities that can be knocked out or dead (for example, animals).", UI.scale(300));

		CheckBox enabled;
		prev = add(enabled = new CheckBox("") {
			{a = true;}
		}, UI.scale(50,421));
		TextEntry alarmName = new TextEntry(UI.scale(120), "");
		prev = add(alarmName, prev.pos("ul").adds(30, -2));
		add(bottomNote = new Label("NOTE: You can add your own alarm sound files in the \"AlarmSounds\" folder. (The file extension must be .wav)", new Text.Foundry(Text.sans, 12)), prev.pos("ul").adds(0, 34).x(UI.scale(140)));
		TextEntry addGobResname = new TextEntry(UI.scale(200), ""){
			protected void changed() {
				this.settext(this.text().replaceAll(" ", ""));
				super.changed();
			}
		};
		prev = add(addGobResname, prev.pos("ul").adds(138, 0));
		TextEntry addAlarmFilename = new TextEntry(UI.scale(100), ""){
			protected void changed() {
				this.settext(this.text().replaceAll(" ", ""));
				super.changed();
			}
		};
		prev = add(addAlarmFilename, prev.pos("ul").adds(218, 0));
		HSlider addVolume = new HSlider(UI.scale(100),0, 100,50);
		prev =  add(addVolume, prev.pos("ul").adds(114, 3));
		CheckBox knocked;
		prev = add(knocked = new CheckBox("") {
			{a = false;}
		}, prev.pos("ul").adds(130,0));

		add(new Button(UI.scale(100), "Add Alarm") {
			@Override
			public void click() {
				if (future != null)
					future.cancel(true);
				if (addGobResname.text().isEmpty()) {
					bottomNote.settext("You need to set a Resource Path!");
					bottomNote.setcolor(Color.RED);
					bottomNote.c.x = UI.scale(340);
					future = executor.scheduleWithFixedDelay(this::resetBottomNote, 4, 5, TimeUnit.SECONDS);
					return;
				}
				if (addAlarmFilename.text().isEmpty()) {
					bottomNote.settext("You need to set an Alarm File!");
					bottomNote.setcolor(Color.RED);
					bottomNote.c.x = UI.scale(345);
					future = executor.scheduleWithFixedDelay(this::resetBottomNote, 4, 5, TimeUnit.SECONDS);
					return;
				}
				boolean alreadyExists = false;
				for (int i = 0; i < al.items.size(); i++)
					if (al.items.get(i).gobResname.texts.equals(addGobResname.buf.line()))
						alreadyExists = true;
				if (!alreadyExists) {
					al.addItem(new AlarmItem(addGobResname.buf.line(), enabled.a, alarmName.buf.line(), addAlarmFilename.buf.line(), addVolume.val, knocked.a));
					enabled.a = true;
					alarmName.settext("");
					addGobResname.settext("");
					addAlarmFilename.settext("");
					addVolume.val = 50;
					knocked.a = false;
					bottomNote.settext("Alarm added successfully!");
					bottomNote.setcolor(Color.GREEN);
					bottomNote.c.x = UI.scale(350);
					AlarmManager.load(al);
					AlarmManager.save();
					future = executor.scheduleWithFixedDelay(this::resetBottomNote, 3, 5, TimeUnit.SECONDS);
				} else {
					bottomNote.settext("Resource Path already in use! Can't have two alarms for the same object!");
					bottomNote.setcolor(Color.RED);
					bottomNote.c.x = UI.scale(230);
					future = executor.scheduleWithFixedDelay(this::resetBottomNote, 4, 5, TimeUnit.SECONDS);
				}
			}

			public void resetBottomNote() {
				bottomNote.settext("NOTE: You can add your own alarm sound files in the \"AlarmSounds\" folder. (The file extension must be .wav)");
				bottomNote.setcolor(Color.WHITE);
				bottomNote.c.x = UI.scale(140);
				future.cancel(true);
			}
		}, prev.pos("ul").adds(50, -10));

		add(defaultsReloadedText = new Label("", warningFoundry), UI.scale(400, 488)).setcolor(Color.GREEN);

		add(new Button(UI.scale(180), "Load Nightdawg's Defaults") {
			@Override
			public void click() {
				AlarmManager.defaultSettings();
				while(!al.items.isEmpty())
					al.deleteItem(al.items.get(0));
				for(AlarmItem ai : AlarmManager.getAlarmItems()) {
					al.addItem(ai);
				}
				defaultsReloadedText.settext("Default Alarms Restored!");
				if (ui != null && ui.gui != null)
					ui.gui.msg("Default alarms restored!", Color.WHITE);
				AlarmManager.load(al);
				AlarmManager.save();
				future2 = executor.scheduleWithFixedDelay(this::resetText, 3, 5, TimeUnit.SECONDS);
			}

			public void resetText() {
				defaultsReloadedText.settext("");
				future2.cancel(true);
			}
		}, UI.scale(20, 480));
		this.c = new Coord (100, 100);
		pack();
	}



	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if((sender == this) && (msg == "close")) {
			hide();
			AlarmManager.load(al);
			AlarmManager.save();
			bottomNote.settext("NOTE: You can add your own alarm sound files in the \"AlarmSounds\" folder. (The file extension must be .wav)");
			bottomNote.setcolor(Color.WHITE);
			bottomNote.c.x = UI.scale(140);
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	public class AlarmList extends Widget {

		ArrayList<AlarmItem> items = new ArrayList<AlarmItem>();
		Scrollbar sb;
		int rowHeight = UI.scale(30);
		int rows, w;

		public AlarmList(int w, int rows) {
			this.rows = rows;
			this.w = w;
			this.sz = new Coord (UI.scale(w), rowHeight * rows);
			sb = new Scrollbar(rowHeight * rows, 0, 100);
			add(sb, UI.scale(0, 0));
		}

		public AlarmItem listitem(int i) {
			return items.get(i);
		}

		public void addItem(AlarmItem item) {
			add(item);
			items.add(item);
		}

		public void deleteItem(AlarmItem item) {
			item.dispose();
			items.remove(item);
		}

		public int listitems() {
			return items.size();
		}

		@Override
		public boolean mousewheel(MouseWheelEvent ev) {
			sb.ch(ev.a);
			return true;
		}

		@Override
		public boolean mousedown(MouseDownEvent ev) {
			int row = ev.c.y / rowHeight + sb.val;
			if(row >= items.size())
				return super.mousedown(ev);
			if(items.get(row).mousedown(new MouseDownEvent(ev.c.sub(0, ev.c.y / rowHeight * rowHeight), ev.b)))
				return true;
			return super.mousedown(ev);
		}

		@Override
		public boolean mouseup(MouseUpEvent ev) {
			int row = ev.c.y / rowHeight + sb.val;
			if(row >= items.size())
				return super.mouseup(ev);
			if(items.get(row).mouseup(new MouseUpEvent(ev.c.sub(0, ev.c.y / rowHeight * rowHeight), ev.b)))
				return true;
			return super.mouseup(ev);
		}

		@Override
		public void draw(GOut g) {
			sb.max = items.size()-rows;
			for(int i=0; i<rows; i++) {
				if(i+sb.val >= items.size())
					break;
				GOut ig = g.reclip(new Coord(0, i*rowHeight), UI.scale(w, rowHeight));
				items.get(i+sb.val).draw(ig);
			}
			super.draw(g);
		}

		@Override
		public void wdgmsg(Widget sender, String msg, Object... args) {
			if(msg.equals("delete") && sender instanceof AlarmItem) {
				deleteItem((AlarmItem) sender);
			} else {
				super.wdgmsg(sender, msg, args);
			}
		}

	}

	public static class AlarmItem extends Widget {

		private Label gobResname;
		private TextEntry alarmName, alarmFilename;
		private HSlider volume;
		private CheckBox enabled, knocked;

		public AlarmItem(String gobResname, boolean enabled, String alarmName, String alarmFilename, int volume, boolean knocked) {
			Widget prev;
			prev = add(this.enabled = new CheckBox("") {
				{a = enabled;}
				@Override
				public void changed(boolean val) {
					AlarmManager.load(al);
					AlarmManager.save();
					super.changed(val);
				}
			}, UI.scale(27,6));
			this.alarmName = new TextEntry(UI.scale(120), alarmName){
				@Override
				protected void changed() {
					AlarmManager.load(al);
					AlarmManager.save();
					super.changed();
				}
			};
			prev = add(this.alarmName, prev.pos("ul").adds(30,-2));
//			this.gobResname = new TextEntry(UI.scale(200), gobResname){
//				@Override
//				protected void changed() {
//					AlarmManager.load(al);
//					AlarmManager.save();
//					super.changed();
//				}
//
//				@Override
//				public boolean keydown(KeyEvent e) {
//					return false;
//				}
//			};
			this.gobResname = new Label(gobResname, UI.scale(200));
			prev = add(this.gobResname, prev.pos("ul").adds(138,0));
			this.alarmFilename = new TextEntry(UI.scale(100), alarmFilename.replace(".wav", "")){
				@Override
				protected void changed() {
					this.settext(this.text().replaceAll(" ", ""));
					AlarmManager.load(al);
					AlarmManager.save();
					super.changed();
				}
			};
			prev = add(this.alarmFilename, prev.pos("ul").adds(218,0));
			this.volume = new HSlider(UI.scale(100), 0, 100, volume){
				@Override
				public void changed() {
					AlarmManager.load(al);
					AlarmManager.save();
					super.changed();
				}
			};
			prev = add(this.volume, prev.pos("ul").adds(114,3));
			prev = add(this.knocked = new CheckBox("") {
				{a = knocked;}
				@Override
				public boolean mousedown(MouseDownEvent ev) {
					return super.mousedown(ev);
				}
				@Override
				public void changed(boolean val) {
					AlarmManager.load(al);
					AlarmManager.save();
					super.changed(val);
				}
			}, prev.pos("ul").adds(130,0));
			prev = add(new Button(UI.scale(70), "Preview") {
				@Override
				public boolean mousedown(MouseDownEvent ev) {
					if(ev.b != 1)
						return true;
					File file = new File(haven.MainFrame.gameDir + "AlarmSounds/" + getAlarmFilename());
					if(!file.exists() || file.isDirectory()) {
						if (ui != null && ui.gui != null)
							ui.gui.msg("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!", Color.WHITE);
						return super.mousedown(ev);
					}
					try {
						AudioInputStream in = AudioSystem.getAudioInputStream(file);
						AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
						AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
						Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
						((Audio.Mixer)Audio.player.stream).add(new Audio.VolAdjust(klippi, getVolume()/50.0));
					} catch(UnsupportedAudioFileException e) {
						e.printStackTrace();
					} catch(IOException e) {
						e.printStackTrace();
					}
					return super.mousedown(ev);

				}
			}, prev.pos("ul").adds(45,-4));
			prev = add(new Button(UI.scale(26), "X") {
				@Override
				public boolean mousedown(MouseDownEvent ev) {
					if(ev.b != 1)
						return super.mousedown(ev);
					wdgmsg(this.parent, "delete");
					AlarmManager.load(al);
					AlarmManager.save();
					return super.mousedown(ev);
				}
			}, prev.pos("ul").adds(80,0));
		}

		public int getVolume() {
			return volume.val;
		}

		public boolean getKnocked() {
			return this.knocked.a;
		}

		public boolean getEnabled() {
			return this.enabled.a;
		}

		public String getGobResname() {
			return gobResname.texts;
		}

		public String getAlarmFilename() {
			if (alarmFilename.buf.line().endsWith(".wav")){
				return alarmFilename.buf.line();
			} else {
				return (alarmFilename.buf.line()+".wav");
			}

		}

		public String getAlarmName() {
			return alarmName.buf.line();
		}

		@Override
		public void draw(GOut g) {
			super.draw(g);
		}

//		@Override
//		public void mousemove(MouseMoveEvent ev) {
//			if(ev.c.x > 470)
//				super.mousemove(new MouseMoveEvent(ev.c.sub(UI.scale(15), 0)));
//			else
//				super.mousemove(ev);
//		}

		@Override
		public boolean mousedown(MouseDownEvent ev) {
			// ND: This is the old mousedown method basically.
			// Child widgets of widgets can't be clicked anymore for some reason but idk how to fix it for now, so just use this old one.
			Coord c = ev.c;
			int button = ev.b;
			for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
				if(!wdg.visible())
					continue;
				Coord cc = xlate(wdg.c, true);
				if(c.isect(cc, wdg.sz)) {
					if(wdg.mousedown(new MouseDownEvent(c.add(cc.inv()), button))) {
						return(true);
					}
				}
			}
			return(false);
		}
	}
}