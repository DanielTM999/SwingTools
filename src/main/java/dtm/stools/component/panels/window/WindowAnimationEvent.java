package dtm.stools.component.panels.window;

import java.awt.*;
import java.util.Map;

public class WindowAnimationEvent extends WindowEvent {
    private final WindowAnimationType animationType;
    private final float progress;

    public WindowAnimationEvent(Component source, WindowPanel window, String eventType,
                                WindowAnimationType animationType, float progress,
                                Map<String, Object> properties) {
        super(source, window, eventType, properties);
        this.animationType = animationType;
        this.progress = progress;
    }

    public WindowAnimationType getAnimationType() { return animationType; }
    public float getProgress() { return progress; }
}
