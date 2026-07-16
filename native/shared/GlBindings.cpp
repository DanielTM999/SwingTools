#include <jni.h>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <vector>

#include "GlBindings.h"

#ifdef _WIN32
#define STGL_APIENTRY __stdcall
#else
#define STGL_APIENTRY
#endif

typedef unsigned int GLenum;
typedef unsigned int GLuint;
typedef unsigned int GLbitfield;
typedef int GLint;
typedef int GLsizei;
typedef unsigned char GLboolean;
typedef unsigned char GLubyte;
typedef float GLfloat;
typedef char GLchar;
typedef ptrdiff_t GLsizeiptr;

typedef GLenum (STGL_APIENTRY *PFN_glGetError)();
typedef const GLubyte* (STGL_APIENTRY *PFN_glGetString)(GLenum);
typedef void (STGL_APIENTRY *PFN_glClear)(GLbitfield);
typedef void (STGL_APIENTRY *PFN_glClearColor)(GLfloat, GLfloat, GLfloat, GLfloat);
typedef void (STGL_APIENTRY *PFN_glViewport)(GLint, GLint, GLsizei, GLsizei);
typedef void (STGL_APIENTRY *PFN_glEnable)(GLenum);
typedef void (STGL_APIENTRY *PFN_glDisable)(GLenum);
typedef void (STGL_APIENTRY *PFN_glGenBuffers)(GLsizei, GLuint*);
typedef void (STGL_APIENTRY *PFN_glDeleteBuffers)(GLsizei, const GLuint*);
typedef void (STGL_APIENTRY *PFN_glBindBuffer)(GLenum, GLuint);
typedef void (STGL_APIENTRY *PFN_glBufferData)(GLenum, GLsizeiptr, const void*, GLenum);
typedef void (STGL_APIENTRY *PFN_glGenVertexArrays)(GLsizei, GLuint*);
typedef void (STGL_APIENTRY *PFN_glDeleteVertexArrays)(GLsizei, const GLuint*);
typedef void (STGL_APIENTRY *PFN_glBindVertexArray)(GLuint);
typedef GLuint (STGL_APIENTRY *PFN_glCreateShader)(GLenum);
typedef void (STGL_APIENTRY *PFN_glShaderSource)(GLuint, GLsizei, const GLchar* const*, const GLint*);
typedef void (STGL_APIENTRY *PFN_glCompileShader)(GLuint);
typedef void (STGL_APIENTRY *PFN_glGetShaderiv)(GLuint, GLenum, GLint*);
typedef void (STGL_APIENTRY *PFN_glGetShaderInfoLog)(GLuint, GLsizei, GLsizei*, GLchar*);
typedef void (STGL_APIENTRY *PFN_glDeleteShader)(GLuint);
typedef GLuint (STGL_APIENTRY *PFN_glCreateProgram)();
typedef void (STGL_APIENTRY *PFN_glAttachShader)(GLuint, GLuint);
typedef void (STGL_APIENTRY *PFN_glLinkProgram)(GLuint);
typedef void (STGL_APIENTRY *PFN_glGetProgramiv)(GLuint, GLenum, GLint*);
typedef void (STGL_APIENTRY *PFN_glGetProgramInfoLog)(GLuint, GLsizei, GLsizei*, GLchar*);
typedef void (STGL_APIENTRY *PFN_glUseProgram)(GLuint);
typedef void (STGL_APIENTRY *PFN_glDeleteProgram)(GLuint);
typedef void (STGL_APIENTRY *PFN_glVertexAttribPointer)(GLuint, GLint, GLenum, GLboolean, GLsizei, const void*);
typedef void (STGL_APIENTRY *PFN_glEnableVertexAttribArray)(GLuint);
typedef void (STGL_APIENTRY *PFN_glDisableVertexAttribArray)(GLuint);
typedef GLint (STGL_APIENTRY *PFN_glGetUniformLocation)(GLuint, const GLchar*);
typedef void (STGL_APIENTRY *PFN_glUniform1f)(GLint, GLfloat);
typedef void (STGL_APIENTRY *PFN_glUniform2f)(GLint, GLfloat, GLfloat);
typedef void (STGL_APIENTRY *PFN_glUniform3f)(GLint, GLfloat, GLfloat, GLfloat);
typedef void (STGL_APIENTRY *PFN_glUniform4f)(GLint, GLfloat, GLfloat, GLfloat, GLfloat);
typedef void (STGL_APIENTRY *PFN_glUniform1i)(GLint, GLint);
typedef void (STGL_APIENTRY *PFN_glUniformMatrix4fv)(GLint, GLsizei, GLboolean, const GLfloat*);
typedef void (STGL_APIENTRY *PFN_glDrawArrays)(GLenum, GLint, GLsizei);
typedef void (STGL_APIENTRY *PFN_glDrawElements)(GLenum, GLsizei, GLenum, const void*);

