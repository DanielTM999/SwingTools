#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>
#include <X11/Xlib.h>
#include <GL/glx.h>
#include <dlfcn.h>
#include <cstdint>
#include <cstring>

#include "../shared/GlBindings.h"

namespace {

typedef GLXContext (*PFNGLXCREATECONTEXTATTRIBSARB)(Display*, GLXFBConfig, GLXContext, Bool, const int*);
typedef void (*PFNGLXSWAPINTERVALEXT)(Display*, GLXDrawable, int);
typedef int (*PFNGLXSWAPINTERVALSGI)(int);

constexpr int GLX_CONTEXT_MAJOR_VERSION_ARB_ = 0x2091;
constexpr int GLX_CONTEXT_MINOR_VERSION_ARB_ = 0x2092;
constexpr int GLX_CONTEXT_PROFILE_MASK_ARB_ = 0x9126;
constexpr int GLX_CONTEXT_CORE_PROFILE_BIT_ARB_ = 0x0001;

struct GlSurface {
    Display* display;
    Drawable drawable;
    GLXContext context;
};

struct SurfaceInfo {
    Display* display;
    Drawable drawable;
    VisualID visualId;
    bool valid;
};

SurfaceInfo get_surface(JNIEnv* env, jobject canvas) {
    SurfaceInfo info = {};
    JAWT awt;
    awt.version = JAWT_VERSION_9;
    if (JAWT_GetAWT(env, &awt) == JNI_FALSE) return info;

    JAWT_DrawingSurface* ds = awt.GetDrawingSurface(env, canvas);
    if (!ds) return info;

    jint lock = ds->Lock(ds);
    if ((lock & JAWT_LOCK_ERROR) == 0) {
        JAWT_DrawingSurfaceInfo* dsi = ds->GetDrawingSurfaceInfo(ds);
        if (dsi && dsi->platformInfo) {
            JAWT_X11DrawingSurfaceInfo* x11 = reinterpret_cast<JAWT_X11DrawingSurfaceInfo*>(dsi->platformInfo);
            info.display = x11->display;
            info.drawable = x11->drawable;
            info.visualId = x11->visualID;
            info.valid = info.display != nullptr && info.drawable != 0;
            ds->FreeDrawingSurfaceInfo(dsi);
        }
        ds->Unlock(ds);
    }
    awt.FreeDrawingSurface(ds);
    return info;
}

GLXFBConfig find_fbconfig(Display* dpy, VisualID visualId) {
    const int attribs[] = {
        GLX_X_RENDERABLE, True,
        GLX_DRAWABLE_TYPE, GLX_WINDOW_BIT,
        GLX_RENDER_TYPE, GLX_RGBA_BIT,
        GLX_DOUBLEBUFFER, True,
        None
    };
    int count = 0;
    GLXFBConfig* configs = glXChooseFBConfig(dpy, DefaultScreen(dpy), attribs, &count);
    if (!configs) return nullptr;

    GLXFBConfig match = nullptr;
    for (int i = 0; i < count; i++) {
        XVisualInfo* vi = glXGetVisualFromFBConfig(dpy, configs[i]);
        if (vi) {
            if (vi->visualid == visualId) {
                match = configs[i];
                XFree(vi);
                break;
            }
            XFree(vi);
        }
    }
    XFree(configs);
    return match;
}

}

void* stgl_get_proc(const char* name) {
    void* p = reinterpret_cast<void*>(glXGetProcAddressARB(reinterpret_cast<const GLubyte*>(name)));
    if (!p) p = dlsym(RTLD_DEFAULT, name);
    return p;
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nCreateContext(JNIEnv* env, jclass, jobject canvas) {
    SurfaceInfo info = get_surface(env, canvas);
    if (!info.valid) return 0;

    Display* dpy = info.display;
    GLXContext ctx = nullptr;

    GLXFBConfig fbc = find_fbconfig(dpy, info.visualId);
    auto createAttribs = reinterpret_cast<PFNGLXCREATECONTEXTATTRIBSARB>(
        glXGetProcAddressARB(reinterpret_cast<const GLubyte*>("glXCreateContextAttribsARB")));

    if (fbc && createAttribs) {
        const int attribs[] = {
            GLX_CONTEXT_MAJOR_VERSION_ARB_, 3,
            GLX_CONTEXT_MINOR_VERSION_ARB_, 3,
            GLX_CONTEXT_PROFILE_MASK_ARB_, GLX_CONTEXT_CORE_PROFILE_BIT_ARB_,
            None
        };
        ctx = createAttribs(dpy, fbc, nullptr, True, attribs);
    }

    if (!ctx) {
        XVisualInfo tpl = {};
        tpl.visualid = info.visualId;
        int n = 0;
        XVisualInfo* vi = XGetVisualInfo(dpy, VisualIDMask, &tpl, &n);
        if (vi) {
            ctx = glXCreateContext(dpy, vi, nullptr, True);
            XFree(vi);
        }
    }

    if (!ctx) return 0;

    if (!glXMakeCurrent(dpy, info.drawable, ctx)) {
        glXDestroyContext(dpy, ctx);
        return 0;
    }

    stgl_load_functions();

    GlSurface* s = new GlSurface{dpy, info.drawable, ctx};
    return reinterpret_cast<jlong>(s);
}

JNIEXPORT jboolean JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nMakeCurrent(JNIEnv*, jclass, jlong handle) {
    if (!handle) return JNI_FALSE;
    GlSurface* s = reinterpret_cast<GlSurface*>(handle);
    return glXMakeCurrent(s->display, s->drawable, s->context) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nSwapBuffers(JNIEnv*, jclass, jlong handle) {
    if (!handle) return;
    GlSurface* s = reinterpret_cast<GlSurface*>(handle);
    glXSwapBuffers(s->display, s->drawable);
}

JNIEXPORT void JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nReadPixels(JNIEnv* env, jclass, jint width, jint height, jintArray pixels) {
    if (!pixels || width <= 0 || height <= 0) return;
    jsize required = width * height;
    if (required <= 0 || env->GetArrayLength(pixels) < required) return;
    void* data = env->GetPrimitiveArrayCritical(pixels, nullptr);
    if (!data) return;
    stgl_read_pixels(width, height, data);
    env->ReleasePrimitiveArrayCritical(pixels, data, 0);
}

JNIEXPORT void JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nSetVsync(JNIEnv*, jclass, jlong handle, jboolean vsync) {
    if (!handle) return;
    GlSurface* s = reinterpret_cast<GlSurface*>(handle);
    int interval = vsync ? 1 : 0;
    auto swapIntervalExt = reinterpret_cast<PFNGLXSWAPINTERVALEXT>(
        glXGetProcAddressARB(reinterpret_cast<const GLubyte*>("glXSwapIntervalEXT")));
    if (swapIntervalExt) {
        swapIntervalExt(s->display, s->drawable, interval);
        return;
    }
    auto swapIntervalSgi = reinterpret_cast<PFNGLXSWAPINTERVALSGI>(
        glXGetProcAddressARB(reinterpret_cast<const GLubyte*>("glXSwapIntervalSGI")));
    if (swapIntervalSgi) swapIntervalSgi(interval);
}

JNIEXPORT void JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nDestroyContext(JNIEnv*, jclass, jlong handle) {
    if (!handle) return;
    GlSurface* s = reinterpret_cast<GlSurface*>(handle);
    glXMakeCurrent(s->display, None, nullptr);
    if (s->context) glXDestroyContext(s->display, s->context);
    delete s;
}

}

