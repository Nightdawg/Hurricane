package haven.automated;


import haven.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackAllItems implements Runnable {
    private GameUI gui;
    private final Inventory inventory;
    String stackOfSuffixForRemoval = ", stack of";

    public StackAllItems(GameUI gui, Inventory inventory) {
        this.gui = gui;
        this.inventory = inventory;
    }

    @Override
    public void run() {
        try {
            if (gui.vhand != null) {
                gui.error("Can't stack items with an occupied cursor!");
                return;
            }

            //Find a list of items of things we have at least 2 of
            Map<String, List<WItem>> itemsExisting = new HashMap<>();
            for (WItem wItem : inventory.getAllItems()) {
                String name = wItem.item.getname(); // ND: Matias was using the res name, but stuff like different types of meat use the same res path
                if (name.endsWith(stackOfSuffixForRemoval)) { // ND: remove the ", stack of" part, so stacks and unstacked items are considered the same item type
                    name = name.substring(0, name.length() - stackOfSuffixForRemoval.length());
                }
                if (!itemsExisting.containsKey(name)) {
                    itemsExisting.put(name, new ArrayList<>());
                }
                itemsExisting.get(name).add(wItem);
            }

            for (List<WItem> similarItems : itemsExisting.values()) {
                if (similarItems.size() < 2) {
                    //don't try stack anything we only have 1 of
                    continue;
                }

                //Now sort the list of similar items by stack size:
                //   multi stacking (shift+ctrl+rightclick) doesn't work if you stack onto a full stack, or
                //   if the stack in your hand is full, therefore we find the two smallest stack sizes and stack them.
                Map<WItem, Integer> stackSizes = new HashMap<>();
                for (WItem wItem : similarItems) {
                    int amount = 1;
                    for (ItemInfo info : wItem.info()) {
                        if (info instanceof GItem.Amount) {
                            amount = ((GItem.Amount)info).itemnum();
                        }
                    }
                    stackSizes.put(wItem, amount);
                }
                List<WItem> sortedByStackSize = stackSizes.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
                WItem lowestStack = sortedByStackSize.get(0);
                WItem nextLowest = sortedByStackSize.get(1);


                //now stack lowest on top of nextlowest
                lowestStack.item.wdgmsg("take", Coord.z);
                nextLowest.item.wdgmsg("itemact", 3);
                Thread.sleep(10);
            }
            Integer ping = GameUI.getPingValue();
            Thread.sleep(ping != null ? ping : 50);
            if (gui.vhand != null) { //if gui hand is not null, then we have to place it back into inventory
                int xsize = gui.vhand.sz.x / Inventory.sqsz.x;
                int ysize = gui.vhand.sz.y / Inventory.sqsz.y;
                inventory.wdgmsg("drop", inventory.isRoom(xsize, ysize));
            }
        } catch (InterruptedException e) {
            gui.error("Stack items script interrupted");
        }
    }
}
