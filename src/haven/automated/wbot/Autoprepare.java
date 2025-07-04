package haven.automated;

import haven.*;
import haven.Window;
import java.awt.*;

public class Autoprepare implements Runnable {
	private GameUI gui;
	WBotUtils wBotUtils;
	
	public Autoprepare(GameUI gui) {
		this.gui=gui;
		this.wBotUtils = new WBotUtils(gui);
	}
	
	private Window containerWindow;
	private Inventory containerInventory;
	
    @Override
    public void run() {
		System.out.println("Autoprepare");
		gui.msg("Autoprepare", Color.WHITE);
		
		for (WItem wItem : gui.maininv.getAllItems()) {
			if(wItem.item.resource().name.contains("dead")
			|| wItem.item.resource().name.contains("plucked")
			|| wItem.item.resource().name.contains("clean")
			|| wItem.item.resource().name.contains("carcass")
			|| wItem.item.resource().name.contains("wblock")
			|| wItem.item.resource().name.contains("invobjs/hen")
			|| wItem.item.resource().name.contains("invobjs/rabbit")
			|| wItem.item.resource().name.contains("rooster")
			|| wItem.item.resource().name.contains("cheesetray-")){
				wItem.item.wdgmsg("iact", Coord.z, gui.ui.modflags());
				wBotUtils.sleep(gui.autoprepareThread, 300);
			}
		}
		
		System.out.println("Done!");
		gui.msg("Done!", Color.WHITE, UI.InfoMessage.sfx);
    }
}