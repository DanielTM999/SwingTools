package dtm.stools.component.panels.editor.sheet.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class SheetWorkbook {
    private final List<SheetWorksheet> sheets;
    private final SheetStylePool styles;
    private WorkbookProperties properties;

    public SheetWorkbook() { this(new ArrayList<>(), new SheetStylePool(), WorkbookProperties.DEFAULT); }

    public SheetWorkbook(List<SheetWorksheet> sheets, SheetStylePool styles, WorkbookProperties properties) {
        this.sheets = new ArrayList<>(sheets);
        this.styles = Objects.requireNonNull(styles);
        this.properties = Objects.requireNonNull(properties);
    }

    public static SheetWorkbook create() { return create("Planilha1"); }

    public static SheetWorkbook create(String firstSheet) {
        SheetWorkbook wb = new SheetWorkbook();
        wb.sheets.add(new SheetWorksheet(firstSheet));
        return wb;
    }

    public List<SheetWorksheet> sheets() { return Collections.unmodifiableList(sheets); }
    public int sheetCount() { return sheets.size(); }
    public SheetWorksheet sheet(int index) { return sheets.get(index); }
    public SheetStylePool styles() { return styles; }
    public WorkbookProperties properties() { return properties; }
    public void setProperties(WorkbookProperties value) { properties = Objects.requireNonNull(value); }

    public int activeSheetIndex() { return Math.min(properties.activeSheet(), Math.max(0, sheets.size() - 1)); }
    public SheetWorksheet activeSheet() { return sheets.get(activeSheetIndex()); }

    public Optional<SheetWorksheet> sheet(String name) {
        for (SheetWorksheet s : sheets) if (s.name().equalsIgnoreCase(name)) return Optional.of(s);
        return Optional.empty();
    }

    public int indexOf(String name) {
        for (int i = 0; i < sheets.size(); i++) if (sheets.get(i).name().equalsIgnoreCase(name)) return i;
        return -1;
    }

    public int indexOfId(String id) {
        for (int i = 0; i < sheets.size(); i++) if (sheets.get(i).id().equals(id)) return i;
        return -1;
    }

    public SheetWorksheet sheetById(String id) { int i = indexOfId(id); return i < 0 ? null : sheets.get(i); }

    public List<SheetWorksheet> replaceSheets(List<SheetWorksheet> value) {
        List<SheetWorksheet> previous = new ArrayList<>(sheets);
        sheets.clear(); sheets.addAll(value);
        return previous;
    }

    public void addSheet(int index, SheetWorksheet sheet) {
        if (indexOf(sheet.name()) >= 0) throw new IllegalArgumentException("Duplicate sheet name: " + sheet.name());
        sheets.add(Math.max(0, Math.min(index, sheets.size())), sheet);
    }

    public String uniqueSheetName(String base) {
        String candidate = base;
        for (int i = 2; indexOf(candidate) >= 0; i++) candidate = base + " (" + i + ")";
        return candidate;
    }

    public String nextSheetName() {
        int i = sheets.size() + 1;
        while (indexOf("Planilha" + i) >= 0) i++;
        return "Planilha" + i;
    }

    public static void validateSheetName(String name) {
        if (name == null || name.isBlank() || name.length() > 31) throw new IllegalArgumentException("O nome da planilha deve ter de 1 a 31 caracteres.");
        for (char c : name.toCharArray()) if ("\\/?*[]:".indexOf(c) >= 0) throw new IllegalArgumentException("O nome da planilha não pode conter \\ / ? * [ ] :");
        if (name.startsWith("'") || name.endsWith("'")) throw new IllegalArgumentException("O nome da planilha não pode começar ou terminar com apóstrofo.");
        if (name.equalsIgnoreCase("History")) throw new IllegalArgumentException("Nome reservado.");
    }

    public Optional<DefinedName> name(String name, Integer scope) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (scope != null) for (DefinedName n : properties.names()) if (Objects.equals(n.sheetScope(), scope) && n.name().toUpperCase(Locale.ROOT).equals(upper)) return Optional.of(n);
        for (DefinedName n : properties.names()) if (n.sheetScope() == null && n.name().toUpperCase(Locale.ROOT).equals(upper)) return Optional.of(n);
        return Optional.empty();
    }

    public Optional<SheetTable> table(String name) {
        for (SheetWorksheet s : sheets) for (SheetTable t : s.properties().tables()) if (t.nameMatches(name)) return Optional.of(t);
        return Optional.empty();
    }

    public int sheetOfTable(String name) {
        for (int i = 0; i < sheets.size(); i++) for (SheetTable t : sheets.get(i).properties().tables()) if (t.nameMatches(name)) return i;
        return -1;
    }

    public int nextTableId() {
        int max = 0;
        for (SheetWorksheet s : sheets) for (SheetTable t : s.properties().tables()) max = Math.max(max, t.id());
        return max + 1;
    }

    public CellStyle style(int id) { return styles.get(id); }

    public SheetWorkbook snapshot() {
        List<SheetWorksheet> copy = new ArrayList<>();
        for (SheetWorksheet s : sheets) copy.add(s.snapshot());
        return new SheetWorkbook(copy, styles, properties);
    }
}
