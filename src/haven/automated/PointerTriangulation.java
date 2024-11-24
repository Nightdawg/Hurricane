package haven.automated;

import haven.*;
import haven.sprites.ClueSprite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;

public class PointerTriangulation extends Window {
    public static boolean pointerChecked = false;
    public static double pointerAngle = 0;
    private final GameUI gui;
    private List<LineData> lines;

    public PointerTriangulation(GameUI gui) {
        super(UI.scale(100, 100), "Pointer Triangulation");
        this.gui = gui;
        this.lines = new ArrayList<>();

        Widget prev;
        prev = add(new Label("Remember: To triangulate a different quest giver, just reopen this window to reset it."), 0, 0);
        prev = add(new Button(UI.scale(160), "Get Pointer Location") {
            @Override
            public void click() {
                saveCheckpoint();
            }
        }, prev.pos("bl").adds(32, 10));
        prev.tooltip = instructions;

        prev = add(new Button(UI.scale(160), "Draw Lines") {
            @Override
            public void click() {
                drawLines();
            }
        }, prev.pos("ur").adds(10, 0));
        prev.tooltip = instructions;

        pack();
    }

    public void drawLines() {
        if(gui.map.player() == null){
            return;
        }
        try {
            for (LineData lineData : lines) {
                Coord playerCoord = ui.gui.map.player().rc.floor(tilesz);
                MCache.Grid grid = ui.sess.glob.map.getgrid(playerCoord.div(cmaps));
                MapFile.GridInfo info = ui.gui.mapfile.file.gridinfo.get(grid.id);
                MapFile.Segment segment = ui.gui.mapfile.file.segments.get(info.seg);
                if (segment.id == lineData.segmentId) {
                    Coord gridCoords = null;
                    Coord curGridCoords = null;
                    for (Map.Entry<Coord, Long> segGrid : segment.map.entrySet()) {
                        if (Objects.equals(segGrid.getValue(), lineData.gridId)) {
                            gridCoords = segGrid.getKey();
                        }
                        if (segGrid.getValue().equals(grid.id)) {
                            curGridCoords = segGrid.getKey();
                        }
                        if (gridCoords != null && curGridCoords != null) {
                            break;
                        }
                    }
                    double calcX = gridCoords.x - curGridCoords.x;
                    double calcY = gridCoords.y - curGridCoords.y;
                    Coord2d firstCoord = new Coord2d(calcX * 1100 + grid.gc.x * 1100 + lineData.initCoords.x, calcY * 1100 + grid.gc.y * 1100 + lineData.initCoords.y);
                    gui.mapfile.view.addSprite(new ClueSprite(firstCoord, lineData.angle, lineData.angle, 1, 10000, 10));

                } else {
                    gui.error("You have to be in the same segment for each pointer");
                    return;
                }
            }
        } catch (Exception e) {
            gui.error("Something went wrong.");
        }
    }

    public void saveCheckpoint() {
        if(gui.map.player() == null){
            gui.error("Questgiver Triangulation: I don't see you on the screen, can't calculate angle.");
            return;
        }
        if (!pointerChecked){
            gui.error("Questgiver Triangulation: You need to mouse over the pointer first. Read the instructions.");
            return;
        }
        try {
            Gob player = gui.map.player();
            if(player == null){
                return;
            }
            Coord2d playerCoord = player.rc;
            Coord initialCoord = playerCoord.floor(MCache.tilesz);
            MCache.Grid grid = ui.sess.glob.map.getgrid(initialCoord.div(cmaps));
            MapFile.GridInfo info = ui.gui.mapfile.file.gridinfo.get(grid.id);
            Long segment = info.seg;
            double xValue = Math.floor((playerCoord.x - (grid.gc.x * 1100)) * 100) / 100;
            double yValue = Math.floor((playerCoord.y - (grid.gc.y * 1100)) * 100) / 100;
            lines.add(new LineData(segment, grid.id, new Coord2d(xValue, yValue), pointerAngle));
        } catch (Exception ignored) {}
        pointerChecked = false;
    }

    private static class LineData {
        private final Long segmentId;
        private final Long gridId;
        private final Coord2d initCoords;
        private final double angle;

        public LineData(Long segmentId, Long gridId, Coord2d initCoords, double angle) {
            this.segmentId = segmentId;
            this.gridId = gridId;
            this.initCoords = initCoords;
            this.angle = angle;
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            reqdestroy();
            gui.pointerTriangulation = null;
        } else
            super.wdgmsg(sender, msg, args);
    }

    private final Object instructions = RichText.render("How to use:\n" +
            "1. Mouse over the Quest Giver's Pointer\n" +
            "2. Click on \"Get Pointer Location\"\n" +
            "3. Move to a new location (use a road for example)\n" +
            "4. Mouse over the quest giver pointer again\n" +
            "5. Click on \"Get Pointer Location\"\n" +
            "6. Click on \"Draw Lines\"\n" +
            "\n" +
            "Now 2 lines are drawn on your map which can be used to triangulate the location.\n" +
            "\n" +
            "OBVIOUSLY, you need to be in the same map segment when you do this. If you travel to some road and you don't have your map connected, it won't work.\n" +
            "\n" +
            "Note: You can do multiple lines, not just two.\n", 300);

    @Override
    public void reqdestroy() {
        Utils.setprefc("wndc-pointerTriangulationWindow", this.c);
        super.reqdestroy();
    }
}
