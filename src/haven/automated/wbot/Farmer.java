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

public class Farmer extends Window implements Runnable, AreaSelectCallback {
    private final GameUI gui;
	WBotUtils wBotUtils;
    private boolean stop = false;
	private boolean active;
	
	private String typeOfPlant;
	private String freshStage;
	
	private boolean selectWorkArea = false;
	private boolean selectDestroyArea;

	private int tileRange = 500;
	private int numOfMats;
	private Coord2d gobCoords;
	private Coord start;
	private Coord end;

    public Farmer(GameUI gui) {
        super(UI.scale((240), 650), "Farmer Tools");
        this.gui = gui;
		this.wBotUtils = new WBotUtils(gui);
        
		add(new Button(UI.scale(225), "Select working area") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Please select an area by dragging.");
                selectWorkArea = true;
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.map.areaSelect = true;
            }
        }, UI.scale(10, 10));
		
		add(new Button(UI.scale(225), "Deselect") {
            @Override
            public void click() {
                selectWorkArea = false;
				gui.map.areaSelect = false;
				wBotUtils.sysMsg("The area has been deselected.");
            }
        }, UI.scale(10, 40));
		
		add(new Button(UI.scale(225), "Barley") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest barley!");
				resetActions();
				active = true;
				typeOfPlant="barley";
				freshStage="";
            }
        }, UI.scale(10, 70));
		add(new Button(UI.scale(225), "Beet Root") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest beet root!");
				resetActions();
				active = true;
				typeOfPlant="beet";
				freshStage="";
            }
        }, UI.scale(10, 100));
		add(new Button(UI.scale(225), "Carrots") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest carrots!");
				resetActions();
				active = true;
				typeOfPlant="carrot";
				freshStage="";
            }
        }, UI.scale(10, 130));
		add(new Button(UI.scale(225), "Carrot seeds") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest carrot seeds!");
				resetActions();
				active = true;
				typeOfPlant="carrot";
				freshStage="-seeds";
            }
        }, UI.scale(10, 160));
		add(new Button(UI.scale(225), "Flax") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest flax!");
				resetActions();
				active = true;
				typeOfPlant="flax";
				freshStage="";
            }
        }, UI.scale(10, 190));
		add(new Button(UI.scale(225), "Hemp") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest hemp!");
				resetActions();
				active = true;
				typeOfPlant="hemp";
				freshStage="";
            }
        }, UI.scale(10, 220));
		add(new Button(UI.scale(225), "Hemp buds") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest hemp buds!");
				resetActions();
				active = true;
				typeOfPlant="hemp";
				freshStage="-buds";
            }
        }, UI.scale(10, 250));
		add(new Button(UI.scale(225), "Leeks") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest leeks!");
				resetActions();
				active = true;
				typeOfPlant="leek";
				freshStage="";
            }
        }, UI.scale(10, 280));
		add(new Button(UI.scale(225), "Lettuces") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest lettuces!");
				resetActions();
				active = true;
				typeOfPlant="lettuce";
				freshStage="";
            }
        }, UI.scale(10, 310));
		add(new Button(UI.scale(225), "Millet") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest millet!");
				resetActions();
				active = true;
				typeOfPlant="millet";
				freshStage="";
            }
        }, UI.scale(10, 340));
		add(new Button(UI.scale(225), "Pipeweed") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest pipeweed!");
				resetActions();
				active = true;
				typeOfPlant="pipeweed";
				freshStage="";
            }
        }, UI.scale(10, 370));
		add(new Button(UI.scale(225), "Poppy") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest poppy!");
				resetActions();
				active = true;
				typeOfPlant="poppy";
				freshStage="";
            }
        }, UI.scale(10, 400));
		add(new Button(UI.scale(225), "Pumpkins") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest pumpkins!");
				resetActions();
				active = true;
				typeOfPlant="pumpkin";
				freshStage="";
            }
        }, UI.scale(10, 430));
		add(new Button(UI.scale(225), "Red onions") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest red onions!");
				resetActions();
				active = true;
				typeOfPlant="redonion";
				freshStage="";
            }
        }, UI.scale(10, 460));
		add(new Button(UI.scale(225), "Turnips") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest turnips!");
				resetActions();
				active = true;
				typeOfPlant="turnip";
				freshStage="";
            }
        }, UI.scale(10, 490));
		add(new Button(UI.scale(225), "Turnip seeds") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest turnip seeds!");
				resetActions();
				active = true;
				typeOfPlant="turnip";
				freshStage="-seeds";
            }
        }, UI.scale(10, 520));
		add(new Button(UI.scale(225), "Wheat") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest wheat!");
				resetActions();
				active = true;
				typeOfPlant="wheat";
				freshStage="";
            }
        }, UI.scale(10, 550));
		add(new Button(UI.scale(225), "Yellow onions") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest yellow onions!");
				resetActions();
				active = true;
				typeOfPlant="yellowonion";
				freshStage="";
            }
        }, UI.scale(10, 580));
		add(new Button(UI.scale(225), "Fallow plants") {
            @Override
            public void click() {
				wBotUtils.sysMsg("Let's harvest fallow plants!");
				resetActions();
				active = true;
				typeOfPlant="fallowplant";
				freshStage="";
            }
        }, UI.scale(10, 610));
    }
	
	@Override
    public void run() {
		wBotUtils.sysMsg("Starting Farmer Tools...");
		resetActions();
		try{
			while(!stop){
				if (active) {
					harvest();
				}
				gui.farmerToolsThread.sleep(100);
			}
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
        }
		wBotUtils.sysMsg("Closing Farmer Tools...");
	}
	
	Gob closestBarrel;
	boolean isSpaceAvailable;
	boolean plantsExist;
	int rightStage = -1;
	public void harvest(){
		closestBarrel = null;
		isSpaceAvailable = false;
		plantsExist = true;
		rightStage = wBotUtils.getPhase(typeOfPlant, freshStage);
		main:
		while(plantsExist){
			System.out.println("Starting to harvest");
			plantsExist = doPlantExist();
			if(!plantsExist || stop){
				resetActions();
				break;
			}
			
			if(closestBarrel==null){
				closestBarrel = wBotUtils.findClosestGob("gfx/terobjs/barrel", 220);
			}
			
			if(closestBarrel!=null){
				System.out.println("1_AUtils.rightClickGob");
				AUtils.rightClickGob(gui, closestBarrel, 0);
				System.out.println("1.1_AUtils.rightClickGob");
				wBotUtils.waitForWindow("Barrel", 2000, gui.farmerToolsThread);
				System.out.println("1.2_AUtils.rightClickGob");
				fillBarrel();//If seeds in your inventory exist, you put them in the barrel
				System.out.println(!isInventoryFull());
				while(!isInventoryFull()){
					if(stop){
						break main;
					}
					System.out.println("harvestPlants");
					//Go to closests plant and harvest until your inventory is full
					harvestPlants(500);
				}
			} else {
				break;
			}
		}
		resetActions();
		wBotUtils.sysMsg("Done!");
	}
	
	Gob closestGob;
	public void harvestPlants(int tileRange){
		getGobsToWorkWith(tileRange);
		
		closestGob = wBotUtils.findClosestCrop(gobsToWorkWith, typeOfPlant, freshStage);
		if(closestGob!=null){
			if (gui.map.player().getv() == 0 && gui.prog == null) {
				System.out.println("2_AUtils.rightClickGob"+gui.map.player().getv()+" "+gui.prog);
				AUtils.rightClickGob(gui, closestGob, 0);
				wBotUtils.sleep(gui.farmerToolsThread, 300);
			} else {
				System.out.println("sleep...");
				wBotUtils.sleep(gui.farmerToolsThread, 1000);
			}
		}
		if (gui.getmeter("stam", 0).a < 0.50){
			System.out.println("Rest and drink...");
			try {
                stopToDrink();
                AUtils.drinkTillFull(gui, 0.99, 0.99);
				wBotUtils.sleep(gui.farmerToolsThread, 3000);
            } catch (InterruptedException e) {
                wBotUtils.sysMsg("Drinking interrupted.");
            }
		}
	}
	
	private void stopToDrink(){
        ui.root.wdgmsg("gk", 27);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
    }
	
	public boolean isInventoryFull(){
		if(gui.maininv.isRoom(1,1) != null){
			return false;
		}
		return true;
	}
	
	public void fillBarrel(){
		System.out.println("...fillBarrel");
		for(WItem wItem : wBotUtils.getPlayerInventory(gui).getAllItems()) {
			if(wItem.item.resource().name.contains("seed") && wItem.item.resource().name.contains(typeOfPlant)){
				wBotUtils.takeItem(wItem, true, gui.farmerToolsThread);
				wBotUtils.interactWithGobUsingItemAtHand(closestBarrel, true, gui.farmerToolsThread, 3, 0);
				break;
			}
		}
		System.out.println("fillBarrel...");
	}
	
	public boolean doPlantExist(){
		getGobsToWorkWith(300);
		if(selectWorkArea){
			gobsToWorkWith = new ArrayList<>();
			for (Gob gob : wBotUtils.getGobsInRadius(tileRange)) {
				Resource rc = wBotUtils.getResource(gob);
				if(rc!=null && rc.name!=null && wBotUtils.gobIntheArea(true, gob, start, end)){
					gobsToWorkWith.add(gob);
				}
			}
		}
		for (Gob gob : gobsToWorkWith) {
			if(stop){
				return false;
			}
			Resource rc = wBotUtils.getResource(gob);
			if(rc!=null && rc.name!=null && rc.name.contains(typeOfPlant) && !rc.name.contains("stockpile") && (rightStage==wBotUtils.getCropStage(gob) || typeOfPlant.contains("fallowplant") && rc.name.contains("fallowplant"))){
				return true;
			}
		}
		return false;
	}
	
	private java.util.List<Gob> gobsToWorkWith;
	public void getGobsToWorkWith(int tilerange){
		gobsToWorkWith = wBotUtils.getGobsInRadius(tilerange);
		if(selectWorkArea){
			gobsToWorkWith = new ArrayList<>();
			for (Gob gob : wBotUtils.getGobsInRadius(tileRange)) {
				Resource rc = wBotUtils.getResource(gob);
				if(rc!=null && rc.name!=null && wBotUtils.gobIntheArea(true, gob, start, end)){
					gobsToWorkWith.add(gob);
				}
			}
		}
	}
	
	private void resetActions(){
		active = false;
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
            gui.farmerTools = null;
            gui.farmerToolsThread = null;
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
        Utils.setprefc("wndc-farmerWindow", this.c);
        super.reqdestroy();
    }
}
