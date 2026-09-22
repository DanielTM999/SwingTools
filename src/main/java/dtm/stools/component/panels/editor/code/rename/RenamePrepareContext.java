package dtm.stools.component.panels.editor.code.rename;

import dtm.stools.component.panels.editor.code.api.Position;

public record RenamePrepareContext(String buffer, Position position, int offset, String wordAtCaret) {

    public RenamePrepareContext {
        buffer = buffer == null ? "" : buffer;
        wordAtCaret = wordAtCaret == null ? "" : wordAtCaret;
    }
}
