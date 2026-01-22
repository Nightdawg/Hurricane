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

public class AutoGiveButton extends Widget {
    public int state;
    private Fightview.Relation rel;

    public AutoGiveButton(Fightview.Relation rel, long gobid) {
        super(GiveButton.bg.sz());
        this.rel = rel;
        if (gobid > 0 && this.rel.gobid == gobid) {
            this.state = 1;
            rel.peace();
        } else {
            this.state = 0;
        }
    }

    public void draw(GOut g) {
        if(state == 0)
            g.chcolor(200, 200, 200, 255);
        else if(state == 1)
            g.chcolor(0, 255, 0, 255);
        g.image(GiveButton.bg, Coord.z, sz);
        g.chcolor();
        if((state & 1) != 0)
            g.image(GiveButton.ol, Coord.z, sz);
        else
            g.image(GiveButton.sl, Coord.z, sz);
        if((state & 2) != 0)
            g.image(GiveButton.or, Coord.z, sz);
        else
            g.image(GiveButton.sr, Coord.z, sz);
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        return RichText.render("Auto-Reaggro current target " + (state == 0 ? "(Off)" : "(On)"), 0);
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
        this.state = ++this.state % 2;
        if (this.state == 1)
            rel.peace();
        else{
            if (!OptWnd.autoPeaceAnimalsWhenCombatStartsCheckBox.a)
                rel.give.wdgmsg("click", 1);
        }
        return(true);
    }

    public void remoteTrigger(){ //there's probably a better way to click this from outside this class, but I'm dumb so this works instead. -Ard
        this.state = ++this.state %2;
        if (this.state == 1)
            rel.peace();
        else{
            if (!OptWnd.autoPeaceAnimalsWhenCombatStartsCheckBox.a)
                rel.give.wdgmsg("click", 1);
        }
    }
}
