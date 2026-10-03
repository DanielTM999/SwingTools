package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.Range;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface SelectionRangeProvider extends CodeEditorProvider {

    List<Range> getSelectionRanges(String buffer, int offset);

    default CompletableFuture<List<Range>> getSelectionRangesAsync(String buffer, int offset, Executor executor) {
        return CompletableFuture.supplyAsync(() -> getSelectionRanges(buffer, offset), executor);
    }
}
