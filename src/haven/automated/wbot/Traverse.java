package haven.automated;

import haven.Coord;
import haven.Coord2d;
import haven.GameUI;
import haven.Gob;
import haven.Loading;
import static haven.OCache.posres;
import haven.Resource;
import java.util.ArrayList;
import java.util.Arrays;

public class Traverse implements Runnable {

    public class doorShiftData {
        public String gobName;
        public ArrayList<doors> doorList;

        public doorShiftData(String gn, ArrayList<doors> dl) {
            gobName = gn;
            doorList = dl;
        }
    }

    public class doors {
        public Coord2d meshRC;
        public int meshID;

        public doors(Coord2d c, int id) {
            meshRC = c;
            meshID = id;
        }
    }

    public class target {
        public Coord2d c;
        public Coord s;
        public long g;
        public int m;

        public target(Coord2d ic, Coord is, long ig, int im) {
            c = ic;
            s = is;
            g = ig;
            m = im;
        }
    }

    private ArrayList<String> gobNameSuffix = new ArrayList<String>(Arrays.asList(
            "-door",
            "ladder",
            "upstairs",
            "downstairs",
            "cellarstairs",
            "cavein",
            "caveout"
    ));

    public ArrayList<doorShiftData> buildings = new ArrayList<doorShiftData>(Arrays.asList(
            new doorShiftData("gfx/terobjs/arch/logcabin", new ArrayList<doors>(Arrays.asList(
                    new doors(new Coord2d(22, 0), 16)
            ))),
            new doorShiftData("gfx/terobjs/arch/timberhouse", new ArrayList<doors>(Arrays.asList(
                    new doors(new Coord2d(33, 0), 16)
            ))),
            new doorShiftData("gfx/terobjs/arch/stonestead", new ArrayList<doors>(Arrays.asList(
                    new doors(new Coord2d(44, 0), 16)
            ))),
            new doorShiftData("gfx/terobjs/arch/stonemansion", new ArrayList<doors>(Arrays.asList(
                    new doors(new Coord2d(48, 0), 16)
            ))),
            new doorShiftData("gfx/terobjs/arch/greathall", new ArrayList<doors>(Arrays.asList(
                    new doors(new Coord2d(77, -28), 18),
                    new doors(new Coord2d(77, 0), 17),
                    new doors(new Coord2d(77, 28), 16)
            ))),
            new doorShiftData("gfx/terobjs/arch/stonetower", new ArrayList<doors>(Arrays.asList(
                    new doors(new Coord2d(36, 0), 16)
            ))),
            new doorShiftData("gfx/terobjs/arch/windmill", new ArrayList<doors>(Arrays.asList(
                    new doors(new Coord2d(0, 28), 16)
            ))),
            new doorShiftData("gfx/terobjs/arch/greathall-door", new ArrayList<doors>(Arrays.asList(
                    new doors(new Coord2d(0, -30), 18),
                    new doors(new Coord2d(0, 0), 17),
                    new doors(new Coord2d(0, 30), 16)
            )))
    ));

	private GameUI gui;
	private WBotUtils wBotUtils;

    public Traverse(GameUI gui) {
        this.gui = gui;
		this.wBotUtils = new WBotUtils(gui);
    }

    @Override
    public void run() {
        Gob player = gui.map.player();
        if (player == null) return;
        Coord2d plc = player.rc;
        target targetDoor = getTarget(gui, buildings, 40 * 11);
        Gob targetGob = getGob(gui, gobNameSuffix, 40 * 11);
        if ((targetDoor == null) && (targetGob == null))
            return;
        if (targetGob != null)
            if ((targetDoor == null) || (targetGob.rc.dist(plc) < targetDoor.c.dist(plc))) //if no door is found or another gob is closer
                targetDoor = new target(targetGob.rc, Coord.z, targetGob.id, -1);

        try {
            gui.map.wdgmsg("click", Coord.z, targetDoor.c.floor(posres), 3, 0, 0, (int) targetDoor.g, targetDoor.c.floor(posres), 0, targetDoor.m);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    private target getTarget(GameUI gui, ArrayList<doorShiftData> b, double r) {
        Coord2d plc = gui.map.player().rc;
        target result = null;
        ArrayList<target> targetList = new ArrayList<target>();
        if (r == 0) r = 1024.0;
        for (Gob gob : wBotUtils.getGobsInRadius(1000)) {
            try {
                Resource res = gob.getres();

                if (res == null)
                    continue;
                if (!res.name.startsWith("gfx/terobjs/arch/"))
                    continue;

                for (doorShiftData bld : b) {
                    if (bld.gobName.equals(res.name)) {
                        for (doors drs : bld.doorList) {
                            targetList.add(new target(
                                    gob.rc.add(drs.meshRC.rotate(gob.a)),
                                    Coord.z,
                                    gob.id,
                                    drs.meshID
                            ));
                        }
                    }
                }
            } catch (Loading l) {
                l.printStackTrace();
            }
        }
        for (target t : targetList) {
            try {
                if ((result == null) || (t.c.dist(plc) < result.c.dist(plc)))
                    result = t;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private Gob getGob(GameUI gui, ArrayList<String> gobNameAL, double maxrange) {
        Coord2d plc = gui.map.player().rc;
        Gob result = null;
        if (maxrange == 0) maxrange = 1024.0;
        for (Gob gob : wBotUtils.getGobsInRadius(1000)) {
            try {
                if (gob.getres() == null)
                    continue;
                boolean skipGob = true;
                for (String n : gobNameAL)
                    if ((gob.getres().name.endsWith(n)) && (!gob.getres().name.endsWith("gfx/terobjs/arch/greathall-door")))
                        skipGob = false;
                if (skipGob) continue;
                if ((result == null || gob.rc.dist(plc) < result.rc.dist(plc)) && gob.rc.dist(plc) < maxrange)
                    result = gob;
            } catch (Loading l) {
                l.printStackTrace();
            }
        }
        return result;
    }
}