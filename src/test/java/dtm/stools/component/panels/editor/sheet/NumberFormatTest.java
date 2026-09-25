package dtm.stools.component.panels.editor.sheet;

import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.format.ParsedInput;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class NumberFormatTest {
    private final NumberFormatter en = new NumberFormatter(Locale.US, false);
    private final NumberFormatter pt = new NumberFormatter(Locale.forLanguageTag("pt-BR"), false);

    private String en(double v, String code) { return en.text(CellValue.of(v), code); }

    @Test void numericFormats() {
        assertEquals("1,234.57", en(1234.567, "#,##0.00"));
        assertEquals("1.234,57", pt.text(CellValue.of(1234.567), "#,##0.00"));
        assertEquals("0.50", en(0.5, "0.00"));
        assertEquals("12%", en(0.12, "0%"));
        assertEquals("1.23E+03", en(1234, "0.00E+00"));
        assertEquals("(1,234)", en(-1234, "#,##0_);(#,##0)").strip());
        assertEquals("-5", en(-5, "0"));
        assertEquals("1 1/2", en(1.5, "# ?/?"));
        assertEquals("1.5", en(1.5, "General"));
        assertEquals("00123", en(123, "00000"));
        assertEquals("1,235", en(1234567, "#,##0,"));
        assertEquals("R$ 10,00", pt.text(CellValue.of(10), "\"R$\" #,##0.00"));
        assertEquals("zero", en(0, "0;-0;\"zero\""));
        assertEquals("alto", en(150, "[>100]\"alto\";\"baixo\""));
        assertEquals(0xFFFF0000, en.format(CellValue.of(-1), "0;[Red]-0").color());
    }

    @Test void dateFormats() {
        assertEquals("01/01/2025", pt.text(CellValue.of(45658), "dd/mm/yyyy"));
        assertEquals("2025-01-01 13:30", en(45658.5625, "yyyy-mm-dd hh:mm"));
        assertEquals("1:30 PM", en(0.5625, "h:mm AM/PM"));
        assertEquals("30:00", en(1.25, "[h]:mm"));
        assertEquals("janeiro", pt.text(CellValue.of(45658), "mmmm"));
        assertEquals("Wednesday", en(45658, "dddd"));
    }

    @Test void parsesTypedInput() {
        ValueParser p = new ValueParser(Locale.forLanguageTag("pt-BR"), false);
        ParsedInput money = p.parse("R$ 1.234,56");
        assertEquals(1234.56, ((NumberValue) money.value()).value(), 1e-9);
        assertNotNull(money.format());
        assertEquals(0.1, ((NumberValue) p.parse("10%").value()).value(), 1e-12);
        assertEquals(45658, ((NumberValue) p.parse("01/01/2025").value()).value());
        assertEquals(0.5625, ((NumberValue) p.parse("13:30").value()).value(), 1e-12);
        assertEquals("texto", p.parse("'texto").value().display());
        assertTrue(p.parse("=A1+1").isFormula());
        assertEquals(CellValue.TRUE, p.parse("VERDADEIRO").value());
    }
}
