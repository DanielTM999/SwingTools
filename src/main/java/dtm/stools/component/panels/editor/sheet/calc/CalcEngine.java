package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.api.CalcMode;
import dtm.stools.component.panels.editor.sheet.api.ProviderRegistration;
import dtm.stools.component.panels.editor.sheet.command.SheetChange;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.FormulaNode;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.function.CellConsumer;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.provider.SheetExternalDataProvider;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;

public class CalcEngine {
    private static final class SheetState {
        final Map<Long, FormulaCell> formulas = new HashMap<>();
        final Map<Long, FormulaCell> spillOwner = new HashMap<>();
        final Set<FormulaCell> anchors = new LinkedHashSet<>();
        CellRange extent;
        boolean extentValid;
    }

    private final FunctionRegistry functions;
    private final List<SheetState> sheets = new ArrayList<>();
    private final DependencyIndex index = new DependencyIndex();
    private final LinkedHashSet<FormulaCell> dirty = new LinkedHashSet<>();
    private final Set<FormulaCell> volatiles = new LinkedHashSet<>();
    private final List<CalcListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, List<CellRange>> changed = new LinkedHashMap<>();
    private final List<String> circular = new ArrayList<>();
    private final Set<FormulaCell> cycleCells = new LinkedHashSet<>();
    private SheetWorkbook workbook = SheetWorkbook.create();
    private CalcMode mode = CalcMode.AUTOMATIC;
    private IterationSettings iteration = IterationSettings.DISABLED;
    private FormulaLocale formulaLocale = FormulaLocale.PT_BR;
    private Locale locale = Locale.forLanguageTag("pt-BR");
    private NumberFormatter formatter = new NumberFormatter();
    private Random random = new Random();
    private LongSupplier clock = System::currentTimeMillis;
    private SheetExternalDataProvider external;
    private int evaluatedCount;
    private boolean calculating;
    private long frozenNow = -1;

    public CalcEngine() { this(FunctionRegistry.defaults()); }
    public CalcEngine(FunctionRegistry functions) { this.functions = Objects.requireNonNull(functions); }

    public FunctionRegistry functions() { return functions; }
    public SheetWorkbook workbook() { return workbook; }
    public CalcMode mode() { return mode; }
    public IterationSettings iteration() { return iteration; }
    public FormulaLocale formulaLocale() { return formulaLocale; }
    public Locale locale() { return locale; }
    public NumberFormatter formatter() { return formatter; }
    public SheetExternalDataProvider external() { return external; }
    public List<String> circularReferences() { return List.copyOf(circular); }
    public boolean hasPendingCalculation() { return !dirty.isEmpty(); }
    public int pendingCount() { return dirty.size(); }

    public void setMode(CalcMode value) { mode = Objects.requireNonNull(value); if (mode != CalcMode.MANUAL) calculate(); }
    public void setIteration(IterationSettings value) { iteration = Objects.requireNonNull(value); }
    public void setLocale(Locale value) {
        locale = Objects.requireNonNull(value);
        formulaLocale = FormulaLocale.of(value);
        formatter = new NumberFormatter(value, workbook.properties().date1904());
    }
    public void setRandomSeed(long seed) { random = new Random(seed); }
    public void setClock(LongSupplier value) { clock = Objects.requireNonNull(value); }
    public void setExternalDataProvider(SheetExternalDataProvider value) { external = value; }
    public double random() { return random.nextDouble(); }
    public long now() { return frozenNow >= 0 ? frozenNow : clock.getAsLong(); }

    public ProviderRegistration addListener(CalcListener l) { listeners.add(Objects.requireNonNull(l)); return () -> listeners.remove(l); }

    public void attach(SheetWorkbook value) {
        workbook = Objects.requireNonNull(value);
        formatter = new NumberFormatter(locale, workbook.properties().date1904());
        rebuild();
    }

