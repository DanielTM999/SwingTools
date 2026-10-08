package dtm.stools.component.panels.editor.pdf.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record PdfChange(int[] pages, int[] mapping) {
    public PdfChange {
        pages = pages == null ? null : pages.clone();
        mapping = mapping == null ? null : mapping.clone();
    }
    public static PdfChange all() { return new PdfChange(null, null); }
    public static PdfChange forPages(int... pages) { return new PdfChange(pages, null); }
    public static PdfChange structure(int[] mapping, int... pages) { return new PdfChange(pages, mapping); }
    public boolean affectsAll() { return pages == null; }
    public boolean structural() { return mapping != null; }
    @Override public int[] pages() { return pages == null ? null : pages.clone(); }
    @Override public int[] mapping() { return mapping == null ? null : mapping.clone(); }
    public PdfChange inverse(int pageCountAfter) {
        if (mapping == null) return this;
        int[] inverse = new int[pageCountAfter];
        Arrays.fill(inverse, -1);
        for (int old = 0; old < mapping.length; old++) if (mapping[old] >= 0 && mapping[old] < pageCountAfter) inverse[mapping[old]] = old;
        List<Integer> affected = new ArrayList<>();
        if (pages != null) for (int page : pages) if (page >= 0 && page < pageCountAfter && inverse[page] >= 0) affected.add(inverse[page]);
        return new PdfChange(pages == null ? null : affected.stream().mapToInt(Integer::intValue).toArray(), inverse);
    }
    public static int[] removed(int pageCount, int page) {
        int[] mapping = new int[pageCount];
        for (int i = 0; i < pageCount; i++) mapping[i] = i < page ? i : i == page ? -1 : i - 1;
        return mapping;
    }
    public static int[] moved(int pageCount, int source, int destination) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) order.add(i);
        order.add(destination, order.remove(source));
        int[] mapping = new int[pageCount];
        for (int position = 0; position < pageCount; position++) mapping[order.get(position)] = position;
        return mapping;
    }
    public static int[] inserted(int pageCount, int destination, int count) {
        int[] mapping = new int[pageCount];
        for (int i = 0; i < pageCount; i++) mapping[i] = i < destination ? i : i + count;
        return mapping;
    }
}
