package dtm.stools.component.panels.editor.word.editing;

import dtm.stools.component.panels.editor.word.model.*;
import java.util.*;

public final class WordTableEditing {
    public record CellRef(int row, int cell) {}
    private WordTableEditing() {}

    public static List<CellRef> cells(WordTable table, int firstRow, int firstColumn, int lastRow, int lastColumn) {
        List<CellRef> result = new ArrayList<>();
        for (int r = Math.max(0,firstRow); r <= Math.min(lastRow,table.rows().size()-1); r++) {
            WordTableRow row = table.rows().get(r);
            for (int c = 0; c < row.cells().size(); c++) {
                int from = row.columnOf(c), to = from + row.cells().get(c).gridSpan() - 1;
                if (to >= firstColumn && from <= lastColumn) result.add(new CellRef(r,c));
            }
        }
        return result;
    }
    private static WordTableCell emptyLike(WordTableCell cell) {
        WordParagraphStyle style = cell.blocks().getFirst() instanceof WordParagraph p ? p.style() : WordParagraphStyle.DEFAULT;
        return WordTableCell.of(List.of(new WordParagraph(UUID.randomUUID(),List.of(),style))).withFill(cell.fill()).withVerticalAlign(cell.verticalAlign()).withBorder(cell.border());
    }
    public static WordTable insertRow(WordTable table, int rowIndex, boolean below) {
        WordTableRow reference = table.rows().get(rowIndex);
        List<WordTableCell> cells = new ArrayList<>();
        for (WordTableCell c : reference.cells()) {
            WordTableCell.Merge merge = c.verticalMerge() == WordTableCell.Merge.NONE ? WordTableCell.Merge.NONE
                    : below && hasContinuation(table,rowIndex,reference.columnOf(reference.cells().indexOf(c))) || !below && c.verticalMerge() == WordTableCell.Merge.CONTINUE ? WordTableCell.Merge.CONTINUE : WordTableCell.Merge.NONE;
            cells.add(emptyLike(c).withGridSpan(c.gridSpan()).withVerticalMerge(merge));
        }
        List<WordTableRow> rows = new ArrayList<>(table.rows());
        rows.add(below ? rowIndex+1 : rowIndex,WordTableRow.of(cells).withHeight(reference.height()).withHeader(!below && reference.header()));
        return table.withRows(rows);
    }
    private static boolean hasContinuation(WordTable table, int rowIndex, int column) {
        if (rowIndex+1 >= table.rows().size()) return false;
        WordTableCell next = table.cell(rowIndex+1,column);
        return next != null && next.verticalMerge() == WordTableCell.Merge.CONTINUE;
    }
    public static WordTable deleteRows(WordTable table, int firstRow, int lastRow) {
        List<WordTableRow> rows = new ArrayList<>(table.rows());
        for (int r = lastRow; r >= firstRow; r--) rows.remove(r);
        if (rows.isEmpty()) return null;
        for (int r = 0; r < rows.size(); r++) {
            WordTableRow row = rows.get(r); List<WordTableCell> cells = new ArrayList<>(row.cells()); boolean changed = false;
            for (int c = 0; c < cells.size(); c++) {
                WordTableCell cell = cells.get(c);
                boolean orphan = cell.verticalMerge() == WordTableCell.Merge.CONTINUE && (r == 0 || rows.get(r-1).cellAt(row.columnOf(c)) < 0);
                boolean lonelyStart = cell.verticalMerge() == WordTableCell.Merge.RESTART && (r+1 >= rows.size() || continuationAt(rows.get(r+1),row.columnOf(c)) == null);
                if (orphan) { cells.set(c,cell.withVerticalMerge(WordTableCell.Merge.RESTART)); changed = true; }
                else if (lonelyStart) { cells.set(c,cell.withVerticalMerge(WordTableCell.Merge.NONE)); changed = true; }
            }
            if (changed) rows.set(r,row.withCells(cells));
        }
        return table.withRows(rows);
    }
    private static WordTableCell continuationAt(WordTableRow row, int column) {
        int i = row.cellAt(column); if (i < 0) return null;
        WordTableCell c = row.cells().get(i); return c.verticalMerge() == WordTableCell.Merge.CONTINUE ? c : null;
    }
    public static WordTable insertColumn(WordTable table, int gridColumn, boolean right) {
        int boundary = right ? gridColumn + 1 : gridColumn;
        float width = table.columnWidths().get(gridColumn);
        List<Float> widths = new ArrayList<>(table.columnWidths());
        float total = table.width(); widths.add(boundary,width);
        float scale = total/(total+width);
        widths.replaceAll(w -> Math.max(12,w*scale));
        List<WordTableRow> rows = new ArrayList<>();
        for (WordTableRow row : table.rows()) {
            List<WordTableCell> cells = new ArrayList<>(row.cells());
            int covering = boundary < row.gridColumns() ? row.cellAt(boundary) : -1;
            if (covering >= 0 && row.columnOf(covering) < boundary) cells.set(covering,cells.get(covering).withGridSpan(cells.get(covering).gridSpan()+1));
            else {
                int insertAt = covering >= 0 ? covering : cells.size();
                WordTableCell reference = cells.get(Math.min(insertAt,cells.size()-1));
                cells.add(insertAt,emptyLike(reference).withVerticalMerge(WordTableCell.Merge.NONE));
            }
            rows.add(row.withCells(cells));
        }
        return new WordTable(table.id(),rows,widths,table.border(),table.alignment(),table.cellPadding(),table.styleId(),table.extras());
    }
    public static WordTable deleteColumns(WordTable table, int firstColumn, int lastColumn) {
        if (firstColumn == 0 && lastColumn >= table.gridColumns()-1) return null;
        List<Float> widths = new ArrayList<>(table.columnWidths());
        for (int c = lastColumn; c >= firstColumn; c--) widths.remove(c);
        List<WordTableRow> rows = new ArrayList<>();
        for (WordTableRow row : table.rows()) {
            List<WordTableCell> cells = new ArrayList<>();
            for (int c = 0; c < row.cells().size(); c++) {
                WordTableCell cell = row.cells().get(c);
                int from = row.columnOf(c), to = from + cell.gridSpan() - 1;
                int overlap = Math.max(0,Math.min(to,lastColumn) - Math.max(from,firstColumn) + 1);
                if (overlap == 0) cells.add(cell);
                else if (overlap < cell.gridSpan()) cells.add(cell.withGridSpan(cell.gridSpan()-overlap));
            }
            if (cells.isEmpty()) cells.add(WordTableCell.of("").withGridSpan(Math.max(1,widths.size())));
            rows.add(row.withCells(cells));
        }
        return new WordTable(table.id(),rows,widths,table.border(),table.alignment(),table.cellPadding(),table.styleId(),table.extras());
    }
    public static WordTable merge(WordTable table, int firstRow, int firstColumn, int lastRow, int lastColumn) {
        if (firstRow == lastRow && firstColumn == lastColumn) return table;
        List<WordTableRow> rows = new ArrayList<>(table.rows());
        List<WordBlock> content = new ArrayList<>();
        for (int r = firstRow; r <= lastRow; r++) {
            WordTableRow row = rows.get(r);
            int first = row.cellAt(firstColumn), last = row.cellAt(lastColumn);
            if (first < 0 || last < 0 || row.columnOf(first) != firstColumn || row.columnOf(last) + row.cells().get(last).gridSpan() - 1 != lastColumn)
                throw new IllegalArgumentException("A seleção atravessa células mescladas de forma não retangular");
            WordTableCell base = row.cells().get(first);
            for (int c = first; c <= last; c++) for (WordBlock b : row.cells().get(c).blocks()) if (!(b instanceof WordParagraph p && p.length() == 0)) content.add(b);
            WordTableCell.Merge merge = firstRow == lastRow ? base.verticalMerge() : r == firstRow ? WordTableCell.Merge.RESTART : WordTableCell.Merge.CONTINUE;
            WordTableCell merged = (r == firstRow ? base : emptyLike(base).withId(UUID.randomUUID())).withGridSpan(lastColumn-firstColumn+1).withVerticalMerge(merge);
            List<WordTableCell> cells = new ArrayList<>(row.cells().subList(0,first));
            cells.add(merged);
            cells.addAll(row.cells().subList(last+1,row.cells().size()));
            rows.set(r,row.withCells(cells));
        }
        WordTableRow top = rows.get(firstRow); int index = top.cellAt(firstColumn);
        WordTableCell merged = top.cells().get(index);
        List<WordBlock> blocks = content.isEmpty() ? List.of(new WordParagraph(UUID.randomUUID(),List.of(),WordParagraphStyle.DEFAULT)) : content;
        rows.set(firstRow,top.withCell(index,merged.withBlocks(blocks)));
        return table.withRows(rows);
    }
    public static WordTable split(WordTable table, int rowIndex, int cellIndex) {
        WordTableRow row = table.rows().get(rowIndex); WordTableCell cell = row.cells().get(cellIndex);
        int column = row.columnOf(cellIndex);
        if (cell.gridSpan() > 1 || cell.verticalMerge() == WordTableCell.Merge.RESTART) {
            List<WordTableRow> rows = new ArrayList<>(table.rows());
            int span = cell.gridSpan();
            for (int r = rowIndex; r < rows.size(); r++) {
                WordTableRow current = rows.get(r); int i = current.cellAt(column);
                if (i < 0) break;
                WordTableCell c = current.cells().get(i);
                if (r > rowIndex && c.verticalMerge() != WordTableCell.Merge.CONTINUE) break;
                List<WordTableCell> cells = new ArrayList<>(current.cells());
                cells.set(i,c.withGridSpan(1).withVerticalMerge(WordTableCell.Merge.NONE));
                for (int k = 1; k < span; k++) cells.add(i+k,emptyLike(c).withVerticalMerge(WordTableCell.Merge.NONE));
                rows.set(r,current.withCells(cells));
                if (cell.verticalMerge() != WordTableCell.Merge.RESTART) break;
            }
            return table.withRows(rows);
        }
        List<Float> widths = new ArrayList<>(table.columnWidths());
        float half = widths.get(column)/2; widths.set(column,Math.max(6,half)); widths.add(column+1,Math.max(6,half));
        List<WordTableRow> rows = new ArrayList<>();
        for (int r = 0; r < table.rows().size(); r++) {
            WordTableRow current = table.rows().get(r); List<WordTableCell> cells = new ArrayList<>(current.cells());
            int i = current.cellAt(column);
            if (r == rowIndex) cells.add(cellIndex+1,emptyLike(cell));
            else if (i >= 0) cells.set(i,cells.get(i).withGridSpan(cells.get(i).gridSpan()+1));
            rows.add(current.withCells(cells));
        }
        return new WordTable(table.id(),rows,widths,table.border(),table.alignment(),table.cellPadding(),table.styleId(),table.extras());
    }
    public static WordTable resizeBoundary(WordTable table, int boundary, float delta) {
        List<Float> widths = new ArrayList<>(table.columnWidths());
        if (boundary < 0 || boundary >= widths.size()) return table;
        float left = widths.get(boundary);
        if (boundary == widths.size()-1) { widths.set(boundary,Math.max(12,left+delta)); return table.withColumnWidths(widths); }
        float right = widths.get(boundary+1);
        float d = Math.max(12-left,Math.min(right-12,delta));
        widths.set(boundary,left+d); widths.set(boundary+1,right-d);
        return table.withColumnWidths(widths);
    }
    public static WordTable distributeColumns(WordTable table) {
        float each = table.width()/table.gridColumns();
        return table.withColumnWidths(Collections.nCopies(table.gridColumns(),each));
    }
    public static WordTable updateCells(WordTable table, int firstRow, int firstColumn, int lastRow, int lastColumn, java.util.function.UnaryOperator<WordTableCell> operation) {
        List<WordTableRow> rows = new ArrayList<>(table.rows());
        for (CellRef ref : cells(table,firstRow,firstColumn,lastRow,lastColumn)) rows.set(ref.row(),rows.get(ref.row()).withCell(ref.cell(),operation.apply(rows.get(ref.row()).cells().get(ref.cell()))));
        return table.withRows(rows);
    }
    public static WordTable clear(WordTable table, int firstRow, int firstColumn, int lastRow, int lastColumn) {
        return updateCells(table,firstRow,firstColumn,lastRow,lastColumn,c -> c.withBlocks(List.of(new WordParagraph(UUID.randomUUID(),List.of(),
                c.blocks().getFirst() instanceof WordParagraph p ? p.style() : WordParagraphStyle.DEFAULT))));
    }
    public static WordTable fill(WordTable table, int firstRow, int firstColumn, List<List<String>> values, WordTextStyle style) {
        WordTable result = table;
        int columns = table.gridColumns();
        while (result.rows().size() < firstRow + values.size()) result = insertRow(result,result.rows().size()-1,true);
        List<WordTableRow> rows = new ArrayList<>(result.rows());
        for (int r = 0; r < values.size(); r++) {
            WordTableRow row = rows.get(firstRow+r);
            for (int c = 0; c < values.get(r).size() && firstColumn + c < columns; c++) {
                int index = row.cellAt(firstColumn+c);
                if (index < 0 || row.columnOf(index) != firstColumn+c) continue;
                WordTableCell cell = row.cells().get(index);
                WordParagraphStyle ps = cell.blocks().getFirst() instanceof WordParagraph p ? p.style() : WordParagraphStyle.DEFAULT;
                row = row.withCell(index,cell.withBlocks(List.of(new WordParagraph(UUID.randomUUID(),List.of(new WordRun(WordDocument.normalize(values.get(r).get(c)).replace('\n',' '),style)),ps))));
            }
            rows.set(firstRow+r,row);
        }
        return result.withRows(rows);
    }
    public static List<List<String>> parseTabular(String text) {
        List<List<String>> rows = new ArrayList<>();
        for (String line : WordDocument.normalize(text).split("\n",-1)) rows.add(List.of(line.split("\t",-1)));
        while (!rows.isEmpty() && rows.getLast().size() == 1 && rows.getLast().getFirst().isEmpty()) rows.removeLast();
        return rows;
    }
    public static String toTabular(WordTable table, int firstRow, int firstColumn, int lastRow, int lastColumn) {
        StringBuilder b = new StringBuilder();
        for (int r = firstRow; r <= lastRow; r++) {
            if (r > firstRow) b.append('\n');
            WordTableRow row = table.rows().get(r); boolean first = true;
            for (int c = 0; c < row.cells().size(); c++) {
                int col = row.columnOf(c);
                if (col < firstColumn || col > lastColumn) continue;
                if (!first) b.append('\t'); first = false;
                b.append(row.cells().get(c).plainText().replace('\n',' ').replace('\t',' '));
            }
        }
        return b.toString();
    }
    public static WordTable subTable(WordTable table, int firstRow, int firstColumn, int lastRow, int lastColumn) {
        List<WordTableRow> rows = new ArrayList<>();
        for (int r = firstRow; r <= lastRow; r++) {
            WordTableRow row = table.rows().get(r); List<WordTableCell> cells = new ArrayList<>();
            for (int c = 0; c < row.cells().size(); c++) {
                int col = row.columnOf(c);
                if (col >= firstColumn && col <= lastColumn) {
                    WordTableCell cell = row.cells().get(c);
                    int span = Math.min(cell.gridSpan(),lastColumn-col+1);
                    WordTableCell.Merge merge = r == firstRow && cell.verticalMerge() == WordTableCell.Merge.CONTINUE ? WordTableCell.Merge.NONE : cell.verticalMerge();
                    cells.add(cell.withGridSpan(span).withVerticalMerge(merge));
                }
            }
            if (!cells.isEmpty()) rows.add(row.withCells(cells).withHeader(false));
        }
        return new WordTable(UUID.randomUUID(),rows,table.columnWidths().subList(firstColumn,lastColumn+1),table.border(),table.alignment(),table.cellPadding(),table.styleId(),table.extras());
    }
}
