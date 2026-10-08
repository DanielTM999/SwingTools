package dtm.stools.component.panels.editor.pdf.command;

import dtm.stools.component.panels.editor.pdf.api.PdfChange;

public record PdfHistoryEntry(byte[] snapshot, long state, String label, PdfChange change, int pageCount) {}
