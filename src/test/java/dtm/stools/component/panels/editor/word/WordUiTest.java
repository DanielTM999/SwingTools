package dtm.stools.component.panels.editor.word;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.panels.editor.word.provider.*;
import dtm.stools.component.panels.editor.word.ui.WordNavigationPanel;
import dtm.stools.component.panels.editor.word.ui.popup.*;
import dtm.stools.activity.DialogActivity;
import dtm.stools.context.WindowContext;
import dtm.stools.examples.WordEditorExample;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.*;
import static dtm.stools.component.panels.editor.word.WordEditorTest.edt;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class WordUiTest {
    @Test void navigationClosesAndReopensWithoutLosingSelection() throws Exception {
        edt(() -> {
            FlatLightLaf.setup();
            try(WordEditor editor=new WordEditor()) {
                editor.setDocument(WordEditorExample.demoDocument());editor.setNavigationVisible(true);
                editor.setSize(1360,900);layout(editor);
                WordNavigationPanel panel=(WordNavigationPanel)find(editor,"word.navigation.panel");
                JList<?> outline=components(panel).stream().filter(JList.class::isInstance).map(JList.class::cast).findFirst().orElseThrow();
                outline.setSelectedIndex(2);var selection=editor.getSession().getSelection();
                int width=editor.getScrollPane().getWidth();
                ((AbstractButton)find(panel,"word.panel.close")).doClick();layout(editor);
                assertFalse(panel.isVisible());assertFalse(editor.getConfig().navigationVisible());
                assertEquals(false,editor.getCommands().get("word.navigation").getValue(Action.SELECTED_KEY));
                assertTrue(editor.getScrollPane().getWidth()>width);
                editor.getCommands().get("word.navigation").actionPerformed(null);layout(editor);
                assertTrue(panel.isVisible());assertEquals(2,outline.getSelectedIndex());assertEquals(selection,editor.getSession().getSelection());
                editor.setCommentsVisible(true);
                ((AbstractButton)find(editor.getCommentsPanel(),"word.panel.close")).doClick();
                assertFalse(editor.getCommentsPanel().isVisible());
            }return null;
        });
    }

    @Test void ribbonTracksFormattingAndFitsEveryTabAtSmallWidths() throws Exception {
        edt(() -> {
            FlatLightLaf.setup();
            try(WordEditor editor=new WordEditor()) {
                editor.setText("Texto");editor.getSession().setSelection(0,5);
                editor.getCommands().get("word.bold").actionPerformed(null);
                assertEquals(true,editor.getCommands().get("word.bold").getValue(Action.SELECTED_KEY));
                editor.getSession().undo();assertEquals(false,editor.getCommands().get("word.bold").getValue(Action.SELECTED_KEY));
                editor.getCommands().get("word.bullets").actionPerformed(null);
                assertEquals(true,editor.getCommands().get("word.bullets").getValue(Action.SELECTED_KEY));
                assertEquals(false,editor.getCommands().get("word.numbering").getValue(Action.SELECTED_KEY));
                var tabs=editor.getDefaultRibbon().getTabs();
                for(int width:new int[]{480,800,1024,1360,1920})for(int tab=0;tab<tabs.getTabCount();tab++) {
                    editor.setSize(width,900);tabs.setSelectedIndex(tab);layout(editor);
                    Container page=(Container)tabs.getSelectedComponent();
                    assertFalse(page instanceof JScrollPane,"ribbon pages must not scroll horizontally");
                    for(Component group:page.getComponents())if(group.isVisible()) {
                        assertTrue(group.getX()>=0);assertTrue(group.getX()+group.getWidth()<=page.getWidth(),"group outside ribbon at "+width);
                    }
                }
                editor.setReadOnly(true);assertFalse(editor.getCommands().get("word.bold").isEnabled());
                assertFalse(editor.getDefaultRibbon().getSizeControl().isEnabled());
                assertTrue(editor.getCommands().get("word.navigation").isEnabled());
            }return null;
        });
    }

    @Test void genericDialogProviderCanBeRegisteredReplacedAndReset() throws Exception {
        edt(() -> {
            try(WordEditor editor=new WordEditor()) {
                WordDialogProvider defaults=editor.getDialogProvider();AtomicInteger calls=new AtomicInteger();
                WordDialogProvider custom=new WordDialogProvider() {
                    public String id(){return "test.dialog";}
                    public <T> Optional<T> show(WordDialogRequest<T> request){calls.incrementAndGet();return Optional.of(request.result().get());}
                };
                var registration=editor.addProvider(custom);
                assertEquals("initial",editor.ask("Title","Message","initial"));assertEquals(1,calls.get());
                registration.close();assertSame(defaults,editor.getDialogProvider());
                editor.setDialogProvider(custom);editor.resetPopupProviders();assertSame(defaults,editor.getDialogProvider());
            }return null;
        });
    }

    @Test void defaultModalFormValidatesCancelsAndDisposesExactlyOnce() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        edt(() -> {
            int initialWindows=WindowContext.size();
            try(WordEditor editor=new WordEditor()) {
                JTextField field=new JTextField("bad",20);AtomicInteger closed=new AtomicInteger();
                WordDialogActivity<String> dialog=new WordDialogActivity<>(new WordDialogRequest<>(editor,"test.form","Form","",field,field::getText,
                        value->{if(!value.equals("valid"))throw new IllegalArgumentException("Corrija o campo");},"Aplicar",false,true));
                dialog.onClosed(closed::incrementAndGet);
                AtomicReference<Throwable> failure=new AtomicReference<>();
                Timer timer=new Timer(100,event->{
                    ((Timer)event.getSource()).stop();
                    try {
                        button(dialog,"Aplicar").doClick();assertTrue(dialog.isShowing());
                        field.setText("valid");button(dialog,"Aplicar").doClick();
                    }catch(Throwable error){failure.set(error);dialog.dispose();}
                });timer.start();
                assertEquals(Optional.of("valid"),dialog.showResult());
                dialog.dispose();assertEquals(1,closed.get());if(failure.get()!=null)throw new AssertionError(failure.get());
                WordDialogActivity<String> cancel=new WordDialogActivity<>(new WordDialogRequest<>(editor,"test.cancel","Cancel","",new JPanel(),()->"unexpected",v->{},"Aplicar",false,false));
                Timer cancelTimer=new Timer(100,e->{((Timer)e.getSource()).stop();cancel.dispatchEvent(new WindowEvent(cancel,WindowEvent.WINDOW_CLOSING));});cancelTimer.start();
                assertTrue(cancel.showResult().isEmpty());
                assertEquals(initialWindows,WindowContext.size());
            }return null;
        });
    }

    @Test void searchAndPaletteBelongToTheirEditorAndCloseWithIt() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        edt(() -> {
            JFrame frameA=new JFrame(),frameB=new JFrame();WordEditor a=new WordEditor(),b=new WordEditor();
            frameA.add(a);frameB.add(b);
            try {
                a.showSearch();a.showSearch();b.showSearch();a.showCommandPalette();
                List<Window> aWindows=Arrays.stream(frameA.getOwnedWindows()).filter(Window::isDisplayable).toList();
                assertEquals(2,aWindows.size());assertTrue(aWindows.stream().allMatch(DialogActivity.class::isInstance));
                assertEquals(1,Arrays.stream(frameB.getOwnedWindows()).filter(Window::isDisplayable).count());
                a.close();assertTrue(aWindows.stream().noneMatch(Window::isDisplayable));assertTrue(b.getPopups().isSearchOpen());
            }finally{a.close();b.close();frameA.dispose();frameB.dispose();}return null;
        });
    }
    @Test void collapsedGroupReusesControlsAndRestoresThemAfterClosing() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        edt(()->{
            FlatLightLaf.setup();JFrame frame=new JFrame();WordEditor editor=new WordEditor();
            try {
                frame.add(editor);frame.setSize(com.formdev.flatlaf.util.UIScale.scale(new Dimension(800,600)));frame.setVisible(true);layout(frame);
                AbstractButton styles=components(editor.getDefaultRibbon()).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
                        .filter(b->"Estilos".equals(b.getToolTipText())&&b.isShowing()).findFirst().orElseThrow();
                styles.doClick();
                JComboBox<?> combo=editor.getDefaultRibbon().getStyleControl();
                Component popup=combo;
                while(popup!=null && !"word.ribbon.popup".equals(popup.getName()))popup=popup.getParent();
                assertNotNull(popup);assertTrue(popup.isShowing());
                combo.showPopup();assertTrue(combo.isPopupVisible());assertTrue(combo.isShowing());
                combo.setSelectedIndex(1);assertEquals("Title",editor.getDocument().paragraphAt(0).style().styleId());
                combo.hidePopup();editor.getDefaultRibbon().closePopups();frame.setSize(com.formdev.flatlaf.util.UIScale.scale(new Dimension(1920,700)));layout(frame);
                assertNull(SwingUtilities.getAncestorOfClass(JPopupMenu.class,combo));
                assertTrue(SwingUtilities.isDescendingFrom(combo,editor.getDefaultRibbon()));
            }finally{editor.close();frame.dispose();}return null;
        });
    }
    @Test void readOnlyDialogCannotSubmitItsValue() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        edt(()->{
            try(WordEditor editor=new WordEditor()){
                JTextField field=new JTextField("read only",20);AtomicInteger submitted=new AtomicInteger();
                WordDialogActivity<String> dialog=new WordDialogActivity<>(new WordDialogRequest<>(editor,"test.readonly","Read only","",field,
                        ()->{submitted.incrementAndGet();return field.getText();},v->{},"Aplicar",true,true));
                assertFalse(field.isEditable());
                Timer timer=new Timer(100,e->{((Timer)e.getSource()).stop();WordUiTest.button(dialog,"Fechar").doClick();});timer.start();
                try{assertTrue(dialog.showResult().isEmpty());assertEquals(0,submitted.get());}
                finally{timer.stop();dialog.dispose();}
            }return null;
        });
    }
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container nested)layout(nested);}
    static List<Component> components(Container c){List<Component> result=new ArrayList<>();for(Component child:c.getComponents()){result.add(child);if(child instanceof Container nested)result.addAll(components(nested));}return result;}
    static Component find(Container c,String name){return components(c).stream().filter(x->name.equals(x.getName())).findFirst().orElseThrow();}
    static AbstractButton button(Container c,String text){return components(c).stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).filter(x->text.equals(x.getText())).findFirst().orElseThrow();}
}
