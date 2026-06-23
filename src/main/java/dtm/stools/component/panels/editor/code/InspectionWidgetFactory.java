package dtm.stools.component.panels.editor.code;

/**
 * Fábrica para criar a implementação do widget de inspeção usado pelo editor.
 * Permite ao usuário fornecer uma subclasse de {@link CodeEditorInspectionWidget}
 * (mudando apenas as partes visuais via override) sem perder o comportamento
 * padrão (contagem alimentada pelos diagnostics, navegação e evento de clique).
 *
 * <pre>{@code
 * editor.setInspectionWidgetFactory(textArea -> new CodeEditorInspectionWidget(textArea) {
 *     @Override
 *     protected void paintWidgetBackground(Graphics2D g2) {
 *         // aparência custom
 *     }
 * });
 * }</pre>
 */
@FunctionalInterface
public interface InspectionWidgetFactory {
    CodeEditorInspectionWidget create(CodeEditorTextArea textArea);
}
