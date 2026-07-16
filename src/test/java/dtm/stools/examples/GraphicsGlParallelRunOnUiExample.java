package dtm.stools.examples;

import dtm.stools.component.panels.graphics.gl.GL;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;
import dtm.stools.component.panels.graphics.gl.GraphicsGlPanel;
import dtm.stools.component.panels.graphics.gl.GraphicsGlRender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphicsGlParallelRunOnUiExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GraphicsGlParallelRunOnUiExample::createAndShow);
    }

    private static void createAndShow() {
        ParallelWaveRender render = new ParallelWaveRender();
        GraphicsGlPanel panel = new GraphicsGlPanel(render);
        panel.setFPS(60);

        JLabel statusLabel = new JLabel("  aguardando contexto GL");
        JButton generateButton = new JButton("Gerar em paralelo");
        generateButton.addActionListener(e -> render.requestGenerate());

        JCheckBox autoBox = new JCheckBox("Auto");
        Timer generateTimer = new Timer(1200, e -> render.requestGenerate());
        autoBox.addActionListener(e -> {
            if (autoBox.isSelected()) {
                generateTimer.start();
            } else {
                generateTimer.stop();
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(generateButton);
        bottom.add(autoBox);
        bottom.add(statusLabel);

        JFrame frame = new JFrame("SwingTools - GL paralelo + runOnUiThread");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                generateTimer.stop();
                panel.dispose();
            }
        });
        frame.setVisible(true);

        new Timer(250, e -> {
            statusLabel.setText("  " + render.getStatus());
            frame.setTitle("SwingTools - GL paralelo + runOnUiThread | FPS: " + panel.getCurrentFPS());
        }).start();
    }

    static class ParallelWaveRender implements GraphicsGlRender {

        private static final int SAMPLE_COUNT = 24_000;
        private static final int FLOATS_PER_VERTEX = 5;

        private static final String VERTEX_SHADER = """
                #version 330 core
                layout(location = 0) in vec2 aPos;
                layout(location = 1) in vec3 aColor;
                uniform float uPulse;
                out vec3 vColor;
                void main() {
                    gl_Position = vec4(aPos, 0.0, 1.0);
                    float glow = 0.78 + 0.22 * sin(uPulse + aPos.x * 8.0);
                    vColor = aColor * glow;
                }
                """;

        private static final String FRAGMENT_SHADER = """
                #version 330 core
                in vec3 vColor;
                out vec4 fragColor;
                void main() {
                    fragColor = vec4(vColor, 1.0);
                }
                """;

        private final AtomicInteger generation = new AtomicInteger();
        private final ExecutorService workers;

        private volatile boolean glReady;
        private volatile String status = "aguardando contexto GL";

        private int program;
        private int vao;
        private int vbo;
        private int pulseLocation;
        private int vertexCount;
        private float pulse;

        ParallelWaveRender() {
            int threads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
            ThreadFactory factory = new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger();

                @Override
                public Thread newThread(Runnable task) {
                    Thread thread = new Thread(task, "GL-Wave-Worker-" + count.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            };
            workers = Executors.newFixedThreadPool(threads, factory);
        }

        String getStatus() {
            return status;
        }

        void requestGenerate() {
            if (!glReady) {
                status = "aguardando contexto GL";
                return;
            }

            int id = generation.incrementAndGet();
            int bands = Math.max(2, Runtime.getRuntime().availableProcessors());
            status = "gerando malha #" + id + " em " + bands + " tarefas";

            List<CompletableFuture<float[]>> futures = new ArrayList<>();
            for (int band = 0; band < bands; band++) {
                int currentBand = band;
                futures.add(CompletableFuture.supplyAsync(
                        () -> buildBand(id, currentBand, bands), workers));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(ignored -> merge(futures))
                    .thenAccept(vertices -> runOnUiThread(() -> {
                        if (id != generation.get()) return;
                        uploadMesh(vertices);
                        status = "malha #" + id + " enviada ao VBO na thread GL";
                    }))
                    .exceptionally(error -> {
                        status = "erro ao gerar malha: " + error.getClass().getSimpleName();
                        error.printStackTrace();
                        return null;
                    });
        }

        @Override
        public void initialize(GraphicsGlContext context) {
            int vertexShader = compileShader(GL.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = compileShader(GL.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

            program = GL.glCreateProgram();
            GL.glAttachShader(program, vertexShader);
            GL.glAttachShader(program, fragmentShader);
            GL.glLinkProgram(program);
            if (GL.glGetProgrami(program, GL.GL_LINK_STATUS) == GL.GL_FALSE) {
                throw new IllegalStateException("Program link failed: " + GL.glGetProgramInfoLog(program));
            }
            GL.glDeleteShader(vertexShader);
            GL.glDeleteShader(fragmentShader);

            pulseLocation = GL.glGetUniformLocation(program, "uPulse");

            vao = GL.glGenVertexArrays();
            GL.glBindVertexArray(vao);

            vbo = GL.glGenBuffers();
            GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
            GL.glBufferData(GL.GL_ARRAY_BUFFER, new float[0], GL.GL_DYNAMIC_DRAW);

            int stride = FLOATS_PER_VERTEX * Float.BYTES;
            GL.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, stride, 0);
            GL.glEnableVertexAttribArray(0);
            GL.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, stride, 2L * Float.BYTES);
            GL.glEnableVertexAttribArray(1);

            glReady = true;
            status = "contexto GL pronto";
            requestGenerate();
        }

        @Override
        public void render(GraphicsGlContext context) {
            pulse += context.getDeltaTime() * 2.0f;

            GL.glClearColor(0.05f, 0.06f, 0.07f, 1f);
            GL.glClear(GL.GL_COLOR_BUFFER_BIT);

            GL.glUseProgram(program);
            GL.glUniform1f(pulseLocation, pulse);
            GL.glBindVertexArray(vao);
            if (vertexCount > 0) {
                GL.glDrawArrays(GL.GL_LINES, 0, vertexCount);
            }
        }

        @Override
        public void resize(GraphicsGlContext context, int width, int height) {
            GL.glViewport(0, 0, width, height);
        }

        @Override
        public void dispose(GraphicsGlContext context) {
            workers.shutdownNow();
            if (program != 0) GL.glDeleteProgram(program);
            if (vbo != 0) GL.glDeleteBuffers(vbo);
            if (vao != 0) GL.glDeleteVertexArrays(vao);
            glReady = false;
        }

        private void uploadMesh(float[] vertices) {
            GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
            GL.glBufferData(GL.GL_ARRAY_BUFFER, vertices, GL.GL_DYNAMIC_DRAW);
            vertexCount = vertices.length / FLOATS_PER_VERTEX;
        }

        private static float[] buildBand(int generationId, int band, int bands) {
            int start = SAMPLE_COUNT * band / bands;
            int end = SAMPLE_COUNT * (band + 1) / bands;
            float[] vertices = new float[(end - start) * 2 * FLOATS_PER_VERTEX];
            int offset = 0;

            for (int i = start; i < end; i++) {
                float x = -1f + 2f * i / (SAMPLE_COUNT - 1f);
                float noise = expensiveWave(x, generationId);
                float y = -0.65f + noise * 0.95f;
                float r = 0.25f + 0.55f * Math.max(0f, noise);
                float g = 0.55f + 0.35f * (1f - Math.abs(x));
                float b = 0.85f - 0.45f * Math.max(0f, noise);

                vertices[offset++] = x;
                vertices[offset++] = -0.78f;
                vertices[offset++] = 0.12f;
                vertices[offset++] = 0.16f;
                vertices[offset++] = 0.20f;

                vertices[offset++] = x;
                vertices[offset++] = y;
                vertices[offset++] = r;
                vertices[offset++] = g;
                vertices[offset++] = b;
            }

            return vertices;
        }

        private static float expensiveWave(float x, int generationId) {
            double value = 0.0;
            double phase = generationId * 0.37;
            for (int i = 1; i <= 140; i++) {
                double frequency = i * 0.18;
                value += Math.sin(x * frequency * 18.0 + phase) / (i * 0.58);
                value += Math.cos(x * frequency * 9.0 - phase * 0.7) / (i * 0.85);
            }
            return (float) Math.max(0.0, Math.min(1.0, 0.5 + value * 0.12));
        }

        private static float[] merge(List<CompletableFuture<float[]>> futures) {
            int size = 0;
            for (CompletableFuture<float[]> future : futures) {
                size += future.join().length;
            }

            float[] merged = new float[size];
            int offset = 0;
            for (CompletableFuture<float[]> future : futures) {
                float[] part = future.join();
                System.arraycopy(part, 0, merged, offset, part.length);
                offset += part.length;
            }
            return merged;
        }

        private static int compileShader(int type, String source) {
            int shader = GL.glCreateShader(type);
            GL.glShaderSource(shader, source);
            GL.glCompileShader(shader);
            if (GL.glGetShaderi(shader, GL.GL_COMPILE_STATUS) == GL.GL_FALSE) {
                throw new IllegalStateException("Shader compile failed: " + GL.glGetShaderInfoLog(shader));
            }
            return shader;
        }
    }
}
