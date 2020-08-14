package com.e.rpist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class SkyBox {

    private static final String vertexShaderCode =
            "#version 300 es                                                 \n" +
            "precision mediump float;                                        \n" +
            "uniform   mat4 u_viewMatrix;                                    \n" +
            "out       vec3 v_position;                                      \n" +
            "                                                                \n" +
            "void main() {                                                   \n" +
            "     const vec3 vertices[4] = vec3[4](vec3(-1.0f, -1.0f, 1.0f), \n"+
            "                                      vec3( 1.0f, -1.0f, 1.0f), \n"+
            "                                      vec3(-1.0f,  1.0f, 1.0f), \n"+
            "                                      vec3( 1.0f,  1.0f, 1.0f));\n"+
            "    v_position  = mat3(u_viewMatrix) * vertices[gl_VertexID];   \n"+
            "    gl_Position = vec4(vertices[gl_VertexID], 1.0f);            \n" +
            "}                                                               \n";

    private static final String fragmentShaderCode =
            "#version 300 es\n" +
            "precision mediump  float;                  \n" +
            "uniform samplerCube u_texture;             \n" +
            "in      vec3        v_position;            \n" +
            "out     vec4        color;                 \n" +
            "                                           \n" +
            "void main() {                              \n" +
            "    color = texture(u_texture, v_position);\n" +
            "}                                          \n";

    private int eglProgram;
    private int texUnitHandle;
    private int viewMatrixHandle;

    public SkyBox(Context applicationContext) {

        int vertexShader   = Renderer.loadShader(GLES20.GL_VERTEX_SHADER  , vertexShaderCode  );
        int fragmentShader = Renderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        eglProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(eglProgram, vertexShader  );
        GLES20.glAttachShader(eglProgram, fragmentShader);
        GLES20.glLinkProgram (eglProgram);

        texUnitHandle    = GLES20.glGetUniformLocation(eglProgram, "u_texture"   );
        viewMatrixHandle = GLES20.glGetUniformLocation(eglProgram, "u_viewMatrix");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, Renderer.textures[2]);

        Bitmap bitmap = BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.skybox_right);
        GLUtils.texImage2D  (GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, bitmap, 0);
        bitmap.recycle();

        bitmap = BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.skybox_left);
        GLUtils.texImage2D  (GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, bitmap, 0);
        bitmap.recycle();

        bitmap = BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.skybox_front);
        GLUtils.texImage2D  (GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, bitmap, 0);
        bitmap.recycle();

        bitmap = BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.skybox_back);
        GLUtils.texImage2D  (GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, bitmap, 0);
        bitmap.recycle();

        bitmap = BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.skybox_bottom);
        GLUtils.texImage2D  (GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, bitmap, 0);
        bitmap.recycle();

        bitmap = BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.skybox_top);
        GLUtils.texImage2D  (GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, bitmap, 0);
        bitmap.recycle();

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_S    , GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_WRAP_T    , GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR       );
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR       );
    }

    public void draw(float[] viewMatrix) {

        GLES20.glUseProgram(eglProgram);

        GLES20.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLES20.glBindTexture  (GLES20.GL_TEXTURE_2D,Renderer.textures[2]);
//        GLES20.glUniform1i    (texUnitHandle, 2);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}