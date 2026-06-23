package dtm.stools.component.panels.editor.code.ghost;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Provider de "ghost text" (texto fantasma / shadow text) inline, no estilo das IDEs modernas.
 *
 * <p>Retorna o trecho sugerido a ser desenhado de forma semitransparente no cursor. O texto é
 * puramente visual (overlay): não existe no documento, não é selecionável e não responde a mouse
 * ou hover. Se o usuário aceitar (tecla configurável, default {@code Tab}), a string é inserida no
 * offset do cursor.</p>
 *
 * <p>O texto retornado é uma única {@link String} e pode conter quebras de linha ({@code \n}) para
 * sugestões multilinha. Retornar {@code null} ou uma string vazia significa "sem sugestão".</p>
 */
@FunctionalInterface
public interface GhostTextProvider extends CodeEditorProvider {

    String getGhostText(GhostTextContext context);

    default CompletableFuture<String> getGhostTextAsync(GhostTextContext context, Executor executor) {
        return CompletableFuture.supplyAsync(() -> getGhostText(context), executor);
    }
}
