package dtm.stools.component;

import dtm.stools.component.command.*;
import dtm.stools.component.events.EventType;
import dtm.stools.component.form.*;
import dtm.stools.component.inputfields.passwordfield.PasswordField;
import dtm.stools.component.inputfields.periodfield.*;
import dtm.stools.component.inputfields.checkfield.*;
import dtm.stools.component.inputfields.switchfield.SwitchField;
import dtm.stools.component.inputfields.sliderfield.SliderField;
import dtm.stools.component.inputfields.ratingfield.RatingField;
import dtm.stools.component.inputfields.segmentedfield.SegmentedField;
import dtm.stools.component.inputfields.stepperfield.StepperField;
import dtm.stools.component.inputfields.pinfield.PinField;
import dtm.stools.component.inputfields.textfield.JTextFieldListener;
import dtm.stools.component.inputfields.duallistfield.DualListField;
import dtm.stools.component.inputfields.colorpicker.ColorPickerField;
import dtm.stools.component.panels.datefield.DatePickerInputField;
import org.junit.jupiter.api.Test;
import javax.accessibility.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class NewControlsTest {
    private void edt(Runnable run) throws Exception { SwingUtilities.invokeAndWait(run); }

    @Test void passwordEyeIsInsideInputAndAccessibleWithoutCoveringText() throws Exception {
        edt(() -> {
            PasswordField field = new PasswordField().setPassword("secret".toCharArray());
            JPasswordField input = field.getPasswordField(); input.setSize(260, 36); input.doLayout();
            JButton eye = descendants(input).stream().filter(JButton.class::isInstance).map(JButton.class::cast).findFirst().orElseThrow();
            assertSame(input, eye.getParent()); assertEquals("", eye.getText()); assertNotNull(eye.getIcon());
            assertFalse(eye.isBorderPainted()); assertFalse(eye.isFocusPainted());
            assertTrue(eye.getX() >= input.getWidth() - input.getInsets().right);
            assertTrue(eye.getX() + eye.getWidth() <= input.getWidth());
            input.select(1, 4); eye.doClick(); assertTrue(field.isPasswordVisible());
            assertEquals(1, input.getSelectionStart()); assertEquals(4, input.getSelectionEnd());
            assertEquals(AccessibleRole.PUSH_BUTTON, eye.getAccessibleContext().getAccessibleRole());
            input.getActionMap().get("password.toggleVisibility").actionPerformed(null); assertFalse(field.isPasswordVisible());
            field.setEditable(false); eye.doClick(); assertFalse(field.isPasswordVisible());
            field.setEnabled(false); assertFalse(eye.isEnabled());
        });
    }

    @Test void clockSelects24HourTimeAndExactMinutesWithKeyboard() throws Exception {
        edt(() -> {
            ClockPickerPanel clock = new ClockPickerPanel(Locale.US); clock.setTime(LocalTime.of(8, 17));
            clockButton(clock, 23).doClick(); assertEquals(LocalTime.of(23, 17), clock.getTime()); assertFalse(clock.isSelectingHours());
            clockButton(clock, 55).doClick(); assertEquals(LocalTime.of(23, 55), clock.getTime());
            clockButton(clock, 55).getActionMap().get("RIGHT").actionPerformed(null); assertEquals(LocalTime.of(23, 56), clock.getTime());
            clock.setTime(LocalTime.of(23, 59));
            clockButton(clock, 55).getActionMap().get("RIGHT").actionPerformed(null); assertEquals(LocalTime.MIDNIGHT, clock.getTime());
            clock.setSelectingHours(true); clockButton(clock, 0).doClick(); assertEquals(LocalTime.MIDNIGHT, clock.getTime());
            assertTrue(clockButton(clock, 5).getAccessibleContext().getAccessibleName().contains("5"));
            clock.setEnabled(false); clockButton(clock, 5).doClick(); assertEquals(LocalTime.MIDNIGHT, clock.getTime());
            clock.setEnabled(true); clockButton(clock, 5).doClick(); assertEquals(LocalTime.of(0, 5), clock.getTime());
        });
    }

    private static JToggleButton clockButton(ClockPickerPanel clock, int value) {
        return descendants(clock).stream().filter(JToggleButton.class::isInstance).map(JToggleButton.class::cast)
                .filter(b -> Objects.equals(value, b.getClientProperty("clock.value"))).findFirst().orElseThrow();
    }

    @Test void clockPointerSelectsEveryMinuteIncludingOverNumberButtonsAndDrag() throws Exception {
        edt(() -> {
            ClockPickerPanel clock = new ClockPickerPanel(Locale.US); clock.setTime(LocalTime.of(8, 0)); clock.setSelectingHours(false);
            JComponent dial = descendants(clock).stream().filter(JComponent.class::isInstance).map(JComponent.class::cast)
                    .filter(c -> "clock.dial".equals(c.getName())).findFirst().orElseThrow();
            dial.setSize(232, 232);
            for (int minute = 0; minute < 60; minute++) {
                dial.doLayout(); Point point = minutePoint(minute);
                Component target = SwingUtilities.getDeepestComponentAt(dial, point.x, point.y);
                Point local = SwingUtilities.convertPoint(dial, point, target);
                target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, 1, MouseEvent.BUTTON1_DOWN_MASK, local.x, local.y, 1, false, MouseEvent.BUTTON1));
                target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, 2, 0, local.x, local.y, 1, false, MouseEvent.BUTTON1));
                assertEquals(LocalTime.of(8, minute), clock.getTime(), "Pointer minute " + minute);
            }
            Point point = minutePoint(37);
            dial.dispatchEvent(new MouseEvent(dial, MouseEvent.MOUSE_DRAGGED, 3, MouseEvent.BUTTON1_DOWN_MASK, point.x, point.y, 0, false, MouseEvent.NOBUTTON));
            assertEquals(LocalTime.of(8, 37), clock.getTime());
            clock.setEnabled(false); point = minutePoint(17);
            dial.dispatchEvent(new MouseEvent(dial, MouseEvent.MOUSE_PRESSED, 4, MouseEvent.BUTTON1_DOWN_MASK, point.x, point.y, 1, false, MouseEvent.BUTTON1));
            assertEquals(LocalTime.of(8, 37), clock.getTime());
        });
    }
    private static Point minutePoint(int minute) {
        double angle = minute * Math.PI / 30;
        return new Point((int) Math.round(116 + Math.sin(angle) * 94), (int) Math.round(116 - Math.cos(angle) * 94));
    }

    @Test void requiredClockDistinguishesMissingChoicesFromExplicitMidnight() throws Exception {
        edt(() -> {
            ClockPickerPanel clock = new ClockPickerPanel(Locale.US).setRequired(true);
            assertNull(clock.getTime()); assertFalse(clock.isSelectionValid());
            clock.setSelectingHours(false); assertFalse(clock.isSelectionValid()); clock.setSelectingHours(true);
            clockButton(clock, 0).doClick(); assertTrue(clock.isHourSelected()); assertFalse(clock.isMinuteSelected()); assertFalse(clock.isSelectionValid());
            clockButton(clock, 0).doClick(); assertTrue(clock.isSelectionValid()); assertEquals(LocalTime.MIDNIGHT, clock.getTime());
            List<Object> values = new ArrayList<>(); clock.addPropertyChangeListener("time", e -> values.add(e.getNewValue()));
            clock.setTime(null); assertFalse(clock.isSelectionValid()); assertNull(clock.getTime());
            clock.setTime(LocalTime.MIDNIGHT); assertTrue(clock.isSelectionValid());
            assertEquals(2, values.size()); assertNull(values.get(0)); assertEquals(LocalTime.MIDNIGHT, values.get(1));
        });
    }

    @Test void requiredPeriodAndFormStayInvalidWhenClearedOrReformatted() throws Exception {
        edt(() -> {
            PeriodField period = new PeriodField(PeriodMode.TIME);
            FormField wrapper = new FormField("time", "Time", period).setRequired(true);
            assertTrue(period.isRequired()); assertFalse(period.isInputValid()); assertFalse(wrapper.validateField().valid());
            period.setFormat("HH:mm"); period.setLocale(Locale.US); assertFalse(period.isInputValid());
            period.setValue(new TimeRange(LocalTime.MIDNIGHT, LocalTime.NOON, false)); assertTrue(period.isInputValid()); assertTrue(wrapper.validateField().valid());
            period.clear(); assertFalse(period.isInputValid()); assertNull(period.getValue());
            wrapper.setRequired(false); assertFalse(period.isRequired()); assertTrue(period.isInputValid()); assertTrue(wrapper.validateField().valid());
        });
    }

    @Test void passwordIsHiddenCopiesArraysAndPreservesSelection() throws Exception {
        edt(() -> {
            PasswordField field = new PasswordField(); char[] source = "test-secret".toCharArray();
            assertFalse(field.isPasswordVisible());
            assertNotEquals(0, field.getPasswordField().getEchoChar());
            field.setPassword(source); source[0] = 'x';
            char[] copy = field.getPassword(); copy[0] = 'y';
            assertArrayEquals("test-secret".toCharArray(), field.getPassword());
            field.getPasswordField().select(2, 7); field.setPasswordVisible(true);
            assertEquals(0, field.getPasswordField().getEchoChar());
            assertEquals(2, field.getPasswordField().getSelectionStart()); assertEquals(7, field.getPasswordField().getSelectionEnd());
            field.setPasswordVisible(false); assertNotEquals(0, field.getPasswordField().getEchoChar());
            assertEquals(AccessibleRole.PASSWORD_TEXT, field.getPasswordField().getAccessibleContext().getAccessibleRole());
        });
    }
    @Test void passwordEventsAndValidationNeverContainTheSecret() throws Exception {
        edt(() -> {
            PasswordField field = new PasswordField(); AtomicInteger changes = new AtomicInteger();
            field.addEventListener(EventType.CHANGE, e -> { assertNull(e.getValue()); assertTrue(e.getProperties().isEmpty()); changes.incrementAndGet(); });
            field.setPassword("secret".toCharArray()); assertEquals(1, changes.get());
            field.setPassword("other".toCharArray(), false); assertEquals(1, changes.get());
            FormPanel form = new FormPanel(); FormField wrapper = new FormField("password", "Password", field).setRequired(true);
            form.addField(wrapper);
            wrapper.addEventListener(FormField.INVALID, e -> assertNull(e.getValue()));
            form.addEventListener(EventType.VALIDATE, e -> assertFalse(((Map<?, ?>) e.getProperties().get("values")).containsKey("password")));
            assertTrue(form.isFormValid());
            assertArrayEquals("other".toCharArray(), (char[]) form.getValues().get("password"));
            field.clear(false); assertFalse(form.submit());
            form.setValues(Map.of("password", "new".toCharArray()));
            form.addEventListener(EventType.SUBMIT, e -> assertArrayEquals("new".toCharArray(), (char[]) ((Map<?, ?>) e.getValue()).get("password")));
            assertTrue(form.submit()); form.reset(); assertEquals(0, field.getPassword().length);
        });
    }
    @Test void passwordValidatorTemporaryArrayIsErasedEvenWhenItThrows() throws Exception {
        edt(() -> {
            PasswordField field = new PasswordField().setPassword("secret".toCharArray());
            char[][] retained = new char[1][];
            FormField wrapper = new FormField("p", "P", field).setValidator((char[] value) -> { retained[0] = value; throw new IllegalStateException(); });
            assertThrows(IllegalStateException.class, wrapper::validateField);
            assertArrayEquals(new char[6], retained[0]);
            assertArrayEquals("secret".toCharArray(), field.getPassword());
        });
    }
    @Test void periodSupportsThreeModesLimitsAndSilentChanges() throws Exception {
        edt(() -> {
            PeriodField dates = new PeriodField(PeriodMode.DATE);
            AtomicInteger changes = new AtomicInteger(); dates.addEventListener(EventType.CHANGE, e -> changes.incrementAndGet());
            DateRange range = new DateRange(LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 2));
            dates.setLimits(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 31));
            dates.setValue(range, false); assertEquals(0, changes.get()); assertEquals(range, dates.getValue());
            assertEquals("29/02/2024", dates.getStartInput().getText());
            dates.setValue(range); assertEquals(0, changes.get()); dates.clear(); assertEquals(1, changes.get());
            assertThrows(IllegalArgumentException.class, () -> dates.setValue(new TimeRange(LocalTime.NOON, LocalTime.NOON, false)));
            assertThrows(IllegalArgumentException.class, () -> dates.setLimits(LocalTime.NOON, null));
            assertThrows(IllegalArgumentException.class, () -> dates.setValue(new DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 2))));
            PeriodField dateTime = new PeriodField(PeriodMode.DATE_TIME);
            DateTimeRange dt = new DateTimeRange(LocalDateTime.of(2024, 2, 29, 23, 0), LocalDateTime.of(2024, 3, 1, 2, 0));
            dateTime.setValue(dt); assertEquals(dt, dateTime.getValue());
            PeriodField time = new PeriodField(PeriodMode.TIME); TimeRange overnight = new TimeRange(LocalTime.of(22, 0), LocalTime.of(2, 0), true);
            assertThrows(IllegalArgumentException.class, () -> time.setValue(overnight));
            time.setAllowOvernight(true).setValue(overnight); assertEquals(overnight, time.getValue());
            assertThrows(IllegalStateException.class, () -> time.setAllowOvernight(false));
            assertThrows(IllegalArgumentException.class, () -> time.setPresentation(PeriodField.Presentation.CALENDAR));
            time.clear().setAllowOvernight(false).setValue(new TimeRange(LocalTime.NOON, LocalTime.NOON, false));
        });
    }
    @Test void invalidDraftPreservesValueAndBlocksFormSubmission() throws Exception {
        edt(() -> {
            PeriodField field = new PeriodField(); DateRange range = new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2)); field.setValue(range);
            FormPanel form = new FormPanel().addField("period", "Period", field);
            field.getEndInput().setText("31/02/2024"); assertFalse(field.isInputValid()); assertEquals(range, field.getValue()); assertFalse(form.submit());
            assertFalse(form.getField("period").isFieldValid());
            field.setValue(range); assertTrue(form.getField("period").isFieldValid());
            field.getEndInput().setText("31/02/2024"); assertFalse(field.isInputValid());
            field.getEndInput().setText("03/01/2024"); assertTrue(field.isInputValid()); assertTrue(form.submit());
            field.getStartInput().setText(""); assertFalse(field.isInputValid()); assertFalse(form.submit());
            field.getEndInput().setText(""); assertTrue(field.isInputValid()); assertNull(field.getValue());
            field.setValue(range); field.getEndInput().setText("01/01/2024"); assertTrue(field.isInputValid());
            form.reset(); assertNull(field.getValue()); assertTrue(field.isInputValid());
        });
    }
    @Test void periodFormatAndConfigurationAreAtomic() throws Exception {
        edt(() -> {
            PeriodField field = new PeriodField(); DateRange range = new DateRange(LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 1)); field.setValue(range);
            field.setLocale(Locale.US); field.setFormat("MM/dd/yyyy"); assertEquals("02/29/2024", field.getStartInput().getText());
            assertThrows(RuntimeException.class, () -> field.setFormat("HH:mm")); assertEquals("MM/dd/yyyy", field.getFormat());
            assertThrows(IllegalArgumentException.class, () -> field.setLimits(LocalDate.of(2025, 1, 1), null)); assertNull(field.getMinimum());
            assertEquals(range, field.getValue()); field.setEditable(false); assertFalse(field.getStartInput().isEditable());
            field.setEnabled(false); assertFalse(field.getEndInput().isEnabled());
        });
    }
    @Test void calendarSelectsAcrossMonthsInEitherDirectionAndEnforcesLimits() throws Exception {
        edt(() -> {
            RangeCalendarPanel calendar = new RangeCalendarPanel(Locale.US);
            calendar.setLimits(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 31));
            calendar.selectDate(LocalDate.of(2024, 3, 2)); assertNull(calendar.getSelection());
            calendar.selectDate(LocalDate.of(2024, 2, 29)); assertEquals(new DateRange(LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 2)), calendar.getSelection());
            calendar.showMonth(YearMonth.of(2024, 2));
            List<JToggleButton> selected = descendants(calendar).stream().filter(JToggleButton.class::isInstance).map(JToggleButton.class::cast).filter(JToggleButton::isSelected).toList();
            assertEquals(2, selected.size());
            assertTrue(selected.stream().noneMatch(b -> LocalDate.of(2024, 3, 1).equals(b.getClientProperty("period.date"))));
            calendar.selectDate(LocalDate.of(2024, 3, 1)); assertNull(calendar.getSelection());
            calendar.selectDate(LocalDate.of(2024, 3, 1)); assertEquals(calendar.getSelection().start(), calendar.getSelection().end());
            assertThrows(IllegalArgumentException.class, () -> calendar.selectDate(LocalDate.of(2024, 4, 1)));
            calendar.showMonth(YearMonth.of(2024, 2));
            assertTrue(descendants(calendar).stream().filter(JToggleButton.class::isInstance).map(JToggleButton.class::cast).anyMatch(b -> b.getAccessibleContext().getAccessibleName().contains("February")));
        });
    }
    @Test void paletteSearchesAllFieldsWithoutAccentsAndRechecksEnabledState() throws Exception {
        edt(() -> {
            List<CommandEntry> commands = new ArrayList<>(List.of(
                new CommandEntry("save", "Salvar", "Edição", "Descrição", "Ctrl+S", true),
                new CommandEntry("disabled", "Desabilitado", "", "", "", false)));
            List<String> calls = new ArrayList<>(); CommandPalette palette = new CommandPalette(() -> commands, id -> { calls.add(id); return true; });
            palette.getSearchField().setText("edicao"); assertEquals(1, palette.getCommandList().getModel().getSize());
            palette.getSearchField().setText("descricao"); assertEquals("save", palette.getCommandList().getSelectedValue().id());
            commands.set(0, new CommandEntry("save", "Salvar", "Edição", "Descrição", "Ctrl+S", false));
            assertFalse(palette.executeSelected()); assertTrue(calls.isEmpty());
            commands.set(0, new CommandEntry("save", "Salvar", "Edição", "Descrição", "Ctrl+S", true));
            assertTrue(palette.executeSelected()); assertEquals(List.of("save"), calls);
            palette.getSearchField().setText("absent"); assertFalse(palette.executeSelected());
            palette.getSearchField().getActionMap().get("next").actionPerformed(null); assertEquals(-1, palette.getCommandList().getSelectedIndex());
            palette.getSearchField().setText("disabled"); assertFalse(palette.executeSelected());
        });
    }
    @Test void paletteRestoresShortcutAndRejectsDuplicateIds() throws Exception {
        edt(() -> {
            CommandEntry entry = new CommandEntry("id", "Name", null, null, null, true);
            assertThrows(IllegalArgumentException.class, () -> new CommandPalette(() -> List.of(entry, entry), id -> true));
            CommandPalette palette = new CommandPalette(() -> List.of(entry), id -> false);
            JRootPane root = new JRootPane(); KeyStroke stroke = KeyStroke.getKeyStroke("control shift P");
            root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "previous");
            AutoCloseable registration = palette.installShortcut(root, stroke);
            assertNotEquals("previous", root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke));
            try { registration.close(); registration.close(); } catch (Exception e) { throw new AssertionError(e); }
            assertEquals("previous", root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(stroke));
            assertFalse(palette.executeSelected());
        });
    }
    @Test void togglesExposeRolesStatesAndAccessibleActionsEvenForSilentUpdates() throws Exception {
        edt(() -> {
            CheckBoxField check = new CheckBoxField("Accept"); AccessibleContext accessible = check.getAccessibleContext();
            assertEquals(AccessibleRole.CHECK_BOX, accessible.getAccessibleRole()); assertEquals("Accept", accessible.getAccessibleName());
            List<String> notifications = new ArrayList<>(); accessible.addPropertyChangeListener(e -> notifications.add(e.getPropertyName()));
            check.setSelected(true, false); assertTrue(accessible.getAccessibleStateSet().contains(AccessibleState.CHECKED));
            assertTrue(notifications.contains(AccessibleContext.ACCESSIBLE_STATE_PROPERTY));
            check.setIndeterminate(true); assertTrue(accessible.getAccessibleStateSet().contains(AccessibleState.INDETERMINATE));
            accessible.getAccessibleAction().doAccessibleAction(0); assertFalse(check.isIndeterminate()); assertTrue(check.isSelected());
            check.setEnabled(false); assertFalse(accessible.getAccessibleAction().doAccessibleAction(0));
            SwitchField toggle = new SwitchField(); assertEquals(AccessibleRole.TOGGLE_BUTTON, toggle.getAccessibleContext().getAccessibleRole());
            toggle.getAccessibleContext().getAccessibleAction().doAccessibleAction(0); assertTrue(toggle.isSelected());
        });
    }
    @Test void numericControlsExposeValuesBoundsAndReadonlyBehavior() throws Exception {
        edt(() -> {
            SliderField slider = new SliderField(0, 10, 2); AccessibleContext accessible = slider.getAccessibleContext();
            assertEquals(AccessibleRole.SLIDER, accessible.getAccessibleRole());
            List<String> notifications = new ArrayList<>(); accessible.addPropertyChangeListener(e -> notifications.add(e.getPropertyName()));
            slider.setValue(4, false); assertEquals(4d, accessible.getAccessibleValue().getCurrentAccessibleValue());
            assertTrue(notifications.contains(AccessibleContext.ACCESSIBLE_VALUE_PROPERTY));
            assertFalse(accessible.getAccessibleValue().setCurrentAccessibleValue(11)); assertTrue(accessible.getAccessibleValue().setCurrentAccessibleValue(5));
            assertFalse(accessible.getAccessibleValue().setCurrentAccessibleValue(Double.NaN));
            RatingField rating = new RatingField().setReadOnly(true); assertFalse(rating.getAccessibleContext().getAccessibleValue().setCurrentAccessibleValue(2));
            StepperField stepper = new StepperField(BigDecimal.ONE); AccessibleContext sc = stepper.getAccessibleContext();
            assertEquals(AccessibleRole.SPIN_BOX, sc.getAccessibleRole()); stepper.setValue(BigDecimal.TEN, false); assertEquals(stepper.getValue(), sc.getAccessibleValue().getCurrentAccessibleValue());
            stepper.getNumberField().setEditable(false); assertFalse(sc.getAccessibleAction().doAccessibleAction(0));
            stepper.increment(); assertEquals(0, BigDecimal.TEN.compareTo(stepper.getValue()));
        });
    }
    @Test void groupsExposeSelectedChildrenAndMaskedPinDoesNotExposeDigits() throws Exception {
        edt(() -> {
            RadioGroupField<String> group = new RadioGroupField<>(); group.addOption("A", "a").addOption("B", "b");
            AccessibleSelection selection = group.getAccessibleContext().getAccessibleSelection(); selection.addAccessibleSelection(1);
            assertEquals("b", group.getSelectedValue()); assertTrue(selection.isAccessibleChildSelected(1));
            assertEquals(AccessibleRole.RADIO_BUTTON, selection.getAccessibleSelection(0).getAccessibleContext().getAccessibleRole());
            selection.clearAccessibleSelection(); assertNull(group.getSelectedValue());
            SegmentedField<String> segments = new SegmentedField<>(); segments.addSegment("A", "a").addSegment("B", "b");
            AccessibleContext ac = segments.getAccessibleContext(); ac.getAccessibleSelection().addAccessibleSelection(1);
            assertEquals("b", segments.getSelectedValue()); assertEquals("B", ac.getAccessibleChild(1).getAccessibleContext().getAccessibleName());
            assertTrue(ac.getAccessibleChild(1).getAccessibleContext().getAccessibleStateSet().contains(AccessibleState.CHECKED));
            PinField pin = new PinField().setValue("123456").setMasked(true);
            assertEquals(AccessibleRole.PASSWORD_TEXT, pin.getAccessibleContext().getAccessibleRole());
            assertEquals("•", pin.getAccessibleContext().getAccessibleText().getAtIndex(AccessibleText.CHARACTER, 0));
        });
    }
    @Test void customButtonsAreFocusableNamedAndActionable() throws Exception {
        edt(() -> {
            DualListField<String> dual = new DualListField<>(List.of("A", "B"));
            List<Component> buttons = descendants(dual).stream().filter(c -> c instanceof dtm.stools.component.accessibility.AccessibleButton).toList();
            assertEquals(6, buttons.size());
            for (Component button : buttons) {
                assertTrue(button.isFocusable()); AccessibleContext context = ((Accessible) button).getAccessibleContext();
                assertEquals(AccessibleRole.PUSH_BUTTON, context.getAccessibleRole()); assertNotNull(context.getAccessibleName());
            }
            JTextFieldListener text = new JTextFieldListener(); text.setText("value");
            AccessibleAction action = text.getAccessibleContext().getAccessibleAction();
            int nativeActions = new JTextField().getAccessibleContext().getAccessibleAction().getAccessibleActionCount();
            assertEquals(nativeActions + 1, action.getAccessibleActionCount());
            assertTrue(action.doAccessibleAction(nativeActions)); assertEquals("", text.getText());
            ColorPickerField color = new ColorPickerField(); assertTrue(descendants(color).stream().filter(JButton.class::isInstance).allMatch(Component::isFocusable));
            DatePickerInputField date = new DatePickerInputField(); date.setEnabled(false); assertTrue(descendants(date).stream().filter(JButton.class::isInstance).noneMatch(Component::isEnabled));
        });
    }
    @Test void formLabelsAndErrorsReachNativeInputsAndPreserveApplicationNames() throws Exception {
        edt(() -> {
            var area = new dtm.stools.component.inputfields.textarea.TextAreaField();
            FormField field = new FormField("notes", "Notes", area).setHelperText("Write notes");
            AccessibleContext ac = area.getTextArea().getAccessibleContext();
            assertEquals("Notes", ac.getAccessibleName()); assertEquals("Write notes", ac.getAccessibleDescription());
            field.setLabelText("Comments"); assertEquals("Comments", ac.getAccessibleName());
            field.setError("Required"); assertEquals("Required", ac.getAccessibleDescription());
            ac.setAccessibleName("Application name"); ac.setAccessibleDescription("Application description");
            field.setLabelText("Other").setHelperText("Other help"); field.clearError();
            assertEquals("Application name", ac.getAccessibleName()); assertEquals("Application description", ac.getAccessibleDescription());
            assertDoesNotThrow(() -> new FormField("custom", "Custom", new JComponent() {}));
        });
    }
    static List<Component> descendants(Container container) {
        List<Component> all = new ArrayList<>();
        for (Component component : container.getComponents()) { all.add(component); if (component instanceof Container child) all.addAll(descendants(child)); }
        return all;
    }
}
