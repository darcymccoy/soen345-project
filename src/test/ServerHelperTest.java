package test;

import main.Server;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Server's private static helper methods.
 * Uses reflection to access parseString, parseLong, and escapeJson.
 */
class ServerHelperTest {

    // ── Reflection helpers ─────────────────────────────────────────────────

    private String invokeParseString(String json, String key) throws Exception {
        Method m = Server.class.getDeclaredMethod("parseString", String.class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, json, key);
    }

    private long invokeParseLong(String json, String key) throws Exception {
        Method m = Server.class.getDeclaredMethod("parseLong", String.class, String.class);
        m.setAccessible(true);
        return (long) m.invoke(null, json, key);
    }

    private String invokeEscapeJson(String value) throws Exception {
        Method m = Server.class.getDeclaredMethod("escapeJson", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, value);
    }

    // ── parseString ────────────────────────────────────────────────────────

    @Test
    void testParseStringBasic() throws Exception {
        String json = "{\"role\":\"admin\",\"location\":\"Montreal\"}";
        assertEquals("admin", invokeParseString(json, "role"));
        assertEquals("Montreal", invokeParseString(json, "location"));
    }

    @Test
    void testParseStringMissingKeyReturnsEmpty() throws Exception {
        String json = "{\"role\":\"admin\"}";
        assertEquals("", invokeParseString(json, "nonexistent"));
    }

    @Test
    void testParseStringEmptyValue() throws Exception {
        String json = "{\"email\":\"\"}";
        assertEquals("", invokeParseString(json, "email"));
    }

    @Test
    void testParseStringWithWhitespace() throws Exception {
        String json = "{\"category\" : \"Concert\"}";
        assertEquals("Concert", invokeParseString(json, "category"));
    }

    // ── parseLong ──────────────────────────────────────────────────────────

    @Test
    void testParseLongBasic() throws Exception {
        String json = "{\"date\":1700000000}";
        assertEquals(1700000000L, invokeParseLong(json, "date"));
    }

    @Test
    void testParseLongMissingKeyReturnsZero() throws Exception {
        String json = "{\"date\":1000}";
        assertEquals(0L, invokeParseLong(json, "nonexistent"));
    }

    @Test
    void testParseLongPhoneNumber() throws Exception {
        String json = "{\"phoneNumber\":5141234567}";
        assertEquals(5141234567L, invokeParseLong(json, "phoneNumber"));
    }

    @Test
    void testParseLongZeroValue() throws Exception {
        String json = "{\"date\":0}";
        assertEquals(0L, invokeParseLong(json, "date"));
    }

    // ── escapeJson ─────────────────────────────────────────────────────────

    @Test
    void testEscapeJsonNull() throws Exception {
        assertEquals("", invokeEscapeJson(null));
    }

    @Test
    void testEscapeJsonPlainString() throws Exception {
        assertEquals("hello", invokeEscapeJson("hello"));
    }

    @Test
    void testEscapeJsonDoubleQuote() throws Exception {
        assertEquals("say \\\"hi\\\"", invokeEscapeJson("say \"hi\""));
    }

    @Test
    void testEscapeJsonBackslash() throws Exception {
        assertEquals("C:\\\\path", invokeEscapeJson("C:\\path"));
    }

    @Test
    void testEscapeJsonNewline() throws Exception {
        assertEquals("line1\\nline2", invokeEscapeJson("line1\nline2"));
    }

    @Test
    void testEscapeJsonCarriageReturn() throws Exception {
        assertEquals("line1\\rline2", invokeEscapeJson("line1\rline2"));
    }

    @Test
    void testEscapeJsonTab() throws Exception {
        assertEquals("col1\\tcol2", invokeEscapeJson("col1\tcol2"));
    }

    @Test
    void testEscapeJsonMixedSpecialChars() throws Exception {
        String input = "msg\n\"value\"\t\\end";
        String expected = "msg\\n\\\"value\\\"\\t\\\\end";
        assertEquals(expected, invokeEscapeJson(input));
    }
}
