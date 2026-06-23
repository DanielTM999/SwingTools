package dtm.stools.component.panels.editor.code.autocomplete;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure planning helper for applying an LSP completion item's {@code additionalTextEdits}
 * (e.g. clangd auto-{@code #include}) around the main insertion.
 *
 * <p>Edits are resolved to absolute document offsets by the caller. This class only decides
 * <em>order</em> and the net length shift produced by edits that sit before the insertion point,
 * so the main insertion / snippet session can be realigned. Kept side-effect free so it is unit
 * testable without a live editor.</p>
 */
public final class AutoCompleteEditApplier {

    private AutoCompleteEditApplier() {
    }

    /** An additionalTextEdit resolved to absolute, non-inverted document offsets. */
    public record ResolvedEdit(int start, int end, String newText) {
        public ResolvedEdit {
            if (end < start) {
                int swap = start;
                start = end;
                end = swap;
            }
            newText = newText == null ? "" : newText;
        }

        int lengthDelta() {
            return newText.length() - (end - start);
        }
    }

    /**
     * @param leading edits fully before {@code insertOff}, ordered by start descending (apply as-is).
     * @param trailing edits fully at/after {@code caretOff}, ordered by start descending (apply after
     *                 the main insertion, shifted by leadingDelta + the main edit's own delta).
     * @param leadingDelta net length change contributed by {@code leading}; add it to insertOff/caretOff.
     */
    public record Plan(List<ResolvedEdit> leading, List<ResolvedEdit> trailing, int leadingDelta) {
    }

    /**
     * Partitions resolved edits relative to the replaced region {@code [insertOff, caretOff]}.
     * Edits overlapping that region are dropped (defensive — header insertions never land there).
     */
    public static Plan plan(List<ResolvedEdit> resolved, int insertOff, int caretOff) {
        List<ResolvedEdit> leading = new ArrayList<>();
        List<ResolvedEdit> trailing = new ArrayList<>();
        if (resolved != null) {
            for (ResolvedEdit edit : resolved) {
                if (edit == null) {
                    continue;
                }
                if (edit.end() <= insertOff) {
                    leading.add(edit);
                } else if (edit.start() >= caretOff) {
                    trailing.add(edit);
                }
            }
        }
        Comparator<ResolvedEdit> byStartDesc = Comparator.comparingInt(ResolvedEdit::start).reversed();
        leading.sort(byStartDesc);
        trailing.sort(byStartDesc);
        int delta = 0;
        for (ResolvedEdit edit : leading) {
            delta += edit.lengthDelta();
        }
        return new Plan(leading, trailing, delta);
    }
}
