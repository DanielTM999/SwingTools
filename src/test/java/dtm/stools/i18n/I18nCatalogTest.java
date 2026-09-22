package dtm.stools.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nCatalogTest {

    private final Locale originalLocale = I18n.getLocale();
    private final I18nLoadStrategy originalStrategy = I18n.getLoadStrategy();

    @AfterEach
    void restoreConfiguration() {
        I18n.setLoadStrategy(originalStrategy);
        I18n.setLocale(originalLocale);
    }

    @Test
    void internalCatalogsUseSwingToolsNamespaceAndAreLoaded() {
        for (String languageTag : List.of("pt-BR", "en-US", "es-ES")) {
            String path = "/META-INF/swingtools/languages/" + languageTag + ".json";
            assertNotNull(I18n.class.getResource(path), path);
        }

        assertTrue(I18n.setLocale(Locale.forLanguageTag("pt-BR")));
        assertEquals("Buscar", I18n.getText("SearchPanel.field.find", "missing"));
    }

    @Test
    void applicationCatalogsStillLoadFromLanguagesAndOverrideInternalTexts(@TempDir Path directory) throws Exception {
        Locale locale = Locale.forLanguageTag("en-US");
        assertTrue(I18n.setLocale(locale));
        I18n.setLoadStrategy(I18nLoadStrategy.KEEP_LAST);
        String key = "Dialogs.title.info";
        String original = I18n.getText(key, "missing");

        Path languages = Files.createDirectories(directory.resolve("languages"));
        char quote = 34;
        String catalog = "{" + quote + key + quote + ":" + quote + "Application information" + quote + "}";
        Files.writeString(languages.resolve("en-US.json"), catalog);
        List<Throwable> failures = new ArrayList<>();

        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{directory.toUri().toURL()}, null)) {
            I18n.load(loader, failures::add);
            assertTrue(failures.isEmpty(), () -> failures.toString());
            assertEquals("Application information", I18n.getText(key, "missing"));
        } finally {
            I18n.load(locale, List.of(new I18nElement(key, original)));
        }
    }
}
