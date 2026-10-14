package dtm.stools.examples;

import dtm.stools.component.charts.data.ChartSeries;
import dtm.stools.component.charts.style.ChartColor;
import dtm.stools.component.charts.style.ChartTheme;
import dtm.stools.component.charts2d.ChartExporter;
import dtm.stools.component.charts2d.ChartImageFormat;
import dtm.stools.component.charts2d.render.Gauge2DRender;
import dtm.stools.component.charts2d.render.Line2DRender;

import java.io.File;
import java.io.IOException;

public class ChartExporterExample {

    public static void main(String[] args) throws IOException {
        File outputDir = new File(args.length > 0 ? args[0] : System.getProperty("java.io.tmpdir"), "chart-exporter");

        Line2DRender line = new Line2DRender();
        line.setTitle("Usage");
        line.getDataSource().addSeries(ChartSeries.of("CPU", 12, 35, 28, 64, 51, 72, 40));
        line.getDataSource().addSeries(ChartSeries.of("Memory", 40, 42, 45, 50, 48, 55, 60));

        Gauge2DRender gauge = new Gauge2DRender();
        gauge.setValueLabel("CPU");
        gauge.setUnit("%");
        gauge.setValue(67.5);

        ChartExporter lineExporter = new ChartExporter(line).setSize(800, 450);
        lineExporter.writeTo(new File(outputDir, "line.png"));
        lineExporter.writeTo(new File(outputDir, "line.jpg"));
        lineExporter.setTheme(ChartTheme.light()).writeTo(new File(outputDir, "line-light.png"));
        lineExporter.setTransparentBackground().writeTo(new File(outputDir, "line-light-transparent.png"));
        lineExporter.setTheme(null).setBackground(ChartColor.hex("#1E3A5F")).writeTo(new File(outputDir, "line-custom.jpg"));

        ChartExporter gaugeExporter = new ChartExporter(gauge).setSize(400, 400).setScale(2);
        gaugeExporter.writeTo(new File(outputDir, "gauge.jpeg"));
        gaugeExporter.setTransparentBackground().writeTo(new File(outputDir, "gauge-transparent.png"));

        byte[] bytes = gaugeExporter.setFormat(ChartImageFormat.PNG).toBytes();
        String base64 = gaugeExporter.setFormat(ChartImageFormat.JPEG).toBase64();

        System.out.println("Output: " + outputDir.getAbsolutePath());
        System.out.println("PNG bytes: " + bytes.length);
        System.out.println("JPEG base64 length: " + base64.length());
    }
}
