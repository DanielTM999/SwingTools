package dtm.stools.component.panels.filepicker.renderers;

import dtm.stools.component.panels.filepicker.utils.FileTreeNode;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.File;

public class FileTreeCellRenderer extends DefaultTreeCellRenderer {
    private final FileSystemView fsv;
    private final Icon folderIcon;

    public FileTreeCellRenderer(FileSystemView fsv) {
        this.fsv = fsv;
        this.folderIcon = UIManager.getIcon("FileView.directoryIcon");
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof FileTreeNode) {
            Object userObject = ((FileTreeNode) value).getUserObject();

            if (userObject instanceof File file) {
                setText(fsv.getSystemDisplayName(file));
                setIcon(fsv.getSystemIcon(file));
            } else if (userObject instanceof String) {
                setText(userObject.toString());
                setIcon(folderIcon);
            }
        }
        return this;
    }
}