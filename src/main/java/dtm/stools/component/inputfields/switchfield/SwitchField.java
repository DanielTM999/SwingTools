package dtm.stools.component.inputfields.switchfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

public class SwitchField extends PanelEventListener {

    public static final String SWITCH_ON = "switchOn";
    public static final String SWITCH_OFF = "switchOff";

    private boolean selected;
    private boolean animated = true;
    private boolean showText;
    private float animationProgress;
    private float animationStartProgress;
    private float animationTargetProgress;
    private long animationStartedAtNanos;
    private long animationRunDurationNanos;
    private int animationDuration = 140;

    private boolean switchSizeExplicitlySet;
    private Insets trackInsets = new Insets(0, 0, 0, 0);
    private int thumbSize;
    private int thumbPadding = 3;
    private int trackArc;
    private float focusStrokeWidth = 2f;
    private int focusGap = 1;

    private String onText = "ON";
    private String offText = "OFF";

    private Color onColor = new Color(0x2563EB);
    private Color offColor = new Color(0xCBD5E1);
    private Color thumbColor = Color.WHITE;
    private Color disabledColor = new Color(0xE5E7EB);
    private Color textColor = Color.WHITE;
    private Color focusColor = new Color(0x93C5FD);

    private final Timer animationTimer;

    public SwitchField() {
        this(false);
    }

