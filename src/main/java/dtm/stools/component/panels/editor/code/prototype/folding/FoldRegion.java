package dtm.stools.component.panels.editor.code.prototype.folding;

public record FoldRegion(int startLine, int endLine, boolean folded) {

    public FoldRegion withFolded(boolean f) {
        return new FoldRegion(startLine, endLine, f);
    }

    public boolean contains(int line) {
        return line >= startLine && line <= endLine;
    }

    public int hiddenLineCount() {
        return folded ? endLine - startLine : 0;
    }
}
