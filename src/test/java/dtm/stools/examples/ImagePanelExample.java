package dtm.stools.examples;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import dtm.stools.component.panels.ImagePanel;

public class ImagePanelExample {
    
    private static final String path = ""; 
    
    public static void main(String[] args) {
	   SwingUtilities.invokeLater(ImagePanelExample::createAndShowUI);
    }
    
    
    private static void createAndShowUI() {
        JFrame frame = new JFrame("ImagePanelExample");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);
        frame.setLocationRelativeTo(null);

        ImagePanel panel = new ImagePanel(path);

        panel.setLayout(new BorderLayout());

        JLabel title = new JLabel("Titulo", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 32));
        title.setForeground(Color.WHITE);

        JButton button = new JButton("Testar");

        panel.add(title, BorderLayout.CENTER);
        panel.add(button, BorderLayout.SOUTH);

        frame.setContentPane(panel);
        frame.setVisible(true);
    }
}
