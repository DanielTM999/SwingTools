package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.model.WordTable;
import dtm.stools.component.panels.editor.word.ui.WordTableGridChooser;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;
import static dtm.stools.component.panels.editor.word.WordEditorTest.edt;
import static org.junit.jupiter.api.Assertions.*;

class WordTableGridChooserTest {
    @Test void repeatedPopupCreationHasStableAccessibleContextAndInsertsChosenDimensions() throws Exception {
        edt(()->{
            for(int[] size:new int[][]{{1,1},{3,5},{8,10}})try(WordEditor editor=new WordEditor()) {
                AtomicInteger calls=new AtomicInteger();
                JPopupMenu popup=WordTableGridChooser.popup((rows,columns)->{calls.incrementAndGet();editor.getObjects().insertTable(rows,columns);});
                WordTableGridChooser chooser=(WordTableGridChooser)popup.getComponent(0);
                assertNotNull(chooser.getAccessibleContext());
                assertSame(chooser.getAccessibleContext(),chooser.getAccessibleContext());
                assertEquals("Escolher tamanho da tabela",chooser.getAccessibleContext().getAccessibleName());
                int x=4+(size[1]-1)*18+5,y=4+(size[0]-1)*18+5;
                chooser.dispatchEvent(new MouseEvent(chooser,MouseEvent.MOUSE_MOVED,0,0,x,y,0,false));
                chooser.dispatchEvent(new MouseEvent(chooser,MouseEvent.MOUSE_CLICKED,0,0,x,y,1,false,MouseEvent.BUTTON1));
                assertEquals(1,calls.get());assertFalse(popup.isVisible());
                WordTable table=editor.getDocument().blocks().stream().filter(WordTable.class::isInstance).map(WordTable.class::cast).findFirst().orElseThrow();
                assertEquals(size[0],table.rows().size());assertEquals(size[1],table.gridColumns());
            }return null;
        });
    }
}
