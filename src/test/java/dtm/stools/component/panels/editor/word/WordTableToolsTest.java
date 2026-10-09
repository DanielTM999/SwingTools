package dtm.stools.component.panels.editor.word;

import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.controller.WordObjectController;
import dtm.stools.component.panels.editor.word.editing.WordTableEditing;
import dtm.stools.component.panels.editor.word.io.DocxCodec;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import dtm.stools.component.panels.editor.word.ui.popup.WordTableBandingPanel;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;
import java.util.concurrent.*;
import static dtm.stools.component.panels.editor.word.WordEditorTest.edt;
import static org.junit.jupiter.api.Assertions.*;

class WordTableToolsTest {
    @Test void singleCellSelectionAndTableRemovalAreAvailableInContextMenu() throws Exception {
        edt(()->{
            try(WordEditor editor=new WordEditor()) {
                WordTable table=WordTable.create(2,2,300);
                WordDocument original=new WordDocument(List.of(WordParagraph.of("before"),table,WordParagraph.of("after")),WordPageSettings.A4);
                editor.setDocument(original);
                int offset=original.cellRange(table.id(),0,1)[0];editor.getSession().setSelection(offset,offset);
                JPopupMenu menu=editor.createContextMenu();
                item(menu,"Selecionar célula").doClick();
                WordCellSelection selected=editor.getSession().getCellSelection().orElseThrow();
                assertEquals(0,selected.firstRow());assertEquals(0,selected.lastRow());
                assertEquals(1,selected.firstColumn());assertEquals(1,selected.lastColumn());
                item(menu,"Excluir tabela").doClick();
                assertTrue(editor.getDocument().findTable(table.id()).isEmpty());
                assertEquals("before\nafter",editor.getDocument().text());
                editor.getSession().undo();assertEquals(original,editor.getDocument());
                editor.setReadOnly(true);menu=editor.createContextMenu();
                assertTrue(item(menu,"Selecionar célula").isEnabled());
                assertFalse(item(menu,"Excluir tabela").isEnabled());
                assertFalse(item(menu,"Cores alternadas…").isEnabled());
            }
            return null;
        });
    }

    @Test void rightClickAndControlClickTargetTheClickedCellIncludingEmptyPadding() throws Exception {
        CountDownLatch ready=new CountDownLatch(1);
        WordTable first=WordTable.create(1,1,300),second=WordTable.create(2,2,300);
        WordEditor editor=edt(()->{
            WordEditor e=new WordEditor();
            e.setDocument(new WordDocument(List.of(first,WordParagraph.of("between"),second,WordParagraph.of("after")),WordPageSettings.A4));
            e.getCanvas().setSize(900,1200);
            e.getCanvas().addPropertyChangeListener("layoutSnapshot",event->ready.countDown());
            e.getCanvas().scheduleLayout();return e;
        });
        try {
            assertTrue(ready.await(20,TimeUnit.SECONDS));
            edt(()->{
                editor.getSession().selectCells(first.id(),0,0,0,0);
                var canvas=editor.getCanvas();var page=canvas.getLayoutSnapshot().pages().getFirst();
                var box=page.cells().stream().filter(c->c.tableId().equals(second.id())&&c.row()==1&&c.gridColumn()==1).findFirst().orElseThrow();
                var screen=canvas.toScreen(page,box.bounds().x+box.bounds().width-8,box.bounds().y+box.bounds().height/2);
                Point point=new Point((int)screen.getX(),(int)screen.getY());
                assertEquals(second.id(),canvas.tableCellAt(point).orElseThrow().table().id());
                assertTrue(canvas.selectForContextMenu(point));
                assertEquals(second.id(),editor.getObjects().currentTable().orElseThrow().table().id());
                item(editor.createContextMenu(),"Selecionar célula").doClick();
                WordCellSelection selected=editor.getSession().getCellSelection().orElseThrow();
                assertEquals(1,selected.firstRow());assertEquals(1,selected.lastRow());
                assertEquals(1,selected.firstColumn());assertEquals(1,selected.lastColumn());
                assertTrue(canvas.selectForContextMenu(point));assertEquals(selected,editor.getSession().getContentSelection());
                editor.getSession().setSelection(0,0);
                MouseEvent click=new MouseEvent(canvas,MouseEvent.MOUSE_PRESSED,System.currentTimeMillis(),InputEvent.CTRL_DOWN_MASK,
                        point.x,point.y,1,false,MouseEvent.BUTTON1);
                for(var listener:canvas.getMouseListeners())listener.mousePressed(click);
                assertEquals(selected,editor.getSession().getContentSelection());
                item(editor.createContextMenu(),"Excluir tabela").doClick();
                assertTrue(editor.getDocument().findTable(second.id()).isEmpty());
                assertTrue(editor.getDocument().findTable(first.id()).isPresent());
                return null;
            });
        } finally {edt(()->{editor.close();return null;});}
    }

