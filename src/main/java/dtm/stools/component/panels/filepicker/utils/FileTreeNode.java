package dtm.stools.component.panels.filepicker.utils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;

public class FileTreeNode extends DefaultMutableTreeNode {
    public FileTreeNode(Object userObject) {
        super(userObject);
    }

    @Override
    public String toString() {
        if (userObject instanceof File) {
            return ((File) userObject).getName();
        }
        return super.toString();
    }
}