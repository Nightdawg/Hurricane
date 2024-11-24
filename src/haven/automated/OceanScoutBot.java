package haven.automated;


import haven.*;
import haven.automated.pathfinder.Pathfinder;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

import static haven.OCache.posres;

public class OceanScoutBot extends Window implements Runnable {
    private int checkClock;
    private GameUI gui;
    public boolean stop;
    private MCache mcache;
    private int clockwiseDirection = 1;
    private double ang = 0;
    private double searchRadius = 5;
    private ArrayList<Gob> nearbyGobs = new ArrayList<>();
    private Random random = new Random();
    private int successLocs;
    private boolean active = false;

    public OceanScoutBot(GameUI gui) {
        super(UI.scale(UI.scale(274, 96)), "Ocean Scouting Bot");
        this.gui = gui;
        checkClock = 0;
        stop = false;
        mcache = gui.map.glob.map;
        add(new Label(""), UI.scale(263, 0)); // ND: Label to fix horizontal size
        add(new Label("Remember: The direction of the Shoreline is always"), UI.scale(10, 4));
        add(new Label("the opposite of the Deeper Water Edge."), UI.scale(10, 18));
        CheckBox dirBox = new CheckBox("Clockwise (Deeper Water Edge)") {
            {
                a = true;
            }

            public void set(boolean val) {
                clockwiseDirection = val ? 1 : -1;
                a = val;
            }
        };
        add(dirBox, UI.scale(16, 42));

        add(new Button(UI.scale(170), "Start"){
            @Override
            public void click() {
                active = !active;
                if (active){
                    this.change("Stop");
                } else {
                    ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
                    this.change("Start");
                }
            }
        }, UI.scale(52, 66));
        pack();
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                if (!active) {
                    Thread.sleep(200);
                    continue;
                }
                if (successLocs > 20) {
                    Coord2d groundTile = findRandomGroundTile();
                    Coord2d groundVector = groundTile.sub(ui.gui.map.player().rc);
                    groundVector = groundVector.div(groundVector.abs()).mul(44);
                    ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.add(groundVector).floor(posres), 1, 0);
                    Thread.sleep(300);
                }

