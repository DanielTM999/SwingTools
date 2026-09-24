package dtm.stools.component.panels.editor.word;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.UIScale;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.ui.popup.*;
import dtm.stools.configs.UiTokens;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static dtm.stools.component.panels.editor.word.WordEditorTest.edt;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class WordDialogVisualTest {
    @Test void complexDialogsFitScreenAndHaveFixedActions() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        for(boolean dark:new boolean[]{false,true}) {
            edt(()->{
                if(dark)FlatDarkLaf.setup();else FlatLightLaf.setup();UiTokens.refresh();
                try(WordEditor editor=new WordEditor()) {
                    List<WordPropertiesPanel<?>> panels=List.of(
                            new WordPageSetupPanel(WordPageSettings.A4),
                            new WordChartEditorPanel(WordChart.sample(),editor.getDocument(),editor.getObjectRegistry()),
                            new WordEquationEditorPanel(WordEquation.parse("x = (-b ± √(b^2-4a c))/(2a)",true)),
                            new WordDiagramEditorPanel(WordDiagram.of(WordDiagramLayout.BASIC_PROCESS,List.of("Planejar","Executar","Verificar")),editor.getDocument(),editor.getObjectRegistry()));
                    for(WordPropertiesPanel<?> panel:panels) capture(editor,panel,dark);
                }return null;
            });
        }
        edt(()->{FlatLightLaf.setup();UiTokens.refresh();return null;});
    }
    private <T> void capture(WordEditor editor,WordPropertiesPanel<T> panel,boolean dark) throws Exception {
        WordPropertiesActivity<T> dialog=new WordPropertiesActivity<>(editor,panel,false);
        AtomicReference<Throwable> failure=new AtomicReference<>();
        Timer timer=new Timer(120,event->{
            ((Timer)event.getSource()).stop();
            try {
                AbstractButton apply=WordUiTest.button(dialog,"Aplicar");
                assertTrue(apply.isShowing());
                Point point=SwingUtilities.convertPoint(apply,0,0,dialog.getContentPane());
                assertTrue(point.y+apply.getHeight()<=dialog.getContentPane().getHeight());
                Rectangle screen=dialog.getGraphicsConfiguration().getBounds();
                assertTrue(dialog.getWidth()<=screen.width);assertTrue(dialog.getHeight()<=screen.height);
                JRootPane root=dialog.getRootPane();BufferedImage image=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();try{root.paint(g);}finally{g.dispose();}
                Files.createDirectories(Path.of("target"));
                ImageIO.write(image,"png",Path.of("target","word-dialog-"+panel.getClass().getSimpleName()+"-"+(dark?"dark":"light")+"-"+Math.round(UIScale.getUserScaleFactor()*100)+".png").toFile());
            }catch(Throwable error){failure.set(error);}finally{dialog.dispose();}
        });timer.start();
        try { assertTrue(dialog.showResult().isEmpty()); }
        finally { timer.stop();dialog.dispose(); }
        if(failure.get()!=null)throw new AssertionError(failure.get());
    }
}