static PFN_glGetError p_glGetError = nullptr;
static PFN_glGetString p_glGetString = nullptr;
static PFN_glClear p_glClear = nullptr;
static PFN_glClearColor p_glClearColor = nullptr;
static PFN_glViewport p_glViewport = nullptr;
static PFN_glEnable p_glEnable = nullptr;
static PFN_glDisable p_glDisable = nullptr;
static PFN_glGenBuffers p_glGenBuffers = nullptr;
static PFN_glDeleteBuffers p_glDeleteBuffers = nullptr;
static PFN_glBindBuffer p_glBindBuffer = nullptr;
static PFN_glBufferData p_glBufferData = nullptr;
static PFN_glGenVertexArrays p_glGenVertexArrays = nullptr;
static PFN_glDeleteVertexArrays p_glDeleteVertexArrays = nullptr;
static PFN_glBindVertexArray p_glBindVertexArray = nullptr;
static PFN_glCreateShader p_glCreateShader = nullptr;
static PFN_glShaderSource p_glShaderSource = nullptr;
static PFN_glCompileShader p_glCompileShader = nullptr;
static PFN_glGetShaderiv p_glGetShaderiv = nullptr;
static PFN_glGetShaderInfoLog p_glGetShaderInfoLog = nullptr;
static PFN_glDeleteShader p_glDeleteShader = nullptr;
static PFN_glCreateProgram p_glCreateProgram = nullptr;
static PFN_glAttachShader p_glAttachShader = nullptr;
static PFN_glLinkProgram p_glLinkProgram = nullptr;
static PFN_glGetProgramiv p_glGetProgramiv = nullptr;
static PFN_glGetProgramInfoLog p_glGetProgramInfoLog = nullptr;
static PFN_glUseProgram p_glUseProgram = nullptr;
static PFN_glDeleteProgram p_glDeleteProgram = nullptr;
static PFN_glVertexAttribPointer p_glVertexAttribPointer = nullptr;
static PFN_glEnableVertexAttribArray p_glEnableVertexAttribArray = nullptr;
static PFN_glDisableVertexAttribArray p_glDisableVertexAttribArray = nullptr;
static PFN_glGetUniformLocation p_glGetUniformLocation = nullptr;
static PFN_glUniform1f p_glUniform1f = nullptr;
static PFN_glUniform2f p_glUniform2f = nullptr;
static PFN_glUniform3f p_glUniform3f = nullptr;
static PFN_glUniform4f p_glUniform4f = nullptr;
static PFN_glUniform1i p_glUniform1i = nullptr;
static PFN_glUniformMatrix4fv p_glUniformMatrix4fv = nullptr;
static PFN_glDrawArrays p_glDrawArrays = nullptr;
static PFN_glDrawElements p_glDrawElements = nullptr;

static const GLenum STGL_INFO_LOG_LENGTH = 0x8B84;

static bool g_loaded = false;

