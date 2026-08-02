package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;

public class WindowTitleBar extends JPanel {
    protected final WindowPanel window;
    protected final JLabel iconLabel = new JLabel();
    protected final JLabel titleLabel = new JLabel();
    protected final JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    protected final JPanel center = new JPanel(new BorderLayout());
    protected final JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    public WindowTitleBar(WindowPanel window) {
        this.window = window;
        setLayout(new BorderLayout(8, 0));
        setOpaque(false);
        leading.setOpaque(false);
        center.setOpaque(false);
        controls.setOpaque(false);
        leading.add(iconLabel);
        center.add(titleLabel, BorderLayout.CENTER);
        add(leading, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(controls, BorderLayout.EAST);
    }

    public void addControl(Component component) {
        if (component != null) component.setCursor(Cursor.getDefaultCursor());
        controls.add(component);
    }
    public void addLeading(Component component) {
        leading.add(component);
        window.installTitleBarDragSource(component);
    }
    public void setCenterComponent(Component component) {
        center.removeAll();
        center.add(component == null ? titleLabel : component, BorderLayout.CENTER);
        window.installTitleBarDragSource(component == null ? titleLabel : component);
        revalidate();
        repaint();
    }

    public JLabel getTitleLabel() { return titleLabel; }
    public JLabel getIconLabel() { return iconLabel; }
    public JPanel getControls() { return controls; }

    public void updateFromWindow() {
        WindowStyle style = window.getWindowStyle();
        titleLabel.setText(window.getTitle());
        titleLabel.setIcon(null);
        titleLabel.setFont(style.getTitleFont());
        titleLabel.setForeground(window.isActive() ? style.getForeground() : style.getInactiveForeground());
        iconLabel.setIcon(window.getIcon());
        iconLabel.setVisible(window.getIcon() != null);
        setPreferredSize(new Dimension(10, style.getTitleBarHeight()));
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 4));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            WindowStyle style = window.getWindowStyle();
            g.setColor(window.isActive() ? style.getActiveTitleBackground() : style.getTitleBackground());
            int arc = window.getEffectiveWindowArc();
            g.fillRoundRect(0, 0, getWidth(), getHeight() + arc, arc, arc);
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }
}
