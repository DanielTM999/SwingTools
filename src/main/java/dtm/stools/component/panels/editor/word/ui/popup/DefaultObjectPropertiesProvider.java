package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.provider.*;

import javax.swing.*;
import java.awt.*;

public final class DefaultObjectPropertiesProvider implements WordObjectPropertiesProvider, AutoCloseable {
    @Override public String id() { return "word.popup.properties.default"; }

    private final java.util.Set<WordPropertiesActivity<?>> open = new java.util.HashSet<>();
    private final DefaultDialogProvider messages = new DefaultDialogProvider();
    @Override public WordPopupHandle show(WordObjectPropertiesContext context) {
        try {
            WordPropertiesPanel<?> panel = panel(context);
            if (panel == null) {
                String label = context.object().map(o -> o instanceof WordOpaqueObject opaque
                        ? "Conteúdo preservado: " + opaque.label() + ". Ele é mantido ao salvar, mas não possui editor."
                        : "Este objeto não possui propriedades editáveis.").orElse("Nada selecionado.");
                messages.show(new WordDialogRequest<>(context.owner(), "word.properties.info", "Propriedades", label,
                        new JPanel(), () -> Boolean.TRUE, value -> {}, "Fechar", true, true));
            } else showPanel(context, panel);
        } finally { context.closed(); }
        return WordPopupHandle.closed();
    }
    private <T> void showPanel(WordObjectPropertiesContext context, WordPropertiesPanel<T> panel) {
        WordPropertiesActivity<T> activity = new WordPropertiesActivity<>(context.owner(), panel, context.readOnly(), value -> {
            if (value instanceof WordTable table) context.applyTable(table);
            else if (value instanceof WordInlineObject object) context.applyObject(object);
        });
        open.add(activity);
        activity.onClosed(() -> open.remove(activity));
        try {
            activity.showResult();
        } finally { activity.dispose(); }
    }
    @Override public void close() {
        for (var activity : java.util.List.copyOf(open)) activity.dispose();
        messages.close();
    }
    public boolean isOpen() { return open.stream().anyMatch(java.awt.Window::isDisplayable); }
    private static WordPropertiesPanel<?> panel(WordObjectPropertiesContext context) {
        if (context.table().isPresent()) {
            var cells = context.cells().orElse(null);
            return new WordTablePropertiesPanel(context.table().get(),cells,cells == null ? 0 : cells.firstRow(),cells == null ? 0 : cells.firstColumn());
        }
        return switch (context.object().orElse(null)) {
            case WordImage image -> new WordImagePropertiesPanel(image);
            case WordChart chart -> new WordChartEditorPanel(chart,context.document(),context.registry());
            case WordShape shape -> new WordShapePropertiesPanel(shape);
            case WordEquation equation -> new WordEquationEditorPanel(equation);
            case WordDiagram diagram -> new WordDiagramEditorPanel(diagram,context.document(),context.registry());
            case WordFormField field -> new WordFormFieldPanel(field);
            case null, default -> null;
        };
    }
}
