package test;

import com.sun.net.httpserver.HttpServer;
import main.Database;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.net.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CCAC tests for Database's private helper methods:
 *   - readResponse(HttpURLConnection): c1 = (line = reader.readLine()) != null
 *   - openConnection(String, String):  c1 = urlStr.contains("?")
 *                                      c2 = !method.equals("GET")
 *
 * A local HttpServer is used so that the methods can be exercised without
 * a live Firebase connection.
 */
class DatabaseCCACTest {

    private static HttpServer mockServer;
    private static final int PORT = 18765; // unlikely to conflict

    @BeforeAll
    static void startMockServer() throws Exception {
        mockServer = HttpServer.create(new InetSocketAddress(PORT), 0);

        // /response  – returns a non-empty body
        mockServer.createContext("/response", exchange -> {
            byte[] body = "mock-response-line".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });

        // /empty  – returns HTTP 200 with a zero-length body
        mockServer.createContext("/empty", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });

        // /test  – accepts any request (used for openConnection tests)
        mockServer.createContext("/test", exchange -> {
            byte[] body = "ok".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.getResponseBody().close();
        });

        mockServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        mockServer.stop(0);
    }

    // ── Reflection helpers ─────────────────────────────────────────────────

    private static String invokeReadResponse(HttpURLConnection conn) throws Exception {
        Method m = Database.class.getDeclaredMethod("readResponse", HttpURLConnection.class);
        m.setAccessible(true);
        return (String) m.invoke(null, conn);
    }

    private static HttpURLConnection invokeOpenConnection(String url, String method) throws Exception {
        Method m = Database.class.getDeclaredMethod("openConnection", String.class, String.class);
        m.setAccessible(true);
        return (HttpURLConnection) m.invoke(null, url, method);
    }

    // ══════════════════════════════════════════════════════════════════════
    // readResponse
    // c1 = (line = reader.readLine()) != null   P1 = c1
    // ══════════════════════════════════════════════════════════════════════

    /**
     * T1 – c1=T: the server returns a non-empty body; readLine returns a line,
     * the while loop executes, and the content is accumulated and returned.
     */
    @Test
    void readResponse_c1_T1_nonEmptyResponse_returnsContent() throws Exception {
        URL url = new URL("http://localhost:" + PORT + "/response");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        String result = invokeReadResponse(conn);
        conn.disconnect();
        assertEquals("mock-response-line", result);
    }

    /**
     * T2 – c1=F: the server returns a zero-length body; readLine returns null
     * immediately, the while loop does not execute, and "" is returned.
     */
    @Test
    void readResponse_c1_T2_emptyResponse_returnsEmptyString() throws Exception {
        URL url = new URL("http://localhost:" + PORT + "/empty");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        String result = invokeReadResponse(conn);
        conn.disconnect();
        assertEquals("", result);
    }

    // ══════════════════════════════════════════════════════════════════════
    // openConnection
    // c1 = urlStr.contains("?")   P1 = c1
    // ══════════════════════════════════════════════════════════════════════

    /**
     * T1 – c1=T: URL already has a "?" query string; the auth param is appended
     * with "&", so the resulting URL contains "existing=param&auth=".
     */
    @Test
    void openConnection_c1_T1_urlContainsQuestionMark_appendsAmpersandAuth() throws Exception {
        String base = "http://localhost:" + PORT + "/test?existing=param";
        HttpURLConnection conn = invokeOpenConnection(base, "GET");
        String actual = conn.getURL().toString();
        conn.disconnect();
        assertTrue(actual.contains("existing=param&auth="),
                "Expected '&auth=' after existing param, got: " + actual);
    }

    /**
     * T2 – c1=F: URL has no "?"; the auth param is appended with "?",
     * so the resulting URL contains exactly one "?" before "auth=".
     */
    @Test
    void openConnection_c1_T2_urlNoQuestionMark_appendsQuestionMarkAuth() throws Exception {
        String base = "http://localhost:" + PORT + "/test";
        HttpURLConnection conn = invokeOpenConnection(base, "GET");
        String actual = conn.getURL().toString();
        conn.disconnect();
        assertTrue(actual.contains("?auth="),
                "Expected '?auth=' in URL, got: " + actual);
        // Ensure there is no '?' before the auth separator
        int firstQ = actual.indexOf('?');
        assertEquals(actual.indexOf("?auth="), firstQ,
                "Expected exactly one '?' in URL, got: " + actual);
    }

    // ══════════════════════════════════════════════════════════════════════
    // openConnection
    // c2 = !method.equals("GET")   P2 = c2
    // ══════════════════════════════════════════════════════════════════════

    /**
     * T1 – c2=T: method is not "GET" → conn.setDoOutput(true) is called.
     */
    @Test
    void openConnection_c2_T1_nonGET_setsDoOutputTrue() throws Exception {
        String base = "http://localhost:" + PORT + "/test";
        HttpURLConnection conn = invokeOpenConnection(base, "POST");
        boolean doOutput = conn.getDoOutput();
        conn.disconnect();
        assertTrue(doOutput, "Expected DoOutput=true for POST");
    }

    /**
     * T2 – c2=F: method is "GET" → setDoOutput is NOT called, remains false.
     */
    @Test
    void openConnection_c2_T2_GET_doesNotSetDoOutput() throws Exception {
        String base = "http://localhost:" + PORT + "/test";
        HttpURLConnection conn = invokeOpenConnection(base, "GET");
        boolean doOutput = conn.getDoOutput();
        conn.disconnect();
        assertFalse(doOutput, "Expected DoOutput=false for GET");
    }
}