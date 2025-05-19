package haven.automated;

import haven.*;
import haven.Window;
import haven.res.ui.tt.q.quality.Quality;
import java.awt.*;

public class GetTheBestSeeds implements Runnable {
	private GameUI gui;
	WBotUtils wBotUtils;
	
	public GetTheBestSeeds(GameUI gui) {
		this.gui=gui;
		this.wBotUtils = new WBotUtils(gui);
	}
	
	private String[] containerTypes = {"Chest","Cupboard"}; 
	private Window containerWindow;
	private Inventory containerInventory;
	
	private double maxQTurnip = 0d;
	private double maxQKale = 0d;
	private double maxQCarrot = 0d;
	private double maxQWheat = 0d;
	private double maxQBarley = 0d;
	private double maxQMillet = 0d;
	private double maxQHemp = 0d;
	private double maxQFlax = 0d;
	private double maxQPoppy = 0d;
	private double maxQPipeweed = 0d;
	private double maxQPumpkin = 0d;
	private double maxQLettuce = 0d;
	private double maxQCucumber = 0d;
	private double maxQGrape = 0d;
	private double maxQLeek = 0d;
	private double maxQPepper = 0d;
	private double maxQStringGrass = 0d;
	private double maxQWildFlower = 0d;
	private double maxQCornGrass = 0d;

	private int numberOfFreeSlots;
	
    @Override
    public void run() {
		System.out.println("Get the best seeds");
		gui.msg("Get the best seeds", Color.WHITE);
		containerWindow = wBotUtils.getContainerWindow(containerTypes);
		if(containerWindow==null){
			System.out.println("You have to open a container first!");
			gui.msg("You have to open a container first!", Color.WHITE, UI.ErrorMessage.sfx);
			return;
		}
		numberOfFreeSlots = wBotUtils.getPlayerInventory(gui).getFreeSpace();
		containerInventory = wBotUtils.getInventoryByWindow(containerWindow);
		getMaxQualities();
		transferItems();
		System.out.println("Done!");
		gui.msg("Done!", Color.WHITE, UI.InfoMessage.sfx);
    }
	
	private void getMaxQualities() {
		for (Widget wdgI = containerInventory.child; wdgI != null; wdgI = wdgI.next) {// Iterate over items from inventory
			if (wdgI instanceof WItem) {
				final WItem wItem = (WItem) wdgI;
				final String itemName = wItem.item.resource().name;
				if(itemName.contains("seed") || itemName.contains("peppercorn")){
					double quality = wItem.item.info().stream().filter(info -> info instanceof Quality).map(info -> ((Quality) info).q).findFirst().orElse(0.0);
					if(itemName.contains("turnip") && quality>=maxQTurnip){
						maxQTurnip=quality;
					} else if(itemName.contains("carrot") && quality>=maxQCarrot){
						maxQCarrot=quality;
					} else if(itemName.contains("wheat") && quality>=maxQWheat){
						maxQWheat=quality;
					} else if(itemName.contains("barley") && quality>=maxQBarley){
						maxQBarley=quality;
					} else if(itemName.contains("millet") && quality>=maxQMillet){
						maxQMillet=quality;
					} else if(itemName.contains("hemp") && quality>=maxQHemp){
						maxQHemp=quality;
					} else if(itemName.contains("flax") && quality>=maxQFlax){
						maxQFlax=quality;
					} else if(itemName.contains("poppy") && quality>=maxQPoppy){
						maxQPoppy=quality;
					} else if(itemName.contains("pipeweed") && quality>=maxQPipeweed){
						maxQPipeweed=quality;
					} else if(itemName.contains("pumpkin") && quality>=maxQPumpkin){
						maxQPumpkin=quality;
					} else if(itemName.contains("lettuce") && quality>=maxQLettuce){
						maxQLettuce=quality;
					} else if(itemName.contains("cucumber") && quality>=maxQCucumber){
						maxQCucumber=quality;
					} else if(itemName.contains("grape") && quality>=maxQGrape){
						maxQGrape=quality;
					} else if(itemName.contains("leek") && quality>=maxQLeek){
						maxQLeek=quality;
					} else if(itemName.contains("pepper") && quality>=maxQPepper){
						maxQPepper=quality;
					} else if(itemName.contains("greenkale") && quality>=maxQKale){
						maxQKale=quality;
					} else if(itemName.contains("cereal") && quality>=maxQCornGrass){
						maxQCornGrass=quality;
					} else if(itemName.contains("flower") && quality>=maxQWildFlower){
						maxQWildFlower=quality;
					} else if(itemName.contains("fibre") && quality>=maxQStringGrass){
						maxQStringGrass=quality;
					}
				}
			}
		}
	}
	
