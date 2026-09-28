package haven.automated.cookbook;

import haven.Defer;
import haven.ItemInfo;
import haven.Resource;
import haven.Utils;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.resutil.FoodInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * An optional second destination for food sightings, for the Kitten Rider cookbook.
 *
 * This is deliberately separate from {@link FoodService} rather than an extra URL on it. The two
 * want different data: FoodService divides FEP amounts by the quality multiplier and rounds to two
 * decimals, which is right for the endpoint it talks to, but this endpoint validates a submission
 * by re-deriving the tier-weighted FEP total and comparing it against the item's own severity
 * figure. Rounded or quality-normalised amounts fail that check and the observation is dropped, so
 * the values here are the raw ones straight off {@link FoodInfo}.
 *
 * Everything is contained in this file. Both destinations can be enabled at once, neither is on by
 * default, and the caches are separate so enabling one does not starve the other.
 */
public class KittenCookbook {
    private static final String DEFAULT_BASE = "https://api.kittenrider.com/cookbook";

    /* The server accepts at most 200 observations per request and 2000 per hour on a token issued
     * by /register. Hovering food is nowhere near that once duplicates are filtered, but chunk
     * anyway so a long session that has been offline does not post one oversized batch. */
    private static final int MAX_BATCH = 200;
    private static final int MAX_QUEUE = 500;

    private static final boolean debug = false;

    private static final Map<String, Boolean> seen = new ConcurrentHashMap<>();
    private static final Queue<Observation> queue = new ConcurrentLinkedQueue<>();

    static {
        /* Reuses FoodService's pool rather than starting a second one. */
        FoodService.scheduler.scheduleAtFixedRate(KittenCookbook::send, 15L, 15, TimeUnit.SECONDS);
    }

    /* On unless turned off. The cookbook is only useful once enough people are looking at enough
     * dishes -- a public dataset that ships switched off never reaches the point of being worth
     * switching on. Nothing identifying is sent (see the options tooltip and the README), and the
     * checkbox in Server Integration Settings turns it off for good. */
    public static boolean enabled() {
        return Utils.getprefb("kittenCookbook", true);
    }

    private static String base() {
        String raw = Utils.getpref("kittenCookbookEndpoint", DEFAULT_BASE);
        if ((raw == null) || raw.trim().isEmpty())
            raw = DEFAULT_BASE;
        raw = raw.trim();
        while (raw.endsWith("/"))
            raw = raw.substring(0, raw.length() - 1);
        return raw;
    }

    /* Collection. Called from GItem.info(), once per item instance, off the UI thread. */

    public static void checkFood(List<ItemInfo> ii, Resource res, String genus) {
        /* Food values are a fact about a particular world, so a sighting with no world attached
         * cannot be filed anywhere. The server rejects these; skip them here instead. */
        if ((genus == null) || genus.trim().isEmpty())
            return;
        List<ItemInfo> infoList = new ArrayList<>(ii);
        Defer.later(() -> {
            try {
                FoodInfo foodInfo = ItemInfo.find(FoodInfo.class, infoList);
                if (foodInfo == null)
                    return (null);

                Observation o = new Observation();
                o.res = res.name;
                o.world = genus.trim();

                QBuff qBuff = ItemInfo.find(QBuff.class, infoList);
                o.quality = (qBuff != null) ? qBuff.q : 10.0;

                /* Raw and unscaled, all four. */
                o.end = foodInfo.end;
                o.glut = foodInfo.glut;
                o.cons = foodInfo.cons;
                o.sev = foodInfo.sev;

                for (FoodInfo.Event ev : foodInfo.evs) {
                    /* The FEP's resource as well as its label. The label is localised and moves
                     * with content updates; the resource does not, and the server's consistency
                     * check reads the attribute tier off the end of it. */
                    String evres = "";
                    try {
                        evres = ev.ev.getres().name;
                    } catch (Exception ignored) {
                    }
                    String evcol = "";
                    try {
                        java.awt.Color c = ev.ev.col;
                        if (c != null)
                            evcol = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
                    } catch (Exception ignored) {
                    }
                    o.feps.add(new Fep(evres, ev.ev.nm, evcol, ev.a));
                }

                if (foodInfo.efs != null) {
                    for (FoodInfo.Effect ef : foodInfo.efs)
                        o.effects.add(new Effect(effectName(ef), ef.p));
                }
                if (foodInfo.types != null) {
                    for (int t : foodInfo.types)
                        o.types.add(t);
                }

                for (ItemInfo info : infoList) {
                    if (info instanceof ItemInfo.Name) {
                        o.name = ((ItemInfo.Name) info).str.text;
                    } else if (info instanceof ItemInfo.AdHoc) {
                        /* Verbatim, including "Peppered" and the truffled variants. FoodService
                         * drops those dishes because the other endpoint cannot represent them;
                         * this one takes the string and works out what it means server-side. */
                        o.tips.add(((ItemInfo.AdHoc) info).str.text);
                    } else if (info.getClass().getName().contains("Ingredient")
                               || info.getClass().getName().contains("Smoke")) {
                        /* Reflection because these classes ship as bytecode inside game resources.
                         * The share stays a double here rather than being cast to whole percent. */
                        String name = (String) info.getClass().getField("name").get(info);
                        Double value = (Double) info.getClass().getField("val").get(info);
                        if ((name != null) && (value != null))
                            o.ingredients.add(new Component(name, value));
                    } else if (!(info instanceof FoodInfo) && !(info instanceof QBuff)) {
                        Unknown u = describe(info);
                        if (u != null)
                            o.extra.add(u);
                    }
                }

                if ((o.name != null) && !o.feps.isEmpty())
                    enqueue(o);
            } catch (Exception e) {
                if (debug)
                    System.out.println("[KittenCookbook] cannot read food info: " + e);
            }
            return (null);
        });
    }

