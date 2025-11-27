package haven.automated;

import haven.*;

import static haven.OCache.posres;

public class AddWoodBlocksToSmokeShed implements Runnable {
    private GameUI gui;
    private Gob smokeShed;
    private int count;
    private static final int TIMEOUT = 2000;
    private static final int HAND_DELAY = 8;

    public AddWoodBlocksToSmokeShed(GameUI gui, int count) {
        this.gui = gui;
        this.count = count;
    }

    @Override
    public void run() {
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                Resource res = gob.getres();
                if (res != null && res.name.contains("smokeshed")) {
                    if (smokeShed == null)
                        smokeShed = gob;
                    else if (gob.rc.dist(gui.map.player().rc) < smokeShed.rc.dist(gui.map.player().rc))
                        smokeShed = gob;
                }
            }
        }

        if (smokeShed == null) {
            gui.error("No smoke shed found");
            return;
        }

        WItem woodblockw = null;
        for(WItem item : gui.getAllItemsFromAllInventoriesAndStacks()){
            String itemName = item.item.getname();
            if (itemName.contains("Block of") && !itemName.contains("stack")) {
                woodblockw = item;
            }
        }
        if (woodblockw == null) {
            gui.error("No wood block found in the inventory");
            return;
        }
        GItem woodblock = woodblockw.item;

        woodblock.wdgmsg("take", new Coord(woodblock.sz.x / 2, woodblock.sz.y / 2));
        int timeout = 0;
        while (gui.hand.isEmpty() || gui.vhand == null) {
            timeout += HAND_DELAY;
            if (timeout >= TIMEOUT) {
                gui.error("No wood block found in the inventory");
                return;
            }
            try {
                Thread.sleep(HAND_DELAY);
            } catch (InterruptedException e) {
                return;
            }
        }
        woodblock = gui.vhand.item;

        for (; count > 0; count--) {
            gui.map.wdgmsg("itemact", Coord.z, smokeShed.rc.floor(posres), count == 1 ? 0 : 1, 0, (int) smokeShed.id, smokeShed.rc.floor(posres), 0, -1);
            timeout = 0;
            while (true) {
                WItem newwoodblock = gui.vhand;
                if (newwoodblock != null && newwoodblock.item != woodblock) {
                    woodblock = newwoodblock.item;
                    break;
                } else if (newwoodblock == null && count == 1) {
                    return;
                }

                timeout += HAND_DELAY;
                if (timeout >= TIMEOUT) {
                    gui.error("Not enough wood blocks. Need to add " + (count - 1) + " more.");
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