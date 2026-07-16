package dtm.stools.component.panels.graphics.gl;

public final class GL {

    public static final int GL_FALSE = 0;
    public static final int GL_TRUE = 1;

    public static final int GL_NO_ERROR = 0;

    public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
    public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
    public static final int GL_STENCIL_BUFFER_BIT = 0x00000400;

    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN = 0x0006;

    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_SCISSOR_TEST = 0x0C11;

    public static final int GL_BYTE = 0x1400;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_INT = 0x1404;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;

    public static final int GL_VENDOR = 0x1F00;
    public static final int GL_RENDERER = 0x1F01;
    public static final int GL_VERSION = 0x1F02;
    public static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;

    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;

    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;

    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_VERTEX_SHADER = 0x8B31;
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_LINK_STATUS = 0x8B82;
    public static final int GL_INFO_LOG_LENGTH = 0x8B84;

    static {
        GlNativeLoader.load();
    }

    private GL() {}

    public static native int glGetError();
    public static native String glGetString(int name);

    public static native void glClear(int mask);
    public static native void glClearColor(float red, float green, float blue, float alpha);
    public static native void glViewport(int x, int y, int width, int height);
    public static native void glEnable(int cap);
    public static native void glDisable(int cap);

    public static native int glGenBuffers();
    public static native void glDeleteBuffers(int buffer);
    public static native void glBindBuffer(int target, int buffer);
    public static native void glBufferData(int target, float[] data, int usage);
    public static native void glBufferDataInt(int target, int[] data, int usage);

    public static native int glGenVertexArrays();
    public static native void glDeleteVertexArrays(int array);
    public static native void glBindVertexArray(int array);

    public static native int glCreateShader(int type);
    public static native void glShaderSource(int shader, String source);
    public static native void glCompileShader(int shader);
    public static native int glGetShaderi(int shader, int pname);
    public static native String glGetShaderInfoLog(int shader);
    public static native void glDeleteShader(int shader);

    public static native int glCreateProgram();
    public static native void glAttachShader(int program, int shader);
    public static native void glLinkProgram(int program);
    public static native int glGetProgrami(int program, int pname);
    public static native String glGetProgramInfoLog(int program);
    public static native void glUseProgram(int program);
    public static native void glDeleteProgram(int program);

    public static native void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset);
    public static native void glEnableVertexAttribArray(int index);
    public static native void glDisableVertexAttribArray(int index);

    public static native int glGetUniformLocation(int program, String name);
    public static native void glUniform1f(int location, float v0);
    public static native void glUniform2f(int location, float v0, float v1);
    public static native void glUniform3f(int location, float v0, float v1, float v2);
    public static native void glUniform4f(int location, float v0, float v1, float v2, float v3);
    public static native void glUniform1i(int location, int v0);
    public static native void glUniformMatrix4fv(int location, boolean transpose, float[] value);

    public static native void glDrawArrays(int mode, int first, int count);
    public static native void glDrawElements(int mode, int count, int type, long offset);
}
