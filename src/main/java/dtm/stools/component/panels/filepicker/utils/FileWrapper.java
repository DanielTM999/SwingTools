package dtm.stools.component.panels.filepicker.utils;

import lombok.Getter;

import javax.swing.Icon;
import java.io.File;
import java.util.Objects;

@Getter
public class FileWrapper {
    private final File file;
    private final Icon icon;
    private final String displayName;
    private final long length;
    private final long lastModified;
    private final boolean isDirectory;

    public FileWrapper(File file, Icon icon, String displayName, long length, long lastModified, boolean isDirectory) {
        this.file = file;
        this.icon = icon;
        this.displayName = displayName;
        this.length = length;
        this.lastModified = lastModified;
        this.isDirectory = isDirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileWrapper that = (FileWrapper) o;
        return Objects.equals(file, that.file);
    }
    @Override
    public int hashCode() {
        return Objects.hash(file);
    }
}