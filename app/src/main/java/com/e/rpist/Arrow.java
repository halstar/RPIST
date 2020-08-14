package com.e.rpist;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Arrow {

    private final String vertexShaderCode =
            "uniform   mat4 u_mvpMatrix;              \n" +
            "attribute vec4 a_position;               \n" +
            "                                         \n" +
            "void main() {                            \n" +
            "  gl_Position = u_mvpMatrix * a_position;\n" +
            "}                                        \n";

    private final String fragmentShaderCode =
            "precision mediump float; \n" +
            "uniform vec4 u_color;    \n" +
            "                         \n" +
            "void main() {            \n" +
            "  gl_FragColor = u_color;\n" +
            "}                        \n";

    private final int eglProgram;
    private int       positionHandle;
    private int       mvpMatrixHandle;
    private int       colorHandle;
    private float     objectColor[];

    static float coords[] = {
             0.4f,  0.1f, 0.0f, // Top left swing
             0.5f,  0.0f, 0.0f, // Top
             0.0f,  0.0f, 0.0f, // Bottom
             0.5f,  0.0f, 0.0f, // Back to top
             0.4f, -0.1f, 0.0f  // Bottom right swing
    };
    private final int   coordsPerVertex = 3;
    private final int   vertexCount     = coords.length / coordsPerVertex;
    private FloatBuffer vertexBuffer;

    public Arrow(float color[]) {

        objectColor = color;

        int vertexShader   = Renderer.loadShader(GLES20.GL_VERTEX_SHADER  , vertexShaderCode  );
        int fragmentShader = Renderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        eglProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(eglProgram, vertexShader  );
        GLES20.glAttachShader(eglProgram, fragmentShader);
        GLES20.glLinkProgram (eglProgram);

        positionHandle  = GLES20.glGetAttribLocation (eglProgram, "a_position" );
        colorHandle     = GLES20.glGetUniformLocation(eglProgram, "u_color"    );
        mvpMatrixHandle = GLES20.glGetUniformLocation(eglProgram, "u_mvpMatrix");

        vertexBuffer = ByteBuffer.allocateDirect(coords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(coords);
        vertexBuffer.position(0);
    }

    public void draw(float[] mvpMatrix) {

        GLES20.glUseProgram(eglProgram);

        GLES20.glUniform4fv(colorHandle, 1, objectColor, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer    (positionHandle, coordsPerVertex, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glLineWidth(20.0f);

        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}