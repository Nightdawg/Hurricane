package haven.automated;

import haven.*;
import haven.Window;
import haven.res.ui.tt.q.quality.Quality;
import java.awt.*;
import java.util.*;

public class MixSeedsByQuality implements Runnable {
    private GameUI gui;
	WBotUtils wBotUtils;
	
	public MixSeedsByQuality(GameUI gui) {
		this.gui=gui;
		this.wBotUtils = new WBotUtils(gui);
	}
	
	private String[] containerTypes = {"Chest","Cupboard","Inventory"}; 
	private Window containerWindow;
	private Inventory inventory;
	private java.util.List<WItem> itemsInTheInventory;
	
	private WItem lastItem;
	private String lastItemName;
	private double lastItemQuality;
	private WItem currentItem;
	private String currentItemName;
	private double currentItemQuality;
	
    @Override
    public void run() {
		System.out.println("Mix seeds by quality");
		gui.msg("Mix seeds by quality", Color.WHITE);
		containerWindow = wBotUtils.getContainerWindow(containerTypes);
		if(containerWindow==null){
			System.out.println("First you have to open the inventory or a container!");
			gui.msg("First you have to open the inventory or a container!", Color.WHITE, UI.ErrorMessage.sfx);
			return;
		}
		inventory = wBotUtils.getInventoryByWindow(containerWindow);
		itemsInTheInventory = inventory.getAllItems();
		mixSeeds();
		
		System.out.println("Done!");
		gui.msg("Done!", Color.WHITE, UI.InfoMessage.sfx);
    }
	
	private void mixSeeds() {
		java.util.List<WItem> itemsAlreadyMixed = new ArrayList<>();
		for (WItem wdgI : itemsInTheInventory) {// Iterate over items from inventory
			lastItem=null;
			lastItemName=null;
			lastItemQuality=0d;
			for (WItem wItem : itemsInTheInventory) {// Iterate over items from inventory
				currentItem = wItem;
				currentItemName = currentItem.item.resource().name;
				currentItemQuality = currentItem.item.info().stream().filter(info -> info instanceof Quality).map(info -> ((Quality) info).q).findFirst().orElse(0.0);
				if(currentItemName.contains("seed")){
					if(lastItem==null && wBotUtils.getAmount(currentItem)<50 && (itemsAlreadyMixed.size()==0 || !itemsAlreadyMixed.contains(currentItem))){
						lastItem = currentItem;
						lastItemName = currentItem.item.resource().name;
						lastItemQuality = currentItem.item.info().stream().filter(info -> info instanceof Quality).map(info -> ((Quality) info).q).findFirst().orElse(0.0);
						
						itemsAlreadyMixed.add(currentItem);
						continue;
					}
					else if(lastItem!=null && lastItemQuality==currentItemQuality && 
					(currentItemName.equals(lastItemName) && 
					wBotUtils.getAmount(currentItem)<50 || 
					lastItemName.contains("gfx/invobjs/coins/") && currentItemName.contains("gfx/invobjs/coins/") && 
					wBotUtils.getAmount(currentItem)<100)){
						wBotUtils.takeItem(currentItem, true, gui.mixSeedsByQualityThread);
						wBotUtils.interactWithItemUsingItemAtHand(lastItem, true, gui.mixSeedsByQualityThread);
						wBotUtils.putItOnTheInventory(inventory, gui.mixSeedsByQualityThread);//If there are seeds remaining
						if(wBotUtils.getAmount(currentItem)+wBotUtils.getAmount(lastItem)>=50d){
							break;
						}
					}
				}
			}
		}
	}
}