package dtm.stools.component.inputfields.textfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.filters.ListenerDocumentFilter;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Um componente de texto que aplica uma máscara de formatação.
 * Esta implementação usa DocumentFilter para um controle mais preciso.
 *
 * <p>Caracteres especiais da máscara:
 * <ul>
 * <li><b>'#'</b> - Representa um dígito (número)</li>
 * <li><b>'U'</b> - Representa uma letra (converte para maiúscula)</li>
 * <li><b>'L'</b> - Representa uma letra (converte para minúscula)</li>
 * <li><b>'$'</b> - Representa uma letra ou dígito (converte para maiúscula)</li>
 * <li><b>'@'</b> - Representa uma letra ou dígito (converte para minúscula)</li>
 * <li><b>'&'</b> - Representa uma letra ou dígito</li>
 * <li><b>'?'</b> - Representa uma letra (sem conversão)</li>
 * <li><b>'*'</b> - Representa qualquer caractere</li>
 * </ul>
 *
 * <p>Alternativas de máscara: use <b>'|'</b> para separar várias máscaras. O campo
 * aplica a primeira alternativa (na ordem) compatível com o que foi digitado.
 * Ex: {@code "#:#|####"} aceita o formato {@code #:#} ou até 4 dígitos.
 *
 * <p>Obrigatoriedade por caractere dentro de uma alternativa:
 * <ul>
 * <li>{@code X} ou {@code {X}} - caractere obrigatório (precisa ser preenchido)</li>
 * <li>{@code [X]} - caractere opcional (pode ficar vazio)</li>
 * <li>{@code X+} - um ou mais (repete o caractere; use como último elemento da alternativa)</li>
 * </ul>
 * Ex: {@code "#[#][#][#]"} exige 1 dígito e aceita até mais 3 opcionais.
 * Ex: {@code "#+"} aceita um ou mais dígitos sem limite.
 *
 * <p>Quando um literal aparece logo após um quantificador {@code +}, ele vira um
 * <b>separador digitável</b>: o usuário precisa digitá-lo para encerrar o grupo
 * de repetição. Ex: em {@code "#+:#+"} o usuário digita os primeiros dígitos,
 * digita {@code ':'} e então continua no segundo grupo.
 *
 * <p>Use {@code '\'} para escapar e tratar como literal os caracteres
 * especiais {@code [ ] { } | + \}. Ex: {@code "\\[##\\]"} produz o literal
 * {@code [##]} e {@code "#\\+#"} produz o literal {@code +} entre dois dígitos.
 */
public class MaskedTextField extends JTextFieldListener {

    private final String mask;
    private final char placeholder;
    private String valueOnFocusGain;

    private List<Alternative> alternatives;
    private String maskHint = "";
    private String typedBuffer = "";
    private String cleanValue = "";
    private String lastRendered = "";
    private int[] lastCleanPositions = new int[0];

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
    @Setter
    private boolean fireChangeOnSetText = false;
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
        setup();
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
        setup();
    }

    private void setup() {
        if (mask != null && !mask.isEmpty()) {
            this.alternatives = parseMask(mask);
            this.maskHint = buildMaskHint();
            ((AbstractDocument) getDocument()).setDocumentFilter(new MaskDocumentFilter());
            setText("");
        } else {
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
        addActionListener(e -> dispachEvent(EventType.SUBMIT, this::getCleanText));
    }

    /**
     * Define o texto do placeholder que é exibido quando o campo está vazio e sem foco.
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
    public void setText(String t) {
        String oldValue = getCleanText();

        super.setText(t);

        if (fireChangeOnSetText) {
            String newValue = getCleanText();
            valueOnFocusGain = newValue;

            if (!oldValue.equals(newValue)) {
                dispachEvent(EventType.CHANGE, this::getCleanText);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getText().isEmpty() && !hasFocus()) {
            String hint = (placeholderText != null && !placeholderText.isEmpty())
                    ? placeholderText
                    : maskHint;

            if (hint != null && !hint.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(placeholderColor);

                g2.setFont(getFont().deriveFont(Font.ITALIC));

                Insets insets = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int x = insets.left + 5;

                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                g2.drawString(hint, x, y);
                g2.dispose();
            }
        }
    }

    /**
     * Obtém o texto "limpo" do campo, sem os literais da máscara.
     * Se não houver máscara, retorna o texto completo do campo.
     *
     * @return Uma string contendo apenas o valor digitado pelo usuário.
     */
    public String getCleanText() {
        if (mask == null || mask.isEmpty()) {
            return getText();
        }
        return cleanValue;
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
            try {
                Document doc = getDocument();
                doc.remove(0, doc.getLength());
                doc.insertString(0, cleanText == null ? "" : cleanText, null);
            } catch (BadLocationException ignored) {
            }
        }

        String newValue = getCleanText();
        valueOnFocusGain = newValue;

        if (!oldValue.equals(newValue)) {
            dispachEvent(EventType.CHANGE, MaskedTextField.this::getCleanText);
        }
    }

    /**
     * Verifica se o valor atual satisfaz todos os caracteres obrigatórios
     * da alternativa de máscara aplicada.
     *
     * @return true se o campo está completo (ou não há máscara), false caso contrário.
     */
    public boolean isComplete() {
        if (mask == null || mask.isEmpty()) {
            return true;
        }
        for (Alternative a : alternatives) {
            MatchResult r = match(typedBuffer, a);
            if (r.matched && r.complete) {
                return true;
            }
        }
        return false;
    }

    private boolean isMaskChar(char c) {
        return c == '#' || c == 'U' || c == 'L' || c == '&' || c == '?' || c == '*' || c == '$' || c == '@';
    }

    private boolean isValidChar(char maskChar, char inputChar) {
        return switch (maskChar) {
            case '#' -> Character.isDigit(inputChar);
            case 'U', 'L', '?' -> Character.isLetter(inputChar);
            case '&', '$', '@' -> Character.isLetterOrDigit(inputChar);
            case '*' -> true;
            default -> false;
        };
    }

    private char applyConversion(char maskChar, char inputChar) {
        return switch (maskChar) {
            case 'U', '$' -> Character.toUpperCase(inputChar);
            case 'L', '@' -> Character.toLowerCase(inputChar);
            default -> inputChar;
        };
    }

    private List<Alternative> parseMask(String mask) {
        List<Alternative> result = new ArrayList<>();
        for (String part : splitAlternatives(mask)) {
            result.add(parseAlternative(part));
        }
        return result;
    }

    private List<String> splitAlternatives(String mask) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < mask.length(); i++) {
            char c = mask.charAt(i);
            if (c == '\\' && i + 1 < mask.length()) {
                current.append(c).append(mask.charAt(i + 1));
                i++;
            } else if (c == '|') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private Alternative parseAlternative(String spec) {
        List<Element> elements = new ArrayList<>();

        int i = 0;
        while (i < spec.length()) {
            char c = spec.charAt(i);
            char maskChar;
            boolean optional;

            if (c == '\\' && i + 1 < spec.length()) {
                elements.add(Element.literal(spec.charAt(i + 1)));
                i += 2;
                continue;
            } else if (c == '[' && i + 2 < spec.length() && spec.charAt(i + 2) == ']' && isMaskChar(spec.charAt(i + 1))) {
                maskChar = spec.charAt(i + 1);
                optional = true;
                i += 3;
            } else if (c == '{' && i + 2 < spec.length() && spec.charAt(i + 2) == '}' && isMaskChar(spec.charAt(i + 1))) {
                maskChar = spec.charAt(i + 1);
                optional = false;
                i += 3;
            } else if (isMaskChar(c)) {
                maskChar = c;
                optional = false;
                i++;
            } else {
                elements.add(Element.literal(c));
                i++;
                continue;
            }

            boolean repeat = false;
            if (i < spec.length() && spec.charAt(i) == '+') {
                repeat = true;
                i++;
            }

            elements.add(Element.input(maskChar, optional, repeat));
        }

        // O primeiro literal logo apos um quantificador '+' vira separador digitavel:
        // o usuario precisa digita-lo para encerrar o grupo de repeticao.
        for (int idx = 1; idx < elements.size(); idx++) {
            Element prev = elements.get(idx - 1);
            Element cur = elements.get(idx);
            if (cur.literal && !cur.separator && !prev.literal && prev.repeat) {
                elements.set(idx, cur.asSeparator());
            }
        }

        return new Alternative(elements);
    }

    private String buildMaskHint() {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < alternatives.size(); a++) {
            if (a > 0) {
                sb.append('|');
            }
            for (Element e : alternatives.get(a).elements) {
                if (e.literal) {
                    sb.append(e.literalChar);
                } else {
                    sb.append(placeholder);
                    if (e.repeat) {
                        sb.append('+');
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Tenta encaixar o texto digitado ({@code typed}) numa alternativa.
     * O texto digitado inclui os separadores digitados pelo usuario; literais
     * automaticos (que nao sao separadores) sao apenas renderizados.
     */
    private MatchResult match(String typed, Alternative a) {
        StringBuilder display = new StringBuilder();
        StringBuilder clean = new StringBuilder();
        List<Integer> positions = new ArrayList<>();
        List<Character> pending = new ArrayList<>();
        List<Element> els = a.elements;
        boolean complete = true;
        int k = 0;

        for (int idx = 0; idx < els.size(); idx++) {
            Element e = els.get(idx);

            if (e.literal) {
                if (e.separator) {
                    if (k < typed.length() && typed.charAt(k) == e.literalChar) {
                        flushPending(pending, display, positions);
                        display.append(e.literalChar);
                        positions.add(k);
                        k++;
                    } else {
                        if (hasRequiredInputFrom(els, idx)) {
                            complete = false;
                        }
                        break;
                    }
                } else {
                    pending.add(e.literalChar);
                }
                continue;
            }

            if (k >= typed.length()) {
                if (hasRequiredInputFrom(els, idx)) {
                    complete = false;
                }
                break;
            }

            if (e.repeat) {
                boolean hasSep = idx + 1 < els.size()
                        && els.get(idx + 1).literal && els.get(idx + 1).separator;
                char sep = hasSep ? els.get(idx + 1).literalChar : '\0';
                int consumed = 0;
                while (k < typed.length()) {
                    char ch = typed.charAt(k);
                    if (hasSep && ch == sep) {
                        break;
                    }
                    if (!isValidChar(e.maskChar, ch)) {
                        return MatchResult.NONE;
                    }
                    flushPending(pending, display, positions);
                    char cc = applyConversion(e.maskChar, ch);
                    display.append(cc);
                    positions.add(k);
                    clean.append(cc);
                    k++;
                    consumed++;
                }
                if (consumed == 0 && !e.optional) {
                    complete = false;
                }
            } else {
                char ch = typed.charAt(k);
                if (!isValidChar(e.maskChar, ch)) {
                    return MatchResult.NONE;
                }
                flushPending(pending, display, positions);
                char cc = applyConversion(e.maskChar, ch);
                display.append(cc);
                positions.add(k);
                clean.append(cc);
                k++;
            }
        }

        if (k != typed.length()) {
            return MatchResult.NONE;
        }

        return new MatchResult(true, display.toString(),
                positions.stream().mapToInt(Integer::intValue).toArray(),
                clean.toString(), complete);
    }

    private void flushPending(List<Character> pending, StringBuilder display, List<Integer> positions) {
        for (char c : pending) {
            display.append(c);
            positions.add(-1);
        }
        pending.clear();
    }

    private boolean hasRequiredInputFrom(List<Element> els, int from) {
        for (int j = from; j < els.size(); j++) {
            Element e = els.get(j);
            if (!e.literal && !e.optional) {
                return true;
            }
        }
        return false;
    }

    private MatchResult selectMatch(String typed) {
        if (alternatives == null) {
            return MatchResult.NONE;
        }
        for (Alternative a : alternatives) {
            MatchResult r = match(typed, a);
            if (r.matched) {
                return r;
            }
        }
        return MatchResult.NONE;
    }

    private int displayOffsetToCleanIndex(int offset) {
        int count = 0;
        for (int i = 0; i < lastCleanPositions.length && i < offset; i++) {
            if (lastCleanPositions[i] >= 0) {
                count++;
            }
        }
        return count;
    }

    private int cleanIndexToDisplayOffset(int cleanIndex) {
        if (cleanIndex <= 0) {
            return 0;
        }
        int seen = 0;
        for (int i = 0; i < lastCleanPositions.length; i++) {
            if (lastCleanPositions[i] >= 0) {
                seen++;
                if (seen == cleanIndex) {
                    return i + 1;
                }
            }
        }
        return lastRendered.length();
    }

    private void setCaretPositionSafe(int position) {
        int len = getDocument().getLength();
        setCaretPosition(Math.max(0, Math.min(position, len)));
    }

    private record Element(boolean literal, char literalChar, char maskChar,
                           boolean optional, boolean repeat, boolean separator) {
        static Element literal(char c) {
            return new Element(true, c, '\0', false, false, false);
        }

        static Element input(char maskChar, boolean optional, boolean repeat) {
            return new Element(false, '\0', maskChar, optional, repeat, false);
        }

        Element asSeparator() {
            return new Element(true, literalChar, '\0', false, false, true);
        }
    }

    private record Alternative(List<Element> elements) {
    }

    private record MatchResult(boolean matched, String display, int[] positions,
                               String clean, boolean complete) {
        static final MatchResult NONE = new MatchResult(false, "", new int[0], "", false);
    }

    private class MaskDocumentFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            handleEdit(fb, offset, 0, string);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            handleEdit(fb, offset, length, text);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            handleEdit(fb, offset, length, "");
        }

        private void handleEdit(FilterBypass fb, int offset, int length, String text) throws BadLocationException {
            if (text == null) {
                text = "";
            }

            int startIndex = displayOffsetToCleanIndex(offset);
            int endIndex = displayOffsetToCleanIndex(offset + length);

            StringBuilder typed = new StringBuilder(typedBuffer);
            if (endIndex > typed.length()) {
                endIndex = typed.length();
            }
            if (startIndex > endIndex) {
                startIndex = endIndex;
            }
            typed.delete(startIndex, endIndex);

            int pos = startIndex;
            for (int i = 0; i < text.length(); i++) {
                typed.insert(pos, text.charAt(i));
                if (selectMatch(typed.toString()).matched) {
                    pos++;
                } else {
                    typed.deleteCharAt(pos);
                }
            }

            typedBuffer = typed.toString();
            MatchResult result = selectMatch(typedBuffer);
            cleanValue = result.clean;
            lastRendered = result.display;
            lastCleanPositions = result.positions;

            fb.remove(0, fb.getDocument().getLength());
            fb.insertString(0, lastRendered, null);

            dispachEvent(EventType.INPUT, MaskedTextField.this::getCleanText);

            int caret = cleanIndexToDisplayOffset(pos);
            SwingUtilities.invokeLater(() -> setCaretPositionSafe(caret));
        }
    }
}
