package dtm.stools.component.panels.editor.sheet.function;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.formula.FormulaNode;
import dtm.stools.component.panels.editor.sheet.formula.MissingNode;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.util.List;

public final class FunctionArgs {
    private final FunctionContext context;
    private final List<FormulaNode> nodes;
    private final CellValue[] cache;
    private final CellValue[] preset;

    public FunctionArgs(FunctionContext context, List<FormulaNode> nodes) {
        this.context = context;
        this.nodes = nodes;
        this.cache = new CellValue[nodes.size()];
        this.preset = null;
    }

    public FunctionArgs(FunctionContext context, CellValue[] values) {
        this.context = context;
        this.nodes = null;
        this.cache = values.clone();
        this.preset = values;
    }

    public int size() { return nodes != null ? nodes.size() : preset.length; }
    public FunctionContext context() { return context; }
    public FormulaNode node(int i) { return nodes == null ? null : nodes.get(i); }
    public boolean has(int i) { return i < size() && !isMissing(i); }

    public boolean isMissing(int i) {
        if (i >= size()) return true;
        if (nodes != null) return nodes.get(i) == MissingNode.INSTANCE;
        return preset[i] instanceof OmittedValue;
    }

    public CellValue value(int i) {
        if (i >= size()) return OmittedValue.INSTANCE;
        CellValue v = cache[i];
        if (v == null) {
            v = isMissing(i) ? OmittedValue.INSTANCE : context.evaluate(nodes.get(i));
            cache[i] = v;
        }
        return v;
    }

    public CellValue raw(int i) { return value(i); }

    public CellValue scalar(int i) {
        CellValue v = context.scalar(value(i));
        return v;
    }

    public CellValue deref(int i) { return context.deref(value(i)); }

    public ArrayValue array(int i) { return context.toArray(value(i)); }

    public ReferenceValue reference(int i) {
        if (value(i) instanceof ReferenceValue r) return r;
        throw EvalError.value();
    }

    public boolean isReference(int i) { return value(i) instanceof ReferenceValue; }

    public double number(int i) { return Coerce.number(scalar(i)); }
    public double number(int i, double fallback) { return has(i) ? number(i) : fallback; }
    public String text(int i) { return Coerce.text(scalar(i)); }
    public String text(int i, String fallback) { return has(i) ? text(i) : fallback; }
    public boolean bool(int i) { return Coerce.bool(scalar(i)); }
    public boolean bool(int i, boolean fallback) { return has(i) ? bool(i) : fallback; }
    public int integer(int i) { return Coerce.truncate(scalar(i)); }
    public int integer(int i, int fallback) { return has(i) ? integer(i) : fallback; }

    public void checkErrors() { for (int i = 0; i < size(); i++) Coerce.check(value(i)); }
}
