package model;

public class User {
    private final String uid; // Firebase User ID
    private final String email;
    private String displayName; // Optional: if you store it

    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
    }

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
    }

    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}