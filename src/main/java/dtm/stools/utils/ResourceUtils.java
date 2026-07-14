package dtm.stools.utils;

import dtm.stools.exceptions.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class ResourceUtils {

    private ResourceUtils(){
        throw new IllegalStateException("utility class");
    }

    public static URL getResource(String path){
        return getResource(ResourceUtils.class, path);
    }

    public static URL getResource(Class<?> aClass, String path){
        return getResource(aClass, path, false);
    }

    public static URL getResource(Class<?> aClass, String path, boolean external){
        if(external){
            return getResourceExternal(aClass, path);
        }else{
            String normalizedPath = "/" + normalizeResourcePath(path);

            URL resource = aClass.getResource(normalizedPath);
            if (resource == null) {
                throw new ResourceNotFoundException("Recurso não encontrado: " + normalizedPath + " (classe: " + aClass.getName() + ")", normalizedPath);
            }

            return resource;
        }
    }

    public static URL getResource(ClassLoader classLoader, String path, boolean external){
        if(external){
            return getResourceExternal(classLoader, path);
        }else{
            String normalizedPath = normalizeResourcePath(path);

            URL resource = classLoader.getResource(normalizedPath);
            if (resource == null) {
                throw new ResourceNotFoundException("Recurso não encontrado: " + normalizedPath + " (classLoader: " + classLoader.getName() + ")", normalizedPath);
            }

            return resource;
        }
    }

    public static void walkResources(String resourcePath, boolean recursive, Consumer<URL> consumer) {
        walkResources(
                ResourceUtils.class,
                resourcePath,
                recursive,
                consumer
        );
    }

    public static void walkResources(Class<?> referenceClass, String resourcePath, boolean recursive, Consumer<URL> consumer) {
        Objects.requireNonNull(
                referenceClass,
                "A classe de referência não pode ser nula"
        );

        Objects.requireNonNull(
                consumer,
                "O consumer não pode ser nulo"
        );

        String normalizedPath = normalizeResourcePath(resourcePath);
        Path classPath = getResourceClassPath(referenceClass);

        try {
            if (Files.isDirectory(classPath)) {
                walkResourceDirectory(
                        classPath,
                        normalizedPath,
                        recursive,
                        consumer
                );
                return;
            }

            if (Files.isRegularFile(classPath)) {
                walkResourceJar(
                        classPath,
                        normalizedPath,
                        recursive,
                        consumer
                );
                return;
            }

            throw new ResourceNotFoundException(
                    "O local da classe não é um diretório nem um arquivo JAR: "
                            + classPath,
                    normalizedPath
            );
        } catch (IOException exception) {
            throw new ResourceNotFoundException(
                    "Erro ao percorrer recursos: " + normalizedPath,
                    normalizedPath,
                    exception
            );
        }
    }

    public static void walkResources(ClassLoader classLoader, String resourcePath, boolean recursive, Consumer<URL> consumer) {
        Objects.requireNonNull(
                classLoader,
                "O ClassLoader não pode ser nulo"
        );

        Objects.requireNonNull(
                consumer,
                "O consumer não pode ser nulo"
        );

        String normalizedPath = normalizeResourcePath(resourcePath);
        Set<String> processedResources = new HashSet<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(normalizedPath);

            boolean found = false;

            while (resources.hasMoreElements()) {
                found = true;

                URL resourceUrl = resources.nextElement();

                walkResourceUrl(
                        resourceUrl,
                        normalizedPath,
                        recursive,
                        url -> {
                            if (processedResources.add(
                                    url.toExternalForm()
                            )) {
                                consumer.accept(url);
                            }
                        }
                );
            }

            if (!found && classLoader instanceof URLClassLoader urlClassLoader) {
                for (URL classPathUrl : urlClassLoader.getURLs()) {
                    if (!"file".equalsIgnoreCase(
                            classPathUrl.getProtocol()
                    )) {
                        continue;
                    }

                    Path classPath = Path.of(classPathUrl.toURI());

                    if (Files.isDirectory(classPath)) {
                        Path resource = classPath.resolve(
                                normalizedPath
                        );

                        if (Files.exists(resource)) {
                            found = true;

                            walkResourceDirectory(
                                    classPath,
                                    normalizedPath,
                                    recursive,
                                    url -> {
                                        if (processedResources.add(
                                                url.toExternalForm()
                                        )) {
                                            consumer.accept(url);
                                        }
                                    }
                            );
                        }
                    } else if (Files.isRegularFile(classPath)) {
                        found = walkResourceJarIfExists(
                                classPath,
                                normalizedPath,
                                recursive,
                                url -> {
                                    if (processedResources.add(
                                            url.toExternalForm()
                                    )) {
                                        consumer.accept(url);
                                    }
                                }
                        ) || found;
                    }
                }
            }

            if (!found) {
                throw new ResourceNotFoundException(
                        "Recurso não encontrado no ClassLoader: "
                                + normalizedPath,
                        normalizedPath
                );
            }
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResourceNotFoundException(
                    "Erro ao percorrer recursos pelo ClassLoader: "
                            + normalizedPath,
                    normalizedPath,
                    exception
            );
        }
    }


    public static InputStream getResourceAsStream(String path) {
        return getResourceAsStream(ResourceUtils.class, path);
    }

    public static InputStream getResourceAsStream(Class<?> aClass, String path) {
        return getResourceAsStream(aClass, path, false);
    }

    public static InputStream getResourceAsStream(Class<?> aClass, String path, boolean classLoader) {
        String normalizedPath = normalizeResourcePath(path);
        String lookupPath = classLoader ? normalizedPath : "/" + normalizedPath;

        InputStream stream;
        if(classLoader){
            stream = aClass.getClassLoader().getResourceAsStream(lookupPath);
        }else{
            stream = aClass.getResourceAsStream(lookupPath);
        }
        if (stream == null) {
            throw new ResourceNotFoundException("Recurso não encontrado como stream: " + lookupPath + " (classe: " + aClass.getName() + ")", lookupPath);
        }
        return stream;
    }

    public static InputStream getExternalResourceAsStream(Class<?> aClass, String path) {
        Path jarPath = getResourceClassPath(aClass);
        String cleanPath = normalizeResourcePath(path);

        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()})) {
            InputStream stream = loader.getResourceAsStream(cleanPath);

            if (stream == null) {
                throw new ResourceNotFoundException("Recurso não encontrado como stream: "
                        + cleanPath + " (classe: " + aClass.getName() + ")", cleanPath);
            }

            byte[] bytes = stream.readAllBytes();
            return new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Erro ao carregar recurso externo: " + path + " (classe: " + aClass.getName() + ")", path ,e);
        }
    }


    public static byte[] getResourceBytesExternal(Class<?> aClass, String path) {
        if (path == null || path.isBlank()) {
            throw new ResourceNotFoundException("O caminho do recurso não pode ser nulo ou vazio", path);
        }
        Path jarPath = getResourceClassPath(aClass);
        String normalizedPath = normalizeResourcePath(path);
        byte[] bytes = null;
        try(URLClassLoader loader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()})){
            InputStream stream = loader.getResourceAsStream(normalizedPath);

            if (stream == null) {
                throw new ResourceNotFoundException("Recurso não encontrado como stream: " + normalizedPath + " (classe: " + aClass.getName() + ")", normalizedPath);
            }
            bytes = stream.readAllBytes();
        }catch (Exception e){
            throw new ResourceNotFoundException("Recurso não encontrado como stream: " + path + " (classe: " + aClass.getName() + ")", path, e);
        }
        return bytes;
    }

    public static Path getResourceClassPath(Class<?> clazz){
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        if (protectionDomain == null) {
            throw new IllegalStateException("Classe sem ProtectionDomain: " + clazz.getName());
        }

        CodeSource codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("Classe sem CodeSource (provavelmente carregada via bootstrap): " + clazz.getName());
        }

        URL location = codeSource.getLocation();
        try {
            return Paths.get(location.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Erro ao converter URL para URI", e);
        }
    }



    private static URL getResourceExternal(Class<?> aClass, String path) {
        Path jarPath = getResourceClassPath(aClass);
        String cleanPath = normalizeResourcePath(path);
        URL resourceUrl = null;

        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()})) {

            resourceUrl = loader.getResource(cleanPath);

            if (resourceUrl == null) {
                throw new ResourceNotFoundException("Recurso não encontrado como URL: " + cleanPath + " (classe: " + aClass.getName() + ")", cleanPath);
            }
        } catch (Exception e) {
            throw new ResourceNotFoundException("Erro ao buscar recurso como URL: " + path + " (classe: " + aClass.getName() + ")", path, e);
        }

        return resourceUrl;
    }

    private static URL getResourceExternal(ClassLoader classLoader, String path) {
        URL resourceUrl = null;
        String cleanPath = normalizeResourcePath(path);
        try {

            resourceUrl = classLoader.getResource(cleanPath);

            if (resourceUrl == null) {
                throw new ResourceNotFoundException("Recurso não encontrado como URL: " + cleanPath + " (classLoader: " + classLoader.getName() + ")", cleanPath);
            }
        } catch (Exception e) {
            throw new ResourceNotFoundException("Erro ao buscar recurso como URL: " + path + " (classLoader: " + classLoader.getName() + ")", path, e);
        }

        return resourceUrl;
    }

    private static void walkResourceDirectory(Path classPath, String resourcePath, boolean recursive, Consumer<URL> consumer) throws IOException {

        Path normalizedClassPath = classPath
                .toAbsolutePath()
                .normalize();

        Path resource = normalizedClassPath
                .resolve(resourcePath)
                .normalize();

        if (!resource.startsWith(normalizedClassPath)) {
            throw new ResourceNotFoundException(
                    "O recurso está fora do classpath: "
                            + resourcePath,
                    resourcePath
            );
        }

        if (!Files.exists(resource)) {
            throw new ResourceNotFoundException(
                    "Recurso não encontrado: " + resource,
                    resourcePath
            );
        }

        if (Files.isRegularFile(resource)) {
            consumer.accept(toResourceUrl(resource));
            return;
        }

        try (Stream<Path> paths = recursive
                ? Files.walk(resource)
                : Files.list(resource)) {

            paths
                    .filter(Files::isRegularFile)
                    .map(ResourceUtils::toResourceUrl)
                    .forEach(consumer);
        }
    }

    private static void walkResourceJar(Path jarPath, String resourcePath, boolean recursive, Consumer<URL> consumer) throws IOException {

        if (!walkResourceJarIfExists(
                jarPath,
                resourcePath,
                recursive,
                consumer
        )) {
            throw new ResourceNotFoundException(
                    "Recurso não encontrado no JAR: "
                            + resourcePath,
                    resourcePath
            );
        }
    }

    private static boolean walkResourceJarIfExists(Path jarPath, String resourcePath, boolean recursive, Consumer<URL> consumer) throws IOException {

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            URL jarUrl = jarPath.toUri().toURL();

            return walkJarEntries(
                    jarFile,
                    jarUrl,
                    resourcePath,
                    recursive,
                    consumer
            );
        }
    }

    private static boolean walkJarEntries(JarFile jarFile, URL jarUrl, String resourcePath, boolean recursive, Consumer<URL> consumer) {
        JarEntry exactEntry = jarFile.getJarEntry(resourcePath);

        if (exactEntry != null && !exactEntry.isDirectory()) {
            consumer.accept(
                    toJarResourceUrl(jarUrl, exactEntry.getName())
            );
            return true;
        }

        String prefix = resourcePath.endsWith("/")
                ? resourcePath
                : resourcePath + "/";

        boolean found = false;
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry.isDirectory()) {
                continue;
            }

            String entryName = entry.getName();

            if (!entryName.startsWith(prefix)) {
                continue;
            }

            String relativePath = entryName.substring(
                    prefix.length()
            );

            if (!recursive && relativePath.contains("/")) {
                continue;
            }

            found = true;

            consumer.accept(
                    toJarResourceUrl(jarUrl, entryName)
            );
        }

        return found;
    }

    private static void walkResourceUrl(URL resourceUrl, String resourcePath, boolean recursive, Consumer<URL> consumer) throws Exception {

        switch (resourceUrl.getProtocol()) {
            case "file" -> {
                Path resource = Path.of(resourceUrl.toURI());

                if (Files.isRegularFile(resource)) {
                    consumer.accept(resourceUrl);
                    return;
                }

                try (Stream<Path> paths = recursive
                        ? Files.walk(resource)
                        : Files.list(resource)) {

                    paths
                            .filter(Files::isRegularFile)
                            .map(ResourceUtils::toResourceUrl)
                            .forEach(consumer);
                }
            }

            case "jar" -> {
                JarURLConnection connection =
                        (JarURLConnection) resourceUrl.openConnection();

                connection.setUseCaches(false);

                String entryName = connection.getEntryName();

                if (entryName == null || entryName.isBlank()) {
                    entryName = resourcePath;
                }

                try (JarFile jarFile = connection.getJarFile()) {
                    walkJarEntries(
                            jarFile,
                            connection.getJarFileURL(),
                            entryName,
                            recursive,
                            consumer
                    );
                }
            }

            default -> throw new IllegalStateException(
                    "Protocolo de recurso não suportado: "
                            + resourceUrl.getProtocol()
            );
        }
    }

    private static URL toResourceUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalStateException(
                    "Erro ao converter Path para URL: " + path,
                    exception
            );
        }
    }

    private static URL toJarResourceUrl(URL jarUrl, String entryName) {
        try {
            String schemeSpecificPart =
                    jarUrl.toExternalForm()
                            + "!/"
                            + entryName;

            return new URI(
                    "jar",
                    schemeSpecificPart,
                    null
            ).toURL();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Erro ao criar URL do recurso no JAR: "
                            + entryName,
                    exception
            );
        }
    }

    public static String normalizeResourcePath(String path) {
        if (path == null || path.isBlank()) {
            throw new ResourceNotFoundException(
                    "O caminho do recurso não pode ser nulo ou vazio",
                    path
            );
        }

        String normalized = path
                .trim()
                .replace('\\', '/');

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(
                    0,
                    normalized.length() - 1
            );
        }

        if (normalized.isBlank()) {
            throw new ResourceNotFoundException(
                    "O caminho do recurso não pode ser vazio",
                    path
            );
        }

        for (String part : normalized.split("/")) {
            if ("..".equals(part)) {
                throw new IllegalArgumentException(
                        "O caminho não pode conter '..': " + path
                );
            }
        }

        return normalized;
    }
}
