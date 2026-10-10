package dtm.stools.component.inputfields.periodfield;

import dtm.stools.configs.UiTokens;
import dtm.stools.i18n.I18n;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;

/** Two consecutive months. Selection is a draft until the owning field applies it. */
public class RangeCalendarPanel extends JPanel {
    private YearMonth month = YearMonth.now();
    private LocalDate start, end, minimum, maximum;
    private final JPanel months = new JPanel(new GridLayout(1, 2, UiTokens.space(3), 0));

    public RangeCalendarPanel(Locale locale) {
        super(new BorderLayout(0, UiTokens.space(2)));
        setLocale(locale);
        JButton previous = new JButton("‹"), next = new JButton("›");
        previous.getAccessibleContext().setAccessibleName(text("previous", "Mês anterior"));
        next.getAccessibleContext().setAccessibleName(text("next", "Próximo mês"));
        previous.addActionListener(e -> showMonth(month.minusMonths(1)));
        next.addActionListener(e -> showMonth(month.plusMonths(1)));
        JPanel navigation = new JPanel(new BorderLayout());
        navigation.add(previous, BorderLayout.WEST); navigation.add(next, BorderLayout.EAST);
        add(navigation, BorderLayout.NORTH); add(months, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName(text("calendar", "Calendário de intervalo"));
        rebuild();
    }

    private String text(String key, String fallback) { return I18n.getText(PeriodField.class, key, fallback); }
    public DateRange getSelection() { return start != null && end != null ? new DateRange(start, end) : null; }
    public void setSelection(DateRange range) {
        start = range == null ? null : range.start(); end = range == null ? null : range.end();
        if (start != null) month = YearMonth.from(start);
        rebuild();
    }
    public void setLimits(LocalDate minimum, LocalDate maximum) {
        if (minimum != null && maximum != null && maximum.isBefore(minimum)) throw new IllegalArgumentException("invalid limits");
        this.minimum = minimum; this.maximum = maximum;
        if (minimum != null && month.plusMonths(1).atEndOfMonth().isBefore(minimum)) month = YearMonth.from(minimum);
        if (maximum != null && month.atDay(1).isAfter(maximum)) month = YearMonth.from(maximum);
        rebuild();
    }
    public void showMonth(YearMonth value) { month = java.util.Objects.requireNonNull(value); rebuild(); }
    public YearMonth getDisplayedMonth() { return month; }
    public void selectDate(LocalDate date) {
        java.util.Objects.requireNonNull(date);
        if (!allowed(date)) throw new IllegalArgumentException("date outside limits");
        DateRange old = getSelection();
        if (start == null || end != null) { start = date; end = null; }
        else if (date.isBefore(start)) { end = start; start = date; }
        else end = date;
        rebuild();
        getAccessibleContext().firePropertyChange(javax.accessibility.AccessibleContext.ACCESSIBLE_SELECTION_PROPERTY, old, getSelection());
        firePropertyChange("selection", old, getSelection());
        focusDate(date);
    }
    private boolean allowed(LocalDate date) {
        return (minimum == null || !date.isBefore(minimum)) && (maximum == null || !date.isAfter(maximum));
    }
    private void rebuild() {
        months.removeAll();
        months.add(buildMonth(month)); months.add(buildMonth(month.plusMonths(1)));
        revalidate(); repaint();
    }
    private JPanel buildMonth(YearMonth value) {
        JPanel panel = new JPanel(new BorderLayout(0, UiTokens.space(1)));
        JLabel title = new JLabel(value.format(DateTimeFormatter.ofPattern("MMMM uuuu", getLocale())), SwingConstants.CENTER);
        title.setFont(UiTokens.font().deriveFont(Font.BOLD)); panel.add(title, BorderLayout.NORTH);
        JPanel days = new JPanel(new GridLayout(7, 7, UiTokens.scale(2), UiTokens.scale(2)));
        DayOfWeek first = WeekFields.of(getLocale()).getFirstDayOfWeek();
        for (int i = 0; i < 7; i++) {
            DayOfWeek day = DayOfWeek.of((first.getValue() - 1 + i) % 7 + 1);
            days.add(new JLabel(day.getDisplayName(TextStyle.SHORT_STANDALONE, getLocale()), SwingConstants.CENTER));
        }
        int offset = Math.floorMod(value.atDay(1).getDayOfWeek().getValue() - first.getValue(), 7);
        for (int i = 0; i < 42; i++) {
            int number = i - offset + 1;
            if (number < 1 || number > value.lengthOfMonth()) { days.add(new JLabel()); continue; }
            LocalDate date = value.atDay(number);
            JToggleButton button = new JToggleButton(String.valueOf(number));
            button.putClientProperty("period.date", date);
            button.setMargin(new Insets(2, 2, 2, 2));
            button.setPreferredSize(new Dimension(UiTokens.scale(34), UiTokens.scale(30)));
            button.setSelected(date.equals(start) || date.equals(end));
            button.setEnabled(allowed(date));
            button.getAccessibleContext().setAccessibleName(date.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL).withLocale(getLocale())));
            if (button.isSelected()) { button.setBackground(UiTokens.accent()); button.setForeground(Color.WHITE); }
            button.addActionListener(e -> selectDate(date));
            bind(button, "LEFT", () -> moveFocus(date.minusDays(1)));
            bind(button, "RIGHT", () -> moveFocus(date.plusDays(1)));
            bind(button, "UP", () -> moveFocus(date.minusWeeks(1)));
            bind(button, "DOWN", () -> moveFocus(date.plusWeeks(1)));
            bind(button, "PAGE_UP", () -> moveFocus(date.minusMonths(1)));
            bind(button, "PAGE_DOWN", () -> moveFocus(date.plusMonths(1)));
            bind(button, "HOME", () -> moveFocus(date.minusDays(Math.floorMod(date.getDayOfWeek().getValue() - first.getValue(), 7))));
            bind(button, "END", () -> moveFocus(date.plusDays(6 - Math.floorMod(date.getDayOfWeek().getValue() - first.getValue(), 7))));
            bind(button, "ENTER", button::doClick);
            days.add(button);
        }
        panel.add(days); return panel;
    }
    private static void bind(JComponent target, String stroke, Runnable action) {
        target.getInputMap().put(KeyStroke.getKeyStroke(stroke), stroke);
        target.getActionMap().put(stroke, new AbstractAction() { public void actionPerformed(ActionEvent e) { action.run(); } });
    }
    private void moveFocus(LocalDate date) {
        if (!allowed(date)) return;
        YearMonth target = YearMonth.from(date);
        if (!target.equals(month) && !target.equals(month.plusMonths(1))) showMonth(target);
        focusDate(date);
    }
    private void focusDate(LocalDate date) { focusDate(months, date); }
    private void focusDate(Container parent, LocalDate date) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JComponent c && date.equals(c.getClientProperty("period.date"))) { c.requestFocusInWindow(); return; }
            if (child instanceof Container c) focusDate(c, date);
        }
    }
}
