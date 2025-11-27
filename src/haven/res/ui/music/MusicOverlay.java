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
public class MusicOverlay extends Sprite implements CS {
    public static final double HZ = 44100;
    public double start;
    public final Collection<NoteOverlay> cur = new ArrayList<NoteOverlay>();
    public final List<NoteOverlay> wait = new ArrayList<NoteOverlay>();
    public final RenderTree.Node player;

    public MusicOverlay(Owner owner, Resource res, double start) {
	super(owner, res);
	this.start = start;
	this.player = new ActAudio.PosClip(this);
    }

    @Override public void added(RenderTree.Slot slot) {
	slot.add(player);
    }

    public int get(double[][] buf, int ns) {
	int nch = buf.length;
	for(int i = 0; i < nch; i++) {
	    for(int o = 0; o < ns; o++)
		buf[i][o] = 0.0;
	}
	double now = System.currentTimeMillis() / 1000.0;
	double rnow = now - start;
	double iv = ns / HZ;

	synchronized(this) {
	    {
		double[][] sbuf = new double[nch][ns];
		note: for(Iterator<NoteOverlay> ni = cur.iterator(); ni.hasNext();) {
		    NoteOverlay note = ni.next();
		    note.setvol(rnow);
		    int left = ns, boff = 0;
		    while(left > 0) {
			int ret = note.clip.get(sbuf, left);
			if(ret < 0) {
			    ni.remove();
			    continue note;
			}
			for(int c = 0; c < nch; c++) {
			    for(int i = 0, o = boff; i < ret; i++, o++)
				buf[c][o] += sbuf[c][i];
			}
			left -= ret;
			boff += ret;
		    }
		}
	    }

	    note: while(!wait.isEmpty()) {
		NoteOverlay note = wait.get(0);
		if(note.ck) {
		    if(note.ns < rnow)
			this.start = Math.min(this.start, now - note.ns);
		    else if(note.ns > rnow + 1)
			this.start = now - note.ns;
		}
		note.ck = true;
		if(note.ns > rnow + iv)
		    break;
		note.setvol(rnow);
		wait.remove(0);
		int off = Math.min(Math.max((int)((note.ns - rnow) * HZ), 0), ns);
		int left = ns - off, boff = 0;
		double[][] sbuf = new double[nch][left];
		while(left > 0) {
		    int ret = note.clip.get(sbuf, left);
		    if(ret < 0)
			continue note;
		    for(int c = 0; c < nch; c++) {
			for(int i = 0, o = off + boff; i < ret; i++, o++)
			    buf[c][o] += sbuf[c][i];
		    }
		    left -= ret; boff += ret;
		}
		cur.add(note);
	    }
	}
	return(ns);
    }
}
