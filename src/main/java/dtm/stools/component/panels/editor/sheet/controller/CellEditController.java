package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.calc.FormulaCell;
import dtm.stools.component.panels.editor.sheet.command.SheetTransaction;
import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.format.ParsedInput;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.formula.ReferenceAdjuster;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.Hyperlink;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.provider.SheetCellEditorProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetValidationRuleProvider;
import dtm.stools.component.panels.editor.sheet.ui.ArgumentHint;
import dtm.stools.component.panels.editor.sheet.ui.FormulaHighlighter;
import dtm.stools.component.panels.editor.sheet.ui.FunctionAutocomplete;
import dtm.stools.component.panels.editor.sheet.ui.SheetCanvas;
import dtm.stools.component.panels.editor.sheet.ui.SheetGeometry;
import dtm.stools.component.panels.editor.sheet.render.SheetPalette;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CellEditController {
    private static final Pattern CELL_PART = Pattern.compile("(\\$?)([A-Za-z]{1,3})(\\$?)(\\d+)");
    private static final Pattern URL = Pattern.compile("(?i)(https?://|www\\.)\\S+|mailto:\\S+");
    private static final long MAX_FILL_CELLS = 2_000_000;

    private final SheetEditor editor;
    private final DefaultStyledDocument document = new DefaultStyledDocument();
    private final JTextPane inCell = new JTextPane(document);
    private final Timer highlightTimer;
    private FunctionAutocomplete autocomplete;
    private ArgumentHint hint;
    private JComponent custom;
    private boolean active, editMode, updating, fromBar;
    private int sheet;
    private CellAddress cell;
    private String original = "";
    private int[] pointSpan;
    private CellRange pointRange;
    private CellAddress pointAnchor;
    private int pointSheet = -1;

    public CellEditController(SheetEditor editor) {
        this.editor = editor;
        highlightTimer = new Timer(60, e -> highlight());
        highlightTimer.setRepeats(false);
    }

    public void install() {
        SheetCanvas canvas = editor.getCanvas();
        inCell.setName("sheet.inCellEditor");
        inCell.setVisible(false);
        inCell.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0x217346), 2), BorderFactory.createEmptyBorder(0, 2, 0, 2)));
        inCell.setFocusTraversalKeysEnabled(false);
        canvas.setLayout(null);
        canvas.add(inCell);
        editor.getFormulaBar().setDocument(document);
        editor.getFormulaBar().field().setFocusTraversalKeysEnabled(false);
        autocomplete = new FunctionAutocomplete(canvas);
        hint = new ArgumentHint(canvas);
        inCell.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { handleKey(e, inCell); }
        });
        document.addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { textChanged(e.getOffset() + e.getLength() == document.getLength() && e.getLength() == 1); }
            @Override public void removeUpdate(DocumentEvent e) { textChanged(false); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });
        inCell.addCaretListener(e -> { if (active && !updating) SwingUtilities.invokeLater(this::updateHints); });
        editor.getFormulaBar().field().addCaretListener(e -> { if (active && !updating) SwingUtilities.invokeLater(this::updateHints); });
    }

    public boolean isActive() { return active || custom != null; }
    public int editingSheet() { return sheet; }
    public CellAddress editingCell() { return cell; }
    public String text() { try { return document.getText(0, document.getLength()); } catch (BadLocationException e) { return ""; } }
    private JTextPane current() { return fromBar ? editor.getFormulaBar().field() : inCell; }

    public String modeText() {
        if (!isActive()) return "Pronto";
        if (isFormulaPointMode()) return "Apontar";
        return editMode ? "Editar" : "Digite";
    }

    private void setText(String text) {
        updating = true;
        try {
            document.remove(0, document.getLength());
            document.insertString(0, text, null);
            document.setCharacterAttributes(0, document.getLength(), plain(), true);
        } catch (BadLocationException ignored) {
        } finally {
            updating = false;
        }
    }

    private SimpleAttributeSet plain() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, inCell.getForeground() == null ? Color.BLACK : inCell.getForeground());
        return a;
    }

    public void showActiveCell() {
        if (isActive()) return;
        SheetSelection sel = editor.getSelection();
        setText(editText(editor.activeSheetIndex(), sel.active()));
        editor.getFormulaBar().field().setCaretPosition(0);
        editor.getFormulaBar().setEditingState(false);
    }

    public String editText(int sheetIndex, CellAddress a) {
        SheetWorksheet ws = editor.getWorkbook().sheet(sheetIndex);
        SheetCell c = ws.cell(a);
        FormulaLocale locale = editor.formulaLocale();
        if (c.hasFormula()) return "=" + Formulas.toDisplay(c.formula(), locale, a, editor.getConfig().r1c1());
        if (c.value().isEmpty()) {
            Optional<FormulaCell> anchor = editor.getEngine().spillAnchor(sheetIndex, a.row(), a.column());
            if (anchor.isPresent() && !anchor.get().address().equals(a)) {
                SheetCell owner = ws.cell(anchor.get().address());
                if (owner.hasFormula()) return "=" + Formulas.toDisplay(owner.formula(), locale, anchor.get().address(), editor.getConfig().r1c1());
            }
            return "";
        }
        CellStyle style = editor.getWorkbook().style(c.style());
        return valueText(c.value(), style.numberFormat());
    }

    public String valueText(CellValue v, String numberFormat) {
        FormulaLocale locale = editor.formulaLocale();
        return switch (v) {
            case NumberValue n -> numberText(n.value(), numberFormat);
            case TextValue t -> {
                if (editor.formatter().isTextFormat(numberFormat)) yield t.value();
                ParsedInput p = editor.valueParser().parse(t.value());
                yield p.value() instanceof TextValue && !p.isFormula() ? t.value() : "'" + t.value();
            }
            case BoolValue b -> b.value() ? locale.trueText() : locale.falseText();
            case ErrorValue e -> locale.localizedErrors() ? e.error().localized() : e.error().text();
            default -> v.display();
        };
    }

    private String numberText(double value, String format) {
        Locale locale = editor.getConfig().locale();
        boolean d1904 = editor.getWorkbook().properties().date1904();
        if (format != null && editor.formatter().isDateFormat(format) && DateSerial.valid(value)) {
            double whole = Math.floor(value), fraction = value - whole;
            LocalDateTime dt = DateSerial.toDateTime(value, d1904);
            String time = String.format("%02d:%02d:%02d", dt.getHour(), dt.getMinute(), dt.getSecond());
            if (whole == 0 && !d1904) return time;
            String date = dt.toLocalDate().format(dateFormatter(locale));
            return fraction > 1e-9 ? date + " " + time : date;
        }
        String general = NumberValue.general(value);
        if (format != null && stripQuoted(format).contains("%")) general = NumberValue.general(value * 100) + "%";
        char dec = editor.formatter().decimalSeparator();
        return dec == '.' ? general : general.replace('.', dec);
    }

    private static String stripQuoted(String format) { return format.replaceAll("\"[^\"]*\"", "").replaceAll("\\\\.", ""); }

    static DateTimeFormatter dateFormatter(Locale locale) {
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale);
        if (!pattern.contains("yyyy")) pattern = pattern.replace("yy", "yyyy");
        if (pattern.contains("d") && !pattern.contains("dd") && locale.getLanguage().equals("pt")) pattern = pattern.replace("d", "dd");
        if (locale.getLanguage().equals("pt")) pattern = "dd/MM/yyyy";
        return DateTimeFormatter.ofPattern(pattern, locale);
    }

    public void start(String initial, boolean replace) {
        if (isActive()) return;
        if (editor.isReadOnlyView()) { Toolkit.getDefaultToolkit().beep(); return; }
        int s = editor.activeSheetIndex();
        CellAddress a = editor.getCanvas().mergeAt(editor.getSelection().active()).first();
        if (!editor.review().canEdit(s, a)) { editor.review().warnProtected(); return; }
        String current = editText(s, a);
        for (SheetCellEditorProvider p : editor.providers(SheetCellEditorProvider.class)) {
            Optional<JComponent> c;
            try {
                c = p.editor(editor, s, a, replace && initial != null ? initial : current, text -> commitCustom(s, a, text), this::cancelCustom);
            } catch (RuntimeException failure) {
                editor.reportError(failure);
                continue;
            }
            if (c.isPresent()) { showCustom(s, a, c.get()); return; }
        }
        active = true;
        fromBar = false;
        editMode = initial == null;
        sheet = s;
        cell = a;
        original = current;
        pointSpan = null;
        setText(replace ? (initial == null ? "" : initial) : current);
        inCell.setCaretPosition(document.getLength());
        reposition();
        inCell.setVisible(true);
        inCell.requestFocusInWindow();
        editor.getFormulaBar().setEditingState(true);
        highlight();
        if (replace && initial != null) textChanged(true);
        editor.refreshStatus();
    }

    public void startFromFormulaBar(int caret) {
        if (isActive() || editor.isReadOnlyView()) return;
        int s = editor.activeSheetIndex();
        CellAddress a = editor.getCanvas().mergeAt(editor.getSelection().active()).first();
        if (!editor.review().canEdit(s, a)) { editor.review().warnProtected(); editor.focusGrid(); return; }
        active = true;
        fromBar = true;
        editMode = true;
        sheet = s;
        cell = a;
        original = text();
        pointSpan = null;
        reposition();
        inCell.setVisible(true);
        JTextPane f = editor.getFormulaBar().field();
        f.setCaretPosition(Math.max(0, Math.min(caret, document.getLength())));
        if (!f.isFocusOwner()) f.requestFocusInWindow();
        editor.getFormulaBar().setEditingState(true);
        highlight();
        editor.refreshStatus();
    }

    public void reposition() {
        if (custom != null) { positionCustom(); return; }
        if (!active) return;
        if (sheet != editor.activeSheetIndex()) { inCell.setVisible(false); return; }
        SheetCanvas canvas = editor.getCanvas();
        SheetGeometry g = canvas.geometry();
        Rectangle r = g.rangeRect(canvas.mergeAt(cell));
        CellStyle style = editor.getWorkbook().style(editor.getWorkbook().sheet(sheet).cell(cell).style());
        Font font = editor.renderer().font(style, g.zoom());
        if (!font.equals(inCell.getFont())) inCell.setFont(font);
        Integer fill = style.fill().primaryColor();
        inCell.setBackground(fill == null ? (canvas.isDark() ? new Color(0x1E1E1E) : Color.WHITE) : SheetPalette.color(fill));
        inCell.setForeground(style.fontColor() == null ? (canvas.isDark() ? Color.WHITE : Color.BLACK) : SheetPalette.color(style.fontColor()));
        FontMetrics fm = inCell.getFontMetrics(font);
        int textWidth = 0;
        for (String line : text().split("\n", -1)) textWidth = Math.max(textWidth, fm.stringWidth(line));
        int width = Math.max(r.width + 2, Math.min(canvas.getWidth() - r.x - 2, textWidth + 16));
        int lines = Math.max(1, text().split("\n", -1).length);
        int height = Math.max(r.height + 2, Math.min(canvas.getHeight() - r.y - 2, lines * fm.getHeight() + 6));
        inCell.setBounds(r.x - 1, r.y - 1, Math.max(20, width), Math.max(fm.getHeight() + 4, height));
        inCell.setVisible(r.x >= g.headerWidth() - 2 && r.y >= g.headerHeight() - 2 && r.x < canvas.getWidth() && r.y < canvas.getHeight());
    }

    private void textChanged(boolean typedAtEnd) {
        if (updating || !active) return;
        pointSpan = null;
        SwingUtilities.invokeLater(() -> {
            if (!active) return;
            reposition();
            highlightTimer.restart();
            updateHints();
            if (typedAtEnd && editor.getConfig().autoComplete() && !fromBar) completeFromColumn();
            editor.refreshStatus();
        });
    }

    private void updateHints() {
        if (!active) return;
        String t = text();
        if (!t.startsWith("=")) { autocomplete.hide(); hint.hide(); return; }
        JTextPane c = current();
        if (!c.isShowing()) { autocomplete.hide(); hint.hide(); return; }
        autocomplete.update(c, editor.getEngine().functions(), editor.formulaLocale(), editor.getWorkbook().properties().names());
        hint.update(c, editor.getEngine().functions(), editor.formulaLocale());
    }

    private void completeFromColumn() {
        String t = text();
        if (t.isEmpty() || t.startsWith("=") || Character.isDigit(t.charAt(0)) || current().getCaretPosition() != t.length()) return;
        SheetWorksheet ws = editor.getWorkbook().sheet(sheet);
        String lower = t.toLowerCase(Locale.ROOT);
        String match = null;
        int col = cell.column();
        for (int dir : new int[]{-1, 1}) {
            int r = cell.row() + dir, blanks = 0;
            while (r >= 0 && r < ws.rows().count() && Math.abs(r - cell.row()) < 2000 && blanks == 0) {
                CellValue v = ws.cell(r, col).value();
                if (v.isEmpty()) { blanks++; break; }
                if (v instanceof TextValue tv && tv.value().length() > t.length() && tv.value().toLowerCase(Locale.ROOT).startsWith(lower)) {
                    if (match != null && !match.equalsIgnoreCase(tv.value())) return;
                    match = tv.value();
                }
                r += dir;
            }
        }
        if (match == null) return;
        String rest = match.substring(t.length());
        updating = true;
        try {
            document.insertString(document.getLength(), rest, null);
        } catch (BadLocationException ignored) {
        } finally {
            updating = false;
        }
        inCell.select(t.length(), document.getLength());
    }

    private void highlight() {
        if (!active) { editor.getCanvas().setHighlights(List.of()); return; }
        String t = text();
        updating = true;
        try {
            document.setCharacterAttributes(0, document.getLength(), plain(), true);
            List<SheetCanvas.Highlight> highlights = new ArrayList<>();
            if (t.startsWith("=")) {
                for (FormulaHighlighter.Span s : FormulaHighlighter.analyze(t, editor.formulaLocale())) {
                    SimpleAttributeSet a = new SimpleAttributeSet();
                    StyleConstants.setForeground(a, s.color());
                    document.setCharacterAttributes(s.start(), s.end() - s.start(), a, false);
                    if (s.range() != null) {
                        int idx = s.sheet() == null ? sheet : editor.getWorkbook().indexOf(s.sheet());
                        if (idx >= 0) highlights.add(new SheetCanvas.Highlight(idx, s.range(), s.color()));
                    }
                }
            }
            editor.getCanvas().setHighlights(highlights);
        } finally {
            updating = false;
        }
    }

    public boolean isFormulaPointMode() {
        if (!active) return false;
        String t = text();
        if (!t.startsWith("=")) return false;
        int caret = current().getCaretPosition();
        if (pointSpan != null && caret == pointSpan[1]) return true;
        return FormulaHighlighter.acceptsReference(t, caret);
    }

    public void insertReference(CellRange range, boolean extend) {
        if (!active) return;
        CellRange r = extend && pointRange != null && pointSheet == editor.activeSheetIndex() ? pointRange.union(range) : range;
        String ref = referenceText(r);
        JTextPane c = current();
        updating = true;
        try {
            int start;
            if (pointSpan != null) {
                start = pointSpan[0];
                document.remove(start, pointSpan[1] - pointSpan[0]);
            } else {
                start = c.getCaretPosition();
                int selStart = c.getSelectionStart(), selEnd = c.getSelectionEnd();
                if (selEnd > selStart) { document.remove(selStart, selEnd - selStart); start = selStart; }
            }
            document.insertString(start, ref, null);
            pointSpan = new int[]{start, start + ref.length()};
            c.setCaretPosition(pointSpan[1]);
        } catch (BadLocationException ignored) {
        } finally {
            updating = false;
        }
        pointRange = r;
        pointSheet = editor.activeSheetIndex();
        reposition();
        highlight();
        editor.refreshStatus();
    }

    private String referenceText(CellRange r) {
        int s = editor.activeSheetIndex();
        String a1 = r.toA1();
        if (s == sheet) return a1;
        String name = editor.getWorkbook().sheet(s).name();
        String quoted = name.matches("[\\p{L}_][\\p{L}\\p{N}_.]*") && !name.matches("[A-Za-z]{1,3}\\d+") ? name : "'" + name.replace("'", "''") + "'";
        return quoted + "!" + a1;
    }

    public void pointFinished() {
        if (!active) return;
        JTextPane c = current();
        c.requestFocusInWindow();
        if (pointSpan != null) c.setCaretPosition(Math.min(pointSpan[1], document.getLength()));
    }

    private void keyboardPoint(int dr, int dc, boolean shift) {
        CellAddress base = pointRange != null && pointSheet == editor.activeSheetIndex() ? (pointAnchor != null && shift ? pointRange.last() : pointRange.first()) : cell;
        if (pointSheet != editor.activeSheetIndex() || pointRange == null) base = cell;
        int row = Math.max(0, Math.min(editor.activeSheet().rows().count() - 1, base.row() + dr));
        int col = Math.max(0, Math.min(editor.activeSheet().columns().count() - 1, base.column() + dc));
        CellAddress target = new CellAddress(row, col);
        CellRange r;
        if (shift) {
            if (pointAnchor == null) pointAnchor = pointRange != null ? pointRange.first() : cell;
            r = CellRange.of(pointAnchor, target);
        } else {
            pointAnchor = null;
            r = editor.getCanvas().mergeAt(target);
        }
        insertReference(r, false);
        editor.getCanvas().scrollToCell(target.row(), target.column());
    }

    public void handleKey(KeyEvent e, JTextPane source) {
        int code = e.getKeyCode();
        boolean ctrl = e.isControlDown(), shift = e.isShiftDown(), alt = e.isAltDown();
        if (!active) {
            if (code == KeyEvent.VK_ESCAPE) { e.consume(); editor.focusGrid(); }
            return;
        }
        if (autocomplete.isVisible()) {
            switch (code) {
                case KeyEvent.VK_UP -> { autocomplete.move(-1); e.consume(); return; }
                case KeyEvent.VK_DOWN -> { autocomplete.move(1); e.consume(); return; }
                case KeyEvent.VK_TAB -> { autocomplete.accept(); e.consume(); return; }
                case KeyEvent.VK_ESCAPE -> { autocomplete.hide(); e.consume(); return; }
                default -> { }
            }
        }
        switch (code) {
            case KeyEvent.VK_ESCAPE -> { e.consume(); cancel(); }
            case KeyEvent.VK_ENTER -> {
                e.consume();
                if (alt) { insertText(source, "\n"); return; }
                if (ctrl && !shift) { commitFill(); return; }
                int dr = editor.getConfig().moveAfterEnter() ? (editor.getConfig().enterMovesDown() ? 1 : 0) : 0;
                int dc = editor.getConfig().moveAfterEnter() && !editor.getConfig().enterMovesDown() ? 1 : 0;
                if (shift) { dr = -dr; dc = -dc; }
                commit(dr, dc);
            }
            case KeyEvent.VK_TAB -> { e.consume(); commit(0, shift ? -1 : 1); }
            case KeyEvent.VK_F2 -> { e.consume(); editMode = !editMode; editor.refreshStatus(); }
            case KeyEvent.VK_F4 -> { e.consume(); toggleAbsolute(source); }
            case KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT -> {
                if (editMode || source != inCell || alt) return;
                int dr = code == KeyEvent.VK_UP ? -1 : code == KeyEvent.VK_DOWN ? 1 : 0;
                int dc = code == KeyEvent.VK_LEFT ? -1 : code == KeyEvent.VK_RIGHT ? 1 : 0;
                if (isFormulaPointMode()) { e.consume(); keyboardPoint(dr, dc, shift); return; }
                if (ctrl) return;
                e.consume();
                commit(dr, dc);
            }
            case KeyEvent.VK_A -> {
                if (ctrl && shift && text().startsWith("=")) { e.consume(); insertArgumentNames(source); }
            }
            case KeyEvent.VK_SEMICOLON -> {
                if (ctrl) {
                    e.consume();
                    Locale locale = editor.getConfig().locale();
                    insertText(source, shift ? java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) : LocalDate.now().format(dateFormatter(locale)));
                }
            }
            default -> { }
        }
    }

    private void insertArgumentNames(JTextPane source) {
        FormulaHighlighter.CallContext call = FormulaHighlighter.callAt(text(), source.getCaretPosition(), editor.formulaLocale());
        if (call == null) return;
        String canonical = editor.formulaLocale().canonicalFunction(call.function());
        editor.getEngine().functions().find(canonical).ifPresent(f -> {
            List<String> params = dtm.stools.component.panels.editor.sheet.ui.FunctionSignatures.parameters(f);
            if (params.isEmpty()) return;
            insertText(source, String.join(String.valueOf(editor.formulaLocale().argumentSeparator()) + " ", params) + ")");
        });
    }

    private void insertText(JTextPane source, String value) {
        try {
            int start = source.getSelectionStart(), end = source.getSelectionEnd();
            if (end > start) document.remove(start, end - start);
            document.insertString(start, value, null);
            source.setCaretPosition(start + value.length());
        } catch (BadLocationException ignored) { }
    }

    private void toggleAbsolute(JTextPane source) {
        String t = text();
        int caret = source.getCaretPosition();
        int[] span = FormulaHighlighter.tokenAt(t, caret, editor.formulaLocale());
        if (span == null) {
            if (!t.startsWith("=") && t.isEmpty()) return;
            return;
        }
        String ref = t.substring(span[0], span[1]);
        int bang = ref.lastIndexOf('!');
        String prefix = bang >= 0 ? ref.substring(0, bang + 1) : "";
        String body = bang >= 0 ? ref.substring(bang + 1) : ref;
        String[] parts = body.split(":", -1);
        int state = absoluteState(parts[0]);
        int next = switch (state) { case 0 -> 3; case 3 -> 2; case 2 -> 1; default -> 0; };
        StringBuilder b = new StringBuilder(prefix);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) b.append(':');
            b.append(applyAbsolute(parts[i], next));
        }
        try {
            document.remove(span[0], span[1] - span[0]);
            document.insertString(span[0], b.toString(), null);
            source.select(span[0], span[0] + b.length());
        } catch (BadLocationException ignored) { }
    }

    private static int absoluteState(String part) {
        Matcher m = CELL_PART.matcher(part);
        if (m.matches()) return (m.group(1).isEmpty() ? 0 : 1) | (m.group(3).isEmpty() ? 0 : 2);
        return part.startsWith("$") ? 3 : 0;
    }

    private static String applyAbsolute(String part, int state) {
        Matcher m = CELL_PART.matcher(part);
        if (m.matches()) return ((state & 1) != 0 ? "$" : "") + m.group(2) + ((state & 2) != 0 ? "$" : "") + m.group(4);
        String bare = part.replace("$", "");
        return state == 0 ? bare : "$" + bare;
    }

    public boolean commitIfActive() {
        if (custom != null) { cancelCustom(); return true; }
        if (!active) return true;
        return commit(0, 0);
    }

    public boolean commit(int dr, int dc) {
        if (custom != null) return true;
        if (!active) return true;
        String t = text();
        boolean ok = t.equals(original) || write(sheet, List.of(CellRange.of(cell)), cell, t, true);
        if (!ok) return false;
        finish();
        if (dr != 0 || dc != 0) editor.navigation().moveWithinSelection(dr, dc);
        return true;
    }

    private void commitFill() {
        String t = text();
        SheetSelection sel = editor.getSelection();
        List<CellRange> targets = sheet == editor.activeSheetIndex() ? sel.ranges() : List.of(CellRange.of(cell));
        if (!write(sheet, targets, cell, t, true)) return;
        finish();
    }

    private void finish() {
        active = false;
        fromBar = false;
        pointSpan = null;
        pointRange = null;
        pointAnchor = null;
        autocomplete.hide();
        hint.hide();
        inCell.setVisible(false);
        editor.getCanvas().setHighlights(List.of());
        if (sheet != editor.activeSheetIndex() && sheet < editor.getWorkbook().sheetCount()) editor.getSession().setActiveSheet(sheet, SheetSelection.of(cell));
        showActiveCell();
        editor.focusGrid();
        editor.refreshStatus();
        editor.commandRegistry().refresh();
    }

    public void cancel() {
        if (custom != null) { cancelCustom(); return; }
        if (!active) return;
        active = false;
        setText(original);
        finish();
    }

    public boolean write(int sheetIndex, List<CellRange> ranges, CellAddress host, String text, boolean interactive) {
        SheetWorksheet ws = editor.getWorkbook().sheet(sheetIndex);
        for (CellRange r : ranges) if (!editor.review().canEdit(sheetIndex, r)) { if (interactive) editor.review().warnProtected(); return false; }
        long cells = 0;
        for (CellRange r : ranges) cells += bounded(ws, r).cellCount();
        if (cells > MAX_FILL_CELLS) { if (interactive) editor.popups().warn("Planilha", "A seleção é grande demais para esta operação."); return false; }
        if (Formulas.isFormula(text) || text.startsWith("=") && text.length() > 1) {
            String canonical;
            try {
                canonical = canonicalize(text, host);
            } catch (RuntimeException failure) {
                if (!interactive) throw failure;
                editor.popups().warn("Fórmula", "Há um problema com esta fórmula.\n\n" + failure.getMessage());
                return false;
            }
            return editor.edit("Digitar " + host.toA1(), tx -> {
                for (CellRange r : ranges) for (CellAddress a : bounded(ws, r)) {
                    String f = a.equals(host) ? canonical : ReferenceAdjuster.shift(canonical, a.row() - host.row(), a.column() - host.column());
                    tx.updateCell(sheetIndex, a.row(), a.column(), c -> c.withFormula(f, CellValue.EMPTY));
                }
                editor.data().autoExpandTable(tx, sheetIndex, host);
            });
        }
        CellStyle style = editor.getWorkbook().style(ws.cell(host).style());
        ParsedInput parsed;
        if (text.isEmpty()) parsed = ParsedInput.of(CellValue.EMPTY);
        else if (editor.formatter().isTextFormat(style.numberFormat())) parsed = ParsedInput.of(CellValue.of(text));
        else parsed = editor.valueParser().parse(text);
        CellValue value = parsed.value();
        if (interactive && !validate(sheetIndex, host, value)) return false;
        String fmt = parsed.format();
        boolean link = interactive && value instanceof TextValue tv && URL.matcher(tv.value()).matches();
        return editor.edit(text.isEmpty() ? "Limpar" : "Digitar " + host.toA1(), tx -> {
            for (CellRange r : ranges) for (CellAddress a : bounded(ws, r)) {
                tx.updateCell(sheetIndex, a.row(), a.column(), c -> c.withValue(value));
                if (fmt != null) tx.setStyle(sheetIndex, a.row(), a.column(), s -> s.numberFormat() == null || s.numberFormat().equals("General") ? s.withNumberFormat(fmt) : s);
                if (link) {
                    String target = ((TextValue) value).value();
                    String href = target.toLowerCase(Locale.ROOT).startsWith("www.") ? "https://" + target : target;
                    tx.updateProperties(sheetIndex, p -> p.withLinks(SheetProperties.put(p.links(), a, new Hyperlink(href, null))));
                }
            }
            if (!text.isEmpty()) editor.data().autoExpandTable(tx, sheetIndex, host);
        });
    }

    public void writeInto(SheetTransaction tx, int sheetIndex, CellAddress a, String text) {
        if (text.startsWith("=") && text.length() > 1) {
            String canonical = canonicalize(text, a);
            tx.updateCell(sheetIndex, a.row(), a.column(), c -> c.withFormula(canonical, CellValue.EMPTY));
            return;
        }
        CellStyle style = editor.getWorkbook().style(tx.cell(sheetIndex, a.row(), a.column()).style());
        CellValue v = text.isEmpty() ? CellValue.EMPTY : editor.formatter().isTextFormat(style.numberFormat()) ? CellValue.of(text) : editor.valueParser().parse(text).value();
        tx.updateCell(sheetIndex, a.row(), a.column(), c -> c.withValue(v));
    }

    public String canonicalize(String text, CellAddress host) {
        try {
            return Formulas.toCanonical(text, editor.formulaLocale(), host, editor.getConfig().r1c1());
        } catch (RuntimeException failure) {
            long open = text.chars().filter(ch -> ch == '(').count(), close = text.chars().filter(ch -> ch == ')').count();
            if (open > close) {
                try { return Formulas.toCanonical(text + ")".repeat((int) (open - close)), editor.formulaLocale(), host, editor.getConfig().r1c1()); } catch (RuntimeException ignored) { }
            }
            throw failure;
        }
    }

    private CellRange bounded(SheetWorksheet ws, CellRange r) {
        if (!r.isWholeColumn() && !r.isWholeRow()) return r;
        CellRange used = ws.usedRange();
        if (used == null) return CellRange.of(r.first());
        CellRange i = r.intersection(used);
        return i == null ? CellRange.of(r.first()) : i;
    }

    private boolean validate(int sheetIndex, CellAddress a, CellValue value) {
        Optional<DataValidation> v = editor.validationEvaluator().find(sheetIndex, a.row(), a.column());
        if (v.isPresent() && !editor.validationEvaluator().isValid(sheetIndex, v.get(), a.row(), a.column(), value)) {
            DataValidation d = v.get();
            if (d.showError()) {
                String title = d.errorTitle().isBlank() ? "Planilha" : d.errorTitle();
                String msg = d.errorMessage().isBlank() ? "Este valor não corresponde às restrições de validação de dados definidas para esta célula." : d.errorMessage();
                switch (d.errorStyle()) {
                    case STOP -> {
                        int choice = editor.popups().choose(title, msg, true, 0, "Tentar Novamente", "Cancelar");
                        if (choice == 1) cancel();
                        return false;
                    }
                    case WARNING -> { if (editor.popups().choose(title, msg + "\n\nDeseja continuar?", true, 1, "Sim", "Não") != 0) return false; }
                    default -> editor.popups().info(title, msg);
                }
            }
        }
        for (SheetValidationRuleProvider p : editor.providers(SheetValidationRuleProvider.class)) {
            Optional<String> error = p.validate(editor, sheetIndex, a, value);
            if (error.isPresent()) { editor.popups().warn("Validação", error.get()); return false; }
        }
        return true;
    }

    private void showCustom(int s, CellAddress a, JComponent component) {
        custom = component;
        sheet = s;
        cell = a;
        editor.getCanvas().add(component);
        positionCustom();
        component.setVisible(true);
        component.requestFocusInWindow();
        editor.refreshStatus();
    }

    private void positionCustom() {
        if (custom == null) return;
        SheetGeometry g = editor.getCanvas().geometry();
        Rectangle r = g.rangeRect(editor.getCanvas().mergeAt(cell));
        java.awt.Dimension pref = custom.getPreferredSize();
        custom.setBounds(r.x, r.y, Math.max(r.width, pref.width), Math.max(r.height, pref.height));
        custom.revalidate();
    }

    private void commitCustom(int s, CellAddress a, String text) {
        JComponent c = custom;
        custom = null;
        if (c != null) { editor.getCanvas().remove(c); editor.getCanvas().repaint(); }
        write(s, List.of(CellRange.of(a)), a, text, true);
        editor.focusGrid();
    }

    private void cancelCustom() {
        JComponent c = custom;
        custom = null;
        if (c != null) { editor.getCanvas().remove(c); editor.getCanvas().repaint(); }
        editor.focusGrid();
        editor.refreshStatus();
    }

    public void insertAtCaret(String value) {
        if (!active) start(null, false);
        if (!active) return;
        insertText(current(), value);
        current().requestFocusInWindow();
    }

    public void dispose() {
        highlightTimer.stop();
        if (autocomplete != null) autocomplete.dispose();
        if (hint != null) hint.dispose();
    }
}
