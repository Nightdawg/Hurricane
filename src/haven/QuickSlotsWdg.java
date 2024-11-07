package haven;

public class QuickSlotsWdg extends Widget implements DTarget {
    private static final Tex sbg = Resource.loadtex("gfx/hud/quickslots");
    public static final Coord lefthandslotc = UI.scale(new Coord(6, 6));
    public static final Coord righthandslotc = UI.scale(new Coord(53, 6));
    public static final Coord beltslotc = UI.scale(new Coord(101, 6));
    public static final Coord backpackslotc = UI.scale(new Coord(149, 6));
    public static final Coord capeslotc = UI.scale(new Coord(197, 6));
    private static final Coord ssz = UI.scale(new Coord(44, 44));
    private UI.Grab dragging;
    private Coord dc;

    public QuickSlotsWdg() {
        super(UI.scale(new Coord(238, 47)));
    }

    @Override
    public void draw(GOut g) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            g.image(sbg, Coord.z);
            WItem left = e.slots[6];
            if (left != null) {
                left.draw(g.reclipl(lefthandslotc, Inventory.invsq.sz()));
            }
            WItem right = e.slots[7];
            if (right != null) {
                right.draw(g.reclipl(righthandslotc, Inventory.invsq.sz()));
            }
            WItem belt = e.slots[5];
            if (belt != null) {
                belt.draw(g.reclipl(beltslotc, Inventory.invsq.sz()));
            }
            WItem backpack = e.slots[11];
            if (backpack != null) {
                backpack.draw(g.reclipl(backpackslotc, Inventory.invsq.sz()));
            }
            WItem cape = e.slots[14];
            if (cape != null) {
                cape.draw(g.reclipl(capeslotc, Inventory.invsq.sz()));
            }
        }
    }


    @Override
    public boolean drop(Coord cc, Coord ul) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            if (cc.x <= UI.scale(44))  e.wdgmsg("drop", 6);
            if (cc.x > UI.scale(44) && cc.x <= UI.scale(94) ) e.wdgmsg("drop", 7);
            if (cc.x > UI.scale(94) && cc.x <= UI.scale(142) ) e.wdgmsg("drop", 5);
            if (cc.x > UI.scale(142) && cc.x <= UI.scale(190) ) e.wdgmsg("drop", 11);
            if (cc.x > UI.scale(190) && cc.x <= UI.scale(238) ) e.wdgmsg("drop", 14);
            return true;
        }
        return false;
    }

    @Override
    public boolean iteminteract(Coord cc, Coord ul) {
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = null;
            if (cc.x <= UI.scale(44)) w = e.slots[6];
            if (cc.x > UI.scale(44) && cc.x <= UI.scale(94) ) w = e.slots[7];
            if (cc.x > UI.scale(94) && cc.x <= UI.scale(142) ) w = e.slots[5];
            if (cc.x > UI.scale(142) && cc.x <= UI.scale(190) ) w = e.slots[11];
            if (cc.x > UI.scale(190) && cc.x <= UI.scale(238) ) w = e.slots[14];
            if (w != null) {
                return w.iteminteract(cc, ul);
            }
        }
        return false;
    }

    @Override
    public boolean mousedown(Coord c, int button) {
        if (super.mousedown(c, button))
            return true;
        if (ui.modmeta || ui.modctrl)
            return true;
        if ((ui.modshift && button == 1) || button == 2) {
            if((dragging != null)) { // ND: I need to do this extra check and remove it in case you do another click before the mouseup. Idk why it has to be done like this, but it solves the issue.
                dragging.remove();
                dragging = null;
            }
            dragging = ui.grabmouse(this);
            dc = c;
            return true;
        }
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = null;
            if (c.x <= UI.scale(44)) w = e.slots[6];
            if (c.x > UI.scale(44) && c.x <= UI.scale(94) ) w = e.slots[7];
            if (c.x > UI.scale(94) && c.x <= UI.scale(142) ) w = e.slots[5];
            if (c.x > UI.scale(142) && c.x <= UI.scale(190) ) w = e.slots[11];
            if (c.x > UI.scale(190) && c.x <= UI.scale(238) ) w = e.slots[14];
            if (w != null) {
                w.mousedown(new Coord(w.sz.x / 2, w.sz.y / 2), button);
                return true;
            } else if (button == 1) {
                if((dragging != null)) { // ND: Same here
                    dragging.remove();
                    dragging = null;
                }
                dragging = ui.grabmouse(this);
                dc = c;
                return true;
            }
        }
        return false;
    }

    public void simulateclick(Coord c) { // ND: Used for Quick-Switch keybinds
        Equipory e = ui.gui.getequipory();
        if (e != null) {
            WItem w = null;
            if (c.x <= UI.scale(44)) w = e.slots[6];
            if (c.x > UI.scale(44) && c.x <= UI.scale(94) ) w = e.slots[7];
            if (w != null)
                w.item.wdgmsg("take", new Coord(w.sz.x / 2, w.sz.y / 2));
        }
    }

    @Override
    public boolean mouseup(Coord c, int button) {
        checkIfOutsideOfUI(); // ND: Prevent the widget from being dragged outside the current window size
        if((dragging != null)) {
            dragging.remove();
            dragging = null;
            Utils.setprefc("wndc-quickslots", this.c);
            return true;
        }
        return super.mouseup(c, button);
    }

    @Override
    public void mousemove(Coord c) {
        if (dragging != null) {
            this.c = this.c.add(c.x, c.y).sub(dc);
            return;
        }
        super.mousemove(c);
    }

    public void checkIfOutsideOfUI() {
        if (this.c.x < 0)
            this.c.x = 0;
        if (this.c.y < 0)
            this.c.y = 0;
        if (this.c.x > (ui.gui.sz.x - this.sz.x))
            this.c.x = ui.gui.sz.x - this.sz.x;
        if (this.c.y > (ui.gui.sz.y - this.sz.y))
            this.c.y = ui.gui.sz.y - this.sz.y;
    }
}