package dtm.stools.component.panels.editor.code.utils;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

public class BracketHighlighter {

    private final TextBuffer buffer;

    private static final Map<Character, Character> PAIRS = Map.of(
            '(', ')',
            '{', '}',
            '[', ']'
    );

    private boolean enabled = true;
    private Color customColor = null;

    private IntUnaryOperator lineToVisualMapper = i -> i;
    private IntUnaryOperator lineToYMapper;
    private IntPredicate lineHiddenPredicate = i -> false;

    public BracketHighlighter(TextBuffer buffer) {
        this.buffer = buffer;
    }

    public void setLineToVisualMapper(IntUnaryOperator mapper) {
        this.lineToVisualMapper = mapper != null ? mapper : i -> i;
    }

    public void setLineToYMapper(IntUnaryOperator mapper) {
        this.lineToYMapper = mapper;
    }

    public void setLineHiddenPredicate(IntPredicate predicate) {
        this.lineHiddenPredicate = predicate != null ? predicate : i -> false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setColor(Color color) {
        this.customColor = color;
    }

    public Color getColor() {
        return resolveColor();
    }

    private Color resolveColor() {
        if (customColor != null) return customColor;

        Color base = UIManager.getColor("TextArea.selectionBackground");
        if (base == null) base = new Color(51, 153, 255);

        return lighten(base, 0.4f);
    }

    private Color lighten(Color color, float factor) {
        int r = (int) (color.getRed() + (255 - color.getRed()) * factor);
        int g = (int) (color.getGreen() + (255 - color.getGreen()) * factor);
        int b = (int) (color.getBlue() + (255 - color.getBlue()) * factor);

        return new Color(r, g, b, 120);
    }

    public void paint(Graphics2D g2, FontMetrics fm, int caretOffset, int lineHeight) {
        if (!enabled) return;
        if (buffer.length() == 0) return;

        int pos = findRelevantChar(caretOffset);
        if (pos == -1) return;

        char c = buffer.charAt(pos);

        boolean forward = PAIRS.containsKey(c);
        char match = forward ? PAIRS.get(c) : getKeyByValue(c);

        int matchPos = findMatch(pos, c, match, forward);
        if (matchPos == -1) return;

        int line1 = buffer.lineOfOffset(pos);
        int line2 = buffer.lineOfOffset(matchPos);

        int startLine = Math.min(line1, line2);
        int endLine = Math.max(line1, line2);

        Color color = resolveColor();

        int yStart = lineToYMapper != null
                ? lineToYMapper.applyAsInt(startLine)
                : lineToVisualMapper.applyAsInt(startLine) * lineHeight;
        int yEnd = lineToYMapper != null
                ? lineToYMapper.applyAsInt(endLine)
                : lineToVisualMapper.applyAsInt(endLine) * lineHeight;
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g2.fillRect(2, yStart, 2, (yEnd - yStart) + lineHeight);

        highlightChar(g2, fm, pos, lineHeight, color);
        highlightChar(g2, fm, matchPos, lineHeight, color);
    }

    private void highlightChar(Graphics2D g2, FontMetrics fm, int offset, int lineHeight, Color color) {
        int line = buffer.lineOfOffset(offset);
        if (lineHiddenPredicate.test(line)) return;
        int col = offset - buffer.offsetOfLine(line);

        String text = buffer.lineAt(line);
        String before = text.substring(0, Math.min(col, text.length()));

        int x = 4 + fm.stringWidth(before);
        int y = lineToYMapper != null
                ? lineToYMapper.applyAsInt(line)
                : lineToVisualMapper.applyAsInt(line) * lineHeight;

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
        g2.fillRect(x, y, fm.charWidth('('), lineHeight);
    }

    private int findRelevantChar(int caret) {
        int length = buffer.length();
        if (length == 0) return -1;

        caret = Math.clamp(caret, 0, length);

        if (caret > 0 && isBracket(buffer.charAt(caret - 1))) {
            return caret - 1;
        }
        if (caret < length && isBracket(buffer.charAt(caret))) {
            return caret;
        }
        return -1;
    }

    private boolean isBracket(char c) {
        return PAIRS.containsKey(c) || PAIRS.containsValue(c);
    }

    private char getKeyByValue(char value) {
        for (var e : PAIRS.entrySet()) {
            if (e.getValue() == value) return e.getKey();
        }
        return 0;
    }

    private int findMatch(int start, char open, char close, boolean forward) {
        int depth = 0;

        if (forward) {
            for (int i = start; i < buffer.length(); i++) {
                char c = buffer.charAt(i);

                if (c == open) depth++;
                if (c == close) depth--;

                if (depth == 0) return i;
            }
        } else {
            for (int i = start; i >= 0; i--) {
                char c = buffer.charAt(i);

                if (c == close) depth++;
                if (c == open) depth--;

                if (depth == 0) return i;
            }
        }

        return -1;
    }
}
