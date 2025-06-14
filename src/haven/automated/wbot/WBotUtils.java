package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.automated.helpers.AreaSelectCallback;
import haven.automated.helpers.FarmingStatic;
import haven.res.ui.tt.q.quality.Quality;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static haven.OCache.posres;

// Button 1 = Left click and 3 = right click
// Modifier 1 - shift; 2 - ctrl; 3 - shift+ctrl;4 - alt;
public class WBotUtils {
	private GameUI gui;
	Gob closestGob;
	double lastGobDistance;
	double currentGobDistance;
	int cropStage;
	int idleCounter;
	boolean isIdle;
	
	public WBotUtils(GameUI gui) {
		this.gui = gui;
	}
	
	public Window getContainerWindow(String[] containerTypes) {
		Window containerWindow;
		for(String containerType : containerTypes){
			containerWindow = getWindowByName(containerType);
			if(containerWindow!=null){
				return containerWindow;
			}
		}
		return null;
	}
	
	public Window getWindowByName(String windowName) {
		return gui.getwnd(windowName);
	}
	
	public Inventory getPlayerInventory(GameUI gui) {
		for (Widget wdg : getWindowByName("Inventory").children()) {
			if (wdg instanceof Inventory) {
				return (Inventory) wdg;
			}
		}
		return null;
	}
	
    public Inventory getInventoryByWindow(Window window) {
		for (Widget wdg : window.children()) {
			if (wdg instanceof Inventory) {
				return (Inventory) wdg;
			}
		}
		return null;
	}
	
	public int getAmount(WItem wItem) {
        int ret = -1;
		for (ItemInfo o : wItem.item.info()) {
			if (o instanceof GItem.Amount)
				ret = ((GItem.Amount) o).itemnum();
		}
        return ret;
    }
	
	public void takeItem(WItem wItem, boolean wait, Thread thread) {
		takeItem(wItem);
		if(wait){
			waitTillHavingItemAtHand(thread);
		}
    }
	
	public void takeItem(WItem wItem) {
		System.out.println("1takeItem "+wItem+" ");
		System.out.println("2takeItem "+wItem.item+" ");
		System.out.println("3takeItem "+getInvLoc(wItem)+" ");
        wItem.item.wdgmsg("take", getInvLoc(wItem));
    }
	
	public void waitTillHavingItemAtHand(Thread thread) {
		while (getItemAtHand() == null){
			if(!sleep(thread)){
				break;
			}
		}
    }
	
	public void waitTillNotHavingItemAtHand(Thread thread) {
		int i = 0;
		while (getItemAtHand() != null && i<10){
			i++;
			if(!sleep(thread)){
				break;
			}
		}
    }
	
	public Coord getInvLoc(WItem wItem) {
        return wItem.c.div(33);
    }
	
	public WItem getItemAtHand() {
        if (gui.vhand == null)
            return null;
        else
            return gui.vhand;
    }
	
	//Modifier: 0 = rigth click
	public void interactWithItemUsingItemAtHand(WItem wItem, boolean wait, Thread thread){
		wItem.item.wdgmsg("itemact", 0);
		if(wait){
			waitTillNotHavingItemAtHand(thread);
		}
	}
	
	// mod1: 1 = left click;  3 = right click;
	// mod2: 0 = none; 1 = shift; 2 = ctrl; 3 = shift+ctrl; 4 = alt;
	public void interactWithGobUsingItemAtHand(Gob gob, boolean wait, Thread thread, int mod1, int mod2){
		gui.map.wdgmsg("itemact", Coord.z, gob.rc.floor(posres), mod1, mod2, (int) gob.id, gob.rc.floor(posres), 0, -1);
		if(wait){
			waitTillNotHavingItemAtHand(thread);
		}
	}
	
