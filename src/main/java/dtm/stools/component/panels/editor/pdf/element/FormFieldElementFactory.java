package dtm.stools.component.panels.editor.pdf.element;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class FormFieldElementFactory extends BasePdfElementFactory {
    private final PdfFormFieldKind kind;

    public FormFieldElementFactory(PdfFormFieldKind kind) {
        super("pdf.factory.field." + kind.name().toLowerCase(java.util.Locale.ROOT), commandId(kind), title(kind),
                icon(kind), "Clique ou arraste na página para criar: " + title(kind).toLowerCase(java.util.Locale.ROOT),
                PdfPlacementMode.DRAG_RECT);
        this.kind = kind;
    }

    @Override public void insert(PdfEditor editor, PdfPlacement placement) throws IOException {
        Optional<String> name = editor.getDialogs().input(editor, title(kind), "Nome do campo:");
        if (name.isEmpty() || name.get().isBlank()) return;
        Rectangle2D.Float area = placement.bounds();
        float x = placement.dragged() ? area.x : placement.point().x;
        float top = placement.dragged() ? area.y + area.height : placement.point().y;
        int page = placement.page();
        switch (kind) {
            case TEXT -> {
                float width = placement.dragged() ? area.width : 180, height = placement.dragged() ? area.height : 24;
                editor.addTextField(page, name.get(), x, top - height, width, height);
            }
            case CHECKBOX -> {
                float size = placement.dragged() ? Math.min(area.width, area.height) : 18;
                editor.addCheckBox(page, name.get(), x, top - size, size);
            }
            case CHOICE -> {
                Optional<String> options = editor.getDialogs().input(editor, title(kind), "Opções separadas por vírgula:");
                if (options.isEmpty()) return;
                float width = placement.dragged() ? area.width : 180, height = placement.dragged() ? area.height : 24;
                editor.addChoiceField(page, name.get(), split(options.get()), x, top - height, width, height);
            }
            case RADIO -> {
                Optional<String> options = editor.getDialogs().input(editor, title(kind), "Valores alfanuméricos separados por vírgula:");
                if (options.isEmpty()) return;
                editor.addRadioGroup(page, name.get(), split(options.get()), x, top - 18, 18, 6);
            }
        }
    }

    private static List<String> split(String value) {
        return Arrays.stream(value.split(",")).map(String::strip).filter(item -> !item.isEmpty()).toList();
    }
    private static String commandId(PdfFormFieldKind kind) {
        return switch (kind) {
            case TEXT -> "pdf.field";
            case CHECKBOX -> "pdf.checkbox";
            case CHOICE -> "pdf.choice";
            case RADIO -> "pdf.radio";
        };
    }
    private static String title(PdfFormFieldKind kind) {
        return switch (kind) {
            case TEXT -> "Campo de texto";
            case CHECKBOX -> "Caixa de seleção";
            case CHOICE -> "Lista";
            case RADIO -> "Opções";
        };
    }
    private static String icon(PdfFormFieldKind kind) {
        return switch (kind) {
            case TEXT -> "field";
            case CHECKBOX -> "checkbox";
            case CHOICE -> "choice";
            case RADIO -> "radio";
        };
    }
}
