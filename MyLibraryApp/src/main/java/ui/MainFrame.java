package ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import database.DatabaseHandler;
import model.Book;
import model.User; 

public class MainFrame extends JFrame {
    private final DatabaseHandler dbHandler;
    private final JTextArea bookListArea;
    private final JTextField titleField, authorField, yearField;
    private ValueEventListener booksListener;
    private final JButton logoutButton;
    private final User currentUser; 


    public MainFrame(User user) {
        this.currentUser = user; 
        dbHandler = new DatabaseHandler();

        if (dbHandler.isInitializationFailed()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "Firebase initialization failed. Application may not function correctly.\nPlease check console output and serviceAccountKey.json.",
                        "Initialization Error", JOptionPane.ERROR_MESSAGE);
            });
        }

        setTitle("Library Management System - User: " + currentUser.getEmail()); 
        setSize(650, 500); 
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); 
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performCleanupAndExit();
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logoutAction());
        JPanel logoutButtonPanel = new JPanel(); 
        logoutButtonPanel.add(logoutButton);
        topPanel.add(logoutButtonPanel, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);


        bookListArea = new JTextArea(15, 50);
        bookListArea.setEditable(false);
        bookListArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(bookListArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputFormPanel = new JPanel(new GridBagLayout());
        inputFormPanel.setBorder(BorderFactory.createTitledBorder("Add New Book to Your Library"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; inputFormPanel.add(new JLabel("Title:"), gbc);
        titleField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; inputFormPanel.add(titleField, gbc);
        gbc.weightx = 0; 

        gbc.gridx = 0; gbc.gridy = 1; inputFormPanel.add(new JLabel("Author:"), gbc);
        authorField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; inputFormPanel.add(authorField, gbc);
        gbc.weightx = 0;

        gbc.gridx = 0; gbc.gridy = 2; inputFormPanel.add(new JLabel("Year:"), gbc);
        yearField = new JTextField(5);
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.LINE_START; inputFormPanel.add(yearField, gbc);
        gbc.anchor = GridBagConstraints.CENTER; 

        JButton addButton = new JButton("Add Book");
        addButton.addActionListener((ActionEvent e) -> addBookAction());
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        inputFormPanel.add(addButton, gbc);

        mainPanel.add(inputFormPanel, BorderLayout.SOUTH);

        add(mainPanel);

        if (!dbHandler.isInitializationFailed() && currentUser != null) {
            setupBooksListener(); 
        } else {
            bookListArea.setText("Could not connect to database or user not identified.");
        }
    }

    @SuppressWarnings("UseSpecificCatch")
    private void addBookAction() {
        if (currentUser == null || currentUser.getUid() == null) {
            JOptionPane.showMessageDialog(MainFrame.this,
                    "No user identified. Cannot add book.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String yearText = yearField.getText().trim();

            if (title.isEmpty() || author.isEmpty() || yearText.isEmpty()) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "All fields (Title, Author, Year) are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int year = Integer.parseInt(yearText);
            Book book = new Book(title, author, year); 
            dbHandler.addBook(currentUser.getUid(), book);
            clearInputFields();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(MainFrame.this,
                    "Invalid year format. Please enter a number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(MainFrame.this,
                    "An error occurred while adding the book: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupBooksListener() {
        if (dbHandler == null || dbHandler.isInitializationFailed() || currentUser == null || currentUser.getUid() == null) {
             bookListArea.setText("Cannot set up book listener: Database Handler not available or user not identified.");
            return;
        }

        booksListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Book> books = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        String id = snapshot.getKey();
                        String title = snapshot.child("title").getValue(String.class);
                        String author = snapshot.child("author").getValue(String.class);
                        Long yearLong = snapshot.child("year").getValue(Long.class);
                        
                        if (title == null || author == null || yearLong == null) {
                            System.err.println("Skipping book with missing data: ID " + id);
                            continue;
                        }
                        int year = yearLong.intValue();
                        books.add(new Book(id, title, author, year));
                    } catch (Exception e) {
                        System.err.println("Error parsing book data: " + e.getMessage() + " for snapshot: " + snapshot.getKey());
                    }
                }
                SwingUtilities.invokeLater(() -> updateBookListArea(books));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                SwingUtilities.invokeLater(() -> {
                    bookListArea.setText("Error fetching books for user " + currentUser.getEmail() + ": " + databaseError.getMessage() +
                                         "\nListener has been cancelled. Try restarting the application.");
                    JOptionPane.showMessageDialog(MainFrame.this,
                        "Error fetching books from Firebase: " + databaseError.getMessage() +
                        "\nThe book list may not update automatically.",
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                );
            }
        };
        dbHandler.addBooksValueEventListener(currentUser.getUid(), booksListener);
    }

    private void updateBookListArea(List<Book> books) {
        bookListArea.setText(""); 
        if (books.isEmpty()) {
            bookListArea.setText("No books in your library yet. Add one using the form below.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Book book : books) {
                sb.append(String.format("ID: %s\nTitle: %s\nAuthor: %s, Year: %d\n--------------------\n",
                        book.getId(), book.getTitle(), book.getAuthor(), book.getYear()));
            }
            bookListArea.setText(sb.toString());
        }
        bookListArea.setCaretPosition(0); 
    }

    private void clearInputFields() {
        titleField.setText("");
        authorField.setText("");
        yearField.setText("");
        titleField.requestFocus();
    }
    
    private void performCleanupForLogout() {
        if (dbHandler != null && booksListener != null && !dbHandler.isInitializationFailed() &&
            currentUser != null && currentUser.getUid() != null) {
            dbHandler.removeBooksValueEventListener(currentUser.getUid(), booksListener);
            System.out.println("Firebase listener removed for user " + currentUser.getEmail() + " upon logout.");
        }
    }

    private void logoutAction() {
        logoutButton.setEnabled(false);
        logoutButton.setText("Logging out...");

        performCleanupForLogout();
        dispose(); 
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true)); 
    }

    private void performCleanupAndExit() {
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit the Library Management System?",
                "Confirm Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirmation == JOptionPane.YES_OPTION) {
            System.out.println("Exiting application...");
            performCleanupForLogout(); 

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    DatabaseHandler.shutdownFirebase();
                    return null;
                }
                @Override
                protected void done() {
                    dispose();
                    System.exit(0);
                }
            }.execute();
        }
    }
}