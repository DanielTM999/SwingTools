package dtm.stools.examples;

import dtm.stools.component.panels.charts.ChartsPanel;
import dtm.stools.component.panels.charts.render.*;
import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.charts.style.LegendPosition;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
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

        BarChartRender barRender = new BarChartRender();
        barRender.setTitle("Vendas por trimestre");
        fillQuarterly(barRender);

        PieChartRender pieRender = new PieChartRender();
        pieRender.setTitle("Participação por produto");
        pieRender.setLegendPosition(LegendPosition.RIGHT);
        fillShare(pieRender);

        AreaChartRender areaRender = new AreaChartRender();
        areaRender.setTitle("Tráfego semanal");
        fillTraffic(areaRender);

        StackedBarChartRender stackedRender = new StackedBarChartRender();
        stackedRender.setTitle("Custos por mês");
        fillCosts(stackedRender);

        HorizontalBarChartRender hbarRender = new HorizontalBarChartRender();
        hbarRender.setTitle("Vendas por região");
        fillRegions(hbarRender);

        RadarChartRender radarRender = new RadarChartRender();
        radarRender.setTitle("Perfil dos times");
        fillTeams(radarRender);

        GaugeChartRender gaugeRender = new GaugeChartRender();
        gaugeRender.setTitle("Uso de CPU");
        gaugeRender.setUnit("%");
        gaugeRender.setValueLabel("núcleo 0");
        gaugeRender.setMaxValue(100);
        gaugeRender.addColorStop(0f, ChartColor.hex("#22C55E"));
        gaugeRender.addColorStop(0.6f, ChartColor.hex("#F59E0B"));
        gaugeRender.addColorStop(0.85f, ChartColor.hex("#EF4444"));
        gaugeRender.setValue(35 + RANDOM.nextInt(60));

        JPanel grid = new JPanel(new GridLayout(2, 4, 8, 8));
        grid.setBackground(new Color(0x0D0F14));
        grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        grid.add(wrap(lineRender));
        grid.add(wrap(barRender));
        grid.add(wrap(pieRender));
        grid.add(wrap(areaRender));
        grid.add(wrap(stackedRender));
        grid.add(wrap(hbarRender));
        grid.add(wrap(radarRender));
        grid.add(wrap(gaugeRender));

        JButton randomize = new JButton("Randomizar dados");
        randomize.addActionListener(e -> {
            fillMonthly(lineRender);
            fillQuarterly(barRender);
            fillShare(pieRender);
            fillTraffic(areaRender);
            fillCosts(stackedRender);
            fillRegions(hbarRender);
            fillTeams(radarRender);
            gaugeRender.setValue(35 + RANDOM.nextInt(60));
        });

        JFrame frame = new JFrame("SwingTools - Charts (OpenGL)");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(grid, BorderLayout.CENTER);
        frame.add(randomize, BorderLayout.SOUTH);
        frame.setSize(1500, 860);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static <R extends ChartBaseRender> ChartsPanel<R> wrap(R render) {
        ChartsPanel<R> panel = new ChartsPanel<>(render);
        panel.setFPS(60);
        return panel;
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
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String product : products) {
            values.put(product, 10 + RANDOM.nextInt(90));
        }
        render.getDataSource().addSeries("Participação", values);
    }

    private static void fillTraffic(AreaChartRender render) {
        String[] days = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String day : days) {
            values.put(day, 200 + RANDOM.nextInt(350));
        }
        render.getDataSource().addSeries("Visitas", values);
    }

    private static void fillCosts(StackedBarChartRender render) {
        String[] months = {"Jan", "Fev", "Mar", "Abr", "Mai"};
        String[] groups = {"Infra", "Pessoal", "Marketing"};
        for (String group : groups) {
            Map<String, Integer> values = new LinkedHashMap<>();
            for (String month : months) {
                values.put(month, 10 + RANDOM.nextInt(45));
            }
            render.getDataSource().addSeries(group, values);
        }
    }

    private static void fillRegions(HorizontalBarChartRender render) {
        String[] regions = {"Sudeste", "Sul", "Nordeste", "Centro-Oeste", "Norte"};
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String region : regions) {
            values.put(region, 15 + RANDOM.nextInt(75));
        }
        render.getDataSource().addSeries("2026", values);
    }

    private static void fillTeams(RadarChartRender render) {
        String[] skills = {"Velocidade", "Qualidade", "Entrega", "Inovação", "Suporte", "Custo"};
        for (String team : new String[]{"Time A", "Time B"}) {
            Map<String, Integer> values = new LinkedHashMap<>();
            for (String skill : skills) {
                values.put(skill, 40 + RANDOM.nextInt(60));
            }
            render.getDataSource().addSeries(team, values);
        }
    }
}
