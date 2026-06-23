package dtm.stools.component.inputfields.osfilepicker;

import java.io.File;

public final class OsFilePicker {

    static {
        OsFilePickerNativeLoader.load();
    }

    private static final File DEFAULT_DIR = new File(".");

    private OsFilePicker() {}

    public static File openFile(String title, DeFilter... filters) {
        return openFile(title, DEFAULT_DIR, filters);
    }

    public static File[] openFiles(String title, DeFilter... filters) {
        return openFiles(title, DEFAULT_DIR, filters);
    }

    public static File saveFile(String title, String defaultName, DeFilter... filters) {
        return saveFile(title, DEFAULT_DIR, defaultName, filters);
    }

    public static File openDirectory(String title) {
        return openDirectory(title, DEFAULT_DIR);
    }

    public static File openFileOrDirectory(String title, DeFilter... filters) {
        return openFileOrDirectory(title, DEFAULT_DIR, filters);
    }

    public static File[] openFilesOrDirectories(String title, DeFilter... filters) {
        return openFilesOrDirectories(title, DEFAULT_DIR, filters);
    }

    public static File openFile(String title, File initialDir, DeFilter... filters) {
        String[] names = filterNames(filters);
        String[] specs = filterSpecs(filters);
        String result = nOpenFile(title, pathOf(initialDir), names, specs);
        return result == null ? null : new File(result);
    }

    public static File[] openFiles(String title, File initialDir, DeFilter... filters) {
        String[] names = filterNames(filters);
        String[] specs = filterSpecs(filters);
        String[] result = nOpenFiles(title, pathOf(initialDir), names, specs);
        if (result == null) return null;
        File[] files = new File[result.length];
        for (int i = 0; i < result.length; i++) files[i] = new File(result[i]);
        return files;
    }

    public static File saveFile(String title, File initialDir, String defaultName, DeFilter... filters) {
        String[] names = filterNames(filters);
        String[] specs = filterSpecs(filters);
        String result = nSaveFile(title, pathOf(initialDir), defaultName, names, specs);
        return result == null ? null : new File(result);
    }

    public static File openDirectory(String title, File initialDir) {
        String result = nOpenDirectory(title, pathOf(initialDir));
        return result == null ? null : new File(result);
    }

    public static File openFileOrDirectory(String title, File initialDir, DeFilter... filters) {
        String[] names = filterNames(filters);
        String[] specs = filterSpecs(filters);
        String result = nOpenFileOrDirectory(title, pathOf(initialDir), names, specs);
        return result == null ? null : new File(result);
    }

    public static File[] openFilesOrDirectories(String title, File initialDir, DeFilter... filters) {
        String[] names = filterNames(filters);
        String[] specs = filterSpecs(filters);
        String[] result = nOpenFilesOrDirectories(title, pathOf(initialDir), names, specs);
        if (result == null) return null;
        File[] files = new File[result.length];
        for (int i = 0; i < result.length; i++) files[i] = new File(result[i]);
        return files;
    }

    private static String pathOf(File f) {
        return f == null ? null : f.getAbsolutePath();
    }

    private static String[] filterNames(DeFilter[] filters) {
        if (filters == null || filters.length == 0) return new String[0];
        String[] out = new String[filters.length];
        for (int i = 0; i < filters.length; i++) {
            DeFilter f = filters[i];
            String[] ext = f.ext();
            String suffix = (ext.length == 0) ? "" : " (" + joinExt(ext, ", ") + ")";
            out[i] = f.name() + suffix;
        }
        return out;
    }

    private static String[] filterSpecs(DeFilter[] filters) {
        if (filters == null || filters.length == 0) return new String[0];
        String[] out = new String[filters.length];
        for (int i = 0; i < filters.length; i++) {
            String[] ext = filters[i].ext();
            if (ext.length == 0) {
                out[i] = "*.*";
            } else {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < ext.length; j++) {
                    if (j > 0) sb.append(';');
                    sb.append(normalizePattern(ext[j]));
                }
                out[i] = sb.toString();
            }
        }
        return out;
    }

    private static String normalizePattern(String e) {
        if (e == null || e.isBlank()) return "*.*";
        String s = e.trim();
        if (s.startsWith("*.")) return s;
        if (s.startsWith(".")) return "*" + s;
        if (s.contains("*") || s.contains("?")) return s;
        return "*." + s;
    }

    private static String joinExt(String[] ext, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ext.length; i++) {
            if (i > 0) sb.append(sep);
            sb.append(normalizePattern(ext[i]));
        }
        return sb.toString();
    }

    private static native String nOpenFile(String title, String initialDir, String[] filterNames, String[] filterSpecs);

    private static native String[] nOpenFiles(String title, String initialDir, String[] filterNames, String[] filterSpecs);

    private static native String nSaveFile(String title, String initialDir, String defaultName, String[] filterNames, String[] filterSpecs);

    private static native String nOpenDirectory(String title, String initialDir);

    private static native String nOpenFileOrDirectory(String title, String initialDir, String[] filterNames, String[] filterSpecs);

    private static native String[] nOpenFilesOrDirectories(String title, String initialDir, String[] filterNames, String[] filterSpecs);
}
