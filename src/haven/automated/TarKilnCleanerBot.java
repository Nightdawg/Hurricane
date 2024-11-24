package haven.automated;

import haven.*;

import java.util.ArrayList;
import java.util.Objects;

import static haven.OCache.posres;

public class TarKilnCleanerBot extends Window implements Runnable {
    private final CheckBox activeBox;
    private GameUI gui;

    private boolean stop;
    private int phase = 1;
    private boolean active;

    public TarKilnCleanerBot(GameUI gui) {
        super(UI.scale(150, 50), "Tar Kiln Emptier");
        this.gui = gui;
        stop = false;
        active = false;

        activeBox = new CheckBox("Active") {
            {
                a = active;
            }

            public void set(boolean val) {
                active = val;
                a = val;
                phase = 1;
            }
        };

        add(activeBox, UI.scale(40, 15));
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                if (active) {
                    if (phase == 1) {
                        if (gui.vhand != null && gui.vhand.item != null) {
                            gui.vhand.item.wdgmsg("drop", Coord.z);
                        }
                        dropCoal();

                        ArrayList<Gob> tarKilns = AUtils.getGobs("gfx/terobjs/tarkiln", gui);

                        Gob closest = null;
                        for (Gob tarKiln : tarKilns) {
                            if (closest == null || tarKiln.rc.dist(gui.map.player().rc) < closest.rc.dist(gui.map.player().rc)) {
                                ResDrawable resDrawable = tarKiln.getattr(ResDrawable.class);
                                if(resDrawable.sdt.checkrbuf(0) == 10 || resDrawable.sdt.checkrbuf(0) == 42){
                                    closest = tarKiln;
                                }
                            }
                        }

                        if (closest == null) {
                            active = false;
                            activeBox.set(false);
                            ui.gui.error("No full tar kilns nearby.");
                            continue;
                        }

                        if(gui.prog == null) {
                            int[][] options = {{33, 0}, {-33, 0}, {0, 33}, {0, -33}};

                            for (int[] option : options) {
                                Coord newCoord = closest.rc.floor().add(option[0], option[1]);
                                gui.map.pfLeftClick(newCoord, null);
                                Thread.sleep(500);
                                AUtils.waitPf(gui);

                                if (gui.map.player().rc.dist(new Coord2d(newCoord)) < 40) {
                                    break;
                                }
                            }
                            AUtils.rightClickShiftCtrl(gui, closest);
                            Thread.sleep(1000);
                            AUtils.waitProgBar(gui);
                        }
                    }
                }

                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            System.out.println("interrupted..");
        }
    }

    private void dropCoal() {
        for (WItem wItem : ui.gui.maininv.getAllItems()) {
            GItem gitem = wItem.item;
            if (gitem.getname().contains("Coal")) {
                gitem.wdgmsg("drop", new Coord(wItem.item.sz.x / 2, wItem.item.sz.y / 2));
            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.tarKilnCleanerBot = null;
            gui.tarKilnCleanerThread = null;
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
        Utils.setprefc("wndc-tarKilnCleanerBotWindow", this.c);
        super.reqdestroy();
    }
}
