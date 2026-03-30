package main;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class Server {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/events", Server::handleEvents);
        server.createContext("/api/users", Server::handleUsers);
        server.createContext("/api/auth", Server::handleAuth);
        server.createContext("/api/reservations", Server::handleReservations);
        server.createContext("/styles.css", Server::handleStaticCss);
        server.createContext("/", Server::handleStatic);

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Server running at http://localhost:8080");
    }

    private static void handleEvents(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if (method.equals("GET") && path.equals("/api/events")) {
                String json = Database.getAllEvents();
                sendResponse(exchange, 200, json);

            } else if (method.equals("POST") && path.equals("/api/events")) {
                String body = readBody(exchange);
                String role = parseString(body, "role");
                if (!"admin".equals(role)) {
                    sendResponse(exchange, 403, "{\"error\":\"Only administrators can add events\"}");
                    return;
                }
                long date = parseLong(body, "date");
                String location = parseString(body, "location");
                String category = parseString(body, "category");
                Event event = new Event(date, location, category);
                String eventId = Database.addEvent(event);
                sendResponse(exchange, 201, "{\"eventId\":\"" + escapeJson(eventId) + "\"}");

            } else if (method.equals("PUT") && path.startsWith("/api/events/")) {
                String eventId = path.substring("/api/events/".length());
                String body = readBody(exchange);
                String role = parseString(body, "role");
                if (!"admin".equals(role)) {
                    sendResponse(exchange, 403, "{\"error\":\"Only administrators can edit events\"}");
                    return;
                }
                long date = parseLong(body, "date");
                String location = parseString(body, "location");
                String category = parseString(body, "category");
                Event event = new Event(eventId, date, location, category);
                Database.updateEvent(event);
                sendResponse(exchange, 200, "{\"status\":\"updated\"}");

            } else if (method.equals("DELETE") && path.startsWith("/api/events/")) {
                String eventId = path.substring("/api/events/".length());
                String query = exchange.getRequestURI().getQuery();
                String role = "";
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("role=")) role = param.substring(5);
                    }
                }
                if (!"admin".equals(role)) {
                    sendResponse(exchange, 403, "{\"error\":\"Only administrators can cancel events\"}");
                    return;
                }
                Event event = new Event(eventId, 0, "", "");
                Database.deleteEvent(event);
                sendResponse(exchange, 200, "{\"status\":\"deleted\"}");

            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleUsers(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if (method.equals("GET")) {
                String json = Database.getAllUsers();
                sendResponse(exchange, 200, json);

            } else if (method.equals("POST")) {
                String body = readBody(exchange);
                String email = parseString(body, "email");
                long phoneNumber = parseLong(body, "phoneNumber");
                User user;
                if (email != null && !email.isEmpty()) {
                    user = new User(email);
                } else {
                    user = new User(phoneNumber);
                }
                Database.addUser(user);
                sendResponse(exchange, 201, "{\"status\":\"created\"}");

            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleAuth(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if (method.equals("POST") && path.equals("/api/auth/register")) {
                String body = readBody(exchange);
                String email = parseString(body, "email");
                String password = parseString(body, "password");

                if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Email and password are required\"}");
                    return;
                }

                String existing = Database.getUserByEmail(email);
                if (existing != null && !existing.equals("null") && !existing.equals("{}")) {
                    sendResponse(exchange, 409, "{\"error\":\"Email already registered\"}");
                    return;
                }

                User user = new User(email, password);
                String userId = Database.addUser(user);
                sendResponse(exchange, 201, "{\"userId\":\"" + escapeJson(userId) + "\",\"email\":\"" + escapeJson(email) + "\",\"role\":\"user\"}");

            } else if (method.equals("POST") && path.equals("/api/auth/login")) {
                String body = readBody(exchange);
                String email = parseString(body, "email");
                String password = parseString(body, "password");

                if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Email and password are required\"}");
                    return;
                }

                String usersJson = Database.getUserByEmail(email);
                if (usersJson == null || usersJson.equals("null") || usersJson.equals("{}")) {
                    sendResponse(exchange, 401, "{\"error\":\"Invalid email or password\"}");
                    return;
                }

                String storedPassword = parseString(usersJson, "password");
                if (!password.equals(storedPassword)) {
                    sendResponse(exchange, 401, "{\"error\":\"Invalid email or password\"}");
                    return;
                }

                String role = parseString(usersJson, "role");
                if (role == null || role.isEmpty()) role = "user";

                String userId = extractFirebaseKey(usersJson);

                sendResponse(exchange, 200, "{\"userId\":\"" + escapeJson(userId) + "\",\"email\":\"" + escapeJson(email) + "\",\"role\":\"" + escapeJson(role) + "\"}");

            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static String extractFirebaseKey(String json) {
        return json.replaceAll("\\{\"([^\"]+)\":\\{.*", "$1");
    }

    private static void handleReservations(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if (method.equals("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            if (method.equals("POST") && path.equals("/api/reservations")) {
                String body = readBody(exchange);
                String userId = parseString(body, "userId");
                String eventId = parseString(body, "eventId");
                String userEmail = parseString(body, "userEmail");
                String eventLocation = parseString(body, "eventLocation");
                String eventCategory = parseString(body, "eventCategory");
                long eventDate = parseLong(body, "eventDate");

                if (userId.isEmpty() || eventId.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"userId and eventId are required\"}");
                    return;
                }

                String existingReservations = Database.getReservationsByUser(userId);
                if (existingReservations != null && !existingReservations.equals("null") && !existingReservations.equals("{}")) {
                    if (existingReservations.contains("\"eventId\":\"" + eventId + "\"")) {
                        sendResponse(exchange, 409, "{\"error\":\"You already have a reservation for this event\"}");
                        return;
                    }
                }

                Reservation reservation = new Reservation(userId, eventId, userEmail);
                String reservationId = Database.addReservation(reservation);

                // Send booking confirmation email
                String dateStr = new Date(eventDate).toString();
                EmailService.sendBookingConfirmation(userEmail, eventLocation, eventCategory, dateStr);

                sendResponse(exchange, 201, "{\"reservationId\":\"" + escapeJson(reservationId) + "\"}");

            } else if (method.equals("GET") && path.equals("/api/reservations")) {
                String query = exchange.getRequestURI().getQuery();
                String userId = "";
                if (query != null) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("userId=")) userId = param.substring(7);
                    }
                }

                String json;
                if (!userId.isEmpty()) {
                    json = Database.getReservationsByUser(userId);
                } else {
                    json = Database.getAllReservations();
                }
                sendResponse(exchange, 200, json != null ? json : "{}");

            } else if (method.equals("DELETE") && path.startsWith("/api/reservations/")) {
                String reservationId = path.substring("/api/reservations/".length());

                // Get reservation details before deleting for email
                String reservationJson = Database.getReservation(reservationId);
                String userEmail = parseString(reservationJson, "userEmail");
                String eventId = parseString(reservationJson, "eventId");

                Database.deleteReservation(reservationId);

                // Send cancellation email
                if (userEmail != null && !userEmail.isEmpty()) {
                    EmailService.sendCancellationConfirmation(userEmail, "", "", "");
                }

                sendResponse(exchange, 200, "{\"status\":\"cancelled\"}");

            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private static void handleStatic(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        Path filePath = Path.of("frontend/index.html");
        if (Files.exists(filePath)) {
            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        } else {
            String msg = "frontend/index.html not found";
            sendResponse(exchange, 404, msg);
        }
    }

    private static void handleStaticCss(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        Path filePath = Path.of("frontend/styles.css");
        if (Files.exists(filePath)) {
            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", "text/css");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private static String parseString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static long parseLong(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*([0-9]+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? Long.parseLong(m.group(1)) : 0;
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
