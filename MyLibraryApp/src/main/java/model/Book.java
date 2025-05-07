package model;

public class Book {
    private String id; // Firebase keys are strings
    private final String title;
    private final String author;
    private final int year;

    // Constructor for retrieving books from Firebase (ID is known)
    public Book(String id, String title, String author, int year) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.year = year;
    }

    // Constructor for creating a new book before adding to Firebase (ID is not yet known)
    public Book(String title, String author, int year) {
        // this.id will be null initially, can be set by Firebase or when retrieved
        this.title = title;
        this.author = author;
        this.year = year;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getYear() { return year; }

    // Setter for ID - might be used if the Book object needs to be updated with Firebase-generated ID
    // However, with ValueEventListener, new Book objects are typically created from snapshots.
    public void setId(String id) {
        this.id = id;
    }
}