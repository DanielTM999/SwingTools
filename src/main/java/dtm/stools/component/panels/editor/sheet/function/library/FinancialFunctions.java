package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.FINANCIAL;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class FinancialFunctions {
    private FinancialFunctions() {}

    static void register(FunctionRegistry r) {
        scalar(r, "PMT", FINANCIAL, 3, 5, (c, a) -> num(pmt(n(a, 0), n(a, 1), n(a, 2), n(a, 3, 0), n(a, 4, 0) != 0)));
        scalar(r, "PV", FINANCIAL, 3, 5, (c, a) -> num(pv(n(a, 0), n(a, 1), n(a, 2), n(a, 3, 0), n(a, 4, 0) != 0)));
        scalar(r, "FV", FINANCIAL, 3, 5, (c, a) -> num(fv(n(a, 0), n(a, 1), n(a, 2), n(a, 3, 0), n(a, 4, 0) != 0)));
        scalar(r, "NPER", FINANCIAL, 3, 5, (c, a) -> {
            double rate = n(a, 0), pmt = n(a, 1), pv = n(a, 2), fv = n(a, 3, 0);
            boolean begin = n(a, 4, 0) != 0;
            if (rate == 0) { if (pmt == 0) throw EvalError.num(); return num(-(pv + fv) / pmt); }
            double adj = pmt * (1 + (begin ? rate : 0)) / rate;
            double v = (adj - fv) / (adj + pv);
            if (v <= 0) throw EvalError.num();
            return num(Math.log(v) / Math.log(1 + rate));
        });
        scalar(r, "RATE", FINANCIAL, 3, 6, (c, a) -> num(rate(n(a, 0), n(a, 1), n(a, 2), n(a, 3, 0), n(a, 4, 0) != 0, n(a, 5, 0.1))));
        scalar(r, "IPMT", FINANCIAL, 4, 6, (c, a) -> num(ipmt(n(a, 0), n(a, 1), n(a, 2), n(a, 3), n(a, 4, 0), n(a, 5, 0) != 0)));
        scalar(r, "PPMT", FINANCIAL, 4, 6, (c, a) -> {
            double rate = n(a, 0), per = n(a, 1), nper = n(a, 2), pv = n(a, 3), fv = n(a, 4, 0);
            boolean begin = n(a, 5, 0) != 0;
            return num(pmt(rate, nper, pv, fv, begin) - ipmt(rate, per, nper, pv, fv, begin));
        });
        scalar(r, "CUMIPMT", FINANCIAL, 6, 6, (c, a) -> num(cumulative(a, true)));
        scalar(r, "CUMPRINC", FINANCIAL, 6, 6, (c, a) -> num(cumulative(a, false)));
        scalar(r, "ISPMT", FINANCIAL, 4, 4, (c, a) -> { double rate = n(a, 0), per = n(a, 1), nper = n(a, 2), pv = n(a, 3); if (nper == 0) throw EvalError.div0(); return num(pv * rate * (per / nper - 1)); });
        raw(r, "NPV", FINANCIAL, 2, 255, (c, a) -> {
            double rate = a.number(0);
            DoubleList flows = numbers(c, a, 1);
            double v = 0;
            for (int k = 0; k < flows.size(); k++) v += flows.get(k) / Math.pow(1 + rate, k + 1);
            return num(v);
        });
        raw(r, "IRR", FINANCIAL, 1, 2, (c, a) -> {
            double[] flows = numbersOf(c, a.value(0), false).toArray();
            double guess = a.number(1, 0.1);
            return num(irr(flows, guess));
        });
        raw(r, "MIRR", FINANCIAL, 3, 3, (c, a) -> {
            double[] flows = numbersOf(c, a.value(0), false).toArray();
            double finance = a.number(1), reinvest = a.number(2);
            int n = flows.length;
            double npvPos = 0, npvNeg = 0;
            for (int k = 0; k < n; k++) {
                if (flows[k] > 0) npvPos += flows[k] / Math.pow(1 + reinvest, k);
                else npvNeg += flows[k] / Math.pow(1 + finance, k);
            }
            if (npvNeg == 0 || npvPos == 0) throw EvalError.div0();
            return num(Math.pow(-npvPos * Math.pow(1 + reinvest, n - 1) / npvNeg, 1.0 / (n - 1)) - 1);
        });
        raw(r, "XNPV", FINANCIAL, 3, 3, (c, a) -> {
            double rate = a.number(0);
            double[] flows = numbersOf(c, a.value(1), false).toArray(), dates = numbersOf(c, a.value(2), false).toArray();
            if (flows.length != dates.length) throw EvalError.num();
            return num(xnpv(rate, flows, dates));
        });
        raw(r, "XIRR", FINANCIAL, 2, 3, (c, a) -> {
            double[] flows = numbersOf(c, a.value(0), false).toArray(), dates = numbersOf(c, a.value(1), false).toArray();
            if (flows.length != dates.length) throw EvalError.num();
            double guess = a.number(2, 0.1);
            return num(solve(x -> xnpv(x, flows, dates), guess, -0.9999999, 1e10));
        });
        scalar(r, "EFFECT", FINANCIAL, 2, 2, (c, a) -> { double rate = n(a, 0), np = Math.floor(n(a, 1)); if (rate <= 0 || np < 1) throw EvalError.num(); return num(Math.pow(1 + rate / np, np) - 1); });
        scalar(r, "NOMINAL", FINANCIAL, 2, 2, (c, a) -> { double rate = n(a, 0), np = Math.floor(n(a, 1)); if (rate <= 0 || np < 1) throw EvalError.num(); return num(np * (Math.pow(1 + rate, 1 / np) - 1)); });
        scalar(r, "SLN", FINANCIAL, 3, 3, (c, a) -> { double life = n(a, 2); if (life == 0) throw EvalError.div0(); return num((n(a, 0) - n(a, 1)) / life); });
        scalar(r, "SYD", FINANCIAL, 4, 4, (c, a) -> {
            double cost = n(a, 0), salvage = n(a, 1), life = n(a, 2), per = n(a, 3);
            if (life <= 0 || per <= 0 || per > life) throw EvalError.num();
            return num((cost - salvage) * (life - per + 1) * 2 / (life * (life + 1)));
        });
        scalar(r, "DB", FINANCIAL, 4, 5, (c, a) -> {
            double cost = n(a, 0), salvage = n(a, 1), life = n(a, 2), period = n(a, 3), month = n(a, 4, 12);
            if (cost < 0 || salvage < 0 || life <= 0 || period <= 0 || month < 1 || month > 12 || period > life + 1) throw EvalError.num();
            double rate = cost == 0 ? 0 : MathFunctions.round(1 - Math.pow(salvage / cost, 1 / life), 3, java.math.RoundingMode.HALF_UP);
            double total = 0, dep = cost * rate * month / 12;
            if (period == 1) return num(dep);
            total = dep;
            for (int k = 2; k <= period; k++) {
                if (k == (int) life + 1) dep = (cost - total) * rate * (12 - month) / 12;
                else dep = (cost - total) * rate;
                total += dep;
            }
            return num(dep);
        });
        scalar(r, "DDB", FINANCIAL, 4, 5, (c, a) -> num(ddb(n(a, 0), n(a, 1), n(a, 2), n(a, 3), n(a, 4, 2))));
        scalar(r, "VDB", FINANCIAL, 5, 7, (c, a) -> num(vdb(n(a, 0), n(a, 1), n(a, 2), n(a, 3), n(a, 4), n(a, 5, 2), b(a, 6, false))));
        scalar(r, "PDURATION", FINANCIAL, 3, 3, (c, a) -> { double rate = n(a, 0), pv = n(a, 1), fv = n(a, 2); if (rate <= 0 || pv <= 0 || fv <= 0) throw EvalError.num(); return num((Math.log(fv) - Math.log(pv)) / Math.log(1 + rate)); });
        scalar(r, "RRI", FINANCIAL, 3, 3, (c, a) -> { double nn = n(a, 0), pv = n(a, 1), fv = n(a, 2); if (nn <= 0 || pv == 0) throw EvalError.num(); return num(Math.pow(fv / pv, 1 / nn) - 1); });
        raw(r, "FVSCHEDULE", FINANCIAL, 2, 2, (c, a) -> {
            double v = a.number(0);
            for (CellValue x : flatten(c, a.value(1))) v *= 1 + (x.isEmpty() ? 0 : dtm.stools.component.panels.editor.sheet.calc.Coerce.number(x));
            return num(v);
        });
        scalar(r, "DOLLARDE", FINANCIAL, 2, 2, (c, a) -> {
            double v = n(a, 0), f = Math.floor(n(a, 1));
            if (f < 0) throw EvalError.num();
            if (f == 0) throw EvalError.div0();
            double whole = v < 0 ? Math.ceil(v) : Math.floor(v), frac = v - whole;
            double digits = Math.ceil(Math.log10(f));
            return num(whole + frac * Math.pow(10, digits) / f);
        });
        scalar(r, "DOLLARFR", FINANCIAL, 2, 2, (c, a) -> {
            double v = n(a, 0), f = Math.floor(n(a, 1));
            if (f < 0) throw EvalError.num();
            if (f == 0) throw EvalError.div0();
            double whole = v < 0 ? Math.ceil(v) : Math.floor(v), frac = v - whole;
            double digits = Math.ceil(Math.log10(f));
            return num(whole + frac * f / Math.pow(10, digits));
        });
        scalar(r, "COUPPCD", FINANCIAL, 3, 4, (c, a) -> num(DateTimeFunctions.serial(c, pcd(c, n(a, 0), n(a, 1), frequency(a, 2)))));
        scalar(r, "COUPNCD", FINANCIAL, 3, 4, (c, a) -> num(DateTimeFunctions.serial(c, ncd(c, n(a, 0), n(a, 1), frequency(a, 2)))));
        scalar(r, "COUPNUM", FINANCIAL, 3, 4, (c, a) -> num(coupNum(c, n(a, 0), n(a, 1), frequency(a, 2))));
        scalar(r, "COUPDAYBS", FINANCIAL, 3, 4, (c, a) -> num(coupDayBs(c, n(a, 0), n(a, 1), frequency(a, 2), basis(a, 3))));
        scalar(r, "COUPDAYS", FINANCIAL, 3, 4, (c, a) -> num(coupDays(c, n(a, 0), n(a, 1), frequency(a, 2), basis(a, 3))));
        scalar(r, "COUPDAYSNC", FINANCIAL, 3, 4, (c, a) -> num(coupDaysNc(c, n(a, 0), n(a, 1), frequency(a, 2), basis(a, 3))));
        scalar(r, "PRICE", FINANCIAL, 6, 7, (c, a) -> num(price(c, n(a, 0), n(a, 1), n(a, 2), n(a, 3), n(a, 4), frequency(a, 5), basis(a, 6))));
        scalar(r, "YIELD", FINANCIAL, 6, 7, (c, a) -> {
            double settlement = n(a, 0), maturity = n(a, 1), rate = n(a, 2), pr = n(a, 3), redemption = n(a, 4);
            int freq = frequency(a, 5), basis = basis(a, 6);
            if (pr <= 0 || redemption <= 0 || rate < 0) throw EvalError.num();
            return num(solve(y -> price(c, settlement, maturity, rate, y, redemption, freq, basis) - pr, 0.05, -0.99, 10));
        });
        scalar(r, "DURATION", FINANCIAL, 5, 6, (c, a) -> num(duration(c, n(a, 0), n(a, 1), n(a, 2), n(a, 3), frequency(a, 4), basis(a, 5))));
        scalar(r, "MDURATION", FINANCIAL, 5, 6, (c, a) -> {
            int freq = frequency(a, 4);
            return num(duration(c, n(a, 0), n(a, 1), n(a, 2), n(a, 3), freq, basis(a, 5)) / (1 + n(a, 3) / freq));
        });
        scalar(r, "ACCRINT", FINANCIAL, 6, 8, (c, a) -> {
            double issue = n(a, 0), settlement = n(a, 2), rate = n(a, 3), par = n(a, 4, 1000);
            int basis = basis(a, 6);
            frequency(a, 5);
            if (rate <= 0 || par <= 0 || issue >= settlement) throw EvalError.num();
            return num(par * rate * DateTimeFunctions.yearFrac(c, issue, settlement, basis));
        });
        scalar(r, "ACCRINTM", FINANCIAL, 4, 5, (c, a) -> {
            double issue = n(a, 0), settlement = n(a, 1), rate = n(a, 2), par = n(a, 3, 1000);
            if (rate <= 0 || par <= 0 || issue >= settlement) throw EvalError.num();
            return num(par * rate * DateTimeFunctions.yearFrac(c, issue, settlement, basis(a, 4)));
        });
        scalar(r, "DISC", FINANCIAL, 4, 5, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), pr = n(a, 2), red = n(a, 3);
            if (pr <= 0 || red <= 0 || s >= m) throw EvalError.num();
            return num((red - pr) / red / DateTimeFunctions.yearFrac(c, s, m, basis(a, 4)));
        });
        scalar(r, "INTRATE", FINANCIAL, 4, 5, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), inv = n(a, 2), red = n(a, 3);
            if (inv <= 0 || red <= 0 || s >= m) throw EvalError.num();
            return num((red - inv) / inv / DateTimeFunctions.yearFrac(c, s, m, basis(a, 4)));
        });
        scalar(r, "RECEIVED", FINANCIAL, 4, 5, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), inv = n(a, 2), disc = n(a, 3);
            if (inv <= 0 || disc <= 0 || s >= m) throw EvalError.num();
            double d = 1 - disc * DateTimeFunctions.yearFrac(c, s, m, basis(a, 4));
            if (d <= 0) throw EvalError.num();
            return num(inv / d);
        });
        scalar(r, "PRICEDISC", FINANCIAL, 4, 5, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), disc = n(a, 2), red = n(a, 3);
            if (disc <= 0 || red <= 0 || s >= m) throw EvalError.num();
            return num(red - disc * red * DateTimeFunctions.yearFrac(c, s, m, basis(a, 4)));
        });
        scalar(r, "YIELDDISC", FINANCIAL, 4, 5, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), pr = n(a, 2), red = n(a, 3);
            if (pr <= 0 || red <= 0 || s >= m) throw EvalError.num();
            return num((red - pr) / pr / DateTimeFunctions.yearFrac(c, s, m, basis(a, 4)));
        });
        scalar(r, "PRICEMAT", FINANCIAL, 5, 6, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), issue = n(a, 2), rate = n(a, 3), yld = n(a, 4);
            int basis = basis(a, 5);
            double dim = DateTimeFunctions.yearFrac(c, issue, m, basis), a1 = DateTimeFunctions.yearFrac(c, issue, s, basis), dsm = DateTimeFunctions.yearFrac(c, s, m, basis);
            return num((100 + dim * rate * 100) / (1 + dsm * yld) - a1 * rate * 100);
        });
        scalar(r, "YIELDMAT", FINANCIAL, 5, 6, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), issue = n(a, 2), rate = n(a, 3), pr = n(a, 4);
            int basis = basis(a, 5);
            double dim = DateTimeFunctions.yearFrac(c, issue, m, basis), a1 = DateTimeFunctions.yearFrac(c, issue, s, basis), dsm = DateTimeFunctions.yearFrac(c, s, m, basis);
            return num(((1 + dim * rate) / (pr / 100 + a1 * rate) - 1) / dsm);
        });
        scalar(r, "TBILLPRICE", FINANCIAL, 3, 3, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), disc = n(a, 2);
            double days = Math.floor(m) - Math.floor(s);
            if (days <= 0 || days > 365 || disc <= 0) throw EvalError.num();
            return num(100 * (1 - disc * days / 360));
        });
        scalar(r, "TBILLYIELD", FINANCIAL, 3, 3, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), pr = n(a, 2);
            double days = Math.floor(m) - Math.floor(s);
            if (days <= 0 || days > 365 || pr <= 0) throw EvalError.num();
            return num((100 - pr) / pr * 360 / days);
        });
        scalar(r, "TBILLEQ", FINANCIAL, 3, 3, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), disc = n(a, 2);
            double days = Math.floor(m) - Math.floor(s);
            if (days <= 0 || days > 365 || disc <= 0) throw EvalError.num();
            if (days <= 182) return num(365 * disc / (360 - disc * days));
            double price = 100 * (1 - disc * days / 360);
            double aa = days / 365.0 / 2 - 0.25, bb = days / 365.0, cc = (price - 100) / price;
            return num((-bb + Math.sqrt(bb * bb - 4 * aa * cc)) / (2 * aa));
        });
        scalar(r, "ODDFPRICE", FINANCIAL, 8, 9, (c, a) -> num(oddPrice(c, n(a, 0), n(a, 1), n(a, 2), n(a, 3), n(a, 4), n(a, 5), n(a, 6), frequency(a, 7), basis(a, 8), true)));
        scalar(r, "ODDFYIELD", FINANCIAL, 8, 9, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), issue = n(a, 2), first = n(a, 3), rate = n(a, 4), pr = n(a, 5), red = n(a, 6);
            int f = frequency(a, 7), basis = basis(a, 8);
            return num(solve(y -> oddPrice(c, s, m, issue, first, rate, y, red, f, basis, true) - pr, 0.05, -0.99, 10));
        });
        scalar(r, "ODDLPRICE", FINANCIAL, 7, 8, (c, a) -> num(oddLast(c, n(a, 0), n(a, 1), n(a, 2), n(a, 3), n(a, 4), n(a, 5), frequency(a, 6), basis(a, 7))));
        scalar(r, "ODDLYIELD", FINANCIAL, 7, 8, (c, a) -> {
            double s = n(a, 0), m = n(a, 1), last = n(a, 2), rate = n(a, 3), pr = n(a, 4), red = n(a, 5);
            int f = frequency(a, 6), basis = basis(a, 7);
            return num(solve(y -> oddLast(c, s, m, last, rate, y, red, f, basis) - pr, 0.05, -0.99, 10));
        });
        scalar(r, "AMORLINC", FINANCIAL, 6, 7, (c, a) -> num(amorlinc(c, n(a, 0), n(a, 1), n(a, 2), n(a, 3), n(a, 4), n(a, 5), basis(a, 6))));
        scalar(r, "AMORDEGRC", FINANCIAL, 6, 7, (c, a) -> num(amordegrc(c, n(a, 0), n(a, 1), n(a, 2), n(a, 3), n(a, 4), n(a, 5), basis(a, 6))));
    }

    static double pmt(double rate, double nper, double pv, double fv, boolean begin) {
        if (nper == 0) throw EvalError.num();
        if (rate == 0) return -(pv + fv) / nper;
        double f = Math.pow(1 + rate, nper);
        return -(rate * (pv * f + fv)) / ((1 + (begin ? rate : 0)) * (f - 1));
    }

    static double pv(double rate, double nper, double pmt, double fv, boolean begin) {
        if (rate == 0) return -(fv + pmt * nper);
        double f = Math.pow(1 + rate, nper);
        return -(fv + pmt * (1 + (begin ? rate : 0)) * (f - 1) / rate) / f;
    }

    static double fv(double rate, double nper, double pmt, double pv, boolean begin) {
        if (rate == 0) return -(pv + pmt * nper);
        double f = Math.pow(1 + rate, nper);
        return -(pv * f + pmt * (1 + (begin ? rate : 0)) * (f - 1) / rate);
    }

    static double ipmt(double rate, double per, double nper, double pv, double fv, boolean begin) {
        if (per < 1 || per > nper) throw EvalError.num();
        double payment = pmt(rate, nper, pv, fv, begin);
        double interest;
        if (per == 1) interest = begin ? 0 : -pv;
        else interest = begin ? fv(rate, per - 2, payment, pv, true) - payment : fv(rate, per - 1, payment, pv, false);
        return interest * rate;
    }

    private static double cumulative(CellValue[] a, boolean interest) {
        double rate = n(a, 0), nper = n(a, 1), pv = n(a, 2);
        int start = (int) n(a, 3), end = (int) n(a, 4), type = (int) n(a, 5);
        if (rate <= 0 || nper <= 0 || pv <= 0 || start < 1 || end < start || end > nper || type != 0 && type != 1) throw EvalError.num();
        double total = 0;
        double payment = pmt(rate, nper, pv, 0, type == 1);
        for (int per = start; per <= end; per++) {
            double i = ipmt(rate, per, nper, pv, 0, type == 1);
            total += interest ? i : payment - i;
        }
        return total;
    }

    static double rate(double nper, double pmt, double pv, double fv, boolean begin, double guess) {
        return solve(r -> {
            if (Math.abs(r) < 1e-12) return pv + pmt * nper + fv;
            double f = Math.pow(1 + r, nper);
            return pv * f + pmt * (1 + (begin ? r : 0)) * (f - 1) / r + fv;
        }, guess, -0.9999999, 1e5);
    }

    static double solve(RootFunction f, double guess, double lo, double hi) {
        double x = guess;
        for (int k = 0; k < 100; k++) {
            double y = f.at(x);
            if (!Double.isFinite(y)) break;
            if (Math.abs(y) < 1e-10) return x;
            double h = Math.max(1e-7, Math.abs(x) * 1e-7);
            double d = (f.at(x + h) - f.at(x - h)) / (2 * h);
            if (d == 0 || !Double.isFinite(d)) break;
            double next = x - y / d;
            if (next <= lo) next = (x + lo) / 2;
            if (next >= hi) next = (x + hi) / 2;
            if (Math.abs(next - x) < 1e-12) return next;
            x = next;
        }
        double a = lo + 1e-12, b = Math.min(hi, 10);
        double fa = f.at(a), fb = f.at(b);
        double scan = guess;
        if (fa * fb > 0) {
            boolean found = false;
            for (double s = -0.99; s <= 10; s += 0.01) {
                double fs = f.at(s), ft = f.at(s + 0.01);
                if (Double.isFinite(fs) && Double.isFinite(ft) && fs * ft <= 0) { a = s; b = s + 0.01; fa = fs; fb = ft; found = true; break; }
            }
            if (!found) throw EvalError.num();
        }
        for (int k = 0; k < 300; k++) {
            double m = (a + b) / 2, fm = f.at(m);
            if (Math.abs(fm) < 1e-10 || b - a < 1e-14) return m;
            if (fa * fm <= 0) { b = m; fb = fm; } else { a = m; fa = fm; }
            scan = m;
        }
        return scan;
    }

    static double irr(double[] flows, double guess) {
        boolean pos = false, neg = false;
        for (double f : flows) { if (f > 0) pos = true; if (f < 0) neg = true; }
        if (!pos || !neg) throw EvalError.num();
        return solve(r -> { double v = 0; for (int k = 0; k < flows.length; k++) v += flows[k] / Math.pow(1 + r, k); return v; }, guess, -0.9999999, 1e10);
    }

    static double xnpv(double rate, double[] flows, double[] dates) {
        double v = 0, d0 = dates[0];
        for (int k = 0; k < flows.length; k++) {
            if (dates[k] < d0) throw EvalError.num();
            v += flows[k] / Math.pow(1 + rate, (dates[k] - d0) / 365.0);
        }
        return v;
    }

    static double ddb(double cost, double salvage, double life, double period, double factor) {
        if (cost < 0 || salvage < 0 || life <= 0 || period <= 0 || factor <= 0 || period > life) throw EvalError.num();
        double book = cost, dep = 0;
        for (int k = 1; k <= Math.ceil(period); k++) {
            dep = Math.min(book * factor / life, Math.max(0, book - salvage));
            book -= dep;
        }
        return dep;
    }

    static double vdb(double cost, double salvage, double life, double start, double end, double factor, boolean noSwitch) {
        if (cost < 0 || salvage < 0 || life <= 0 || start < 0 || end < start || end > life || factor <= 0) throw EvalError.num();
        double total = 0, book = cost;
        int periods = (int) Math.ceil(end);
        boolean switched = false;
        for (int p = 0; p < periods; p++) {
            double ddbDep = Math.min(book * factor / life, Math.max(0, book - salvage));
            double slDep = life - p > 0 ? (book - salvage) / (life - p) : 0;
            double dep;
            if (!noSwitch && (switched || slDep > ddbDep)) { switched = true; dep = slDep; } else dep = ddbDep;
            double from = Math.max(start, p), to = Math.min(end, p + 1);
            if (to > from) total += dep * (to - from);
            book -= dep;
        }
        return total;
    }

    static int frequency(CellValue[] a, int i) {
        int f = (int) n(a, i);
        if (f != 1 && f != 2 && f != 4) throw EvalError.num();
        return f;
    }

    static int basis(CellValue[] a, int i) {
        int b = (int) n(a, i, 0);
        if (b < 0 || b > 4) throw EvalError.num();
        return b;
    }

    static LocalDate pcd(FunctionContext c, double settlement, double maturity, int freq) {
        if (settlement >= maturity) throw EvalError.num();
        LocalDate s = DateTimeFunctions.localDate(c, settlement), m = DateTimeFunctions.localDate(c, maturity);
        int months = 12 / freq;
        LocalDate d = m;
        int k = 0;
        while (d.isAfter(s)) { k++; d = couponDate(m, -k * months); }
        return d;
    }

    static LocalDate ncd(FunctionContext c, double settlement, double maturity, int freq) {
        LocalDate p = pcd(c, settlement, maturity, freq);
        LocalDate m = DateTimeFunctions.localDate(c, maturity);
        long monthsBack = ChronoUnit.MONTHS.between(p, m);
        return couponDate(m, -(int) (monthsBack - 12 / freq));
    }

    private static LocalDate couponDate(LocalDate maturity, int months) {
        LocalDate d = maturity.plusMonths(months);
        if (maturity.getDayOfMonth() == maturity.lengthOfMonth()) d = d.withDayOfMonth(d.lengthOfMonth());
        return d;
    }

    static double coupNum(FunctionContext c, double settlement, double maturity, int freq) {
        LocalDate p = pcd(c, settlement, maturity, freq), m = DateTimeFunctions.localDate(c, maturity);
        return Math.round(ChronoUnit.MONTHS.between(p, m) / (12.0 / freq));
    }

    static double days(FunctionContext c, LocalDate from, LocalDate to, int basis) {
        return switch (basis) {
            case 0 -> DateTimeFunctions.days360(from, to, false);
            case 4 -> DateTimeFunctions.days360(from, to, true);
            default -> ChronoUnit.DAYS.between(from, to);
        };
    }

    static double coupDayBs(FunctionContext c, double settlement, double maturity, int freq, int basis) {
        return days(c, pcd(c, settlement, maturity, freq), DateTimeFunctions.localDate(c, settlement), basis);
    }

    static double coupDays(FunctionContext c, double settlement, double maturity, int freq, int basis) {
        return switch (basis) {
            case 1 -> ChronoUnit.DAYS.between(pcd(c, settlement, maturity, freq), ncd(c, settlement, maturity, freq));
            case 3 -> 365.0 / freq;
            default -> 360.0 / freq;
        };
    }

    static double coupDaysNc(FunctionContext c, double settlement, double maturity, int freq, int basis) {
        if (basis == 0) return coupDays(c, settlement, maturity, freq, basis) - coupDayBs(c, settlement, maturity, freq, basis);
        return days(c, DateTimeFunctions.localDate(c, settlement), ncd(c, settlement, maturity, freq), basis);
    }

    static double price(FunctionContext c, double settlement, double maturity, double rate, double yld, double redemption, int freq, int basis) {
        if (rate < 0 || yld < 0 && yld <= -freq || redemption <= 0) throw EvalError.num();
        double n = coupNum(c, settlement, maturity, freq), e = coupDays(c, settlement, maturity, freq, basis);
        double aDays = coupDayBs(c, settlement, maturity, freq, basis), dsc = coupDaysNc(c, settlement, maturity, freq, basis);
        double coupon = 100 * rate / freq;
        if (n == 1) return (redemption + coupon) / (1 + dsc / e * yld / freq) - coupon * aDays / e;
        double base = 1 + yld / freq, p = redemption / Math.pow(base, n - 1 + dsc / e);
        for (int k = 1; k <= n; k++) p += coupon / Math.pow(base, k - 1 + dsc / e);
        return p - coupon * aDays / e;
    }

    static double duration(FunctionContext c, double settlement, double maturity, double coupon, double yld, int freq, int basis) {
        if (coupon < 0 || yld < 0) throw EvalError.num();
        double n = coupNum(c, settlement, maturity, freq), e = coupDays(c, settlement, maturity, freq, basis), dsc = coupDaysNc(c, settlement, maturity, freq, basis);
        double base = 1 + yld / freq, weighted = 0, total = 0;
        for (int k = 1; k <= n; k++) {
            double t = k - 1 + dsc / e;
            double cf = 100 * coupon / freq + (k == n ? 100 : 0);
            double pv = cf / Math.pow(base, t);
            weighted += t * pv;
            total += pv;
        }
        return weighted / total / freq;
    }

    static double oddPrice(FunctionContext c, double settlement, double maturity, double issue, double firstCoupon, double rate, double yld, double redemption, int freq, int basis, boolean first) {
        if (!(issue < settlement && settlement < firstCoupon && firstCoupon < maturity)) throw EvalError.num();
        double e = coupDays(c, firstCoupon, maturity, freq, basis);
        LocalDate issueDate = DateTimeFunctions.localDate(c, issue), settle = DateTimeFunctions.localDate(c, settlement), fc = DateTimeFunctions.localDate(c, firstCoupon);
        double dfc = days(c, issueDate, fc, basis), dsc = days(c, settle, fc, basis), aDays = days(c, issueDate, settle, basis);
        double n = coupNum(c, firstCoupon, maturity, freq) + 1;
        double coupon = 100 * rate / freq, base = 1 + yld / freq;
        double p = redemption / Math.pow(base, n - 1 + dsc / e) + coupon * dfc / e / Math.pow(base, dsc / e);
        for (int k = 2; k <= n; k++) p += coupon / Math.pow(base, k - 1 + dsc / e);
        return p - coupon * aDays / e;
    }

    static double oddLast(FunctionContext c, double settlement, double maturity, double lastInterest, double rate, double yld, double redemption, int freq, int basis) {
        if (!(lastInterest < settlement && settlement < maturity)) throw EvalError.num();
        LocalDate last = DateTimeFunctions.localDate(c, lastInterest), settle = DateTimeFunctions.localDate(c, settlement), mat = DateTimeFunctions.localDate(c, maturity);
        double periodDays = basis == 1 ? ChronoUnit.DAYS.between(last, last.plusMonths(12 / freq)) : basis == 3 ? 365.0 / freq : 360.0 / freq;
        double dc = days(c, last, mat, basis) / periodDays, a = days(c, last, settle, basis) / periodDays, dsc = days(c, settle, mat, basis) / periodDays;
        double coupon = 100 * rate / freq;
        return (redemption + dc * coupon) / (1 + dsc * yld / freq) - a * coupon;
    }

    static double amorlinc(FunctionContext c, double cost, double purchased, double firstPeriod, double salvage, double period, double rate, int basis) {
        if (cost < 0 || salvage < 0 || rate <= 0 || period < 0) throw EvalError.num();
        double full = cost * rate;
        double first = full * DateTimeFunctions.yearFrac(c, purchased, firstPeriod, basis);
        double remaining = cost - salvage;
        if (period == 0) return Math.min(first, remaining);
        remaining -= first;
        for (int p = 1; p <= period; p++) {
            double dep = Math.min(full, Math.max(0, remaining));
            if (p == (int) period) return dep;
            remaining -= dep;
        }
        return 0;
    }

    static double amordegrc(FunctionContext c, double cost, double purchased, double firstPeriod, double salvage, double period, double rate, int basis) {
        if (cost < 0 || salvage < 0 || rate <= 0 || period < 0) throw EvalError.num();
        double life = 1 / rate;
        double coefficient = life < 3 ? 1 : life < 5 ? 1.5 : life <= 6 ? 2 : 2.5;
        double r = rate * coefficient;
        double dep = Math.round(DateTimeFunctions.yearFrac(c, purchased, firstPeriod, basis) * r * cost);
        double book = cost - dep;
        if (period == 0) return dep;
        for (int p = 1; p <= period; p++) {
            double remainingPeriods = life - p;
            if (remainingPeriods < 1) dep = book - salvage > 0 ? book - salvage : 0;
            else if (remainingPeriods < 2) dep = Math.round(book * 0.5);
            else dep = Math.round(book * r);
            if (book - dep < salvage) dep = Math.max(0, book - salvage);
            book -= dep;
            if (p == (int) period) return dep;
        }
        return 0;
    }
}
