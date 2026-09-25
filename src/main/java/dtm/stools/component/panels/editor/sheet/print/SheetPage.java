package dtm.stools.component.panels.editor.sheet.print;

import dtm.stools.component.panels.editor.sheet.model.CellRange;

public record SheetPage(int sheet, int number, CellRange cells, CellRange repeatRows, CellRange repeatColumns) {}
