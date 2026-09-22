package dtm.stools.component.panels.editor.code.documenthighlight;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.List;

@FunctionalInterface
public interface DocumentHighlightProvider extends CodeEditorProvider {

    List<DocumentHighlight> getDocumentHighlights(DocumentHighlightContext context);
}
