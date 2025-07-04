package haven.automated;

import haven.*;
import haven.Window;
import java.awt.*;
import java.util.*;

public class FillInventoryWithSeeds implements Runnable {
    private GameUI gui;
	WBotUtils wBotUtils;

    public FillInventoryWithSeeds(GameUI gui) {
        this.gui=gui;
		this.wBotUtils = new WBotUtils(gui);
    }

	private int numberOfFreeSlots;
	Gob closestBarrel;

	@Override
    public void run() {
		System.out.println("Fill inventory with seeds");
		gui.msg("Fill inventory with seeds", Color.WHITE);
		
		closestBarrel = wBotUtils.findClosestGob("gfx/terobjs/barrel", 220);
		
		if(closestBarrel==null){
			System.out.println("No barrels around!");
			gui.msg("No barrels around!", Color.WHITE, UI.ErrorMessage.sfx);
			return;
		}
		
		//numberOfFreeSlots = wBotUtils.getPlayerInventory(gui).getFreeSpace();
		//AUtils.rightClickGob(gui, closestBarrel, 0);
		//Window barrelWindow = wBotUtils.waitForWindow("Barrel", 2000, gui.fillInventoryWithSeedsThread);
		//wBotUtils.takeItemFromBarrel(barrelWindow, numberOfFreeSlots);
		AUtils.rightClickShiftCtrl(gui, closestBarrel);
		
		System.out.println("Done!");
		gui.msg("Done!", Color.WHITE, UI.InfoMessage.sfx);
    }
}
