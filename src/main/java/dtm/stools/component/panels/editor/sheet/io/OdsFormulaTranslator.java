package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.FormulaPrinter;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.formula.RefNode;
import dtm.stools.component.panels.editor.sheet.formula.RefPart;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OdsFormulaTranslator {
    private static final Pattern REF = Pattern.compile("\\[(\\$?(?:'(?:[^']|'')+'|[^.\\[\\]:']*))?\\.([$A-Za-z0-9]+)(?::(\\$?(?:'(?:[^']|'')+'|[^.\\[\\]:']*))?\\.([$A-Za-z0-9]+))?\\]");

    private OdsFormulaTranslator() {}

    public static String toCanonical(String odf) {
        String f = odf;
        if (f.startsWith("of:")) f = f.substring(3);
        else if (f.startsWith("oooc:")) f = f.substring(5);
        if (f.startsWith("=")) f = f.substring(1);
        StringBuilder out = new StringBuilder();
        boolean quoted = false;
        int i = 0;
        while (i < f.length()) {
            char c = f.charAt(i);
            if (c == '"') { quoted = !quoted; out.append(c); i++; continue; }
            if (!quoted && c == '[') {
                Matcher m = REF.matcher(f).region(i, f.length());
                if (m.lookingAt()) { out.append(ref(m)); i = m.end(); continue; }
            }
            if (!quoted && c == ';') { out.append(','); i++; continue; }
            out.append(c);
            i++;
        }
        try { return Formulas.toCanonical(out.toString(), FormulaLocale.EN); } catch (RuntimeException e) { return out.toString(); }
    }

    private static String ref(Matcher m) {
        String sheet = m.group(1), a = m.group(2), sheet2 = m.group(3), b = m.group(4);
        StringBuilder s = new StringBuilder();
        if (sheet != null && !sheet.isEmpty() && !sheet.equals("$")) {
            String name = sheet.startsWith("$") ? sheet.substring(1) : sheet;
            if (name.startsWith("'")) name = name.substring(1, name.length() - 1).replace("''", "'");
            s.append(FormulaPrinter.sheet(name)).append('!');
        }
        s.append(a);
        if (b != null) s.append(':').append(b);
        return s.toString();
    }

    public static String toOdf(String canonical) {
        try {
            String text = new FormulaPrinter(new FormulaLocale("odf", ';', '.', ';', '|', null, "TRUE()", "FALSE()", false))
                    .withReferenceMapper(OdsFormulaTranslator::odfRef)
                    .print(Formulas.parseCanonical(canonical));
            return "of:=" + text;
        } catch (RuntimeException e) {
            return "of:=" + canonical;
        }
    }

    private static String odfRef(RefNode r) {
        String prefix = r.sheet() == null ? "." : "$" + quote(r.sheet()) + ".";
        StringBuilder b = new StringBuilder("[").append(prefix).append(part(r.first()));
        if (r.second() != null) b.append(":").append(r.sheetEnd() != null ? "$" + quote(r.sheetEnd()) + "." : ".").append(part(r.second()));
        return b.append("]").toString();
    }

    private static String quote(String s) { return s.matches("[\\p{L}_][\\p{L}\\p{N}_]*") ? s : "'" + s.replace("'", "''") + "'"; }

    private static String part(RefPart p) {
        StringBuilder b = new StringBuilder();
        if (!p.wholeRow()) { if (p.columnAbsolute()) b.append('$'); b.append(CellAddress.columnName(p.column())); }
        else b.append("$A");
        if (!p.wholeColumn()) { if (p.rowAbsolute()) b.append('$'); b.append(p.row() + 1); }
        else b.append("$1");
        return b.toString();
    }
}
