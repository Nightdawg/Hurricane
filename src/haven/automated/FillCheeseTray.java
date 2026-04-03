package haven.automated;

import haven.*;

public class FillCheeseTray implements Runnable {
    private GameUI gui;

    public FillCheeseTray(final GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        WItem curd;
        while ((curd = AUtils.findItemByPrefixInAllInventories(gui, "gfx/invobjs/curd")) != null || this.gui.vhand != null) {
            WItem tray = null;
            for (Widget w = this.gui.lchild; w != null && tray == null; w = w.prev) {
                if (w instanceof Window) {
                    for (Widget inv = w.lchild; inv != null; inv = inv.prev) {
                        Inventory inventory = Inventory.fromWidget(inv);
                        if (inventory == null)
                            continue;
                        tray = AUtils.findItemInInv(inventory, "gfx/invobjs/cheesetray");
                        if (tray != null)
                            break;
                    }
                }
            }
            if (tray == null) {
                try {
                    Thread.sleep(100L);
                    if (this.gui.vhand != null) {
                        final Coord freeroom = this.gui.maininv.isRoom(1, 1);
                        this.gui.maininv.wdgmsg("drop", freeroom);
                        AUtils.waitForEmptyHand(this.gui, 500, "ehwat mate");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return;
            }
            try {
                if (this.gui.vhand == null) {
                    curd.item.wdgmsg("take", Coord.z);
                    if (!AUtils.waitForOccupiedHand(this.gui, 2000, "waitForOccupiedHand timed-out")) {
                        return;
                    }
                } else {
                    tray.item.wdgmsg("itemact", 1);
                    Thread.sleep(20L);
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
