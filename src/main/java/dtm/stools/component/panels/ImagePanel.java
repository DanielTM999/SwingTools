package dtm.stools.component.panels;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import lombok.Getter;

public class ImagePanel extends BlockingPanel {
    
    @Getter
    private Image image;

    public ImagePanel(Image image) {
        this.image = image;
        setOpaque(false);
    }
    
    public ImagePanel(String imagePath) {
        this(new ImageIcon(imagePath));
    }

    public ImagePanel(ImageIcon imageIcon) {
        this(imageIcon.getImage());
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image != null) {
            g.drawImage(
                image,
                0,
                0,
                getWidth(),
                getHeight(),
                this
            );
            
            super.paintComponent(g);
        }
        
        
    }

    public void setImage(Image image) {
        this.image = image;
        repaint();
    }
    
}
