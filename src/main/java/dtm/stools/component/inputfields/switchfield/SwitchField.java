package dtm.stools.component.inputfields.switchfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class SwitchField extends PanelEventListener {

    public static final String SWITCH_ON = "switchOn";
    public static final String SWITCH_OFF = "switchOff";

    private boolean selected;
    private boolean animated = true;
    private boolean showText;
    private float animationProgress;
    private long animationStartedAt;
    private int animationDuration = 140;

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

        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(56, 30));
        setMinimumSize(new Dimension(44, 24));
        setOpaque(false);

        animationTimer = new Timer(16, e -> updateAnimation());
        installListeners();
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isEnabled()) return;
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
        return this;
    }

    public SwitchField setAnimationDuration(int animationDuration) {
        this.animationDuration = Math.max(0, animationDuration);
        return this;
    }

    public SwitchField setShowText(boolean showText) {
        this.showText = showText;
        repaint();
        return this;
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

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        int width = Math.max(preferred.width, showText ? 72 : 56);
        int height = Math.max(preferred.height, 30);
        return new Dimension(width, height);
    }

    protected void animateToSelection() {
        if (!animated || animationDuration <= 0 || !isShowing()) {
            animationProgress = selected ? 1f : 0f;
            animationTimer.stop();
            repaint();
            return;
        }

        animationStartedAt = System.currentTimeMillis();
        animationTimer.restart();
    }

    protected void updateAnimation() {
        float elapsed = System.currentTimeMillis() - animationStartedAt;
        float amount = Math.min(1f, elapsed / Math.max(1, animationDuration));
        animationProgress = selected ? amount : 1f - amount;
        repaint();

        if (amount >= 1f) {
            animationProgress = selected ? 1f : 0f;
            animationTimer.stop();
        }
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
        int width = Math.max(36, getWidth() - 2);
        int height = Math.max(20, Math.min(getHeight() - 2, 32));
        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;
        return new Rectangle(x, y, width, height);
    }

    protected void paintTrack(Graphics2D g2, Rectangle bounds) {
        g2.setColor(resolveTrackColor());
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, bounds.height, bounds.height);
    }

    protected void paintThumb(Graphics2D g2, Rectangle bounds) {
        int padding = 3;
        int size = bounds.height - padding * 2;
        int travel = bounds.width - size - padding * 2;
        int x = bounds.x + padding + Math.round(travel * animationProgress);
        int y = bounds.y + padding;

        g2.setColor(new Color(0, 0, 0, isEnabled() ? 35 : 18));
        g2.fillOval(x, y + 1, size, size);
        g2.setColor(thumbColor);
        g2.fillOval(x, y, size, size);
    }

    protected void paintText(Graphics2D g2, Rectangle bounds) {
        String text = selected ? onText : offText;
        if (text.isBlank()) return;

        g2.setFont(getFont().deriveFont(Font.BOLD, Math.max(9f, getFont().getSize2D() - 2f)));
        g2.setColor(textColor);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = selected ? bounds.x + 8 : bounds.x + bounds.width - textWidth - 8;
        int y = bounds.y + (bounds.height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
    }

    protected void paintFocus(Graphics2D g2, Rectangle bounds) {
        g2.setColor(focusColor);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4, bounds.height + 4, bounds.height + 4);
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
