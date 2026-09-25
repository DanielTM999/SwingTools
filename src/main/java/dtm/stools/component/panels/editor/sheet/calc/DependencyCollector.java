package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.formula.ArrayNode;
import dtm.stools.component.panels.editor.sheet.formula.BinaryNode;
import dtm.stools.component.panels.editor.sheet.formula.CallNode;
import dtm.stools.component.panels.editor.sheet.formula.FormulaNode;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.formula.FunctionNode;
import dtm.stools.component.panels.editor.sheet.formula.NameNode;
import dtm.stools.component.panels.editor.sheet.formula.ParenNode;
import dtm.stools.component.panels.editor.sheet.formula.RefNode;
import dtm.stools.component.panels.editor.sheet.formula.StructuredRefNode;
import dtm.stools.component.panels.editor.sheet.formula.UnaryNode;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.function.SheetFunction;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.DefinedName;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class DependencyCollector {
    private final SheetWorkbook workbook;
    private final FunctionRegistry functions;
    private final int hostSheet, hostRow;
    private final List<Dependency> out = new ArrayList<>();
    private final Set<String> visitingNames = new HashSet<>();
    private boolean volatileFlag;

    DependencyCollector(SheetWorkbook workbook, FunctionRegistry functions, int hostSheet, int hostRow) {
        this.workbook = workbook; this.functions = functions; this.hostSheet = hostSheet; this.hostRow = hostRow;
    }

    List<Dependency> dependencies() { return out; }
    boolean isVolatile() { return volatileFlag; }

    void collect(FormulaNode node) { collect(node, Set.of()); }

    private void collect(FormulaNode node, Set<String> locals) {
        switch (node) {
            case RefNode r -> {
                if (r.external() != null) return;
                int s1 = r.sheet() == null ? hostSheet : workbook.indexOf(r.sheet());
                int s2 = r.sheetEnd() == null ? s1 : workbook.indexOf(r.sheetEnd());
                if (s1 < 0 || s2 < 0) return;
                out.add(new Dependency(Math.min(s1, s2), Math.max(s1, s2), r.range()));
            }
            case NameNode n -> name(n, locals);
            case StructuredRefNode s -> structured(s);
            case FunctionNode f -> {
                String name = f.name();
                Optional<SheetFunction> fn = functions.find(name);
                if (fn.isPresent() && fn.get().isVolatile()) volatileFlag = true;
                if (fn.isEmpty()) volatileFlag |= workbook.name(name, hostSheet).isEmpty();
                if (name.equals("LET")) { let(f, locals); return; }
                if (name.equals("LAMBDA")) {
                    Set<String> inner = new HashSet<>(locals);
                    for (int i = 0; i < f.args().size() - 1; i++) if (f.args().get(i) instanceof NameNode p) inner.add(p.name().toUpperCase(Locale.ROOT));
                    if (!f.args().isEmpty()) collect(f.args().getLast(), inner);
                    return;
                }
                for (FormulaNode a : f.args()) collect(a, locals);
                if (fn.isEmpty()) name(new NameNode(null, name), locals);
            }
            case CallNode c -> { collect(c.target(), locals); for (FormulaNode a : c.args()) collect(a, locals); }
            case UnaryNode u -> collect(u.operand(), locals);
            case BinaryNode b -> {
                collect(b.left(), locals); collect(b.right(), locals);
                if (b.operator().equals(":") && (!(b.left() instanceof RefNode) || !(b.right() instanceof RefNode))) volatileFlag = true;
            }
            case ParenNode p -> collect(p.inner(), locals);
            case ArrayNode a -> { for (List<FormulaNode> row : a.rows()) for (FormulaNode n : row) collect(n, locals); }
            default -> { }
        }
    }

    private void let(FunctionNode f, Set<String> locals) {
        Set<String> inner = new HashSet<>(locals);
        List<FormulaNode> args = f.args();
        for (int i = 0; i + 1 < args.size(); i += 2) {
            collect(args.get(i + 1), inner);
            if (args.get(i) instanceof NameNode n) inner.add(n.name().toUpperCase(Locale.ROOT));
        }
        if (!args.isEmpty() && args.size() % 2 == 1) collect(args.getLast(), inner);
    }

    private void name(NameNode n, Set<String> locals) {
        String upper = n.name().toUpperCase(Locale.ROOT);
        if (n.sheet() == null && locals.contains(upper)) return;
        Integer scope = n.sheet() == null ? hostSheet : workbook.indexOf(n.sheet());
        Optional<DefinedName> dn = workbook.name(n.name(), scope);
        if (dn.isPresent()) {
            String key = dn.get().key();
            if (!visitingNames.add(key)) return;
            try { collect(Formulas.parseCanonical(dn.get().formula()), locals); } catch (RuntimeException ignored) { }
            visitingNames.remove(key);
            return;
        }
        Optional<SheetTable> t = workbook.table(n.name());
        if (t.isPresent()) {
            int s = workbook.sheetOfTable(n.name());
            out.add(new Dependency(s, s, t.get().dataRange()));
        }
    }

    private void structured(StructuredRefNode s) {
        SheetTable table = null;
        int sheet = hostSheet;
        if (s.table().isEmpty()) {
            for (SheetTable t : workbook.sheet(hostSheet).properties().tables()) if (t.range().contains(hostRow, t.range().firstColumn())) table = t;
        } else {
            table = workbook.table(s.table()).orElse(null);
            sheet = workbook.sheetOfTable(s.table());
        }
        if (table == null) return;
        CellRange r = table.range();
        out.add(new Dependency(sheet, sheet, s.thisRow() && r.contains(hostRow, r.firstColumn()) ? new CellRange(hostRow, r.firstColumn(), hostRow, r.lastColumn()) : r));
    }
}
