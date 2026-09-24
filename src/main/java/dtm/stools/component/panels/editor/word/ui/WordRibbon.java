package dtm.stools.component.panels.editor.word.ui;

import dtm.stools.component.panels.editor.word.WordEditor;
import dtm.stools.component.panels.editor.word.api.WordCellSelection;
import dtm.stools.component.panels.editor.word.api.WordSession;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.configs.UiTokens;
import com.formdev.flatlaf.util.UIScale;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public final class WordRibbon extends JPanel {
    private record StyleItem(String id, String name) { @Override public String toString() { return name; } }
    private final WordEditor editor;
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<String,JComponent> contextual = new LinkedHashMap<>();
    private final WordFontPicker fonts = new WordFontPicker();
    private final JSpinner size = new JSpinner(new SpinnerNumberModel(11.0,1.0,1638.0,0.5));
    private final JComboBox<StyleItem> styles = new JComboBox<>();
    private final JComboBox<String> zoom = new JComboBox<>(new String[]{"50%","75%","100%","125%","150%","200%"});
    private final JLabel title = new JLabel("Documento");
    private final JPanel header = new JPanel(new BorderLayout(16,0));
    private final JPanel gallery = new JPanel(new GridLayout(1, 3, 4, 0));
    private final java.util.List<AbstractButton> editingMenus = new ArrayList<>();
    private String galleryKey = "";
    private boolean updating;

    public WordRibbon(WordEditor editor) {
        super(new BorderLayout());
        this.editor = editor;
        setBorder(BorderFactory.createEmptyBorder(8,12,6,12));
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.putClientProperty("JTabbedPane.contentBorderInsets",new Insets(0,0,0,0));
        tabs.putClientProperty("JTabbedPane.tabHeight",32);
        header.setOpaque(false);
        title.setFont(UiTokens.fontBold().deriveFont(UIScale.scale(15f)));
        header.add(title,BorderLayout.WEST);
        JToolBar quick = toolbar("word.save","word.undo","word.redo","word.find","word.palette");
        header.add(quick,BorderLayout.EAST);
        for(Component c:quick.getComponents()) if(c instanceof AbstractButton b) b.setText(null);
        header.setBorder(BorderFactory.createEmptyBorder(0,0,4,0));
        add(header,BorderLayout.NORTH);
        tabs.putClientProperty("JTabbedPane.tabType","underlined");
        tabs.addTab("Arquivo",page(group("Documento","word.new","word.open","word.save","word.saveAs"),group("Exportar","word.export.html","word.export.text","word.export.pdf","word.print"),
                group("Histórico","word.versions","word.recover"),group("Modelos","word.template","word.mailmerge")));
        tabs.addTab("Início",page(group("Área de transferência","word.paste","word.cut","word.copy"),fontGroup(),paragraphGroup(),styleGroup(),group("Edição","word.find","word.palette")));
        tabs.addTab("Inserir",page(insertTableGroup(),group("Ilustrações","word.insert.image",menu("Gráfico",chartMenu()),menu("Formas",shapeMenu()),"word.insert.textbox",menu("Diagrama",diagramMenu())),
                group("Símbolos","word.insert.equation"),group("Links","word.insert.link","word.link.remove","word.insert.bookmark","word.insert.crossref"),
                group("Quebras","word.insert.pagebreak","word.insert.columnbreak","word.insert.sectionbreak"),group("Campos","word.insert.field.page","word.insert.field.date"),
                group("Formulários","word.insert.form.text","word.insert.form.checkbox","word.insert.form.dropdown","word.insert.form.date"),group("Blocos","word.block.save",menu("Inserir bloco",(JPopupMenu)null))));
        tabs.addTab("Layout",page(group("Página","word.page.setup","word.page.portrait","word.page.landscape",menu("Tamanho",menu("word.page.a4","word.page.letter","word.page.legal","word.page.a5")),
                menu("Margens",menu("word.margins.normal","word.margins.narrow","word.margins.wide"))),group("Colunas","word.page.columns.1","word.page.columns.2","word.page.columns.3"),
                group("Cabeçalho e rodapé","word.header","word.footer")));
        tabs.addTab("Referências",page(group("Sumário","word.toc"),group("Notas","word.footnote","word.endnote"),group("Legendas","word.caption","word.insert.crossref","word.insert.bookmark")));
        tabs.addTab("Revisão",page(group("Comentários","word.comment.new","word.comments.panel"),group("Controle","word.track","word.author"),
                group("Alterações","word.change.accept","word.change.reject","word.change.next","word.change.acceptAll","word.change.rejectAll"),group("Comparar","word.compare")));
        tabs.addTab("Exibir",page(group("Modos","word.view.pages","word.view.continuous"),group("Mostrar","word.navigation","word.comments.panel","word.focus"),zoomGroup()));
        contextual.put("Tabela",page(group("Linhas e colunas","word.table.row.above","word.table.row.below","word.table.column.left","word.table.column.right","word.table.row.delete","word.table.column.delete","word.table.delete"),
                group("Mesclar","word.table.merge","word.table.split","word.table.distribute"),group("Formatação","word.table.header","word.table.fill","word.table.valign.top","word.table.valign.center","word.table.valign.bottom","word.table.properties")));
        contextual.put("Imagem",page(group("Imagem","word.object.properties","word.image.replace","word.object.rotate","word.object.alt","word.object.delete"),arrangeGroup()));
        contextual.put("Gráfico",page(group("Gráfico","word.chart.data","word.object.properties","word.object.delete"),arrangeGroup()));
        contextual.put("Equação",page(group("Equação","word.equation.edit","word.object.delete")));
        contextual.put("Forma",page(group("Forma","word.object.properties","word.object.rotate","word.shape.group","word.shape.ungroup","word.object.delete"),arrangeGroup()));
        add(tabs,BorderLayout.CENTER);
        tabs.setSelectedIndex(1);
        fonts.setChooser(editor::applyFontFamily);
        size.setPreferredSize(UIScale.scale(new Dimension(70,28))); size.getAccessibleContext().setAccessibleName("Tamanho da fonte");
        size.addChangeListener(e -> { if (!updating) editor.applyFontSize(((Number)size.getValue()).floatValue()); });
        styles.setPreferredSize(UIScale.scale(new Dimension(150,28))); styles.getAccessibleContext().setAccessibleName("Estilo do parágrafo");
        styles.addActionListener(e -> { if (!updating && styles.getSelectedItem() instanceof StyleItem item) editor.applyStyle(item.id()); });
        zoom.setSelectedItem("100%"); zoom.setPreferredSize(UIScale.scale(new Dimension(85,28)));
        zoom.addActionListener(e -> { if (!updating) editor.setZoom(Integer.parseInt(zoom.getSelectedItem().toString().replace("%",""))/100.0); });
    }
    public WordFontPicker getFontPicker() { return fonts; }
    public JSpinner getSizeControl() { return size; }
    public JComboBox<?> getStyleControl() { return styles; }
    public JTabbedPane getTabs() { return tabs; }
    public List<String> visibleContextualTabs() {
        List<String> r = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) if (contextual.containsKey(tabs.getTitleAt(i))) r.add(tabs.getTitleAt(i));
        return r;
    }
    public void setTitle(String value) { title.setText(value); }
    public void closePopups() {
        for(int i=0;i<tabs.getTabCount();i++)if(tabs.getComponentAt(i) instanceof WordRibbonPage page)page.closePopups();
        for(JComponent component:contextual.values())if(component instanceof WordRibbonPage page)page.closePopups();
    }

    private Action action(String id) { return editor.getCommands().get(id); }
    private AbstractButton button(String id) {
        Action a = action(id);
        if (a == null) throw new IllegalStateException("Unknown command " + id);
        boolean toggle = Set.of("word.bold","word.italic","word.underline","word.strike","word.superscript","word.subscript",
                "word.highlight","word.bullets","word.numbering","word.navigation","word.comments.panel","word.track","word.view.pages","word.view.continuous").contains(id) || id.startsWith("word.align.");
        AbstractButton b = toggle ? new JToggleButton(a) : new JButton(a);
        b.setName(id); b.setIcon(new WordIcon(id,16));
        b.putClientProperty("JButton.buttonType","toolBarButton");
        b.setMargin(new Insets(5,6,5,6));
        String label = switch(id) {
            case "word.bold" -> "Negrito"; case "word.italic" -> "Itálico"; case "word.underline" -> "Sublinhado";
            case "word.strike" -> "Tachado"; case "word.superscript" -> "Sobrescrito"; case "word.subscript" -> "Subscrito";
            default -> String.valueOf(a.getValue(Action.NAME));
        };
        String shortcut = switch(id) {
            case "word.bold" -> "Ctrl+B";case "word.italic" -> "Ctrl+I";case "word.underline" -> "Ctrl+U";
            case "word.copy" -> "Ctrl+C";case "word.cut" -> "Ctrl+X";case "word.paste" -> "Ctrl+V";
            case "word.save" -> "Ctrl+S";case "word.find" -> "Ctrl+F";case "word.undo" -> "Ctrl+Z";case "word.redo" -> "Ctrl+Y";
            case "word.palette" -> "Ctrl+Shift+P";default -> "";
        };
        b.getAccessibleContext().setAccessibleName(label);
        b.setToolTipText(label+(shortcut.isEmpty()?"":" ("+shortcut+")"));
        boolean iconOnly = id.startsWith("word.align.") || Set.of("word.bold","word.italic","word.underline","word.strike","word.superscript","word.subscript",
                "word.color","word.highlight","word.clearFormat","word.bullets","word.numbering","word.indent.less","word.indent.more","word.list.restart","word.shading").contains(id);
        if(iconOnly) { b.setText(null); b.setPreferredSize(UIScale.scale(new Dimension(30,30))); }
        else b.putClientProperty("word.fullText",b.getText());
        return b;
    }
    private JToolBar toolbar(String... ids) {
        JToolBar bar = new JToolBar(); bar.setFloatable(false); bar.setOpaque(false); bar.setBorderPainted(false);
        for (String id : ids) bar.add(button(id));
        return bar;
    }
    private JComponent page(JComponent... groups) { return new WordRibbonPage(groups); }
    private JComponent group(String name, Object... items) {
        java.util.List<Component> components = new ArrayList<>();
        for(Object item:items) components.add(item instanceof String id ? button(id) : (Component)item);
        JPanel content = new JPanel(new BorderLayout(4,0)); content.setOpaque(false);
        if(components.size()==1) content.add(components.getFirst());
        else if(name.equals("Área de transferência")) {
            AbstractButton paste=(AbstractButton)components.removeFirst();
            paste.setIcon(new WordIcon("word.paste",32));paste.putClientProperty("word.large",true);
            paste.setHorizontalTextPosition(SwingConstants.CENTER);paste.setVerticalTextPosition(SwingConstants.BOTTOM);
            content.add(paste,BorderLayout.WEST);
            JPanel small=new JPanel(new GridLayout(2,1,0,4));small.setOpaque(false);components.forEach(small::add);content.add(small);
        } else {
            JPanel grid=new JPanel(new GridLayout(components.size()>6?3:2,0,3,3));grid.setOpaque(false);
            components.forEach(grid::add);content.add(grid);
        }
        return new WordRibbonPage.Group(name,content);
    }
    private JButton menu(String text, JPopupMenu menu) {
        JButton b = new JButton(text + " ▾"); b.setMargin(new Insets(5,6,5,6));
        b.setIcon(new WordIcon("word."+switch(text){case "Espaçamento"->"spacing";case "Gráfico"->"chart";case "Formas"->"shape";case "Diagrama"->"diagram";case "Gerenciar"->"styles";case "Disposição"->"arrange";case "Inserir bloco"->"block.insert";case "Tamanho"->"page.size";case "Margens"->"margins";default->"page";},16));
        b.setToolTipText(text);b.getAccessibleContext().setAccessibleName(text);
        b.putClientProperty("word.fullText",b.getText());
        editingMenus.add(b);
        b.addActionListener(e -> { JPopupMenu m = menu != null ? menu : blocksMenu(); if (m.getComponentCount() > 0) m.show(b,0,b.getHeight()); });
        return b;
    }
    private JPopupMenu menu(String... ids) { JPopupMenu m = new JPopupMenu(); for (String id : ids) m.add(new JMenuItem(action(id))); return m; }
    private JPopupMenu blocksMenu() {
        JPopupMenu m = new JPopupMenu();
        for (String name : editor.getDocumentTools().buildingBlocks().keySet()) { JMenuItem item = new JMenuItem(name); item.addActionListener(e -> editor.run(() -> editor.getDocumentTools().insertBuildingBlock(name))); m.add(item); }
        if (m.getComponentCount() == 0) { JMenuItem empty = new JMenuItem("Nenhum bloco salvo"); empty.setEnabled(false); m.add(empty); }
        return m;
    }
    private JPopupMenu chartMenu() {
        JPopupMenu m = new JPopupMenu();
        for (WordChartType type : WordChartType.values()) { JMenuItem item = new JMenuItem(type.displayName()); item.addActionListener(e -> editor.run(() -> editor.getObjects().insertChart(type))); m.add(item); }
        return m;
    }
    private JPopupMenu shapeMenu() {
        JPopupMenu m = new JPopupMenu();
        for (WordShapeType type : WordShapeType.values()) {
            if (type == WordShapeType.GROUP) continue;
            JMenuItem item = new JMenuItem(type.displayName()); item.addActionListener(e -> editor.run(() -> editor.getObjects().insertShape(type))); m.add(item);
        }
        return m;
    }
    private JPopupMenu diagramMenu() {
        JPopupMenu m = new JPopupMenu();
        for (WordDiagramLayout layout : WordDiagramLayout.values()) {
            JMenuItem item = new JMenuItem(layout.toString());
            item.addActionListener(e -> editor.run(() -> editor.getObjects().insertDiagram(layout,"Planejar\nExecutar\nVerificar\nAgir")));
            m.add(item);
        }
        return m;
    }
    private JComponent insertTableGroup() {
        JButton table = new JButton("Tabela ▾"); table.setMargin(new Insets(5,6,5,6)); table.setIcon(new WordIcon("table",24)); editingMenus.add(table);
        table.addActionListener(e -> { if (!editor.isReadOnly()) WordTableGridChooser.popup((r,c) -> editor.run(() -> editor.getObjects().insertTable(r,c))).show(table,0,table.getHeight()); });
        return group("Tabelas",table,"word.insert.table");
    }
    private JComponent fontGroup() {
        JPanel box = new JPanel(new GridLayout(2,1,0,4)); box.setOpaque(false);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING,3,0)); top.setOpaque(false);
        top.add(fonts); top.add(size); top.add(button("word.clearFormat"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEADING,3,0)); bottom.setOpaque(false);
        for (String id : List.of("word.bold","word.italic","word.underline","word.strike","word.superscript","word.subscript","word.color","word.highlight")) bottom.add(button(id));
        box.add(top); box.add(bottom);
        return group("Fonte",box);
    }
    private JComponent paragraphGroup() {
        JPanel rows=new JPanel(new GridLayout(2,1,0,4));rows.setOpaque(false);
        JPanel top=new JPanel(new FlowLayout(FlowLayout.LEADING,3,0));top.setOpaque(false);
        for(String id:List.of("word.bullets","word.numbering","word.indent.less","word.indent.more","word.list.restart"))top.add(button(id));
        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.LEADING,3,0));bottom.setOpaque(false);
        for(String id:List.of("word.align.LEFT","word.align.CENTER","word.align.RIGHT","word.align.JUSTIFY","word.shading"))bottom.add(button(id));
        JButton spacing=menu("Espaçamento",menu("word.spacing.1","word.spacing.115","word.spacing.15","word.spacing.2"));
        spacing.putClientProperty("word.fullText",null);spacing.setText(null);spacing.setToolTipText("Espaçamento entre linhas");
        spacing.getAccessibleContext().setAccessibleName("Espaçamento entre linhas");spacing.setPreferredSize(UIScale.scale(new Dimension(30,30)));top.add(spacing);
        rows.add(top);rows.add(bottom);return group("Parágrafo",rows);
    }
    private JComponent styleGroup() {
        JPanel content=new JPanel(new BorderLayout(0,4));content.setOpaque(false);gallery.setOpaque(false);
        gallery.setPreferredSize(UIScale.scale(new Dimension(270,46)));content.add(gallery);
        JPanel bottom=new JPanel(new BorderLayout(4,0));bottom.setOpaque(false);bottom.add(styles);
        bottom.add(menu("Gerenciar",menu("word.style.update","word.style.new")),BorderLayout.EAST);
        content.add(bottom,BorderLayout.SOUTH);return group("Estilos",content);
    }
    private void updateGallery(String active, boolean editable) {
        var sheet=editor.getDocument().styles();
        String key=sheet.paragraphStyles().toString();
        if(!key.equals(galleryKey)) {
            galleryKey=key;gallery.removeAll();
            for(var style:sheet.paragraphStyles().stream().limit(3).toList()) {
                JToggleButton tile=new JToggleButton(style.name());tile.setName(style.id());
                tile.setFont(sheet.resolveText(style.id()).font().deriveFont(UIScale.scale(Math.min(18,sheet.resolveText(style.id()).size()))));
                tile.setToolTipText("Aplicar estilo: "+style.name());tile.setMargin(new Insets(4,6,4,6));
                tile.addActionListener(e->editor.applyStyle(style.id()));gallery.add(tile);
            }
        }
        for(Component component:gallery.getComponents()) if(component instanceof JToggleButton tile) {
            tile.setSelected(tile.getName().equals(active));tile.setEnabled(editable);
        }
    }
    private JComponent zoomGroup() { return group("Zoom",zoom,"word.zoom.out","word.zoom.in"); }
    private JComponent arrangeGroup() {
        return group("Organizar",menu("Disposição",menu("word.object.wrap.inline","word.object.wrap.square","word.object.wrap.tight","word.object.wrap.topbottom","word.object.wrap.behind","word.object.wrap.front")),
                "word.object.forward","word.object.backward");
    }

    public void update() {
        updating = true;
        try {
            WordSession session = editor.getSession();
            WordTextStyle style = session.getInsertionStyle();
            if (!fonts.getFamilies().equals(editor.getAvailableFonts())) fonts.setFamilies(editor.getAvailableFonts());
            fonts.setCurrentFamily(style.family());
            if (((Number)size.getValue()).floatValue() != style.size()) size.setValue((double)style.size());
            List<StyleItem> items = new ArrayList<>();
            for (WordNamedStyle s : editor.getDocument().styles().paragraphStyles()) items.add(new StyleItem(s.id(),s.name()));
            DefaultComboBoxModel<StyleItem> model = (DefaultComboBoxModel<StyleItem>)styles.getModel();
            boolean same = model.getSize() == items.size();
            for (int i = 0; same && i < items.size(); i++) same = model.getElementAt(i).equals(items.get(i));
            if (!same) { model.removeAllElements(); items.forEach(model::addElement); }
            String current = editor.getDocument().paragraphAt(session.getSelection().start()).style().styleId();
            String id = current == null ? WordStyleSheet.NORMAL : current;
            for (int i = 0; i < model.getSize(); i++) if (model.getElementAt(i).id().equals(id)) { styles.setSelectedIndex(i); break; }
            String z = Math.round(editor.getConfig().zoom()*100) + "%";
            if (!z.equals(zoom.getSelectedItem())) zoom.setSelectedItem(z);
            boolean editable = !session.isReadOnly();
            fonts.setEnabled(editable); size.setEnabled(editable); styles.setEnabled(editable);
            editingMenus.forEach(b->b.setEnabled(editable));updateGallery(id,editable);
            Set<String> wanted = new LinkedHashSet<>();
            var object = session.getSelectedObject();
            if (object.isPresent()) switch (object.get()) {
                case WordImage i -> wanted.add("Imagem");
                case WordChart c -> wanted.add("Gráfico");
                case WordEquation q -> wanted.add("Equação");
                case WordShape s -> wanted.add("Forma");
                case WordDiagram d -> wanted.add("Forma");
                default -> { }
            }
            if (session.getContentSelection() instanceof WordCellSelection || editor.getDocument().tableAt(session.getSelection().caret()).isPresent()) wanted.add("Tabela");
            Component selected = tabs.getSelectedComponent();
            for (var entry : contextual.entrySet()) {
                int index = tabs.indexOfComponent(entry.getValue());
                if (wanted.contains(entry.getKey()) && index < 0) tabs.addTab(entry.getKey(),entry.getValue());
                if (!wanted.contains(entry.getKey()) && index >= 0) tabs.removeTabAt(index);
            }
            if (selected != null && tabs.indexOfComponent(selected) >= 0) tabs.setSelectedComponent(selected);
        } finally { updating = false; }
    }
    @Override public void updateUI() { super.updateUI(); }
    public void onThemeChanged() {
        setBackground(UiTokens.surface()); title.setForeground(UiTokens.foreground());
        tabs.setBackground(UiTokens.surface());
        Set<Component> pages=new HashSet<>(contextual.values());
        for(int i=0;i<tabs.getTabCount();i++) pages.add(tabs.getComponentAt(i));
        for(Component page:pages) if(page instanceof WordRibbonPage ribbonPage) ribbonPage.applyTheme();
        repaint();
    }
}
