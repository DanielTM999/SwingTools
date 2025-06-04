package dtm.stools.context;

import dtm.stools.activity.NotificationActivity;
import lombok.NonNull;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class NotificationManager {
    private static final AtomicInteger gap = new AtomicInteger(10);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Queue<NotificationActivity> notificationActivities = new ConcurrentLinkedQueue<>(){
        @Override
        public boolean remove(Object o) {
            boolean result =  super.remove(o);
            debounceRearrange();
            return result;
        }

        @Override
        public boolean add(NotificationActivity notificationActivity) {
            boolean result = super.add(notificationActivity);
            debounceRearrange();
            return result;
        }
    };
    private static ScheduledFuture<?> scheduledRearrange;

    private NotificationManager() {}

    public static Queue<NotificationActivity> getRegisteredNotification(){
        return notificationActivities;
    }

    public static void startNotification(@NonNull NotificationActivity notificationActivity){
        SwingUtilities.invokeLater(() -> {
            notificationActivity.init();
            notificationActivities.add(notificationActivity);
        });
    }

    public static void remove(@NonNull NotificationActivity notificationActivity){
        notificationActivities.remove(notificationActivity);
    }

    public static void setNotificationGap(int gap){
        NotificationManager.gap.set(gap);
        debounceRearrange();
    }

    private static void debounceRearrange() {
        if (scheduledRearrange != null && !scheduledRearrange.isDone()) {
            scheduledRearrange.cancel(false);
        }
        scheduledRearrange = scheduler.schedule(() -> {
            SwingUtilities.invokeLater(NotificationManager::rearrangeNotifications);
        }, 200, TimeUnit.MILLISECONDS);
    }

    private static void rearrangeNotifications() {
        SwingUtilities.invokeLater(() -> {
            int gap = NotificationManager.gap.get();

            Map<NotificationActivity.NotificationActivityLocation, List<NotificationActivity>> grouped =
                    notificationActivities.stream()
                            .filter(a -> a.isDisplayable() && a.isVisible())
                            .collect(Collectors.groupingBy(NotificationActivity::getLocationNotification));

            for (var entry : grouped.entrySet()) {
                List<NotificationActivity> list = entry.getValue();

                int offset = 0;
                for (NotificationActivity notification : list) {
                    notification.positionWindow(offset);
                    offset += notification.getHeight() + gap;
                }
            }
        });
    }

}
