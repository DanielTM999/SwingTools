package dtm.stools.component.panels.editor.code.signature;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

/**
 * Contexto passado ao {@link SignatureHelpProvider} quando o editor solicita
 * informacoes de assinatura.
 *
 * @param buffer              snapshot imutavel do conteudo do editor
 * @param caretOffset         posicao absoluta do caret no buffer
 * @param caretLine           linha do caret (base zero)
 * @param caretCol            coluna do caret (base zero)
 * @param triggerKind         como a consulta foi disparada
 * @param triggerCharacter    caractere que disparou a consulta, ou {@code '\0'}
 * @param retrigger           {@code true} se um popup de signature help ja estava visivel
 * @param activeSignatureHelp resultado atualmente exibido (em caso de retrigger), ou {@code null}
 */
public record SignatureHelpContext(
        TextBuffer buffer,
        int caretOffset,
        int caretLine,
        int caretCol,
        TriggerKind triggerKind,
        char triggerCharacter,
        boolean retrigger,
        SignatureHelp activeSignatureHelp
) {

    public enum TriggerKind {
        /** Disparo explicito (atalho de teclado ou API). */
        INVOKED,
        /** Disparo por digitacao de um caractere de gatilho/retrigger. */
        TRIGGER_CHARACTER,
        /** Reavaliacao apos mudanca de conteudo ou movimentacao do caret. */
        CONTENT_CHANGE
    }

    public String currentLine() {
        return buffer.lineAt(caretLine);
    }

    public boolean hasTriggerCharacter() {
        return triggerCharacter != '\0';
    }

    /** Trecho da linha atual a esquerda do caret. */
    public String textBeforeCaret() {
        String line = currentLine();
        return line.substring(0, Math.min(caretCol, line.length()));
    }
}
