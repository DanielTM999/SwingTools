package dtm.stools.component.panels.editor.word;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.panels.editor.word.api.WordViewMode;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.examples.WordEditorExample;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static dtm.stools.component.panels.editor.word.WordEditorTest.edt;
import static dtm.stools.component.panels.editor.word.WordUiTest.*;
import static org.junit.jupiter.api.Assertions.*;

class WordNavigationTest {
    @Test void outlineTracksCaretAndEditsWithoutChangingSelection() throws Exception {
        edt(()->{
            try(WordEditor editor=new WordEditor()) {
                WordParagraphStyle heading=WordParagraphStyle.DEFAULT.withHeadingLevel(1);
                editor.setDocument(new WordDocument(List.of(WordParagraph.of("Intro"),
                        WordParagraph.of("First").withStyle(heading),WordParagraph.of("Body"),
                        WordParagraph.of("Second").withStyle(heading),WordParagraph.of("End")),WordPageSettings.A4));
                JList<?> outline=outline(editor);
                assertEquals(-1,outline.getSelectedIndex());
                int body=editor.getDocument().paragraphStart(2)+2;
                editor.getSession().setSelection(body-1,body);
                assertEquals(0,outline.getSelectedIndex());
                assertEquals(body-1,editor.getSession().getSelection().anchor());
                assertEquals(body,editor.getSession().getSelection().caret());
                editor.insertText("updated");
                int caret=editor.getSession().getSelection().caret();
                assertEquals(body-1+7,caret);assertEquals(0,outline.getSelectedIndex());
                editor.getSession().undo();assertEquals(body,editor.getSession().getSelection().caret());
                assertEquals(0,outline.getSelectedIndex());
                editor.getSession().redo();assertEquals(caret,editor.getSession().getSelection().caret());
                assertEquals(0,outline.getSelectedIndex());
                int second=editor.getDocument().paragraphStart(3);
                editor.getSession().setSelection(second,second+6);editor.insertText("");
                assertEquals(1,outline.getModel().getSize());assertEquals(0,outline.getSelectedIndex());
                editor.getSession().undo();assertEquals(2,outline.getModel().getSize());assertEquals(1,outline.getSelectedIndex());
                editor.getSession().setSelection(0,0);assertEquals(-1,outline.getSelectedIndex());
                editor.setText("No headings");assertEquals(0,outline.getModel().getSize());assertEquals(-1,outline.getSelectedIndex());
            }return null;
        });
    }

    @Test void navigationAlignsTitlesAcrossPagesZoomAndContinuousLayout() throws Exception {
        WordEditor editor=createEditor();
        try {
            for(WordViewMode mode:WordViewMode.values())for(double zoom:new double[]{.75,1,1.5}) {
                edt(()->{editor.setZoom(zoom);editor.setViewMode(mode);layout(editor);return null;});
                for(int index:new int[]{2,5,0}) {
                    int offset=edt(()->{
                        editor.getScrollPane().getViewport().setViewPosition(new Point(0,0));
                        click(outline(editor),index);return editor.getSession().getSelection().caret();
                    });
                    await(()->aligned(editor,offset));
                    edt(()->{assertEquals(index,outline(editor).getSelectedIndex());return null;});
                }
            }
        } finally {edt(()->{editor.close();return null;});}
    }

