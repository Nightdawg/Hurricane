package haven;

import haven.render.MixColor;
import haven.render.Pipe;

import java.awt.*;

public class GobPartyHighlight extends GAttrib implements Gob.SetupMod {
    public MixColor color;
    public static MixColor MEMBER_OL_COLOR = new MixColor(OptWnd.memberPartyColorOptionWidget.currentColor);
    public static MixColor LEADER_OL_COLOR = new MixColor(OptWnd.leaderPartyColorOptionWidget.currentColor);
    public static MixColor YOURSELF_OL_COLOR = new MixColor(OptWnd.yourselfPartyColorOptionWidget.currentColor);
    
    public GobPartyHighlight(Gob g, MixColor color) {
	super(g);
	this.color = color;
    }
    
    public Pipe.Op gobstate() {
        return color;
    }
}