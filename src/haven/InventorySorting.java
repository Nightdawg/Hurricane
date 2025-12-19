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

import java.util.*;

/**
 * Helper class to manage inventory sorting functionality.
 */
public class InventorySorting {
    private final Inventory inventory;

    // Windows where sorting UI should NOT be shown
    private static final Set<String> SORTING_EXCLUDED_WINDOWS = new HashSet<>(Arrays.asList(
            "Character Sheet",
            "Study",
            "Chicken Coop",
            "Belt",
            "Pouch",
            "Purse",
            "Cauldron",
            "Finery Forge",
            "Fireplace",
            "Frame",
            "Herbalist Table",
            "Kiln",
            "Ore Smelter",
            "Smith's Smelter",
            "Oven",
            "Pane mold",
            "Rack",
            "Smoke shed",
            "Stack Furnace",
            "Steelbox",
            "Tub"
    ));

    // Combined comparator: sorts by name/resname first, then by quality
    public static final Comparator<WItem> COMBINED_COMPARATOR_ASC = new Comparator<WItem>() {
        @Override
        public int compare(WItem o1, WItem o2) {
            // First compare by name
            String name1 = o1.item.getname() != null ? o1.item.getname() : "";
            String name2 = o2.item.getname() != null ? o2.item.getname() : "";
            int nameCompare = name1.compareToIgnoreCase(name2);
            if (nameCompare != 0) {
                return nameCompare;
            }

            // If names are equal, compare by resource name
            String resname1 = "";
            String resname2 = "";
            try {
                if (o1.item.res != null && o1.item.res.get() != null) {
                    resname1 = o1.item.res.get().name;
                }
            } catch (Loading e) {
            }
            try {
                if (o2.item.res != null && o2.item.res.get() != null) {
                    resname2 = o2.item.res.get().name;
                }
            } catch (Loading e) {
            }
            int resnameCompare = resname1.compareToIgnoreCase(resname2);
            if (resnameCompare != 0) {
                return resnameCompare;
            }

            // If names and resnames are equal, compare by quality
            double q1 = o1.item.getQBuff() != null ? o1.item.getQBuff().q : 0;
            double q2 = o2.item.getQBuff() != null ? o2.item.getQBuff().q : 0;
            return Double.compare(q1, q2);
        }
    };
    public static final Comparator<WItem> COMBINED_COMPARATOR_DESC = new Comparator<WItem>() {
        @Override
        public int compare(WItem o1, WItem o2) {
            return COMBINED_COMPARATOR_ASC.compare(o2, o1);
        }
    };

    // Quality comparator (for internal use in getItemsByName)
    public static final Comparator<WItem> QUALITY_COMPARATOR_ASC = new Comparator<WItem>() {
        @Override
        public int compare(WItem o1, WItem o2) {
            double q1 = o1.item.getQBuff() != null ? o1.item.getQBuff().q : 0;
            double q2 = o2.item.getQBuff() != null ? o2.item.getQBuff().q : 0;
            return Double.compare(q1, q2);
        }
    };
    public static final Comparator<WItem> QUALITY_COMPARATOR_DESC = new Comparator<WItem>() {
        @Override
        public int compare(WItem o1, WItem o2) {
            return QUALITY_COMPARATOR_ASC.compare(o2, o1);
        }
    };

    // Helper class to store item data for sorting
    private static class ItemData {
        WItem item;
        Coord currentPos;
        Coord targetPos;
        Coord size;
        List<Coord> validTargets; // Pre-calculated valid positions (P&C)

        ItemData(WItem item, Coord currentPos, Coord size) {
            this.item = item;
            this.currentPos = currentPos;
            this.size = size;
            this.targetPos = null;
            this.validTargets = new ArrayList<>();
        }
    }

    // UI state
    private boolean sortUIAdded = false;
    private Button sortAscButton = null;
    private Button sortDescButton = null;

    public InventorySorting(Inventory inventory) {
        this.inventory = inventory;
    }

    public boolean isSortUIAdded() {
        return sortUIAdded;
    }

    /**
     * Called when inventory is added to UI
     */
    public void onAdded() {
        if (inventory.ui != null && inventory.isz != null && !sortUIAdded) {
            Window parentWindow = inventory.getparent(Window.class);
            if (parentWindow != null) {
                addSortUI();
            }
        }
    }

    /**
     * Called during draw (fallback)
     */
    public void onDraw() {
        if (inventory.ui != null && inventory.isz != null && !sortUIAdded) {
            Window parentWindow = inventory.getparent(Window.class);
            if (parentWindow != null) {
                addSortUI();
            }
        }
    }

