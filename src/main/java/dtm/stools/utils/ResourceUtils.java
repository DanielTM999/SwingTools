package dtm.stools.utils;

import dtm.stools.exceptions.ResourceNotFoundException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;

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
            if (path == null || path.isBlank()) {
                throw new ResourceNotFoundException("O caminho do recurso não pode ser nulo ou vazio", path);
            }
            String normalizedPath = path.startsWith("/") ? path : "/" + path;

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
            if (path == null || path.isBlank()) {
                throw new ResourceNotFoundException("O caminho do recurso não pode ser nulo ou vazio", path);
            }
            String normalizedPath = path.startsWith("/") ? path : "/" + path;

            URL resource = classLoader.getResource(normalizedPath);
            if (resource == null) {
                throw new ResourceNotFoundException("Recurso não encontrado: " + normalizedPath + " (classLoader: " + classLoader.getName() + ")", normalizedPath);
            }

            return resource;
        }
    }

    public static InputStream getResourceAsStream(String path) {
        return getResourceAsStream(ResourceUtils.class, path);
    }

    public static InputStream getResourceAsStream(Class<?> aClass, String path) {
        return getResourceAsStream(aClass, path, false);
    }

    public static InputStream getResourceAsStream(Class<?> aClass, String path, boolean classLoader) {
        if (path == null || path.isBlank()) {
            throw new ResourceNotFoundException("O caminho do recurso não pode ser nulo ou vazio", path);
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        InputStream stream;
        if(classLoader){
            String cleanPath = normalizedPath.startsWith("/") ? normalizedPath.substring(1) : normalizedPath;
            stream = aClass.getClassLoader().getResourceAsStream(cleanPath);
        }else{
            stream = aClass.getResourceAsStream(normalizedPath);
        }
        if (stream == null) {
            throw new ResourceNotFoundException("Recurso não encontrado como stream: " + normalizedPath + " (classe: " + aClass.getName() + ")", normalizedPath);
        }
        return stream;
    }

    public static InputStream getExternalResourceAsStream(Class<?> aClass, String path) {
        if (path == null || path.isBlank()) {
            throw new ResourceNotFoundException("O caminho do recurso não pode ser nulo ou vazio", path);
        }

        Path jarPath = getResourceClassPath(aClass);
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;

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
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        byte[] bytes = null;
        try(URLClassLoader loader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()})){
            InputStream stream = loader.getResourceAsStream(cleanPath);

            if (stream == null) {
                throw new ResourceNotFoundException("Recurso não encontrado como stream: " + cleanPath + " (classe: " + aClass.getName() + ")", cleanPath);
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
        if (path == null || path.isBlank()) {
            throw new ResourceNotFoundException("O caminho do recurso não pode ser nulo ou vazio", path);
        }

        Path jarPath = getResourceClassPath(aClass);
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
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
        if (path == null || path.isBlank()) {
            throw new ResourceNotFoundException("O caminho do recurso não pode ser nulo ou vazio", path);
        }
        URL resourceUrl = null;
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
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



}
