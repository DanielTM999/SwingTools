package dtm.stools.component.panels.editor.code.autocomplete;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface AutoCompleteProvider extends CodeEditorProvider {

    List<AutoCompleteItem> getSuggestions(CompletionContext context);

    default CompletableFuture<List<AutoCompleteItem>> getSuggestionsAsync(CompletionContext context, Executor executor) {
        return CompletableFuture.supplyAsync(() -> getSuggestions(context), executor);
    }

    default boolean shouldAutoTrigger(CompletionContext context) {
        return false;
    }
}
