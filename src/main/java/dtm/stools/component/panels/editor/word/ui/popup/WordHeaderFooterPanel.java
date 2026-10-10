package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.*;
import javax.swing.*;
import java.util.List;

public final class WordHeaderFooterPanel extends WordPropertiesPanel<WordHeaderFooterPanel.Result> {
    public record Result(WordHeaders.Kind kind, String text, WordParagraphStyle.Alignment alignment, boolean pageNumber, boolean differentFirst, boolean differentOddEven, boolean linked, boolean textChanged) {
        public Result(WordHeaders.Kind kind,String text,WordParagraphStyle.Alignment alignment,boolean pageNumber,boolean differentFirst,boolean differentOddEven) {
            this(kind,text,alignment,pageNumber,differentFirst,differentOddEven,false,true);
        }
    }
    private final JComboBox<String> kind;
    private final WordHeaders.Kind[] kinds;
    private final JTextArea text = new JTextArea(4,30);
    private final JComboBox<String> alignment = new JComboBox<>(new String[]{"Esquerda","Centro","Direita"});
    private final JCheckBox pageNumber = new JCheckBox("Incluir “Página X de Y”"), first = new JCheckBox("Primeira página diferente"), oddEven = new JCheckBox("Páginas pares e ímpares diferentes");
    private final WordHeaders headers;
    private final java.util.Set<WordHeaders.Kind> links;
    private final boolean firstSection;
    private final JCheckBox linked=new JCheckBox("Vincular à seção anterior");
    private String initialText;private int initialAlignment;private boolean initialPageNumber,complex;
    private final JLabel warning = new JLabel(" ");

    public WordHeaderFooterPanel(WordHeaders headers, boolean footer) {
        this(headers,footer,java.util.Set.of(),true);
    }
    public WordHeaderFooterPanel(WordHeaders headers,boolean footer,java.util.Set<WordHeaders.Kind> links,boolean firstSection) {
        this.headers = headers;this.links=java.util.Set.copyOf(links);this.firstSection=firstSection;
        kinds = footer ? new WordHeaders.Kind[]{WordHeaders.Kind.FOOTER,WordHeaders.Kind.FIRST_FOOTER,WordHeaders.Kind.EVEN_FOOTER} : new WordHeaders.Kind[]{WordHeaders.Kind.HEADER,WordHeaders.Kind.FIRST_HEADER,WordHeaders.Kind.EVEN_HEADER};
        kind = new JComboBox<>(new String[]{footer ? "Rodapé padrão" : "Cabeçalho padrão","Primeira página","Páginas pares"});
        row("Editar",kind);
        linked.setOpaque(false);linked.setEnabled(!firstSection);row(null,linked);
        linked.addActionListener(e->sync());
        text.setLineWrap(true); row("Texto",new JScrollPane(text));
        row("Alinhamento",alignment);
        pageNumber.setOpaque(false); row(null,pageNumber);
        first.setOpaque(false); first.setSelected(headers.differentFirst()); row(null,first);
        oddEven.setOpaque(false); oddEven.setSelected(headers.differentOddEven()); row(null,oddEven);
        warning.setForeground(new java.awt.Color(0xB45309)); wide(warning,0);
        kind.addActionListener(e -> load()); load();
    }
    private void load() {
        List<WordBlock> blocks = headers.get(kinds[kind.getSelectedIndex()]);
        StringBuilder b = new StringBuilder(); boolean page = false, objects = false;
        WordParagraphStyle.Alignment a = WordParagraphStyle.Alignment.CENTER;
        for (WordBlock block : blocks) {
            if (!(block instanceof WordParagraph p)) { objects = true; continue; }
            a = p.style().alignment();
            StringBuilder line = new StringBuilder();
            for (WordInline inline : p.runs()) {
                if (inline instanceof WordObjectRun o) { if (o.object() instanceof WordField f && (f.kind() == WordField.Kind.PAGE || f.kind() == WordField.Kind.NUM_PAGES)) page = true; else objects = true; }
                else line.append(inline.text());
            }
            String l = line.toString().replace("  •  Página ","").replace("Página ","").replace(" de ","").strip();
            if (!b.isEmpty()) b.append('\n'); b.append(page ? l : line);
        }
        text.setText(b.toString().strip()); pageNumber.setSelected(page); alignment.setSelectedIndex(a.ordinal() > 2 ? 0 : a.ordinal());
        complex=objects;initialText=text.getText();initialAlignment=alignment.getSelectedIndex();initialPageNumber=pageNumber.isSelected();
        linked.setSelected(!firstSection&&links.contains(kinds[kind.getSelectedIndex()]));
        warning.setText(objects ? "Conteúdo com objetos: preservado. Este formulário permite alterar opções e vínculo." : " ");
        sync();
    }
    private void sync(){boolean edit=!complex&&!linked.isSelected();text.setEditable(edit);alignment.setEnabled(edit);pageNumber.setEnabled(edit);}
    @Override
    public String title() { return "Cabeçalho e rodapé"; }
    @Override
    public Result result() {
        return new Result(kinds[kind.getSelectedIndex()],text.getText(),WordParagraphStyle.Alignment.values()[alignment.getSelectedIndex()],pageNumber.isSelected(),first.isSelected(),oddEven.isSelected(),linked.isSelected(),
                !text.getText().equals(initialText)||alignment.getSelectedIndex()!=initialAlignment||pageNumber.isSelected()!=initialPageNumber);
    }
}
