package dtm.stools.component.inputfields;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.filters.ListenerDocumentFilter;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Um componente de texto que aplica uma máscara de formatação.
 * Esta implementação usa DocumentFilter para um controle mais preciso.
 *
 * <p>Caracteres especiais da máscara:
 * <ul>
 * <li><b>'#'</b> - Representa um dígito (número)</li>
 * <li><b>'U'</b> - Representa uma letra (converte para maiúscula)</li>
 * <li><b>'L'</b> - Representa uma letra (converte para minúscula)</li>
 * <li><b>'A'</b> - Representa uma letra ou dígito</li>
 * <li><b>'?'</b> - Representa uma letra (sem conversão)</li>
 * <li><b>'*'</b> - Representa qualquer caractere</li>
 * </ul>
 */
public class MaskedTextField extends JTextFieldListener {

    private final String mask;
    private final char placeholder;
    private String valueOnFocusGain;

    /**
     * -- GETTER --
     *  Verifica se o campo está em modo somente leitura.
     *
     * @return true se o campo é somente leitura, false caso contrário
     */
    @Getter
    private boolean readOnly = false;
    private Border originalBorder;
    private Border readOnlyBorder;
    private String placeholderText;
    private Color placeholderColor = Color.GRAY;

    /**
     * Cria um novo campo de texto sem máscara.
     * Funciona como um JTextField normal.
     */
    public MaskedTextField() {
        this(null, '_');
    }

    /**
     * Cria um novo campo de texto sem máscara e com colunas.
     * Funciona como um JTextField normal.
     */
    public MaskedTextField(int columns) {
        this(null, '_', columns);
    }

    /**
     * Cria um novo campo de texto com máscara.
     *
     * @param mask A string de máscara (ex: "###.###.###-##"). Se null ou vazio, funciona como JTextField normal.
     */
    public MaskedTextField(String mask) {
        this(mask, '_');
    }

    /**
     * Cria um novo campo de texto com máscara.
     *
     * @param mask A string de máscara (ex: "###.###.###-##"). Se null ou vazio, funciona como JTextField normal.
     */
    public MaskedTextField(String mask, int columns) {
        this(mask, '_', columns);
    }

