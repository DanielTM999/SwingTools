package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;

import javax.swing.JPopupMenu;
import java.awt.event.MouseEvent;

@FunctionalInterface
public interface ContextMenuProvider extends CodeEditorProvider {

    JPopupMenu getPopupMenu(MouseEvent e);

    static ContextMenuProvider of(JPopupMenu menu) {
        return e -> menu;
    }

    static ContextMenuProvider defaultFor(CodeEditorTextArea editor) {
        return e -> editor.createDefaultContextMenu();
    }
}
