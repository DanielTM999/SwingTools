package dtm.stools.component.panels.editor.code;

interface WrappedSpanVisitor {
    void visit(int x1, int x2, int y, boolean lastRow);
}
