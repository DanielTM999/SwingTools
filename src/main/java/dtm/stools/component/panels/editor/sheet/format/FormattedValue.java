package dtm.stools.component.panels.editor.sheet.format;

public record FormattedValue(String text, Integer color, Character fill, boolean numeric) {
    public static FormattedValue of(String text) { return new FormattedValue(text, null, null, false); }
}
