package haven.automated.pathfinder;


import haven.*;
import haven.automated.helpers.HitBoxes;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static haven.OCache.posres;

public class Pathfinder implements Runnable {
    private OCache oc;
    private MCache map;
    private MapView mv;
    private Coord dest;
    public boolean terminate = false;
    public boolean moveinterupted = false;
    private int meshid;
    private int clickb;
    private Gob gob;
    private String action;
    public Coord mc;
    private int modflags;
    private int interruptedRetries = 5;
    private static final int RESPONSE_TIMEOUT = 800;

    public Pathfinder(MapView mv, Coord dest, String action) {
        this.dest = dest;
        this.action = action;
        this.oc = mv.glob.oc;
        this.map = mv.glob.map;
        this.mv = mv;
    }

    public Pathfinder(MapView mv, Coord dest, Gob gob, int meshid, int clickb, int modflags, String action) {
        this.dest = dest;
        this.meshid = meshid;
        this.clickb = clickb;
        this.gob = gob;
        this.modflags = modflags;
        this.action = action;
        this.oc = mv.glob.oc;
        this.map = mv.glob.map;
        this.mv = mv;
    }

    private final Set<PFListener> listeners = new CopyOnWriteArraySet<PFListener>();
    public final void addListener(final PFListener listener) {
        listeners.add(listener);
    }

    public final void removeListener(final PFListener listener) {
        listeners.remove(listener);
    }

    private final void notifyListeners() {
        for (PFListener listener : listeners) {
            listener.pfDone(this);
        }
    }

    @Override
    public void run() {
        do {
            moveinterupted = false;
            pathfind(mv.player().rc.floor());
        } while (moveinterupted && !terminate);
        notifyListeners();
    }

    public void pathfind(Coord src) {
        long starttotal = System.nanoTime();
        Map m = new Map(src, dest, map);
        Gob player = mv.player();

        long start = System.nanoTime();
        synchronized (oc) {
            for (Gob gob : oc) {
                if (gob.isPlgob(this.mv.ui.gui)) {
                    continue;
                }
                if (this.gob != null && this.gob.id == gob.id) {
                    continue;
                }
                if (gob.getres() != null && isInsideBoundBox(gob, player.rc.floor())) {
                    for (HitBoxes.CollisionBox collisionBox : HitBoxes.getCollisionBoxes(gob)) {
                        if (collisionBox.hitAble) {
                            if (collisionBox.coords.length > 2) {
                                double minX = Double.MAX_VALUE;
                                double minY = Double.MAX_VALUE;
                                double maxX = Double.MIN_VALUE;
                                double maxY = Double.MIN_VALUE;

                                for (Coord2d coord : collisionBox.coords) {
                                    minX = Math.min(minX, coord.x);
                                    minY = Math.min(minY, coord.y);
                                    maxX = Math.max(maxX, coord.x);
                                    maxY = Math.max(maxY, coord.y);
                                }
                                Coord2d topLeft = new Coord2d(minX, minY);
                                Coord2d bottomRight = new Coord2d(maxX, maxY);
                                m.excludeGob(topLeft.floor(), bottomRight.floor(), gob);
                            }
                        }
                    }
                }
                m.analyzeGobHitBoxes(gob);
            }
        }

        // if player is located at a position occupied by a gob (can happen when starting too close to gobs)
        // move it slightly away from it
        if (m.isOriginBlocked()) {
            Pair<Integer, Integer> freeloc = m.getFreeLocation();

            if (freeloc == null) {
                terminate = true;
                m.dbgdump();
                return;
            }

            mc = new Coord2d(src.x + freeloc.a - Map.origin, src.y + freeloc.b - Map.origin).floor(posres);
            mv.wdgmsg("click", Coord.z, mc, 1, 0);

            // FIXME
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // need to recalculate map
            moveinterupted = true;
            m.dbgdump();
            return;
        }

        // exclude any bounding boxes overlapping the destination gob
        if (this.gob != null) {
            for (HitBoxes.CollisionBox collisionBox : HitBoxes.getCollisionBoxes(gob)) {
                if (collisionBox.hitAble) {
                    if (collisionBox.coords.length > 2) {
                        double minX = Double.MAX_VALUE;
                        double minY = Double.MAX_VALUE;
                        double maxX = Double.MIN_VALUE;
                        double maxY = Double.MIN_VALUE;

                        for (Coord2d coord : collisionBox.coords) {
                            minX = Math.min(minX, coord.x);
                            minY = Math.min(minY, coord.y);
                            maxX = Math.max(maxX, coord.x);
                            maxY = Math.max(maxY, coord.y);
                        }
                        Coord2d topLeft = new Coord2d(minX, minY);
                        Coord2d bottomRight = new Coord2d(maxX, maxY);
                        m.excludeGob(topLeft.floor(), bottomRight.floor(), this.gob);
                    }
                }
            }
        }

        if (Map.DEBUG_TIMINGS)
            System.out.println("      Gobs Processing: " + (double) (System.nanoTime() - start) / 1000000.0 + " ms.");

        Iterable<Edge> path = m.main();
        if (Map.DEBUG_TIMINGS)
            System.out.println("--------------- Total: " + (double) (System.nanoTime() - starttotal) / 1000000.0 + " ms.");

        m.dbgdump();
        //System.out.println("path length: " + path.spliterator().getExactSizeIfKnown());
        Iterator<Edge> it = path.iterator();
        while (it.hasNext() && !moveinterupted && !terminate) {
            Edge e = it.next();

            mc = new Coord2d(src.x + e.dest.x - Map.origin, src.y + e.dest.y - Map.origin).floor(posres);
            //System.out.println("moving to mc: " + mc);

            if (action != null && !it.hasNext())
                mv.ui.gui.act(action);

            if (gob != null && !it.hasNext())
                mv.wdgmsg("click", Coord.z, mc, clickb, modflags, 0, (int) gob.id, gob.rc.floor(posres), 0, meshid);
            else
                mv.wdgmsg("click", Coord.z, mc, 1, 0);

            // wait for gob to start moving
            long moveWaitStart = System.currentTimeMillis();
            while (!player.isMoving() && !terminate) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e1) {
                    return;
                }
                if (System.currentTimeMillis() - moveWaitStart > RESPONSE_TIMEOUT)
                    return;
            }

