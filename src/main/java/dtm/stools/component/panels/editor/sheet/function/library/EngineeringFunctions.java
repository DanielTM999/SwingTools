package dtm.stools.component.panels.editor.sheet.function.library;

import java.util.function.UnaryOperator;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.ENGINEERING;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class EngineeringFunctions {
    private record Unit(String category, double factor, boolean prefixable) {}
    private record Complex(double re, double im, String suffix) {}

    private static final Map<String, Unit> UNITS = new HashMap<>();
    private static final Map<String, Double> PREFIXES = new HashMap<>();
    private static final Map<String, Double> BINARY_PREFIXES = new HashMap<>();
    private static final Pattern COMPLEX = Pattern.compile("^([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)?(?:([+-])((?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)?([ij]))?$");

    static {
        unit("mass", 1, true, "g"); unit("mass", 14593.9029372064, false, "sg"); unit("mass", 453.59237, false, "lbm");
        unit("mass", 1.66053906660e-24, true, "u"); unit("mass", 28.349523125, false, "ozm"); unit("mass", 0.06479891, false, "grain");
        unit("mass", 45359.237, false, "cwt", "shweight"); unit("mass", 50802.34544, false, "uk_cwt", "lcwt"); unit("mass", 6350.29318, false, "stone");
        unit("mass", 907184.74, false, "ton"); unit("mass", 1016046.9088, false, "uk_ton", "LTON", "brton");
        unit("distance", 1, true, "m"); unit("distance", 1609.344, false, "mi"); unit("distance", 1852, false, "Nmi"); unit("distance", 0.0254, false, "in");
        unit("distance", 0.3048, false, "ft"); unit("distance", 0.9144, false, "yd"); unit("distance", 1e-10, true, "ang"); unit("distance", 1.143, false, "ell");
        unit("distance", 9.4607304725808e15, true, "ly"); unit("distance", 3.08567758128155e16, true, "parsec", "pc");
        unit("distance", 0.0254 / 72, false, "Picapt", "Pica"); unit("distance", 0.0254 / 6, false, "pica"); unit("distance", 1609.3472186944, false, "survey_mi");
        unit("time", 31557600, false, "yr"); unit("time", 86400, false, "day", "d"); unit("time", 3600, false, "hr"); unit("time", 60, false, "mn", "min"); unit("time", 1, true, "sec", "s");
        unit("pressure", 1, true, "Pa", "p"); unit("pressure", 101325, true, "atm", "at"); unit("pressure", 133.322, true, "mmHg"); unit("pressure", 6894.75729316836, false, "psi"); unit("pressure", 133.322368421053, false, "Torr");
        unit("force", 1, true, "N"); unit("force", 1e-5, true, "dyn", "dy"); unit("force", 4.4482216152605, false, "lbf"); unit("force", 0.00980665, true, "pond");
        unit("energy", 1, true, "J"); unit("energy", 1e-7, true, "e"); unit("energy", 4.1868, true, "cal"); unit("energy", 4.184, true, "c");
        unit("energy", 1.602176634e-19, true, "eV", "ev"); unit("energy", 2684519.53769617, false, "HPh", "hh"); unit("energy", 3600, true, "Wh", "wh");
        unit("energy", 1.3558179483314, false, "flb"); unit("energy", 1055.05585262, false, "BTU", "btu");
        unit("power", 745.69987158227, false, "HP", "h"); unit("power", 735.49875, false, "PS"); unit("power", 1, true, "W", "w");
        unit("magnetism", 1, true, "T"); unit("magnetism", 1e-4, true, "ga");
        unit("temperature", 1, true, "C", "cel"); unit("temperature", 1, false, "F", "fah"); unit("temperature", 1, true, "K", "kel"); unit("temperature", 1, false, "Rank"); unit("temperature", 1, false, "Reau");
        unit("volume", 4.92892159375e-6, false, "tsp"); unit("volume", 5e-6, false, "tspm"); unit("volume", 1.478676478125e-5, false, "tbs"); unit("volume", 2.95735295625e-5, false, "oz");
        unit("volume", 2.365882365e-4, false, "cup"); unit("volume", 4.73176473e-4, false, "pt", "us_pt"); unit("volume", 5.6826125e-4, false, "uk_pt"); unit("volume", 9.46352946e-4, false, "qt");
        unit("volume", 1.1365225e-3, false, "uk_qt"); unit("volume", 3.785411784e-3, false, "gal"); unit("volume", 4.54609e-3, false, "uk_gal"); unit("volume", 1e-3, true, "l", "L", "lt");
        unit("volume", 1e-30, true, "ang3", "ang^3"); unit("volume", 0.158987294928, false, "barrel"); unit("volume", 0.03523907016688, false, "bushel");
        unit("volume", 0.028316846592, false, "ft3", "ft^3"); unit("volume", 1.6387064e-5, false, "in3", "in^3"); unit("volume", 8.46786664623715e47, false, "ly3", "ly^3");
        unit("volume", 1, true, "m3", "m^3"); unit("volume", 4168181825.44058, false, "mi3", "mi^3"); unit("volume", 0.764554857984, false, "yd3", "yd^3");
        unit("volume", 6352182208.0, false, "Nmi3", "Nmi^3"); unit("volume", 2.83315e-3, false, "GRT", "regton"); unit("volume", 1.13267386368, false, "MTON");
        unit("area", 4046.8564224, false, "uk_acre"); unit("area", 4046.87260987425, false, "us_acre"); unit("area", 1e-20, true, "ang2", "ang^2"); unit("area", 100, true, "ar");
        unit("area", 0.09290304, false, "ft2", "ft^2"); unit("area", 10000, false, "ha"); unit("area", 6.4516e-4, false, "in2", "in^2"); unit("area", 8.95054210748189e31, false, "ly2", "ly^2");
        unit("area", 1, true, "m2", "m^2"); unit("area", 2500, false, "Morgen"); unit("area", 2589988.110336, false, "mi2", "mi^2"); unit("area", 3429904, false, "Nmi2", "Nmi^2");
        unit("area", 0.83612736, false, "yd2", "yd^2");
        unit("information", 1, true, "bit"); unit("information", 8, true, "byte");
        unit("speed", 0.514773333333333, false, "admkn"); unit("speed", 0.514444444444444, false, "kn"); unit("speed", 1 / 3600.0, true, "m/h", "m/hr");
        unit("speed", 1, true, "m/s", "m/sec"); unit("speed", 0.44704, false, "mph");
        String[] p = {"Y", "Z", "E", "P", "T", "G", "M", "k", "h", "da", "e", "d", "c", "m", "u", "n", "p", "f", "a", "z", "y"};
        double[] v = {1e24, 1e21, 1e18, 1e15, 1e12, 1e9, 1e6, 1e3, 1e2, 1e1, 1e1, 1e-1, 1e-2, 1e-3, 1e-6, 1e-9, 1e-12, 1e-15, 1e-18, 1e-21, 1e-24};
        for (int k = 0; k < p.length; k++) PREFIXES.put(p[k], v[k]);
        String[] bp = {"Yi", "Zi", "Ei", "Pi", "Ti", "Gi", "Mi", "ki"};
        for (int k = 0; k < bp.length; k++) BINARY_PREFIXES.put(bp[k], Math.pow(2, 10 * (8 - k)));
    }

    private EngineeringFunctions() {}

    private static void unit(String category, double factor, boolean prefixable, String... names) {
        for (String n : names) UNITS.put(n, new Unit(category, factor, prefixable));
    }

    static void register(FunctionRegistry r) {
        radix(r, "BIN2DEC", 2, 10, -1);
        radix(r, "BIN2HEX", 2, 10, 16);
        radix(r, "BIN2OCT", 2, 10, 8);
        radix(r, "HEX2DEC", 16, 10, -1);
        radix(r, "HEX2BIN", 16, 10, 2);
        radix(r, "HEX2OCT", 16, 10, 8);
        radix(r, "OCT2DEC", 8, 10, -1);
        radix(r, "OCT2BIN", 8, 10, 2);
        radix(r, "OCT2HEX", 8, 10, 16);
        scalar(r, "DEC2BIN", ENGINEERING, 1, 2, (c, a) -> text(fromDecimal(n(a, 0), 2, given(a, 1) ? i(a, 1) : -1, -512, 511)));
        scalar(r, "DEC2OCT", ENGINEERING, 1, 2, (c, a) -> text(fromDecimal(n(a, 0), 8, given(a, 1) ? i(a, 1) : -1, -536870912L, 536870911L)));
        scalar(r, "DEC2HEX", ENGINEERING, 1, 2, (c, a) -> text(fromDecimal(n(a, 0), 16, given(a, 1) ? i(a, 1) : -1, -549755813888L, 549755813887L)));
        scalar(r, "BITAND", ENGINEERING, 2, 2, (c, a) -> num(bits(n(a, 0)) & bits(n(a, 1))));
        scalar(r, "BITOR", ENGINEERING, 2, 2, (c, a) -> num(bits(n(a, 0)) | bits(n(a, 1))));
        scalar(r, "BITXOR", ENGINEERING, 2, 2, (c, a) -> num(bits(n(a, 0)) ^ bits(n(a, 1))));
        scalar(r, "BITLSHIFT", ENGINEERING, 2, 2, (c, a) -> num(shift(bits(n(a, 0)), (int) n(a, 1))));
        scalar(r, "BITRSHIFT", ENGINEERING, 2, 2, (c, a) -> num(shift(bits(n(a, 0)), -(int) n(a, 1))));
        scalar(r, "DELTA", ENGINEERING, 1, 2, (c, a) -> num(n(a, 0) == n(a, 1, 0) ? 1 : 0));
        scalar(r, "GESTEP", ENGINEERING, 1, 2, (c, a) -> num(n(a, 0) >= n(a, 1, 0) ? 1 : 0));
        scalar(r, "ERF", ENGINEERING, 1, 2, (c, a) -> num(given(a, 1) ? SpecialFunctions.erf(n(a, 1)) - SpecialFunctions.erf(n(a, 0)) : SpecialFunctions.erf(n(a, 0))));
        scalar(r, "ERF.PRECISE", ENGINEERING, 1, 1, (c, a) -> num(SpecialFunctions.erf(n(a, 0))));
        scalar(r, "ERFC", ENGINEERING, 1, 1, (c, a) -> num(SpecialFunctions.erfc(n(a, 0))));
        scalar(r, "ERFC.PRECISE", ENGINEERING, 1, 1, (c, a) -> num(SpecialFunctions.erfc(n(a, 0))));
        scalar(r, "BESSELJ", ENGINEERING, 2, 2, (c, a) -> { int order = (int) n(a, 1); if (order < 0) throw EvalError.num(); return num(SpecialFunctions.besselJ(n(a, 0), order)); });
        scalar(r, "BESSELI", ENGINEERING, 2, 2, (c, a) -> { int order = (int) n(a, 1); if (order < 0) throw EvalError.num(); return num(SpecialFunctions.besselI(n(a, 0), order)); });
        scalar(r, "BESSELK", ENGINEERING, 2, 2, (c, a) -> { int order = (int) n(a, 1); if (order < 0) throw EvalError.num(); return num(SpecialFunctions.besselK(n(a, 0), order)); });
        scalar(r, "BESSELY", ENGINEERING, 2, 2, (c, a) -> { int order = (int) n(a, 1); if (order < 0) throw EvalError.num(); return num(SpecialFunctions.besselY(n(a, 0), order)); });
        scalar(r, "CONVERT", ENGINEERING, 3, 3, (c, a) -> num(convert(n(a, 0), t(a, 1), t(a, 2))));
        scalar(r, "COMPLEX", ENGINEERING, 2, 3, (c, a) -> {
            String suffix = t(a, 2, "i");
            if (!suffix.equals("i") && !suffix.equals("j") && !suffix.isEmpty()) throw EvalError.value();
            return text(format(new Complex(n(a, 0), n(a, 1), suffix.isEmpty() ? "i" : suffix)));
        });
        scalar(r, "IMREAL", ENGINEERING, 1, 1, (c, a) -> num(complex(arg(a, 0)).re));
        scalar(r, "IMAGINARY", ENGINEERING, 1, 1, (c, a) -> num(complex(arg(a, 0)).im));
        scalar(r, "IMABS", ENGINEERING, 1, 1, (c, a) -> { Complex z = complex(arg(a, 0)); return num(Math.hypot(z.re, z.im)); });
        scalar(r, "IMARGUMENT", ENGINEERING, 1, 1, (c, a) -> { Complex z = complex(arg(a, 0)); if (z.re == 0 && z.im == 0) throw EvalError.div0(); return num(Math.atan2(z.im, z.re)); });
        scalar(r, "IMCONJUGATE", ENGINEERING, 1, 1, (c, a) -> { Complex z = complex(arg(a, 0)); return text(format(new Complex(z.re, -z.im, z.suffix))); });
        complex1(r, "IMEXP", z -> { double e = Math.exp(z.re); return new Complex(e * Math.cos(z.im), e * Math.sin(z.im), z.suffix); });
        complex1(r, "IMLN", EngineeringFunctions::ln);
        complex1(r, "IMLOG10", z -> scale(ln(z), 1 / Math.log(10)));
        complex1(r, "IMLOG2", z -> scale(ln(z), 1 / Math.log(2)));
        complex1(r, "IMSQRT", z -> { double m = Math.sqrt(Math.hypot(z.re, z.im)), t = Math.atan2(z.im, z.re) / 2; return new Complex(m * Math.cos(t), m * Math.sin(t), z.suffix); });
        complex1(r, "IMSIN", z -> new Complex(Math.sin(z.re) * Math.cosh(z.im), Math.cos(z.re) * Math.sinh(z.im), z.suffix));
        complex1(r, "IMCOS", z -> new Complex(Math.cos(z.re) * Math.cosh(z.im), -Math.sin(z.re) * Math.sinh(z.im), z.suffix));
        complex1(r, "IMSINH", z -> new Complex(Math.sinh(z.re) * Math.cos(z.im), Math.cosh(z.re) * Math.sin(z.im), z.suffix));
        complex1(r, "IMCOSH", z -> new Complex(Math.cosh(z.re) * Math.cos(z.im), Math.sinh(z.re) * Math.sin(z.im), z.suffix));
        complex1(r, "IMTAN", z -> divide(new Complex(Math.sin(z.re) * Math.cosh(z.im), Math.cos(z.re) * Math.sinh(z.im), z.suffix), new Complex(Math.cos(z.re) * Math.cosh(z.im), -Math.sin(z.re) * Math.sinh(z.im), z.suffix)));
        complex1(r, "IMCOT", z -> divide(new Complex(Math.cos(z.re) * Math.cosh(z.im), -Math.sin(z.re) * Math.sinh(z.im), z.suffix), new Complex(Math.sin(z.re) * Math.cosh(z.im), Math.cos(z.re) * Math.sinh(z.im), z.suffix)));
        complex1(r, "IMSEC", z -> divide(new Complex(1, 0, z.suffix), new Complex(Math.cos(z.re) * Math.cosh(z.im), -Math.sin(z.re) * Math.sinh(z.im), z.suffix)));
        complex1(r, "IMCSC", z -> divide(new Complex(1, 0, z.suffix), new Complex(Math.sin(z.re) * Math.cosh(z.im), Math.cos(z.re) * Math.sinh(z.im), z.suffix)));
        complex1(r, "IMSECH", z -> divide(new Complex(1, 0, z.suffix), new Complex(Math.cosh(z.re) * Math.cos(z.im), Math.sinh(z.re) * Math.sin(z.im), z.suffix)));
        complex1(r, "IMCSCH", z -> divide(new Complex(1, 0, z.suffix), new Complex(Math.sinh(z.re) * Math.cos(z.im), Math.cosh(z.re) * Math.sin(z.im), z.suffix)));
        scalar(r, "IMPOWER", ENGINEERING, 2, 2, (c, a) -> {
            Complex z = complex(arg(a, 0));
            double p = n(a, 1);
            double m = Math.pow(Math.hypot(z.re, z.im), p), t = Math.atan2(z.im, z.re) * p;
            return text(format(new Complex(m * Math.cos(t), m * Math.sin(t), z.suffix)));
        });
        scalar(r, "IMDIV", ENGINEERING, 2, 2, (c, a) -> text(format(divide(complex(arg(a, 0)), complex(arg(a, 1))))));
        scalar(r, "IMSUB", ENGINEERING, 2, 2, (c, a) -> { Complex x = complex(arg(a, 0)), y = complex(arg(a, 1)); return text(format(new Complex(x.re - y.re, x.im - y.im, x.suffix))); });
        raw(r, "IMSUM", ENGINEERING, 1, 255, (c, a) -> {
            double re = 0, im = 0;
            String suffix = "i";
            for (CellValue v : flattenArgs(c, a, 0)) { if (v.isEmpty()) continue; Complex z = complex(v); re += z.re; im += z.im; suffix = z.suffix; }
            return text(format(new Complex(re, im, suffix)));
        });
        raw(r, "IMPRODUCT", ENGINEERING, 1, 255, (c, a) -> {
            Complex acc = new Complex(1, 0, "i");
            for (CellValue v : flattenArgs(c, a, 0)) { if (v.isEmpty()) continue; Complex z = complex(v); acc = new Complex(acc.re * z.re - acc.im * z.im, acc.re * z.im + acc.im * z.re, z.suffix); }
            return text(format(acc));
        });
    }

    private static void complex1(FunctionRegistry r, String name, UnaryOperator<Complex> op) {
        scalar(r, name, ENGINEERING, 1, 1, (c, a) -> text(format(op.apply(complex(arg(a, 0))))));
    }

    private static Complex ln(Complex z) {
        if (z.re == 0 && z.im == 0) throw EvalError.num();
        return new Complex(Math.log(Math.hypot(z.re, z.im)), Math.atan2(z.im, z.re), z.suffix);
    }

    private static Complex scale(Complex z, double f) { return new Complex(z.re * f, z.im * f, z.suffix); }

    private static Complex divide(Complex x, Complex y) {
        double d = y.re * y.re + y.im * y.im;
        if (d == 0) throw EvalError.num();
        return new Complex((x.re * y.re + x.im * y.im) / d, (x.im * y.re - x.re * y.im) / d, x.suffix);
    }

    private static Complex complex(CellValue v) {
        if (v instanceof NumberValue n) return new Complex(n.value(), 0, "i");
        String s = dtm.stools.component.panels.editor.sheet.calc.Coerce.text(v).strip();
        if (s.isEmpty()) return new Complex(0, 0, "i");
        if (s.equals("i") || s.equals("j")) return new Complex(0, 1, s);
        if (s.equals("-i") || s.equals("-j")) return new Complex(0, -1, s.substring(1));
        Matcher m = COMPLEX.matcher(s);
        if (!m.matches()) {
            Matcher onlyImag = Pattern.compile("^([+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)?([ij])$").matcher(s);
            if (onlyImag.matches()) {
                String num = onlyImag.group(1);
                double im = num == null || num.equals("+") ? 1 : num.equals("-") ? -1 : Double.parseDouble(num);
                return new Complex(0, im, onlyImag.group(2));
            }
            throw EvalError.num();
        }
        double re = m.group(1) == null ? 0 : Double.parseDouble(m.group(1));
        double im = 0;
        String suffix = "i";
        if (m.group(4) != null) {
            suffix = m.group(4);
            double mag = m.group(3) == null ? 1 : Double.parseDouble(m.group(3));
            im = "-".equals(m.group(2)) ? -mag : mag;
        }
        return new Complex(re, im, suffix);
    }

    private static String format(Complex z) {
        double re = clean(z.re), im = clean(z.im);
        if (im == 0) return fmt(re);
        String imag = im == 1 ? "" : im == -1 ? "-" : fmt(im);
        if (re == 0) return imag + z.suffix;
        return fmt(re) + (im > 0 ? "+" : "") + imag + z.suffix;
    }

    private static double clean(double v) { return Math.abs(v) < 1e-15 ? 0 : NumberValue.round15(v); }
    private static String fmt(double v) { return NumberValue.general(v).replace("E+", "E+"); }

    private static void radix(FunctionRegistry r, String name, int from, int digits, int to) {
        scalar(r, name, ENGINEERING, 1, 2, (c, a) -> {
            CellValue v = arg(a, 0);
            String s = v instanceof NumberValue n ? String.valueOf((long) n.value()) : t(a, 0);
            s = s.strip();
            if (s.length() > 10) throw EvalError.num();
            long value = parseRadix(s, from);
            if (to < 0) return num(value);
            long min = to == 2 ? -512 : to == 8 ? -536870912L : -549755813888L, max = to == 2 ? 511 : to == 8 ? 536870911L : 549755813887L;
            return text(fromDecimal(value, to, given(a, 1) ? i(a, 1) : -1, min, max));
        });
    }

    private static long parseRadix(String s, int radix) {
        if (s.isEmpty()) return 0;
        long v;
        try { v = Long.parseLong(s, radix); } catch (NumberFormatException e) { throw EvalError.num(); }
        int bits = radix == 2 ? 10 : radix == 8 ? 30 : 40;
        if (s.length() == 10 && v >= (1L << (bits - 1))) v -= 1L << bits;
        return v;
    }

    private static String fromDecimal(double value, int radix, int places, long min, long max) {
        long v = (long) Math.floor(value);
        if (v < min || v > max) throw EvalError.num();
        if (v < 0) {
            int bits = radix == 2 ? 10 : radix == 8 ? 30 : 40;
            return Long.toString((1L << bits) + v, radix).toUpperCase(Locale.ROOT);
        }
        String s = Long.toString(v, radix).toUpperCase(Locale.ROOT);
        if (places >= 0) {
            if (places < s.length() || places > 10) throw EvalError.num();
            while (s.length() < places) s = "0" + s;
        }
        return s;
    }

    private static long bits(double v) {
        if (v < 0 || v != Math.floor(v) || v >= 281474976710656.0) throw EvalError.num();
        return (long) v;
    }

    private static double shift(long v, int by) {
        if (Math.abs(by) > 53) throw EvalError.num();
        double r = by >= 0 ? v * Math.pow(2, by) : Math.floor(v / Math.pow(2, -by));
        if (r >= 281474976710656.0) throw EvalError.num();
        return r;
    }

    private static double convert(double value, String from, String to) {
        double[] f = resolve(from), t = resolve(to);
        Unit uf = UNITS.get(baseName(from)), ut = UNITS.get(baseName(to));
        if (!uf.category.equals(ut.category)) throw EvalError.na();
        if (uf.category.equals("temperature")) return fromKelvin(toKelvin(value * f[0], baseName(from)), baseName(to)) / t[0];
        return value * f[0] * uf.factor / (ut.factor * t[0]);
    }

    private static String baseName(String name) {
        if (UNITS.containsKey(name)) return name;
        for (String p : List.of("da", "Yi", "Zi", "Ei", "Pi", "Ti", "Gi", "Mi", "ki")) if (name.startsWith(p) && UNITS.containsKey(name.substring(2))) return name.substring(2);
        if (name.length() > 1 && UNITS.containsKey(name.substring(1))) return name.substring(1);
        throw EvalError.na();
    }

    private static double[] resolve(String name) {
        if (UNITS.containsKey(name)) return new double[]{1};
        String base = baseName(name);
        Unit u = UNITS.get(base);
        if (!u.prefixable) throw EvalError.na();
        String prefix = name.substring(0, name.length() - base.length());
        Double p = PREFIXES.get(prefix);
        if (p == null && u.category.equals("information")) p = BINARY_PREFIXES.get(prefix);
        if (p == null) throw EvalError.na();
        double factor = p;
        if (base.endsWith("2") || base.endsWith("^2")) factor = p * p;
        if (base.endsWith("3") || base.endsWith("^3")) factor = p * p * p;
        return new double[]{factor};
    }

    private static double toKelvin(double v, String unit) {
        return switch (unit) {
            case "C", "cel" -> v + 273.15;
            case "F", "fah" -> (v - 32) * 5 / 9 + 273.15;
            case "Rank" -> v * 5 / 9;
            case "Reau" -> v * 1.25 + 273.15;
            default -> v;
        };
    }

    private static double fromKelvin(double v, String unit) {
        return switch (unit) {
            case "C", "cel" -> v - 273.15;
            case "F", "fah" -> (v - 273.15) * 9 / 5 + 32;
            case "Rank" -> v * 9 / 5;
            case "Reau" -> (v - 273.15) * 0.8;
            default -> v;
        };
    }
}
