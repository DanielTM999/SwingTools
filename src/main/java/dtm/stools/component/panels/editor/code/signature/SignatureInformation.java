package dtm.stools.component.panels.editor.code.signature;

import java.util.List;

/**
 * Representa uma assinatura (sobrecarga) de uma funcao ou metodo.
 *
 * <p>O {@code label} e o texto completo da assinatura (ex.: {@code "max(int a, int b)"}).
 * Os {@link ParameterInformation parametros} sao usados para destacar o argumento
 * ativo enquanto o usuario digita.</p>
 *
 * <p>{@code activeParameter} permite que uma sobrecarga especifica sobrescreva o
 * parametro ativo definido em {@link SignatureHelp}; use {@code -1} para herdar.</p>
 */
public record SignatureInformation(
        String label,
        String documentation,
        List<ParameterInformation> parameters,
        int activeParameter
) {

    public SignatureInformation {
        label = label == null ? "" : label;
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    public SignatureInformation(String label) {
        this(label, null, List.of(), -1);
    }

    public SignatureInformation(String label, List<ParameterInformation> parameters) {
        this(label, null, parameters, -1);
    }

    public SignatureInformation(String label, String documentation, List<ParameterInformation> parameters) {
        this(label, documentation, parameters, -1);
    }

    public boolean hasDocumentation() {
        return documentation != null && !documentation.isBlank();
    }
}
