package haven;

import haven.render.MixColor;
import haven.render.Pipe;

import java.awt.*;

public class GobPartyHighlight extends GAttrib implements Gob.SetupMod {
    public final Color c;
    
    public GobPartyHighlight(Gob g, Color c) {
	super(g);
	this.c = c;
    }
    
    public void start() {
    }
    
    public Pipe.Op gobstate() {
        return new MixColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }
}