    @Test void repeatedClickAndEnterRepositionSelectedHeading() throws Exception {
        WordEditor editor=createEditor();
        try {
            int offset=edt(()->{click(outline(editor),2);return editor.getSession().getSelection().caret();});
            await(()->aligned(editor,offset));
            edt(()->{editor.getScrollPane().getViewport().setViewPosition(new Point(0,0));click(outline(editor),2);return null;});
            await(()->aligned(editor,offset));
            edt(()->{
                editor.getScrollPane().getViewport().setViewPosition(new Point(0,0));
                JList<?> list=outline(editor);
                Object binding=list.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0));
                list.getActionMap().get(binding).actionPerformed(new ActionEvent(list,ActionEvent.ACTION_PERFORMED,""));
                return null;
            });
            await(()->aligned(editor,offset));
            // Capture the same navigation destination shown in the bug report for visual review.
            edt(()->{
                java.awt.image.BufferedImage capture=new java.awt.image.BufferedImage(editor.getWidth(),editor.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                Graphics2D g=capture.createGraphics();try{editor.paint(g);}finally{g.dispose();}
                javax.imageio.ImageIO.write(capture,"png",java.nio.file.Path.of("target","word-navigation-table.png").toFile());return null;
            });
        } finally {edt(()->{editor.close();return null;});}
    }

    @Test void pendingNavigationUsesLatestDestinationAndCancelsOnSelectionOrDocumentChange() throws Exception {
        WordEditor editor=createEditor();
        try {
            int offset=edt(()->{
                editor.getCanvas().scheduleLayout();click(outline(editor),5);click(outline(editor),2);
                return editor.getSession().getSelection().caret();
            });
            await(()->aligned(editor,offset));
            edt(()->{
                editor.getCanvas().scheduleLayout();click(outline(editor),5);editor.getSession().setSelection(0,0);return null;
            });
            await(()->editor.getCanvas().isLayoutCurrent()&&editor.getScrollPane().getViewport().getViewRect().contains(editor.getCanvas().caretBounds()));
            edt(()->{assertEquals(0,editor.getSession().getSelection().caret());click(outline(editor),5);editor.setText("Replacement");return null;});
            await(()->editor.getCanvas().isLayoutCurrent()&&editor.getScrollPane().getViewport().getViewRect().contains(editor.getCanvas().caretBounds()));
            edt(()->{assertEquals(0,outline(editor).getModel().getSize());return null;});
        } finally {edt(()->{editor.close();return null;});}
    }

    @Test void endOfDocumentClampsNavigationToScrollRange() throws Exception {
        WordEditor editor=createEditor();
        try {
            edt(()->{editor.setViewMode(WordViewMode.CONTINUOUS);return null;});
            int end=edt(()->{
                int offset=editor.getDocument().length();editor.getSession().setSelection(offset,offset);
                editor.getCanvas().revealOffsetAtTop(offset);return offset;
            });
            await(()->aligned(editor,end));
            edt(()->{JViewport viewport=editor.getScrollPane().getViewport();assertEquals(editor.getCanvas().getHeight()-viewport.getHeight(),viewport.getViewPosition().y);return null;});
        } finally {edt(()->{editor.close();return null;});}
    }

    private static WordEditor createEditor() throws Exception {
        return edt(()->{FlatLightLaf.setup();WordEditor editor=new WordEditor();editor.setDocument(WordEditorExample.demoDocument());
            editor.setNavigationVisible(true);editor.setSize(1350,850);layout(editor);return editor;});
    }
    private static JList<?> outline(WordEditor editor) {
        return components((Container)find(editor,"word.navigation.panel")).stream().filter(JList.class::isInstance).map(JList.class::cast).findFirst().orElseThrow();
    }
    private static void click(JList<?> list,int index) {
        list.setSelectedIndex(index);
        Rectangle bounds=list.getCellBounds(index,index);
        MouseEvent event=new MouseEvent(list,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,bounds.x+8,bounds.y+bounds.height/2,1,false,MouseEvent.BUTTON1);
        for(MouseListener listener:list.getMouseListeners())listener.mouseClicked(event);
    }
    private static boolean aligned(WordEditor editor,int offset) {
        if(!editor.getCanvas().isLayoutCurrent())return false;
        JViewport viewport=editor.getScrollPane().getViewport();
        int expected=Math.max(0,Math.min(editor.getCanvas().boundsAt(offset).y-16,editor.getCanvas().getHeight()-viewport.getHeight()));
        return viewport.getViewPosition().y==expected;
    }
    private static void await(BooleanSupplier condition) throws Exception {
        CompletableFuture<Void> ready=new CompletableFuture<>();
        Timer timer=edt(()->{Timer t=new Timer(20,e->{try{if(condition.getAsBoolean())ready.complete(null);}catch(Throwable failure){ready.completeExceptionally(failure);}});t.start();return t;});
        try{ready.get(20,TimeUnit.SECONDS);}finally{edt(()->{timer.stop();return null;});}
    }
}
