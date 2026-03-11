public class Event {
    private String eventId;
    private long date;
    private String location;
    private String category;

    public Event(String eventId, long date, String location, String category) {
        this.eventId = eventId;
        this.date = date;
        this.location = location;
        this.category = category;
    }

    public Event(long date, String location, String category) {
        this.date = date;
        this.location = location;
        this.category = category;
    }

    public String getEventId() {
        return eventId;
    }

    public long getDate() {
        return date;
    }

    public String getLocation() {
        return location;
    }

    public String getCategory() {
        return category;
    }

    public String toJson() {
        return "{\"date\":" + date + ",\"location\":\"" + location + "\",\"category\":\"" + category + "\"}";
    }
}
