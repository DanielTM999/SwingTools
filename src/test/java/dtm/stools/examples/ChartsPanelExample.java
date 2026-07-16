package dtm.stools.examples;

import dtm.stools.component.panels.charts.ChartsPanel;
import dtm.stools.component.panels.charts.render.BarChartRender;
import dtm.stools.component.panels.charts.render.LineChartRender;
import dtm.stools.component.panels.charts.render.PieChartRender;
import dtm.stools.component.panels.charts.style.LegendPosition;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class ChartsPanelExample {

    private static final String[] MONTHS = {"Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"};
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChartsPanelExample::createAndShow);
    }

    private static void createAndShow() {

        LineChartRender lineRender = new LineChartRender();
        lineRender.setTitle("Receita mensal");
        lineRender.setSubtitle("Comparativo 2025 x 2026");
        fillMonthly(lineRender);

        ChartsPanel<LineChartRender> lineChart = new ChartsPanel<>(lineRender);
        lineChart.setFPS(60);

        BarChartRender barRender = new BarChartRender();
        barRender.setTitle("Vendas por trimestre");
        fillQuarterly(barRender);

        ChartsPanel<BarChartRender> barChart = new ChartsPanel<>(barRender);
        barChart.setFPS(60);

        PieChartRender pieRender = new PieChartRender();
        pieRender.setTitle("Participação por produto");
        pieRender.setLegendPosition(LegendPosition.RIGHT);
        fillShare(pieRender);

        ChartsPanel<PieChartRender> pieChart = new ChartsPanel<>(pieRender);
        pieChart.setFPS(60);

        JPanel grid = new JPanel(new GridLayout(1, 3, 8, 8));
        grid.setBackground(new Color(0x0D0F14));
        grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        grid.add(lineChart);
        grid.add(barChart);
        grid.add(pieChart);

        JButton randomize = new JButton("Randomizar dados");
        randomize.addActionListener(e -> {
            fillMonthly(lineRender);
            fillQuarterly(barRender);
            fillShare(pieRender);
        });

        JFrame frame = new JFrame("SwingTools - Charts (OpenGL)");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(grid, BorderLayout.CENTER);
        frame.add(randomize, BorderLayout.SOUTH);
        frame.setSize(1500, 520);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void fillMonthly(LineChartRender render) {
        var previous = render.getDataSource().series("2025");
        var current = render.getDataSource().series("2026");
        double[] previousValues = new double[MONTHS.length];
        double[] currentValues = new double[MONTHS.length];
        double a = 40, b = 55;
        for (int i = 0; i < MONTHS.length; i++) {
            a = Math.max(10, a + RANDOM.nextGaussian() * 9);
            b = Math.max(10, b + RANDOM.nextGaussian() * 9);
            previousValues[i] = Math.round(a);
            currentValues[i] = Math.round(b);
        }
        if (previous.isEmpty()) {
            for (int i = 0; i < MONTHS.length; i++) {
                previous.add(MONTHS[i], previousValues[i]);
                current.add(MONTHS[i], currentValues[i]);
            }
        } else {
            previous.setValues(previousValues);
            current.setValues(currentValues);
        }
    }

    private static void fillQuarterly(BarChartRender render) {
        String[] quarters = {"T1", "T2", "T3", "T4"};
        var online = render.getDataSource().series("Online");
        var stores = render.getDataSource().series("Lojas");
        double[] onlineValues = new double[quarters.length];
        double[] storeValues = new double[quarters.length];
        for (int i = 0; i < quarters.length; i++) {
            onlineValues[i] = 30 + RANDOM.nextInt(70);
            storeValues[i] = 25 + RANDOM.nextInt(60);
        }
        if (online.isEmpty()) {
            for (int i = 0; i < quarters.length; i++) {
                online.add(quarters[i], onlineValues[i]);
                stores.add(quarters[i], storeValues[i]);
            }
        } else {
            online.setValues(onlineValues);
            stores.setValues(storeValues);
        }
    }

    private static void fillShare(PieChartRender render) {
        String[] products = {"Alpha", "Beta", "Gamma", "Delta", "Omega"};
        var share = render.getDataSource().series("Participação");
        double[] values = new double[products.length];
        for (int i = 0; i < products.length; i++) {
            values[i] = 10 + RANDOM.nextInt(90);
        }
        if (share.isEmpty()) {
            for (int i = 0; i < products.length; i++) {
                share.add(products[i], values[i]);
            }
        } else {
            share.setValues(values);
        }
    }
}
