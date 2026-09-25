package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.FormulaPrinter;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class XlsxFormulas {
    private static final Pattern PREFIXES = Pattern.compile("(?i)_xlfn\\.|_xlws\\.|_xlpm\\.");
    private static final Set<String> WORKSHEET = Set.of("FILTER", "SORT");
    private static final Set<String> FUTURE = Set.of("AGGREGATE", "BETA.DIST", "BETA.INV", "BINOM.DIST", "BINOM.INV", "CEILING.PRECISE", "CHISQ.DIST", "CHISQ.DIST.RT",
            "CHISQ.INV", "CHISQ.INV.RT", "CHISQ.TEST", "CONFIDENCE.NORM", "CONFIDENCE.T", "COVARIANCE.P", "COVARIANCE.S", "ERF.PRECISE", "ERFC.PRECISE", "EXPON.DIST",
            "F.DIST", "F.DIST.RT", "F.INV", "F.INV.RT", "F.TEST", "FLOOR.PRECISE", "GAMMA.DIST", "GAMMA.INV", "GAMMALN.PRECISE", "HYPGEOM.DIST", "LOGNORM.DIST",
            "LOGNORM.INV", "MODE.MULT", "MODE.SNGL", "NEGBINOM.DIST", "NETWORKDAYS.INTL", "NORM.DIST", "NORM.INV", "NORM.S.DIST", "NORM.S.INV", "PERCENTILE.EXC",
            "PERCENTILE.INC", "PERCENTRANK.EXC", "PERCENTRANK.INC", "POISSON.DIST", "QUARTILE.EXC", "QUARTILE.INC", "RANK.AVG", "RANK.EQ", "STDEV.P", "STDEV.S",
            "T.DIST", "T.DIST.2T", "T.DIST.RT", "T.INV", "T.INV.2T", "T.TEST", "VAR.P", "VAR.S", "WEIBULL.DIST", "WORKDAY.INTL", "Z.TEST", "ISO.CEILING",
            "ACOT", "ACOTH", "ARABIC", "BASE", "BINOM.DIST.RANGE", "BITAND", "BITLSHIFT", "BITOR", "BITRSHIFT", "BITXOR", "CEILING.MATH", "COMBINA", "COT", "COTH",
            "CSC", "CSCH", "DAYS", "DECIMAL", "ENCODEURL", "FILTERXML", "FLOOR.MATH", "FORMULATEXT", "GAMMA", "GAUSS", "IFNA", "IMCOSH", "IMCOT", "IMCSC", "IMCSCH",
            "IMSEC", "IMSECH", "IMSINH", "IMTAN", "ISFORMULA", "ISOWEEKNUM", "MUNIT", "NUMBERVALUE", "PDURATION", "PERMUTATIONA", "PHI", "RRI", "SEC", "SECH",
            "SHEET", "SHEETS", "SKEW.P", "UNICHAR", "UNICODE", "WEBSERVICE", "XOR", "CONCAT", "FORECAST.ETS", "FORECAST.ETS.CONFINT", "FORECAST.ETS.SEASONALITY",
            "FORECAST.ETS.STAT", "FORECAST.LINEAR", "IFS", "MAXIFS", "MINIFS", "SWITCH", "TEXTJOIN", "FILTER", "SORT", "SORTBY", "UNIQUE", "SEQUENCE", "RANDARRAY",
            "XLOOKUP", "XMATCH", "LET", "LAMBDA", "MAP", "REDUCE", "SCAN", "BYROW", "BYCOL", "MAKEARRAY", "ISOMITTED", "TAKE", "DROP", "CHOOSEROWS", "CHOOSECOLS",
            "VSTACK", "HSTACK", "TOCOL", "TOROW", "WRAPROWS", "WRAPCOLS", "EXPAND", "TEXTBEFORE", "TEXTAFTER", "TEXTSPLIT", "VALUETOTEXT", "ARRAYTOTEXT", "GROUPBY",
            "PIVOTBY", "TRIMRANGE", "REGEXTEST", "REGEXEXTRACT", "REGEXREPLACE", "IMAGE", "STOCKHISTORY");

    private XlsxFormulas() {}

    public static String fromFile(String text) {
        String t = text.startsWith("=") ? text.substring(1) : text;
        t = PREFIXES.matcher(t).replaceAll("");
        try { return Formulas.toCanonical(t, FormulaLocale.EN); } catch (RuntimeException e) { return t; }
    }

    public static String toFile(String canonical) {
        try {
            return new FormulaPrinter(FormulaLocale.EN)
                    .withFunctionMapper(XlsxFormulas::prefix)
                    .withNameMapper((name, scope) -> scope.contains(name.toUpperCase(Locale.ROOT)) ? "_xlpm." + name : name)
                    .print(Formulas.parseCanonical(canonical));
        } catch (RuntimeException e) {
            return canonical;
        }
    }

    public static String prefix(String name) {
        String u = name.toUpperCase(Locale.ROOT);
        if (WORKSHEET.contains(u)) return "_xlfn._xlws." + u;
        if (FUTURE.contains(u)) return "_xlfn." + u;
        return u;
    }

    public static boolean isFuture(String name) { return FUTURE.contains(name.toUpperCase(Locale.ROOT)); }
}
