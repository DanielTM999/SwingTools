package dtm.stools.component.panels.loading;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class LoadingPanel extends PanelEventListener {

    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String PROGRESS = "progress";
    public static final String MESSAGE_CHANGE = "messageChange";

    @Getter
    private Component content;
    @Getter
    private boolean loading = true;
    private boolean blockInput = true;
    private boolean showMessage = true;
    private boolean showProgressText = true;
    @Getter
    private String message = "Carregando...";
    @Getter
    private double progress = -1d;
    private int spinnerAngle;
    private int spinnerSize = 34;
    private int progressWidth = 180;

    private Color overlayColor = new Color(255, 255, 255, 255);
    private Color textColor = new Color(0x111827);
    private Color accentColor = new Color(0x2563EB);
    private Color trackColor = new Color(0xD1D5DB);

    private final Timer spinnerTimer;

    public LoadingPanel() {
        this(null);
    }

    public LoadingPanel(Component content) {
        super(new BorderLayout(), false);
        setOpaque(false);
        this.spinnerTimer = new Timer(40, e -> {
            spinnerAngle = (spinnerAngle + 12) % 360;
            repaint();
        });
        setContent(content);
        updateTimer();
    }

    public void setContent(Component content) {
        if (this.content != null) {
            remove(this.content);
        }

        this.content = content;
        if (content != null) {
            add(content);
        }

        revalidate();
        repaint();
    }

    public LoadingPanel setLoading(boolean loading) {
        if (this.loading == loading) return this;

        this.loading = loading;
        updateTimer();

        if (blockInput) {
            if (loading) {
                lockUI();
            } else {
                unlockUI();
            }
        }

        dispatchEvent(EventType.CHANGE, this, loading, Map.of("loading", loading));
        dispatchEvent(loading ? START : STOP, this, loading, Map.of("loading", loading));
        repaint();
        return this;
    }

    public LoadingPanel start() {
        return setLoading(true);
    }

    public LoadingPanel stop() {
        return setLoading(false);
    }

    public LoadingPanel setMessage(String message) {
        this.message = message == null ? "" : message;
        dispatchEvent(MESSAGE_CHANGE, this, this.message, Map.of("message", this.message));
        repaint();
        return this;
    }

    public LoadingPanel setProgress(double progress) {
        this.progress = progress < 0 ? -1d : Math.max(0d, Math.min(1d, progress));
        updateTimer();
        dispatchEvent(PROGRESS, this, this.progress, Map.of("progress", this.progress));
        repaint();
        return this;
    }

    public LoadingPanel setIndeterminate() {
        return setProgress(-1d);
    }

    public LoadingPanel setBlockInput(boolean blockInput) {
        this.blockInput = blockInput;
        if (!blockInput) {
            unlockUI();
        } else if (loading) {
            lockUI();
        }
        return this;
    }

    public LoadingPanel setShowMessage(boolean showMessage) {
        this.showMessage = showMessage;
        repaint();
        return this;
    }

    public LoadingPanel setShowProgressText(boolean showProgressText) {
        this.showProgressText = showProgressText;
        repaint();
        return this;
    }

    public LoadingPanel setSpinnerSize(int spinnerSize) {
        this.spinnerSize = Math.max(16, spinnerSize);
        repaint();
        return this;
    }

    public LoadingPanel setProgressWidth(int progressWidth) {
        this.progressWidth = Math.max(60, progressWidth);
        repaint();
        return this;
    }

    public LoadingPanel setOverlayColor(Color overlayColor) {
        if (overlayColor != null) {
            this.overlayColor = overlayColor;
            repaint();
        }
        return this;
    }

    public LoadingPanel setTextColor(Color textColor) {
        if (textColor != null) {
            this.textColor = textColor;
            repaint();
        }
        return this;
    }

    public LoadingPanel setAccentColor(Color accentColor) {
        if (accentColor != null) {
            this.accentColor = accentColor;
            repaint();
        }
        return this;
    }

    public LoadingPanel setTrackColor(Color trackColor) {
        if (trackColor != null) {
            this.trackColor = trackColor;
            repaint();
        }
        return this;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        updateTimer();
        if (loading && blockInput) {
            lockUI();
        }
    }

    @Override
    public void removeNotify() {
        spinnerTimer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (!loading) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintLoadingLayer(g2);
        } finally {
            g2.dispose();
        }
    }

    protected void paintLoadingLayer(Graphics2D g2) {
        g2.setColor(overlayColor);
        g2.fillRect(0, 0, getWidth(), getHeight());

        Rectangle spinnerBounds = getSpinnerBounds();
        paintSpinner(g2, spinnerBounds);

        int nextY = spinnerBounds.y + spinnerBounds.height + 14;
        if (showMessage && !message.isBlank()) {
            nextY = paintMessage(g2, nextY) + 10;
        }

        if (progress >= 0d) {
            paintProgress(g2, nextY);
        }
    }

    protected Rectangle getSpinnerBounds() {
        int x = (getWidth() - spinnerSize) / 2;
        int blockHeight = spinnerSize + (showMessage ? 30 : 0) + (progress >= 0d ? 24 : 0);
        int y = Math.max(8, (getHeight() - blockHeight) / 2);
        return new Rectangle(x, y, spinnerSize, spinnerSize);
    }

    protected void paintSpinner(Graphics2D g2, Rectangle bounds) {
        int stroke = Math.max(3, bounds.width / 9);
        g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(trackColor);
        g2.drawArc(bounds.x, bounds.y, bounds.width, bounds.height, 0, 360);
        g2.setColor(accentColor);
        g2.drawArc(bounds.x, bounds.y, bounds.width, bounds.height, spinnerAngle, 110);
    }

    protected int paintMessage(Graphics2D g2, int y) {
        g2.setFont(getFont().deriveFont(Font.BOLD));
        g2.setColor(textColor);
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(message)) / 2;
        int baseline = y + fm.getAscent();
        g2.drawString(message, Math.max(8, x), baseline);
        return baseline;
    }

    protected void paintProgress(Graphics2D g2, int y) {
        int width = Math.min(progressWidth, Math.max(60, getWidth() - 32));
        int height = 7;
        int x = (getWidth() - width) / 2;
        int arc = height;

        g2.setColor(trackColor);
        g2.fillRoundRect(x, y, width, height, arc, arc);
        g2.setColor(accentColor);
        g2.fillRoundRect(x, y, (int) Math.round(width * progress), height, arc, arc);

        if (showProgressText) {
            String text = Math.round(progress * 100) + "%";
            g2.setFont(getFont().deriveFont(Font.PLAIN, Math.max(10f, getFont().getSize2D() - 1f)));
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, y + height + fm.getAscent() + 4);
        }
    }

    protected void updateTimer() {
        if (loading && isDisplayable()) {
            spinnerTimer.start();
        } else {
            spinnerTimer.stop();
        }
    }
}
