package es.valledecernego.game;

import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Requests the same RGBA8888 / GLES2 surface as before, but prefers a 24-bit
 * depth buffer to reduce z-fighting between overlapping character parts.
 * Devices without a 24-bit EGL config fall back to the previous 16-bit depth.
 */
public final class PreferredDepthConfigChooser implements GLSurfaceView.EGLConfigChooser {
    private static final int EGL_OPENGL_ES2_BIT = 4;

    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        for (int depthBits : DepthBufferPolicy.preferredDepthBits()) {
            int[] attributes = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, depthBits,
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL10.EGL_NONE
            };
            int[] count = new int[1];
            if (!egl.eglChooseConfig(display, attributes, null, 0, count) || count[0] <= 0) {
                continue;
            }
            EGLConfig[] configs = new EGLConfig[count[0]];
            if (egl.eglChooseConfig(display, attributes, configs, configs.length, count)
                    && count[0] > 0) {
                return configs[0];
            }
        }
        throw new IllegalArgumentException(
            "No compatible RGBA8888 GLES2 EGL config with 24-bit or 16-bit depth");
    }
}
