package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.activity.Activity;
import dtm.stools.component.panels.tab.EventTabbedPanel;
import dtm.stools.component.panels.tab.TabEvent;
import dtm.stools.component.panels.tab.TabWindowRequest;
import dtm.stools.component.panels.tab.TabbedPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;


public class TabbedPanelActivityWindowFactoryExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            createAndShow();
        });
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("TabbedPanel - Window Factory com Activity");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 640);
        frame.setLocationRelativeTo(null);

        TabbedPanel tabs = new TabbedPanel(JTabbedPane.TOP);
        tabs.setTabWindowEnabled(true);
        tabs.setScrollableTabsEnabled(true);
        tabs.setDefaultTabWindowSize(new Dimension(680, 460));

       tabs.setTabWindowFactory((source, request) -> {
            if ("readme".equals(request.getKey())) {
                // devolver null faz cair no comportamento padrao para esta aba
                return null;
            }
            TabActivityWindow activity = new TabActivityWindow(request);
            activity.init();
            return activity;
        });

        tabs.addEventListener(EventTabbedPanel.TAB_WINDOW_OPEN, event -> {
            TabEvent tabEvent = (TabEvent) event;
            Window window = (Window) tabEvent.getProperties().get("window");
            System.out.println("Janela aberta para '" + tabEvent.getKey() + "': " + window.getClass().getSimpleName());
        });

        tabs.addEventListener(EventTabbedPanel.TAB_WINDOW_CLOSE, event -> {
            TabEvent tabEvent = (TabEvent) event;
            System.out.println("Aba reencaixada: " + tabEvent.getKey());
        });

        tabs.addTab("editor", "Editor.java", createTextPanel("""
                public class Editor {
                    public void open() {
                        System.out.println("arraste esta aba para fora");
                    }
                }
                """));
        tabs.addTab("console", "Console", createTextPanel("""
                > build finished
                > 0 warnings
                """));
        tabs.addTab("readme", "README.md (janela padrao)", createTextPanel("""
                Esta aba devolve null na factory,
                entao abre no JDialog padrao do TabbedPanel.
                """));

        JButton openInWindow = new JButton("Abrir aba atual em janela");
        openInWindow.addActionListener(e -> {
            String key = tabs.getCurrentKey();
            if (key != null) tabs.openTabInWindow(key);
        });

        JButton reattachAll = new JButton("Reencaixar todas");
        reattachAll.addActionListener(e -> tabs.reattachAllWindows());

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.add(openInWindow);
        toolbar.add(reattachAll);

        frame.setLayout(new BorderLayout());
        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(tabs, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static JComponent createTextPanel(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return new JScrollPane(area);
    }

    /**
     * Activity usada como janela de aba destacada.
     */
    private static class TabActivityWindow extends Activity {

        private final TabWindowRequest request;

        private TabActivityWindow(TabWindowRequest request) {
            super(request.getTitle());
            this.request = request;
        }

        @Override
        protected void onDrawing() {
            setAlwaysOnTop(request.isAlwaysOnTop());

            if (request.getIcon() instanceof ImageIcon imageIcon && imageIcon.getImage() != null) {
                setIconImage(imageIcon.getImage());
            }

            JLabel banner = new JLabel("  Aba '" + request.getKey() + "' rodando dentro de uma Activity");
            banner.setOpaque(true);
            banner.setBackground(new Color(0x1D4ED8));
            banner.setForeground(Color.WHITE);
            banner.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(banner, BorderLayout.NORTH);
            getContentPane().add(request.getComponent(), BorderLayout.CENTER);

            Dimension size = request.getPreferredWindowSize();
            setSize(size != null ? size : new Dimension(640, 420));

            Point screenLocation = request.getScreenLocation();
            if (screenLocation != null) {
                setLocation(screenLocation.x - getWidth() / 2, Math.max(0, screenLocation.y - 12));
            } else {
                setLocationRelativeTo(request.getOwner());
            }
        }

        /**
         * A Activity intercepta o WINDOW_CLOSING e decide sozinha o que fazer, entao basta
         * fechar a janela aqui. O TabbedPanel detecta que a janela e uma IWindow e reencaixa
         * a aba quando ela e realmente descartada (windowClosed).
         */
        @Override
        protected void onClose(WindowEvent e) {
            dispose();
        }
    }
}
