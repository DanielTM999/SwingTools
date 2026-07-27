package dtm.stools.component.panels.editor.code;

import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import javax.swing.SwingUtilities;

public class SelectedTextOccurrencesRepro {

    public static void main(String[] args) throws Exception {
        verifiesIdentifierOccurrences();
        verifiesMultilineOccurrences();
        verifiesConfiguration();
        verifiesSelectionLifecycle();
        System.out.println("ALL OK");
        System.exit(0);
    }

    static void verifiesIdentifierOccurrences() {
        String text = "item otherItem item item2 item";
        int[] matches = CodeEditorTextArea.findSelectedTextOccurrences(text, "item", 0, 4);
        require(Arrays.equals(matches, new int[]{15, 19, 26, 30}));
    }

    static void verifiesMultilineOccurrences() {
        String selected = "first\nsecond";
        String text = selected + "\nvalue\n" + selected;
        int start = text.lastIndexOf(selected);
        int[] matches = CodeEditorTextArea.findSelectedTextOccurrences(
                text,
                selected,
                start,
                start + selected.length());
        require(Arrays.equals(matches, new int[]{0, selected.length()}));
    }

    static void verifiesConfiguration() {
        CodeEditorTextArea textArea = new CodeEditorTextArea("value value");
        require(textArea.isHighlightSelectedTextOccurrences());
        textArea.setHighlightSelectedTextOccurrences(false);
        require(!textArea.isHighlightSelectedTextOccurrences());
        textArea.setSelectedTextOccurrencesColor(null);
        require(textArea.getSelectedTextOccurrencesColor() == null);
        textArea.setSelectedTextOccurrencesColor(Color.RED);
        require(Color.RED.equals(textArea.getSelectedTextOccurrencesColor()));
    }

    static void verifiesSelectionLifecycle() throws Exception {
        CodeEditorTextArea textArea = new CodeEditorTextArea("value other value");
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        SwingUtilities.invokeAndWait(() -> textArea.setSelection(0, 0, 0, 5));
        long deadline = System.nanoTime() + 2_000_000_000L;
        while (textArea.selectedTextOccurrenceOffsets.length == 0
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        require(Arrays.equals(textArea.selectedTextOccurrenceOffsets, new int[]{12, 17}));
        SwingUtilities.invokeAndWait(() -> textArea.setCaretPosition(0, 0));
        require(textArea.selectedTextOccurrenceOffsets.length == 0);
    }

    static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }
}
