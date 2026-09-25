package dtm.stools.component.panels;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;

import dtm.stools.utils.ImageUtils;
import lombok.Getter;
import lombok.NonNull;

public class ImagePanel extends BlockingPanel {
    
    @Getter
    private Image image;

    public ImagePanel(Image image) {
        this.image = image;
        setOpaque(false);
    }
    
    public ImagePanel(@NonNull String imagePath) {
        this(ImageUtils.getImageByResourceOrThrow(ImagePanel.class, imagePath));
    }

    public ImagePanel(@NonNull Class<?> aClass, @NonNull String imagePath) {
        this(ImageUtils.getImageByResourceOrThrow(aClass, imagePath));
    }

    public ImagePanel(@NonNull ImageIcon imageIcon) {
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
