package haven.automated;

import haven.Coord;
import haven.GameUI;
import haven.Gob;

import static haven.OCache.posres;

public class PushPlayer implements Runnable {
    private final GameUI gui;

    public PushPlayer(GameUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        Gob nearestPlayer = null;
        synchronized (gui.map.glob.oc) {
            for (Gob gob : gui.map.glob.oc) {
                if (isPlayer(gob) && !gob.isFriend()) {
                    double distFromPlayer = gob.rc.dist(gui.map.player().rc);
                    if (distFromPlayer <= 20 * 4 && (nearestPlayer == null || distFromPlayer < nearestPlayer.rc.dist(gui.map.player().rc)))
                        nearestPlayer = gob;
                }
            }
        }
        if (nearestPlayer == null)
            return;

        gui.act("push");

        gui.map.wdgmsg("click", Coord.z, nearestPlayer.rc.floor(posres), 1, 0, 0, (int) nearestPlayer.id, nearestPlayer.rc.floor(posres), 0, -1);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            return;
        }
        gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 3, 0);
    }

    private boolean isPlayer(Gob gob){
        return gob.getres() != null && gob.getres().name != null && gob.getres().name.equals("gfx/borka/body");
    }
}
