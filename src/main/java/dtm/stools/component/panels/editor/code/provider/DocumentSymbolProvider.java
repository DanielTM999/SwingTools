package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.DocumentSymbol;

import java.util.List;

@FunctionalInterface
public interface DocumentSymbolProvider extends CodeEditorProvider {

    List<DocumentSymbol> getDocumentSymbols(String buffer);
}
