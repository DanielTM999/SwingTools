package dtm.stools.component.panels.editor.sheet.io.pdf;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetVisibility;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.print.SheetPage;
import dtm.stools.component.panels.editor.sheet.print.SheetPageLayout;
import dtm.stools.component.panels.editor.sheet.print.SheetPageRenderer;
import dtm.stools.component.panels.editor.sheet.print.SheetPagination;
import dtm.stools.component.panels.editor.sheet.provider.SheetExportOptions;
import dtm.stools.component.panels.editor.sheet.provider.SheetExportProvider;
import dtm.stools.component.panels.editor.sheet.render.ChartData;
import dtm.stools.component.panels.editor.sheet.render.SheetRenderer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SheetPdfExportProvider implements SheetExportProvider {
    private final SheetRenderer renderer;
    private final Function<SheetChart, ChartData> charts;

    public SheetPdfExportProvider() { this(new SheetRenderer(), null); }

    public SheetPdfExportProvider(SheetRenderer renderer, Function<SheetChart, ChartData> charts) {
        this.renderer = renderer == null ? new SheetRenderer() : renderer;
        this.charts = charts;
    }

    @Override public String id() { return "sheet.export.pdf"; }
    @Override public String extension() { return "pdf"; }
    @Override public String description() { return "PDF (*.pdf)"; }

    @Override
    public void export(SheetWorkbook workbook, CalcEngine values, SheetExportOptions options, OutputStream output) throws IOException {
        NumberFormatter formatter = values == null ? new NumberFormatter(options.locale(), workbook.properties().date1904()) : values.formatter();
        SheetPageRenderer pages = new SheetPageRenderer(workbook, values, formatter, renderer, charts, workbook.properties().title());
        List<SheetPageLayout> layouts = new ArrayList<>();
        if (options.allSheets()) {
            for (int s = 0; s < workbook.sheetCount(); s++) if (workbook.sheet(s).properties().visibility() == SheetVisibility.VISIBLE) layouts.add(SheetPagination.layout(workbook, values, s));
        } else layouts.add(SheetPagination.layout(workbook, values, options.sheet()));
        int total = layouts.stream().mapToInt(l -> l.pages().size()).sum();
        double scale = Math.max(0.5, options.dpi() / 96.0);
        SheetPdfWriter pdf = new SheetPdfWriter().title(workbook.properties().title());
        for (SheetPageLayout layout : layouts) {
            for (SheetPage page : layout.pages()) {
                int w = (int) Math.ceil(layout.pageWidth() * scale), h = (int) Math.ceil(layout.pageHeight() * scale);
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                try {
                    g.scale(scale, scale);
                    pages.paint(g, layout, page, total);
                } finally {
                    g.dispose();
                }
                pdf.addPage(img, layout.pageWidth() / SheetPagination.PX_PER_POINT, layout.pageHeight() / SheetPagination.PX_PER_POINT);
            }
        }
        pdf.write(output);
    }
}
