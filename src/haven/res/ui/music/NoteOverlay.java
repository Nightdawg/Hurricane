/* Preprocessed source code */
package haven.res.ui.music;

import haven.*;
import haven.render.*;
import java.awt.Color;
import java.util.*;
import java.io.*;
import java.awt.event.KeyEvent;
import haven.Audio.CS;

/* >wdg: MusicWnd */
@haven.FromResource(name = "ui/music", version = 35)
public class NoteOverlay extends Sprite implements Sprite.CUpd {
    public static final int A4 = 9;
    public final MusicOverlay base;
    public Audio.VolAdjust clip;
    public double ns, ne;
    public double dt = 0.1;
    public boolean mend = true;
    boolean ck = false;

    public NoteOverlay(Owner owner, Resource res, MusicOverlay base, CS clip, double ns, double ne) {
	super(owner, res);
	this.base = base;
	this.clip = new Audio.VolAdjust(clip);
	this.ns = ns;
	this.ne = ne;
	add: synchronized(base) {
	    for(ListIterator<NoteOverlay> i = base.wait.listIterator(); i.hasNext();) {
		NoteOverlay note = i.next();
		if(note.ns > ns) {
		    i.previous();
		    i.add(this);
		    break add;
		}
	    }
	    base.wait.add(this);
	}
    }

    public static CS tuneclip(Resource res, int basekey, int key) {
	CS ret = null;
	if(ret == null) {
	    final Resource.Audio clip = res.layer(Resource.audio, "rep");
	    if(clip != null) {
		final Resource.Audio beg = res.layer(Resource.audio, "beg");
		ret = new Audio.Repeater() {
			private boolean f = true;

			public CS cons() {
			    if(f && (beg != null)) {
				f = false;
				return(beg.stream());
			    }
			    return(clip.stream());
			}
		    };
	    }
	}
	if(ret == null) {
	    Resource.Audio clip = res.layer(Resource.audio, "cl");
	    ret = clip.stream();
	}
	Audio.Resampler tuned = new Audio.Resampler(ret);
	tuned.sp = Math.pow(2.0, (key - basekey) / 12.0);
	return(tuned);
    }

    public boolean setup(RenderList rl) {
	return(false);
    }

    public void update(Message sdt) {
	int key = sdt.uint8();
	int bid = sdt.int32();
	double ns = sdt.float32();
	double ne = 0.0;
	if(!sdt.eom())
	    ne = sdt.float32();
	this.ne = ne;
    }

    public static final Audio.VolAdjust eofclip = new Audio.VolAdjust(new Audio.CS() {
	    public int get(double[][] buf, int ns) {return(-1);}
	});
    protected void eof() {
	clip = eofclip;
    }

    void setvol(double rnow) {
	double ev = 1.0;
	if(mend && (ne != 0) && (rnow > ne)) {
	    ev = 1.0 - ((rnow - ne) / dt);
	    if(ev <= 0.0) {
		ev = 0.0;
		eof();
	    }
	}
	clip.vol = ev*OptWnd.instrumentsSoundVolumeSlider.val/100d;;
    }
}
