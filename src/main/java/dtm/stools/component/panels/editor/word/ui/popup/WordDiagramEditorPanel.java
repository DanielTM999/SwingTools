package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.function.Supplier;

public final class WordDiagramEditorPanel extends WordPropertiesPanel<WordDiagram> {
    private final WordDiagram diagram;
    private final WordDocument document;
    private final WordObjectRegistry registry;
    private final JList<WordDiagramLayout> gallery = new JList<>(WordDiagramLayout.values());
    private final JTextArea outline = new JTextArea(7,28);
    private final ColorButton color;
    private final JSpinner width, height;
    private final JTextField alt = new JTextField();
    private final Supplier<WordPlacement> placement;
    private final JLabel error = new JLabel(" ");
    private WordDiagram last;
    private final JComponent preview = new JComponent() {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            try {
                g2.setColor(Color.WHITE); g2.fillRect(0,0,getWidth(),getHeight());
                float s = Math.min((getWidth()-8)/last.width(),(getHeight()-8)/last.height()), w = last.width()*s, h = last.height()*s;
                registry.paint(g2,last.resize(Math.max(48,w),Math.max(36,h)),new Rectangle2D.Float((getWidth()-w)/2,(getHeight()-h)/2,w,h),document);
            } finally { g2.dispose(); }
        }
    };

    public WordDiagramEditorPanel(WordDiagram diagram, WordDocument document, WordObjectRegistry registry) {
        this.diagram = diagram; this.document = document; this.registry = registry; this.last = diagram;
        gallery.setVisibleRowCount(5); gallery.setSelectedValue(diagram.layout(),true);
        row("Layout",new JScrollPane(gallery));
        outline.setText(diagram.outline()); outline.setTabSize(4);
        row("Itens (Tab cria subnível)",new JScrollPane(outline));
        color = new ColorButton(diagram.color(),false); row("Cor",color);
        width = row("Largura (pt)",number(diagram.width(),48,1440,1)); height = row("Altura (pt)",number(diagram.height(),36,1440,1));
        placement = placement(diagram.placement());
        alt.setText(diagram.altText()); row("Texto alternativo",alt);
        preview.setPreferredSize(new Dimension(420,180)); preview.setBorder(BorderFactory.createLineBorder(new Color(0xD1D5DB)));
        wide(preview,1);
        error.setForeground(new Color(0xB91C1C)); wide(error,0);
        gallery.addListSelectionListener(e -> refresh());
        color.addActionListener(e -> SwingUtilities.invokeLater(this::refresh));
        outline.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
    }
    private void refresh() {
        try { last = build(); error.setText(" "); } catch (IllegalArgumentException e) { error.setText(e.getMessage()); }
        preview.repaint();
    }
    private WordDiagram build() {
        WordDiagram parsed = WordDiagram.parse(gallery.getSelectedValue() == null ? diagram.layout() : gallery.getSelectedValue(),outline.getText());
        return diagram.withLayout(parsed.layout()).withNodes(parsed.nodes()).withColor(color.color()).resize(value(width),value(height));
    }
    @Override public String title() { return "Diagrama"; }
    @Override public WordDiagram result() { return build().withPlacement(placement.get()).withAltText(alt.getText().strip()); }
}
