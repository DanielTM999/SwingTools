package dtm.stools.component.panels.editor.word.api;

public record WordSelection(int anchor, int caret) implements WordContentSelection {
    public WordSelection { if(anchor<0 || caret<0) throw new IllegalArgumentException("Negative selection"); }
    public int start() { return Math.min(anchor,caret); }
    public int end() { return Math.max(anchor,caret); }
    public boolean isEmpty() { return anchor==caret; }
    @Override public WordSelection range() { return this; }
}
