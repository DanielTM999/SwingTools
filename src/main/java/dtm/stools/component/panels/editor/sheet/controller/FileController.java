package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetTask;
import dtm.stools.component.panels.editor.sheet.io.CsvCodec;
import dtm.stools.component.panels.editor.sheet.io.CsvDialect;
import dtm.stools.component.panels.editor.sheet.io.SheetHtmlExporter;
import dtm.stools.component.panels.editor.sheet.io.SheetImportResult;
import dtm.stools.component.panels.editor.sheet.io.SheetTextExporter;
import dtm.stools.component.panels.editor.sheet.io.pdf.SheetPdfExportProvider;
import dtm.stools.component.panels.editor.sheet.command.SheetOperations;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.PageOrientation;
import dtm.stools.component.panels.editor.sheet.model.PaperSize;
import dtm.stools.component.panels.editor.sheet.model.PrintSettings;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetViewMode;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.provider.SheetExportOptions;
import dtm.stools.component.panels.editor.sheet.provider.SheetExportProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetFileDialogRequest;
import dtm.stools.component.panels.editor.sheet.provider.SheetImportProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetRecoveryStore;
import dtm.stools.component.panels.editor.sheet.print.SheetPageLayout;
import dtm.stools.component.panels.editor.sheet.print.SheetPageRenderer;
import dtm.stools.component.panels.editor.sheet.print.SheetPagination;
import dtm.stools.component.panels.editor.sheet.print.SheetPrintable;
import dtm.stools.component.panels.editor.sheet.ui.popup.PageSetupPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.SheetForm;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class FileController {
    private static final List<SheetFileDialogRequest.Filter> OPEN_FILTERS = List.of(
            new SheetFileDialogRequest.Filter("Todas as planilhas", List.of("xlsx", "xlsm", "ods", "csv", "tsv", "txt")),
            new SheetFileDialogRequest.Filter("Pasta de Trabalho do Excel", List.of("xlsx", "xlsm")),
            new SheetFileDialogRequest.Filter("Planilha OpenDocument", List.of("ods")),
            new SheetFileDialogRequest.Filter("Texto (CSV/TSV)", List.of("csv", "tsv", "txt")));

    private final SheetEditor editor;
    private final ExecutorService io = Executors.newVirtualThreadPerTaskExecutor();
    private final String recoveryKey = "sheet-" + UUID.randomUUID();
    private Path file;
    private String format;
    private FileTime modified;
    private byte[] original;

    public FileController(SheetEditor editor) { this.editor = editor; }

    public Path currentFile() { return file; }
    public String currentFormat() { return format; }

    public void setCurrentFile(Path path, String fmt) {
        Path old = file;
        file = path;
        format = fmt;
        original = null;
        modified = null;
        editor.firePropertyChangeHook("file", old, path);
    }

    private static String extension(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot < 0 ? "" : n.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String baseName(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot <= 0 ? n : n.substring(0, dot);
    }

    public void openDialog() {
        if (!confirmDiscard()) return;
        editor.popups().chooseFile(SheetFileDialogRequest.Mode.OPEN, "Abrir", OPEN_FILTERS, file == null ? null : file.getParent(), null).ifPresent(p -> open(p, true));
    }

    public void newWorkbook() {
        if (!confirmDiscard()) return;
        editor.newWorkbook();
    }

    public boolean confirmDiscard() {
        if (!editor.getSession().isDirty() || !editor.isShowing()) return true;
        int choice = editor.popups().choose("Planilha", "Deseja salvar as alterações feitas em " + (file == null ? "Pasta1" : file.getFileName()) + "?", false, 0, "Salvar", "Não Salvar", "Cancelar");
        if (choice == 0) return saveNow();
        return choice == 1;
    }

    public boolean saveNow() {
        editor.commitEditingIfActive();
        Path target = file;
        if (target == null || !isNative(extension(target))) {
            List<SheetFileDialogRequest.Filter> filters = List.of(new SheetFileDialogRequest.Filter("Pasta de Trabalho do Excel (*.xlsx)", List.of("xlsx")));
            Optional<Path> p = editor.popups().chooseFile(SheetFileDialogRequest.Mode.SAVE, "Salvar Como", filters, null, "Pasta1.xlsx");
            if (p.isEmpty()) return false;
            target = p.get();
        }
        try {
            persistViewState();
            writeAtomic(target, encode(extension(target)));
            Path old = file;
            file = target;
            format = extension(target);
            modified = Files.getLastModifiedTime(target);
            editor.getSession().markSaved();
            clearRecovery();
            editor.firePropertyChangeHook("file", old, target);
            return true;
        } catch (IOException | RuntimeException failure) {
            editor.reportError(failure);
            return false;
        }
    }

    public SheetTask<Path> open(Path path, boolean discard) {
        if (editor.getSession().isDirty() && !discard) return SheetTask.failed(new IllegalStateException("Existem alterações não salvas. Use open(path, true) para descartá-las."));
        editor.commitEditingIfActive();
        SheetTask<Path> task = new SheetTask<>();
        long max = editor.getConfig().limits().maxFileBytes();
        editor.getStatusBar().update("Abrindo…", path.getFileName().toString(), "", editor.effectiveZoom(), editor.activeSheet().properties().viewMode());
        Future<?> f = io.submit(() -> {
            try {
                long size = Files.size(path);
                if (size > max) throw new IOException("O arquivo excede o tamanho máximo permitido (" + (max / (1024 * 1024)) + " MB).");
                byte[] bytes = Files.readAllBytes(path);
                task.progress(40);
                SheetImportResult result = importBytes(path, bytes);
                FileTime time = Files.getLastModifiedTime(path);
                task.progress(90);
                SwingUtilities.invokeLater(() -> {
                    if (task.isCancelled() || editor.isClosed()) return;
                    try {
                        apply(result, path, time);
                        task.complete(path);
                    } catch (RuntimeException failure) {
                        task.fail(failure);
                        editor.reportError(failure);
                    }
                });
            } catch (Throwable failure) {
                task.fail(failure);
                SwingUtilities.invokeLater(() -> { editor.refreshStatus(); editor.reportError(failure); });
            }
        });
        task.attach(f);
        return task;
    }

    public SheetImportResult importBytes(Path path, byte[] bytes) throws IOException {
        String ext = extension(path);
        for (SheetImportProvider p : editor.providers(SheetImportProvider.class))
            if (p.extensions().stream().anyMatch(e -> e.equalsIgnoreCase(ext))) return p.read(new ByteArrayInputStream(bytes));
        boolean zip = bytes.length > 3 && bytes[0] == 'P' && bytes[1] == 'K';
        return switch (ext) {
            case "xlsx", "xlsm", "xltx", "xltm" -> editor.getServices().xlsx().read(bytes);
            case "ods", "ots" -> editor.getServices().ods().read(bytes);
            case "csv", "tsv", "txt", "tab" -> readCsv(path, bytes, null);
            default -> zip ? (new String(bytes, 0, Math.min(bytes.length, 200), StandardCharsets.ISO_8859_1).contains("mimetypeapplication/vnd.oasis") ? editor.getServices().ods().read(bytes) : editor.getServices().xlsx().read(bytes)) : readCsv(path, bytes, null);
        };
    }

    private SheetImportResult readCsv(Path path, byte[] bytes, CsvDialect dialect) {
        CsvCodec csv = editor.getServices().csv();
        CsvDialect d = dialect != null ? dialect : csv.detect(bytes, editor.getConfig().locale());
        if (dialect == null && extension(path).equals("tsv")) d = d.withDelimiter('\t');
        String name = baseName(path);
        try { SheetWorkbook.validateSheetName(name); } catch (IllegalArgumentException e) { name = "Planilha1"; }
        return csv.read(bytes, d, name.length() > 31 ? name.substring(0, 31) : name, false);
    }

    private void apply(SheetImportResult result, Path path, FileTime time) {
        editor.load(result.workbook());
        editor.getSession().setReadOnly(!result.editable());
        List<String> diagnostics = new ArrayList<>(result.workbook().properties().diagnostics());
        for (String d : result.diagnostics()) if (!diagnostics.contains(d)) diagnostics.add(d);
        editor.showDiagnostics(diagnostics);
        Path old = file;
        file = path;
        format = result.format() == null ? extension(path) : result.format();
        modified = time;
        original = result.original();
        editor.firePropertyChangeHook("file", old, path);
        editor.refreshAll();
    }

    public SheetTask<Path> save() {
        if (file == null || !isNative(extension(file))) return saveAs();
        return save(file);
    }

    private static boolean isNative(String ext) { return ext.equals("xlsx") || ext.equals("xlsm") || ext.equals("ods") || ext.equals("csv") || ext.equals("tsv"); }

    public SheetTask<Path> saveAs() {
        List<SheetFileDialogRequest.Filter> filters = List.of(
                new SheetFileDialogRequest.Filter("Pasta de Trabalho do Excel (*.xlsx)", List.of("xlsx")),
                new SheetFileDialogRequest.Filter("Planilha OpenDocument (*.ods)", List.of("ods")),
                new SheetFileDialogRequest.Filter("CSV (separado por vírgulas) (*.csv)", List.of("csv")),
                new SheetFileDialogRequest.Filter("Texto (separado por tabulações) (*.tsv)", List.of("tsv")));
        Optional<Path> p = editor.popups().chooseFile(SheetFileDialogRequest.Mode.SAVE, "Salvar Como", filters, file == null ? null : file.getParent(), file == null ? "Pasta1.xlsx" : file.getFileName().toString());
        return p.map(this::save).orElseGet(() -> SheetTask.completed(null));
    }

    public SheetTask<Path> save(Path path) {
        editor.commitEditingIfActive();
        String ext = extension(path);
        if (path.equals(file) && modified != null) {
            try {
                if (Files.exists(path) && Files.getLastModifiedTime(path).compareTo(modified) > 0 && editor.isShowing()
                        && !editor.popups().confirm("Salvar", "O arquivo foi alterado por outro programa desde que foi aberto. Deseja substituí-lo?")) return SheetTask.completed(null);
            } catch (IOException ignored) { }
        }
        if ((ext.equals("csv") || ext.equals("tsv")) && editor.getWorkbook().sheetCount() > 1 && editor.isShowing()
                && !editor.popups().confirm("Salvar", "O formato selecionado não oferece suporte a pastas de trabalho com várias planilhas. Apenas a planilha ativa será salva. Continuar?")) return SheetTask.completed(null);
        byte[] bytes;
        try {
            persistViewState();
            boolean unchanged = !editor.getSession().isDirty() && original != null && path.equals(file);
            bytes = unchanged ? original : encode(ext);
        } catch (IOException | RuntimeException failure) {
            editor.reportError(failure);
            return SheetTask.failed(failure);
        }
        SheetTask<Path> task = new SheetTask<>();
        long revision = editor.getSession().getRevision();
        Future<?> f = io.submit(() -> {
            try {
                writeAtomic(path, bytes);
                FileTime time = Files.getLastModifiedTime(path);
                SwingUtilities.invokeLater(() -> {
                    Path old = file;
                    file = path;
                    format = ext;
                    modified = time;
                    if (editor.getSession().getRevision() == revision) editor.getSession().markSaved();
                    clearRecovery();
                    editor.firePropertyChangeHook("file", old, path);
                    editor.refreshAll();
                    task.complete(path);
                });
            } catch (Throwable failure) {
                task.fail(failure);
                SwingUtilities.invokeLater(() -> editor.reportError(failure));
            }
        });
        task.attach(f);
        return task;
    }

    private void persistViewState() {
        for (var e : editor.sheetZooms().entrySet()) {
            SheetWorksheet ws = editor.getWorkbook().sheetById(e.getKey());
            if (ws != null && ws.properties().zoom() != e.getValue()) ws.setProperties(ws.properties().withZoom(e.getValue()));
        }
        editor.getWorkbook().setProperties(editor.getWorkbook().properties().withActiveSheet(editor.activeSheetIndex()));
    }

    public byte[] encode(String ext) throws IOException {
        SheetWorkbook wb = editor.getWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        switch (ext) {
            case "ods" -> editor.getServices().ods().write(wb, editor.getEngine(), out);
            case "csv" -> editor.getServices().csv().write(wb, editor.activeSheetIndex(), editor.getEngine(), csvDialect(), out);
            case "tsv", "txt" -> editor.getServices().csv().write(wb, editor.activeSheetIndex(), editor.getEngine(), CsvDialect.TAB, out);
            case "html", "htm" -> new SheetHtmlExporter().write(wb, editor.getEngine(), editor.formatter(), out);
            case "pdf" -> pdfProvider().export(wb, editor.getEngine(), SheetExportOptions.activeSheet(editor.activeSheetIndex(), editor.getConfig().locale()), out);
            default -> {
                for (SheetExportProvider p : editor.providers(SheetExportProvider.class)) if (p.extension().equalsIgnoreCase(ext)) {
                    p.export(wb, editor.getEngine(), SheetExportOptions.activeSheet(editor.activeSheetIndex(), editor.getConfig().locale()), out);
                    return out.toByteArray();
                }
                editor.getServices().xlsx().write(wb, editor.getEngine(), out);
            }
        }
        return out.toByteArray();
    }

    private CsvDialect csvDialect() {
        return editor.getConfig().locale().getLanguage().equals("pt") || editor.formatter().decimalSeparator() == ',' ? CsvDialect.SEMICOLON.withLocale(editor.getConfig().locale()) : CsvDialect.COMMA.withLocale(editor.getConfig().locale());
    }

    public SheetPdfExportProvider pdfProvider() { return new SheetPdfExportProvider(editor.renderer(), editor.objects()::chartData); }

    private static void writeAtomic(Path path, byte[] bytes) throws IOException {
        Path dir = path.toAbsolutePath().getParent();
        if (dir != null) Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, ".~" + path.getFileName(), ".tmp");
        try {
            Files.write(temp, bytes);
            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public SheetTask<Path> export(Path path, SheetEditor.ExportFormat fmt) {
        editor.commitEditingIfActive();
        String ext = switch (fmt) { case XLSX -> "xlsx"; case ODS -> "ods"; case CSV -> "csv"; case TSV -> "tsv"; case HTML -> "html"; case PDF -> "pdf"; case TXT -> "txt"; };
        byte[] bytes;
        try {
            if (fmt == SheetEditor.ExportFormat.TXT) {
                CellRange used = editor.getEngine().usedRange(editor.activeSheetIndex());
                bytes = (used == null ? "" : new SheetTextExporter().text(editor.getWorkbook(), editor.getEngine(), editor.formatter(), editor.activeSheetIndex(), new CellRange(0, 0, used.lastRow(), used.lastColumn()))).getBytes(StandardCharsets.UTF_8);
            } else bytes = encode(ext);
        } catch (IOException | RuntimeException failure) {
            return SheetTask.failed(failure);
        }
        SheetTask<Path> task = new SheetTask<>();
        task.attach(io.submit(() -> {
            try {
                writeAtomic(path, bytes);
                task.complete(path);
            } catch (Throwable failure) {
                task.fail(failure);
            }
        }));
        return task;
    }

    public void exportDialog(SheetEditor.ExportFormat fmt) {
        String ext = fmt.name().toLowerCase(Locale.ROOT);
        String description = switch (fmt) { case PDF -> "PDF"; case HTML -> "Página da Web"; case CSV -> "CSV"; case TSV -> "Texto separado por tabulação"; case ODS -> "Planilha OpenDocument"; case TXT -> "Texto"; case XLSX -> "Pasta de Trabalho do Excel"; };
        String base = file == null ? "Pasta1" : baseName(file);
        editor.popups().chooseFile(SheetFileDialogRequest.Mode.EXPORT, "Exportar", List.of(new SheetFileDialogRequest.Filter(description + " (*." + ext + ")", List.of(ext))), file == null ? null : file.getParent(), base + "." + ext)
                .ifPresent(p -> export(p, fmt).completion().whenComplete((ok, error) -> SwingUtilities.invokeLater(() -> {
                    if (error != null) editor.reportError(error);
                    else editor.popups().info("Exportar", "Arquivo exportado: " + p.getFileName());
                })));
    }

    public void importCsvDialog() {
        editor.popups().chooseFile(SheetFileDialogRequest.Mode.IMPORT, "Importar Texto", List.of(new SheetFileDialogRequest.Filter("Arquivos de texto", List.of("csv", "tsv", "txt"))), null, null).ifPresent(path -> {
            byte[] bytes;
            try { bytes = Files.readAllBytes(path); } catch (IOException failure) { editor.reportError(failure); return; }
            CsvCodec csv = editor.getServices().csv();
            CsvDialect detected = csv.detect(bytes, editor.getConfig().locale());
            SheetForm f = new SheetForm();
            JComboBox<String> delimiter = SheetForm.combo("Vírgula", "Ponto e vírgula", "Tabulação", "Espaço", "Barra vertical");
            char[] delims = {',', ';', '\t', ' ', '|'};
            for (int i = 0; i < delims.length; i++) if (delims[i] == detected.delimiter()) delimiter.setSelectedIndex(i);
            JComboBox<String> charset = SheetForm.combo("UTF-8", "Windows-1252", "ISO-8859-1", "UTF-16");
            String cs = detected.charset().name();
            charset.setSelectedItem(cs.equalsIgnoreCase("windows-1252") ? "Windows-1252" : cs.equalsIgnoreCase("ISO-8859-1") ? "ISO-8859-1" : cs.startsWith("UTF-16") ? "UTF-16" : "UTF-8");
            JComboBox<String> locale = SheetForm.combo("Português (Brasil)", "Inglês (Estados Unidos)");
            locale.setSelectedIndex(detected.locale().getLanguage().equals("en") ? 1 : 0);
            JComboBox<String> parse = SheetForm.combo("Detectar tipos de dados", "Importar tudo como texto");
            JTextArea preview = new JTextArea(8, 60);
            preview.setEditable(false);
            preview.setText(CsvCodec.decode(bytes, detected.charset()).lines().limit(12).reduce("", (a, b) -> a + b + "\n"));
            JScrollPane scroll = new JScrollPane(preview);
            scroll.setPreferredSize(new Dimension(560, 160));
            f.add("Delimitador:", delimiter);
            f.add("Origem do arquivo:", charset);
            f.add("Localidade dos números:", locale);
            f.add("Tipos:", parse);
            f.grow(scroll);
            editor.popups().dialog("sheet.importCsv", "Importar " + path.getFileName(), f, () -> new CsvDialect(delims[delimiter.getSelectedIndex()], '"', Charset.forName(charset.getSelectedItem().toString()),
                    locale.getSelectedIndex() == 1 ? Locale.US : Locale.forLanguageTag("pt-BR"), parse.getSelectedIndex() == 0, false)).ifPresent(d -> {
                SheetImportResult r = readCsv(path, bytes, d);
                SheetWorksheet src = r.workbook().sheet(0);
                SheetWorkbook imported = r.workbook();
                int s = editor.activeSheetIndex();
                String name = editor.getWorkbook().uniqueSheetName(src.name());
                editor.edit("Importar texto", tx -> {
                    int idx = SheetOperations.addSheet(tx, s + 1, name);
                    src.cells().forEach((row, col, cell) -> {
                        int style = cell.style() == 0 ? 0 : editor.getWorkbook().styles().intern(imported.style(cell.style()));
                        tx.setCell(idx, row, col, new SheetCell(cell.value(), cell.formula(), style));
                    });
                    tx.select(idx, dtm.stools.component.panels.editor.sheet.api.SheetSelection.home());
                });
            });
        });
    }

    public List<SheetPageLayout> layouts(boolean allSheets) {
        List<SheetPageLayout> list = new ArrayList<>();
        if (allSheets) {
            for (int s = 0; s < editor.getWorkbook().sheetCount(); s++)
                if (editor.getWorkbook().sheet(s).properties().visibility() == dtm.stools.component.panels.editor.sheet.model.SheetVisibility.VISIBLE) list.add(SheetPagination.layout(editor.getWorkbook(), editor.getEngine(), s));
        } else list.add(SheetPagination.layout(editor.getWorkbook(), editor.getEngine(), editor.activeSheetIndex()));
        return list;
    }

    public void print() {
        editor.commitEditingIfActive();
        SheetPageRenderer renderer = new SheetPageRenderer(editor.getWorkbook(), editor.getEngine(), editor.formatter(), editor.renderer(), editor.objects()::chartData, file == null ? "Pasta1" : file.getFileName().toString());
        SheetPrintable printable = new SheetPrintable(renderer, layouts(false));
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(file == null ? "Pasta1" : file.getFileName().toString());
        job.setPageable(printable);
        if (!job.printDialog()) return;
        try {
            job.print();
        } catch (PrinterException failure) {
            editor.reportError(failure);
        }
    }

    public void updatePageBreaks() {
        SheetWorksheet ws = editor.activeSheet();
        if (ws.properties().viewMode() == SheetViewMode.NORMAL) { editor.getCanvas().setPageBreaks(List.of(), List.of()); return; }
        SheetPageLayout layout = SheetPagination.layout(editor.getWorkbook(), editor.getEngine(), editor.activeSheetIndex());
        editor.getCanvas().setPageBreaks(layout.rowBreaks(), layout.columnBreaks());
    }

    public void updatePrint(String label, java.util.function.UnaryOperator<PrintSettings> change) {
        int s = editor.activeSheetIndex();
        editor.edit(label, tx -> tx.updateProperties(s, p -> p.withPrint(change.apply(p.print()))));
        updatePageBreaks();
    }

    public void pageSetup() {
        PrintSettings ps = editor.activeSheet().properties().print();
        PageSetupPanel panel = new PageSetupPanel(ps, text -> editor.navigation().resolve(text).map(t -> t.ranges().getFirst()).orElseThrow(() -> new IllegalArgumentException("Referência inválida: " + text)));
        editor.popups().dialog(SheetDialogIds.PAGE_SETUP, "Configurar Página", panel, panel::result).ifPresent(next -> updatePrint("Configurar página", p -> next));
    }

    public void margins(String preset) {
        updatePrint("Margens", p -> switch (preset) {
            case "wide" -> p.withMarginLeft(1).withMarginRight(1).withMarginTop(1).withMarginBottom(1).withMarginHeader(0.5).withMarginFooter(0.5);
            case "narrow" -> p.withMarginLeft(0.25).withMarginRight(0.25).withMarginTop(0.75).withMarginBottom(0.75).withMarginHeader(0.3).withMarginFooter(0.3);
            default -> p.withMarginLeft(0.7).withMarginRight(0.7).withMarginTop(0.75).withMarginBottom(0.75).withMarginHeader(0.3).withMarginFooter(0.3);
        });
    }

    public void orientation(PageOrientation o) { updatePrint("Orientação", p -> p.withOrientation(o)); }
    public void paper(PaperSize size) { updatePrint("Tamanho", p -> p.withPaper(size)); }
    public void setPrintArea() { CellRange r = editor.getSelection().range(); updatePrint("Área de impressão", p -> p.withPrintArea(r)); }
    public void clearPrintArea() { updatePrint("Limpar área de impressão", p -> p.withPrintArea(null)); }

    public void insertBreak() {
        var a = editor.getSelection().active();
        updatePrint("Inserir quebra de página", p -> {
            List<Integer> rows = new ArrayList<>(p.rowBreaks()), cols = new ArrayList<>(p.columnBreaks());
            if (a.row() > 0 && !rows.contains(a.row())) rows.add(a.row());
            if (a.column() > 0 && !cols.contains(a.column())) cols.add(a.column());
            rows.sort(null);
            cols.sort(null);
            return p.withRowBreaks(rows).withColumnBreaks(cols);
        });
    }

    public void removeBreak() {
        var a = editor.getSelection().active();
        updatePrint("Remover quebra de página", p -> p.withRowBreaks(p.rowBreaks().stream().filter(r -> r != a.row()).toList()).withColumnBreaks(p.columnBreaks().stream().filter(c -> c != a.column()).toList()));
    }

    public void resetBreaks() { updatePrint("Redefinir quebras", p -> p.withRowBreaks(List.of()).withColumnBreaks(List.of())); }

    public void printTitles() {
        PrintSettings ps = editor.activeSheet().properties().print();
        JTextField rows = SheetForm.text(ps.repeatRowFirst() == null ? "" : "$" + (ps.repeatRowFirst() + 1) + ":$" + ((ps.repeatRowLast() == null ? ps.repeatRowFirst() : ps.repeatRowLast()) + 1), 10);
        SheetForm f = new SheetForm();
        f.add("Linhas a repetir na parte superior:", rows);
        editor.popups().dialog("sheet.printTitles", "Imprimir Títulos", f, () -> rows.getText().isBlank() ? null : editor.navigation().resolve(rows.getText()).map(t -> t.ranges().getFirst()).orElseThrow(() -> new IllegalArgumentException("Referência inválida.")))
                .ifPresentOrElse(r -> updatePrint("Imprimir títulos", p -> p.withRepeatRowFirst(r.firstRow()).withRepeatRowLast(r.lastRow())), () -> { });
    }

    public void autoRecover() {
        if (!editor.getSession().isDirty() || editor.isClosed()) return;
        List<SheetRecoveryStore> stores = editor.providers(SheetRecoveryStore.class);
        if (stores.isEmpty()) return;
        byte[] bytes;
        try {
            bytes = editor.getServices().xlsx().toBytes(editor.getWorkbook(), editor.getEngine());
        } catch (IOException | RuntimeException failure) {
            return;
        }
        String key = recoveryKey();
        io.submit(() -> {
            for (SheetRecoveryStore s : stores) {
                try { s.save(key, bytes); } catch (IOException | RuntimeException failure) { SwingUtilities.invokeLater(() -> editor.reportError(failure)); }
            }
        });
    }

    private String recoveryKey() { return file == null ? recoveryKey : file.toAbsolutePath().toString(); }

    private void clearRecovery() {
        String key = recoveryKey();
        List<SheetRecoveryStore> stores = editor.providers(SheetRecoveryStore.class);
        if (stores.isEmpty()) return;
        io.submit(() -> { for (SheetRecoveryStore s : stores) { try { s.clear(key); } catch (IOException | RuntimeException ignored) { } } });
    }

    public void recover() {
        List<SheetRecoveryStore> stores = editor.providers(SheetRecoveryStore.class);
        if (stores.isEmpty()) { editor.popups().info("Recuperar", "Nenhum repositório de recuperação está configurado. Registre um SheetRecoveryStore."); return; }
        try {
            List<SheetRecoveryStore.Snapshot> history = new ArrayList<>(stores.getFirst().history(recoveryKey()));
            if (history.isEmpty()) { editor.popups().info("Recuperar", "Nenhuma versão recuperada disponível."); return; }
            JList<String> list = new JList<>(history.stream().map(s -> s.created().toString() + "  (" + s.data().length / 1024 + " KB)").toArray(String[]::new));
            list.setSelectedIndex(0);
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(360, 180));
            editor.popups().dialog("sheet.recover", "Recuperar Versão", scroll, list::getSelectedIndex).filter(i -> i >= 0).ifPresent(i -> {
                try {
                    SheetImportResult r = editor.getServices().xlsx().read(history.get(i).data());
                    editor.load(r.workbook());
                    editor.getSession().markDirty();
                } catch (IOException failure) {
                    editor.reportError(failure);
                }
            });
        } catch (IOException failure) {
            editor.reportError(failure);
        }
    }

    public void properties() {
        var p = editor.getWorkbook().properties();
        JTextField title = SheetForm.text(p.title(), 26), author = SheetForm.text(p.author(), 26);
        SheetForm f = new SheetForm();
        f.add("Título:", title);
        f.add("Autor:", author);
        f.add("Arquivo:", SheetForm.text(file == null ? "(não salvo)" : file.toString(), 26));
        editor.popups().dialog("sheet.properties", "Propriedades", f, () -> new String[]{title.getText(), author.getText()})
                .ifPresent(o -> editor.edit("Propriedades", tx -> tx.updateWorkbook(q -> q.withTitle(o[0]).withAuthor(o[1]))));
    }

    public void writeTo(OutputStream out, String ext) throws IOException { out.write(encode(ext)); }

    public void dispose() { io.shutdownNow(); }
}
