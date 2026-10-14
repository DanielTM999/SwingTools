package dtm.stools.component.inputfields.textarea;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;

final class TextAreaEditActions {

    private final SmartTextArea area;

    TextAreaEditActions(SmartTextArea area) {
        this.area = area;
    }

    void install() {
        InputMap input = area.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actions = area.getActionMap();
        int menu = SmartTextArea.menuShortcutMask();
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_D, menu), "smartTextArea.duplicateLines", this::duplicateLines, this::shortcutsActive);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK), "smartTextArea.moveLinesUp", this::moveLinesUp, this::shortcutsActive);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK), "smartTextArea.moveLinesDown", this::moveLinesDown, this::shortcutsActive);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_K, menu | InputEvent.SHIFT_DOWN_MASK), "smartTextArea.deleteLines", this::deleteLines, this::shortcutsActive);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "smartTextArea.indent", this::indent, this::editable);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "smartTextArea.outdent", this::outdent, this::shortcutsActive);
        bind(input, actions, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "smartTextArea.newline", this::newline, this::editable);
    }

    void duplicateLines() {
        edit(() -> {
            int[] lines = selectedLines();
            int blockStart = area.getLineStartOffset(lines[0]);
            int blockEnd = area.getLineEndOffset(lines[1]);
            String block = text(blockStart, blockEnd);
            boolean newlineEnded = block.endsWith("\n");
            int mark = area.getCaret().getMark();
            int dot = area.getCaret().getDot();
            area.getDocument().insertString(blockEnd, newlineEnded ? block : "\n" + block, null);
            int shift = newlineEnded ? block.length() : block.length() + 1;
            restore(mark + shift, dot + shift);
        });
    }

    void moveLinesUp() {
        edit(() -> {
            int[] lines = selectedLines();
            if (lines[0] == 0) return;
            int blockStart = area.getLineStartOffset(lines[0]);
            int blockEnd = area.getLineEndOffset(lines[1]);
            int previousStart = area.getLineStartOffset(lines[0] - 1);
            String previous = text(previousStart, blockStart);
            String block = text(blockStart, blockEnd);
            String replacement = block.endsWith("\n")
                    ? block + previous
                    : block + "\n" + previous.substring(0, previous.length() - 1);
            int mark = area.getCaret().getMark();
            int dot = area.getCaret().getDot();
            replace(previousStart, blockEnd, replacement);
            restore(mark - previous.length(), dot - previous.length());
        });
    }

    void moveLinesDown() {
        edit(() -> {
            int[] lines = selectedLines();
            if (lines[1] >= area.getLineCount() - 1) return;
            int blockStart = area.getLineStartOffset(lines[0]);
            int blockEnd = area.getLineEndOffset(lines[1]);
            int nextEnd = area.getLineEndOffset(lines[1] + 1);
            String block = text(blockStart, blockEnd);
            String next = text(blockEnd, nextEnd);
            String replacement;
            int shift;
            if (next.endsWith("\n")) {
                replacement = next + block;
                shift = next.length();
            } else {
                replacement = next + "\n" + block.substring(0, block.length() - 1);
                shift = next.length() + 1;
            }
            int mark = area.getCaret().getMark();
            int dot = area.getCaret().getDot();
            replace(blockStart, nextEnd, replacement);
            restore(mark + shift, dot + shift);
        });
    }

    void deleteLines() {
        edit(() -> {
            int[] lines = selectedLines();
            int blockStart = area.getLineStartOffset(lines[0]);
            int blockEnd = area.getLineEndOffset(lines[1]);
            int removeStart = blockStart;
            if (!text(blockStart, blockEnd).endsWith("\n") && blockStart > 0) removeStart = blockStart - 1;
            area.getDocument().remove(removeStart, blockEnd - removeStart);
            int caret = removeStart < blockStart
                    ? area.getLineStartOffset(area.getLineOfOffset(removeStart))
                    : Math.min(blockStart, area.getDocument().getLength());
            area.setCaretPosition(caret);
        });
    }

    void indent() {
        edit(() -> {
            if (!area.isEditShortcutsEnabled()) {
                area.replaceSelection("\t");
                return;
            }
            int[] lines = selectedLines();
            if (lines[0] == lines[1]) insertTab();
            else shiftLines(lines, true);
        });
    }

    void outdent() {
        edit(() -> shiftLines(selectedLines(), false));
    }

    void newline() {
        edit(() -> {
            if (!area.isAutoIndent()) {
                area.replaceSelection("\n");
                return;
            }
            int start = area.getSelectionStart();
            int lineStart = area.getLineStartOffset(area.getLineOfOffset(start));
            String before = text(lineStart, start);
            int length = 0;
            while (length < before.length() && isIndentChar(before.charAt(length))) length++;
            area.replaceSelection("\n" + before.substring(0, length));
        });
    }

    private void insertTab() throws BadLocationException {
        if (!area.isUseSpacesForTab()) {
            area.replaceSelection("\t");
            return;
        }
        int start = area.getSelectionStart();
        int lineStart = area.getLineStartOffset(area.getLineOfOffset(start));
        int size = tabSize();
        int column = visualColumn(text(lineStart, start), size);
        area.replaceSelection(" ".repeat(size - column % size));
    }

    private void shiftLines(int[] lines, boolean indent) throws BadLocationException {
        Document document = area.getDocument();
        Position mark = document.createPosition(area.getCaret().getMark());
        Position dot = document.createPosition(area.getCaret().getDot());
        String unit = area.isUseSpacesForTab() ? " ".repeat(tabSize()) : "\t";
        for (int line = lines[1]; line >= lines[0]; line--) {
            int start = area.getLineStartOffset(line);
            int end = lineContentEnd(line);
            if (indent) {
                if (end > start) document.insertString(start, unit, null);
            } else {
                int removable = leadingRemovable(text(start, end));
                if (removable > 0) document.remove(start, removable);
            }
        }
        restore(mark.getOffset(), dot.getOffset());
    }

    private int leadingRemovable(String line) {
        if (line.startsWith("\t")) return 1;
        int size = tabSize();
        int count = 0;
        while (count < line.length() && count < size && line.charAt(count) == ' ') count++;
        return count;
    }

    private int[] selectedLines() throws BadLocationException {
        int start = area.getSelectionStart();
        int end = area.getSelectionEnd();
        int first = area.getLineOfOffset(start);
        int last = area.getLineOfOffset(end);
        if (end > start && last > first && end == area.getLineStartOffset(last)) last--;
        return new int[]{first, last};
    }

    private int lineContentEnd(int line) throws BadLocationException {
        int end = area.getLineEndOffset(line);
        if (line < area.getLineCount() - 1) end--;
        return end;
    }

    private void replace(int start, int end, String replacement) throws BadLocationException {
        Document document = area.getDocument();
        document.remove(start, end - start);
        document.insertString(start, replacement, null);
    }

    private void restore(int mark, int dot) {
        int length = area.getDocument().getLength();
        area.setCaretPosition(Math.clamp(mark, 0, length));
        area.moveCaretPosition(Math.clamp(dot, 0, length));
    }

    private String text(int start, int end) throws BadLocationException {
        return area.getDocument().getText(start, end - start);
    }

    private int tabSize() {
        return Math.max(1, area.getTabSize());
    }

    private void edit(TextAreaEdit body) {
        area.beginCompoundEdit();
        try {
            body.run();
        } catch (BadLocationException ex) {
            UIManager.getLookAndFeel().provideErrorFeedback(area);
        } finally {
            area.endCompoundEdit();
        }
    }

    private boolean editable() {
        return area.isEditable() && area.isEnabled();
    }

    private boolean shortcutsActive() {
        return editable() && area.isEditShortcutsEnabled();
    }

    private static int visualColumn(String text, int tabSize) {
        int column = 0;
        for (int i = 0; i < text.length(); i++) {
            column = text.charAt(i) == '\t' ? column + tabSize - column % tabSize : column + 1;
        }
        return column;
    }

    private static boolean isIndentChar(char c) {
        return c == ' ' || c == '\t';
    }

    private static void bind(InputMap input, ActionMap actions, KeyStroke key, String name, Runnable action, BooleanSupplier enabled) {
        input.put(key, name);
        actions.put(name, new TextAreaAction(action, enabled));
    }
}
