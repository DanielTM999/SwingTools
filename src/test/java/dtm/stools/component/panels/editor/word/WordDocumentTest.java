package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WordDocumentTest {
    @Test void replacesAcrossParagraphsWithoutLosingAdjacentFormattingOrIdentity() {
        WordDocument original=WordDocument.fromText("primeiro\nsegundo\nintacto");
        original=original.format(0,3,s->s.withBold(true));
        UUID untouched=original.paragraphs().get(2).id();
        WordDocument changed=original.replace(3,12,"A\nB",WordTextStyle.DEFAULT.withItalic(true));
        assertEquals("priA\nBundo\nintacto",changed.text());
        assertTrue(changed.paragraphs().getFirst().runs().getFirst().style().bold());
        assertTrue(changed.paragraphs().get(1).runs().getFirst().style().italic());
        assertEquals(untouched,changed.paragraphs().getLast().id());
        assertEquals("primeiro\nsegundo\nintacto",original.text());
    }
    @Test void respectsGraphemeClustersInsteadOfUtf16CodeUnits() {
        WordDocument doc=WordDocument.fromText("a👨‍👩‍👧‍👦e\u0301z");
        int familyEnd=doc.nextBoundary(1);
        assertEquals("👨‍👩‍👧‍👦",doc.text().substring(1,familyEnd));
        assertEquals(1,doc.previousBoundary(familyEnd));
        assertThrows(IllegalArgumentException.class,()->doc.replace(2,3,"",WordTextStyle.DEFAULT));
        assertEquals(familyEnd+2,doc.nextBoundary(familyEnd));
    }
    @Test void compoundTransformationIsAtomicAndUndoRestoresSavedIdentity() {
        WordSession session=new WordSession();session.load(WordDocument.fromText("abc"));
        session.setSelection(1,2);session.replaceSelection("XYZ");assertEquals("aXYZc",session.getDocument().text());assertTrue(session.isDirty());
        session.undo();assertEquals("abc",session.getDocument().text());assertFalse(session.isDirty());assertEquals(new WordSelection(1,2),session.getSelection());
        session.redo();WordDocument before=session.getDocument();long revision=session.getRevision();
        assertThrows(IllegalArgumentException.class,()->session.execute("Broken",d->{throw new IllegalArgumentException();},new WordSelection(0,0)));
        assertSame(before,session.getDocument());assertEquals(revision,session.getRevision());
    }
    @Test void completionOfOlderSaveDoesNotMarkNewerEditsClean() {
        WordSession session=new WordSession();session.replaceSelection("one");WordDocument saved=session.getDocument();
        session.replaceSelection(" two");session.markSaved(saved);assertTrue(session.isDirty());session.undo();assertFalse(session.isDirty());
    }
    @Test void readOnlyIsEnforcedAtTheCommandBoundary() {
        WordSession session=new WordSession();session.setReadOnly(true);
        assertThrows(IllegalStateException.class,()->session.replaceSelection("bad"));
        assertThrows(IllegalStateException.class,()->session.formatSelection(s->s.withBold(true)));
        assertThrows(IllegalStateException.class,session::undo);assertEquals("",session.getDocument().text());
    }
    @Test void immutableModelsDefensivelyCopyLists() {
        List<WordParagraph> source=new ArrayList<>();source.add(WordParagraph.of("x"));
        WordDocument doc=new WordDocument(source,WordPageSettings.A4);source.clear();assertEquals("x",doc.text());
        assertThrows(UnsupportedOperationException.class,()->doc.paragraphs().clear());
    }
}