    /* An ItemInfo this client has no class for, described generically: its class name plus whatever
     * public scalar fields it carries. This is how effects that do not travel in FoodInfo -- salt's
     * satiation reduction, for one -- reach the dataset at all. Bounded, since it runs for every
     * info on every food hovered. */
    private static final int MAX_EXTRA_FIELDS = 12;
    private static final int MAX_EXTRA_LEN = 128;

    private static Unknown describe(ItemInfo info) {
        try {
            Class<?> cls = info.getClass();
            Map<String, String> fields = new LinkedHashMap<>();
            for (java.lang.reflect.Field f : cls.getFields()) {
                if (fields.size() >= MAX_EXTRA_FIELDS)
                    break;
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                    continue;
                Object v;
                try {
                    v = f.get(info);
                } catch (Exception ignored) {
                    continue;
                }
                if (v == null)
                    continue;
                /* Only values that mean something on their own. A nested widget would serialise to
                 * an identity hash, which is noise that changes every session. */
                if (!(v instanceof String) && !(v instanceof Number) && !(v instanceof Boolean)
                    && !(v instanceof haven.Text))
                    continue;
                String s = (v instanceof haven.Text) ? ((haven.Text) v).text : String.valueOf(v);
                if ((s == null) || s.isEmpty())
                    continue;
                fields.put(f.getName(), (s.length() > MAX_EXTRA_LEN) ? s.substring(0, MAX_EXTRA_LEN) : s);
            }
            return new Unknown(cls.getName(), fields);
        } catch (Exception e) {
            return (null);
        }
    }

    private static String effectName(FoodInfo.Effect ef) {
        if (ef.info != null) {
            for (ItemInfo i : ef.info) {
                if (i instanceof ItemInfo.Name)
                    return ((ItemInfo.Name) i).str.text;
                if (i instanceof ItemInfo.AdHoc)
                    return ((ItemInfo.AdHoc) i).str.text;
            }
        }
        return ("");
    }

    /* Dedup. Quality and the FEP amounts are part of the key on purpose: the same dish at a
     * different quality is a different data point, and it is the spread across qualities that lets
     * the server solve for the underlying recipe. */

    private static void enqueue(Observation o) {
        String hash = hash(o);
        if (hash == null)
            return;
        if (seen.putIfAbsent(hash, Boolean.TRUE) == null)
            queue.add(o);
    }

