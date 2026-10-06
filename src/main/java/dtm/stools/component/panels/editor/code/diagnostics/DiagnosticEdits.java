package dtm.stools.component.panels.editor.code.diagnostics;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import java.util.ArrayList;
import java.util.List;

public final class DiagnosticEdits {
    private DiagnosticEdits() { }
    public static List<Diagnostic> rebase(List<Diagnostic> diagnostics, String before, String after) {
        if (before == null || after == null || before.equals(after)) return List.copyOf(diagnostics);
        int prefix = 0;
        while (prefix < before.length() && prefix < after.length()
                && before.charAt(prefix) == after.charAt(prefix)) prefix++;
        int oldEnd = before.length(), newEnd = after.length();
        while (oldEnd > prefix && newEnd > prefix && before.charAt(oldEnd - 1) == after.charAt(newEnd - 1)) {
            oldEnd--; newEnd--;
        }
        TextBuffer oldBuffer = new TextBuffer(before), newBuffer = new TextBuffer(after);
        List<Diagnostic> result = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            int start = offset(oldBuffer, diagnostic.startLine(), diagnostic.startCol());
            int end = offset(oldBuffer, diagnostic.endLine(), diagnostic.endCol());
            boolean touched = oldEnd == prefix ? start <= prefix && prefix < Math.max(start + 1, end)
                    : start < oldEnd && Math.max(start + 1, end) > prefix;
            if (touched) continue;
            if (start >= oldEnd) { start += newEnd - oldEnd; end += newEnd - oldEnd; }
            result.add(Diagnostic.ofOffset(newBuffer, start, end, diagnostic.severity(), diagnostic.message(),
                    diagnostic.source(), diagnostic.overrideColor()).withUnnecessary(diagnostic.unnecessary()));
        }
        return List.copyOf(result);
    }
    private static int offset(TextBuffer buffer, int line, int col) {
        int safeLine = Math.max(0, Math.min(line, buffer.lineCount() - 1));
        return Math.min(buffer.length(), buffer.offsetOfLine(safeLine) + Math.max(0, Math.min(col, buffer.lineAt(safeLine).length())));
    }
}
