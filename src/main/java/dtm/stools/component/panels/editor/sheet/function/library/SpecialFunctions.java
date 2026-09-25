package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.EvalError;

final class SpecialFunctions {
    private static final double[] LANCZOS = {0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313, -176.61502916214059,
            12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
    private static final double EPS = 1e-15;

    private SpecialFunctions() {}

    static double logGamma(double x) {
        if (x <= 0) throw EvalError.num();
        if (x < 0.5) return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * x))) - logGamma(1 - x);
        x -= 1;
        double a = LANCZOS[0], t = x + 7.5;
        for (int i = 1; i < 9; i++) a += LANCZOS[i] / (x + i);
        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a);
    }

    static double gamma(double x) {
        if (x == Math.rint(x)) {
            if (x <= 0) throw EvalError.num();
            if (x <= 171) { double r = 1; for (int i = 2; i < x; i++) r *= i; return r; }
        }
        if (x < 0.5) return Math.PI / (Math.sin(Math.PI * x) * gamma(1 - x));
        x -= 1;
        double a = LANCZOS[0], t = x + 7.5;
        for (int i = 1; i < 9; i++) a += LANCZOS[i] / (x + i);
        return Math.sqrt(2 * Math.PI) * Math.pow(t, x + 0.5) * Math.exp(-t) * a;
    }

    static double lowerGammaP(double a, double x) {
        if (x < 0 || a <= 0) throw EvalError.num();
        if (x == 0) return 0;
        if (x < a + 1) {
            double sum = 1 / a, term = sum;
            for (int n = 1; n < 1000; n++) { term *= x / (a + n); sum += term; if (Math.abs(term) < Math.abs(sum) * EPS) break; }
            return sum * Math.exp(-x + a * Math.log(x) - logGamma(a));
        }
        return 1 - upperGammaQ(a, x);
    }

    static double upperGammaQ(double a, double x) {
        if (x < a + 1) return 1 - lowerGammaP(a, x);
        double b = x + 1 - a, c = 1 / 1e-300, d = 1 / b, h = d;
        for (int i = 1; i < 1000; i++) {
            double an = -i * (i - a);
            b += 2;
            d = an * d + b; if (Math.abs(d) < 1e-300) d = 1e-300;
            c = b + an / c; if (Math.abs(c) < 1e-300) c = 1e-300;
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < EPS) break;
        }
        return Math.exp(-x + a * Math.log(x) - logGamma(a)) * h;
    }

    static double betaFunction(double a, double b) { return Math.exp(logGamma(a) + logGamma(b) - logGamma(a + b)); }

    static double regularizedBeta(double x, double a, double b) {
        if (x <= 0) return 0;
        if (x >= 1) return 1;
        double bt = Math.exp(logGamma(a + b) - logGamma(a) - logGamma(b) + a * Math.log(x) + b * Math.log(1 - x));
        if (x < (a + 1) / (a + b + 2)) return bt * betaContinuedFraction(x, a, b) / a;
        return 1 - bt * betaContinuedFraction(1 - x, b, a) / b;
    }

    private static double betaContinuedFraction(double x, double a, double b) {
        double qab = a + b, qap = a + 1, qam = a - 1, c = 1, d = 1 - qab * x / qap;
        if (Math.abs(d) < 1e-300) d = 1e-300;
        d = 1 / d;
        double h = d;
        for (int m = 1; m <= 1000; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1 + aa * d; if (Math.abs(d) < 1e-300) d = 1e-300;
            c = 1 + aa / c; if (Math.abs(c) < 1e-300) c = 1e-300;
            d = 1 / d; h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1 + aa * d; if (Math.abs(d) < 1e-300) d = 1e-300;
            c = 1 + aa / c; if (Math.abs(c) < 1e-300) c = 1e-300;
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < EPS) break;
        }
        return h;
    }

    static double erf(double x) {
        if (x < 0) return -erf(-x);
        if (x > 6) return 1;
        if (x < 2.5) {
            double sum = x, term = x, x2 = x * x;
            for (int n = 1; n < 200; n++) { term *= -x2 / n; double t = term / (2 * n + 1); sum += t; if (Math.abs(t) < 1e-17 * Math.abs(sum)) break; }
            return 2 / Math.sqrt(Math.PI) * sum;
        }
        return 1 - erfc(x);
    }

    static double erfc(double x) {
        if (x < 2.5) return 1 - erf(x);
        double f = 0;
        for (int k = 60; k >= 1; k--) f = k / 2.0 / (x + f);
        return Math.exp(-x * x) / Math.sqrt(Math.PI) / (x + f);
    }

    static double normalCdf(double z) { return z < 0 ? 0.5 * erfc(-z / Math.sqrt(2)) : 0.5 * (1 + erf(z / Math.sqrt(2))); }
    static double normalPdf(double z) { return Math.exp(-z * z / 2) / Math.sqrt(2 * Math.PI); }

    static double normalInverse(double p) {
        if (p <= 0 || p >= 1) throw EvalError.num();
        double[] a = {-3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02, 1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00};
        double[] b = {-5.447609879822406e+01, 1.615858368580409e+02, -1.556989798598866e+02, 6.680131188771972e+01, -1.328068155288572e+01};
        double[] c = {-7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00, -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00};
        double[] d = {7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00};
        double q, r, x;
        if (p < 0.02425) {
            q = Math.sqrt(-2 * Math.log(p));
            x = (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
        } else if (p <= 1 - 0.02425) {
            q = p - 0.5; r = q * q;
            x = (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1);
        } else {
            q = Math.sqrt(-2 * Math.log(1 - p));
            x = -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1);
        }
        for (int i = 0; i < 2; i++) {
            double e = normalCdf(x) - p;
            double u = e * Math.sqrt(2 * Math.PI) * Math.exp(x * x / 2);
            x = x - u / (1 + x * u / 2);
        }
        return x;
    }

    static double studentCdf(double t, double df) {
        double x = df / (df + t * t);
        double tail = 0.5 * regularizedBeta(x, df / 2, 0.5);
        return t > 0 ? 1 - tail : tail;
    }

    static double studentPdf(double t, double df) {
        return Math.exp(logGamma((df + 1) / 2) - logGamma(df / 2)) / Math.sqrt(df * Math.PI) * Math.pow(1 + t * t / df, -(df + 1) / 2);
    }

    static double chiSquareCdf(double x, double df) { return x <= 0 ? 0 : lowerGammaP(df / 2, x / 2); }
    static double chiSquarePdf(double x, double df) { return x < 0 ? 0 : Math.exp((df / 2 - 1) * Math.log(x) - x / 2 - df / 2 * Math.log(2) - logGamma(df / 2)); }
    static double fCdf(double x, double d1, double d2) { return x <= 0 ? 0 : regularizedBeta(d1 * x / (d1 * x + d2), d1 / 2, d2 / 2); }

    static double fPdf(double x, double d1, double d2) {
        if (x < 0) return 0;
        return Math.exp(0.5 * (d1 * Math.log(d1 * x) + d2 * Math.log(d2) - (d1 + d2) * Math.log(d1 * x + d2)) - Math.log(x) - Math.log(betaFunction(d1 / 2, d2 / 2)));
    }

    static double gammaCdf(double x, double alpha, double beta) { return x <= 0 ? 0 : lowerGammaP(alpha, x / beta); }
    static double gammaPdf(double x, double alpha, double beta) { return x < 0 ? 0 : Math.exp((alpha - 1) * Math.log(x) - x / beta - logGamma(alpha) - alpha * Math.log(beta)); }
    static double betaCdf(double x, double a, double b) { return regularizedBeta(x, a, b); }
    static double betaPdf(double x, double a, double b) { return Math.exp((a - 1) * Math.log(x) + (b - 1) * Math.log(1 - x) - Math.log(betaFunction(a, b))); }

    static double invert(Cdf cdf, double p, double lo, double hi) {
        if (p < 0 || p > 1) throw EvalError.num();
        double a = lo, b = hi;
        int guard = 0;
        while (cdf.at(b) < p && guard++ < 200) { a = b; b = b * 2 + 1; }
        for (int i = 0; i < 300; i++) {
            double m = (a + b) / 2;
            if (cdf.at(m) < p) a = m; else b = m;
            if (Math.abs(b - a) < 1e-14 * Math.max(1, Math.abs(m))) break;
        }
        return (a + b) / 2;
    }

    static double combin(double n, double k) {
        if (k < 0 || n < 0 || k > n) throw EvalError.num();
        if (k == 0 || k == n) return 1;
        k = Math.min(k, n - k);
        double r = 1;
        for (int i = 1; i <= k; i++) r = r * (n - k + i) / i;
        return Math.rint(r);
    }

    static double logCombin(double n, double k) { return logGamma(n + 1) - logGamma(k + 1) - logGamma(n - k + 1); }

    static double binomialPmf(double k, double n, double p) {
        if (k < 0 || k > n) return 0;
        if (p == 0) return k == 0 ? 1 : 0;
        if (p == 1) return k == n ? 1 : 0;
        return Math.exp(logCombin(n, k) + k * Math.log(p) + (n - k) * Math.log(1 - p));
    }

    static double binomialCdf(double k, double n, double p) {
        double s = 0;
        for (int i = 0; i <= k; i++) s += binomialPmf(i, n, p);
        return Math.min(1, s);
    }

    static double poissonPmf(double k, double lambda) { return Math.exp(k * Math.log(lambda) - lambda - logGamma(k + 1)); }

    static double factorial(double n) {
        if (n < 0) throw EvalError.num();
        n = Math.floor(n);
        if (n > 170) throw EvalError.num();
        double r = 1;
        for (int i = 2; i <= n; i++) r *= i;
        return r;
    }

    static double besselJ(double x, int n) {
        double sum = 0;
        for (int k = 0; k < 100; k++) {
            double term = Math.pow(-1, k) * Math.pow(x / 2, 2 * k + n) / (factorialSafe(k) * factorialSafe(k + n));
            sum += term;
            if (Math.abs(term) < 1e-17 * Math.max(1, Math.abs(sum)) && k > 5) break;
        }
        return sum;
    }

    static double besselI(double x, int n) {
        double sum = 0;
        for (int k = 0; k < 200; k++) {
            double term = Math.exp((2 * k + n) * Math.log(Math.abs(x) / 2) - logGamma(k + 1) - logGamma(k + n + 1));
            sum += term;
            if (term < 1e-17 * sum && k > 5) break;
        }
        return x < 0 && n % 2 == 1 ? -sum : sum;
    }

    static double besselK(double x, int n) {
        if (x <= 0) throw EvalError.num();
        double k0 = besselK0(x), k1 = besselK1(x);
        if (n == 0) return k0;
        if (n == 1) return k1;
        double km = k0, k = k1;
        for (int j = 1; j < n; j++) { double kp = km + 2.0 * j / x * k; km = k; k = kp; }
        return k;
    }

    private static double besselK0(double x) {
        if (x <= 2) {
            double y = x * x / 4;
            return -Math.log(x / 2) * besselI(x, 0) + (-0.57721566 + y * (0.42278420 + y * (0.23069756 + y * (0.3488590e-1 + y * (0.262698e-2 + y * (0.10750e-3 + y * 0.74e-5))))));
        }
        double y = 2 / x;
        return Math.exp(-x) / Math.sqrt(x) * (1.25331414 + y * (-0.7832358e-1 + y * (0.2189568e-1 + y * (-0.1062446e-1 + y * (0.587872e-2 + y * (-0.251540e-2 + y * 0.53208e-3))))));
    }

    private static double besselK1(double x) {
        if (x <= 2) {
            double y = x * x / 4;
            return Math.log(x / 2) * besselI(x, 1) + (1 / x) * (1 + y * (0.15443144 + y * (-0.67278579 + y * (-0.18156897 + y * (-0.1919402e-1 + y * (-0.110404e-2 + y * (-0.4686e-4)))))));
        }
        double y = 2 / x;
        return Math.exp(-x) / Math.sqrt(x) * (1.25331414 + y * (0.23498619 + y * (-0.3655620e-1 + y * (0.1504268e-1 + y * (-0.780353e-2 + y * (0.325614e-2 + y * (-0.68245e-3)))))));
    }

    static double besselY(double x, int n) {
        if (x <= 0) throw EvalError.num();
        double y0 = besselY0(x), y1 = besselY1(x);
        if (n == 0) return y0;
        if (n == 1) return y1;
        double ym = y0, y = y1;
        for (int j = 1; j < n; j++) { double yp = 2.0 * j / x * y - ym; ym = y; y = yp; }
        return y;
    }

    private static double besselY0(double x) {
        if (x < 8) {
            double y = x * x;
            double a = -2957821389.0 + y * (7062834065.0 + y * (-512359803.6 + y * (10879881.29 + y * (-86327.92757 + y * 228.4622733))));
            double b = 40076544269.0 + y * (745249964.8 + y * (7189466.438 + y * (47447.26470 + y * (226.1030244 + y))));
            return a / b + 0.636619772 * besselJ(x, 0) * Math.log(x);
        }
        double z = 8 / x, y = z * z, xx = x - 0.785398164;
        double a = 1 + y * (-0.1098628627e-2 + y * (0.2734510407e-4 + y * (-0.2073370639e-5 + y * 0.2093887211e-6)));
        double b = -0.1562499995e-1 + y * (0.1430488765e-3 + y * (-0.6911147651e-5 + y * (0.7621095161e-6 + y * (-0.934935152e-7))));
        return Math.sqrt(0.636619772 / x) * (Math.sin(xx) * a + z * Math.cos(xx) * b);
    }

    private static double besselY1(double x) {
        if (x < 8) {
            double y = x * x;
            double a = x * (-0.4900604943e13 + y * (0.1275274390e13 + y * (-0.5153438139e11 + y * (0.7349264551e9 + y * (-0.4237922726e7 + y * 0.8511937935e4)))));
            double b = 0.2499580570e14 + y * (0.4244419664e12 + y * (0.3733650367e10 + y * (0.2245904002e8 + y * (0.1020426050e6 + y * (0.3549632885e3 + y)))));
            return a / b + 0.636619772 * (besselJ(x, 1) * Math.log(x) - 1 / x);
        }
        double z = 8 / x, y = z * z, xx = x - 2.356194491;
        double a = 1 + y * (0.183105e-2 + y * (-0.3516396496e-4 + y * (0.2457520174e-5 + y * (-0.240337019e-6))));
        double b = 0.04687499995 + y * (-0.2002690873e-3 + y * (0.8449199096e-5 + y * (-0.88228987e-6 + y * 0.105787412e-6)));
        return Math.sqrt(0.636619772 / x) * (Math.sin(xx) * a + z * Math.cos(xx) * b);
    }

    private static double factorialSafe(int n) { return Math.exp(logGamma(n + 1)); }
}
