package dtm.stools.component.panels.editor.code.rename;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.api.SymbolKind;
import dtm.stools.i18n.I18n;

import javax.swing.JComponent;
import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class RenameSession {

    private final JComponent owner;
    private final Point anchor;
    private final String currentName;
    private final Range range;
    private final List<Range> occurrences;
    private final List<RenameOption> options;
    private final Function<String, String> validator;
    private final BiConsumer<String, Map<String, Boolean>> committer;
    private final Runnable canceller;
    private final Consumer<RenameSession> inlineStarter;
    private final SymbolKind kind;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    public RenameSession(
            JComponent owner,
            Point anchor,
            String currentName,
            Range range,
            List<Range> occurrences,
            List<RenameOption> options,
            Function<String, String> validator,
            BiConsumer<String, Map<String, Boolean>> committer,
            Runnable canceller,
            Consumer<RenameSession> inlineStarter
    ) {
        this(owner, anchor, currentName, range, occurrences, options, validator, committer, canceller, inlineStarter, null);
    }

    public RenameSession(
            JComponent owner,
            Point anchor,
            String currentName,
            Range range,
            List<Range> occurrences,
            List<RenameOption> options,
            Function<String, String> validator,
            BiConsumer<String, Map<String, Boolean>> committer,
            Runnable canceller,
            Consumer<RenameSession> inlineStarter,
            SymbolKind kind
    ) {
        this.kind = kind;
        this.owner = owner;
        this.anchor = anchor == null ? new Point(0, 0) : new Point(anchor);
        this.currentName = currentName == null ? "" : currentName;
        this.range = range;
        this.occurrences = occurrences == null ? List.of() : List.copyOf(occurrences);
        this.options = options == null ? List.of() : List.copyOf(options);
        this.validator = validator;
        this.committer = committer;
        this.canceller = canceller;
        this.inlineStarter = inlineStarter;
    }

    public JComponent owner() {
        return owner;
    }

    public Point anchor() {
        return new Point(anchor);
    }

    public String currentName() {
        return currentName;
    }

    public Range range() {
        return range;
    }

    public SymbolKind kind() {
        return kind;
    }

    public String kindLabel() {
        if (kind == null) return "";
        return text("rename.kind." + kind.name(), defaultKindLabel(kind));
    }

    public String displayTitle() {
        if (kind == null) {
            return currentName.isEmpty()
                    ? text("rename.title", "Rename")
                    : text("rename.title.named", "Rename '{name}'").replace("{name}", currentName);
        }
        return text("rename.title.kind", "Rename {kind} '{name}'")
                .replace("{kind}", kindLabel())
                .replace("{name}", currentName);
    }

    private static String defaultKindLabel(SymbolKind kind) {
        return switch (kind) {
            case CLASS -> "class";
            case INTERFACE -> "interface";
            case ENUM -> "enum";
            case STRUCT -> "record";
            case METHOD, FUNCTION -> "method";
            case CONSTRUCTOR -> "constructor";
            case FIELD, PROPERTY -> "field";
            case CONSTANT -> "constant";
            case ENUM_MEMBER -> "enum constant";
            case TYPE_PARAMETER -> "type parameter";
            case PACKAGE, NAMESPACE, MODULE -> "package";
            case VARIABLE -> "variable";
            default -> "symbol";
        };
    }

    private static String text(String key, String defaultValue) {
        return I18n.getText(CodeEditorTextArea.class, key, defaultValue);
    }

    public List<Range> occurrences() {
        return occurrences;
    }

    public List<RenameOption> options() {
        return options;
    }

    public Map<String, Boolean> defaultOptionValues() {
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (RenameOption option : options) {
            values.put(option.id(), option.defaultValue());
        }
        return values;
    }

    public boolean supportsInline() {
        return inlineStarter != null && range != null;
    }

    public boolean isFinished() {
        return finished.get();
    }

    public String validate(String newName) {
        if (validator == null) return null;
        try {
            return validator.apply(newName);
        } catch (Exception ex) {
            String message = ex.getMessage();
            return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
        }
    }

    public boolean commit(String newName) {
        return commit(newName, defaultOptionValues());
    }

    public boolean commit(String newName, Map<String, Boolean> optionValues) {
        if (finished.get()) return false;
        String name = newName == null ? "" : newName.trim();
        if (validate(name) != null) return false;
        if (!finished.compareAndSet(false, true)) return false;
        Map<String, Boolean> resolved = defaultOptionValues();
        if (optionValues != null) resolved.putAll(optionValues);
        if (committer != null) committer.accept(name, Map.copyOf(resolved));
        return true;
    }

    public void cancel() {
        if (!finished.compareAndSet(false, true)) return;
        if (canceller != null) canceller.run();
    }

    public void startInline() {
        if (finished.get()) return;
        if (!supportsInline()) {
            new ModernDialogRenamePresenter().present(this);
            return;
        }
        inlineStarter.accept(this);
    }
}