	public Inventory takeItemFromInventory(Thread thread, String itemName){
		java.util.List<Inventory> inventories = gui.getAllInventories();
		loop:
		for(Inventory i : inventories){
			for(WItem wItem : i.getAllItems()) {
				if(wItem.item.resource().name.contains(itemName)){
					takeItem(wItem, true, thread);
					return i;
				}
			}
		}
		return null;
	}
	
	public Gob findClosestGob(String resname, int tileRange){
		java.util.List<Gob> gobsInRadius = getGobsInRadius(tileRange);
		return findClosestGob(resname, gobsInRadius);
	}
	
	public Gob findClosestGob(String resname, java.util.List<Gob> gobsToWorkWith){
		closestGob = null;
		lastGobDistance=-1d;
		currentGobDistance=-1d;
		
		for (Gob gob : gobsToWorkWith) {
			Resource rc = getResource(gob);
			if(rc!=null && resname.equals(rc.name)){
				currentGobDistance = getCoords(gob).dist(player().rc);
				if(lastGobDistance<0d || currentGobDistance<lastGobDistance){
					lastGobDistance = currentGobDistance;
					closestGob = gob;
				}
			}
		}
		return closestGob;
	}
	
	public Gob findClosestCrop(java.util.List<Gob> gobsToWorkWith, String typeOfPlant, String freshStage){
		closestGob = null;
		lastGobDistance=-1d;
		currentGobDistance=-1d;
		
		for (Gob gob : gobsToWorkWith) {
			Resource rc = getResource(gob);
			if(rc!=null && rc.name.contains(typeOfPlant) && !rc.name.contains("stockpile") && !rc.name.contains("fibre") && (getPhase(typeOfPlant, freshStage)==getCropStage(gob) || typeOfPlant.contains("fallowplant") && rc.name.contains("fallowplant"))){
				currentGobDistance = getCoords(gob).dist(player().rc);
				if(lastGobDistance<0d || currentGobDistance<lastGobDistance){
					lastGobDistance = currentGobDistance;
					closestGob = gob;
				}
			}
		}
		return closestGob;
	}
	
	String typeName;
	public int getPhase(String typeOfPlant, String freshStage) {
		typeName=typeOfPlant+freshStage;
		switch (typeName) {
			case "turnip-seeds":
				return 1;
			case "yellowonion":
			case "redonion":
			case "beetroot":
			case "turnip":
			case "barley":
			case "wheat":
			case "millet":
			case "flax":
			case "carrot-seeds":
			case "hemp-buds":
				return 3;
			case "carrot":
			case "leek":
			case "poppy":
			case "pipeweed":
			case "lettuce":
			case "hemp":
			case "pumpkin":
				return 4;
			default:
				return -1;
		}
	}
	
	public Gob findClosestGob(java.util.List<Gob> gobsToWorkWith){
		closestGob = null;
		lastGobDistance=-1d;
		currentGobDistance=-1d;
		
		for (Gob gob : gobsToWorkWith) {
			currentGobDistance = getCoords(gob).dist(player().rc);
			if(lastGobDistance<0d || currentGobDistance<lastGobDistance){
				lastGobDistance = currentGobDistance;
				closestGob = gob;
			}
		}
		return closestGob;
	}
	
	public java.util.List<Gob> getGobsInRadius(int radius){
		Gob player = player();
		while(player==null){
			player = player();
		}
		Coord2d plc = getCoords(player());
        double min = radius;
		
		java.util.List<Gob> gobsInRadius = new ArrayList<>();
		synchronized (gui.map.glob.oc) {
			for (Gob gob : gui.map.glob.oc) {
				if (isPlayer(gob)) continue;
				double dist = gob.rc.dist(plc);
				if (dist < min) {
					gobsInRadius.add(gob);
				}
			}
		}
		return gobsInRadius;
	}
	
