public class User {
    private String email;
    private long phoneNumber;

    public User(String email, long phoneNumber) {
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public long getPhoneNumber() {
        return phoneNumber;
    }

    public String toJson() {
        return "{\"email\":\"" + email + "\",\"phoneNumber\":" + phoneNumber + "}";
    }
}
