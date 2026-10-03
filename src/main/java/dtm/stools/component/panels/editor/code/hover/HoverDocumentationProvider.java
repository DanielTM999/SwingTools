package dtm.stools.component.panels.editor.code.hover;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface HoverDocumentationProvider extends CodeEditorProvider {

    HoverInfo provideHover(HoverDocumentationContext context);

    default CompletableFuture<HoverInfo> provideHoverAsync(HoverDocumentationContext context, Executor executor) {
        return CompletableFuture.supplyAsync(() -> provideHover(context), executor);
    }
}
