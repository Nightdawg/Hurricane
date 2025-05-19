package haven.automated;

import haven.*;
import java.awt.*;
import java.util.*;

public class FillTrays implements Runnable {
    private GameUI gui;
	WBotUtils wBotUtils;

    public FillTrays(GameUI gui) {
        this.gui=gui;
		this.wBotUtils = new WBotUtils(gui);
    }

	Gob closestGob;
	
	@Override
    public void run() {
		wBotUtils.sysMsg("Fill those trays, please.");
		java.util.List<Inventory> inventories;
		for(Inventory i : gui.getAllInventories()){
			for (WItem curd : i.getAllItems()){
				System.out.println("-"+curd.item.resource().name);
				if(!curd.item.resource().name.contains("gfx/invobjs/curd")){
					continue;
				}
				
				inventories = gui.getAllInventories();
				loop:
				for(Inventory i2 : inventories){
					for (WItem tray : i2.getAllItems()){
						if(tray.item.resource().name.equals("gfx/invobjs/cheesetray")){
							wBotUtils.takeItem(curd, true, gui.fillTraysThread);
							wBotUtils.interactWithItemUsingItemAtHand(tray, true, gui.fillTraysThread);
							break loop;
						}
					}
				}
			}
		}
    }
}
