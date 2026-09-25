package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.SheetEditor;

import javax.swing.JComponent;

@FunctionalInterface
public interface SheetUiFactory {
    SheetCanvas createCanvas(SheetEditor editor);

    default SheetFormulaBar createFormulaBar(SheetEditor editor) { return new SheetFormulaBar(editor); }
    default SheetTabBar createTabBar(SheetEditor editor) { return new SheetTabBar(editor); }
    default SheetStatusBar createStatusBar(SheetEditor editor) { return new SheetStatusBar(editor); }
    default JComponent createRibbon(SheetEditor editor) { return new SheetRibbon(editor); }

    static SheetUiFactory defaults() { return SheetCanvas::new; }
}
