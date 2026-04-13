package test;

import main.Reservation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    // ── Constructor tests ──────────────────────────────────────────────────

    @Test
    void testShortConstructor() {
        long before = System.currentTimeMillis();
        Reservation r = new Reservation("user-1", "evt-1", "alice@example.com");
        long after = System.currentTimeMillis();

        assertEquals("user-1", r.getUserId());
        assertEquals("evt-1", r.getEventId());
        assertEquals("alice@example.com", r.getUserEmail());
        // reservedAt must be set automatically within the test window
        assertTrue(r.getReservedAt() >= before && r.getReservedAt() <= after);
    }

    @Test
    void testFullConstructor() {
        Reservation r = new Reservation("res-99", "user-2", "evt-2", "bob@example.com", 1700000000L);
        assertEquals("res-99", r.getReservationId());
        assertEquals("user-2", r.getUserId());
        assertEquals("evt-2", r.getEventId());
        assertEquals("bob@example.com", r.getUserEmail());
        assertEquals(1700000000L, r.getReservedAt());
    }

    // ── setReservationId ───────────────────────────────────────────────────

    @Test
    void testSetReservationId() {
        Reservation r = new Reservation("user-3", "evt-3", "carol@example.com");
        r.setReservationId("res-001");
        assertEquals("res-001", r.getReservationId());
    }

    // ── reservedAt is auto-set ─────────────────────────────────────────────

    @Test
    void testReservedAtIsPositive() {
        Reservation r = new Reservation("u", "e", "email@test.com");
        assertTrue(r.getReservedAt() > 0);
    }

    // ── toJson ─────────────────────────────────────────────────────────────

    @Test
    void testToJsonContainsUserId() {
        Reservation r = new Reservation("user-10", "evt-10", "x@y.com");
        assertTrue(r.toJson().contains("\"userId\":\"user-10\""));
    }

    @Test
    void testToJsonContainsEventId() {
        Reservation r = new Reservation("user-10", "evt-10", "x@y.com");
        assertTrue(r.toJson().contains("\"eventId\":\"evt-10\""));
    }

    @Test
    void testToJsonContainsUserEmail() {
        Reservation r = new Reservation("user-10", "evt-10", "hello@world.com");
        assertTrue(r.toJson().contains("\"userEmail\":\"hello@world.com\""));
    }

    @Test
    void testToJsonWithNullEmail() {
        Reservation r = new Reservation("u", "e", null);
        // Should not throw; email defaults to empty string
        String json = r.toJson();
        assertTrue(json.contains("\"userEmail\":\"\""));
    }

    @Test
    void testToJsonContainsReservedAt() {
        Reservation r = new Reservation("res-1", "u", "e", "m@m.com", 555555L);
        assertTrue(r.toJson().contains("\"reservedAt\":555555"));
    }

    @Test
    void testToJsonStructure() {
        Reservation r = new Reservation("u", "e", "a@b.com");
        String json = r.toJson();
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }
}
