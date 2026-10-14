package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textarea.SmartTextArea;
import dtm.stools.component.inputfields.textarea.TextAreaResize;
import dtm.stools.component.inputfields.textarea.diagnostic.DictionarySpellChecker;
import dtm.stools.configs.UiTokens;
import dtm.stools.models.SearchPanelMode;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class SmartTextAreaExample {

    private static final String SAMPLE = """
            SmartTextArea herda de JTextArea e adiciona:
              - placeholder, limite de caracteres e eventos
              - desfazer/refazer agrupado por palavra (Ctrl+Z / Ctrl+Y)
              - menu de contexto (botao direito)
              - numeracao de linhas e destaque da linha atual
              - busca e substituicao (Ctrl+F / Ctrl+H, F3 / Shift+F3)

            Atalhos de edicao:
              Ctrl+D          duplica a linha
              Alt+Cima/Baixo  move as linhas
              Ctrl+Shift+K    exclui as linhas
              Tab/Shift+Tab   indenta/desindenta a selecao
              Enter           mantem a indentacao da linha anterior
              F8 / Shift+F8   proximo / anterior diagnostico
            """;

    private static final List<String> WORDS = List.of(
            "o", "a", "e", "de", "do", "da", "que", "com", "para", "um", "uma", "no", "na", "em", "se", "por",
            "texto", "campo", "palavra", "palavras", "erro", "erros", "aqui", "escreva", "digite", "observacao",
            "curta", "teste", "isso", "corretor", "ortografico", "sublinha", "desconhecidas", "clique", "direito",
            "sugestoes", "nao", "esta", "dicionario", "arraste", "canto", "redimensionar", "botao", "ver");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SmartTextAreaExample::createAndShow);
    }

    private static void createAndShow() {
        FlatLightLaf.setup();

        JFrame frame = new JFrame("SmartTextArea");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(860, 620);
        frame.setLocationRelativeTo(null);

        SmartTextArea area = new SmartTextArea(SAMPLE);
        area.setFont(UiTokens.fontMono());
        area.setTabSize(4);
        area.setPlaceholder("Escreva alguma coisa...");

        SmartTextArea notes = new SmartTextArea(3, 20);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setLineNumbersVisible(false)
                .setHighlightCurrentLine(false)
                .setMaxLength(140)
                .setPlaceholder("Observacao curta (max. 140 caracteres)")
                .addDiagnosticProvider(new DictionarySpellChecker(WORDS));
        notes.setText("O corretor sublinha palavras desconhecidas: tetso, palavars.\nClique com o botao direito para ver sugestoes.");

        JLabel status = new JLabel(" ");
        notes.addEventListener(EventType.INPUT, e -> status.setText(notes.getDocument().getLength() + "/140"));
        notes.addEventListener(SmartTextArea.LIMIT_REACHED, e -> status.setText("Limite atingido"));

        JToolBar options = new JToolBar();
        options.setFloatable(false);
        options.add(toggle("Numeros de linha", area.isLineNumbersVisible(), area::setLineNumbersVisible));
        options.add(toggle("Linha atual", area.isHighlightCurrentLine(), area::setHighlightCurrentLine));
        options.add(toggle("Quebra de linha", area.getLineWrap(), area::setLineWrap));
        options.add(toggle("Editavel", area.isEditable(), area::setEditable));
        options.add(toggle("Espacos no Tab", area.isUseSpacesForTab(), area::setUseSpacesForTab));
        options.addSeparator();
        JComboBox<TextAreaResize> resize = new JComboBox<>(TextAreaResize.values());
        resize.setSelectedItem(notes.getResize());
        resize.setMaximumSize(resize.getPreferredSize());
        resize.setToolTipText("Resize da area de observacao");
        resize.addActionListener(e -> notes.setResize((TextAreaResize) resize.getSelectedItem()));
        options.add(resize);
        JComboBox<SearchPanelMode> searchMode = new JComboBox<>(SearchPanelMode.values());
        searchMode.setSelectedItem(area.getSearchMode());
        searchMode.setMaximumSize(searchMode.getPreferredSize());
        searchMode.setToolTipText("Modo da busca (Ctrl+F)");
        searchMode.addActionListener(e -> {
            area.setSearchMode((SearchPanelMode) searchMode.getSelectedItem());
            notes.setSearchMode((SearchPanelMode) searchMode.getSelectedItem());
        });
        options.add(searchMode);
        options.addSeparator();
        JButton find = new JButton("Buscar");
        find.addActionListener(e -> area.openSearch());
        options.add(find);
        options.add(toggle("Tema escuro", false, dark -> {
            if (dark) FlatDarkLaf.setup();
            else FlatLightLaf.setup();
            UiTokens.refresh();
            SwingUtilities.updateComponentTreeUI(frame);
        }));

        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JScrollPane notesPane = notes.createScrollPane();
        notesPane.setPreferredSize(new Dimension(520, 90));
        JPanel notesHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        notesHolder.add(notesPane);
        bottom.add(notesHolder, BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(area.createScrollPane(), BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        frame.add(options, BorderLayout.NORTH);
        frame.add(content, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private static JCheckBox toggle(String text, boolean selected, Consumer<Boolean> action) {
        JCheckBox box = new JCheckBox(text, selected);
        box.addActionListener(e -> action.accept(box.isSelected()));
        return box;
    }
}
