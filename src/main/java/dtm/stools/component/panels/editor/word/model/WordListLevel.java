package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordListLevel(Format format, String text, int start, float indent, float hanging) {
    public enum Format { BULLET, DECIMAL, LOWER_LETTER, UPPER_LETTER, LOWER_ROMAN, UPPER_ROMAN, NONE }
    public WordListLevel {
        Objects.requireNonNull(format); Objects.requireNonNull(text);
        if (start < 0 || start > 32767 || !Float.isFinite(indent) || !Float.isFinite(hanging) || indent < 0 || hanging < 0) throw new IllegalArgumentException("Invalid list level");
    }
    public WordListLevel withStart(int value) { return new WordListLevel(format,text,value,indent,hanging); }
    public static String number(Format format, int value) {
        return switch (format) {
            case DECIMAL -> Integer.toString(value);
            case LOWER_LETTER -> letters(value).toLowerCase(java.util.Locale.ROOT);
            case UPPER_LETTER -> letters(value);
            case LOWER_ROMAN -> roman(value).toLowerCase(java.util.Locale.ROOT);
            case UPPER_ROMAN -> roman(value);
            case BULLET, NONE -> "";
        };
    }
    private static String letters(int value) {
        if (value <= 0) return "";
        StringBuilder b = new StringBuilder(); int n = value;
        while (n > 0) { n--; b.insert(0,(char)('A' + n % 26)); n /= 26; }
        return b.toString();
    }
    private static String roman(int value) {
        if (value <= 0 || value >= 4000) return Integer.toString(value);
        int[] n = {1000,900,500,400,100,90,50,40,10,9,5,4,1};
        String[] s = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
        StringBuilder b = new StringBuilder(); int v = value;
        for (int i = 0; i < n.length; i++) while (v >= n[i]) { b.append(s[i]); v -= n[i]; }
        return b.toString();
    }
}
