package dtm.stools.context;

import dtm.stools.activity.NotificationActivity;
import lombok.NonNull;

import java.util.*;
import javax.swing.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public final class NotificationManager {
    private static final AtomicInteger gap = new AtomicInteger(10);
    private static final Object schedulerLock = new Object();
    private static ScheduledExecutorService scheduler = createScheduler();

    private static ScheduledExecutorService createScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("NotificationManager-Main-Worker-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
        });
    }

    private static final Queue<NotificationActivity> notificationActivities = new ConcurrentLinkedDeque<>() {
        @Override
        public boolean add(NotificationActivity notificationActivity) {
            boolean result = super.add(notificationActivity);
            debounceRearrange();
            return result;
        }

        @Override
        public boolean remove(Object o) {
            boolean result = super.remove(o);
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
        TimeUnit effectiveTimeUnit = timeUnit == null ? TimeUnit.MILLISECONDS : timeUnit;
        SwingUtilities.invokeLater(() -> {
            notificationActivities.add(notificationActivity);
            notificationActivity.init();
            rearrangeNotificationsOnEdt();
            scheduleRearrangeBurst();

            if (time > 0) {
                scheduler().schedule(() -> {
                    SwingUtilities.invokeLater(() -> {
                        notificationActivity.dispose();
                        notificationActivities.remove(notificationActivity);
                        debounceRearrange();
                    });
                }, time, effectiveTimeUnit);
            }
        });

    }

    public static void remove(@NonNull NotificationActivity notificationActivity){
        notificationActivities.remove(notificationActivity);
        debounceRearrange();
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
                scheduler().shutdown();
                if(onShutdown != null){
                    onShutdown.run();
                }
            }catch (Exception ignored){}
        });
    }

    public static void setNotificationGap(int gap){
        NotificationManager.gap.set(gap);
        rearrange();
    }

    public static void rearrange() {
        if (SwingUtilities.isEventDispatchThread()) {
            rearrangeNotificationsOnEdt();
        } else {
            SwingUtilities.invokeLater(NotificationManager::rearrangeNotificationsOnEdt);
        }
    }

    private static void debounceRearrange() {
        if (scheduledRearrange != null && !scheduledRearrange.isDone()) {
            scheduledRearrange.cancel(false);
        }
        scheduledRearrange = scheduleRearrange(200);
    }

    private static void scheduleRearrangeBurst() {
        scheduleRearrange(50);
        scheduleRearrange(150);
        scheduleRearrange(350);
        scheduleRearrange(700);
    }

    private static ScheduledFuture<?> scheduleRearrange(long delayMillis) {
        return scheduler().schedule(() -> {
            SwingUtilities.invokeLater(NotificationManager::rearrangeNotificationsOnEdt);
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    private static ScheduledExecutorService scheduler() {
        synchronized (schedulerLock) {
            if (scheduler.isShutdown() || scheduler.isTerminated()) {
                scheduler = createScheduler();
            }
            return scheduler;
        }
    }

    private static void rearrangeNotificationsOnEdt() {
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
                notification.setAlwaysOnTop(true);
                notification.toFront();
                offset += notification.getHeight() + gap;
            }
        }
    }

}
