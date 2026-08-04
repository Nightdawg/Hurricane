package haven.automated;

import haven.*;
import haven.resutil.WaterTile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RefillWaterContainers implements Runnable {
    private static final Coord2d posres = Coord2d.of(0x1.0p-10, 0x1.0p-10).mul(11, 11);
    private static final Map<String, Float> CONTAINER_CAPACITIES = Map.ofEntries(
            Map.entry("gfx/invobjs/waterskin", 3.0F),
            Map.entry("gfx/invobjs/small/waterskin", 3.0F),
            Map.entry("gfx/invobjs/waterflask", 2.0F),
            Map.entry("gfx/invobjs/glassjug", 5.0F),
            Map.entry("gfx/invobjs/glassjug-full", 5.0F),
            Map.entry("gfx/invobjs/small/glassjug", 5.0F),
            Map.entry("gfx/invobjs/small/glassjug-full", 5.0F),
            Map.entry("gfx/invobjs/kuksa", 0.8F),
            Map.entry("gfx/invobjs/kuksa-full", 0.8F),
            Map.entry("gfx/invobjs/woodencup", 0.6F),
            Map.entry("gfx/invobjs/woodencup-full", 0.6F),
            Map.entry("gfx/invobjs/leafcup", 0.4F),
            Map.entry("gfx/invobjs/leafcup-full", 0.4F)
    );
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
                Inventory inv = Inventory.fromWidget(ww);
                if (inv == null) continue;
                Coord inventorySize = inv.isz;
                for (int i = 0; i < inventorySize.x; i++) {
                    for (int j = 0; j < inventorySize.y; j++) {
                        Coord indexCoord = new Coord(i, j);
                        Coord calculatedCoord = indexCoord.mul(sqsz).add(1, 1);
                        for (Map.Entry<GItem, WItem> entry : inv.wmap.entrySet()) {
                            if (entry.getValue().c.equals(calculatedCoord)) {
                                String resName = entry.getKey().res.get().name;
                                ItemInfo.Contents.Content content = getContent(entry.getKey());
                                if (shouldAddToContainers(resName, content)) {
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
                Inventory inv = Inventory.fromWidget(ww);
                if (inv == null) continue;
                belt = inv;
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
                        if (shouldAddToContainers(resName, content)) {
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
            if (shouldAddToContainers(resName, content)) {
                containers.put(leftPouch, 19);
            }
        }
        if (rightPouch != null) {
            String resName = rightPouch.item.res.get().name;
            ItemInfo.Contents.Content content = getContent(rightPouch.item);
            if (shouldAddToContainers(resName, content)) {
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

    private boolean shouldAddToContainers(String resName, ItemInfo.Contents.Content content) {
        Float capacity = CONTAINER_CAPACITIES.get(resName);
        return capacity != null
                && (content == null || (content.count != capacity && Objects.equals(content.name, "Water")));
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
