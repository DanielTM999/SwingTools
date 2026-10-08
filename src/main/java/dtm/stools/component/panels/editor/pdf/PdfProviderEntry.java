package dtm.stools.component.panels.editor.pdf;

import dtm.stools.component.panels.editor.pdf.provider.PdfProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfProviderRegistration;

import java.util.List;

record PdfProviderEntry(PdfProvider provider, List<PdfProviderRegistration> hooks, long order) {}
