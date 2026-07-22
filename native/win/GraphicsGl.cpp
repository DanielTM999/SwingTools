#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>
#include <windows.h>
#include <cstdint>

#include "../shared/GlBindings.h"

namespace {

typedef HGLRC (WINAPI *PFNWGLCREATECONTEXTATTRIBSARB)(HDC, HGLRC, const int*);
typedef BOOL (WINAPI *PFNWGLSWAPINTERVALEXT)(int);

constexpr int WGL_CONTEXT_MAJOR_VERSION_ARB = 0x2091;
constexpr int WGL_CONTEXT_MINOR_VERSION_ARB = 0x2092;
constexpr int WGL_CONTEXT_PROFILE_MASK_ARB = 0x9126;
constexpr int WGL_CONTEXT_CORE_PROFILE_BIT_ARB = 0x0001;

struct GlSurface {
    HWND hwnd;
    HDC hdc;
    HGLRC hglrc;
};

void release_frame_dc(GlSurface* s) {
    if (!s->hdc) return;
    if (wglGetCurrentDC() == s->hdc) wglMakeCurrent(nullptr, nullptr);
    ReleaseDC(s->hwnd, s->hdc);
    s->hdc = nullptr;
}

HWND get_hwnd(JNIEnv* env, jobject canvas) {
    JAWT awt;
    awt.version = JAWT_VERSION_9;
    if (JAWT_GetAWT(env, &awt) == JNI_FALSE) return nullptr;

    JAWT_DrawingSurface* ds = awt.GetDrawingSurface(env, canvas);
    if (!ds) return nullptr;

    HWND hwnd = nullptr;
    jint lock = ds->Lock(ds);
    if ((lock & JAWT_LOCK_ERROR) == 0) {
        JAWT_DrawingSurfaceInfo* dsi = ds->GetDrawingSurfaceInfo(ds);
        if (dsi && dsi->platformInfo) {
            hwnd = reinterpret_cast<JAWT_Win32DrawingSurfaceInfo*>(dsi->platformInfo)->hwnd;
            ds->FreeDrawingSurfaceInfo(dsi);
        }
        ds->Unlock(ds);
    }
    awt.FreeDrawingSurface(ds);
    return hwnd;
}

}

void* stgl_get_proc(const char* name) {
    void* p = reinterpret_cast<void*>(wglGetProcAddress(name));
    intptr_t v = reinterpret_cast<intptr_t>(p);
    if (v == 0 || v == 1 || v == 2 || v == 3 || v == -1) {
        static HMODULE mod = LoadLibraryA("opengl32.dll");
        p = mod ? reinterpret_cast<void*>(GetProcAddress(mod, name)) : nullptr;
    }
    return p;
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nCreateContext(JNIEnv* env, jclass, jobject canvas) {
    HWND hwnd = get_hwnd(env, canvas);
    if (!hwnd) return 0;

    HDC hdc = GetDC(hwnd);
    if (!hdc) return 0;

    PIXELFORMATDESCRIPTOR pfd = {};
    pfd.nSize = sizeof(pfd);
    pfd.nVersion = 1;
    pfd.dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
    pfd.iPixelType = PFD_TYPE_RGBA;
    pfd.cColorBits = 32;
    pfd.cDepthBits = 24;
    pfd.cStencilBits = 8;
    pfd.iLayerType = PFD_MAIN_PLANE;

    int pf = ChoosePixelFormat(hdc, &pfd);
    if (pf == 0) {
        ReleaseDC(hwnd, hdc);
        return 0;
    }

    if (GetPixelFormat(hdc) == 0 && !SetPixelFormat(hdc, pf, &pfd)) {
        ReleaseDC(hwnd, hdc);
        return 0;
    }

    HGLRC legacy = wglCreateContext(hdc);
    if (!legacy) {
        ReleaseDC(hwnd, hdc);
        return 0;
    }
    wglMakeCurrent(hdc, legacy);

    HGLRC ctx = nullptr;
    auto createAttribs = reinterpret_cast<PFNWGLCREATECONTEXTATTRIBSARB>(
        wglGetProcAddress("wglCreateContextAttribsARB"));
    if (createAttribs) {
        const int attribs[] = {
            WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
            WGL_CONTEXT_MINOR_VERSION_ARB, 3,
            WGL_CONTEXT_PROFILE_MASK_ARB, WGL_CONTEXT_CORE_PROFILE_BIT_ARB,
            0
        };
        ctx = createAttribs(hdc, nullptr, attribs);
    }

    if (ctx) {
        wglMakeCurrent(nullptr, nullptr);
        wglDeleteContext(legacy);
        wglMakeCurrent(hdc, ctx);
    } else {
        ctx = legacy;
    }

    stgl_load_functions();

    wglMakeCurrent(nullptr, nullptr);
    ReleaseDC(hwnd, hdc);

    GlSurface* s = new GlSurface{hwnd, nullptr, ctx};
    return reinterpret_cast<jlong>(s);
}

JNIEXPORT jboolean JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nMakeCurrent(JNIEnv*, jclass, jlong handle) {
    if (!handle) return JNI_FALSE;
    GlSurface* s = reinterpret_cast<GlSurface*>(handle);

    release_frame_dc(s);

    HDC hdc = GetDC(s->hwnd);
    if (!hdc) return JNI_FALSE;

    if (!wglMakeCurrent(hdc, s->hglrc)) {
        ReleaseDC(s->hwnd, hdc);
        return JNI_FALSE;
    }

    s->hdc = hdc;
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nSwapBuffers(JNIEnv*, jclass, jlong handle) {
    if (!handle) return;
    GlSurface* s = reinterpret_cast<GlSurface*>(handle);
    if (!s->hdc) return;
    SwapBuffers(s->hdc);
    release_frame_dc(s);
}

JNIEXPORT void JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nSetVsync(JNIEnv*, jclass, jlong handle, jboolean vsync) {
    if (!handle) return;
    auto swapInterval = reinterpret_cast<PFNWGLSWAPINTERVALEXT>(
        wglGetProcAddress("wglSwapIntervalEXT"));
    if (swapInterval) swapInterval(vsync ? 1 : 0);
}

JNIEXPORT void JNICALL
Java_dtm_stools_component_panels_graphics_gl_GlNative_nDestroyContext(JNIEnv*, jclass, jlong handle) {
    if (!handle) return;
    GlSurface* s = reinterpret_cast<GlSurface*>(handle);
    wglMakeCurrent(nullptr, nullptr);
    if (s->hglrc) wglDeleteContext(s->hglrc);
    if (s->hdc) ReleaseDC(s->hwnd, s->hdc);
    delete s;
}

}

