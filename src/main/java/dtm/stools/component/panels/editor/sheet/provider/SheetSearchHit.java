package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;

public record SheetSearchHit(int sheet, String sheetName, CellAddress cell, String text) {}
