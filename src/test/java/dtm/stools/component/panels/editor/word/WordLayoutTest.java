package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.layout.*;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.render.WordRenderer;
import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;

class WordLayoutTest {
    @Test void paginatesWithoutLosingCharactersOrExceedingMargins(){
        WordDocument doc=WordDocument.fromText(("Este parágrafo contém texto para verificar paginação. ".repeat(8)+"\n").repeat(80));
        WordLayout layout=new WordLayoutEngine().layout(doc);assertTrue(layout.pages().size()>5);
        int characters=0;for(var page:layout.pages())for(var line:page.lines()){
            characters+=line.end()-line.start();assertTrue(line.top()>=doc.pageSettings().top()-.1);
            assertTrue(line.bottom()<=page.height()-doc.pageSettings().bottom()+.1);
        }
        assertEquals(doc.text().replace("\n","").length(),characters);
    }
    @Test void rendererDoesNotMutateTheLayoutAtDifferentScales(){
        var layout=new WordLayoutEngine().layout(WordDocument.fromText("Layout estável\nTexto com acentos."));var page=layout.pages().getFirst();
        float advance=page.lines().getFirst().text().getAdvance();
        for(double scale:new double[]{1,1.25,1.5,2}){var image=new BufferedImage(1300,1800,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{g.scale(scale,scale);new WordRenderer().paintPage(g,page,null,null);}finally{g.dispose();}}
        assertEquals(advance,page.lines().getFirst().text().getAdvance());assertEquals(1,layout.pages().size());
    }
}
