package me.islinjw.shadercamera.gl.shader;

import android.content.Context;

import me.islinjw.shadercamera.gl.GLCore;

public interface IShader {
    void onAttach(Context context, GLCore core);

    void onDetach(Context context, GLCore core);

    void draw(GLCore core, float[] transformMatrix, int oesTexture);
}
