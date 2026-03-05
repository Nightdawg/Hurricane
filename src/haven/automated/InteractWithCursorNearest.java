package haven.automated;


import haven.*;

import java.util.*;

import static haven.OCache.posres;

public class InteractWithCursorNearest implements Runnable {
    private GameUI gui;

    public InteractWithCursorNearest(GameUI gui) {
        this.gui = gui;
    }


    double maxDistance = 12 * 11;
    @Override
    public void run() {
        gui.map.new Hittest(gui.map.currentCursorLocation) {
            protected void hit(Coord pc, Coord2d mc, ClickData inf) {
                Gob player = gui.map.player();
                if (player == null)
                    return; // player is null, possibly taking a road, don't bother trying to do any of the below
                if (inf != null) {
                    Long gobid = Long.valueOf((Integer) inf.clickargs()[1]);
                    Gob clickedGob = gui.map.glob.oc.getgob(gobid);
                    if (clickedGob != null) {
                        Resource res = null;
                        try {
                            res = clickedGob.getres();
                        } catch (Loading l) {
                        }
                        if (res != null) {
                            // Open nearby gates, but not visitor gates
                            boolean isSmallGate = InteractWithNearestObject.smallGates.contains(res.basename());
                            boolean isReinforcedGate = InteractWithNearestObject.reinforcedGates.contains(res.basename());
                            try {
                                if (isReinforcedGate) {
                                    if (gui.genus.equals("b7c199a4557503a8")) {
                                        isReinforcedGate = false;
                                    } else {
                                        for (Gob.Overlay ol : clickedGob.ols) {
                                            String oname = ol.spr.res.name;
                                            if (oname.equals("gfx/fx/eq"))
                                                isReinforcedGate = false;
                                        }
                                    }
                                }
                            } catch (NullPointerException ignored) {}
                            boolean isNonVisitorGate = isSmallGate || isReinforcedGate;
                            if ((isNonVisitorGate && Utils.getprefb("clickNearestObject_NonVisitorGates", true))
                            || ((res.name.startsWith("gfx/terobjs/herbs") || InteractWithNearestObject.otherPickableObjects.contains(res.basename())) && Utils.getprefb("clickNearestObject_Forageables", true))
                            || (Arrays.stream(Config.critterResPaths).anyMatch(res.name::matches) || res.name.matches(".*(rabbit|bunny)$")) && Utils.getprefb("clickNearestObject_Critters", true)
                            || (InteractWithNearestObject.caves.contains(res.name) && Utils.getprefb("clickNearestObject_Caves", false))
                            || (InteractWithNearestObject.mines.contains(res.name) && Utils.getprefb("clickNearestObject_MineholesAndLadders", false))) {
                                if (res.name.startsWith("gfx/terobjs/herbs")) FlowerMenu.setNextSelection("Pick"); // ND: Set the flower menu option to "pick" only for these particular ones.
                                if (OptWnd.autoEquipBunnySlippersPlateBootsCheckBox.a) {
                                    gui.map.switchBunnySlippersAndPlateBoots(clickedGob);
                                }
                                gui.map.wdgmsg("click", Coord.z, clickedGob.rc.floor(posres), 3, 0, 0, (int) clickedGob.id, clickedGob.rc.floor(posres), 0, -1);
                                if (gui.interactWithNearestObjectThread != null) {
                                    gui.interactWithNearestObjectThread.interrupt();
                                    gui.interactWithNearestObjectThread = null;
                                }
                                return;
                            }
                        }
                    }
                }
                Gob theObject = null;
                for (Gob gob : Utils.getAllGobs(gui)) {
                    double distFromPlayer = gob.rc.dist(mc);
                    if (gob.id == gui.map.plgob || distFromPlayer >= maxDistance)
                        continue;
                    Resource res = null;
                    try {
                        res = gob.getres();
                    } catch (Loading l) {
                    }
                    if (res != null) {
                        // Open nearby gates, but not visitor gates
                        boolean isGate = InteractWithNearestObject.smallGates.contains(res.basename());
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
                                || ((res.name.startsWith("gfx/terobjs/herbs") || InteractWithNearestObject.otherPickableObjects.contains(res.basename())) && Utils.getprefb("clickNearestObject_Forageables", true))
                                || (Arrays.stream(Config.critterResPaths).anyMatch(res.name::matches) || res.name.matches(".*(rabbit|bunny)$")) && Utils.getprefb("clickNearestObject_Critters", true)
                                || (InteractWithNearestObject.caves.contains(res.name) && Utils.getprefb("clickNearestObject_Caves", false))
                                || (InteractWithNearestObject.mines.contains(res.name) && Utils.getprefb("clickNearestObject_MineholesAndLadders", false))) {
                            if (distFromPlayer < maxDistance && (theObject == null || distFromPlayer < theObject.rc.dist(mc))) {
                                theObject = gob;
                                if (res.name.startsWith("gfx/terobjs/herbs")) FlowerMenu.setNextSelection("Pick"); // ND: Set the flower menu option to "pick" only for these particular ones.
                            }
                        }
                    }
                }
                InteractWithNearestObject.Target best = null;
                if (Utils.getprefb("clickNearestObject_Doors", true)) {
                    InteractWithNearestObject.Target fromTable = nearestFromBuildings(maxDistance, mc);
                    Gob suffixGob = nearestBySuffix(maxDistance, mc);
                    InteractWithNearestObject.Target fromSuffix = (suffixGob == null) ? null : new InteractWithNearestObject.Target(suffixGob.rc, Coord.z, suffixGob.id, -1);

                    best = InteractWithNearestObject.minByDist(mc, fromTable, fromSuffix);
                }
                boolean aDoorIsClosest = false;
                if (theObject != null && best != null) {
                    if (best.c.dist(mc) < theObject.rc.dist(mc))
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
        }.run();
    }

    public InteractWithNearestObject.Target nearestFromBuildings(double r, Coord2d mc) {
        if (r <= 0) r = 1024.0;
        InteractWithNearestObject.Target best = null;
        for (Gob gob : Utils.getAllGobs(gui)) {
            try {
                Resource res = gob.getres();
                if (res == null) continue;
                List<InteractWithNearestObject.Door> doors = InteractWithNearestObject.BUILDINGS.get(res.name);
                if (doors == null) continue;
                for (InteractWithNearestObject.Door d : doors) {
                    Coord2d c = gob.rc.add(d.rc.rotate(gob.a));
                    if (c.dist(mc) < r && (best == null || c.dist(mc) < best.c.dist(mc)))
                        best = new InteractWithNearestObject.Target(c, Coord.z, gob.id, d.id);
                }
            } catch (Loading ignored) {}
        }
        return best;
    }

    public Gob nearestBySuffix(double r, Coord2d mc) {
        if (r <= 0) r = 1024.0;
        Gob best = null;
        double bestd = Double.MAX_VALUE;

        for (Gob gob : Utils.getAllGobs(gui)) {
            try {
                Resource res = gob.getres();
                if (res == null) continue;
                if ("gfx/terobjs/arch/greathall-door".equals(res.name)) continue;
                boolean match = false;
                for (String s : InteractWithNearestObject.SUFFIXES) { if (res.name.endsWith(s)) { match = true; break; } }
                if (!match) continue;
                double d = gob.rc.dist(mc);
                if (d < r && d < bestd) { best = gob; bestd = d; }
            } catch (Loading ignored) {}
        }
        return best;
    }
}
