package com.e.rpist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Tank {

    private static final String vertexShaderCode =
            "precision mediump float;                \n" +
            "uniform   mat4 u_mvpMatrix;             \n" +
            "attribute vec4 a_position;              \n" +
            "attribute vec4 a_texPosition;           \n" +
            "varying   vec2 v_position;              \n" +
            "                                        \n" +
            "void main() {                           \n" +
            " gl_Position = u_mvpMatrix * a_position;\n" +
            " v_position  = a_texPosition.xy;        \n" +
            "}                                       \n";

    private static final String fragmentShaderCode =
            "precision mediump float;                            \n" +
            "uniform sampler2D u_texture;                        \n" +
            "varying vec2      v_position;                       \n" +
            "                                                    \n" +
            "void main() {                                       \n" +
            "    gl_FragColor = texture2D(u_texture, v_position);\n" +
            "}                                                   \n";

    private int   eglProgram;
    private int   positionHandle;
    private int   texUnitHandle;
    private int   texPositionHandle;
    private int   mvpMatrixHandle;
    private int   colorHandle;
    private float bodyColor[];


    private static final float[] tankTopCoords = {
            -0.10f, -0.10f, 0.05f,
             0.10f, -0.10f, 0.05f,
            -0.10f,  0.10f, 0.05f,
             0.10f,  0.10f, 0.05f,
    };

    private static final float textureCoords[] = {
            0, 1,
            1, 1,
            0, 0,
            1, 0,
    };
    
    private final int tankTopCoordsPerVertex = 3;
    private final int textureCoordsPerVertex = 2;
    private final int tankTopVertexCount     = tankTopCoords.length / tankTopCoordsPerVertex;

    private static final float[] tankBodyCoords = {
            // Bottom
            -0.10f, -0.10f, 0.00f,
             0.10f, -0.10f, 0.00f,
            -0.10f,  0.10f, 0.00f,
             0.10f,  0.10f, 0.00f,
            // Front
            -0.10f,  0.10f, 0.00f,
             0.10f,  0.10f, 0.00f,
            -0.10f,  0.10f, 0.05f,
             0.10f,  0.10f, 0.05f,
            // Back
            -0.10f, -0.10f, 0.00f,
             0.10f, -0.10f, 0.00f,
            -0.10f, -0.10f, 0.05f,
             0.10f, -0.10f, 0.05f,
            // Left
            -0.10f, -0.10f, 0.00f,
            -0.10f,  0.10f, 0.00f,
            -0.10f, -0.10f, 0.05f,
            -0.10f,  0.10f, 0.05f,
            // Right
             0.10f, -0.10f, 0.00f,
             0.10f,  0.10f, 0.00f,
             0.10f, -0.10f, 0.05f,
             0.10f,  0.10f, 0.05f,
    };

    private final int tankBodyCoordsPerVertex = 3;
    private final int tankBodyVertexCount     = tankBodyCoords.length / tankBodyCoordsPerVertex;

    private FloatBuffer tankTopVertexBuffer;
    private FloatBuffer textureVertexBuffer;
    private FloatBuffer tankBodyVertexBuffer;

    public Tank(Context applicationContext, float inputColor[]) {

        bodyColor = inputColor;

        int vertexShader   = Renderer.loadShader(GLES20.GL_VERTEX_SHADER  , vertexShaderCode  );
        int fragmentShader = Renderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        eglProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(eglProgram, vertexShader  );
        GLES20.glAttachShader(eglProgram, fragmentShader);
        GLES20.glLinkProgram (eglProgram);

        positionHandle    = GLES20.glGetAttribLocation (eglProgram, "a_position"   );
        colorHandle       = GLES20.glGetUniformLocation(eglProgram, "u_color"      );
        texUnitHandle     = GLES20.glGetUniformLocation(eglProgram, "u_texture"    );
        texPositionHandle = GLES20.glGetAttribLocation (eglProgram, "a_texPosition");
        mvpMatrixHandle   = GLES20.glGetUniformLocation(eglProgram, "u_mvpMatrix"  );

        tankTopVertexBuffer = ByteBuffer.allocateDirect(tankTopCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        tankTopVertexBuffer.put(tankTopCoords);
        tankTopVertexBuffer.position(0);

        textureVertexBuffer = ByteBuffer.allocateDirect(textureCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureVertexBuffer.put(textureCoords);
        textureVertexBuffer.position(0);

        tankBodyVertexBuffer = ByteBuffer.allocateDirect(tankBodyCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        tankBodyVertexBuffer.put(tankBodyCoords);
        tankBodyVertexBuffer.position(0);

        Bitmap bitmap = BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.tank_top);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, Renderer.textures[0]);
        GLUtils.texImage2D  (GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S    , GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T    , GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR       );
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR       );
    }

    public void draw(float[] mvpMatrix, boolean render2d) {

        GLES20.glUseProgram(eglProgram);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture  (GLES20.GL_TEXTURE_2D,Renderer.textures[0]);
        GLES20.glUniform1i    (texUnitHandle, 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer    (positionHandle, tankTopCoordsPerVertex, GLES20.GL_FLOAT, false, 0, tankTopVertexBuffer);

        GLES20.glEnableVertexAttribArray(texPositionHandle);
        GLES20.glVertexAttribPointer    (texPositionHandle, textureCoordsPerVertex, GLES20.GL_FLOAT, false, 0, textureVertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, tankTopVertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle   );
        GLES20.glDisableVertexAttribArray(texPositionHandle);

        if (render2d == false) {

            GLES20.glUniform4fv(colorHandle, 1, bodyColor, 0);

            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, tankBodyCoordsPerVertex, GLES20.GL_FLOAT, false, 0, tankBodyVertexBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, tankBodyVertexCount);

            GLES20.glDisableVertexAttribArray(positionHandle);
        }
    }
}