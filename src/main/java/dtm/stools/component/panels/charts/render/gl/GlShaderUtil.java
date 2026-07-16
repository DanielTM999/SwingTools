package dtm.stools.component.panels.charts.render.gl;

import dtm.stools.component.panels.graphics.gl.GL;

final class GlShaderUtil {

    private GlShaderUtil() {}

    static int buildProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compile(GL.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compile(GL.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GL.glCreateProgram();
        GL.glAttachShader(program, vertexShader);
        GL.glAttachShader(program, fragmentShader);
        GL.glLinkProgram(program);
        GL.glDeleteShader(vertexShader);
        GL.glDeleteShader(fragmentShader);
        if (GL.glGetProgrami(program, GL.GL_LINK_STATUS) == GL.GL_FALSE) {
            String log = GL.glGetProgramInfoLog(program);
            GL.glDeleteProgram(program);
            throw new IllegalStateException("Chart shader program link failed: " + log);
        }
        return program;
    }

    private static int compile(int type, String source) {
        int shader = GL.glCreateShader(type);
        GL.glShaderSource(shader, source);
        GL.glCompileShader(shader);
        if (GL.glGetShaderi(shader, GL.GL_COMPILE_STATUS) == GL.GL_FALSE) {
            String log = GL.glGetShaderInfoLog(shader);
            GL.glDeleteShader(shader);
            throw new IllegalStateException("Chart shader compile failed: " + log);
        }
        return shader;
    }
}
