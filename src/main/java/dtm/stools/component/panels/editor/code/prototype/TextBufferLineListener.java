package dtm.stools.component.panels.editor.code.prototype;

public interface TextBufferLineListener {

    void linesChanged(int startLine, int removedLineBreaks, int insertedLineBreaks);
}
