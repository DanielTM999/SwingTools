package dtm.stools.component.panels.editor.code;

@FunctionalInterface
public interface InspectionWidgetFactory {
    CodeEditorInspectionWidget create(CodeEditorTextArea textArea);
}
