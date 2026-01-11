package haven.automated;

import haven.*;
import haven.resutil.WaterTile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static haven.OCache.posres;

public class RefillWaterContainers implements Runnable {
    private static final Coord2d posres = Coord2d.of(0x1.0p-10, 0x1.0p-10).mul(11, 11);
    private GameUI gui;

    public RefillWaterContainers(GameUI gui) {
        this.gui = gui;
    }

    double maxDistanceToBarrel = 1 * 11;

    @Override
    public void run() {
        try {
            do {
                Gob player = gui.map.player();
                if (player == null)
                    return;
                Coord2d plc = player.rc;
                MCache mcache = gui.ui.sess.glob.map;
                int t = mcache.gettile(plc.floor(MCache.tilesz));
                Tiler tl = mcache.tiler(t);
                if (tl instanceof WaterTile) {
                    Resource res = mcache.tilesetr(t);
                    if (res != null) {
                        if (res.name.equals("gfx/tiles/water") || res.name.equals("gfx/tiles/deep")) {
                            refillContainers(plc, null);
                        } else if (res.name.equals("gfx/tiles/owater") || res.name.equals("gfx/tiles/odeep") || res.name.equals("gfx/tiles/odeeper")){
                            gui.ui.error("Refill Water Script: This is salt water, you can't drink this!");
                            return;
                        }
                    } else {
                        gui.ui.error("Refill Water Script: Error checking tile, try again!");
                        return;
                    }
                } else { // ND: We're not sitting on a water tile, so let's look for a barrel instead
                    Gob barrelGob = null;
                    for (Gob gob : Utils.getAllGobs(gui)) {
                        double distFromPlayer = gob.rc.dist(plc);
                        if (gob.id == gui.map.plgob || distFromPlayer >= maxDistanceToBarrel)
                            continue;
                        Resource res = null;
                        try {
                            res = gob.getres();
                        } catch (Loading l) {
                        }
                        if (res != null) {
                            if (res.name.startsWith("gfx/terobjs/barrel")) {
                                if (distFromPlayer < maxDistanceToBarrel && (barrelGob == null || distFromPlayer < barrelGob.rc.dist(plc))) {
                                    Optional<String> contents = Optional.empty();
                                    contents = gob.ols.stream()
                                            .map(Gob.Overlay::getSprResName)
                                            .filter(name -> name.startsWith("gfx/terobjs/barrel-"))
                                            .map(name -> name.substring(name.lastIndexOf("-") + 1))
                                            .findAny();
                                    if(contents.isPresent()) {
                                        String text = contents.get();
                                        if (!text.isEmpty() && text.equals("water")) {
                                            barrelGob = gob;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (barrelGob == null) {
                        gui.ui.error("Refill Water Script: You must be on a water tile or next to a water barrel, in order to refill your containers!");
                        return;
                    } else {
                        refillContainers(null, barrelGob);
                    }
                }
            } while (getInventoryContainers().size() != 0 || getBeltContainers().size() != 0 || getEquiporyPouchContainers().size() != 0);
            gui.ui.msg("Water Refilled!");
        } catch (Exception e) {
//            gui.ui.error("Refill Water Containers Script: An Unknown Error has occured.");
        }
    }


    public Map<WItem, Coord> getBeltContainers() {
        Map<WItem, Coord> containers = new HashMap<>();
        Coord sqsz = Inventory.sqsz;
        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (!(w instanceof GItem.ContentsWindow) || !((GItem.ContentsWindow) w).myOwnEquipory) continue;
            for (Widget ww : w.children()) {
                if (!(ww instanceof Inventory)) continue;
                Coord inventorySize = ((Inventory) ww).isz;
                for (int i = 0; i < inventorySize.x; i++) {
                    for (int j = 0; j < inventorySize.y; j++) {
                        Coord indexCoord = new Coord(i, j);
                        Coord calculatedCoord = indexCoord.mul(sqsz).add(1, 1);
                        for (Map.Entry<GItem, WItem> entry : ((Inventory) ww).wmap.entrySet()) {
                            if (entry.getValue().c.equals(calculatedCoord)) {
                                String resName = entry.getKey().res.get().name;
                                ItemInfo.Contents.Content content = getContent(entry.getKey());
                                if (resName.equals("gfx/invobjs/small/waterskin") && shouldAddToContainers(content, 3.0F)) {
                                    containers.put(entry.getValue(), indexCoord);
                                } else if (resName.equals("gfx/invobjs/waterflask") && shouldAddToContainers(content, 2.0F)) {
                                    containers.put(entry.getValue(), indexCoord);
                                } else if (resName.equals("gfx/invobjs/small/glassjug") && shouldAddToContainers(content, 5.0F)) {
                                    containers.put(entry.getValue(), indexCoord);
                                }
                            }
                        }
                    }
                }
            }
        }
        return containers;
    }

    public Inventory returnBelt() {
        Inventory belt = null;
        for (Widget w = gui.lchild; w != null; w = w.prev) {
            if (!(w instanceof GItem.ContentsWindow) || !((GItem.ContentsWindow) w).myOwnEquipory) continue;
            if (!((GItem.ContentsWindow) w).cap.contains("Belt")) continue;
            for (Widget ww : w.children()) {
                if (!(ww instanceof Inventory)) continue;
                belt = (Inventory) ww;
            }
        }
        return belt;
    }

    public Map<WItem, Coord> getInventoryContainers() {
        Inventory playerInventory = gui.maininv;
        Coord inventorySize = playerInventory.isz;
        Coord sqsz = Inventory.sqsz;
        Map<WItem, Coord> containers = new HashMap<>();
        for (int i = 0; i < inventorySize.x; i++) {
            for (int j = 0; j < inventorySize.y; j++) {
                Coord indexCoord = new Coord(i, j);
                Coord calculatedCoord = indexCoord.mul(sqsz).add(1, 1);

                for (Map.Entry<GItem, WItem> entry : playerInventory.wmap.entrySet()) {
                    if (entry.getValue().c.equals(calculatedCoord)) {
                        String resName = entry.getKey().res.get().name;
                        ItemInfo.Contents.Content content = getContent(entry.getKey());
                        if (resName.equals("gfx/invobjs/waterskin") && shouldAddToContainers(content, 3.0F)) {
                            containers.put(entry.getValue(), indexCoord);
                        } else if (resName.equals("gfx/invobjs/waterflask") && shouldAddToContainers(content, 2.0F)) {
                            containers.put(entry.getValue(), indexCoord);
                        } else if (resName.equals("gfx/invobjs/glassjug") && shouldAddToContainers(content, 5.0F)) {
                            containers.put(entry.getValue(), indexCoord);
                        }
                    }
                }
            }
        }
        return containers;
    }

    public Map<WItem, Integer> getEquiporyPouchContainers() {
        WItem leftPouch = gui.getequipory().slots[19];
        WItem rightPouch = gui.getequipory().slots[20];
        Map<WItem, Integer> containers = new HashMap<>();
        if (leftPouch != null) {
            String resName = leftPouch.item.res.get().name;
            ItemInfo.Contents.Content content = getContent(leftPouch.item);
            if ((resName.equals("gfx/invobjs/small/waterskin") && shouldAddToContainers(content, 3.0F))
                    || (resName.equals("gfx/invobjs/waterflask") && shouldAddToContainers(content, 2.0F))
                    || (resName.equals("gfx/invobjs/small/glassjug") && shouldAddToContainers(content, 5.0F))) {
                containers.put(leftPouch, 19);
            }
        }
        if (rightPouch != null) {
            String resName = rightPouch.item.res.get().name;
            ItemInfo.Contents.Content content = getContent(rightPouch.item);
            if ((resName.equals("gfx/invobjs/small/waterskin") && shouldAddToContainers(content, 3.0F))
                    || (resName.equals("gfx/invobjs/waterflask") && shouldAddToContainers(content, 2.0F))
                    || (resName.equals("gfx/invobjs/small/glassjug") && shouldAddToContainers(content, 5.0F))) {
                containers.put(rightPouch, 20);
            }
        }
        return containers;
    }

    private ItemInfo.Contents.Content getContent(GItem item) {
        ItemInfo.Contents.Content content = null;
        try {
            for (ItemInfo info : item.info()) {
                if (info instanceof ItemInfo.Contents) {
                    content = ((ItemInfo.Contents) info).content;
                }
            }
        } catch (Loading ignored) {

        }
        return content;
    }

    private boolean shouldAddToContainers(ItemInfo.Contents.Content content, float contentCount) {
        return content == null || (content.count != contentCount && Objects.equals(content.name, "Water"));
    }

    private void refillContainers(Coord2d lc, Gob gob){
        Inventory belt = returnBelt();
        Equipory equipory = gui.getequipory();
        Map<WItem, Coord> inventoryItems = getInventoryContainers();
        for (Map.Entry<WItem, Coord> item : inventoryItems.entrySet()) {
            try {
                item.getKey().item.wdgmsg("take", Coord.z);
                Thread.sleep(5);
                if (gob == null)
                    gui.map.wdgmsg("itemact", Coord.z, lc.floor(posres), 0);
                else
                    gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), 4, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                Thread.sleep(30);
                gui.maininv.wdgmsg("drop", item.getValue());
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
                return;
            }
        }
        Map<WItem, Coord> beltItems = getBeltContainers();
        for (Map.Entry<WItem, Coord> item : beltItems.entrySet()) {
            try {
                item.getKey().item.wdgmsg("take", Coord.z);
                Thread.sleep(5);
                if (gob == null)
                    gui.map.wdgmsg("itemact", Coord.z, lc.floor(posres), 0);
                else
                    gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), 4, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                Thread.sleep(40);
                belt.wdgmsg("drop", item.getValue());
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
                return;
            }
        }
        Map<WItem, Integer> equiporyPouchItems = getEquiporyPouchContainers();
        for (Map.Entry<WItem, Integer> item : equiporyPouchItems.entrySet()) {
            try {
                item.getKey().item.wdgmsg("take", Coord.z);
                Thread.sleep(5);
                if (gob == null)
                    gui.map.wdgmsg("itemact", Coord.z, lc.floor(posres), 0);
                else
                    gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), 4, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                Thread.sleep(40);
                equipory.wdgmsg("drop", item.getValue());
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
                return;
            }
        }
    }
}
