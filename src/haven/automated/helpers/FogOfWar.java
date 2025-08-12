package haven.automated.helpers;

import haven.Coord;

public class FogOfWar {
    public static final java.util.ArrayList<long[]> seen = new java.util.ArrayList<>();
    private static final java.util.HashSet<Long> keys = new java.util.HashSet<>();

    public static void snapshot(haven.Gob player) {
        if (player == null) return;

        haven.Coord2d sgsz = new haven.Coord2d(new Coord(100, 100));

        haven.Coord gidx = player.rc.floor(sgsz);
        int tlgx = gidx.x - 4;
        int tlgy = gidx.y - 4;

        long k = (((long)tlgx) << 32) ^ (tlgy & 0xffffffffL);
        if (keys.add(k)) {
            seen.add(new long[] { tlgx, tlgy });
        }
    }

    public static void reset() {
        seen.clear();
        keys.clear();
    }
}