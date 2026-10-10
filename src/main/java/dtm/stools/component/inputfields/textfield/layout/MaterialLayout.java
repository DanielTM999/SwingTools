package dtm.stools.component.inputfields.textfield.layout;

import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Layout contornado com rótulo flutuante e erro persistente. Use uma instância por campo,
 * configurada na EDT. O rótulo pertence ao campo; mensagem e cor do erro pertencem ao layout.
 */
public class MaterialLayout implements FieldLayoutManager {
    private static final long ANIMATION_NANOS = 150_000_000L;
    private FieldLayoutTarget owner;
    private String error;
    private Color errorColor;
    private String previousDescription;
    private String appliedDescription;
    private boolean focused;
    private float labelProgress;
    private float animationFrom;
    private float animationTarget;
    private long animationStarted;
    private final Timer animationTimer = new Timer(16, e -> tickAnimation());
    private final FocusAdapter focusListener = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) { focused = true; updateLabelState(); }
        @Override
        public void focusLost(FocusEvent e) { focused = false; updateLabelState(); }
    };
    private final ComponentAdapter componentListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) { refresh(); }
        @Override
        public void componentHidden(ComponentEvent e) { settleLabel(); }
        @Override
        public void componentShown(ComponentEvent e) { updateLabelState(); }
    };
    private final PropertyChangeListener propertyListener = e -> {
        switch (e.getPropertyName()) {
            case "font", "label", "enabled", "editable", "border", "background", "foreground" -> refresh();
            default -> { }
        }
    };

    public String getError() { return error; }

    /** null ou vazio remove a mensagem; editar ou limpar o campo mantém o erro. */
    public MaterialLayout setError(String error) {
        this.error = error == null || error.isEmpty() ? null : error;
        updateAccessibleDescription();
        refresh();
        return this;
    }

    public Color getErrorColor() { return errorColor == null ? UiTokens.danger() : errorColor; }

    /** null restaura a cor danger do tema. */
    public MaterialLayout setErrorColor(Color color) {
        errorColor = color;
        refresh();
        return this;
    }

    @Override
    public void install(FieldLayoutTarget field) {
        Objects.requireNonNull(field, "field");
        if (owner != null && owner != field) {
            throw new IllegalStateException("MaterialLayout já está instalado em outro campo");
        }
        if (owner == field) return;
        owner = field;
        previousDescription = field.getFieldComponent().getAccessibleContext().getAccessibleDescription();
        appliedDescription = previousDescription;
        focused = field.getFieldComponent().hasFocus();
        field.getFieldComponent().addFocusListener(focusListener);
        field.getFieldComponent().addComponentListener(componentListener);
        field.getFieldComponent().addPropertyChangeListener(propertyListener);
        updateAccessibleDescription();
        settleLabel();
        refresh();
    }

    @Override
    public void uninstall(FieldLayoutTarget field) {
        if (owner != field) return;
        animationTimer.stop();
        field.getFieldComponent().removeFocusListener(focusListener);
        field.getFieldComponent().removeComponentListener(componentListener);
        field.getFieldComponent().removePropertyChangeListener(propertyListener);
        if (Objects.equals(field.getFieldComponent().getAccessibleContext().getAccessibleDescription(), appliedDescription)) {
            field.getFieldComponent().getAccessibleContext().setAccessibleDescription(previousDescription);
        }
        owner = null;
        previousDescription = appliedDescription = null;
        focused = false;
    }

    @Override
    public void fieldChanged(FieldLayoutTarget field) {
        if (owner == field) updateLabelState();
    }

    @Override
    public void fieldShown(FieldLayoutTarget field) {
        if (owner != field) return;
        focused = field.getFieldComponent().hasFocus();
        updateLabelState();
    }

    @Override
    public void fieldRemoved(FieldLayoutTarget field) {
        if (owner != field) return;
        focused = false;
        settleLabel();
    }

    @Override
    public void themeChanged(FieldLayoutTarget field) {
        UiTokens.refresh();
        refresh();
    }

    private void updateAccessibleDescription() {
        if (owner == null) return;
        appliedDescription = error == null ? previousDescription : error;
        owner.getFieldComponent().getAccessibleContext().setAccessibleDescription(appliedDescription);
    }

    private void refresh() {
        if (owner == null) return;
        owner.getFieldComponent().revalidate();
        owner.getFieldComponent().repaint();
    }

    private float targetProgress() {
        return owner != null && (focused || !owner.isFieldContentEmpty()) ? 1f : 0f;
    }

    private void settleLabel() {
        animationTimer.stop();
        labelProgress = animationTarget = targetProgress();
        if (owner != null) owner.getFieldComponent().repaint();
    }

    private void updateLabelState() {
        if (owner == null) return;
        float target = targetProgress();
        if (!owner.getFieldComponent().isShowing()) {
            settleLabel();
        } else if (target != animationTarget) {
            if (animationTimer.isRunning()) tickAnimation();
            animationFrom = labelProgress;
            animationTarget = target;
            animationStarted = System.nanoTime();
            animationTimer.restart();
        }
        owner.getFieldComponent().repaint();
    }

    private void tickAnimation() {
        if (owner == null || !owner.getFieldComponent().isShowing()) { settleLabel(); return; }
        float time = Math.min(1f, (System.nanoTime() - animationStarted) / (float) ANIMATION_NANOS);
        float eased = time * time * (3f - 2f * time);
        labelProgress = animationFrom + (animationTarget - animationFrom) * eased;
        if (time >= 1f) animationTimer.stop();
        owner.getFieldComponent().repaint();
    }

    /** Progresso para subclasses que personalizam a pintura (0 interno, 1 elevado). */
    protected float getLabelProgress() { return labelProgress; }

    @Override
    public boolean isPlaceholderVisible(FieldLayoutTarget field) {
        return field.getLabel().isEmpty() || labelProgress >= 1f;
    }

    private Font font(FieldLayoutTarget field) {
        return field.getFieldComponent().getFont() == null ? UiTokens.font() : field.getFieldComponent().getFont();
    }

    private Font smallFont(FieldLayoutTarget field) {
        Font font = font(field);
        return font.deriveFont(Math.max(1f, font.getSize2D() * 0.8f));
    }

    private int topSpace(FieldLayoutTarget field) {
        return field.getFieldComponent().getFontMetrics(smallFont(field)).getHeight() / 2 + UiTokens.space(1);
    }

    private int footerHeight(FieldLayoutTarget field, int width) {
        int lines = Math.max(1, errorLines(field, Math.max(1, width - UiTokens.space(6))).size());
        return UiTokens.space(2) + lines * field.getFieldComponent().getFontMetrics(smallFont(field)).getHeight();
    }

    @Override
    public Insets getInsets(FieldLayoutTarget field) {
        int width = field.getFieldComponent().getWidth() > 0 ? field.getFieldComponent().getWidth() : UiTokens.scale(280);
        return new Insets(topSpace(field) + UiTokens.space(2), UiTokens.space(3),
                footerHeight(field, width) + UiTokens.space(2), UiTokens.space(3));
    }

    @Override
    public Rectangle getFieldBounds(FieldLayoutTarget field) {
        int top = topSpace(field);
        return new Rectangle(0, top, field.getFieldComponent().getWidth(),
                Math.max(0, field.getFieldComponent().getHeight() - top - footerHeight(field, field.getFieldComponent().getWidth())));
    }

    @Override
    public Dimension getPreferredSize(FieldLayoutTarget field, Dimension naturalSize) {
        int width = Math.max(UiTokens.scale(280), naturalSize.width);
        int available = field.getFieldComponent().getWidth() > 0 ? field.getFieldComponent().getWidth() : width;
        int body = Math.max(UiTokens.scale(56), field.getFieldComponent().getFontMetrics(font(field)).getHeight() + UiTokens.space(4));
        return new Dimension(width, topSpace(field) + body + footerHeight(field, available));
    }

    @Override
    public Dimension getMinimumSize(FieldLayoutTarget field, Dimension naturalSize) {
        Dimension preferred = getPreferredSize(field, naturalSize);
        return new Dimension(UiTokens.scale(80), preferred.height);
    }

    private Color stateColor(FieldLayoutTarget field, boolean border) {
        Color color = error != null ? getErrorColor()
                : focused ? UiTokens.primary() : border ? UiTokens.border() : UiTokens.muted();
        return field.getFieldComponent().isEnabled() ? color : UiTokens.disabled(color);
    }

    private record LabelLayout(Font font, String text, float x, float baseline, int width, int height) {}

    private LabelLayout labelLayout(FieldLayoutTarget field) {
        Rectangle bounds = getFieldBounds(field);
        Font base = font(field);
        Font font = base.deriveFont(base.getSize2D() + (smallFont(field).getSize2D() - base.getSize2D()) * labelProgress);
        FontMetrics metrics = field.getFieldComponent().getFontMetrics(font);
        Rectangle content = field.getFieldContentBounds();
        String text = PaintUtils.fitText(metrics, field.getLabel(), content.width);
        float resting = content.y + (content.height - metrics.getHeight()) / 2f + metrics.getAscent();
        float raised = bounds.y + (metrics.getAscent() - metrics.getDescent()) / 2f;
        return new LabelLayout(font, text, content.x, resting + (raised - resting) * labelProgress,
                metrics.stringWidth(text), metrics.getHeight());
    }

    @Override
    public void paintBackground(Graphics2D g, FieldLayoutTarget field) {
        Color fill = field.getFieldComponent().getBackground();
        if (!field.getFieldComponent().isEnabled()) fill = UiTokens.disabled(fill);
        PaintUtils.fillRoundRect(g, getFieldBounds(field), UiTokens.radius(UiTokens.Radius.SM), fill);
    }

    @Override
    public void paintBorder(Graphics2D g, FieldLayoutTarget field) {
        Graphics2D border = (Graphics2D) g.create();
        try {
            LabelLayout layout = labelLayout(field);
            if (!layout.text().isEmpty()) {
                Area clip = new Area(new Rectangle(0, 0, field.getFieldComponent().getWidth(), field.getFieldComponent().getHeight()));
                FontMetrics metrics = field.getFieldComponent().getFontMetrics(layout.font());
                clip.subtract(new Area(new Rectangle2D.Float(layout.x() - UiTokens.space(1),
                        layout.baseline() - metrics.getAscent(),
                        layout.width() + UiTokens.space(2), layout.height())));
                border.clip(clip);
            }
            PaintUtils.drawRoundRect(border, getFieldBounds(field), UiTokens.radius(UiTokens.Radius.SM),
                    stateColor(field, true), focused || error != null ? UiTokens.scale(2) : UiTokens.scale(1));
        } finally { border.dispose(); }
    }

    @Override
    public void paintOverlay(Graphics2D g, FieldLayoutTarget field) {
        LabelLayout layout = labelLayout(field);
        g.setFont(layout.font());
        g.setColor(stateColor(field, false));
        g.drawString(layout.text(), layout.x(), layout.baseline());
        if (error != null) {
            Rectangle bounds = getFieldBounds(field);
            g.setFont(smallFont(field));
            g.setColor(field.getFieldComponent().isEnabled() ? getErrorColor() : UiTokens.disabled(getErrorColor()));
            FontMetrics metrics = g.getFontMetrics();
            int y = bounds.y + bounds.height + UiTokens.space(2) + metrics.getAscent();
            for (String line : errorLines(field, Math.max(1, bounds.width - UiTokens.space(6)))) {
                g.drawString(line, bounds.x + UiTokens.space(3), y);
                y += metrics.getHeight();
            }
        }
    }

    private List<String> errorLines(FieldLayoutTarget field, int width) {
        if (error == null) return List.of();
        FontMetrics metrics = field.getFieldComponent().getFontMetrics(smallFont(field));
        List<String> lines = new ArrayList<>();
        for (String paragraph : error.split("\\R", -1)) {
            String remaining = paragraph;
            while (metrics.stringWidth(remaining) > width) {
                int end = 0;
                for (int next = 0; next < remaining.length();) {
                    next += Character.charCount(remaining.codePointAt(next));
                    if (end > 0 && metrics.stringWidth(remaining.substring(0, next)) > width) break;
                    end = next;
                }
                int space = remaining.lastIndexOf(' ', end);
                if (space > 0) end = space;
                lines.add(remaining.substring(0, end));
                remaining = remaining.substring(end).stripLeading();
            }
            lines.add(remaining);
        }
        return lines;
    }
}
