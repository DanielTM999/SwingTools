package dtm.stools.component.panels.editor.code.provider.def;

import dtm.stools.component.panels.editor.code.prototype.Token;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import dtm.stools.component.panels.editor.code.provider.PreparedTokenRenderCodeEditorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenColorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenRenderSnapshot;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultTokenRenderProvider implements PreparedTokenRenderCodeEditorProvider {

    @Override
    public Collection<StyledRange> prepare(Collection<Token> tokens,
                                           TokenColorProvider colorProvider,
                                           TokenRenderSnapshot snapshot) {
        return buildRanges(tokens, colorProvider, snapshot.defaultStyle());
    }

    protected List<StyledRange> buildRanges(Collection<Token> tokens,
                                            TokenColorProvider colorProvider,
                                            TextStyle baseStyle) {
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
