package dtm.stools.component.panels.editor.pdf.ui;

final class PdfRenderTask implements Runnable, Comparable<PdfRenderTask> {
    private final int priority;
    private final long sequence;
    private final Runnable body;

    PdfRenderTask(int priority, long sequence, Runnable body) {
        this.priority = priority;
        this.sequence = sequence;
        this.body = body;
    }

    @Override public void run() { body.run(); }

    @Override public int compareTo(PdfRenderTask other) {
        int order = Integer.compare(other.priority, priority);
        return order != 0 ? order : Long.compare(other.sequence, sequence);
    }
}
