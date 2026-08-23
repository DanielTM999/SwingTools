package dtm.stools.component.inputfields.stepperfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.NumberField;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.layouts.FlexBoxLayout;
import dtm.stools.utils.PaintUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Campo numérico com botões de incremento e decremento, repetição automática ao manter pressionado.
 */
public class StepperField extends PanelEventListener {

    public static final String INCREMENTED = "stepperIncremented";
    public static final String DECREMENTED = "stepperDecremented";

    private final NumberField numberField = new NumberField();
    private final StepButton decrementButton = new StepButton(false);
    private final StepButton incrementButton = new StepButton(true);

    private boolean wheelEnabled = true;
    private int buttonWidth = 30;
    private int preferredHeight = 34;
    private int arc = UiTokens.radius(UiTokens.Radius.MD);

    private Color borderColor;
    private Color backgroundColor;

    public StepperField() {
        this(BigDecimal.ZERO);
    }

    public StepperField(BigDecimal initialValue) {
        super(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.ROW)
                .align(FlexBoxLayout.Align.STRETCH)
                .gap(0)
                .padding(1)
                .build(), false);

        setOpaque(false);
        configureNumberField(initialValue);

        add(decrementButton, FlexBoxLayout.FlexConstraints.of().fixedWidth(buttonWidth));
        add(numberField, FlexBoxLayout.FlexConstraints.of().grow(1));
        add(incrementButton, FlexBoxLayout.FlexConstraints.of().fixedWidth(buttonWidth));

