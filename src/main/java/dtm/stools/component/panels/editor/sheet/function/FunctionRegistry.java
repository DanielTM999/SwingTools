package dtm.stools.component.panels.editor.sheet.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class FunctionRegistry {
    private final Map<String, SheetFunction> functions = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    public static FunctionRegistry defaults() {
        FunctionRegistry r = new FunctionRegistry();
        dtm.stools.component.panels.editor.sheet.function.library.Libraries.registerAll(r);
        return r;
    }

    public FunctionRegistry register(SheetFunction f) {
        functions.put(f.name().toUpperCase(Locale.ROOT), Objects.requireNonNull(f));
        return this;
    }

    public FunctionRegistry register(FunctionDefinition.Builder b) { return register(b.build()); }

    public void registerAll(Collection<? extends SheetFunction> list) { for (SheetFunction f : list) register(f); }

    public void alias(String alias, String target) { aliases.put(alias.toUpperCase(Locale.ROOT), target.toUpperCase(Locale.ROOT)); }

    public boolean unregister(String name) { return functions.remove(name.toUpperCase(Locale.ROOT)) != null; }

    public Optional<SheetFunction> find(String name) {
        String u = name.toUpperCase(Locale.ROOT);
        if (u.startsWith("_XLFN.")) u = u.substring(6);
        if (u.startsWith("_XLWS.")) u = u.substring(6);
        if (u.startsWith("__XLUDF.")) u = u.substring(8);
        SheetFunction f = functions.get(u);
        if (f == null) { String a = aliases.get(u); if (a != null) f = functions.get(a); }
        return Optional.ofNullable(f);
    }

    public boolean contains(String name) { return find(name).isPresent(); }
    public int size() { return functions.size(); }

    public List<SheetFunction> all() {
        List<SheetFunction> l = new ArrayList<>(functions.values());
        l.sort(Comparator.comparing(SheetFunction::name));
        return l;
    }

    public List<SheetFunction> byCategory(FunctionCategory c) { return all().stream().filter(f -> f.category() == c).toList(); }

    public FunctionRegistry copy() {
        FunctionRegistry r = new FunctionRegistry();
        r.functions.putAll(functions);
        r.aliases.putAll(aliases);
        return r;
    }

    public static String ooxmlPrefix(SheetFunction f) {
        if (f.origin() == FunctionOrigin.EXCEL_365) return f.name().equals("FILTER") || f.name().equals("SORT") || f.name().equals("SORTBY") || f.name().equals("UNIQUE") ? "_xlfn._xlws." : "_xlfn.";
        return "";
    }
}
