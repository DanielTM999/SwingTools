package dtm.stools.component.panels.editor.code.signature;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Fornece informacoes de assinatura (signature help) para chamadas de funcao/metodo,
 * no estilo do recurso de mesmo nome do LSP. O editor consulta o provider quando o
 * usuario digita um caractere de gatilho (por padrao {@code '('}), retrigger
 * ({@code ','}) ou aciona explicitamente o atalho.
 *
 * <p>O provider deve inspecionar o {@link SignatureHelpContext} (em especial o caret)
 * para decidir quais sobrecargas mostrar e qual parametro destacar. Retornar
 * {@code null} ou uma {@link SignatureHelp} vazia oculta o popup.</p>
 */
@FunctionalInterface
public interface SignatureHelpProvider extends CodeEditorProvider {

    /**
     * Calcula o signature help para a posicao atual. Pode ser invocado em uma
     * thread de background; nao deve tocar a UI Swing diretamente.
     *
     * @return as assinaturas a exibir, ou {@code null} para ocultar o popup
     */
    SignatureHelp provideSignatureHelp(SignatureHelpContext context);

    default CompletableFuture<SignatureHelp> provideSignatureHelpAsync(SignatureHelpContext context, Executor executor) {
        return CompletableFuture.supplyAsync(() -> provideSignatureHelp(context), executor);
    }

    /** Caracteres que disparam o signature help ao serem digitados. */
    default Set<Character> getTriggerCharacters() {
        return Set.of('(');
    }

    /** Caracteres que reavaliam o signature help quando ja exibido (ex.: virgula). */
    default Set<Character> getRetriggerCharacters() {
        return Set.of(',');
    }
}
