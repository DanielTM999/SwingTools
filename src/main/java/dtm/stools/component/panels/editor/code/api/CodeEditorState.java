package dtm.stools.component.panels.editor.code.api;

import java.util.List;

public record CodeEditorState(
        boolean modified,
        boolean canUndo,
        boolean canRedo,
        int caretLine,
        int caretCol,
        int caretOffset,
        boolean selectionActive,
        int selectionStartLine,
        int selectionStartCol,
        int selectionEndLine,
        int selectionEndCol,
        int selectionStartOffset,
        int selectionEndOffset,
        int extraCaretCount,
        List<Integer> extraCaretOffsets,
        boolean overwriteMode,
        int tabSize,
        boolean useSpacesForTab,
        String indentText,
        boolean smartIndentEnabled,
        boolean showIndentGuides,
        boolean activeSnippetSession
) {
}
