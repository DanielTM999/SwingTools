package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.prototype.constants.TokenType;

public interface TokenClassifierCodeEditorProvider extends CodeEditorProvider {
    String classify(String token);
}
