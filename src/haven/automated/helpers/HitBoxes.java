package haven.automated.helpers;

import haven.*;

import java.io.*;
import java.sql.Connection;
import java.sql.*;
import java.util.*;

public class HitBoxes {
    private static final String DATABASE = "jdbc:sqlite:hitboxes.db";
    public static Map<String, CollisionBoxSecondary[]> collisionBoxMap = new HashMap<>();

    private static Set<String> passableGobs = new HashSet<>(Arrays.asList(
            "gfx/terobjs/herbs", "gfx/terobjs/items", "gfx/terobjs/plants", "gfx/terobjs/clue", "gfx/terobjs/boostspeed",
            "gfx/kritter/jellyfish/jellyfish"
    ));

    static {
        //Add missing animals...
        collisionBoxMap.put("gfx/kritter/horse", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-8, -4), new Coord(8, 4), new Coord(8, -4), new Coord(-8, 4))});
        collisionBoxMap.put("gfx/kritter/horse/mare", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-8, -4), new Coord(8, 4), new Coord(8, -4), new Coord(-8, 4))});
        collisionBoxMap.put("gfx/kritter/horse/stallion", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-8, -4), new Coord(8, 4), new Coord(8, -4), new Coord(-8, 4))});
        collisionBoxMap.put("gfx/kritter/horse/foal", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-8, -4), new Coord(8, 4), new Coord(8, -4), new Coord(-8, 4))});
        collisionBoxMap.put("gfx/kritter/cattle/calf", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-9, -3), new Coord(9, 3), new Coord(9, -3), new Coord(-9, 3))});
        collisionBoxMap.put("gfx/kritter/cattle/cattle", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-9, -3), new Coord(9, 3), new Coord(9, -3), new Coord(-9, 3))});
        collisionBoxMap.put("gfx/kritter/pig/piglet", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-6, -3), new Coord(6, 3), new Coord(6, -3), new Coord(-6, 3))});
        collisionBoxMap.put("gfx/kritter/pig/sow", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-6, -3), new Coord(6, 3), new Coord(6, -3), new Coord(-6, 3))});
        collisionBoxMap.put("gfx/kritter/pig/hog", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-6, -3), new Coord(6, 3), new Coord(6, -3), new Coord(-6, 3))});
        collisionBoxMap.put("gfx/kritter/goat/nanny", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-3, -2), new Coord(4, 2), new Coord(4, -2), new Coord(-3, 2))});
        collisionBoxMap.put("gfx/kritter/goat/billy", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-3, -2), new Coord(4, 2), new Coord(4, -2), new Coord(-3, 2))});
        collisionBoxMap.put("gfx/kritter/goat/kid", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-3, -2), new Coord(4, 2), new Coord(4, -2), new Coord(-3, 2))});
        collisionBoxMap.put("gfx/kritter/sheep/lamb", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-4, -2), new Coord(5, 2), new Coord(5, -2), new Coord(-4, 2))});
        collisionBoxMap.put("gfx/kritter/sheep/sheep", new CollisionBoxSecondary[]{new CollisionBoxSecondary(
                new Coord(-4, -2), new Coord(5, 2), new Coord(5, -2), new Coord(-4, 2))});
    }



    public static void addHitBox(Gob gob){
        Resource res = gob.getres();
        if(collisionBoxMap.get(res.name) == null){
            for (String gobResName : passableGobs) {
                if (res.name.contains(gobResName) && !res.name.contains("trellis")) {
                    collisionBoxMap.put(res.name, new CollisionBoxSecondary[]{new CollisionBoxSecondary(false)});
                    return;
                }
            }
            try {
                extractCollisionBoxesFromResource(gob);
            } catch (Loading ignored){}
        }
    }

    private static CollisionBoxSecondary[] extractCollisionBoxesFromResource(Gob gob) {
        Resource res = gob.getres();
        if (res.name.endsWith("/consobj")) {
            ResDrawable rd = gob.getattr(ResDrawable.class);
            if (rd != null && rd.sdt.rbuf.length >= 4) {
                MessageBuf buf = rd.sdt.clone();
                return (new CollisionBoxSecondary[]{new CollisionBoxSecondary(new Coord(buf.rbuf[0], buf.rbuf[1]), new Coord(buf.rbuf[2], buf.rbuf[3]))});
            }
        }
        try {
            List<Resource.Neg> negs = new ArrayList<>(res.layers(Resource.Neg.class));
            List<Resource.Obstacle> obstacles = new ArrayList<>(res.layers(Resource.Obstacle.class));
            for (RenderLink.Res link : res.layers(RenderLink.Res.class)) {
                RenderLink l = link.l;
                if (l instanceof RenderLink.MeshMat) {
                    RenderLink.MeshMat mm = (RenderLink.MeshMat) l;
                    addIf(negs, getLayer(Resource.Neg.class, mm.srcres.indir(), mm.mesh));
                    addIf(obstacles, getLayer(Resource.Obstacle.class, mm.srcres.indir(), mm.mesh));
                }
                if (l instanceof RenderLink.AmbientLink) {
                    RenderLink.AmbientLink al = (RenderLink.AmbientLink) l;
                    addIf(negs, getLayer(Resource.Neg.class, al.res));
                    addIf(obstacles, getLayer(Resource.Obstacle.class, al.res));
                }
                if (l instanceof RenderLink.Collect) {
                    RenderLink.Collect cl = (RenderLink.Collect) l;
                    addIf(negs, getLayer(Resource.Neg.class, cl.from));
                    addIf(obstacles, getLayer(Resource.Obstacle.class, cl.from));
                }
                if (l instanceof RenderLink.Parameters) {
                    RenderLink.Parameters pl = (RenderLink.Parameters) l;
                    addIf(negs, getLayer(Resource.Neg.class, pl.res));
                    addIf(obstacles, getLayer(Resource.Obstacle.class, pl.res));
                }
            }

            final List<CollisionBoxSecondary> collisionBoxesListSecondary = new ArrayList<>();
            for (Resource.Obstacle o : obstacles) {
                for (int i = 0; i < o.p.length; i++) {
                    boolean hitAble = checkHitAble(gob, o);
                    CollisionBoxSecondary collisionBox = new CollisionBoxSecondary(o.p[i], hitAble);
                    collisionBoxesListSecondary.add(collisionBox);
                }
            }
            for (Resource.Neg o : negs) {
                boolean hitAble = checkHitAble(gob);
                CollisionBoxSecondary collisionBox = new CollisionBoxSecondary(o.bc, o.ac, hitAble, false);
                collisionBoxesListSecondary.add(collisionBox);
            }

            CollisionBoxSecondary[] collisionBoxSecondaries = collisionBoxesListSecondary.toArray(new CollisionBoxSecondary[0]);

            collisionBoxMap.put(res.name, collisionBoxSecondaries);
            saveCollisionBoxMapEntry(res.name, collisionBoxSecondaries);

            return (collisionBoxSecondaries);
        } catch (Exception ignore) {
        }
        return null;
    }

    public static <T extends Resource.Layer> void addIf(final List<T> end, final List<T> start) {
        for (T l : start)
            if (!end.contains(l))
                end.add(l);
    }

    @SafeVarargs
    public static <T extends Resource.Layer> List<T> getLayer(final Class<T> layer, Indir<Resource>... reses) {
        final List<T> list = new ArrayList<>();
        for (Indir<Resource> ires : reses) {
            if (ires != null) {
                Resource res = ires.get();
                if (res != null) {
                    T l = res.layer(layer);
                    if (l != null)
                        list.add(layer.cast(l));
                }
            }
        }
        return (list);
    }

    public static boolean checkHitAble(Gob gob, Resource.Obstacle obst) {
        if (gob.getres() != null) {
            String id = obst.id;
            if (id != null)
                if (id.equals("build") || id.equals("ext")) {
                    return (false);
                }
            ResDrawable rd = gob.getattr(ResDrawable.class);
            if (rd != null)
                return !gob.getres().name.endsWith("gate") || rd.sdt.checkrbuf(0) != 1;
        }
        return (true);
    }

    public static boolean checkHitAble(Gob gob) {
        if (gob.getres() != null) {
            Resource res = gob.getres();
            if (res.name.equals("gfx/terobjs/vflag"))
                return (false);
        }
        return (true);
    }

    public static void loadCollisionBoxMap() {
        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            if (conn != null) {
                String sql = "SELECT key, value FROM collision_box_map;";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        String key = rs.getString("key");
                        String serialized = rs.getString("value");
                        CollisionBoxSecondary[] collisionBoxSecondaries = deserialize(serialized);
                        if (collisionBoxSecondaries != null) {
                            collisionBoxMap.put(key, collisionBoxSecondaries);
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error while executing SQL statement: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while connecting to database: " + e.getMessage());
        }
    }

    public static CollisionBoxSecondary[] deserialize(String s) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(s));
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (CollisionBoxSecondary[]) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error while deserializing CollisionBox[]: " + e.getMessage());
            return null;
        }
    }

    public static void saveCollisionBoxMapEntry(String key, CollisionBoxSecondary[] collisionBoxSecondaries) {
        String serialized = serialize(collisionBoxSecondaries);
        if (serialized != null) {
            try (Connection conn = DriverManager.getConnection(DATABASE)) {
                if (conn != null) {
                    String sql = "INSERT OR REPLACE INTO collision_box_map (key, value) VALUES (?, ?);";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, key);
                        pstmt.setString(2, serialized);
                        pstmt.executeUpdate();
                    } catch (SQLException e) {
                        System.err.println("Error while executing SQL statement: " + e.getMessage());
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error while connecting to database: " + e.getMessage());
            }
        }
    }

    public static String serialize(CollisionBoxSecondary[] collisionBoxSecondaries) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(collisionBoxSecondaries);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            System.err.println("Error while serializing CollisionBox[]: " + e.getMessage());
            return null;
        }
    }

    public static void createDatabaseIfNotExist() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DATABASE)) {
            if (conn != null) {
                createSchemaElementIfNotExist(conn, "collision_box_map",
                        "key VARCHAR(255) PRIMARY KEY NOT NULL, " +
                                "value BLOB NOT NULL",
                        "table");
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

    public static class CollisionBoxSecondary implements Serializable {
        private static final long serialVersionUID = 2554246893705809471L;
        public Coord2d[] coords;
        public boolean hitAble;

        public CollisionBoxSecondary(Coord a, Coord b) {
            this.coords = new Coord2d[] {
                    new Coord2d(a.x, a.y),
                    new Coord2d(b.x, b.y)
            };
            this.hitAble = true;
        }

        public CollisionBoxSecondary(Coord a, Coord b, Coord c, Coord d) {
            this.coords = new Coord2d[] {
                    new Coord2d(a.x, a.y),
                    new Coord2d(b.x, b.y),
                    new Coord2d(c.x, c.y),
                    new Coord2d(d.x, d.y)
            };
            this.hitAble = true;
        }

        public CollisionBoxSecondary(boolean hitAble) {
            this.coords = new Coord2d[0];
            this.hitAble = hitAble;
        }

        public CollisionBoxSecondary(Coord2d[] coords, boolean hitAble) {
            this.coords = coords;
            this.hitAble = hitAble;
        }

        public CollisionBoxSecondary(final Coord off, final Coord br, boolean hitAble, boolean buffer) {
            Coord ac = !buffer ? off : off.add(2, 2);
            Coord bc = !buffer ? br : br.add(4, 4);
            this.coords = new Coord2d[]{
                    new Coord2d(ac.x, -ac.y), new Coord2d(bc.x, -ac.y), new Coord2d(bc.x, -bc.y), new Coord2d(ac.x, -bc.y)
            };
            this.hitAble = hitAble;
        }

        @Override
        public String toString() {
            StringBuilder coordsStr = new StringBuilder();
            for (Coord2d coord : coords) {
                coordsStr.append("(").append(coord.x).append(", ").append(coord.y).append("), ");
            }
            if (coordsStr.length() > 0) {
                coordsStr.setLength(coordsStr.length() - 2);
            }

            return "CollisionBox{" +
                    "coords=[" + coordsStr + "]" +
                    ", hitAble=" + hitAble + ", size: " + coords.length +
                    "}";
        }
    }
}
