package test;

import main.Database;
import main.Event;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpTestPrint() {
        System.setOut(new PrintStream(outContent));
    }

    @org.junit.jupiter.api.Test
    void testAddEvent() throws Exception {
        Event testEvent = new Event(1000, "Montreal", "Music");

        testEvent.setEventId(Database.addEvent(testEvent));
        Database.deleteEvent(testEvent);

        assertTrue(outContent.toString().contains("addEvent response: 200"));
        assertTrue(outContent.toString().contains("deleteEvent response: 200"));
    }

    @AfterEach
    public void restorePrint() {
        System.setOut(originalOut);
    }

}