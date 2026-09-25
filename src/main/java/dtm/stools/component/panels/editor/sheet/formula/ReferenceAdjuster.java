package dtm.stools.component.panels.editor.sheet.formula;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ReferenceAdjuster {
    private ReferenceAdjuster() {}

    public static String shift(String canonical, int rows, int columns) {
        if (rows == 0 && columns == 0) return canonical;
        FormulaNode node = Formulas.parseCanonical(canonical);
        return FormulaPrinter.canonical(transform(node, r -> shiftRef(r, rows, columns)));
    }

    public static String rebase(String canonical, CellAddress from, CellAddress to) {
        return shift(canonical, to.row() - from.row(), to.column() - from.column());
    }

    public static String map(String canonical, String hostSheet, RangeMapper mapper) {
        FormulaNode node = Formulas.parseCanonical(canonical);
        FormulaNode out = transform(node, r -> {
            String target = r.sheet() == null ? hostSheet : r.sheet();
            return mapRef(r, target, mapper);
        });
        return FormulaPrinter.canonical(out);
    }

    public static String renameSheet(String canonical, String oldName, String newName) {
        FormulaNode node = Formulas.parseCanonical(canonical);
        FormulaNode out = transform(node, r -> {
            String s = r.sheet() != null && r.sheet().equalsIgnoreCase(oldName) ? newName : r.sheet();
            String e = r.sheetEnd() != null && r.sheetEnd().equalsIgnoreCase(oldName) ? newName : r.sheetEnd();
            return Objects.equals(s, r.sheet()) && Objects.equals(e, r.sheetEnd()) ? r : r.withSheet(s, e);
        }, n -> n.sheet() != null && n.sheet().equalsIgnoreCase(oldName) ? new NameNode(newName, n.name()) : n);
        return FormulaPrinter.canonical(out);
    }

    public static String sheetDeleted(String canonical, String sheetName) {
        FormulaNode node = Formulas.parseCanonical(canonical);
        FormulaNode out = transform(node, r -> r.sheet() != null && r.sheet().equalsIgnoreCase(sheetName) || r.sheetEnd() != null && r.sheetEnd().equalsIgnoreCase(sheetName) ? new ErrorNode(CellError.REF) : r);
        return FormulaPrinter.canonical(out);
    }

    public static boolean references(String canonical, String hostSheet, String sheet, CellRange range) {
        boolean[] hit = {false};
        try {
            transform(Formulas.parseCanonical(canonical), r -> {
                String target = r.sheet() == null ? hostSheet : r.sheet();
                if (target.equalsIgnoreCase(sheet) && r.range().intersects(range)) hit[0] = true;
                return r;
            });
        } catch (RuntimeException ignored) { }
        return hit[0];
    }

    public static List<RefNode> references(String canonical) {
        List<RefNode> list = new ArrayList<>();
        try { transform(Formulas.parseCanonical(canonical), r -> { list.add(r); return r; }); } catch (RuntimeException ignored) { }
        return list;
    }

    public static FormulaNode transform(FormulaNode node, RefTransform f) { return transform(node, f, n -> n); }

    public static FormulaNode transform(FormulaNode node, RefTransform f, NameTransform names) {
        return switch (node) {
            case RefNode r -> f.apply(r);
            case NameNode n -> names.apply(n);
            case FunctionNode fn -> new FunctionNode(fn.name(), fn.args().stream().map(a -> transform(a, f, names)).toList());
            case CallNode c -> new CallNode(transform(c.target(), f, names), c.args().stream().map(a -> transform(a, f, names)).toList());
            case UnaryNode u -> new UnaryNode(u.operator(), transform(u.operand(), f, names));
            case BinaryNode b -> new BinaryNode(b.operator(), transform(b.left(), f, names), transform(b.right(), f, names));
            case ParenNode p -> new ParenNode(transform(p.inner(), f, names));
            case ArrayNode a -> new ArrayNode(a.rows().stream().map(row -> row.stream().map(x -> transform(x, f, names)).toList()).toList());
            default -> node;
        };
    }

    private static FormulaNode shiftRef(RefNode r, int dr, int dc) {
        RefPart a = shiftPart(r.first(), dr, dc), b = r.second() == null ? null : shiftPart(r.second(), dr, dc);
        if (a == null || r.second() != null && b == null) return new ErrorNode(CellError.REF);
        return r.withParts(a, b);
    }

    private static RefPart shiftPart(RefPart p, int dr, int dc) {
        int row = p.row(), col = p.column();
        if (!p.wholeColumn() && !p.rowAbsolute()) row += dr;
        if (!p.wholeRow() && !p.columnAbsolute()) col += dc;
        if (!p.wholeColumn() && (row < 0 || row >= CellAddress.MAX_ROWS)) return null;
        if (!p.wholeRow() && (col < 0 || col >= CellAddress.MAX_COLUMNS)) return null;
        return new RefPart(row, col, p.rowAbsolute(), p.columnAbsolute());
    }

    private static FormulaNode mapRef(RefNode r, String sheet, RangeMapper mapper) {
        if (r.is3D()) return r;
        CellRange original = r.range();
        CellRange mapped = mapper.map(sheet, original);
        if (mapped == null) return new ErrorNode(CellError.REF);
        if (mapped.equals(original)) return r;
        RefPart a = r.first(), b = r.second();
        RefPart na = new RefPart(a.wholeColumn() ? -1 : mapped.firstRow(), a.wholeRow() ? -1 : mapped.firstColumn(), a.rowAbsolute(), a.columnAbsolute());
        if (b == null) {
            if (!mapped.isSingleCell()) return r.withParts(na, new RefPart(mapped.lastRow(), mapped.lastColumn(), a.rowAbsolute(), a.columnAbsolute()));
            return r.withParts(na, null);
        }
        RefPart nb = new RefPart(b.wholeColumn() ? -1 : mapped.lastRow(), b.wholeRow() ? -1 : mapped.lastColumn(), b.rowAbsolute(), b.columnAbsolute());
        return r.withParts(na, nb);
    }

    public static RangeMapper insertRows(String sheet, int at, int count) {
        return (s, r) -> {
            if (!s.equalsIgnoreCase(sheet) || r.isWholeColumn()) return r;
            int first = r.firstRow() >= at ? r.firstRow() + count : r.firstRow();
            int last = r.lastRow() >= at ? r.lastRow() + count : r.lastRow();
            if (last >= CellAddress.MAX_ROWS) last = CellAddress.MAX_ROWS - 1;
            if (first >= CellAddress.MAX_ROWS) return null;
            return new CellRange(first, r.firstColumn(), last, r.lastColumn());
        };
    }

    public static RangeMapper deleteRows(String sheet, int at, int count) {
        int end = at + count - 1;
        return (s, r) -> {
            if (!s.equalsIgnoreCase(sheet) || r.isWholeColumn()) return r;
            if (r.firstRow() >= at && r.lastRow() <= end) return null;
            int first = r.firstRow() > end ? r.firstRow() - count : r.firstRow() >= at ? at : r.firstRow();
            int last = r.lastRow() > end ? r.lastRow() - count : r.lastRow() >= at ? at - 1 : r.lastRow();
            if (last < first) return null;
            return new CellRange(first, r.firstColumn(), last, r.lastColumn());
        };
    }

    public static RangeMapper insertColumns(String sheet, int at, int count) {
        return (s, r) -> {
            if (!s.equalsIgnoreCase(sheet) || r.isWholeRow()) return r;
            int first = r.firstColumn() >= at ? r.firstColumn() + count : r.firstColumn();
            int last = r.lastColumn() >= at ? r.lastColumn() + count : r.lastColumn();
            if (last >= CellAddress.MAX_COLUMNS) last = CellAddress.MAX_COLUMNS - 1;
            if (first >= CellAddress.MAX_COLUMNS) return null;
            return new CellRange(r.firstRow(), first, r.lastRow(), last);
        };
    }

    public static RangeMapper deleteColumns(String sheet, int at, int count) {
        int end = at + count - 1;
        return (s, r) -> {
            if (!s.equalsIgnoreCase(sheet) || r.isWholeRow()) return r;
            if (r.firstColumn() >= at && r.lastColumn() <= end) return null;
            int first = r.firstColumn() > end ? r.firstColumn() - count : r.firstColumn() >= at ? at : r.firstColumn();
            int last = r.lastColumn() > end ? r.lastColumn() - count : r.lastColumn() >= at ? at - 1 : r.lastColumn();
            if (last < first) return null;
            return new CellRange(r.firstRow(), first, r.lastRow(), last);
        };
    }

    public static RangeMapper move(String sheet, CellRange source, String targetSheet, int dr, int dc) {
        return (s, r) -> {
            if (!s.equalsIgnoreCase(sheet) || !source.contains(r)) return r;
            return r.offset(dr, dc);
        };
    }
}
