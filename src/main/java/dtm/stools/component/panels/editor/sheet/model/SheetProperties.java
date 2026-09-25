package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

@With
@Builder(toBuilder = true)
public record SheetProperties(String name, List<CellRange> merges, FreezePane freeze, Map<CellAddress, SheetNote> notes, Map<CellAddress, SheetThread> threads,
                              Map<CellAddress, Hyperlink> links, List<DataValidation> validations, List<ConditionalFormat> conditionalFormats,
                              AutoFilter autoFilter, List<FilterView> filterViews, List<SheetTable> tables, List<SheetObject> objects,
                              List<Sparkline> sparklines, List<PivotTable> pivots, SheetProtection protection, List<ProtectedRange> protectedRanges,
                              PrintSettings print, Integer tabColor, SheetVisibility visibility, boolean showGridlines, boolean showHeaders,
                              boolean showFormulas, boolean showZeros, boolean rightToLeft, double zoom, SheetViewMode viewMode, List<Scenario> scenarios,
                              Map<String, byte[]> preserved) {
    public SheetProperties {
        Objects.requireNonNull(name);
        merges = merges == null ? List.of() : List.copyOf(merges);
        freeze = Objects.requireNonNullElse(freeze, FreezePane.NONE);
        notes = notes == null ? Map.of() : Map.copyOf(notes);
        threads = threads == null ? Map.of() : Map.copyOf(threads);
        links = links == null ? Map.of() : Map.copyOf(links);
        validations = validations == null ? List.of() : List.copyOf(validations);
        conditionalFormats = conditionalFormats == null ? List.of() : List.copyOf(conditionalFormats);
        filterViews = filterViews == null ? List.of() : List.copyOf(filterViews);
        tables = tables == null ? List.of() : List.copyOf(tables);
        objects = objects == null ? List.of() : List.copyOf(objects);
        sparklines = sparklines == null ? List.of() : List.copyOf(sparklines);
        pivots = pivots == null ? List.of() : List.copyOf(pivots);
        protection = Objects.requireNonNullElse(protection, SheetProtection.NONE);
        protectedRanges = protectedRanges == null ? List.of() : List.copyOf(protectedRanges);
        print = Objects.requireNonNullElse(print, PrintSettings.DEFAULT);
        visibility = Objects.requireNonNullElse(visibility, SheetVisibility.VISIBLE);
        zoom = zoom <= 0 ? 1 : Math.max(.1, Math.min(4, zoom));
        viewMode = Objects.requireNonNullElse(viewMode, SheetViewMode.NORMAL);
        scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
        preserved = preserved == null ? Map.of() : Map.copyOf(preserved);
    }

    public static SheetProperties named(String name) {
        return new SheetProperties(name, List.of(), FreezePane.NONE, Map.of(), Map.of(), Map.of(), List.of(), List.of(), null, List.of(), List.of(), List.of(),
                List.of(), List.of(), SheetProtection.NONE, List.of(), PrintSettings.DEFAULT, null, SheetVisibility.VISIBLE, true, true, false, true, false, 1,
                SheetViewMode.NORMAL, List.of(), Map.of());
    }

    public SheetProperties withNote(CellAddress a, SheetNote note) { return withNotes(put(notes, a, note)); }
    public SheetProperties withThread(CellAddress a, SheetThread t) { return withThreads(put(threads, a, t)); }
    public SheetProperties withLink(CellAddress a, Hyperlink l) { return withLinks(put(links, a, l)); }
    public SheetProperties addObject(SheetObject o) { return withObjects(add(objects, o)); }
    public SheetProperties addTable(SheetTable t) { return withTables(add(tables, t)); }
    public SheetProperties addMerge(CellRange r) { return withMerges(add(merges, r)); }
    public SheetProperties addValidation(DataValidation v) { return withValidations(add(validations, v)); }
    public SheetProperties addConditionalFormat(ConditionalFormat f) { return withConditionalFormats(add(conditionalFormats, f)); }
    public SheetProperties addSparkline(Sparkline s) { return withSparklines(add(sparklines, s)); }
    public SheetProperties addPivot(PivotTable p) { return withPivots(add(pivots, p)); }

    public SheetProperties replaceObject(String id, UnaryOperator<SheetObject> change) {
        List<SheetObject> list = new ArrayList<>();
        for (SheetObject o : objects) list.add(o.id().equals(id) ? change.apply(o) : o);
        list.removeIf(Objects::isNull);
        return withObjects(list);
    }

    public SheetProperties replaceTable(String tableName, UnaryOperator<SheetTable> change) {
        List<SheetTable> list = new ArrayList<>();
        for (SheetTable t : tables) { SheetTable n = t.nameMatches(tableName) ? change.apply(t) : t; if (n != null) list.add(n); }
        return withTables(list);
    }

    public static <K, V> Map<K, V> put(Map<K, V> map, K key, V value) {
        Map<K, V> m = new HashMap<>(map);
        if (value == null) m.remove(key); else m.put(key, value);
        return m;
    }

    public static <T> List<T> add(List<T> list, T value) { List<T> l = new ArrayList<>(list); l.add(value); return l; }

    public CellRange mergeAt(int row, int column) { for (CellRange m : merges) if (m.contains(row, column)) return m; return null; }
}
