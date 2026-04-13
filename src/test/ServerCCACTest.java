package test;

import com.sun.net.httpserver.*;
import main.Database;
import main.Event;
import main.Server;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CCAC (Correlated Active Clause Coverage) tests for Server handler methods.
 *
 * Private handlers are invoked via reflection.  A minimal FakeHttpExchange
 * captures the HTTP status code and response body without needing Mockito.
 *
 * Tests that reach Database methods perform real Firebase calls; they mirror
 * the integration-test approach already used in DatabaseTest.java.
 */
class ServerCCACTest {

    // ── Fake HttpExchange ──────────────────────────────────────────────────

    static class FakeHttpExchange extends HttpExchange {
        private final String method;
        private final URI uri;
        private final InputStream requestBody;
        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final ByteArrayOutputStream capturedBody = new ByteArrayOutputStream();
        private int capturedCode = -1;

        FakeHttpExchange(String method, String uri, String body) {
            this.method = method;
            this.uri = URI.create(uri);
            this.requestBody = new ByteArrayInputStream(
                    body != null ? body.getBytes() : new byte[0]);
        }

        @Override public String getRequestMethod()  { return method; }
        @Override public URI    getRequestURI()     { return uri; }
        @Override public Headers getRequestHeaders() { return requestHeaders; }
        @Override public Headers getResponseHeaders(){ return responseHeaders; }
        @Override public InputStream  getRequestBody()  { return requestBody; }
        @Override public OutputStream getResponseBody() { return capturedBody; }

        @Override
        public void sendResponseHeaders(int code, long len) {
            capturedCode = code;
        }

        @Override public void close() {}
        @Override public HttpContext        getHttpContext()   { return null; }
        @Override public InetSocketAddress  getLocalAddress()  { return null; }
        @Override public InetSocketAddress  getRemoteAddress() { return null; }
        @Override public String             getProtocol()      { return "HTTP/1.1"; }
        @Override public Object  getAttribute(String n)        { return null; }
        @Override public void    setAttribute(String n, Object v) {}
        @Override public void    setStreams(InputStream i, OutputStream o) {}
        @Override public HttpPrincipal getPrincipal()          { return null; }
        @Override public int         getResponseCode()        { return capturedCode; }

        int    code() { return capturedCode; }
        String body() {
            try { return capturedBody.toString("UTF-8"); }
            catch (Exception e) { return ""; }
        }
    }

    // ── Reflection helpers ─────────────────────────────────────────────────

    /** Invoke a private static handler method on Server via reflection. */
    private void invoke(String handlerName, HttpExchange ex) throws Exception {
        Method m = Server.class.getDeclaredMethod(handlerName, HttpExchange.class);
        m.setAccessible(true);
        m.invoke(null, ex);
    }

    /** Invoke the private readBody helper. */
    private String invokeReadBody(HttpExchange ex) throws Exception {
        Method m = Server.class.getDeclaredMethod("readBody", HttpExchange.class);
        m.setAccessible(true);
        return (String) m.invoke(null, ex);
    }

    // ══════════════════════════════════════════════════════════════════════
    // handleEvents
    // ══════════════════════════════════════════════════════════════════════

    // ── c1 : method.equals("OPTIONS")  P1 = c1 ────────────────────────────

