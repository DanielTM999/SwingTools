package dtm.stools.component.panels.filepicker.renderers;

import dtm.stools.component.panels.filepicker.utils.FileWrapper;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class FileWrapperCellRenderer extends DefaultTableCellRenderer {
    private final FileSystemView fsv;

    public FileWrapperCellRenderer(FileSystemView fsv) {
        this.fsv = fsv;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (c instanceof JLabel label && value instanceof FileWrapper wrapper) {
            label.setIcon(wrapper.getIcon());
            label.setText(wrapper.getDisplayName());
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setIconTextGap(5);
        }
        return c;
    }
}