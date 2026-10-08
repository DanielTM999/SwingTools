package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;

import java.util.List;

import static dtm.stools.component.panels.editor.pdf.ui.PdfRibbonItem.component;
import static dtm.stools.component.panels.editor.pdf.ui.PdfRibbonItem.large;
import static dtm.stools.component.panels.editor.pdf.ui.PdfRibbonItem.small;

public final class PdfRibbonLayout {
    private PdfRibbonLayout() {}

    private static PdfRibbonGroup group(String id, String title, int priority, String icon, PdfRibbonItem... items) {
        return new PdfRibbonGroup(id, title, priority, icon, List.of(items));
    }

    public static List<PdfRibbonTab> defaults(PdfEditor editor, PdfRibbon ribbon) {
        PdfRibbonTab file = new PdfRibbonTab("file", "Arquivo", List.of(
                group("document", "Documento", 100, "save", large("pdf.new"), large("pdf.open"), large("pdf.save"),
                        small("pdf.saveAs"), small("pdf.print"), small("pdf.title")),
                group("export", "Exportar", 60, "extract", large("pdf.extract"))), false);
        PdfRibbonTab home = new PdfRibbonTab("home", "Página Inicial", List.of(
                group("clipboard", "Área de Transferência", 30, "paste", large("pdf.paste"), small("pdf.cut"),
                        small("pdf.copy"), small("pdf.duplicate")),
                group("tools", "Ferramentas", 100, "view", large("pdf.view"), large("pdf.select"), large("pdf.selectArea"), large("pdf.eraser")),
                group("eraser", "Borracha", 25, "eraser", small("pdf.eraser.brush"), small("pdf.eraser.rect"),
                        component(ribbon::eraserSizeBox)),
                group("editing", "Edição", 80, "delete", large("pdf.eraseSelection"), small("pdf.selectAll"),
                        small("pdf.editText"), small("pdf.rotateSelection")),
                group("font", "Texto", 70, "text", component(ribbon::fontBox), component(ribbon::sizeBox),
                        component(ribbon::textFormatButtons)),
                group("quickInsert", "Inserir", 60, "text", large("pdf.addText"), large("pdf.image"), small("pdf.shapes"),
                        small("pdf.draw"), small("pdf.note")),
                group("zoom", "Zoom", 40, "zoom-in", small("pdf.zoomIn"), small("pdf.zoomOut"), small("pdf.zoom100"),
                        small("pdf.fitWidth"), small("pdf.fitPage")),
                group("history", "Histórico", 20, "undo", small("pdf.undo"), small("pdf.redo"))), false);
        PdfRibbonTab insert = new PdfRibbonTab("insert", "Inserir", List.of(
                group("text", "Texto", 100, "text", large("pdf.addText")),
                group("illustrations", "Ilustrações", 90, "image", large("pdf.image"), small("pdf.square"), small("pdf.ellipse"),
                        small("pdf.line"), small("pdf.arrow"), large("pdf.draw")),
                group("annotations", "Anotações", 80, "note", large("pdf.note"), large("pdf.highlight")),
                group("shapeStyle", "Estilo", 50, "color-stroke", component(ribbon::shapeStyleButtons),
                        component(ribbon::lineWidthBox)),
                group("pages", "Páginas", 60, "page-blank", large("pdf.blankPage"), large("pdf.insertPages"))), false);
        PdfRibbonTab organize = new PdfRibbonTab("organize", "Organizar", List.of(
                group("object", "Objeto", 100, "forward", large("pdf.forward"), large("pdf.backward"),
                        small("pdf.rotateSelection"), small("pdf.rotateSelectionLeft"), small("pdf.rotateFree"), large("pdf.align")),
                group("pages", "Páginas", 90, "pages", large("pdf.rotate"), small("pdf.rotateLeft"), small("pdf.moveUp"),
                        small("pdf.moveDown"), large("pdf.delete")),
                group("navigate", "Navegar", 50, "next", small("pdf.previous"), small("pdf.next"), small("pdf.goTo"))), false);
        PdfRibbonTab forms = new PdfRibbonTab("forms", "Formulários", List.of(
                group("fields", "Campos", 100, "field", large("pdf.field"), large("pdf.checkbox"), large("pdf.choice"),
                        large("pdf.radio")),
                group("fill", "Preenchimento", 80, "fill-form", large("pdf.fill"))), false);
        PdfRibbonTab review = new PdfRibbonTab("review", "Assinar e Revisar", List.of(
                group("signatures", "Assinaturas", 100, "sign", large("pdf.visualSign"), large("pdf.digitalSign"),
                        large("pdf.validate")),
                group("search", "Localizar", 90, "find", large("pdf.find"), large("pdf.replace")),
                group("recognition", "Reconhecimento", 80, "ocr", large("pdf.ocr"))), false);
        PdfRibbonTab view = new PdfRibbonTab("view", "Exibir", List.of(
                group("mode", "Modo", 100, "view-continuous", large("pdf.view.single"), large("pdf.view.continuous")),
                group("panels", "Painéis", 80, "sidebar", large("pdf.toggleSidebar"), small("pdf.toggleStatus"),
                        small("pdf.text")),
                group("zoom", "Zoom", 70, "zoom-in", large("pdf.zoomIn"), large("pdf.zoomOut"), small("pdf.zoom100"),
                        small("pdf.fitWidth"), small("pdf.fitPage"))), false);
        PdfRibbonTab format = new PdfRibbonTab("format", "Formato do Objeto", List.of(
                group("style", "Estilo", 100, "color-stroke", component(ribbon::shapeStyleButtons), component(ribbon::lineWidthBox)),
                group("text", "Texto", 90, "text", component(ribbon::fontBox), component(ribbon::sizeBox),
                        component(ribbon::textFormatButtons)),
                group("arrange", "Organizar", 80, "forward", large("pdf.forward"), large("pdf.backward"),
                        small("pdf.rotateSelection"), small("pdf.rotateSelectionLeft"), small("pdf.duplicate"),
                        large("pdf.eraseSelection"))), true);
        return List.of(file, home, insert, organize, forms, review, view, format);
    }
}