    @Test void alternatingRowsPreserveHeadersContentAndUndoAndSurviveDocx() throws Exception {
        WordTable table=WordTableEditing.fill(WordTable.create(5,2,300),0,0,
                List.of(List.of("Name","Value"),List.of("a","1"),List.of("b","2"),List.of("c","3"),List.of("d","4")),WordTextStyle.DEFAULT);
        table=table.withRow(0,table.rows().getFirst().withHeader(true).withCells(table.rows().getFirst().cells().stream().map(c->c.withFill(0x203864)).toList()));
        WordDocument original=new WordDocument(List.of(table),WordPageSettings.A4);WordSession session=new WordSession();session.load(original);
        session.selectCells(table.id(),2,1,2,1);var selection=session.getContentSelection();
        WordObjectController controller=new WordObjectController(session,WordObjectRegistry::defaults);
        controller.alternateRows(new WordTableBanding(0xDEEAF6,0xFFFFFF,2,true));
        WordTable changed=session.getDocument().findTable(table.id()).orElseThrow();
        assertEquals(table.plainText(),changed.plainText());assertEquals(table.rows().getFirst(),changed.rows().getFirst());
        assertEquals(0xDEEAF6,changed.cell(1,0).fill());assertEquals(0xDEEAF6,changed.cell(2,1).fill());
        assertEquals(0xFFFFFF,changed.cell(3,0).fill());assertEquals(0xFFFFFF,changed.cell(4,1).fill());
        assertEquals(selection,session.getContentSelection());
        DocxCodec codec=new DocxCodec();ByteArrayOutputStream out=new ByteArrayOutputStream();codec.write(session.getDocument(),null,out);
        WordTable reopened=codec.read(new ByteArrayInputStream(out.toByteArray())).document().blocks().stream().filter(WordTable.class::isInstance).map(WordTable.class::cast).findFirst().orElseThrow();
        for(int r=0;r<5;r++)for(int c=0;c<2;c++)assertEquals(changed.cell(r,c).fill(),reopened.cell(r,c).fill());
        session.undo();assertEquals(original,session.getDocument());session.redo();assertEquals(changed,session.getDocument().findTable(table.id()).orElseThrow());
    }

    @Test void bandingDialogSupportsCustomColorsAndBandSize() throws Exception {
        edt(()->{
            WordTableBandingPanel panel=new WordTableBandingPanel(WordTable.create(3,2,300));
            var first=(dtm.stools.component.panels.editor.word.ui.popup.WordPropertiesPanel.ColorButton)WordUiTest.find(panel,"word.table.banding.first");
            first.setColor(0x123456);
            ((JSpinner)WordUiTest.find(panel,"word.table.banding.size")).setValue(3.0);
            assertEquals(new WordTableBanding(0x123456,0xFFFFFF,3,true),panel.result());
            first.clear();assertNull(panel.result().firstColor());
            return null;
        });
    }

    @Test void rowIntervalLeavesOtherRowsUntouchedAndRestartsAlternation() {
        WordTable table=WordTable.create(6,2,300);
        table=WordTableEditing.updateCells(table,0,0,5,1,c->c.withFill(0xABCDEF));
        WordSession session=new WordSession();WordDocument original=new WordDocument(List.of(table),WordPageSettings.A4);
        session.load(original);session.setSelection(0,0);
        new WordObjectController(session,WordObjectRegistry::defaults).alternateRows(new WordTableBanding(0x123456,0xFFFFFF,1,false,1,3));
        WordTable changed=session.getDocument().findTable(table.id()).orElseThrow();
        for(int r:new int[]{0,4,5})assertEquals(table.rows().get(r),changed.rows().get(r));
        assertEquals(0x123456,changed.cell(1,0).fill());assertEquals(0xFFFFFF,changed.cell(2,1).fill());assertEquals(0x123456,changed.cell(3,0).fill());
        session.undo();assertEquals(original,session.getDocument());
        WordTable single=WordTableEditing.alternateRows(table,new WordTableBanding(null,0xFFFFFF,1,false,2,2));
        assertNull(single.cell(2,0).fill());assertEquals(table.rows().get(3),single.rows().get(3));
    }

    @Test void dialogCanSwitchBetweenWholeTableAndValidatedRowInterval() throws Exception {
        edt(()->{
            WordTableBandingPanel panel=new WordTableBandingPanel(WordTable.create(10,2,300));
            JComboBox<?> scope=(JComboBox<?>)WordUiTest.find(panel,"word.table.banding.scope");
            JSpinner start=(JSpinner)WordUiTest.find(panel,"word.table.banding.start"),end=(JSpinner)WordUiTest.find(panel,"word.table.banding.end");
            assertFalse(start.isEnabled());assertFalse(end.isEnabled());
            scope.setSelectedIndex(1);assertTrue(start.isEnabled());assertTrue(end.isEnabled());
            start.setValue(2.0);end.setValue(4.0);
            assertEquals(1,panel.result().firstRow());assertEquals(3,panel.result().lastRow());
            JLabel before=(JLabel)WordUiTest.find(panel,"word.table.banding.preview.0"),after=(JLabel)WordUiTest.find(panel,"word.table.banding.preview.4");
            assertEquals(Color.WHITE,before.getBackground());assertEquals(Color.WHITE,after.getBackground());
            assertEquals(new Color(0xDEEAF6),((JLabel)WordUiTest.find(panel,"word.table.banding.preview.1")).getBackground());
            start.setValue(5.0);assertThrows(IllegalArgumentException.class,panel::result);
            scope.setSelectedIndex(0);assertEquals(0,panel.result().firstRow());assertEquals(Integer.MAX_VALUE,panel.result().lastRow());
            assertFalse(start.isEnabled());assertFalse(end.isEnabled());
            return null;
        });
    }

    private static JMenuItem item(JPopupMenu menu,String text) {
        for(Component c:menu.getComponents())if(c instanceof JMenuItem item&&text.equals(item.getText()))return item;
        throw new AssertionError("Missing menu item: "+text);
    }
}
