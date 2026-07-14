package dtm.stools.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dtm.stools.utils.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;


public final class I18n {

    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final static Map<Locale, Map<String, String>> TEXTS = new ConcurrentHashMap<>();
    private static final AtomicReference<Locale> LOCALE_REF = new AtomicReference<>(Locale.getDefault());

    static {
        load(I18n.class);
    }

    private I18n() {
        throw new AssertionError("No dtm.stools.i18n.I18n instances for you!");
    }


    public static void load(Class<?> loaderClass){
        load(loaderClass, null);
    }

    public static void load(Class<?> loaderClass, Consumer<Throwable> exceptionHandler){
        ResourceUtils.walkResources(loaderClass, "languages", false, url -> {
            try{
                loadFromURL(url);
            }catch (Exception e){
                if(exceptionHandler != null){
                    exceptionHandler.accept(e);
                }
            }
        });
    }

    public static void load(ClassLoader loader) {
        load(loader, null);
    }

    public static void load(ClassLoader loader, Consumer<Throwable> exceptionHandler) {
        ResourceUtils.walkResources(loader, "languages", false, url -> {
            try{
                loadFromURL(url);
            }catch (Exception e){
                if(exceptionHandler != null){
                    exceptionHandler.accept(e);
                }
            }
        });
    }


    public static boolean setLocale(Locale locale) {
        boolean localeExiste = Arrays.stream(Locale.getAvailableLocales())
                .anyMatch(availableLocale ->
                        availableLocale.toLanguageTag()
                                .equalsIgnoreCase(locale.toLanguageTag())
                );

        if (!localeExiste) return false;

        LOCALE_REF.set(locale);
        return true;
    }


    public static String getText(String key, String defaultValue) {
        return getText(key, () -> defaultValue);
    }

    public static String getText(Class<?> ownerClass, String key, String defaultValue) {
        return getText(ownerClass, key, () -> defaultValue);
    }

    public static String getText(Class<?> ownerClass, String key, Supplier<String> defaultValueAction) {
        return getText(key(ownerClass, key), defaultValueAction);
    }

    public static String getText(String key, Supplier<String> defaultValueAction) {
        Locale locale = Objects.requireNonNullElse(
                LOCALE_REF.get(),
                Locale.getDefault()
        );
        Map<String, String> texts = TEXTS.computeIfAbsent(
                locale,
                keyM -> new ConcurrentHashMap<>()
        );
        String target = texts.get(key);
        return (target != null) ? target : defaultValueAction.get();
    }

    public static String key(Class<?> ownerClass, String key) {
        Objects.requireNonNull(ownerClass, "ownerClass");
        Objects.requireNonNull(key, "key");
        return ownerClass.getSimpleName() + "." + key;
    }


    private static void loadFromURL(URL url){
        File file = new File(url.getPath());
        String fileName = file.getName();

        int extensionIndex = fileName.lastIndexOf('.');

        if (extensionIndex <= 0) {
            throw new IllegalArgumentException("Arquivo sem extensão válida: " + fileName);
        }

        String extension = fileName.substring(extensionIndex + 1);

        if(!"json".equals(extension)){
            throw new IllegalArgumentException(
                    "O arquivo não possui extensão JSON: " + fileName
            );
        }


        String localeName = fileName.substring(0, extensionIndex);

        String languageTag = localeName.replace('_', '-');
        Locale locale = Locale.forLanguageTag(languageTag);

        boolean localeExiste = Arrays.stream(Locale.getAvailableLocales())
                .anyMatch(availableLocale ->
                        availableLocale.toLanguageTag()
                                .equalsIgnoreCase(locale.toLanguageTag())
                );

        if (!localeExiste) {
            throw new IllegalArgumentException("Locale não disponível: " + localeName);
        }

        loadMapFromFile(locale, file);
    }

    private static void loadMapFromFile(Locale locale, File file){
        try {
            Map<String, String> loadedTexts = MAPPER.readValue(
                    file,
                    new TypeReference<Map<String, String>>() {}
            );

            Map<String, String> texts = TEXTS.computeIfAbsent(
                    locale,
                    key -> new ConcurrentHashMap<>()
            );

            texts.putAll(loadedTexts);

        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Não foi possível carregar o arquivo JSON: " + file.getAbsolutePath(),
                    exception
            );
        }
    }

}
