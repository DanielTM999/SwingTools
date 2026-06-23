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
import java.util.concurrent.CompletableFuture;

public class DefaultTokenRenderProvider implements TokenRenderCodeEditorProvider {

    @Override
    public void render(Collection<Token> tokens, TokenColorProvider colorProvider, CodeEditorTextArea textArea) {
        Runnable renderTask = () -> renderOffEdt(tokens, colorProvider, textArea);
        if (SwingUtilities.isEventDispatchThread()) {
            CompletableFuture.runAsync(renderTask);
        } else {
            renderTask.run();
        }
    }

    protected void renderOffEdt(Collection<Token> tokens, TokenColorProvider colorProvider, CodeEditorTextArea textArea) {
        TextStyle baseStyle = textArea.getDefaultStyle();
        boolean baseBold = baseStyle.isBold();
        boolean baseItalic = baseStyle.isItalic();

        // Constrói toda a lista off-EDT — sem inundar a fila de eventos.
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

        // Um único invokeLater que aplica tudo de uma vez.
        SwingUtilities.invokeLater(() -> {
            textArea.replaceStyledRanges(ranges);
        });
    }
}
