package dtm.stools.component.inputfields.periodfield;

import dtm.stools.configs.UiTokens;
import dtm.stools.i18n.I18n;
import javax.swing.*;
import javax.accessibility.AccessibleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;

/** A 24-hour clock with native, keyboard-accessible hour and minute controls. */
public class ClockPickerPanel extends JPanel {
    private LocalTime time = LocalTime.MIDNIGHT;
    private LocalTime publishedTime;
    private boolean selectingHours = true;
    private boolean hourSelected, minuteSelected, required;
    private boolean selectionComplete;
    private final JToggleButton hours = new JToggleButton(), minutes = new JToggleButton();
    private final Dial dial = new Dial();

    public ClockPickerPanel(Locale locale) {
        super(new BorderLayout(0, UiTokens.space(2)));
        setLocale(Objects.requireNonNull(locale));
        JPanel header = new JPanel();
        ButtonGroup units = new ButtonGroup(); units.add(hours); units.add(minutes);
        hours.setSelected(true);
        hours.getAccessibleContext().setAccessibleName(text("hours", "Horas"));
        minutes.getAccessibleContext().setAccessibleName(text("minutes", "Minutos"));
        hours.addActionListener(e -> setSelectingHours(true));
        minutes.addActionListener(e -> setSelectingHours(false));
        header.add(hours); header.add(new JLabel(":")); header.add(minutes); add(header, BorderLayout.NORTH);
        add(dial, BorderLayout.CENTER);
        JButton previous = new JButton("−"), next = new JButton("+");
        previous.getAccessibleContext().setAccessibleName(text("decreaseTime", "Diminuir horário"));
        next.getAccessibleContext().setAccessibleName(text("increaseTime", "Aumentar horário"));
        previous.addActionListener(e -> step(-1)); next.addActionListener(e -> step(1));
        JPanel adjustments = new JPanel(); adjustments.add(previous); adjustments.add(next); add(adjustments, BorderLayout.SOUTH);
        getAccessibleContext().setAccessibleName(text("clock", "Relógio"));
        refresh();
    }

