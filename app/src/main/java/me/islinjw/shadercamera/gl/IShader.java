package me.islinjw.shadercamera.gl;

import android.content.Context;

public interface IShader {
    void onAttach(Context context, GLCore core);

    void onDetach(Context context, GLCore core);

    void draw(GLCore core, float[] transformMatrix, int oesTexture);
}
