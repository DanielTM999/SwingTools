package dtm.stools.component.textfields.events;

import java.awt.*;

public interface EventComponent {
    Component getComponent();
    Object getValue();
    <T> T tryGetValue();
    String getEventType();
}
