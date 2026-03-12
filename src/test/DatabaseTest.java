package test;

import main.Database;
import main.Event;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    @org.junit.jupiter.api.Test
    void addEvent() throws Exception {
        Event testEvent = new Event(1000, "Montreal", "Music");

        testEvent.setEventId(Database.addEvent(testEvent));
        Database.deleteEvent(testEvent);
    }

}