    public SwitchField(boolean selected) {
        super(null, false);
        this.selected = selected;
        this.animationProgress = selected ? 1f : 0f;
        this.animationTargetProgress = this.animationProgress;

        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        super.setPreferredSize(new Dimension(56, 30));
        setMinimumSize(new Dimension(44, 24));
        setOpaque(false);

        animationTimer = new Timer(16, e -> updateAnimation());
        installListeners();
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled() || !SwingUtilities.isLeftMouseButton(e)) return;
                requestFocusInWindow();
                toggle();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled()) return;
                if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    toggle();
                    e.consume();
                }
            }
        });
    }

    public void toggle() {
        setSelected(!selected);
    }

    public boolean isSelected() {
        return selected;
    }

    public SwitchField setSelected(boolean selected) {
        return setSelected(selected, true);
    }

    public SwitchField setSelected(boolean selected, boolean fireEvent) {
        if (this.selected == selected) return this;

        boolean oldValue = this.selected;
        this.selected = selected;
        animateToSelection();
        firePropertyChange("selected", oldValue, selected);

        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", oldValue, "newValue", selected);
            dispatchEvent(EventType.CHANGE, this, selected, props);
            dispatchEvent(selected ? SWITCH_ON : SWITCH_OFF, this, selected, props);
            if (selected) {
                dispatchEvent(EventType.SELECT, this, selected, props);
            }
        }
        return this;
    }

    public SwitchField setAnimated(boolean animated) {
        this.animated = animated;
        if (!animated) {
            animationTimer.stop();
            animationProgress = selected ? 1f : 0f;
            animationTargetProgress = animationProgress;
            repaint();
        }
        return this;
    }

    public SwitchField setAnimationDuration(int animationDuration) {
        this.animationDuration = Math.max(0, animationDuration);
        if (this.animationDuration == 0 && animationTimer.isRunning()) {
            animationTimer.stop();
            animationProgress = selected ? 1f : 0f;
            animationTargetProgress = animationProgress;
            repaint();
        }
        return this;
    }

    public SwitchField setShowText(boolean showText) {
        this.showText = showText;
        if (!switchSizeExplicitlySet) {
            super.setPreferredSize(new Dimension(showText ? 72 : 56, 30));
            revalidate();
        }
        repaint();
        return this;
    }

    /**
     * Defines the preferred size used by layout managers. The component still
     * paints responsively if its parent assigns different bounds.
     */
    public SwitchField setSwitchSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Switch width and height must be greater than zero");
        }
        switchSizeExplicitlySet = true;
        super.setPreferredSize(new Dimension(width, height));
        revalidate();
        repaint();
        return this;
    }

    public SwitchField setTrackInsets(Insets insets) {
        if (insets == null) throw new IllegalArgumentException("Track insets cannot be null");
        if (insets.top < 0 || insets.left < 0 || insets.bottom < 0 || insets.right < 0) {
            throw new IllegalArgumentException("Track insets cannot be negative");
        }
        this.trackInsets = new Insets(insets.top, insets.left, insets.bottom, insets.right);
        repaint();
        return this;
    }

    public SwitchField setThumbSize(int thumbSize) {
        if (thumbSize < 0) throw new IllegalArgumentException("Thumb size cannot be negative");
        this.thumbSize = thumbSize;
        repaint();
        return this;
    }

    public SwitchField setThumbPadding(int thumbPadding) {
        if (thumbPadding < 0) throw new IllegalArgumentException("Thumb padding cannot be negative");
        this.thumbPadding = thumbPadding;
        repaint();
        return this;
    }

    public SwitchField setTrackArc(int trackArc) {
        if (trackArc < 0) throw new IllegalArgumentException("Track arc cannot be negative");
        this.trackArc = trackArc;
        repaint();
        return this;
    }

    public SwitchField setFocusStrokeWidth(float focusStrokeWidth) {
        if (!Float.isFinite(focusStrokeWidth) || focusStrokeWidth <= 0f) {
            throw new IllegalArgumentException("Focus stroke width must be finite and greater than zero");
        }
        this.focusStrokeWidth = focusStrokeWidth;
        repaint();
        return this;
    }

    public SwitchField setFocusGap(int focusGap) {
        if (focusGap < 0) throw new IllegalArgumentException("Focus gap cannot be negative");
        this.focusGap = focusGap;
        repaint();
        return this;
    }

    public Insets getTrackInsets() {
        return new Insets(trackInsets.top, trackInsets.left, trackInsets.bottom, trackInsets.right);
    }

    public int getThumbSize() {
        return thumbSize;
    }

    public int getThumbPadding() {
        return thumbPadding;
    }

    public int getTrackArc() {
        return trackArc;
    }

    public float getFocusStrokeWidth() {
        return focusStrokeWidth;
    }

    public int getFocusGap() {
        return focusGap;
    }

    public SwitchField setTexts(String onText, String offText) {
        this.onText = onText == null ? "" : onText;
        this.offText = offText == null ? "" : offText;
        repaint();
        return this;
    }

    public SwitchField setColors(Color onColor, Color offColor, Color thumbColor) {
        if (onColor != null) this.onColor = onColor;
        if (offColor != null) this.offColor = offColor;
        if (thumbColor != null) this.thumbColor = thumbColor;
        repaint();
        return this;
    }

    public SwitchField setDisabledColor(Color disabledColor) {
        if (disabledColor != null) this.disabledColor = disabledColor;
        repaint();
        return this;
    }

    public SwitchField setFocusColor(Color focusColor) {
        if (focusColor != null) this.focusColor = focusColor;
        repaint();
        return this;
    }

    public SwitchField setTextColor(Color textColor) {
        if (textColor != null) this.textColor = textColor;
        repaint();
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    protected void animateToSelection() {
        if (!animated || animationDuration <= 0 || !isShowing()) {
            animationProgress = selected ? 1f : 0f;
            animationTargetProgress = animationProgress;
            animationTimer.stop();
            repaint();
            return;
        }

        if (animationTimer.isRunning()) updateAnimationProgress();
        animationStartProgress = animationProgress;
        animationTargetProgress = selected ? 1f : 0f;
        float distance = Math.abs(animationTargetProgress - animationStartProgress);
        animationRunDurationNanos = Math.max(1L, Math.round(animationDuration * 1_000_000d * distance));
        animationStartedAtNanos = System.nanoTime();
        animationTimer.restart();
    }

    protected void updateAnimation() {
        updateAnimationProgress();
        repaint();

        if (animationProgress == animationTargetProgress) {
            animationTimer.stop();
        }
    }

    /** Returns the current thumb position for subclasses that customize painting. */
    protected float getAnimationProgress() {
        return animationProgress;
    }

    private void updateAnimationProgress() {
        long elapsed = Math.max(0L, System.nanoTime() - animationStartedAtNanos);
        float amount = Math.min(1f, elapsed / (float) Math.max(1L, animationRunDurationNanos));
        animationProgress = animationStartProgress
                + (animationTargetProgress - animationStartProgress) * amount;
        if (amount >= 1f) animationProgress = animationTargetProgress;
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        animationProgress = selected ? 1f : 0f;
        animationTargetProgress = animationProgress;
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle bounds = getSwitchBounds();
            paintTrack(g2, bounds);
            if (showText) {
                paintText(g2, bounds);
            }
            paintThumb(g2, bounds);
            if (isFocusOwner()) {
                paintFocus(g2, bounds);
            }
        } finally {
            g2.dispose();
        }
    }

    protected Rectangle getSwitchBounds() {
        int focusExtent = focusGap + (int) Math.ceil(focusStrokeWidth / 2f);
        int availableWidth = Math.max(0, getWidth() - trackInsets.left - trackInsets.right - focusExtent * 2);
        int availableHeight = Math.max(0, getHeight() - trackInsets.top - trackInsets.bottom - focusExtent * 2);
        int width = availableWidth;
        int height = availableHeight;
        int x = Math.min(getWidth(), trackInsets.left + focusExtent);
        int y = Math.min(getHeight(), trackInsets.top + focusExtent);
        return new Rectangle(x, y, width, height);
    }

    protected void paintTrack(Graphics2D g2, Rectangle bounds) {
        if (bounds.width <= 0 || bounds.height <= 0) return;
        g2.setColor(resolveTrackColor());
        int arc = trackArc == 0 ? bounds.height : Math.min(trackArc, Math.min(bounds.width, bounds.height));
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);
    }

    protected void paintThumb(Graphics2D g2, Rectangle bounds) {
        int maxSize = Math.max(0, Math.min(bounds.height - thumbPadding * 2, bounds.width - thumbPadding * 2));
        int size = thumbSize == 0 ? maxSize : Math.min(thumbSize, maxSize);
        if (size <= 0) return;
        int travel = Math.max(0, bounds.width - size - thumbPadding * 2);
        int x = bounds.x + thumbPadding + Math.round(travel * animationProgress);
        int y = bounds.y + (bounds.height - size) / 2;

        g2.setColor(new Color(0, 0, 0, isEnabled() ? 35 : 18));
        g2.fillOval(x, y + 1, size, size);
        g2.setColor(thumbColor);
        g2.fillOval(x, y, size, size);
    }

    protected void paintText(Graphics2D g2, Rectangle bounds) {
        String text = selected ? onText : offText;
        if (text.isBlank() || bounds.width <= 0 || bounds.height <= 0) return;

        g2.setFont(getFont().deriveFont(Font.BOLD, Math.max(9f, getFont().getSize2D() - 2f)));
        g2.setColor(textColor);
        FontMetrics fm = g2.getFontMetrics();
        int effectiveThumbSize = thumbSize == 0
                ? Math.max(0, bounds.height - thumbPadding * 2)
                : Math.min(thumbSize, Math.max(0, bounds.height - thumbPadding * 2));
        int zonePadding = Math.max(3, thumbPadding);
        int zoneWidth = Math.max(0, bounds.width - effectiveThumbSize - thumbPadding * 2 - zonePadding * 2);
        text = fitText(text, fm, zoneWidth);
        if (text.isEmpty()) return;
        int textWidth = fm.stringWidth(text);
        int x = selected
                ? bounds.x + thumbPadding + zonePadding
                : bounds.x + bounds.width - thumbPadding - zonePadding - textWidth;
        int y = bounds.y + (bounds.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
    }

    private String fitText(String text, FontMetrics metrics, int availableWidth) {
        if (availableWidth <= 0) return "";
        if (metrics.stringWidth(text) <= availableWidth) return text;
        String ellipsis = "…";
        if (metrics.stringWidth(ellipsis) > availableWidth) return "";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end) + ellipsis) > availableWidth) end--;
        return text.substring(0, end) + ellipsis;
    }

    protected void paintFocus(Graphics2D g2, Rectangle bounds) {
        if (bounds.width <= 0 || bounds.height <= 0) return;
        g2.setColor(focusColor);
        g2.setStroke(new BasicStroke(focusStrokeWidth));
        float offset = focusStrokeWidth / 2f;
        float x = bounds.x - focusGap - offset;
        float y = bounds.y - focusGap - offset;
        float width = bounds.width + focusGap * 2f + focusStrokeWidth;
        float height = bounds.height + focusGap * 2f + focusStrokeWidth;
        float arc = trackArc == 0 ? height : trackArc + focusGap * 2f;
        g2.draw(new RoundRectangle2D.Float(x, y, width, height, arc, arc));
    }

    protected Color resolveTrackColor() {
        if (!isEnabled()) return disabledColor;
        return blend(offColor, onColor, animationProgress);
    }

    protected Color blend(Color from, Color to, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        int a = Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped);
        return new Color(r, g, b, a);
    }
}
