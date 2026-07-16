package dtm.stools.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dtm.stools.utils.ResourceUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class I18n {

    private static final AtomicReference<I18nLoadStrategy> LOAD_STRATEGY_REF = new AtomicReference<>(I18nLoadStrategy.KEEP_LAST);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<I18nElement> REQUESTED_ELEMENTS = ConcurrentHashMap.newKeySet();
    private static final Map<Locale, Map<String, String>> TEXTS = new ConcurrentHashMap<>();
    private static final AtomicReference<Locale> LOCALE_REF = new AtomicReference<>(Locale.getDefault());

    static {
        load(I18n.class);
    }

    private I18n() {
        throw new AssertionError("No dtm.stools.i18n.I18n instances for you!");
    }

    public static I18nLoadStrategy getLoadStrategy() {
        return LOAD_STRATEGY_REF.get();
    }

    public static void setLoadStrategy(I18nLoadStrategy loadStrategy) {
        LOAD_STRATEGY_REF.set(Objects.requireNonNull(loadStrategy, "loadStrategy must not be null"));
    }

    public static void load(Class<?> loaderClass){
        load(loaderClass, null);
    }

    public static void load(Class<?> loaderClass, Consumer<Throwable> exceptionHandler){
        try{
            ResourceUtils.walkResources(loaderClass, "languages", false, url -> {
                try{
                    loadFromURL(url);
                }catch (Exception e){
                    if(exceptionHandler != null){
                        exceptionHandler.accept(e);
                    }
                }
            });
        }catch (Exception e){
            if(exceptionHandler != null){
                exceptionHandler.accept(e);
            }
        }
    }

    public static void load(Locale locale, Collection<I18nElement> elements){
        load(locale, elements, null);
    }

    public static void load(Locale locale, Collection<I18nElement> elements, Consumer<Throwable> exceptionHandler){
        try {
            Objects.requireNonNull(locale, "locale must not be null");
            Objects.requireNonNull(elements, "elements must not be null");
            Map<String, String> texts = TEXTS.computeIfAbsent(
                    locale,
                    key -> new ConcurrentHashMap<>()
            );

            for(I18nElement element : elements){
                String key = element.key();
                if(key == null || key.isEmpty())continue;
                putText(texts, key, element.text());
            }
        }catch (Exception e){
            if(exceptionHandler != null){
                exceptionHandler.accept(e);
            }
        }
    }

    public static void load(ClassLoader loader) {
        load(loader, null);
    }

    public static void load(ClassLoader loader, Consumer<Throwable> exceptionHandler) {
        try{
            ResourceUtils.walkResources(loader, "languages", false, url -> {
                try{
                    loadFromURL(url);
                }catch (Exception e){
                    if(exceptionHandler != null){
                        exceptionHandler.accept(e);
                    }
                }
            });
        }catch (Exception e){
            if(exceptionHandler != null){
                exceptionHandler.accept(e);
            }
        }
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

    public static Locale getLocale(){
        return LOCALE_REF.get() != null ? LOCALE_REF.get() : Locale.getDefault();
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
        Objects.requireNonNull(key, "key must not be null");
        Locale locale = Objects.requireNonNullElse(
                LOCALE_REF.get(),
                Locale.getDefault()
        );
        Map<String, String> texts = TEXTS.computeIfAbsent(
                locale,
                keyM -> new ConcurrentHashMap<>()
        );
        String target = texts.get(key);
        String value = (target != null) ? target : defaultValueAction.get();
        REQUESTED_ELEMENTS.add(new I18nElement(key, value));
        return value;
    }

    public static String key(Class<?> ownerClass, String key) {
        Objects.requireNonNull(ownerClass, "ownerClass");
        Objects.requireNonNull(key, "key");
        return ownerClass + "." + key;
    }

    public static Set<I18nElement> getElementsFromLocale(Locale locale) {
        Map<String, String> map = TEXTS.get(locale);
        if(map == null) return Set.of();

        Set<I18nElement> i18nElements = ConcurrentHashMap.newKeySet();
        map.forEach((key, value) -> i18nElements.add(new I18nElement(key, value)));
        return i18nElements;
    }

    public static Set<I18nLocaleElements> getElements() {
        Set<I18nLocaleElements> i18nElements = ConcurrentHashMap.newKeySet();
        TEXTS.keySet().forEach((key) -> {
            i18nElements.add(new I18nLocaleElements(key, getElementsFromLocale(key)));
        });
        return i18nElements;
    }

    public static Set<Locale> getRegisteredLocales() {
        Set<Locale> locales = ConcurrentHashMap.newKeySet();
        locales.addAll(TEXTS.keySet());
        return locales;
    }

    private static void loadFromURL(URL url){
        String fileName = getResourceFileName(url);

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

        loadMapFromURL(locale, url);
    }

    private static String getResourceFileName(URL url) {
        String path = url.getPath();
        int separatorIndex = path.lastIndexOf('/');

        String fileName = separatorIndex >= 0
                ? path.substring(separatorIndex + 1)
                : path;

        return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
    }

    private static void loadMapFromURL(Locale locale, URL url){
        try (InputStream inputStream = openResourceStream(url)) {
            Map<String, String> loadedTexts = MAPPER.readValue(
                    inputStream,
                    new TypeReference<Map<String, String>>() {}
            );

            Map<String, String> texts = TEXTS.computeIfAbsent(
                    locale,
                    key -> new ConcurrentHashMap<>()
            );

            loadedTexts.forEach((key, value) -> {
                if(key != null && !key.isEmpty()){
                    putText(texts, key, value);
                }
            });

        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Não foi possível carregar o arquivo JSON: " + url,
                    exception
            );
        }
    }

    private static InputStream openResourceStream(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    private static void putText(Map<String, String> texts, String key, String value) {
        if (Objects.requireNonNull(LOAD_STRATEGY_REF.get()) == I18nLoadStrategy.KEEP_FIRST) {
            texts.putIfAbsent(key, value);
        } else {
            texts.put(key, value);
        }
    }

}