    private static String hash(Observation o) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(o.world).append(';').append(o.name).append(';').append(o.res).append(';')
              .append(o.quality).append(';');
            for (Component c : o.ingredients)
                sb.append(c.name).append(':').append(c.val).append(';');
            for (String t : o.tips)
                sb.append('+').append(t).append(';');
            for (Fep f : o.feps)
                sb.append('#').append(f.name).append(':').append(f.a).append(';');
            sb.append('=').append(o.end).append(':').append(o.glut)
              .append(':').append(o.cons).append(':').append(o.sev).append(';');
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return new BigInteger(1, digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8))).toString(16);
        } catch (Exception e) {
            return (null);
        }
    }

    /* Transport. */

    private static void send() {
        if (!enabled() || queue.isEmpty())
            return;

        String token = token();
        if (token == null) {
            /* No token yet and registration failed. Leave the queue alone and try again on the
             * next tick rather than throwing away a session's worth of sightings. */
            return;
        }

        List<Observation> batch = new ArrayList<>();
        Observation o;
        while ((batch.size() < MAX_BATCH) && ((o = queue.poll()) != null))
            batch.add(o);
        if (batch.isEmpty())
            return;

        HttpURLConnection conn = null;
        try {
            String payload = body(batch);
            conn = open(base() + "/v2/observe");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            if (code != 200) {
                if (debug)
                    System.out.println("[KittenCookbook] HTTP " + code + " for " + batch.size() + " observations");
                requeue(batch);
            }
        } catch (Exception e) {
            if (debug)
                System.out.println("[KittenCookbook] " + e);
            requeue(batch);
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    /* Puts a failed batch back, dropping the oldest once the backlog is silly. Also unmarks them so
     * a later sighting of the same dish can re-queue if these are dropped. */
    private static void requeue(List<Observation> batch) {
        for (Observation o : batch) {
            if (queue.size() >= MAX_QUEUE) {
                String h = hash(o);
                if (h != null)
                    seen.remove(h);
                continue;
            }
            queue.add(o);
        }
    }

    /**
     * The stored contribution token, registering for one if there is none.
     *
     * No account and no credentials: POSTing an empty body to /register hands back an anonymous
     * token. It is written to the same preference the options field reads, so it survives restarts
     * -- registering afresh each session would fragment one person's contributions across many
     * identities and inflate the server's agreement counts, which helps nobody.
     */
    private static String token() {
        String stored = Utils.getpref("kittenCookbookToken", "");
        if ((stored != null) && !stored.trim().isEmpty())
            return stored.trim();

        HttpURLConnection conn = null;
        try {
            conn = open(base() + "/register");
            try (OutputStream out = conn.getOutputStream()) {
                out.write("{}".getBytes(StandardCharsets.UTF_8));
            }
            if (conn.getResponseCode() != 200)
                return (null);
            StringBuilder sb = new StringBuilder();
            try (InputStream in = conn.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0)
                    sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            String token = new JSONObject(sb.toString()).optString("token", "");
            if (token.isEmpty())
                return (null);
            Utils.setpref("kittenCookbookToken", token);
            return token;
        } catch (Exception e) {
            if (debug)
                System.out.println("[KittenCookbook] cannot register: " + e);
            return (null);
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }

    private static HttpURLConnection open(String url) throws java.io.IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", haven.Config.confid + "/" + haven.Config.clientVersion);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setDoOutput(true);
        return conn;
    }

    private static String body(List<Observation> obs) {
        JSONObject root = new JSONObject();
        root.put("schema", 2);
        root.put("world", obs.get(0).world);
        JSONObject client = new JSONObject();
        client.put("name", haven.Config.confid);
        client.put("version", haven.Config.clientVersion);
        root.put("client", client);

        JSONArray list = new JSONArray();
        for (Observation o : obs) {
            JSONObject j = new JSONObject();
            j.put("res", o.res);
            j.put("name", o.name);
            j.put("quality", o.quality);

            JSONObject food = new JSONObject();
            food.put("end", o.end);
            food.put("glut", o.glut);
            food.put("cons", o.cons);
            food.put("sev", o.sev);
            j.put("food", food);

            JSONArray feps = new JSONArray();
            for (Fep f : o.feps) {
                JSONObject e = new JSONObject();
                e.put("res", f.res);
                e.put("name", f.name);
                if (!f.col.isEmpty())
                    e.put("col", f.col);
                e.put("a", f.a);
                feps.put(e);
            }
            j.put("feps", feps);

            JSONArray ing = new JSONArray();
            for (Component c : o.ingredients) {
                JSONObject e = new JSONObject();
                e.put("name", c.name);
                e.put("val", c.val);
                ing.put(e);
            }
            j.put("ingredients", ing);

            JSONArray tips = new JSONArray();
            for (String t : o.tips)
                tips.put(t);
            j.put("tips", tips);

            JSONArray efs = new JSONArray();
            for (Effect e : o.effects) {
                JSONObject x = new JSONObject();
                x.put("name", e.name);
                x.put("p", e.p);
                efs.put(x);
            }
            j.put("effects", efs);

            JSONArray types = new JSONArray();
            for (int t : o.types)
                types.put(t);
            j.put("types", types);

            JSONArray extra = new JSONArray();
            for (Unknown u : o.extra) {
                JSONObject x = new JSONObject();
                x.put("cls", u.cls);
                JSONObject fs = new JSONObject();
                for (Map.Entry<String, String> e : u.fields.entrySet())
                    fs.put(e.getKey(), e.getValue());
                x.put("fields", fs);
                extra.put(x);
            }
            j.put("extra", extra);

            list.put(j);
        }
        root.put("observations", list);
        return root.toString();
    }

    /* Plain carriers. Serialised explicitly above rather than by bean reflection, so adding a field
     * here cannot silently change the wire format. */

    private static class Observation {
        String res, name, world;
        double quality, end, glut, cons, sev;
        final List<Component> ingredients = new ArrayList<>();
        final List<Fep> feps = new ArrayList<>();
        final List<String> tips = new ArrayList<>();
        final List<Effect> effects = new ArrayList<>();
        final List<Integer> types = new ArrayList<>();
        final List<Unknown> extra = new ArrayList<>();
    }

    private static class Component {
        final String name;
        final double val;

        Component(String name, double val) {
            this.name = name;
            this.val = val;
        }
    }

    private static class Fep {
        final String res, name, col;
        final double a;

        Fep(String res, String name, String col, double a) {
            this.res = res;
            this.name = name;
            this.col = col;
            this.a = a;
        }
    }

    private static class Effect {
        final String name;
        final double p;

        Effect(String name, double p) {
            this.name = name;
            this.p = p;
        }
    }

    private static class Unknown {
        final String cls;
        final Map<String, String> fields;

        Unknown(String cls, Map<String, String> fields) {
            this.cls = cls;
            this.fields = fields;
        }
    }
}
