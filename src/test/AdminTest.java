package test;

import main.Admin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    // ── Role is always "admin" ─────────────────────────────────────────────

    @Test
    void testEmailPasswordConstructorSetsAdminRole() {
        Admin admin = new Admin("admin@example.com", "securepass");
        assertEquals("admin", admin.getRole());
    }

    @Test
    void testEmailOnlyConstructorSetsAdminRole() {
        Admin admin = new Admin("admin2@example.com");
        assertEquals("admin", admin.getRole());
    }

    @Test
    void testPhoneConstructorSetsAdminRole() {
        Admin admin = new Admin(5141234567L);
        assertEquals("admin", admin.getRole());
    }

    // ── Inherits User fields correctly ─────────────────────────────────────

    @Test
    void testEmailIsStoredCorrectly() {
        Admin admin = new Admin("superadmin@example.com", "pw");
        assertEquals("superadmin@example.com", admin.getEmail());
    }

    @Test
    void testPasswordIsStoredCorrectly() {
        Admin admin = new Admin("a@b.com", "mypassword");
        assertEquals("mypassword", admin.getPassword());
    }

    @Test
    void testPhoneNumberIsStoredCorrectly() {
        Admin admin = new Admin(9998887777L);
        assertEquals(9998887777L, admin.getPhoneNumber());
    }

    // ── toJson reflects admin role ─────────────────────────────────────────

    @Test
    void testToJsonContainsAdminRole() {
        Admin admin = new Admin("admin@site.com", "pw");
        assertTrue(admin.toJson().contains("\"role\":\"admin\""));
    }

    @Test
    void testToJsonContainsEmail() {
        Admin admin = new Admin("admin@site.com", "pw");
        assertTrue(admin.toJson().contains("\"email\":\"admin@site.com\""));
    }

    // ── setUserId inherited ────────────────────────────────────────────────

    @Test
    void testSetUserIdInherited() {
        Admin admin = new Admin("a@b.com");
        admin.setUserId("admin-uid-001");
        assertEquals("admin-uid-001", admin.getUserId());
    }
}
