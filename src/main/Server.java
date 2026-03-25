package main;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class Server {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/events", Server::handleEvents);
        server.createContext("/api/users", Server::handleUsers);
        server.createContext("/", Server::handleStatic);

        server.setExecutor(null);
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
                long date = parseLong(body, "date");
                String location = parseString(body, "location");
                String category = parseString(body, "category");
                Event event = new Event(date, location, category);
                String eventId = Database.addEvent(event);
                sendResponse(exchange, 201, "{\"eventId\":\"" + eventId + "\"}");

            } else if (method.equals("PUT") && path.startsWith("/api/events/")) {
                String eventId = path.substring("/api/events/".length());
                String body = readBody(exchange);
                long date = parseLong(body, "date");
                String location = parseString(body, "location");
                String category = parseString(body, "category");
                Event event = new Event(eventId, date, location, category);
                Database.updateEvent(event);
                sendResponse(exchange, 200, "{\"status\":\"updated\"}");

            } else if (method.equals("DELETE") && path.startsWith("/api/events/")) {
                String eventId = path.substring("/api/events/".length());
                Event event = new Event(eventId, 0, "", "");
                Database.deleteEvent(event);
                sendResponse(exchange, 200, "{\"status\":\"deleted\"}");

            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
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
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
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
}
