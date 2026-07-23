package dtm.stools.component.panels.charts2d;

import dtm.stools.component.panels.charts.data.ChartDataSource;
import dtm.stools.component.panels.charts.style.ChartTheme;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class Chart2DPanel<R extends Chart2DRender> extends JPanel {

    private static final int DEFAULT_REFRESH_MILLIS = 100;

    private final R render;
    private final Timer refreshTimer;
    private long lastRevision = Long.MIN_VALUE;

    public Chart2DPanel(R render) {
        this.render = Objects.requireNonNull(render, "render cannot be null");
        setOpaque(false);
        this.refreshTimer = new Timer(DEFAULT_REFRESH_MILLIS, event -> refreshIfChanged());
        this.refreshTimer.setRepeats(true);
        installHover();
    }

    private void installHover() {
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                render.setHoverPoint(event.getX(), event.getY());
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                render.setHoverPoint(event.getX(), event.getY());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                render.clearHover();
                repaint();
            }
        };
        addMouseMotionListener(hover);
        addMouseListener(hover);
    }

    public R getRender() {
        return render;
    }

    public ChartDataSource getDataSource() {
        return render.getDataSource();
    }

    public ChartTheme getChartTheme() {
        return render.getTheme();
    }

    public void setChartTheme(ChartTheme theme) {
        render.setTheme(theme);
        repaint();
    }

    public void setRefreshMillis(int millis) {
        refreshTimer.setDelay(Math.max(16, millis));
    }

    public void refreshChart() {
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refreshTimer.start();
    }

    @Override
    public void removeNotify() {
        refreshTimer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            render.paint(g2, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
        lastRevision = render.getRevision();
    }

    private void refreshIfChanged() {
        if (!isShowing()) {
            return;
        }
        if (render.getRevision() != lastRevision) {
            repaint();
        }
    }
}
