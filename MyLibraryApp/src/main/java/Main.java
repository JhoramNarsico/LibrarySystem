import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import ui.LoginFrame;

public class Main {
    @SuppressWarnings("UseSpecificCatch")
    public static void main(String[] args) {
        // Set UIManager properties early for a better look and feel.
        try {
            // Use the system's native look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Couldn't set system look and feel. Using default.");
        }

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}