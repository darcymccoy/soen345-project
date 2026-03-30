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

    // ── User methods ──

    public static String addUser(User user) throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/users.json", "POST");
        writeBody(conn, user.toJson());
        System.out.println("addUser response: " + conn.getResponseCode());
        String response = readResponse(conn);
        conn.disconnect();
        return response.replaceAll(".*\"name\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    public static String getAllUsers() throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/users.json", "GET");
        String response = readResponse(conn);
        conn.disconnect();
        return response;
    }

    public static String getUserByEmail(String email) throws Exception {
        String url = BASE_URL + "/users.json?orderBy=%22email%22&equalTo=%22" + email + "%22";
        HttpURLConnection conn = openConnection(url, "GET");
        String response = readResponse(conn);
        conn.disconnect();
        return response;
    }

    // ── Event methods ──

    public static String addEvent(Event event) throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/events.json", "POST");
        writeBody(conn, event.toJson());
        System.out.println("addEvent response: " + conn.getResponseCode());
        String response = readResponse(conn);
        conn.disconnect();
        return response.replaceAll(".*\"name\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    public static String getAllEvents() throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/events.json", "GET");
        String response = readResponse(conn);
        conn.disconnect();
        return response;
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

    // ── Reservation methods ──

    public static String addReservation(Reservation reservation) throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/reservations.json", "POST");
        writeBody(conn, reservation.toJson());
        System.out.println("addReservation response: " + conn.getResponseCode());
        String response = readResponse(conn);
        conn.disconnect();
        return response.replaceAll(".*\"name\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    public static String getAllReservations() throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/reservations.json", "GET");
        String response = readResponse(conn);
        conn.disconnect();
        return response;
    }

    public static String getReservationsByUser(String userId) throws Exception {
        String url = BASE_URL + "/reservations.json?orderBy=%22userId%22&equalTo=%22" + userId + "%22";
        HttpURLConnection conn = openConnection(url, "GET");
        String response = readResponse(conn);
        conn.disconnect();
        return response;
    }

    public static void deleteReservation(String reservationId) throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/reservations/" + reservationId + ".json", "DELETE");
        System.out.println("deleteReservation response: " + conn.getResponseCode());
        conn.disconnect();
    }

    public static String getReservation(String reservationId) throws Exception {
        HttpURLConnection conn = openConnection(BASE_URL + "/reservations/" + reservationId + ".json", "GET");
        String response = readResponse(conn);
        conn.disconnect();
        return response;
    }

    // ── Helper methods ──

    private static String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();
        return response.toString();
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
