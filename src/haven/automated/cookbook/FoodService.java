package haven.automated.cookbook;

import haven.*;
import haven.res.ui.tt.q.qbuff.QBuff;
import haven.resutil.FoodInfo;
import org.json.JSONArray;

import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FoodService {
    private static final Map<String, ParsedFoodInfo> cachedItems = new ConcurrentHashMap<>();
    private static final Queue<HashedFoodInfo> sendQueue = new ConcurrentLinkedQueue<>();
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private static final boolean cookbookDebug = false;

    static {
        scheduler.scheduleAtFixedRate(FoodService::sendItems, 10L, 10, TimeUnit.SECONDS);
    }

    public static void checkFood(List<ItemInfo> ii, Resource res, String genus) {
        List<ItemInfo> infoList = new ArrayList<>(ii);
        Defer.later(() -> {
            try {
                String resName = res.name;
                FoodInfo foodInfo = ItemInfo.find(FoodInfo.class, infoList);
                if (foodInfo != null) {
                    QBuff qBuff = ItemInfo.find(QBuff.class, infoList);
                    double quality = qBuff != null ? qBuff.q : 10.0;
                    double multiplier = Math.sqrt(quality / 10.0);
                    double multiplier2 = Math.sqrt(multiplier);

                    ParsedFoodInfo parsedFoodInfo = new ParsedFoodInfo();
                    parsedFoodInfo.resourceName = resName;
                    parsedFoodInfo.energy = (int) (Math.round(foodInfo.end * 100));
                    parsedFoodInfo.hunger = round2Dig(foodInfo.glut * 1000 / multiplier2);
                    parsedFoodInfo.genus = genus;

                    for (int i = 0; i < foodInfo.evs.length; i++) {
                        parsedFoodInfo.feps.add(new FoodFEP(foodInfo.evs[i].ev.nm, round2Dig(foodInfo.evs[i].a / multiplier)));
                    }

                    for (ItemInfo info : infoList) {
                        if (info instanceof ItemInfo.AdHoc) {
                            String text = ((ItemInfo.AdHoc) info).str.text;
                            if (text.equals("White-truffled") || text.equals("Black-truffled") || text.equals("Peppered")) {
                                return (null);
                            }
                        }
                        if (info instanceof ItemInfo.Name) {
                            parsedFoodInfo.itemName = ((ItemInfo.Name) info).str.text;
                        }

                        if (info.getClass().getName().contains("Ingredient")) {
                            String name = (String) info.getClass().getField("name").get(info);
                            Double value = (Double) info.getClass().getField("val").get(info);
                            parsedFoodInfo.ingredients.add(new FoodIngredient(name, (int) (value * 100)));
                        } else if (info.getClass().getName().contains("Smoke")) {
                            String name = (String) info.getClass().getField("name").get(info);
                            Double value = (Double) info.getClass().getField("val").get(info);
                            parsedFoodInfo.ingredients.add(new FoodIngredient(name, (int) (value * 100)));
                        }
                    }
                    checkAndSend(parsedFoodInfo);
                }
            } catch (Exception exception) {
                if (cookbookDebug) {
                    System.out.println("Cannot create food info: " + exception.getMessage());
                }
            }
            return (null);
        });
    }

    private static double round2Dig(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static void checkAndSend(ParsedFoodInfo info) {
        String hash = generateHash(info);
        if (hash == null) return;
        if (cachedItems.containsKey(hash)) {
            return;
        }
        sendQueue.add(new HashedFoodInfo(hash, info));
    }

    public static boolean isValidEndpoint() {
        String raw = OptWnd.cookBookEndpointTextEntry.buf.line();
        if (raw == null) return false;
        raw = raw.trim();
        return raw.length() >= 5;
    }

    private static void sendItems() {
        if (sendQueue.isEmpty()) {
            return;
        }

        final String endpoint = OptWnd.cookBookEndpointTextEntry.buf.line();
        if (endpoint == null || !isValidEndpoint()) return;
        final java.net.URI apiBase = java.net.URI.create(endpoint.trim());

        List<ParsedFoodInfo> toSend = new ArrayList<>();
        while (!sendQueue.isEmpty()) {
            HashedFoodInfo info = sendQueue.poll();
            if (cachedItems.containsKey(info.hash)) {
                continue;
            }
            cachedItems.put(info.hash, info.foodInfo);
            toSend.add(info.foodInfo);
        }

        if (!toSend.isEmpty()) {
            HttpURLConnection connection = null;
            try {
                String jsonPayload = new JSONArray(toSend.toArray()).toString();

                connection = (HttpURLConnection) apiBase.toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "H&H Client");
                connection.setDoOutput(true);

                String token = OptWnd.cookBookTokenTextEntry.buf.line();
                if (token != null && !(token = token.trim()).isEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                try (OutputStream out = connection.getOutputStream()) {
                    out.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                int code = connection.getResponseCode();

                if (code != 200) {
                    if (cookbookDebug) {
                        String responseMessage = connection.getResponseMessage();

                        System.out.println("[Cookbook] Failed to send food items");
                        System.out.println("  URL: " + apiBase);
                        System.out.println("  HTTP " + code + " " + responseMessage);
                        System.out.println("  Items: " + toSend.size());
                        System.out.println("  Payload size: " + jsonPayload.length() + " bytes");
                    }
                }
            } catch (Exception ex) {
                if (cookbookDebug) {
                    System.out.println("[Cookbook] Exception while sending " + toSend.size() + " food items to " + apiBase + ": " + ex);
                    ex.printStackTrace(System.out);
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    private static String generateHash(ParsedFoodInfo foodInfo) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(foodInfo.itemName).append(";")
                    .append(foodInfo.resourceName).append(";");
            foodInfo.ingredients.forEach(it -> stringBuilder.append(it.name).append(";").append(it.percentage).append(";"));

            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
            return getHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            if (cookbookDebug) {
                System.out.println("Cannot generate food hash");
            }
        }
        return null;
    }

    private static String getHex(byte[] bytes) {
        BigInteger bigInteger = new BigInteger(1, bytes);
        return bigInteger.toString(16);
    }

    private static class HashedFoodInfo {
        public String hash;
        public ParsedFoodInfo foodInfo;

        public HashedFoodInfo(String hash, ParsedFoodInfo foodInfo) {
            this.hash = hash;
            this.foodInfo = foodInfo;
        }
    }

    public static class FoodIngredient {
        public String name;
        public Integer percentage;

        public FoodIngredient(String name, Integer percentage) {
            this.name = name;
            this.percentage = percentage;
        }

        public String getName() {
            return name;
        }

        public Integer getPercentage() {
            return percentage;
        }
    }

    public static class FoodFEP {
        public String name;
        public Double value;

        public FoodFEP(String name, Double value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Double getValue() {
            return value;
        }
    }

    public static class ParsedFoodInfo {
        public String genus;
        public String itemName;
        public String resourceName;
        public Integer energy;
        public double hunger;
        public ArrayList<FoodIngredient> ingredients;
        public ArrayList<FoodFEP> feps;

        public ParsedFoodInfo() {
            this.itemName = "";
            this.resourceName = "";
            this.ingredients = new ArrayList<>();
            this.feps = new ArrayList<>();
        }

        public String getItemName() {
            return itemName;
        }

        public String getResourceName() {
            return resourceName;
        }

        public Integer getEnergy() {
            return energy;
        }

        public double getHunger() {
            return hunger;
        }

        public ArrayList<FoodIngredient> getIngredients() {
            return ingredients;
        }

        public ArrayList<FoodFEP> getFeps() {
            return feps;
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemName, resourceName, ingredients);
        }
    }
}
