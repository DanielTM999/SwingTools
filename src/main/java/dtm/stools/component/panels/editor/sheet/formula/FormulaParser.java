package dtm.stools.component.panels.editor.sheet.formula;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellError;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FormulaParser {
    private static final Set<String> COMPARISON = Set.of("=", "<>", "<", ">", "<=", ">=");

    private final List<FormulaToken> tokens;
    private final FormulaLocale locale;
    private int index;
    private int groupDepth;

    private FormulaParser(List<FormulaToken> tokens, FormulaLocale locale) { this.tokens = tokens; this.locale = locale; }

    public static FormulaNode parse(String text, FormulaLocale locale) { return parse(text, locale, null, false); }

    public static FormulaNode parse(String text, FormulaLocale locale, CellAddress host, boolean r1c1) {
        String t = text.startsWith("=") ? text.substring(1) : text;
        if (t.isBlank()) throw new FormulaException("Empty formula", 0);
        FormulaParser p = new FormulaParser(new FormulaLexer(t, locale, host, r1c1).tokenize(), locale);
        FormulaNode node = p.expression();
        p.skipSpaces();
        if (!p.peek().is(TokenType.END)) throw new FormulaException("Unexpected '" + p.peek().text() + "'", p.peek().start());
        return node;
    }

    private FormulaToken peek() { skipSpaces(); return tokens.get(index); }
    private FormulaToken raw() { return tokens.get(index); }
    private void skipSpaces() { while (tokens.get(index).is(TokenType.SPACE)) index++; }
    private FormulaToken take() { FormulaToken t = peek(); index++; return t; }

    private FormulaToken expect(TokenType type, String what) {
        FormulaToken t = peek();
        if (!t.is(type)) throw new FormulaException("Expected " + what, t.start());
        index++;
        return t;
    }

    private FormulaNode expression() { return comparison(); }

    private FormulaNode comparison() {
        FormulaNode left = concat();
        while (peek().is(TokenType.OPERATOR) && COMPARISON.contains(peek().text())) {
            String op = take().text();
            left = new BinaryNode(op, left, concat());
        }
        return left;
    }

    private FormulaNode concat() {
        FormulaNode left = additive();
        while (peek().isOperator("&")) { take(); left = new BinaryNode("&", left, additive()); }
        return left;
    }

    private FormulaNode additive() {
        FormulaNode left = multiplicative();
        while (peek().isOperator("+") || peek().isOperator("-")) { String op = take().text(); left = new BinaryNode(op, left, multiplicative()); }
        return left;
    }

    private FormulaNode multiplicative() {
        FormulaNode left = power();
        while (peek().isOperator("*") || peek().isOperator("/")) { String op = take().text(); left = new BinaryNode(op, left, power()); }
        return left;
    }

    private FormulaNode power() {
        FormulaNode left = percent();
        while (peek().isOperator("^")) { take(); left = new BinaryNode("^", left, percent()); }
        return left;
    }

    private FormulaNode percent() {
        FormulaNode node = unary();
        while (peek().isOperator("%")) { take(); node = new UnaryNode("%", node); }
        return node;
    }

    private FormulaNode unary() {
        FormulaToken t = peek();
        if (t.isOperator("-") || t.isOperator("+") || t.isOperator("@")) {
            take();
            FormulaNode operand = unary();
            return new UnaryNode(t.text(), operand);
        }
        return intersection();
    }

    private FormulaNode intersection() {
        FormulaNode left = range();
        while (true) {
            if (!raw().is(TokenType.SPACE)) break;
            int save = index;
            skipSpaces();
            FormulaToken n = tokens.get(index);
            boolean startsRef = n.is(TokenType.REF) || n.is(TokenType.NAME) || n.is(TokenType.STRUCTURED) || n.is(TokenType.LPAREN) || n.is(TokenType.FUNCTION);
            if (startsRef && left.isReference()) { left = new BinaryNode(" ", left, range()); continue; }
            index = save;
            break;
        }
        return left;
    }

    private FormulaNode range() {
        FormulaNode left = postfix();
        while (peek().isOperator(":")) { take(); left = new BinaryNode(":", left, postfix()); }
        return left;
    }

    private FormulaNode postfix() {
        FormulaNode node = primary();
        while (true) {
            FormulaToken t = raw();
            if (t.isOperator("#")) { index++; node = new UnaryNode("#", node); continue; }
            if (t.is(TokenType.LPAREN) && (node instanceof ParenNode || node instanceof FunctionNode || node instanceof CallNode)) {
                index++;
                node = new CallNode(node, arguments());
                continue;
            }
            break;
        }
        return node;
    }

    private FormulaNode primary() {
        FormulaToken t = take();
        switch (t.type()) {
            case NUMBER -> { return new NumberNode((Double) t.value()); }
            case STRING -> { return new StringNode((String) t.value()); }
            case BOOL -> { return new BoolNode((Boolean) t.value()); }
            case ERROR -> { return new ErrorNode((CellError) t.value()); }
            case REF -> { return (RefNode) t.value(); }
            case NAME -> { return (NameNode) t.value(); }
            case STRUCTURED -> { return (StructuredRefNode) t.value(); }
            case FUNCTION -> {
                expect(TokenType.LPAREN, "(");
                return new FunctionNode((String) t.value(), arguments());
            }
            case LPAREN -> {
                groupDepth++;
                FormulaNode inner = expression();
                while (peek().is(TokenType.SEPARATOR)) { take(); inner = new BinaryNode(",", inner, expression()); }
                expect(TokenType.RPAREN, ")");
                groupDepth--;
                return new ParenNode(inner);
            }
            case LBRACE -> { return array(); }
            case END -> throw new FormulaException("Unexpected end of formula", t.start());
            default -> throw new FormulaException("Unexpected '" + t.text() + "'", t.start());
        }
    }

    private List<FormulaNode> arguments() {
        List<FormulaNode> args = new ArrayList<>();
        if (peek().is(TokenType.RPAREN)) { take(); return args; }
        while (true) {
            FormulaToken t = peek();
            if (t.is(TokenType.SEPARATOR) || t.is(TokenType.RPAREN)) args.add(MissingNode.INSTANCE);
            else args.add(expression());
            FormulaToken sep = take();
            if (sep.is(TokenType.RPAREN)) break;
            if (!sep.is(TokenType.SEPARATOR)) throw new FormulaException("Expected " + locale.argumentSeparator() + " or )", sep.start());
            if (peek().is(TokenType.RPAREN)) { take(); args.add(MissingNode.INSTANCE); break; }
        }
        return args;
    }

    private FormulaNode array() {
        List<List<FormulaNode>> rows = new ArrayList<>();
        List<FormulaNode> row = new ArrayList<>();
        boolean commaLocale = locale.arrayColumnSeparator() == '\\';
        while (true) {
            row.add(expression());
            FormulaToken sep = take();
            if (sep.is(TokenType.RBRACE)) break;
            boolean column = sep.is(TokenType.COLUMN_SEPARATOR) || !commaLocale && sep.is(TokenType.SEPARATOR);
            boolean newRow = sep.is(TokenType.ROW_SEPARATOR) || commaLocale && sep.is(TokenType.SEPARATOR);
            if (column) continue;
            if (newRow) { rows.add(row); row = new ArrayList<>(); continue; }
            throw new FormulaException("Invalid array constant", sep.start());
        }
        rows.add(row);
        int width = rows.getFirst().size();
        for (List<FormulaNode> r : rows) if (r.size() != width) throw new FormulaException("Array rows must have the same size", 0);
        return new ArrayNode(rows);
    }
}
