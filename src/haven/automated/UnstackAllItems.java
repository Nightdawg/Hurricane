package haven.automated;


import haven.*;
import haven.res.ui.stackinv.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnstackAllItems implements Runnable {
    private GameUI gui;
    private final Inventory inventory;

    public UnstackAllItems(GameUI gui, Inventory inventory) {
        this.gui = gui;
        this.inventory = inventory;
    }

    @Override
    public void run() {
        try {
            if (gui.vhand != null) {
                gui.error("Can't unstack items with an occupied cursor!");
                return;
            }

            for (WItem wItem : inventory.getAllItems()) {
                GItem gitem = wItem.item;
                if (gitem.getname().contains("stack of")){
                    gitem.wdgmsg("iact", Coord.z, 3);
                }
            }
        } catch (Exception e) {
            gui.error("Unstack items script failed. Try again.");
        }
    }
}
