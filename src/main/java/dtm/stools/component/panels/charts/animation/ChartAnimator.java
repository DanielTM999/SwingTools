package dtm.stools.component.panels.charts.animation;

public class ChartAnimator {

    private volatile float durationSeconds = 0.9f;
    private volatile ChartEasing easing = ChartEasing.EASE_OUT_CUBIC;
    private volatile boolean restartRequested = true;

    private float elapsedSeconds;

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(float durationSeconds) {
        this.durationSeconds = Math.max(0.01f, durationSeconds);
    }

    public ChartEasing getEasing() {
        return easing;
    }

    public void setEasing(ChartEasing easing) {
        if (easing == null) throw new IllegalArgumentException("easing cannot be null");
        this.easing = easing;
    }

    public void restart() {
        restartRequested = true;
    }

    public float update(float deltaSeconds) {
        if (restartRequested) {
            restartRequested = false;
            elapsedSeconds = 0f;
        }
        if (deltaSeconds > 0f && deltaSeconds < 1f) {
            elapsedSeconds += deltaSeconds;
        }
        float t = Math.min(1f, elapsedSeconds / durationSeconds);
        return easing.applyClamped(t);
    }

    public boolean isFinished() {
        return !restartRequested && elapsedSeconds >= durationSeconds;
    }
}