    /**
     * Cria um novo campo de texto com máscara e placeholder customizado.
     *
     * @param mask        A string de máscara (ex: "###.###.###-##"). Se null ou vazio, funciona como JTextField normal.
     * @param placeholder O caractere que representa posições vazias.
     */
    public MaskedTextField(String mask, char placeholder) {
        super();
        this.mask = mask;
        this.placeholder = placeholder;

        if (mask != null && !mask.isEmpty()) {
            ((AbstractDocument) getDocument()).setDocumentFilter(new MaskDocumentFilter());
            setText(createEmptyMask());
        }else{
            ((AbstractDocument) getDocument()).setDocumentFilter(new ListenerDocumentFilter(() -> {
                dispachEvent(EventType.INPUT, this::getCleanText);
            }));
        }

        this.valueOnFocusGain = getCleanText();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                valueOnFocusGain = getCleanText();
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!getCleanText().equals(valueOnFocusGain)) {
                    dispachEvent(EventType.CHANGE, MaskedTextField.this::getCleanText);
                    valueOnFocusGain = getCleanText();
                }
                repaint();
            }
        });
    }

    /**
     * Cria um novo campo de texto com máscara e placeholder customizado.
     *
     * @param mask        A string de máscara (ex: "###.###.###-##"). Se null ou vazio, funciona como JTextField normal.
     * @param placeholder O caractere que representa posições vazias.
     * @param columns O numero de colunas
     */
    public MaskedTextField(String mask, char placeholder, int columns) {
        super(columns);
        this.mask = mask;
        this.placeholder = placeholder;

        if (mask != null && !mask.isEmpty()) {
            ((AbstractDocument) getDocument()).setDocumentFilter(new MaskDocumentFilter());
            setText(createEmptyMask());
        }else{
            ((AbstractDocument) getDocument()).setDocumentFilter(new ListenerDocumentFilter(() -> {
                dispachEvent(EventType.INPUT, this::getCleanText);
            }));
        }

        this.valueOnFocusGain = getCleanText();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                valueOnFocusGain = getCleanText();
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!getCleanText().equals(valueOnFocusGain)) {
                    dispachEvent(EventType.CHANGE, MaskedTextField.this::getCleanText);
                    valueOnFocusGain = getCleanText();
                }
                repaint();
            }
        });
    }


    /**
     * Define o texto do placeholder que é exibido quando o campo está vazio e sem foco.
     * Esta função só funciona se o campo NÃO tiver uma máscara.
     * Nenhum evento (INPUT, CHANGE) é disparado.
     *
     * @param placeholderText O texto a ser exibido.
     */
    public void setPlaceholder(String placeholderText) {
        this.placeholderText = placeholderText;
        repaint();
    }

    /**
     * Define a cor do texto do placeholder.
     *
     * @param placeholderColor A cor a ser usada.
     */
    public void setPlaceholderColor(Color placeholderColor) {
        this.placeholderColor = placeholderColor;
        repaint();
    }

    /**
     * Define se o campo é somente leitura.
     * Quando readOnly = true, o campo fica com borda pontilhada e não permite edição.
     *
     * @param readOnly true para tornar o campo somente leitura, false para permitir edição
     */
    public void setReadonly(boolean readOnly) {
        this.readOnly = readOnly;

        if (originalBorder == null) {
            originalBorder = getBorder();
        }

        if (readOnly) {
            setEditable(false);

            Color borderColor = UIManager.getColor("TextField.border");
            if (borderColor == null) {
                borderColor = UIManager.getColor("controlShadow");
            }
            if (borderColor == null) {
                borderColor = Color.GRAY;
            }

            readOnlyBorder = BorderFactory.createDashedBorder(
                    borderColor,
                    1.0f,
                    3.0f,
                    3.0f,
                    false
            );

            setBorder(readOnlyBorder);

            Color disabledBg = UIManager.getColor("TextField.disabledBackground");
            if (disabledBg == null) {
                disabledBg = UIManager.getColor("control");
            }
            if (disabledBg != null) {
                setBackground(disabledBg);
            }
        } else {
            setEditable(true);
            setBorder(originalBorder);

            Color normalBg = UIManager.getColor("TextField.background");
            if (normalBg != null) {
                setBackground(normalBg);
            }
        }

        repaint();
    }

    public boolean isReadonly(){
        return readOnly;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (mask == null && placeholderText != null && !placeholderText.isEmpty() && getText().isEmpty() && !hasFocus()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(placeholderColor);

            g2.setFont(getFont().deriveFont(Font.ITALIC));

            Insets insets = getInsets();
            FontMetrics fm = g2.getFontMetrics();
            int x = insets.left + 5;

            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.drawString(placeholderText, x, y);
            g2.dispose();
        }
    }

    @Override
    public void addEventListner(String eventType, Consumer<EventComponent> event) {
        if(eventType == null || eventType.isEmpty()) return;


        if (eventType.equals(EventType.CHANGE) || eventType.equals(EventType.INPUT)) {
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        }
    }

    /**
     * Obtém o texto "limpo" do campo, sem os literais da máscara e placeholders.
     * Se não houver máscara, retorna o texto completo do campo.
     *
     * @return Uma string contendo apenas o valor digitado pelo usuário.
     */
    public String getCleanText() {
        if (mask == null || mask.isEmpty()) {
            return getText();
        }

        String text = getText();
        StringBuilder clean = new StringBuilder();

        for (int i = 0; i < text.length() && i < mask.length(); i++) {
            char c = text.charAt(i);
            if (isMaskChar(mask.charAt(i)) && c != placeholder) {
                clean.append(c);
            }
        }

        return clean.toString();
    }

    /**
     * Define o texto limpo (sem máscara) no campo.
     * Se não houver máscara, define o texto diretamente.
     *
     * @param cleanText O texto sem formatação.
     */
    public void setCleanText(String cleanText) {
        String oldValue = getCleanText();

        if (mask == null || mask.isEmpty()) {
            setText(cleanText);
        } else {
            StringBuilder formatted = new StringBuilder(createEmptyMask());
            int cleanIndex = 0;

            for (int i = 0; i < mask.length() && cleanIndex < cleanText.length(); i++) {
                if (isMaskChar(mask.charAt(i))) {
                    char inputChar = cleanText.charAt(cleanIndex);
                    if (isValidChar(mask.charAt(i), inputChar)) {
                        formatted.setCharAt(i, applyConversion(mask.charAt(i), inputChar));
                        cleanIndex++;
                    }
                }
            }
            setText(formatted.toString());
        }

        String newValue = getCleanText();
        valueOnFocusGain = newValue;

        if (!oldValue.equals(newValue)) {
            dispachEvent(EventType.CHANGE, MaskedTextField.this::getCleanText);
        }
    }


    private String createEmptyMask() {
        StringBuilder sb = new StringBuilder();
        for (char c : mask.toCharArray()) {
            if (isMaskChar(c)) {
                sb.append(placeholder);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private boolean isMaskChar(char c) {
        return c == '#' || c == 'U' || c == 'L' || c == 'A' || c == '?' || c == '*';
    }

    private boolean isValidChar(char maskChar, char inputChar) {
        switch (maskChar) {
            case '#':
                return Character.isDigit(inputChar);
            case 'U':
            case 'L':
            case '?':
                return Character.isLetter(inputChar);
            case 'A':
                return Character.isLetterOrDigit(inputChar);
            case '*':
                return true;
            default:
                return false;
        }
    }

    private char applyConversion(char maskChar, char inputChar) {
        switch (maskChar) {
            case 'U':
                return Character.toUpperCase(inputChar);
            case 'L':
                return Character.toLowerCase(inputChar);
            default:
                return inputChar;
        }
    }

    private int getNextEditablePosition(int position) {
        while (position < mask.length() && !isMaskChar(mask.charAt(position))) {
            position++;
        }
        return position;
    }

    private class MaskDocumentFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {

            if (text == null || text.isEmpty()) {
                return;
            }

            int docLength = fb.getDocument().getLength();
            String currentText;

            if (docLength == 0) {
                currentText = createEmptyMask();
            } else {
                currentText = fb.getDocument().getText(0, docLength);
            }

            if (currentText.length() < mask.length()) {
                currentText = createEmptyMask();
            }

            StringBuilder newText = new StringBuilder(currentText);
            int position = getNextEditablePosition(offset);

            for (char c : text.toCharArray()) {
                if (position >= mask.length()) {
                    break;
                }

                char maskChar = mask.charAt(position);

                if (isValidChar(maskChar, c)) {
                    newText.setCharAt(position, applyConversion(maskChar, c));
                    position = getNextEditablePosition(position + 1);
                }
            }

            fb.remove(0, fb.getDocument().getLength());
            fb.insertString(0, newText.toString(), attrs);

            dispachEvent(EventType.INPUT, MaskedTextField.this::getCleanText);

            final int finalPosition = Math.min(position, mask.length());
            SwingUtilities.invokeLater(() -> MaskedTextField.this.setCaretPosition(finalPosition));
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            int docLength = fb.getDocument().getLength();

            if (docLength == 0) {
                return;
            }

            String currentText = fb.getDocument().getText(0, docLength);
            StringBuilder newText = new StringBuilder(currentText);

            for (int i = offset; i < offset + length && i < mask.length() && i < newText.length(); i++) {
                if (isMaskChar(mask.charAt(i))) {
                    newText.setCharAt(i, placeholder);
                }
            }

            fb.remove(0, fb.getDocument().getLength());
            fb.insertString(0, newText.toString(), null);

            dispachEvent(EventType.INPUT, MaskedTextField.this::getCleanText);

            final int finalOffset = offset;
            SwingUtilities.invokeLater(() -> MaskedTextField.this.setCaretPosition(finalOffset));
        }
    }
}