	private String[] animalTypes = {
		"gfx/kritter/lynx/lynx",
		"gfx/kritter/bear/bear",
		"gfx/kritter/badger/badger",
		"gfx/kritter/goldeneagle/goldeneagle",
		"gfx/kritter/fox/fox",
		"gfx/kritter/moose/moose",
		"gfx/kritter/walrus/walrus",
		"gfx/kritter/horse/horse",
		"gfx/kritter/wolverine/wolverine",
		"gfx/kritter/greyseal/greyseal",
		"gfx/kritter/swan/swan",
		"gfx/kritter/bat/bat",
		"gfx/kritter/reddeer/reddeer",
		"gfx/kritter/boar/boar",
		"gfx/kritter/reindeer/reindeer",
		"gfx/kritter/caveangler/caveangler",
		"gfx/kritter/otter/otter",
		"gfx/kritter/beaver/beaver",
		"gfx/kritter/pelican/pelican",
		"gfx/kritter/wolf/wolf",
		"gfx/kritter/ooze/greenooze",
		"gfx/kritter/goat/wildgoat",
		"gfx/kritter/rat/caverat",
		"gfx/borka/body"
	}; 
	
	public Gob findClosestTarget(int distance){
		closestGob = null;
		lastGobDistance=-1d;
		currentGobDistance=-1d;
		
		for (Gob gob : getGobsInRadius(distance)) {
			Resource rc = getResource(gob);
			if(rc!=null){
				if(rc.name.contains("gfx/borka/body") && gob.isFriend() || (gob.knocked != null && gob.knocked)){
					continue;
				}
				for(String animalType : animalTypes){
					if(animalType.equals(rc.name)){
						currentGobDistance = getCoords(gob).dist(player().rc);
						if(lastGobDistance<0d || currentGobDistance<lastGobDistance){
							lastGobDistance = currentGobDistance;
							closestGob = gob;
						}
						break;
					}
				}
			}
		}
		return closestGob;
	}
	
	public Gob findClosestTamedAnimal(int distance){
		closestGob = null;
		lastGobDistance=-1d;
		currentGobDistance=-1d;
		
		for (Gob gob : getGobsInRadius(distance)) {
			Resource rc = getResource(gob);
			if (rc != null && (rc.name.startsWith("gfx/kritter/horse") ||
			rc.name.startsWith("gfx/kritter/sheep") ||
			rc.name.startsWith("gfx/kritter/cattle") ||
			rc.name.startsWith("gfx/kritter/pig") ||
			rc.name.startsWith("gfx/kritter/goat"))) {
				currentGobDistance = getCoords(gob).dist(player().rc);
				if(lastGobDistance<0d || currentGobDistance<lastGobDistance){
					lastGobDistance = currentGobDistance;
					closestGob = gob;
				}
			}
		}
		return closestGob;
	}
	
	private boolean isPlayer(Gob gob){
        return getResource(gob) != null && getResource(gob).name != null && getResource(gob).name.equals("gfx/borka/body");
    }
	
