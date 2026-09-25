package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;

import java.util.Arrays;

final class Ets {
    private record Series(double[] values, double start, double step) {}
    private record Model(double alpha, double beta, double gamma, int season, double level, double trend, double[] seasonal, double sse, double mae, double smape, double mase, int n) {}

    private Ets() {}

    static double forecast(FunctionContext c, FunctionArgs a) {
        Series s = series(c, a, 1, 2, a.integer(4, 1), a.integer(5, 1));
        Model m = fit(s, seasonality(s.values, a.has(3) ? a.integer(3) : 1));
        double h = (a.number(0) - (s.start + (s.values.length - 1) * s.step)) / s.step;
        if (h < 0) throw EvalError.num();
        return predict(m, h);
    }

    static double confidence(FunctionContext c, FunctionArgs a) {
        Series s = series(c, a, 1, 2, a.integer(5, 1), a.integer(6, 1));
        Model m = fit(s, seasonality(s.values, a.has(4) ? a.integer(4) : 1));
        double level = a.number(3, 0.95);
        if (level <= 0 || level >= 1) throw EvalError.num();
        double h = Math.max(1, (a.number(0) - (s.start + (s.values.length - 1) * s.step)) / s.step);
        double z = SpecialFunctions.normalInverse((1 + level) / 2);
        return z * Math.sqrt(m.sse / Math.max(1, m.n)) * Math.sqrt(h);
    }

    static double seasonality(FunctionContext c, FunctionArgs a) {
        Series s = series(c, a, 0, 1, a.integer(2, 1), a.integer(3, 1));
        return seasonality(s.values, 1);
    }

    static double statistic(FunctionContext c, FunctionArgs a) {
        Series s = series(c, a, 0, 1, a.integer(4, 1), a.integer(5, 1));
        Model m = fit(s, seasonality(s.values, a.has(3) ? a.integer(3) : 1));
        return switch (a.integer(2)) {
            case 1 -> m.alpha;
            case 2 -> m.beta;
            case 3 -> m.gamma;
            case 4 -> m.mase;
            case 5 -> m.smape;
            case 6 -> m.mae;
            case 7 -> Math.sqrt(m.sse / Math.max(1, m.n));
            case 8 -> s.step;
            default -> throw EvalError.num();
        };
    }

    private static Series series(FunctionContext c, FunctionArgs a, int valuesIndex, int timelineIndex, int completion, int aggregation) {
        double[] values = Lib.numbersOf(c, a.value(valuesIndex), false).toArray();
        double[] time = Lib.numbersOf(c, a.value(timelineIndex), false).toArray();
        if (values.length != time.length || values.length < 3) throw EvalError.num();
        int[] order = Lib.sortedIndexes(time);
        double[] t = new double[time.length], v = new double[values.length];
        for (int k = 0; k < order.length; k++) { t[k] = time[order[k]]; v[k] = values[order[k]]; }
        double step = Double.MAX_VALUE;
        for (int k = 1; k < t.length; k++) if (t[k] - t[k - 1] > 0) step = Math.min(step, t[k] - t[k - 1]);
        if (step == Double.MAX_VALUE || step <= 0) throw EvalError.num();
        int slots = (int) Math.round((t[t.length - 1] - t[0]) / step) + 1;
        double[] regular = new double[slots];
        int[] counts = new int[slots];
        Arrays.fill(regular, Double.NaN);
        for (int k = 0; k < t.length; k++) {
            int slot = (int) Math.round((t[k] - t[0]) / step);
            if (Double.isNaN(regular[slot])) regular[slot] = v[k];
            else regular[slot] = aggregation == 7 ? regular[slot] + v[k] : Math.max(regular[slot], v[k]);
            counts[slot]++;
        }
        for (int k = 0; k < slots; k++) {
            if (!Double.isNaN(regular[k])) continue;
            if (completion == 0) { regular[k] = 0; continue; }
            int prev = k - 1, next = k + 1;
            while (next < slots && Double.isNaN(regular[next])) next++;
            regular[k] = next < slots ? regular[prev] + (regular[next] - regular[prev]) / (next - prev) : regular[prev];
        }
        return new Series(regular, t[0], step);
    }

    private static int seasonality(double[] values, int requested) {
        if (requested == 0) return 1;
        if (requested > 1) { if (requested > values.length / 2) throw EvalError.num(); return requested; }
        int n = values.length;
        double mean = 0;
        for (double v : values) mean += v;
        mean /= n;
        double var = 0;
        for (double v : values) var += (v - mean) * (v - mean);
        if (var == 0) return 1;
        int best = 1;
        double bestCorr = 0.5;
        for (int lag = 2; lag <= n / 2; lag++) {
            double s = 0;
            for (int k = lag; k < n; k++) s += (values[k] - mean) * (values[k - lag] - mean);
            double corr = s / var;
            if (corr > bestCorr) { bestCorr = corr; best = lag; }
        }
        return best;
    }

    private static Model fit(Series s, int season) {
        Model best = null;
        double[] grid = {0.05, 0.1, 0.2, 0.3, 0.5, 0.7, 0.9};
        for (double alpha : grid) for (double beta : new double[]{0.01, 0.05, 0.1, 0.3}) for (double gamma : season > 1 ? grid : new double[]{0}) {
            Model m = run(s.values, alpha, beta, gamma, season);
            if (best == null || m.sse < best.sse) best = m;
        }
        return best;
    }

    private static Model run(double[] y, double alpha, double beta, double gamma, int season) {
        int n = y.length;
        double level = y[0], trend = n > 1 ? y[1] - y[0] : 0;
        double[] seasonal = new double[Math.max(1, season)];
        if (season > 1 && n >= season) {
            double avg = 0;
            for (int k = 0; k < season; k++) avg += y[k];
            avg /= season;
            for (int k = 0; k < season; k++) seasonal[k] = y[k] - avg;
            level = avg;
            trend = n >= 2 * season ? (avgOf(y, season, 2 * season) - avg) / season : 0;
        }
        double sse = 0, mae = 0, smape = 0, naive = 0;
        int count = 0;
        for (int k = 1; k < n; k++) {
            double s = season > 1 ? seasonal[k % season] : 0;
            double forecast = level + trend + s;
            double error = y[k] - forecast;
            sse += error * error;
            mae += Math.abs(error);
            double denom = Math.abs(y[k]) + Math.abs(forecast);
            smape += denom == 0 ? 0 : 2 * Math.abs(error) / denom;
            naive += Math.abs(y[k] - y[k - 1]);
            count++;
            double previousLevel = level;
            level = alpha * (y[k] - s) + (1 - alpha) * (level + trend);
            trend = beta * (level - previousLevel) + (1 - beta) * trend;
            if (season > 1) seasonal[k % season] = gamma * (y[k] - level) + (1 - gamma) * s;
        }
        double mase = naive == 0 ? 0 : (mae / count) / (naive / count);
        return new Model(alpha, beta, gamma, season, level, trend, seasonal, sse, mae / Math.max(1, count), smape / Math.max(1, count), mase, count);
    }

    private static double avgOf(double[] y, int from, int to) { double s = 0; for (int k = from; k < to; k++) s += y[k]; return s / (to - from); }

    private static double predict(Model m, double h) {
        double s = m.season > 1 ? m.seasonal[(int) ((m.n + Math.round(h)) % m.season)] : 0;
        return m.level + h * m.trend + s;
    }
}