    /**
     * Called when inventory size is set via uimsg
     */
    public void onSizeSet() {
        if (!sortUIAdded) {
            if (inventory.ui != null) {
                inventory.ui.sess.glob.loader.defer(() -> {
                    if (!sortUIAdded && inventory.isz != null) {
                        Window parentWindow = inventory.getparent(Window.class);
                        if (parentWindow != null) {
                            addSortUI();
                        }
                    }
                }, null);
            }
        } else {
            updateSortUIPositions();
            inventory.pack();
            Window parentWindow = inventory.getparent(Window.class);
            if (parentWindow != null) {
                parentWindow.pack();
            }
        }
    }

    /**
     * Called when inventory is resized
     */
    public void onResize() {
        if (sortUIAdded) {
            updateSortUIPositions();
        }
    }

    /**
     * Add sort UI to inventory
     */
    void addSortUI() {
        if (sortUIAdded || inventory.isz == null || inventory.ui == null)
            return;

        if (OptWnd.enableInventorySortingCheckBox != null && !OptWnd.enableInventorySortingCheckBox.a) {
            return;
        }
        if (OptWnd.enableInventorySortingCheckBox == null && !Utils.getprefb("enableInventorySorting", false)) {
            return;
        }
        Window parentWindow = inventory.getparent(Window.class);
        if (parentWindow == null)
            return;

        if (SORTING_EXCLUDED_WINDOWS.contains(parentWindow.cap))
            return;

        int invWidthPx = inventory.isz.x * Inventory.sqsz.x + 1;
        int btnWidth = UI.scale(24);
        int btnHeight = UI.scale(20);
        int spacing = UI.scale(3);
        int buttonX = invWidthPx + UI.scale(2);
        int buttonY = UI.scale(5);

        sortAscButton = inventory.add(new Button(btnWidth, "↑").action(() -> {
            sortInventory(COMBINED_COMPARATOR_ASC);
        }), buttonX, buttonY);

        buttonY += btnHeight + spacing;
        sortDescButton = inventory.add(new Button(btnWidth, "↓").action(() -> {
            sortInventory(COMBINED_COMPARATOR_DESC);
        }), buttonX, buttonY);

        inventory.pack();
        parentWindow.pack();

        sortUIAdded = true;
    }

    /**
     * Remove sort UI when sorting is disabled
     */
    void removeSortUI() {
        if (!sortUIAdded)
            return;

        if (sortAscButton != null) {
            sortAscButton.destroy();
            sortAscButton = null;
        }
        if (sortDescButton != null) {
            sortDescButton.destroy();
            sortDescButton = null;
        }

        sortUIAdded = false;
        inventory.pack();
        Window parentWindow = inventory.getparent(Window.class);
        if (parentWindow != null) {
            parentWindow.pack();
        }
    }

    private void updateSortUIPositions() {
        if (sortAscButton == null || sortDescButton == null || inventory.isz == null)
            return;

        int invWidthPx = inventory.isz.x * Inventory.sqsz.x + 1;
        int btnHeight = UI.scale(20);
        int spacing = UI.scale(3);
        int buttonX = invWidthPx + UI.scale(5);
        int buttonY = UI.scale(5);

        sortAscButton.c = new Coord(buttonX, buttonY);
        buttonY += btnHeight + spacing;
        sortDescButton.c = new Coord(buttonX, buttonY);
    }

    private boolean isAreaFree(boolean[][] grid, Coord pos, Coord size, int invWidth, int invHeight) {
        if (pos.x + size.x > invWidth || pos.y + size.y > invHeight)
            return false;
        if (pos.x < 0 || pos.y < 0)
            return false;

        for (int x = 0; x < size.x; x++) {
            for (int y = 0; y < size.y; y++) {
                if (grid[pos.x + x][pos.y + y])
                    return false;
            }
        }
        return true;
    }

    private boolean rectanglesOverlap(Coord pos1, Coord size1, Coord pos2, Coord size2) {
        return !(pos1.x + size1.x <= pos2.x ||
                pos2.x + size2.x <= pos1.x ||
                pos1.y + size1.y <= pos2.y ||
                pos2.y + size2.y <= pos1.y);
    }

