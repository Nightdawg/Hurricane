package haven.automated;

import haven.*;
import haven.resutil.WaterTile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RefillWaterContainers implements Runnable {
    private static final Coord2d posres = Coord2d.of(0x1.0p-10, 0x1.0p-10).mul(11, 11);
    public static final int TIMEOUT_MS = 3000;
    private GameUI gui;

    public RefillWaterContainers(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        try {
            MCache mcache = gui.ui.sess.glob.map;
            int t = mcache.gettile(gui.map.player().rc.floor(MCache.tilesz));
            Tiler tl = mcache.tiler(t);
            if (tl instanceof WaterTile) {
                Resource res = mcache.tilesetr(t);
                if (res != null) {
                    if (res.name.equals("gfx/tiles/water") || res.name.equals("gfx/tiles/deep")) {
                        try {
                            List<ItemToFill> items = gatherItemsToFill();
                            for (ItemToFill item : items) {
                                if (!fillFromTile(item.wItem, item.inventoryLocation, item.inventory)) {
                                    return;
                                }
                            }
                        } catch (InterruptedException ignored) {
                            return;
                        }
                    } else if (res.name.equals("gfx/tiles/owater") || res.name.equals("gfx/tiles/odeep") || res.name.equals("gfx/tiles/odeeper")){
                        gui.ui.error("Refill Water Script: This is salt water, you can't drink this!");
                        return;
                    }
                } else {
                    gui.ui.error("Refill Water Script: Error checking tile, try again!");
                    return;
                }
            } else {
                ArrayList<Gob> waterBarrels = new ArrayList<>();
                for (Gob barrel : AUtils.getGobs("gfx/terobjs/barrel", gui)) {
                    if (isBarrelFilledWithWater(barrel)) {
                        waterBarrels.add(barrel);
                    }
                }

                waterBarrels.sort(Comparator.comparing(g -> g.rc.dist(gui.map.player().rc)));

                Iterator<Gob> barrelIterator = waterBarrels.iterator();
                if (!waterBarrels.isEmpty()) {
                    Gob barrel = barrelIterator.next();
                    try {
                        List<ItemToFill> items = gatherItemsToFill();
                        for (ItemToFill item : items) {
                            if (!isBarrelFilledWithWater(barrel)) {
                                if (barrelIterator.hasNext()) {
                                    barrel = barrelIterator.next();
                                } else {
                                    gui.ui.error("Refill Water Script: No more nearby water barrels!");
                                    return;
                                }
                            }

                            if (!fillFromBarrel(item.wItem, item.inventoryLocation, item.inventory, barrel)) {
                                return;
                            }
                        }
                    } catch (InterruptedException ignored) {
                        return;
                    }
                } else {
                    gui.ui.error("Refill Water Script: You must be on a water tile or near a water barrel, in order to refill your containers!");
                    return;
                }
            }
            gui.ui.msg("Water Refilled!");
        } catch (Exception e) {
            //gui.ui.error("Refill Water Containers Script: An Unknown Error has occured.");
        }
    }

    private static boolean isBarrelFilledWithWater(Gob barrel) {
        for (Gob.Overlay ol : barrel.ols) {
             if (ol.spr != null && ol.spr.res != null && Objects.equals("gfx/terobjs/barrel-water", ol.spr.res.name)) {
                return true;
            }
        }
        return false;
    }

    private boolean fillFromTile(WItem item, Object dropLocation, Widget inventory) throws InterruptedException {
        item.item.wdgmsg("take", Coord.z);
        double beforeFillAmount = getContentsAmount(item);
        if (!AUtils.waitForOccupiedHand(gui, TIMEOUT_MS, "Timeout waiting for hand to take item.")) {
            return false;
        }

        gui.map.wdgmsg("itemact", Coord.z, gui.map.player().rc.floor(posres), 0);
        boolean successfullyFilled = AUtils.waitUntil(() -> getContentsAmount(gui.vhand) > beforeFillAmount, TIMEOUT_MS);
        if (!successfullyFilled) {
            gui.error("Timeout waiting to fill item.");
        }

        inventory.wdgmsg("drop", dropLocation);
        boolean successfullyDropped = AUtils.waitForEmptyHand(gui, TIMEOUT_MS, "Timeout waiting for hand to empty");

        return successfullyFilled && successfullyDropped;
    }

    private boolean fillFromBarrel(WItem item, Object dropLocation, Widget inventory, Gob barrel) throws InterruptedException {
        if (getContentsAmount(item) > 0) {
            return true;
        }
        item.item.wdgmsg("take", Coord.z);
        if (!AUtils.waitForOccupiedHand(gui, TIMEOUT_MS, "Timeout waiting for hand to take item.")) {
            return false;
        }

        boolean nearBarrel = gui.map.player().rc.dist(barrel.rc) <= 11;
        if (!nearBarrel) {
            gui.map.pfRightClick(barrel, -1, 1, 0, "");
            AUtils.waitPf(gui);
            nearBarrel = gui.map.player().rc.dist(barrel.rc) <= 11;
            if (!nearBarrel) {
                gui.error("Moving to next nearest water barrel failed");
            }
        }

        boolean successfullyFilled = false;
        if (nearBarrel) {
            gui.map.wdgmsg("itemact", Coord.z, barrel.rc.floor(posres), 0, 0, (int) barrel.id, barrel.rc.floor(posres), 0, -1);
            successfullyFilled = AUtils.waitUntil(() -> getContentsAmount(gui.vhand) > 0, TIMEOUT_MS);
            if (!successfullyFilled) {
                gui.error("Timeout waiting to fill item.");
            }
        }

        inventory.wdgmsg("drop", dropLocation);
        boolean successfullyDropped = AUtils.waitForEmptyHand(gui, TIMEOUT_MS, "Timeout waiting for hand to empty");

        return successfullyFilled && successfullyDropped;
    }

    private double getContentsAmount(WItem item) {
        List<ItemInfo> infos;
        try {
            infos = item.item.info();
        } catch (Loading notReady) {
            return 0;
        }

        for (ItemInfo itemInfo : infos) {
            if (itemInfo instanceof ItemInfo.Contents) {
                for (ItemInfo subInfo : ((ItemInfo.Contents) itemInfo).sub) {
                    if (subInfo instanceof ItemInfo.Name) {
                        String text = ((ItemInfo.Name) subInfo).str.text;
                        int firstWordEnd = text.indexOf(" ");
                        if (firstWordEnd < 0) {
                            continue;
                        }
                        try {
                            return Double.parseDouble(text.substring(0, firstWordEnd));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        return 0;
    }

    private List<ItemToFill> gatherItemsToFill() {
        Inventory belt = returnBelt();
        Equipory equipory = gui.getequipory();
        Map<WItem, Coord> inventoryItems = getInventoryContainers();
        ArrayList<ItemToFill> result = new ArrayList<>();

        for (Map.Entry<WItem, Coord> item : inventoryItems.entrySet()) {
            result.add(new ItemToFill(item.getKey(), gui.maininv, item.getValue()));
        }
        Map<WItem, Coord> beltItems = getBeltContainers();
        for (Map.Entry<WItem, Coord> item : beltItems.entrySet()) {
            result.add(new ItemToFill(item.getKey(), belt, item.getValue()));
        }
        Map<WItem, Integer> equiporyPouchItems = getEquiporyPouchContainers();
        for (Map.Entry<WItem, Integer> item : equiporyPouchItems.entrySet()) {
            result.add(new ItemToFill(item.getKey(), equipory, item.getValue()));
        }
        return result;
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
        for (ItemInfo info : item.info()) {
            if (info instanceof ItemInfo.Contents) {
                content = ((ItemInfo.Contents) info).content;
            }
        }
        return content;
    }

    private boolean shouldAddToContainers(ItemInfo.Contents.Content content, float contentCount) {
        return content == null || (content.count != contentCount && Objects.equals(content.name, "Water"));
    }
}

class ItemToFill {
    public final WItem wItem;
    public final Widget inventory;
    public final Object inventoryLocation;

    public ItemToFill(WItem wItem, Widget inventory, Object inventoryLocation) {
        this.wItem = wItem;
        this.inventory = inventory;
        this.inventoryLocation = inventoryLocation;
    }

}