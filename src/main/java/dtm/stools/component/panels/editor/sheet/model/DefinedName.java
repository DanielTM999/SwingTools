package dtm.stools.component.panels.editor.sheet.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record DefinedName(String name, String formula, Integer sheetScope, boolean hidden, String comment) {
    private static final Pattern VALID = Pattern.compile("[\\p{L}_\\\\][\\p{L}\\p{N}_.\\\\?]*");

    public DefinedName {
        Objects.requireNonNull(name); Objects.requireNonNull(formula);
        if (!isValidName(name)) throw new IllegalArgumentException("Invalid name: " + name);
        formula = formula.startsWith("=") ? formula.substring(1) : formula;
        comment = Objects.requireNonNullElse(comment, "");
    }

    public static DefinedName of(String name, String formula) { return new DefinedName(name, formula, null, false, ""); }

    public static boolean isValidName(String name) {
        if (name == null || name.isBlank() || name.length() > 255 || !VALID.matcher(name).matches()) return false;
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.equals("R") || upper.equals("C") || upper.equals("TRUE") || upper.equals("FALSE")) return false;
        if (upper.matches("[A-Z]{1,3}[0-9]+")) {
            try { CellAddress.parse(upper); return false; } catch (RuntimeException ignored) { return true; }
        }
        return !upper.matches("R[0-9]*C[0-9]*");
    }

    public String key() { return (sheetScope == null ? "" : sheetScope + "!") + name.toUpperCase(Locale.ROOT); }
    public DefinedName withFormula(String f) { return new DefinedName(name, f, sheetScope, hidden, comment); }
}
