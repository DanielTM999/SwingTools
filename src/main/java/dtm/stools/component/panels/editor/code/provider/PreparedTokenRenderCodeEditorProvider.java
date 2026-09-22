package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.panels.editor.code.prototype.Token;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;

import java.util.Collection;
import java.util.List;

/**
 * Optional rendering contract that separates style preparation from the EDT commit.
 */
public interface PreparedTokenRenderCodeEditorProvider extends TokenRenderCodeEditorProvider {

    Collection<StyledRange> prepare(
            Collection<Token> tokens,
            TokenColorProvider colorProvider,
            TokenRenderSnapshot snapshot);

    default void afterApply(CodeEditorTextArea textArea) {
    }

    @Override
    default void render(Collection<Token> tokens, TokenColorProvider colorProvider,
                        CodeEditorTextArea textArea) {
        Collection<StyledRange> ranges = prepare(
                tokens,
                colorProvider,
                new TokenRenderSnapshot(textArea.getBuffer().getText(), textArea.getDefaultStyle()));
        textArea.replaceStyledRanges(ranges == null ? List.of() : ranges);
        afterApply(textArea);
    }
}
