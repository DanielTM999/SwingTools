package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.LambdaValue;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.function.FunctionDefinition;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.EmptyValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.Locale;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.INFORMATION;
import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.TEXT;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class InformationFunctions {
    private InformationFunctions() {}

    static void register(FunctionRegistry r) {
        scalar(r, "ISBLANK", INFORMATION, 1, 1, (c, a) -> bool(arg(a, 0) instanceof EmptyValue));
        scalar(r, "ISNUMBER", INFORMATION, 1, 1, (c, a) -> bool(arg(a, 0) instanceof NumberValue));
        scalar(r, "ISTEXT", INFORMATION, 1, 1, (c, a) -> bool(arg(a, 0) instanceof TextValue));
        scalar(r, "ISNONTEXT", INFORMATION, 1, 1, (c, a) -> bool(!(arg(a, 0) instanceof TextValue)));
        scalar(r, "ISLOGICAL", INFORMATION, 1, 1, (c, a) -> bool(arg(a, 0) instanceof BoolValue));
        scalar(r, "ISERROR", INFORMATION, 1, 1, (c, a) -> bool(arg(a, 0) instanceof ErrorValue));
        scalar(r, "ISERR", INFORMATION, 1, 1, (c, a) -> bool(arg(a, 0) instanceof ErrorValue e && e.error() != CellError.NA));
        scalar(r, "ISNA", INFORMATION, 1, 1, (c, a) -> bool(arg(a, 0) instanceof ErrorValue e && e.error() == CellError.NA));
        scalar(r, "ISEVEN", INFORMATION, 1, 1, (c, a) -> bool(((long) Math.floor(Math.abs(n(a, 0)))) % 2 == 0));
        scalar(r, "ISODD", INFORMATION, 1, 1, (c, a) -> bool(((long) Math.floor(Math.abs(n(a, 0)))) % 2 == 1));
        raw(r, "ISREF", INFORMATION, 1, 1, (c, a) -> bool(a.value(0) instanceof ReferenceValue));
        raw(r, "ISFORMULA", INFORMATION, 1, 1, (c, a) -> {
            ReferenceValue ref = a.reference(0);
            return bool(c.formulaAt(ref.sheet(), ref.range().firstRow(), ref.range().firstColumn()).isPresent());
        });
        raw(r, "FORMULATEXT", dtm.stools.component.panels.editor.sheet.function.FunctionCategory.LOOKUP, 1, 1, (c, a) -> {
            ReferenceValue ref = a.reference(0);
            return c.formulaAt(ref.sheet(), ref.range().firstRow(), ref.range().firstColumn())
                    .<CellValue>map(f -> text("=" + dtm.stools.component.panels.editor.sheet.formula.Formulas.toDisplay(f, c.formulaLocale())))
                    .orElse(err(CellError.NA));
        });
        scalar(r, "N", INFORMATION, 1, 1, (c, a) -> switch (arg(a, 0)) {
            case NumberValue n -> n;
            case BoolValue b -> num(b.value() ? 1 : 0);
            case ErrorValue e -> e;
            default -> num(0);
        });
        scalar(r, "T", TEXT, 1, 1, (c, a) -> switch (arg(a, 0)) {
            case TextValue t -> t;
            case ErrorValue e -> e;
            default -> text("");
        });
        scalar(r, "NA", INFORMATION, 0, 0, (c, a) -> err(CellError.NA));
        raw(r, "TYPE", INFORMATION, 1, 1, (c, a) -> {
            CellValue v = a.value(0);
            if (v instanceof ReferenceValue ref && !ref.isSingleCell()) return num(64);
            v = c.deref(v);
            return num(switch (v) {
                case NumberValue n -> 1;
                case EmptyValue e -> 1;
                case TextValue t -> 2;
                case BoolValue b -> 4;
                case ErrorValue e -> 16;
                case ArrayValue arr -> 64;
                case LambdaValue l -> 128;
                default -> 1;
            });
        });
        scalar(r, "ERROR.TYPE", INFORMATION, 1, 1, (c, a) -> arg(a, 0) instanceof ErrorValue e ? num(e.error().code()) : err(CellError.NA));
        raw(r, "SHEET", INFORMATION, 0, 1, (c, a) -> {
            if (!a.has(0)) return num(c.hostSheet() + 1);
            CellValue v = a.value(0);
            if (v instanceof ReferenceValue ref) return num(ref.sheet() + 1);
            int idx = c.sheetIndex(Coerce.text(c.scalar(v)));
            return idx < 0 ? err(CellError.NA) : num(idx + 1);
        });
        raw(r, "SHEETS", INFORMATION, 0, 1, (c, a) -> {
            if (!a.has(0)) return num(c.workbook().sheetCount());
            CellValue v = a.value(0);
            if (v instanceof ReferenceValue ref) return num(ref.sheetEnd() - ref.sheet() + 1);
            throw EvalError.value();
        });
        r.register(FunctionDefinition.raw("CELL", INFORMATION, 1, 2, (c, a) -> {
            String info = a.text(0).toLowerCase(Locale.ROOT);
            ReferenceValue ref = a.has(1) ? a.reference(1) : ReferenceValue.cell(c.hostSheet(), c.host().row(), c.host().column());
            int s = ref.sheet(), row = ref.range().firstRow(), col = ref.range().firstColumn();
            SheetWorksheet ws = c.workbook().sheet(s);
            SheetCell cell = ws.cell(row, col);
            CellStyle style = c.workbook().style(cell.style());
            CellValue value = c.cell(s, row, col);
            return switch (info) {
                case "address", "endereço", "endereco" -> text((s != c.hostSheet() ? dtm.stools.component.panels.editor.sheet.formula.FormulaPrinter.sheet(ws.name()) + "!" : "") + new CellAddress(row, col).toAbsolute());
                case "col" -> num(col + 1);
                case "row", "lin" -> num(row + 1);
                case "contents", "conteúdo", "conteudo" -> value;
                case "filename", "nomearquivo" -> text("[" + c.workbook().properties().title() + "]" + ws.name());
                case "type", "tipo" -> text(value.isEmpty() ? "b" : value instanceof TextValue ? "l" : "v");
                case "width", "largura" -> num(Math.max(1, Math.round(ws.columns().size(col) / 7.0)));
                case "prefix", "prefixo" -> text(value instanceof TextValue ? switch (style.horizontal()) { case CENTER -> "^"; case RIGHT -> "\""; case FILL -> "\\"; default -> "'"; } : "");
                case "protect", "proteger" -> num(style.locked() ? 1 : 0);
                case "color", "cor" -> num(style.numberFormat().contains("[Red]") || style.numberFormat().contains("[Vermelho]") ? 1 : 0);
                case "parentheses", "parênteses" -> num(style.numberFormat().contains("(") ? 1 : 0);
                case "format", "formato" -> text(cellFormatCode(style.numberFormat()));
                case "sheetname" -> text(ws.name());
                default -> throw EvalError.value();
            };
        }).volatileFunction().build());
        r.register(FunctionDefinition.scalar("INFO", INFORMATION, 1, 1, (c, a) -> switch (t(a, 0).toLowerCase(Locale.ROOT)) {
            case "numfile", "numarquivo" -> num(c.workbook().sheetCount());
            case "osversion", "sistemaoperacional" -> text(System.getProperty("os.name") + " " + System.getProperty("os.version"));
            case "recalc" -> text(c.locale().getLanguage().equals("pt") ? "Automático" : "Automatic");
            case "release", "versão" -> text("16.0");
            case "system", "sistema" -> text(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "pcdos" : "mac");
            case "directory", "diretório" -> text(System.getProperty("user.dir"));
            case "origin", "origem" -> text("$A:$A$1");
            default -> throw EvalError.value();
        }).volatileFunction().build());
    }

    static String cellFormatCode(String code) {
        if (code == null || code.equals("General")) return "G";
        String c = code.toLowerCase(Locale.ROOT);
        if (c.contains("%")) return "P" + decimals(c);
        if (c.contains("e+")) return "S" + decimals(c);
        if (c.contains("$") || c.contains("r$")) return "C" + decimals(c);
        if (c.contains("d") && c.contains("m") && c.contains("y")) return "D1";
        if (c.contains("h") && c.contains("s")) return "D6";
        if (c.contains("h")) return "D9";
        if (c.contains(",")) return "," + decimals(c);
        return "F" + decimals(c);
    }

    private static int decimals(String code) {
        int dot = code.indexOf('.');
        if (dot < 0) return 0;
        int n = 0;
        for (int i = dot + 1; i < code.length() && (code.charAt(i) == '0' || code.charAt(i) == '#'); i++) n++;
        return n;
    }

    static boolean omitted(CellValue v) { return v instanceof OmittedValue; }
    static HorizontalAlignment align() { return HorizontalAlignment.GENERAL; }
}
