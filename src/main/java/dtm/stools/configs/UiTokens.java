package dtm.stools.configs;

import dtm.stools.utils.ColorUtils;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fonte central de tokens visuais (cor, espaçamento, raio e tipografia) usada pelos componentes modernos.
 */
public final class UiTokens {

    /**
     * Escalas de raio de canto disponíveis.
     */
    public enum Radius {
        NONE(0),
        SM(6),
        MD(10),
        LG(14),
        XL(20),
        PILL(999);

        private final int value;

        Radius(int value) {
            this.value = value;
        }

        /**
         * Retorna o raio em pixels já escalonado.
         */
        public int px() {
            return value >= 999 ? value : scale(value);
        }
    }

    private static final String NAMESPACE = "SwingTools.color.";
    private static final int SPACE_UNIT = 4;
    private static final float STROKE = 1.5f;

    private static final Map<String, Color> CACHE = new ConcurrentHashMap<>();

    private static float scaleFactor = 1f;

    private UiTokens() {
        throw new IllegalStateException("utility class");
    }

    /**
     * Cor de fundo da janela ou área principal.
     */
    public static Color background() {
        return resolve("background", "Panel.background", 0xF8FAFC, 0x1E1F22);
    }

    /**
     * Cor de superfície de cartões, popups e campos.
     */
    public static Color surface() {
        return resolve("surface", "TextField.background", 0xFFFFFF, 0x2B2D30);
    }

    /**
     * Variação sutil da superfície, usada em cabeçalhos e faixas alternadas.
     */
    public static Color surfaceAlt() {
        Color base = surface();
        return isDarkTheme() ? ColorUtils.brighter(base, 0.06f) : ColorUtils.darker(base, 0.04f);
    }

    /**
     * Cor principal de texto.
     */
    public static Color foreground() {
        return resolve("foreground", "Label.foreground", 0x0F172A, 0xDFE1E5);
    }

    /**
     * Cor de texto secundário e legendas.
     */
    public static Color muted() {
        return resolve("disabledForeground", "Label.disabledForeground", 0x64748B, 0x9DA0A8);
    }

    /**
     * Cor de bordas e divisores.
     */
    public static Color border() {
        return resolve("border", "Component.borderColor", 0xE2E8F0, 0x4B4E54);
    }

    /**
     * Cor de ação primária.
     */
    public static Color primary() {
        return resolve("primary", "Component.focusColor", 0x2563EB, 0x3B82F6);
    }

    /**
     * Cor de destaque, usada em seleção e foco.
     */
    public static Color accent() {
        return resolve("accent", "Component.focusColor", 0x3B82F6, 0x4C8DF6);
    }

    /**
     * Cor semântica de sucesso.
     */
    public static Color success() {
        return resolve("success", null, 0x22C55E, 0x34D06E);
    }

    /**
     * Cor semântica de alerta.
     */
    public static Color warning() {
        return resolve("warning", null, 0xF59E0B, 0xFBBF24);
    }

    /**
     * Cor semântica de erro.
     */
    public static Color danger() {
        return resolve("danger", null, 0xEF4444, 0xF26D6D);
    }

    /**
     * Cor semântica informativa.
     */
    public static Color info() {
        return resolve("info", null, 0x3B82F6, 0x60A5FA);
    }

    /**
     * Cor de texto legível sobre a cor informada.
     */
    public static Color onColor(Color background) {
        return ColorUtils.foregroundFor(background);
    }

    /**
     * Variação da cor para o estado de hover.
     */
    public static Color hover(Color base) {
        if (base == null) {
            return null;
        }
        return isDarkTheme() ? ColorUtils.brighter(base, 0.12f) : ColorUtils.darker(base, 0.08f);
    }

    /**
     * Variação da cor para o estado pressionado.
     */
    public static Color pressed(Color base) {
        if (base == null) {
            return null;
        }
        return isDarkTheme() ? ColorUtils.brighter(base, 0.22f) : ColorUtils.darker(base, 0.18f);
    }

    /**
     * Variação da cor para o estado desabilitado.
     */
    public static Color disabled(Color base) {
        if (base == null) {
            return null;
        }
        return ColorUtils.mix(base, background(), 0.55f);
    }

    /**
     * Superfície translúcida usada em overlays e realces sutis.
     */
    public static Color overlay(Color base, float alpha) {
        return ColorUtils.withAlpha(base, alpha);
    }

    /**
     * Espaçamento em pixels equivalente à quantidade de passos de 4px.
     */
    public static int space(int steps) {
        return scale(steps * SPACE_UNIT);
    }

    /**
     * Raio de canto em pixels para a escala informada.
     */
    public static int radius(Radius radius) {
        return radius == null ? 0 : radius.px();
    }

    /**
     * Espessura padrão de traço dos componentes.
     */
    public static float stroke() {
        return STROKE * scaleFactor;
    }

    /**
     * Converte um valor em pixels aplicando o fator de escala corrente.
     */
    public static int scale(int px) {
        return scaleFactor == 1f ? px : Math.round(px * scaleFactor);
    }

    /**
     * Fator de escala corrente aplicado aos tokens dimensionais.
     */
    public static float getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Define o fator de escala aplicado aos tokens dimensionais.
     */
    public static void setScaleFactor(float factor) {
        if (factor <= 0f) {
            throw new IllegalArgumentException("scale factor must be greater than zero");
        }
        scaleFactor = factor;
    }

    /**
     * Fonte padrão da interface.
     */
    public static Font font() {
        Font font = UIManager.getFont("Label.font");
        return font != null ? font : new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    /**
     * Fonte padrão em negrito.
     */
    public static Font fontBold() {
        return font().deriveFont(Font.BOLD);
    }

    /**
     * Fonte reduzida para legendas e textos auxiliares.
     */
    public static Font fontSmall() {
        Font base = font();
        return base.deriveFont(Math.max(10f, base.getSize2D() - 2f));
    }

    /**
     * Fonte ampliada para títulos.
     */
    public static Font fontTitle() {
        Font base = font();
        return base.deriveFont(Font.BOLD, base.getSize2D() + 4f);
    }

    /**
     * Fonte monoespaçada.
     */
    public static Font fontMono() {
        Font font = UIManager.getFont("TextArea.font");
        if (font != null && font.getFamily().toLowerCase().contains("mono")) {
            return font;
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, font().getSize());
    }

    /**
     * Indica se o tema corrente é escuro.
     */
    public static boolean isDarkTheme() {
        Color base = UIManager.getColor("Panel.background");
        return base != null && ColorUtils.isDark(base);
    }

    /**
     * Descarta os tokens memorizados, forçando releitura do tema.
     */
    public static void refresh() {
        CACHE.clear();
    }

    private static Color resolve(String token, String lafKey, int lightFallback, int darkFallback) {
        return CACHE.computeIfAbsent(token, key -> {
            Color namespaced = UIManager.getColor(NAMESPACE + key);
            if (namespaced != null) {
                return namespaced;
            }
            if (lafKey != null) {
                Color fromLaf = UIManager.getColor(lafKey);
                if (fromLaf != null) {
                    return fromLaf;
                }
            }
            return new Color(isDarkTheme() ? darkFallback : lightFallback);
        });
    }
}
