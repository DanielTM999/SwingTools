package dtm.stools.component.panels.editor.pdf.command;

import dtm.stools.component.panels.editor.pdf.provider.PdfProviderRegistration;

import javax.swing.Action;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class PdfCommands {
    private final Map<String, Action> builtins = new LinkedHashMap<>();
    private final Map<String, Action> custom = new LinkedHashMap<>();
    private final Consumer<Throwable> errors;
    private final BooleanSupplier writable;

    public PdfCommands(Consumer<Throwable> errors, BooleanSupplier writable) {
        this.errors = errors;
        this.writable = writable;
    }

    public PdfAction add(String id, String name, Runnable body) {
        if (contains(id)) throw new IllegalArgumentException("Comando duplicado: " + id);
        PdfAction action = new PdfAction(id, name, body, errors);
        builtins.put(id, action);
        return action;
    }

    public PdfProviderRegistration addCustom(Map<String, Action> actions) {
        for (String id : actions.keySet()) if (contains(id)) throw new IllegalArgumentException("Comando duplicado: " + id);
        custom.putAll(actions);
        List<String> ids = List.copyOf(actions.keySet());
        return () -> ids.forEach(custom::remove);
    }

    public PdfProviderRegistration put(String id, Action action) {
        if (contains(id)) throw new IllegalArgumentException("Comando duplicado: " + id);
        custom.put(id, action);
        return () -> custom.remove(id, action);
    }

    public boolean contains(String id) { return builtins.containsKey(id) || custom.containsKey(id); }

    public Action get(String id) {
        Action action = custom.get(id);
        return action != null ? action : builtins.get(id);
    }

    public Map<String, Action> all() {
        Map<String, Action> all = new LinkedHashMap<>(builtins);
        all.putAll(custom);
        return Collections.unmodifiableMap(all);
    }

    public void refresh() {
        boolean canWrite = writable.getAsBoolean();
        for (Action action : builtins.values()) if (action instanceof PdfAction pdf) pdf.refresh(canWrite);
        for (Action action : custom.values()) if (action instanceof PdfAction pdf) pdf.refresh(canWrite);
    }
}
