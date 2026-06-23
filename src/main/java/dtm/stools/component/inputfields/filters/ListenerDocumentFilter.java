package dtm.stools.component.inputfields.filters;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class ListenerDocumentFilter extends DocumentFilter {

    private final Runnable action;

    public ListenerDocumentFilter(Runnable action) {
        this.action = action;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, string, attr);
        if (action != null) action.run();
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        super.replace(fb, offset, length, text, attrs);
        if (action != null) action.run();
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length);
        if (action != null) action.run();
    }

}
