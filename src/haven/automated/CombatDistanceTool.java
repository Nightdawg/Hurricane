package haven.automated;

import haven.*;
import haven.Button;
import haven.Label;
import haven.Window;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static haven.OCache.posres;

public class CombatDistanceTool extends Window implements Runnable {
    public static final Map<String, Double> animalDistances = Map.ofEntries(
            Map.entry("gfx/kritter/adder/adder", 17.1),
            Map.entry("gfx/kritter/ant/ant", 15.2),
            Map.entry("gfx/kritter/cattle/cattle", 27.0),
            Map.entry("gfx/kritter/badger/badger", 19.9),
            Map.entry("gfx/kritter/bear/bear", 24.7),
            Map.entry("gfx/kritter/boar/boar", 25.1),
            Map.entry("gfx/kritter/caveangler/caveangler", 27.2),
            Map.entry("gfx/kritter/cavelouse/cavelouse", 22.0),
            Map.entry("gfx/kritter/fox/fox", 18.1),
            Map.entry("gfx/kritter/horse/horse", 23.0),
            Map.entry("gfx/kritter/lynx/lynx", 20.0),
            Map.entry("gfx/kritter/mammoth/mammoth", 30.3),
            Map.entry("gfx/kritter/moose/moose", 25.0),
            Map.entry("gfx/kritter/narwhal/narwhal", 32.0),
            Map.entry("gfx/kritter/orca/orca", 49.25),
            Map.entry("gfx/kritter/reddeer/reddeer", 25.0),
            Map.entry("gfx/kritter/roedeer/roedeer", 22.0),
            Map.entry("gfx/kritter/spermwhale/spermwhale", 112.2),
            Map.entry("gfx/kritter/goat/wildgoat", 18.9),
            Map.entry("gfx/kritter/wolf/wolf", 25.0),
            Map.entry("gfx/kritter/wolverine/wolverine", 21.0),
            Map.entry("gfx/borka/body", 55.0)
    );
    public static final Map<String, Double> vehicleDistance = Map.ofEntries(
            Map.entry("gfx/terobjs/vehicle/rowboat", 13.3),
            Map.entry("gfx/terobjs/vehicle/dugout", 7.4),
            Map.entry("gfx/terobjs/vehicle/snekkja", 29.35),
            Map.entry("gfx/terobjs/vehicle/knarr", 54.5),
            Map.entry("gfx/kritter/horse/stallion", 5.4),
            Map.entry("gfx/kritter/horse/mare", 5.4)
    );

    private final GameUI gui;
    public boolean stop;

    private final Label currentDistanceLabel;

    private String value;

    public void setValue(String value) {
        this.value = value;
    }

    public CombatDistanceTool(GameUI gui) {
        super(new Coord(180, 60), "Combat Distancing Tool", true);
        this.gui = gui;
        this.stop = false;
        this.value = "";

        Widget prev;

        prev = add(new Label("Set Distance:"), 0, 6);

        prev = add(new TextEntry(UI.scale(100), value) {
            @Override
            protected void changed() {
                setValue(this.buf.line());
            }
        }, prev.pos("ur").adds(2, 0));

        prev = add(new Button(UI.scale(40), "Go") {
            @Override
            public void click() {
                moveToDistance();
            }
        }, prev.pos("ur").adds(4, -2));

        prev = add(new Button(UI.scale(50), "Auto") {
            @Override
            public void click() {
                tryToAutoDistance();
            }
        }, prev.pos("bl").adds(0, 6));

        currentDistanceLabel = new Label("Current dist: No target");
        add(currentDistanceLabel, UI.scale(new Coord(0, 40)));
        pack();
    }

    @Override
    public void run() {
        while (!stop) {
            if (gui.fv.current != null) {
                double dist = getDistance(gui.fv.current.gobid);
                if (dist < 0) {
                    currentDistanceLabel.settext("No target");
                } else {
                    DecimalFormat df = new DecimalFormat("#.##");
                    String result = df.format(dist);
                    currentDistanceLabel.settext("Current dist: " + result + " units.");
                }
            } else {
                currentDistanceLabel.settext("Current dist: No target");
            }

            sleep(500);
        }
    }

    private void tryToAutoDistance() {
        if (gui != null && gui.map != null && gui.map.player() != null && gui.fv.current != null) {
            Double value = -1.0;

            double addedValue = 0.0;

            Gob player = ui.gui.map.player();
            if (player.occupiedGobID != null) {
                Gob vehicle = ui.sess.glob.oc.getgob(player.occupiedGobID);
                if (vehicle != null && vehicle.getres() != null) {
                    addedValue = vehicleDistance.getOrDefault(vehicle.getres().name, 0.0);
                }
            }
            Gob enemy = getEnemy();
            if(enemy != null && enemy.getres() != null){
                value = animalDistances.get(enemy.getres().name);
            }
            if(value != null && value > 0){
                moveToDistance(value+addedValue);
            }

        }
    }

    private void moveToDistance() {
        try {
            double distance = Double.parseDouble(value);
            Gob enemy = getEnemy();
            if (enemy != null && gui.map.player() != null) {
                double angle = enemy.rc.angle(gui.map.player().rc);
                gui.map.wdgmsg("click", Coord.z, getNewCoord(enemy, distance, angle).floor(posres), 1, 0);
            } else {
                gui.msg("No visible target.", Color.WHITE);
            }
            setfocus(ui.gui.portrait); // ND: do this to defocus the text entry box after you click on "Go"
        } catch (NumberFormatException e) {
            gui.error("Wrong distance format. Use ##.###");
        }
    }

    private void moveToDistance(double distance) {
        try {
            Gob enemy = getEnemy();
            if (enemy != null && gui.map.player() != null) {
                double angle = enemy.rc.angle(gui.map.player().rc);
                gui.map.wdgmsg("click", Coord.z, getNewCoord(enemy, distance, angle).floor(posres), 1, 0);
            } else {
                gui.msg("No visible target.", Color.WHITE);
            }
            setfocus(ui.gui.portrait); // ND: do this to defocus the text entry box after you click on "Go"
        } catch (NumberFormatException e) {
            gui.error("Wrong distance format. Use ##.###");
        }
    }

    private Coord2d getNewCoord(Gob enemy, double distance, double angle) {
        return new Coord2d(enemy.rc.x + distance * Math.cos(angle), enemy.rc.y + distance * Math.sin(angle));
    }

    private Gob getEnemy() {
        if (gui.fv.current != null) {
            long id = gui.fv.current.gobid;
            synchronized (gui.map.glob.oc) {
                for (Gob gob : gui.map.glob.oc) {
                    if (gob.id == id) {
                        return gob;
                    }
                }
            }
        }
        return null;
    }

    private double getDistance(long gobId) {
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gob.id == gobId && gui.map.player() != null) {
                    return gob.rc.dist(gui.map.player().rc);
                }
            }
        }
        return -1;
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
            if (gui.combatDistanceTool != null) {
                gui.combatDistanceTool.stop();
                gui.combatDistanceTool.reqdestroy();
                gui.combatDistanceTool = null;
                gui.combatDistanceToolThread = null;
            }
        } else
            super.wdgmsg(sender, msg, args);
    }

    public void stop() {
        stop = true;
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
        this.destroy();
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-combatDistanceToolWindow", this.c);
        super.reqdestroy();
    }
}
