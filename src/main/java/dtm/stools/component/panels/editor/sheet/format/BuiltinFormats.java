package dtm.stools.component.panels.editor.sheet.format;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuiltinFormats {
    private static final Map<Integer, String> FORMATS = new HashMap<>();
    private static final Map<String, Integer> IDS = new HashMap<>();

    static {
        put(0, "General"); put(1, "0"); put(2, "0.00"); put(3, "#,##0"); put(4, "#,##0.00");
        put(5, "\"$\"#,##0_);(\"$\"#,##0)"); put(6, "\"$\"#,##0_);[Red](\"$\"#,##0)"); put(7, "\"$\"#,##0.00_);(\"$\"#,##0.00)"); put(8, "\"$\"#,##0.00_);[Red](\"$\"#,##0.00)");
        put(9, "0%"); put(10, "0.00%"); put(11, "0.00E+00"); put(12, "# ?/?"); put(13, "# ??/??"); put(14, "dd/mm/yyyy"); put(15, "d-mmm-yy");
        put(16, "d-mmm"); put(17, "mmm-yy"); put(18, "h:mm AM/PM"); put(19, "h:mm:ss AM/PM"); put(20, "h:mm"); put(21, "h:mm:ss"); put(22, "dd/mm/yyyy hh:mm");
        put(37, "#,##0_);(#,##0)"); put(38, "#,##0_);[Red](#,##0)"); put(39, "#,##0.00_);(#,##0.00)"); put(40, "#,##0.00_);[Red](#,##0.00)");
        put(41, "_(* #,##0_);_(* (#,##0);_(* \"-\"_);_(@_)"); put(42, "_(\"$\"* #,##0_);_(\"$\"* (#,##0);_(\"$\"* \"-\"_);_(@_)");
        put(43, "_(* #,##0.00_);_(* (#,##0.00);_(* \"-\"??_);_(@_)"); put(44, "_(\"$\"* #,##0.00_);_(\"$\"* (#,##0.00);_(\"$\"* \"-\"??_);_(@_)");
        put(45, "mm:ss"); put(46, "[h]:mm:ss"); put(47, "mm:ss.0"); put(48, "##0.0E+0"); put(49, "@");
    }

    private BuiltinFormats() {}

    private static void put(int id, String code) { FORMATS.put(id, code); IDS.putIfAbsent(code, id); }

    public static String code(int id) { return FORMATS.get(id); }
    public static Integer id(String code) { return IDS.get(code); }
    public static boolean isBuiltin(int id) { return FORMATS.containsKey(id); }

    public static final String CURRENCY_BRL = "\"R$\" #,##0.00";
    public static final String ACCOUNTING_BRL = "_-\"R$\"* #,##0.00_-;-\"R$\"* #,##0.00_-;_-\"R$\"* \"-\"??_-;_-@_-";

    public static Map<String, List<String>> categories() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("Geral", List.of("General"));
        m.put("Número", List.of("0", "0.00", "#,##0", "#,##0.00", "#,##0.00;[Red]-#,##0.00", "#,##0.00;(#,##0.00)"));
        m.put("Moeda", List.of(CURRENCY_BRL, "\"R$\" #,##0.00;[Red]-\"R$\" #,##0.00", "[$$-409]#,##0.00", "[$€-2] #,##0.00"));
        m.put("Contábil", List.of(ACCOUNTING_BRL, "_(* #,##0.00_);_(* (#,##0.00);_(* \"-\"??_);_(@_)"));
        m.put("Data", List.of("dd/mm/yyyy", "d/m/yy", "dd/mm/yy", "d \"de\" mmmm \"de\" yyyy", "dddd, d \"de\" mmmm \"de\" yyyy", "mmm-yy", "d-mmm", "yyyy-mm-dd"));
        m.put("Hora", List.of("hh:mm", "hh:mm:ss", "h:mm AM/PM", "[h]:mm:ss", "dd/mm/yyyy hh:mm"));
        m.put("Porcentagem", List.of("0%", "0.00%"));
        m.put("Fração", List.of("# ?/?", "# ??/??", "# ?/2", "# ?/4", "# ?/8", "# ??/16", "# ?/10", "# ??/100"));
        m.put("Científico", List.of("0.00E+00", "##0.0E+0"));
        m.put("Texto", List.of("@"));
        m.put("Especial", List.of("00000-000", "000.000.000-00", "00.000.000/0000-00", "(00) 00000-0000"));
        return m;
    }
}
