package haven;

import java.sql.Connection;
import java.sql.*;
import java.io.*;

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
}