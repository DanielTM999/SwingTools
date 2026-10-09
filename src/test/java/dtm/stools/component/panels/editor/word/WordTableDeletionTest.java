package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.api.WordCellSelection;
import dtm.stools.component.panels.editor.word.api.WordSession;
import dtm.stools.component.panels.editor.word.editing.WordTableEditing;
import dtm.stools.component.panels.editor.word.model.*;
import org.junit.jupiter.api.Test;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import static dtm.stools.component.panels.editor.word.WordEditorTest.edt;
import static org.junit.jupiter.api.Assertions.*;

class WordTableDeletionTest {
    @Test void deletingTextAndTableTogetherDoesNotReportErrors() throws Exception {
        edt(() -> {
            for (int key : new int[]{KeyEvent.VK_DELETE,KeyEvent.VK_BACK_SPACE}) {
                try (WordEditor editor = new WordEditor()) {
                    List<Throwable> errors = new ArrayList<>();
                    editor.setErrorHandler(errors::add);
                    WordTable table = WordTable.create(2,2,300);
                    WordDocument original = new WordDocument(List.of(WordParagraph.of("before"),table,WordParagraph.of("after")),WordPageSettings.A4);
                    editor.setDocument(original);
                    editor.getSession().setSelection(3,original.length()-2);
                    press(editor,key);
                    assertTrue(errors.isEmpty(),errors.toString());
                    assertEquals("befer",editor.getDocument().text());
                    assertTrue(editor.getDocument().findTable(table.id()).isEmpty());
                    editor.getSession().undo();
                    assertEquals(original,editor.getDocument());
                }
            }
            return null;
        });
    }

    @Test void selectionAcrossTableEdgeTerminates() {
        for (boolean reverse : new boolean[]{false,true}) for (boolean fromTable : new boolean[]{false,true}) {
            WordTable table = WordTable.create(2,2,300);
            WordDocument document = new WordDocument(List.of(WordParagraph.of("before"),table,WordParagraph.of("after")),WordPageSettings.A4);
            WordSession session = new WordSession();
            session.load(document);
            int start = fromTable ? document.cellRange(table.id(),0,1)[0] : 3;
            int end = fromTable ? document.length()-2 : document.cellRange(table.id(),1,0)[0];
            session.setSelection(reverse ? end : start,reverse ? start : end);
            var selection = session.getContentSelection();
            session.deleteSelection(!reverse);
            assertTrue(session.getDocument().findTable(table.id()).isEmpty());
            assertEquals(fromTable ? "before\ner" : "bef\nafter",session.getDocument().text());
            session.undo();
            assertEquals(document,session.getDocument());
            assertEquals(selection,session.getContentSelection());
            session.redo();
            assertTrue(session.getDocument().findTable(table.id()).isEmpty());
        }
    }

    @Test void deletingAcrossNestedTableEdgePreservesOuterTableAndOtherCells() {
        WordTable nested = WordTable.create(2,2,120);
        WordTable outer = WordTable.create(1,2,300);
        outer = outer.withRow(0,outer.rows().getFirst().withCell(0,outer.cell(0,0).withBlocks(
                List.of(WordParagraph.of("before"),nested,WordParagraph.of("after")))));
        WordDocument document = new WordDocument(List.of(outer),WordPageSettings.A4);
        WordSession session = new WordSession();
        session.load(document);
        session.setSelection(document.cellRange(outer.id(),0,0)[0]+3,document.cellRange(nested.id(),1,0)[0]);
        session.deleteSelection(true);
        assertTrue(session.getDocument().findTable(nested.id()).isEmpty());
        WordTable preserved = session.getDocument().findTable(outer.id()).orElseThrow();
        assertEquals("bef\nafter",preserved.cell(0,0).plainText());
        assertEquals(outer.cell(0,1),preserved.cell(0,1));
        session.undo();
        assertEquals(document,session.getDocument());
    }

    @Test void deletingEmptySingleCellKeepsCellSelection() throws Exception {
        edt(() -> {
            for (int key : new int[]{KeyEvent.VK_DELETE,KeyEvent.VK_BACK_SPACE}) {
                try (WordEditor editor = new WordEditor()) {
                    List<Throwable> errors = new ArrayList<>();
                    editor.setErrorHandler(errors::add);
                    WordTable table = WordTable.create(1,1,300);
                    editor.setDocument(new WordDocument(List.of(table,WordParagraph.of("after")),WordPageSettings.A4));
                    editor.getSession().selectCells(table.id(),0,0,0,0);
                    press(editor,key);
                    assertInstanceOf(WordCellSelection.class,editor.getSession().getContentSelection());
                    assertEquals("after",editor.getDocument().paragraphs().getLast().text());
                    assertTrue(errors.isEmpty(),errors.toString());
                }
            }
            return null;
        });
    }

    @Test void deleteAndBackspaceClearSelectedCellsWithoutErrorsAndCanBeUndone() throws Exception {
        edt(() -> {
            for (int key : new int[]{KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE}) {
                try (WordEditor editor = new WordEditor()) {
                    List<Throwable> errors = new ArrayList<>();
                    editor.setErrorHandler(errors::add);
                    WordTable table = WordTableEditing.fill(WordTable.create(2,2,300),0,0,
                            List.of(List.of("one","two"),List.of("three","four")),WordTextStyle.DEFAULT);
                    WordDocument original = new WordDocument(List.of(WordParagraph.of("before"),table,WordParagraph.of("after")),WordPageSettings.A4);
                    editor.setDocument(original);
                    editor.getSession().selectCells(table.id(),0,0,1,1);
                    var selection = editor.getSession().getContentSelection();
                    press(editor,key);
                    assertTrue(errors.isEmpty(),errors.toString());
                    WordTable cleared = editor.getDocument().findTable(table.id()).orElseThrow();
                    for (var row : cleared.rows()) for (var cell : row.cells()) assertEquals("",cell.plainText());
                    assertInstanceOf(WordCellSelection.class,editor.getSession().getContentSelection());
                    editor.getSession().undo();
                    assertEquals(original,editor.getDocument());
                    assertEquals(selection,editor.getSession().getContentSelection());
                    editor.getSession().redo();
                    press(editor,key);
                    assertTrue(errors.isEmpty(),errors.toString());
                }
            }
            return null;
        });
    }

    private static void press(WordEditor editor,int key) {
        KeyEvent event = new KeyEvent(editor.getCanvas(),KeyEvent.KEY_PRESSED,System.currentTimeMillis(),0,key,KeyEvent.CHAR_UNDEFINED);
        for (var listener : editor.getCanvas().getKeyListeners()) listener.keyPressed(event);
    }
}
