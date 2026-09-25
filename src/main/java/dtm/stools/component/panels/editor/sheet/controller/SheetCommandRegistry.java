package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.ProviderRegistration;
import dtm.stools.component.panels.editor.sheet.provider.SheetCommandEntry;
import dtm.stools.component.panels.editor.sheet.ui.SheetRibbon;

import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class SheetCommandRegistry {
    public static final String GROUP = "sheet.group";
    public static final String SHORTCUT = "sheet.shortcut";

    private final SheetEditor editor;
    private final Map<String, Action> actions = new LinkedHashMap<>();
    private final Map<String, Action> view = Collections.unmodifiableMap(actions);
    private final Map<String, List<KeyStroke>> keys = new HashMap<>();
    private boolean running;

    public SheetCommandRegistry(SheetEditor editor) { this.editor = editor; }

    public Map<String, Action> actions() { return view; }

    public SheetAction add(String id, String name, String icon, String group, boolean edit, Runnable body) {
        SheetAction a = new SheetAction(this, id, name, edit, body);
        if (icon != null) a.putValue(SheetRibbon.ICON, icon);
        a.putValue(GROUP, group);
        actions.put(id, a);
        return a;
    }

    public SheetAction toggle(String id, String name, String icon, String group, boolean edit, BooleanSupplier selected, Runnable body) {
        return add(id, name, icon, group, edit, body).selected(selected);
    }

    public SheetAction menu(String id, String name, String icon, String group, String... ids) {
        SheetAction a = add(id, name, icon, group, false, null);
        a.menu(List.of(ids));
        return a;
    }

    public ProviderRegistration register(String id, Action action, String group) {
        if (actions.containsKey(id)) throw new IllegalArgumentException("Comando já registrado: " + id);
        action.putValue(Action.ACTION_COMMAND_KEY, id);
        if (action.getValue(GROUP) == null) action.putValue(GROUP, group);
        actions.put(id, action);
        return () -> { if (actions.get(id) == action) actions.remove(id); List<KeyStroke> ks = keys.remove(id); if (ks != null) for (KeyStroke k : ks) editor.getCanvas().unbind(k); };
    }

    public void bind(String id, KeyStroke... strokes) {
        Action a = actions.get(id);
        if (a == null) return;
        strokes = java.util.Arrays.stream(strokes).filter(java.util.Objects::nonNull).toArray(KeyStroke[]::new);
        for (KeyStroke k : strokes) {
            editor.getCanvas().bind(id, k, a);
            keys.computeIfAbsent(id, x -> new ArrayList<>()).add(k);
        }
        if (strokes.length > 0) {
            if (a.getValue(Action.ACCELERATOR_KEY) == null) a.putValue(Action.ACCELERATOR_KEY, strokes[0]);
            a.putValue(SHORTCUT, describe(strokes[0]));
            Object tip = a.getValue(Action.NAME);
            a.putValue(Action.SHORT_DESCRIPTION, tip + " (" + describe(strokes[0]) + ")");
        }
    }

    public static KeyStroke key(int code, int modifiers) { return KeyStroke.getKeyStroke(code, modifiers); }
    public static KeyStroke ctrl(int code) { return KeyStroke.getKeyStroke(code, InputEvent.CTRL_DOWN_MASK); }
    public static KeyStroke ctrlShift(int code) { return KeyStroke.getKeyStroke(code, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK); }
    public static KeyStroke shift(int code) { return KeyStroke.getKeyStroke(code, InputEvent.SHIFT_DOWN_MASK); }
    public static KeyStroke alt(int code) { return KeyStroke.getKeyStroke(code, InputEvent.ALT_DOWN_MASK); }
    public static KeyStroke plain(int code) { return KeyStroke.getKeyStroke(code, 0); }

    public static String describe(KeyStroke k) {
        String mods = InputEvent.getModifiersExText(k.getModifiers());
        String key = KeyEvent.getKeyText(k.getKeyCode());
        return mods.isEmpty() ? key : mods + "+" + key;
    }

    public boolean execute(String id) {
        Action a = actions.get(id);
        if (a == null || !a.isEnabled()) return false;
        a.actionPerformed(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, id));
        return true;
    }

    void perform(SheetAction a) {
        if (!a.isEnabled() || running && a.body() == null) return;
        if (a.getValue(SheetRibbon.MENU) instanceof List<?> ids) { editor.popups().showCommandMenu(ids); return; }
        if (a.isEdit() && editor.isReadOnlyView()) { editor.popups().info("Somente leitura", "A pasta de trabalho está aberta somente para leitura."); return; }
        if (!a.runsWhileEditing() && editor.isEditing() && !editor.commitEditingIfActive()) return;
        running = true;
        try {
            a.body().run();
        } catch (RuntimeException failure) {
            editor.reportError(failure);
        } finally {
            running = false;
        }
        refresh();
        if (!a.runsWhileEditing() && !editor.isEditing() && editor.isShowing() && !editor.isClosed()) editor.focusGrid();
    }

    public void refresh() {
        boolean readOnly = editor.isReadOnlyView();
        for (Action action : actions.values()) {
            if (!(action instanceof SheetAction a)) continue;
            boolean enabled = !(a.isEdit() && readOnly);
            if (enabled && a.availability() != null) {
                try { enabled = a.availability().getAsBoolean(); } catch (RuntimeException ignored) { enabled = false; }
            }
            if (a.isEnabled() != enabled) a.setEnabled(enabled);
            if (a.selectedState() != null) {
                boolean on;
                try { on = a.selectedState().getAsBoolean(); } catch (RuntimeException ignored) { on = false; }
                if (!Boolean.valueOf(on).equals(a.getValue(Action.SELECTED_KEY))) a.putValue(Action.SELECTED_KEY, on);
            }
        }
    }

    public List<SheetCommandEntry> entries() {
        List<SheetCommandEntry> list = new ArrayList<>();
        for (Map.Entry<String, Action> e : actions.entrySet()) {
            Action a = e.getValue();
            if (a.getValue(SheetRibbon.MENU) != null || Boolean.TRUE.equals(a.getValue("sheet.hidden"))) continue;
            Object shortcut = a.getValue(SHORTCUT);
            list.add(new SheetCommandEntry(e.getKey(), String.valueOf(a.getValue(Action.NAME)), String.valueOf(a.getValue(GROUP)), a.isEnabled(), shortcut == null ? "" : shortcut.toString()));
        }
        return list;
    }
}
