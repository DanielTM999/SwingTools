package dtm.stools.component.panels.editor.word.controller;

import dtm.stools.utils.FontUtils;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WordFontCatalog {
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private List<String> system;
    private List<String> custom;

    public WordFontCatalog() { system = load(); }

    public List<String> available() { return custom != null ? custom : system; }
    public List<String> systemFonts() { return system; }
    public boolean isCustom() { return custom != null; }
    public boolean allows(String family) {
        if (family == null) return false;
        for (String f : available()) if (f.equalsIgnoreCase(family)) return true;
        return false;
    }
    public void setAvailable(List<String> families) {
        Objects.requireNonNull(families,"families");
        if (families.isEmpty()) throw new IllegalArgumentException("The font list cannot be empty");
        List<String> result = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (String family : families) {
            if (family == null) throw new IllegalArgumentException("Font names cannot be null");
            if (family.isBlank()) throw new IllegalArgumentException("Font names cannot be blank");
            String name = family.strip();
            if (seen.add(name.toLowerCase(Locale.ROOT))) result.add(name);
        }
        custom = List.copyOf(result);
        fire();
    }
    public void reset() { if (custom != null) { custom = null; fire(); } }
    public void refreshSystem() {
        List<String> next = load();
        boolean changed = !next.equals(system);
        system = next;
        if (changed && custom == null) fire();
    }
    public Runnable addListener(Runnable listener) { listeners.add(Objects.requireNonNull(listener)); return () -> listeners.remove(listener); }
    private void fire() { for (Runnable r : listeners) r.run(); }
    private static List<String> load() {
        List<String> result = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (String f : FontUtils.listInstalledFonts()) if (f != null && !f.isBlank() && seen.add(f.toLowerCase(Locale.ROOT))) result.add(f);
        return List.copyOf(result);
    }
}
