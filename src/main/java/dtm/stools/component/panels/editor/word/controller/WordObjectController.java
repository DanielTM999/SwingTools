package dtm.stools.component.panels.editor.word.controller;

import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.editing.WordTableEditing;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.provider.WordObjectPropertiesContext;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class WordObjectController {
    private final WordSession session;
    private final Supplier<WordObjectRegistry> registry;

    public WordObjectController(WordSession session, Supplier<WordObjectRegistry> registry) { this.session = session; this.registry = registry; }

    private void requireEditable() { if (session.isReadOnly()) throw new IllegalStateException("Document is read-only"); }
    public void insert(WordInlineObject object, WordResource... resources) {
        requireEditable();
        int start = session.getSelection().start(), end = session.getSelection().end();
        boolean cells = session.getContentSelection() instanceof WordCellSelection;
        WordDocument base = start == end || cells ? session.getDocument() : session.getDocument().delete(start,end);
        for (WordResource r : resources) base = base.withResource(r);
        WordDocument next = base.insertObject(start,object);
        session.executeWithSelection("Inserir " + object.type(),d -> next,d -> object.textual() ? new WordSelection(start+1,start+1) : new WordObjectSelection(start,object.id()));
    }
    public WordImage imageFromBytes(byte[] data, String contentType, String altText) throws IOException {
        WordResource resource = WordResource.of(data,contentType);
        float width = 200, height = 150;
        try { BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(data)); if (decoded != null) { width = decoded.getWidth()*0.75f; height = decoded.getHeight()*0.75f; } } catch (IOException ignored) {}
        float max = session.getDocument().pageSettings().columnWidth();
        if (width > max) { height = height*max/width; width = max; }
        WordImage image = WordImage.of(resource.id(),Math.max(4,width),Math.max(4,height)).withAltText(altText == null ? "" : altText);
        insert(image,resource);
        return image;
    }
    public WordImage insertImage(Path file) throws IOException {
        long size = Files.size(file);
        if (size > 32L*1024*1024) throw new IOException("Imagem muito grande (limite de 32 MB)");
        String type = WordResource.contentTypeFor(file.getFileName().toString());
        if (!type.startsWith("image/")) throw new IOException("Formato de imagem não suportado: " + file.getFileName());
        return imageFromBytes(Files.readAllBytes(file),type,file.getFileName().toString());
    }
    public WordImage insertImage(Image image, String altText) throws IOException {
        return imageFromBytes(png(image),"image/png",altText);
    }
    static byte[] png(Image image) throws IOException {
        int w = image.getWidth(null), h = image.getHeight(null);
        if (w <= 0 || h <= 0) throw new IOException("Imagem inválida");
        BufferedImage buffered = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buffered.createGraphics(); try { g.drawImage(image,0,0,null); } finally { g.dispose(); }
        ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(buffered,"png",out); return out.toByteArray();
    }
    public void replaceImage(Path file) throws IOException {
        requireEditable();
        WordObjectSelection selection = session.getObjectSelection().orElseThrow(() -> new IllegalStateException("Selecione uma imagem"));
        WordObjectRun run = session.getDocument().objectAt(selection.offset());
        if (!(run.object() instanceof WordImage image)) throw new IllegalStateException("Selecione uma imagem");
        WordResource resource = WordResource.of(Files.readAllBytes(file),WordResource.contentTypeFor(file.getFileName().toString()));
        float height = image.height();
        try { BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(resource.data())); if (decoded != null) height = image.width()*decoded.getHeight()/(float)decoded.getWidth(); } catch (IOException ignored) {}
        WordImage updated = image.withResource(resource.id()).resize(image.width(),Math.max(1,height)).withCrop(WordCrop.NONE);
        int offset = selection.offset();
        session.executeWithSelection("Substituir imagem",d -> d.withResource(resource).replaceObject(offset,updated),d -> new WordObjectSelection(offset,updated.id()));
    }
    public void insertTable(int rows, int columns) {
        requireEditable();
        WordDocument d = session.getDocument();
        float width = d.pageSettings().columnWidth();
        var outer = d.tableAt(session.getSelection().caret());
        if (outer.isPresent()) width = Math.max(48,outer.get().table().spanWidth(outer.get().gridColumn(),outer.get().cellValue().gridSpan()) - 2*outer.get().table().cellPadding() - 2);
        WordTable base = WordTable.create(rows,columns,width);
        WordTable table = new WordTable(base.id(),base.rows(),base.columnWidths(),WordBorder.DEFAULT,WordTable.Alignment.LEFT,5.4f,"TableGrid",List.of());
        session.insertBlock(table);
    }
    public WordChart insertChart(WordChartType type) {
        WordChart chart = WordChart.sample().withChartType(type);
        insert(chart);
        return chart;
    }
    public WordShape insertShape(WordShapeType type) {
        WordShape shape = type.isLinear() ? WordShape.of(type,120,type == WordShapeType.ELBOW_CONNECTOR ? 60 : 0.01f) : WordShape.of(type,type == WordShapeType.TEXT_BOX ? 180 : 120,type == WordShapeType.TEXT_BOX ? 60 : 80);
        insert(shape);
        return shape;
    }
    public WordEquation insertEquation(String linear, boolean display) { WordEquation e = WordEquation.parse(linear,display); insert(e); return e; }
    public WordDiagram insertDiagram(WordDiagramLayout layout, String outline) { WordDiagram d = WordDiagram.parse(layout,outline); insert(d); return d; }
    public WordFormField insertFormField(WordFormField.Kind kind, String name) {
        WordFormField field = switch (kind) {
            case TEXT -> WordFormField.text(name);
            case CHECKBOX -> WordFormField.checkbox(name,false);
            case DROPDOWN -> WordFormField.dropdown(name,List.of("Opção 1","Opção 2","Opção 3"));
            case DATE -> WordFormField.date(name);
        };
        insert(field);
        return field;
    }
    public void deleteSelectedObject() {
        requireEditable();
        WordObjectSelection selection = session.getObjectSelection().orElseThrow(() -> new IllegalStateException("Nenhum objeto selecionado"));
        int offset = selection.offset();
        session.execute("Excluir objeto",d -> d.delete(offset,offset+1),new WordSelection(offset,offset));
    }
    public void updateSelectedObject(String label, UnaryOperator<WordInlineObject> operation) {
        requireEditable();
        WordObjectSelection selection = session.getObjectSelection().orElseThrow(() -> new IllegalStateException("Nenhum objeto selecionado"));
        WordInlineObject current = session.getDocument().objectAt(selection.offset()).object();
        session.replaceObject(selection.offset(),operation.apply(current),label);
    }
    public void setWrap(WordPlacement.Wrap wrap) {
        updateSelectedObject("Disposição do objeto",o -> {
            WordPlacement p = o.placement();
            if (wrap == WordPlacement.Wrap.INLINE) return o.withPlacement(WordPlacement.INLINE);
            return o.withPlacement(p.floating() ? p.withWrap(wrap) : WordPlacement.floating(0,0,wrap));
        });
    }
    public void changeOrder(int delta) {
        updateSelectedObject(delta > 0 ? "Trazer para frente" : "Enviar para trás",o -> o.placement().floating() ? o.withPlacement(o.placement().withZOrder(o.placement().zOrder()+delta)) : o);
    }
    public void groupShapesInParagraph() {
        requireEditable();
        WordDocument d = session.getDocument();
        int index = d.paragraphIndex(session.getSelection().caret());
        WordParagraph p = d.paragraphs().get(index);
        List<WordShape> shapes = new ArrayList<>(); List<WordInline> rest = new ArrayList<>(); int firstShape = -1, offset = 0;
        for (WordInline inline : p.runs()) {
            if (inline instanceof WordObjectRun o && o.object() instanceof WordShape s && s.placement().floating() && s.shapeType() != WordShapeType.GROUP) { shapes.add(s); if (firstShape < 0) firstShape = rest.size(); }
            else rest.add(inline);
            offset += inline.length();
        }
        if (shapes.size() < 2) throw new IllegalStateException("O parágrafo precisa de ao menos duas formas flutuantes para agrupar");
        WordShape group = WordShape.group(shapes);
        rest.add(firstShape,new WordObjectRun(group));
        int at = d.paragraphStart(index) + textOffset(rest,firstShape);
        WordParagraph next = p.withRuns(rest);
        session.executeWithSelection("Agrupar formas",doc -> doc.replaceParagraphs(Map.of(p.id(),next)),doc -> new WordObjectSelection(at,group.id()));
    }
    private static int textOffset(List<WordInline> runs, int index) { int n = 0; for (int i = 0; i < index; i++) n += runs.get(i).length(); return n; }
    public void ungroupSelected() {
        requireEditable();
        WordObjectSelection selection = session.getObjectSelection().orElseThrow();
        WordDocument d = session.getDocument();
        if (!(d.objectAt(selection.offset()).object() instanceof WordShape group) || group.shapeType() != WordShapeType.GROUP) throw new IllegalStateException("Selecione um grupo");
        int offset = selection.offset();
        session.execute("Desagrupar",doc -> {
            WordDocument next = doc.delete(offset,offset+1);
            int at = offset;
            for (WordShape s : group.ungroup()) { next = next.insertObject(at,s); at++; }
            return next;
        },new WordSelection(offset,offset));
    }

    public Optional<WordTableLocation> currentTable() {
        var content = session.getContentSelection();
        WordDocument d = session.getDocument();
        if (content instanceof WordCellSelection cells) {
            WordTable table = d.findTable(cells.tableId()).orElseThrow();
            return Optional.of(new WordTableLocation(table,cells.firstRow(),Math.max(0,table.rows().get(cells.firstRow()).cellAt(cells.firstColumn())),cells.firstColumn()));
        }
        return d.tableAt(content.range().caret());
    }
    private int[] rect() {
        if (session.getContentSelection() instanceof WordCellSelection c) return new int[]{c.firstRow(),c.firstColumn(),c.lastRow(),c.lastColumn()};
        WordTableLocation l = currentTable().orElseThrow(() -> new IllegalStateException("Posicione o cursor em uma tabela"));
        return new int[]{l.row(),l.gridColumn(),l.row(),l.gridColumn()+l.cellValue().gridSpan()-1};
    }
    public void tableOperation(String label, java.util.function.BiFunction<WordTable,int[],WordTable> operation) {
        requireEditable();
        WordTableLocation location = currentTable().orElseThrow(() -> new IllegalStateException("Posicione o cursor em uma tabela"));
        int[] r = rect(); UUID id = location.table().id();
        WordTable updated = operation.apply(location.table(),r);
        if (updated == null) { deleteTable(); return; }
        int row = Math.min(r[0],updated.rows().size()-1);
        session.executeWithSelection(label,d -> d.updateTable(id,t -> updated),d -> {
            WordTable t = d.findTable(id).orElseThrow();
            int cell = Math.max(0,t.rows().get(row).cellAt(Math.min(r[1],t.gridColumns()-1)));
            int start = d.cellRange(id,row,cell)[0];
            return new WordSelection(start,start);
        });
    }
    public void insertRow(boolean below) { tableOperation("Inserir linha",(t,r) -> WordTableEditing.insertRow(t,below ? r[2] : r[0],below)); }
    public void deleteRows() { tableOperation("Excluir linhas",(t,r) -> WordTableEditing.deleteRows(t,r[0],r[2])); }
    public void insertColumn(boolean right) { tableOperation("Inserir coluna",(t,r) -> WordTableEditing.insertColumn(t,right ? r[3] : r[1],right)); }
    public void deleteColumns() { tableOperation("Excluir colunas",(t,r) -> WordTableEditing.deleteColumns(t,r[1],r[3])); }
    public void mergeCells() { tableOperation("Mesclar células",(t,r) -> WordTableEditing.merge(t,r[0],r[1],r[2],r[3])); }
    public void splitCell() { tableOperation("Dividir célula",(t,r) -> WordTableEditing.split(t,r[0],Math.max(0,t.rows().get(r[0]).cellAt(r[1])))); }
    public void distributeColumns() { tableOperation("Distribuir colunas",(t,r) -> WordTableEditing.distributeColumns(t)); }
    public void toggleHeaderRow() { tableOperation("Linha de cabeçalho",(t,r) -> t.withRow(r[0],t.rows().get(r[0]).withHeader(!t.rows().get(r[0]).header()))); }
    public void fillCells(Integer color) { tableOperation("Preenchimento",(t,r) -> WordTableEditing.updateCells(t,r[0],r[1],r[2],r[3],c -> c.withFill(color))); }
    public void alignCells(WordTableCell.VerticalAlign align) { tableOperation("Alinhamento vertical",(t,r) -> WordTableEditing.updateCells(t,r[0],r[1],r[2],r[3],c -> c.withVerticalAlign(align))); }
    public void tableBorder(WordBorder border) { tableOperation("Bordas da tabela",(t,r) -> t.withBorder(border)); }
    public void deleteTable() {
        requireEditable();
        WordTableLocation location = currentTable().orElseThrow(() -> new IllegalStateException("Posicione o cursor em uma tabela"));
        UUID id = location.table().id();
        int start = session.getDocument().tableRange(id)[0];
        session.execute("Excluir tabela",d -> d.replaceBlock(id,List.of()),new WordSelection(0,0));
        int caret = Math.min(start,session.getDocument().length());
        if (!session.getDocument().isBoundary(caret)) caret = session.getDocument().previousBoundary(caret);
        session.setSelection(caret,caret);
    }

    public void setLink(String target) {
        requireEditable();
        if (target == null || target.isBlank()) throw new IllegalArgumentException("Informe o endereço do link");
        String link = target.strip();
        if (!link.startsWith("#") && !link.matches("(?i)(https?|mailto|ftp|file):.*")) link = "https://" + link;
        WordSelection s = session.getSelection();
        if (s.isEmpty()) { String text = link.startsWith("#") ? link.substring(1) : link; WordTextStyle style = session.getInsertionStyle().withLink(link).withColor(0x0563C1).withUnderline(true); int start = s.start(); session.execute("Inserir link",d -> d.replaceContent(start,start,text,style),new WordSelection(start+text.length(),start+text.length())); return; }
        String value = link;
        session.formatSelection(st -> st.withLink(value).withColor(0x0563C1).withUnderline(true));
    }
    public void removeLink() {
        requireEditable();
        WordDocument d = session.getDocument(); WordSelection s = session.getSelection();
        int start = s.start(), end = s.end();
        if (start == end) {
            String link = start < d.length() ? d.styleAt(start).link() : start > 0 ? d.styleAt(start-1).link() : null;
            if (link == null) return;
            int a = start; while (a > 0 && link.equals(d.styleAt(a-1).link()) && d.text().charAt(a-1) != '\n') a--;
            int b = start; while (b < d.length() && link.equals(d.styleAt(b).link()) && d.text().charAt(b) != '\n') b++;
            start = a; end = b;
        }
        int from = start, to = end;
        WordTextStyle base = d.styles().resolveText(d.paragraphAt(from).style().styleId());
        session.execute("Remover link",doc -> doc.format(from,to,st -> st.link() == null ? st : st.withLink(null).withUnderline(false).withColor(base.color())));
    }

    public WordObjectPropertiesContext propertiesContext(Component owner, Locale locale, Runnable onClose) {
        WordDocument document = session.getDocument();
        Optional<WordInlineObject> object = session.getSelectedObject();
        Optional<WordTableLocation> table = object.isPresent() ? Optional.empty() : currentTable();
        Optional<WordCellSelection> cells = session.getCellSelection();
        if (cells.isEmpty() && table.isPresent()) {
            WordTableLocation l = table.get(); int[] range = document.cellRange(l.table().id(),l.row(),l.cell());
            cells = Optional.of(new WordCellSelection(l.table().id(),l.row(),l.gridColumn(),l.row(),l.gridColumn()+l.cellValue().gridSpan()-1,new WordSelection(range[0],range[1])));
        }
        int objectOffset = session.getObjectSelection().map(WordObjectSelection::offset).orElse(-1);
        Optional<WordCellSelection> finalCells = cells;
        long revision = session.getRevision();
        return new WordObjectPropertiesContext() {
            public Component owner() { return owner; }
            public Locale locale() { return locale; }
            public WordDocument document() { return document; }
            public boolean readOnly() { return session.isReadOnly(); }
            public Optional<WordInlineObject> object() { return object; }
            public Optional<WordTable> table() { return table.map(WordTableLocation::table); }
            public Optional<WordCellSelection> cells() { return finalCells; }
            public WordObjectRegistry registry() { return registry.get(); }
            public void applyObject(WordInlineObject updated) {
                requireEditable();
                if (objectOffset < 0 || session.getRevision() != revision) throw new IllegalStateException("O documento mudou; reabra as propriedades");
                session.replaceObject(objectOffset,Objects.requireNonNull(updated),"Propriedades do objeto");
            }
            public void applyTable(WordTable updated) {
                requireEditable();
                if (table.isEmpty() || session.getRevision() != revision) throw new IllegalStateException("O documento mudou; reabra as propriedades");
                UUID id = table.get().table().id();
                if (!updated.id().equals(id)) throw new IllegalArgumentException("A tabela atualizada deve manter o identificador");
                session.execute("Propriedades da tabela",d -> d.updateTable(id,t -> updated));
            }
            public void closed() { onClose.run(); }
        };
    }
}
