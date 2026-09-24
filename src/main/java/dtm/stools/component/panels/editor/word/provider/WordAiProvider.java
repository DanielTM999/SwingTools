package dtm.stools.component.panels.editor.word.provider;

import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

public interface WordAiProvider extends WordProvider {
    record Request(String instruction,String text,Locale locale,BooleanSupplier cancelled) {}
    CompletionStage<String> suggest(Request request);
}
