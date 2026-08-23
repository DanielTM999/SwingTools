package dtm.stools.component.feedback.steps;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Indicador de etapas de um fluxo, com círculos numerados, marca de conclusão e conector.
 */
public class StepsPanel extends PanelEventListener {

    public static final String STEP_SELECTED = "stepSelected";

    /**
     * Orientação da trilha de etapas.
     */
    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    /**
     * Etapa exibida na trilha.
     */
    public record Step(String title, String description) {
    }

    private final List<Step> steps = new ArrayList<>();

    private Orientation orientation = Orientation.HORIZONTAL;
    private int currentStep;
    private int hoverStep = -1;
    private boolean clickable;

    private int circleSize = 28;
    private int connectorThickness = 2;
    private int labelGap = UiTokens.space(2);

    private Color activeColor;
    private Color doneColor;
    private Color pendingColor;

    public StepsPanel() {
        this(Orientation.HORIZONTAL);
    }

    public StepsPanel(Orientation orientation) {
        super(null, false);
        this.orientation = orientation != null ? orientation : Orientation.HORIZONTAL;

        setOpaque(false);
        setFont(UiTokens.fontSmall());
        installListeners();
        updatePreferredSize();
    }

    /**
     * Adiciona uma etapa com título e descrição opcional.
     */
    public StepsPanel addStep(String title, String description) {
        steps.add(new Step(title != null ? title : "", description != null ? description : ""));
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Substitui as etapas usando apenas os títulos.
     */
    public StepsPanel setSteps(List<String> titles) {
        steps.clear();
        if (titles != null) {
            titles.forEach(title -> steps.add(new Step(title, "")));
        }
        currentStep = 0;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Índice da etapa corrente.
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * Define a etapa corrente disparando eventos.
     */
    public StepsPanel setCurrentStep(int currentStep) {
        return setCurrentStep(currentStep, true);
    }

    /**
     * Define a etapa corrente, opcionalmente sem disparar eventos.
     */
    public StepsPanel setCurrentStep(int currentStep, boolean fireEvent) {
        if (currentStep < 0 || currentStep >= Math.max(1, steps.size()) || currentStep == this.currentStep) {
            return this;
        }

        int oldValue = this.currentStep;
        this.currentStep = currentStep;
        repaint();

        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", oldValue, "newValue", currentStep);
            dispatchEvent(STEP_SELECTED, this, currentStep, props);
            dispatchEvent(EventType.STEP, this, currentStep, props);
            dispatchEvent(EventType.CHANGE, this, currentStep, props);
        }
        return this;
    }

    /**
     * Avança uma etapa.
     */
    public StepsPanel next() {
        return setCurrentStep(Math.min(steps.size() - 1, currentStep + 1));
    }

    /**
     * Retrocede uma etapa.
     */
    public StepsPanel previous() {
        return setCurrentStep(Math.max(0, currentStep - 1));
    }

    /**
     * Permite que o usuário selecione etapas já concluídas.
     */
    public StepsPanel setClickable(boolean clickable) {
        this.clickable = clickable;
        return this;
    }

    /**
     * Define a orientação da trilha.
     */
    public StepsPanel setOrientation(Orientation orientation) {
        if (orientation == null) {
            throw new IllegalArgumentException("orientation cannot be null");
        }
        this.orientation = orientation;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define o diâmetro dos círculos.
     */
    public StepsPanel setCircleSize(int circleSize) {
        if (circleSize <= 0) {
            throw new IllegalArgumentException("circleSize must be greater than zero");
        }
        this.circleSize = circleSize;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define as cores das etapas ativa, concluída e pendente.
     */
    public StepsPanel setColors(Color activeColor, Color doneColor, Color pendingColor) {
        this.activeColor = activeColor;
        this.doneColor = doneColor;
        this.pendingColor = pendingColor;
        repaint();
        return this;
    }

    /**
     * Etapas registradas na trilha.
     */
    public List<Step> getSteps() {
        return List.copyOf(steps);
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!clickable || !isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int index = indexAt(e.getPoint());
                if (index >= 0 && index <= currentStep) {
                    setCurrentStep(index);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverStep = -1;
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!clickable || !isEnabled()) {
                    return;
                }
                int index = indexAt(e.getPoint());
                boolean selectable = index >= 0 && index <= currentStep;
                setCursor(Cursor.getPredefinedCursor(selectable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                if (index != hoverStep) {
                    hoverStep = selectable ? index : -1;
                    repaint();
                }
            }
        });
    }

    private void updatePreferredSize() {
        if (steps == null) {
            return;
        }
        FontMetrics metrics = getFontMetrics(getFont() != null ? getFont() : UiTokens.fontSmall());
        if (orientation == Orientation.HORIZONTAL) {
            int height = circleSize + labelGap + metrics.getHeight() * 2;
            setPreferredSize(new Dimension(UiTokens.scale(120) * Math.max(1, steps.size()), height));
            setMinimumSize(new Dimension(UiTokens.scale(60) * Math.max(1, steps.size()), height));
        } else {
            int height = Math.max(1, steps.size()) * (circleSize + UiTokens.space(6));
            setPreferredSize(new Dimension(UiTokens.scale(220), height));
            setMinimumSize(new Dimension(UiTokens.scale(120), height));
        }
        revalidate();
    }

    private int indexAt(Point point) {
        for (int i = 0; i < steps.size(); i++) {
            if (getCircleBounds(i).contains(point)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Retângulo do círculo da etapa informada.
     */
    protected Rectangle getCircleBounds(int index) {
        if (orientation == Orientation.HORIZONTAL) {
            float slot = (float) getWidth() / Math.max(1, steps.size());
            int cx = Math.round(slot * index + slot / 2f);
            return new Rectangle(cx - circleSize / 2, 0, circleSize, circleSize);
        }
        int slot = getHeight() / Math.max(1, steps.size());
        return new Rectangle(0, slot * index + (slot - circleSize) / 2, circleSize, circleSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            for (int i = 0; i < steps.size(); i++) {
                if (i < steps.size() - 1) {
                    paintConnector(g2, i);
                }
            }
            for (int i = 0; i < steps.size(); i++) {
                Rectangle circle = getCircleBounds(i);
                paintCircle(g2, circle, i);
                paintLabels(g2, circle, i);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Pinta o conector entre a etapa informada e a seguinte.
     */
    protected void paintConnector(Graphics2D g2, int index) {
        Rectangle from = getCircleBounds(index);
        Rectangle to = getCircleBounds(index + 1);

        g2.setColor(index < currentStep ? resolveDone() : resolvePending());
        g2.setStroke(new BasicStroke(connectorThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (orientation == Orientation.HORIZONTAL) {
            int y = from.y + from.height / 2;
            g2.drawLine(from.x + from.width + UiTokens.space(1), y, to.x - UiTokens.space(1), y);
        } else {
            int x = from.x + from.width / 2;
            g2.drawLine(x, from.y + from.height + UiTokens.space(1), x, to.y - UiTokens.space(1));
        }
    }

    /**
     * Pinta o círculo de uma etapa com número ou marca de conclusão.
     */
    protected void paintCircle(Graphics2D g2, Rectangle circle, int index) {
        boolean done = index < currentStep;
        boolean active = index == currentStep;

        Color color = done ? resolveDone() : active ? resolveActive() : resolvePending();
        if (!isEnabled()) {
            color = UiTokens.disabled(color);
        }

        if (done || active) {
            g2.setColor(index == hoverStep ? UiTokens.hover(color) : color);
            g2.fillOval(circle.x, circle.y, circle.width, circle.height);
        } else {
            g2.setColor(UiTokens.surface());
            g2.fillOval(circle.x, circle.y, circle.width, circle.height);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(UiTokens.stroke()));
            g2.drawOval(circle.x, circle.y, circle.width - 1, circle.height - 1);
        }

        if (done) {
            paintCheck(g2, circle, UiTokens.onColor(color));
            return;
        }

        g2.setFont(UiTokens.fontSmall().deriveFont(Font.BOLD));
        Color textColor = active ? UiTokens.onColor(color) : UiTokens.muted();
        PaintUtils.drawCenteredText(g2, String.valueOf(index + 1), circle, textColor);
    }

    private void paintCheck(Graphics2D g2, Rectangle circle, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = circle.x + circle.width / 2;
        int cy = circle.y + circle.height / 2;
        int arm = circle.width / 5;
        g2.drawLine(cx - arm, cy, cx - arm / 3, cy + arm * 2 / 3);
        g2.drawLine(cx - arm / 3, cy + arm * 2 / 3, cx + arm, cy - arm * 2 / 3);
    }

    /**
     * Pinta o título e a descrição da etapa.
     */
    protected void paintLabels(Graphics2D g2, Rectangle circle, int index) {
        Step step = steps.get(index);
        g2.setFont(UiTokens.fontSmall().deriveFont(index == currentStep ? Font.BOLD : Font.PLAIN));
        FontMetrics metrics = g2.getFontMetrics();

        Color titleColor = index <= currentStep ? UiTokens.foreground() : UiTokens.muted();
        if (!isEnabled()) {
            titleColor = UiTokens.disabled(titleColor);
        }

        if (orientation == Orientation.HORIZONTAL) {
            float slot = (float) getWidth() / Math.max(1, steps.size());
            Rectangle titleBounds = new Rectangle(
                    Math.round(slot * index), circle.y + circle.height + labelGap,
                    Math.round(slot), metrics.getHeight());
            PaintUtils.drawCenteredText(g2, step.title(), titleBounds, titleColor);

            if (!step.description().isEmpty()) {
                g2.setFont(UiTokens.fontSmall());
                Rectangle descriptionBounds = new Rectangle(
                        titleBounds.x, titleBounds.y + titleBounds.height, titleBounds.width, metrics.getHeight());
                PaintUtils.drawCenteredText(g2, step.description(), descriptionBounds, UiTokens.muted());
            }
            return;
        }

        int x = circle.x + circle.width + labelGap;
        int width = Math.max(0, getWidth() - x);
        PaintUtils.drawLeftText(g2, step.title(),
                new Rectangle(x, circle.y, width, circle.height / 2 + metrics.getHeight() / 2), titleColor);

        if (!step.description().isEmpty()) {
            g2.setFont(UiTokens.fontSmall());
            PaintUtils.drawLeftText(g2, step.description(),
                    new Rectangle(x, circle.y + circle.height / 2, width, circle.height / 2 + metrics.getHeight() / 2),
                    UiTokens.muted());
        }
    }

    private Color resolveActive() {
        return activeColor != null ? activeColor : UiTokens.primary();
    }

    private Color resolveDone() {
        return doneColor != null ? doneColor : UiTokens.success();
    }

    private Color resolvePending() {
        return pendingColor != null ? pendingColor : UiTokens.border();
    }
}
