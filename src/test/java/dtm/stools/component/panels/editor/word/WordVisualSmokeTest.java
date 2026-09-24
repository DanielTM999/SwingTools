package dtm.stools.component.panels.editor.word;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.util.UIScale;
import javax.swing.SwingUtilities;
import dtm.stools.examples.WordEditorExample;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import static dtm.stools.component.panels.editor.word.WordEditorTest.edt;
import static org.junit.jupiter.api.Assertions.*;

class WordVisualSmokeTest {
    @Test void rendersEditorAndPhysicalPageWithoutAWindow()throws Exception{
        CountDownLatch ready=new CountDownLatch(1);
        WordEditor editor=edt(()->{
            FlatLightLaf.setup();WordEditor e=new WordEditor();
            e.setDocument(WordEditorExample.demoDocument());
            e.setNavigationVisible(true);e.setSize(1300,900);layout(e);
            e.getCanvas().addPropertyChangeListener("layoutSnapshot",event->ready.countDown());e.getCanvas().scheduleLayout();return e;
        });
        try{
            assertTrue(ready.await(20,TimeUnit.SECONDS));
            BufferedImage image=edt(()->{layout(editor);BufferedImage result=new BufferedImage(1300,900,BufferedImage.TYPE_INT_RGB);Graphics2D g=result.createGraphics();try{editor.paint(g);}finally{g.dispose();}return result;});
            Path output=Path.of("target","word-editor-preview.png");Files.createDirectories(output.getParent());ImageIO.write(image,"png",output.toFile());
            String scale=edt(()->Math.round(UIScale.getUserScaleFactor()*100)+"");
            for(boolean dark:new boolean[]{false,true}) {
                edt(()->{if(dark)FlatDarkLaf.setup();else FlatLightLaf.setup();
                    SwingUtilities.updateComponentTreeUI(editor);editor.onThemeChanged();return null;});
                for(int width:new int[]{800,1024,1360,1920}) {
                    edt(()->{
                        editor.setSize(width,900);layout(editor);
                        editor.getScrollPane().getViewport().setViewPosition(new Point(0,0));
                        BufferedImage capture=new BufferedImage(width,900,BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics=capture.createGraphics();try{editor.paint(graphics);}finally{graphics.dispose();}
                        ImageIO.write(capture,"png",Path.of("target","word-ui-"+(dark?"dark":"light")+"-"+width+"-"+scale+".png").toFile());return null;
                    });
                }
            }
            edt(()->{FlatLightLaf.setup();SwingUtilities.updateComponentTreeUI(editor);editor.onThemeChanged();return null;});
            int pages=edt(()->editor.getCanvas().getLayoutSnapshot().pages().size());
            assertTrue(pages>=2,"demo document should span pages");
            for(int i=0;i<pages;i++){
                BufferedImage page=editor.renderPage(edt(editor::getDocument),i,96);assertTrue(page.getWidth()>700);assertTrue(page.getHeight()>1000);
                ImageIO.write(page,"png",Path.of("target","word-page-preview-"+(i+1)+".png").toFile());
            }
        }finally{edt(()->{editor.close();return null;});}
    }
    private static void layout(Container container){container.doLayout();for(Component child:container.getComponents())if(child instanceof Container c)layout(c);}
}
