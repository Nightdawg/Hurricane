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
    private static Map<String, Double> animalDistances = new HashMap<>();
    private static Map<String, Double> vehicleDistance = new HashMap<>();

    static {
        animalDistances.put("gfx/kritter/adder/adder", 17.1);
        animalDistances.put("gfx/kritter/ant/ant", 15.2);
        animalDistances.put("gfx/kritter/aurochs/aurochs", 27.0);
        animalDistances.put("gfx/kritter/badger/badger", 19.9);
        animalDistances.put("gfx/kritter/bear/bear", 24.7);
        animalDistances.put("gfx/kritter/boar/boar", 25.1);
        animalDistances.put("gfx/kritter/caveangler/caveangler", 27.2);
        animalDistances.put("gfx/kritter/cavelouse/cavelouse", 22.0);
        animalDistances.put("gfx/kritter/fox/fox", 18.1);
        animalDistances.put("gfx/kritter/horse/horse", 23.0);
        animalDistances.put("gfx/kritter/lynx/lynx", 20.0);
        animalDistances.put("gfx/kritter/mammoth/mammoth", 30.3);
        animalDistances.put("gfx/kritter/moose/moose", 25.0);
        animalDistances.put("gfx/kritter/orca/orca", 49.25);
        animalDistances.put("gfx/kritter/reddeer/reddeer", 25.0);
        animalDistances.put("gfx/kritter/roedeer/roedeer", 22.0);
        animalDistances.put("gfx/kritter/spermwhale/spermwhale", 112.2);
        animalDistances.put("gfx/kritter/goat/wildgoat", 18.9);
        animalDistances.put("gfx/kritter/wolf/wolf", 25.0);
        animalDistances.put("gfx/kritter/wolverine/wolverine", 21.0);
        animalDistances.put("gfx/borka/body", 55.0);

        vehicleDistance.put("gfx/terobjs/vehicle/rowboat", 13.3);
        vehicleDistance.put("gfx/terobjs/vehicle/dugout", 7.4);
        vehicleDistance.put("gfx/terobjs/vehicle/snekkja", 29.35);
        vehicleDistance.put("gfx/terobjs/vehicle/knarr", 54.5);
        vehicleDistance.put("gfx/kritter/horse/stallion", 5.4);
        vehicleDistance.put("gfx/kritter/horse/mare", 5.4);
    }

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
            synchronized (ui.sess.glob.oc) {
                for (Gob gob : ui.sess.glob.oc) {
                    if (gob.getres() != null && gob.rc.dist(gui.map.player().rc) < 11) {
                        addedValue = vehicleDistance.getOrDefault(gob.getres().name, 0.0);
                    }
                    if(gob.id == gui.fv.current.gobid){
                        if(gob.getres() != null){
                            value = animalDistances.get(gob.getres().name);
                        }
                    }
                }
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
