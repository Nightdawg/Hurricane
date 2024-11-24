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

public class TurnipBot extends Window implements Runnable, AreaSelectCallback {
    private final GameUI gui;
    private boolean stop;
    private boolean selectGranary;

    private final List<TurnipField> fields;
    private final Label fieldsLabel;

    private Gob granary;
    private final Label granaryLabel;

    private boolean harvest = true;
    private boolean plant = true;

    private boolean active;
    private final Button startButton;

    private int currentField;
    private int stage;

    public TurnipBot(GameUI gui) {
        super(UI.scale((250), 130), "TurnipFarmer");
        this.gui = gui;
        this.fields = new ArrayList<>();
        currentField = 0;
        stage = 0;

        add(new Button(UI.scale(60), "Field") {
            @Override
            public void click() {
                selectGranary = false;
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.msg("Select single field.", Color.WHITE);
                gui.map.areaSelect = true;
            }
        }, UI.scale(15, 15));

        add(new Button(UI.scale(60), "Granary") {
            @Override
            public void click() {
                selectGranary = true;
                gui.map.registerAreaSelect((AreaSelectCallback) this.parent);
                gui.msg("Select area with granary.", Color.WHITE);
                gui.map.areaSelect = true;
            }
        }, UI.scale(80, 15));

        add(new Button(UI.scale(50), "Reset") {
            @Override
            public void click() {
                fields.clear();
                fieldsLabel.settext("Fields: 0");
                granary = null;
                granaryLabel.settext("Granary: ✘");
                active = false;
                startButton.change("Start");
                currentField = 0;
                stage = 0;
            }
        }, UI.scale(150, 15));

        fieldsLabel = new Label("Fields: 0");
        add(fieldsLabel, UI.scale(50, 50));

        granaryLabel = new Label("Granary: ✘");
        add(granaryLabel, UI.scale(120, 50));


        add(new CheckBox("Harvest") {
            {
                a = true;
            }

            public void set(boolean val) {
                harvest = val;
                a = val;
            }
        }, UI.scale(30, 80));

        add(new CheckBox("Plant") {
            {
                a = true;
            }

            public void set(boolean val) {
                plant = val;
                a = val;
            }
        }, UI.scale(100, 80));



        startButton = add(new Button(UI.scale(50), "Start") {
            @Override
            public void click() {
                if (fields.size() > 0 && granary != null) {
                    active = !active;
                    if (active) {
                        this.change("Stop");
                    } else {
                        this.change("Start");
                    }
                } else {
                    gui.error("Need to select at least one field and granary.");
                }
            }
        }, UI.scale(160, 80));

    }

    @Override
    public void areaselect(Coord a, Coord b) {
        Coord nw = a.mul(MCache.tilesz2);
        Coord se = b.mul(MCache.tilesz2);
        if (selectGranary) {
            handleGranarySelection(nw, se);
        } else {
            handleFieldSelection(nw, se);
        }
        gui.map.unregisterAreaSelect();
    }

    private void handleGranarySelection(Coord nw, Coord se) {
        List<Gob> granaries = AUtils.getGobsInSelectionStartingWith("gfx/terobjs/granary", nw, se, gui);
        if (granaries.size() == 1) {
            granary = granaries.get(0);
            granaryLabel.settext("Granary: ✔");
            gui.msg("Granary found.", Color.WHITE);
        } else {
            gui.msg("No granary in selected area.", Color.WHITE);
        }
    }

    private void handleFieldSelection(Coord nw, Coord se) {
        fields.add(new TurnipField(fields.size(), nw, se));
        fieldsLabel.settext("Fields: " + fields.size());
        gui.msg("Area selected: " + (se.x - nw.x) / 11 + "x" + (se.y - nw.y) / 11, Color.WHITE);
    }