template <typename T>
static bool stgl_load(T& fn, const char* name) {
    fn = reinterpret_cast<T>(stgl_get_proc(name));
    return fn != nullptr;
}

bool stgl_load_functions() {
    if (g_loaded) return true;
    bool ok = true;
    ok &= stgl_load(p_glGetError, "glGetError");
    ok &= stgl_load(p_glGetString, "glGetString");
    ok &= stgl_load(p_glClear, "glClear");
    ok &= stgl_load(p_glClearColor, "glClearColor");
    ok &= stgl_load(p_glViewport, "glViewport");
    ok &= stgl_load(p_glEnable, "glEnable");
    ok &= stgl_load(p_glDisable, "glDisable");
    ok &= stgl_load(p_glGenBuffers, "glGenBuffers");
    ok &= stgl_load(p_glDeleteBuffers, "glDeleteBuffers");
    ok &= stgl_load(p_glBindBuffer, "glBindBuffer");
    ok &= stgl_load(p_glBufferData, "glBufferData");
    ok &= stgl_load(p_glGenVertexArrays, "glGenVertexArrays");
    ok &= stgl_load(p_glDeleteVertexArrays, "glDeleteVertexArrays");
    ok &= stgl_load(p_glBindVertexArray, "glBindVertexArray");
    ok &= stgl_load(p_glCreateShader, "glCreateShader");
    ok &= stgl_load(p_glShaderSource, "glShaderSource");
    ok &= stgl_load(p_glCompileShader, "glCompileShader");
    ok &= stgl_load(p_glGetShaderiv, "glGetShaderiv");
    ok &= stgl_load(p_glGetShaderInfoLog, "glGetShaderInfoLog");
    ok &= stgl_load(p_glDeleteShader, "glDeleteShader");
    ok &= stgl_load(p_glCreateProgram, "glCreateProgram");
    ok &= stgl_load(p_glAttachShader, "glAttachShader");
    ok &= stgl_load(p_glLinkProgram, "glLinkProgram");
    ok &= stgl_load(p_glGetProgramiv, "glGetProgramiv");
    ok &= stgl_load(p_glGetProgramInfoLog, "glGetProgramInfoLog");
    ok &= stgl_load(p_glUseProgram, "glUseProgram");
    ok &= stgl_load(p_glDeleteProgram, "glDeleteProgram");
    ok &= stgl_load(p_glVertexAttribPointer, "glVertexAttribPointer");
    ok &= stgl_load(p_glEnableVertexAttribArray, "glEnableVertexAttribArray");
    ok &= stgl_load(p_glDisableVertexAttribArray, "glDisableVertexAttribArray");
    ok &= stgl_load(p_glGetUniformLocation, "glGetUniformLocation");
    ok &= stgl_load(p_glUniform1f, "glUniform1f");
    ok &= stgl_load(p_glUniform2f, "glUniform2f");
    ok &= stgl_load(p_glUniform3f, "glUniform3f");
    ok &= stgl_load(p_glUniform4f, "glUniform4f");
    ok &= stgl_load(p_glUniform1i, "glUniform1i");
    ok &= stgl_load(p_glUniformMatrix4fv, "glUniformMatrix4fv");
    ok &= stgl_load(p_glDrawArrays, "glDrawArrays");
    ok &= stgl_load(p_glDrawElements, "glDrawElements");
    g_loaded = ok;
    return ok;
}

#define STGL_CLASS(name) Java_dtm_stools_component_panels_graphics_gl_GL_##name

