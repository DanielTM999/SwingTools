package dtm.stools.context;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface DomElementLoader {
    void load();
    void reload();
    void completeLoad();
    boolean isLoad();
    boolean isInitialized();
    Map<String, List<Component>> getDomElements();
    Future<Void> getLoadAction();
}
