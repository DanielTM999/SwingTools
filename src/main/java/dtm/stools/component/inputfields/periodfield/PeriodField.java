package dtm.stools.component.inputfields.periodfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.i18n.I18n;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.Objects;

/** A local, typed period. Invalid editor drafts never replace the last committed value. */
public class PeriodField extends PanelEventListener {
    public enum Presentation { FIELDS, CALENDAR }
    private final PeriodMode mode;
    private final JTextField startInput = new JTextField(16), endInput = new JTextField(16);
    private final JButton calendarButton = new JButton();
    private final JLabel error = new JLabel();
    private PeriodValue value;
    private Temporal minimum, maximum;
    private boolean updating, inputValid = true, editable = true, allowOvernight, required;
    private Presentation presentation = Presentation.FIELDS;
    private String pattern;
    private DateTimeFormatter formatter;
    private JPopupMenu popup;

    public PeriodField() { this(PeriodMode.DATE); }
    public PeriodField(PeriodMode mode) {
        super(new BorderLayout(0, UiTokens.space(1)), false);
        this.mode = Objects.requireNonNull(mode);
        pattern = switch (mode) { case DATE -> "dd/MM/uuuu"; case DATE_TIME -> "dd/MM/uuuu HH:mm"; case TIME -> "HH:mm"; };
        formatter = makeFormatter(pattern);
        JPanel inputs = new JPanel(new GridLayout(1, 2, UiTokens.space(2), 0));
        inputs.add(labeled(text("start", "Início"), startInput)); inputs.add(labeled(text("end", "Fim"), endInput));
        add(inputs, BorderLayout.CENTER);
        calendarButton.setText(mode == PeriodMode.TIME ? text("clock", "Relógio") : text("calendar", "Calendário de intervalo"));
        calendarButton.getAccessibleContext().setAccessibleName(calendarButton.getText());
        calendarButton.addActionListener(e -> showCalendar());
        add(calendarButton, BorderLayout.EAST); calendarButton.setVisible(mode != PeriodMode.DATE);
        error.setForeground(UiTokens.danger()); error.setVisible(false); add(error, BorderLayout.SOUTH);
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { edited(); }
            public void removeUpdate(DocumentEvent e) { edited(); }
            public void changedUpdate(DocumentEvent e) { edited(); }
        };
        startInput.getDocument().addDocumentListener(listener); endInput.getDocument().addDocumentListener(listener);
    }
    private JPanel labeled(String name, JTextField input) {
        JPanel panel = new JPanel(new BorderLayout(0, UiTokens.space(1)));
        JLabel label = new JLabel(name); label.setLabelFor(input);
        input.getAccessibleContext().setAccessibleName(name);
        panel.add(label, BorderLayout.NORTH); panel.add(input); return panel;
    }
    private static String text(String key, String fallback) { return I18n.getText(PeriodField.class, key, fallback); }
    public PeriodMode getMode() { return mode; }
    public PeriodValue getValue() { return value; }
    public JTextField getStartInput() { return startInput; }
    public JTextField getEndInput() { return endInput; }
    public boolean isInputValid() { return inputValid; }
    public String getValidationMessage() { return inputValid ? "" : error.getText(); }
    public PeriodField setValue(PeriodValue value) { return setValue(value, true); }
    public PeriodField setValue(PeriodValue next, boolean fireEvent) {
        validate(next);
        boolean wasValid = inputValid;
        PeriodValue old = value; value = next;
        writeInputs(); updateValueError();
        if (!Objects.equals(old, next)) {
            getAccessibleContext().firePropertyChange(javax.accessibility.AccessibleContext.ACCESSIBLE_VALUE_PROPERTY, old, next);
            if (fireEvent) dispatchEvent(EventType.CHANGE, next);
        }
        if (wasValid != inputValid && fireEvent) dispatchEvent(EventType.INPUT, inputValid);
        return this;
    }
    public PeriodField clear() { return clear(true); }
    public PeriodField clear(boolean fireEvent) { return setValue(null, fireEvent); }
    public boolean isRequired() { return required; }
    public PeriodField setRequired(boolean required) {
        boolean old = this.required; this.required = required;
        cancelPopup(); edited(); firePropertyChange("required", old, required); return this;
    }
    public boolean isAllowOvernight() { return allowOvernight; }
    public PeriodField setAllowOvernight(boolean enabled) {
        if (!enabled && value instanceof TimeRange r && r.nextDay()) throw new IllegalStateException("existing value is overnight");
        allowOvernight = enabled; edited(); return this;
    }
    public Presentation getPresentation() { return presentation; }
    public PeriodField setPresentation(Presentation presentation) {
        Objects.requireNonNull(presentation);
        if (mode == PeriodMode.TIME && presentation == Presentation.CALENDAR) throw new IllegalArgumentException("TIME has no calendar");
        cancelPopup(); this.presentation = presentation;
        calendarButton.setVisible(mode != PeriodMode.DATE || presentation == Presentation.CALENDAR); revalidate(); return this;
    }
    public String getFormat() { return pattern; }
    public PeriodField setFormat(String pattern) {
        DateTimeFormatter next = makeFormatter(Objects.requireNonNull(pattern));
        // Reject patterns which cannot round-trip the current mode before modifying the field.
        Temporal sample = switch (mode) { case DATE -> LocalDate.of(2024, 2, 29); case DATE_TIME -> LocalDateTime.of(2024, 2, 29, 13, 15); case TIME -> LocalTime.of(13, 15); };
        parse(next.format(sample), next);
        this.pattern = pattern; formatter = next; writeInputs(); updateValueError(); return this;
    }
    private DateTimeFormatter makeFormatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern.replace("yyyy", "uuuu"), getLocale()).withResolverStyle(ResolverStyle.STRICT);
    }
    @Override public void setLocale(Locale locale) {
        super.setLocale(Objects.requireNonNull(locale));
        if (pattern != null) { formatter = makeFormatter(pattern); writeInputs(); updateValueError(); }
    }
    public PeriodField setLimits(Temporal minimum, Temporal maximum) {
        checkTemporal(minimum); checkTemporal(maximum);
        if (minimum != null && maximum != null && compare(minimum, maximum) > 0) throw new IllegalArgumentException("invalid limits");
        Temporal oldMin = this.minimum, oldMax = this.maximum;
        this.minimum = minimum; this.maximum = maximum;
        try { validate(value); } catch (RuntimeException e) { this.minimum = oldMin; this.maximum = oldMax; throw e; }
        cancelPopup(); edited(); return this;
    }
    public Temporal getMinimum() { return minimum; }
    public Temporal getMaximum() { return maximum; }
    private void checkTemporal(Temporal temporal) {
        if (temporal == null) return;
        boolean correct = switch (mode) { case DATE -> temporal instanceof LocalDate; case DATE_TIME -> temporal instanceof LocalDateTime; case TIME -> temporal instanceof LocalTime; };
        if (!correct) throw new IllegalArgumentException("limit type does not match " + mode);
    }
    private static int compare(Temporal a, Temporal b) {
        if (a instanceof LocalDate v) return v.compareTo((LocalDate) b);
        if (a instanceof LocalDateTime v) return v.compareTo((LocalDateTime) b);
        return ((LocalTime) a).compareTo((LocalTime) b);
    }
    private Temporal start(PeriodValue v) {
        return switch (v) { case DateRange r -> r.start(); case DateTimeRange r -> r.start(); case TimeRange r -> r.start(); };
    }
    private Temporal end(PeriodValue v) {
        return switch (v) { case DateRange r -> r.end(); case DateTimeRange r -> r.end(); case TimeRange r -> r.end(); };
    }
    private void validate(PeriodValue v) {
        if (v == null) return;
        Temporal a = start(v), b = end(v); checkTemporal(a); checkTemporal(b);
        if (v instanceof TimeRange r && r.nextDay() && !allowOvernight) throw new IllegalArgumentException("overnight periods disabled");
        if ((minimum != null && (compare(a, minimum) < 0 || compare(b, minimum) < 0)) ||
            (maximum != null && (compare(a, maximum) > 0 || compare(b, maximum) > 0))) throw new IllegalArgumentException("period outside limits");
    }
    private Temporal parse(String text, DateTimeFormatter format) {
        return switch (mode) { case DATE -> LocalDate.parse(text, format); case DATE_TIME -> LocalDateTime.parse(text, format); case TIME -> LocalTime.parse(text, format); };
    }
    private PeriodValue range(Temporal a, Temporal b) {
        return switch (mode) {
            case DATE -> new DateRange((LocalDate) a, (LocalDate) b);
            case DATE_TIME -> new DateTimeRange((LocalDateTime) a, (LocalDateTime) b);
            case TIME -> new TimeRange((LocalTime) a, (LocalTime) b, ((LocalTime) b).isBefore((LocalTime) a));
        };
    }
    private void edited() {
        if (updating) return;
        try {
            String a = startInput.getText().strip(), b = endInput.getText().strip();
            PeriodValue next = a.isEmpty() && b.isEmpty() ? null : range(parse(a, formatter), parse(b, formatter));
            validate(next);
            PeriodValue old = value; value = next; updateValueError();
            if (!Objects.equals(old, next)) {
                getAccessibleContext().firePropertyChange(javax.accessibility.AccessibleContext.ACCESSIBLE_VALUE_PROPERTY, old, next);
                dispatchEvent(EventType.CHANGE, next);
            }
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            setError(text("invalid", "Informe um período válido dentro dos limites permitidos"));
        }
        dispatchEvent(EventType.INPUT, inputValid);
    }
    private void writeInputs() {
        updating = true;
        try {
            String a = value == null ? "" : formatter.format(start(value)), b = value == null ? "" : formatter.format(end(value));
            if (!startInput.getText().equals(a)) startInput.setText(a);
            if (!endInput.getText().equals(b)) endInput.setText(b);
        } finally { updating = false; }
    }
    private void updateValueError() { setError(required && value == null ? text("required", "Selecione o período obrigatório") : null); }
    private void setError(String message) {
        boolean old = inputValid; inputValid = message == null;
        error.setText(message == null ? "" : message); error.setVisible(!inputValid);
        for (JTextField input : new JTextField[]{startInput, endInput}) {
            input.putClientProperty("JComponent.outline", inputValid ? null : "error");
            input.getAccessibleContext().setAccessibleDescription(message);
        }
        if (old != inputValid) getAccessibleContext().firePropertyChange(javax.accessibility.AccessibleContext.ACCESSIBLE_STATE_PROPERTY, old, inputValid);
    }
    public boolean isEditable() { return editable; }
    public PeriodField setEditable(boolean editable) {
        this.editable = editable; startInput.setEditable(editable); endInput.setEditable(editable);
        calendarButton.setEnabled(isEnabled() && editable); if (!editable) cancelPopup(); return this;
    }
    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (startInput != null) { startInput.setEnabled(enabled); endInput.setEnabled(enabled); calendarButton.setEnabled(enabled && editable); }
        if (!enabled) cancelPopup();
    }
    public void showCalendar() {
        if (!isEnabled() || !editable) return;
        cancelPopup();
        RangeCalendarPanel calendar = new RangeCalendarPanel(getLocale());
        DateRange dates = value instanceof DateRange r ? r : value instanceof DateTimeRange r ? new DateRange(r.start().toLocalDate(), r.end().toLocalDate()) : null;
        calendar.setSelection(dates);
        if (mode != PeriodMode.TIME) calendar.setLimits(datePart(minimum), datePart(maximum));
        ClockPickerPanel startTime = new ClockPickerPanel(getLocale()).setRequired(required), endTime = new ClockPickerPanel(getLocale()).setRequired(required);
        startTime.setTime(value instanceof DateTimeRange r ? r.start().toLocalTime() : value instanceof TimeRange r ? r.start() : null);
        endTime.setTime(value instanceof DateTimeRange r ? r.end().toLocalTime() : value instanceof TimeRange r ? r.end() : null);
        startTime.getAccessibleContext().setAccessibleName(text("startTime", "Hora inicial"));
        endTime.getAccessibleContext().setAccessibleName(text("endTime", "Hora final"));
        JPanel content = new JPanel(new BorderLayout(0, UiTokens.space(2)));
        JTabbedPane tabs = mode == PeriodMode.DATE_TIME ? new JTabbedPane() : null;
        content.setBorder(BorderFactory.createEmptyBorder(UiTokens.space(2), UiTokens.space(2), UiTokens.space(2), UiTokens.space(2)));
        if (mode == PeriodMode.DATE) content.add(calendar);
        else {
            JPanel times = new JPanel(new GridLayout(1, 2, UiTokens.space(2), 0));
            times.add(labeledClock(text("startTime", "Hora inicial"), startTime)); times.add(labeledClock(text("endTime", "Hora final"), endTime));
            if (mode == PeriodMode.TIME) content.add(times);
            else {
                tabs.addTab(text("dates", "Datas"), calendar); tabs.addTab(text("times", "Horários"), times);
                content.add(tabs);
            }
        }
        JPanel footer = new JPanel(new BorderLayout());
        JLabel popupError = new JLabel(); popupError.setForeground(UiTokens.danger()); footer.add(popupError, BorderLayout.CENTER);
        JButton apply = new JButton(text("apply", "Aplicar")), cancel = new JButton(text("cancel", "Cancelar"));
        apply.setName("period.apply"); cancel.setName("period.cancel");
        JPanel actions = new JPanel(); actions.add(apply); actions.add(cancel); footer.add(actions, BorderLayout.SOUTH); content.add(footer, BorderLayout.SOUTH);
        JPopupMenu menu = new JPopupMenu(); popup = menu; menu.add(content);
        apply.addActionListener(e -> {
            try {
                if (mode != PeriodMode.DATE && (!startTime.isSelectionValid() || !endTime.isSelectionValid())) {
                    if (tabs != null) tabs.setSelectedIndex(1);
                    ClockPickerPanel incomplete = !startTime.isSelectionValid() ? startTime : endTime;
                    incomplete.focusIncompleteSelection();
                    popupError.setText(text("requiredTime", "Selecione as horas e os minutos de início e fim"));
                    popupError.getAccessibleContext().setAccessibleDescription(popupError.getText());
                    return;
                }
                LocalTime a = Objects.requireNonNullElse(startTime.getTime(), LocalTime.MIDNIGHT);
                LocalTime b = Objects.requireNonNullElse(endTime.getTime(), LocalTime.MIDNIGHT);
                PeriodValue next;
                if (mode == PeriodMode.TIME) next = new TimeRange(a, b, b.isBefore(a));
                else {
                    DateRange selected = calendar.getSelection(); if (selected == null) throw new IllegalArgumentException("incomplete selection");
                    next = mode == PeriodMode.DATE ? selected : new DateTimeRange(selected.start().atTime(a), selected.end().atTime(b));
                }
                setValue(next); cancelPopup();
            } catch (IllegalArgumentException | java.time.DateTimeException ex) { popupError.setText(text("invalid", "Informe um período válido dentro dos limites permitidos")); }
        });
        cancel.addActionListener(e -> cancelPopup());
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        content.getActionMap().put("cancel", new AbstractAction() { public void actionPerformed(ActionEvent e) { cancelPopup(); } });
        menu.show(this, 0, getHeight());
    }
    /** Opens the clock draft; DATE_TIME also offers a dates tab. */
    public void showTimePicker() { if (mode != PeriodMode.DATE) showCalendar(); }
    private JPanel labeledClock(String name, ClockPickerPanel clock) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(name, SwingConstants.CENTER), BorderLayout.NORTH); panel.add(clock);
        return panel;
    }
    private LocalDate datePart(Temporal temporal) {
        return temporal == null ? null : temporal instanceof LocalDate d ? d : ((LocalDateTime) temporal).toLocalDate();
    }
    private void cancelPopup() { if (popup != null) { popup.setVisible(false); popup = null; } }
    @Override public void removeNotify() { cancelPopup(); super.removeNotify(); }
}
