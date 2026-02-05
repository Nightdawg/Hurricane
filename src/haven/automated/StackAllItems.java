package haven.automated;


import haven.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static haven.Inventory.sqsz;

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
        // TODO: ND: Gotta make this crap ignore "quantity" items, like Seeds, Flour, Coins, etc.
        try {
            if (gui.vhand != null) {
                gui.error("Can't stack items with an occupied cursor!");
                return;
            }

            //Find a list of items of things we have at least 2 of
            Map<String, List<WItem>> itemsExisting = new HashMap<>();
            for (WItem wItem : inventory.getAllItems()) {
                String name = wItem.item.getname(); // ND: Matias was using the res name, but stuff like different types of meat use the same res path

                // ND: If you try to click while the script is running, you MIGHT drop the last item you have on your cursor on the floor.
                // The only valuable "double" item you can hold in your inventory is usually just a pair of rings. I can't think of anything else right now.
                if (name.contains("Ring")) continue;


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
                List<WItem> sortedByStackSize =
                        stackSizes.entrySet().stream()
                                .sorted(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toList());

                WItem lowestStack = sortedByStackSize.get(0);
                WItem nextLowest = sortedByStackSize.get(1);


                //now stack lowest on top of nextlowest
                lowestStack.item.wdgmsg("take", Coord.z);
                nextLowest.item.wdgmsg("itemact", 3);
                inventory.wdgmsg("drop", new Coord (lowestStack.c.x/sqsz.x, lowestStack.c.y/sqsz.y));
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            gui.error("Stack items script interrupted");
        }
    }
}
