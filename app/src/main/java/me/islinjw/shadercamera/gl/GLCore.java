package me.islinjw.shadercamera.gl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.view.Surface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GLCore {
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EGLConfig mEGLConfig;

    private int mGLTextureId = -1;

    public void init() {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        if (!EGL14.eglInitialize(mEGLDisplay, null, 0, null, 0)) {
            throw new RuntimeException("eglInitialize failed");
        }

        mEGLConfig = chooseEglConfig(mEGLDisplay);
        mEGLContext = createEglContext(mEGLDisplay, mEGLConfig);
        if (mEGLContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("eglCreateContext failed");
        }
    }

    private EGLConfig chooseEglConfig(EGLDisplay display) {
        int[] attribList = {
                EGL14.EGL_BUFFER_SIZE, 32,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(
                display,
                attribList,
                0,
                configs,
                0,
                configs.length,
                numConfigs,
                0)) {
            throw new RuntimeException("eglChooseConfig failed");
        }
        return configs[0];
    }

    private EGLContext createEglContext(EGLDisplay display, EGLConfig config) {
        int[] contextList = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        return EGL14.eglCreateContext(
                display,
                config,
                EGL14.EGL_NO_CONTEXT,
                contextList,
                0);
    }

    public EGLSurface createEGLSurface(Surface surface) {
        int[] attribList = {
                EGL14.EGL_NONE
        };
        return EGL14.eglCreateWindowSurface(
                mEGLDisplay,
                mEGLConfig,
                surface,
                attribList,
                0);
    }

    public void makeCurrent(EGLSurface eglSurface) {
        EGL14.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext);
    }

    public void swapBuffers(EGLSurface eglSurface) {
        EGL14.eglSwapBuffers(mEGLDisplay, eglSurface);
    }

    public int createProgram(InputStream vShaderSource, InputStream fShaderSource) {
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, loadShader(GLES20.GL_VERTEX_SHADER, vShaderSource));
        GLES20.glAttachShader(program, loadShader(GLES20.GL_FRAGMENT_SHADER, fShaderSource));
        GLES20.glLinkProgram(program);

        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("link program failed : " + log);
        }
        return program;
    }

    public void deleteProgram(int program) {
        GLES20.glDeleteProgram(program);
    }

    public int loadShader(int shaderType, InputStream source) {
        String sourceStr;
        try {
            sourceStr = readStringFromStream(source);
        } catch (IOException e) {
            throw new RuntimeException("read shaderType " + shaderType + " source failed", e);
        }

        int shader = GLES20.glCreateShader(shaderType);
        GLES20.glShaderSource(shader, sourceStr);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("create shaderType " + shaderType + " failed : " + log);
        }
        return shader;
    }

    public String readStringFromStream(InputStream input) throws IOException {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line = reader.readLine();
        while (line != null) {
            builder.append(line)
                    .append("\n");
            line = reader.readLine();
        }
        return builder.toString();
    }

    public int getTexture() {
        if (mGLTextureId == -1) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            mGLTextureId = textures[0];
        }

        return mGLTextureId;
    }
}
