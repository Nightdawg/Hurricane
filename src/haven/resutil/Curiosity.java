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

package haven.resutil;

import haven.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Curiosity extends ItemInfo.Tip implements GItem.ColorInfo {
    public final Color better = new Color(0, 200, 0, 110), worse = new Color(175, 0, 0, 110);
    public final int exp, mw, enc, time;
    public final UI ui;
	public transient final int lph;

    public Curiosity(Owner owner, int exp, int mw, int enc, int time) {
	super(owner);
	this.exp = exp;
	this.mw = mw;
	this.enc = enc;
	this.time = time;
	this.lph = (exp > 0 && time > 0) ? (int)((3600 * exp / time)* GameUI.gameTimeSpeedMultiplier) : 0;
	UI ui = null;
	Widget wdg = owner.fcontext(Widget.class, false);
	if(wdg != null && (wdg.getparent(SAttrWnd.class) != null))
	    ui = wdg.ui;
    if (owner instanceof GItem) {
        ((GItem) owner).studytime = this.time / GameUI.gameTimeSpeedMultiplier / 60;
    }
	this.ui = ui;
    if (owner instanceof GItem) {
        ((GItem) owner).studytime = this.time / GameUI.gameTimeSpeedMultiplier / 60;
    }
    }

    static String[] units = {"s", "m", "h", "d"};
    static int[] div = {60, 60, 24};
    static String timefmt(int time) {
	time =	(int) (time/ GameUI.gameTimeSpeedMultiplier);
	int[] vals = new int[units.length];
	vals[0] = time;
	for(int i = 0; i < div.length; i++) {
	    vals[i + 1] = vals[i] / div[i];
	    vals[i] = vals[i] % div[i];
	}
	StringBuilder buf = new StringBuilder();
	for(int i = units.length - 1; i >= 0; i--) {
	    if(vals[i] > 0) {
		if(buf.length() > 0)
			buf.append(String.format(" %02d", vals[i]));
		else
			buf.append(vals[i]);
		buf.append(units[i]);
	    }
	}
	return(buf.toString());
    }

    public BufferedImage tipimg() {
	StringBuilder buf = new StringBuilder();
	if(exp > 0)
	    buf.append(String.format("Learning points: $col[192,192,255]{%s}\n", Utils.thformat(exp)));
	if(time > 0)
	    buf.append(String.format("Study time: $col[192,255,192]{%s}\n", timefmt(time)));
	String remaining = remainingLongTip();
	if(remaining != null)
		buf.append(String.format("Remaining: $col[255,224,192]{%s}\n", remaining));
	buf.append(String.format("LP/H: $col[192,255,255]{%d}\n", this.lph));
	if(mw > 0) {
		buf.append(String.format("LP/H/Weight: $col[192,255,255]{%d}\n", this.lph/mw));
	    buf.append(String.format("Mental weight: $col[255,192,255]{%d}\n", mw));
	}
	if(enc > 0)
	    buf.append(String.format("Experience cost: $col[255,255,192]{%d}\n", enc));
	return(RichText.render(buf.toString(), 0).img);
    }

    public Color olcol() {
	Object tip = (ui == null) ? null : ui.lasttip;
	if(tip instanceof ItemInfo.InfoTip) {
	    Curiosity that = ItemInfo.find(Curiosity.class, ((ItemInfo.InfoTip)tip).info());
	    if(that != null) {
		double crate = (double)that.exp / (double)that.time;
		double trate = (double)this.exp / (double)this.time;
		if(Debug.ff)
		    Debug.dump(trate, crate);
		double ε = 0.5 / 3600.0;
		if(trate < crate - ε)
		    return(worse);
		if(trate > crate + ε)
		    return(better);
	    }
	}
	return(null);
    }

	private String remainingLongTip() {
		return remainingLongTip(remaining());
	}
	private String remainingLongTip(int remaining) {
		if(remaining >= 0) {
			return timefmt(remaining);
		}
		return null;
	}

	private String remainingShortTip(int time) {
		if (time < 0) {
			return null;
		}

		// Convert seconds into "real time"
		time = (int) (time / GameUI.gameTimeSpeedMultiplier);

		// If time is greater than or equal to 10 hours
		if (time >= 36000) {
			// Show only hours, e.g., 10h, 12h, etc.
			return String.format("%dh", time / 3600);
		} else if (time >= 3600) {
			// If time is less than 10 hours but greater than or equal to 1 hour
			int hours = time / 3600;
			int minutes = (time % 3600) / 60;
			return String.format("%dh %dm", hours, minutes);
		} else if (time >= 60) {
			// If time is greater than or equal to 60 seconds but less than 1 hour
			return String.format("%dm", time / 60);
		} else {
			// If time is less than 60 seconds
			return "1m";
		}
	}
	public Pair<String, String> remainingTip() {
		int time = remaining();
		return new Pair<>(remainingShortTip(time), remainingLongTip(time));
	}
	public int remaining() {
		if(owner instanceof GItem) {
			GItem item = ((GItem) owner);
			GItem.MeterInfo m = ItemInfo.find(GItem.MeterInfo.class, item.info());
			double meter = (m != null) ? m.meter() : 0;
			if(meter > 0) {
				long now = System.currentTimeMillis();
				long remStudy = (long) ((1.0 - meter) * time);
				long elapsed = (long) (GameUI.gameTimeSpeedMultiplier * (now - item.meterUpdated) / 1000);
				return (int) (remStudy - elapsed);
			}
		}
		return -1;
	}
}
