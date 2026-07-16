package dtm.stools.component.panels.graphics.gl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class GlNativeLoader {

    private static volatile boolean loaded;

    private GlNativeLoader() {}

    static synchronized void load() {
        if (loaded) return;

        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String fileName;
        String resourceDir;
        String fallbackName;
        String[] dependencies;

        if (os.contains("win")) {
            fileName = "graphicsgl.dll";
            resourceDir = "/native/win/" + arch;
            fallbackName = "graphicsgl";
            dependencies = new String[]{"libwinpthread-1.dll"};
        } else if (os.contains("mac") || os.contains("darwin")) {
            throw new UnsupportedOperationException("OpenGL panel is not supported on macOS yet");
        } else {
            fileName = "libgraphicsgl.so";
            resourceDir = "/native/linux/" + arch;
            fallbackName = "graphicsgl";
            dependencies = new String[0];
        }

        try {
            java.awt.Toolkit.getDefaultToolkit();
            loadJawt(os);
            if (!tryLoadFromResource(resourceDir, fileName, dependencies)) {
                if (!tryLoadFromResource("/native", fileName, new String[0])) {
                    System.loadLibrary(fallbackName);
                }
            }
            loaded = true;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to load native graphicsgl library (" + resourceDir + "/" + fileName + ")", t);
        }
    }

    private static void loadJawt(String os) {
        try {
            Path jawt = os.contains("win")
                    ? Path.of(System.getProperty("java.home"), "bin", "jawt.dll")
                    : Path.of(System.getProperty("java.home"), "lib", "libjawt.so");
            if (Files.exists(jawt)) {
                System.load(jawt.toAbsolutePath().toString());
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            System.loadLibrary("jawt");
        } catch (Throwable ignored) {
        }
    }

    private static boolean tryLoadFromResource(String resourceDir, String fileName, String[] dependencies) throws IOException {
        String resourcePath = resourceDir + "/" + fileName;
        try (InputStream in = GlNativeLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) return false;
            Path tmpDir = Files.createTempDirectory("graphicsgl-");
            tmpDir.toFile().deleteOnExit();

            for (String dependency : dependencies) {
                Path dependencyPath = copyResource(resourceDir + "/" + dependency, tmpDir, dependency);
                if (dependencyPath != null) {
                    try {
                        System.load(dependencyPath.toAbsolutePath().toString());
                    } catch (Throwable ignored) {
                    }
                }
            }

            Path tmp = copyResource(in, tmpDir, fileName);
            System.load(tmp.toAbsolutePath().toString());
            return true;
        }
    }

    private static Path copyResource(String resourcePath, Path targetDir, String fileName) throws IOException {
        try (InputStream in = GlNativeLoader.class.getResourceAsStream(resourcePath)) {
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
