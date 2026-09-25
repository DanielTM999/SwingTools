package dtm.stools.component.panels.editor.sheet.formula;

import dtm.stools.component.panels.editor.sheet.function.FunctionNameCatalog;

import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

public record FormulaLocale(String id, char argumentSeparator, char decimalSeparator, char arrayColumnSeparator, char arrayRowSeparator,
                            FunctionNameCatalog names, String trueText, String falseText, boolean localizedErrors) {
    public static final FormulaLocale EN = new FormulaLocale("en-US", ',', '.', ',', ';', null, "TRUE", "FALSE", false);
    public static final FormulaLocale PT_BR = new FormulaLocale("pt-BR", ';', ',', '\\', ';', FunctionNameCatalog.portuguese(), "VERDADEIRO", "FALSO", true);
    public static final FormulaLocale COMMA_DECIMAL = new FormulaLocale("comma", ';', ',', '\\', ';', null, "TRUE", "FALSE", false);

    public FormulaLocale { Objects.requireNonNull(id); }

    public static FormulaLocale of(Locale locale) {
        if (locale == null) return EN;
        if (locale.getLanguage().equals("pt")) return PT_BR;
        char decimal = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
        return decimal == ',' ? COMMA_DECIMAL : EN;
    }

    public String localizeFunction(String canonical) { return names == null ? canonical : names.localize(canonical); }
    public String canonicalFunction(String localized) { return names == null ? localized.toUpperCase(Locale.ROOT) : names.canonical(localized); }
    public boolean isCanonical() { return this == EN; }
}