extern "C" {

JNIEXPORT jint JNICALL STGL_CLASS(glGetError)(JNIEnv*, jclass) {
    return p_glGetError ? (jint)p_glGetError() : 0;
}

JNIEXPORT jstring JNICALL STGL_CLASS(glGetString)(JNIEnv* env, jclass, jint name) {
    if (!p_glGetString) return nullptr;
    const GLubyte* s = p_glGetString((GLenum)name);
    if (!s) return nullptr;
    return env->NewStringUTF(reinterpret_cast<const char*>(s));
}

JNIEXPORT void JNICALL STGL_CLASS(glClear)(JNIEnv*, jclass, jint mask) {
    if (p_glClear) p_glClear((GLbitfield)mask);
}

JNIEXPORT void JNICALL STGL_CLASS(glClearColor)(JNIEnv*, jclass, jfloat r, jfloat g, jfloat b, jfloat a) {
    if (p_glClearColor) p_glClearColor(r, g, b, a);
}

JNIEXPORT void JNICALL STGL_CLASS(glViewport)(JNIEnv*, jclass, jint x, jint y, jint w, jint h) {
    if (p_glViewport) p_glViewport(x, y, w, h);
}

JNIEXPORT void JNICALL STGL_CLASS(glEnable)(JNIEnv*, jclass, jint cap) {
    if (p_glEnable) p_glEnable((GLenum)cap);
}

JNIEXPORT void JNICALL STGL_CLASS(glDisable)(JNIEnv*, jclass, jint cap) {
    if (p_glDisable) p_glDisable((GLenum)cap);
}

JNIEXPORT jint JNICALL STGL_CLASS(glGenBuffers)(JNIEnv*, jclass) {
    GLuint id = 0;
    if (p_glGenBuffers) p_glGenBuffers(1, &id);
    return (jint)id;
}

JNIEXPORT void JNICALL STGL_CLASS(glDeleteBuffers)(JNIEnv*, jclass, jint buffer) {
    GLuint id = (GLuint)buffer;
    if (p_glDeleteBuffers) p_glDeleteBuffers(1, &id);
}

JNIEXPORT void JNICALL STGL_CLASS(glBindBuffer)(JNIEnv*, jclass, jint target, jint buffer) {
    if (p_glBindBuffer) p_glBindBuffer((GLenum)target, (GLuint)buffer);
}

JNIEXPORT void JNICALL STGL_CLASS(glBufferData)(JNIEnv* env, jclass, jint target, jfloatArray data, jint usage) {
    if (!p_glBufferData || !data) return;
    jsize len = env->GetArrayLength(data);
    void* ptr = env->GetPrimitiveArrayCritical(data, nullptr);
    if (!ptr) return;
    p_glBufferData((GLenum)target, (GLsizeiptr)len * (GLsizeiptr)sizeof(jfloat), ptr, (GLenum)usage);
    env->ReleasePrimitiveArrayCritical(data, ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL STGL_CLASS(glBufferDataInt)(JNIEnv* env, jclass, jint target, jintArray data, jint usage) {
    if (!p_glBufferData || !data) return;
    jsize len = env->GetArrayLength(data);
    void* ptr = env->GetPrimitiveArrayCritical(data, nullptr);
    if (!ptr) return;
    p_glBufferData((GLenum)target, (GLsizeiptr)len * (GLsizeiptr)sizeof(jint), ptr, (GLenum)usage);
    env->ReleasePrimitiveArrayCritical(data, ptr, JNI_ABORT);
}

JNIEXPORT jint JNICALL STGL_CLASS(glGenVertexArrays)(JNIEnv*, jclass) {
    GLuint id = 0;
    if (p_glGenVertexArrays) p_glGenVertexArrays(1, &id);
    return (jint)id;
}

JNIEXPORT void JNICALL STGL_CLASS(glDeleteVertexArrays)(JNIEnv*, jclass, jint array) {
    GLuint id = (GLuint)array;
    if (p_glDeleteVertexArrays) p_glDeleteVertexArrays(1, &id);
}

JNIEXPORT void JNICALL STGL_CLASS(glBindVertexArray)(JNIEnv*, jclass, jint array) {
    if (p_glBindVertexArray) p_glBindVertexArray((GLuint)array);
}

JNIEXPORT jint JNICALL STGL_CLASS(glCreateShader)(JNIEnv*, jclass, jint type) {
    return p_glCreateShader ? (jint)p_glCreateShader((GLenum)type) : 0;
}

JNIEXPORT void JNICALL STGL_CLASS(glShaderSource)(JNIEnv* env, jclass, jint shader, jstring source) {
    if (!p_glShaderSource || !source) return;
    const char* src = env->GetStringUTFChars(source, nullptr);
    if (!src) return;
    GLint len = (GLint)strlen(src);
    p_glShaderSource((GLuint)shader, 1, &src, &len);
    env->ReleaseStringUTFChars(source, src);
}

JNIEXPORT void JNICALL STGL_CLASS(glCompileShader)(JNIEnv*, jclass, jint shader) {
    if (p_glCompileShader) p_glCompileShader((GLuint)shader);
}

JNIEXPORT jint JNICALL STGL_CLASS(glGetShaderi)(JNIEnv*, jclass, jint shader, jint pname) {
    GLint v = 0;
    if (p_glGetShaderiv) p_glGetShaderiv((GLuint)shader, (GLenum)pname, &v);
    return (jint)v;
}

JNIEXPORT jstring JNICALL STGL_CLASS(glGetShaderInfoLog)(JNIEnv* env, jclass, jint shader) {
    if (!p_glGetShaderiv || !p_glGetShaderInfoLog) return env->NewStringUTF("");
    GLint len = 0;
    p_glGetShaderiv((GLuint)shader, STGL_INFO_LOG_LENGTH, &len);
    if (len <= 1) return env->NewStringUTF("");
    std::vector<GLchar> buf((size_t)len);
    GLsizei written = 0;
    p_glGetShaderInfoLog((GLuint)shader, len, &written, buf.data());
    return env->NewStringUTF(buf.data());
}

JNIEXPORT void JNICALL STGL_CLASS(glDeleteShader)(JNIEnv*, jclass, jint shader) {
    if (p_glDeleteShader) p_glDeleteShader((GLuint)shader);
}

JNIEXPORT jint JNICALL STGL_CLASS(glCreateProgram)(JNIEnv*, jclass) {
    return p_glCreateProgram ? (jint)p_glCreateProgram() : 0;
}

JNIEXPORT void JNICALL STGL_CLASS(glAttachShader)(JNIEnv*, jclass, jint program, jint shader) {
    if (p_glAttachShader) p_glAttachShader((GLuint)program, (GLuint)shader);
}

JNIEXPORT void JNICALL STGL_CLASS(glLinkProgram)(JNIEnv*, jclass, jint program) {
    if (p_glLinkProgram) p_glLinkProgram((GLuint)program);
}

JNIEXPORT jint JNICALL STGL_CLASS(glGetProgrami)(JNIEnv*, jclass, jint program, jint pname) {
    GLint v = 0;
    if (p_glGetProgramiv) p_glGetProgramiv((GLuint)program, (GLenum)pname, &v);
    return (jint)v;
}

JNIEXPORT jstring JNICALL STGL_CLASS(glGetProgramInfoLog)(JNIEnv* env, jclass, jint program) {
    if (!p_glGetProgramiv || !p_glGetProgramInfoLog) return env->NewStringUTF("");
    GLint len = 0;
    p_glGetProgramiv((GLuint)program, STGL_INFO_LOG_LENGTH, &len);
    if (len <= 1) return env->NewStringUTF("");
    std::vector<GLchar> buf((size_t)len);
    GLsizei written = 0;
    p_glGetProgramInfoLog((GLuint)program, len, &written, buf.data());
    return env->NewStringUTF(buf.data());
}

JNIEXPORT void JNICALL STGL_CLASS(glUseProgram)(JNIEnv*, jclass, jint program) {
    if (p_glUseProgram) p_glUseProgram((GLuint)program);
}

JNIEXPORT void JNICALL STGL_CLASS(glDeleteProgram)(JNIEnv*, jclass, jint program) {
    if (p_glDeleteProgram) p_glDeleteProgram((GLuint)program);
}

JNIEXPORT void JNICALL STGL_CLASS(glVertexAttribPointer)(JNIEnv*, jclass, jint index, jint size, jint type, jboolean normalized, jint stride, jlong offset) {
    if (p_glVertexAttribPointer) {
        p_glVertexAttribPointer((GLuint)index, size, (GLenum)type, normalized ? 1 : 0, stride,
                                reinterpret_cast<const void*>((intptr_t)offset));
    }
}

JNIEXPORT void JNICALL STGL_CLASS(glEnableVertexAttribArray)(JNIEnv*, jclass, jint index) {
    if (p_glEnableVertexAttribArray) p_glEnableVertexAttribArray((GLuint)index);
}

JNIEXPORT void JNICALL STGL_CLASS(glDisableVertexAttribArray)(JNIEnv*, jclass, jint index) {
    if (p_glDisableVertexAttribArray) p_glDisableVertexAttribArray((GLuint)index);
}

JNIEXPORT jint JNICALL STGL_CLASS(glGetUniformLocation)(JNIEnv* env, jclass, jint program, jstring name) {
    if (!p_glGetUniformLocation || !name) return -1;
    const char* n = env->GetStringUTFChars(name, nullptr);
    if (!n) return -1;
    GLint loc = p_glGetUniformLocation((GLuint)program, n);
    env->ReleaseStringUTFChars(name, n);
    return (jint)loc;
}

JNIEXPORT void JNICALL STGL_CLASS(glUniform1f)(JNIEnv*, jclass, jint location, jfloat v0) {
    if (p_glUniform1f) p_glUniform1f(location, v0);
}

JNIEXPORT void JNICALL STGL_CLASS(glUniform2f)(JNIEnv*, jclass, jint location, jfloat v0, jfloat v1) {
    if (p_glUniform2f) p_glUniform2f(location, v0, v1);
}

JNIEXPORT void JNICALL STGL_CLASS(glUniform3f)(JNIEnv*, jclass, jint location, jfloat v0, jfloat v1, jfloat v2) {
    if (p_glUniform3f) p_glUniform3f(location, v0, v1, v2);
}

JNIEXPORT void JNICALL STGL_CLASS(glUniform4f)(JNIEnv*, jclass, jint location, jfloat v0, jfloat v1, jfloat v2, jfloat v3) {
    if (p_glUniform4f) p_glUniform4f(location, v0, v1, v2, v3);
}

JNIEXPORT void JNICALL STGL_CLASS(glUniform1i)(JNIEnv*, jclass, jint location, jint v0) {
    if (p_glUniform1i) p_glUniform1i(location, v0);
}

JNIEXPORT void JNICALL STGL_CLASS(glUniformMatrix4fv)(JNIEnv* env, jclass, jint location, jboolean transpose, jfloatArray value) {
    if (!p_glUniformMatrix4fv || !value) return;
    jsize len = env->GetArrayLength(value);
    if (len < 16) return;
    void* ptr = env->GetPrimitiveArrayCritical(value, nullptr);
    if (!ptr) return;
    p_glUniformMatrix4fv(location, len / 16, transpose ? 1 : 0, static_cast<const GLfloat*>(ptr));
    env->ReleasePrimitiveArrayCritical(value, ptr, JNI_ABORT);
}

JNIEXPORT void JNICALL STGL_CLASS(glDrawArrays)(JNIEnv*, jclass, jint mode, jint first, jint count) {
    if (p_glDrawArrays) p_glDrawArrays((GLenum)mode, first, count);
}

JNIEXPORT void JNICALL STGL_CLASS(glDrawElements)(JNIEnv*, jclass, jint mode, jint count, jint type, jlong offset) {
    if (p_glDrawElements) {
        p_glDrawElements((GLenum)mode, count, (GLenum)type, reinterpret_cast<const void*>((intptr_t)offset));
    }
}

}
