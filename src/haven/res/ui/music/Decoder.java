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
public abstract class Decoder implements Sprite.Factory {
    public Sprite create(Sprite.Owner owner, Resource res, Message sdt) {
	int key = sdt.uint8();
	if(key == 255) {
	    return(new MusicOverlay(owner, res, System.currentTimeMillis() / 1000.0));
	} else {
	    int bid = sdt.int32();
	    double ns = sdt.float32();
	    double ne = 0.0;
	    if(!sdt.eom())
		ne = sdt.float32();
	    MusicOverlay base = (MusicOverlay)(owner.context(Gob.class).findol(bid).spr);
	    return(makenote(owner, res, base, key, ns, ne));
	}
    }

    public abstract Sprite makenote(Sprite.Owner owner, Resource res, MusicOverlay base, int key, double ns, double ne);
}
