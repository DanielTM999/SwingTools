package dtm.stools.component.panels.editor.code.provider.def;

import dtm.stools.component.panels.editor.code.prototype.constants.TokenType;
import dtm.stools.component.panels.editor.code.provider.TokenColorProvider;

import javax.swing.*;
import java.awt.Color;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class DefaultTokenColorProvider implements TokenColorProvider {

    private final Map<String, Color> colorMap = new HashMap<>();

    public DefaultTokenColorProvider() {
        registerDefaults();
    }

    private void registerDefaults() {
        Color base = UIManager.getColor("TextArea.background");

        if (base == null) {
            base = Color.WHITE;
        }

        if (isDark(base)) {
            registerDarkTheme();
        } else {
            registerLightTheme();
        }
    }

    private void registerDarkTheme() {
        colorMap.put(TokenType.KEYWORD, new Color(197, 134, 192));
        colorMap.put(TokenType.IDENTIFIER, new Color(220, 220, 220));
        colorMap.put(TokenType.STRING, new Color(206, 145, 120));
        colorMap.put(TokenType.NUMBER, new Color(181, 206, 168));
        colorMap.put(TokenType.COMMENT, new Color(106, 153, 85));
        colorMap.put(TokenType.SYMBOL, new Color(212, 212, 212));
    }

    private void registerLightTheme() {
        colorMap.put(TokenType.KEYWORD, new Color(50, 104, 216));
        colorMap.put(TokenType.IDENTIFIER, new Color(0, 0, 0));
        colorMap.put(TokenType.STRING, new Color(163, 21, 21));
        colorMap.put(TokenType.NUMBER, new Color(0, 128, 0));
        colorMap.put(TokenType.COMMENT, new Color(0, 128, 0));
        colorMap.put(TokenType.SYMBOL, new Color(0, 0, 0));
    }

    private boolean isDark(Color color) {
        double luminance = (0.299 * color.getRed()
                + 0.587 * color.getGreen()
                + 0.114 * color.getBlue()) / 255;
        return luminance < 0.5;
    }

    @Override
    public Color getColor(String type) {
        Color fallback = UIManager.getColor("TextArea.foreground");
        if (fallback == null) fallback = Color.BLACK;

        if (TokenType.UNKNOWN.equals(type)) {
            return fallback;
        }

        return colorMap.getOrDefault(type, fallback);
    }
}