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

public class BuilderTools extends Window implements Runnable, AreaSelectCallback {
    private final GameUI gui;
	WBotUtils wBotUtils;
    private boolean stop = false;
	private boolean active;
	private boolean chopIntoBlocks;
	private boolean makeBoards;
	private boolean chipStone;
	private boolean clearReeds;
	private boolean butcherAnimals;
	private boolean destroyFences;
	private boolean destroyStumps;
	private boolean destroyRocks;
	private boolean destroyTrellis;
	private boolean destroyEverything;
	private boolean scanArea;
	
	private boolean selectWorkArea = false;
	private boolean selectDestroyArea;

	private int tileRange = 200;
	private int numOfMats;
	private Coord2d gobCoords;
	private Coord start;
	private Coord end;
	
	private java.util.List<Gob> gobsToWorkWith;
	Gob closestGob;

    public BuilderTools(GameUI gui) {
        super(UI.scale((520), 205), "Builder Tools");
        this.gui = gui;
		this.wBotUtils = new WBotUtils(gui);
        
        add(new Label("Work:"), UI.scale(10, 10));
		add(new Button(UI.scale(400), "Select area where mats are located") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Please select an area by dragging.");
                selectWorkArea = true;
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.map.areaSelect = true;
            }
        }, UI.scale(10, 25));
		add(new Button(UI.scale(100), "Deselect") {
            @Override
            public void click() {
                selectWorkArea = false;
				gui.map.areaSelect = false;
				wBotUtils.sysMsg("The area has been deselected.");
            }
        }, UI.scale(410, 25));
		add(new Button(UI.scale(100), "Chop into blocks") {
            @Override
            public void click() {
				resetActions();
				active = true;
				chopIntoBlocks = true;
            }
        }, UI.scale(10, 50));
		add(new Button(UI.scale(100), "Make boards") {
            @Override
            public void click() {
				resetActions();
				active = true;
				makeBoards = true;
            }
        }, UI.scale(110, 50));
		add(new Button(UI.scale(100), "Chip stone") {
            @Override
            public void click() {
				resetActions();
				active = true;
				chipStone = true;
            }
        }, UI.scale(210, 50));
		add(new Button(UI.scale(100), "Clear reeds") {
            @Override
            public void click() {
				resetActions();
				active = true;
				clearReeds = true;
            }
        }, UI.scale(310, 50));
		add(new Button(UI.scale(100), "Butcher animals") {
            @Override
            public void click() {
				resetActions();
				active = true;
				butcherAnimals = true;
            }
        }, UI.scale(410, 50));
		
		add(new Label("Destroy (selecting the area is not required):"), UI.scale(10, 85));
		add(new Button(UI.scale(400), "Select the area where you are going to destroy") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Please select an area by dragging.");
                selectDestroyArea = true;
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.map.areaSelect = true;
            }
        }, UI.scale(10, 100));
		add(new Button(UI.scale(100), "Deselect") {
            @Override
            public void click() {
                selectDestroyArea = false;
				gui.map.areaSelect = false;
				wBotUtils.sysMsg("The area has been deselected.");
            }
        }, UI.scale(410, 100));
		add(new Button(UI.scale(100), "Fences") {
            @Override
            public void click() {
                resetActions();
				active = true;
				destroyFences = true;
            }
        }, UI.scale(10, 125));
		add(new Button(UI.scale(100), "Stumps") {
            @Override
            public void click() {
                resetActions();
				active = true;
				destroyStumps = true;
            }
        }, UI.scale(110, 125));
		add(new Button(UI.scale(100), "Rocks") {
            @Override
            public void click() {
                resetActions();
				active = true;
				destroyRocks = true;
            }
        }, UI.scale(210, 125));
		add(new Button(UI.scale(100), "Trellis") {
            @Override
            public void click() {
                resetActions();
				active = true;
				destroyTrellis = true;
            }
        }, UI.scale(310, 125));
		add(new Button(UI.scale(100), "All") {
            @Override
            public void click() {
                resetActions();
				active = true;
				destroyEverything = true;
            }
        }, UI.scale(410, 125));
		add(new Button(UI.scale(500), "Scan area") {
            @Override
            public void click() {
				resetActions();
				active = true;
				scanArea = true;
            }
        }, UI.scale(10, 150));
    }
	
	@Override
    public void run() {
		wBotUtils.sysMsg("Starting Builder Tools...");
		resetActions();
		try{
			while(!stop){
				if (active) {
					if(chopIntoBlocks){
						chopIntoBlocks();
					} else if(makeBoards){
						makeBoards();
					} else if(chipStone){
						chipStone();
					} else if(clearReeds){
						clearReeds();
					} else if(butcherAnimals){
						butcherAnimals();
					} else if(destroyFences){
						destroyFences();
					} else if(destroyStumps){
						destroyStumps();
					} else if(destroyRocks){
						destroyRocks();
					} else if(destroyTrellis){
						destroyTrellis();
					} else if(destroyEverything){
						destroyEverything();
					} else if(scanArea){
						scanArea();
					}
				}
				gui.builderToolsThread.sleep(100);
			}
        } catch (InterruptedException e) {
            System.out.println("interrupted");
        }
		wBotUtils.sysMsg("Closing Builder Tools...");
	}
	
	private void resetActions(){
		active = false;
		chopIntoBlocks = false;
		makeBoards = false;
		chipStone = false;
		clearReeds = false;
		butcherAnimals = false;
		destroyFences = false;
		destroyStumps = false;
		destroyRocks = false;
		destroyTrellis = false;
		destroyEverything = false;
		scanArea = false;
	}
	
	private void scanArea(){
		wBotUtils.sysMsg("Le'scan the area");
		scan();
	}
	
	private void scan(){
		int i = 0;
		for (Gob gob : wBotUtils.getGobsInRadius(1500)) {
			i++;
		}
		resetActions();
		wBotUtils.sysMsg("Number of gobs: "+i);
	}
	
	private void destroyFences(){
		wBotUtils.sysMsg("Let's destroy some fences.");
		destroy("arch/pole");
	}
	
	private void destroyStumps(){
		wBotUtils.sysMsg("Let's destroy some stumps.");
		destroy("stump");
	}
	
	private void destroyRocks(){
		wBotUtils.sysMsg("Let's destroy some rocks.");
		destroy("bumlings");
	}
	
	private void destroyTrellis(){
		wBotUtils.sysMsg("Let's destroy some trellis.");
		destroy("trellis");
	}
	
	private void destroyEverything(){
		wBotUtils.sysMsg("Let's destroy everything.");
		destroy("");
	}
	
	private void destroy(String name){
		findGobsToDestroy(selectDestroyArea, name);
		wBotUtils.sysMsg("Elements to destroy: "+numOfMats);
		for(int i=0; i<numOfMats; i++){
			if(stop){
				return;
			}
			doDestroy();
		}
		resetActions();
		wBotUtils.sysMsg("Done!");
	}
	
	private int findGobsToDestroy(boolean isAreaSelected, String name){
		numOfMats = 0;
		gobsToWorkWith = new ArrayList<>();
		for (Gob gob : wBotUtils.getGobsInRadius(tileRange)) {
			Resource rc = wBotUtils.getResource(gob);
			if(rc!=null && rc.name!=null && 
			rc.name.contains(name) && 
			wBotUtils.gobIntheArea(isAreaSelected, gob, start, end)){
				numOfMats++;
				gobsToWorkWith.add(gob);
			}
		}
		return numOfMats;
	}
	
	private void doDestroy(){
		java.util.List<String> idlePoses = wBotUtils.getIdlePoses();
		
		closestGob = wBotUtils.findClosestGob(gobsToWorkWith);
		gobsToWorkWith.remove(closestGob);
		if(closestGob!=null){
			wBotUtils.destroy(closestGob);
			wBotUtils.waitForIdle(gui.builderToolsThread, idlePoses);
		}
	}
	
	private void chopIntoBlocks(){
		wBotUtils.sysMsg("Starting to chop into blocks.");
		findNumberOfLogsForBlocks(selectWorkArea);
		wBotUtils.sysMsg("Logs: "+numOfMats);
		for(int i=0; i<numOfMats; i++){
			if(stop){
				return;
			}
			doChopIntoBlocks();
		}
		resetActions();
		wBotUtils.sysMsg("Done!");
	}
	
	private int findNumberOfLogsForBlocks(boolean isAreaSelected){
		numOfMats = 0;
		gobsToWorkWith = new ArrayList<>();
		for (Gob gob : wBotUtils.getGobsInRadius(tileRange)) {
			Resource rc = wBotUtils.getResource(gob);
			if(rc!=null && rc.name!=null && 
			rc.name.contains("/trees/") && (rc.name.contains("log") || rc.name.contains("trunk")) && 
			wBotUtils.gobIntheArea(isAreaSelected, gob, start, end)){
				numOfMats++;
				gobsToWorkWith.add(gob);
			}
		}
		return numOfMats;
	}
	
	private void doChopIntoBlocks(){
		java.util.List<String> idlePoses = wBotUtils.getIdlePoses();
		
		closestGob = wBotUtils.findClosestGob(gobsToWorkWith);
		gobsToWorkWith.remove(closestGob);
		if(closestGob!=null){
			wBotUtils.clearhand();
			int flowerMenuIndex = 0;
			if(wBotUtils.getResource(closestGob).name.contains("trunk")){
				flowerMenuIndex = 1;
			}
			AUtils.rightClickGobAndSelectOption(gui, closestGob, flowerMenuIndex);
			wBotUtils.waitForIdle(gui.builderToolsThread, idlePoses);
		}
	}
	
	private void makeBoards(){
		wBotUtils.sysMsg("Starting to make boards.");
		findNumberOfLogsForBoards(selectWorkArea);
		wBotUtils.sysMsg("Logs: "+numOfMats);
		for(int i=0; i<numOfMats; i++){
			if(stop){
				return;
			}
			doMakeBoards();
		}
		resetActions();
		wBotUtils.sysMsg("Done!");
	}
	
	private int findNumberOfLogsForBoards(boolean isAreaSelected){
		numOfMats = 0;
		gobsToWorkWith = new ArrayList<>();
		for (Gob gob : wBotUtils.getGobsInRadius(tileRange)) {
			Resource rc = wBotUtils.getResource(gob);
			if(rc!=null && rc.name!=null && 
			rc.name.contains("/trees/") && (rc.name.contains("log")) && 
			wBotUtils.gobIntheArea(isAreaSelected, gob, start, end)){
				numOfMats++;
				gobsToWorkWith.add(gob);
			}
		}
		return numOfMats;
	}
	
	private void doMakeBoards(){
		java.util.List<String> idlePoses = wBotUtils.getIdlePoses();
		
		closestGob = wBotUtils.findClosestGob(gobsToWorkWith);
		gobsToWorkWith.remove(closestGob);
		if(closestGob!=null){
			wBotUtils.clearhand();
			AUtils.rightClickGobAndSelectOption(gui, closestGob, 1);
			wBotUtils.waitForIdle(gui.builderToolsThread, idlePoses);
		}
	}
	
	private void chipStone(){
		wBotUtils.sysMsg("Starting to chip stone.");
		findNumberOfStones(selectWorkArea);
		wBotUtils.sysMsg("Stones: "+numOfMats);
		for(int i=0; i<numOfMats; i++){
			if(stop){
				return;
			}
			doChipStone();
		}
		resetActions();
		wBotUtils.sysMsg("Done!");
	}
	
	private int findNumberOfStones(boolean isAreaSelected){
		numOfMats = 0;
		gobsToWorkWith = new ArrayList<>();
		for (Gob gob : wBotUtils.getGobsInRadius(tileRange)) {
			Resource rc = wBotUtils.getResource(gob);
			if(rc!=null && rc.name!=null && 
			rc.name.contains("/bumlings/") && 
			wBotUtils.gobIntheArea(isAreaSelected, gob, start, end)){
				numOfMats++;
				gobsToWorkWith.add(gob);
			}
		}
		return numOfMats;
	}
	
	private void doChipStone(){
		java.util.List<String> idlePoses = wBotUtils.getIdlePoses();
		
		closestGob = wBotUtils.findClosestGob(gobsToWorkWith);
		gobsToWorkWith.remove(closestGob);
		if(closestGob!=null){
			wBotUtils.clearhand();
			AUtils.rightClickGobAndSelectOption(gui, closestGob, 0);
			wBotUtils.waitForIdle(gui.builderToolsThread, idlePoses);
		}
	}
	
	private void clearReeds(){
		wBotUtils.sysMsg("Starting to clear reeds.");
		findNumberOfReeds(selectWorkArea);
		wBotUtils.sysMsg("Reeds: "+numOfMats);
		for(int i=0; i<numOfMats; i++){
			if(stop){
				return;
			}
			doClearReeds();
		}
		resetActions();
		wBotUtils.sysMsg("Done!");
	}
	
	private int findNumberOfReeds(boolean isAreaSelected){
		numOfMats = 0;
		gobsToWorkWith = new ArrayList<>();
		for (Gob gob : wBotUtils.getGobsInRadius(tileRange)) {
			Resource rc = wBotUtils.getResource(gob);
			if(rc!=null && rc.name!=null && 
			rc.name.contains("/reeds") && 
			wBotUtils.gobIntheArea(isAreaSelected, gob, start, end)){
				numOfMats++;
				gobsToWorkWith.add(gob);
			}
		}
		return numOfMats;
	}
	
	private void doClearReeds(){
		java.util.List<String> idlePoses = wBotUtils.getIdlePoses();
		
		closestGob = wBotUtils.findClosestGob(gobsToWorkWith);
		gobsToWorkWith.remove(closestGob);
		if(closestGob!=null){
			wBotUtils.clearhand();
			AUtils.rightClickGobAndSelectOption(gui, closestGob, 0);
			wBotUtils.waitForIdle(gui.builderToolsThread, idlePoses);
		}
	}
	
	private void butcherAnimals(){
		wBotUtils.sysMsg("Starting to butcher animals.");
		findNumberOfAnimals(selectWorkArea);
		wBotUtils.sysMsg("Animals: "+numOfMats);
		for(int i=0; i<numOfMats; i++){
			if(stop){
				return;
			}
			doButcherAnimals();
		}
		resetActions();
		wBotUtils.sysMsg("Done!");
	}
	
	private int findNumberOfAnimals(boolean isAreaSelected){
		numOfMats = 0;
		gobsToWorkWith = new ArrayList<>();
		for (Gob gob : wBotUtils.getGobsInRadius(tileRange)) {
			Resource rc = wBotUtils.getResource(gob);
			if(rc!=null && rc.name!=null && 
			rc.name.contains("kritter") && 
			wBotUtils.gobIntheArea(isAreaSelected, gob, start, end) && gob.knocked){
				numOfMats++;
				gobsToWorkWith.add(gob);
			}
		}
		return numOfMats;
	}
	
	private void doButcherAnimals(){
		java.util.List<String> idlePoses = wBotUtils.getIdlePoses();
		
		closestGob = wBotUtils.findClosestGob(gobsToWorkWith);
		gobsToWorkWith.remove(closestGob);
		if(closestGob!=null){
			wBotUtils.clearhand();
			AUtils.rightClickGobAndSelectOption(gui, closestGob, 0);//Skin
			wBotUtils.waitForIdle(gui.builderToolsThread, idlePoses);
			wBotUtils.clearhand();
			AUtils.rightClickGobAndSelectOption(gui, closestGob, 0);//Clean
			wBotUtils.waitForIdle(gui.builderToolsThread, idlePoses);
			wBotUtils.clearhand();
			AUtils.rightClickGobAndSelectOption(gui, closestGob, 0);//Butcher
			wBotUtils.waitForIdle(gui.builderToolsThread, idlePoses);
			wBotUtils.clearhand();
			AUtils.rightClickGobAndSelectOption(gui, closestGob, 0);//Collect bones
			wBotUtils.waitForIdle(gui.builderToolsThread, idlePoses);
		}
	}
	
	@Override
    public void areaselect(Coord a, Coord b) {
        Coord nw = a.mul(MCache.tilesz2);
        Coord se = b.mul(MCache.tilesz2);
		start = nw;
		end = se;
		
		System.out.println("The area has been selected.");
        gui.msg("The area has been selected.", Color.WHITE, UI.InfoMessage.sfx);
		
        gui.map.unregisterAreaSelect();
    }
	
	@Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.builderTools = null;
            gui.builderToolsThread = null;
        } else
            super.wdgmsg(sender, msg, args);
    }
	
	public void stop() {
        wBotUtils.leftClickGob(gui);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
        this.destroy();
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-getTheBestSeedsWindow", this.c);
        super.reqdestroy();
    }
}