    @Override
    public void run() {
        while (!stop) {
            if (active) {
                if(!FarmingStatic.turnipDrop){
                    FarmingStatic.turnipDrop = true;
                }
                if(!harvest && !plant){
                 startButton.change("start");
                 active = false;
                 gui.msg("Need to choose either harvest, plant or both", Color.WHITE);
                }

                if(stage < 2 && !harvest){
                    stage = 2;
                }
                clearhand();
                if (fields.size() > 0 && granary != null) {
                    TurnipField currentField = getFieldByIndex(this.currentField);
                    checkHealthStaminaEnergy();
                    if (currentField == null) {
                        resetFarmBot();
                    } else {
                        if (currentField.closestCoord == null) {
                            TurnipField field = getFieldByIndex(this.currentField);
                            currentField.closestCoord = new Coord(0,0);
                            field.initializeHarvestingSegments();
                            field.initializePlantingSegments();
                        } else {
                            //need to empty inv before using so can equip scythe - uh just force scythe before clicking start maybe?
                            //also some kind of checkbox if tsacks or wbindles - self explanatory
                            handleStage(currentField);
                        }
                    }
                }
            } else {
                if(FarmingStatic.turnipDrop){
                    FarmingStatic.turnipDrop = false;
                }
            }
            sleep(200);
        }
    }

    private void stopToDrink(){
        ui.root.wdgmsg("gk", 27);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
    }

    private void handleStage(TurnipField currentField) {
        switch (stage) {
            case 0:
                handleStage0(currentField);
                break;
            case 1:
                handleStage1(currentField);
                break;
            case 2:
                handleStage2(currentField);
                break;
            case 3:
                handleStage3(currentField);
                break;
        }
    }

    private void handleStage0(TurnipField currentField) {
        sleep(100);
        if (gui.maininv.getFreeSpace() < 1) {
            depositIfFullInventory();
        } else {
            if(currentField.currentIndex == currentField.harvestingSegments.size()){
                stage = 1;
                gui.msg("Field finished, Depositing seeds", Color.WHITE);
            } else {
                FieldSegment currentFieldSegment = currentField.harvestingSegments.get(currentField.currentIndex);
                processField(currentField, currentFieldSegment);
            }
        }
    }

    private void handleStage1(TurnipField currentField) {
        if (checkIfSeedsInInventory()) {
            depositAllSeeds();
        } else {
            if(plant){
                new Thread(new EquipFromBelt(gui, "Equip_TravelersSacks"), "EquipFromBelt").start();
                stage = 2;
                currentField.setCurrentIndex(0);
                gui.msg("Seeds deposited, planting.", Color.WHITE);
            } else {
                stage = 0;
                gui.msg("Planting skipped. Next field", Color.WHITE);
                this.currentField++;
            }

        }
    }

    private void handleStage2(TurnipField currentField) {
        if(currentField.currentIndex == currentField.plantingSegments.size()){
            stage = 3;
            this.currentField++;
            gui.msg("Field finished, Depositing seeds", Color.WHITE);
        } else {
            FieldSegment currentFieldSegment = currentField.plantingSegments.get(currentField.currentIndex);
            List<Gob> gobs = AUtils.getGobsInSelectionStartingWith("gfx/terobjs/plants/turnip", currentFieldSegment.topLeft, currentFieldSegment.bottomRight, gui);
            if (!checkIfSeedsInInventory() && gobs.size() < currentFieldSegment.size) {
                getHighestQualitySeeds();
            } else if (checkIfSeedsInInventory() && gobs.size() < currentFieldSegment.size) {
                plantSeeds(currentFieldSegment);
            } else if (gobs.size() >= currentFieldSegment.size) {
                currentField.setCurrentIndex(currentField.currentIndex+1);
            }
        }
    }

    private void handleStage3(TurnipField currentField) {
        if (checkIfSeedsInInventory()) {
            depositAllSeeds();
        } else {
            if(harvest){
                new Thread(new EquipFromBelt(gui, "Equip_Scythe"), "EquipFromBelt").start();
                stage = 0;
            } else {
                stage = 2;
            }
            currentField.setCurrentIndex(0);
            gui.msg("Seeds deposited, going to next field.", Color.WHITE);
        }
    }

