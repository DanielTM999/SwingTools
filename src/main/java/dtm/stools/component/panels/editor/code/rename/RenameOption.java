package dtm.stools.component.panels.editor.code.rename;

public record RenameOption(String id, String label, boolean defaultValue) {

    public RenameOption {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id");
        label = label == null || label.isBlank() ? id : label;
    }

    public static RenameOption of(String id, String label) {
        return new RenameOption(id, label, false);
    }

    public static RenameOption of(String id, String label, boolean defaultValue) {
        return new RenameOption(id, label, defaultValue);
    }
}
