package dtm.stools.component.panels.filepicker.renderers;

import dtm.stools.component.panels.filepicker.utils.FileWrapper;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;

public class FileWrapperGridCellRenderer extends DefaultListCellRenderer {
    private static final Border PADDING_BORDER = new EmptyBorder(5, 5, 5, 5);

    public FileWrapperGridCellRenderer(FileSystemView fsv) {
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
        setHorizontalTextPosition(CENTER);
        setVerticalTextPosition(BOTTOM);
        setBorder(PADDING_BORDER);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof FileWrapper wrapper) {
            setIcon(wrapper.getIcon());
            setText(wrapper.getDisplayName());
            setToolTipText(wrapper.getFile().getAbsolutePath());
        }
        return this;
    }
}