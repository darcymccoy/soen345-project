package test;

import main.Event;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    // ── Constructor tests ──────────────────────────────────────────────────

    @Test
    void testFullConstructor() {
        Event event = new Event("evt-1", 1700000000L, "Montreal", "Concert");
        assertEquals("evt-1", event.getEventId());
        assertEquals(1700000000L, event.getDate());
        assertEquals("Montreal", event.getLocation());
        assertEquals("Concert", event.getCategory());
    }

    @Test
    void testConstructorWithoutId() {
        Event event = new Event(1700000000L, "Toronto", "Sports");
        assertNull(event.getEventId());
        assertEquals(1700000000L, event.getDate());
        assertEquals("Toronto", event.getLocation());
        assertEquals("Sports", event.getCategory());
    }

    // ── setEventId ─────────────────────────────────────────────────────────

    @Test
    void testSetEventId() {
        Event event = new Event(1000L, "Quebec City", "Movie");
        event.setEventId("evt-99");
        assertEquals("evt-99", event.getEventId());
    }

    // ── Getters ────────────────────────────────────────────────────────────

    @Test
    void testGetDate() {
        Event event = new Event(9876543210L, "Ottawa", "Theater");
        assertEquals(9876543210L, event.getDate());
    }

    @Test
    void testGetLocation() {
        Event event = new Event(0L, "Vancouver", "Music");
        assertEquals("Vancouver", event.getLocation());
    }

    @Test
    void testGetCategory() {
        Event event = new Event(0L, "Calgary", "Comedy");
        assertEquals("Comedy", event.getCategory());
    }

    // ── toJson ─────────────────────────────────────────────────────────────

    @Test
    void testToJsonContainsDate() {
        Event event = new Event(1700000000L, "Montreal", "Concert");
        assertTrue(event.toJson().contains("\"date\":1700000000"));
    }

    @Test
    void testToJsonContainsLocation() {
        Event event = new Event(1000L, "Montreal", "Concert");
        assertTrue(event.toJson().contains("\"location\":\"Montreal\""));
    }

    @Test
    void testToJsonContainsCategory() {
        Event event = new Event(1000L, "Montreal", "Concert");
        assertTrue(event.toJson().contains("\"category\":\"Concert\""));
    }

    @Test
    void testToJsonStructure() {
        Event event = new Event(1000L, "Halifax", "Jazz");
        String json = event.toJson();
        // Ensure it starts and ends like a JSON object
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }

    @Test
    void testToJsonDoesNotContainEventId() {
        // eventId is not stored in the Firebase node body
        Event event = new Event(1000L, "Halifax", "Jazz");
        assertFalse(event.toJson().contains("\"eventId\""));
    }
}
