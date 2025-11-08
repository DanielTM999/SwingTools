package dtm.stools.component.panels.filepicker.models;

import dtm.stools.component.panels.filepicker.utils.FileWrapper;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileWrapperTableModel extends AbstractTableModel {
    private final List<FileWrapper> wrappers = new ArrayList<>();
    private final String[] columns = {"Nome", "Data de Modificação", "Tamanho"};

    public void setFiles(List<FileWrapper> wrappers) {
        this.wrappers.clear();
        this.wrappers.addAll(wrappers);
        fireTableDataChanged();
    }

    public FileWrapper getFileWrapperAt(int row) {
        return wrappers.get(row);
    }

    public int findFile(File file) {
        for (int i = 0; i < wrappers.size(); i++) {
            if (wrappers.get(i).getFile().equals(file)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return wrappers.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> FileWrapper.class;
            case 1 -> Date.class;
            case 2 -> Long.class;
            default -> Object.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        FileWrapper wrapper = wrappers.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> wrapper;
            case 1 -> new Date(wrapper.getLastModified());
            case 2 -> wrapper.isDirectory() ? null : wrapper.getLength();
            default -> null;
        };
    }
}