    public void rebuild() {
        sheets.clear();
        index.clear();
        dirty.clear();
        volatiles.clear();
        for (int s = 0; s < workbook.sheetCount(); s++) {
            SheetState state = new SheetState();
            sheets.add(state);
            int sheetIndex = s;
            workbook.sheet(s).cells().forEach((r, c, cell) -> { if (cell.hasFormula()) register(sheetIndex, r, c, cell); });
        }
        changedAll();
        if (mode != CalcMode.MANUAL) calculate();
    }

    public void onChange(SheetChange change) {
        if (change.structural() || sheets.size() != workbook.sheetCount()) { rebuild(); return; }
        Set<FormulaCell> seeds = new LinkedHashSet<>();
        for (Map.Entry<String, List<CellRange>> e : change.touched().entrySet()) {
            int s = workbook.indexOfId(e.getKey());
            if (s < 0) continue;
            SheetState st = sheets.get(s);
            st.extentValid = false;
            for (CellRange r : e.getValue()) {
                if (r.cellCount() > 4096) {
                    Set<Long> keys = new HashSet<>();
                    workbook.sheet(s).cells().forEach(r, (row, col, cell) -> keys.add(CellAddress.key(row, col)));
                    for (FormulaCell f : new ArrayList<>(st.formulas.values())) if (r.contains(f.row, f.column)) keys.add(f.key());
                    for (long k : keys) cellChanged(s, CellAddress.keyRow(k), CellAddress.keyColumn(k), seeds);
                    index.dependents(s, r, seeds);
                    for (FormulaCell a : new ArrayList<>(st.anchors)) if (a.desiredSpill != null && a.desiredSpill.intersects(r) || a.spill != null && a.spill.intersects(r)) seeds.add(a);
                    record(s, r);
                } else {
                    for (int row = r.firstRow(); row <= r.lastRow(); row++) for (int col = r.firstColumn(); col <= r.lastColumn(); col++) cellChanged(s, row, col, seeds);
                    record(s, r);
                }
            }
        }
        markDirty(seeds);
        if (mode != CalcMode.MANUAL) calculate(); else fireValues();
    }

    public void cellsChanged(int sheet, Collection<CellAddress> cells) {
        Set<FormulaCell> seeds = new LinkedHashSet<>();
        sheets.get(sheet).extentValid = false;
        for (CellAddress a : cells) { cellChanged(sheet, a.row(), a.column(), seeds); record(sheet, CellRange.of(a)); }
        markDirty(seeds);
        if (mode != CalcMode.MANUAL) calculate();
    }

    private void cellChanged(int s, int row, int col, Set<FormulaCell> seeds) {
        SheetState st = sheets.get(s);
        long key = CellAddress.key(row, col);
        SheetCell cell = workbook.sheet(s).cells().get(row, col);
        FormulaCell old = st.formulas.get(key);
        if (cell != null && cell.hasFormula()) {
            if (old == null || !old.text.equals(cell.formula())) {
                if (old != null) unregister(s, old);
                FormulaCell f = register(s, row, col, cell);
                seeds.add(f);
            }
        } else if (old != null) {
            unregister(s, old);
        }
        index.dependents(s, row, col, seeds);
        for (FormulaCell a : st.anchors) {
            if (a.row == row && a.column == col) continue;
            if (a.desiredSpill != null && a.desiredSpill.contains(row, col) || a.spill != null && a.spill.contains(row, col)) seeds.add(a);
        }
    }

    private FormulaCell register(int s, int row, int col, SheetCell cell) {
        FormulaNode ast = null;
        CellValue parseError = null;
        try { ast = Formulas.parseCanonical(cell.formula()); } catch (RuntimeException e) { parseError = CellValue.error(CellError.NAME); }
        FormulaCell f = new FormulaCell(s, row, col, cell.formula(), ast, parseError);
        if (!cell.value().isEmpty()) f.value = cell.value();
        if (ast != null) {
            DependencyCollector dc = new DependencyCollector(workbook, functions, s, row);
            dc.collect(ast);
            f.dependencies = List.copyOf(dc.dependencies());
            f.volatileCell = dc.isVolatile();
        }
        SheetState st = sheets.get(s);
        st.formulas.put(f.key(), f);
        index.add(f);
        if (f.volatileCell) volatiles.add(f);
        f.state = FormulaCell.DIRTY;
        dirty.add(f);
        st.extentValid = false;
        return f;
    }