    private boolean canDisplaceSingleItem(boolean[][] grid, Coord pos, Coord size,
                                          List<ItemData> allItems, int invWidth, int invHeight) {
        if (isAreaFree(grid, pos, size, invWidth, invHeight))
            return true;

        int overlappingItems = 0;
        ItemData overlappingItem = null;

        for (ItemData item : allItems) {
            if (rectanglesOverlap(pos, size, item.currentPos, item.size)) {
                overlappingItems++;
                overlappingItem = item;
                if (overlappingItems > 1)
                    return false;
            }
        }

        if (overlappingItems == 1) {
            int droppedArea = size.x * size.y;
            int freedArea = overlappingItem.size.x * overlappingItem.size.y;
            return droppedArea <= freedArea;
        }

        return false;
    }

    private List<Coord> generateValidPositions(ItemData item, boolean[][] grid,
                                               List<ItemData> allItems, int invWidth, int invHeight) {
        List<Coord> validPositions = new ArrayList<>();

        for (int y = 0; y < invHeight; y++) {
            for (int x = 0; x < invWidth; x++) {
                Coord pos = new Coord(x, y);
                if (canDisplaceSingleItem(grid, pos, item.size, allItems, invWidth, invHeight)) {
                    validPositions.add(pos);
                }
            }
        }

        return validPositions;
    }

    private boolean isReserved(boolean[][] reserved, Coord pos, Coord size, int invWidth, int invHeight) {
        if (pos.x + size.x > invWidth || pos.y + size.y > invHeight)
            return true;
        if (pos.x < 0 || pos.y < 0)
            return true;

        for (int x = 0; x < size.x; x++) {
            for (int y = 0; y < size.y; y++) {
                if (reserved[pos.x + x][pos.y + y])
                    return true;
            }
        }
        return false;
    }

    private void markReserved(boolean[][] reserved, Coord pos, Coord size, int invWidth, int invHeight) {
        for (int x = 0; x < size.x && pos.x + x < invWidth; x++) {
            for (int y = 0; y < size.y && pos.y + y < invHeight; y++) {
                if (pos.x + x >= 0 && pos.y + y >= 0) {
                    reserved[pos.x + x][pos.y + y] = true;
                }
            }
        }
    }

    private Coord findFirstValidPosition(ItemData item, boolean[][] reserved, int invWidth, int invHeight) {
        for (Coord pos : item.validTargets) {
            if (!isReserved(reserved, pos, item.size, invWidth, invHeight)) {
                return pos;
            }
        }
        return null;
    }

    private boolean isTargetStillValid(ItemData item,
                                       boolean[][] currentGrid, List<ItemData> allItems,
                                       int invWidth, int invHeight) {
        if (item.targetPos == null)
            return false;
        return canDisplaceSingleItem(currentGrid, item.targetPos, item.size, allItems, invWidth, invHeight);
    }

    private ItemData findItemAtPosition(Map<WItem, Coord> currentState, Coord pos, Coord size,
                                        List<ItemData> allItems) {
        for (ItemData item : allItems) {
            Coord itemPos = currentState.get(item.item);
            if (itemPos != null && rectanglesOverlap(pos, size, itemPos, item.size)) {
                return item;
            }
        }
        return null;
    }

    private void updateGridState(boolean[][] grid, Coord oldPos, Coord newPos, Coord size) {
        if (oldPos != null) {
            for (int x = 0; x < size.x && oldPos.x + x < grid.length; x++) {
                for (int y = 0; y < size.y && oldPos.y + y < grid[0].length; y++) {
                    if (oldPos.x + x >= 0 && oldPos.y + y >= 0) {
                        grid[oldPos.x + x][oldPos.y + y] = false;
                    }
                }
            }
        }

        if (newPos != null) {
            for (int x = 0; x < size.x && newPos.x + x < grid.length; x++) {
                for (int y = 0; y < size.y && newPos.y + y < grid[0].length; y++) {
                    if (newPos.x + x >= 0 && newPos.y + y >= 0) {
                        grid[newPos.x + x][newPos.y + y] = true;
                    }
                }
            }
        }
    }

    private Coord findNewValidTarget(ItemData item, boolean[][] currentGrid, List<ItemData> allItems,
                                     int invWidth, int invHeight) {
        List<Coord> validTargets = generateValidPositions(item, currentGrid, allItems, invWidth, invHeight);
        return validTargets.isEmpty() ? null : validTargets.get(0);
    }

