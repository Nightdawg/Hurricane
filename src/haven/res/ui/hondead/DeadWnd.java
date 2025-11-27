/* Preprocessed source code */
package haven.res.ui.hondead;

import haven.*;
import java.util.*;

/* >wdg: DeadWnd */
@haven.FromResource(name = "ui/hondead", version = 7)
public class DeadWnd extends Window {
    CList ls;
    
    public DeadWnd() {
	super(Coord.z, "Characters", true);
	add(new Label("Choose a character:"), Coord.z);
	ls = add(new CList(UI.scale(200, 200)), UI.scale(0, 20));
	add(new Button(UI.scale(200), "To Valhalla!", () -> {
		    if(ls.sel != null)
			DeadWnd.this.wdgmsg("ch", ls.sel.text);
	}), UI.scale(0, 230));
	pack();
    }
    
    public static Widget mkwidget(UI ui, Object... args) {
	return(new DeadWnd());
    }
	
    private static class CList extends Widget {
	static final int itemh = UI.scale(20);
	List<Text> chrs = new ArrayList<Text>();
	Text sel;
	Scrollbar sb;
	
	private CList(Coord sz) {
	    super(sz);
	    sb = adda(new Scrollbar(sz.y, 0, 0), sz.x, 0, 1, 0);
	}
	
	public void draw(GOut g) {
	    g.chcolor(0, 0, 0, 128);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	    synchronized(chrs) {
		for(int i = 0, y = 0; (y < sz.y) && (i + sb.val < chrs.size()); i++, y += itemh) {
		    Text c = chrs.get(i + sb.val);
		    if(c == sel) {
				g.chcolor(100, 100, 100, 128);
			g.frect(new Coord(0, y), new Coord(sz.x, itemh));
			g.chcolor();
		    }
		    g.aimage(c.tex(), new Coord(0, y + (itemh / 2)), 0, 0.5);
		}
	    }
	    super.draw(g);
	}
	
	public boolean mousedown(MouseDownEvent ev) {
	    if(ev.propagate(this) || super.mousedown(ev))
		return(true);
	    int sel = (ev.c.y / itemh) + sb.val;
	    synchronized(chrs) {
		this.sel = (sel >= chrs.size()) ? null : chrs.get(sel);
	    }
	    return(true);
	}
	
	public boolean mousewheel(MouseWheelEvent ev) {
	    sb.ch(ev.a);
	    return(true);
	}
	
	public void add(Text chr) {
	    synchronized(chrs) {
		chrs.add(chr);
		sb.max = chrs.size();
	    }
	}
    }
    
    public void uimsg(String name, Object... args) {
	if(name == "add") {
	    ls.add(Text.render((String)args[0]));
	} else {
	    super.uimsg(name, args);
	}
    }
}
