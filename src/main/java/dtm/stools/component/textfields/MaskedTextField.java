package dtm.stools.component.textfields;

import dtm.stools.component.textfields.events.EventComponent;
import dtm.stools.component.textfields.events.EventListenerComponent;
import dtm.stools.component.textfields.events.EventType;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 *
 * <p><b>Exemplos de Máscara:</b>
 * <ul>
 * <li><b>CPF:</b> "###.###.###-##"</li>
 * <li><b>CNPJ:</b> "##.###.###/####-##"</li>
 * <li><b>Data:</b> "##/##/####"</li>
 * <li><b>Telefone:</b> "(##) #####-####"</li>
 * <li><b>Placa:</b> "UUU-####"</li>
 * </ul>
 */
public class MaskedTextField extends JTextField implements EventListenerComponent {

    private final String mask;
    private final char placeholder;
    private final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();
    private String valueOnFocusGain;

    /**
     * Cria um novo campo de texto sem máscara.
     * Funciona como um JTextField normal.
     */
    public MaskedTextField() {
        this(null, '_');
    }

    /**
     * Cria um novo campo de texto com máscara.
     *
     * @param mask A string de máscara (ex: "###.###.###-##"). Se null ou vazio, funciona como JTextField normal.
     */
    public MaskedTextField(String mask) {
        this(mask, '_');
    }

    @Override
    public void addEventListner(String eventType, Consumer<EventComponent> event) {
        if(eventType == null || eventType.isEmpty()) return;


        if (eventType.equals(EventType.CHANGE) || eventType.equals(EventType.INPUT)) {
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        }
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
        }

        this.valueOnFocusGain = getCleanText();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                valueOnFocusGain = getCleanText();
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!getCleanText().equals(valueOnFocusGain)) {
                    dispachEvent(EventType.CHANGE);
                    valueOnFocusGain = getCleanText();
                }
            }
        });
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
            dispachEvent(EventType.CHANGE);
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

    private void dispachEvent(String eventType){
        if (listeners != null && !listeners.isEmpty()) {
            List<Consumer<EventComponent>> listeners = this.listeners.get(eventType);
            if(listeners != null && !listeners.isEmpty()){
                EventComponent event = new EventComponent() {
                    @Override
                    public Component getComponent() {
                        return MaskedTextField.this;
                    }

                    @Override
                    public Object getValue() {
                        return getCleanText();
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> T tryGetValue() {
                        try {
                            return (T) getCleanText();
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public String getEventType() {
                        return eventType;
                    }
                };
                listeners.forEach(listener -> {
                    try{
                        listener.accept(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
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

            dispachEvent(EventType.INPUT);

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

            dispachEvent(EventType.INPUT);

            final int finalOffset = offset;
            SwingUtilities.invokeLater(() -> MaskedTextField.this.setCaretPosition(finalOffset));
        }
    }
}