            // wait for it to finish
            while (!moveinterupted && !terminate) {
                if (!player.isMoving()) {
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e1) {
                        return;
                    }
                    if (!player.isMoving())
                        break;
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e1) {
                    return;
                }

                long now = System.currentTimeMillis();

                // FIXME
                // when right clicking gobs, char will try to navigate towards gob's rc
                // however he will be blocked by gob's bounding box.
                // therefore we just wait for a bit
                LinMove lm = player.getLinMove();
                if (gob != null && !it.hasNext() && lm != null && now - lm.lastupd > 500)
                    break;
            }

            if (moveinterupted) {
                interruptedRetries--;
                if (interruptedRetries == 0)
                    terminate = true;
                m.dbgdump();
                return;
            }
        }
        terminate = true;
    }


    public static boolean isInsideBoundBox(Gob gob, Coord point) {
        double gobA = gob.a;
        Coord rc = gob.rc.floor();
        for (HitBoxes.CollisionBox collisionBox : HitBoxes.getCollisionBoxes(gob)) {
            if (collisionBox.hitAble) {
                if (collisionBox.coords.length > 3) {
                    double minX = Double.MAX_VALUE;
                    double minY = Double.MAX_VALUE;
                    double maxX = Double.MIN_VALUE;
                    double maxY = Double.MIN_VALUE;

                    for (Coord2d coord : collisionBox.coords) {
                        minX = Math.min(minX, coord.x);
                        minY = Math.min(minY, coord.y);
                        maxX = Math.max(maxX, coord.x);
                        maxY = Math.max(maxY, coord.y);
                    }
                    Coord2d topLeft = new Coord2d(minX, minY);
                    Coord2d bottomRight = new Coord2d(maxX, maxY);

                    final Coordf relative = new Coordf(point.sub(rc)).rotate(-gobA);
                    if (relative.x >= topLeft.x && relative.x <= bottomRight.x &&
                            relative.y >= topLeft.y && relative.y <= bottomRight.y) {
                        return true;
                    }

                }
                if (collisionBox.coords.length == 3) {
                    double minX = Double.MAX_VALUE;
                    double minY = Double.MAX_VALUE;
                    double maxX = Double.MIN_VALUE;
                    double maxY = Double.MIN_VALUE;

                    for (Coord2d coord : collisionBox.coords) {
                        if (coord.x < minX) {
                            minX = coord.x;
                        }
                        if (coord.y < minY) {
                            minY = coord.y;
                        }
                        if (coord.x > maxX) {
                            maxX = coord.x;
                        }
                        if (coord.y > maxY) {
                            maxY = coord.y;
                        }
                    }
                    Coord2d topLeft = new Coord2d(minX, minY);
                    Coord2d bottomRight = new Coord2d(maxX, maxY);
                    final Coordf relative = new Coordf(point.sub(rc)).rotate(-gobA);
                    if (relative.x >= topLeft.x && relative.x <= bottomRight.x &&
                            relative.y >= topLeft.y && relative.y <= bottomRight.y) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
