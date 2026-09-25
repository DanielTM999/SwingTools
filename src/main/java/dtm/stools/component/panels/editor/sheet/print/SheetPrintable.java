package dtm.stools.component.panels.editor.sheet.print;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.List;

public final class SheetPrintable implements Printable, Pageable {
    private record Entry(SheetPageLayout layout, SheetPage page) {}

    private final SheetPageRenderer renderer;
    private final List<Entry> entries = new ArrayList<>();

    public SheetPrintable(SheetPageRenderer renderer, List<SheetPageLayout> layouts) {
        this.renderer = renderer;
        for (SheetPageLayout l : layouts) for (SheetPage p : l.pages()) entries.add(new Entry(l, p));
    }

    @Override public int getNumberOfPages() { return entries.size(); }

    @Override
    public PageFormat getPageFormat(int index) {
        Entry e = entries.get(index);
        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        double w = e.layout().settings().paper().width(), h = e.layout().settings().paper().height();
        paper.setSize(w, h);
        paper.setImageableArea(0, 0, w, h);
        pf.setPaper(paper);
        pf.setOrientation(e.layout().settings().orientation() == dtm.stools.component.panels.editor.sheet.model.PageOrientation.LANDSCAPE ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
        return pf;
    }

    @Override public Printable getPrintable(int index) { return this; }

    @Override
    public int print(Graphics graphics, PageFormat format, int index) {
        if (index < 0 || index >= entries.size()) return NO_SUCH_PAGE;
        Entry e = entries.get(index);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(format.getImageableX(), format.getImageableY());
            g.scale(1 / SheetPagination.PX_PER_POINT, 1 / SheetPagination.PX_PER_POINT);
            renderer.paint(g, e.layout(), e.page(), entries.size());
        } finally {
            g.dispose();
        }
        return PAGE_EXISTS;
    }
}
