package dtm.stools.component.panels.editor.sheet.calc;

public record IterationSettings(boolean enabled, int maxIterations, double maxChange) {
    public static final IterationSettings DISABLED = new IterationSettings(false, 100, 0.001);

    public IterationSettings {
        if (maxIterations < 1 || maxIterations > 32767) throw new IllegalArgumentException("Iterations must be between 1 and 32767");
        if (maxChange < 0) throw new IllegalArgumentException("Negative max change");
    }

    public IterationSettings withEnabled(boolean value) { return new IterationSettings(value, maxIterations, maxChange); }
}
