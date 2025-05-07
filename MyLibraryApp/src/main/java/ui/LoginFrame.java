package ui;

import database.DatabaseHandler;
import model.User; // <-- ADD THIS IMPORT

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutionException;

public class LoginFrame extends JFrame {
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final DatabaseHandler dbHandler;
    private final JButton loginButton;
    private final JLabel registerLabel;
    private final JLabel statusLabel; 

    public LoginFrame() {
        dbHandler = new DatabaseHandler(); 
        setTitle("Library Management System - Login");
        setSize(400, 280); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        statusLabel = new JLabel("", JLabel.CENTER);
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(statusLabel, gbc);
        gbc.gridwidth = 1; 

        if (dbHandler.isInitializationFailed()) {
            statusLabel.setText("<html><center>Firebase initialization failed.<br>Login and registration may not work.</center></html>");
        }


        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Email:"), gbc);

        emailField = new JTextField(20);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Password:"), gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(passwordField, gbc);

        loginButton = new JButton("Login");
        loginButton.addActionListener((ActionEvent e) -> loginAction());
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(loginButton, gbc);

        registerLabel = new JLabel("<html><a href=''>Don't have an account? Register here.</a></html>");
        registerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        registerLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openRegistrationFrame();
            }
        });
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.insets = new Insets(15, 5, 5, 5); 
        panel.add(registerLabel, gbc);


        add(panel);

        passwordField.addActionListener((ActionEvent e) -> loginAction());
        emailField.addActionListener((ActionEvent e) -> passwordField.requestFocusInWindow());

        if (dbHandler.isInitializationFailed()) {
            loginButton.setEnabled(false); 
        }
    }

    private void loginAction() {
        if (dbHandler.isInitializationFailed()) {
            JOptionPane.showMessageDialog(LoginFrame.this,
                    "Cannot login: Firebase initialization failed. Please check the console.",
                    "Login Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(LoginFrame.this,
                    "Email and Password cannot be empty.", "Login Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");
        statusLabel.setText(" "); 

        // MODIFIED: SwingWorker now expects User object
        SwingWorker<User, Void> worker = new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return dbHandler.authenticateUser(email, password);
            }

            @Override
            protected void done() {
                try {
                    User authenticatedUser = get(); // Get the User object
                    if (authenticatedUser != null) { // Check if User object is not null
                        dispose(); 
                        new MainFrame(authenticatedUser).setVisible(true); // Pass User to MainFrame
                    } else {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Invalid email or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                        passwordField.setText(""); 
                        passwordField.requestFocusInWindow();
                    }
                } catch (HeadlessException | InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "An error occurred during login: " + ex.getMessage(),
                            "Login Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                }
            }
        };
        worker.execute();
    }

    private void openRegistrationFrame() {
        if (dbHandler.isInitializationFailed()) {
            JOptionPane.showMessageDialog(LoginFrame.this,
                    "Cannot open registration: Firebase initialization failed. Please check the console.",
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.dispose(); 
        new RegistrationFrame().setVisible(true); 
    }
}