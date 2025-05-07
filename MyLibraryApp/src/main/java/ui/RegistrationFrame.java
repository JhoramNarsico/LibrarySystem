package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

import database.DatabaseHandler;

public class RegistrationFrame extends JFrame {
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final JTextField displayNameField;
    private final JButton registerButton;
    private final JLabel loginLabel;
    private final DatabaseHandler dbHandler;

    // Basic email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");


    public RegistrationFrame() {
        dbHandler = new DatabaseHandler(); // Initializes Firebase
        setTitle("User Registration");
        setSize(450, 350); // Adjusted size
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Or DISPOSE_ON_CLOSE if LoginFrame is main entry
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        if (dbHandler.isInitializationFailed()) {
            // Add a status label or disable form if Firebase init failed critically
            JLabel errorLabel = new JLabel("<html><center>Firebase initialization failed.<br>Registration is not available.</center></html>", JLabel.CENTER);
            errorLabel.setForeground(Color.RED);
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            panel.add(errorLabel, gbc);
            gbc.gridy++; gbc.gridwidth = 1; // Reset
        }

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Display Name:"), gbc);
        displayNameField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(displayNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Email:"), gbc);
        emailField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 3; panel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Confirm Password:"), gbc);
        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 4; panel.add(confirmPasswordField, gbc);

        registerButton = new JButton("Register");
        registerButton.addActionListener(event -> registerAction());
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(registerButton, gbc);

        loginLabel = new JLabel("<html><a href=''>Already have an account? Login.</a></html>");
        loginLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loginLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openLoginFrame();
            }
        });
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.insets = new Insets(15, 5, 5, 5);
        panel.add(loginLabel, gbc);

        add(panel);

        // Allow registration with Enter key on confirm password field
        confirmPasswordField.addActionListener(event -> registerAction());
        
        if (dbHandler.isInitializationFailed()) {
            registerButton.setEnabled(false);
        }
    }

    private void registerAction() {
        if (dbHandler.isInitializationFailed()) {
            JOptionPane.showMessageDialog(this,
                "Cannot register: Firebase initialization failed. Please check the console.",
                "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String displayName = displayNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (displayName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            JOptionPane.showMessageDialog(this, "Invalid email format.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText("Registering...");

        // Use SwingWorker for background Firebase operation
        SwingWorker<UserRecord, String> worker = new SwingWorker<UserRecord, String>() {
            @Override
            protected UserRecord doInBackground() throws Exception {
                // The registerUser method now returns a CompletableFuture
                // We get the result of the future here (blocks this worker thread, not EDT)
                return dbHandler.registerUser(email, password, displayName).get();
            }

            @Override
            protected void done() {
                try {
                    UserRecord userRecord = get(); // This can throw ExecutionException if doInBackground failed
                    JOptionPane.showMessageDialog(RegistrationFrame.this,
                            "User registered successfully: " + userRecord.getEmail() + "\nPlease login.",
                            "Registration Successful", JOptionPane.INFORMATION_MESSAGE);
                    openLoginFrame();
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause(); // Get the actual exception from CompletableFuture
                    String errorMessage = "An error occurred during registration: ";
                    if (!(cause instanceof FirebaseAuthException)) if (cause != null) {
                        errorMessage += cause.getMessage();
                    } else {
                        errorMessage += ex.getMessage();
                    } else {
                        FirebaseAuthException authEx = (FirebaseAuthException) cause;
                        AuthErrorCode errorCode = authEx.getAuthErrorCode();
                        if (null == errorCode) {
                            errorMessage += authEx.getMessage();
                        } else switch (errorCode) {
                            case EMAIL_ALREADY_EXISTS -> errorMessage = "This email address is already in use.";
                            case CERTIFICATE_FETCH_FAILED -> errorMessage = "The password is too weak. Please choose a stronger password.";
                            default -> errorMessage += authEx.getMessage();
                        }
                        System.err.println("FirebaseAuthException: " + authEx.getAuthErrorCode() + " - " + authEx.getMessage());
                    }
                    JOptionPane.showMessageDialog(RegistrationFrame.this,
                            errorMessage, "Registration Failed", JOptionPane.ERROR_MESSAGE);
                } finally {
                    registerButton.setEnabled(true);
                    registerButton.setText("Register");
                }
            }
        };
        worker.execute();
    }

    private void openLoginFrame() {
        this.dispose();
        new LoginFrame().setVisible(true);
    }
}