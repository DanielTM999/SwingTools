package dtm.stools.context;

import dtm.stools.activity.NotificationActivity;
import dtm.stools.internal.wrapper.ConcurrentWeakReferenceDeque;
import dtm.stools.internal.wrapper.ConcurrentWeakReferenceQueue;
import lombok.NonNull;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public final class NotificationManager {
    private static final AtomicInteger gap = new AtomicInteger(10);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Queue<NotificationActivity> notificationActivities = new ConcurrentWeakReferenceDeque<>(){
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
        startNotification(notificationActivity, -1);
    }

    public static void startNotification(@NonNull NotificationActivity notificationActivity, long time){
        startNotification(notificationActivity, time, TimeUnit.MILLISECONDS);
    }

    public static void startNotification(@NonNull NotificationActivity notificationActivity, long time, TimeUnit timeUnit){
        SwingUtilities.invokeLater(() -> {
            notificationActivity.init();
            notificationActivities.add(notificationActivity);

            if (time > 0) {
                long durationMillis = timeUnit.toMillis(time);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        SwingUtilities.invokeLater(() -> {
                            notificationActivity.dispose();
                            notificationActivities.remove(notificationActivity);
                        });
                    }
                }, durationMillis);
            }
        });

    }

    public static void remove(@NonNull NotificationActivity notificationActivity){
        notificationActivities.remove(notificationActivity);
    }

    public static void shutdown(){
        shutdown(null);
    }

    public static void shutdown(Runnable onShutdown){
        CompletableFuture.runAsync(() -> {
            try{
                while (!notificationActivities.isEmpty()){
                    LockSupport.parkNanos(100);
                }
                scheduler.shutdown();
                if(onShutdown != null){
                    onShutdown.run();
                }
            }catch (Exception ignored){}
        });
    }

    public static void setNotificationGap(int gap){
        NotificationManager.gap.set(gap);
        debounceRearrange();
    }

    private static void debounceRearrange() {
        if (scheduledRearrange != null && !scheduledRearrange.isDone()) {
            scheduledRearrange.cancel(false);
        }
        if(!scheduler.isShutdown() && !scheduler.isTerminated()){
            scheduledRearrange = scheduler.schedule(() -> {
                SwingUtilities.invokeLater(NotificationManager::rearrangeNotifications);
            }, 200, TimeUnit.MILLISECONDS);
        }
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
