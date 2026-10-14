package dtm.stools.component.charts2d;

import java.io.File;
import java.util.Locale;

public enum ChartImageFormat {

    PNG("png", false, "png"),
    JPEG("jpg", true, "jpg", "jpeg");

    private final String imageIoName;
    private final boolean opaque;
    private final String[] extensions;

    ChartImageFormat(String imageIoName, boolean opaque, String... extensions) {
        this.imageIoName = imageIoName;
        this.opaque = opaque;
        this.extensions = extensions;
    }

    public String getImageIoName() {
        return imageIoName;
    }

    public boolean isOpaque() {
        return opaque;
    }

    public String getDefaultExtension() {
        return extensions[0];
    }

    public boolean matchesExtension(String extension) {
        if (extension == null) {
            return false;
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        for (String candidate : extensions) {
            if (candidate.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static ChartImageFormat fromFile(File file) {
        if (file == null) {
            return null;
        }
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        String extension = name.substring(dot + 1);
        for (ChartImageFormat format : values()) {
            if (format.matchesExtension(extension)) {
                return format;
            }
        }
        return null;
    }
}
