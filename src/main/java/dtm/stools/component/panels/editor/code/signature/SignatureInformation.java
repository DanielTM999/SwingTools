package dtm.stools.component.panels.editor.code.signature;

import java.util.List;

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
