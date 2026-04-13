package test;

import main.Database;
import main.Event;
import main.Reservation;
import main.User;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Database.java.
 * These tests hit the live Firebase REST API.
 * Each test cleans up after itself so the database stays clean.
 *
 * Requires: config.properties with valid firebase.database.url and firebase.database.secret
 */
class DatabaseTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUpPrint() {
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void restorePrint() {
        System.setOut(originalOut);
    }

    // ── Event tests ────────────────────────────────────────────────────────

    @Test
    void testAddEvent() throws Exception {
        Event event = new Event(1000L, "Montreal", "Music");
        event.setEventId(Database.addEvent(event));
        Database.deleteEvent(event);

        assertTrue(outContent.toString().contains("addEvent response: 200"));
        assertTrue(outContent.toString().contains("deleteEvent response: 200"));
    }

    @Test
    void testGetAllEventsReturnsJson() throws Exception {
        String result = Database.getAllEvents();
        assertNotNull(result);
        // Firebase returns either "null" or a JSON object
        assertTrue(result.equals("null") || result.startsWith("{"));
    }

    @Test
    void testUpdateEvent() throws Exception {
        // Add an event, update it, then delete it
        Event event = new Event(2000L, "Toronto", "Sports");
        event.setEventId(Database.addEvent(event));

        Event updated = new Event(event.getEventId(), 3000L, "Ottawa", "Concert");
        Database.updateEvent(updated);

        Database.deleteEvent(updated);

        assertTrue(outContent.toString().contains("updateEvent response: 200"));
        assertTrue(outContent.toString().contains("deleteEvent response: 200"));
    }

    @Test
    void testDeleteEvent() throws Exception {
        Event event = new Event(5000L, "Quebec", "Theater");
        event.setEventId(Database.addEvent(event));
        Database.deleteEvent(event);

        assertTrue(outContent.toString().contains("deleteEvent response: 200"));
    }

    // ── User tests ─────────────────────────────────────────────────────────

    @Test
    void testAddUser() throws Exception {
        User user = new User("testuser_" + System.currentTimeMillis() + "@example.com", "pw");
        String userId = Database.addUser(user);

        assertNotNull(userId);
        assertFalse(userId.isEmpty());
    }

    @Test
    void testGetAllUsersReturnsJson() throws Exception {
        String result = Database.getAllUsers();
        assertNotNull(result);
        assertTrue(result.equals("null") || result.startsWith("{"));
    }

    @Test
    void testGetUserByEmail() throws Exception {
        String uniqueEmail = "lookup_" + System.currentTimeMillis() + "@example.com";
        User user = new User(uniqueEmail, "pw");
        Database.addUser(user);

        String result = Database.getUserByEmail(uniqueEmail);
        assertNotNull(result);
        assertTrue(result.contains(uniqueEmail));
    }

    // ── Reservation tests ──────────────────────────────────────────────────

    @Test
    void testAddReservation() throws Exception {
        Reservation reservation = new Reservation("user-test", "evt-test", "test@example.com");
        String reservationId = Database.addReservation(reservation);

        assertNotNull(reservationId);
        assertFalse(reservationId.isEmpty());

        // Cleanup
        Database.deleteReservation(reservationId);
    }

    @Test
    void testGetAllReservationsReturnsJson() throws Exception {
        String result = Database.getAllReservations();
        assertNotNull(result);
        assertTrue(result.equals("null") || result.startsWith("{"));
    }

    @Test
    void testGetReservationsByUser() throws Exception {
        String userId = "user-filter-" + System.currentTimeMillis();
        Reservation reservation = new Reservation(userId, "evt-filter", "filter@example.com");
        String reservationId = Database.addReservation(reservation);

        String result = Database.getReservationsByUser(userId);
        assertNotNull(result);
        assertTrue(result.contains(userId));

        // Cleanup
        Database.deleteReservation(reservationId);
    }

    @Test
    void testDeleteReservation() throws Exception {
        Reservation reservation = new Reservation("u-del", "e-del", "del@example.com");
        String reservationId = Database.addReservation(reservation);
        Database.deleteReservation(reservationId);

        assertTrue(outContent.toString().contains("deleteReservation response: 200"));
    }

    @Test
    void testGetReservation() throws Exception {
        Reservation reservation = new Reservation("u-get", "e-get", "get@example.com");
        String reservationId = Database.addReservation(reservation);

        String result = Database.getReservation(reservationId);
        assertNotNull(result);
        assertTrue(result.contains("u-get"));

        // Cleanup
        Database.deleteReservation(reservationId);
    }
}