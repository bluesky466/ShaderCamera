package me.islinjw.shadercamera.gl.shader;

public class ReversalShader extends ColorMatrixShader {
    private float[] DECOLOR_MATRIX = {
            -1.0f, 0.0f, 0.0f, 1.0f,
            0.0f, -1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, -1.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    protected float[] getColorMatrix() {
        return DECOLOR_MATRIX;
    }
}
