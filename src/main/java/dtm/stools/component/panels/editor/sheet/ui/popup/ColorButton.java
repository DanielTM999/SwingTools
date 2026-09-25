package dtm.stools.component.panels.editor.sheet.ui.popup;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

public class ColorButton extends JButton {
    private Integer argb;
    private final String noneLabel;

    public ColorButton(Integer initial, String noneLabel) {
        this.argb = initial;
        this.noneLabel = noneLabel;
        setIcon(new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                if (argb == null) { g.setColor(Color.GRAY); g.drawRect(x, y, 27, 13); g.drawLine(x, y + 13, x + 27, y); return; }
                g.setColor(new Color(argb, true));
                g.fillRect(x, y, 28, 14);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(x, y, 27, 13);
            }
            @Override public int getIconWidth() { return 28; }
            @Override public int getIconHeight() { return 14; }
        });
        setText(initial == null ? noneLabel : null);
        addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, "Cor", argb == null ? Color.WHITE : new Color(argb, true));
            if (chosen != null) setColor(chosen.getRGB() | 0xFF000000);
        });
    }

    public void setColor(Integer value) { argb = value; setText(value == null ? noneLabel : null); repaint(); firePropertyChange("color", null, value); }
    public Integer color() { return argb; }
}
