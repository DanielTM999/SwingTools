package dtm.stools.component.panels.editor.code.provider.def;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.prototype.Token;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import dtm.stools.component.panels.editor.code.provider.TokenColorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenRenderCodeEditorProvider;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultTokenRenderProvider implements TokenRenderCodeEditorProvider {

    @Override
    public void render(Collection<Token> tokens, TokenColorProvider colorProvider, CodeEditorTextArea textArea) {
        if (SwingUtilities.isEventDispatchThread()) {

            textArea.replaceStyledRanges(buildRanges(tokens, colorProvider, textArea));
        } else {
            List<StyledRange> ranges = buildRanges(tokens, colorProvider, textArea);

            SwingUtilities.invokeLater(() -> textArea.replaceStyledRanges(ranges));
        }
    }

    protected List<StyledRange> buildRanges(Collection<Token> tokens, TokenColorProvider colorProvider, CodeEditorTextArea textArea) {
        TextStyle baseStyle = textArea.getDefaultStyle();
        boolean baseBold = baseStyle.isBold();
        boolean baseItalic = baseStyle.isItalic();

        List<StyledRange> ranges = new ArrayList<>(tokens.size());
        for (Token token : tokens) {
            Color color = colorProvider.getColor(token.getType());
            if (color == null) continue;
            ranges.add(new StyledRange(
                    TextStyle.builder()
                            .bold(baseBold)
                            .italic(baseItalic)
                            .foreground(color)
                            .build(),
                    token.getStartOffset(), token.getEndOffset()
            ));
        }
        return ranges;
    }
}
