package dtm.stools.component.panels.editor.pdf.command;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfEraserMode;
import dtm.stools.component.panels.editor.pdf.api.PdfPageElement;
import dtm.stools.component.panels.editor.pdf.api.PdfSelection;
import dtm.stools.component.panels.editor.pdf.api.PdfViewMode;
import dtm.stools.component.panels.editor.pdf.provider.PdfOcrProvider;

import javax.swing.SwingUtilities;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PdfCommandCatalog {
    private PdfCommandCatalog() {}

    public static void registerAll(PdfEditor editor, PdfCommands commands) {
        file(editor, commands);
        clipboard(editor, commands);
        tools(editor, commands);
        selection(editor, commands);
        view(editor, commands);
        pages(editor, commands);
        review(editor, commands);
    }

    private static void file(PdfEditor editor, PdfCommands commands) {
        commands.add("pdf.new", "Novo", editor::newDocument).icon("new").tip("Novo PDF (Ctrl+N)");
        commands.add("pdf.open", "Abrir", editor::openWithDialog).icon("open").tip("Abrir PDF (Ctrl+O)");
        commands.add("pdf.save", "Salvar", editor::saveCurrent).icon("save").tip("Salvar (Ctrl+S)");
        commands.add("pdf.saveAs", "Salvar como", editor::saveWithDialog).icon("save-as").tip("Salvar uma cópia (Ctrl+Shift+S)");
        commands.add("pdf.print", "Imprimir", () -> editor.perform(editor::print)).icon("print").tip("Imprimir (Ctrl+P)");
        commands.add("pdf.title", "Propriedades", () -> editor.getDialogs()
                .input(editor, "Propriedades do PDF", "Título:", Optional.ofNullable(editor.getTitle()).orElse(""))
                .ifPresent(value -> editor.perform(() -> editor.setTitle(value)))).icon("properties").edits();
        commands.add("pdf.extract", "Extrair páginas", () -> extractPages(editor)).icon("extract")
                .tip("Gerar um novo PDF só com as páginas escolhidas (Ctrl/Shift+clique nas miniaturas para selecionar)");
    }

    private static void clipboard(PdfEditor editor, PdfCommands commands) {
        commands.add("pdf.paste", "Colar", editor::pasteClipboard).icon("paste").tip("Colar (Ctrl+V)").edits();
        commands.add("pdf.cut", "Recortar", editor::cutSelection).icon("cut").tip("Recortar (Ctrl+X)").edits()
                .when(() -> !editor.getSelection().isEmpty());
        commands.add("pdf.copy", "Copiar", editor::copySelection).icon("copy").tip("Copiar (Ctrl+C)")
                .when(() -> !editor.getSelection().isEmpty());
        commands.add("pdf.duplicate", "Duplicar", editor::duplicateSelection).icon("duplicate").tip("Duplicar (Ctrl+D)").edits()
                .when(() -> !editor.getSelection().isEmpty());
    }

    private static void tools(PdfEditor editor, PdfCommands commands) {
        commands.add(PdfEditor.TOOL_SELECT, "Selecionar", () -> editor.selectTool(PdfEditor.TOOL_SELECT)).icon("cursor")
                .tip("Selecionar, mover, redimensionar e girar objetos; duplo clique edita texto")
                .selected(() -> PdfEditor.TOOL_SELECT.equals(editor.getActiveTool()));
        commands.add(PdfEditor.TOOL_AREA, "Área", () -> editor.selectTool(PdfEditor.TOOL_AREA)).icon("area")
                .tip("Selecionar uma área retangular da página")
                .selected(() -> PdfEditor.TOOL_AREA.equals(editor.getActiveTool()));
        commands.add(PdfEditor.TOOL_ERASER, "Borracha", () -> editor.selectTool(PdfEditor.TOOL_ERASER)).icon("eraser")
                .tip("Arraste sobre a página para apagar o conteúdo da área").edits()
                .selected(() -> PdfEditor.TOOL_ERASER.equals(editor.getActiveTool()));
        commands.add("pdf.eraser.brush", "Pincel", () -> editor.setEraserMode(PdfEraserMode.BRUSH)).icon("eraser")
                .tip("Borracha de pincel: apaga só onde passar").edits()
                .selected(() -> editor.getEraserMode() == PdfEraserMode.BRUSH);
        commands.add("pdf.eraser.rect", "Retângulo", () -> editor.setEraserMode(PdfEraserMode.RECTANGLE)).icon("area")
                .tip("Borracha retangular: apaga a área arrastada").edits()
                .selected(() -> editor.getEraserMode() == PdfEraserMode.RECTANGLE);
        commands.add("pdf.moveSelection", "Mover", () -> editor.selectTool(PdfEditor.TOOL_SELECT)).icon("cursor");
        commands.add("pdf.resizeSelection", "Redimensionar", () -> editor.selectTool(PdfEditor.TOOL_SELECT)).icon("cursor");
        commands.add("pdf.shapes", "Formas", () -> { }).icon("shapes").tip("Inserir formas")
                .menu(List.of("pdf.square", "pdf.ellipse", "pdf.line", "pdf.arrow"));
    }

    private static void selection(PdfEditor editor, PdfCommands commands) {
        commands.add("pdf.eraseSelection", "Excluir", editor::deleteSelection).icon("delete").tip("Excluir a seleção (Delete)")
                .edits().when(() -> !editor.getSelection().isEmpty());
        commands.add("pdf.selectAll", "Selecionar tudo", editor::selectAll).icon("select-all").tip("Selecionar tudo na página (Ctrl+A)");
        commands.add("pdf.editText", "Editar texto", editor::editSelectedText).icon("text").tip("Editar o texto selecionado (F2)")
                .edits().when(() -> {
                    PdfSelection selection = editor.getSelection();
                    return selection.elements().size() == 1 && (selection.elements().getFirst().textual()
                            || selection.elements().getFirst().textBox());
                });
        commands.add("pdf.rotateSelection", "Girar 90°", () -> editor.rotateSelection(90)).icon("rotate-right")
                .tip("Girar a seleção no sentido horário").edits().when(() -> !editor.getSelection().isEmpty());
        commands.add("pdf.rotateSelectionLeft", "Girar -90°", () -> editor.rotateSelection(-90)).icon("rotate-left")
                .tip("Girar a seleção no sentido anti-horário").edits().when(() -> !editor.getSelection().isEmpty());
        commands.add("pdf.rotateFree", "Girar…", () -> editor.getDialogs()
                .input(editor, "Girar seleção", "Ângulo em graus (sentido horário):", "15").ifPresent(value -> {
                    try { editor.rotateSelection(Double.parseDouble(value.replace(',', '.'))); }
                    catch (NumberFormatException error) { editor.reportError(new IllegalArgumentException("Ângulo inválido", error)); }
                })).icon("rotate-free").edits().when(() -> !editor.getSelection().isEmpty());
        commands.add("pdf.forward", "Trazer à frente", () -> editor.arrangeSelection(true)).icon("forward").edits()
                .when(() -> singleAnnotation(editor));
        commands.add("pdf.backward", "Enviar para trás", () -> editor.arrangeSelection(false)).icon("backward").edits()
                .when(() -> singleAnnotation(editor));
        commands.add("pdf.align", "Alinhar", () -> { }).icon("align").tip("Alinhar objetos selecionados")
                .menu(List.of("pdf.align.left", "pdf.align.center", "pdf.align.right", "-", "pdf.align.top",
                        "pdf.align.middle", "pdf.align.bottom")).edits().when(() -> !editor.getSelection().isEmpty());
        align(editor, commands, "left", "À esquerda");
        align(editor, commands, "center", "Ao centro");
        align(editor, commands, "right", "À direita");
        align(editor, commands, "top", "Em cima");
        align(editor, commands, "middle", "No meio");
        align(editor, commands, "bottom", "Embaixo");
    }

    private static void align(PdfEditor editor, PdfCommands commands, String mode, String title) {
        commands.add("pdf.align." + mode, title, () -> editor.alignSelection(mode)).icon("align-" + mode).edits()
                .when(() -> !editor.getSelection().isEmpty());
    }

    private static boolean singleAnnotation(PdfEditor editor) {
        List<PdfPageElement> elements = editor.getSelection().elements();
        return elements.size() == 1 && elements.getFirst().annotation();
    }

    private static void view(PdfEditor editor, PdfCommands commands) {
        commands.add("pdf.undo", "Desfazer", editor::undo).icon("undo").tip("Desfazer (Ctrl+Z)").when(editor::canUndo);
        commands.add("pdf.redo", "Refazer", editor::redo).icon("redo").tip("Refazer (Ctrl+Y)").when(editor::canRedo);
        commands.add("pdf.zoomIn", "Aumentar zoom", () -> editor.setZoom(editor.getZoom() * 1.25)).icon("zoom-in").tip("Aumentar zoom (Ctrl +)");
        commands.add("pdf.zoomOut", "Diminuir zoom", () -> editor.setZoom(editor.getZoom() / 1.25)).icon("zoom-out").tip("Diminuir zoom (Ctrl -)");
        commands.add("pdf.zoom100", "Tamanho real", () -> editor.setZoom(1)).icon("view-single").tip("Zoom 100% (Ctrl 0)");
        commands.add("pdf.fitWidth", "Ajustar à largura", editor::fitWidth).icon("fit-width");
        commands.add("pdf.fitPage", "Página inteira", editor::fitPage).icon("fit-page");
        commands.add("pdf.view.single", "Página única", () -> editor.setViewMode(PdfViewMode.SINGLE_PAGE)).icon("view-single")
                .selected(() -> editor.getViewMode() == PdfViewMode.SINGLE_PAGE);
        commands.add("pdf.view.continuous", "Contínuo", () -> editor.setViewMode(PdfViewMode.CONTINUOUS)).icon("view-continuous")
                .selected(() -> editor.getViewMode() == PdfViewMode.CONTINUOUS);
        commands.add("pdf.viewMode", "Páginas", () -> editor.setViewMode(editor.getViewMode() == PdfViewMode.SINGLE_PAGE
                ? PdfViewMode.CONTINUOUS : PdfViewMode.SINGLE_PAGE)).icon("view-continuous");
        commands.add("pdf.toggleSidebar", "Painel lateral", () -> editor.setSidebarVisible(!editor.getConfig().thumbnailsVisible()))
                .icon("sidebar").selected(() -> editor.getConfig().thumbnailsVisible());
        commands.add("pdf.toggleStatus", "Barra de status", () -> editor.setStatusVisible(!editor.getConfig().statusVisible()))
                .icon("status").selected(() -> editor.getConfig().statusVisible());
        commands.add("pdf.text", "Texto da página", () -> {
            if (!editor.getConfig().thumbnailsVisible()) editor.setSidebarVisible(true);
            editor.getSidebar().show("text");
        }).icon("text-panel");
        commands.add("pdf.find", "Buscar", () -> {
            if (!editor.getConfig().thumbnailsVisible()) editor.setSidebarVisible(true);
            editor.getSidebar().show("text");
        }).icon("find").tip("Buscar no documento (Ctrl+F)");
    }

    private static void pages(PdfEditor editor, PdfCommands commands) {
        commands.add("pdf.previous", "Anterior", () -> editor.setCurrentPage(editor.getCurrentPage() - 1)).icon("previous")
                .when(() -> editor.getCurrentPage() > 0);
        commands.add("pdf.next", "Próxima", () -> editor.setCurrentPage(editor.getCurrentPage() + 1)).icon("next")
                .when(() -> editor.getCurrentPage() + 1 < editor.getPageCount());
        commands.add("pdf.goTo", "Ir para…", () -> editor.getDialogs().input(editor, "Ir para página",
                "Página (1 a " + editor.getPageCount() + "):", String.valueOf(editor.getCurrentPage() + 1)).ifPresent(value -> {
                    try { editor.setCurrentPage(Integer.parseInt(value.strip()) - 1); }
                    catch (NumberFormatException ignored) { editor.reportError(new IllegalArgumentException("Número de página inválido")); }
                })).icon("pages");
        commands.add("pdf.rotate", "Girar página", () -> editor.perform(() -> editor.rotatePage(editor.getCurrentPage(), 90)))
                .icon("rotate-right").tip("Girar a página atual no sentido horário").edits();
        commands.add("pdf.rotateLeft", "Girar à esquerda", () -> editor.perform(() -> editor.rotatePage(editor.getCurrentPage(), -90)))
                .icon("rotate-left").edits();
        commands.add("pdf.moveUp", "Mover antes", () -> editor.perform(() -> editor.movePage(editor.getCurrentPage(), editor.getCurrentPage() - 1)))
                .icon("page-up").edits().when(() -> editor.getCurrentPage() > 0);
        commands.add("pdf.moveDown", "Mover depois", () -> editor.perform(() -> editor.movePage(editor.getCurrentPage(), editor.getCurrentPage() + 1)))
                .icon("page-down").edits().when(() -> editor.getCurrentPage() + 1 < editor.getPageCount());
        commands.add("pdf.delete", "Excluir página", () -> {
            if (editor.getDialogs().confirm(editor, "Excluir página", "Excluir a página " + (editor.getCurrentPage() + 1) + "?"))
                editor.perform(() -> editor.removePage(editor.getCurrentPage()));
        }).icon("page-delete").edits().when(() -> editor.getPageCount() > 1);
        commands.add("pdf.blankPage", "Página em branco", () -> editor.perform(() -> editor.insertBlankPage(editor.getCurrentPage() + 1)))
                .icon("page-blank").tip("Inserir uma página em branco depois da atual").edits();
        commands.add("pdf.insertPages", "Inserir de arquivo", () -> editor.getFiles().chooseOpen(editor).ifPresent(path ->
                editor.perform(() -> editor.insertPages(path, editor.getCurrentPage() + 1))))
                .icon("page-insert").tip("Inserir páginas de outro PDF depois da atual").edits();
    }

    private static void review(PdfEditor editor, PdfCommands commands) {
        commands.add("pdf.fill", "Preencher", () -> promptFillField(editor)).icon("fill-form").edits()
                .when(editor::hasFormFields);
        commands.add("pdf.replace", "Substituir", () -> promptReplaceText(editor)).icon("replace").edits();
        commands.add("pdf.digitalSign", "Assinar digital", () -> promptDigitalSignature(editor)).icon("sign")
                .tip("Assinar com certificado PKCS#12 em uma nova cópia");
        commands.add("pdf.validate", "Validar", () -> validate(editor)).icon("validate").tip("Validar assinaturas digitais");
        commands.add("pdf.ocr", "OCR", () -> editor.recognizePage(editor.getCurrentPage(), "por+eng", true).completion()
                .whenComplete((result, error) -> SwingUtilities.invokeLater(() -> {
                    if (error != null) editor.reportError(error);
                    else editor.getDialogs().message(editor, "OCR", "Reconhecidas " + result.words().size() + " palavras.");
                }))).icon("ocr").edits().when(() -> editor.active(PdfOcrProvider.class).isPresent());
    }

    private static void extractPages(PdfEditor editor) {
        editor.getDialogs().input(editor, "Extrair páginas",
                        "Páginas do novo PDF (ex.: 1, 3-5) de 1 a " + editor.getPageCount() + ":", describe(editor.getSelectedPages()))
                .ifPresent(text -> {
                    List<Integer> pages;
                    try { pages = parsePages(text, editor.getPageCount()); }
                    catch (IllegalArgumentException error) { editor.reportError(error); return; }
                    editor.getFiles().chooseSave(editor).ifPresent(path -> editor.perform(() -> editor.extractPages(pages, path)));
                });
    }

    public static List<Integer> parsePages(String text, int count) {
        List<Integer> pages = new java.util.ArrayList<>();
        for (String part : text.split("[,;]")) {
            String item = part.strip();
            if (item.isEmpty()) continue;
            String[] bounds = item.split("-", 2);
            try {
                int first = Integer.parseInt(bounds[0].strip()), last = bounds.length == 2 ? Integer.parseInt(bounds[1].strip()) : first;
                if (first < 1 || last < 1 || first > count || last > count) throw new IllegalArgumentException("Página fora do documento: " + item);
                int step = first <= last ? 1 : -1;
                for (int page = first; page != last + step; page += step) if (!pages.contains(page - 1)) pages.add(page - 1);
            } catch (NumberFormatException error) { throw new IllegalArgumentException("Intervalo inválido: " + item, error); }
        }
        if (pages.isEmpty()) throw new IllegalArgumentException("Informe ao menos uma página");
        return List.copyOf(pages);
    }

    static String describe(List<Integer> pages) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < pages.size(); index++) {
            int start = pages.get(index), end = start;
            while (index + 1 < pages.size() && pages.get(index + 1) == end + 1) end = pages.get(++index);
            if (!text.isEmpty()) text.append(", ");
            text.append(start + 1);
            if (end > start) text.append('-').append(end + 1);
        }
        return text.toString();
    }

    private static void promptFillField(PdfEditor editor) {
        List<String> names = editor.getFormFields();
        if (names.isEmpty()) { editor.getDialogs().message(editor, "Formulário", "Este PDF não possui campos."); return; }
        editor.getDialogs().input(editor, "Preencher campo", "Nome do campo: " + String.join(", ", names)).ifPresent(name ->
                editor.getDialogs().input(editor, "Preencher campo", "Valor:").ifPresent(value ->
                        editor.perform(() -> editor.setFormField(name, value))));
    }

    private static void promptReplaceText(PdfEditor editor) {
        editor.getDialogs().input(editor, "Substituir texto", "Texto original:").ifPresent(original ->
                editor.getDialogs().input(editor, "Substituir texto", "Novo texto:").ifPresent(replacement -> {
                    int page = editor.getCurrentPage();
                    Optional<PdfPageElement> word = editor.getPageElements(page).stream()
                            .filter(element -> element.textual() && element.direct() && element.text().equals(original)).findFirst();
                    if (word.isPresent()) {
                        editor.perform(() -> editor.edit("Substituir texto", d -> d.replaceElementText(page, word.get().id(), replacement)));
                        return;
                    }
                    Rectangle2D.Float area = editor.getSelection().isEmpty() ? null : editor.getSelection().bounds();
                    if (editor.getConfig().pageReconstructionEnabled() && !editor.getDialogs().confirm(editor, "Substituir texto",
                            "Se a edição interna não for possível, a página será reconstruída como imagem. Continuar?")) return;
                    editor.perform(() -> {
                        if (!editor.replaceText(page, original, replacement, area))
                            editor.getDialogs().message(editor, "Substituir texto", "Texto substituído.");
                    });
                }));
    }

    private static void promptDigitalSignature(PdfEditor editor) {
        if (editor.isDirty() || editor.getCurrentFile().isEmpty()) {
            editor.getDialogs().message(editor, "Assinar PDF", "Salve o PDF antes de assinar.");
            return;
        }
        editor.getFiles().chooseCertificate(editor).ifPresent(certificate -> editor.getFiles().chooseSave(editor).ifPresent(destination ->
                editor.getDialogs().password(editor, "Assinar PDF", "Senha do certificado PKCS#12:").ifPresent(password -> {
                    try {
                        editor.sign(destination, certificate, password, "Assinado no SwingTools").completion()
                                .whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
                                    Arrays.fill(password, '\0');
                                    if (error != null) editor.reportError(error);
                                    else editor.getDialogs().message(editor, "Assinatura", "PDF assinado em " + destination);
                                }));
                    } catch (RuntimeException error) {
                        Arrays.fill(password, '\0');
                        editor.reportError(error);
                    }
                })));
    }

    private static void validate(PdfEditor editor) {
        editor.validateSignatures().completion().whenComplete((values, error) -> SwingUtilities.invokeLater(() -> {
            if (error != null) editor.reportError(error);
            else editor.getDialogs().message(editor, "Assinaturas", values.isEmpty() ? "Sem assinaturas digitais."
                    : values.stream().map(value -> value.signer() + ": " + value.status() + " — " + value.reason())
                    .collect(Collectors.joining("\n")));
        }));
    }

}
