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

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class IButton extends SIWidget {
    public final BufferedImage up, down, hover;
    public boolean h = false, a = false;
    public Runnable action = null;
    private UI.Grab d = null;

    @RName("ibtn")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int a = 0;
	    Indir<Resource> up;
	    if(args[0] instanceof String)
		up = new Resource.Spec(Resource.local(), (String)args[a++]);
	    else
		up = ui.sess.getresv(args[a++]);
	    Indir<Resource> down;
	    if(args[1] instanceof String)
		down = new Resource.Spec(Resource.local(), (String)args[a++]);
	    else
		down = ui.sess.getresv(args[a++]);
	    Indir<Resource> hover = up;
	    if(args.length > a)
		hover = ui.sess.getresv(args[a++]);
	    return(new IButton(up.get().flayer(Resource.imgc).scaled(), down.get().flayer(Resource.imgc).scaled(), hover.get().flayer(Resource.imgc).scaled()));
	}
    }

    public IButton(BufferedImage up, BufferedImage down, BufferedImage hover, Runnable action) {
	super(Utils.imgsz(up));
	this.up = up;
	this.down = down;
	this.hover = hover;
	this.action = action;
    }

    public IButton(BufferedImage up, BufferedImage down, BufferedImage hover) {
	this(up, down, hover, null);
	this.action = () -> wdgmsg("activate");
    }

    public IButton(BufferedImage up, BufferedImage down) {
	this(up, down, up);
    }

    public IButton(String base, String up, String down, String hover, Runnable action) {
	this(Resource.loadsimg(base + up), Resource.loadsimg(base + down), Resource.loadsimg(base + (hover == null?up:hover)), action);
    }

    public IButton(String base, String up, String down, String hover) {
	this(base, up, down, hover, null);
	this.action = () -> wdgmsg("activate");
    }

    public IButton action(Runnable action) {
	this.action = action;
	return(this);
    }

    public void draw(BufferedImage buf) {
	Graphics g = buf.getGraphics();
	BufferedImage img;
	if(a && h)
	    img = down;
	else if(h || (d != null))
	    img = hover;
	else
	    img = up;
	g.drawImage(img, 0, 0, null);
	g.dispose();
    }

    public boolean checkhit(Coord c) {
	if(!c.isect(Coord.z, sz))
	    return(false);
	if(up.getRaster().getNumBands() < 4)
	    return(true);
	return(up.getRaster().getSample(c.x, c.y, 3) >= 128);
    }

    public void click() {
	if(action != null)
	    action.run();
    }

    public boolean gkeytype(GlobKeyEvent ev) {
	click();
	return(true);
    }
    
    protected void depress() {
    }

    protected void unpress() {
    }

    public boolean mousedown(MouseDownEvent ev) {
	if(ev.b != 1)
	    return(false);
	if(!checkhit(ev.c))
	    return(false);
	a = true;
	d = ui.grabmouse(this);
	depress();
	redraw();
	return(true);
    }

    public boolean mouseup(MouseUpEvent ev) {
	if((d != null) && (ev.b == 1)) {
	    d.remove();
	    d = null;
	    a = false;
	    redraw();
	    if(checkhit(ev.c)) {
		unpress();
		click();
	    }
	    return(true);
	}
	return(false);
    }

    public void mousemove(MouseMoveEvent ev) {
	boolean h = checkhit(ev.c);
	if(h != this.h) {
	    this.h = h;
	    redraw();
	}
    }
}