	public ArrayList<Gob> getGobs(String name, GameUI gui) {
        ArrayList<Gob> gobs = new ArrayList<>();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
				Resource res = getResource(gob);
				if (res != null && res.name.equals(name)) {
					gobs.add(gob);
				}
            }
        }
        return gobs;
    }
	
	@Deprecated
	public Resource getResource(Gob gob){
		try {
			return gob.getres();
		} catch (Loading l) {
			return null;
		}
	}
	
	public Gob player() {
        if (gui != null && gui.map != null) {
            if (gui.map.player() != null)
                return gui.map.player();
            else
                return null;
        } else
        return null;
    }
	
	public Coord2d getCoords(Gob gob) {
		return gob != null ? gob.rc : null;
	}

	public Window waitForWindow(String windowName, long timeout, Thread thread) {
        Window window;
        int retries = 0;
        while ((window = gui.getwnd(windowName)) == null) {
            if (retries * 25 >= timeout) {
                return null;
            }
            retries++;
            if(!sleep(thread)){
				break;
			}
        }
        return window;
    }
	
	public boolean sleep(Thread thread) {
		try {
			thread.sleep(25);
			return true;
		} catch (InterruptedException e) {
			System.out.println("Error: WBotUtils.sleep(Thread thread)");
			return false;
		}
    }
	
	public boolean sleep(Thread thread, int time){
		try {
			thread.sleep(time);
			return true;
		} catch (InterruptedException e) {
			System.out.println("Error: WBotUtils.sleep(Thread thread, int time)");
			return false;
		}
	}
	
	public void takeItemFromBarrel(Window barrelWindow, int numberOfTimes){
		if(barrelWindow!=null && barrelWindow.lchild!=null){
			for (Widget w = barrelWindow.lchild; w != null; w = w.prev) {
				System.out.println("*******"+w.children().size());
				if(w.children().size()>=3){
					for(int e = 0; e < numberOfTimes; e++){
						for(int i = 0; i < w.children().size(); i++){
							if(w.children().get(i) instanceof Button && "Take".equals(((Button) w.children().get(i)).text.text)){
								((Button) w.children().get(i)).click();
							}
						}
					}
					break;
				}
			}
		}
	}
	
	public void putItOnTheInventory(Inventory inventory, Thread thread){
		if(gui.vhand != null && inventory!=null){
			final Coord freeroom = inventory.isRoom(1, 1);
			inventory.wdgmsg("drop", freeroom);
		}
		waitTillNotHavingItemAtHand(thread);
	}
	
	public boolean gobIntheArea(boolean isAreaSelected, Gob gob, Coord start, Coord end){
		if (!isAreaSelected || gob.rc.x > start.x && gob.rc.x < end.x && gob.rc.y > start.y && gob.rc.y < end.y) {
			return true;
		}
		return false;
	}
	
	public void clearhand() {
        if (!gui.hand.isEmpty()) {
            if (gui.vhand != null) {
                gui.vhand.item.wdgmsg("drop", Coord.z);
            }
        }
        AUtils.rightClick(gui);
    }
	
	public static void leftClickGob(GameUI gui){
		gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 1, 0);
	}
	
	public static void leftClickGob(GameUI gui, Gob gob, int mods) {
        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, mods, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
    }
	
	public void waitForIdle(Thread thread, java.util.List<String> idlePoses){
		idleCounter = 0;
		while(true){
			isIdle = true;
			for (String currentPose : gui.map.player().getPoses()) {
				if(!idlePoses.contains(currentPose)){
					isIdle = false;
				}
			}
			
			if(isIdle){
				idleCounter++;
			}
			
			if(idleCounter>3){
				return;
			}
			sleep(thread, 500);
		}
	}
	
	public java.util.List<String> getIdlePoses(){
		java.util.List<String> poses = new ArrayList<>();
		for (String s : gui.map.player().getPoses()) {
			poses.add(s);
		}
		return poses;
	}
	
	public void sysMsg(String s){
		System.out.println(s);
		gui.msg(s, Color.WHITE, UI.InfoMessage.sfx);
	}
	
	public void destroy(Gob gob){
		doAction(gob, "destroy");
	}
	
	public void shoot(Gob gob){
		doAction(gob, "shoot");
	}
	
	public void leash(Thread thread, Gob closestGob){
		takeItemFromInventory(thread, "rope");
		interactWithGobUsingItemAtHand(closestGob, true, thread, 3, 0);
	}
	
	public void doAction(Gob gob, String action){
		gui.act(action);
        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
        gui.map.wdgmsg("click", Coord.z, Coord.z, 3, 0);
	}
	
	public int getCropStage(Gob gob){
		Message data = getDrawableData(gob);
		cropStage = -1;
		if(data != null) {
			try{
				cropStage = data.uint8();
			} catch(Exception e){
				System.out.println("Error getCropStage...");
			}
		}
		return cropStage;
	}
	
	public Message getDrawableData(Gob gob) {
		Drawable dr = gob.getattr(Drawable.class);
		ResDrawable d = (dr instanceof ResDrawable) ? (ResDrawable) dr : null;
		if(d != null)
			return d.sdt.clone();
		else
			return null;
    }
}
