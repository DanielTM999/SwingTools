package dtm.stools.component.panels.editor.code;

import java.util.Arrays;

final class LineMetricsCache {

    static final int UNKNOWN = -1;

    private int[] values = new int[0];
    private int size = -1;

    boolean isValidFor(int lineCount) {
        return size == lineCount;
    }

    void reset(int lineCount) {
        if (values.length < lineCount) {
            values = new int[lineCount];
        }
        Arrays.fill(values, 0, lineCount, UNKNOWN);
        size = lineCount;
    }

    void invalidateAll() {
        size = -1;
    }

    int get(int line) {
        return values[line];
    }

    void set(int line, int value) {
        values[line] = value;
    }

    void replaceRange(int startLine, int removedLineBreaks, int insertedLineBreaks) {
        if (size < 0) return;
        int oldEnd = startLine + removedLineBreaks + 1;
        if (startLine < 0 || removedLineBreaks < 0 || insertedLineBreaks < 0 || oldEnd > size) {
            size = -1;
            return;
        }
        int newEnd = startLine + insertedLineBreaks + 1;
        int newSize = size - removedLineBreaks + insertedLineBreaks;
        if (values.length < newSize) {
            values = Arrays.copyOf(values, Math.max(newSize, values.length + (values.length >> 1)));
        }
        System.arraycopy(values, oldEnd, values, newEnd, size - oldEnd);
        Arrays.fill(values, startLine, newEnd, UNKNOWN);
        size = newSize;
    }
}
