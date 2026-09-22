package dtm.stools.component.panels.editor.code.search;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.inputfields.textfield.MaskedTextField;
import dtm.stools.i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchPanelI18nTest {

    private final Locale originalLocale = I18n.getLocale();

    @AfterEach
    void restoreLocale() {
        I18n.setLocale(originalLocale);
    }

    @Test
    void localizesTextsPlaceholdersAndTooltipsFromInternalCatalogs() {
        assertPanelTexts("pt-BR", "0 resultados", "Substituir", "Todos", "Buscar", "Substituir",
                "Alternar substituição", "Diferenciar maiúsculas/minúsculas", "Palavra inteira",
                "Regex", "Resultado anterior", "Próximo resultado", "Fechar (Esc)",
                "Substituir resultado atual", "Substituir todos os resultados");
        assertPanelTexts("en-US", "0 results", "Replace", "All", "Find", "Replace",
                "Toggle replace", "Match case", "Whole word", "Regex", "Previous match",
                "Next match", "Close (Esc)", "Replace current match", "Replace all matches");
        assertPanelTexts("es-ES", "0 resultados", "Reemplazar", "Todo", "Buscar", "Reemplazar",
                "Alternar reemplazo", "Coincidir mayúsculas/minúsculas", "Palabra completa",
                "Regex", "Resultado anterior", "Siguiente resultado", "Cerrar (Esc)",
                "Reemplazar resultado actual", "Reemplazar todos los resultados");
    }

    private static void assertPanelTexts(String languageTag, String count, String replace, String replaceAll,
                                         String findPlaceholder, String replacePlaceholder, String toggleReplace,
                                         String matchCase, String wholeWord, String regex, String previous,
                                         String next, String close, String replaceCurrent, String replaceAllTooltip) {
        assertTrue(I18n.setLocale(Locale.forLanguageTag(languageTag)));
        CodeEditorTextArea area = new CodeEditorTextArea();
        SearchPanel panel = new SearchPanel(area);

        assertAll(languageTag,
                () -> assertEquals(count, panel.getCountLabel().getText()),
                () -> assertEquals(replace, panel.getReplaceButton().getText()),
                () -> assertEquals(replaceAll, panel.getReplaceAllButton().getText()),
                () -> assertEquals(findPlaceholder, placeholderOf(panel.getFindField())),
                () -> assertEquals(findPlaceholder, panel.getFindField().getToolTipText()),
                () -> assertEquals(replacePlaceholder, placeholderOf(panel.getReplaceField())),
                () -> assertEquals(replacePlaceholder, panel.getReplaceField().getToolTipText()),
                () -> assertEquals(toggleReplace, panel.getExpandToggle().getToolTipText()),
                () -> assertEquals(matchCase, panel.getCaseToggle().getToolTipText()),
                () -> assertEquals(wholeWord, panel.getWordToggle().getToolTipText()),
                () -> assertEquals(regex, panel.getRegexToggle().getToolTipText()),
                () -> assertEquals(previous, panel.getPrevButton().getToolTipText()),
                () -> assertEquals(next, panel.getNextButton().getToolTipText()),
                () -> assertEquals(close, panel.getCloseButton().getToolTipText()),
                () -> assertEquals(replaceCurrent, panel.getReplaceButton().getToolTipText()),
                () -> assertEquals(replaceAllTooltip, panel.getReplaceAllButton().getToolTipText()));

        area.removeNotify();
    }

    private static String placeholderOf(MaskedTextField field) {
        try {
            java.lang.reflect.Field placeholder = MaskedTextField.class.getDeclaredField("placeholderText");
            placeholder.setAccessible(true);
            return (String) placeholder.get(field);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
