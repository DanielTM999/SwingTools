package dtm.stools.component.panels.editor.code.utils;

import javax.swing.JComponent;

public interface LoadingIndicator {

    JComponent getComponent();

    void start();

    void stop();
}
