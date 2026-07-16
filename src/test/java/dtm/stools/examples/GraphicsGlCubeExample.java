package dtm.stools.examples;

import dtm.stools.component.panels.graphics.GraphicsInput;
import dtm.stools.component.panels.graphics.gl.GL;
import dtm.stools.component.panels.graphics.gl.GraphicsGlContext;
import dtm.stools.component.panels.graphics.gl.GraphicsGlPanel;
import dtm.stools.component.panels.graphics.gl.GraphicsGlRender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class GraphicsGlCubeExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GraphicsGlCubeExample::createAndShow);
    }

    private static void createAndShow() {
        GraphicsGlPanel panel = new GraphicsGlPanel(new CubeRender());
        panel.setFPS(60);

        JFrame frame = new JFrame("SwingTools - Cubo 3D com câmera orbital");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(new JLabel("  Arrastar: orbitar câmera | Scroll: zoom"), BorderLayout.SOUTH);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();

        new Timer(500, e -> frame.setTitle(
                "SwingTools - Cubo 3D | FPS: " + panel.getCurrentFPS())).start();
    }

    static class CubeRender implements GraphicsGlRender {

        private static final String VERTEX_SHADER = """
                #version 330 core
                layout(location = 0) in vec3 aPos;
                layout(location = 1) in vec3 aColor;
                uniform mat4 uMvp;
                out vec3 vColor;
                void main() {
                    gl_Position = uMvp * vec4(aPos, 1.0);
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
        private int ebo;
        private int mvpLocation;

        private float yaw = 0.7f;
        private float pitch = 0.4f;
        private boolean dragging;
        private int lastMouseX;
        private int lastMouseY;
        private float modelAngle;

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

            mvpLocation = GL.glGetUniformLocation(program, "uMvp");

            float[] vertices = {
                    -0.5f, -0.5f, -0.5f, 0f, 0f, 0f,
                     0.5f, -0.5f, -0.5f, 1f, 0f, 0f,
                     0.5f,  0.5f, -0.5f, 1f, 1f, 0f,
                    -0.5f,  0.5f, -0.5f, 0f, 1f, 0f,
                    -0.5f, -0.5f,  0.5f, 0f, 0f, 1f,
                     0.5f, -0.5f,  0.5f, 1f, 0f, 1f,
                     0.5f,  0.5f,  0.5f, 1f, 1f, 1f,
                    -0.5f,  0.5f,  0.5f, 0f, 1f, 1f
            };

            int[] indices = {
                    0, 1, 2, 2, 3, 0,
                    4, 6, 5, 6, 4, 7,
                    0, 4, 1, 1, 4, 5,
                    2, 6, 3, 3, 6, 7,
                    0, 3, 4, 3, 7, 4,
                    1, 5, 2, 2, 5, 6
            };

            vao = GL.glGenVertexArrays();
            GL.glBindVertexArray(vao);

            vbo = GL.glGenBuffers();
            GL.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);
            GL.glBufferData(GL.GL_ARRAY_BUFFER, vertices, GL.GL_STATIC_DRAW);

            ebo = GL.glGenBuffers();
            GL.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL.glBufferDataInt(GL.GL_ELEMENT_ARRAY_BUFFER, indices, GL.GL_STATIC_DRAW);

            GL.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 6 * Float.BYTES, 0);
            GL.glEnableVertexAttribArray(0);
            GL.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
            GL.glEnableVertexAttribArray(1);

            GL.glEnable(GL.GL_DEPTH_TEST);
        }

        @Override
        public void render(GraphicsGlContext context) {
            GraphicsInput input = context.getInput();

            if (input.isMouseButtonDown(MouseEvent.BUTTON1) && input.isMouseInside()) {
                if (dragging) {
                    yaw += (input.getMouseX() - lastMouseX) * 0.01f;
                    pitch += (input.getMouseY() - lastMouseY) * 0.01f;
                    pitch = Math.max(-1.4f, Math.min(1.4f, pitch));
                }
                lastMouseX = input.getMouseX();
                lastMouseY = input.getMouseY();
                dragging = true;
            } else {
                dragging = false;
            }

            float distance = (float) Math.max(1.5, Math.min(10.0, 3.0 + input.getWheelRotation() * 0.3));
            float eyeX = (float) (distance * Math.cos(pitch) * Math.sin(yaw));
            float eyeY = (float) (distance * Math.sin(pitch));
            float eyeZ = (float) (distance * Math.cos(pitch) * Math.cos(yaw));

            modelAngle += context.getDeltaTime() * 0.5f;

            float aspect = context.getWidth() / (float) Math.max(1, context.getHeight());
            float[] projection = perspective(60f, aspect, 0.1f, 100f);
            float[] view = lookAt(eyeX, eyeY, eyeZ, 0f, 0f, 0f, 0f, 1f, 0f);
            float[] model = rotationY(modelAngle);
            float[] mvp = multiply(projection, multiply(view, model));

            GL.glClearColor(0.08f, 0.09f, 0.12f, 1f);
            GL.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            GL.glUseProgram(program);
            GL.glUniformMatrix4fv(mvpLocation, false, mvp);
            GL.glBindVertexArray(vao);
            GL.glDrawElements(GL.GL_TRIANGLES, 36, GL.GL_UNSIGNED_INT, 0);
        }

        @Override
        public void resize(GraphicsGlContext context, int width, int height) {
            GL.glViewport(0, 0, width, height);
        }

        @Override
        public void dispose(GraphicsGlContext context) {
            if (program != 0) GL.glDeleteProgram(program);
            if (vbo != 0) GL.glDeleteBuffers(vbo);
            if (ebo != 0) GL.glDeleteBuffers(ebo);
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

        private static float[] perspective(float fovDegrees, float aspect, float near, float far) {
            float f = (float) (1.0 / Math.tan(Math.toRadians(fovDegrees) / 2.0));
            float[] m = new float[16];
            m[0] = f / aspect;
            m[5] = f;
            m[10] = (far + near) / (near - far);
            m[11] = -1f;
            m[14] = 2f * far * near / (near - far);
            return m;
        }

        private static float[] lookAt(float eyeX, float eyeY, float eyeZ,
                                      float centerX, float centerY, float centerZ,
                                      float upX, float upY, float upZ) {
            float fx = centerX - eyeX;
            float fy = centerY - eyeY;
            float fz = centerZ - eyeZ;
            float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
            fx /= fl; fy /= fl; fz /= fl;

            float sx = fy * upZ - fz * upY;
            float sy = fz * upX - fx * upZ;
            float sz = fx * upY - fy * upX;
            float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            sx /= sl; sy /= sl; sz /= sl;

            float ux = sy * fz - sz * fy;
            float uy = sz * fx - sx * fz;
            float uz = sx * fy - sy * fx;

            float[] m = new float[16];
            m[0] = sx;  m[4] = sy;  m[8] = sz;
            m[1] = ux;  m[5] = uy;  m[9] = uz;
            m[2] = -fx; m[6] = -fy; m[10] = -fz;
            m[12] = -(sx * eyeX + sy * eyeY + sz * eyeZ);
            m[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ);
            m[14] = fx * eyeX + fy * eyeY + fz * eyeZ;
            m[15] = 1f;
            return m;
        }

        private static float[] rotationY(float angle) {
            float c = (float) Math.cos(angle);
            float s = (float) Math.sin(angle);
            float[] m = new float[16];
            m[0] = c;  m[8] = s;
            m[5] = 1f;
            m[2] = -s; m[10] = c;
            m[15] = 1f;
            return m;
        }

        private static float[] multiply(float[] a, float[] b) {
            float[] r = new float[16];
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    float sum = 0f;
                    for (int k = 0; k < 4; k++) {
                        sum += a[k * 4 + row] * b[col * 4 + k];
                    }
                    r[col * 4 + row] = sum;
                }
            }
            return r;
        }
    }
}
