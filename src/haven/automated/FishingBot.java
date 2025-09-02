package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;
import haven.automated.helpers.FishingData;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static haven.OCache.posres;

public class FishingBot extends Window implements Runnable {
    private final GameUI gui;

    private boolean stop;
    private boolean active;
    private final Button startButton;
    private final Label fishingDirLabel;
    private Coord fishingDir;

    public FishingBot(GameUI gui) {
        super(UI.scale(120, 140), "FishingBot");
        this.gui = gui;
        this.stop = false;
        this.active = false;
        fishingDir = new Coord(0, -44);

        startButton = add(new Button(UI.scale(50), "Start") {
            @Override
            public void click() {
                active = !active;
                if (active) {
                    this.change("Stop");
                } else {
                    this.change("Start");
                }
            }
        }, UI.scale(30, 100));

        Button northDirection = new Button(UI.scale(25), "N") {
            @Override
            public void click() {
                changeDirection(1);
            }
        };
        add(northDirection, UI.scale(45, 10));
        Button eastDirection = new Button(UI.scale(25), "E") {
            @Override
            public void click() {
                changeDirection(2);
            }
        };
        add(eastDirection, UI.scale(75, 35));
        Button southDirection = new Button(UI.scale(25), "S") {
            @Override
            public void click() {
                changeDirection(3);
            }
        };
        add(southDirection, UI.scale(45, 60));
        Button westDirection = new Button(UI.scale(25), "W") {
            @Override
            public void click() {
                changeDirection(4);
            }
        };
        add(westDirection, UI.scale(15, 35));

        fishingDirLabel = new Label("N");
        add(fishingDirLabel, UI.scale(52, 38));
        fishingDirLabel.tooltip = RichText.render("Choose water direction. 4 Tiles in front of player.", UI.scale(300));
    }


    private void changeDirection(int dir){
        if(dir == 1){
            fishingDir = new Coord(0, -44);
            fishingDirLabel.settext("N");
        } else if (dir == 2) {
            fishingDir = new Coord(44, 0);
            fishingDirLabel.settext("E");
        } else if (dir == 3) {
            fishingDir = new Coord(0, 44);
            fishingDirLabel.settext("S");
        } else if (dir == 4) {
            fishingDir = new Coord(-44, 0);
            fishingDirLabel.settext("W");
        }
    }

    private int contentAnalysis(WItem item) {
        int count = 0;
        try {
            for (ItemInfo info : item.item.info()) {
                if (info instanceof ItemInfo.Contents) {
                    count++;
                }
            }
        } catch (Loading ignored){}
        return count;
    }



    private void manageFishingPole(){
        Equipory equipory = gui.getequipory();
        WItem leftHand = equipory.slots[6];
        WItem rightHand = equipory.slots[7];
        int fishingPoleState = -1;
        int hand = -1;
        if (leftHand != null && leftHand.item != null && leftHand.item.getres() != null && leftHand.item.getres().name.contains("gfx/invobjs/small/bushpole")) {
            fishingPoleState = contentAnalysis(leftHand);
            hand = 6;
        } else if (rightHand != null && rightHand.item != null && rightHand.item.getres() != null && rightHand.item.getres().name.contains("gfx/invobjs/small/bushpole")) {
            fishingPoleState = contentAnalysis(rightHand);
            hand = 7;
        }

        if(fishingPoleState == -1){
            startButton.change("Start");
            active = false;
            gui.error("No fishing pole in hand.");
            return;
        }

        List<WItem> items = new ArrayList<>();
        if(fishingPoleState < 3){
            items = AUtils.getAllItemsFromAllInventoriesAndStacksExcludeBeltAndKeyring(gui);
        }

        if(fishingPoleState == 0){
            putOnFishingLine(items, hand);
        } else if (fishingPoleState == 1){
            putOnHook(items, hand);
        } else if (fishingPoleState == 2){
            putOnBait(items, hand);
        } else if (fishingPoleState == 3){
            if(gui.prog == null){
                gui.wdgmsg("act", "fish");
                gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(Coord2d.of(fishingDir)).floor(posres), 1, 0);
                sleep(1000);
                gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.add(Coord2d.of(fishingDir)).floor(posres), 3, 0);
            } else {
                sleep(1000);
            }
        }
    }

    private void putOnFishingLine(List<WItem> items, int hand){
        List<WItem> fishingLines = new ArrayList<>();
        for(WItem item : items){
            if(item.item.getname().contains("Fishline")){
                fishingLines.add(item);
            }
        }
        if(fishingLines.size() == 0){
            active = false;
            startButton.change("Start");
            gui.error("No fishing line in inventories. Stopping");
        } else {
            gui.msg("Putting on fishing line.", Color.WHITE);
            fishingLines.get(0).item.wdgmsg("take", Coord.z);
            gui.getequipory().slots[hand].item.wdgmsg("itemact", 0);
            sleep(500);
        }
    }

    private void putOnHook(List<WItem> items, int hand){
        List<WItem> hooks = new ArrayList<>();
        for(WItem item : items){
            if(item.item.getname().contains("Hook")){
                hooks.add(item);
            }
        }
        if(hooks.size() == 0){
            active = false;
            startButton.change("Start");
            gui.error("No hook in inventories. Stopping");
        } else {
            gui.msg("Putting on fishing hook.", Color.WHITE);
            hooks.get(0).item.wdgmsg("take", Coord.z);
            gui.getequipory().slots[hand].item.wdgmsg("itemact", 0);
            sleep(500);
        }
    }

    private void putOnBait(List<WItem> items, int hand){
        List<WItem> bait = new ArrayList<>();
        for(WItem item : items){
            if(FishingData.baits.contains(item.item.getname())){
                bait.add(item);
            }
        }
        if(bait.size() == 0){
            active = false;
            startButton.change("Start");
            gui.error("No bait in inventories. Stopping");
        } else {
            gui.msg("Putting on bait.", Color.WHITE);
            bait.get(0).item.wdgmsg("take", Coord.z);
            gui.getequipory().slots[hand].item.wdgmsg("itemact", 0);
            sleep(500);
        }
    }



    @Override
    public void run() {
        while (!stop) {
            if(active){
                int freeSpace = 0;
                for(Inventory inventory: gui.getAllInventories()){
                    if (!AUtils.isBeltOrKeyring(inventory)) {
                        freeSpace = freeSpace + inventory.getFreeSpace();
                    }
                }
                if(freeSpace < 3){
                    active = false;
                    gui.error("full inv stopping");
                    startButton.change("Start");
                }
                manageFishingPole();
            }
            sleep(200);
        }
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.fishingBot = null;
            gui.fishingThread = null;
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void stop() {
        gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 1, 0);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
        this.destroy();
    }
}