                nearbyGobs = getNearbyGobs();
                Coord loc = getNextLoc();
                if (loc != null) {
                    ang -= clockwiseDirection * Math.PI / 2;
                    ui.gui.map.wdgmsg("click", Coord.z, new Coord2d(loc.x, loc.y).floor(posres), 1, 0);
                } else {
                    Coord2d pcCoord = ui.gui.map.player().rc;
                    Coord2d dangerMob = isVeryDangerZone(ui.gui.map.player().rc.floor());
                    if (dangerMob != null) {
                        Coord2d addCoord = pcCoord.sub(dangerMob);
                        Coord2d clickCoord = pcCoord.add(addCoord.div(addCoord.abs()).mul(11 * 2));
                        ui.gui.map.wdgmsg("click", Coord.z, clickCoord.floor(posres), 1, 0);
                    } else {
                        Coord2d gocoord = findRandomWaterTile();
                        Coord2d groundVector = gocoord.sub(ui.gui.map.player().rc);
                        groundVector = groundVector.div(groundVector.abs()).mul(44);
                        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.add(groundVector).floor(posres), 1, 0);
                    }
                    Thread.sleep(300);
                }
                Thread.sleep(200);
                checkClock++;
            }
        } catch (InterruptedException e) {
//            System.out.println("interrupted.. after checkclock: " + checkClock);
        }

    }

    private Coord2d findRandomGroundTile() {
        Coord2d basecoord = gui.map.player().rc;
        int radius = 40 * 11;
        for (int i = 0; i < 1000; i++) {
            Coord2d rancoord = new Coord2d(random.nextInt(radius * 2) - radius, random.nextInt(radius * 2) - radius);
            if (!isWater(basecoord.add(rancoord).floor())) {
                return basecoord.add(rancoord);
            }
        }
        return basecoord;
    }

    private Coord2d findRandomWaterTile() {
        Coord2d basecoord = gui.map.player().rc;
        int radius = 30 * 11;
        for (int i = 0; i < 1000; i++) {
            Coord2d rancoord = new Coord2d(random.nextInt(radius * 2) - radius, random.nextInt(radius * 2) - radius);
            if (isWater(basecoord.add(rancoord).floor())) {
                return basecoord.add(rancoord);
            }
        }
        return basecoord;
    }

    private ArrayList<Gob> getNearbyGobs() {
        ArrayList<Gob> gobs = new ArrayList<>();
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (gui.map.player().rc.dist(gob.rc) < 3) {
                    continue;
                }
                if (gui.map.player().rc.dist(gob.rc) < 25 * 11 && gob.collisionBox != null && gob.collisionBox.fx != null) {
                    gobs.add(gob);
                }
            }
        }
        return gobs;
    }

    private Coord getNextLoc() {
//        Coord pltc = new Coord(gui.map.player().rc.floor().x / 11, gui.map.player().rc.floor().y / 11);
        Coord pc = gui.map.player().rc.floor();
        double curAng = ang;
        int angles = 20;
        while (clockwiseDirection == 1 ? ang <= curAng + 2 * Math.PI : ang >= curAng - 2 * Math.PI) {
            boolean foundground = false;
            for (int i = 0; i < 20; i++) {
                Coord2d addcoord = new Coord2d(-Math.cos(-ang) * i * searchRadius, Math.sin(-ang) * i * searchRadius);
                Coord t = pc.add(addcoord.floor());


                if (checkTiles(t)) {
                    foundground = true;
                }
            }
            if (!foundground) {
                Coord2d addcoord = new Coord2d(-Math.cos(-ang) * 20 * searchRadius, Math.sin(-ang) * 20 * searchRadius);
                successLocs++;
                return (pc.add(addcoord.floor()));
            } else {
                successLocs = 0;
            }
            ang += clockwiseDirection * 2 * Math.PI / angles;
        }
        return null;
    }

    private boolean checkTiles(Coord t) {
        int rad = 2;
        for (int i = -rad; i <= rad; i++) {
            for (int j = -rad; j <= rad; j++) {
                if (!isWater(t.add(i * 11, j * 11))) {
                    return true;
                } else if (isGobCollision(t.add(i * 11, j * 11))) {
                    return true;
                } else if (isDangerZone(t.add(i * 11, j * 11))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGobCollision(Coord t) {
        for (Gob gob : nearbyGobs) {
            if (gob != null && gob.getres() != null) {
                if (Pathfinder.isInsideBoundBox(gob.rc.floor(), gob.a, gob.getres().name, t)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isDangerZone(Coord t) {
        for (Gob gob : nearbyGobs) {
            if(gob.getres() != null){
                if ((gob.getres().name.endsWith("/walrus") || (gob.getres().name.endsWith("/orca")) && t.dist(gob.rc.floor()) < 11 * 14)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Coord2d isVeryDangerZone(Coord t) {
        for (Gob gob : nearbyGobs) {
            if (gob.getres() != null) {
                if ((gob.getres().name.endsWith("/walrus") || (gob.getres().name.endsWith("/orca")) && t.dist(gob.rc.floor()) < 11 * 11)) {
                    return gob.rc;
                }
            }
        }
        return null;
    }


    private boolean isWater(Coord t) {
        Coord pltc = new Coord(t.x / 11, t.y / 11);
        try {
            int dt = mcache.gettile(pltc);
            Resource res = mcache.tilesetr(dt);
            if (res == null)
                return false;

            String name = res.name;
            if (name.equals("gfx/tiles/odeep")) {
                return true;
            } else {
                return false;
            }
        } catch (Loading e) {
            return false;
        }
    }


    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if((sender == this) && (Objects.equals(msg, "close"))) {
            stop = true;
            stop();
            reqdestroy();
            gui.OceanScoutBot = null;
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void stop() {
        ui.gui.map.wdgmsg("click", Coord.z, ui.gui.map.player().rc.floor(posres), 1, 0);
        if (gui.map.pfthread != null) {
            gui.map.pfthread.interrupt();
        }
        if (gui.oceanScoutBotThread != null) {
            gui.oceanScoutBotThread.interrupt();
            gui.oceanScoutBotThread = null;
        }
        this.destroy();
    }

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-oceanScoutBotWindow", this.c);
        super.reqdestroy();
    }

}
