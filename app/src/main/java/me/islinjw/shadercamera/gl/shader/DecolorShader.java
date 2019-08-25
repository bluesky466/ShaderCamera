package me.islinjw.shadercamera.gl.shader;

public class DecolorShader extends ColorMatrixShader {
    private float[] DECOLOR_MATRIX = {
            0.299f, 0.587f, 0.114f, 0.0f,
            0.299f, 0.587f, 0.114f, 0.0f,
            0.299f, 0.587f, 0.114f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    protected float[] getColorMatrix() {
        return DECOLOR_MATRIX;
    }
}
