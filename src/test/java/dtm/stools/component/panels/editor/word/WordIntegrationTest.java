package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.editing.WordTemplates;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.provider.WordAiProvider;
import dtm.stools.component.panels.editor.word.ui.WordTransferable;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static dtm.stools.component.panels.editor.word.WordEditorTest.edt;

class WordIntegrationTest {
    @Test void richFragmentRetainsStylesAndUsesNewParagraphIds()throws Exception{
        WordDocument source=WordDocument.fromText("one\ntwo").format(0,3,s->s.withBold(true));
        WordTransferable clipboard=new WordTransferable(source.fragment(0,source.length()));
        WordDocument fragment=(WordDocument)clipboard.getTransferData(WordTransferable.DOCUMENT);
        WordDocument target=source.replace(source.length(),source.length(),fragment);
        assertEquals("one\ntwoone\ntwo",target.text());assertTrue(target.paragraphs().get(1).runs().getLast().style().bold());
        assertEquals(target.paragraphs().size(),target.paragraphs().stream().map(WordParagraph::id).distinct().count());
        assertTrue(((String)clipboard.getTransferData(WordTransferable.HTML)).contains("font-weight:bold"));
    }
    @Test void templateVariablesAreLiteralAndMissingValuesFailBeforeMutation(){
        WordDocument template=WordDocument.fromText("Olá ${name}, total ${amount}");
        assertEquals(Set.of("name","amount"),WordTemplates.variables(template));
        assertThrows(IllegalArgumentException.class,()->WordTemplates.fill(template,Map.of("name","Ana")));
        assertEquals("Olá <Ana>, total $10",WordTemplates.fill(template,Map.of("name","<Ana>","amount","$10")).text());
    }
    @Test void aiReceivesOnlyExplicitScopeAndNeverAppliesAutomatically()throws Exception{
        WordEditor editor=edt(WordEditor::new);AtomicReference<String> sent=new AtomicReference<>();
        try{
            WordTask<WordSuggestion> task=edt(()->{
                editor.setText("private selected secret");editor.getSession().setSelection(8,16);
                editor.addProvider(new WordAiProvider(){public String id(){return "test.ai";}public CompletionStage<String> suggest(Request request){sent.set(request.text());return CompletableFuture.completedFuture("revised");}});
                return editor.requestSuggestion("test.ai","Rewrite",false);
            });
            WordSuggestion result=task.completion().toCompletableFuture().get(10,TimeUnit.SECONDS);
            assertEquals("selected",sent.get());assertEquals("private selected secret",edt(editor::getText));
            edt(()->{editor.applySuggestion(result);return null;});assertEquals("private revised secret",edt(editor::getText));
            edt(()->{assertThrows(IllegalStateException.class,()->editor.applySuggestion(result));editor.getSession().undo();return null;});
            assertEquals("private selected secret",edt(editor::getText));
        }finally{edt(()->{editor.close();return null;});}
    }
    @Test void taskCannotReportCancellationAfterIrreversiblePublication(){
        WordTask<String> task=new WordTask<>();assertTrue(task.beginCommit());assertFalse(task.cancel());task.complete("saved");
        assertEquals("saved",task.completion().toCompletableFuture().join());
        WordTask<String> cancelled=new WordTask<>();assertTrue(cancelled.cancel());assertFalse(cancelled.beginCommit());
    }
}
