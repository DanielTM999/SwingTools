package dtm.stools.examples;

import dtm.stools.component.events.EventType;
import dtm.stools.component.icon.TintedIconLoader;
import dtm.stools.component.inputfields.textfield.JTextFieldListener;
import dtm.stools.component.inputfields.textfield.MaskedTextField;
import dtm.stools.component.inputfields.textfield.SearchTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

public class TextFieldIconAndClearButtonExample {

    private static final Color ACCENT = new Color(0x2563EB);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TextFieldIconAndClearButtonExample::createAndShow);
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("JTextFieldListener - icone e botao de limpar");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(640, 620);
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        JTextFieldListener basic = new JTextFieldListener(20);
        basic.setIcon(new SearchIcon(15, new Color(0x6B7280)));
        JLabel basicStatus = new JLabel("digite algo para o X aparecer");
        basic.addEventListener(EventType.CLEAR, e -> basicStatus.setText("CLEAR disparado"));
        row = addRow(content, gbc, row, "Icone + X (padrao):", basic, basicStatus);

        JTextFieldListener tinted = new JTextFieldListener(20);
        tinted.setIcon(TintedIconLoader.load("/drawables/arrowRightNoTail.png", 14, null));
        tinted.setIconColor(ACCENT);
        tinted.setClearIconColor(ACCENT);
        tinted.setIconGap(10);
        tinted.setText("icone e X tingidos");
        row = addRow(content, gbc, row, "setIconColor + gap 10:", tinted,
                new JLabel("icone vem de /drawables e e recolorido em runtime"));

        JTextFieldListener noClear = new JTextFieldListener(20);
        noClear.setIcon(new SearchIcon(15, new Color(0x6B7280)));
        noClear.setClearButtonEnabled(false);
        noClear.setText("sem botao de limpar");
        row = addRow(content, gbc, row, "setClearButtonEnabled(false):", noClear,
                new JLabel("margem direita volta ao normal"));

        JTextFieldListener customClear = new JTextFieldListener(20);
        customClear.setIcon(new SearchIcon(15, new Color(0x6B7280)));
        customClear.setClearIcon(TintedIconLoader.load("/drawables/close.png", 12, new Color(0xDC2626)));
        customClear.setText("X customizado");
        row = addRow(content, gbc, row, "setClearIcon(Icon):", customClear,
                new JLabel("qualquer Icon no lugar do glifo interno"));

        JTextFieldListener readOnly = new JTextFieldListener(20);
        readOnly.setIcon(new SearchIcon(15, new Color(0x6B7280)));
        readOnly.setText("campo nao editavel");
        readOnly.setEditable(false);
        row = addRow(content, gbc, row, "setEditable(false):", readOnly,
                new JLabel("X some automaticamente"));

        MaskedTextField masked = new MaskedTextField("###.###.###-##", 20);
        masked.setPlaceholder("CPF");
        masked.setIcon(new SearchIcon(15, new Color(0x6B7280)));
        JLabel maskedStatus = new JLabel("placeholder respeita o espaco do icone");
        masked.addEventListener(EventType.CLEAR, e -> maskedStatus.setText("CLEAR disparado"));
        row = addRow(content, gbc, row, "MaskedTextField:", masked, maskedStatus);

        SearchTextField<String> search = new SearchTextField<>();
        search.setColumns(20);
        search.setIcon(new SearchIcon(15, ACCENT));
        search.setDataSource(List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo"));
        search.addSearchOption(value -> value);
        search.setMinLength(1);
        JLabel searchStatus = new JLabel("digite \"a\" para ver as sugestoes");
        search.addEventListener(EventType.SELECT, e -> searchStatus.setText("selecionado: " + e.getValue()));
        search.addEventListener(EventType.CLEAR, e -> searchStatus.setText("CLEAR disparado"));
        row = addRow(content, gbc, row, "SearchTextField:", search, searchStatus);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        frame.setContentPane(scroll);
        frame.setVisible(true);
    }

    private static int addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field, JComponent status) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);

        gbc.gridx = 0;
        gbc.gridy = row + 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        status.setForeground(ACCENT);
        panel.add(status, gbc);
        gbc.gridwidth = 1;

        return row + 2;
    }

    private static final class SearchIcon implements Icon {

        private final int size;
        private final Color color;

        private SearchIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double diameter = size * 0.62;
                g2.draw(new Ellipse2D.Double(x + 1, y + 1, diameter, diameter));
                int tail = (int) Math.round(diameter * 0.75);
                g2.drawLine(x + tail, y + tail, x + size - 1, y + size - 1);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
