package dtm.stools.component.panels.editor.code.signature;

import java.util.List;

/**
 * Resultado de uma consulta de signature help: a lista de assinaturas
 * (sobrecargas) disponiveis, a assinatura ativa e o parametro ativo.
 *
 * <p>{@code activeSignature} e o indice da sobrecarga a ser exibida; {@code activeParameter}
 * e o indice do argumento atualmente em edicao. Ambos sao limitados aos intervalos
 * validos pelos metodos de consulta.</p>
 */
public record SignatureHelp(
        List<SignatureInformation> signatures,
        int activeSignature,
        int activeParameter
) {

    public SignatureHelp {
        signatures = signatures == null ? List.of() : List.copyOf(signatures);
        if (activeSignature < 0) activeSignature = 0;
        if (activeParameter < 0) activeParameter = -1;
    }

    public SignatureHelp(List<SignatureInformation> signatures, int activeParameter) {
        this(signatures, 0, activeParameter);
    }

    public boolean isEmpty() {
        return signatures.isEmpty();
    }

    public int signatureCount() {
        return signatures.size();
    }

    /** Indice da assinatura ativa limitado ao intervalo valido. */
    public int safeActiveSignature() {
        if (signatures.isEmpty()) return -1;
        return Math.max(0, Math.min(activeSignature, signatures.size() - 1));
    }

    /** Assinatura ativa, ou {@code null} se a lista estiver vazia. */
    public SignatureInformation activeSignatureInfo() {
        int idx = safeActiveSignature();
        return idx < 0 ? null : signatures.get(idx);
    }

    /**
     * Indice do parametro ativo, considerando a sobrecarga ativa (que pode
     * sobrescrever via {@link SignatureInformation#activeParameter()}).
     * Retorna {@code -1} quando nenhum parametro esta ativo.
     */
    public int safeActiveParameter() {
        SignatureInformation info = activeSignatureInfo();
        if (info == null) return -1;
        int param = info.activeParameter() >= 0 ? info.activeParameter() : activeParameter;
        if (param < 0 || info.parameters().isEmpty()) return -1;
        return Math.min(param, info.parameters().size() - 1);
    }

    /** Cria uma copia desta {@code SignatureHelp} com outra assinatura ativa. */
    public SignatureHelp withActiveSignature(int signatureIndex) {
        return new SignatureHelp(signatures, signatureIndex, activeParameter);
    }
}
