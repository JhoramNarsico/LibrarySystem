import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import ui.LoginFrame;

public class Main {
    @SuppressWarnings("UseSpecificCatch")
    public static void main(String[] args) {
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Couldn't set system look and feel. Using default.");
        }
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}