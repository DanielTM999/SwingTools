package dtm.stools.component.panels.charts.render.gl;

import dtm.stools.component.panels.charts.style.ChartColor;
import dtm.stools.component.panels.graphics.gl.GL;

import java.util.Arrays;

public final class Gl2D {

    @FunctionalInterface
    public interface VertexColorFunction {
        ChartColor colorAt(float x, float y);
    }

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout(location = 0) in vec2 aPos;
            layout(location = 1) in vec4 aColor;
            uniform vec2 uViewport;
            out vec4 vColor;
            void main() {
                vec2 ndc = vec2(aPos.x / uViewport.x * 2.0 - 1.0, 1.0 - aPos.y / uViewport.y * 2.0);
                gl_Position = vec4(ndc, 0.0, 1.0);
                vColor = aColor;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec4 vColor;
            out vec4 fragColor;
            void main() {
                fragColor = vColor;
            }
            """;

    private static final int FLOATS_PER_VERTEX = 6;
    private static final int MAX_VERTICES = 12288;
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private final float[] buffer = new float[MAX_VERTICES * FLOATS_PER_VERTEX];
    private int vertexCount;

    private int program;
    private int vao;
    private int vbo;
    private int viewportLocation;
    private float feather = 1.2f;
    private boolean initialized;

    public void init() {
        if (initialized) return;
        program = GlShaderUtil.buildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        viewportLocation = GL.glGetUniformLocation(program, "uViewport");

        vao = GL.glGenVertexArrays();
        GL.glBindVertexArray(vao);
        vbo = GL.glGenBuffers();
        GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        GL.glBufferData(GL.GL_ARRAY_BUFFER, new float[FLOATS_PER_VERTEX], GL.GL_DYNAMIC_DRAW);
        GL.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        GL.glEnableVertexAttribArray(0);
        GL.glVertexAttribPointer(1, 4, GL.GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 2L * Float.BYTES);
        GL.glEnableVertexAttribArray(1);
        GL.glBindVertexArray(0);
        initialized = true;
    }

    public void dispose() {
        if (!initialized) return;
        if (program != 0) GL.glDeleteProgram(program);
        if (vbo != 0) GL.glDeleteBuffers(vbo);
        if (vao != 0) GL.glDeleteVertexArrays(vao);
        program = vbo = vao = 0;
        initialized = false;
    }

    public float getFeather() {
        return feather;
    }

    public void setFeather(float feather) {
        this.feather = Math.max(0f, feather);
    }

    public void begin(int width, int height) {
        vertexCount = 0;
        GL.glDisable(GL.GL_DEPTH_TEST);
        GL.glDisable(GL.GL_CULL_FACE);
        GL.glEnable(GL.GL_BLEND);
        GL.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        GL.glUseProgram(program);
        GL.glUniform2f(viewportLocation, Math.max(1, width), Math.max(1, height));
    }

    public void end() {
        flush();
    }

    public void flush() {
        if (vertexCount == 0) return;
        GL.glUseProgram(program);
        GL.glBindVertexArray(vao);
        GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
        GL.glBufferData(GL.GL_ARRAY_BUFFER,
                Arrays.copyOf(buffer, vertexCount * FLOATS_PER_VERTEX), GL.GL_DYNAMIC_DRAW);
        GL.glDrawArrays(GL.GL_TRIANGLES, 0, vertexCount);
        GL.glBindVertexArray(0);
        vertexCount = 0;
    }

    public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, ChartColor color) {
        triangle(x1, y1, color, 1f, x2, y2, color, 1f, x3, y3, color, 1f);
    }

    public void fillRect(float x, float y, float width, float height, ChartColor color) {
        if (width <= 0 || height <= 0) return;
        quad(x, y, color, 1f,
                x + width, y, color, 1f,
                x + width, y + height, color, 1f,
                x, y + height, color, 1f);
    }

    public void fillRectGradientVertical(float x, float y, float width, float height,
                                         ChartColor top, ChartColor bottom) {
        if (width <= 0 || height <= 0) return;
        quad(x, y, top, 1f,
                x + width, y, top, 1f,
                x + width, y + height, bottom, 1f,
                x, y + height, bottom, 1f);
    }

    public void fillQuad(float x1, float y1, ChartColor c1,
                         float x2, float y2, ChartColor c2,
                         float x3, float y3, ChartColor c3,
                         float x4, float y4, ChartColor c4) {
        quad(x1, y1, c1, 1f, x2, y2, c2, 1f, x3, y3, c3, 1f, x4, y4, c4, 1f);
    }

    public void strokeLine(float x1, float y1, float x2, float y2, float width, ChartColor color) {
        polylineXs[0] = x1; polylineYs[0] = y1;
        polylineXs[1] = x2; polylineYs[1] = y2;
        strokePolyline(polylineXs, polylineYs, 2, width, color);
    }

    private final float[] polylineXs = new float[2];
    private final float[] polylineYs = new float[2];

    public void strokePolyline(float[] xs, float[] ys, int n, float width, ChartColor color) {
        if (n < 2) {
            if (n == 1) fillCircle(xs[0], ys[0], Math.max(0.5f, width * 0.5f), color);
            return;
        }
        float half = Math.max(0.4f, width * 0.5f);
        float core = Math.max(0.15f, half - feather * 0.5f);
        float outer = half + feather * 0.5f;

        float[] offX = new float[n];
        float[] offY = new float[n];
        computeJoinOffsets(xs, ys, n, offX, offY);

        for (int i = 0; i < n - 1; i++) {
            float ax = xs[i], ay = ys[i];
            float bx = xs[i + 1], by = ys[i + 1];
            float oax = offX[i], oay = offY[i];
            float obx = offX[i + 1], oby = offY[i + 1];

            quad(ax + oax * core, ay + oay * core, color, 1f,
                    bx + obx * core, by + oby * core, color, 1f,
                    bx - obx * core, by - oby * core, color, 1f,
                    ax - oax * core, ay - oay * core, color, 1f);

            quad(ax + oax * core, ay + oay * core, color, 1f,
                    bx + obx * core, by + oby * core, color, 1f,
                    bx + obx * outer, by + oby * outer, color, 0f,
                    ax + oax * outer, ay + oay * outer, color, 0f);
            quad(ax - oax * core, ay - oay * core, color, 1f,
                    bx - obx * core, by - oby * core, color, 1f,
                    bx - obx * outer, by - oby * outer, color, 0f,
                    ax - oax * outer, ay - oay * outer, color, 0f);
        }
    }

    public void fillCircle(float cx, float cy, float radius, ChartColor color) {
        fillArc(cx, cy, 0f, radius, 0f, TWO_PI, color);
    }

    public void strokeCircle(float cx, float cy, float radius, float width, ChartColor color) {
        float half = Math.max(0.4f, width * 0.5f);
        fillArc(cx, cy, Math.max(0f, radius - half), radius + half, 0f, TWO_PI, color);
    }

    public void fillArc(float cx, float cy, float innerRadius, float outerRadius,
                        float startAngle, float sweepAngle, ChartColor color) {
        if (outerRadius <= 0f || Math.abs(sweepAngle) < 1e-5f) return;
        innerRadius = Math.max(0f, Math.min(innerRadius, outerRadius));

        float f = Math.min(feather, Math.max(0.01f, (outerRadius - innerRadius) * 0.4f));
        boolean hollow = innerRadius > f;
        float in0 = hollow ? innerRadius : 0f;
        float in1 = hollow ? innerRadius + f : 0f;
        float out1 = Math.max(in1, outerRadius - f);
        float out0 = outerRadius;

        int steps = arcSteps(outerRadius, Math.abs(sweepAngle));
        float prevCos = (float) Math.cos(startAngle);
        float prevSin = (float) Math.sin(startAngle);
        for (int i = 1; i <= steps; i++) {
            float angle = startAngle + sweepAngle * i / steps;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            if (hollow) {
                ringQuad(cx, cy, prevCos, prevSin, cos, sin, in0, in1, 0f, 1f, color);
            }
            ringQuad(cx, cy, prevCos, prevSin, cos, sin, in1, out1, 1f, 1f, color);
            ringQuad(cx, cy, prevCos, prevSin, cos, sin, out1, out0, 1f, 0f, color);

            prevCos = cos;
            prevSin = sin;
        }
    }

    public void fillConvexPolygon(float[] xs, float[] ys, int n, ChartColor color) {
        fillConvexPolygon(xs, ys, n, (x, y) -> color);
    }

    public void fillConvexPolygon(float[] xs, float[] ys, int n, VertexColorFunction colorFn) {
        if (n < 3) return;

        float cx = 0f, cy = 0f;
        for (int i = 0; i < n; i++) {
            cx += xs[i];
            cy += ys[i];
        }
        cx /= n;
        cy /= n;
        ChartColor centerColor = colorFn.colorAt(cx, cy);

        float area2 = 0f;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area2 += xs[i] * ys[j] - xs[j] * ys[i];
        }
        float orientation = area2 >= 0f ? 1f : -1f;

        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            ChartColor ci = colorFn.colorAt(xs[i], ys[i]);
            ChartColor cj = colorFn.colorAt(xs[j], ys[j]);
            triangle(cx, cy, centerColor, 1f, xs[i], ys[i], ci, 1f, xs[j], ys[j], cj, 1f);
        }

        if (feather <= 0f) return;
        for (int i = 0; i < n; i++) {
            int prev = (i - 1 + n) % n;
            int next = (i + 1) % n;

            float nx1 = (ys[i] - ys[prev]) * orientation;
            float ny1 = (xs[prev] - xs[i]) * orientation;
            float nx2 = (ys[next] - ys[i]) * orientation;
            float ny2 = (xs[i] - xs[next]) * orientation;
            float len1 = (float) Math.sqrt(nx1 * nx1 + ny1 * ny1);
            float len2 = (float) Math.sqrt(nx2 * nx2 + ny2 * ny2);
            if (len1 > 1e-6f) { nx1 /= len1; ny1 /= len1; }
            if (len2 > 1e-6f) { nx2 /= len2; ny2 /= len2; }
            float nx = nx1 + nx2;
            float ny = ny1 + ny2;
            float len = (float) Math.sqrt(nx * nx + ny * ny);
            if (len > 1e-6f) { nx /= len; ny /= len; } else { nx = nx2; ny = ny2; }

            polyRingX[i] = xs[i] + nx * feather;
            polyRingY[i] = ys[i] + ny * feather;
        }
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            ChartColor ci = colorFn.colorAt(xs[i], ys[i]);
            ChartColor cj = colorFn.colorAt(xs[j], ys[j]);
            quad(xs[i], ys[i], ci, 1f,
                    xs[j], ys[j], cj, 1f,
                    polyRingX[j], polyRingY[j], cj, 0f,
                    polyRingX[i], polyRingY[i], ci, 0f);
        }
    }

    private static final int MAX_POLY_POINTS = 256;
    private final float[] polyRingX = new float[MAX_POLY_POINTS];
    private final float[] polyRingY = new float[MAX_POLY_POINTS];
    private final float[] roundRectXs = new float[MAX_POLY_POINTS];
    private final float[] roundRectYs = new float[MAX_POLY_POINTS];

    public void fillRoundRect(float x, float y, float width, float height, float radius, ChartColor color) {
        fillRoundRect(x, y, width, height, radius, radius, radius, radius, (px, py) -> color);
    }

    public void fillRoundRect(float x, float y, float width, float height, float radius,
                              VertexColorFunction colorFn) {
        fillRoundRect(x, y, width, height, radius, radius, radius, radius, colorFn);
    }

    public void fillRoundRect(float x, float y, float width, float height,
                              float radiusTopLeft, float radiusTopRight,
                              float radiusBottomRight, float radiusBottomLeft,
                              VertexColorFunction colorFn) {
        if (width <= 0 || height <= 0) return;
        float maxRadius = Math.min(width, height) * 0.5f;
        float rtl = clampRadius(radiusTopLeft, maxRadius);
        float rtr = clampRadius(radiusTopRight, maxRadius);
        float rbr = clampRadius(radiusBottomRight, maxRadius);
        float rbl = clampRadius(radiusBottomLeft, maxRadius);

        int n = 0;
        n = appendCorner(n, x + width - rtr, y + rtr, rtr, -TWO_PI / 4f, TWO_PI / 4f);
        n = appendCorner(n, x + width - rbr, y + height - rbr, rbr, 0f, TWO_PI / 4f);
        n = appendCorner(n, x + rbl, y + height - rbl, rbl, TWO_PI / 4f, TWO_PI / 4f);
        n = appendCorner(n, x + rtl, y + rtl, rtl, TWO_PI / 2f, TWO_PI / 4f);
        fillConvexPolygon(roundRectXs, roundRectYs, n, colorFn);
    }

    private int appendCorner(int n, float cx, float cy, float radius, float startAngle, float sweep) {
        if (radius <= 0.01f) {
            if (n < MAX_POLY_POINTS) {
                roundRectXs[n] = cx;
                roundRectYs[n] = cy;
                n++;
            }
            return n;
        }
        int steps = Math.max(2, Math.min(24, (int) Math.ceil(radius * 0.45f) + 2));
        for (int i = 0; i <= steps && n < MAX_POLY_POINTS; i++) {
            float angle = startAngle + sweep * i / steps;
            roundRectXs[n] = cx + (float) Math.cos(angle) * radius;
            roundRectYs[n] = cy + (float) Math.sin(angle) * radius;
            n++;
        }
        return n;
    }

    private static float clampRadius(float radius, float max) {
        return Math.max(0f, Math.min(radius, max));
    }

    private static int arcSteps(float radius, float sweep) {
        float maxStep = (float) (2.0 * Math.sqrt(0.4 / Math.max(4f, radius)));
        int steps = (int) Math.ceil(sweep / maxStep);
        return Math.max(2, Math.min(720, steps));
    }

    private void computeJoinOffsets(float[] xs, float[] ys, int n, float[] offX, float[] offY) {
        float prevNx = 0f, prevNy = 0f;
        for (int i = 0; i < n; i++) {
            float segNx, segNy;
            if (i < n - 1) {
                float dx = xs[i + 1] - xs[i];
                float dy = ys[i + 1] - ys[i];
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len < 1e-6f) { segNx = prevNx; segNy = prevNy; }
                else { segNx = -dy / len; segNy = dx / len; }
            } else {
                segNx = prevNx;
                segNy = prevNy;
            }

            if (i == 0 || i == n - 1) {
                offX[i] = segNx;
                offY[i] = segNy;
            } else {
                float nx = prevNx + segNx;
                float ny = prevNy + segNy;
                float len = (float) Math.sqrt(nx * nx + ny * ny);
                if (len < 1e-6f) {
                    offX[i] = segNx;
                    offY[i] = segNy;
                } else {
                    nx /= len;
                    ny /= len;
                    float dot = nx * segNx + ny * segNy;
                    float miter = 1f / Math.max(0.35f, dot);
                    offX[i] = nx * miter;
                    offY[i] = ny * miter;
                }
            }
            prevNx = segNx;
            prevNy = segNy;
        }
    }

    private void ringQuad(float cx, float cy, float cos0, float sin0, float cos1, float sin1,
                          float rInner, float rOuter, float alphaInner, float alphaOuter,
                          ChartColor color) {
        if (rOuter - rInner < 1e-4f) return;
        quad(cx + cos0 * rInner, cy + sin0 * rInner, color, alphaInner,
                cx + cos1 * rInner, cy + sin1 * rInner, color, alphaInner,
                cx + cos1 * rOuter, cy + sin1 * rOuter, color, alphaOuter,
                cx + cos0 * rOuter, cy + sin0 * rOuter, color, alphaOuter);
    }

    private void quad(float x1, float y1, ChartColor c1, float a1,
                      float x2, float y2, ChartColor c2, float a2,
                      float x3, float y3, ChartColor c3, float a3,
                      float x4, float y4, ChartColor c4, float a4) {
        triangle(x1, y1, c1, a1, x2, y2, c2, a2, x3, y3, c3, a3);
        triangle(x1, y1, c1, a1, x3, y3, c3, a3, x4, y4, c4, a4);
    }

    private void triangle(float x1, float y1, ChartColor c1, float a1,
                          float x2, float y2, ChartColor c2, float a2,
                          float x3, float y3, ChartColor c3, float a3) {
        if (vertexCount + 3 > MAX_VERTICES) {
            flush();
        }
        vertex(x1, y1, c1, a1);
        vertex(x2, y2, c2, a2);
        vertex(x3, y3, c3, a3);
    }

    private void vertex(float x, float y, ChartColor color, float alphaMul) {
        int base = vertexCount * FLOATS_PER_VERTEX;
        buffer[base] = x;
        buffer[base + 1] = y;
        buffer[base + 2] = color.r();
        buffer[base + 3] = color.g();
        buffer[base + 4] = color.b();
        buffer[base + 5] = color.a() * alphaMul;
        vertexCount++;
    }
}
