package dtm.stools.component.panels.editor.code.signature;

import java.util.List;

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

    public int safeActiveSignature() {
        if (signatures.isEmpty()) return -1;
        return Math.max(0, Math.min(activeSignature, signatures.size() - 1));
    }

    public SignatureInformation activeSignatureInfo() {
        int idx = safeActiveSignature();
        return idx < 0 ? null : signatures.get(idx);
    }

    public int safeActiveParameter() {
        SignatureInformation info = activeSignatureInfo();
        if (info == null) return -1;
        int param = info.activeParameter() >= 0 ? info.activeParameter() : activeParameter;
        if (param < 0 || info.parameters().isEmpty()) return -1;
        return Math.min(param, info.parameters().size() - 1);
    }

    public SignatureHelp withActiveSignature(int signatureIndex) {
        return new SignatureHelp(signatures, signatureIndex, activeParameter);
    }
}
