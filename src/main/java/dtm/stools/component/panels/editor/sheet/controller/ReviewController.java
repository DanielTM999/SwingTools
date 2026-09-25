package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.calc.Dependency;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.formula.RefNode;
import dtm.stools.component.panels.editor.sheet.formula.ReferenceAdjuster;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.DefinedName;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.Hyperlink;
import dtm.stools.component.panels.editor.sheet.model.ProtectedRange;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetComment;
import dtm.stools.component.panels.editor.sheet.model.SheetNote;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetProtection;
import dtm.stools.component.panels.editor.sheet.model.SheetThread;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.provider.SheetAiProvider;
import dtm.stools.component.panels.editor.sheet.ui.SheetCanvas;
import dtm.stools.component.panels.editor.sheet.ui.popup.AiAssistantPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.NameManagerPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.SheetForm;
import dtm.stools.component.panels.editor.sheet.ui.popup.WatchWindow;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ReviewController {
    private final SheetEditor editor;
    private final WatchWindow watch;
    private boolean errorIndicators = true;

    public ReviewController(SheetEditor editor) {
        this.editor = editor;
        this.watch = new WatchWindow(editor);
    }

    private int sheet() { return editor.activeSheetIndex(); }
    private SheetWorksheet ws() { return editor.activeSheet(); }

    public boolean errorIndicators() { return errorIndicators; }
    public void toggleErrorIndicators() { errorIndicators = !errorIndicators; editor.getCanvas().repaint(); }

    public boolean canEdit(int sheetIndex, CellAddress a) { return canEdit(sheetIndex, CellRange.of(a)); }

    public boolean canEdit(int sheetIndex, CellRange range) {
        SheetWorksheet w = editor.getWorkbook().sheet(sheetIndex);
        SheetProtection p = w.properties().protection();
        if (!p.enabled()) return true;
        CellRange r = range.isWholeColumn() || range.isWholeRow() ? editor.clipboard().bounded(w, range) : range;
        if (r.cellCount() > 200_000) return false;
        String user = editor.getSession().getAuthor();
        for (CellAddress a : r) {
            CellStyle style = editor.getWorkbook().style(w.cell(a).style());
            if (!style.locked()) continue;
            boolean allowed = false;
            for (ProtectedRange pr : w.properties().protectedRanges()) if (pr.covers(a.row(), a.column()) && pr.canEdit(user) && pr.passwordHash() == null) { allowed = true; break; }
            if (!allowed) return false;
        }
        return true;
    }

    public boolean canFormat(int sheetIndex, CellRange range) {
        SheetProtection p = editor.getWorkbook().sheet(sheetIndex).properties().protection();
        return !p.enabled() || p.formatCells();
    }

    public void warnProtected() {
        editor.popups().warn("Planilha protegida", "A célula ou o gráfico que você está tentando alterar está em uma planilha protegida.\nPara fazer uma alteração, desproteja a planilha.");
    }

    public void protectSheet() {
        SheetProtection current = ws().properties().protection();
        int s = sheet();
        if (current.enabled()) {
            if (current.passwordHash() == null) { editor.edit("Desproteger planilha", tx -> tx.updateProperties(s, p -> p.withProtection(SheetProtection.NONE))); return; }
            JPasswordField pwd = new JPasswordField(18);
            SheetForm f = new SheetForm();
            f.add("Senha:", pwd);
            editor.popups().dialog("sheet.unprotect", "Desproteger Planilha", f, () -> new String(pwd.getPassword())).ifPresent(pw -> {
                if (!current.verify(pw)) { editor.popups().warn("Desproteger", "A senha fornecida não está correta."); return; }
                editor.getSession().applyRemote("Desproteger planilha", tx -> tx.updateProperties(s, p -> p.withProtection(SheetProtection.NONE)));
            });
            return;
        }
        JPasswordField pwd = new JPasswordField(18), confirm = new JPasswordField(18);
        SheetForm f = new SheetForm();
        f.add("Senha (opcional):", pwd);
        f.add("Confirmar senha:", confirm);
        f.section("Permitir que todos os usuários desta planilha possam:");
        JCheckBox selLocked = SheetForm.check("Selecionar células bloqueadas", true), selUnlocked = SheetForm.check("Selecionar células desbloqueadas", true),
                fmtCells = SheetForm.check("Formatar células", false), fmtCols = SheetForm.check("Formatar colunas", false), fmtRows = SheetForm.check("Formatar linhas", false),
                insCols = SheetForm.check("Inserir colunas", false), insRows = SheetForm.check("Inserir linhas", false), links = SheetForm.check("Inserir hiperlinks", false),
                delCols = SheetForm.check("Excluir colunas", false), delRows = SheetForm.check("Excluir linhas", false), sort = SheetForm.check("Classificar", false),
                filter = SheetForm.check("Usar AutoFiltro", false), pivots = SheetForm.check("Usar tabela dinâmica", false), objects = SheetForm.check("Editar objetos", false),
                scenarios = SheetForm.check("Editar cenários", false);
        for (JCheckBox b : new JCheckBox[]{selLocked, selUnlocked, fmtCells, fmtCols, fmtRows, insCols, insRows, links, delCols, delRows, sort, filter, pivots, objects, scenarios}) f.full(b);
        editor.popups().dialog("sheet.protect", "Proteger Planilha", f, () -> {
            String a = new String(pwd.getPassword()), b = new String(confirm.getPassword());
            if (!a.equals(b)) throw new IllegalArgumentException("As senhas não coincidem.");
            return new SheetProtection(true, a.isEmpty() ? null : SheetProtection.hash(a), selLocked.isSelected(), selUnlocked.isSelected(), fmtCells.isSelected(), fmtCols.isSelected(),
                    fmtRows.isSelected(), insCols.isSelected(), insRows.isSelected(), links.isSelected(), delCols.isSelected(), delRows.isSelected(), sort.isSelected(), filter.isSelected(),
                    pivots.isSelected(), objects.isSelected(), scenarios.isSelected());
        }).ifPresent(prot -> editor.edit("Proteger planilha", tx -> tx.updateProperties(s, p -> p.withProtection(prot))));
    }

    public boolean isSheetProtected() { return ws().properties().protection().enabled(); }
    public boolean isWorkbookProtected() { return editor.getWorkbook().properties().protectStructure(); }

    public void protectWorkbook() {
        boolean on = isWorkbookProtected();
        String hash = editor.getWorkbook().properties().protectionHash();
        if (on && hash != null) {
            JPasswordField pwd = new JPasswordField(18);
            SheetForm f = new SheetForm();
            f.add("Senha:", pwd);
            Optional<String> pw = editor.popups().dialog("sheet.unprotectWorkbook", "Desproteger Pasta de Trabalho", f, () -> new String(pwd.getPassword()));
            if (pw.isEmpty()) return;
            if (!SheetProtection.hash(pw.get()).equalsIgnoreCase(hash)) { editor.popups().warn("Desproteger", "A senha fornecida não está correta."); return; }
            editor.getSession().applyRemote("Desproteger pasta de trabalho", tx -> tx.updateWorkbook(p -> p.withProtectStructure(false).withProtectionHash(null)));
            return;
        }
        if (on) { editor.edit("Desproteger pasta de trabalho", tx -> tx.updateWorkbook(p -> p.withProtectStructure(false))); return; }
        JPasswordField pwd = new JPasswordField(18);
        SheetForm f = new SheetForm();
        f.section("Proteger a estrutura da pasta de trabalho");
        f.add("Senha (opcional):", pwd);
        editor.popups().dialog("sheet.protectWorkbook", "Proteger Estrutura", f, () -> new String(pwd.getPassword()))
                .ifPresent(pw -> editor.edit("Proteger pasta de trabalho", tx -> tx.updateWorkbook(p -> p.withProtectStructure(true).withProtectionHash(pw.isEmpty() ? null : SheetProtection.hash(pw)))));
    }

    public void allowEditRanges() {
        JTextField name = SheetForm.text("Intervalo" + (ws().properties().protectedRanges().size() + 1), 16);
        JTextField ref = SheetForm.text("=" + NavigationController.absolute(editor.getSelection().range()), 18);
        JTextField users = SheetForm.text("", 22);
        SheetForm f = new SheetForm();
        f.add("Título:", name);
        f.add("Refere-se às células:", ref);
        f.add("Usuários (separados por vírgula):", users);
        int s = sheet();
        editor.popups().dialog("sheet.allowRanges", "Permitir Edição de Intervalos", f, () -> {
            NavigationController.Target t = editor.navigation().resolve(ref.getText()).orElseThrow(() -> new IllegalArgumentException("Referência inválida."));
            List<String> editors = users.getText().isBlank() ? List.of() : List.of(users.getText().split("\\s*,\\s*"));
            return new ProtectedRange(name.getText().strip(), t.ranges(), editors, null, "");
        }).ifPresent(pr -> editor.edit("Intervalo editável", tx -> tx.updateProperties(s, p -> p.withProtectedRanges(SheetProperties.add(p.protectedRanges(), pr)))));
    }

    public void defineName(String name, CellRange range, Integer scope) {
        String sheetName = ws().name();
        String formula = NavigationController.reference(sheetName, range, true);
        if (editor.getWorkbook().name(name, scope).isPresent()) { editor.popups().warn("Nome", "Esse nome já existe."); return; }
        editor.edit("Definir nome", tx -> tx.updateWorkbook(p -> p.withNames(SheetProperties.add(p.names(), new DefinedName(name, formula, scope, false, null)))));
    }

    public void defineNameDialog() {
        JTextField name = SheetForm.text(suggestName(), 20);
        JTextField ref = SheetForm.text("=" + NavigationController.reference(ws().name(), editor.getSelection().range(), true), 22);
        JCheckBox local = SheetForm.check("Escopo: somente esta planilha", false);
        SheetForm f = new SheetForm();
        f.add("Nome:", name);
        f.full(local);
        f.add("Refere-se a:", ref);
        CellAddress host = editor.getSelection().active();
        int s = sheet();
        editor.popups().dialog("sheet.defineName", "Novo Nome", f, () -> {
            String n = name.getText().strip();
            if (!n.matches("[\\p{L}_\\\\][\\p{L}\\p{N}_.]*") || n.replace("$", "").matches("[A-Za-z]{1,3}\\d+")) throw new IllegalArgumentException("O nome inserido não é válido.");
            if (editor.getWorkbook().name(n, local.isSelected() ? s : null).filter(x -> java.util.Objects.equals(x.sheetScope(), local.isSelected() ? s : null)).isPresent()) throw new IllegalArgumentException("Esse nome já existe.");
            return new DefinedName(n, editor.editing().canonicalize(ref.getText().startsWith("=") ? ref.getText() : "=" + ref.getText(), host), local.isSelected() ? s : null, false, null);
        }).ifPresent(dn -> editor.edit("Definir nome", tx -> tx.updateWorkbook(p -> p.withNames(SheetProperties.add(p.names(), dn)))));
    }

    private String suggestName() {
        CellAddress a = editor.getSelection().active();
        String above = a.row() > 0 ? editor.displayText(new CellAddress(a.row() - 1, a.column())) : "";
        String left = a.column() > 0 ? editor.displayText(new CellAddress(a.row(), a.column() - 1)) : "";
        String base = !above.isBlank() ? above : left;
        String n = base.strip().replaceAll("[^\\p{L}\\p{N}_.]", "_");
        return n.isEmpty() || Character.isDigit(n.charAt(0)) ? "" : n;
    }

    public void nameManager() {
        List<String> sheets = editor.getWorkbook().sheets().stream().map(SheetWorksheet::name).toList();
        CellAddress host = editor.getSelection().active();
        NameManagerPanel panel = new NameManagerPanel(editor.getWorkbook().properties().names(), sheets, "=" + NavigationController.reference(ws().name(), editor.getSelection().range(), true),
                f -> "=" + Formulas.toDisplay(f, editor.formulaLocale(), host, false),
                text -> editor.editing().canonicalize(text.startsWith("=") ? text : "=" + text, host));
        editor.popups().dialog(SheetDialogIds.NAME_MANAGER, "Gerenciador de Nomes", panel, panel::result)
                .ifPresent(names -> editor.edit("Gerenciador de nomes", tx -> tx.updateWorkbook(p -> p.withNames(names))));
    }

    public void createNamesFromSelection() {
        CellRange r = editor.getSelection().range();
        JCheckBox top = SheetForm.check("Linha superior", r.rowCount() > 1), left = SheetForm.check("Coluna esquerda", r.columnCount() > 1 && r.rowCount() == 1);
        SheetForm f = new SheetForm();
        f.section("Criar nomes com base nos valores da:");
        f.full(top);
        f.full(left);
        String sheetName = ws().name();
        int s = sheet();
        editor.popups().dialog("sheet.createNames", "Criar Nomes a Partir da Seleção", f, () -> new boolean[]{top.isSelected(), left.isSelected()}).ifPresent(o -> {
            List<DefinedName> add = new ArrayList<>();
            if (o[0]) for (int c = r.firstColumn(); c <= r.lastColumn(); c++) {
                String n = clean(editor.displayText(s, new CellAddress(r.firstRow(), c)));
                if (n != null && r.rowCount() > 1) add.add(new DefinedName(n, NavigationController.reference(sheetName, new CellRange(r.firstRow() + 1, c, r.lastRow(), c), true), null, false, null));
            }
            if (o[1]) for (int row = r.firstRow() + (o[0] ? 1 : 0); row <= r.lastRow(); row++) {
                String n = clean(editor.displayText(s, new CellAddress(row, r.firstColumn())));
                if (n != null && r.columnCount() > 1) add.add(new DefinedName(n, NavigationController.reference(sheetName, new CellRange(row, r.firstColumn() + 1, row, r.lastColumn()), true), null, false, null));
            }
            if (add.isEmpty()) return;
            editor.edit("Criar nomes", tx -> tx.updateWorkbook(p -> {
                List<DefinedName> all = new ArrayList<>(p.names());
                for (DefinedName n : add) { all.removeIf(x -> x.name().equalsIgnoreCase(n.name()) && x.sheetScope() == null); all.add(n); }
                return p.withNames(all);
            }));
        });
    }

    private static String clean(String s) {
        String n = s.strip().replaceAll("[^\\p{L}\\p{N}_.]", "_");
        if (n.isEmpty()) return null;
        if (!Character.isLetter(n.charAt(0)) && n.charAt(0) != '_') n = "_" + n;
        if (n.replace("$", "").matches("[A-Za-z]{1,3}\\d+")) n = "_" + n;
        return n;
    }

    public void useNameMenu() {
        List<DefinedName> names = editor.getWorkbook().properties().names().stream().filter(n -> !n.hidden()).toList();
        if (names.isEmpty()) { editor.popups().info("Usar em Fórmula", "Não há nomes definidos."); return; }
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        for (DefinedName n : names) {
            javax.swing.JMenuItem item = new javax.swing.JMenuItem(n.name());
            item.addActionListener(e -> {
                if (!editor.isEditing()) editor.startEditing("=", true);
                editor.editing().insertAtCaret(n.name());
            });
            menu.add(item);
        }
        java.awt.Rectangle r = editor.popups().activeCellRect();
        menu.show(editor.getCanvas(), r.x, r.y + r.height);
    }

    public boolean hasHyperlink(CellAddress cell) { return ws().properties().links().containsKey(cell); }
    public boolean followLinksOnClick() { return true; }

    public void openHyperlink(CellAddress cell) {
        Hyperlink link = ws().properties().links().get(cell);
        if (link == null) return;
        openTarget(link.target());
    }

    public void openTarget(String target) {
        if (target.startsWith("#")) {
            if (!editor.navigation().goTo(target.substring(1))) editor.popups().warn("Hiperlink", "Referência inválida: " + target.substring(1));
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) throw new UnsupportedOperationException("Não é possível abrir links neste sistema.");
            URI uri = URI.create(target.contains("://") || target.startsWith("mailto:") ? target : "https://" + target);
            if (uri.getScheme().equalsIgnoreCase("mailto")) Desktop.getDesktop().mail(uri); else Desktop.getDesktop().browse(uri);
        } catch (Exception failure) {
            editor.popups().warn("Hiperlink", "Não foi possível abrir o endereço especificado.\n" + target);
        }
    }

    public void insertLink() {
        CellAddress a = editor.getSelection().active();
        Hyperlink current = ws().properties().links().get(a);
        JTextField text = SheetForm.text(editor.displayText(a), 26);
        JTextField address = SheetForm.text(current == null ? "" : current.target(), 26);
        address.setToolTipText("URL, e-mail (mailto:) ou #Planilha!A1 para um local neste documento");
        JTextField tip = SheetForm.text(current == null || current.tooltip() == null ? "" : current.tooltip(), 26);
        SheetForm f = new SheetForm();
        f.add("Texto para exibição:", text);
        f.add("Endereço:", address);
        f.add("Dica de tela:", tip);
        int s = sheet();
        if (!canEdit(s, a)) { warnProtected(); return; }
        editor.popups().dialog(SheetDialogIds.HYPERLINK, current == null ? "Inserir Hiperlink" : "Editar Hiperlink", f, () -> {
            if (address.getText().isBlank()) throw new IllegalArgumentException("Informe o endereço.");
            return new String[]{text.getText(), address.getText().strip(), tip.getText()};
        }).ifPresent(o -> editor.edit("Hiperlink", tx -> {
            tx.updateProperties(s, p -> p.withLinks(SheetProperties.put(p.links(), a, new Hyperlink(o[1], o[2].isBlank() ? null : o[2]))));
            if (!o[0].isEmpty() && !o[0].equals(editor.displayText(a))) editor.editing().writeInto(tx, s, a, o[0]);
            if (o[0].isEmpty() && !tx.cell(s, a.row(), a.column()).hasContent()) tx.updateCell(s, a.row(), a.column(), c -> c.withValue(CellValue.of(o[1])));
            tx.setStyle(s, a.row(), a.column(), st -> st.withFontColor(0xFF0563C1).withUnderline(dtm.stools.component.panels.editor.sheet.model.UnderlineStyle.SINGLE));
        }));
    }

    public void removeLinks() {
        int s = sheet();
        List<CellRange> ranges = editor.getSelection().ranges();
        editor.edit("Remover hiperlinks", tx -> tx.updateProperties(s, p -> {
            Map<CellAddress, Hyperlink> m = new HashMap<>(p.links());
            m.keySet().removeIf(k -> ranges.stream().anyMatch(r -> r.contains(k)));
            return p.withLinks(m);
        }));
    }

    public void editNote() {
        CellAddress a = editor.getSelection().active();
        SheetNote current = ws().properties().notes().get(a);
        JTextArea area = new JTextArea(current == null ? "" : current.text(), 6, 32);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(area);
        int s = sheet();
        String author = editor.getSession().getAuthor();
        editor.popups().dialog("sheet.note", (current == null ? "Nova Anotação — " : "Editar Anotação — ") + a.toA1(), scroll, area::getText).ifPresent(text -> editor.edit("Anotação", tx ->
                tx.updateProperties(s, p -> p.withNotes(SheetProperties.put(p.notes(), a, text.isBlank() ? null : new SheetNote(current == null ? author : current.author(), text, current != null && current.visible()))))));
    }

    public void deleteNotes() {
        int s = sheet();
        List<CellRange> ranges = editor.getSelection().ranges();
        editor.edit("Excluir anotação", tx -> tx.updateProperties(s, p -> {
            Map<CellAddress, SheetNote> m = new HashMap<>(p.notes());
            m.keySet().removeIf(k -> ranges.stream().anyMatch(r -> r.contains(k)));
            Map<CellAddress, SheetThread> t = new HashMap<>(p.threads());
            t.keySet().removeIf(k -> ranges.stream().anyMatch(r -> r.contains(k)));
            return p.withNotes(m).withThreads(t);
        }));
    }

    public boolean allNotesVisible() {
        var notes = ws().properties().notes();
        return !notes.isEmpty() && notes.values().stream().allMatch(SheetNote::visible);
    }

    public void toggleAllNotes() {
        boolean show = !allNotesVisible();
        int s = sheet();
        editor.edit(show ? "Mostrar anotações" : "Ocultar anotações", tx -> tx.updateProperties(s, p -> {
            Map<CellAddress, SheetNote> m = new HashMap<>();
            p.notes().forEach((k, v) -> m.put(k, new SheetNote(v.author(), v.text(), show)));
            return p.withNotes(m);
        }));
    }

    public void newComment() {
        CellAddress a = editor.getSelection().active();
        SheetThread current = ws().properties().threads().get(a);
        JTextArea area = new JTextArea(4, 32);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JPanel p = new JPanel(new BorderLayout(0, 6));
        if (current != null) {
            DefaultListModel<String> history = new DefaultListModel<>();
            for (SheetComment c : current.comments()) history.addElement(c.author() + ": " + c.text());
            JScrollPane h = new JScrollPane(new JList<>(history));
            h.setPreferredSize(new Dimension(360, 120));
            p.add(h, BorderLayout.NORTH);
        }
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        int s = sheet();
        String author = editor.getSession().getAuthor();
        editor.popups().dialog("sheet.comment", (current == null ? "Novo Comentário — " : "Responder — ") + a.toA1(), p, area::getText).filter(t -> !t.isBlank()).ifPresent(text -> editor.edit("Comentário", tx ->
                tx.updateProperties(s, q -> {
                    SheetComment c = new SheetComment(UUID.randomUUID().toString(), author, text.strip(), Instant.now());
                    SheetThread t = current == null ? new SheetThread(List.of(c), false) : new SheetThread(SheetProperties.add(current.comments(), c), current.resolved());
                    return q.withThreads(SheetProperties.put(q.threads(), a, t));
                })));
    }

    public void resolveComment() {
        CellAddress a = editor.getSelection().active();
        SheetThread t = ws().properties().threads().get(a);
        if (t == null) return;
        int s = sheet();
        editor.edit(t.resolved() ? "Reabrir comentário" : "Resolver comentário", tx -> tx.updateProperties(s, p -> p.withThreads(SheetProperties.put(p.threads(), a, new SheetThread(t.comments(), !t.resolved())))));
    }

    public void nextAnnotation(boolean comments, int direction) {
        List<CellAddress> cells = new ArrayList<>(comments ? ws().properties().threads().keySet() : ws().properties().notes().keySet());
        if (comments) cells.addAll(ws().properties().notes().keySet());
        if (cells.isEmpty()) return;
        cells.sort(Comparator.comparingInt(CellAddress::row).thenComparingInt(CellAddress::column));
        CellAddress cur = editor.getSelection().active();
        CellAddress target = null;
        if (direction > 0) { for (CellAddress c : cells) if (c.compareTo(cur) > 0) { target = c; break; } if (target == null) target = cells.getFirst(); }
        else { for (int i = cells.size() - 1; i >= 0; i--) if (cells.get(i).compareTo(cur) < 0) { target = cells.get(i); break; } if (target == null) target = cells.getLast(); }
        editor.select(SheetSelection.of(target));
    }

    public void showAllComments() {
        DefaultListModel<String> model = new DefaultListModel<>();
        List<CellAddress> order = new ArrayList<>();
        ws().properties().threads().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            SheetComment first = e.getValue().comments().getFirst();
            model.addElement(e.getKey().toA1() + (e.getValue().resolved() ? " (resolvido)" : "") + " — " + first.author() + ": " + first.text() + (e.getValue().comments().size() > 1 ? "  [+" + (e.getValue().comments().size() - 1) + " respostas]" : ""));
            order.add(e.getKey());
        });
        ws().properties().notes().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> { model.addElement(e.getKey().toA1() + " — Anotação: " + e.getValue().text()); order.add(e.getKey()); });
        if (model.isEmpty()) { editor.popups().info("Comentários", "Não há comentários ou anotações nesta planilha."); return; }
        JList<String> list = new JList<>(model);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(520, 260));
        editor.popups().dialog("sheet.comments", "Comentários", scroll, list::getSelectedIndex).filter(i -> i >= 0).ifPresent(i -> editor.select(SheetSelection.of(order.get(i))));
    }

    public String tooltipFor(CellAddress cell) {
        List<String> parts = new ArrayList<>();
        int s = sheet();
        SheetProperties p = ws().properties();
        editor.validationEvaluator().find(s, cell.row(), cell.column()).filter(DataValidation::showInput).filter(v -> !v.inputMessage().isBlank() || !v.inputTitle().isBlank())
                .ifPresent(v -> parts.add("<b>" + esc(v.inputTitle()) + "</b><br>" + esc(v.inputMessage())));
        SheetNote note = p.notes().get(cell);
        if (note != null) parts.add("<b>" + esc(note.author()) + ":</b><br>" + esc(note.text()));
        SheetThread thread = p.threads().get(cell);
        if (thread != null) for (SheetComment c : thread.comments()) parts.add("<b>" + esc(c.author()) + "</b>: " + esc(c.text()));
        Hyperlink link = p.links().get(cell);
        if (link != null) parts.add(esc(link.tooltip() != null ? link.tooltip() : link.target()) + "<br><i>Clique para seguir o link.</i>");
        CellValue v = editor.getEngine().valueAt(s, cell);
        SheetCell c = ws().cell(cell);
        if (v instanceof ErrorValue e && c.hasFormula() && errorIndicators) parts.add(errorExplanation(e));
        if (parts.isEmpty()) return null;
        return "<html><div style='width:260px'>" + String.join("<hr>", parts) + "</div></html>";
    }

    private static String esc(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>"); }

    public static String errorExplanation(ErrorValue e) {
        return switch (e.error()) {
            case DIV0 -> "Erro de divisão por zero.";
            case NA -> "Um valor não está disponível para a fórmula ou função.";
            case NAME -> "A fórmula contém texto não reconhecido.";
            case REF -> "Referência de célula inválida.";
            case VALUE -> "Um valor usado na fórmula é do tipo de dados errado.";
            case NUM -> "Um número na fórmula ou função é inválido.";
            case SPILL -> "O intervalo de despejo não está vazio.";
            case CALC -> "A fórmula encontrou um erro de cálculo.";
            case NULL -> "A interseção especificada não existe.";
            default -> "A fórmula retornou " + e.error().text();
        };
    }

    public void tracePrecedents() {
        int s = sheet();
        CellAddress a = editor.getSelection().active();
        List<SheetCanvas.Highlight> list = new ArrayList<>();
        for (Dependency d : editor.getEngine().precedents(s, a.row(), a.column())) if (d.sheet() == s) list.add(new SheetCanvas.Highlight(s, d.range(), new Color(0x2B78D0)));
        if (list.isEmpty()) { editor.popups().info("Rastrear Precedentes", "A célula ativa não contém fórmulas que façam referência a outras células."); return; }
        editor.getCanvas().setHighlights(list);
    }

    public void traceDependents() {
        int s = sheet();
        CellAddress a = editor.getSelection().active();
        List<SheetCanvas.Highlight> list = new ArrayList<>();
        for (CellAddress d : editor.getEngine().dependents(s, a.row(), a.column())) list.add(new SheetCanvas.Highlight(s, CellRange.of(d), new Color(0x107C41)));
        if (list.isEmpty()) { editor.popups().info("Rastrear Dependentes", "Nenhuma fórmula faz referência à célula ativa."); return; }
        editor.getCanvas().setHighlights(list);
    }

    public void removeArrows() { editor.getCanvas().setHighlights(List.of()); }

    public void circleInvalid() {
        int s = sheet();
        List<SheetCanvas.Highlight> list = new ArrayList<>();
        for (CellAddress a : editor.validationEvaluator().invalidCells(s)) list.add(new SheetCanvas.Highlight(s, CellRange.of(a), new Color(0xD13438)));
        if (list.isEmpty()) editor.popups().info("Circular Dados Inválidos", "Nenhum dado inválido encontrado.");
        editor.getCanvas().setHighlights(list);
    }

    public void toggleShowFormulas() {
        SheetWorksheet w = ws();
        w.setProperties(w.properties().withShowFormulas(!w.properties().showFormulas()));
        editor.getCanvas().repaint();
        editor.commandRegistry().refresh();
    }

    public void errorCheck() {
        int s = sheet();
        CellAddress start = editor.getSelection().active();
        List<CellAddress> errors = new ArrayList<>();
        ws().cells().forEach((row, col, cell) -> {
            if (cell.hasFormula() && editor.getEngine().valueAt(s, row, col) instanceof ErrorValue) errors.add(new CellAddress(row, col));
        });
        if (errors.isEmpty()) { editor.popups().info("Verificação de Erros", "A verificação de erros foi concluída para toda a planilha."); return; }
        errors.sort(Comparator.naturalOrder());
        CellAddress next = errors.stream().filter(e -> e.compareTo(start) > 0).findFirst().orElse(errors.getFirst());
        editor.select(SheetSelection.of(next));
        ErrorValue e = (ErrorValue) editor.getEngine().valueAt(s, next);
        editor.popups().info("Verificação de Erros", "Erro na célula " + next.toA1() + ":\n" + editor.editing().editText(s, next) + "\n\n" + errorExplanation(e));
    }

    public void evaluateFormula() {
        int s = sheet();
        CellAddress a = editor.getSelection().active();
        SheetCell c = ws().cell(a);
        if (!c.hasFormula()) { editor.popups().info("Avaliar Fórmula", "A célula ativa não contém fórmula."); return; }
        StringBuilder b = new StringBuilder();
        b.append("Referência: ").append(ws().name()).append("!").append(a.toA1()).append("\n\n");
        b.append("Fórmula: ").append(editor.editing().editText(s, a)).append("\n\n");
        String canonical = c.formula();
        String replaced = canonical;
        for (RefNode ref : ReferenceAdjuster.references(canonical)) {
            if (ref.range() == null || !ref.range().isSingleCell()) continue;
            int rs = ref.sheet() == null ? s : editor.getWorkbook().indexOf(ref.sheet());
            if (rs < 0) continue;
            CellValue v = editor.getEngine().valueAt(rs, ref.range().first());
            b.append("  ").append(ref.sheet() == null ? "" : ref.sheet() + "!").append(ref.range().toA1()).append(" = ").append(editor.displayText(rs, ref.range().first())).append("\n");
            if (v instanceof dtm.stools.component.panels.editor.sheet.model.NumberValue n) replaced = replaced.replaceAll("(?<![A-Za-z$!])\\$?" + CellAddress.columnName(ref.range().firstColumn()) + "\\$?" + (ref.range().firstRow() + 1) + "(?!\\d)", java.util.regex.Matcher.quoteReplacement(dtm.stools.component.panels.editor.sheet.model.NumberValue.general(n.value())));
        }
        try {
            b.append("\nCom valores: =").append(Formulas.toDisplay(replaced, editor.formulaLocale()));
        } catch (RuntimeException ignored) { }
        b.append("\n\nResultado: ").append(editor.displayText(s, a));
        JTextArea area = new JTextArea(b.toString(), 14, 56);
        area.setEditable(false);
        area.setLineWrap(true);
        editor.popups().dialog(dtm.stools.component.panels.editor.sheet.provider.SheetDialogRequest.of(editor, "sheet.evaluate", "Avaliar Fórmula", new JScrollPane(area), () -> Boolean.TRUE).withConfirmText("Fechar"));
    }

    public void watchWindow() { watch.addSelection(); watch.open(); }

    public void spelling() {
        editor.popups().info("Verificar Ortografia", "Nenhum verificador ortográfico está configurado. Registre um SheetCommandProvider com o comando de revisão desejado.");
    }

    public void statistics() {
        int formulas = 0, cells = 0, charts = 0, images = 0, tables = 0, notes = 0, comments = 0;
        for (SheetWorksheet w : editor.getWorkbook().sheets()) {
            int[] c = {0, 0};
            w.cells().forEach((r, col, cell) -> { if (cell.hasContent()) c[0]++; if (cell.hasFormula()) c[1]++; });
            cells += c[0];
            formulas += c[1];
            tables += w.properties().tables().size();
            notes += w.properties().notes().size();
            comments += w.properties().threads().size();
            for (var o : w.properties().objects()) { if (o instanceof dtm.stools.component.panels.editor.sheet.model.SheetChart) charts++; else if (o instanceof dtm.stools.component.panels.editor.sheet.model.SheetImage) images++; }
        }
        CellRange used = ws().usedRange();
        editor.popups().info("Estatísticas da Pasta de Trabalho", "Planilha atual: " + ws().name() + "\n  Fim da planilha: " + (used == null ? "A1" : used.last().toA1())
                + "\n\nPasta de trabalho\n  Planilhas: " + editor.getWorkbook().sheetCount() + "\n  Células com dados: " + cells + "\n  Fórmulas: " + formulas + "\n  Tabelas: " + tables
                + "\n  Gráficos: " + charts + "\n  Imagens: " + images + "\n  Anotações: " + notes + "\n  Comentários: " + comments);
    }

    public void aiAssistant() {
        List<SheetAiProvider> providers = editor.providers(SheetAiProvider.class);
        if (providers.isEmpty()) { editor.popups().info("Assistente", "Nenhum assistente de IA está configurado. Registre um SheetAiProvider para habilitar este recurso."); return; }
        AiAssistantPanel.open(editor, providers.getFirst());
    }
}
