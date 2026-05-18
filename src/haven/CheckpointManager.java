package haven;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.*;
import java.util.List;
import java.util.*;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;


public class CheckpointManager extends Window implements Runnable {
    private static final String DATABASE = "jdbc:sqlite:saved_routes.db";
    private static final int delayMs = 250;
    private final GameUI gui;
    private final boolean stop = false;
    private boolean paused = true;
    private final Label estimatedArrivalTime;
    public CheckpointList checkpointList;
    private int notMovingCounter = 0;
    private final Button pause;
    private Coord2d lastPlayerCoord;
    private Button transformIntoArea;
    private Button reverseButton;
    private Button resizeButton;
    private boolean extendedView;
    private TextEntry routeNameInput;
    private String nameInput = "";
    private TextEntry selectFilterEntry;
    private String selectFilter = "";
    private Button saveButton;
    private Button loadButton;
    private Label searchLabel;
    private Label savedRoutesTitle;
    private Label routeNameLabel;
    private Label routeDistanceLabel;
    private Label routeRemoveLabel;
    private RouteList routeList;
    private final Text.Foundry savedRoutesTitleFoundry = new Text.Foundry(Text.sans, 14);

    static {
        try {
            createDatabaseIfNotExist();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public CheckpointManager(GameUI gui) {
        super(UI.scale(350, 200), "Queued Movement - Checkpoint Route");
        this.gui = gui;
        if(gui.map.player() != null){
            this.lastPlayerCoord = gui.map.player().rc;
        } else {
            this.lastPlayerCoord = new Coord2d(0,0);
        }

        add(new Label("Checkpoint"), UI.scale(20, 8));
        add(new Label("Coords"), UI.scale(106, 8));
        add(new Label("Add Checkpoint"), UI.scale(169, 1));
        add(new Label("behind this one"), UI.scale(171, 12));
        add(new Label("Remove"), UI.scale(296, 8));
        checkpointList = new CheckpointList(390, 5);
        add(checkpointList, UI.scale(5, 30));
        pause = add(new Button(UI.scale(50), "Start") {
            @Override
            public void click() {
                if(!paused && checkpointList.items.size() > 1){
                    checkpointList.setCurrentIndex(0);
                    if(gui.map.player() != null){
                        addCoord(gui.map.player().rc);
                    }
                }
                paused = !paused;
                this.change(paused ? "Start" : "Pause");
                ui.root.wdgmsg("gk", 27);
            }
        }, UI.scale(97, 184));

        // Add transform into area button
        transformIntoArea = add(new Button(UI.scale(80), "Area Convert") {
            @Override
            public void click() {
                transformIntoArea();
            }
        }, UI.scale(15, 184));
        transformIntoArea.tooltip = RichText.render("If you want to scan a specific area on the map, draw a polygon with checkpoints, then press this button to convert it before starting.", UI.scale(350));

        resizeButton = add(new Button(UI.scale(26), "▼") {
            @Override
            public void click() {
                toggleExtendWindow();
            }
        }, UI.scale(332, 184));
        resizeButton.tooltip = RichText.render("Show Routes Manager", UI.scale(350));

        this.c = new Coord(100, 100);

        add(estimatedArrivalTime = new Label(""), UI.scale(150, 184));

        reverseButton = add(new Button(UI.scale(26), "⭮") {
            @Override
            public void click() {
                checkpointList.reverseCheckpoints(gui);
            }
        }, UI.scale(-10, 184));
        reverseButton.tooltip = RichText.render("Reverse path.", UI.scale(350));
    }

    public void toggleExtendWindow() {
        // Reset inputs and text fields
        nameInput = "";
        selectFilter = "";
        if (routeNameInput != null) {
            routeNameInput.settext("");
        }
        if (selectFilterEntry != null) {
            selectFilterEntry.settext("");
        }

        if (extendedView) {
            // Collapsing operations

            // Remove existing widgets
            savedRoutesTitle.remove();
            routeNameInput.remove();
            selectFilterEntry.remove();
            saveButton.remove();
            loadButton.remove();
            searchLabel.remove();
            resizeButton.remove();
            routeList.removeAllItems();
            routeList.remove();
            routeNameLabel.remove();
            routeDistanceLabel.remove();
            routeRemoveLabel.remove();

            // Resize window
            this.resize(UI.scale(350), UI.scale(200));

            // Add resize button with ↓
            resizeButton = add(new Button(UI.scale(26), "▼") {
                @Override
                public void click() {
                    toggleExtendWindow();
                }
            }, UI.scale(332, 184));
            resizeButton.tooltip = RichText.render("Show Routes Manager", UI.scale(350));

        } else {
            // Expanding operations

            // Resize window
            this.resize(UI.scale(350), UI.scale(561));

            savedRoutesTitle = add(new Label("Routes Manager", savedRoutesTitleFoundry), UI.scale(124, 212));
            searchLabel = add(new Label("Name:"), UI.scale(20, 243));

            // Add select filter entry
            selectFilterEntry = add(new TextEntry(UI.scale(160), selectFilter) {
                @Override
                protected void changed() {
                    setSelectFilter(this.buf.line());
                }
            }, UI.scale(65, 240));

            // Add load button
            loadButton = add(new Button(UI.scale(100), "Search", false) {
                @Override
                public void click() {
                    loadSavedRoutes();
                    routeList.sb.val = 0;
                }
            }, UI.scale(237, 238));

            routeNameLabel = add(new Label("Route Name"), UI.scale(50, 274));
            routeDistanceLabel = add(new Label("Distance"), UI.scale(149, 274));
            routeRemoveLabel = add(new Label("Remove"), UI.scale(296, 274));

            // Add route name input
            routeNameInput = add(new TextEntry(200, nameInput) {
                @Override
                protected void changed() {
                    setNameInput(this.buf.line());
                }
            }, UI.scale(25, 521));

            // Add save button
            saveButton = add(new Button(UI.scale(100), "Save Route", false) {
                @Override
                public void click() {
                    saveCurrentRoute();
                }
            }, UI.scale(237, 519));

            // Remove existing resize button
            resizeButton.remove();

            // Add resize button with ↑
            resizeButton = add(new Button(UI.scale(26), "▲", false) {
                @Override
                public void click() {
                    toggleExtendWindow();
                    preventDraggingOutside(); // ND: Retard proofing.
                }
            }, UI.scale(332, 545));
            resizeButton.tooltip = RichText.render("Hide Routes Manager", UI.scale(350));

            routeList = new RouteList(390, 7);
            add(routeList, UI.scale(5, 295));
            loadSavedRoutes();
            routeList.sb.val = 0;
        }

        // Toggle the extendedView flag
        extendedView = !extendedView;
    }


    private void setNameInput(String input) {
        nameInput = input;
    }

    private void setSelectFilter(String input) {
        selectFilter = input;
    }

    private void launchRouteCheckpoints(int id){
        if(gui.map.player() == null){
            return;
        }
        String selectSql = "SELECT * FROM routes WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String[] initialData = rs.getString("initial_point").split(";");
                    long segmentId = Long.parseLong(initialData[0]);
                    long gridId = Long.parseLong(initialData[1]);
                    double xInitialOffset = Double.parseDouble(initialData[2]);
                    double yInitialOffset = Double.parseDouble(initialData[3]);
                    String[] arrayOfCheckpoints = rs.getString("route").split(";");
                    Coord playerCoord = ui.gui.map.player().rc.floor(tilesz);
                    MCache.Grid grid = ui.sess.glob.map.getgrid(playerCoord.div(cmaps));
                    MapFile.GridInfo info = ui.gui.mapfile.file.gridinfo.get(grid.id);
                    MapFile.Segment segment = ui.gui.mapfile.file.segments.get(info.seg);
                    if (segment.id == segmentId) {
                        Coord gridCoords = null;
                        Coord curGridCoords = null;
                        for (Map.Entry<Coord, Long> segGrid : segment.map.entrySet()) {
                            if (segGrid.getValue() == gridId) {
                                gridCoords = segGrid.getKey();
                            }
                            if (segGrid.getValue().equals(grid.id)) {
                                curGridCoords = segGrid.getKey();
                            }
                        }
                        if (gridCoords != null && curGridCoords != null) {
                            double calcX = gridCoords.x - curGridCoords.x;
                            double calcY = gridCoords.y - curGridCoords.y;


                            List<Coord2d> loadedCoords = new ArrayList<>();
                            Coord2d firstCoord = new Coord2d(calcX * 1100 + grid.gc.x * 1100 + xInitialOffset, calcY * 1100 + grid.gc.y * 1100 + yInitialOffset);
                            loadedCoords.add(firstCoord);

                            for (int i = 0; i < arrayOfCheckpoints.length; i++) {
                                double checkpointX = Double.parseDouble(arrayOfCheckpoints[i].split(",")[0]);
                                double checkpointY = Double.parseDouble(arrayOfCheckpoints[i].split(",")[1]);
                                if(i == 0){
                                    loadedCoords.add(firstCoord.add(checkpointX, checkpointY));
                                } else {
                                    Coord2d prev = loadedCoords.get(i);
                                    loadedCoords.add(prev.add(checkpointX, checkpointY));
                                }
                            }
                            checkpointList.removeAllItems();
                            for(Coord2d coord : loadedCoords){
                                addCoord(coord);
                            }
                        } else {
                            gui.error("Cannot import this route, grid with starting point is not present in your map data.");
                        }
                    } else {
                        gui.error("Cannot import this route, you are in different map segment.");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedRoute(int id){
        String deleteSql = "DELETE FROM routes WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, id);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    gui.error("No routes found with the given id.");
                } else {
                    gui.msg("Successfully removed the route.", Color.WHITE);
                    selectFilterEntry.settext("");
                    loadSavedRoutes();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fixSelectedRoute(int id){
        if(gui.map.player() == null){
            return;
        }
        String selectSql = "SELECT id, initial_point FROM routes WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setInt(1, id);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String[] initialPoint = rs.getString("initial_point").split(";");
                    String xOffset = initialPoint[2];
                    String yOffset = initialPoint[3];

                    try {
                        Coord playerCoord = ui.gui.map.player().rc.floor(tilesz);
                        MCache.Grid obg = ui.sess.glob.map.getgrid(playerCoord.div(cmaps));
                        MapFile.GridInfo info = ui.gui.mapfile.file.gridinfo.get(obg.id);

                        String newInitialPoint = info.seg + ";" + obg.id + ";" + xOffset + ";" + yOffset;

                        String updateSql = "UPDATE routes SET initial_point = ? WHERE id = ?";
                        try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateSql)) {
                            pstmtUpdate.setString(1, newInitialPoint);
                            pstmtUpdate.setInt(2, id);
                            int affectedRows = pstmtUpdate.executeUpdate();

                            if (affectedRows == 0) {
                                gui.error("Failed to update route.");
                            } else {
                                gui.msg("Successfully updated the route.", Color.WHITE);
                            }
                        }
                    } catch (Loading ignored){}


                } else {
                    gui.error("No route found with the given id.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSavedRoutes() {
        String selectSql = "SELECT id, name, length FROM routes WHERE name like ? order by id";
        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setString(1, "%" + selectFilter + "%");
                ResultSet rs = pstmt.executeQuery();
                routeList.removeAllItems();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    double length = rs.getDouble("length");

                    addRoute(id, name, length);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveCurrentRoute() {
        List<Coord2d> coords = getAllCoords();

        if (coords == null || coords.size() <= 2) {
            gui.error("Route must have at least 3 checkpoints.");
            return;
        }

        if (Objects.equals(nameInput, "")) {
            gui.error("Name for route is required.");
            return;
        }

        try {
            Coord initialCoord = coords.get(0).floor(tilesz);
            MCache.Grid obg = ui.sess.glob.map.getgrid(initialCoord.div(cmaps));
            MapFile.GridInfo info = ui.gui.mapfile.file.gridinfo.get(obg.id);
            double xValue = Math.floor((coords.get(0).x - (obg.gc.x * 1100)) * 100) / 100;
            double yValue = Math.floor((coords.get(0).y - (obg.gc.y * 1100)) * 100) / 100;
            String stringInitialCoord = info.seg + ";" + obg.id + ";" + xValue + ";" + yValue;
            String insertSql = "INSERT INTO routes(name, length, initial_point, route) VALUES (?, ?, ?, ?)";
            StringBuilder route = new StringBuilder();
            double length = 0;
            for (int i = 0; i < coords.size() - 1; i++) {
                Coord2d current = coords.get(i);
                Coord2d next = coords.get(i + 1);
                length = length + next.dist(current);
                double xOffset = (Math.floor(next.x - current.x) * 100) / 100;
                double yOffset = (Math.floor(next.y - current.y) * 100) / 100;
                route.append(xOffset).append(",").append(yOffset).append(";");
            }

            try (Connection conn = DriverManager.getConnection(DATABASE)) {
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, nameInput);
                    pstmt.setString(2, String.valueOf(Math.floor(length)));
                    pstmt.setString(3, stringInitialCoord);
                    pstmt.setString(4, route.toString());
                    pstmt.executeUpdate();
                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        gui.msg("New route saved.", Color.WHITE);
                        routeNameInput.settext("");
                        nameInput = "";
                        selectFilterEntry.settext("");
                        loadSavedRoutes();
                    } else {
                        throw new SQLException("Creating route failed, no ID obtained.");
                    }
                }
            } catch (SQLException e) {
                gui.error("Something went wrong with database while saving your route.");
            }
        } catch (Loading e) {
            gui.error("You must be in the same or neighbouring grid with first checkpoint to save your route.");
        } catch (IllegalMonitorStateException e) {
            gui.error("Something went wrong with map while saving your route.");
        }
    }

    private void transformIntoArea() {
        if (checkpointList.listitems() > 4 && checkpointList.listitems() < 10) {
            List<Coord> initialCoords = new ArrayList<>();
            for (CheckPoint checkpoint : checkpointList.items) {
                initialCoords.add(checkpoint.getCoord().floor());
            }
            initialCoords.set(initialCoords.size() - 1, initialCoords.get(0));

            double minX = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;

            for (Coord coord : initialCoords) {
                minX = Math.min(minX, coord.x);
                maxX = Math.max(maxX, coord.x);
                minY = Math.min(minY, coord.y);
                maxY = Math.max(maxY, coord.y);
            }

            double step = 850;
            List<Coord2d> gridPoints = new ArrayList<>();

            int[] xpoints = new int[initialCoords.size()];
            int[] ypoints = new int[initialCoords.size()];

            for (int i = 0; i < initialCoords.size(); i++) {
                xpoints[i] = initialCoords.get(i).x;
                ypoints[i] = initialCoords.get(i).y;
            }

            Polygon polygon = new Polygon(xpoints, ypoints, initialCoords.size());

            boolean reverse = false;
            for (double x = minX; x <= maxX; x += step) {
                List<Coord2d> intersectionPoints = new ArrayList<>();
                if (reverse) {
                    for (double y = maxY; y >= minY; y -= step) {
                        if (polygon.contains(x, y)) {
                            intersectionPoints.add(new Coord2d(x, y));
                        }
                    }
                } else {
                    for (double y = minY; y <= maxY; y += step) {
                        if (polygon.contains(x, y)) {
                            intersectionPoints.add(new Coord2d(x, y));
                        }
                    }
                }

                if (!intersectionPoints.isEmpty()) {
                    intersectionPoints.sort(Comparator.comparingDouble(c -> c.y));
                    Coord2d bottomPoint = intersectionPoints.get(0);
                    Coord2d topPoint = intersectionPoints.get(intersectionPoints.size() - 1);

                    for (double newStep : new double[]{400, 200}) {
                        Coord2d newBottomPoint = new Coord2d(bottomPoint.x, bottomPoint.y - newStep);
                        Coord2d newTopPoint = new Coord2d(topPoint.x, topPoint.y + newStep);
                        if (polygon.contains(newBottomPoint.x, newBottomPoint.y)) {
                            bottomPoint = newBottomPoint;
                        }
                        if (polygon.contains(newTopPoint.x, newTopPoint.y)) {
                            topPoint = newTopPoint;
                        }
                    }

                    if (reverse) {
                        if (!bottomPoint.equals(topPoint)) {
                            gridPoints.add(topPoint);
                        }
                        gridPoints.add(bottomPoint);
                    } else {
                        gridPoints.add(bottomPoint);
                        if (!bottomPoint.equals(topPoint)) {
                            gridPoints.add(topPoint);
                        }
                    }
                }

                reverse = !reverse;
            }

            checkpointList.removeAllItems();

            for (Coord2d coord : gridPoints) {
                addCoord(coord);
            }
            gui.msg("Path transformed into Scan-area.", Color.WHITE);
        } else {
            gui.error("Incorrect polygon.");
        }
    }

    public void setLastPlayerCoord(Coord2d lastPlayerCoord) {
        this.lastPlayerCoord = lastPlayerCoord;
    }

    @Override
    public void run() {
        while (!stop) {
            if (!paused && gui.map.player() != null) {
                if (checkpointList.listitems() > 0) {
                    Coord2d posres = Coord2d.of(0x1.0p-10, 0x1.0p-10).mul(11, 11);
                    if (gui.map.player().rc.equals(lastPlayerCoord)) {
                        tryToMove(posres);
                        notMovingCounter++;
                    } else {
                        tryToMove(posres);
                        notMovingCounter = 0;
                    }
                    if (notMovingCounter == 30) {
                        gui.ui.error("Queued Movement PAUSED: I'm stuck!!!");
                        File file = new File(haven.MainFrame.gameDir + "res/customclient/sfx/ImStuck.wav");
                        if (file.exists()) {
                            try {
                                AudioInputStream in = AudioSystem.getAudioInputStream(file);
                                AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
                                AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
                                Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
                                ((Audio.Mixer) Audio.player.stream).add(new Audio.VolAdjust(klippi, 1));
                            } catch (UnsupportedAudioFileException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        notMovingCounter = 0;
                        paused = true;
                        pause.change("Start");
                    }
                }
                try {
                    double distance = getWholeDistance();
                    if (distance > 0 && gui.map.player().gobSpeed > 0) {
                        estimatedArrivalTime.settext("ETA: ~ " + formatTime(Math.floor(distance / gui.map.player().gobSpeed)) + ", dist: " + Math.floor(distance));
                    }
                } catch (Exception ignored) {
                }
                setLastPlayerCoord(gui.map.player().rc);
            } else {
                notMovingCounter = 0;
                if (!estimatedArrivalTime.text.equals("")) {
                    estimatedArrivalTime.settext("");
                }
            }
            if (checkpointList.listitems() < 1) {
                this.hide();
                if (extendedView) {
                    toggleExtendWindow();
                }
                paused = true;
                pause.change("Start");
            } else {
                if (!this.visible())
                    this.show();
            }
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void tryToMove(Coord2d posres) {
        gui.map.wdgmsg("click", Coord.z, checkpointList.items.get(0).coord.floor(posres), 1, 0);
        if (checkpointList.listitems() == 1 && gui.map.player().rc.dist(checkpointList.items.get(checkpointList.listitems() - 1).coord) < 3) {
            gui.map.wdgmsg("click", Coord.z, gui.map.player().rc.floor(posres), 1, 0);
            gui.msg("Destination reached.", Color.WHITE);
            checkpointList.deleteItem(checkpointList.items.get(0));
        } else if (checkpointList.listitems() > 0 && gui.map.player().rc.dist(checkpointList.items.get(0).coord) < 3) {
            checkpointList.deleteItem(checkpointList.items.get(0));
            if (checkpointList.listitems() > 0) {
                gui.map.wdgmsg("click", Coord.z, checkpointList.items.get(0).coord.floor(posres), 1, 0);
            }
        }
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if ((sender == this) && (Objects.equals(msg, "close"))) {
            for (Widget w : this.children()) {
                if (w instanceof CheckpointList) {
                    if (w.children().size() > 0) {
                        w.mousewheel(new MouseWheelEvent(Coord.z, -(w.children().size()), 1));
                    }
                }
            }
            checkpointList.removeAllItems();
            this.hide();
            if (extendedView) {
                toggleExtendWindow();
            }
            paused = true;
            pause.change("Start");
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    public void pauseIt() {
        this.paused = true;
        pause.change("Start");
    }

    public void addCoord(Coord2d coord) {
        synchronized (checkpointList) {
            checkpointList.addItem(new CheckPoint(coord, checkpointList.currentIndex + 1 == 0 ? checkpointList.items.size() + 1 : checkpointList.currentIndex + 1));
        }
    }

    public void addRoute(int id, String name, double distance) {
        synchronized (routeList) {
            routeList.addItem(new Route(id, name, distance));
        }
    }

    public double getWholeDistance() {
        if(gui.map.player() == null){
            return 0;
        }
        double distance = 0;
        synchronized (checkpointList) {
            if (checkpointList.listitems() > 1) {
                Iterator<CheckPoint> iterator = checkpointList.items.iterator();
                CheckPoint currentCheckPoint = iterator.next();
                distance += currentCheckPoint.getCoord().dist(gui.map.player().rc);
                while (iterator.hasNext()) {
                    CheckPoint nextCheckPoint = iterator.next();
                    distance += currentCheckPoint.getCoord().dist(nextCheckPoint.getCoord());
                    currentCheckPoint = nextCheckPoint;
                }
            } else if (checkpointList.listitems() == 1) {
                Coord2d coord2d = checkpointList.items.get(0).coord;
                return coord2d.dist(gui.map.player().rc);
            }
        }
        return distance;
    }

    public static String formatTime(double timeInSeconds) {
        int hours = (int) (timeInSeconds / 3600);
        int remainder = (int) (timeInSeconds - hours * 3600);
        int minutes = remainder / 60;
        int seconds = remainder - minutes * 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public List<Coord2d> getAllCoords() {
        List<Coord2d> coords = new ArrayList<>();
        synchronized (checkpointList) {
            for (CheckPoint checkPoint : checkpointList.items) {
                coords.add(checkPoint.getCoord());
            }
        }
        return coords;
    }

    public static void createDatabaseIfNotExist() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            if (conn != null) {
                createSchemaElementIfNotExist(conn, "routes",
                        "id INTEGER PRIMARY KEY, " +
                                "name VARCHAR(255) NOT NULL, " +
                                "length REAL, " +
                                "initial_point TEXT, " +
                                "route TEXT", "table");
            }
        }
    }

    private static void createSchemaElementIfNotExist(Connection conn, String name, String definitions, String type) throws SQLException {
        if (!schemaElementExists(conn, name, type)) {
            String sql = type.equals("table") ? "CREATE TABLE " + name + " (\n" + definitions + "\n);" : "CREATE INDEX " + name + " ON " + definitions + ";";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
            System.out.println("A new " + type + " (" + name + ") has been created in the database.");
        }
    }

    private static boolean schemaElementExists(Connection conn, String name, String type) throws SQLException {
        String checkExistsQuery = "SELECT name FROM sqlite_master WHERE type='" + type + "' AND name='" + name + "';";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkExistsQuery)) {
            return rs.next();
        }
    }

    public static class CheckpointList extends Widget {
        ArrayList<CheckPoint> items = new ArrayList<>();
        Scrollbar sb;
        int rowHeight = UI.scale(30);
        int rows, w;
        int currentIndex = -1;

        public CheckpointList(int w, int rows) {
            this.rows = rows;
            this.w = w;
            this.sz = new Coord(UI.scale(w), rowHeight * rows);
            sb = new Scrollbar(rowHeight * rows, 0, 100);
            add(sb, UI.scale(0, 0));
        }

        public void reverseCheckpoints(GameUI gui){
            if(items.size() > 1){
                gui.msg("Route reversed.", Color.WHITE);
                Collections.reverse(items);
                refreshCheckpointNumbers();
            }
        }

        public CheckPoint listitem(int i) {
            return items.get(i);
        }

        public void setCurrentIndex(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        public void addItem(CheckPoint item) {
            add(item);
            if (items.size() > 1 && currentIndex == -1 && sb.val <= sb.max) {
                sb.val = sb.max + 1;
            }
            if (currentIndex < 0 || currentIndex > listitems()) {
                items.add(item);
                setCurrentIndex(-1);
            } else {
                items.add(currentIndex, item);
                setCurrentIndex(-1);
            }
            refreshCheckpointNumbers();
        }

        public void deleteItem(CheckPoint item) {
            item.dispose();
            items.remove(item);
            if (sb.max - 1 < sb.min) {
                sb.val = 0;
            } else if (sb.max - 1 < sb.val) {
                sb.val = sb.max - 1;
            }
            setCurrentIndex(-1);
            refreshCheckpointNumbers();
        }

        public void removeAllItems() {
            for (CheckPoint item : items) {
                item.dispose();
            }
            items.clear();
        }

        public int listitems() {
            return items.size();
        }

        @Override
        public boolean mousewheel(MouseWheelEvent ev) {
            sb.ch(ev.a);
            return true;
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            int row = ev.c.y / rowHeight + sb.val;
            if (row >= items.size())
                return super.mousedown(ev);
            if (items.get(row).mousedown(new MouseDownEvent(ev.c.sub(UI.scale(15), ev.c.y / rowHeight * rowHeight), ev.b)))
                return true;
            return super.mousedown(ev);
        }

        @Override
        public boolean mouseup(MouseUpEvent ev) {
            int row = ev.c.y / rowHeight + sb.val;
            if (row >= items.size())
                return super.mouseup(ev);
            if (items.get(row).mouseup(new MouseUpEvent(ev.c.sub(UI.scale(15), ev.c.y / rowHeight * rowHeight), ev.b)))
                return true;
            return super.mouseup(ev);
        }

        @Override
        public void draw(GOut g) {
            sb.max = items.size() - rows;
            for (int i = 0; i < rows; i++) {
                if (i + sb.val >= items.size())
                    break;
                GOut ig = g.reclip(new Coord(UI.scale(15), i * rowHeight), UI.scale(w - UI.scale(15), rowHeight));
                items.get(i + sb.val).draw(ig);
            }
            super.draw(g);
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (msg.equals("delete") && sender instanceof CheckPoint) {
                deleteItem((CheckPoint) sender);
            } else {
                super.wdgmsg(sender, msg, args);
            }
        }

        public void refreshCheckpointNumbers() {
            for (int counter = 0; counter < items.size(); counter++) {
                items.get(counter).checkpointNumber.settext(Integer.toString(counter + 1));
            }
        }
    }

    public static class CheckPoint extends Widget {
        private final Label checkpointNumber;
        private final Label coordText;
        private final Coord2d coord;

        public Coord2d getCoord() {
            return coord;
        }

        public CheckPoint(Coord2d coord, int checkpointNumber) {
            this.coord = coord;
            Widget prev;
            this.checkpointNumber = new Label(Integer.toString(checkpointNumber), UI.scale(30));
            this.coordText = new Label(coord.floor().toString(), UI.scale(100));
            prev = add(this.checkpointNumber, UI.scale(20, 4));
            prev = add(this.coordText, prev.pos("ul").adds(44, 0));

            prev = add(new Button(UI.scale(26), "+") {
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b != 1) {
                        return true;
                    }
                    ui.gui.map.checkpointManager.checkpointList.setCurrentIndex(ui.gui.map.checkpointManager.checkpointList.items.indexOf(this.parent));
                    return super.mousedown(ev);
                }

                @Override
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if (!(sender == this)) {
                        super.wdgmsg(sender, msg, args);
                    }
                }
            }, prev.pos("ul").adds(110, -5));

            prev = add(new Button(UI.scale(60), "Preview") {
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b != 1) {
                        return true;
                    }
                    try {
                        Coord playerCoord = ui.gui.map.player().rc.floor(tilesz);
                        Coord actualCoord = coord.floor(tilesz);
                        MCache.Grid obg = ui.sess.glob.map.getgrid(playerCoord.div(cmaps));
                        MapFile.GridInfo info = ui.gui.mapfile.file.gridinfo.get(obg.id);
                        Coord sc = actualCoord.add(info.sc.sub(obg.gc).mul(cmaps));
                        ui.gui.mapfile.view.center(new MiniMap.SpecLocator(info.seg, sc));
                    } catch (Exception ignored) {
                    }
                    return super.mousedown(ev);
                }

                @Override
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if (!(sender == this)) {
                        super.wdgmsg(sender, msg, args);
                    }
                }

            }, prev.pos("ur").adds(12, 0));

            prev = add(new Button(UI.scale(26), "X") {
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b != 1) {
                        return super.mousedown(ev);
                    }
                    wdgmsg(this.parent, "delete");
                    return super.mousedown(ev);
                }

                @Override
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if (!(sender == this)) {
                        super.wdgmsg(sender, msg, args);
                    }
                }
            }, prev.pos("ur").adds(12, 0));
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
        }

        @Override
        public void mousemove(MouseMoveEvent ev) {
            if (ev.c.x > 470)
                super.mousemove(new MouseMoveEvent(ev.c.sub(UI.scale(15), 0)));
            else
                super.mousemove(ev);
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            // ND: This is the old mousedown method basically.
            // Child widgets of widgets can't be clicked anymore for some reason but idk how to fix it for now, so just use this old one.
            Coord c = ev.c;
            int button = ev.b;
            for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
                if(!wdg.visible())
                    continue;
                Coord cc = xlate(wdg.c, true);
                if(c.isect(cc, wdg.sz)) {
                    if(wdg.mousedown(new MouseDownEvent(c.add(cc.inv()), button))) {
                        return(true);
                    }
                }
            }
            return(false);
        }
    }

    public static class RouteList extends Widget {
        ArrayList<Route> items = new ArrayList<>();
        Scrollbar sb;
        int rowHeight = UI.scale(30);
        int rows, w;
        int currentIndex = -1;

        public RouteList(int w, int rows) {
            this.rows = rows;
            this.w = w;
            this.sz = new Coord(UI.scale(w), rowHeight * rows);
            sb = new Scrollbar(rowHeight * rows, 0, 100);
            add(sb, UI.scale(0, 0));
        }

        public Route listitem(int i) {
            return items.get(i);
        }

        public void setCurrentIndex(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        public void addItem(Route item) {
            add(item);
            items.add(item);
        }

        public void deleteItem(Route item) {
            item.dispose();
            items.remove(item);
            if (sb.max - 1 < sb.min) {
                sb.val = 0;
            } else if (sb.max - 1 < sb.val) {
                sb.val = sb.max - 1;
            }
            setCurrentIndex(-1);
        }

        public void removeAllItems() {
            for (Route item : items) {
                item.dispose();
            }
            items.clear();
        }

        public int listitems() {
            return items.size();
        }

        @Override
        public boolean mousewheel(MouseWheelEvent ev) {
            sb.ch(ev.a);
            return true;
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            int row = ev.c.y / rowHeight + sb.val;
            if (row >= items.size())
                return super.mousedown(ev);
            if (items.get(row).mousedown(new MouseDownEvent(ev.c.sub(UI.scale(15), ev.c.y / rowHeight * rowHeight), ev.b)))
                return true;
            return super.mousedown(ev);
        }

        @Override
        public boolean mouseup(MouseUpEvent ev) {
            int row = ev.c.y / rowHeight + sb.val;
            if (row >= items.size())
                return super.mouseup(ev);
            if (items.get(row).mouseup(new MouseUpEvent(ev.c.sub(UI.scale(15), ev.c.y / rowHeight * rowHeight), ev.b)))
                return true;
            return super.mouseup(ev);
        }

        @Override
        public void draw(GOut g) {
            sb.max = items.size() - rows;
            for (int i = 0; i < rows; i++) {
                if (i + sb.val >= items.size())
                    break;
                GOut ig = g.reclip(new Coord(UI.scale(15), i * rowHeight), UI.scale(w - UI.scale(15), rowHeight));
                items.get(i + sb.val).draw(ig);
            }
            super.draw(g);
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (msg.equals("delete") && sender instanceof Route) {
                deleteItem((Route) sender);
            } else {
                super.wdgmsg(sender, msg, args);
            }
        }
    }

    public static class Route extends Widget {
//        private final Label routeId;
        private final Label routeName;
        private final Label routeDistance;
        private final int id;

        public Route(int id, String name, double distance) {
            this.id = id;
            Widget prev;
//            this.routeId = new Label(Integer.toString(id), 30);
            this.routeName = new Label(name.length() > 23 ? name.substring(0,23).concat("...") : name, 100);
            this.routeDistance = new Label(Double.toString(distance), 100);
//            prev = add(this.routeId, UI.scale(10, 4));
            prev = add(this.routeName, UI.scale(10, 4));
            prev = add(this.routeDistance, prev.pos("ul").adds(110, 0));


            prev = add(new Button(UI.scale(50), "Load") {
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b != 1) {
                        return true;
                    }
                    ui.gui.map.checkpointManager.launchRouteCheckpoints(Route.this.id);
                    return super.mousedown(ev);
                }

                @Override
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if (!(sender == this)) {
                        super.wdgmsg(sender, msg, args);
                    }
                }
            }, prev.pos("ul").adds(70, -5));

            prev = add(new Button(UI.scale(36), "Fix?") {
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b != 1) {
                        return true;
                    }
                    ui.gui.map.checkpointManager.fixSelectedRoute(Route.this.id);
                    return super.mousedown(ev);
                }
                @Override
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if (!(sender == this)) {
                        super.wdgmsg(sender, msg, args);
                    }
                }
            }, prev.pos("ur").adds(4, 0));

            prev = add(new Button(UI.scale(25), "X") {
                @Override
                public boolean mousedown(MouseDownEvent ev) {
                    if (ev.b != 1) {
                        return super.mousedown(ev);
                    }
                    ui.gui.map.checkpointManager.deleteSelectedRoute(Route.this.id);
                    wdgmsg(this.parent, "delete");
                    return super.mousedown(ev);
                }

                @Override
                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if (!(sender == this)) {
                        super.wdgmsg(sender, msg, args);
                    }
                }
            }, prev.pos("ur").adds(4, 0));
        }

        @Override
        public void draw(GOut g) {
            super.draw(g);
        }

        @Override
        public void mousemove(MouseMoveEvent ev) {
            if (ev.c.x > 470)
                super.mousemove(new MouseMoveEvent(ev.c.sub(UI.scale(15), 0)));
            else
                super.mousemove(ev);
        }

        @Override
        public boolean mousedown(MouseDownEvent ev) {
            // ND: This is the old mousedown method basically.
            // Child widgets of widgets can't be clicked anymore for some reason but idk how to fix it for now, so just use this old one.
            Coord c = ev.c;
            int button = ev.b;
            for(Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
                if(!wdg.visible())
                    continue;
                Coord cc = xlate(wdg.c, true);
                if(c.isect(cc, wdg.sz)) {
                    if(wdg.mousedown(new MouseDownEvent(c.add(cc.inv()), button))) {
                        return(true);
                    }
                }
            }
            return(false);
        }
    }

    @Override
    public boolean keydown(KeyDownEvent ev) { // ND: do this to override the escape key being able to close the window
        if(key_esc.match(ev)) {
            return(false);
        }
        if(super.keydown(ev))
            return(true);
        return(false);
    }
}
