package dtm.stools.component.panels.editor.code.gutter.layer;

import java.util.List;

public interface TransferableLayer {

    List<Object> getTransferableListeners();

    void receiveTransferableListeners(List<Object> listeners);
}
