package test;

import main.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    // ── Constructor tests ──────────────────────────────────────────────────

    @Test
    void testEmailPasswordConstructor() {
        User user = new User("alice@example.com", "secret");
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals("user", user.getRole());
    }

    @Test
    void testEmailOnlyConstructor() {
        User user = new User("bob@example.com");
        assertEquals("bob@example.com", user.getEmail());
        assertNull(user.getPassword());
        assertEquals("user", user.getRole());
    }

    @Test
    void testPhoneNumberConstructor() {
        User user = new User(5141234567L);
        assertEquals(5141234567L, user.getPhoneNumber());
        assertEquals("user", user.getRole());
        assertNull(user.getEmail());
    }

    @Test
    void testFullConstructor() {
        User user = new User("carol@example.com", 5149876543L, "pass123", "admin");
        assertEquals("carol@example.com", user.getEmail());
        assertEquals(5149876543L, user.getPhoneNumber());
        assertEquals("pass123", user.getPassword());
        assertEquals("admin", user.getRole());
    }

    // ── Default role ───────────────────────────────────────────────────────

    @Test
    void testDefaultRoleIsUser() {
        User u1 = new User("a@b.com", "pw");
        User u2 = new User("a@b.com");
        User u3 = new User(123456789L);
        assertEquals("user", u1.getRole());
        assertEquals("user", u2.getRole());
        assertEquals("user", u3.getRole());
    }

    // ── setRole ────────────────────────────────────────────────────────────

    @Test
    void testSetRole() {
        User user = new User("d@e.com", "pw");
        user.setRole("admin");
        assertEquals("admin", user.getRole());
    }

    // ── setUserId ──────────────────────────────────────────────────────────

    @Test
    void testSetUserId() {
        User user = new User("f@g.com");
        user.setUserId("uid-001");
        assertEquals("uid-001", user.getUserId());
    }

    // ── toJson ─────────────────────────────────────────────────────────────

    @Test
    void testToJsonContainsEmail() {
        User user = new User("test@test.com", "pw");
        assertTrue(user.toJson().contains("\"email\":\"test@test.com\""));
    }

    @Test
    void testToJsonContainsRole() {
        User user = new User("x@y.com", "pw");
        assertTrue(user.toJson().contains("\"role\":\"user\""));
    }

    @Test
    void testToJsonContainsPassword() {
        User user = new User("x@y.com", "mypassword");
        assertTrue(user.toJson().contains("\"password\":\"mypassword\""));
    }

    @Test
    void testToJsonWithNullEmail() {
        User user = new User(9999999L);
        // Should not throw, email defaults to empty string in JSON
        String json = user.toJson();
        assertTrue(json.contains("\"email\":\"\""));
    }

    @Test
    void testToJsonWithNullPassword() {
        User user = new User("a@b.com");
        String json = user.toJson();
        assertTrue(json.contains("\"password\":\"\""));
    }

    // ── CCAC: toJson  c3 = role != null ───────────────────────────────────

    /** c3 T1 – role == null → toJson substitutes the "user" default. */
    @Test
    void testToJsonRoleNull_defaultsToUser() {
        // Full constructor allows passing null role
        User user = new User("a@b.com", 0L, "pw", null);
        String json = user.toJson();
        assertTrue(json.contains("\"role\":\"user\""),
                "Expected default role 'user' when role is null, got: " + json);
    }

    /** c3 T2 – role != null → toJson uses the stored role value. */
    @Test
    void testToJsonRoleNotNull_usesStoredRole() {
        User user = new User("a@b.com", 0L, "pw", "admin");
        String json = user.toJson();
        assertTrue(json.contains("\"role\":\"admin\""));
    }
}
