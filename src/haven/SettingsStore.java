package haven;

import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.sql.*;
import java.io.*;
import org.json.JSONObject;

public class SettingsStore {
    private Connection connection;

    // Constructor to initialize the database connection
    public SettingsStore(String dbPath) throws SQLException{
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initializeDatabase();
    }

    // Initialize the database (create the settings table if it doesn't exist)
    private void initializeDatabase() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS settings (" +
                     "key TEXT PRIMARY KEY, " +
                     "type TEXT NOT NULL, " +
                     "value BLOB)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    // Set a value for a key (store as BLOB)
    public void set(String key, Object value) {
        try {
            if (value == null) {
                // If the value is null, delete the record
                delete(key);
                return;
            }

            // Serialize the value to a byte array
            byte[] byteValue = serialize(value);

            // Determine the type of the value
            String type = value.getClass().getSimpleName();

            // Insert or replace the key-value pair
            String sql = "INSERT OR REPLACE INTO settings (key, type, value) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, key);
                pstmt.setString(2, type);
                pstmt.setBytes(3, byteValue);
                pstmt.executeUpdate();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Get a value by key (deserialize from BLOB)
    public Object get(String key, Object defaultValue){
        String sql = "SELECT value FROM settings WHERE key = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] byteValue = rs.getBytes("value");
                    return deserialize(byteValue);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return defaultValue; // Return default value if key not found
    }

    // Delete a key-value pair
    public void delete(String key) throws SQLException {
        String sql = "DELETE FROM settings WHERE key = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.executeUpdate();
        }
    }

    // Serialize an object to a byte array
    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }

    // Deserialize a byte array to an object
    private Object deserialize(byte[] byteValue) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(byteValue);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        }
    }

    // Close the database connection
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    // Type-specific getters and setters with default values
    public void setInt(String key, Integer value){
        set(key, value);
    }

    public Integer getInt(String key, Integer defaultValue){
        return (Integer) get(key, defaultValue);
    }

    public void setString(String key, String value){
        set(key, value);
    }

    public String getString(String key, String defaultValue) {
        return (String) get(key, defaultValue);
    }

    public void setDouble(String key, Double value){
        set(key, value);
    }

    public Double getDouble(String key, Double defaultValue){
        return (Double) get(key, defaultValue);
    }

    public void setBoolean(String key, Boolean value){
        set(key, value);
    }

    public Boolean getBoolean(String key, Boolean defaultValue){
        return (Boolean) get(key, defaultValue);
    }

    public void setByteArray(String key, byte[] value){
        set(key, value);
    }

    public byte[] getByteArray(String key, byte[] defaultValue){
        return (byte[]) get(key, defaultValue);
    }

    // Export settings to a JSON file
    public void exportToJson(String filePath) throws SQLException, IOException, ClassNotFoundException {
        Map<String, Object> jsonMap = new TreeMap<>();
        String sql = "SELECT key, type, value FROM settings";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString("key");
                String type = rs.getString("type");
                byte[] byteValue = rs.getBytes("value");
                Object value = deserialize(byteValue);

                // Convert value to appropriate JSON type
                switch (type) {
                    case "Integer":
                        jsonMap.put(key, (Integer) value);
                        break;
                    case "String":
                        jsonMap.put(key, (String) value);
                        break;
                    case "Double":
                        jsonMap.put(key, (Double) value);
                        break;
                    case "Boolean":
                        jsonMap.put(key, (Boolean) value);
                        break;
                    case "Byte[]":
                        break;
                        // Do not export bytearrays
                        //json.put(key, new String((byte[]) value));
                        //break;
                    default:
                        jsonMap.put(key, value.toString());
                        break;
                }
            }
        }

        // Write JSON to file
        try (FileWriter file = new FileWriter(filePath)) {
            // Use a custom method to serialize the map with proper formatting
            String jsonString = serializeMap(jsonMap);
            file.write(jsonString);
        }
    }
    // Helper method to serialize the map with proper formatting
    //   Needed to serialize doubles with .0 if they are whole numbers
    private String serializeMap(Map<String, Object> map) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        int size = map.size();
        int count = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            jsonBuilder.append("    \"").append(key).append("\": ");
            if (value instanceof Integer) {
                // Serialize integers as-is
                jsonBuilder.append(value);
            } else if (value instanceof Double) {
                // Serialize doubles with .0 if they are whole numbers
                double doubleValue = (Double) value;
                if (doubleValue == (long) doubleValue) {
                    jsonBuilder.append(String.format("%.1f", doubleValue)); // Add .0
                } else {
                    jsonBuilder.append(doubleValue); // Keep as-is
                }
            } else if (value instanceof String) {
                // Serialize strings with quotes
                jsonBuilder.append("\"").append(value).append("\"");
            } else if (value instanceof Boolean) {
                // Serialize booleans as-is
                jsonBuilder.append(value);
            } else {
                // Fallback for other types
                jsonBuilder.append("\"").append(value.toString()).append("\"");
            }
            if (++count < size) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }
    // Import settings from a JSON file
    public void importFromJson(String filePath) throws IOException, SQLException {
        StringBuilder jsonContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }

        JSONObject json = new JSONObject(jsonContent.toString());
        for (String key : json.keySet()) {
            Object value = json.get(key);
            set(key, value);
        }
    }
}