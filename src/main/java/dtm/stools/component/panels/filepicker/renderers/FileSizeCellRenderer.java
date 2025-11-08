package dtm.stools.component.panels.filepicker.renderers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class FileSizeCellRenderer extends DefaultTableCellRenderer {
    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};

    public FileSizeCellRenderer() {
        setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Long) {
            long size = (Long) value;
            if (size <= 0) {
                value = "";
            } else {
                int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
                if (digitGroups >= UNITS.length) digitGroups = UNITS.length - 1;
                value = String.format("%.1f %s", size / Math.pow(1024, digitGroups), UNITS[digitGroups]);
            }
        } else {
            value = "";
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}