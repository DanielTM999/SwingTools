package dtm.stools.component.tree;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Collections;
import java.util.List;

public record TreeExternalDropContext<T>(
        TreeView<T> tree,
        TreeNode<T> targetNode,
        int childIndex,
        Transferable transferable,
        DataFlavor[] dataFlavors
) {

    public boolean hasFlavor(DataFlavor flavor) {
        return transferable != null && transferable.isDataFlavorSupported(flavor);
    }

    public String getStringData() {
        if (!hasFlavor(DataFlavor.stringFlavor)) return null;
        try {
            return String.valueOf(transferable.getTransferData(DataFlavor.stringFlavor));
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<File> getFileListData() {
        if (!hasFlavor(DataFlavor.javaFileListFlavor)) return Collections.emptyList();
        try {
            Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
            return data instanceof List<?> list ? (List<File>) list : Collections.emptyList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
