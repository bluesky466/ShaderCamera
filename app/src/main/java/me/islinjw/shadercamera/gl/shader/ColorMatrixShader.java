package me.islinjw.shadercamera.gl.shader;

import android.content.Context;
import android.content.res.AssetManager;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import me.islinjw.shadercamera.CommonUtils;
import me.islinjw.shadercamera.gl.GLCore;

public abstract class ColorMatrixShader implements IShader {
    private static final String VERTICES_SHADER = "color_matrix_shader.vert.glsl";
    private static final String FRAGMENT_SHADER = "color_matrix_shader.frag.glsl";

    float[] VERTICES = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f
    };

    private float[] TEXTURE_COORDS = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };

    short[] ORDERS = {
            0, 1, 2,
            2, 3, 0
    };

    private int mProgram;

    private FloatBuffer mVertices;
    private FloatBuffer mCoords;
    private ShortBuffer mOrder;

    private int mPositionId;
    private int mCoordId;
    private int mTexPreviewId;
    private int mTransformMatrixId;
    private int mColorMatrixId;

    @Override
    public void onAttach(Context context, GLCore core) {
        AssetManager asset = context.getAssets();

        try {
            mProgram = core.createProgram(asset.open(VERTICES_SHADER), asset.open(FRAGMENT_SHADER));
        } catch (IOException e) {
            throw new RuntimeException("can't open " + VERTICES_SHADER, e);
        }
        GLES20.glUseProgram(mProgram);

        mVertices = CommonUtils.toFloatBuffer(VERTICES);
        mCoords = CommonUtils.toFloatBuffer(TEXTURE_COORDS);
        mOrder = CommonUtils.toShortBuffer(ORDERS);
        mPositionId = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mCoordId = GLES20.glGetAttribLocation(mProgram, "vCoord");
        mColorMatrixId = GLES20.glGetUniformLocation(mProgram, "uColorMatrix");
        mTexPreviewId = GLES20.glGetUniformLocation(mProgram, "texPreview");
        mTransformMatrixId = GLES20.glGetUniformLocation(mProgram, "matTransform");
    }

    @Override
    public void onDetach(Context context, GLCore core) {
        core.deleteProgram(mProgram);
        mProgram = -1;
    }

    @Override
    public void draw(GLCore core, float[] transformMatrix, int oesTexture) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_SAMPLER_EXTERNAL_OES, oesTexture);
        GLES20.glUniform1i(mTexPreviewId, 0);

        GLES20.glVertexAttribPointer(mPositionId, 2, GLES20.GL_FLOAT, false, 0, mVertices);
        GLES20.glEnableVertexAttribArray(mPositionId);

        GLES20.glVertexAttribPointer(mCoordId, 2, GLES20.GL_FLOAT, false, 0, mCoords);
        GLES20.glEnableVertexAttribArray(mCoordId);

        GLES20.glUniformMatrix4fv(mColorMatrixId, 1, true, getColorMatrix(), 0);
        GLES20.glUniformMatrix4fv(mTransformMatrixId, 1, false, transformMatrix, 0);

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, ORDERS.length, GLES20.GL_UNSIGNED_SHORT, mOrder);
    }

    protected abstract float[] getColorMatrix();
}