    private void processField(TurnipField currentField, FieldSegment currentFieldSegment) {
        if (!AUtils.isPlayerInSelectedArea(currentFieldSegment.topLeft, currentFieldSegment.bottomRight, gui)) {
            try {
                Thread.sleep(300);
                gui.map.pfLeftClick(currentFieldSegment.initCoord, null);
                AUtils.waitPf(gui);
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
        } else {
            Gob closest = AUtils.getClosestCropInSelectionStartingWith("gfx/terobjs/plants/turnip", currentFieldSegment.topLeft, currentFieldSegment.bottomRight, gui, 1);
            if (closest == null) {
                currentField.setCurrentIndex(currentField.currentIndex+1);
                gui.msg("Harvesting next row.", Color.WHITE);
            } else {
                if (gui.map.player().getv() == 0 && gui.prog == null) {
                    AUtils.rightClickGob(gui, closest, 1);
                    gui.map.wdgmsg("sel", currentFieldSegment.start.div(11), currentFieldSegment.end.sub(1, 1).div(11), 0);
                } else {
                    sleep(500);
                }
            }
        }
    }

    private void plantSeeds(FieldSegment currentFieldSegment) {
        if (!AUtils.isPlayerInSelectedArea(currentFieldSegment.topLeft, currentFieldSegment.bottomRight, gui)) {
            try {
                Thread.sleep(300);
                gui.map.pfLeftClick(currentFieldSegment.initCoord, null);
                AUtils.waitPf(gui);
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
        } else {
            if (gui.map.player().getv() == 0 && gui.prog == null) {
                GItem firstSeedInInventory = null;
                for (WItem wItem : gui.maininv.getAllItems()) {
                    try {
                        if (wItem.item.getres() != null && wItem.item.getres().name.equals("gfx/invobjs/seed-turnip")) {
                            firstSeedInInventory = wItem.item;
                        }
                    } catch (Loading e) {
                    }
                }
                if (firstSeedInInventory != null) {
                    firstSeedInInventory.wdgmsg("iact", Coord.z, 1);
                    gui.map.wdgmsg("sel", currentFieldSegment.start.div(11), currentFieldSegment.end.sub(1, 1).div(11), 0);
                } else {
                    gui.error("Something went wrong.");
                }
            } else {
                sleep(500);
            }
        }
    }

    private boolean checkIfSeedsInInventory() {
        boolean seeds = false;
        for (WItem wItem : gui.maininv.getAllItems()) {
            try {
                if (wItem.item.getres() != null && wItem.item.getres().name.equals("gfx/invobjs/seed-turnip")) {
                    seeds = true;
                }
            } catch (Loading e) {
            }
        }
        return seeds;
    }

    private void getHighestQualitySeeds() {
        try {
            Thread.sleep(300);
            if (FarmingStatic.grainSlots.size() == 0) {
                gui.map.pfRightClick(granary, -1, 3, 0, null);
                AUtils.waitPf(gui);
                Thread.sleep(1000);
            } else if (FarmingStatic.grainSlots.size() == 10) {
                takeBestSeeds();
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void takeBestSeeds() {
        Grainslot best = FarmingStatic.grainSlots.stream()
                .filter(grainslot -> grainslot.getRawinfo() != null)
                .filter(grainslot -> {
                    boolean turnip = grainslot.info().stream().anyMatch(info -> info instanceof ItemInfo.Name && ((ItemInfo.Name) info).original.contains("Turnip"));
                    boolean enoughForField = grainslot.info().stream().anyMatch(info -> info instanceof GItem.Amount && ((GItem.Amount) info).itemnum() >= gui.maininv.getFreeSpace() * 50);
                    double qualityTemp = grainslot.info().stream().filter(info -> info instanceof Quality).mapToDouble(info -> ((Quality) info).q).findFirst().orElse(0.0);
                    boolean betterQl = qualityTemp > 0;
                    return turnip && betterQl && enoughForField;
                })
                .findFirst().orElse(null);

        if (best != null) {
            while (gui.maininv.getFreeSpace() > 0) {
                takeSeeds(best, gui.maininv.getFreeSpace());
                sleep(1000);
            }
        }
    }

    private void takeSeeds(Grainslot best, int freeSpace) {
        for (int i = 0; i < freeSpace; i++) {
            best.wdgmsg("take");
        }
    }

    private void depositAllSeeds() {
        handleSeedsDeposit(false);
    }

    private void depositIfFullInventory() {
        handleSeedsDeposit(true);
    }

    private void handleSeedsDeposit(boolean checkFreeSpace) {
        try {
            int freeSpace = checkFreeSpace ? gui.maininv.getFreeSpace() : 0;
            Thread.sleep(300);
            if (freeSpace < 1 && FarmingStatic.grainSlots.size() == 0) {
                gui.map.pfRightClick(granary, -1, 3, 0, null);
                AUtils.waitPf(gui);
                Thread.sleep(1000);
            } else if (freeSpace < 1 && FarmingStatic.grainSlots.size() == 10) {
                iterateThroughSeeds();
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void iterateThroughSeeds() {
        gui.maininv.getAllItems().stream()
                .filter(wItem -> wItem.item.getres() != null && wItem.item.getres().name.equals("gfx/invobjs/seed-turnip"))
                .forEach(wItem -> {
                    try {
                        double quality = wItem.item.info().stream().filter(info -> info instanceof Quality).map(info -> ((Quality) info).q).findFirst().orElse(0.0);
                        int amount = wItem.item.info().stream().filter(info -> info instanceof GItem.Amount).mapToInt(info -> ((GItem.Amount) info).itemnum()).findFirst().orElse(0);

                        Grainslot firstEmpty = FarmingStatic.grainSlots.stream().filter(grainslot -> grainslot.getRawinfo() == null).findFirst().orElse(null);
                        Grainslot matchingQl = FarmingStatic.grainSlots.stream()
                                .filter(grainslot -> grainslot.getRawinfo() != null)
                                .filter(grainslot -> {
                                    boolean turnip = grainslot.info().stream().anyMatch(info -> info instanceof ItemInfo.Name && ((ItemInfo.Name) info).original.contains("Turnip"));
                                    boolean fitAll = grainslot.info().stream().anyMatch(info -> info instanceof GItem.Amount && ((GItem.Amount) info).itemnum() + amount <= 200000);
                                    boolean qlMatch = grainslot.info().stream().anyMatch(info -> info instanceof Quality && ((Quality) info).q == quality);
                                    return turnip && fitAll && qlMatch;
                                })
                                .findFirst().orElse(null);

                        handleGrainslot(wItem, matchingQl, firstEmpty);
                    } catch (Loading ignored) {
                    }
                });
    }

    private void handleGrainslot(WItem wItem, Grainslot matchingQl, Grainslot firstEmpty) {
        if (matchingQl != null) {
            moveItem(wItem, matchingQl);
        } else if (firstEmpty != null) {
            moveItem(wItem, firstEmpty);
        } else {
            active = false;
            startButton.change("Start");
            gui.error("No space in granary. Stopping.");
        }
    }

    private void clearhand() {
        if (!gui.hand.isEmpty()) {
            if (gui.vhand != null) {
                gui.vhand.item.wdgmsg("drop", Coord.z);
            }
        }
        AUtils.rightClick(gui);
    }

    private void dropTurnips() {
        for (WItem wItem : ui.gui.maininv.getAllItems()) {
            GItem gitem = wItem.item;
            if (gitem.getname().equals("Turnip") || gitem.getname().equals("Turnip, stack of")) {
                gitem.wdgmsg("drop", new Coord(wItem.item.sz.x / 2, wItem.item.sz.y / 2));
            }
        }
    }

    private void moveItem(WItem wItem, Grainslot grainslot) {
        wItem.item.wdgmsg("take", Coord.z);
        grainslot.wdgmsg("drop", 0);
    }

    private void checkHealthStaminaEnergy() {
        if (gui.getmeters("hp").get(1).a < 0.1) {
            System.out.println("Low HP, porting home.");
            gui.act("travel", "hearth");
            stop();
            try {
                Thread.sleep(8000);
            } catch (InterruptedException ignored) {
            }
        } else if (gui.getmeter("nrj", 0).a < 0.30) {
            gui.error("Energy critical. Farmer stopping.");
            stop();
        } else if (gui.getmeter("stam", 0).a < 0.40) {
            try {
                stopToDrink();
                AUtils.drinkTillFull(gui, 0.99, 0.99);
            } catch (InterruptedException e) {
                System.out.println("Drinking interrupted.");
            }
        }
    }

    public TurnipField getFieldByIndex(int index) {
        for (TurnipField field : fields) {
            if (field.fieldIndex == index) {
                return field;
            }
        }
        return null;
    }

    private void resetFarmBot() {
        gui.msg("Farming Finished.", Color.WHITE);
        fields.clear();
        fieldsLabel.settext("Fields: 0");
        granary = null;
        granaryLabel.settext("Granary: ✘");
        active = false;
        startButton.change("Start");
        currentField = 0;
        stage = 0;
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
            FarmingStatic.turnipDrop = false;
            gui.turnipBot = null;
            gui.turnipThread = null;
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

    private static class TurnipField {
        private final int fieldIndex;
        private final int size;
        private final int height;
        private final int width;
        private int currentIndex;

        private final Coord fieldNW;
        private final Coord fieldSE;
        private Coord closestCoord;
        private List<FieldSegment> harvestingSegments;
        private List<FieldSegment> plantingSegments;

        public TurnipField(int fieldIndex, Coord farmNW, Coord farmSE) {
            this.harvestingSegments = new ArrayList<>();
            this.plantingSegments = new ArrayList<>();
            this.fieldIndex = fieldIndex;
            this.height = (farmSE.y - farmNW.y) / 11;
            this.width = (farmSE.x - farmNW.x) / 11;
            this.size = height * width;
            this.fieldNW = farmNW;
            this.fieldSE = farmSE;
            this.currentIndex = 0;
        }

        public Coord getFieldNW() {
            return fieldNW;
        }

        public Coord getFieldSE() {
            return fieldSE;
        }

        public void setClosestCoord(Coord closestCoord) {
            this.closestCoord = closestCoord;
        }

        public void setCurrentIndex(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        public void initializeHarvestingSegments() {
            boolean isStartLeft = true;

            for (int i = 0; i < height; i += 3) {
                int top = fieldNW.y + i*11;
                int bottom = Math.min(fieldNW.y + i*11 + 33, fieldSE.y);

                Coord start = new Coord(isStartLeft ? fieldNW.x : fieldSE.x, top);
                Coord end = new Coord(isStartLeft ? fieldSE.x : fieldNW.x, bottom);

                int numberOfRows = (bottom - top) / 11;

                harvestingSegments.add(new FieldSegment(start, end, isStartLeft, numberOfRows));

                isStartLeft = !isStartLeft;
            }
        }

        public void initializePlantingSegments() {
            boolean isStartLeft = true;

            for (int i = 0; i < height; i += 2) {
                int top = fieldNW.y + i*11;
                int bottom = Math.min(fieldNW.y + i*11 + 22, fieldSE.y);

                Coord start = new Coord(isStartLeft ? fieldNW.x : fieldSE.x, top);
                Coord end = new Coord(isStartLeft ? fieldSE.x : fieldNW.x, bottom);

                plantingSegments.add(new FieldSegment(start, end, isStartLeft,(fieldNW.y + i*11 + 22 > fieldSE.y) ? 1 : 2));

                isStartLeft = !isStartLeft;
            }
        }

    }

    private static class FieldSegment {
        private final Coord start;
        private final Coord end;
        private final Coord topLeft;
        private final Coord bottomRight;
        private final Coord initCoord;
        private final int height;
        private final int width;
        private final int size;

        public FieldSegment(Coord start, Coord end, boolean isStartLeft, int rows) {
            this.start = start;
            this.end = end;

            this.height = Math.abs(end.y - start.y) / 11;
            this.width = Math.abs(end.x - start.x) / 11;
            this.size = height * width;

            int topLeftX = Math.min(start.x, end.x);
            int topLeftY = Math.min(start.y, end.y);
            this.topLeft = new Coord(topLeftX, topLeftY);

            int bottomRightX = Math.max(start.x, end.x);
            int bottomRightY = Math.max(start.y, end.y);
            this.bottomRight = new Coord(bottomRightX, bottomRightY);

            if (isStartLeft) {
                if(rows == 1 || rows == 2){
                    this.initCoord = start.add(4, 5);
                } else {
                    this.initCoord = start.add(4, 16);
                }
            } else {
                if(rows == 1){
                    this.initCoord = start.add(-4, 5);
                } else {
                    this.initCoord = start.add(-4, 16);
                }
            }
        }
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-turnipBotWindow", this.c);
        super.reqdestroy();
    }
}
