package dtm.stools.component.panels.editor.pdf.command;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfChange;
import dtm.stools.component.panels.editor.pdf.api.PdfEdit;
import dtm.stools.component.panels.editor.pdf.api.PdfFieldInfo;
import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfSelection;
import dtm.stools.component.panels.editor.pdf.provider.PdfContextMenuProvider;
import dtm.stools.component.panels.editor.pdf.ui.PdfRibbon;

import javax.swing.Action;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PdfFormFieldMenuProvider implements PdfContextMenuProvider {
    @Override public String id() { return "pdf.menu.formFields"; }

    @Override public List<Action> canvasActions(PdfEditor editor, int page, PdfSelection selection) {
        if (selection.elements().size() != 1 || page < 0) return List.of();
        PdfPageElement element = selection.elements().getFirst();
        if (!element.annotation() || !"Widget".equals(element.type())) return List.of();
        Optional<PdfFieldInfo> found;
        try { found = editor.getDocument().fieldInfo(selection.page(), element.id()); }
        catch (IOException error) { return List.of(); }
        if (found.isEmpty()) return List.of();
        PdfFieldInfo field = found.get();
        int target = selection.page();
        boolean writable = !editor.isReadOnly();
        List<Action> actions = new ArrayList<>();
        switch (field.kind()) {
            case TEXT -> {
                actions.add(action(editor, "Preencher…", "fill-form", writable, () -> editor.editElement(target, element)));
                actions.add(action(editor, "Tamanho da fonte…", "text", writable, () -> editor.getDialogs()
                        .input(editor, "Tamanho da fonte", "Tamanho em pontos (0 ajusta automaticamente):",
                                field.fontSize() == 0 ? "0" : String.valueOf(field.fontSize()))
                        .ifPresent(value -> apply(editor, "Fonte do campo", target, d -> d.setFieldFontSize(field.name(), number(value))))));
                actions.add(toggle(editor, "Várias linhas", field.multiline(), writable, () -> apply(editor, "Várias linhas", target,
                        d -> d.setFieldFlags(field.name(), field.required(), field.readOnly(), !field.multiline()))));
            }
            case CHECKBOX -> actions.add(action(editor, Boolean.parseBoolean(field.value()) ? "Desmarcar" : "Marcar", "checkbox",
                    writable, () -> editor.editElement(target, element)));
            case RADIO -> {
                actions.add(action(editor, "Selecionar esta opção", "radio", writable && !field.widgetValue().isBlank(),
                        () -> editor.editElement(target, element)));
                actions.add(action(editor, "Adicionar opção…", "page-blank", writable, () -> editor.getDialogs()
                        .input(editor, "Adicionar opção", "Valor da nova opção (letras, números ou _):")
                        .ifPresent(value -> apply(editor, "Adicionar opção", target, d -> d.addRadioOption(target, field.name(), value.strip())))));
                actions.add(action(editor, "Renomear opção…", "replace", writable && !field.widgetValue().isBlank(), () -> editor.getDialogs()
                        .input(editor, "Renomear opção", "Novo valor da opção:", field.widgetValue())
                        .ifPresent(value -> apply(editor, "Renomear opção", target, d -> d.renameRadioOption(target, element.id(), value.strip())))));
                actions.add(action(editor, "Excluir opção", "delete", writable, editor::deleteSelection));
            }
            case CHOICE -> {
                actions.add(action(editor, "Escolher valor…", "choice", writable && !field.options().isEmpty(), () -> editor.editElement(target, element)));
                actions.add(action(editor, "Editar itens…", "align", writable, () -> editor.getDialogs()
                        .editList(editor, "Itens da lista", "Um item por linha:", field.options())
                        .ifPresent(values -> apply(editor, "Editar itens", target, d -> d.setFieldOptions(field.name(), values)))));
                actions.add(action(editor, "Tamanho da fonte…", "text", writable, () -> editor.getDialogs()
                        .input(editor, "Tamanho da fonte", "Tamanho em pontos (0 ajusta automaticamente):",
                                field.fontSize() == 0 ? "0" : String.valueOf(field.fontSize()))
                        .ifPresent(value -> apply(editor, "Fonte do campo", target, d -> d.setFieldFontSize(field.name(), number(value))))));
            }
            default -> { }
        }
        actions.add(action(editor, "Renomear campo…", "properties", writable, () -> editor.getDialogs()
                .input(editor, "Renomear campo", "Nome do campo:", field.name())
                .ifPresent(value -> apply(editor, "Renomear campo", target, d -> d.renameField(field.name(), value)))));
        actions.add(toggle(editor, "Obrigatório", field.required(), writable, () -> apply(editor, "Campo obrigatório", target,
                d -> d.setFieldFlags(field.name(), !field.required(), field.readOnly(), field.multiline()))));
        actions.add(toggle(editor, "Somente leitura", field.readOnly(), writable, () -> apply(editor, "Campo somente leitura", target,
                d -> d.setFieldFlags(field.name(), field.required(), !field.readOnly(), field.multiline()))));
        return actions;
    }

    private static void apply(PdfEditor editor, String label, int page, PdfEdit operation) {
        if (editor.perform(() -> editor.edit(label, PdfChange.forPages(page), operation))) editor.reloadSelection();
    }

    private static float number(String value) {
        try { return Float.parseFloat(value.strip().replace(',', '.')); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Número inválido: " + value, error); }
    }

    private static PdfAction action(PdfEditor editor, String name, String icon, boolean enabled, Runnable body) {
        PdfAction action = new PdfAction("pdf.field.menu", name, body, editor::reportError).icon(icon);
        action.setEnabled(enabled);
        return action;
    }

    private static PdfAction toggle(PdfEditor editor, String name, boolean selected, boolean enabled, Runnable body) {
        PdfAction action = action(editor, name, "", enabled, body);
        action.putValue(PdfRibbon.TOGGLE, Boolean.TRUE);
        action.putValue(Action.SELECTED_KEY, selected);
        return action;
    }
}
