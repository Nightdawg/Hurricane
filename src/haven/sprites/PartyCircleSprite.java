package haven.sprites;

import haven.Gob;
import haven.OptWnd;
import haven.sprites.ColoredCircleSprite;

import java.awt.*;

public class PartyCircleSprite extends ColoredCircleSprite {
    public Color partyMemberColor;
    public static Color MEMBER_OL_COLOR = OptWnd.memberPartyColorOptionWidget.currentColor;
    public static Color LEADER_OL_COLOR = OptWnd.leaderPartyColorOptionWidget.currentColor;
    public static Color YOURSELF_OL_COLOR = OptWnd.yourselfPartyColorOptionWidget.currentColor;

    public PartyCircleSprite(final Gob g, final Color col) {
        super(g, col, 4.0f, 5.25f, 0.6f);
        this.partyMemberColor = col;
    }
}