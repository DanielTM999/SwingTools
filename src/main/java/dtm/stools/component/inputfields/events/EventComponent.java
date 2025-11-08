package dtm.stools.component.inputfields.events;

import java.awt.*;

public interface EventComponent {
    Component getComponent();
    Object getValue();
    <T> T tryGetValue();
    String getEventType();
}
