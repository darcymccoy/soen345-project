package main;

public class User {
    private String email;
    private long phoneNumber;

    public User(String email) {
        this.email = email;
    }

    public User(long phoneNumber){
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
