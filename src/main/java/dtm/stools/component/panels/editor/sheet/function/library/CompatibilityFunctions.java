package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.COMPATIBILITY;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class CompatibilityFunctions {
    private CompatibilityFunctions() {}

    static void register(FunctionRegistry r) {
        String[][] aliases = {
                {"BINOMDIST", "BINOM.DIST"}, {"CHIDIST", "CHISQ.DIST.RT"}, {"CHIINV", "CHISQ.INV.RT"}, {"CHITEST", "CHISQ.TEST"}, {"CONFIDENCE", "CONFIDENCE.NORM"},
                {"COVAR", "COVARIANCE.P"}, {"CRITBINOM", "BINOM.INV"}, {"EXPONDIST", "EXPON.DIST"}, {"FDIST", "F.DIST.RT"}, {"FINV", "F.INV.RT"}, {"FTEST", "F.TEST"},
                {"GAMMADIST", "GAMMA.DIST"}, {"GAMMAINV", "GAMMA.INV"}, {"LOGINV", "LOGNORM.INV"}, {"MODE", "MODE.SNGL"}, {"NORMDIST", "NORM.DIST"},
                {"NORMINV", "NORM.INV"}, {"NORMSINV", "NORM.S.INV"}, {"PERCENTILE", "PERCENTILE.INC"}, {"PERCENTRANK", "PERCENTRANK.INC"}, {"POISSON", "POISSON.DIST"},
                {"QUARTILE", "QUARTILE.INC"}, {"RANK", "RANK.EQ"}, {"STDEV", "STDEV.S"}, {"STDEVP", "STDEV.P"}, {"TINV", "T.INV.2T"}, {"TTEST", "T.TEST"},
                {"VAR", "VAR.S"}, {"VARP", "VAR.P"}, {"WEIBULL", "WEIBULL.DIST"}, {"ZTEST", "Z.TEST"}, {"CEILING.XCL", "CEILING.MATH"}, {"FORECAST.LINEAR2", "FORECAST.LINEAR"}
        };
        for (String[] a : aliases) r.alias(a[0], a[1]);
        scalar(r, "NORMSDIST", COMPATIBILITY, 1, 1, (c, a) -> num(SpecialFunctions.normalCdf(n(a, 0))));
        scalar(r, "LOGNORMDIST", COMPATIBILITY, 3, 3, (c, a) -> {
            double x = n(a, 0), sd = n(a, 2);
            if (x <= 0 || sd <= 0) throw EvalError.num();
            return num(SpecialFunctions.normalCdf((Math.log(x) - n(a, 1)) / sd));
        });
        scalar(r, "HYPGEOMDIST", COMPATIBILITY, 4, 4, (c, a) -> {
            double k = Math.floor(n(a, 0)), nn = Math.floor(n(a, 1)), K = Math.floor(n(a, 2)), N = Math.floor(n(a, 3));
            if (k < 0 || k > nn || k > K || nn > N || K > N) throw EvalError.num();
            return num(Math.exp(SpecialFunctions.logCombin(K, k) + SpecialFunctions.logCombin(N - K, nn - k) - SpecialFunctions.logCombin(N, nn)));
        });
        scalar(r, "NEGBINOMDIST", COMPATIBILITY, 3, 3, (c, a) -> {
            double f = Math.floor(n(a, 0)), s = Math.floor(n(a, 1)), p = n(a, 2);
            if (f < 0 || s < 1 || p < 0 || p > 1) throw EvalError.num();
            return num(SpecialFunctions.combin(f + s - 1, s - 1) * Math.pow(p, s) * Math.pow(1 - p, f));
        });
        scalar(r, "BETADIST", COMPATIBILITY, 3, 5, (c, a) -> {
            double x = n(a, 0), alpha = n(a, 1), beta = n(a, 2), lo = n(a, 3, 0), hi = n(a, 4, 1);
            if (alpha <= 0 || beta <= 0 || x < lo || x > hi || lo == hi) throw EvalError.num();
            return num(SpecialFunctions.betaCdf((x - lo) / (hi - lo), alpha, beta));
        });
        scalar(r, "BETAINV", COMPATIBILITY, 3, 5, (c, a) -> {
            double p = n(a, 0), alpha = n(a, 1), beta = n(a, 2), lo = n(a, 3, 0), hi = n(a, 4, 1);
            if (p <= 0 || p > 1 || alpha <= 0 || beta <= 0 || lo >= hi) throw EvalError.num();
            return num(lo + StatisticalFunctions.bisect(x -> SpecialFunctions.betaCdf(x, alpha, beta), p, 0, 1) * (hi - lo));
        });
        scalar(r, "TDIST", COMPATIBILITY, 3, 3, (c, a) -> {
            double x = n(a, 0), df = Math.floor(n(a, 1));
            int tails = (int) n(a, 2);
            if (x < 0 || df < 1 || tails != 1 && tails != 2) throw EvalError.num();
            double p = 1 - SpecialFunctions.studentCdf(x, df);
            return num(tails == 2 ? 2 * p : p);
        });
    }
}
