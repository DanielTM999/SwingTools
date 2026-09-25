package dtm.stools.component.panels.editor.sheet.model;

import java.util.Optional;

public enum CellError {
    NULL("#NULL!", "#NULO!", 1), DIV0("#DIV/0!", "#DIV/0!", 2), VALUE("#VALUE!", "#VALOR!", 3), REF("#REF!", "#REF!", 4),
    NAME("#NAME?", "#NOME?", 5), NUM("#NUM!", "#NÚM!", 6), NA("#N/A", "#N/D", 7), GETTING_DATA("#GETTING_DATA", "#OBTENDO_DADOS", 8),
    SPILL("#SPILL!", "#DESPEJAR!", 9), CONNECT("#CONNECT!", "#CONECTAR!", 10), BLOCKED("#BLOCKED!", "#BLOQUEADO!", 11),
    UNKNOWN("#UNKNOWN!", "#DESCONHECIDO!", 12), FIELD("#FIELD!", "#CAMPO!", 13), CALC("#CALC!", "#CALC!", 14), BUSY("#BUSY!", "#OCUPADO!", 15);

    private final String text, localized;
    private final int code;

    CellError(String text, String localized, int code) { this.text = text; this.localized = localized; this.code = code; }

    public String text() { return text; }
    public String localized() { return localized; }
    public int code() { return code; }

    public String text(boolean portuguese) { return portuguese ? localized : text; }

    public static Optional<CellError> parse(String value) {
        if (value == null) return Optional.empty();
        String v = value.strip().toUpperCase(java.util.Locale.ROOT);
        for (CellError e : values()) if (e.text.equals(v) || e.localized.toUpperCase(java.util.Locale.ROOT).equals(v)) return Optional.of(e);
        return Optional.empty();
    }
}
