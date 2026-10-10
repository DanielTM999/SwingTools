package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfTextStyle;
import dtm.stools.configs.UiTokens;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;

public class PdfTextOverlay extends JTextArea {
    private static final int PAD = 4;

    private final PdfEditor editor;
    private final PdfCanvas canvas;
    private final int page;
    private final Rectangle2D.Float anchor;
    private final boolean fixedWidth;
    private final String elementId;
    private final boolean word;
    private PdfTextStyle style;
    private boolean finished;

    public PdfTextOverlay(PdfEditor editor, PdfCanvas canvas, int page, Rectangle2D.Float anchor, boolean fixedWidth,
                          String text, PdfTextStyle style, String elementId, boolean word) {
        super(text == null ? "" : text);
        this.editor = editor;
        this.canvas = canvas;
        this.page = page;
        this.anchor = (Rectangle2D.Float) anchor.clone();
        this.fixedWidth = fixedWidth;
        this.elementId = elementId;
        this.word = word;
        this.style = style;
        setName("pdf.text.overlay");
        setOpaque(false);
        setLineWrap(fixedWidth);
        setWrapStyleWord(true);
        setBorder(new OverlayBorder());
        setCaretColor(UiTokens.accent());
        setSelectionColor(UiTokens.overlay(UiTokens.accent(), .3f));
        applyStyle(style);
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "pdf.cancelText");
        getActionMap().put("pdf.cancelText", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) { cancel(); }
        });
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "pdf.commitText");
        if (word) getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "pdf.commitText");
        getActionMap().put("pdf.commitText", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) { commit(); }
        });
        UndoManager history = new UndoManager();
        getDocument().addUndoableEditListener(history);
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "pdf.text.undo");
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "pdf.text.redo");
        getActionMap().put("pdf.text.undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) { if (history.canUndo()) history.undo(); }
        });
        getActionMap().put("pdf.text.redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) { if (history.canRedo()) history.redo(); }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                if (event.isTemporary()) return;
                Component next = event.getOppositeComponent();
                if (next != null && SwingUtilities.isDescendingFrom(next, editor) && !(next instanceof PdfCanvas)
                        && SwingUtilities.getAncestorOfClass(PdfRibbon.class, next) != null) return;
                SwingUtilities.invokeLater(() -> commit());
            }
        });
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) { relayout(); }
            @Override
            public void removeUpdate(DocumentEvent event) { relayout(); }
            @Override
            public void changedUpdate(DocumentEvent event) { relayout(); }
        });
    }

    public int page() { return page; }
    public Rectangle2D.Float anchor() { return (Rectangle2D.Float) anchor.clone(); }
    public boolean fixedWidth() { return fixedWidth; }
    public String elementId() { return elementId; }
    public boolean word() { return word; }
    public PdfTextStyle style() { return style; }
    public boolean finished() { return finished; }

    public void applyStyle(PdfTextStyle value) {
        style = value;
        double scale = canvas.getPageLayout().geometry(page) == null ? 1 : canvas.getPageLayout().geometry(page).scale();
        String family = switch (value.family()) {
            case PdfTextStyle.TIMES -> Font.SERIF;
            case PdfTextStyle.COURIER -> Font.MONOSPACED;
            default -> Font.SANS_SERIF;
        };
        int fontStyle = (value.bold() ? Font.BOLD : 0) | (value.italic() ? Font.ITALIC : 0);
        setFont(new Font(family, fontStyle, 12).deriveFont((float) Math.max(6, value.size() * scale)));
        setForeground(value.color());
        relayout();
    }

    public void relayout() {
        PdfPageLayout layout = canvas.getPageLayout();
        if (!layout.contains(page)) return;
        Rectangle2D.Double view = layout.toView(page, anchor);
        int x = (int) Math.round(view.x) - PAD, y = (int) Math.round(view.y) - PAD;
        int width;
        if (fixedWidth) width = (int) Math.round(view.width) + 2 * PAD;
        else {
            setSize(new Dimension(4000, 100));
            width = Math.max(80, getPreferredSize().width + 12);
        }
        setSize(width, 10);
        int height = Math.max(getPreferredSize().height, fixedWidth ? (int) Math.round(view.height) + 2 * PAD : 0);
        setBounds(x, y, width, height);
        canvas.repaint();
    }

    public void commit() {
        if (finished) return;
        finished = true;
        detach();
        editor.finishTextInput(this, getText());
    }

    public void cancel() {
        if (finished) return;
        finished = true;
        detach();
        editor.cancelTextInput(this);
    }

    private void detach() {
        canvas.remove(this);
        canvas.repaint();
        canvas.requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(new Color(255, 255, 255, 215));
            g.fillRect(0, 0, getWidth(), getHeight());
        } finally { g.dispose(); }
        super.paintComponent(graphics);
    }

    private static final class OverlayBorder implements Border {
        @Override
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setColor(UiTokens.accent());
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 4, new float[]{4, 3}, 0));
                g.drawRect(x, y, width - 1, height - 1);
            } finally { g.dispose(); }
        }
        @Override
        public Insets getBorderInsets(Component component) { return new Insets(PAD, PAD, PAD, PAD); }
        @Override
        public boolean isBorderOpaque() { return false; }
    }
}