    private void executeMoveWithConstraint(ItemData item, Map<WItem, Coord> currentState,
                                           boolean[][] currentGrid, List<ItemData> allItems,
                                           int invWidth, int invHeight) {
        WItem witem = item.item;
        Coord target = item.targetPos;
        Coord size = item.size;
        Coord currentPos = currentState.get(witem);

        if (currentPos != null && currentPos.equals(target)) {
            return;
        }

        witem.item.wdgmsg("take", witem.c);
        ItemData atTarget = findItemAtPosition(currentState, target, size, allItems);

        if (atTarget != null) {
            int droppedArea = size.x * size.y;
            int freedArea = atTarget.size.x * atTarget.size.y;

            if (droppedArea > freedArea) {
                return;
            }

            inventory.wdgmsg("drop", target);
            currentState.put(witem, target);
            updateGridState(currentGrid, currentPos, target, size);

            if (atTarget.targetPos != null && !atTarget.targetPos.equals(atTarget.currentPos)) {
                currentState.put(atTarget.item, target);

                if (!isTargetStillValid(atTarget, currentGrid, allItems, invWidth, invHeight)) {
                    atTarget.targetPos = findNewValidTarget(atTarget, currentGrid, allItems, invWidth, invHeight);
                }

                if (atTarget.targetPos != null) {
                    executeMoveWithConstraint(atTarget, currentState, currentGrid, allItems, invWidth, invHeight);
                }
            }
        } else {
            inventory.wdgmsg("drop", target);
            currentState.put(witem, target);
            updateGridState(currentGrid, currentPos, target, size);
        }
    }

    private void sortInventory(Comparator<WItem> comparator) {
        GameUI gui = inventory.getparent(GameUI.class);
        if (gui != null && gui.vhand != null) {
            gui.ui.error("Need to have default cursor active to sort inventory!");
            return;
        }

        boolean[][] initialGrid = new boolean[inventory.isz.x][inventory.isz.y];
        if (inventory.sqmask != null) {
            int mo = 0;
            for (int y = 0; y < inventory.isz.y; y++) {
                for (int x = 0; x < inventory.isz.x; x++) {
                    initialGrid[x][y] = inventory.sqmask[mo++];
                }
            }
        }

        List<ItemData> allItems = new ArrayList<>();
        Map<WItem, Coord> currentState = new HashMap<>();

        for (Widget wdg = inventory.lchild; wdg != null; wdg = wdg.prev) {
            if (wdg.visible && wdg instanceof WItem) {
                WItem wItem = (WItem) wdg;
                Coord sz = wItem.sz.div(Inventory.sqsz);
                Coord loc = wItem.c.sub(1, 1).div(Inventory.sqsz);

                ItemData itemData = new ItemData(wItem, loc, sz);
                allItems.add(itemData);
                currentState.put(wItem, loc);

                for (int x = 0; x < sz.x && loc.x + x < inventory.isz.x; x++) {
                    for (int y = 0; y < sz.y && loc.y + y < inventory.isz.y; y++) {
                        if (loc.x + x >= 0 && loc.y + y >= 0) {
                            initialGrid[loc.x + x][loc.y + y] = true;
                        }
                    }
                }
            }
        }

        Collections.sort(allItems, (a, b) -> comparator.compare(a.item, b.item));

        for (ItemData item : allItems) {
            item.validTargets = generateValidPositions(item, initialGrid, allItems, inventory.isz.x, inventory.isz.y);
        }

        boolean[][] reserved = new boolean[inventory.isz.x][inventory.isz.y];
        for (ItemData item : allItems) {
            Coord target = findFirstValidPosition(item, reserved, inventory.isz.x, inventory.isz.y);
            if (target != null) {
                item.targetPos = target;
                markReserved(reserved, target, item.size, inventory.isz.x, inventory.isz.y);
            }
        }

        boolean[][] currentGrid = new boolean[inventory.isz.x][inventory.isz.y];
        for (int x = 0; x < inventory.isz.x; x++) {
            for (int y = 0; y < inventory.isz.y; y++) {
                currentGrid[x][y] = initialGrid[x][y];
            }
        }

        for (ItemData item : allItems) {
            if (item.targetPos == null)
                continue;

            if (!isTargetStillValid(item, currentGrid, allItems, inventory.isz.x, inventory.isz.y)) {
                item.targetPos = findNewValidTarget(item, currentGrid, allItems, inventory.isz.x, inventory.isz.y);
                if (item.targetPos == null) {
                    continue;
                }
            }

            executeMoveWithConstraint(item, currentState, currentGrid, allItems, inventory.isz.x, inventory.isz.y);
        }
    }
}
