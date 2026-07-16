package dtm.stools.component.panels.editor.code.signature;

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
