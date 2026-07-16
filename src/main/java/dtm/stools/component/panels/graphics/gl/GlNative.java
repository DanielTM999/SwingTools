package dtm.stools.component.panels.graphics.gl;

import java.awt.*;

final class GlNative {

    static {
        GlNativeLoader.load();
    }

    private GlNative() {}

    static native long nCreateContext(Canvas canvas);
    static native boolean nMakeCurrent(long handle);
    static native void nSwapBuffers(long handle);
    static native void nSetVsync(long handle, boolean vsync);
    static native void nDestroyContext(long handle);
}