	private void transferItems(){
		for (Widget wdgI = containerInventory.child; wdgI != null; wdgI = wdgI.next) {// Iterate over items from inventory
			if (wdgI instanceof WItem) {
				final WItem wItem = (WItem) wdgI;
				final String itemName = wItem.item.resource().name;
				final double itemQuality = wItem.item.info().stream().filter(info -> info instanceof Quality).map(info -> ((Quality) info).q).findFirst().orElse(0.0);
				if(itemName.contains("seed") || itemName.contains("peppercorn")){
					if(isMaxQualitySeed(itemName, itemQuality)){
						wItem.item.wdgmsg("transfer", Coord.z);
						numberOfFreeSlots--;
					}
				}
				
				if(numberOfFreeSlots==0d){
					break;
				}
			}
		}
	}
	
	private boolean isMaxQualitySeed(String itemName, double itemQuality) {
		if(itemName.contains("turnip") && itemQuality==maxQTurnip){
			maxQTurnip=0d;
			return true;
		}
		if(itemName.contains("carrot") && itemQuality==maxQCarrot){
			maxQCarrot=0d;
			return true;
		}
		if(itemName.contains("wheat") && itemQuality==maxQWheat){
			maxQWheat=0d;
			return true;
		}
		if(itemName.contains("barley") && itemQuality==maxQBarley){
			maxQBarley=0d;
			return true;
		}
		if(itemName.contains("millet") && itemQuality==maxQMillet){
			maxQMillet=0d;
			return true;
		}
		if(itemName.contains("hemp") && itemQuality==maxQHemp){
			maxQHemp=0d;
			return true;
		}
		if(itemName.contains("flax") && itemQuality==maxQFlax){
			maxQFlax=0d;
			return true;
		}
		if(itemName.contains("poppy") && itemQuality==maxQPoppy){
			maxQPoppy=0d;
			return true;
		}
		if(itemName.contains("pipeweed") && itemQuality==maxQPipeweed){
			maxQPipeweed=0d;
			return true;
		}
		if(itemName.contains("pumpkin") && itemQuality==maxQPumpkin){
			maxQPumpkin=0d;
			return true;
		}
		if(itemName.contains("lettuce") && itemQuality==maxQLettuce){
			maxQLettuce=0d;
			return true;
		}
		if(itemName.contains("cucumber") && itemQuality==maxQCucumber){
			maxQCucumber=0d;
			return true;
		}
		if(itemName.contains("grape") && itemQuality==maxQGrape){
			maxQGrape=0d;
			return true;
		}
		if(itemName.contains("leek") && itemQuality==maxQLeek){
			maxQLeek=0d;
			return true;
		}
		if(itemName.contains("pepper") && itemQuality==maxQPepper){
			maxQPepper=0d;
			return true;
		}
		if(itemName.contains("greenkale") && itemQuality==maxQKale){
			maxQKale=0d;
			return true;
		}
		if(itemName.contains("cereal") && itemQuality==maxQCornGrass){
			maxQCornGrass=0d;
			return true;
		}
		if(itemName.contains("flower") && itemQuality==maxQWildFlower){
			maxQWildFlower=0d;
			return true;
		}
		if(itemName.contains("fibre") && itemQuality==maxQStringGrass){
			maxQStringGrass=0d;
			return true;
		}
		return false;
	}
}