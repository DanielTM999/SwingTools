package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.CodeEditorMinimap;
import dtm.stools.component.panels.editor.code.hover.HoverDocumentationProvider;
import dtm.stools.component.panels.editor.code.hover.HoverInfo;
import dtm.stools.component.panels.editor.code.provider.ContextMenuProvider;
import dtm.stools.component.panels.tab.TabbedPanel;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;

public class CodeEditorTransientPopupLeakExample {

    private static final String SOURCE = """
            public class LeakDemo {

                private final CodeEditor editor = new CodeEditor();

                public void openHoverThenWaitForTheTabSwitch() {
                    HoverInfo info = HoverInfo.markdown("documentation");
                    editor.addProvider(provider());
                }

                private HoverDocumentationProvider provider() {
                    return context -> null;
                }

                public void foldThisBlock() {
                    int a = 1;
                    int b = 2;
                    int c = 3;
                    int d = 4;
                    int e = 5;
                    int f = 6;
                }
            }
            """;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorTransientPopupLeakExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor transient popup leak example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1080, 700);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        CodeEditor tabEditor = createEditor();
        CodeEditor cardEditor = createEditor();

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);
        cards.add(cardEditor, "editor");
        cards.add(new JPanel(), "empty");

        TabbedPanel tabs = new TabbedPanel();
        tabs.addTab("hover", "Hover / menu", tabEditor);
        tabs.addTab("cards", "CardLayout", cards);

        Timer tabSwitcher = new Timer(1200, e -> tabs.switchNext());
        tabSwitcher.start();

        Timer cardSwitcher = new Timer(1700, new java.awt.event.ActionListener() {
            private boolean showingEditor = true;

            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showingEditor = !showingEditor;
                cardLayout.show(cards, showingEditor ? "editor" : "empty");
            }
        });
        cardSwitcher.start();

        JButton detach = new JButton("Mover editor da aba 1 para nova janela");
        detach.addActionListener(e -> moveToNewWindow(tabs, tabEditor));

        JPanel toolbar = new JPanel();
        toolbar.add(detach);

        frame.add(tabs, BorderLayout.CENTER);
        frame.add(toolbar, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static void moveToNewWindow(TabbedPanel tabs, CodeEditor editor) {
        tabs.removeTab("hover");

        JFrame detached = new JFrame("Editor destacado");
        detached.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        detached.setSize(760, 520);
        detached.setLocationRelativeTo(null);
        detached.setLayout(new BorderLayout());
        detached.add(editor, BorderLayout.CENTER);
        detached.setVisible(true);
    }

    private static CodeEditor createEditor() {
        CodeEditor editor = new CodeEditor(SOURCE);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        editor.setFoldingEnabled(true);
        editor.setMinimapVisibilityMode(CodeEditorMinimap.VisibilityMode.ALWAYS);
        editor.setHoverDelay(200);

        editor.addProvider((HoverDocumentationProvider) context -> {
            sleep(600);
            return HoverInfo.markdown("""
                    **Documentacao lenta**

                    Este popup so chega na EDT depois que a aba ja trocou.
                    Nenhum retangulo deve sobrar na tela.
                    """);
        });

        editor.addProvider((ContextMenuProvider) e -> {
            sleep(600);
            JPopupMenu menu = new JPopupMenu();
            menu.add(new JMenuItem("Acao lenta 1"));
            menu.add(new JMenuItem("Acao lenta 2"));
            return menu;
        });

        return editor;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
