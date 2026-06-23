package dtm.stools.component.panels.editor.code.signature;

/**
 * Representa um parametro individual de uma assinatura. O {@code label} deve,
 * preferencialmente, ser um trecho exato do rotulo da {@link SignatureInformation}
 * a que pertence, permitindo que o popup destaque o parametro ativo.
 */
public record ParameterInformation(String label, String documentation) {

    public ParameterInformation {
        label = label == null ? "" : label;
    }

    public ParameterInformation(String label) {
        this(label, null);
    }

    public boolean hasDocumentation() {
        return documentation != null && !documentation.isBlank();
    }
}
