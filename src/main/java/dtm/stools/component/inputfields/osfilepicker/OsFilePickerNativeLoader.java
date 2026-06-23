package dtm.stools.component.inputfields.osfilepicker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class OsFilePickerNativeLoader {

    private static volatile boolean loaded;

    private OsFilePickerNativeLoader() {}

    static synchronized void load() {
        if (loaded) return;

        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String fileName;
        String resourceDir;
        String fallbackName;
        String[] dependencies;

        if (os.contains("win")) {
            fileName = "osfilepicker.dll";
            resourceDir = "/native/win/" + arch;
            fallbackName = "osfilepicker";
            dependencies = new String[]{"libwinpthread-1.dll"};
        } else if (os.contains("mac") || os.contains("darwin")) {
            fileName = "libosfilepicker.dylib";
            resourceDir = "/native/mac/" + arch;
            fallbackName = "osfilepicker";
            dependencies = new String[0];
        } else {
            fileName = "libosfilepicker.so";
            resourceDir = "/native/linux/" + arch;
            fallbackName = "osfilepicker";
            dependencies = new String[0];
        }

        try {
            if (!tryLoadFromResource(resourceDir, fileName, dependencies)) {
                if (!tryLoadFromResource("/native/" + fileName, fileName)) {
                    System.loadLibrary(fallbackName);
                }
            }
            loaded = true;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to load native osfilepicker library (" + fileName + ")", t);
        }
    }

    private static boolean tryLoadFromResource(String resourceDir, String fileName, String[] dependencies) throws IOException {
        String resourcePath = resourceDir + "/" + fileName;
        try (InputStream in = OsFilePickerNativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) return false;
            Path tmpDir = Files.createTempDirectory("osfilepicker-");
            tmpDir.toFile().deleteOnExit();

            for (String dependency : dependencies) {
                Path dependencyPath = copyResource(resourceDir + "/" + dependency, tmpDir, dependency);
                if (dependencyPath != null) {
                    System.load(dependencyPath.toAbsolutePath().toString());
                }
            }

            Path tmp = copyResource(in, tmpDir, fileName);
            System.load(tmp.toAbsolutePath().toString());
            return true;
        }
    }

    private static boolean tryLoadFromResource(String resourcePath, String fileName) throws IOException {
        try (InputStream in = OsFilePickerNativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) return false;
            Path tmp = Files.createTempFile("osfilepicker-", "-" + fileName);
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            System.load(tmp.toAbsolutePath().toString());
            return true;
        }
    }

    private static Path copyResource(String resourcePath, Path targetDir, String fileName) throws IOException {
        try (InputStream in = OsFilePickerNativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            return copyResource(in, targetDir, fileName);
        }
    }

    private static Path copyResource(InputStream in, Path targetDir, String fileName) throws IOException {
        Path target = targetDir.resolve(fileName);
        target.toFile().deleteOnExit();
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }
}
