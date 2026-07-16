package dtm.stools.component.panels.charts.render.gl;

import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.graphics.gl.GL;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GlTextRenderer {

    public enum HAlign { LEFT, CENTER, RIGHT }

    public enum VAlign { TOP, MIDDLE, BASELINE, BOTTOM }

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec2 aUV;
            uniform vec2 uViewport;
            out vec2 vUV;
            void main() {
                vec2 ndc = vec2(aPos.x / uViewport.x * 2.0 - 1.0, 1.0 - aPos.y / uViewport.y * 2.0);
                gl_Position = vec4(ndc, 0.0, 1.0);
                vUV = aUV;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec2 vUV;
            uniform sampler2D uTexture;
            uniform vec4 uColor;
            out vec4 fragColor;
            void main() {
                float alpha = texture(uTexture, vUV).a;
                fragColor = vec4(uColor.rgb, uColor.a * alpha);
            }
            """;

    private static final int MAX_CACHE_ENTRIES = 512;
    private static final int PADDING = 2;

    private static final class TextTexture {
        int textureId;
        int width;
        int height;
        int ascent;
    }

    private final LinkedHashMap<String, TextTexture> cache = new LinkedHashMap<>(128, 0.75f, true);
    private final Map<Font, FontMetrics> metricsCache = new HashMap<>();
    private final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    private final float[] quadBuffer = new float[6 * 4];

    private int program;
    private int vao;
    private int vbo;
    private int viewportLocation;
    private int colorLocation;
    private int textureLocation;
    private int viewportWidth = 1;
    private int viewportHeight = 1;
    private boolean initialized;

    public void init() {
        if (initialized) return;
        program = GlShaderUtil.buildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        viewportLocation = GL.glGetUniformLocation(program, "uViewport");
        colorLocation = GL.glGetUniformLocation(program, "uColor");
        textureLocation = GL.glGetUniformLocation(program, "uTexture");

        vao = GL.glGenVertexArrays();
        GL.glBindVertexArray(vao);
        vbo = GL.glGenBuffers();
        GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        GL.glBufferData(GL.GL_ARRAY_BUFFER, new float[4], GL.GL_DYNAMIC_DRAW);
        GL.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL.glEnableVertexAttribArray(0);
        GL.glVertexAttribPointer(1, 2, GL.GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        GL.glEnableVertexAttribArray(1);
        GL.glBindVertexArray(0);
        initialized = true;
    }

    public void dispose() {
        if (!initialized) return;
        for (TextTexture entry : cache.values()) {
            if (entry.textureId != 0) GL.glDeleteTextures(entry.textureId);
        }
        cache.clear();
        metricsCache.clear();
        if (program != 0) GL.glDeleteProgram(program);
        if (vbo != 0) GL.glDeleteBuffers(vbo);
        if (vao != 0) GL.glDeleteVertexArrays(vao);
        program = vbo = vao = 0;
        initialized = false;
    }

    public void begin(int width, int height) {
        viewportWidth = Math.max(1, width);
        viewportHeight = Math.max(1, height);
    }

    public float measureWidth(String text, Font font) {
        if (text == null || text.isEmpty()) return 0f;
        return metrics(font).stringWidth(text);
    }

    public float lineHeight(Font font) {
        FontMetrics fm = metrics(font);
        return fm.getAscent() + fm.getDescent();
    }

    public float ascent(Font font) {
        return metrics(font).getAscent();
    }

    public void drawText(String text, Font font, float x, float y, ChartColor color) {
        drawText(text, font, x, y, color, HAlign.LEFT, VAlign.TOP);
    }

    public void drawText(String text, Font font, float x, float y, ChartColor color,
                         HAlign hAlign, VAlign vAlign) {
        if (text == null || text.isEmpty() || color == null || color.a() <= 0f) return;
        TextTexture entry = entryFor(text, font);
        if (entry == null) return;

        float drawX = switch (hAlign) {
            case LEFT -> x - PADDING;
            case CENTER -> x - entry.width * 0.5f;
            case RIGHT -> x - entry.width + PADDING;
        };
        float drawY = switch (vAlign) {
            case TOP -> y - PADDING;
            case MIDDLE -> y - entry.height * 0.5f;
            case BASELINE -> y - PADDING - entry.ascent;
            case BOTTOM -> y - entry.height + PADDING;
        };
        drawX = Math.round(drawX);
        drawY = Math.round(drawY);

        float x2 = drawX + entry.width;
        float y2 = drawY + entry.height;
        fillQuadBuffer(drawX, drawY, x2, y2);

        GL.glUseProgram(program);
        GL.glUniform2f(viewportLocation, viewportWidth, viewportHeight);
        GL.glUniform4f(colorLocation, color.r(), color.g(), color.b(), color.a());
        GL.glActiveTexture(GL.GL_TEXTURE0);
        GL.glBindTexture(GL.GL_TEXTURE_2D, entry.textureId);
        GL.glUniform1i(textureLocation, 0);

        GL.glEnable(GL.GL_BLEND);
        GL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        GL.glBindVertexArray(vao);
        GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        GL.glBufferData(GL.GL_ARRAY_BUFFER, quadBuffer, GL.GL_DYNAMIC_DRAW);
        GL.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        GL.glBindVertexArray(0);
        GL.glBindTexture(GL.GL_TEXTURE_2D, 0);
    }

    private void fillQuadBuffer(float x1, float y1, float x2, float y2) {

        put(0, x1, y1, 0f, 0f);
        put(1, x2, y1, 1f, 0f);
        put(2, x2, y2, 1f, 1f);

        put(3, x1, y1, 0f, 0f);
        put(4, x2, y2, 1f, 1f);
        put(5, x1, y2, 0f, 1f);
    }

    private void put(int vertexIndex, float x, float y, float u, float v) {
        int base = vertexIndex * 4;
        quadBuffer[base] = x;
        quadBuffer[base + 1] = y;
        quadBuffer[base + 2] = u;
        quadBuffer[base + 3] = v;
    }

    private TextTexture entryFor(String text, Font font) {
        String key = font.getFontName() + '|' + font.getStyle() + '|' + font.getSize2D() + '|' + text;
        TextTexture entry = cache.get(key);
        if (entry != null) return entry;

        entry = rasterize(text, font);
        if (entry == null) return null;
        cache.put(key, entry);
        while (cache.size() > MAX_CACHE_ENTRIES) {
            var iterator = cache.entrySet().iterator();
            TextTexture eldest = iterator.next().getValue();
            iterator.remove();
            if (eldest.textureId != 0) GL.glDeleteTextures(eldest.textureId);
        }
        return entry;
    }

    private TextTexture rasterize(String text, Font font) {
        FontMetrics fm = metrics(font);
        int width = fm.stringWidth(text) + PADDING * 2;
        int height = fm.getAscent() + fm.getDescent() + PADDING * 2;
        if (width <= PADDING * 2 || height <= 0) return null;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setFont(font);
            graphics.setColor(java.awt.Color.WHITE);
            graphics.drawString(text, PADDING, PADDING + fm.getAscent());
        } finally {
            graphics.dispose();
        }

        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        TextTexture entry = new TextTexture();
        entry.width = width;
        entry.height = height;
        entry.ascent = PADDING + fm.getAscent();
        entry.textureId = GL.glGenTextures();
        GL.glBindTexture(GL.GL_TEXTURE_2D, entry.textureId);
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        GL.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        GL.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, width, height, 0,
                GL.GL_BGRA, GL.GL_UNSIGNED_BYTE, pixels);
        GL.glBindTexture(GL.GL_TEXTURE_2D, 0);
        return entry;
    }

    private FontMetrics metrics(Font font) {
        return metricsCache.computeIfAbsent(font, f -> {
            Graphics2D graphics = scratch.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                return graphics.getFontMetrics(f);
            } finally {
                graphics.dispose();
            }
        });
    }
}
