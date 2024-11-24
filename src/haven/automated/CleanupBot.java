package haven.automated;

import haven.*;

import java.util.Objects;

import static haven.OCache.posres;
import static java.lang.Thread.sleep;

public class CleanupBot extends Window implements Runnable {
    private GameUI gui;
    private boolean chopBushes;
    private boolean stop;
    private boolean chopTrees;
    private boolean chipRocks;
    private boolean destroyStumps;
    private boolean destroySoil;
    private CheckBox bushcheckBox;
    private CheckBox treecheckBox;
    private CheckBox stumpcheckBox;
    private CheckBox rockcheckBox;
    private CheckBox soilcheckBox;
    private boolean active;
    private Button activeButton;


    public CleanupBot(GameUI gui) {
        super(UI.scale(220, 105), "Cleanup Bot");
        this.gui = gui;
        stop = false;
        chopBushes = false;
        chopTrees = false;
        chipRocks = false;
        destroyStumps = false;
        destroySoil = false;

        bushcheckBox = new CheckBox("Bushes") {
            {
                a = chopBushes;
            }

            public void set(boolean val) {
                chopBushes = val;
                a = val;
            }
        };
        add(bushcheckBox, UI.scale(10, 10));
        treecheckBox = new CheckBox("Trees") {
            {
                a = chopTrees;
            }

            public void set(boolean val) {
                chopTrees = val;
                a = val;
            }
        };
        add(treecheckBox, UI.scale(10, 30));
        rockcheckBox = new CheckBox("Rocks") {
            {
                a = chipRocks;
            }

            public void set(boolean val) {
                chipRocks = val;
                a = val;
            }
        };
        add(rockcheckBox, UI.scale(10, 50));
        stumpcheckBox = new CheckBox("Stumps") {
            {
                a = destroyStumps;
            }

            public void set(boolean val) {
                destroyStumps = val;
                a = val;
            }
        };
        add(stumpcheckBox, UI.scale(10, 70));

        soilcheckBox = new CheckBox("Soil") {
            {
                a = destroySoil;
            }

            public void set(boolean val) {
                destroySoil = val;
                a = val;
            }
        };

        add(soilcheckBox, UI.scale(90, 10));

        activeButton = new Button(UI.scale(50), "Start") {
            @Override
            public void click() {
                active = !active;
                if (active) {
                    this.change("Stop");
                } else {
                    ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
                    this.change("Start");
                }
            }
        };
        add(activeButton, UI.scale(120, 70));
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                if ((chopBushes || chopTrees || destroyStumps || chipRocks || destroySoil) && active) {
                    if (gui.getmeters("hp").get(1).a < 0.02) {
                        System.out.println("HP IS " + gui.getmeters("hp").get(1).a + " .. PORTING HOME!");
                        gui.act("travel", "hearth");
                        try {
                            Thread.sleep(8000);
                        } catch (InterruptedException e) {
                        }
                    }
                    else if (ui.gui.getmeter("nrj", 0).a < 0.25) {
                        gui.error("Need food");
                        stop();
                    }
                    else if (gui.getmeter("stam", 0).a < 0.40) {
                        try {
                            AUtils.drinkTillFull(gui, 0.99, 0.99);
                        } catch (InterruptedException e) {
                            System.out.println("interrupted");
                        }
                    } else {
                        Gob gob = findClosestGob();
                        if (chipRocks) {
                            dropStones();
                        }
                        destroyGob(gob);
                    }
                }
                sleep(2000);
            }
        } catch (InterruptedException e) {
            System.out.println("interrupted");
        }
    }

    private void destroyGob(Gob gob) throws InterruptedException {
        if(gui.prog != null && gui.map.player().getPoses().contains("pickan") || gui.map.player().getPoses().contains("treechop") || gui.map.player().getPoses().contains("chopping") || gui.map.player().getPoses().contains("shoveldig") || gui.map.player().getPoses().contains("drinkan")){
            waitWhileWorking(2000);
        } else {
            if (gob != null) {
                gui.map.pfLeftClick(gob.rc.floor().add(20, 0), null);
                if (!AUtils.waitPf(gui)) {
                    AUtils.unstuck(gui);
                }
                if (gob.rc.dist(gui.map.player().rc) < 11 * 5) {
                    Resource res = gob.getres();
                    clearhand();
                    if (res.name.contains("/trees/") && !res.name.endsWith("stump") && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk") || res.name.contains("/bushes/")) {
                        AUtils.rightClickGobAndSelectOption(gui, gob, 0);
                        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                        waitWhileWorking(2000);
                    } else if (res.name.contains("/bumlings/")) {
                        AUtils.rightClickGobAndSelectOption(gui, gob, 0);
                        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 3, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                        waitWhileWorking(2000);
                    } else if (res.name.endsWith("stump") || res.name.endsWith("/stockpile-soil")) {
                        gui.act("destroy");
                        gui.map.wdgmsg("click", Coord.z, gob.rc.floor(posres), 1, 0, 0, (int) gob.id, gob.rc.floor(posres), 0, -1);
                        gui.map.wdgmsg("click", Coord.z, Coord.z, 3, 0);
                        waitWhileWorking(2000);
                    }
                }
            } else {
                gui.error("Nothing left to destroy.");
                activeButton.change("Start");
                active = false;
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void clearhand() {
        if (gui.vhand != null) {
            gui.vhand.item.wdgmsg("drop", Coord.z);
        }
        AUtils.rightClick(gui);
    }

    private void waitWhileWorking(int timeout) throws InterruptedException {
        sleep(1500);
        int hz = 50;
        int time = 0;
        while (gui.prog != null && gui.prog.prog != -1 && time < timeout && gui.getmeter("stam", 0).a > 0.40) {
            time += hz;
            sleep(hz);
        }
    }

    private void dropStones() {
        for (WItem wItem : ui.gui.maininv.getAllItems()) {
            GItem gitem = wItem.item;
            if (Config.stoneItemBaseNames.contains(gitem.resource().basename())) {
                gitem.wdgmsg("drop", new Coord(wItem.item.sz.x / 2, wItem.item.sz.y / 2));
            }
        }
    }

    private Gob findClosestGob() {
        Gob closestGob = null;
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                try {
                    Resource res = gob.getres();
                    boolean selected =
                            (res.name.contains("/bumlings/") && chipRocks)
                                    || (res.name.endsWith("stump") && destroyStumps)
                                    || ((res.name.contains("/trees/") && !res.name.endsWith("stump")) && !res.name.endsWith("log") && !res.name.endsWith("oldtrunk") && chopTrees)
                                    || (res.name.contains("/bushes/") && chopBushes)
                                    || (res.name.endsWith("/stockpile-soil") && destroySoil);
                    if (res != null && selected) {
                        Coord2d plc = gui.map.player().rc;
                        if ((closestGob == null || gob.rc.dist(plc) < closestGob.rc.dist(plc)))
                            closestGob = gob;
                    }
                } catch (Loading | NullPointerException ignored) {
                }
            }
        }
        return closestGob;
    }


    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.cleanupBot = null;
            gui.cleanupThread = null;
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void stop() {
        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        if (ui.gui.map.pfthread != null) {
            ui.gui.map.pfthread.interrupt();
        }
        this.destroy();
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-cleanupBotWindow", this.c);
        super.reqdestroy();
    }

}
