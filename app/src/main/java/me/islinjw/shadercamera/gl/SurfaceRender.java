package me.islinjw.shadercamera.gl;

import android.content.Context;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.view.Surface;

import me.islinjw.shadercamera.gl.shader.IShader;

public class SurfaceRender {
    private GLCore mGLCore;
    private EGLSurface mSurface;
    private IShader mShader;

    private int mWidth;
    private int mHeight;

    public SurfaceRender(GLCore glCore, Surface surface, int width, int height) {
        mGLCore = glCore;
        mSurface = mGLCore.createEGLSurface(surface);
        mWidth = width;
        mHeight = height;
    }

    public void setShader(Context context, IShader shader) {
        if (mShader != null) {
            mShader.onDetach(context, mGLCore);
        }
        mShader = shader;
        mGLCore.makeCurrent(mSurface);
        mShader.onAttach(context, mGLCore);

        GLES20.glClearColor(1, 0, 0, 1.0f);
        GLES20.glViewport(0, 0, mWidth, mHeight);
    }

    public void render(float[] transformMatrix) {
        render(transformMatrix, mSurface);
    }

    public void render(float[] transformMatrix, EGLSurface surface) {
        mGLCore.makeCurrent(surface);
        mShader.draw(mGLCore, transformMatrix, mGLCore.getTexture());
        mGLCore.swapBuffers(surface);
    }
}
