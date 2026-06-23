package dtm.stools.component.panels.editor.code.api;

import java.util.Collections;
import java.util.List;

/**
 * An editor-offered action for the cursor range (quick fix, refactoring, source action).
 * A CodeAction may carry a list of {@link TextEdit} to apply atomically,
 * a {@link Command} to dispatch, or both (edits run first, then the command).
 */
public record CodeAction(
        String title,
        CodeActionKind kind,
        List<TextEdit> edits,
        Command command,
        boolean preferred
) {
    public CodeAction {
        if (kind == null) kind = CodeActionKind.OTHER;
        if (edits == null) edits = Collections.emptyList();
        else edits = List.copyOf(edits);
    }

    public static CodeAction quickFix(String title, List<TextEdit> edits) {
        return new CodeAction(title, CodeActionKind.QUICK_FIX, edits, null, false);
    }

    public static CodeAction refactor(String title, List<TextEdit> edits) {
        return new CodeAction(title, CodeActionKind.REFACTOR, edits, null, false);
    }

    public static CodeAction command(String title, Command command) {
        return new CodeAction(title, CodeActionKind.OTHER, Collections.emptyList(), command, false);
    }

    public enum CodeActionKind {
        QUICK_FIX,
        REFACTOR,
        REFACTOR_EXTRACT,
        REFACTOR_INLINE,
        REFACTOR_REWRITE,
        SOURCE,
        SOURCE_ORGANIZE_IMPORTS,
        SOURCE_FIX_ALL,
        OTHER
    }
}
