package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.menu.bar.CollapsibleMenuBar;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CollapsibleMenuBarShrinkingParentExample {

    private static final int ROOT_WIDTH = 1000;
    private static final int ROOT_HEIGHT = 600;
    private static final int TITLE_HEIGHT = 40;

    public static void main(String[] args) {
        boolean auto = args.length > 0 && "auto".equalsIgnoreCase(args[0]);
        if (auto) {
            runHeadless();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();
            createAndShow();
        });
    }

    private static void runHeadless() {
        StringBuilder log = new StringBuilder();
        boolean passed;
        try {
            passed = verifyShrinkingParentRepaint(log);
        } catch (Exception e) {
            log.append("EXCEPTION: ").append(e).append('\n');
            passed = false;
        }
        System.out.println(log);
        System.out.println(passed
                ? "RESULT: PASS - collapse/expand dirties the full title strip."
                : "RESULT: FAIL - the freed strip was not repainted.");
        System.exit(passed ? 0 : 1);
    }

    private static boolean verifyShrinkingParentRepaint(StringBuilder log) throws Exception {
        Harness harness = buildHarness();

        layoutOnEdt(harness.rootPane);
        int expandedHostWidth = widthOf(harness.titleBar.menuHost);

        harness.rootPane.startRecording();
        runOnEdt(() -> harness.menuBar.setCollapsed(true));
        drainEdt();
        layoutOnEdt(harness.rootPane);
        List<Rectangle> collapseRepaints = harness.rootPane.stopRecording();
        int collapsedHostWidth = widthOf(harness.titleBar.menuHost);

        harness.rootPane.startRecording();
        runOnEdt(() -> harness.menuBar.setCollapsed(false));
        drainEdt();
        layoutOnEdt(harness.rootPane);
        List<Rectangle> expandRepaints = harness.rootPane.stopRecording();
        int reexpandedHostWidth = widthOf(harness.titleBar.menuHost);

        Rectangle collapseStrip = widestThinStrip(collapseRepaints);
        Rectangle expandStrip = widestThinStrip(expandRepaints);

        log.append("=== CollapsibleMenuBar shrinking-parent simulation ===\n\n");
        log.append("Root pane size ............... ").append(ROOT_WIDTH).append('x').append(ROOT_HEIGHT).append('\n');
        log.append("Host width when expanded ..... ").append(expandedHostWidth).append(" px\n");
        log.append("Host width when collapsed .... ").append(collapsedHostWidth).append(" px\n");
        log.append("Host width after re-expand ... ").append(reexpandedHostWidth).append(" px\n\n");
        log.append("Collapse repaint strip ....... ").append(describe(collapseStrip)).append('\n');
        log.append("Expand repaint strip ......... ").append(describe(expandStrip)).append('\n');
        log.append("Repaints seen on collapse .... ").append(collapseRepaints.size()).append('\n');
        log.append("Repaints seen on expand ...... ").append(expandRepaints.size()).append('\n');

        boolean c1 = expandedHostWidth > 0;
        boolean c2 = collapsedHostWidth < expandedHostWidth;
        boolean c3 = isFullWidthStrip(collapseStrip);
        boolean c4 = isFullWidthStrip(expandStrip);
        boolean c5 = collapseStrip != null && collapseStrip.x == 0
                && collapseStrip.x + collapseStrip.width >= expandedHostWidth;

        log.append('\n');
        log.append(check("C1 host laid out while expanded", c1));
        log.append(check("C2 parent host SHRINKS on collapse (reproduces the bug scenario)", c2));
        log.append(check("C3 collapse dirties a FULL-WIDTH title strip (not just the host)", c3));
        log.append(check("C4 expand dirties a FULL-WIDTH title strip", c4));
        log.append(check("C5 the strip covers the area the menus used to occupy", c5));
        log.append('\n');
        log.append("Note: before the fix, only the shrunken host (")
                .append(collapsedHostWidth)
                .append(" px wide) was repainted, so C3/C5 would FAIL.\n");

        return c1 && c2 && c3 && c4 && c5;
    }

    private static Harness buildHarness() throws Exception {
        Harness[] holder = new Harness[1];
        SwingUtilities.invokeAndWait(() -> {
            RecordingRootPane rootPane = new RecordingRootPane();
            CollapsibleMenuBar menuBar = newConfiguredMenuBar();
            ShrinkingTitleBar titleBar = new ShrinkingTitleBar(menuBar);

            JPanel fakeContent = new JPanel();
            fakeContent.setOpaque(true);
            fakeContent.setBackground(new Color(0x1E1F22));

            Container content = rootPane.getContentPane();
            content.setLayout(new BorderLayout());
            content.add(titleBar, BorderLayout.NORTH);
            content.add(fakeContent, BorderLayout.CENTER);
            rootPane.setSize(ROOT_WIDTH, ROOT_HEIGHT);

            holder[0] = new Harness(rootPane, titleBar, menuBar);
        });
        return holder[0];
    }

    private static CollapsibleMenuBar newConfiguredMenuBar() {
        CollapsibleMenuBar bar = new CollapsibleMenuBar();
        bar.setCollapseButtonVisibleWhenExpanded(true);
        bar.addMenu("file", "Arquivo").addItem("new", "Novo");
        bar.addMenu("edit", "Editar").addItem("undo", "Desfazer");
        bar.addMenu("view", "Exibir").addItem("zoom", "Zoom");
        bar.addMenu("run", "Executar").addItem("start", "Iniciar");
        bar.addMenu("tools", "Ferramentas").addItem("opt", "Opcoes");
        bar.addMenu("help", "Ajuda").addItem("about", "Sobre");

        JLabel vcs = new JLabel("VCS: main");
        vcs.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        bar.preMenu(vcs);
        bar.setCollapsed(false);
        return bar;
    }

    private record Harness(RecordingRootPane rootPane,
                            ShrinkingTitleBar titleBar,
                            CollapsibleMenuBar menuBar) {
    }

    private static boolean isFullWidthStrip(Rectangle strip) {
        return strip != null
                && strip.width >= ROOT_WIDTH
                && strip.height > 0
                && strip.height < ROOT_HEIGHT / 2
                && strip.y < ROOT_HEIGHT / 4;
    }

    private static Rectangle widestThinStrip(List<Rectangle> rectangles) {
        Rectangle best = null;
        for (Rectangle r : rectangles) {
            boolean thin = r.height > 0 && r.height < ROOT_HEIGHT / 2 && r.y < ROOT_HEIGHT / 4;
            if (thin && (best == null || r.width > best.width)) {
                best = r;
            }
        }
        return best;
    }

    private static String describe(Rectangle r) {
        return r == null ? "<none>"
                : "x=" + r.x + " y=" + r.y + " w=" + r.width + " h=" + r.height;
    }

    private static String check(String name, boolean ok) {
        return (ok ? "  [PASS] " : "  [FAIL] ") + name + '\n';
    }

    private static int widthOf(Component component) throws Exception {
        int[] holder = new int[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = component.getWidth());
        return holder[0];
    }

    private static void layoutOnEdt(Component root) throws Exception {
        SwingUtilities.invokeAndWait(() -> layoutTree(root));
    }

    private static void layoutTree(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutTree(child);
            }
        }
    }

    private static void runOnEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static void drainEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static final class RecordingRootPane extends JRootPane {
        private final List<Rectangle> recorded = new CopyOnWriteArrayList<>();
        private volatile boolean recording;

        void startRecording() {
            recorded.clear();
            recording = true;
        }

        List<Rectangle> stopRecording() {
            recording = false;
            return List.copyOf(recorded);
        }

        @Override
        public void repaint(long tm, int x, int y, int width, int height) {
            if (recording) {
                recorded.add(new Rectangle(x, y, width, height));
            }
            super.repaint(tm, x, y, width, height);
        }
    }

    private static final class ShrinkingTitleBar extends JPanel {
        private final JPanel menuHost = new JPanel(new GridBagLayout());
        private final JPanel caption = new JPanel();

        ShrinkingTitleBar(CollapsibleMenuBar menuBar) {
            setLayout(null);
            setOpaque(true);
            setBackground(new Color(0x2B2D3A));
            setPreferredSize(new Dimension(10, TITLE_HEIGHT));

            menuHost.setOpaque(false);
            menuHost.add(menuBar);

            caption.setOpaque(false);

            add(caption);
            add(menuHost);
        }

        @Override
        public void doLayout() {
            int width = getWidth();
            int height = getHeight();
            int hostWidth = Math.min(menuHost.getPreferredSize().width, width);
            menuHost.setBounds(0, 0, hostWidth, height);
            caption.setBounds(hostWidth, 0, Math.max(0, width - hostWidth), height);
        }
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("CollapsibleMenuBar - Shrinking Parent Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(ROOT_WIDTH, ROOT_HEIGHT);
        frame.setLocationRelativeTo(null);

        CollapsibleMenuBar menuBar = newConfiguredMenuBar();
        ShrinkingTitleBar titleBar = new ShrinkingTitleBar(menuBar);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(new Color(0x1E1F22));

        JTextArea notes = new JTextArea("""
                Simulacao do TitleMenuBar: o host do menu e dimensionado pelo
                preferredSize da barra, entao ele encolhe ao colapsar.

                Use o botao hamburguer da barra (ou os botoes abaixo) para
                colapsar/expandir e confirme que NAO sobra rastro de menu na
                faixa que o host liberou.
                """);
        notes.setEditable(false);
        notes.setOpaque(false);
        notes.setForeground(new Color(0xC8C8C8));
        notes.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        notes.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel state = new JLabel();
        Runnable refreshState = () -> state.setText(
                "  Estado: " + (menuBar.isCollapsed() ? "COLAPSADO" : "EXPANDIDO")
                        + "   |   largura do host: " + menuBar.getParent().getWidth() + " px");
        state.setForeground(new Color(0x9CDCFE));

        JButton collapse = new JButton("Colapsar");
        collapse.addActionListener(e -> {
            menuBar.setCollapsed(true);
            SwingUtilities.invokeLater(refreshState);
        });
        JButton expand = new JButton("Expandir");
        expand.addActionListener(e -> {
            menuBar.setCollapsed(false);
            SwingUtilities.invokeLater(refreshState);
        });
        JButton toggle = new JButton("Alternar 20x rapido");
        toggle.addActionListener(e -> {
            for (int i = 0; i < 20; i++) {
                final boolean collapsed = i % 2 == 0;
                SwingUtilities.invokeLater(() -> menuBar.setCollapsed(collapsed));
            }
            SwingUtilities.invokeLater(refreshState);
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        controls.setOpaque(false);
        controls.add(collapse);
        controls.add(expand);
        controls.add(toggle);
        controls.add(state);

        content.add(controls, BorderLayout.NORTH);
        content.add(notes, BorderLayout.CENTER);

        frame.setLayout(new BorderLayout());
        frame.add(titleBar, BorderLayout.NORTH);
        frame.add(content, BorderLayout.CENTER);
        frame.setVisible(true);
        SwingUtilities.invokeLater(refreshState);
    }
}
