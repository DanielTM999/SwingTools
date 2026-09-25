package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.calc.OmittedValue;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.EmptyValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.STATISTICAL;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class StatisticalFunctions {
    private StatisticalFunctions() {}

    static void register(FunctionRegistry r) {
        raw(r, "AVERAGE", STATISTICAL, 1, 255, (c, a) -> num(numbers(c, a, 0).mean()));
        raw(r, "AVERAGEA", STATISTICAL, 1, 255, (c, a) -> num(numbers(c, a, 0, a.size(), true).mean()));
        raw(r, "AVERAGEIF", STATISTICAL, 2, 3, (c, a) -> {
            ArrayValue values = c.toArray(a.value(0));
            ArrayValue avg = a.has(2) ? MathFunctions.sumRange(c, a.value(2), values) : values;
            Predicate<CellValue> p = criteria(a.scalar(1));
            DoubleList l = new DoubleList();
            for (int row = 0; row < values.rows(); row++) for (int col = 0; col < values.columns(); col++) {
                if (!p.test(values.get(row, col))) continue;
                CellValue v = row < avg.rows() && col < avg.columns() ? avg.get(row, col) : CellValue.EMPTY;
                if (v instanceof ErrorValue e) throw EvalError.of(e.error());
                if (v instanceof NumberValue n) l.add(n.value());
            }
            return num(l.mean());
        });
        raw(r, "AVERAGEIFS", STATISTICAL, 3, 255, (c, a) -> num(ifsValues(c, a).mean()));
        raw(r, "MAXIFS", STATISTICAL, 3, 255, (c, a) -> { DoubleList l = ifsValues(c, a); return num(l.size() == 0 ? 0 : MathFunctions.max(l)); });
        raw(r, "MINIFS", STATISTICAL, 3, 255, (c, a) -> { DoubleList l = ifsValues(c, a); return num(l.size() == 0 ? 0 : MathFunctions.min(l)); });
        raw(r, "COUNT", STATISTICAL, 1, 255, (c, a) -> num(count(c, a, false)));
        raw(r, "COUNTA", STATISTICAL, 1, 255, (c, a) -> num(count(c, a, true)));
        raw(r, "COUNTBLANK", STATISTICAL, 1, 1, (c, a) -> {
            ArrayValue arr = c.toArray(a.value(0));
            long n = 0;
            if (a.value(0) instanceof ReferenceValue ref && ref.isSingleArea()) {
                long total = ref.range().cellCount();
                long[] filled = {0};
                c.forEachCell(ref, (s, row, col, v) -> { if (!(v instanceof TextValue t && t.value().isEmpty())) filled[0]++; });
                return num(total - filled[0]);
            }
            for (CellValue v : arr.list()) if (v.isEmpty() || v instanceof TextValue t && t.value().isEmpty()) n++;
            return num(n);
        });
        raw(r, "COUNTIF", STATISTICAL, 2, 2, (c, a) -> {
            CellValue crit = c.deref(a.value(1));
            if (crit instanceof ArrayValue ca) {
                ArrayValue values = c.toArray(a.value(0));
                return ca.map(x -> { Predicate<CellValue> p = criteria(x); long n = 0; for (CellValue v : values.list()) if (p.test(v)) n++; return num(n); });
            }
            Predicate<CellValue> p = criteria(crit);
            if (a.value(0) instanceof ReferenceValue ref && ref.isSingleArea() && !p.test(CellValue.EMPTY)) {
                long[] n = {0};
                c.forEachCell(ref, (s, row, col, v) -> { if (p.test(v)) n[0]++; });
                return num(n[0]);
            }
            long n = 0;
            for (CellValue v : c.toArray(a.value(0)).list()) if (p.test(v)) n++;
            if (a.value(0) instanceof ReferenceValue ref && ref.isSingleArea() && p.test(CellValue.EMPTY)) {
                ArrayValue clipped = c.toArray(a.value(0));
                n += ref.range().cellCount() - clipped.size();
            }
            return num(n);
        });
        raw(r, "COUNTIFS", STATISTICAL, 2, 254, (c, a) -> {
            ArrayValue first = c.toArray(a.value(0));
            FunctionArgs shifted = a;
            boolean[] mask = MathFunctions.ifsMask(c, shifted, 0, first.rows(), first.columns());
            long n = 0;
            for (boolean b : mask) if (b) n++;
            return num(n);
        });
        raw(r, "MAX", STATISTICAL, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0); return num(l.size() == 0 ? 0 : MathFunctions.max(l)); });
        raw(r, "MIN", STATISTICAL, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0); return num(l.size() == 0 ? 0 : MathFunctions.min(l)); });
        raw(r, "MAXA", STATISTICAL, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0, a.size(), true); return num(l.size() == 0 ? 0 : MathFunctions.max(l)); });
        raw(r, "MINA", STATISTICAL, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0, a.size(), true); return num(l.size() == 0 ? 0 : MathFunctions.min(l)); });
        raw(r, "MEDIAN", STATISTICAL, 1, 255, (c, a) -> num(median(numbers(c, a, 0).sorted())));
        raw(r, "MODE.SNGL", STATISTICAL, 1, 255, (c, a) -> mode(ordered(c, a, 0)));
        raw(r, "MODE.MULT", STATISTICAL, 1, 255, (c, a) -> {
            double[] values = ordered(c, a, 0);
            Map<Double, Integer> counts = new LinkedHashMap<>();
            for (double v : values) counts.merge(v, 1, Integer::sum);
            int best = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            if (best < 2) throw EvalError.na();
            List<CellValue> out = new ArrayList<>();
            counts.forEach((k, v) -> { if (v == best) out.add(num(k)); });
            return ArrayValue.column(out);
        });
        raw(r, "STDEV.S", STATISTICAL, 1, 255, (c, a) -> num(Math.sqrt(variance(numbers(c, a, 0), true))));
        raw(r, "STDEV.P", STATISTICAL, 1, 255, (c, a) -> num(Math.sqrt(variance(numbers(c, a, 0), false))));
        raw(r, "STDEVA", STATISTICAL, 1, 255, (c, a) -> num(Math.sqrt(variance(numbers(c, a, 0, a.size(), true), true))));
        raw(r, "STDEVPA", STATISTICAL, 1, 255, (c, a) -> num(Math.sqrt(variance(numbers(c, a, 0, a.size(), true), false))));
        raw(r, "VAR.S", STATISTICAL, 1, 255, (c, a) -> num(variance(numbers(c, a, 0), true)));
        raw(r, "VAR.P", STATISTICAL, 1, 255, (c, a) -> num(variance(numbers(c, a, 0), false)));
        raw(r, "VARA", STATISTICAL, 1, 255, (c, a) -> num(variance(numbers(c, a, 0, a.size(), true), true)));
        raw(r, "VARPA", STATISTICAL, 1, 255, (c, a) -> num(variance(numbers(c, a, 0, a.size(), true), false)));
        raw(r, "AVEDEV", STATISTICAL, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0); double m = l.mean(), s = 0; for (int k = 0; k < l.size(); k++) s += Math.abs(l.get(k) - m); return num(s / l.size()); });
        raw(r, "DEVSQ", STATISTICAL, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0); if (l.size() == 0) throw EvalError.num(); double m = l.mean(), s = 0; for (int k = 0; k < l.size(); k++) s += Math.pow(l.get(k) - m, 2); return num(s); });
        raw(r, "GEOMEAN", STATISTICAL, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0); if (l.size() == 0) throw EvalError.num(); double s = 0; for (int k = 0; k < l.size(); k++) { if (l.get(k) <= 0) throw EvalError.num(); s += Math.log(l.get(k)); } return num(Math.exp(s / l.size())); });
        raw(r, "HARMEAN", STATISTICAL, 1, 255, (c, a) -> { DoubleList l = numbers(c, a, 0); if (l.size() == 0) throw EvalError.num(); double s = 0; for (int k = 0; k < l.size(); k++) { if (l.get(k) <= 0) throw EvalError.num(); s += 1 / l.get(k); } return num(l.size() / s); });
        raw(r, "TRIMMEAN", STATISTICAL, 2, 2, (c, a) -> {
            double[] s = numbersOf(c, a.value(0), false).sorted();
            double p = a.number(1);
            if (p < 0 || p >= 1 || s.length == 0) throw EvalError.num();
            int trim = (int) Math.floor(s.length * p / 2);
            double sum = 0;
            for (int k = trim; k < s.length - trim; k++) sum += s[k];
            return num(sum / (s.length - 2 * trim));
        });
        raw(r, "KURT", STATISTICAL, 1, 255, (c, a) -> {
            DoubleList l = numbers(c, a, 0);
            int n = l.size();
            if (n < 4) throw EvalError.div0();
            double m = l.mean(), sd = Math.sqrt(variance(l, true)), s = 0;
            if (sd == 0) throw EvalError.div0();
            for (int k = 0; k < n; k++) s += Math.pow((l.get(k) - m) / sd, 4);
            return num((double) n * (n + 1) / ((n - 1.0) * (n - 2) * (n - 3)) * s - 3.0 * (n - 1) * (n - 1) / ((n - 2.0) * (n - 3)));
        });
        raw(r, "SKEW", STATISTICAL, 1, 255, (c, a) -> {
            DoubleList l = numbers(c, a, 0);
            int n = l.size();
            if (n < 3) throw EvalError.div0();
            double m = l.mean(), sd = Math.sqrt(variance(l, true)), s = 0;
            if (sd == 0) throw EvalError.div0();
            for (int k = 0; k < n; k++) s += Math.pow((l.get(k) - m) / sd, 3);
            return num(n / ((n - 1.0) * (n - 2)) * s);
        });
        raw(r, "SKEW.P", STATISTICAL, 1, 255, (c, a) -> {
            DoubleList l = numbers(c, a, 0);
            int n = l.size();
            if (n < 1) throw EvalError.div0();
            double m = l.mean(), sd = Math.sqrt(variance(l, false)), s = 0;
            if (sd == 0) throw EvalError.div0();
            for (int k = 0; k < n; k++) s += Math.pow((l.get(k) - m) / sd, 3);
            return num(s / n);
        });
        raw(r, "LARGE", STATISTICAL, 2, 2, (c, a) -> kth(c, a, true));
        raw(r, "SMALL", STATISTICAL, 2, 2, (c, a) -> kth(c, a, false));
        raw(r, "PERCENTILE.INC", STATISTICAL, 2, 2, (c, a) -> liftK(c, a, 1, p -> num(percentile(numbersOf(c, a.value(0), false).sorted(), p, false))));
        raw(r, "PERCENTILE.EXC", STATISTICAL, 2, 2, (c, a) -> liftK(c, a, 1, p -> num(percentile(numbersOf(c, a.value(0), false).sorted(), p, true))));
        raw(r, "QUARTILE.INC", STATISTICAL, 2, 2, (c, a) -> liftK(c, a, 1, q -> { int qi = (int) q; if (qi < 0 || qi > 4) throw EvalError.num(); return num(percentile(numbersOf(c, a.value(0), false).sorted(), qi / 4.0, false)); }));
        raw(r, "QUARTILE.EXC", STATISTICAL, 2, 2, (c, a) -> liftK(c, a, 1, q -> { int qi = (int) q; if (qi < 1 || qi > 3) throw EvalError.num(); return num(percentile(numbersOf(c, a.value(0), false).sorted(), qi / 4.0, true)); }));
        raw(r, "PERCENTRANK.INC", STATISTICAL, 2, 3, (c, a) -> num(percentRank(numbersOf(c, a.value(0), false).sorted(), a.number(1), a.integer(2, 3), false)));
        raw(r, "PERCENTRANK.EXC", STATISTICAL, 2, 3, (c, a) -> num(percentRank(numbersOf(c, a.value(0), false).sorted(), a.number(1), a.integer(2, 3), true)));
        raw(r, "RANK.EQ", STATISTICAL, 2, 3, (c, a) -> liftK(c, a, 0, x -> num(rank(numbersOf(c, a.value(1), false).toArray(), x, a.bool(2, false), false))));
        raw(r, "RANK.AVG", STATISTICAL, 2, 3, (c, a) -> liftK(c, a, 0, x -> num(rank(numbersOf(c, a.value(1), false).toArray(), x, a.bool(2, false), true))));
        raw(r, "FREQUENCY", STATISTICAL, 2, 2, (c, a) -> {
            double[] data = numbersOf(c, a.value(0), false).toArray();
            double[] bins = numbersOf(c, a.value(1), false).toArray();
            int[] order = sortedIndexes(bins);
            double[] counts = new double[bins.length + 1];
            for (double d : data) {
                int slot = bins.length;
                for (int k : order) if (d <= bins[k]) { slot = k; break; }
                counts[slot]++;
            }
            List<CellValue> out = new ArrayList<>();
            for (double v : counts) out.add(num(v));
            return ArrayValue.column(out);
        });
        raw(r, "CORREL", STATISTICAL, 2, 2, (c, a) -> num(correlation(pairs(c, a.value(0), a.value(1)))));
        raw(r, "PEARSON", STATISTICAL, 2, 2, (c, a) -> num(correlation(pairs(c, a.value(0), a.value(1)))));
        raw(r, "RSQ", STATISTICAL, 2, 2, (c, a) -> { double rr = correlation(pairs(c, a.value(0), a.value(1))); return num(rr * rr); });
        raw(r, "COVARIANCE.P", STATISTICAL, 2, 2, (c, a) -> num(covariance(pairs(c, a.value(0), a.value(1)), false)));
        raw(r, "COVARIANCE.S", STATISTICAL, 2, 2, (c, a) -> num(covariance(pairs(c, a.value(0), a.value(1)), true)));
        raw(r, "SLOPE", STATISTICAL, 2, 2, (c, a) -> num(regression(pairs(c, a.value(1), a.value(0)))[0]));
        raw(r, "INTERCEPT", STATISTICAL, 2, 2, (c, a) -> num(regression(pairs(c, a.value(1), a.value(0)))[1]));
        raw(r, "STEYX", STATISTICAL, 2, 2, (c, a) -> {
            double[][] p = pairs(c, a.value(1), a.value(0));
            int n = p[0].length;
            if (n < 3) throw EvalError.div0();
            double[] reg = regression(p);
            double s = 0;
            for (int k = 0; k < n; k++) { double e = p[1][k] - (reg[0] * p[0][k] + reg[1]); s += e * e; }
            return num(Math.sqrt(s / (n - 2)));
        });
        raw(r, "FORECAST.LINEAR", STATISTICAL, 3, 3, StatisticalFunctions::forecast);
        raw(r, "FORECAST", STATISTICAL, 3, 3, StatisticalFunctions::forecast);
        raw(r, "TREND", STATISTICAL, 1, 4, (c, a) -> trend(c, a, false));
        raw(r, "GROWTH", STATISTICAL, 1, 4, (c, a) -> trend(c, a, true));
        raw(r, "LINEST", STATISTICAL, 1, 4, (c, a) -> linest(c, a, false));
        raw(r, "LOGEST", STATISTICAL, 1, 4, (c, a) -> linest(c, a, true));
        raw(r, "FORECAST.ETS", STATISTICAL, 3, 6, (c, a) -> num(Ets.forecast(c, a)));
        raw(r, "FORECAST.ETS.CONFINT", STATISTICAL, 3, 7, (c, a) -> num(Ets.confidence(c, a)));
        raw(r, "FORECAST.ETS.SEASONALITY", STATISTICAL, 2, 4, (c, a) -> num(Ets.seasonality(c, a)));
        raw(r, "FORECAST.ETS.STAT", STATISTICAL, 3, 6, (c, a) -> num(Ets.statistic(c, a)));
        raw(r, "PROB", STATISTICAL, 3, 4, (c, a) -> {
            double[][] p = pairs(c, a.value(0), a.value(1));
            double total = 0;
            for (double v : p[1]) { if (v < 0 || v > 1) throw EvalError.num(); total += v; }
            if (Math.abs(total - 1) > 1e-9) throw EvalError.num();
            double lo = a.number(2), hi = a.number(3, lo), s = 0;
            for (int k = 0; k < p[0].length; k++) if (p[0][k] >= lo && p[0][k] <= hi) s += p[1][k];
            return num(s);
        });
        scalar(r, "STANDARDIZE", STATISTICAL, 3, 3, (c, a) -> { double sd = n(a, 2); if (sd <= 0) throw EvalError.num(); return num((n(a, 0) - n(a, 1)) / sd); });
        scalar(r, "FISHER", STATISTICAL, 1, 1, (c, a) -> { double x = n(a, 0); if (x <= -1 || x >= 1) throw EvalError.num(); return num(0.5 * Math.log((1 + x) / (1 - x))); });
        scalar(r, "FISHERINV", STATISTICAL, 1, 1, (c, a) -> { double y = n(a, 0); return num((Math.exp(2 * y) - 1) / (Math.exp(2 * y) + 1)); });
        scalar(r, "PERMUT", STATISTICAL, 2, 2, (c, a) -> { double nn = Math.floor(n(a, 0)), k = Math.floor(n(a, 1)); if (nn < 0 || k < 0 || k > nn) throw EvalError.num(); double p = 1; for (int x = 0; x < k; x++) p *= nn - x; return num(p); });
        scalar(r, "PERMUTATIONA", STATISTICAL, 2, 2, (c, a) -> { double nn = Math.floor(n(a, 0)), k = Math.floor(n(a, 1)); if (nn < 0 || k < 0) throw EvalError.num(); return num(Math.pow(nn, k)); });
        scalar(r, "GAMMA", STATISTICAL, 1, 1, (c, a) -> { double x = n(a, 0); if (x <= 0 && x == Math.rint(x)) throw EvalError.num(); return num(SpecialFunctions.gamma(x)); });
        scalar(r, "GAMMALN", STATISTICAL, 1, 1, (c, a) -> num(SpecialFunctions.logGamma(n(a, 0))));
        scalar(r, "GAMMALN.PRECISE", STATISTICAL, 1, 1, (c, a) -> num(SpecialFunctions.logGamma(n(a, 0))));
        scalar(r, "GAUSS", STATISTICAL, 1, 1, (c, a) -> num(SpecialFunctions.normalCdf(n(a, 0)) - 0.5));
        scalar(r, "PHI", STATISTICAL, 1, 1, (c, a) -> num(SpecialFunctions.normalPdf(n(a, 0))));
        scalar(r, "NORM.DIST", STATISTICAL, 4, 4, (c, a) -> {
            double x = n(a, 0), mean = n(a, 1), sd = n(a, 2);
            if (sd <= 0) throw EvalError.num();
            double z = (x - mean) / sd;
            return num(b(a, 3) ? SpecialFunctions.normalCdf(z) : SpecialFunctions.normalPdf(z) / sd);
        });
        scalar(r, "NORM.INV", STATISTICAL, 3, 3, (c, a) -> { double p = n(a, 0), sd = n(a, 2); if (sd <= 0 || p <= 0 || p >= 1) throw EvalError.num(); return num(n(a, 1) + sd * SpecialFunctions.normalInverse(p)); });
        scalar(r, "NORM.S.DIST", STATISTICAL, 2, 2, (c, a) -> num(b(a, 1) ? SpecialFunctions.normalCdf(n(a, 0)) : SpecialFunctions.normalPdf(n(a, 0))));
        scalar(r, "NORM.S.INV", STATISTICAL, 1, 1, (c, a) -> num(SpecialFunctions.normalInverse(n(a, 0))));
        scalar(r, "LOGNORM.DIST", STATISTICAL, 4, 4, (c, a) -> {
            double x = n(a, 0), mean = n(a, 1), sd = n(a, 2);
            if (x <= 0 || sd <= 0) throw EvalError.num();
            double z = (Math.log(x) - mean) / sd;
            return num(b(a, 3) ? SpecialFunctions.normalCdf(z) : SpecialFunctions.normalPdf(z) / (x * sd));
        });
        scalar(r, "LOGNORM.INV", STATISTICAL, 3, 3, (c, a) -> { double p = n(a, 0), sd = n(a, 2); if (p <= 0 || p >= 1 || sd <= 0) throw EvalError.num(); return num(Math.exp(n(a, 1) + sd * SpecialFunctions.normalInverse(p))); });
        scalar(r, "EXPON.DIST", STATISTICAL, 3, 3, (c, a) -> {
            double x = n(a, 0), l = n(a, 1);
            if (x < 0 || l <= 0) throw EvalError.num();
            return num(b(a, 2) ? 1 - Math.exp(-l * x) : l * Math.exp(-l * x));
        });
        scalar(r, "WEIBULL.DIST", STATISTICAL, 4, 4, (c, a) -> {
            double x = n(a, 0), alpha = n(a, 1), beta = n(a, 2);
            if (x < 0 || alpha <= 0 || beta <= 0) throw EvalError.num();
            return num(b(a, 3) ? 1 - Math.exp(-Math.pow(x / beta, alpha)) : alpha / Math.pow(beta, alpha) * Math.pow(x, alpha - 1) * Math.exp(-Math.pow(x / beta, alpha)));
        });
        scalar(r, "GAMMA.DIST", STATISTICAL, 4, 4, (c, a) -> {
            double x = n(a, 0), alpha = n(a, 1), beta = n(a, 2);
            if (x < 0 || alpha <= 0 || beta <= 0) throw EvalError.num();
            return num(b(a, 3) ? SpecialFunctions.gammaCdf(x, alpha, beta) : SpecialFunctions.gammaPdf(x, alpha, beta));
        });
        scalar(r, "GAMMA.INV", STATISTICAL, 3, 3, (c, a) -> {
            double p = n(a, 0), alpha = n(a, 1), beta = n(a, 2);
            if (p < 0 || p >= 1 || alpha <= 0 || beta <= 0) throw EvalError.num();
            return num(SpecialFunctions.invert(x -> SpecialFunctions.gammaCdf(x, alpha, beta), p, 0, Math.max(1, alpha * beta * 4)));
        });
        scalar(r, "BETA.DIST", STATISTICAL, 4, 6, (c, a) -> {
            double x = n(a, 0), alpha = n(a, 1), beta = n(a, 2), lo = n(a, 4, 0), hi = n(a, 5, 1);
            if (alpha <= 0 || beta <= 0 || x < lo || x > hi || lo == hi) throw EvalError.num();
            double z = (x - lo) / (hi - lo);
            return num(b(a, 3) ? SpecialFunctions.betaCdf(z, alpha, beta) : SpecialFunctions.betaPdf(z, alpha, beta) / (hi - lo));
        });
        scalar(r, "BETA.INV", STATISTICAL, 3, 5, (c, a) -> {
            double p = n(a, 0), alpha = n(a, 1), beta = n(a, 2), lo = n(a, 3, 0), hi = n(a, 4, 1);
            if (p <= 0 || p > 1 || alpha <= 0 || beta <= 0 || lo >= hi) throw EvalError.num();
            double z = bisect(x -> SpecialFunctions.betaCdf(x, alpha, beta), p, 0, 1);
            return num(lo + z * (hi - lo));
        });
        scalar(r, "CHISQ.DIST", STATISTICAL, 3, 3, (c, a) -> {
            double x = n(a, 0), df = Math.floor(n(a, 1));
            if (x < 0 || df < 1) throw EvalError.num();
            return num(b(a, 2) ? SpecialFunctions.chiSquareCdf(x, df) : SpecialFunctions.chiSquarePdf(x, df));
        });
        scalar(r, "CHISQ.DIST.RT", STATISTICAL, 2, 2, (c, a) -> { double x = n(a, 0), df = Math.floor(n(a, 1)); if (x < 0 || df < 1) throw EvalError.num(); return num(1 - SpecialFunctions.chiSquareCdf(x, df)); });
        scalar(r, "CHISQ.INV", STATISTICAL, 2, 2, (c, a) -> { double p = n(a, 0), df = Math.floor(n(a, 1)); if (p < 0 || p >= 1 || df < 1) throw EvalError.num(); return num(SpecialFunctions.invert(x -> SpecialFunctions.chiSquareCdf(x, df), p, 0, df * 2 + 10)); });
        scalar(r, "CHISQ.INV.RT", STATISTICAL, 2, 2, (c, a) -> { double p = n(a, 0), df = Math.floor(n(a, 1)); if (p <= 0 || p > 1 || df < 1) throw EvalError.num(); return num(SpecialFunctions.invert(x -> SpecialFunctions.chiSquareCdf(x, df), 1 - p, 0, df * 2 + 10)); });
        raw(r, "CHISQ.TEST", STATISTICAL, 2, 2, (c, a) -> {
            ArrayValue obs = c.toArray(a.value(0)), exp = c.toArray(a.value(1));
            if (obs.rows() != exp.rows() || obs.columns() != exp.columns()) throw EvalError.na();
            double chi = 0;
            for (int k = 0; k < obs.size(); k++) { double o = Coerce.number(obs.at(k)), e = Coerce.number(exp.at(k)); if (e == 0) throw EvalError.div0(); chi += (o - e) * (o - e) / e; }
            int df = obs.rows() > 1 && obs.columns() > 1 ? (obs.rows() - 1) * (obs.columns() - 1) : obs.size() - 1;
            if (df < 1) throw EvalError.na();
            return num(1 - SpecialFunctions.chiSquareCdf(chi, df));
        });
        scalar(r, "T.DIST", STATISTICAL, 3, 3, (c, a) -> {
            double x = n(a, 0), df = Math.floor(n(a, 1));
            if (df < 1) throw EvalError.num();
            return num(b(a, 2) ? SpecialFunctions.studentCdf(x, df) : SpecialFunctions.studentPdf(x, df));
        });
        scalar(r, "T.DIST.2T", STATISTICAL, 2, 2, (c, a) -> { double x = n(a, 0), df = Math.floor(n(a, 1)); if (x < 0 || df < 1) throw EvalError.num(); return num(2 * (1 - SpecialFunctions.studentCdf(x, df))); });
        scalar(r, "T.DIST.RT", STATISTICAL, 2, 2, (c, a) -> { double x = n(a, 0), df = Math.floor(n(a, 1)); if (df < 1) throw EvalError.num(); return num(1 - SpecialFunctions.studentCdf(x, df)); });
        scalar(r, "T.INV", STATISTICAL, 2, 2, (c, a) -> { double p = n(a, 0), df = Math.floor(n(a, 1)); if (p <= 0 || p >= 1 || df < 1) throw EvalError.num(); return num(tInverse(p, df)); });
        scalar(r, "T.INV.2T", STATISTICAL, 2, 2, (c, a) -> { double p = n(a, 0), df = Math.floor(n(a, 1)); if (p <= 0 || p > 1 || df < 1) throw EvalError.num(); return num(Math.abs(tInverse(p / 2, df))); });
        raw(r, "T.TEST", STATISTICAL, 4, 4, (c, a) -> num(tTest(c, a)));
        scalar(r, "F.DIST", STATISTICAL, 4, 4, (c, a) -> {
            double x = n(a, 0), d1 = Math.floor(n(a, 1)), d2 = Math.floor(n(a, 2));
            if (x < 0 || d1 < 1 || d2 < 1) throw EvalError.num();
            return num(b(a, 3) ? SpecialFunctions.fCdf(x, d1, d2) : SpecialFunctions.fPdf(x, d1, d2));
        });
        scalar(r, "F.DIST.RT", STATISTICAL, 3, 3, (c, a) -> { double x = n(a, 0), d1 = Math.floor(n(a, 1)), d2 = Math.floor(n(a, 2)); if (x < 0 || d1 < 1 || d2 < 1) throw EvalError.num(); return num(1 - SpecialFunctions.fCdf(x, d1, d2)); });
        scalar(r, "F.INV", STATISTICAL, 3, 3, (c, a) -> { double p = n(a, 0), d1 = Math.floor(n(a, 1)), d2 = Math.floor(n(a, 2)); if (p < 0 || p > 1 || d1 < 1 || d2 < 1) throw EvalError.num(); return num(SpecialFunctions.invert(x -> SpecialFunctions.fCdf(x, d1, d2), p, 0, 10)); });
        scalar(r, "F.INV.RT", STATISTICAL, 3, 3, (c, a) -> { double p = n(a, 0), d1 = Math.floor(n(a, 1)), d2 = Math.floor(n(a, 2)); if (p < 0 || p > 1 || d1 < 1 || d2 < 1) throw EvalError.num(); return num(SpecialFunctions.invert(x -> SpecialFunctions.fCdf(x, d1, d2), 1 - p, 0, 10)); });
        raw(r, "F.TEST", STATISTICAL, 2, 2, (c, a) -> {
            DoubleList x = numbersOf(c, a.value(0), false), y = numbersOf(c, a.value(1), false);
            if (x.size() < 2 || y.size() < 2) throw EvalError.div0();
            double vx = variance(x, true), vy = variance(y, true);
            if (vx == 0 || vy == 0) throw EvalError.div0();
            double f = vx / vy, d1 = x.size() - 1, d2 = y.size() - 1;
            double p = SpecialFunctions.fCdf(f, d1, d2);
            return num(2 * Math.min(p, 1 - p));
        });
        scalar(r, "BINOM.DIST", STATISTICAL, 4, 4, (c, a) -> {
            double k = Math.floor(n(a, 0)), nn = Math.floor(n(a, 1)), p = n(a, 2);
            if (k < 0 || k > nn || p < 0 || p > 1) throw EvalError.num();
            return num(b(a, 3) ? SpecialFunctions.binomialCdf(k, nn, p) : SpecialFunctions.binomialPmf(k, nn, p));
        });
        scalar(r, "BINOM.DIST.RANGE", STATISTICAL, 3, 4, (c, a) -> {
            double nn = Math.floor(n(a, 0)), p = n(a, 1), s1 = Math.floor(n(a, 2)), s2 = given(a, 3) ? Math.floor(n(a, 3)) : s1;
            if (p < 0 || p > 1 || s1 < 0 || s1 > nn || s2 < s1 || s2 > nn) throw EvalError.num();
            double s = 0;
            for (double k = s1; k <= s2; k++) s += SpecialFunctions.binomialPmf(k, nn, p);
            return num(s);
        });
        scalar(r, "BINOM.INV", STATISTICAL, 3, 3, (c, a) -> {
            double nn = Math.floor(n(a, 0)), p = n(a, 1), alpha = n(a, 2);
            if (nn < 0 || p < 0 || p > 1 || alpha < 0 || alpha > 1) throw EvalError.num();
            double s = 0;
            for (int k = 0; k <= nn; k++) { s += SpecialFunctions.binomialPmf(k, nn, p); if (s >= alpha - 1e-12) return num(k); }
            return num(nn);
        });
        scalar(r, "NEGBINOM.DIST", STATISTICAL, 4, 4, (c, a) -> {
            double f = Math.floor(n(a, 0)), s = Math.floor(n(a, 1)), p = n(a, 2);
            if (f < 0 || s < 1 || p < 0 || p > 1) throw EvalError.num();
            if (!b(a, 3)) return num(SpecialFunctions.combin(f + s - 1, s - 1) * Math.pow(p, s) * Math.pow(1 - p, f));
            return num(SpecialFunctions.regularizedBeta(p, s, f + 1));
        });
        scalar(r, "POISSON.DIST", STATISTICAL, 3, 3, (c, a) -> {
            double x = Math.floor(n(a, 0)), l = n(a, 1);
            if (x < 0 || l < 0) throw EvalError.num();
            if (!b(a, 2)) return num(SpecialFunctions.poissonPmf(x, l));
            return num(SpecialFunctions.upperGammaQ(x + 1, l));
        });
        scalar(r, "HYPGEOM.DIST", STATISTICAL, 5, 5, (c, a) -> {
            double k = Math.floor(n(a, 0)), nn = Math.floor(n(a, 1)), K = Math.floor(n(a, 2)), N = Math.floor(n(a, 3));
            if (k < 0 || k > nn || k > K || nn > N || K > N || nn < 1 || K < 1 || N < 1) throw EvalError.num();
            if (!b(a, 4)) return num(hypergeom(k, nn, K, N));
            double s = 0;
            for (double x = 0; x <= k; x++) s += hypergeom(x, nn, K, N);
            return num(s);
        });
        scalar(r, "CONFIDENCE.NORM", STATISTICAL, 3, 3, (c, a) -> {
            double alpha = n(a, 0), sd = n(a, 1), size = Math.floor(n(a, 2));
            if (alpha <= 0 || alpha >= 1 || sd <= 0 || size < 1) throw EvalError.num();
            return num(-SpecialFunctions.normalInverse(alpha / 2) * sd / Math.sqrt(size));
        });
        scalar(r, "CONFIDENCE.T", STATISTICAL, 3, 3, (c, a) -> {
            double alpha = n(a, 0), sd = n(a, 1), size = Math.floor(n(a, 2));
            if (alpha <= 0 || alpha >= 1 || sd <= 0 || size < 2) throw EvalError.num();
            return num(Math.abs(tInverse(alpha / 2, size - 1)) * sd / Math.sqrt(size));
        });
        raw(r, "Z.TEST", STATISTICAL, 2, 3, (c, a) -> {
            DoubleList l = numbersOf(c, a.value(0), false);
            double x = a.number(1);
            double sd = a.has(2) ? a.number(2) : Math.sqrt(variance(l, true));
            return num(1 - SpecialFunctions.normalCdf((l.mean() - x) / (sd / Math.sqrt(l.size()))));
        });
    }

    static double median(double[] sorted) {
        int n = sorted.length;
        if (n == 0) throw EvalError.num();
        return n % 2 == 1 ? sorted[n / 2] : (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
    }

    static CellValue mode(double[] values) {
        Map<Double, Integer> counts = new LinkedHashMap<>();
        for (double v : values) counts.merge(v, 1, Integer::sum);
        double best = 0;
        int bestCount = 1;
        for (Map.Entry<Double, Integer> e : counts.entrySet()) if (e.getValue() > bestCount) { best = e.getKey(); bestCount = e.getValue(); }
        if (bestCount < 2) throw EvalError.na();
        return num(best);
    }

    private static double[] ordered(FunctionContext c, FunctionArgs a, int from) {
        DoubleList l = new DoubleList();
        for (int i = from; i < a.size(); i++) {
            CellValue v = a.value(i);
            if (v instanceof ReferenceValue || v instanceof ArrayValue) {
                for (CellValue x : c.toArray(v).list()) {
                    if (x instanceof ErrorValue e) throw EvalError.of(e.error());
                    if (x instanceof NumberValue n) l.add(n.value());
                }
            } else collect(c, v, l, false, true);
        }
        return l.toArray();
    }

    private static long count(FunctionContext c, FunctionArgs a, boolean all) {
        long n = 0;
        for (int i = 0; i < a.size(); i++) {
            CellValue v = a.value(i);
            if (v instanceof ReferenceValue ref) {
                long[] k = {0};
                c.forEachCell(ref, (s, row, col, value) -> { if (all || value instanceof NumberValue) k[0]++; });
                n += k[0];
                continue;
            }
            if (v instanceof ArrayValue arr) {
                for (CellValue x : arr.list()) if (all ? !x.isEmpty() : x instanceof NumberValue) n++;
                continue;
            }
            if (v instanceof OmittedValue) { if (all) n++; continue; }
            if (all) { if (!v.isEmpty()) n++; continue; }
            if (v instanceof NumberValue || v instanceof BoolValue) n++;
            else if (v instanceof TextValue t && Coerce.tryNumber(t) != null) n++;
        }
        return n;
    }

    private static DoubleList ifsValues(FunctionContext c, FunctionArgs a) {
        ArrayValue values = c.toArray(a.value(0));
        boolean[] mask = MathFunctions.ifsMask(c, a, 1, values.rows(), values.columns());
        DoubleList l = new DoubleList();
        for (int k = 0; k < values.size(); k++) if (mask[k]) { CellValue v = values.at(k); if (v instanceof ErrorValue e) throw EvalError.of(e.error()); if (v instanceof NumberValue n) l.add(n.value()); }
        return l;
    }

    private static CellValue liftK(FunctionContext c, FunctionArgs a, int index, KOperation op) {
        CellValue k = c.deref(a.value(index));
        if (k instanceof ArrayValue arr) return arr.map(x -> { try { return op.apply(Coerce.number(x)); } catch (EvalError e) { return e.toValue(); } });
        if (k instanceof ErrorValue) return k;
        return op.apply(Coerce.number(k));
    }

    private static CellValue kth(FunctionContext c, FunctionArgs a, boolean largest) {
        double[] sorted = numbersOf(c, a.value(0), false).sorted();
        CellValue kv = c.deref(a.value(1));
        if (kv instanceof ArrayValue arr) return arr.map(x -> { try { return num(pick(sorted, Coerce.number(x), largest)); } catch (EvalError e) { return e.toValue(); } });
        return num(pick(sorted, Coerce.number(kv), largest));
    }

    private static double pick(double[] sorted, double kd, boolean largest) {
        int k = (int) Math.ceil(kd);
        if (k < 1 || k > sorted.length) throw EvalError.num();
        return largest ? sorted[sorted.length - k] : sorted[k - 1];
    }

    static double percentRank(double[] sorted, double x, int significance, boolean exclusive) {
        int n = sorted.length;
        if (n == 0 || significance < 1) throw EvalError.num();
        if (x < sorted[0] || x > sorted[n - 1]) throw EvalError.na();
        double rank;
        int lessCount = 0;
        while (lessCount < n && sorted[lessCount] < x) lessCount++;
        if (lessCount < n && sorted[lessCount] == x) rank = lessCount;
        else {
            double lo = sorted[lessCount - 1], hi = sorted[lessCount];
            rank = lessCount - 1 + (x - lo) / (hi - lo);
        }
        double result = exclusive ? (rank + 1) / (n + 1) : n == 1 ? 1 : rank / (n - 1);
        double f = Math.pow(10, significance);
        return Math.floor(result * f + 1e-9) / f;
    }

    static double rank(double[] values, double x, boolean ascending, boolean average) {
        int less = 0, equal = 0;
        for (double v : values) { if (ascending ? v < x : v > x) less++; else if (v == x) equal++; }
        if (equal == 0) throw EvalError.na();
        return average ? less + (equal + 1) / 2.0 : less + 1;
    }

    static double[][] pairs(FunctionContext c, CellValue x, CellValue y) {
        ArrayValue ax = c.toArray(x), ay = c.toArray(y);
        if (ax.size() != ay.size()) throw EvalError.na();
        DoubleList lx = new DoubleList(), ly = new DoubleList();
        for (int k = 0; k < ax.size(); k++) {
            CellValue u = ax.at(k), v = ay.at(k);
            if (u instanceof ErrorValue e) throw EvalError.of(e.error());
            if (v instanceof ErrorValue e) throw EvalError.of(e.error());
            if (u instanceof NumberValue nu && v instanceof NumberValue nv) { lx.add(nu.value()); ly.add(nv.value()); }
        }
        return new double[][]{lx.toArray(), ly.toArray()};
    }

    static double correlation(double[][] p) {
        int n = p[0].length;
        if (n < 2) throw EvalError.div0();
        double mx = 0, my = 0;
        for (int k = 0; k < n; k++) { mx += p[0][k]; my += p[1][k]; }
        mx /= n; my /= n;
        double sxy = 0, sxx = 0, syy = 0;
        for (int k = 0; k < n; k++) { double dx = p[0][k] - mx, dy = p[1][k] - my; sxy += dx * dy; sxx += dx * dx; syy += dy * dy; }
        if (sxx == 0 || syy == 0) throw EvalError.div0();
        return sxy / Math.sqrt(sxx * syy);
    }

    static double covariance(double[][] p, boolean sample) {
        int n = p[0].length;
        if (n == 0 || sample && n < 2) throw EvalError.div0();
        double mx = 0, my = 0;
        for (int k = 0; k < n; k++) { mx += p[0][k]; my += p[1][k]; }
        mx /= n; my /= n;
        double s = 0;
        for (int k = 0; k < n; k++) s += (p[0][k] - mx) * (p[1][k] - my);
        return s / (sample ? n - 1 : n);
    }

    static double[] regression(double[][] p) {
        int n = p[0].length;
        if (n == 0) throw EvalError.div0();
        double mx = 0, my = 0;
        for (int k = 0; k < n; k++) { mx += p[0][k]; my += p[1][k]; }
        mx /= n; my /= n;
        double sxy = 0, sxx = 0;
        for (int k = 0; k < n; k++) { sxy += (p[0][k] - mx) * (p[1][k] - my); sxx += (p[0][k] - mx) * (p[0][k] - mx); }
        if (sxx == 0) throw EvalError.div0();
        double slope = sxy / sxx;
        return new double[]{slope, my - slope * mx};
    }

    private static CellValue forecast(FunctionContext c, FunctionArgs a) {
        double[] reg = regression(pairs(c, a.value(2), a.value(1)));
        CellValue x = c.deref(a.value(0));
        if (x instanceof ArrayValue arr) return arr.map(v -> { try { return num(reg[0] * Coerce.number(v) + reg[1]); } catch (EvalError e) { return e.toValue(); } });
        return num(reg[0] * Coerce.number(x) + reg[1]);
    }

    private static CellValue trend(FunctionContext c, FunctionArgs a, boolean exponential) {
        ArrayValue ys = c.toArray(a.value(0));
        ArrayValue xs = a.has(1) ? c.toArray(a.value(1)) : sequence(ys);
        ArrayValue nx = a.has(2) ? c.toArray(a.value(2)) : xs;
        boolean constant = a.bool(3, true);
        double[] y = toDoubles(ys);
        if (exponential) for (int k = 0; k < y.length; k++) { if (y[k] <= 0) throw EvalError.num(); y[k] = Math.log(y[k]); }
        int predictors = xs.size() / y.length;
        double[][] x = design(xs, y.length, predictors);
        double[] coef = leastSquares(x, y, constant);
        ArrayValue out = ArrayValue.of(nx.size() / predictors == nx.rows() ? nx.rows() : nx.rows(), predictors == 1 ? nx.columns() : 1);
        int points = nx.size() / predictors;
        double[][] newX = design(nx, points, predictors);
        List<CellValue> results = new ArrayList<>();
        for (int k = 0; k < points; k++) {
            double v = constant ? coef[predictors] : 0;
            for (int j = 0; j < predictors; j++) v += coef[j] * newX[k][j];
            results.add(num(exponential ? Math.exp(v) : v));
        }
        if (predictors == 1 && nx.rows() * nx.columns() == points) {
            ArrayValue shaped = ArrayValue.of(nx.rows(), nx.columns());
            for (int k = 0; k < points; k++) shaped.set(k / nx.columns(), k % nx.columns(), results.get(k));
            return shaped;
        }
        return out.rows() == 0 ? ArrayValue.column(results) : ArrayValue.column(results);
    }

    private static CellValue linest(FunctionContext c, FunctionArgs a, boolean exponential) {
        ArrayValue ys = c.toArray(a.value(0));
        ArrayValue xs = a.has(1) ? c.toArray(a.value(1)) : sequence(ys);
        boolean constant = a.bool(2, true), stats = a.bool(3, false);
        double[] y = toDoubles(ys);
        if (exponential) for (int k = 0; k < y.length; k++) { if (y[k] <= 0) throw EvalError.num(); y[k] = Math.log(y[k]); }
        int predictors = xs.size() / y.length;
        double[][] x = design(xs, y.length, predictors);
        double[] coef = leastSquares(x, y, constant);
        int cols = predictors + 1;
        ArrayValue out = ArrayValue.of(stats ? 5 : 1, cols);
        for (int j = 0; j < predictors; j++) out.set(0, predictors - 1 - j, num(exponential ? Math.exp(coef[j]) : coef[j]));
        out.set(0, predictors, num(constant ? (exponential ? Math.exp(coef[predictors]) : coef[predictors]) : (exponential ? 1 : 0)));
        if (stats) {
            int n = y.length;
            double mean = 0;
            for (double v : y) mean += v;
            mean /= n;
            double ssres = 0, sstot = 0;
            for (int k = 0; k < n; k++) {
                double fit = constant ? coef[predictors] : 0;
                for (int j = 0; j < predictors; j++) fit += coef[j] * x[k][j];
                ssres += (y[k] - fit) * (y[k] - fit);
                sstot += constant ? (y[k] - mean) * (y[k] - mean) : y[k] * y[k];
            }
            int df = n - predictors - (constant ? 1 : 0);
            double r2 = sstot == 0 ? 1 : 1 - ssres / sstot;
            double sey = df > 0 ? Math.sqrt(ssres / df) : 0;
            double[][] cov = covarianceMatrix(x, constant, predictors, sey);
            for (int j = 0; j < predictors; j++) out.set(1, predictors - 1 - j, num(Math.sqrt(Math.max(0, cov[j][j]))));
            out.set(1, predictors, constant ? num(Math.sqrt(Math.max(0, cov[predictors][predictors]))) : err(CellError.NA));
            out.set(2, 0, num(r2));
            out.set(2, 1, num(sey));
            out.set(3, 0, df > 0 && ssres > 0 ? num((sstot - ssres) / predictors / (ssres / df)) : err(CellError.NUM));
            out.set(3, 1, num(df));
            out.set(4, 0, num(sstot - ssres));
            out.set(4, 1, num(ssres));
            for (int row = 2; row < 5; row++) for (int col = 2; col < cols; col++) out.set(row, col, err(CellError.NA));
        }
        return out;
    }

    private static double[][] covarianceMatrix(double[][] x, boolean constant, int p, double sey) {
        int m = p + (constant ? 1 : 0), n = x.length;
        double[][] xtx = new double[m][m];
        for (int k = 0; k < n; k++) {
            double[] row = new double[m];
            for (int j = 0; j < p; j++) row[j] = x[k][j];
            if (constant) row[p] = 1;
            for (int i = 0; i < m; i++) for (int j = 0; j < m; j++) xtx[i][j] += row[i] * row[j];
        }
        double[][] inv;
        try { inv = MathFunctions.inverse(xtx); } catch (EvalError e) { inv = new double[m][m]; }
        double[][] out = new double[p + 1][p + 1];
        for (int i = 0; i < m; i++) for (int j = 0; j < m; j++) out[i][j] = inv[i][j] * sey * sey;
        return out;
    }

    private static ArrayValue sequence(ArrayValue shape) {
        ArrayValue out = ArrayValue.of(shape.rows(), shape.columns());
        for (int k = 0; k < shape.size(); k++) out.set(k / shape.columns(), k % shape.columns(), num(k + 1));
        return out;
    }

    private static double[] toDoubles(ArrayValue a) {
        double[] d = new double[a.size()];
        for (int k = 0; k < d.length; k++) d[k] = Coerce.number(a.at(k));
        return d;
    }

    private static double[][] design(ArrayValue xs, int n, int predictors) {
        double[][] x = new double[n][predictors];
        boolean byColumns = xs.rows() == n;
        for (int k = 0; k < n; k++) for (int j = 0; j < predictors; j++) x[k][j] = Coerce.number(predictors == 1 ? xs.at(k) : byColumns ? xs.get(k, j) : xs.get(j, k));
        return x;
    }

    private static double[] leastSquares(double[][] x, double[] y, boolean constant) {
        int n = y.length, p = x[0].length, m = p + (constant ? 1 : 0);
        double[][] xtx = new double[m][m];
        double[] xty = new double[m];
        for (int k = 0; k < n; k++) {
            double[] row = new double[m];
            for (int j = 0; j < p; j++) row[j] = x[k][j];
            if (constant) row[p] = 1;
            for (int i = 0; i < m; i++) { xty[i] += row[i] * y[k]; for (int j = 0; j < m; j++) xtx[i][j] += row[i] * row[j]; }
        }
        double[][] inv = MathFunctions.inverse(xtx);
        double[] coef = new double[p + 1];
        for (int i = 0; i < m; i++) { double s = 0; for (int j = 0; j < m; j++) s += inv[i][j] * xty[j]; coef[i] = s; }
        return coef;
    }

    static double tInverse(double p, double df) {
        if (p == 0.5) return 0;
        double x = SpecialFunctions.invert(t -> SpecialFunctions.studentCdf(t - 1e6, df), p, 0, 2e6) - 1e6;
        double lo = x - 1, hi = x + 1;
        return bisect(t -> SpecialFunctions.studentCdf(t, df), p, lo, hi);
    }

    static double bisect(Cdf cdf, double p, double lo, double hi) {
        for (int k = 0; k < 200; k++) {
            double m = (lo + hi) / 2;
            if (cdf.at(m) < p) lo = m; else hi = m;
        }
        return (lo + hi) / 2;
    }

    private static double tTest(FunctionContext c, FunctionArgs a) {
        int tails = a.integer(2), type = a.integer(3);
        if (tails < 1 || tails > 2 || type < 1 || type > 3) throw EvalError.num();
        double t, df;
        if (type == 1) {
            double[][] p = pairs(c, a.value(0), a.value(1));
            int n = p[0].length;
            DoubleList d = new DoubleList();
            for (int k = 0; k < n; k++) d.add(p[0][k] - p[1][k]);
            double sd = Math.sqrt(variance(d, true));
            t = d.mean() / (sd / Math.sqrt(n));
            df = n - 1;
        } else {
            DoubleList x = numbersOf(c, a.value(0), false), y = numbersOf(c, a.value(1), false);
            double vx = variance(x, true), vy = variance(y, true);
            int nx = x.size(), ny = y.size();
            if (type == 2) {
                double pooled = ((nx - 1) * vx + (ny - 1) * vy) / (nx + ny - 2);
                t = (x.mean() - y.mean()) / Math.sqrt(pooled * (1.0 / nx + 1.0 / ny));
                df = nx + ny - 2;
            } else {
                double sx = vx / nx, sy = vy / ny;
                t = (x.mean() - y.mean()) / Math.sqrt(sx + sy);
                df = (sx + sy) * (sx + sy) / (sx * sx / (nx - 1) + sy * sy / (ny - 1));
            }
        }
        double p = 1 - SpecialFunctions.studentCdf(Math.abs(t), df);
        return tails == 2 ? 2 * p : p;
    }

    private static double hypergeom(double k, double n, double K, double N) {
        return Math.exp(SpecialFunctions.logCombin(K, k) + SpecialFunctions.logCombin(N - K, n - k) - SpecialFunctions.logCombin(N, n));
    }

    static List<Double> asList(double[] d) { List<Double> l = new ArrayList<>(); for (double v : d) l.add(v); return l; }
    static double[] copy(double[] d) { return Arrays.copyOf(d, d.length); }
    static boolean isEmpty(CellValue v) { return v instanceof EmptyValue; }
}
