package main;

public class Admin extends User {
    public Admin(String email, String password) {
        super(email, password);
        setRole("admin");
    }

    public Admin(String email) {
        super(email);
        setRole("admin");
    }

    public Admin(long phoneNumber) {
        super(phoneNumber);
        setRole("admin");
    }
}
