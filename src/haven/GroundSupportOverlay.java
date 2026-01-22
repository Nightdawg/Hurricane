package haven;

import haven.render.*;
import java.awt.Color;
import java.util.*;

public class GroundSupportOverlay implements MCache.OverlayInfo {
    public static final String TAG = "groundsupport";
    private static GroundSupportOverlay instance = null;
    public static Material material = new Material(new BaseColor(OptWnd.safeTilesColorOptionWidget.currentColor),
            new States.Depthtest(States.Depthtest.Test.LE));
    public static Material outlineMaterial = new Material(new BaseColor(new Color(OptWnd.safeTilesColorOptionWidget.currentColor.getRed(), OptWnd.safeTilesColorOptionWidget.currentColor.getGreen(), OptWnd.safeTilesColorOptionWidget.currentColor.getBlue(), 255)),
            States.Depthtest.none, States.maskdepth);

    private final Set<Coord> highlightedTiles = Collections.synchronizedSet(new HashSet<>());
    private MCache map = null;

    private GroundSupportOverlay() {}

    public static GroundSupportOverlay getInstance() {
        if (instance == null) {
            instance = new GroundSupportOverlay();
        }
        return instance;
    }

    public void setMap(MCache map) {
        this.map = map;
    }

    @Override
    public Collection<String> tags() {
        return Collections.singleton(TAG);
    }

    @Override
    public Material mat() {
        return material;
    }

    @Override
    public Material omat() {
        return outlineMaterial;
    }


    public void addTilesInRadius(Coord2d supportPos, double radiusInGameUnits) {
        int tilesToCheck = (int) Math.ceil(radiusInGameUnits / MCache.tilesz.x) + 1;
        Coord supportTile = supportPos.floor(MCache.tilesz);

        for (int dx = -tilesToCheck; dx <= tilesToCheck; dx++) {
            for (int dy = -tilesToCheck; dy <= tilesToCheck; dy++) {
                Coord tileCoord = supportTile.add(dx, dy);

                Coord2d tileCenter = new Coord2d(
                    tileCoord.x * MCache.tilesz.x + MCache.tilesz.x / 2.0,
                    tileCoord.y * MCache.tilesz.y + MCache.tilesz.y / 2.0
                );

                double distance = tileCenter.dist(supportPos);
                if (distance <= radiusInGameUnits) {
                    highlightedTiles.add(tileCoord);
                }
            }
        }
        invalidateCache();
    }


    public boolean isTileHighlighted(Coord tileCoord) {
        return highlightedTiles.contains(tileCoord);
    }

    public void clear() {
        highlightedTiles.clear();
        invalidateCache();
    }

    public int getTileCount() {
        return highlightedTiles.size();
    }

    private void invalidateCache() {
        if (map != null) {
            map.olseq++;
        }
    }
}
