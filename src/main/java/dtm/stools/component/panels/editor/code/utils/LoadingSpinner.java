package dtm.stools.component.panels.editor.code.utils;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class LoadingSpinner extends JComponent implements LoadingIndicator {
    private int frame;
    private final Timer timer = new Timer(70, e -> {
        frame = (frame + 1) % 24;
        repaint();
    });

    public LoadingSpinner() {
        setPreferredSize(new Dimension(20, 20));
        setMinimumSize(new Dimension(20, 20));
        setOpaque(false);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void start() {
        if (!timer.isRunning()) timer.start();
    }

    @Override
    public void stop() {
        timer.stop();
        frame = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight());
            float stroke = Math.max(2f, size / 9f);
            float pad = stroke + 1f;
            Color base = getForeground() != null ? getForeground() : new Color(0x2563EB);

            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 42));
            g2.drawOval(Math.round(pad), Math.round(pad), Math.round(size - pad * 2), Math.round(size - pad * 2));

            int start = 90 - frame * 15;
            g2.setColor(base);
            g2.drawArc(Math.round(pad), Math.round(pad), Math.round(size - pad * 2), Math.round(size - pad * 2), start, 115);
        } finally {
            g2.dispose();
        }
    }
}
