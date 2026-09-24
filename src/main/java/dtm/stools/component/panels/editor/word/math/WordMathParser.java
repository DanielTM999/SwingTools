package dtm.stools.component.panels.editor.word.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WordMathParser {
    static final Set<String> FUNCTIONS = Set.of("sin","cos","tan","cot","sec","csc","log","ln","exp","lim","max","min","det","sinh","cosh","tanh","arcsin","arccos","arctan");
    static final Set<String> ACCENTS = Set.of("hat","bar","vec","dot","tilde");
    static final String NARY = "∑∏∐∫∬∭∮⋃⋂";
    private static final Map<String,String> COMMANDS = Map.ofEntries(
            Map.entry("alpha","α"),Map.entry("beta","β"),Map.entry("gamma","γ"),Map.entry("delta","δ"),Map.entry("epsilon","ε"),Map.entry("theta","θ"),
            Map.entry("lambda","λ"),Map.entry("mu","μ"),Map.entry("pi","π"),Map.entry("rho","ρ"),Map.entry("sigma","σ"),Map.entry("tau","τ"),Map.entry("phi","φ"),
            Map.entry("omega","ω"),Map.entry("Gamma","Γ"),Map.entry("Delta","Δ"),Map.entry("Theta","Θ"),Map.entry("Lambda","Λ"),Map.entry("Pi","Π"),Map.entry("Sigma","Σ"),
            Map.entry("Phi","Φ"),Map.entry("Omega","Ω"),Map.entry("infty","∞"),Map.entry("pm","±"),Map.entry("times","×"),Map.entry("cdot","·"),Map.entry("div","÷"),
            Map.entry("le","≤"),Map.entry("ge","≥"),Map.entry("ne","≠"),Map.entry("approx","≈"),Map.entry("to","→"),Map.entry("rightarrow","→"),Map.entry("in","∈"),
            Map.entry("partial","∂"),Map.entry("nabla","∇"),Map.entry("sum","∑"),Map.entry("prod","∏"),Map.entry("int","∫"),Map.entry("iint","∬"),Map.entry("oint","∮"),
            Map.entry("sqrt","√"),Map.entry("cbrt","∛"));
    private final String input;
    private int position;

    private WordMathParser(String input) { this.input = input; }

    public static WordMath parse(String linear) {
        if (linear == null) throw new IllegalArgumentException("Equation is required");
        if (linear.length() > 10_000) throw new IllegalArgumentException("Equation is too long");
        WordMathParser parser = new WordMathParser(linear.strip());
        WordMath result = parser.row(Set.of());
        parser.skip();
        if (parser.position < parser.input.length()) throw new IllegalArgumentException("Unexpected '" + parser.input.charAt(parser.position) + "' at " + parser.position);
        return result;
    }

    private WordMath row(Set<Character> stops) {
        List<WordMath> items = new ArrayList<>();
        while (true) {
            skip();
            if (position >= input.length() || stops.contains(input.charAt(position))) break;
            char c = input.charAt(position);
            if (c == ')' || c == ']' || c == '}') throw new IllegalArgumentException("Unbalanced '" + c + "' at " + position);
            if (c == '/') {
                position++;
                WordMath numerator = items.isEmpty() ? new WordMathText("") : unwrap(items.removeLast());
                skip();
                items.add(new WordMathFraction(numerator,unwrap(term())));
                continue;
            }
            items.add(term());
        }
        if (items.isEmpty()) return new WordMathText("");
        return WordMathRow.of(items);
    }

    private WordMath term() {
        WordMath base = atom();
        if (base instanceof WordMathNary nary) return nary;
        WordMath sub = null, sup = null;
        while (position < input.length() && (input.charAt(position) == '^' || input.charAt(position) == '_')) {
            char c = input.charAt(position++);
            WordMath script = unwrap(atom());
            if (c == '^') sup = script; else sub = script;
        }
        return sub == null && sup == null ? base : new WordMathScript(base,sub,sup);
    }

    private WordMath atom() {
        skip();
        if (position >= input.length()) throw new IllegalArgumentException("Incomplete equation");
        char c = input.charAt(position);
        switch (c) {
            case '(' -> { position++; WordMath content = row(Set.of(')')); expect(')'); return new WordMathDelimiter("(",")",content); }
            case '[' -> { position++; WordMath content = row(Set.of(']')); expect(']'); return new WordMathDelimiter("[","]",content); }
            case '{' -> { position++; WordMath content = row(Set.of('}')); expect('}'); return content; }
            case '|' -> { position++; WordMath content = row(Set.of('|')); expect('|'); return new WordMathDelimiter("|","|",content); }
            case '√', '∛', '∜' -> { position++; return radical(c == '√' ? null : new WordMathText(c == '∛' ? "3" : "4",true)); }
            case '\\' -> { return command(); }
            default -> { }
        }
        if (NARY.indexOf(c) >= 0) { position++; return nary(String.valueOf(c)); }
        if (Character.isDigit(c) || c == '.' && position+1 < input.length() && Character.isDigit(input.charAt(position+1))) {
            int start = position;
            while (position < input.length() && (Character.isDigit(input.charAt(position)) || input.charAt(position) == '.' || input.charAt(position) == ',')
                    && !(input.charAt(position) == ',' && (position+1 >= input.length() || !Character.isDigit(input.charAt(position+1))))) position++;
            return new WordMathText(input.substring(start,position),true);
        }
        if (Character.isLetter(c)) {
            int start = position;
            while (position < input.length() && Character.isLetter(input.charAt(position))) position++;
            String word = input.substring(start,position);
            boolean call = position < input.length() && input.charAt(position) == '(';
            if (word.equals("sqrt") && call) return radical(null);
            if (ACCENTS.contains(word) && call) return new WordMathAccent(word,unwrap(atom()));
            if (FUNCTIONS.contains(word)) { skip(); return new WordMathFunction(word,position < input.length() && !isOperator(input.charAt(position)) ? term() : new WordMathText("")); }
            return new WordMathText(word);
        }
        position += Character.charCount(input.codePointAt(position));
        return new WordMathText(input.substring(position-Character.charCount(input.codePointBefore(position)),position),true);
    }

    private WordMath radical(WordMath degree) {
        skip();
        if (degree == null && position < input.length() && input.charAt(position) == '(') {
            position++;
            WordMath first = row(Set.of(')','&'));
            if (position < input.length() && input.charAt(position) == '&') { position++; WordMath body = row(Set.of(')')); expect(')'); return new WordMathRadical(first,body); }
            expect(')');
            return new WordMathRadical(null,first);
        }
        return new WordMathRadical(degree,unwrap(atom()));
    }

    private WordMath nary(String operator) {
        WordMath lower = null, upper = null;
        while (position < input.length() && (input.charAt(position) == '^' || input.charAt(position) == '_')) {
            char c = input.charAt(position++);
            WordMath script = unwrap(atom());
            if (c == '^') upper = script; else lower = script;
        }
        skip();
        WordMath body = position >= input.length() || isCloser(input.charAt(position)) ? new WordMathText("") : unwrap(term());
        return new WordMathNary(operator,lower,upper,body);
    }

    private WordMath command() {
        int start = ++position;
        while (position < input.length() && Character.isLetter(input.charAt(position))) position++;
        String name = input.substring(start,position);
        String symbol = COMMANDS.get(name);
        if (symbol == null) throw new IllegalArgumentException("Unknown command \\" + name);
        if (NARY.contains(symbol)) return nary(symbol);
        if (symbol.equals("√")) return radical(null);
        if (symbol.equals("∛")) return radical(new WordMathText("3",true));
        return new WordMathText(symbol,!Character.isLetter(symbol.charAt(0)));
    }

    private static WordMath unwrap(WordMath math) { return math instanceof WordMathDelimiter d && d.open().equals("(") && d.close().equals(")") ? d.content() : math; }
    private static boolean isOperator(char c) { return "+-=<>≤≥≠±×·÷,;)]}|".indexOf(c) >= 0; }
    private static boolean isCloser(char c) { return ")]}|".indexOf(c) >= 0; }
    private void expect(char c) {
        if (position >= input.length() || input.charAt(position) != c) throw new IllegalArgumentException("Expected '" + c + "'");
        position++;
    }
    private void skip() { while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++; }

    public static String format(WordMath math) {
        return switch (math) {
            case WordMathRow row -> {
                StringBuilder b = new StringBuilder();
                for (WordMath item : row.items()) {
                    String next = format(item);
                    if (!b.isEmpty() && !next.isEmpty() && needsSpace(b.charAt(b.length()-1),next.charAt(0))) b.append(' ');
                    b.append(next);
                }
                yield b.toString();
            }
            case WordMathText text -> text.text();
            case WordMathFraction f -> group(f.numerator()) + "/" + group(f.denominator());
            case WordMathScript s -> group(s.base()) + (s.subscript() == null ? "" : "_" + group(s.subscript())) + (s.superscript() == null ? "" : "^" + group(s.superscript()));
            case WordMathRadical r -> r.degree() == null ? "√" + group(r.body()) : "√(" + format(r.degree()) + "&" + format(r.body()) + ")";
            case WordMathDelimiter d -> d.open() + format(d.content()) + d.close();
            case WordMathNary n -> n.operator() + (n.lower() == null ? "" : "_" + group(n.lower())) + (n.upper() == null ? "" : "^" + group(n.upper())) + " " + group(n.body());
            case WordMathFunction f -> f.name() + (f.argument() instanceof WordMathDelimiter ? "" : " ") + format(f.argument());
            case WordMathAccent a -> a.accent() + "(" + format(a.base()) + ")";
            default -> "";
        };
    }
    private static boolean needsSpace(char previous, char next) { return Character.isLetter(previous) && Character.isLetter(next); }
    private static String group(WordMath math) {
        String value = format(math);
        boolean simple = math instanceof WordMathDelimiter || math instanceof WordMathText t && (t.text().codePointCount(0,t.text().length()) <= 1 || t.text().chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '.'));
        return simple && !value.isEmpty() ? value : "(" + value + ")";
    }
}
