package dtm.stools.component.tree;

import javax.swing.AbstractCellEditor;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.TreeCellEditor;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.function.BiFunction;

public class TreeNodeEditor extends AbstractCellEditor implements TreeCellEditor {

    protected final JTextField field = new JTextField();
    protected TreeNode<?> editing;
    protected BiFunction<TreeNode<?>, String, Object> commitHandler;

    public TreeNodeEditor() {
        field.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
    }

    public void setCommitHandler(BiFunction<TreeNode<?>, String, Object> handler) {
        this.commitHandler = handler;
    }

    @Override
    public Object getCellEditorValue() {
        if (editing == null) return field.getText();
        if (commitHandler != null) {
            try { return commitHandler.apply(editing, field.getText()); }
            catch (Exception e) { return editing.getData(); }
        }
        if (editing.getData() instanceof String || editing.getData() == null) {
            return field.getText();
        }
        editing.setLabel(field.getText());
        return editing.getData();
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return !(anEvent instanceof MouseEvent);
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected,
                                                boolean expanded, boolean leaf, int row) {
        if (value instanceof TreeNode<?> node) {
            this.editing = node;
            field.setText(node.computeLabel());
        } else {
            this.editing = null;
            field.setText(value == null ? "" : value.toString());
        }
        return field;
    }
}
