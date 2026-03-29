package main;

public class User {
    private String userId;
    private String email;
    private long phoneNumber;
    private String password;
    private String role;

    public User(String email, long phoneNumber, String password, String role) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.role = role;
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.role = "user";
    }

    public User(String email) {
        this.email = email;
        this.role = "user";
    }

    public User(long phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.role = "user";
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public long getPhoneNumber() { return phoneNumber; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String toJson() {
        return "{\"email\":\"" + (email != null ? email : "") + "\""
             + ",\"phoneNumber\":" + phoneNumber
             + ",\"password\":\"" + (password != null ? password : "") + "\""
             + ",\"role\":\"" + (role != null ? role : "user") + "\"}";
    }
}
