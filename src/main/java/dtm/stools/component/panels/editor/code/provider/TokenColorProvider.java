package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.prototype.constants.TokenType;

import java.awt.*;

public interface TokenColorProvider extends CodeEditorProvider {
    Color getColor(String type);
}