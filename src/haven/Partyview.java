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

import haven.Party.Member;

import java.util.*;
import java.awt.Color;

public class Partyview extends Widget {
    public static final int marg = UI.scale(1);
    public final Party party;
    public final long ign;
    private final Button leave;
    private Map<Member, MemberView> avs = Collections.emptyMap();
    private Map<Long, Member> om = null;
    private Member oldLeader;
    private int ownNumber;
    private Tex ownNumberText;
    public static Color myColor = Color.WHITE;

    @RName("pv")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    return(new Partyview(ui.sess.glob.party, Utils.uiv(args[0])));
	}
    }

    Partyview(Party party, long ign) {
	super(Coord.of(UI.scale(111) + Window.wbox.bisz().x, 130));
	this.party = party;
	this.ign = ign;
	this.leave = add(new Button(sz.x, "Leave Party"), Coord.z);
	this.leave.settip("Leave party");
	pack();
	this.leave.hide();
    }

    protected void added() {
	update();
    }

    private static Tex renderNumber(int number) {
	return new TexI(GobHealthInfo.renderLabel(Integer.toString(number), Color.WHITE));
    }

    private static void drawNumber(GOut g, Coord sz, Tex text, int scale) {
	g.chcolor(Color.BLACK);
	g.frect(Coord.z, sz);
	g.chcolor();
	if(text != null)
	    g.aimage(text, sz.div(2), 0.5, 0.5, text.sz().mul(scale));
    }

    private void setOwnNumber(int number) {
	if(ownNumber != number) {
	    if(ownNumberText != null)
		ownNumberText.dispose();
	    ownNumberText = (number == 0) ? null : renderNumber(number);
	    ownNumber = number;
	}
    }

    void drawOwnNumber(GOut g, Coord sz) {
	if(ownNumberText != null && OptWnd.displayPartyMemberNumbersCheckBox.a)
	    drawNumber(g, sz, ownNumberText, 3);
    }

    public static class MemberView extends Frame {
	public final Member m;
	public Color color = Color.WHITE;
	private Tex tooltip = null;
	private int number;
	private Tex numberText;

	public MemberView(Coord sz, Member m) {
	    super(sz, false);
	    this.m = m;
	}

	private void setNumber(int number) {
	    if(this.number != number) {
		if(numberText != null)
		    numberText.dispose();
		this.number = number;
		numberText = renderNumber(number);
	    }
	}

	public void dispose() {
	    if(numberText != null)
		numberText.dispose();
	    super.dispose();
	}

	public void draw(GOut g) {
	    drawNumber(g, sz, OptWnd.displayPartyMemberNumbersCheckBox.a ? numberText : null, 1);
	    super.draw(g);
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if(!ev.c.isect(box.btloff(), inner()))
		return(super.mousedown(ev));
	    wdgmsg("click", ev.b);
	    return(true);
	}

	public void drawframe(GOut g) {
	    g.chcolor(color);
	    box.draw(g, Coord.z, sz);
	    g.chcolor();
	}

	public Object tooltip(Coord c, Widget prev) {
	    Gob gob = m.getgob();
	    if(gob == null)
		return(tooltip);
	    return(null);
	    /*
	    KinInfo ki = gob.getattr(KinInfo.class);
	    if(ki == null)
		return(null);
	    return(tooltip = ki.rendered());
	    */
	}
    }

    private void update() {
	int asz = (sz.x - 2 * marg) / 3;
	boolean rosterChanged = party.memb != this.om;
	if(rosterChanged || party.leader != oldLeader) {
	    Map<Member, MemberView> old = new HashMap<>(this.avs);
	    Map<Member, MemberView> avs = new HashMap<>();
	    List<Member> order = new ArrayList<>(party.memb.values());
	    if(party.leader != null && order.remove(party.leader))
		order.add(0, party.leader);
	    int number = 0, i = 0, self = 0;
	    for(Member m : order) {
		number++;
		if(m.gobid == ign) {
		    self = number;
		    continue;
		}
		MemberView ava = old.remove(m);
		if(ava == null)
		    ava = add(new MemberView(Coord.of(asz), m));
		ava.setNumber(number);
		ava.move(leave.pos("bl").add((i % 3) * (asz + marg),(i / 3) * (asz + marg) + marg));
		avs.put(m, ava);
		i++;
	    }
	    setOwnNumber(order.size() > 1 ? self : 0);
	    for(MemberView ava : old.values())
		ava.reqdestroy();
	    this.om = party.memb;
	    this.oldLeader = party.leader;
	    this.avs = avs;
	    if(leave.show(!avs.isEmpty()))
		pack();
        if (rosterChanged && ui != null && ui.gui != null) {
            ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
            ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
        }
	}
	for(MemberView view : avs.values())
	    view.color = view.m.col;
    if (party.memb.size() == 1) {
        myColor = Color.WHITE;
    }
    }

    public void tick(double dt) {
	update();
	super.tick(dt);
    }

    public void wdgmsg(Widget sender, String msg, Object... args) {
	if(sender == leave) {
	    wdgmsg("leave");
	    return;
	}
	if(sender instanceof MemberView) {
	    wdgmsg("click", (int)((MemberView)sender).m.gobid, args[0]);
	    return;
	}
	super.wdgmsg(sender, msg, args);
    }

    private void updsteam() {
	Steam api = Steam.get();
	if(api != null)
	    api.setparty(Integer.toString(party.id), party.memb.size());
    }

    public void destroy() {
	super.destroy();
	Steam api = Steam.get();
	if(api != null)
	    api.setparty(null, 0);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "list") {
	    Map<Long, Member> nmemb = new LinkedHashMap<>(), cmemb = party.memb;
	    for(int a = 0; a < args.length; a++) {
		long id = Utils.uiv(args[a]);
		Member m = cmemb.get(id);
		if(m == null) {
            m = party.new Member(id);
            if (party.leader != null && party.leader.gobid == ui.gui.map.plgob) {
                Map<String, ChatUI.MultiChat> chats = ui.gui.chat.getMultiChannels();
                if (chats.get("Party") != null) {
                    chats.get("Party").send(party.encodeMarkerListMessage());
                }
            }

        }
		nmemb.put(id, m);
	    }
	    party.memb = nmemb;
	    updsteam();
	    if (ui != null && ui.gui != null) {
		ui.sess.glob.oc.gobAction(Gob::updateLeaderPingArrow);
	    }
	} else if(msg == "ldr") {
	    party.leader = party.memb.get(Utils.uiv(args[0]));
        if (ui != null && ui.gui != null) {
            ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
            ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
        }
	} else if(msg == "m") {
	    int a = 0;
	    Member m = party.memb.get(Utils.uiv(args[a++]));
	    if(m != null) {
		Coord2d c = null;
		if((a < args.length) && (args[a] instanceof Coord))
		    c = ((Coord)args[a++]).mul(OCache.posres);
		if((a < args.length) && (args[a] instanceof Color)){
		    m.col = (Color)args[a++];
            if (m.gobid == ui.gui.plid && party.memb.size() > 1) {
                myColor = m.col;
            }
        }
		m.setc(c);
	    }
	} else if(msg == "pid") {
	    party.id = Utils.iv(args[0]);
	    updsteam();
	} else {
	    super.uimsg(msg, args);
	}
    }

    public void dispose() {
	setOwnNumber(0);
	/* XXX: Arguably, glob.party should be removed entirely, but
	 * until then, at least clear it when logging out. */
	party.memb = Collections.emptyMap();
	party.leader = null;

	if (GameUI.leaderTargetPing != -1 && ui != null && ui.gui != null) {
	    Gob target = ui.sess.glob.oc.getgob(GameUI.leaderTargetPing);
	    if (target != null) {
		target.removeLeaderPingArrow();
	    }
	    GameUI.leaderTargetPing = -1;
	}

        if (ui != null && ui.gui != null) {
            ui.sess.glob.oc.gobAction(Gob::updatePartyCircleOverlay);
            ui.sess.glob.oc.gobAction(Gob::updatePartyHighlightOverlay);
        }
	super.dispose();
    }
}
