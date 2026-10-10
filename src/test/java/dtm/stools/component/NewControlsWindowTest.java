package dtm.stools.component;

import dtm.stools.component.command.*;
import dtm.stools.component.inputfields.periodfield.*;
import dtm.stools.component.panels.editor.word.provider.*;
import dtm.stools.component.panels.editor.sheet.provider.*;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class NewControlsWindowTest {
    @Test
    void paletteClosesOnceAndCanReopenAndOwnerDisposesIt() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        CommandPalette[] palette = new CommandPalette[1]; JFrame[] owner = new JFrame[1]; AtomicInteger closed = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            owner[0] = new JFrame();
            palette[0] = new CommandPalette(() -> List.of(new CommandEntry("id", "Name", "", "", "", true)), id -> true).onClosed(closed::incrementAndGet);
            try {
                palette[0].open(owner[0]); assertTrue(palette[0].isOpen());
                palette[0].close(); palette[0].close(); assertEquals(1, closed.get());
                palette[0].open(owner[0]); assertTrue(palette[0].isOpen()); owner[0].dispose();
            } catch (Throwable failure) { palette[0].close(); owner[0].dispose(); throw failure; }
        });
        SwingUtilities.invokeAndWait(() -> { assertFalse(palette[0].isOpen()); assertEquals(2, closed.get()); });
    }
    @Test
    void editorProvidersAdaptCatalogsAndPreserveHandles() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            JFrame owner = new JFrame(); AtomicInteger closed = new AtomicInteger();
            WordPopupHandle word = null; SheetPopupHandle sheet = null;
            try {
                WordCommandPaletteContext wordContext = new WordCommandPaletteContext() {
                    public Component owner() { return owner; }
                    public Locale locale() { return Locale.US; }
                    public List<WordCommandEntry> commands() { return List.of(new WordCommandEntry("w", "Word", "Group", true)); }
                    public boolean execute(String id) { return true; }
                    public void closed() { closed.incrementAndGet(); }
                };
                var wp = new dtm.stools.component.panels.editor.word.ui.popup.DefaultCommandPaletteProvider();
                assertEquals("word.popup.palette.default", wp.id()); word = wp.show(wordContext);
                assertTrue(word.isOpen()); word.toFront(); word.close(); word.close(); assertEquals(1, closed.get());
                SheetCommandPaletteContext sheetContext = new SheetCommandPaletteContext() {
                    public Component owner() { return owner; }
                    public Locale locale() { return Locale.US; }
                    public List<SheetCommandEntry> commands() { return List.of(new SheetCommandEntry("s", "Sheet", "Group", true, "Ctrl+S")); }
                    public boolean execute(String id) { return true; }
                };
                var sp = new dtm.stools.component.panels.editor.sheet.ui.popup.DefaultCommandPaletteProvider();
                assertEquals("sheet.popup.palette.default", sp.id()); sheet = sp.open(sheetContext);
                assertTrue(sheet.isOpen()); sheet.toFront(); sheet.close(); assertFalse(sheet.isOpen());
            } finally { if (word != null) word.close(); if (sheet != null) sheet.close(); owner.dispose(); }
        });
    }
    @Test
    void calendarPopupOnlyCommitsOnApplyAndCancelPreservesValue() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            JFrame owner = new JFrame(); PeriodField field = new PeriodField(PeriodMode.DATE_TIME).setPresentation(PeriodField.Presentation.CALENDAR);
            DateTimeRange original = new DateTimeRange(LocalDateTime.of(2024, 2, 28, 8, 0), LocalDateTime.of(2024, 3, 1, 18, 0)); field.setValue(original);
            owner.add(field); owner.pack(); owner.setVisible(true);
            try {
                field.showCalendar(); JPopupMenu popup = activePopup();
                RangeCalendarPanel calendar = NewControlsTest.descendants(popup).stream().filter(RangeCalendarPanel.class::isInstance).map(RangeCalendarPanel.class::cast).findFirst().orElseThrow();
                calendar.selectDate(LocalDate.of(2024, 2, 29)); calendar.selectDate(LocalDate.of(2024, 3, 2));
                assertEquals(original, field.getValue());
                button(popup, "period.cancel").doClick(); assertEquals(original, field.getValue());
                field.showCalendar(); popup = activePopup();
                calendar = NewControlsTest.descendants(popup).stream().filter(RangeCalendarPanel.class::isInstance).map(RangeCalendarPanel.class::cast).findFirst().orElseThrow();
                calendar.selectDate(LocalDate.of(2024, 2, 29)); calendar.selectDate(LocalDate.of(2024, 3, 2));
                List<ClockPickerPanel> clocks = NewControlsTest.descendants(popup).stream().filter(ClockPickerPanel.class::isInstance).map(ClockPickerPanel.class::cast).toList();
                assertEquals(2, clocks.size()); clocks.get(0).setTime(LocalTime.of(9, 13)); clocks.get(1).setTime(LocalTime.of(17, 42));
                assertEquals(original, field.getValue());
                button(popup, "period.apply").doClick();
                assertEquals(new DateTimeRange(LocalDateTime.of(2024, 2, 29, 9, 13), LocalDateTime.of(2024, 3, 2, 17, 42)), field.getValue());
                field.showCalendar(); field.setEditable(false); assertEquals(0, MenuSelectionManager.defaultManager().getSelectedPath().length);
            } finally { owner.dispose(); MenuSelectionManager.defaultManager().clearSelectedPath(); }
        });
    }
    @Test
    void timeClockDraftRequiresApplyAndValidatesOvernightAndLimits() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            JFrame owner = new JFrame(); PeriodField field = new PeriodField(PeriodMode.TIME);
            TimeRange original = new TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0), false); field.setValue(original);
            owner.add(field); owner.pack(); owner.setVisible(true);
            try {
                field.showTimePicker(); JPopupMenu popup = activePopup();
                List<ClockPickerPanel> clocks = NewControlsTest.descendants(popup).stream().filter(ClockPickerPanel.class::isInstance).map(ClockPickerPanel.class::cast).toList();
                clocks.get(0).setTime(LocalTime.of(22, 0)); clocks.get(1).setTime(LocalTime.of(2, 0));
                button(popup, "period.apply").doClick(); assertEquals(original, field.getValue()); assertTrue(popup.isVisible());
                button(popup, "period.cancel").doClick(); assertEquals(original, field.getValue());
                field.setAllowOvernight(true); field.showTimePicker(); popup = activePopup();
                clocks = NewControlsTest.descendants(popup).stream().filter(ClockPickerPanel.class::isInstance).map(ClockPickerPanel.class::cast).toList();
                clocks.get(0).setTime(LocalTime.of(22, 17)); clocks.get(1).setTime(LocalTime.of(2, 43));
                button(popup, "period.apply").doClick(); assertEquals(new TimeRange(LocalTime.of(22, 17), LocalTime.of(2, 43), true), field.getValue());
                field.clear().setAllowOvernight(false).setLimits(LocalTime.of(9, 0), LocalTime.of(17, 0));
                field.showTimePicker(); popup = activePopup();
                button(popup, "period.apply").doClick(); assertNull(field.getValue()); assertTrue(popup.isVisible());
                field.setEnabled(false); assertEquals(0, MenuSelectionManager.defaultManager().getSelectedPath().length);
            } finally { owner.dispose(); MenuSelectionManager.defaultManager().clearSelectedPath(); }
        });
    }
    private static JPopupMenu activePopup() {
        for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) if (element instanceof JPopupMenu popup) return popup;
        throw new AssertionError("No active popup");
    }
    @Test
    void requiredTimeAndDateTimeCannotApplyMissingHoursOrMinutes() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            for (PeriodMode mode : List.of(PeriodMode.TIME, PeriodMode.DATE_TIME)) {
                JFrame owner = new JFrame(); PeriodField field = new PeriodField(mode).setRequired(true);
                owner.add(field); owner.pack(); owner.setVisible(true);
                try {
                    field.showTimePicker(); JPopupMenu popup = activePopup();
                    if (mode == PeriodMode.DATE_TIME) {
                        RangeCalendarPanel calendar = NewControlsTest.descendants(popup).stream().filter(RangeCalendarPanel.class::isInstance).map(RangeCalendarPanel.class::cast).findFirst().orElseThrow();
                        calendar.selectDate(LocalDate.of(2024, 2, 29)); calendar.selectDate(LocalDate.of(2024, 2, 29));
                    }
                    List<ClockPickerPanel> clocks = NewControlsTest.descendants(popup).stream().filter(ClockPickerPanel.class::isInstance).map(ClockPickerPanel.class::cast).toList();
                    assertTrue(clocks.stream().allMatch(c -> c.getTime() == null));
                    JTabbedPane tabs = NewControlsTest.descendants(popup).stream().filter(JTabbedPane.class::isInstance).map(JTabbedPane.class::cast).findFirst().orElse(null);
                    if (tabs != null) assertEquals(0, tabs.getSelectedIndex());
                    button(popup, "period.apply").doClick(); assertNull(field.getValue()); assertTrue(popup.isVisible());
                    if (tabs != null) assertEquals(1, tabs.getSelectedIndex());
                    assertTrue(clocks.get(0).isSelectingHours());
                    for (ClockPickerPanel clock : clocks) clockButton(clock, 0).doClick();
                    if (tabs != null) tabs.setSelectedIndex(0);
                    clocks.get(0).setSelectingHours(true);
                    button(popup, "period.apply").doClick(); assertNull(field.getValue()); assertTrue(popup.isVisible());
                    if (tabs != null) assertEquals(1, tabs.getSelectedIndex());
                    assertFalse(clocks.get(0).isSelectingHours());
                    clockButton(clocks.get(0), 0).doClick();
                    clocks.get(1).setSelectingHours(true);
                    button(popup, "period.apply").doClick(); assertNull(field.getValue()); assertTrue(popup.isVisible());
                    assertFalse(clocks.get(1).isSelectingHours());
                    clockButton(clocks.get(1), 0).doClick(); button(popup, "period.apply").doClick();
                    assertFalse(popup.isVisible()); assertTrue(field.isInputValid());
                    if (mode == PeriodMode.TIME) assertEquals(new TimeRange(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, false), field.getValue());
                    else assertEquals(new DateTimeRange(LocalDateTime.of(2024, 2, 29, 0, 0), LocalDateTime.of(2024, 2, 29, 0, 0)), field.getValue());
                    field.clear(); field.showTimePicker(); popup = activePopup(); button(popup, "period.cancel").doClick(); assertNull(field.getValue()); assertFalse(field.isInputValid());
                } finally { owner.dispose(); MenuSelectionManager.defaultManager().clearSelectedPath(); }
            }
        });
    }
    private static JToggleButton clockButton(ClockPickerPanel clock, int value) {
        return NewControlsTest.descendants(clock).stream().filter(JToggleButton.class::isInstance).map(JToggleButton.class::cast)
                .filter(b -> java.util.Objects.equals(value, b.getClientProperty("clock.value"))).findFirst().orElseThrow();
    }
    private static JButton button(JPopupMenu popup, String name) {
        return NewControlsTest.descendants(popup).stream().filter(JButton.class::isInstance).map(JButton.class::cast).filter(b -> name.equals(b.getName())).findFirst().orElseThrow();
    }
}
