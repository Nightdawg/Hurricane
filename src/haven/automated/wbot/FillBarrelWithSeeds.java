package haven.automated;

import haven.*;
import java.awt.*;
import java.util.*;

public class FillBarrelWithSeeds implements Runnable {
    private GameUI gui;
	WBotUtils wBotUtils;

    public FillBarrelWithSeeds(GameUI gui) {
        this.gui=gui;
		this.wBotUtils = new WBotUtils(gui);
    }
	
	Gob closestBarrel;

	@Override
    public void run() {
		System.out.println("Fill barrel with seeds");
		gui.msg("Fill barrel with seeds", Color.WHITE);
		
		closestBarrel = wBotUtils.findClosestGob("gfx/terobjs/barrel", 220);
		
		if(closestBarrel==null){
			System.out.println("No barrels around!");
			gui.msg("No barrels around!", Color.WHITE, UI.ErrorMessage.sfx);
			return;
		}
		
		AUtils.rightClickGob(gui, closestBarrel, 0);
		wBotUtils.waitForWindow("Barrel", 2000, gui.fillBarrelWithSeedsThread);
		
		int i = 0;
		for(WItem wItem : wBotUtils.getPlayerInventory(gui).getAllItems()) {
			if(wItem.item.resource().name.contains("seed")){
				i++;
				wBotUtils.takeItem(wItem, true, gui.fillBarrelWithSeedsThread);
				wBotUtils.interactWithGobUsingItemAtHand(closestBarrel, true, gui.fillBarrelWithSeedsThread, 3, 0);
			}
			if(i>=2){
				break;
			}
		}
		
		System.out.println("Done!");
		gui.msg("Done!", Color.WHITE, UI.InfoMessage.sfx);
    }
}
