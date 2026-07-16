package dtm.stools.component.panels.editor.code.autocomplete;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AutoCompleteEditApplier {

    private AutoCompleteEditApplier() {
    }

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

    public record Plan(List<ResolvedEdit> leading, List<ResolvedEdit> trailing, int leadingDelta) {
    }

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
