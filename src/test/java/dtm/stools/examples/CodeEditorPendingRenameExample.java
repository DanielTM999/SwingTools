package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.CodeEditor;
import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteProvider;
import dtm.stools.component.panels.editor.code.autocomplete.CompletionContext;
import dtm.stools.component.panels.editor.code.provider.RenameContext;
import dtm.stools.component.panels.editor.code.provider.RenameProvider;
import dtm.stools.component.panels.editor.code.rename.RenamePrepareContext;
import dtm.stools.component.panels.editor.code.rename.RenamePreparation;
import dtm.stools.component.panels.editor.code.utils.LoadingIndicator;
import dtm.stools.component.panels.editor.code.utils.LoadingSpinnerContext;
import dtm.stools.component.panels.editor.code.utils.LoadingSpinnerFactory;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEditorPendingRenameExample {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CodeEditorPendingRenameExample::launch);
    }

    private static void launch() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("CodeEditor pending rename example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1040, 720);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        CodeEditor editor = new CodeEditor("""
                public class Colaborador {

                    // Coloque o cursor em idFuncionario e aperte Shift+F6.
                    // Digite o nome novo e Enter: o rename "demora" o tempo configurado acima.
                    // Durante a espera: contorno pulsando nas ocorrencias + aviso com o indicador.
                    // Esc cancela. Com o bloqueio ligado, digitar nao faz nada;
                    // desligado, a primeira edicao descarta o rename.
                    // "Salvar" simula o Ctrl+S: espera o rename terminar e so entao grava.
                    // Ctrl+Espaco abre o autocomplete lento, que usa a mesma factory de indicador.

                    private Long idFuncionario;

                    public Long getIdFuncionario() {
                        return idFuncionario;
                    }

                    public void setIdFuncionario(Long idFuncionario) {
                        this.idFuncionario = idFuncionario;
                    }

                    public boolean mesmoId(Colaborador outro) {
                        return idFuncionario != null && idFuncionario.equals(outro.idFuncionario);
                    }
                }
                """);
        editor.getTextArea().setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));

        JTextArea log = new JTextArea(9, 80);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        SpinnerNumberModel delayModel = new SpinnerNumberModel(3, 0, 15, 1);
        editor.setRenameProvider(new SlowRenameProvider(delayModel, message -> log(log, message)));
        editor.addProvider(new SlowAutoCompleteProvider());

        JCheckBox block = new JCheckBox("Bloquear edição durante o rename", editor.isBlockEditsWhileRenamePending());
        block.addActionListener(e -> {
            editor.setBlockEditsWhileRenamePending(block.isSelected());
            log(log, "bloqueio de edição: " + (block.isSelected() ? "ligado" : "desligado"));
        });

        JComboBox<String> indicator = new JComboBox<>(new String[]{
                "Padrão (LoadingSpinner)", "Pontos pulsando", "Barra indeterminada"});
        indicator.addActionListener(e -> {
            editor.setLoadingSpinnerFactory(switch (indicator.getSelectedIndex()) {
                case 1 -> DotsIndicator::new;
                case 2 -> BarIndicator::new;
                default -> LoadingSpinnerFactory.defaults();
            });
            log(log, "indicador: " + indicator.getSelectedItem());
        });

        JButton rename = new JButton("Renomear (Shift+F6)");
        rename.addActionListener(e -> {
            editor.getTextArea().requestFocusInWindow();
            editor.triggerRename();
        });

        JButton save = new JButton("Salvar (simula Ctrl+S)");
        save.addActionListener(e -> {
            if (editor.isRenamePending()) {
                log(log, "salvar pedido com rename pendente: aguardando...");
            }
            editor.whenRenameSettled(() -> log(log, "salvo -> " + declarationLine(editor.getText())));
            editor.getTextArea().requestFocusInWindow();
        });

        JLabel pending = new JLabel();
        Timer pendingWatcher = new Timer(100, e -> pending.setText(editor.isRenamePending()
                ? "● rename pendente" : "○ sem rename pendente"));
        pendingWatcher.start();

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        toolbar.add(new JLabel("Atraso do rename (s):"));
        toolbar.add(new JSpinner(delayModel));
        toolbar.add(block);
        toolbar.add(new JLabel("Indicador:"));
        toolbar.add(indicator);
        toolbar.add(rename);
        toolbar.add(save);
        toolbar.add(pending);

        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setBorder(BorderFactory.createTitledBorder("Eventos"));

        frame.add(toolbar, BorderLayout.NORTH);
        frame.add(editor, BorderLayout.CENTER);
        frame.add(logScroll, BorderLayout.SOUTH);
        frame.setVisible(true);
        log(log, "pronto: Shift+F6 em idFuncionario para começar");
    }

    private static String declarationLine(String text) {
        for (String line : text.split("\n")) {
            if (line.contains("private Long")) {
                return line.strip();
            }
        }
        return "(declaração não encontrada)";
    }

    private static void log(JTextArea log, String message) {
        Runnable append = () -> {
            log.append(LocalTime.now().format(CLOCK) + "  " + message + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        };
        if (SwingUtilities.isEventDispatchThread()) {
            append.run();
        } else {
            SwingUtilities.invokeLater(append);
        }
    }

    private interface Logger {
        void log(String message);
    }

    private static final class SlowRenameProvider implements RenameProvider {

        private final SpinnerNumberModel delay;
        private final Logger logger;

        private SlowRenameProvider(SpinnerNumberModel delay, Logger logger) {
            this.delay = delay;
            this.logger = logger;
        }

        @Override
        public RenamePreparation prepareRename(RenamePrepareContext context) {
            String word = wordAt(context.buffer(), context.offset());
            if (word.isEmpty()) {
                return RenamePreparation.rejected("Coloque o cursor sobre um identificador");
            }
            List<Range> occurrences = occurrences(context.buffer(), word);
            Range primary = occurrences.stream()
                    .filter(range -> contains(context.buffer(), range, context.offset()))
                    .findFirst()
                    .orElse(occurrences.getFirst());
            return RenamePreparation.of(primary, word).withOccurrences(occurrences);
        }

        @Override
        public List<TextEdit> computeRenameEdits(RenameContext context) {
            String word = wordAt(context.buffer(), context.offset());
            int seconds = ((Number) delay.getValue()).intValue();
            logger.log("rename '" + word + "' -> '" + context.newName() + "' calculando por " + seconds + "s...");
            try {
                TimeUnit.SECONDS.sleep(seconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return List.of();
            }
            List<TextEdit> edits = new ArrayList<>();
            for (Range range : occurrences(context.buffer(), word)) {
                edits.add(TextEdit.replace(range, context.newName()));
            }
            logger.log("rename calculado: " + edits.size() + " ocorrência(s)");
            return edits;
        }

        @Override
        public void onRenameApplied(RenameContext context, List<TextEdit> appliedEdits) {
            logger.log("rename aplicado no editor (" + appliedEdits.size() + " edição(ões))");
        }
    }

    private static final class SlowAutoCompleteProvider implements AutoCompleteProvider {

        @Override
        public List<AutoCompleteItem> getSuggestions(CompletionContext context) {
            return List.of(
                    new AutoCompleteItem("idFuncionario", "idFuncionario", "Long"),
                    new AutoCompleteItem("getIdFuncionario()", "getIdFuncionario()", "Long"),
                    new AutoCompleteItem("setIdFuncionario(", "setIdFuncionario(Long)", "void"));
        }

        @Override
        public CompletableFuture<List<AutoCompleteItem>> getSuggestionsAsync(CompletionContext context, Executor executor) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return getSuggestions(context);
            }, executor);
        }
    }

    private static final class DotsIndicator extends JComponent implements LoadingIndicator {

        private final AtomicInteger frame = new AtomicInteger();
        private final Timer timer = new Timer(120, e -> {
            frame.incrementAndGet();
            repaint();
        });
        private final Color color;

        private DotsIndicator(LoadingSpinnerContext context) {
            this.color = context.color() != null ? context.color() : new Color(0x3B82F6);
            int size = context.size();
            setPreferredSize(new Dimension(size * 2, size));
            setOpaque(false);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void start() {
            timer.start();
        }

        @Override
        public void stop() {
            timer.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int diameter = Math.max(4, getHeight() / 3);
                int gap = (getWidth() - diameter * 3) / 4;
                int active = frame.get() % 3;
                for (int i = 0; i < 3; i++) {
                    int alpha = i == active ? 255 : 80;
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                    g2.fillOval(gap + i * (diameter + gap), (getHeight() - diameter) / 2, diameter, diameter);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private static final class BarIndicator implements LoadingIndicator {

        private final JProgressBar bar = new JProgressBar();

        private BarIndicator(LoadingSpinnerContext context) {
            bar.setPreferredSize(new Dimension(context.size() * 4, Math.max(6, context.size() / 2)));
            if (context.color() != null) {
                bar.setForeground(context.color());
            }
        }

        @Override
        public JComponent getComponent() {
            return bar;
        }

        @Override
        public void start() {
            bar.setIndeterminate(true);
        }

        @Override
        public void stop() {
            bar.setIndeterminate(false);
        }
    }

    private static String wordAt(String text, int offset) {
        int start = Math.max(0, Math.min(offset, text.length()));
        int end = start;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        while (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) {
            end++;
        }
        return text.substring(start, end);
    }

    private static List<Range> occurrences(String text, String word) {
        List<Range> ranges = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text);
        while (matcher.find()) {
            ranges.add(new Range(positionOf(text, matcher.start()), positionOf(text, matcher.end())));
        }
        return ranges;
    }

    private static boolean contains(String text, Range range, int offset) {
        return offsetOf(text, range.start()) <= offset && offset <= offsetOf(text, range.end());
    }

    private static Position positionOf(String text, int offset) {
        int line = 0;
        int lineStart = 0;
        for (int i = 0; i < offset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                lineStart = i + 1;
            }
        }
        return new Position(line, offset - lineStart);
    }

    private static int offsetOf(String text, Position position) {
        int line = 0;
        int index = 0;
        while (line < position.line()) {
            index = text.indexOf('\n', index) + 1;
            line++;
        }
        return index + position.col();
    }
}
