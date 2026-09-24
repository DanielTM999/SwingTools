package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.api.ProviderRegistration;
import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.provider.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.awt.event.ActionEvent;
import static org.junit.jupiter.api.Assertions.*;

class WordEditorTest {
    @TempDir Path temporary;
    static <T>T edt(Callable<T> callable)throws Exception{AtomicReference<T> result=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();SwingUtilities.invokeAndWait(()->{try{result.set(callable.call());}catch(Throwable e){failure.set(e);}});if(failure.get()!=null)throw new AssertionError(failure.get());return result.get();}
    @Test void providersContributeCommandsAndDetachIdempotently()throws Exception{
        edt(()->{try(WordEditor editor=new WordEditor()){
            AtomicInteger detached=new AtomicInteger();WordCommandProvider provider=new WordCommandProvider(){
                public String id(){return "test.extension";}
                public ProviderRegistration attach(WordEditor editor){return detached::incrementAndGet;}
                public Map<String,Action> commands(WordEditor editor){return Map.of("test.insert",new AbstractAction("Insert"){public void actionPerformed(ActionEvent e){editor.insertText("extension");}});}
            };
            ProviderRegistration registration=editor.addProvider(provider);assertThrows(IllegalArgumentException.class,()->editor.addProvider(provider));
            editor.getCommands().get("test.insert").actionPerformed(new ActionEvent(editor,0,"test"));assertEquals("extension",editor.getText());
            registration.close();registration.close();assertEquals(1,detached.get());assertFalse(editor.getCommands().containsKey("test.insert"));
            editor.setReadOnly(true);assertFalse(editor.getCommands().get("word.bold").isEnabled());assertThrows(IllegalStateException.class,()->editor.insertText("bad"));
        }return null;});
    }
    @Test void savesAndOpensOnBackgroundWorkers()throws Exception{
        WordEditor editor=edt(WordEditor::new);try{
            edt(()->{editor.insertText("Olá documento\nSegunda linha");return null;});
            Path path=temporary.resolve("document.docx");WordTask<Path> save=edt(()->editor.save(path));save.completion().toCompletableFuture().get(15,TimeUnit.SECONDS);
            assertTrue(Files.size(path)>0);assertFalse(edt(editor::isDirty));
            edt(()->{editor.setText("");return null;});
            edt(()->editor.open(path)).completion().toCompletableFuture().get(15,TimeUnit.SECONDS);
            assertEquals("Olá documento\nSegunda linha",edt(editor::getText));
            Files.writeString(path,"changed outside");
            assertThrows(ExecutionException.class,()->edt(()->editor.save(path)).completion().toCompletableFuture().get(15,TimeUnit.SECONDS));
            assertEquals("changed outside",Files.readString(path));
        }finally{edt(()->{editor.close();return null;});}
    }
    @Test void replacementIsOneUndoableOperation()throws Exception{
        edt(()->{try(WordEditor editor=new WordEditor()){editor.setText("um dois um");assertEquals(2,editor.replaceAll("um","três",false));assertEquals("três dois três",editor.getText());editor.getSession().undo();assertEquals("um dois um",editor.getText());}return null;});
    }
}
