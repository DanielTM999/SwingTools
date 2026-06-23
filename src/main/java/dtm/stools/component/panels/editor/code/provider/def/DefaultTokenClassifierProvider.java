package dtm.stools.component.panels.editor.code.provider.def;

import dtm.stools.component.panels.editor.code.prototype.constants.TokenType;
import dtm.stools.component.panels.editor.code.provider.TokenClassifierCodeEditorProvider;

import java.util.HashMap;
import java.util.Map;

public class DefaultTokenClassifierProvider implements TokenClassifierCodeEditorProvider {

    private final Map<String, String> tokenMap = new HashMap<>();

    public DefaultTokenClassifierProvider() {
        registerKeywords();
        registerCommonTypes();
        registerLiterals();
    }

    private void registerKeywords() {
        tokenMap.put("if", TokenType.KEYWORD);
        tokenMap.put("else", TokenType.KEYWORD);
        tokenMap.put("for", TokenType.KEYWORD);
        tokenMap.put("while", TokenType.KEYWORD);
        tokenMap.put("do", TokenType.KEYWORD);
        tokenMap.put("switch", TokenType.KEYWORD);
        tokenMap.put("case", TokenType.KEYWORD);
        tokenMap.put("break", TokenType.KEYWORD);
        tokenMap.put("continue", TokenType.KEYWORD);
        tokenMap.put("return", TokenType.KEYWORD);

        tokenMap.put("try", TokenType.KEYWORD);
        tokenMap.put("catch", TokenType.KEYWORD);
        tokenMap.put("finally", TokenType.KEYWORD);
        tokenMap.put("throw", TokenType.KEYWORD);
        tokenMap.put("throws", TokenType.KEYWORD);

        tokenMap.put("new", TokenType.KEYWORD);
        tokenMap.put("this", TokenType.KEYWORD);
        tokenMap.put("super", TokenType.KEYWORD);

        tokenMap.put("public", TokenType.KEYWORD);
        tokenMap.put("private", TokenType.KEYWORD);
        tokenMap.put("protected", TokenType.KEYWORD);
        tokenMap.put("static", TokenType.KEYWORD);
        tokenMap.put("final", TokenType.KEYWORD);
        tokenMap.put("abstract", TokenType.KEYWORD);

        tokenMap.put("class", TokenType.KEYWORD);
        tokenMap.put("interface", TokenType.KEYWORD);
        tokenMap.put("enum", TokenType.KEYWORD);
        tokenMap.put("extends", TokenType.KEYWORD);
        tokenMap.put("implements", TokenType.KEYWORD);

        tokenMap.put("package", TokenType.KEYWORD);
        tokenMap.put("import", TokenType.KEYWORD);
    }

    private void registerCommonTypes() {
        tokenMap.put("void", TokenType.KEYWORD);
        tokenMap.put("int", TokenType.KEYWORD);
        tokenMap.put("long", TokenType.KEYWORD);
        tokenMap.put("double", TokenType.KEYWORD);
        tokenMap.put("float", TokenType.KEYWORD);
        tokenMap.put("boolean", TokenType.KEYWORD);
        tokenMap.put("char", TokenType.KEYWORD);
        tokenMap.put("byte", TokenType.KEYWORD);
        tokenMap.put("short", TokenType.KEYWORD);
        tokenMap.put("String", TokenType.KEYWORD);
    }

    private void registerLiterals() {
        tokenMap.put("true", TokenType.KEYWORD);
        tokenMap.put("false", TokenType.KEYWORD);
        tokenMap.put("null", TokenType.KEYWORD);
    }

    @Override
    public String classify(String token) {
        return tokenMap.getOrDefault(token, TokenType.UNKNOWN);
    }
}