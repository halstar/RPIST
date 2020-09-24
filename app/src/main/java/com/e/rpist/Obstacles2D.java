package com.e.rpist;

import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Obstacles2D {

    private final String vertexShaderCode =
            "uniform   mat4 u_mvpMatrix;              \n" +
            "attribute vec4 a_position;               \n" +
            "attribute vec4 a_color;                  \n" +
            "varying   vec4 v_color;                  \n" +
            "                                         \n" +
            "void main() {                            \n" +
            "  v_color     = a_color;                 \n" +
            "  gl_Position = u_mvpMatrix * a_position;\n" +
            "}                                        \n";

    private final String fragmentShaderCode =
            "precision mediump float; \n" +
            "varying vec4 v_color;    \n" +
            "                         \n" +
            "void main() {            \n" +
            "  gl_FragColor = v_color;\n" +
            "}                        \n";

    private final int eglProgram;
    private int       positionHandle;
    private int       mvpMatrixHandle;
    private int       colorHandle;
    private float     colorSafe[], colorWarning[], colorDanger[];
    private float     fov;
    private Lock      lock;
    private int       secondLastSetAzimuth, lastSetAzimuth;

    private final int   coordsPerVertex    =   4;
    private final int   verticesPerAzimuth =   6;
    private final int   vertexCount        = 362 * verticesPerAzimuth;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;

    public Obstacles2D(float inputColorSafe[], float inputColorWarning[], float inputColorDanger[], float inputFov) {

        colorSafe    = inputColorSafe;
        colorWarning = inputColorWarning;
        colorDanger  = inputColorDanger;
        fov          = inputFov;
        lock         = new ReentrantLock();

        int vertexShader   = Renderer.loadShader(GLES20.GL_VERTEX_SHADER  , vertexShaderCode  );
        int fragmentShader = Renderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        eglProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(eglProgram, vertexShader  );
        GLES20.glAttachShader(eglProgram, fragmentShader);
        GLES20.glLinkProgram (eglProgram);

        positionHandle  = GLES20.glGetAttribLocation (eglProgram, "a_position" );
        colorHandle     = GLES20.glGetAttribLocation (eglProgram, "a_color"    );
        mvpMatrixHandle = GLES20.glGetUniformLocation(eglProgram, "u_mvpMatrix");

        vertexBuffer = ByteBuffer.allocateDirect(vertexCount * coordsPerVertex * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        colorBuffer  = ByteBuffer.allocateDirect(vertexCount * coordsPerVertex * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

        clear();
    }

    public void setPosition(int azimuth, float distance) {

        float[] modelMatrix   = new float[16];
        float[] coords        = new float[coordsPerVertex * verticesPerAzimuth];
        float[] rotatedCoords = new float[coordsPerVertex * verticesPerAzimuth];
        float   width         = (float)(2 * distance * Math.tan(Math.toRadians(fov / 2)));
        float[] color;

        if (distance > 0.80f) {
            color = colorSafe;
        } else if (distance > 0.35f) {
            color = colorWarning;
        } else {
            color = colorDanger;
        }

        // Upper part
        coords[0] = 0.0f    ; coords[1] = 0.0f        ; coords[2]  = 0.0f; coords[3]  = 1.0f;
        coords[4] = distance; coords[5] = 0.0f        ; coords[6]  = 0.0f; coords[7]  = 1.0f;
        coords[8] = distance; coords[9] = width / 2.0f; coords[10] = 0.0f; coords[11] = 1.0f;

        // Lower part
        coords[12] = 0.0f    ; coords[13] = 0.0f         ; coords[14] = 0.0f; coords[15] = 1.0f;
        coords[16] = distance; coords[17] = 0.0f         ; coords[18] = 0.0f; coords[19] = 1.0f;
        coords[20] = distance; coords[21] = -width / 2.0f; coords[22] = 0.0f; coords[23] = 1.0f;

        Matrix.setRotateM(modelMatrix, 0, azimuth, 0.0f, 0.0f, 1.0f);

        for (int i = 0; i < verticesPerAzimuth; i++) {
            Matrix.multiplyMV(rotatedCoords, i * coordsPerVertex, modelMatrix, 0, coords, i * coordsPerVertex);
        }

        lock.lock();

        for (int i = 0; i < verticesPerAzimuth; i++) {
            for (int j = 0; j < coordsPerVertex; j++) {
                vertexBuffer.put(azimuth * coordsPerVertex * verticesPerAzimuth + i * coordsPerVertex + j, rotatedCoords[i * coordsPerVertex + j]);
                colorBuffer.put (azimuth * coordsPerVertex * verticesPerAzimuth + i * coordsPerVertex + j, color[j]);
            }
        }

        secondLastSetAzimuth = lastSetAzimuth;
        lastSetAzimuth       = azimuth;

        lock.unlock();
    }

    public void clear() {

        lock.lock();

        for (int i = 0; i < vertexCount; i++) {
            for (int j = 0; j < coordsPerVertex; j++) {
                vertexBuffer.put(i * coordsPerVertex + j, 0.0f);
                colorBuffer.put (i * coordsPerVertex + j, 0.0f);
            }
        }
        vertexBuffer.position(0);
        colorBuffer.position (0);

        lock.unlock();
    }

    public void draw(float[] mvpMatrix) {

        GLES20.glUseProgram  (eglProgram);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        lock.lock();

        for (int i = 0; i < verticesPerAzimuth; i++) {
            vertexBuffer.put(secondLastSetAzimuth * coordsPerVertex * verticesPerAzimuth + i * coordsPerVertex + 3, 1.0f);
        }

        for (int i = 0; i < verticesPerAzimuth; i++) {
            vertexBuffer.put(lastSetAzimuth * coordsPerVertex * verticesPerAzimuth + i * coordsPerVertex + 3, 0.0f);
        }

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer    (positionHandle, coordsPerVertex, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer    (colorHandle, coordsPerVertex, GLES20.GL_FLOAT, false, 0, colorBuffer  );

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle   );

        lock.unlock();
    }
}