package com.e.rpist;

import android.opengl.GLES20;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Obstacles3D {

    private final String vertexShaderCode =
            "uniform   mat4 u_mvpMatrix;               \n" +
            "attribute vec4 a_position;                \n" +
            "                                          \n" +
            "void main() {                             \n" +
            "  gl_Position  = u_mvpMatrix * a_position;\n" +
            "  gl_PointSize = 5.0;                     \n" +
            "}                                         \n";

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
    private float     color[];
    private Lock      lock;

    private final int   coordsPerVertex = 3;
    private int         vertexCount     = 362 * 362;
    private FloatBuffer vertexBuffer;

    public Obstacles3D(float inputColor[]) {

        color = inputColor;
        lock  = new ReentrantLock();

        int vertexShader   = Renderer.loadShader(GLES20.GL_VERTEX_SHADER  , vertexShaderCode  );
        int fragmentShader = Renderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        eglProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(eglProgram, vertexShader  );
        GLES20.glAttachShader(eglProgram, fragmentShader);
        GLES20.glLinkProgram (eglProgram);

        positionHandle  = GLES20.glGetAttribLocation (eglProgram, "a_position" );
        colorHandle     = GLES20.glGetUniformLocation(eglProgram, "u_color"    );
        mvpMatrixHandle = GLES20.glGetUniformLocation(eglProgram, "u_mvpMatrix");

        vertexBuffer = ByteBuffer.allocateDirect(vertexCount * coordsPerVertex * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

        clear();
    }

    public void setPosition(int elevation, int azimuth, float distance) {

        float[] coords = new float[coordsPerVertex];

        coords[0] = (float)(distance * Math.cos(Math.toRadians(elevation)) * Math.cos(Math.toRadians(azimuth)));
        coords[1] = (float)(distance * Math.cos(Math.toRadians(elevation)) * Math.sin(Math.toRadians(azimuth)));
        coords[2] = (float)(distance * Math.sin(Math.toRadians(elevation)));

        lock.lock();

        for (int i = 0; i < coordsPerVertex; i++) {
            vertexBuffer.put(elevation * 360 * coordsPerVertex + azimuth * coordsPerVertex + i , coords[i]);
        }

        lock.unlock();
    }

    public void clear() {

        lock.lock();

        for (int i = 0; i < vertexCount; i++) {
            for (int j = 0; j < coordsPerVertex; j++) {
                vertexBuffer.put(i * coordsPerVertex + j, 0.0f);
            }
        }
        vertexBuffer.position(0);

        lock.unlock();
    }

    public void draw(float[] mvpMatrix) {

        GLES20.glUseProgram(eglProgram);

        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        lock.lock();

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer    (positionHandle, coordsPerVertex, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);

        lock.unlock();
    }
}