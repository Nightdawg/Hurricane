package haven.automated;


import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class InteractWithNearestObject implements Runnable {
    private GameUI gui;

    public InteractWithNearestObject(GameUI gui) {
        this.gui = gui;
    }

    public final static HashSet<String> gates = new HashSet<String>(Arrays.asList(
            "brickwallgate",
            "brickbiggate",
            "drystonewallgate",
            "drystonewallbiggate",
            "palisadegate",
            "palisadebiggate",
            "polegate",
            "polebiggate"
    ));

    public final static Set<String> otherPickableObjects = new HashSet<String>(Arrays.asList( // ND: Pretty much any ground item can be added here
            "adder",
            "arrow",
            "bat",
            "precioussnowflake",
            "truffle-black0",
            "truffle-black1",
            "truffle-black2",
            "truffle-black3",
            "truffle-white0",
            "truffle-white1",
            "truffle-white2",
            "truffle-white3",
            "gemstone",
            "boarspear"
    ));

    public final static HashSet<String> mines = new HashSet<String>(Arrays.asList(
            "gfx/terobjs/ladder",
            "gfx/terobjs/minehole"
    ));

    public final static HashSet<String> caves = new HashSet<String>(Arrays.asList(
            "gfx/tiles/ridges/cavein",
            "gfx/tiles/ridges/caveout"
    ));

    double maxDistance = 12 * 11;
    @Override
    public void run() {
        Gob theObject = null;
        Gob player = gui.map.player();
        if (player == null)
            return; // player is null, possibly taking a road, don't bother trying to do any of the below
        Coord2d plc = player.rc;
        for (Gob gob : Utils.getAllGobs(gui)) {
            double distFromPlayer = gob.rc.dist(plc);
            if (gob.id == gui.map.plgob || distFromPlayer >= maxDistance)
                continue;
            Resource res = null;
            try {
                res = gob.getres();
            } catch (Loading l) {
            }
            if (res != null) {
                // Open nearby gates, but not visitor gates
                boolean isGate = gates.contains(res.basename());
                try {
                    if (isGate) {
                        for (Gob.Overlay ol : gob.ols) {
                            String oname = ol.spr.res.name;
                            if (oname.equals("gfx/fx/eq"))
                                isGate = false;
                        }
                    }
                } catch (NullPointerException ignored) {}
                if ((isGate && Utils.getprefb("clickNearestObject_NonVisitorGates", true))
                || ((res.name.startsWith("gfx/terobjs/herbs") || otherPickableObjects.contains(res.basename())) && Utils.getprefb("clickNearestObject_Forageables", true))
                || (Arrays.stream(Config.critterResPaths).anyMatch(res.name::matches) || res.name.matches(".*(rabbit|bunny)$")) && Utils.getprefb("clickNearestObject_Critters", true)
                || (caves.contains(res.name) && Utils.getprefb("clickNearestObject_Caves", false))
                || (mines.contains(res.name) && Utils.getprefb("clickNearestObject_MineholesAndLadders", false))) {
                    if (distFromPlayer < maxDistance && (theObject == null || distFromPlayer < theObject.rc.dist(plc))) {
                        theObject = gob;
                        if (res.name.startsWith("gfx/terobjs/herbs")) FlowerMenu.setNextSelection("Pick"); // ND: Set the flower menu option to "pick" only for these particular ones.
                    }
                }
            }
        }
        Target best = null;
        if (Utils.getprefb("clickNearestObject_Doors", true)) {
            Target fromTable = nearestFromBuildings(maxDistance);
            Gob suffixGob = nearestBySuffix(maxDistance);
            Target fromSuffix = (suffixGob == null) ? null : new Target(suffixGob.rc, Coord.z, suffixGob.id, -1);

            best = minByDist(plc, fromTable, fromSuffix);
        }
        boolean aDoorIsClosest = false;
        if (theObject != null && best != null) {
            if (best.c.dist(plc) < theObject.rc.dist(plc))
                aDoorIsClosest = true;
        } else if (theObject == null && best != null) {
            aDoorIsClosest = true;
        } else if (theObject != null && best == null) {
            aDoorIsClosest = false;
        } else if (theObject == null && best == null) {
            return;
        }
        if (aDoorIsClosest) {
            try {
                gui.map.wdgmsg("click", best.s, best.c.floor(posres), 3, 0, 0,
                        (int) best.g, best.c.floor(posres), 0, best.m);
                if (gui.interactWithNearestObjectThread != null) {
                    gui.interactWithNearestObjectThread.interrupt();
                    gui.interactWithNearestObjectThread = null;
                }
            } catch (Exception ignored) {}
        } else {
            if (OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.a) {
                gui.map.switchBunnySlippersAndPlateBoots(theObject);
            }
            gui.map.wdgmsg("click", Coord.z, theObject.rc.floor(posres), 3, 0, 0, (int) theObject.id, theObject.rc.floor(posres), 0, -1);
            if (gui.interactWithNearestObjectThread != null) {
                gui.interactWithNearestObjectThread.interrupt();
                gui.interactWithNearestObjectThread = null;
            }
        }
    }


    public Target nearestFromBuildings(double r) {
        if (r <= 0) r = 1024.0;
        Coord2d plc = gui.map.player().rc;
        Target best = null;
        for (Gob gob : Utils.getAllGobs(gui)) {
            try {
                Resource res = gob.getres();
                if (res == null) continue;
                List<Door> doors = BUILDINGS.get(res.name);
                if (doors == null) continue;
                for (Door d : doors) {
                    Coord2d c = gob.rc.add(d.rc.rotate(gob.a));
                    if (c.dist(plc) < r && (best == null || c.dist(plc) < best.c.dist(plc)))
                        best = new Target(c, Coord.z, gob.id, d.id);
                }
            } catch (Loading ignored) {}
        }
        return best;
    }

    public Gob nearestBySuffix(double r) {
        if (r <= 0) r = 1024.0;
        Coord2d plc = gui.map.player().rc;
        Gob best = null;
        double bestd = Double.MAX_VALUE;

        for (Gob gob : Utils.getAllGobs(gui)) {
            try {
                Resource res = gob.getres();
                if (res == null) continue;
                if ("gfx/terobjs/arch/greathall-door".equals(res.name)) continue;
                boolean match = false;
                for (String s : SUFFIXES) { if (res.name.endsWith(s)) { match = true; break; } }
                if (!match) continue;
                double d = gob.rc.dist(plc);
                if (d < r && d < bestd) { best = gob; bestd = d; }
            } catch (Loading ignored) {}
        }
        return best;
    }

    public static Target minByDist(Coord2d plc, Target... ts) {
        Target best = null;
        for (Target t : ts) if (t != null && (best == null || t.c.dist(plc) < best.c.dist(plc))) best = t;
        return best;
    }

    public static class Door {
        public final Coord2d rc;
        public final int id;
        public Door(Coord2d rc, int id) { this.rc = rc; this.id = id; }
    }

    public static class Target {
        public final Coord2d c;
        public final Coord s;
        public final long g;
        public final int m;
        public Target(Coord2d c, Coord s, long g, int m) { this.c = c; this.s = s; this.g = g; this.m = m; }
    }

    public static final List<String> SUFFIXES = Arrays.asList(
            "-door"
    );

    public static final Map<String, List<Door>> BUILDINGS = new HashMap<>();
    static {
        BUILDINGS.put("gfx/terobjs/arch/logcabin", Arrays.asList(new Door(new Coord2d(22, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/timberhouse", Arrays.asList(new Door(new Coord2d(33, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/stonestead", Arrays.asList(new Door(new Coord2d(44, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/stonemansion", Arrays.asList(new Door(new Coord2d(48, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/greathall", Arrays.asList(
                new Door(new Coord2d(77, -28), 18),
                new Door(new Coord2d(77, 0), 17),
                new Door(new Coord2d(77, 28), 16)
        ));
        BUILDINGS.put("gfx/terobjs/arch/stonetower", Arrays.asList(new Door(new Coord2d(36, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/windmill", Arrays.asList(new Door(new Coord2d(0, 28), 16)));
        BUILDINGS.put("gfx/terobjs/arch/greathall-door", Arrays.asList(
                new Door(new Coord2d(0, -30), 18),
                new Door(new Coord2d(0, 0), 17),
                new Door(new Coord2d(0, 30), 16)
        ));
        BUILDINGS.put("gfx/terobjs/arch/greenhouse", Arrays.asList(new Door(new Coord2d(22, 0), 16)));
        BUILDINGS.put("gfx/terobjs/arch/stonehut", Arrays.asList(new Door(new Coord2d(20, 0), 16)));
    }


}
