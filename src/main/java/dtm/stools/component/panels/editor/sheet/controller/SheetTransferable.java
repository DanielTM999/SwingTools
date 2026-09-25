package dtm.stools.component.panels.editor.sheet.controller;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class SheetTransferable implements Transferable {
    static final DataFlavor HTML;

    static {
        try {
            HTML = new DataFlavor("text/html;class=java.lang.String");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private final String text;
    private final String html;

    SheetTransferable(String text, String html) { this.text = text; this.html = html; }

    @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{HTML, DataFlavor.stringFlavor, DataFlavor.getTextPlainUnicodeFlavor()}; }

    @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor f : getTransferDataFlavors()) if (f.equals(flavor)) return true;
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (HTML.equals(flavor)) return html;
        if (DataFlavor.stringFlavor.equals(flavor)) return text;
        if (DataFlavor.getTextPlainUnicodeFlavor().equals(flavor)) {
            InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_16LE));
            return in;
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
