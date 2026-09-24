package dtm.stools.component.panels.editor.word.provider;

import dtm.stools.component.panels.editor.word.api.WordSelection;
import java.awt.Component;
import java.util.Optional;

public interface WordSearchContext {
    Component owner();
    String initialQuery();
    boolean readOnly();
    Optional<WordSelection> currentSelection();
    void validate(String query, WordSearchOptions options);
    Optional<WordSelection> findNext(String query, WordSearchOptions options);
    Optional<WordSelection> findPrevious(String query, WordSearchOptions options);
    int count(String query, WordSearchOptions options);
    int currentIndex(String query, WordSearchOptions options);
    boolean replace(String query, String replacement, WordSearchOptions options);
    int replaceAll(String query, String replacement, WordSearchOptions options);
    void closed();
}
