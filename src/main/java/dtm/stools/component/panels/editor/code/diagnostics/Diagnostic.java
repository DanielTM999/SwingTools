package dtm.stools.component.panels.editor.code.diagnostics;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

import java.awt.Color;

public record Diagnostic(
        int startLine,
        int startCol,
        int endLine,
        int endCol,
        DiagnosticSeverity severity,
        String message,
        String source,
        Color overrideColor
) {

    public Diagnostic(int startLine, int startCol, int endLine, int endCol,
                      DiagnosticSeverity severity, String message) {
        this(startLine, startCol, endLine, endCol, severity, message, null, null);
    }

    public Diagnostic(int line, int col, int endCol, DiagnosticSeverity severity, String message) {
        this(line, col, line, endCol, severity, message, null, null);
    }

    public Color effectiveColor() {
        return overrideColor != null ? overrideColor : severity.defaultColor;
    }

    public static Diagnostic ofOffset(TextBuffer buffer, int startOffset, int endOffset,
                                      DiagnosticSeverity severity, String message) {
        return ofOffset(buffer, startOffset, endOffset, severity, message, null, null);
    }

    public static Diagnostic ofOffset(TextBuffer buffer, int startOffset, int endOffset,
                                      DiagnosticSeverity severity, String message,
                                      String source) {
        return ofOffset(buffer, startOffset, endOffset, severity, message, source, null);
    }

    public static Diagnostic ofOffset(TextBuffer buffer, int startOffset, int endOffset,
                                      DiagnosticSeverity severity, String message,
                                      String source, Color overrideColor) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer is required to resolve offsets");
        }
        int s = clamp(startOffset, 0, buffer.length());
        int e = clamp(endOffset, 0, buffer.length());
        if (e < s) {
            int tmp = s;
            s = e;
            e = tmp;
        }
        int startLine = buffer.lineOfOffset(s);
        int endLine = buffer.lineOfOffset(e);
        int startCol = s - buffer.offsetOfLine(startLine);
        int endCol = e - buffer.offsetOfLine(endLine);
        return new Diagnostic(startLine, startCol, endLine, endCol,
                severity, message, source, overrideColor);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