    private void unregister(int s, FormulaCell f) {
        SheetState st = sheets.get(s);
        st.formulas.remove(f.key());
        index.remove(f);
        volatiles.remove(f);
        dirty.remove(f);
        clearSpill(s, f);
        st.anchors.remove(f);
        f.state = FormulaCell.CLEAN;
        st.extentValid = false;
    }

    private void markDirty(Set<FormulaCell> seeds) {
        Deque<FormulaCell> queue = new ArrayDeque<>(seeds);
        Set<FormulaCell> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            FormulaCell f = queue.poll();
            if (!visited.add(f)) continue;
            if (sheets.get(f.sheet).formulas.get(f.key()) != f) continue;
            f.state = FormulaCell.DIRTY;
            dirty.add(f);
            Set<FormulaCell> deps = new LinkedHashSet<>();
            index.dependents(f.sheet, f.row, f.column, deps);
            if (f.spill != null) index.dependents(f.sheet, f.spill, deps);
            for (FormulaCell d : deps) if (!visited.contains(d)) queue.add(d);
        }
    }

    public void calculate() {
        if (calculating) return;
        for (FormulaCell v : volatiles) { v.state = FormulaCell.DIRTY; dirty.add(v); }
        if (dirty.isEmpty()) { fireValues(); return; }
        run();
    }

    public void calculateFull() {
        for (SheetState st : sheets) for (FormulaCell f : st.formulas.values()) { f.state = FormulaCell.DIRTY; dirty.add(f); }
        changedAll();
        run();
    }

    private void run() {
        calculating = true;
        long start = System.nanoTime();
        evaluatedCount = 0;
        circular.clear();
        cycleCells.clear();
        ValueParser previous = ValueParser.current();
        ValueParser.setCurrent(new ValueParser(locale, workbook.properties().date1904()));
        frozenNow = clock.getAsLong();
        for (CalcListener l : listeners) l.calculationStarted(dirty.size());
        try {
            int guard = 0, limit = Math.max(10_000, (int) Math.min(Integer.MAX_VALUE / 4, totalFormulas() * 20L));
            while (!dirty.isEmpty() && guard++ < limit) evaluateFrom(dirty.getFirst());
            if (!cycleCells.isEmpty() && iteration.enabled()) iterate();
            dirty.removeIf(f -> f.state == FormulaCell.CLEAN);
        } finally {
            frozenNow = -1;
            ValueParser.setCurrent(previous);
            calculating = false;
        }
        CalcStatistics stats = new CalcStatistics(evaluatedCount, System.nanoTime() - start, circular, !dirty.isEmpty());
        fireValues();
        for (CalcListener l : listeners) l.calculationFinished(stats);
    }

    private int totalFormulas() { int n = 0; for (SheetState s : sheets) n += s.formulas.size(); return n; }

    private void evaluateFrom(FormulaCell root) {
        Deque<FormulaCell> stack = new ArrayDeque<>();
        stack.push(root);
        root.visit = 0;
        while (!stack.isEmpty()) {
            FormulaCell f = stack.peek();
            if (f.state == FormulaCell.CLEAN) { stack.pop(); continue; }
            if (f.state == FormulaCell.DIRTY) {
                f.state = FormulaCell.VISITING;
                List<FormulaCell> pending = dirtyPrecedents(f);
                boolean pushed = false;
                for (FormulaCell p : pending) {
                    if (p.state == FormulaCell.VISITING) { registerCycle(f, p); continue; }
                    if (p.state == FormulaCell.DIRTY) { stack.push(p); pushed = true; }
                }
                if (pushed) continue;
            }
            stack.pop();
            compute(f);
        }
    }

    private void registerCycle(FormulaCell f, FormulaCell p) {
        cycleCells.add(f); cycleCells.add(p);
        String label = workbook.sheet(p.sheet).name() + "!" + p.address().toA1();
        if (!circular.contains(label)) circular.add(label);
    }

    private List<FormulaCell> dirtyPrecedents(FormulaCell f) {
        List<FormulaCell> out = new ArrayList<>();
        if (f.dependencies.isEmpty() || dirty.isEmpty()) return out;
        for (Dependency d : f.dependencies) {
            for (int s = d.sheet(); s <= d.sheetEnd() && s < sheets.size(); s++) {
                SheetState st = sheets.get(s);
                CellRange r = d.range();
                if (r.cellCount() <= 64 || r.cellCount() <= st.formulas.size()) {
                    if (r.cellCount() <= 4096) {
                        for (int row = r.firstRow(); row <= r.lastRow(); row++)
                            for (int col = r.firstColumn(); col <= r.lastColumn(); col++) {
                                FormulaCell p = st.formulas.get(CellAddress.key(row, col));
                                if (p != null && p != f && p.state != FormulaCell.CLEAN) out.add(p);
                            }
                    } else {
                        int sheetIndex = s;
                        workbook.sheet(s).cells().forEach(r, (row, col, cell) -> {
                            if (!cell.hasFormula()) return;
                            FormulaCell p = sheets.get(sheetIndex).formulas.get(CellAddress.key(row, col));
                            if (p != null && p != f && p.state != FormulaCell.CLEAN) out.add(p);
                        });
                    }
                } else {
                    for (FormulaCell p : dirty) if (p != f && p.sheet == s && r.contains(p.row, p.column) && p.state != FormulaCell.CLEAN) out.add(p);
                }
                for (FormulaCell a : st.anchors) {
                    if (a == f || a.state == FormulaCell.CLEAN) continue;
                    CellRange sp = a.spill != null ? a.spill : a.desiredSpill;
                    if (sp != null && sp.intersects(r)) out.add(a);
                }
            }
        }
        return out;
    }

    private void compute(FormulaCell f) {
        SheetState st = sheets.get(f.sheet);
        if (st.formulas.get(f.key()) != f) { f.state = FormulaCell.CLEAN; dirty.remove(f); return; }
        f.state = FormulaCell.VISITING;
        CellValue before = f.value;
        CellRange spillBefore = f.spill;
        CellValue result;
        if (f.ast == null) result = f.parseError;
        else {
            SheetCell cell = workbook.sheet(f.sheet).cells().get(f.row, f.column);
            result = new Evaluator(this, f.sheet, f.row, f.column).evaluateTop(f.ast);
            if (result instanceof ErrorValue e && e.error() == CellError.NAME && cell != null && !cell.value().isEmpty()) result = cell.value();
        }
        evaluatedCount++;
        applyResult(f, result);
        f.state = FormulaCell.CLEAN;
        dirty.remove(f);
        if (!Objects.equals(before, f.value) || !Objects.equals(spillBefore, f.spill)) {
            record(f.sheet, CellRange.of(f.row, f.column));
            Set<FormulaCell> deps = new LinkedHashSet<>();
            if (spillBefore != null) { record(f.sheet, spillBefore); if (!spillBefore.equals(f.spill)) index.dependents(f.sheet, spillBefore, deps); }
            if (f.spill != null) { record(f.sheet, f.spill); index.dependents(f.sheet, f.spill, deps); }
            index.dependents(f.sheet, f.row, f.column, deps);
            for (FormulaCell d : deps) {
                if (d == f || d.state == FormulaCell.VISITING) continue;
                if (d.state == FormulaCell.CLEAN) { d.state = FormulaCell.DIRTY; dirty.add(d); }
            }
        }
    }

    private void applyResult(FormulaCell f, CellValue result) {
        SheetState st = sheets.get(f.sheet);
        if (result instanceof ArrayValue a && a.size() > 1) {
            if (f.row + a.rows() > CellAddress.MAX_ROWS || f.column + a.columns() > CellAddress.MAX_COLUMNS) {
                clearSpill(f.sheet, f);
                f.value = CellValue.error(CellError.SPILL);
                f.desiredSpill = null;
                st.anchors.add(f);
                return;
            }
            CellRange range = new CellRange(f.row, f.column, f.row + a.rows() - 1, f.column + a.columns() - 1);
            boolean blocked = false;
            SheetWorksheet ws = workbook.sheet(f.sheet);
            if (range.cellCount() <= 1_000_000) {
                boolean[] hit = {false};
                ws.cells().forEach(range, (r, c, cell) -> { if ((r != f.row || c != f.column) && cell.hasContent()) hit[0] = true; });
                blocked = hit[0];
                if (!blocked) for (FormulaCell other : st.anchors) {
                    if (other == f || other.spill == null) continue;
                    if (other.spill.intersects(range)) { blocked = true; break; }
                }
                if (!blocked) for (CellRange m : ws.properties().merges()) if (m.intersects(range)) { blocked = true; break; }
            } else blocked = true;
            clearSpill(f.sheet, f);
            st.anchors.add(f);
            f.desiredSpill = range;
            if (blocked) { f.value = CellValue.error(CellError.SPILL); return; }
            f.value = a;
            f.spill = range;
            for (int r = range.firstRow(); r <= range.lastRow(); r++)
                for (int c = range.firstColumn(); c <= range.lastColumn(); c++)
                    if (r != f.row || c != f.column) st.spillOwner.put(CellAddress.key(r, c), f);
            st.extentValid = false;
            return;
        }
        if (result instanceof ArrayValue a) result = a.get(0, 0);
        if (f.spill != null) clearSpill(f.sheet, f);
        f.desiredSpill = null;
        st.anchors.remove(f);
        f.value = result;
    }

    private void clearSpill(int s, FormulaCell f) {
        if (f.spill == null) return;
        SheetState st = sheets.get(s);
        CellRange r = f.spill;
        for (int row = r.firstRow(); row <= r.lastRow(); row++)
            for (int col = r.firstColumn(); col <= r.lastColumn(); col++) {
                long k = CellAddress.key(row, col);
                if (st.spillOwner.get(k) == f) st.spillOwner.remove(k);
            }
        f.spill = null;
        st.extentValid = false;
    }

    private void iterate() {
        List<FormulaCell> cells = new ArrayList<>(cycleCells);
        for (int i = 0; i < iteration.maxIterations(); i++) {
            double maxDelta = 0;
            for (FormulaCell f : cells) {
                CellValue before = f.value;
                CellValue result = new Evaluator(this, f.sheet, f.row, f.column).evaluateTop(f.ast);
                applyResult(f, result);
                if (before instanceof NumberValue a && f.value instanceof NumberValue b) maxDelta = Math.max(maxDelta, Math.abs(a.value() - b.value()));
                else if (!Objects.equals(before, f.value)) maxDelta = Double.MAX_VALUE;
                record(f.sheet, CellRange.of(f.row, f.column));
            }
            if (maxDelta <= iteration.maxChange()) break;
        }
        circular.clear();
    }

    CellValue valueForEvaluation(int s, int row, int col) {
        if (s < 0 || s >= sheets.size()) throw EvalError.ref();
        SheetState st = sheets.get(s);
        long key = CellAddress.key(row, col);
        FormulaCell f = st.formulas.get(key);
        if (f != null) {
            if (f.state == FormulaCell.DIRTY) evaluateFrom(f);
            else if (f.state == FormulaCell.VISITING) registerCycle(f, f);
            return anchorValue(f);
        }
        FormulaCell owner = st.spillOwner.get(key);
        if (owner != null) {
            if (owner.state == FormulaCell.DIRTY) evaluateFrom(owner);
            if (owner.value instanceof ArrayValue a && owner.spill != null && owner.spill.contains(row, col)) return a.get(row - owner.row, col - owner.column);
            return CellValue.EMPTY;
        }
        SheetCell cell = workbook.sheet(s).cells().get(row, col);
        return cell == null ? CellValue.EMPTY : cell.value();
    }

    private static CellValue anchorValue(FormulaCell f) {
        if (f.value instanceof ArrayValue a) return a.get(0, 0);
        return f.value;
    }

    public CellValue valueAt(int s, int row, int col) {
        if (s < 0 || s >= sheets.size()) return CellValue.EMPTY;
        SheetState st = sheets.get(s);
        long key = CellAddress.key(row, col);
        FormulaCell f = st.formulas.get(key);
        if (f != null) return anchorValue(f);
        FormulaCell owner = st.spillOwner.get(key);
        if (owner != null && owner.value instanceof ArrayValue a && owner.spill != null) return a.get(row - owner.row, col - owner.column);
        SheetCell cell = workbook.sheet(s).cells().get(row, col);
        return cell == null ? CellValue.EMPTY : cell.value();
    }

    public CellValue valueAt(int s, CellAddress a) { return valueAt(s, a.row(), a.column()); }

    public CellValue evaluate(int sheet, CellAddress host, String canonicalFormula) {
        FormulaNode ast = Formulas.parseCanonical(canonicalFormula);
        boolean wasCalculating = calculating;
        calculating = true;
        try { return new Evaluator(this, sheet, host.row(), host.column()).evaluateTop(ast); }
        finally { calculating = wasCalculating; }
    }

    public CellValue evaluateRaw(int sheet, CellAddress host, FormulaNode ast) {
        return new Evaluator(this, sheet, host.row(), host.column()).evaluate(ast);
    }

    public Optional<FormulaCell> formulaCell(int s, int row, int col) {
        if (s < 0 || s >= sheets.size()) return Optional.empty();
        return Optional.ofNullable(sheets.get(s).formulas.get(CellAddress.key(row, col)));
    }

    public boolean isSpilled(int s, int row, int col) { return s >= 0 && s < sheets.size() && sheets.get(s).spillOwner.containsKey(CellAddress.key(row, col)); }

    public Optional<FormulaCell> spillAnchor(int s, int row, int col) {
        if (s < 0 || s >= sheets.size()) return Optional.empty();
        SheetState st = sheets.get(s);
        FormulaCell f = st.formulas.get(CellAddress.key(row, col));
        if (f != null && f.spill != null) return Optional.of(f);
        return Optional.ofNullable(st.spillOwner.get(CellAddress.key(row, col)));
    }

    public CellRange spillRange(int s, int row, int col) {
        if (s < 0 || s >= sheets.size()) return null;
        FormulaCell f = sheets.get(s).formulas.get(CellAddress.key(row, col));
        if (f == null) return null;
        if (f.state == FormulaCell.DIRTY) evaluateFrom(f);
        if (f.spill != null) return f.spill;
        return f.value instanceof ErrorValue ? null : CellRange.of(row, col);
    }

    public List<CellRange> spillRanges(int s) {
        if (s < 0 || s >= sheets.size()) return List.of();
        List<CellRange> list = new ArrayList<>();
        for (FormulaCell a : sheets.get(s).anchors) if (a.spill != null) list.add(a.spill);
        return list;
    }

    public List<Dependency> precedents(int s, int row, int col) { return formulaCell(s, row, col).map(FormulaCell::dependencies).orElse(List.of()); }

    public List<CellAddress> dependents(int s, int row, int col) {
        Set<FormulaCell> out = new LinkedHashSet<>();
        index.dependents(s, row, col, out);
        List<CellAddress> list = new ArrayList<>();
        for (FormulaCell f : out) if (f.sheet == s) list.add(f.address());
        return list;
    }

    public Set<FormulaCell> dependentCells(int s, CellRange range) { return index.dependents(s, range); }

    CellRange extent(int s) {
        if (s < 0 || s >= sheets.size()) return null;
        SheetState st = sheets.get(s);
        if (!st.extentValid) {
            CellRange r = workbook.sheet(s).cells().usedRange();
            for (FormulaCell a : st.anchors) if (a.spill != null) r = r == null ? a.spill : r.union(a.spill);
            st.extent = r;
            st.extentValid = true;
        }
        return st.extent;
    }

    public CellRange usedRange(int s) { return extent(s); }

    void fill(int s, CellRange range, ArrayValue out) {
        SheetState st = sheets.get(s);
        int r0 = range.firstRow(), c0 = range.firstColumn();
        if (range.cellCount() <= 4096) {
            for (int r = range.firstRow(); r <= range.lastRow(); r++)
                for (int c = range.firstColumn(); c <= range.lastColumn(); c++) out.set(r - r0, c - c0, valueForEvaluation(s, r, c));
            return;
        }
        workbook.sheet(s).cells().forEach(range, (r, c, cell) -> out.set(r - r0, c - c0, cell.hasFormula() ? valueForEvaluation(s, r, c) : cell.value()));
        for (FormulaCell a : new ArrayList<>(st.anchors)) {
            if (a.state == FormulaCell.DIRTY) evaluateFrom(a);
            if (a.spill == null || !a.spill.intersects(range) || !(a.value instanceof ArrayValue arr)) continue;
            CellRange i = a.spill.intersection(range);
            for (int r = i.firstRow(); r <= i.lastRow(); r++)
                for (int c = i.firstColumn(); c <= i.lastColumn(); c++) out.set(r - r0, c - c0, arr.get(r - a.row, c - a.column));
        }
    }

    void forEachValue(int s, CellRange area, CellConsumer consumer) {
        if (s < 0 || s >= sheets.size()) throw EvalError.ref();
        SheetState st = sheets.get(s);
        CellRange used = extent(s);
        if (used == null) return;
        CellRange clipped = area.intersection(used);
        if (clipped == null) return;
        workbook.sheet(s).cells().forEach(clipped, (r, c, cell) -> {
            CellValue v = cell.hasFormula() ? valueForEvaluation(s, r, c) : cell.value();
            if (!v.isEmpty()) consumer.accept(s, r, c, v);
        });
        for (FormulaCell a : new ArrayList<>(st.anchors)) {
            if (a.state == FormulaCell.DIRTY) evaluateFrom(a);
            if (a.spill == null || !a.spill.intersects(clipped) || !(a.value instanceof ArrayValue arr)) continue;
            CellRange i = a.spill.intersection(clipped);
            for (int r = i.firstRow(); r <= i.lastRow(); r++)
                for (int c = i.firstColumn(); c <= i.lastColumn(); c++) {
                    if (r == a.row && c == a.column) continue;
                    CellValue v = arr.get(r - a.row, c - a.column);
                    if (!v.isEmpty()) consumer.accept(s, r, c, v);
                }
        }
    }

    private void record(int s, CellRange r) {
        if (s < 0 || s >= workbook.sheetCount()) return;
        List<CellRange> list = changed.computeIfAbsent(workbook.sheet(s).id(), k -> new ArrayList<>());
        if (list.size() < 256) list.add(r);
        else { CellRange b = list.getFirst(); for (CellRange x : list) b = b.union(x); list.clear(); list.add(b.union(r)); }
    }

    private void changedAll() {
        for (SheetWorksheet s : workbook.sheets()) changed.put(s.id(), new ArrayList<>(List.of(CellRange.all())));
    }

    private void fireValues() {
        if (changed.isEmpty()) return;
        Map<String, List<CellRange>> copy = new LinkedHashMap<>();
        changed.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        changed.clear();
        for (CalcListener l : listeners) l.valuesChanged(copy);
    }

    public Map<CellAddress, CellValue> snapshotValues(int s) {
        Map<CellAddress, CellValue> map = new LinkedHashMap<>();
        if (s < 0 || s >= sheets.size()) return map;
        for (FormulaCell f : sheets.get(s).formulas.values()) map.put(f.address(), anchorValue(f));
        return map;
    }
}
