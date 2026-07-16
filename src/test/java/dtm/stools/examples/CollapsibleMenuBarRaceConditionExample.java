package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.menu.bar.CollapsibleMenuBar;
import dtm.stools.component.menu.bar.MenuBar;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class CollapsibleMenuBarRaceConditionExample {

    private static final int WORKER_THREADS = 12;
    private static final int ITERATIONS_PER_THREAD = 200;

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
        InstrumentedCollapsibleMenuBar menuBar = buildOnEdt();
        StressResult result = executeStressTest(menuBar);
        System.out.println(result.report());
        System.exit(result.passed() ? 0 : 1);
    }

    private static InstrumentedCollapsibleMenuBar buildOnEdt() {
        InstrumentedCollapsibleMenuBar[] holder = new InstrumentedCollapsibleMenuBar[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                InstrumentedCollapsibleMenuBar bar = new InstrumentedCollapsibleMenuBar();
                bar.addMenu("file", "Arquivo").addItem("new", "Novo");
                bar.addMenu("edit", "Editar").addItem("undo", "Desfazer");
                bar.addMenu("view", "Exibir").addItem("zoom", "Zoom");
                holder[0] = bar;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return holder[0];
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("CollapsibleMenuBar - Race Condition Stress Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 560);
        frame.setLocationRelativeTo(null);

        InstrumentedCollapsibleMenuBar menuBar = new InstrumentedCollapsibleMenuBar();
        menuBar.addMenu("file", "Arquivo").addItem("new", "Novo");
        menuBar.addMenu("edit", "Editar").addItem("undo", "Desfazer");
        menuBar.addMenu("view", "Exibir").addItem("zoom", "Zoom");
        frame.setJMenuBar(menuBar);

        JTextArea report = new JTextArea();
        report.setEditable(false);
        report.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JLabel status = new JLabel("Pronto. Clique em \"Run stress test\" para iniciar.");
        status.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        status.setFont(status.getFont().deriveFont(Font.BOLD, 15f));

        JButton run = new JButton("Run stress test");
        run.addActionListener(e -> {
            run.setEnabled(false);
            status.setText("Executando " + WORKER_THREADS + " threads concorrentes...");
            status.setForeground(Color.LIGHT_GRAY);
            report.setText("");
            new Thread(() -> runStressTest(menuBar, report, status, run), "stress-launcher").start();
        });

        JPanel top = new JPanel(new BorderLayout());
        top.add(status, BorderLayout.CENTER);
        top.add(run, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(report), BorderLayout.CENTER);
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static void runStressTest(InstrumentedCollapsibleMenuBar menuBar,
                                      JTextArea report,
                                      JLabel status,
                                      JButton run) {
        StressResult result = executeStressTest(menuBar);
        SwingUtilities.invokeLater(() -> {
            report.setText(result.report());
            report.setCaretPosition(0);
            status.setText(result.passed()
                    ? "PASS - nenhuma mutacao fora da EDT (" + result.elapsedMs() + " ms)"
                    : "FAIL - condicao de corrida detectada");
            status.setForeground(result.passed() ? new Color(0x4CAF50) : new Color(0xE53935));
            run.setEnabled(true);
        });
    }

    private record StressResult(boolean passed, long elapsedMs, String report) {
    }

    private static StressResult executeStressTest(InstrumentedCollapsibleMenuBar menuBar) {
        InstrumentedCollapsibleMenuBar.reset();
        int baselineMenus = menuBar.getMenus().size();
        AtomicInteger operations = new AtomicInteger();
        AtomicInteger addedMenus = new AtomicInteger();
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(WORKER_THREADS);

        for (int t = 0; t < WORKER_THREADS; t++) {
            final int threadId = t;
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                        switch (random.nextInt(7)) {
                            case 0 -> menuBar.setCollapsed(true);
                            case 1 -> menuBar.setCollapsed(false);
                            case 2 -> menuBar.toggleCollapsed();
                            case 3 -> menuBar.setCollapseButtonIconSize(16 + random.nextInt(16));
                            case 4 -> menuBar.setCollapseButtonSize(28 + random.nextInt(24));
                            case 5 -> menuBar.setCollapseButtonColor(new Color(random.nextInt(0xFFFFFF)));
                            case 6 -> {
                                if (random.nextInt(20) == 0) {
                                    menuBar.addMenu("dyn-" + threadId + "-" + i, "M" + i);
                                    addedMenus.incrementAndGet();
                                }
                            }
                        }
                        operations.incrementAndGet();
                        if (random.nextInt(10) == 0) {
                            Thread.sleep(random.nextInt(2));
                        }
                    }
                } catch (Throwable error) {
                    failures.add(error);
                } finally {
                    done.countDown();
                }
            }, "stress-worker-" + t);
            worker.setUncaughtExceptionHandler((th, ex) -> failures.add(ex));
            worker.start();
        }

        long startedAt = System.nanoTime();
        start.countDown();
        try {
            done.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

        try {
            SwingUtilities.invokeAndWait(() -> { });
        } catch (Exception ignored) {
        }

        int totalMutations = InstrumentedCollapsibleMenuBar.TOTAL_MUTATIONS.get();
        int offEdt = InstrumentedCollapsibleMenuBar.OFF_EDT_MUTATIONS.get();
        List<String> violations = InstrumentedCollapsibleMenuBar.VIOLATIONS;
        int expectedMenus = baselineMenus + addedMenus.get();
        boolean menuCountConsistent = menuBar.getMenus().size() == expectedMenus;
        boolean passed = offEdt == 0 && failures.isEmpty() && menuCountConsistent;

        StringBuilder sb = new StringBuilder();
        sb.append("=== CollapsibleMenuBar race condition stress test ===\n\n");
        sb.append("Worker threads ............... ").append(WORKER_THREADS).append('\n');
        sb.append("Iterations per thread ........ ").append(ITERATIONS_PER_THREAD).append('\n');
        sb.append("Total operations issued ...... ").append(operations.get()).append('\n');
        sb.append("Menus added concurrently ..... ").append(addedMenus.get()).append('\n');
        sb.append("Elapsed ...................... ").append(elapsedMs).append(" ms\n\n");
        sb.append("Swing mutations observed ..... ").append(totalMutations).append('\n');
        sb.append("Mutations OFF the EDT ........ ").append(offEdt)
                .append(offEdt == 0 ? "   <- OK" : "   <- RACE CONDITION!").append('\n');
        sb.append("Worker thread failures ....... ").append(failures.size()).append('\n');
        sb.append("Menu collection coherent ..... ")
                .append(menuCountConsistent ? "yes" : "NO")
                .append("  (expected ").append(expectedMenus)
                .append(", got ").append(menuBar.getMenus().size()).append(")\n");

        if (!violations.isEmpty()) {
            sb.append("\n--- off-EDT mutations (first ").append(violations.size()).append(") ---\n");
            for (String violation : violations) {
                sb.append("  ").append(violation).append('\n');
            }
        }
        if (!failures.isEmpty()) {
            sb.append("\n--- worker failures ---\n");
            for (Throwable error : failures) {
                sb.append("  ").append(error).append('\n');
            }
        }
        sb.append('\n');
        sb.append(passed
                ? "RESULT: PASS - every Swing mutation was marshalled onto the EDT.\n"
                : "RESULT: FAIL - the component touched Swing state off the EDT.\n");

        return new StressResult(passed, elapsedMs, sb.toString());
    }

    private static final class InstrumentedCollapsibleMenuBar extends CollapsibleMenuBar {

        static final AtomicInteger TOTAL_MUTATIONS = new AtomicInteger();
        static final AtomicInteger OFF_EDT_MUTATIONS = new AtomicInteger();
        static final List<String> VIOLATIONS = new CopyOnWriteArrayList<>();

        static void reset() {
            TOTAL_MUTATIONS.set(0);
            OFF_EDT_MUTATIONS.set(0);
            VIOLATIONS.clear();
        }

        static void recordMutation(String operation) {
            TOTAL_MUTATIONS.incrementAndGet();
            if (!SwingUtilities.isEventDispatchThread()) {
                OFF_EDT_MUTATIONS.incrementAndGet();
                if (VIOLATIONS.size() < 25) {
                    VIOLATIONS.add(operation + " on thread '" + Thread.currentThread().getName() + "'");
                }
            }
        }

        @Override
        protected Menu createMenu(String id, String text) {
            return new RaceCheckedMenu(this, id, text);
        }

        @Override
        protected JButton createCollapseButton() {
            return new RaceCheckedButton();
        }

        private static final class RaceCheckedMenu extends MenuBar.Menu {
            RaceCheckedMenu(MenuBar owner, String id, String text) {
                super(owner, id, text);
            }

            @Override
            public void setVisible(boolean visible) {
                recordMutation("Menu.setVisible");
                super.setVisible(visible);
            }
        }

        private static final class RaceCheckedButton extends JButton {
            @Override
            public void setIcon(Icon icon) {
                recordMutation("CollapseButton.setIcon");
                super.setIcon(icon);
            }

            @Override
            public void setText(String text) {
                recordMutation("CollapseButton.setText");
                super.setText(text);
            }

            @Override
            public void setPreferredSize(Dimension size) {
                recordMutation("CollapseButton.setPreferredSize");
                super.setPreferredSize(size);
            }

            @Override
            public void setMinimumSize(Dimension size) {
                recordMutation("CollapseButton.setMinimumSize");
                super.setMinimumSize(size);
            }
        }
    }
}
