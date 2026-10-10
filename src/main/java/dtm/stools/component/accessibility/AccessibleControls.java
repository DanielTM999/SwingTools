package dtm.stools.component.accessibility;

import dtm.stools.component.inputfields.checkfield.*;
import dtm.stools.component.inputfields.switchfield.SwitchField;
import dtm.stools.component.inputfields.sliderfield.SliderField;
import dtm.stools.component.inputfields.ratingfield.RatingField;
import dtm.stools.component.inputfields.segmentedfield.SegmentedField;
import dtm.stools.component.inputfields.stepperfield.StepperField;
import dtm.stools.component.inputfields.pinfield.PinField;
import dtm.stools.i18n.I18n;
import javax.accessibility.*;
import javax.swing.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.*;

/** Semantic adapters for painted controls. Native Swing children retain their own contexts. */
public final class AccessibleControls {
    private AccessibleControls() {}
    public static class Model {
        public AccessibleRole role() { return AccessibleRole.PANEL; }
        public String name() { return null; }
        public boolean selected() { return false; }
        public boolean mixed() { return false; }
        public boolean editable() { return true; }
        public int actionCount() { return 0; }
        public String actionName(int index) { return null; }
        public void action(int index) {}
        public boolean numeric() { return false; }
        public Number value() { return null; }
        public Number minimum() { return null; }
        public Number maximum() { return null; }
        public void value(Number value) {}
        public boolean selection() { return false; }
        public List<String> options() { return List.of(); }
        public int selectedIndex() { return -1; }
        public void select(int index) {}
        public void clearSelection() {}
        public Accessible child(int index) { return null; }
        public String text() { return null; }
    }
    public static Model forControl(JComponent component) {
        if (component instanceof CheckBoxField c) return toggle(AccessibleRole.CHECK_BOX, c::getText, c::isSelected, c::isIndeterminate, () -> c.setSelected(!c.isSelected()));
        if (component instanceof SwitchField c) return toggle(AccessibleRole.TOGGLE_BUTTON, () -> null, c::isSelected, () -> false, () -> c.setSelected(!c.isSelected()));
        if (component instanceof RadioField<?> c) return toggle(AccessibleRole.RADIO_BUTTON, c::getText, c::isSelected, () -> false, () -> c.setSelected(true));
        if (component instanceof SliderField c) return numeric(AccessibleRole.SLIDER, c::getValue, c::getMinimum, c::getMaximum, n -> c.setValue(n.doubleValue()), () -> true,
            () -> c.setValue(c.getValue() + c.getStep()), () -> c.setValue(c.getValue() - c.getStep()));
        if (component instanceof RatingField c) return numeric(AccessibleRole.SLIDER, c::getValue, () -> 0, c::getCount, n -> c.setValue(n.doubleValue()), () -> !c.isReadOnly(),
            () -> c.setValue(c.getValue() + (c.isAllowHalf() ? .5 : 1)), () -> c.setValue(c.getValue() - (c.isAllowHalf() ? .5 : 1)));
        if (component instanceof StepperField c) return numeric(AccessibleRole.SPIN_BOX, c::getValue, () -> c.getNumberField().getMinimumValue(), () -> c.getNumberField().getMaximumValue(),
            n -> c.setValue(n instanceof BigDecimal b ? b : new BigDecimal(n.toString())), () -> c.getNumberField().isEditable(), c::increment, c::decrement);
        if (component instanceof SegmentedField<?> c) return new Model() {
            @Override
            public boolean selection() { return true; }
            @Override
            public List<String> options() { return c.getSegments().stream().map(SegmentedField.Segment::label).toList(); }
            @Override
            public int selectedIndex() { return c.getSelectedIndex(); }
            @Override
            public void select(int index) { c.setSelectedIndex(index); }

        };
        if (component instanceof RadioGroupField<?> c) return new Model() {
            @Override
            public boolean selection() { return true; }
            @Override
            public List<String> options() { return c.getOptions().stream().map(RadioField::getText).toList(); }
            @Override
            public int selectedIndex() { return c.getSelectedOption() == null ? -1 : c.getOptions().indexOf(c.getSelectedOption()); }
            @Override
            public void select(int index) { c.getOptions().get(index).setSelected(true); }
            @Override
            public void clearSelection() { c.clearSelection(true); }
            @Override
            public Accessible child(int index) { return c.getOptions().get(index); }
        };
        if (component instanceof PinField c) return new Model() {
            @Override
            public AccessibleRole role() { return c.isMasked() ? AccessibleRole.PASSWORD_TEXT : AccessibleRole.TEXT; }
            @Override
            public String text() { return c.isMasked() ? "•".repeat(c.getValue().length()) : c.getValue(); }
        };
        return null;
    }
    private static Model toggle(AccessibleRole role, Supplier<String> name, BooleanSupplier selected, BooleanSupplier mixed, Runnable action) {
        return new Model() {
            @Override
            public AccessibleRole role() { return role; }
            @Override
            public String name() { return name.get(); }
            @Override
            public boolean selected() { return selected.getAsBoolean(); }
            @Override
            public boolean mixed() { return mixed.getAsBoolean(); }
            @Override
            public int actionCount() { return 1; }
            @Override
            public String actionName(int index) { return I18n.getText(AccessibleControls.class, "activate", "Ativar"); }
            @Override
            public void action(int index) { action.run(); }
        };
    }
    private static Model numeric(AccessibleRole role, Supplier<Number> value, Supplier<Number> minimum, Supplier<Number> maximum, Consumer<Number> setter,
                                 BooleanSupplier editable, Runnable increment, Runnable decrement) {
        return new Model() {
            @Override
            public AccessibleRole role() { return role; }
            @Override
            public boolean numeric() { return true; }
            @Override
            public Number value() { return value.get(); }
            @Override
            public Number minimum() { return minimum.get(); }
            @Override
            public Number maximum() { return maximum.get(); }
            @Override
            public void value(Number n) { setter.accept(n); }
            @Override
            public boolean editable() { return editable.getAsBoolean(); }
            @Override
            public int actionCount() { return 2; }
            @Override
            public String actionName(int index) { return index == 0 ? I18n.getText(AccessibleControls.class, "increment", "Aumentar") : I18n.getText(AccessibleControls.class, "decrement", "Diminuir"); }
            @Override
            public void action(int index) { if (index == 0) increment.run(); else decrement.run(); }
        };
    }
}
