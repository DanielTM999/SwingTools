package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.api.PdfDocument;

import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class PdfRenderScheduler implements AutoCloseable {
    private static final int PAGE_PRIORITY = 10, THUMBNAIL_PRIORITY = 5, BACKGROUND_PRIORITY = 1;
    private static final double MAX_PIXELS = 18_000_000;
    private static final int THUMBNAIL_LIMIT = 200;

    private final Supplier<PdfDocument> document;
    private final IntFunction<PdfPageGeometry> pageGeometry;
    private final IntSupplier cacheLimit;
    private final ThreadPoolExecutor executor;
    private final AtomicLong sequence = new AtomicLong();
    private final LinkedHashMap<Integer, PdfRenderedImage> pages = new LinkedHashMap<>(16, .75f, true);
    private final LinkedHashMap<Integer, PdfRenderedImage> thumbnails = new LinkedHashMap<>(64, .75f, true);
    private final Map<Integer, Long> revisions = new HashMap<>();
    private final Map<String, Long> wanted = new ConcurrentHashMap<>();
    private final Map<String, String> pending = new HashMap<>();
    private long epoch, structure;
    private volatile boolean closed;

    public PdfRenderScheduler(Supplier<PdfDocument> document, IntFunction<PdfPageGeometry> pageGeometry, IntSupplier cacheLimit) {
        this.document = document;
        this.pageGeometry = pageGeometry;
        this.cacheLimit = cacheLimit;
        executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(), runnable -> {
            Thread thread = new Thread(runnable, "swingtools-pdf-render");
            thread.setDaemon(true);
            return thread;
        });
    }

    public BufferedImage page(int page, float dpi, Runnable ready) {
        return request(pages, "p", page, limit(page, dpi), PAGE_PRIORITY, cacheLimit.getAsInt(), ready);
    }

    public boolean isFresh(int page, float dpi) {
        PdfRenderedImage entry = pages.get(page);
        return entry != null && entry.revision() == revision(page) && Math.abs(entry.resolution() - limit(page, dpi)) < .5f;
    }

    public BufferedImage thumbnail(int page, int width, Runnable ready) {
        PdfPageGeometry geometry = pageGeometry.apply(page);
        if (geometry == null) return null;
        double pageWidth = geometry.withScale(1).viewWidth();
        float dpi = Math.max(8, Math.round(width * 72f / (float) Math.max(1, pageWidth) * 4) / 4f);
        return request(thumbnails, "t", page, dpi, THUMBNAIL_PRIORITY, THUMBNAIL_LIMIT, ready);
    }

    public void background(Runnable task) {
        if (!closed) executor.execute(new PdfRenderTask(BACKGROUND_PRIORITY, sequence.incrementAndGet(), task));
    }

    public void invalidate(int... changed) {
        for (int page : changed) revisions.merge(page, 1L, Long::sum);
    }

    public void invalidateAll() {
        epoch++;
        pending.clear();
    }

    public void remap(int[] mapping) {
        structure++;
        pending.clear();
        wanted.clear();
        remap(pages, mapping);
        remap(thumbnails, mapping);
        Map<Integer, Long> moved = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : revisions.entrySet()) {
            int page = entry.getKey();
            if (page >= 0 && page < mapping.length && mapping[page] >= 0) moved.put(mapping[page], entry.getValue());
        }
        revisions.clear();
        revisions.putAll(moved);
    }

    public void clear() {
        epoch++;
        structure++;
        pages.clear();
        thumbnails.clear();
        revisions.clear();
        pending.clear();
        wanted.clear();
    }

    private void remap(LinkedHashMap<Integer, PdfRenderedImage> cache, int[] mapping) {
        Map<Integer, PdfRenderedImage> moved = new LinkedHashMap<>();
        for (Map.Entry<Integer, PdfRenderedImage> entry : cache.entrySet()) {
            int page = entry.getKey();
            if (page >= 0 && page < mapping.length && mapping[page] >= 0) moved.put(mapping[page], entry.getValue());
        }
        cache.clear();
        cache.putAll(moved);
    }

    private long revision(int page) { return (epoch << 32) + revisions.getOrDefault(page, 0L); }

    private float limit(int page, float dpi) {
        PdfPageGeometry geometry = pageGeometry.apply(page);
        if (geometry == null) return dpi;
        double pixels = geometry.width() * geometry.height() / (72.0 * 72.0) * dpi * dpi;
        float bounded = pixels <= MAX_PIXELS ? dpi : (float) (dpi * Math.sqrt(MAX_PIXELS / pixels));
        return Math.round(bounded * 2) / 2f;
    }

    private BufferedImage request(LinkedHashMap<Integer, PdfRenderedImage> cache, String kind, int page, float dpi,
                                  int priority, int capacity, Runnable ready) {
        if (closed) return null;
        PdfRenderedImage entry = cache.get(page);
        long revision = revision(page);
        boolean fresh = entry != null && entry.revision() == revision && Math.abs(entry.resolution() - dpi) < .5f;
        if (!fresh) schedule(cache, kind, page, dpi, revision, priority, capacity, ready);
        return entry == null ? null : entry.image();
    }

    private void schedule(LinkedHashMap<Integer, PdfRenderedImage> cache, String kind, int page, float dpi, long revision,
                          int priority, int capacity, Runnable ready) {
        String key = kind + page;
        String signature = revision + "@" + dpi;
        if (signature.equals(pending.get(key))) return;
        pending.put(key, signature);
        long ticket = sequence.incrementAndGet();
        wanted.put(key, ticket);
        long layout = structure;
        PdfDocument source = document.get();
        executor.execute(new PdfRenderTask(priority, ticket, () -> {
            Long current = wanted.get(key);
            if (closed || current == null || current != ticket) return;
            BufferedImage image;
            try { image = source.render(page, dpi); }
            catch (Throwable error) {
                SwingUtilities.invokeLater(() -> { if (signature.equals(pending.get(key))) pending.remove(key); });
                return;
            }
            SwingUtilities.invokeLater(() -> {
                if (signature.equals(pending.get(key))) pending.remove(key);
                if (closed || layout != structure || revision(page) != revision || source != document.get()) return;
                cache.put(page, new PdfRenderedImage(image, dpi, revision));
                while (cache.size() > Math.max(1, capacity)) cache.remove(cache.keySet().iterator().next());
                if (ready != null) ready.run();
            });
        }));
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdownNow();
        pages.clear();
        thumbnails.clear();
    }
}