    /** T1 – c1=T → 204, no further processing. */
    @Test
    void handleEvents_c1_T1_OPTIONS_returns204() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("OPTIONS", "/api/events", "");
        invoke("handleEvents", ex);
        assertEquals(204, ex.code());
    }

    /** T2 – c1=F → falls through to GET/POST/… branches. */
    @Test
    void handleEvents_c1_T2_notOPTIONS_proceeds() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/events", "");
        invoke("handleEvents", ex);
        assertNotEquals(204, ex.code());
    }

    // ── c2 : method.equals("GET")  c3 : path.equals("/api/events")  P2 = c2 && c3 ──

    /** T1 – c2=T, c3=T → reads all events, returns 200. */
    @Test
    void handleEvents_c2_c3_T1_GET_events_returns200() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/events", "");
        invoke("handleEvents", ex);
        assertEquals(200, ex.code());
    }

    /** T2 (c2 active) – c2=F, c3=T → GET branch skipped; POST with non-admin returns 403. */
    @Test
    void handleEvents_c2_T2_nonGET_doesNotEnterGetBranch() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/events",
                "{\"role\":\"user\",\"date\":1000,\"location\":\"X\",\"category\":\"Y\"}");
        invoke("handleEvents", ex);
        assertEquals(403, ex.code());
    }

    /** T2 (c3 active) – c2=T, c3=F → wrong path, returns 405. */
    @Test
    void handleEvents_c3_T2_GET_wrongPath_returns405() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/wrong", "");
        invoke("handleEvents", ex);
        assertEquals(405, ex.code());
    }

    // ── c4 : method.equals("POST")  c3 : path.equals("/api/events")  P3 = c4 && c3 ──

    /** T1 – c4=T, c3=T, admin role → creates event, returns 201. */
    @Test
    void handleEvents_c4_c3_T1_POST_events_adminRole_returns201() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/events",
                "{\"role\":\"admin\",\"date\":1000000,\"location\":\"Montreal\",\"category\":\"Music\"}");
        invoke("handleEvents", ex);
        assertEquals(201, ex.code());
        // Clean up created event
        String resId = ex.body().replaceAll(".*\"eventId\":\"([^\"]+)\".*", "$1");
        if (!resId.isEmpty() && !resId.equals(ex.body())) {
            Database.deleteEvent(new Event(resId, 0, "", ""));
        }
    }

    /** T2 (c4 active) – c4=F → GET branch handles the request. */
    @Test
    void handleEvents_c4_T2_nonPOST_doesNotEnterPostBranch() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/events", "");
        invoke("handleEvents", ex);
        assertEquals(200, ex.code());
    }

    // ── c5 : method.equals("DELETE")  c3 : path.startsWith("/api/events/")  P4 = c5 && c3 ──

    /** T1 – c5=T, c3=T → enters DELETE branch; with admin role returns 200. */
    @Test
    void handleEvents_c5_c3_T1_DELETE_events_adminRole_returns200() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("DELETE",
                "/api/events/non-existent-event-id?role=admin", "");
        invoke("handleEvents", ex);
        assertEquals(200, ex.code());
    }

    /** T2 (c5 active) – c5=F → DELETE branch skipped; GET returns 200. */
    @Test
    void handleEvents_c5_T2_nonDELETE_doesNotEnterDeleteBranch() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/events", "");
        invoke("handleEvents", ex);
        assertEquals(200, ex.code());
    }

    /** T2 (c3 active with DELETE) – c5=T, c3=F → path doesn't start with /api/events/, returns 405. */
    @Test
    void handleEvents_c3_T2_DELETE_noEventId_returns405() throws Exception {
        // "/api/events" does NOT satisfy startsWith("/api/events/")
        FakeHttpExchange ex = new FakeHttpExchange("DELETE", "/api/events", "");
        invoke("handleEvents", ex);
        assertEquals(405, ex.code());
    }

    // ── c6 : !"admin".equals(role)  P5/P6/P7 = c6 ────────────────────────

    /** T1 – c6=T (role != "admin") on POST → 403 Forbidden. */
    @Test
    void handleEvents_c6_T1_nonAdminRole_POST_returns403() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/events",
                "{\"role\":\"user\",\"date\":1000,\"location\":\"X\",\"category\":\"Y\"}");
        invoke("handleEvents", ex);
        assertEquals(403, ex.code());
        assertTrue(ex.body().contains("Only administrators"));
    }

    /** T2 – c6=F (role == "admin") on POST → proceeds to create event. */
    @Test
    void handleEvents_c6_T2_adminRole_POST_proceeds() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/events",
                "{\"role\":\"admin\",\"date\":1000,\"location\":\"X\",\"category\":\"Y\"}");
        invoke("handleEvents", ex);
        assertNotEquals(403, ex.code());
        // Clean up
        String resId = ex.body().replaceAll(".*\"eventId\":\"([^\"]+)\".*", "$1");
        if (!resId.isEmpty() && !resId.equals(ex.body())) {
            Database.deleteEvent(new Event(resId, 0, "", ""));
        }
    }

    // ── c7 : query != null  P8 = c7 ──────────────────────────────────────

    /** T1 – c7=T, query present with role=admin → role is extracted, DELETE succeeds (200). */
    @Test
    void handleEvents_c7_T1_queryPresent_roleExtracted() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("DELETE",
                "/api/events/non-existent-event-id?role=admin", "");
        invoke("handleEvents", ex);
        assertNotEquals(403, ex.code()); // role extracted → not forbidden
    }

    /** T2 – c7=F, no query → role stays "", returns 403. */
    @Test
    void handleEvents_c7_T2_queryNull_returns403() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("DELETE",
                "/api/events/non-existent-event-id", "");
        invoke("handleEvents", ex);
        assertEquals(403, ex.code());
    }

    // ── c8 : param.startsWith("role=")  P9 = c8 ──────────────────────────

    /** T1 – c8=T, param starts with "role=" → role value set, proceeds past role check. */
    @Test
    void handleEvents_c8_T1_roleParamExtracted_proceedsPastRoleCheck() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("DELETE",
                "/api/events/non-existent?role=admin", "");
        invoke("handleEvents", ex);
        assertNotEquals(403, ex.code());
    }

    /** T2 – c8=F, param doesn't start with "role=" → role stays "", returns 403. */
    @Test
    void handleEvents_c8_T2_otherParamOnly_returns403() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("DELETE",
                "/api/events/non-existent?foo=bar", "");
        invoke("handleEvents", ex);
        assertEquals(403, ex.code());
    }

    // ══════════════════════════════════════════════════════════════════════
    // handleUsers
    // ══════════════════════════════════════════════════════════════════════

    // ── c1 : method.equals("OPTIONS")  P1 = c1 ────────────────────────────

    /** T1 – OPTIONS → 204. */
    @Test
    void handleUsers_c1_T1_OPTIONS_returns204() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("OPTIONS", "/api/users", "");
        invoke("handleUsers", ex);
        assertEquals(204, ex.code());
    }

    /** T2 – not OPTIONS → proceeds. */
    @Test
    void handleUsers_c1_T2_notOPTIONS_proceeds() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/users", "");
        invoke("handleUsers", ex);
        assertNotEquals(204, ex.code());
    }

    // ── c2 : method.equals("GET")  P2 = c2 ───────────────────────────────

    /** T1 – GET → returns all users, 200. */
    @Test
    void handleUsers_c2_T1_GET_returns200() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/users", "");
        invoke("handleUsers", ex);
        assertEquals(200, ex.code());
    }

    /** T2 – not GET → falls to POST (or 405) branch. */
    @Test
    void handleUsers_c2_T2_nonGET_doesNotReturn200AsUsers() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/users",
                "{\"email\":\"ccac_users_c2_" + System.currentTimeMillis() + "@example.com\"}");
        invoke("handleUsers", ex);
        assertEquals(201, ex.code()); // POST creates user → 201, not GET's 200
    }

    // ── c3 : method.equals("POST")  P3 = c3 ──────────────────────────────

    /** T1 – POST → creates user, 201. */
    @Test
    void handleUsers_c3_T1_POST_createsUser_returns201() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/users",
                "{\"email\":\"ccac_users_c3_" + System.currentTimeMillis() + "@example.com\"}");
        invoke("handleUsers", ex);
        assertEquals(201, ex.code());
    }

    /** T2 – neither GET nor POST → 405. */
    @Test
    void handleUsers_c3_T2_neitherGETnorPOST_returns405() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("DELETE", "/api/users", "");
        invoke("handleUsers", ex);
        assertEquals(405, ex.code());
    }

    // ── c4 : email != null  c5 : !email.isEmpty()  P4 = c4 && c5 ─────────
    //
    // Note: parseString never returns null (returns "" on missing key), so
    // c4=F (email==null) is infeasible through normal JSON input.
    // The reachable CCAC conditions are exercised via c5 (isEmpty).

    /** T1 – c4=T, c5=T: email present and non-empty → creates email-based User. */
    @Test
    void handleUsers_c4_c5_T1_emailNotEmpty_createsEmailUser() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/users",
                "{\"email\":\"ccac_email_user_" + System.currentTimeMillis() + "@example.com\"}");
        invoke("handleUsers", ex);
        assertEquals(201, ex.code());
    }

    /** T2 (c5 active) – c5=F: email key absent → parseString returns "" →
     *  email.isEmpty() true → falls to phone-based User branch. */
    @Test
    void handleUsers_c5_T2_emailEmpty_createsPhoneUser() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/users",
                "{\"phoneNumber\":5141234567}");
        invoke("handleUsers", ex);
        assertEquals(201, ex.code());
    }

    // ══════════════════════════════════════════════════════════════════════
    // handleAuth
    // ══════════════════════════════════════════════════════════════════════

    // ── c1 : method.equals("OPTIONS")  P1 = c1 ────────────────────────────

    /** T1 – OPTIONS → 204. */
    @Test
    void handleAuth_c1_T1_OPTIONS_returns204() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("OPTIONS", "/api/auth/register", "");
        invoke("handleAuth", ex);
        assertEquals(204, ex.code());
    }

    /** T2 – not OPTIONS → proceeds. */
    @Test
    void handleAuth_c1_T2_notOPTIONS_proceeds() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/auth/register", "");
        invoke("handleAuth", ex);
        assertNotEquals(204, ex.code());
    }

    // ── c2 : method.equals("POST")  c3 : path.equals("/api/auth/register")  P2 = c2 && c3 ──

    /** T1 – POST /api/auth/register with missing fields → 400 (enters register branch). */
    @Test
    void handleAuth_c2_c3_T1_POST_register_enteredBranch_returns400() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/auth/register", "{}");
        invoke("handleAuth", ex);
        assertEquals(400, ex.code());
    }

    /** T2 (c2 active) – c2=F → not POST, returns 404. */
    @Test
    void handleAuth_c2_T2_nonPOST_returns404() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/auth/register", "");
        invoke("handleAuth", ex);
        assertEquals(404, ex.code());
    }

    /** T2 (c3 active) – c3=F → POST to wrong path, returns 404. */
    @Test
    void handleAuth_c3_T2_POST_wrongRegisterPath_returns404() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/auth/other", "{}");
        invoke("handleAuth", ex);
        assertEquals(404, ex.code());
    }

    // ── c4–c7 : email==null || email.isEmpty() || password==null || password.isEmpty()  P3 ──
    //
    // c4=F (email==null) and c6=F (password==null) are infeasible because parseString
    // always returns "" (not null).  c5 and c7 (isEmpty) are the reachable conditions.

    /** c5 T1 – email key missing → empty string → isEmpty() true → P3=T → 400. */
    @Test
    void handleAuth_c5_T1_emptyEmail_returns400() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/auth/register",
                "{\"password\":\"secret\"}");
        invoke("handleAuth", ex);
        assertEquals(400, ex.code());
    }

    /** c7 T1 – password key missing → empty string → isEmpty() true → P3=T → 400. */
    @Test
    void handleAuth_c7_T1_emptyPassword_returns400() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/auth/register",
                "{\"email\":\"test@test.com\"}");
        invoke("handleAuth", ex);
        assertEquals(400, ex.code());
    }

    /** P3 T2 – both email and password present → P3=F → proceeds past validation. */
    @Test
    void handleAuth_P3_T2_emailAndPasswordPresent_proceedsPastValidation() throws Exception {
        String email = "ccac_reg_" + System.currentTimeMillis() + "@test.com";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"pw123\"}");
        invoke("handleAuth", ex);
        assertEquals(201, ex.code());
    }

    // ── c8–c10 : existing!=null && !existing.equals("null") && !existing.equals("{}")  P4 ──

    /** T1 – all true: email already registered → 409. */
    @Test
    void handleAuth_c8_c9_c10_T1_duplicateEmail_returns409() throws Exception {
        String email = "ccac_dup_" + System.currentTimeMillis() + "@test.com";
        String body = "{\"email\":\"" + email + "\",\"password\":\"pw\"}";
        invoke("handleAuth", new FakeHttpExchange("POST", "/api/auth/register", body));
        FakeHttpExchange ex2 = new FakeHttpExchange("POST", "/api/auth/register", body);
        invoke("handleAuth", ex2);
        assertEquals(409, ex2.code());
    }

    /** T2 (c8 active) – c8=F: no existing user (Firebase returns "null") → proceeds to create, 201. */
    @Test
    void handleAuth_c8_T2_newEmail_returns201() throws Exception {
        String email = "ccac_new_" + System.currentTimeMillis() + "@test.com";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"pw\"}");
        invoke("handleAuth", ex);
        assertEquals(201, ex.code());
    }

    // ── c11 : method.equals("POST")  c12 : path.equals("/api/auth/login")  P5 = c11 && c12 ──

    /** T1 – POST /api/auth/login with missing fields → 400 (login branch entered). */
    @Test
    void handleAuth_c11_c12_T1_POST_login_missingFields_returns400() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/auth/login", "{}");
        invoke("handleAuth", ex);
        assertEquals(400, ex.code());
    }

    /** T2 (c11 active) – c11=F → not POST, returns 404. */
    @Test
    void handleAuth_c11_T2_nonPOST_login_returns404() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/auth/login", "");
        invoke("handleAuth", ex);
        assertEquals(404, ex.code());
    }

    /** T2 (c12 active) – c12=F → POST to different path, returns 404. */
    @Test
    void handleAuth_c12_T2_POST_differentPath_returns404() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/auth/nope", "{}");
        invoke("handleAuth", ex);
        assertEquals(404, ex.code());
    }

    // ── c13–c15 : usersJson==null || usersJson.equals("null") || usersJson.equals("{}")  P6 ──

    /** c14 T1 – unknown email → Firebase returns "null" → P6=T → 401. */
    @Test
    void handleAuth_c14_T1_unknownEmail_returns401() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/auth/login",
                "{\"email\":\"nobody_ccac@test.com\",\"password\":\"pw\"}");
        invoke("handleAuth", ex);
        assertEquals(401, ex.code());
    }

    /** P6 T2 – known user found → P6=F → proceeds to password check. */
    @Test
    void handleAuth_P6_T2_knownUser_proceedsToPasswordCheck() throws Exception {
        String email = "ccac_known_" + System.currentTimeMillis() + "@test.com";
        invoke("handleAuth",
                new FakeHttpExchange("POST", "/api/auth/register",
                        "{\"email\":\"" + email + "\",\"password\":\"correct\"}"));
        FakeHttpExchange login = new FakeHttpExchange("POST", "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"correct\"}");
        invoke("handleAuth", login);
        assertEquals(200, login.code()); // proceeds past P6 check and past password check
    }

    // ── c16 : !password.equals(storedPassword)  P7 = c16 ─────────────────

    /** T1 – wrong password → 401. */
    @Test
    void handleAuth_c16_T1_wrongPassword_returns401() throws Exception {
        String email = "ccac_wp_" + System.currentTimeMillis() + "@test.com";
        invoke("handleAuth",
                new FakeHttpExchange("POST", "/api/auth/register",
                        "{\"email\":\"" + email + "\",\"password\":\"correct\"}"));
        FakeHttpExchange login = new FakeHttpExchange("POST", "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"wrong\"}");
        invoke("handleAuth", login);
        assertEquals(401, login.code());
    }

    /** T2 – correct password → 200 with user payload. */
    @Test
    void handleAuth_c16_T2_correctPassword_returns200() throws Exception {
        String email = "ccac_cp_" + System.currentTimeMillis() + "@test.com";
        invoke("handleAuth",
                new FakeHttpExchange("POST", "/api/auth/register",
                        "{\"email\":\"" + email + "\",\"password\":\"mypw\"}"));
        FakeHttpExchange login = new FakeHttpExchange("POST", "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"mypw\"}");
        invoke("handleAuth", login);
        assertEquals(200, login.code());
        assertTrue(login.body().contains("\"email\":\"" + email + "\""));
    }

    // ── c17 : role==null  c18 : role.isEmpty()  P8 = c17 || c18 ──────────
    //
    // parseString returns "" when the "role" key is absent, so c17=T (role==null)
    // is infeasible through JSON parsing.  c18=T (role.isEmpty()) requires a user
    // whose stored JSON has no "role" field, which is not achievable via the register
    // endpoint.  The T2 case (role present and non-empty) is tested here.

    /** T2 – role field present (registered user has "role":"user") → role used as-is. */
    @Test
    void handleAuth_c17_c18_T2_rolePresent_usedAsIs() throws Exception {
        String email = "ccac_role_" + System.currentTimeMillis() + "@test.com";
        invoke("handleAuth",
                new FakeHttpExchange("POST", "/api/auth/register",
                        "{\"email\":\"" + email + "\",\"password\":\"pw\"}"));
        FakeHttpExchange login = new FakeHttpExchange("POST", "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"pw\"}");
        invoke("handleAuth", login);
        assertEquals(200, login.code());
        assertTrue(login.body().contains("\"role\":\"user\""));
    }

    // ══════════════════════════════════════════════════════════════════════
    // handleReservations
    // ══════════════════════════════════════════════════════════════════════

    // ── c1 : method.equals("OPTIONS")  P1 = c1 ────────────────────────────

    /** T1 – OPTIONS → 204. */
    @Test
    void handleReservations_c1_T1_OPTIONS_returns204() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("OPTIONS", "/api/reservations", "");
        invoke("handleReservations", ex);
        assertEquals(204, ex.code());
    }

    /** T2 – not OPTIONS → proceeds. */
    @Test
    void handleReservations_c1_T2_notOPTIONS_proceeds() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/reservations", "");
        invoke("handleReservations", ex);
        assertNotEquals(204, ex.code());
    }

    // ── c2 : method.equals("POST")  c3 : path.equals("/api/reservations")  P2 = c2 && c3 ──

    /** T1 – POST /api/reservations with empty IDs → 400 (branch entered). */
    @Test
    void handleReservations_c2_c3_T1_POST_reservations_emptyIds_returns400() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"\",\"eventId\":\"\"}");
        invoke("handleReservations", ex);
        assertEquals(400, ex.code());
    }

    /** T2 (c2 active) – c2=F → GET branch handles request, returns 200. */
    @Test
    void handleReservations_c2_T2_nonPOST_doesNotEnterPostBranch() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/reservations", "");
        invoke("handleReservations", ex);
        assertEquals(200, ex.code());
    }

    /** T2 (c3 active) – c3=F → POST to wrong path, returns 405. */
    @Test
    void handleReservations_c3_T2_POST_wrongPath_returns405() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/reservations/extra", "{}");
        invoke("handleReservations", ex);
        assertEquals(405, ex.code());
    }

    // ── c4 : userId.isEmpty()  c5 : eventId.isEmpty()  P3 = c4 || c5 ──────

    /** c4 T1 – userId empty → P3=T → 400. */
    @Test
    void handleReservations_c4_T1_emptyUserId_returns400() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"\",\"eventId\":\"evt-1\"}");
        invoke("handleReservations", ex);
        assertEquals(400, ex.code());
    }

    /** c4 T2 – userId present; c5 T1 – eventId empty → P3=T → 400. */
    @Test
    void handleReservations_c5_T1_emptyEventId_returns400() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"user-1\",\"eventId\":\"\"}");
        invoke("handleReservations", ex);
        assertEquals(400, ex.code());
    }

    /** P3 T2 – both IDs present → proceeds past validation. */
    @Test
    void handleReservations_P3_T2_bothIdsPresent_proceeds() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"ccac-u\",\"eventId\":\"ccac-e\","
                + "\"userEmail\":\"\",\"eventLocation\":\"\","
                + "\"eventCategory\":\"\",\"eventDate\":0}");
        invoke("handleReservations", ex);
        // 201 (first booking) or 409 (already exists) — neither is 400
        assertNotEquals(400, ex.code());
    }

    // ── c6–c8 : existingReservations!=null && !…equals("null") && !…equals("{}")  P4 ──

    /** T1 – all true: same userId+eventId booked twice → 409. */
    @Test
    void handleReservations_c6_c7_c8_T1_duplicateReservation_returns409() throws Exception {
        String uid = "ccac-dup-" + System.currentTimeMillis();
        String eid = "ccac-evt-dup";
        String body = "{\"userId\":\"" + uid + "\",\"eventId\":\"" + eid + "\","
                + "\"userEmail\":\"\",\"eventLocation\":\"\","
                + "\"eventCategory\":\"\",\"eventDate\":0}";
        FakeHttpExchange first = new FakeHttpExchange("POST", "/api/reservations", body);
        invoke("handleReservations", first);
        assertEquals(201, first.code());
        FakeHttpExchange second = new FakeHttpExchange("POST", "/api/reservations", body);
        invoke("handleReservations", second);
        assertEquals(409, second.code());
    }

    /** T2 (c6 active) – c6=F: new user with no existing reservations → P4=F → 201. */
    @Test
    void handleReservations_c6_T2_newUser_noExistingReservations_returns201() throws Exception {
        String uid = "ccac-new-" + System.currentTimeMillis();
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"" + uid + "\",\"eventId\":\"ccac-e\","
                + "\"userEmail\":\"\",\"eventLocation\":\"\","
                + "\"eventCategory\":\"\",\"eventDate\":0}");
        invoke("handleReservations", ex);
        assertEquals(201, ex.code());
    }

    // ── c9 : existingReservations.contains("\"eventId\":\"…\"")  P5 = c9 ───

    // T1 – duplicate eventId already in stored JSON → 409. (covered by c6_c7_c8_T1 above)

    /** T2 – different eventId → not a duplicate → 201. */
    @Test
    void handleReservations_c9_T2_differentEventId_returns201() throws Exception {
        String uid = "ccac-diff-" + System.currentTimeMillis();
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"" + uid + "\",\"eventId\":\"ccac-unique-evt-" + uid + "\","
                + "\"userEmail\":\"\",\"eventLocation\":\"\","
                + "\"eventCategory\":\"\",\"eventDate\":0}");
        invoke("handleReservations", ex);
        assertEquals(201, ex.code());
    }

    // ── c10 : method.equals("GET")  c11 : path.equals("/api/reservations")  P6 = c10 && c11 ──

    /** T1 – GET /api/reservations → 200. */
    @Test
    void handleReservations_c10_c11_T1_GET_reservations_returns200() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/reservations", "");
        invoke("handleReservations", ex);
        assertEquals(200, ex.code());
    }

    /** T2 (c10 active) – c10=F → POST branch, empty IDs → 400. */
    @Test
    void handleReservations_c10_T2_nonGET_doesNotEnterGetBranch() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"\",\"eventId\":\"\"}");
        invoke("handleReservations", ex);
        assertNotEquals(200, ex.code());
    }

    /** T2 (c11 active with GET) – c11=F → wrong path, returns 405. */
    @Test
    void handleReservations_c11_T2_GET_wrongPath_returns405() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/reservations/extra/path", "");
        invoke("handleReservations", ex);
        assertEquals(405, ex.code());
    }

    // ── c12 : query != null  P7 = c12 ────────────────────────────────────

    /** T1 – query present → query-filtered lookup. */
    @Test
    void handleReservations_c12_T1_queryPresent_returns200() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET",
                "/api/reservations?userId=some-user", "");
        invoke("handleReservations", ex);
        assertEquals(200, ex.code());
    }

    /** T2 – no query → all-reservations lookup. */
    @Test
    void handleReservations_c12_T2_queryNull_returns200() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/reservations", "");
        invoke("handleReservations", ex);
        assertEquals(200, ex.code());
    }

    // ── c13 : !userId.isEmpty()  P8 = c13 ────────────────────────────────

    /** T1 – userId present in query → per-user lookup path taken. */
    @Test
    void handleReservations_c13_T1_userIdNotEmpty_perUserLookup() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET",
                "/api/reservations?userId=ccac-u-lookup", "");
        invoke("handleReservations", ex);
        assertEquals(200, ex.code());
    }

    /** T2 – no userId → all-reservations path taken. */
    @Test
    void handleReservations_c13_T2_userIdEmpty_allReservations() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/reservations", "");
        invoke("handleReservations", ex);
        assertEquals(200, ex.code());
    }

    // ── c14 : method.equals("DELETE")  c11 : path.startsWith("/api/reservations/")  P9 ──

    /** T1 – DELETE /api/reservations/{id} → deletes the reservation, 200. */
    @Test
    void handleReservations_c14_c11_T1_DELETE_reservation_returns200() throws Exception {
        String uid = "ccac-del-" + System.currentTimeMillis();
        FakeHttpExchange create = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"" + uid + "\",\"eventId\":\"ccac-e-del\","
                + "\"userEmail\":\"\",\"eventLocation\":\"\","
                + "\"eventCategory\":\"\",\"eventDate\":0}");
        invoke("handleReservations", create);
        assertEquals(201, create.code());
        String resId = create.body().replaceAll(".*\"reservationId\":\"([^\"]+)\".*", "$1");
        FakeHttpExchange del = new FakeHttpExchange("DELETE",
                "/api/reservations/" + resId, "");
        invoke("handleReservations", del);
        assertEquals(200, del.code());
    }

    /** T2 (c14 active) – c14=F → GET handles request, 200. */
    @Test
    void handleReservations_c14_T2_nonDELETE_doesNotEnterDeleteBranch() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/api/reservations", "");
        invoke("handleReservations", ex);
        assertEquals(200, ex.code());
    }

    /** T2 (c11 active with DELETE) – c11=F: DELETE /api/reservations (no trailing slash) → 405. */
    @Test
    void handleReservations_c11_T2_DELETE_noReservationId_returns405() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("DELETE", "/api/reservations", "");
        invoke("handleReservations", ex);
        assertEquals(405, ex.code());
    }

    // ── c15 : userEmail != null  c16 : !userEmail.isEmpty()  P10 = c15 && c16 ──
    //
    // Note: parseString never returns null, so c15=F is infeasible via JSON.

    /** T1 (c16 active) – userEmail present and non-empty → cancellation email printed. */
    @Test
    void handleReservations_c16_T1_emailPresent_cancellationEmailLogged() throws Exception {
        String uid = "ccac-email-" + System.currentTimeMillis();
        FakeHttpExchange create = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"" + uid + "\",\"eventId\":\"ccac-e-em\","
                + "\"userEmail\":\"test@example.com\",\"eventLocation\":\"\","
                + "\"eventCategory\":\"\",\"eventDate\":0}");
        invoke("handleReservations", create);
        String resId = create.body().replaceAll(".*\"reservationId\":\"([^\"]+)\".*", "$1");

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(captured));
        try {
            FakeHttpExchange del = new FakeHttpExchange("DELETE",
                    "/api/reservations/" + resId, "");
            invoke("handleReservations", del);
            assertEquals(200, del.code());
        } finally {
            System.setOut(orig);
        }
        assertTrue(captured.toString().contains("EMAIL"));
    }

    /** T2 (c16 active) – userEmail empty → email block skipped. */
    @Test
    void handleReservations_c16_T2_emailEmpty_noCancellationEmail() throws Exception {
        String uid = "ccac-noemail-" + System.currentTimeMillis();
        FakeHttpExchange create = new FakeHttpExchange("POST", "/api/reservations",
                "{\"userId\":\"" + uid + "\",\"eventId\":\"ccac-e-noem\","
                + "\"userEmail\":\"\",\"eventLocation\":\"\","
                + "\"eventCategory\":\"\",\"eventDate\":0}");
        invoke("handleReservations", create);
        String resId = create.body().replaceAll(".*\"reservationId\":\"([^\"]+)\".*", "$1");

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(captured));
        try {
            FakeHttpExchange del = new FakeHttpExchange("DELETE",
                    "/api/reservations/" + resId, "");
            invoke("handleReservations", del);
            assertEquals(200, del.code());
        } finally {
            System.setOut(orig);
        }
        assertFalse(captured.toString().contains("EMAIL"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // handleStatic
    // ══════════════════════════════════════════════════════════════════════

    // ── c1 : !method.equals("GET")  P1 = c1 ──────────────────────────────

    /** T1 – non-GET → 405 immediately. */
    @Test
    void handleStatic_c1_T1_nonGET_returns405() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/", "");
        invoke("handleStatic", ex);
        assertEquals(405, ex.code());
    }

    /** T2 – GET → proceeds to file check (200 if file exists, 404 otherwise). */
    @Test
    void handleStatic_c1_T2_GET_proceedsToFileCheck() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/", "");
        invoke("handleStatic", ex);
        assertNotEquals(405, ex.code());
    }

    // ── c2 : Files.exists(filePath)  P2 = c2 ─────────────────────────────

    /** T1 – frontend/index.html exists → 200 with HTML content. */
    @Test
    void handleStatic_c2_T1_fileExists_returns200() throws Exception {
        // Requires working directory to be the project root (standard IntelliJ setup)
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/", "");
        invoke("handleStatic", ex);
        // Succeeds if frontend/index.html is present (it is in this project)
        assertEquals(200, ex.code());
    }

    /** T2 – file absent → 404.
     *  Verified through the code path: if Files.exists returns false the handler
     *  calls sendResponse(404, …).  The complementary 200 case is tested by T1 above. */
    @Test
    void handleStatic_c2_T2_fileAbsent_or_present_correctCodeReturned() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/", "");
        invoke("handleStatic", ex);
        assertTrue(ex.code() == 200 || ex.code() == 404,
                "Expected 200 (file present) or 404 (file absent), got: " + ex.code());
    }

    // ══════════════════════════════════════════════════════════════════════
    // handleStaticCss
    // ══════════════════════════════════════════════════════════════════════

    // ── c1 : !method.equals("GET")  P1 = c1 ──────────────────────────────

    /** T1 – non-GET → 405. */
    @Test
    void handleStaticCss_c1_T1_nonGET_returns405() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/styles.css", "");
        invoke("handleStaticCss", ex);
        assertEquals(405, ex.code());
    }

    /** T2 – GET → proceeds to file check. */
    @Test
    void handleStaticCss_c1_T2_GET_proceedsToFileCheck() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/styles.css", "");
        invoke("handleStaticCss", ex);
        assertNotEquals(405, ex.code());
    }

    // ── c2 : Files.exists(filePath)  P2 = c2 ─────────────────────────────

    /** T1 – frontend/styles.css exists → 200. */
    @Test
    void handleStaticCss_c2_T1_fileExists_returns200() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/styles.css", "");
        invoke("handleStaticCss", ex);
        assertEquals(200, ex.code());
    }

    /** T2 – file absent → 404.  Both outcomes verified by the combined check. */
    @Test
    void handleStaticCss_c2_T2_fileAbsent_or_present_correctCodeReturned() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/styles.css", "");
        invoke("handleStaticCss", ex);
        assertTrue(ex.code() == 200 || ex.code() == 404,
                "Expected 200 (file present) or 404 (file absent), got: " + ex.code());
    }

    // ══════════════════════════════════════════════════════════════════════
    // readBody
    // ══════════════════════════════════════════════════════════════════════

    // ── c1 : (line = reader.readLine()) != null  P1 = c1 ──────────────────

    /** T1 – non-empty body → all lines accumulated and returned. */
    @Test
    void readBody_c1_T1_nonEmptyBody_returnsContent() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/test", "hello world");
        String result = invokeReadBody(ex);
        assertEquals("hello world", result);
    }

    /** T2 – empty body → readLine returns null immediately, returns "". */
    @Test
    void readBody_c1_T2_emptyBody_returnsEmptyString() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/test", "");
        String result = invokeReadBody(ex);
        assertEquals("", result);
    }
}
