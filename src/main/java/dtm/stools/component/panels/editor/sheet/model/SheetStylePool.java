package dtm.stools.component.panels.editor.sheet.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class SheetStylePool {
    private final List<CellStyle> styles = new ArrayList<>();
    private final Map<CellStyle, Integer> index = new HashMap<>();

    public SheetStylePool() { intern(CellStyle.DEFAULT); }

    public synchronized int intern(CellStyle style) {
        Objects.requireNonNull(style);
        Integer id = index.get(style);
        if (id != null) return id;
        styles.add(style);
        index.put(style, styles.size() - 1);
        return styles.size() - 1;
    }

    public synchronized CellStyle get(int id) { return id >= 0 && id < styles.size() ? styles.get(id) : CellStyle.DEFAULT; }
    public synchronized int size() { return styles.size(); }
    public synchronized List<CellStyle> all() { return List.copyOf(styles); }
    public int derive(int id, UnaryOperator<CellStyle> change) { return intern(change.apply(get(id))); }

    public synchronized SheetStylePool copy() {
        SheetStylePool p = new SheetStylePool();
        for (int i = 1; i < styles.size(); i++) p.intern(styles.get(i));
        return p;
    }
}
