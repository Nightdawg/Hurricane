package haven.automated;


import haven.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import static haven.OCache.posres;
import static haven.automated.AUtils.potentialAggroTargets;

public class AggroOrTargetCursorNearest implements Runnable {
    private final GameUI gui;

    public AggroOrTargetCursorNearest(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
            attackClosestAttackable();

    }

    private void attackClosestAttackable() {
        gui.map.new Hittest(gui.map.currentCursorLocation) {
            protected void hit(Coord pc, Coord2d mc, ClickData inf) {
                Gob player = gui.map.player();
                if (player == null)
                    return;
                if (inf != null) {
                    Long gobid = Long.valueOf((Integer) inf.clickargs()[1]);
                    Gob clickedGob = gui.map.glob.oc.getgob(gobid);
                    if (clickedGob != null) {
                        if (isPlayer(clickedGob)) {
                            if (!clickedGob.isFriend()) {
                                AUtils.attackGob(gui, clickedGob);
                            }
                            return;
                        }
                        if (potentialAggroTargets.contains(clickedGob.getres().name)) {
                            AUtils.attackGob(gui, clickedGob);
                        } else if (clickedGob.getres().name.equals("gfx/kritter/cattle/cattle")) { // ND: Special case for Aurochs
                            for (GAttrib g : clickedGob.attr.values()) {
                                if (g instanceof Drawable) {
                                    if (g instanceof Composite) {
                                        Composite c = (Composite) g;
                                        if (c.comp.cmod.size() > 0) {
                                            for (Composited.MD item : c.comp.cmod) {
                                                if (item.mod.get().basename().equals("aurochs")) {
                                                    AUtils.attackGob(gui, clickedGob);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (clickedGob.getres().name.equals("gfx/kritter/sheep/sheep")) { // ND: Special case for Mouflon
                            for (GAttrib g : clickedGob.attr.values()) {
                                if (g instanceof Drawable) {
                                    if (g instanceof Composite) {
                                        Composite c = (Composite) g;
                                        if (c.comp.cmod.size() > 0) {
                                            for (Composited.MD item : c.comp.cmod) {
                                                if (item.mod.get().basename().equals("mouflon")) {
                                                    AUtils.attackGob(gui, clickedGob);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                HashMap<Long, Gob> allAttackableMap = AUtils.getAllAttackableMap(gui);
                // try and find the closest animal or player to attack
                Gob closestEnemy = null;
                OUTER_LOOP:
                for (Gob gob : allAttackableMap.values()) {
                    if (isPlayer(gob) && gob.isFriend()) {
                        continue;
                    }
                    if (gob.getres().name.equals("gfx/kritter/horse/horse") && gob.occupants.size() > 0) { // ND: Wild horse special case. Tamed horses are never attacked anyway
                        for (Gob occupant : gob.occupants) {
                            if (occupant.isFriend() || occupant.isItMe()) {
                                continue OUTER_LOOP;
                            }
                        }
                    }
                    //if gob is an enemy player and not already aggroed
                    if ((closestEnemy == null || gob.rc.dist(mc) < closestEnemy.rc.dist(mc))
                            && (gob.knocked == null || (gob.knocked != null && !gob.knocked))) { // ND: Retarded workaround that I need to add, just like in Gob.java
                        closestEnemy = gob;
                    }
                }

                if (closestEnemy != null) {
                    AUtils.attackGob(gui, closestEnemy);
                }

            }
        }.run();
    }

    private boolean isPlayer(Gob gob){
        return gob.getres() != null && gob.getres().name != null && gob.getres().name.equals("gfx/borka/body");
    }
}
