package dtm.stools.core;

import java.awt.*;
import java.util.List;

public interface DomViewer {
    String getName();
    Component getRoot();
    List<DomViewer> getDomViewer();
}
