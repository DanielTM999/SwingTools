package dtm.stools.examples;

import dtm.stools.component.panels.graphics.GraphicsInput;
import dtm.stools.component.panels.graphics.gl.GL;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;
import dtm.stools.component.panels.graphics.gl.GraphicsGlPanel;
import dtm.stools.component.panels.graphics.gl.GraphicsGlRender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ThreadLocalRandom;

public class GraphicsGlPanelExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GraphicsGlPanelExample::createAndShow);
    }

    private static void createAndShow() {
        TriangleRender render = new TriangleRender();
        GraphicsGlPanel panel = new GraphicsGlPanel(render);
        panel.setFPS(60);

        JButton colorButton = new JButton("Cor de fundo aleatória");
        colorButton.addActionListener(e -> panel.runOnUiThread(() -> {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            render.setClearColor(r.nextFloat(), r.nextFloat(), r.nextFloat());
        }));

        JCheckBox vsyncBox = new JCheckBox("VSync");
        vsyncBox.addActionListener(e -> panel.setVsync(vsyncBox.isSelected()));

        JPanel bottom = new JPanel();
        bottom.add(colorButton);
        bottom.add(vsyncBox);
        bottom.add(new JLabel("Setas: rotação | Arrastar: mover | Scroll: zoom"));

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("click em " + e.getX() + "," + e.getY());
            }
        });

        JFrame frame = new JFrame("SwingTools - GraphicsGlPanel");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();

        new Timer(500, e -> frame.setTitle(
                "SwingTools - GraphicsGlPanel | FPS: " + panel.getCurrentFPS())).start();
    }

    static class TriangleRender implements GraphicsGlRender {

        private static final String VERTEX_SHADER = """
                #version 330 core
                layout(location = 0) in vec2 aPos;
                layout(location = 1) in vec3 aColor;
                uniform float uAngle;
                uniform vec2 uOffset;
                uniform float uScale;
                out vec3 vColor;
                void main() {
                    float c = cos(uAngle);
                    float s = sin(uAngle);
                    vec2 p = vec2(aPos.x * c - aPos.y * s, aPos.x * s + aPos.y * c);
                    gl_Position = vec4(p * uScale + uOffset, 0.0, 1.0);
                    vColor = aColor;
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

        private int program;
        private int vao;
        private int vbo;
        private int angleLocation;
        private int offsetLocation;
        private int scaleLocation;
        private float angle;
        private volatile float clearR = 0.1f;
        private volatile float clearG = 0.1f;
        private volatile float clearB = 0.14f;

        void setClearColor(float r, float g, float b) {
            clearR = r;
            clearG = g;
            clearB = b;
        }

        @Override
        public void initialize(GraphicsGlContext context) {
            System.out.println("GL_VERSION: " + GL.glGetString(GL.GL_VERSION));
            System.out.println("GL_RENDERER: " + GL.glGetString(GL.GL_RENDERER));

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

            angleLocation = GL.glGetUniformLocation(program, "uAngle");
            offsetLocation = GL.glGetUniformLocation(program, "uOffset");
            scaleLocation = GL.glGetUniformLocation(program, "uScale");

            float[] vertices = {
                    -0.6f, -0.5f, 1f, 0f, 0f,
                     0.6f, -0.5f, 0f, 1f, 0f,
                     0.0f,  0.7f, 0f, 0f, 1f
            };

            vao = GL.glGenVertexArrays();
            GL.glBindVertexArray(vao);

            vbo = GL.glGenBuffers();
            GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
            GL.glBufferData(GL.GL_ARRAY_BUFFER, vertices, GL.GL_STATIC_DRAW);

            GL.glVertexAttribPointer(0, 2, GL.GL_FLOAT, false, 5 * Float.BYTES, 0);
            GL.glEnableVertexAttribArray(0);
            GL.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 5 * Float.BYTES, 2L * Float.BYTES);
            GL.glEnableVertexAttribArray(1);
        }

        @Override
        public void render(GraphicsGlContext context) {
            GraphicsInput input = context.getInput();

            if (input.isKeyDown(KeyEvent.VK_LEFT)) {
                angle -= 2f * context.getDeltaTime();
            } else if (input.isKeyDown(KeyEvent.VK_RIGHT)) {
                angle += 2f * context.getDeltaTime();
            } else {
                angle += context.getDeltaTime();
            }

            float offsetX = 0f;
            float offsetY = 0f;
            if (input.isMouseButtonDown(MouseEvent.BUTTON1) && input.isMouseInside()) {
                offsetX = input.getMouseX() / (float) context.getWidth() * 2f - 1f;
                offsetY = 1f - input.getMouseY() / (float) context.getHeight() * 2f;
            }

            float scale = (float) Math.max(0.1, 1.0 - input.getWheelRotation() * 0.1);

            GL.glClearColor(clearR, clearG, clearB, 1f);
            GL.glClear(GL.GL_COLOR_BUFFER_BIT);

            GL.glUseProgram(program);
            GL.glUniform1f(angleLocation, angle);
            GL.glUniform2f(offsetLocation, offsetX, offsetY);
            GL.glUniform1f(scaleLocation, scale);
            GL.glBindVertexArray(vao);
            GL.glDrawArrays(GL.GL_TRIANGLES, 0, 3);
        }

        @Override
        public void resize(GraphicsGlContext context, int width, int height) {
            GL.glViewport(0, 0, width, height);
        }

        @Override
        public void dispose(GraphicsGlContext context) {
            if (program != 0) GL.glDeleteProgram(program);
            if (vbo != 0) GL.glDeleteBuffers(vbo);
            if (vao != 0) GL.glDeleteVertexArrays(vao);
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
