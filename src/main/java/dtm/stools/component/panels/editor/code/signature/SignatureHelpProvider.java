package dtm.stools.component.panels.editor.code.signature;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface SignatureHelpProvider extends CodeEditorProvider {

    SignatureHelp provideSignatureHelp(SignatureHelpContext context);

    default CompletableFuture<SignatureHelp> provideSignatureHelpAsync(SignatureHelpContext context, Executor executor) {
        return CompletableFuture.supplyAsync(() -> provideSignatureHelp(context), executor);
    }

    default Set<Character> getTriggerCharacters() {
        return Set.of('(');
    }

    default Set<Character> getRetriggerCharacters() {
        return Set.of(',');
    }
}
