package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.model.BorderStyle;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.FillPattern;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.SheetBorder;
import dtm.stools.component.panels.editor.sheet.model.SheetFill;
import dtm.stools.component.panels.editor.sheet.model.UnderlineStyle;
import dtm.stools.component.panels.editor.sheet.model.VerticalAlignment;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class FormatCellsPanel extends JPanel {
    public static final int NUMBER = 0, ALIGNMENT = 1, FONT = 2, BORDER = 3, FILL = 4, PROTECTION = 5;

    private final CellStyle initial;
    private final NumberFormatter formatter;
    private final CellValue sample;
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<String, List<String>> categories = new LinkedHashMap<>();
    private final JList<String> categoryList;
    private final DefaultListModel<String> codes = new DefaultListModel<>();
    private final JList<String> codeList = new JList<>(codes);
    private final JTextField code = new JTextField(24);
    private final JLabel preview = new JLabel(" ");
    private final JSpinner decimals = SheetForm.integer(2, 0, 30);
    private final JCheckBox thousands = SheetForm.check("Usar separador de milhar", true);
    private final JComboBox<String> horizontal = SheetForm.combo("Geral", "Esquerda (Recuo)", "Centro", "Direita (Recuo)", "Preencher", "Justificar", "Centralizar seleção", "Distribuído");
    private final JComboBox<String> vertical = SheetForm.combo("Superior", "Centro", "Inferior", "Justificar", "Distribuído");
    private final JSpinner indent = SheetForm.integer(0, 0, 250);
    private final JCheckBox wrap = SheetForm.check("Quebrar texto automaticamente", false), shrink = SheetForm.check("Reduzir para caber", false), verticalText = SheetForm.check("Texto vertical", false);
    private final JSpinner rotation = SheetForm.integer(0, -90, 90);
    private final JComboBox<String> family;
    private final JComboBox<String> fontStyle = SheetForm.combo("Regular", "Itálico", "Negrito", "Negrito Itálico");
    private final JSpinner size = SheetForm.number(11, 1, 409, 0.5);
    private final JComboBox<String> underline = SheetForm.combo("Nenhum", "Simples", "Duplo", "Simples Contábil", "Duplo Contábil");
    private final ColorButton fontColor;
    private final JCheckBox strike = SheetForm.check("Tachado", false), superscript = SheetForm.check("Sobrescrito", false), subscript = SheetForm.check("Subscrito", false);
    private final JComboBox<BorderStyle> lineStyle = new JComboBox<>(BorderStyle.values());
    private final ColorButton lineColor = new ColorButton(0xFF000000, "Automático");
    private final JCheckBox top = SheetForm.check("Superior", false), bottom = SheetForm.check("Inferior", false), left = SheetForm.check("Esquerda", false), right = SheetForm.check("Direita", false),
            diagUp = SheetForm.check("Diagonal para cima", false), diagDown = SheetForm.check("Diagonal para baixo", false);
    private final JComboBox<String> borderScope = SheetForm.combo("Aplicar a cada célula", "Aplicar ao contorno da seleção", "Não alterar bordas");
    private final ColorButton background;
    private final JComboBox<FillPattern> pattern = new JComboBox<>(FillPattern.values());
    private final ColorButton patternColor;
    private final JCheckBox locked = SheetForm.check("Bloqueadas", true), hidden = SheetForm.check("Ocultas", false);

    public FormatCellsPanel(CellStyle style, NumberFormatter formatter, CellValue sample, Map<String, List<String>> extraFormats, int initialTab) {
        super(new BorderLayout());
        this.initial = style;
        this.formatter = formatter;
        this.sample = sample == null || sample.isEmpty() ? CellValue.of(1234.5678) : sample;
        String cur = formatter.decimalSeparator() == ',' ? "\"R$\" " : "\"$\"";
        categories.put("Geral", List.of("General"));
        categories.put("Número", List.of("0", "0.00", "#,##0", "#,##0.00", "#,##0.00;[Red]#,##0.00", "#,##0.00;-#,##0.00", "#,##0.00;[Red]-#,##0.00"));
        categories.put("Moeda", List.of(cur + "#,##0.00", cur + "#,##0.00;[Red]-" + cur + "#,##0.00", cur + "#,##0", "\"US$\" #,##0.00", "#,##0.00 \"€\""));
        categories.put("Contábil", List.of("_-" + cur + "* #,##0.00_-;-" + cur + "* #,##0.00_-;_-" + cur + "* \"-\"??_-;_-@_-", "_-* #,##0.00_-;-* #,##0.00_-;_-* \"-\"??_-;_-@_-"));
        categories.put("Data", List.of("dd/mm/yyyy", "d/m/yy", "dd-mmm-yy", "d \"de\" mmmm \"de\" yyyy", "dddd, d \"de\" mmmm \"de\" yyyy", "mmm-yy", "mmmm-yy", "yyyy-mm-dd", "dd/mm/yyyy hh:mm"));
        categories.put("Hora", List.of("hh:mm", "hh:mm:ss", "h:mm AM/PM", "h:mm:ss AM/PM", "[h]:mm:ss", "mm:ss.0"));
        categories.put("Porcentagem", List.of("0%", "0.00%", "0.0%"));
        categories.put("Fração", List.of("# ?/?", "# ??/??", "# ???/???", "# ?/2", "# ?/4", "# ?/8", "# ??/16", "# ?/10", "# ??/100"));
        categories.put("Científico", List.of("0.00E+00", "0.0E+0", "##0.0E+0"));
        categories.put("Texto", List.of("@"));
        categories.put("Especial", List.of("00000-000", "000\\.000\\.000\\-00", "00\\.000\\.000\\/0000\\-00", "(00) 0000-0000", "(00) 00000-0000"));
        if (extraFormats != null) extraFormats.forEach((k, v) -> categories.merge(k, List.copyOf(v), (a, b) -> { List<String> l = new ArrayList<>(a); l.addAll(b); return l; }));
        categories.put("Personalizado", List.of("General", "0", "0.00", "#,##0", "#,##0.00", "0%", "0.00%", "0.00E+00", "# ?/?", "dd/mm/yyyy", "hh:mm:ss", "@", ";;;"));
        categoryList = new JList<>(categories.keySet().toArray(String[]::new));
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        family = new JComboBox<>(fonts);
        family.setEditable(true);
        fontColor = new ColorButton(style.fontColor(), "Automático");
        background = new ColorButton(style.fill().visible() ? style.fill().primaryColor() : null, "Sem Cor");
        patternColor = new ColorButton(style.fill().pattern() != FillPattern.SOLID && style.fill().visible() ? style.fill().foreground() : null, "Automático");
        tabs.addTab("Número", numberTab());
        tabs.addTab("Alinhamento", alignmentTab());
        tabs.addTab("Fonte", fontTab());
        tabs.addTab("Borda", borderTab());
        tabs.addTab("Preenchimento", fillTab());
        tabs.addTab("Proteção", protectionTab());
        tabs.setSelectedIndex(Math.max(0, Math.min(5, initialTab)));
        add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(620, 400));
        load();
    }

    private JPanel numberTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        JScrollPane cats = new JScrollPane(categoryList);
        cats.setPreferredSize(new Dimension(150, 300));
        p.add(cats, BorderLayout.WEST);
        SheetForm right = new SheetForm();
        right.add("Exemplo:", preview);
        right.add("Casas decimais:", decimals);
        right.full(thousands);
        right.add("Código:", code);
        JScrollPane list = new JScrollPane(codeList);
        list.setPreferredSize(new Dimension(360, 180));
        right.grow(list);
        p.add(right, BorderLayout.CENTER);
        categoryList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showCategory(); });
        codeList.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && codeList.getSelectedValue() != null) code.setText(codeList.getSelectedValue()); });
        decimals.addChangeListener(e -> rebuildNumber());
        thousands.addActionListener(e -> rebuildNumber());
        code.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updatePreview(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePreview(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePreview(); }
        });
        return p;
    }

    private void showCategory() {
        String c = categoryList.getSelectedValue();
        if (c == null) return;
        codes.clear();
        categories.get(c).forEach(codes::addElement);
        boolean numeric = c.equals("Número") || c.equals("Moeda") || c.equals("Porcentagem") || c.equals("Científico") || c.equals("Contábil");
        decimals.setEnabled(numeric);
        thousands.setEnabled(c.equals("Número"));
        if (!c.equals("Personalizado") && !categories.get(c).contains(code.getText())) code.setText(categories.get(c).getFirst());
    }

    private void rebuildNumber() {
        String c = categoryList.getSelectedValue();
        if (c == null) return;
        int d = SheetForm.integer(decimals);
        String dec = d == 0 ? "" : "." + "0".repeat(d);
        String cur = formatter.decimalSeparator() == ',' ? "\"R$\" " : "\"$\"";
        switch (c) {
            case "Número" -> code.setText((thousands.isSelected() ? "#,##0" : "0") + dec);
            case "Moeda" -> code.setText(cur + "#,##0" + dec);
            case "Contábil" -> code.setText("_-" + cur + "* #,##0" + dec + "_-;-" + cur + "* #,##0" + dec + "_-;_-" + cur + "* \"-\"??_-;_-@_-");
            case "Porcentagem" -> code.setText("0" + dec + "%");
            case "Científico" -> code.setText("0" + dec + "E+00");
            default -> { }
        }
    }

    private void updatePreview() {
        try {
            var f = formatter.format(sample, code.getText().isBlank() ? "General" : code.getText());
            preview.setText(f.text().isEmpty() ? " " : f.text());
            preview.setForeground(f.color() == null ? null : new java.awt.Color(f.color(), true));
        } catch (RuntimeException failure) {
            preview.setText("Formato inválido");
        }
    }

    private JPanel alignmentTab() {
        SheetForm f = new SheetForm();
        f.section("Alinhamento de texto");
        f.add("Horizontal:", horizontal);
        f.add("Vertical:", vertical);
        f.add("Recuo:", indent);
        f.section("Controle de texto");
        f.full(wrap);
        f.full(shrink);
        f.section("Orientação");
        f.add("Graus:", rotation);
        f.full(verticalText);
        return wrapTop(f);
    }

    private JPanel fontTab() {
        SheetForm f = new SheetForm();
        f.add("Fonte:", family);
        f.add("Estilo:", fontStyle);
        f.add("Tamanho:", size);
        f.add("Sublinhado:", underline);
        f.add("Cor:", fontColor);
        f.section("Efeitos");
        f.full(strike);
        f.full(superscript);
        f.full(subscript);
        return wrapTop(f);
    }

    private JPanel borderTab() {
        SheetForm f = new SheetForm();
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton none = new JButton("Nenhuma"), outline = new JButton("Contorno"), all = new JButton("Todas");
        none.addActionListener(e -> { for (JCheckBox b : new JCheckBox[]{top, bottom, left, right, diagUp, diagDown}) b.setSelected(false); borderScope.setSelectedIndex(0); });
        outline.addActionListener(e -> { for (JCheckBox b : new JCheckBox[]{top, bottom, left, right}) b.setSelected(true); borderScope.setSelectedIndex(1); });
        all.addActionListener(e -> { for (JCheckBox b : new JCheckBox[]{top, bottom, left, right}) b.setSelected(true); borderScope.setSelectedIndex(0); });
        presets.add(none);
        presets.add(outline);
        presets.add(all);
        f.full(presets);
        f.add("Estilo da linha:", lineStyle);
        lineStyle.setSelectedItem(BorderStyle.THIN);
        f.add("Cor:", lineColor);
        f.full(top); f.full(bottom); f.full(left); f.full(right); f.full(diagUp); f.full(diagDown);
        f.add("Modo:", borderScope);
        borderScope.setSelectedIndex(2);
        for (JCheckBox b : new JCheckBox[]{top, bottom, left, right, diagUp, diagDown}) b.addActionListener(e -> { if (borderScope.getSelectedIndex() == 2) borderScope.setSelectedIndex(0); });
        return wrapTop(f);
    }

    private JPanel fillTab() {
        SheetForm f = new SheetForm();
        f.add("Cor do plano de fundo:", background);
        JButton noFill = new JButton("Sem Cor");
        noFill.addActionListener(e -> { background.setColor(null); pattern.setSelectedItem(FillPattern.NONE); });
        f.full(noFill);
        f.add("Estilo do padrão:", pattern);
        f.add("Cor do padrão:", patternColor);
        background.addPropertyChangeListener("color", e -> { if (background.color() != null && pattern.getSelectedItem() == FillPattern.NONE) pattern.setSelectedItem(FillPattern.SOLID); });
        return wrapTop(f);
    }

    private JPanel protectionTab() {
        SheetForm f = new SheetForm();
        f.full(locked);
        f.full(hidden);
        f.full(new JLabel("<html><div style='width:400px'>Bloquear células ou ocultar fórmulas só terá efeito depois que a planilha for protegida (guia Revisão, Proteger Planilha).</div></html>"));
        return wrapTop(f);
    }

    private static JPanel wrapTop(JPanel p) {
        JPanel w = new JPanel(new BorderLayout());
        w.add(p, BorderLayout.NORTH);
        return w;
    }

    private void load() {
        String fmt = initial.numberFormat() == null ? "General" : initial.numberFormat();
        String cat = "Personalizado";
        for (Map.Entry<String, List<String>> e : categories.entrySet()) if (!e.getKey().equals("Personalizado") && e.getValue().contains(fmt)) { cat = e.getKey(); break; }
        if (cat.equals("Personalizado")) {
            if (fmt.contains("%")) cat = "Porcentagem";
            else if (formatter.isDateFormat(fmt)) cat = fmt.matches(".*[dy].*") ? "Data" : "Hora";
        }
        categoryList.setSelectedValue(cat, true);
        showCategory();
        code.setText(fmt);
        horizontal.setSelectedIndex(initial.horizontal().ordinal());
        vertical.setSelectedIndex(initial.vertical().ordinal());
        indent.setValue(initial.indent());
        wrap.setSelected(initial.wrap());
        shrink.setSelected(initial.shrinkToFit());
        verticalText.setSelected(initial.isVertical());
        int rot = initial.rotation();
        rotation.setValue(rot == 255 ? 0 : rot > 90 ? 90 - rot : rot);
        family.setSelectedItem(initial.fontFamily());
        fontStyle.setSelectedIndex((initial.bold() ? 2 : 0) + (initial.italic() ? 1 : 0));
        size.setValue(initial.fontSize());
        underline.setSelectedIndex(initial.underline().ordinal());
        strike.setSelected(initial.strikethrough());
        superscript.setSelected(initial.superscript());
        subscript.setSelected(initial.subscript());
        top.setSelected(initial.top().visible());
        bottom.setSelected(initial.bottom().visible());
        left.setSelected(initial.left().visible());
        right.setSelected(initial.right().visible());
        diagUp.setSelected(initial.diagonalUp());
        diagDown.setSelected(initial.diagonalDown());
        pattern.setSelectedItem(initial.fill().pattern());
        locked.setSelected(initial.locked());
        hidden.setSelected(initial.hidden());
        updatePreview();
    }

    public UnaryOperator<CellStyle> result() {
        String fmt = code.getText().isBlank() ? "General" : code.getText().strip();
        formatter.parse(fmt);
        HorizontalAlignment h = HorizontalAlignment.values()[horizontal.getSelectedIndex()];
        VerticalAlignment v = VerticalAlignment.values()[vertical.getSelectedIndex()];
        int ind = SheetForm.integer(indent);
        int r = SheetForm.integer(rotation);
        int rot = verticalText.isSelected() ? 255 : r >= 0 ? r : 90 - r;
        String fam = Objects.toString(family.getSelectedItem(), initial.fontFamily());
        boolean b = fontStyle.getSelectedIndex() >= 2, i = fontStyle.getSelectedIndex() % 2 == 1;
        double sz = SheetForm.number(size);
        UnderlineStyle u = UnderlineStyle.values()[underline.getSelectedIndex()];
        Integer fc = fontColor.color();
        int bs = borderScope.getSelectedIndex();
        SheetBorder line = SheetBorder.of((BorderStyle) lineStyle.getSelectedItem(), lineColor.color());
        FillPattern pat = (FillPattern) pattern.getSelectedItem();
        Integer bg = background.color(), pc = patternColor.color();
        SheetFill fill = pat == FillPattern.NONE || bg == null && pc == null ? SheetFill.NONE : pat == FillPattern.SOLID ? SheetFill.solid(bg == null ? pc : bg) : SheetFill.pattern(pat, pc == null ? 0xFF000000 : pc, bg);
        boolean fillChanged = !Objects.equals(fill, initial.fill()) && !(fill.equals(SheetFill.NONE) && !initial.fill().visible());
        boolean fillTouched = !Objects.equals(bg, initial.fill().visible() ? initial.fill().primaryColor() : null) || pat != initial.fill().pattern();
        return s -> {
            CellStyle n = s;
            if (!fmt.equals(initial.numberFormat())) n = n.withNumberFormat(fmt);
            if (h != initial.horizontal()) n = n.withHorizontal(h);
            if (v != initial.vertical()) n = n.withVertical(v);
            if (ind != initial.indent()) n = n.withIndent(ind);
            if (wrap.isSelected() != initial.wrap()) n = n.withWrap(wrap.isSelected());
            if (shrink.isSelected() != initial.shrinkToFit()) n = n.withShrinkToFit(shrink.isSelected());
            if (rot != initial.rotation()) n = n.withRotation(rot);
            if (!fam.equals(initial.fontFamily())) n = n.withFontFamily(fam);
            if (b != initial.bold()) n = n.withBold(b);
            if (i != initial.italic()) n = n.withItalic(i);
            if (sz != initial.fontSize()) n = n.withFontSize(sz);
            if (u != initial.underline()) n = n.withUnderline(u);
            if (!Objects.equals(fc, initial.fontColor())) n = n.withFontColor(fc);
            if (strike.isSelected() != initial.strikethrough()) n = n.withStrikethrough(strike.isSelected());
            if (superscript.isSelected() != initial.superscript()) n = n.withSuperscript(superscript.isSelected()).withSubscript(false);
            if (subscript.isSelected() != initial.subscript()) n = n.withSubscript(subscript.isSelected()).withSuperscript(false);
            if (bs == 0) {
                n = n.withTop(top.isSelected() ? line : SheetBorder.NONE).withBottom(bottom.isSelected() ? line : SheetBorder.NONE)
                        .withLeft(left.isSelected() ? line : SheetBorder.NONE).withRight(right.isSelected() ? line : SheetBorder.NONE)
                        .withDiagonal(diagUp.isSelected() || diagDown.isSelected() ? line : SheetBorder.NONE).withDiagonalUp(diagUp.isSelected()).withDiagonalDown(diagDown.isSelected());
            }
            if (fillChanged || fillTouched) n = n.withFill(fill);
            if (locked.isSelected() != initial.locked()) n = n.withLocked(locked.isSelected());
            if (hidden.isSelected() != initial.hidden()) n = n.withHidden(hidden.isSelected());
            return n;
        };
    }

    public SheetBorder outlineBorder() {
        if (borderScope.getSelectedIndex() != 1) return null;
        return SheetBorder.of((BorderStyle) lineStyle.getSelectedItem(), lineColor.color());
    }
}
