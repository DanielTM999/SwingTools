package dtm.stools.activity;

import dtm.stools.context.NotificationManager;
import lombok.Getter;

import java.awt.*;

@Getter
public abstract class NotificationActivity extends TransientPopupActivity{

    private final NotificationActivityLocation locationNotification;

    protected NotificationActivity() {
        this.locationNotification = NotificationActivityLocation.BOTTOM_RIGHT;
    }

    protected NotificationActivity(NotificationActivityLocation location) {
        this.locationNotification = location;
    }

    @Override
    protected void onDrawing() {
        super.onDrawing();
        setSize(300, 100);
        positionWindow(0);
    }

    @Override
    public void dispose() {
        super.dispose();
        NotificationManager.remove(this);
    }

    public void positionWindow(int verticalOffset) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension windowSize = getSize();

        final int padding = 20;
        int x = 0;
        int y = 0;

        switch (locationNotification) {
            case TOP_LEFT -> {
                x = padding;
                y = padding + verticalOffset;
            }
            case TOP_RIGHT -> {
                x = screenSize.width - windowSize.width - padding;
                y = padding + verticalOffset;
            }
            case BOTTOM_LEFT -> {
                x = padding;
                y = screenSize.height - windowSize.height - padding - verticalOffset;
            }
            case BOTTOM_RIGHT -> {
                x = screenSize.width - windowSize.width - padding;
                y = screenSize.height - windowSize.height - padding - verticalOffset;
            }
        }

        setLocation(x, y);
    }

    public enum NotificationActivityLocation{
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_LEFT,
        TOP_RIGHT,
    }

}
