package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.prototype.Token;

import java.util.Collection;

public interface TokenRenderCodeEditorProvider extends CodeEditorProvider {

    void render(Collection<Token> tokens, TokenColorProvider colorProvider, CodeEditorTextArea textArea);

}