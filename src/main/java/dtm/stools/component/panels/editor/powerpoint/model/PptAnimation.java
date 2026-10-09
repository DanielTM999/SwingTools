package dtm.stools.component.panels.editor.powerpoint.model;

import java.util.Objects;
import java.util.UUID;

/** Timing is in milliseconds. An effect is attached to a stable object ID. */
public record PptAnimation(String id, String targetId, Effect effect, Start start,
                           int durationMs, int delayMs, int repeat, String direction) {
    public enum Effect { APPEAR, DISAPPEAR, FADE_IN, FADE_OUT, FLY_IN, FLY_OUT,
        WIPE_IN, WIPE_OUT, SPLIT_IN, SPLIT_OUT, ZOOM_IN, ZOOM_OUT,
        PULSE, SPIN, GROW_SHRINK, COLOR, TRANSPARENCY, LINE, ARC, POLYLINE }
    public enum Start { ON_CLICK, WITH_PREVIOUS, AFTER_PREVIOUS }
    public PptAnimation {
        Objects.requireNonNull(id); Objects.requireNonNull(targetId); Objects.requireNonNull(effect);
        Objects.requireNonNull(start); direction=Objects.requireNonNullElse(direction,"default");
        if (durationMs < 0 || delayMs < 0 || repeat < 1) throw new IllegalArgumentException("Invalid animation timing");
    }
    public static PptAnimation create(String targetId, Effect effect) {
        return new PptAnimation(UUID.randomUUID().toString(),targetId,effect,Start.ON_CLICK,600,0,1,"default");
    }
}
