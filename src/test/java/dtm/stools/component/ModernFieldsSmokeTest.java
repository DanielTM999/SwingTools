package dtm.stools.component;

import dtm.stools.component.inputfields.checkfield.CheckBoxField;
import dtm.stools.component.inputfields.checkfield.RadioGroupField;
import dtm.stools.component.inputfields.duallistfield.DualListField;
import dtm.stools.component.inputfields.pinfield.PinField;
import dtm.stools.component.inputfields.ratingfield.RatingField;
import dtm.stools.component.inputfields.segmentedfield.SegmentedField;
import dtm.stools.component.inputfields.sliderfield.SliderField;
import dtm.stools.component.inputfields.stepperfield.StepperField;
import dtm.stools.component.inputfields.textarea.TextAreaField;
import dtm.stools.configs.UiTokens;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernFieldsSmokeTest {

    @Test
    void tokensResolveWithoutLookAndFeel() {
        UiTokens.refresh();
        assertNotNull(UiTokens.primary());
        assertNotNull(UiTokens.foreground());
        assertEquals(16, UiTokens.space(4));
        assertTrue(UiTokens.radius(UiTokens.Radius.MD) > 0);
    }

    @Test
    void checkBoxPaintsAndTogglesState() {
        CheckBoxField field = new CheckBoxField("Aceito", false);
        field.setAnimated(false);
        paint(field);

        field.setSelected(true);
        assertTrue(field.isSelected());

        field.setIndeterminate(true);
        assertTrue(field.isIndeterminate());
        assertFalse(field.isSelected());
        paint(field);
    }

    @Test
    void radioGroupKeepsSingleSelection() {
        RadioGroupField<String> group = new RadioGroupField<>();
        group.addOption("A", "a").addOption("B", "b").addOption("C", "c");

        group.setSelectedValue("b");
        assertEquals("b", group.getSelectedValue());

        group.setSelectedValue("c");
        assertEquals("c", group.getSelectedValue());
        assertEquals(1, group.getOptions().stream().filter(option -> option.isSelected()).count());
    }

    @Test
    void segmentedFieldSelectsByValue() {
        SegmentedField<String> field = new SegmentedField<>();
        field.setAnimated(false);
        field.addSegment("Dia", "day").addSegment("Mês", "month");

        field.setSelectedValue("month");
        assertEquals("month", field.getSelectedValue());
        assertEquals(1, field.getSelectedIndex());
        paint(field);
    }

    @Test
    void sliderClampsAndSnapsToStep() {
        SliderField slider = new SliderField(0, 10, 0);
        slider.setStep(2);

        slider.setValue(7);
        assertEquals(8d, slider.getValue());

        slider.setValue(50);
        assertEquals(10d, slider.getValue());

        slider.setValue(-5);
        assertEquals(0d, slider.getValue());
        paint(slider);
    }

    @Test
    void ratingSnapsToHalfWhenAllowed() {
        RatingField rating = new RatingField(5, 0);
        rating.setValue(3.4);
        assertEquals(3d, rating.getValue());

        rating.setAllowHalf(true);
        rating.setValue(3.4);
        assertEquals(3.5d, rating.getValue());
        paint(rating);
    }

    @Test
    void pinFieldAcceptsOnlyDigitsAndReportsCompletion() {
        PinField pin = new PinField(4);
        pin.setValue("12ab34");
        assertEquals("1234", pin.getValue());
        assertTrue(pin.isComplete());

        pin.clear();
        assertEquals("", pin.getValue());
        paint(pin);
    }

    @Test
    void stepperRespectsRange() {
        StepperField stepper = new StepperField(BigDecimal.ZERO);
        stepper.setRange(BigDecimal.ZERO, BigDecimal.valueOf(2)).setStep(BigDecimal.ONE);

        stepper.increment();
        stepper.increment();
        stepper.increment();
        assertEquals(0, BigDecimal.valueOf(2).compareTo(stepper.getValue()));

        stepper.decrement();
        stepper.decrement();
        stepper.decrement();
        assertEquals(0, BigDecimal.ZERO.compareTo(stepper.getValue()));
        paint(stepper);
    }

    @Test
    void textAreaEnforcesMaxLength() {
        TextAreaField area = new TextAreaField("descreva");
        area.setMaxLength(5);
        area.setText("abcdefghij");
        assertEquals(5, area.getText().length());
        paint(area);
    }

    @Test
    void dualListTransfersItemsBothWays() {
        DualListField<String> dual = new DualListField<>(List.of("a", "b", "c", "d"));

        dual.setSelected(List.of("b", "c"));
        assertEquals(List.of("b", "c"), dual.getSelected());
        assertEquals(List.of("a", "d"), dual.getAvailable());

        dual.removeAllItems();
        assertTrue(dual.getSelected().isEmpty());
        assertEquals(4, dual.getAvailable().size());

        dual.addAllItems();
        assertEquals(4, dual.getSelected().size());
        assertTrue(dual.getAvailable().isEmpty());
        paint(dual);
    }

    @Test
    void dualListHonoursMaxSelected() {
        DualListField<String> dual = new DualListField<>(List.of("a", "b", "c"));
        dual.setMaxSelected(2);

        dual.addAllItems();
        assertEquals(2, dual.getSelected().size());
        assertEquals(1, dual.getAvailable().size());
    }

    private static void paint(JComponent component) {
        component.setSize(component.getPreferredSize().width > 0 ? component.getPreferredSize().width : 200,
                component.getPreferredSize().height > 0 ? component.getPreferredSize().height : 40);
        component.doLayout();

        BufferedImage image = new BufferedImage(
                Math.max(1, component.getWidth()),
                Math.max(1, component.getHeight()),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            component.paint(g2);
        } finally {
            g2.dispose();
        }
    }
}
