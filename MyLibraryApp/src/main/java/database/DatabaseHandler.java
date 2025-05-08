package database;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject; 

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import model.Book;
import model.User; 

public class DatabaseHandler {

    private static final String FIREBASE_API_KEY = "AIzaSyBB-9BrEzZF_fTd6QMHL2ecn2xtZkpkIwI"; 
    private static final String FIREBASE_DATABASE_URL = "https://itcc-11-libary-default-rtdb.firebaseio.com/"; 
    private static final String SERVICE_ACCOUNT_KEY_PATH = "serviceAccountKey.json";

    private DatabaseReference databaseRootRef;
    private FirebaseAuth firebaseAuth;
    private boolean initializationFailed = false;


    @SuppressWarnings("deprecation")
    public DatabaseHandler() {
        InputStream serviceAccountStream;
        try {
            System.out.println("Attempting to load Firebase service account key: " + SERVICE_ACCOUNT_KEY_PATH);
            serviceAccountStream = getClass().getClassLoader().getResourceAsStream(SERVICE_ACCOUNT_KEY_PATH);

            if (serviceAccountStream == null) {
                String errorMessage = "CRITICAL ERROR: " + SERVICE_ACCOUNT_KEY_PATH + " not found in classpath. \n" +
                        "1. Ensure it's named exactly 'serviceAccountKey.json'.\n" +
                        "2. Ensure it's placed in 'src/main/resources/' (or equivalent build path).\n" +
                        "3. Ensure your build is successful (target/classes/ should contain the file).\n" +
                        "Attempted to locate via classloader: " + getClass().getClassLoader().getResource(SERVICE_ACCOUNT_KEY_PATH);
                System.err.println(errorMessage);
                initializationFailed = true;
                throw new IOException(errorMessage);
            }

            System.out.println("Successfully located " + SERVICE_ACCOUNT_KEY_PATH + ". Stream object: " + serviceAccountStream);
            if (serviceAccountStream.available() <= 0) {
                 System.out.println("WARNING: Stream for " + SERVICE_ACCOUNT_KEY_PATH + " reported 0 or fewer available bytes. The file might be empty or unreadable.");
            }

            FirebaseOptions options;
            try (InputStream in = serviceAccountStream) {
                System.out.println("Attempting to create GoogleCredentials from stream...");
                GoogleCredentials credentials = GoogleCredentials.fromStream(in);
                System.out.println("Successfully created GoogleCredentials from stream.");

                System.out.println("Attempting to build FirebaseOptions...");
                options = new FirebaseOptions.Builder()
                        .setCredentials(credentials)
                        .setDatabaseUrl(FIREBASE_DATABASE_URL)
                        .build();
                System.out.println("Successfully built FirebaseOptions.");
            }

            System.out.println("Attempting to initialize FirebaseApp...");
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK initialized successfully.");
            } else {
                FirebaseApp app = FirebaseApp.getInstance(); 
                System.out.println("Firebase Admin SDK already initialized. Using existing app: " + app.getName());
            }

            firebaseAuth = FirebaseAuth.getInstance();
            databaseRootRef = FirebaseDatabase.getInstance().getReference("library_app_data");
            System.out.println("FirebaseAuth and DatabaseReference obtained.");

            initializeDefaultAdminUser();

        } catch (IOException e) {
            System.err.println("FATAL: Firebase Admin SDK initialization failed due to an I/O or credential processing issue.");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("Detailed stack trace:");
            System.err.println("Troubleshooting steps:");
            System.err.println("1. Verify '" + SERVICE_ACCOUNT_KEY_PATH + "' is in 'src/main/resources' and is a valid, non-empty JSON file from Firebase.");
            System.err.println("2. Perform a 'Maven Clean and Build' (e.g., 'mvn clean install' or via IDE).");
            System.err.println("3. Check the 'target/classes' directory to ensure '" + SERVICE_ACCOUNT_KEY_PATH + "' is present there after building.");
            System.err.println("4. Ensure your FIREBASE_DATABASE_URL ('" + FIREBASE_DATABASE_URL + "') is correct.");
            initializationFailed = true;
        } catch (Exception e) {
            System.err.println("FATAL: An unexpected error occurred during Firebase Admin SDK initialization.");
            System.err.println("Error Message: " + e.getMessage());
            initializationFailed = true;
        }
    }

    public boolean isInitializationFailed() {
        return initializationFailed;
    }

    private void initializeDefaultAdminUser() {
        if (initializationFailed) {
            System.err.println("Skipping default admin user initialization due to earlier Firebase setup failure.");
            return;
        }
        String adminEmail = "admin@example.com";
        String adminPassword = "admin123";

        try {
            System.out.println("Checking for existing admin user: " + adminEmail);
            firebaseAuth.getUserByEmail(adminEmail);
            System.out.println("Admin user (" + adminEmail + ") already exists.");
        } catch (FirebaseAuthException e) {
            AuthErrorCode authErrorCode = e.getAuthErrorCode();

            if (authErrorCode == AuthErrorCode.USER_NOT_FOUND) {
                System.out.println("Admin user (" + adminEmail + ") not found (AuthErrorCode: " +
                                   (authErrorCode != null ? authErrorCode.name() : "N/A") + "). Attempting to create...");
                UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                        .setEmail(adminEmail)
                        .setEmailVerified(true)
                        .setPassword(adminPassword)
                        .setDisplayName("Admin User")
                        .setDisabled(false);
                try {
                    UserRecord userRecord = firebaseAuth.createUser(request);
                    System.out.println("Successfully created new admin user: " + userRecord.getUid() + " (Email: " + adminEmail + ")");
                } catch (FirebaseAuthException createUserEx) {
                    System.err.println("Error creating default admin user (" + adminEmail +
                                       "). CreateAuthErrorCode: [" + (createUserEx.getAuthErrorCode() != null ? createUserEx.getAuthErrorCode().name() : "N/A") +
                                       "], CreateRawErrorCode (Firebase general): [" + createUserEx.getErrorCode() + // This is com.google.firebase.ErrorCode
                                       "], Message: " + createUserEx.getMessage());
                }
            } else {
                System.err.println("Error checking for admin user (" + adminEmail +
                                   "). AuthErrorCode: [" + (authErrorCode != null ? authErrorCode.name() : "N/A") +
                                   "], RawErrorCode (Firebase general): [" + e.getErrorCode() +
                                   "], Message: " + e.getMessage());
            }
        }  catch (Exception e) {
            System.err.println("Unexpected error during admin user initialization for " + adminEmail + ": " + e.getMessage());
        }
    }
    
    public CompletableFuture<UserRecord> registerUser(String email, String password, String displayName) {
        CompletableFuture<UserRecord> future = new CompletableFuture<>();
        if (initializationFailed) {
            future.completeExceptionally(new IllegalStateException("Firebase Admin SDK not initialized. Cannot register user."));
            return future;
        }
        if (firebaseAuth == null) {
            future.completeExceptionally(new IllegalStateException("FirebaseAuth instance is null. Cannot register user."));
            return future;
        }

        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setEmailVerified(false) 
                .setPassword(password)
                .setDisplayName(displayName)
                .setDisabled(false);

        try {
            UserRecord userRecord = firebaseAuth.createUser(request);
            System.out.println("Successfully created new user: " + userRecord.getUid() + " (Email: " + email + ")");
            future.complete(userRecord);
        } catch (FirebaseAuthException e) {
            System.err.println("Error creating new user (" + email +
                               "). AuthErrorCode: [" + (e.getAuthErrorCode() != null ? e.getAuthErrorCode().name() : "N/A") +
                               "], Message: " + e.getMessage());
            future.completeExceptionally(e);
        } catch (Exception e) {
            System.err.println("Unexpected error during user registration for " + email + ": " + e.getMessage());
            future.completeExceptionally(e);
        }
        return future;
    }

    public User authenticateUser(String email, String password) {
        if (initializationFailed) {
            System.err.println("Firebase Admin SDK not initialized. Authentication via REST API might still work if API key is valid, but other features will be broken.");
        }
        if (FIREBASE_API_KEY.equals("AIzaSyC1bFLQYXfZdSmBDO3BxYM7-ai8HpfFzv8") || FIREBASE_API_KEY.trim().isEmpty() || FIREBASE_API_KEY.contains("YOUR_WEB_API_KEY")) {
            System.err.println("Firebase Web API Key is a placeholder or not configured in DatabaseHandler.java. REST API Authentication will fail.");
            System.err.println("Please replace '" + FIREBASE_API_KEY + "' with your actual Firebase Web API Key.");
            return null;
        }
        System.out.println("Attempting to authenticate user: " + email);
        String urlString = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FIREBASE_API_KEY;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            String jsonInputString = String.format("{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}", email, password);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Firebase Auth Response Code: " + responseCode + " for user: " + email);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                    String responseBody = scanner.useDelimiter("\\A").next();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String uid = jsonResponse.getString("localId");
                    String userEmail = jsonResponse.getString("email"); 
                    System.out.println("Authentication successful for: " + userEmail + " (UID: " + uid + ")");
                    return new User(uid, userEmail); 
                }
            } else {
                System.err.println("Authentication failed for: " + email + ". Response Code: " + responseCode);
                 try (Scanner scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name())) {
                    String errorBody = scanner.useDelimiter("\\A").next();
                    System.err.println("Firebase Auth Error Body: " + errorBody);
                } catch (Exception ex) {
                    System.err.println("Could not read error body from Firebase Auth response: " + ex.getMessage());
                }
                return null;
            }

        } catch (IOException e) {
            System.err.println("An error occurred during authentication REST API call for " + email + ": " + e.getMessage());
            return null;
        } catch (org.json.JSONException e) { 
            System.err.println("Error parsing Firebase Auth JSON response: " + e.getMessage());
            return null;
        }
    }

    public void addBook(String userID, Book book) {
        if (initializationFailed || databaseRootRef == null) {
            System.err.println("Database reference not initialized (or init failed). Cannot add book.");
            return;
        }
        if (userID == null || userID.trim().isEmpty()) {
            System.err.println("UserID is null or empty. Cannot add book to user-specific library.");
            return;
        }
        DatabaseReference userBooksRef = databaseRootRef.child("user_libraries").child(userID).child("books");
        String newBookId = userBooksRef.push().getKey();
        System.out.println("Adding book for user " + userID + " with generated ID: " + newBookId + " Title: " + book.getTitle());

        Map<String, Object> bookValues = new HashMap<>();
        bookValues.put("title", book.getTitle());
        bookValues.put("author", book.getAuthor());
        bookValues.put("year", book.getYear());

        if (newBookId != null) {
            userBooksRef.child(newBookId).setValue(bookValues, (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    System.err.println("Book data for user " + userID + " ID " + newBookId + " could not be saved: " + databaseError.getMessage());
                } else {
                    System.out.println("Book data for user " + userID + " ID " + newBookId + " saved successfully.");
                }
            });
        } else {
            System.err.println("Could not generate a new key for the book: " + book.getTitle() + " for user " + userID);
        }
    }

    public void addBooksValueEventListener(String userID, ValueEventListener listener) {
        if (initializationFailed || databaseRootRef == null) {
            System.err.println("Database reference not initialized (or init failed). Cannot add listener.");
            return;
        }
         if (userID == null || userID.trim().isEmpty()) {
            System.err.println("UserID is null or empty. Cannot add listener to user-specific library.");
            return;
        }
        DatabaseReference userBooksRef = databaseRootRef.child("user_libraries").child(userID).child("books");
        System.out.println("Adding ValueEventListener to books node for user: " + userID);
        userBooksRef.addValueEventListener(listener);
    }

    public void removeBooksValueEventListener(String userID, ValueEventListener listener) {
        if (initializationFailed || databaseRootRef == null) {
            System.err.println("Database reference not initialized (or init failed). Cannot remove listener.");
            return;
        }
        if (userID == null || userID.trim().isEmpty()) {
            System.err.println("UserID is null or empty. Cannot remove listener from user-specific library.");
            return;
        }
        DatabaseReference userBooksRef = databaseRootRef.child("user_libraries").child(userID).child("books");
        System.out.println("Removing ValueEventListener from books node for user: " + userID);
        userBooksRef.removeEventListener(listener);
    }

    public static void shutdownFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            try {
                FirebaseApp.getInstance().delete(); 
                System.out.println("Firebase App instance deleted successfully.");
            } catch (IllegalStateException e) {
                System.err.println("Error deleting Firebase App instance: " + e.getMessage());
            }
        }
    }
}