    private String text(String key, String fallback) { return I18n.getText(PeriodField.class, key, fallback); }
    public LocalTime getTime() { return hourSelected || minuteSelected ? time : null; }
    public void setTime(LocalTime time) {
        hourSelected = minuteSelected = time != null;
        updateTime(time == null ? LocalTime.MIDNIGHT : time);
    }
    public ClockPickerPanel setRequired(boolean required) {
        boolean old = this.required; this.required = required; firePropertyChange("required", old, required); return this;
    }
    public boolean isRequired() { return required; }
    public boolean isHourSelected() { return hourSelected; }
    public boolean isMinuteSelected() { return minuteSelected; }
    public boolean isSelectionValid() { return !required || (hourSelected && minuteSelected); }
    void focusIncompleteSelection() {
        setSelectingHours(!hourSelected);
        (selectingHours ? hours : minutes).requestFocusInWindow();
    }
    private void updateTime(LocalTime next) {
        LocalTime old = publishedTime; boolean wasComplete = selectionComplete;
        time = next; publishedTime = getTime(); selectionComplete = hourSelected && minuteSelected; refresh();
        firePropertyChange("time", old, publishedTime);
        firePropertyChange("selectionComplete", wasComplete, selectionComplete);
        getAccessibleContext().firePropertyChange(AccessibleContext.ACCESSIBLE_VALUE_PROPERTY, old, publishedTime);
    }
    public boolean isSelectingHours() { return selectingHours; }
    public void setSelectingHours(boolean hours) {
        selectingHours = hours; this.hours.setSelected(hours); minutes.setSelected(!hours); refresh();
    }
    private void step(int amount) {
        if (!isEnabled()) return;
        if (selectingHours) hourSelected = true; else minuteSelected = true;
        updateTime(selectingHours ? time.plusHours(amount) : time.plusMinutes(amount));
    }
    private void refresh() {
        hours.setText(hourSelected ? String.format(getLocale(), "%02d", time.getHour()) : "--");
        minutes.setText(minuteSelected ? String.format(getLocale(), "%02d", time.getMinute()) : "--");
        dial.rebuild();
    }
    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (dial != null) enableChildren(this, enabled);
    }
    private static void enableChildren(Container parent, boolean enabled) {
        for (Component child : parent.getComponents()) {
            child.setEnabled(enabled);
            if (child instanceof Container container) enableChildren(container, enabled);
        }
    }

    private final class Dial extends JPanel {
        Dial() {
            super(null); setOpaque(false);
            setName("clock.dial");
            setPreferredSize(new Dimension(UiTokens.scale(232), UiTokens.scale(232)));
            MouseAdapter pointer = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) selectMinute(e.getX(), e.getY()); }
                @Override public void mouseDragged(MouseEvent e) { if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) selectMinute(e.getX(), e.getY()); }
            };
            addMouseListener(pointer); addMouseMotionListener(pointer);
        }
        private void selectMinute(int x, int y) {
            if (selectingHours || !ClockPickerPanel.this.isEnabled()) return;
            double dx = x - getWidth() / 2.0, dy = y - getHeight() / 2.0;
            double radius = Math.min(getWidth(), getHeight()) / 2.0 - UiTokens.scale(22);
            double distance = Math.hypot(dx, dy);
            if (distance < radius * 0.45 || distance > radius + UiTokens.scale(22)) return;
            int minute = Math.floorMod((int) Math.round(Math.atan2(dx, -dy) * 30 / Math.PI), 60);
            minuteSelected = true; updateTime(time.withMinute(minute)); focusSelected();
        }
        void rebuild() {
            Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            boolean restore = focused != null && SwingUtilities.isDescendingFrom(focused, this);
            removeAll();
            int count = selectingHours ? 24 : 12;
            for (int index = 0; index < count; index++) {
                int number = selectingHours ? index : index * 5;
                JToggleButton button = new JToggleButton(String.format(getLocale(), "%02d", number)) {
                    @Override protected void processMouseEvent(MouseEvent e) {
                        if (!selectingHours && SwingUtilities.isLeftMouseButton(e)) {
                            if (e.getID() == MouseEvent.MOUSE_PRESSED) selectMinute(getX() + e.getX(), getY() + e.getY());
                            return;
                        }
                        super.processMouseEvent(e);
                    }
                    @Override protected void processMouseMotionEvent(MouseEvent e) {
                        if (!selectingHours && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
                            selectMinute(getX() + e.getX(), getY() + e.getY()); return;
                        }
                        super.processMouseMotionEvent(e);
                    }
                };
                button.putClientProperty("clock.value", number);
                button.setSelected((selectingHours ? hourSelected : minuteSelected) && number == (selectingHours ? time.getHour() : time.getMinute()));
                button.setMargin(new Insets(0, 0, 0, 0)); button.setBorderPainted(false);
                button.setContentAreaFilled(button.isSelected());
                if (button.isSelected()) { button.setBackground(UiTokens.accent()); button.setForeground(Color.WHITE); }
                button.setEnabled(ClockPickerPanel.this.isEnabled());
                button.getAccessibleContext().setAccessibleName(number + " " + (selectingHours ? text("hours", "Horas") : text("minutes", "Minutos")));
                button.addActionListener(e -> {
                    if (selectingHours) { hourSelected = true; updateTime(time.withHour(number)); setSelectingHours(false); focusSelected(); }
                    else { minuteSelected = true; updateTime(time.withMinute(number)); }
                });
                bind(button, "RIGHT", 1); bind(button, "UP", 1); bind(button, "LEFT", -1); bind(button, "DOWN", -1);
                button.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "select");
                button.getActionMap().put("select", new AbstractAction() { public void actionPerformed(ActionEvent e) { button.doClick(); } });
                add(button);
            }
            revalidate(); repaint();
            if (restore) focusSelected();
        }
        private void focusSelected() {
            int value = selectingHours ? time.getHour() : (time.getMinute() / 5) * 5;
            for (Component child : getComponents()) if (Objects.equals(((JComponent) child).getClientProperty("clock.value"), value)) child.requestFocusInWindow();
        }
        private void bind(JComponent button, String key, int amount) {
            button.getInputMap().put(KeyStroke.getKeyStroke(key), key);
            button.getActionMap().put(key, new AbstractAction() { public void actionPerformed(ActionEvent e) { step(amount); } });
        }
        @Override public void doLayout() {
            double radius = Math.min(getWidth(), getHeight()) / 2.0 - UiTokens.scale(22);
            int size = UiTokens.scale(32);
            for (Component child : getComponents()) {
                int number = (int) ((JComponent) child).getClientProperty("clock.value");
                double angle = Math.PI * 2 * number / (selectingHours ? 12 : 60);
                double ring = selectingHours && (number == 0 || number > 12) ? radius * 0.65 : radius;
                child.setBounds((int) (getWidth() / 2.0 + Math.sin(angle) * ring - size / 2.0),
                        (int) (getHeight() / 2.0 - Math.cos(angle) * ring - size / 2.0), size, size);
            }
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                double radius = Math.min(getWidth(), getHeight()) / 2.0 - UiTokens.scale(22);
                g.setColor(UiTokens.surfaceAlt());
                int backgroundRadius = (int) (radius + UiTokens.scale(20));
                g.fillOval(cx - backgroundRadius, cy - backgroundRadius, backgroundRadius * 2, backgroundRadius * 2);
                if (!selectingHours) {
                    g.setColor(UiTokens.muted());
                    for (int tick = 0; tick < 60; tick++) {
                        if (tick % 5 == 0) continue;
                        double tickAngle = Math.PI * 2 * tick / 60;
                        int size = Math.max(1, UiTokens.scale(2));
                        g.fillOval((int) (cx + Math.sin(tickAngle) * radius) - size / 2,
                                (int) (cy - Math.cos(tickAngle) * radius) - size / 2, size, size);
                    }
                }
                if (!(selectingHours ? hourSelected : minuteSelected)) return;
                int number = selectingHours ? time.getHour() : time.getMinute();
                double angle = Math.PI * 2 * number / (selectingHours ? 12 : 60);
                if (selectingHours && (number == 0 || number > 12)) radius *= 0.65;
                g.setColor(isEnabled() ? UiTokens.accent() : UiTokens.muted()); g.setStroke(new BasicStroke(UiTokens.scale(2)));
                g.drawLine(cx, cy, (int) (cx + Math.sin(angle) * radius), (int) (cy - Math.cos(angle) * radius));
                int dot = UiTokens.scale(6); g.fillOval(cx - dot / 2, cy - dot / 2, dot, dot);
                g.fillOval((int) (cx + Math.sin(angle) * radius) - dot / 2, (int) (cy - Math.cos(angle) * radius) - dot / 2, dot, dot);
            } finally { g.dispose(); }
        }
    }
}
