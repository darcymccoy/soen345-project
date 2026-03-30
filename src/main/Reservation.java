package main;

public class Reservation {
    private String reservationId;
    private String userId;
    private String eventId;
    private String userEmail;
    private long reservedAt;

    public Reservation(String userId, String eventId, String userEmail) {
        this.userId = userId;
        this.eventId = eventId;
        this.userEmail = userEmail;
        this.reservedAt = System.currentTimeMillis();
    }

    public Reservation(String reservationId, String userId, String eventId, String userEmail, long reservedAt) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.eventId = eventId;
        this.userEmail = userEmail;
        this.reservedAt = reservedAt;
    }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public String getUserId() { return userId; }
    public String getEventId() { return eventId; }
    public String getUserEmail() { return userEmail; }
    public long getReservedAt() { return reservedAt; }

    public String toJson() {
        return "{\"userId\":\"" + userId + "\""
             + ",\"eventId\":\"" + eventId + "\""
             + ",\"userEmail\":\"" + (userEmail != null ? userEmail : "") + "\""
             + ",\"reservedAt\":" + reservedAt + "}";
    }
}
