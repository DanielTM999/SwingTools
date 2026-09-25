package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.calc.PivotEngine;
import dtm.stools.component.panels.editor.sheet.command.SheetOperations;
import dtm.stools.component.panels.editor.sheet.data.FilterEngine;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ChartSeries;
import dtm.stools.component.panels.editor.sheet.model.ChartType;
import dtm.stools.component.panels.editor.sheet.model.FilterCriteria;
import dtm.stools.component.panels.editor.sheet.model.LegendPosition;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.ShapeType;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetImage;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetShape;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.Slicer;
import dtm.stools.component.panels.editor.sheet.model.Sparkline;
import dtm.stools.component.panels.editor.sheet.model.SparklineType;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.provider.SheetFileDialogRequest;
import dtm.stools.component.panels.editor.sheet.render.ChartData;
import dtm.stools.component.panels.editor.sheet.ui.popup.ChartEditorPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.PivotFieldListPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.SheetForm;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ObjectController {
    private final SheetEditor editor;
    private final Map<String, ObjectAnchor> previewOriginal = new HashMap<>();

    public ObjectController(SheetEditor editor) { this.editor = editor; }

    private int sheet() { return editor.activeSheetIndex(); }
    private SheetWorksheet ws() { return editor.activeSheet(); }

    public void workbookLoaded() { previewOriginal.clear(); editor.getCanvas().setSelectedObject(null); }

    public void selected(SheetObject object) {
        String id = object == null ? null : object.id();
        if (!java.util.Objects.equals(id, editor.getCanvas().selectedObject())) editor.getCanvas().setSelectedObject(id);
        editor.refreshAll();
    }

    public SheetObject selectedObject() {
        String id = editor.getCanvas().selectedObject();
        if (id == null) return null;
        for (SheetObject o : ws().properties().objects()) if (o.id().equals(id)) return o;
        return null;
    }

    public SheetObject find(String id) {
        for (SheetObject o : ws().properties().objects()) if (o.id().equals(id)) return o;
        return null;
    }

    public void preview(String id, ObjectAnchor anchor) {
        SheetObject o = find(id);
        if (o == null || editor.isReadOnlyView()) return;
        previewOriginal.putIfAbsent(id, o.anchor());
        SheetWorksheet w = ws();
        w.setProperties(w.properties().replaceObject(id, x -> x.withAnchor(anchor)));
        editor.getCanvas().repaint();
    }

    public void commitPreview(String id) {
        ObjectAnchor original = previewOriginal.remove(id);
        SheetObject o = find(id);
        if (original == null || o == null) return;
        ObjectAnchor target = o.anchor();
        SheetWorksheet w = ws();
        w.setProperties(w.properties().replaceObject(id, x -> x.withAnchor(original)));
        if (original.equals(target)) return;
        int s = sheet();
        editor.edit("Mover objeto", tx -> tx.updateProperties(s, p -> p.replaceObject(id, x -> x.withAnchor(target))));
    }

    private ObjectAnchor defaultAnchor(int width, int height) {
        CellRange r = editor.getSelection().range();
        int col = r.isWholeRow() ? 0 : Math.min(ws().columns().count() - 1, r.lastColumn() + 2);
        int row = r.isWholeColumn() ? 0 : r.firstRow();
        return new ObjectAnchor(row, col, 0, 0, width, height);
    }

    public void addObject(SheetObject object, String label) {
        int s = sheet();
        if (ws().properties().protection().enabled() && !ws().properties().protection().objects()) { editor.review().warnProtected(); return; }
        if (editor.edit(label, tx -> tx.updateProperties(s, p -> p.withObjects(SheetProperties.add(p.objects(), object))))) {
            editor.getCanvas().setSelectedObject(object.id());
            editor.refreshAll();
        }
    }

    public void deleteSelected() {
        SheetObject o = selectedObject();
        if (o == null) return;
        int s = sheet();
        editor.edit("Excluir objeto", tx -> tx.updateProperties(s, p -> p.replaceObject(o.id(), x -> null)));
        editor.getCanvas().setSelectedObject(null);
        editor.refreshAll();
    }

    public void order(boolean front) {
        SheetObject o = selectedObject();
        if (o == null) return;
        int s = sheet();
        editor.edit(front ? "Trazer para a frente" : "Enviar para trás", tx -> tx.updateProperties(s, p -> {
            List<SheetObject> list = new ArrayList<>(p.objects());
            list.removeIf(x -> x.id().equals(o.id()));
            if (front) list.add(o); else list.addFirst(o);
            return p.withObjects(list);
        }));
    }

    public void pasteObject(SheetObject object) {
        ObjectAnchor a = object.anchor();
        String id = UUID.randomUUID().toString();
        SheetObject copy = switch (object) {
            case SheetChart c -> c.toBuilder().id(id).anchor(a.moved(a.row() + 1, a.column() + 1, a.offsetX(), a.offsetY())).build();
            case SheetImage i -> new SheetImage(id, i.data(), i.format(), a.moved(a.row() + 1, a.column() + 1, a.offsetX(), a.offsetY()), i.altText());
            case SheetShape sh -> new SheetShape(id, sh.type(), a.moved(a.row() + 1, a.column() + 1, a.offsetX(), a.offsetY()), sh.text(), sh.fill(), sh.line(), sh.lineWidth());
            default -> null;
        };
        if (copy != null) addObject(copy, "Colar objeto");
    }

    public void altText() {
        SheetObject o = selectedObject();
        if (!(o instanceof SheetImage img)) { editor.popups().info("Texto Alternativo", "Selecione uma imagem."); return; }
        int s = sheet();
        editor.popups().prompt("Texto Alternativo", "Descrição:", img.altText()).ifPresent(t -> editor.edit("Texto alternativo", tx -> tx.updateProperties(s, p -> p.replaceObject(img.id(), x -> new SheetImage(img.id(), img.data(), img.format(), img.anchor(), t)))));
    }

    public void insertChart(ChartType type) {
        CellRange r = chartSource();
        if (r == null) { editor.popups().warn("Gráfico", "Selecione um intervalo com dados para criar o gráfico."); return; }
        int s = sheet();
        String sheetName = ws().name();
        boolean header = false;
        for (int c = r.firstColumn(); c <= r.lastColumn(); c++) if (editor.getEngine().valueAt(s, r.firstRow(), c) instanceof TextValue && r.rowCount() > 1) header = true;
        int firstData = r.firstRow() + (header ? 1 : 0);
        boolean categories = r.columnCount() > 1 && !(editor.getEngine().valueAt(s, firstData, r.firstColumn()) instanceof NumberValue n && !editor.formatter().isDateFormat(editor.getWorkbook().style(ws().cell(firstData, r.firstColumn()).style()).numberFormat()) && !header)
                || type == ChartType.SCATTER && r.columnCount() > 1;
        String catRef = categories ? NavigationController.reference(sheetName, new CellRange(firstData, r.firstColumn(), r.lastRow(), r.firstColumn()), true) : null;
        List<ChartSeries> series = new ArrayList<>();
        for (int c = r.firstColumn() + (categories ? 1 : 0); c <= r.lastColumn(); c++) {
            String name = header ? editor.displayText(s, new CellAddress(r.firstRow(), c)) : "Série" + (series.size() + 1);
            String nameRef = header ? NavigationController.reference(sheetName, CellRange.of(r.firstRow(), c), true) : null;
            series.add(new ChartSeries(name, nameRef, catRef, NavigationController.reference(sheetName, new CellRange(firstData, c, r.lastRow(), c), true), null, null, null, false));
        }
        if (series.isEmpty()) return;
        boolean pie = type == ChartType.PIE || type == ChartType.DOUGHNUT;
        SheetChart chart = SheetChart.builder().id(UUID.randomUUID().toString()).type(type).anchor(defaultAnchor(480, 288))
                .title(series.size() == 1 && header ? series.getFirst().name() : "Título do Gráfico").series(pie ? List.of(series.getFirst()) : series)
                .legend(series.size() > 1 || pie ? LegendPosition.BOTTOM : LegendPosition.NONE).xAxisTitle("").yAxisTitle("").dataLabels(false).gridlines(!pie).styleIndex(0).build();
        addObject(chart, "Inserir gráfico");
    }

    private CellRange chartSource() {
        SheetSelection sel = editor.getSelection();
        CellRange r = sel.range();
        if (r.isSingleCell()) r = FilterEngine.detectRegion(ws(), r.firstRow(), r.firstColumn());
        else r = editor.clipboard().bounded(ws(), r);
        if (r == null || r.isSingleCell() && ws().cell(r.first()).value().isEmpty()) return null;
        return r;
    }

    public void recommendedChart() {
        CellRange r = chartSource();
        if (r == null) { insertChart(ChartType.COLUMN); return; }
        int s = sheet();
        CellValue first = editor.getEngine().valueAt(s, r.firstRow() + 1 > r.lastRow() ? r.firstRow() : r.firstRow() + 1, r.firstColumn());
        boolean dates = first instanceof NumberValue && editor.formatter().isDateFormat(editor.getWorkbook().style(ws().cell(r.firstRow() + 1, r.firstColumn()).style()).numberFormat());
        boolean numericX = first instanceof NumberValue && !dates;
        ChartType type = dates ? ChartType.LINE : numericX && r.columnCount() == 2 ? ChartType.SCATTER : r.columnCount() == 2 && r.rowCount() <= 8 ? ChartType.PIE : ChartType.COLUMN;
        insertChart(type);
    }

    public void updateChart(String label, java.util.function.UnaryOperator<SheetChart> change) {
        if (!(selectedObject() instanceof SheetChart c)) { editor.popups().info("Gráfico", "Selecione um gráfico."); return; }
        int s = sheet();
        editor.edit(label, tx -> tx.updateProperties(s, p -> p.replaceObject(c.id(), x -> change.apply((SheetChart) x))));
    }

    public void editChart() {
        if (!(selectedObject() instanceof SheetChart c)) { editor.popups().info("Gráfico", "Selecione um gráfico."); return; }
        ChartEditorPanel panel = new ChartEditorPanel(c);
        editor.popups().dialog("sheet.chartEditor", "Editar Gráfico", panel, panel::result).ifPresent(next -> updateChart("Editar gráfico", x -> next));
    }

    public void chartTitle() {
        if (!(selectedObject() instanceof SheetChart c)) return;
        editor.popups().prompt("Título do Gráfico", "Título:", c.title()).ifPresent(t -> updateChart("Título do gráfico", x -> x.withTitle(t)));
    }

    public ChartData chartData(SheetChart chart) {
        List<String> categories = new ArrayList<>();
        List<ChartData.Series> series = new ArrayList<>();
        boolean categoriesRead = false;
        double[] xs = null;
        for (ChartSeries s : chart.series()) {
            List<CellValue> values = values(s.valuesRef());
            double[] v = new double[values.size()];
            for (int i = 0; i < v.length; i++) v[i] = values.get(i) instanceof NumberValue n ? n.value() : Double.NaN;
            double[] x = null;
            if (s.categoriesRef() != null) {
                List<CellValue> cats = values(s.categoriesRef());
                x = new double[cats.size()];
                for (int i = 0; i < x.length; i++) x[i] = cats.get(i) instanceof NumberValue n ? n.value() : i + 1;
                if (!categoriesRead) {
                    Optional<NavigationController.Target> t = editor.navigation().resolve(s.categoriesRef());
                    for (int i = 0; i < cats.size(); i++) {
                        CellValue cv = cats.get(i);
                        String text = cv.display();
                        if (t.isPresent()) {
                            CellRange cr = t.get().ranges().getFirst();
                            CellAddress at = cr.columnCount() == 1 ? new CellAddress(cr.firstRow() + i, cr.firstColumn()) : new CellAddress(cr.firstRow(), cr.firstColumn() + i);
                            text = editor.displayText(t.get().sheet(), at);
                        }
                        categories.add(text);
                    }
                    categoriesRead = true;
                    xs = x;
                }
            }
            double[] sizes = null;
            if (s.sizesRef() != null) {
                List<CellValue> sz = values(s.sizesRef());
                sizes = new double[sz.size()];
                for (int i = 0; i < sizes.length; i++) sizes[i] = sz.get(i) instanceof NumberValue n ? n.value() : 1;
            }
            String name = s.nameRef() != null ? values(s.nameRef()).stream().findFirst().map(CellValue::display).orElse(s.name()) : s.name();
            series.add(new ChartData.Series(name == null ? "" : name, v, x == null ? xs : x, sizes, s.color(), s.type()));
        }
        if (!categoriesRead) {
            int n = series.stream().mapToInt(x -> x.values().length).max().orElse(0);
            for (int i = 1; i <= n; i++) categories.add(String.valueOf(i));
        }
        return new ChartData(categories, series);
    }

    private List<CellValue> values(String ref) {
        List<CellValue> list = new ArrayList<>();
        if (ref == null) return list;
        Optional<NavigationController.Target> t = editor.navigation().resolve(ref);
        if (t.isEmpty()) return list;
        int s = t.get().sheet();
        for (CellRange r0 : t.get().ranges()) {
            CellRange r = editor.clipboard().bounded(editor.getWorkbook().sheet(s), r0);
            if (r.cellCount() > 100_000) r = r.resize(Math.min(r.rowCount(), 100_000 / Math.max(1, r.columnCount())), r.columnCount());
            for (CellAddress a : r) list.add(editor.getEngine().valueAt(s, a));
        }
        return list;
    }

    public double[] sparklineData(Sparkline s) {
        List<CellValue> values = values(s.dataRef());
        double[] out = new double[values.size()];
        for (int i = 0; i < out.length; i++) out[i] = values.get(i) instanceof NumberValue n ? n.value() : Double.NaN;
        return out;
    }

    public void insertSparkline(SparklineType type) {
        CellRange locations = editor.getSelection().range();
        editor.popups().prompt("Criar Minigráficos", "Intervalo de dados:", "").filter(t -> !t.isBlank()).ifPresent(text -> {
            Optional<NavigationController.Target> target = editor.navigation().resolve(text);
            if (target.isEmpty()) { editor.popups().warn("Minigráficos", "Intervalo de dados inválido."); return; }
            CellRange data = target.get().ranges().getFirst();
            String sheetName = editor.getWorkbook().sheet(target.get().sheet()).name();
            List<Sparkline> list = new ArrayList<>();
            int n = (int) Math.min(locations.cellCount(), 10_000);
            boolean byRows = data.rowCount() == n || locations.columnCount() == 1;
            int k = 0;
            for (CellAddress loc : locations) {
                if (k >= n) break;
                CellRange part = n == 1 ? data : byRows ? new CellRange(data.firstRow() + k, data.firstColumn(), data.firstRow() + k, data.lastColumn()) : new CellRange(data.firstRow(), data.firstColumn() + k, data.lastRow(), data.firstColumn() + k);
                list.add(new Sparkline(loc, NavigationController.reference(sheetName, part, false), type, 0xFF4472C4, type == SparklineType.LINE, false, false, true));
                k++;
            }
            int s = sheet();
            editor.edit("Minigráficos", tx -> tx.updateProperties(s, p -> {
                List<Sparkline> all = new ArrayList<>(p.sparklines());
                all.removeIf(x -> list.stream().anyMatch(y -> y.location().equals(x.location())));
                all.addAll(list);
                return p.withSparklines(all);
            }));
        });
    }

    public void clearSparklines() {
        int s = sheet();
        List<CellRange> ranges = editor.getSelection().ranges();
        editor.edit("Limpar minigráficos", tx -> tx.updateProperties(s, p -> p.withSparklines(p.sparklines().stream().filter(x -> ranges.stream().noneMatch(r -> r.contains(x.location()))).toList())));
    }

    public void insertImageFromFile() {
        editor.popups().chooseFile(SheetFileDialogRequest.Mode.IMAGE, "Inserir Imagem", List.of(new SheetFileDialogRequest.Filter("Imagens", List.of("png", "jpg", "jpeg", "gif", "bmp"))), null, null).ifPresent(this::insertImage);
    }

    public void insertImage(Path path) {
        try {
            byte[] data = Files.readAllBytes(path);
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (img == null) throw new IOException("Formato de imagem não suportado.");
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            String format = name.endsWith(".png") ? "png" : name.endsWith(".gif") ? "gif" : name.endsWith(".bmp") ? "png" : "jpeg";
            if (name.endsWith(".bmp")) data = encodePng(img);
            addImage(data, format, img.getWidth(), img.getHeight(), path.getFileName().toString());
        } catch (IOException failure) {
            editor.reportError(failure);
        }
    }

    public void insertImage(Image image) {
        BufferedImage b = new BufferedImage(Math.max(1, image.getWidth(null)), Math.max(1, image.getHeight(null)), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = b.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        try {
            addImage(encodePng(b), "png", b.getWidth(), b.getHeight(), "Imagem colada");
        } catch (IOException failure) {
            editor.reportError(failure);
        }
    }

    private static byte[] encodePng(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private void addImage(byte[] data, String format, int w, int h, String alt) {
        double scale = Math.min(1, Math.min(640.0 / Math.max(1, w), 480.0 / Math.max(1, h)));
        CellAddress a = editor.getSelection().active();
        addObject(new SheetImage(UUID.randomUUID().toString(), data, format, ObjectAnchor.at(a, (int) (w * scale), (int) (h * scale)), alt), "Inserir imagem");
    }

    public void insertShape(ShapeType type) {
        String text = type == ShapeType.TEXT_BOX ? "Texto" : "";
        CellAddress a = editor.getSelection().active();
        int w = type == ShapeType.LINE ? 160 : 144, h = type == ShapeType.LINE ? 2 : type == ShapeType.TEXT_BOX ? 60 : 96;
        addObject(new SheetShape(UUID.randomUUID().toString(), type, ObjectAnchor.at(a, w, h), text, null, null, 1.5f), type == ShapeType.TEXT_BOX ? "Caixa de texto" : "Inserir forma");
    }

    public void editShapeText() {
        if (!(selectedObject() instanceof SheetShape sh)) return;
        int s = sheet();
        editor.popups().prompt("Texto da Forma", "Texto:", sh.text()).ifPresent(t -> editor.edit("Texto da forma", tx -> tx.updateProperties(s, p -> p.replaceObject(sh.id(), x -> new SheetShape(sh.id(), sh.type(), sh.anchor(), t, sh.fill(), sh.line(), sh.lineWidth())))));
    }

    public void insertSlicer() {
        SheetTable t = editor.data().activeTable();
        if (t == null) { editor.popups().info("Segmentação de Dados", "Selecione uma célula dentro de uma tabela."); return; }
        JComboBox<String> field = new JComboBox<>(t.columns().stream().map(c -> c.name()).toArray(String[]::new));
        SheetForm f = new SheetForm();
        f.add("Campo:", field);
        editor.popups().dialog("sheet.slicer", "Inserir Segmentação de Dados", f, () -> (String) field.getSelectedItem()).ifPresent(name -> {
            ObjectAnchor anchor = new ObjectAnchor(t.range().firstRow(), Math.min(ws().columns().count() - 1, t.range().lastColumn() + 2), 0, 0, 180, 220);
            addObject(new Slicer(UUID.randomUUID().toString(), name, t.name(), null, name, Set.of(), anchor), "Segmentação de dados");
        });
    }

    public List<String> slicerItems(Slicer s) {
        Optional<SheetTable> table = editor.getWorkbook().table(s.tableName() == null ? "" : s.tableName());
        if (table.isEmpty()) return List.of();
        SheetTable t = table.get();
        int sheet = editor.getWorkbook().sheetOfTable(t.name());
        int col = t.range().firstColumn() + Math.max(0, t.columnIndex(s.field()));
        Set<String> items = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        CellRange data = t.dataRange();
        for (int r = data.firstRow(); r <= data.lastRow() && items.size() < 500; r++) items.add(editor.displayText(sheet, new CellAddress(r, col)));
        return new ArrayList<>(items);
    }

    public void slicerClicked(Slicer s, Point p, Rectangle bounds) {
        List<String> items = slicerItems(s);
        int top = bounds.y + 15 + 12 + 4;
        int index = (p.y - top) / 24;
        if (index < 0 || index >= items.size()) return;
        String item = items.get(index);
        Set<String> selected = new LinkedHashSet<>(s.selected());
        if (selected.contains(item)) selected.remove(item); else selected.add(item);
        if (selected.size() == items.size()) selected.clear();
        int sheet = sheet();
        editor.edit("Segmentação", tx -> tx.updateProperties(sheet, q -> q.replaceObject(s.id(), x -> s.withSelected(Set.copyOf(selected)))));
        editor.getWorkbook().table(s.tableName()).ifPresent(t -> {
            int col = t.range().firstColumn() + Math.max(0, t.columnIndex(s.field()));
            editor.data().setColumnFilter(t, col, selected.isEmpty() ? null : FilterCriteria.values(Set.copyOf(selected), selected.contains("")));
        });
    }

    public PivotTable pivotAt(int sheet, CellAddress a) {
        for (PivotTable p : editor.getWorkbook().sheet(sheet).properties().pivots()) {
            if (p.output() != null && p.output().contains(a)) return p;
            if (p.output() == null && p.target().equals(a)) return p;
        }
        return null;
    }

    private ArrayValue sourceArray(PivotTable p) {
        CellRange r = editor.clipboard().bounded(editor.getWorkbook().sheet(p.sourceSheet()), p.source());
        ArrayValue a = ArrayValue.of(r.rowCount(), r.columnCount());
        for (int i = 0; i < r.rowCount(); i++) for (int j = 0; j < r.columnCount(); j++) a.set(i, j, editor.getEngine().valueAt(p.sourceSheet(), r.firstRow() + i, r.firstColumn() + j));
        return a;
    }

    public void insertPivot() {
        CellRange r = FilterEngine.detectRegion(ws(), editor.getSelection().active().row(), editor.getSelection().active().column());
        if (!editor.getSelection().isSingleCell()) r = editor.clipboard().bounded(ws(), editor.getSelection().range());
        if (r == null || r.rowCount() < 2) { editor.popups().warn("Tabela Dinâmica", "Selecione um intervalo com cabeçalhos e dados."); return; }
        JTextField source = SheetForm.text(NavigationController.reference(ws().name(), r, true), 26);
        JComboBox<String> where = SheetForm.combo("Nova Planilha", "Planilha Existente");
        JTextField location = SheetForm.text("", 12);
        SheetForm f = new SheetForm();
        f.add("Tabela/Intervalo:", source);
        f.add("Local:", where);
        f.add("Célula de destino:", location);
        Optional<Object[]> answer = editor.popups().dialog("sheet.pivotCreate", "Criar Tabela Dinâmica", f, () -> new Object[]{
                editor.navigation().resolve(source.getText()).orElseThrow(() -> new IllegalArgumentException("Intervalo de origem inválido.")), where.getSelectedIndex(), location.getText().strip()});
        answer.ifPresent(o -> {
            NavigationController.Target src = (NavigationController.Target) o[0];
            boolean newSheet = (int) o[1] == 0;
            int s = sheet();
            String name = "TabelaDinâmica" + (editor.getWorkbook().sheets().stream().mapToLong(w -> w.properties().pivots().size()).sum() + 1);
            PivotTable base;
            int targetSheet;
            if (newSheet) {
                int[] idx = {0};
                if (!editor.edit("Nova planilha", tx -> idx[0] = SheetOperations.addSheet(tx, s, null))) return;
                targetSheet = idx[0];
                base = PivotTable.create(name, src.sheet() >= targetSheet ? src.sheet() + 1 : src.sheet(), src.ranges().getFirst(), new CellAddress(2, 0));
            } else {
                NavigationController.Target t = editor.navigation().resolve((String) o[2]).orElseThrow(() -> new IllegalArgumentException("Destino inválido."));
                targetSheet = t.sheet();
                base = PivotTable.create(name, src.sheet(), src.ranges().getFirst(), t.ranges().getFirst().first());
            }
            PivotTable p = base;
            editor.edit("Tabela dinâmica", tx -> { tx.updateProperties(targetSheet, q -> q.addPivot(p)); tx.select(targetSheet, SheetSelection.of(p.target())); });
            editPivot(targetSheet, p);
        });
    }

    public void editPivot() {
        PivotTable p = pivotAt(sheet(), editor.getSelection().active());
        if (p == null) { editor.popups().info("Tabela Dinâmica", "Selecione uma célula dentro de uma tabela dinâmica."); return; }
        editPivot(sheet(), p);
    }

    private void editPivot(int sheetIndex, PivotTable p) {
        PivotEngine engine = new PivotEngine(editor.getWorkbook().properties().date1904(), editor.getConfig().locale());
        List<String> fields = engine.fields(sourceArray(p));
        PivotFieldListPanel panel = new PivotFieldListPanel(p, fields);
        editor.popups().dialog(SheetDialogIds.PIVOT_FIELDS, "Campos da Tabela Dinâmica", panel, panel::result).ifPresent(next -> refreshPivot(sheetIndex, p, next));
    }

    public void refreshPivot() {
        PivotTable p = pivotAt(sheet(), editor.getSelection().active());
        if (p != null) refreshPivot(sheet(), p, p);
    }

    public void refreshAllPivots() {
        for (int s = 0; s < editor.getWorkbook().sheetCount(); s++) for (PivotTable p : List.copyOf(editor.getWorkbook().sheet(s).properties().pivots())) refreshPivot(s, p, p);
    }

    public void refreshPivot(int sheetIndex, PivotTable old, PivotTable next) {
        PivotEngine engine = new PivotEngine(editor.getWorkbook().properties().date1904(), editor.getConfig().locale());
        PivotEngine.Result result = engine.compute(next, sourceArray(next));
        List<List<CellValue>> grid = result.grid();
        int rows = grid.size(), cols = grid.isEmpty() ? 0 : grid.getFirst().size();
        CellAddress t = next.target();
        CellRange output = rows == 0 || cols == 0 ? CellRange.of(t) : new CellRange(t.row(), t.column(), t.row() + rows - 1, t.column() + cols - 1);
        editor.edit("Atualizar tabela dinâmica", tx -> {
            if (old.output() != null) SheetOperations.clear(tx, sheetIndex, old.output(), SheetOperations.ClearMode.ALL);
            for (int i = 0; i < rows; i++) {
                List<CellValue> row = grid.get(i);
                for (int j = 0; j < row.size(); j++) {
                    CellValue v = row.get(j);
                    int r = t.row() + i, c = t.column() + j;
                    tx.setCell(sheetIndex, r, c, SheetCell.of(v));
                    boolean header = i < result.headerRows() || i == rows - 1 && next.columnGrandTotals();
                    int fill = i < result.headerRows() ? 0xFFDDEBF7 : 0xFFF2F2F2;
                    if (header) tx.setStyle(sheetIndex, r, c, s -> s.withBold(true).withFill(dtm.stools.component.panels.editor.sheet.model.SheetFill.solid(fill)));
                }
            }
            tx.updateProperties(sheetIndex, p -> {
                List<PivotTable> list = new ArrayList<>();
                for (PivotTable x : p.pivots()) list.add(x.name().equals(old.name()) ? next.withOutput(output) : x);
                return p.withPivots(list);
            });
        });
    }

    public void deletePivot() {
        PivotTable p = pivotAt(sheet(), editor.getSelection().active());
        if (p == null) return;
        int s = sheet();
        editor.edit("Excluir tabela dinâmica", tx -> {
            if (p.output() != null) SheetOperations.clear(tx, s, p.output(), SheetOperations.ClearMode.ALL);
            tx.updateProperties(s, q -> q.withPivots(q.pivots().stream().filter(x -> !x.name().equals(p.name())).toList()));
        });
    }
}
