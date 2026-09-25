package dtm.stools.component.panels.editor.sheet.provider;

import java.util.concurrent.CompletionStage;

public interface SheetAiProvider extends SheetProvider {
    CompletionStage<SheetAiResponse> ask(SheetAiRequest request);
}
