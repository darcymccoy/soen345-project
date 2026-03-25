package main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Properties;

public class Database {

    private static final String BASE_URL;
    private static final String DB_SECRET;

    static {
        try {
            Properties config = new Properties();
            config.load(new FileInputStream("config.properties"));
            BASE_URL = config.getProperty("firebase.database.url");
            DB_SECRET = config.getProperty("firebase.database.secret");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static void addUser(User user) throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/users.json", "POST");
        writeBody(conn, user.toJson());
        System.out.println("addUser response: " + conn.getResponseCode());
        conn.disconnect();
    }

    public static String addEvent(Event event) throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/events.json", "POST");
        writeBody(conn, event.toJson());
        System.out.println("addEvent response: " + conn.getResponseCode());
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            response.append(line);
        reader.close();
        conn.disconnect();
        String responseStr = response.toString();
        String eventId = responseStr.replaceAll(".*\"name\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        return eventId;
    }

    public static String getAllUsers() throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/users.json", "GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();
        conn.disconnect();
        return response.toString();
    }

    public static String getAllEvents() throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/events.json", "GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();
        conn.disconnect();
        return response.toString();
    }

    public static void updateEvent(Event event) throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/events/" + event.getEventId() + ".json", "PUT");
        writeBody(conn, event.toJson());
        System.out.println("updateEvent response: " + conn.getResponseCode());
        conn.disconnect();
    }

    public static void deleteEvent(Event event) throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/events/" + event.getEventId() + ".json", "DELETE");
        System.out.println("deleteEvent response: " + conn.getResponseCode());
        conn.disconnect();
    }

    private static HttpURLConnection openConnection(String urlStr, String method) throws Exception {
        String authedUrl = urlStr + (urlStr.contains("?") ? "&" : "?") + "auth=" + DB_SECRET;
        HttpURLConnection conn = (HttpURLConnection) URI.create(authedUrl).toURL().openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        if (!method.equals("GET")) conn.setDoOutput(true);
        return conn;
    }

    private static void writeBody(HttpURLConnection conn, String json) throws Exception {
        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes());
        os.flush();
        os.close();
    }
}
