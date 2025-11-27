package haven.automated;

import haven.*;

import static haven.OCache.posres;

public class AddBranchesToFurnace implements Runnable {
    private GameUI gui;
    private Gob furnace;
    private int count;
    private static final int TIMEOUT = 2000;
    private static final int HAND_DELAY = 8;

    public AddBranchesToFurnace(GameUI gui, int count) {
        this.gui = gui;
        this.count = count;
    }

    @Override
    public void run() {
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                Resource res = gob.getres();
                if (res != null && (res.basename().equals("kiln") || res.basename().equals("oven"))) {
                    if (furnace == null)
                        furnace = gob;
                    else if (gob.rc.dist(gui.map.player().rc) < furnace.rc.dist(gui.map.player().rc))
                        furnace = gob;
                }
            }
        }

        if (furnace == null) {
            gui.error("No Kiln or Oven found");
            return;
        }

        WItem branchw = null;
        for(WItem item : gui.getAllItemsFromAllInventoriesAndStacks()){
            String itemName = item.item.getname();
            if (itemName.equals("Branch")) {
                branchw = item;
            }
        }
        if (branchw == null) {
            gui.error("No branch found in the inventory");
            return;
        }
        GItem branch = branchw.item;

        branch.wdgmsg("take", new Coord(branch.sz.x / 2, branch.sz.y / 2));
        int timeout = 0;
        while (gui.hand.isEmpty() || gui.vhand == null) {
            timeout += HAND_DELAY;
            if (timeout >= TIMEOUT) {
                gui.error("No branch found in the inventory");
                return;
            }
            try {
                Thread.sleep(HAND_DELAY);
            } catch (InterruptedException e) {
                return;
            }
        }
        branch = gui.vhand.item;

        for (; count > 0; count--) {
            gui.map.wdgmsg("itemact", Coord.z, furnace.rc.floor(posres), count == 1 ? 0 : 1, 0, (int) furnace.id, furnace.rc.floor(posres), 0, -1);
            timeout = 0;
            while (true) {
                WItem newbranch = gui.vhand;
                if (newbranch != null && newbranch.item != branch) {
                    branch = newbranch.item;
                    break;
                } else if (newbranch == null && count == 1) {
                    return;
                }

                timeout += HAND_DELAY;
                if (timeout >= TIMEOUT) {
                    gui.error("Not enough branches. Need to add " + (count - 1) + " more.");
                    return;
                }
                try {
                    Thread.sleep(HAND_DELAY);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}