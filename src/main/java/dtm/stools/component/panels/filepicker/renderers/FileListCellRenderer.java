package dtm.stools.component.panels.filepicker.renderers;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;

public class FileListCellRenderer extends DefaultListCellRenderer {
    private final FileSystemView fsv;

    public FileListCellRenderer(FileSystemView fsv) {
        this.fsv = fsv;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (c instanceof JLabel label && value instanceof File file) {
            label.setIcon(fsv.getSystemIcon(file));
            label.setText(file.getName());
            label.setToolTipText(file.getAbsolutePath());
        }
        return c;
    }
}