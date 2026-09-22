package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.prototype.Token;
import dtm.stools.component.panels.editor.code.prototype.constants.TokenType;
import dtm.stools.component.panels.editor.code.prototype.styles.StyledRange;
import dtm.stools.component.panels.editor.code.prototype.styles.TextStyle;
import dtm.stools.component.panels.editor.code.provider.PreparedTokenRenderCodeEditorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenClassifierCodeEditorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenColorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenRenderSnapshot;
import dtm.stools.component.panels.editor.code.provider.TokenizeChange;
import dtm.stools.component.panels.editor.code.provider.TokenizerCodeEditorProvider;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorHighlightTypingTest {

    @Test
    void hidesStaleStylesFromTheEditedLineWithoutRebuildingTheRangeList() throws Exception {
        TestEditor editor = new TestEditor();
        TextStyle firstLine = TextStyle.builder().foreground(Color.BLUE).build();
        TextStyle secondLine = TextStyle.builder().foreground(Color.GREEN).build();

        SwingUtilities.invokeAndWait(() -> {
            editor.setSyntaxHighlightEnabled(false);
            editor.setText("class A {\nint n;\n}");
            editor.replaceStyledRanges(List.of(
                    new StyledRange(firstLine, 0, 5),
                    new StyledRange(secondLine, 10, 13)));

            int rangeCount = editor.getStyledRanges().size();
            editor.type(editor.getBuffer().getText().indexOf('n'), "ew");

            assertSame(firstLine, editor.getStyleAt(0));
            assertSame(editor.getDefaultStyle(), editor.getStyleAt(10));
            assertEquals(rangeCount, editor.getStyledRanges().size());
        });
    }

    @Test
    void coalescesRapidTypingAndPreparesStylesOffTheEdt() throws Exception {
        TestEditor editor = new TestEditor();
        CountDownLatch initialApplied = new CountDownLatch(1);
        CountDownLatch editedApplied = new CountDownLatch(1);
        AtomicReference<TokenizeChange> incrementalChange = new AtomicReference<>();
        AtomicBoolean preparedOnEdt = new AtomicBoolean();
        AtomicBoolean appliedOutsideEdt = new AtomicBoolean();

        TokenizerCodeEditorProvider tokenizer = new TokenizerCodeEditorProvider() {
            @Override
            public Collection<Token> tokenize(String text,
                                              TokenClassifierCodeEditorProvider classifier) {
                return tokens(text);
            }

            @Override
            public boolean supportsIncremental() {
                return true;
            }

            @Override
            public Collection<Token> tokenize(TokenizeChange change,
                                              TokenClassifierCodeEditorProvider classifier) {
                incrementalChange.set(change);
                return tokens(change.newText());
            }
        };
        PreparedTokenRenderCodeEditorProvider renderer = new PreparedTokenRenderCodeEditorProvider() {
            @Override
            public Collection<StyledRange> prepare(Collection<Token> tokens,
                                                   TokenColorProvider colors,
                                                   TokenRenderSnapshot snapshot) {
                if (SwingUtilities.isEventDispatchThread()) {
                    preparedOnEdt.set(true);
                }
                return tokens.stream()
                        .map(token -> new StyledRange(
                                TextStyle.builder().foreground(Color.WHITE).build(),
                                token.getStartOffset(), token.getEndOffset()))
                        .toList();
            }

            @Override
            public void afterApply(CodeEditorTextArea textArea) {
                if (!SwingUtilities.isEventDispatchThread()) {
                    appliedOutsideEdt.set(true);
                }
                if ("abc".equals(textArea.getBuffer().getText())) {
                    initialApplied.countDown();
                } else if ("abcde".equals(textArea.getBuffer().getText())) {
                    editedApplied.countDown();
                }
            }
        };

        SwingUtilities.invokeAndWait(() -> {
            editor.setSyntaxHighlightDebounceMs(20);
            editor.addProvider(tokenizer);
            editor.addProvider((TokenClassifierCodeEditorProvider) token -> token);
            editor.addProvider((TokenColorProvider) type -> Color.WHITE);
            editor.addProvider(renderer);
            editor.setText("abc");
            editor.applySyntaxHighlight();
        });
        assertTrue(initialApplied.await(5, TimeUnit.SECONDS));

        SwingUtilities.invokeAndWait(() -> {
            editor.type(3, "d");
            editor.type(4, "e");
        });

        assertTrue(editedApplied.await(5, TimeUnit.SECONDS));
        TokenizeChange change = incrementalChange.get();
        assertEquals("abc", change.oldText());
        assertEquals("abcde", change.newText());
        assertEquals(3, change.changeOffset());
        assertEquals(0, change.removedLength());
        assertEquals("de", change.insertedText());
        assertTrue(!preparedOnEdt.get(), "a preparação dos estilos não deve rodar na EDT");
        assertTrue(!appliedOutsideEdt.get(), "a aplicação dos estilos deve rodar na EDT");
    }

    private static Collection<Token> tokens(String text) {
        return text == null || text.isEmpty()
                ? List.of()
                : List.of(new Token(0, text.length(), TokenType.IDENTIFIER, text));
    }

    private static final class TestEditor extends CodeEditorTextArea {
        void type(int offset, String text) {
            insertText(offset, text);
        }
    }
}
