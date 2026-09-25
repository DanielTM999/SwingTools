package dtm.stools.component.panels.editor.sheet.io;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

public record CsvDialect(char delimiter, char quote, Charset charset, Locale locale, boolean parseValues, boolean bom) {
    public static final CsvDialect COMMA = new CsvDialect(',', '"', StandardCharsets.UTF_8, Locale.US, true, false);
    public static final CsvDialect SEMICOLON = new CsvDialect(';', '"', StandardCharsets.UTF_8, Locale.forLanguageTag("pt-BR"), true, true);
    public static final CsvDialect TAB = new CsvDialect('\t', '"', StandardCharsets.UTF_8, Locale.forLanguageTag("pt-BR"), true, false);

    public CsvDialect {
        Objects.requireNonNull(charset);
        locale = Objects.requireNonNullElse(locale, Locale.getDefault());
    }

    public CsvDialect withDelimiter(char d) { return new CsvDialect(d, quote, charset, locale, parseValues, bom); }
    public CsvDialect withCharset(Charset c) { return new CsvDialect(delimiter, quote, c, locale, parseValues, bom); }
    public CsvDialect withLocale(Locale l) { return new CsvDialect(delimiter, quote, charset, l, parseValues, bom); }
    public CsvDialect withParseValues(boolean v) { return new CsvDialect(delimiter, quote, charset, locale, v, bom); }
}
