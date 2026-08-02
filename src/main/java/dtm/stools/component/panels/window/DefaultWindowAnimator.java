package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DefaultWindowAnimator implements WindowAnimator {
    private final Map<WindowPanel, Timer> runningAnimations = new IdentityHashMap<>();
    private boolean enabled = true;
    private int durationMillis = -1;

    public DefaultWindowAnimator enabled(boolean value) { enabled = value; return this; }
    public DefaultWindowAnimator durationMillis(int value) { durationMillis = Math.max(0, value); return this; }
    public boolean isEnabled() { return enabled; }
    public int getDurationMillis() { return durationMillis; }

    @Override
    public void animate(WindowAnimationRequest request, Consumer<WindowAnimationFrame> frameConsumer,
                        Runnable completion) {
        WindowPanel window = request.getWindow();
        Rectangle from = request.getFromBounds();
        Rectangle to = request.getToBounds();
        int duration = durationMillis >= 0 ? durationMillis : request.getDurationMillis();
        cancel(window);
        if (!enabled || !window.isShowing() || duration == 0
                || (from.equals(to) && request.getFromAlpha() == request.getToAlpha())) {
            frameConsumer.accept(new WindowAnimationFrame(1f, to, request.getToAlpha()));
            if (completion != null) completion.run();
            return;
        }
        long started = System.nanoTime();
        Timer timer = new Timer(15, null);
        runningAnimations.put(window, timer);
        timer.addActionListener(event -> {
            float elapsed = (System.nanoTime() - started) / 1_000_000f;
            float progress = Math.min(1f, elapsed / duration);
            float eased = easeOutCubic(progress);
            float alpha = interpolate(request.getFromAlpha(), request.getToAlpha(), eased);
            if (progress >= 1f) {
                timer.stop();
                runningAnimations.remove(window);
                frameConsumer.accept(new WindowAnimationFrame(1f, to, request.getToAlpha()));
                if (completion != null) completion.run();
            } else {
                frameConsumer.accept(new WindowAnimationFrame(progress, interpolate(from, to, eased), alpha));
            }
        });
        timer.start();
    }

    @Override
    public boolean cancel(WindowPanel window) {
        Timer timer = runningAnimations.remove(window);
        if (timer == null) return false;
        timer.stop();
        return true;
    }

    protected Rectangle interpolate(Rectangle from, Rectangle to, float amount) {
        return new Rectangle(
                interpolate(from.x, to.x, amount), interpolate(from.y, to.y, amount),
                interpolate(from.width, to.width, amount), interpolate(from.height, to.height, amount));
    }

    protected int interpolate(int from, int to, float amount) {
        return Math.round(from + (to - from) * amount);
    }

    protected float interpolate(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    protected float easeOutCubic(float value) {
        float inverse = 1f - value;
        return 1f - inverse * inverse * inverse;
    }
}