        installListeners();
        setPreferredSize(new Dimension(UiTokens.scale(150), preferredHeight));
        setMinimumSize(new Dimension(UiTokens.scale(100), preferredHeight));
    }

    /**
     * Valor corrente do campo.
     */
    public BigDecimal getValue() {
        return numberField.getValue();
    }

    /**
     * Define o valor corrente disparando eventos.
     */
    public StepperField setValue(BigDecimal value) {
        return setValue(value, true);
    }

    /**
     * Define o valor corrente, opcionalmente sem disparar eventos.
     */
    public StepperField setValue(BigDecimal value, boolean fireEvent) {
        numberField.setValue(value, fireEvent);
        updateButtonStates();
        return this;
    }

    /**
     * Define o intervalo aceito pelo campo.
     */
    public StepperField setRange(BigDecimal minimum, BigDecimal maximum) {
        numberField.setRange(minimum, maximum);
        updateButtonStates();
        return this;
    }

    /**
     * Define o incremento aplicado pelos botões.
     */
    public StepperField setStep(BigDecimal step) {
        numberField.setStep(step);
        return this;
    }

    /**
     * Incremento corrente.
     */
    public BigDecimal getStep() {
        return numberField.getStep();
    }

    /**
     * Define a quantidade de casas decimais exibidas.
     */
    public StepperField setDecimalPlaces(int decimalPlaces) {
        numberField.setDecimalPlaces(decimalPlaces);
        return this;
    }

    /**
     * Habilita a alteração do valor pela roda do mouse.
     */
    public StepperField setWheelEnabled(boolean wheelEnabled) {
        this.wheelEnabled = wheelEnabled;
        return this;
    }

    /**
     * Define a largura de cada botão.
     */
    public StepperField setButtonWidth(int buttonWidth) {
        if (buttonWidth <= 0) {
            throw new IllegalArgumentException("buttonWidth must be greater than zero");
        }
        this.buttonWidth = buttonWidth;
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define o raio de canto do campo.
     */
    public StepperField setArc(int arc) {
        if (arc < 0) {
            throw new IllegalArgumentException("arc cannot be negative");
        }
        this.arc = arc;
        repaint();
        return this;
    }

    /**
     * Define as cores de fundo e de borda do campo.
     */
    public StepperField setColors(Color backgroundColor, Color borderColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        numberField.setBackground(backgroundColor != null ? backgroundColor : UiTokens.surface());
        repaint();
        return this;
    }

    /**
     * Campo numérico interno, exposto para configurações avançadas.
     */
    public NumberField getNumberField() {
        return numberField;
    }

    /**
     * Incrementa o valor em um passo.
     */
    public StepperField increment() {
        return shift(true);
    }

    /**
     * Decrementa o valor em um passo.
     */
    public StepperField decrement() {
        return shift(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        numberField.setEnabled(enabled);
        decrementButton.setEnabled(enabled);
        incrementButton.setEnabled(enabled);
        updateButtonStates();
    }

    private void configureNumberField(BigDecimal initialValue) {
        numberField.setValue(initialValue != null ? initialValue : BigDecimal.ZERO, false);
        numberField.setHorizontalAlignment(SwingConstants.CENTER);
        numberField.setBorder(BorderFactory.createEmptyBorder());
        numberField.setOpaque(false);
        numberField.setFont(UiTokens.font());
        numberField.setForeground(UiTokens.foreground());
    }

    private void installListeners() {
        decrementButton.onStep(() -> shift(false));
        incrementButton.onStep(() -> shift(true));

        numberField.addEventListener(EventType.CHANGE, event -> updateButtonStates());

        addMouseWheelListener((MouseWheelEvent e) -> {
            if (!wheelEnabled || !isEnabled()) {
                return;
            }
            shift(e.getWheelRotation() < 0);
            e.consume();
        });
    }

    private StepperField shift(boolean up) {
        if (!isEnabled()) {
            return this;
        }

        BigDecimal current = numberField.getValue() != null ? numberField.getValue() : BigDecimal.ZERO;
        BigDecimal step = numberField.getStep() != null ? numberField.getStep() : BigDecimal.ONE;
        BigDecimal candidate = up ? current.add(step) : current.subtract(step);

        BigDecimal minimum = numberField.getMinimumValue();
        BigDecimal maximum = numberField.getMaximumValue();
        if (minimum != null && candidate.compareTo(minimum) < 0) {
            candidate = minimum;
        }
        if (maximum != null && candidate.compareTo(maximum) > 0) {
            candidate = maximum;
        }

        if (candidate.compareTo(current) == 0) {
            return this;
        }

        numberField.setValue(candidate, true);
        updateButtonStates();

        Map<String, Object> props = Map.of("oldValue", current, "newValue", candidate);
        dispatchEvent(up ? INCREMENTED : DECREMENTED, this, candidate, props);
        dispatchEvent(EventType.CHANGE, this, candidate, props);
        return this;
    }

    private void updateButtonStates() {
        BigDecimal current = numberField.getValue();
        BigDecimal minimum = numberField.getMinimumValue();
        BigDecimal maximum = numberField.getMaximumValue();

        boolean enabled = isEnabled();
        decrementButton.setEnabled(enabled && (minimum == null || current == null || current.compareTo(minimum) > 0));
        incrementButton.setEnabled(enabled && (maximum == null || current == null || current.compareTo(maximum) < 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
            Color fill = backgroundColor != null ? backgroundColor : UiTokens.surface();
            Color stroke = borderColor != null ? borderColor : UiTokens.border();
            if (!isEnabled()) {
                fill = UiTokens.disabled(fill);
                stroke = UiTokens.disabled(stroke);
            }
            PaintUtils.fillRoundRect(g2, bounds, arc, fill);
            PaintUtils.drawRoundRect(g2, bounds, arc, stroke, UiTokens.stroke());
        } finally {
            g2.dispose();
        }
    }

    /**
     * Botão lateral que aplica o passo e repete enquanto permanecer pressionado.
     */
    private static final class StepButton extends JComponent {

        private static final int INITIAL_DELAY = 400;
        private static final int REPEAT_DELAY = 70;

        private final boolean up;
        private final Timer repeatTimer;

        private Runnable action = () -> { };
        private boolean hover;
        private boolean pressed;

        private StepButton(boolean up) {
            this.up = up;
            this.repeatTimer = new Timer(REPEAT_DELAY, e -> action.run());
            this.repeatTimer.setInitialDelay(INITIAL_DELAY);

            setOpaque(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            installMouse();
        }

        private void onStep(Runnable action) {
            this.action = action != null ? action : () -> { };
        }

        private void installMouse() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    repeatTimer.stop();
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) {
                        return;
                    }
                    pressed = true;
                    action.run();
                    repeatTimer.start();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repeatTimer.stop();
                    repaint();
                }
            });
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (!enabled) {
                hover = false;
                pressed = false;
                repeatTimer.stop();
            }
            repaint();
        }

        @Override
        public void removeNotify() {
            repeatTimer.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
            try {
                if (isEnabled() && (hover || pressed)) {
                    Color base = UiTokens.surfaceAlt();
                    PaintUtils.fillRoundRect(g2, new Rectangle(2, 2, getWidth() - 4, getHeight() - 4),
                            UiTokens.radius(UiTokens.Radius.SM),
                            pressed ? UiTokens.pressed(base) : UiTokens.hover(base));
                }

                Color color = isEnabled()
                        ? (hover ? UiTokens.primary() : UiTokens.foreground())
                        : UiTokens.disabled(UiTokens.foreground());
                g2.setColor(color);
                g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int arm = UiTokens.scale(5);
                g2.drawLine(cx - arm, cy, cx + arm, cy);
                if (up) {
                    g2.drawLine(cx, cy - arm, cx, cy + arm);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
