package com.e.rpist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ObstaclesLive {

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
    private float fov;
    private Lock  lock;

    private static final float textureCoords[] = {
            // Top
            0, 1,
            1, 1,
            0, 0,
            1, 0,
            // Bottom
            0, 1,
            1, 1,
            0, 0,
            1, 0,
            // Front
            0, 1,
            1, 1,
            0, 0,
            1, 0,
            // Back
            0, 1,
            1, 1,
            0, 0,
            1, 0,
            // Left
            0, 1,
            1, 1,
            0, 0,
            1, 0,
            // Right
            0, 1,
            1, 1,
            0, 0,
            1, 0,
    };

    private final int textureCoordsPerVertex = 2;

    private static final float[] obstacleCoords = {
            // Top
            -0.02f, -0.50f, 0.60f, 1.0f,
             0.02f, -0.50f, 0.60f, 1.0f,
            -0.02f,  0.50f, 0.60f, 1.0f,
             0.02f,  0.50f, 0.60f, 1.0f,
            // Bottom
            -0.02f, -0.50f, 0.00f, 1.0f,
             0.02f, -0.50f, 0.00f, 1.0f,
            -0.02f,  0.50f, 0.00f, 1.0f,
             0.02f,  0.50f, 0.00f, 1.0f,
            // Front
            -0.02f, 0.50f, 0.00f, 1.0f,
             0.02f, 0.50f, 0.00f, 1.0f,
            -0.02f, 0.50f, 0.60f, 1.0f,
             0.02f, 0.50f, 0.60f, 1.0f,
            // Back
            -0.02f, -0.50f, 0.00f, 1.0f,
             0.02f, -0.50f, 0.00f, 1.0f,
            -0.02f, -0.50f, 0.60f, 1.0f,
             0.02f, -0.50f, 0.60f, 1.0f,
            // Left
            -0.02f, -0.50f, 0.00f, 1.0f,
            -0.02f,  0.50f, 0.00f, 1.0f,
            -0.02f, -0.50f, 0.60f, 1.0f,
            -0.02f,  0.50f, 0.60f, 1.0f,
            // Right
             0.02f, -0.50f, 0.00f, 1.0f,
             0.02f,  0.50f, 0.00f, 1.0f,
             0.02f, -0.50f, 0.60f, 1.0f,
             0.02f,  0.50f, 0.60f, 1.0f,
    };

    private final int   obstacleCoordsPerVertex =   4;
    private final int   verticesPerAzimuth      = obstacleCoords.length / obstacleCoordsPerVertex;
    private final int   obstacleVertexCount     = 360 * verticesPerAzimuth;

    private FloatBuffer textureVertexBuffer;
    private FloatBuffer obstacleVertexBuffer;

    public ObstaclesLive(Context applicationContext, float inputFov) {

        fov  = inputFov;
        lock = new ReentrantLock();

        int vertexShader   = Renderer.loadShader(GLES20.GL_VERTEX_SHADER  , vertexShaderCode  );
        int fragmentShader = Renderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        eglProgram = GLES20.glCreateProgram();

        GLES20.glAttachShader(eglProgram, vertexShader  );
        GLES20.glAttachShader(eglProgram, fragmentShader);
        GLES20.glLinkProgram (eglProgram);

        positionHandle    = GLES20.glGetAttribLocation (eglProgram, "a_position"   );
        texPositionHandle = GLES20.glGetAttribLocation (eglProgram, "a_texPosition");
        texUnitHandle     = GLES20.glGetUniformLocation (eglProgram, "u_texture"   );
        mvpMatrixHandle   = GLES20.glGetUniformLocation(eglProgram, "u_mvpMatrix"  );

        textureVertexBuffer  = ByteBuffer.allocateDirect(obstacleVertexCount * textureCoordsPerVertex  * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        obstacleVertexBuffer = ByteBuffer.allocateDirect(obstacleVertexCount * obstacleCoordsPerVertex * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

        Bitmap bitmap = BitmapFactory.decodeResource(applicationContext.getResources(), R.drawable.obstacle);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, Renderer.textures[1]);
        GLUtils.texImage2D  (GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S    , GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T    , GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);

        for (int a = 0; a < 360; a++) {
            for (int i = 0; i < verticesPerAzimuth; i++) {
                for (int j = 0; j < textureCoordsPerVertex; j++) {
                    textureVertexBuffer.put(a * textureCoordsPerVertex * verticesPerAzimuth + i * textureCoordsPerVertex + j, textureCoords[i * textureCoordsPerVertex + j]);
                }
            }
        }

        clear();
    }

    public void setPosition(int azimuth, float distance) {

        float[] modelMatrix       = new float[16];
        float[] translationMatrix = new float[16];
        float[] rotationMatrix    = new float[16];
        float[] positionnedCoords = new float[obstacleCoordsPerVertex * verticesPerAzimuth];

        Matrix.setIdentityM(translationMatrix, 0);
        Matrix.translateM  (translationMatrix, 0, distance, 0.0f, 0.0f);
        Matrix.setRotateM  (rotationMatrix   , 0, azimuth, 0.0f, 0.0f, 1.0f);
        Matrix.multiplyMM  (modelMatrix      , 0, rotationMatrix, 0, translationMatrix, 0);

        for (int i = 0; i < verticesPerAzimuth; i++) {
            Matrix.multiplyMV(positionnedCoords, i * obstacleCoordsPerVertex, modelMatrix, 0, obstacleCoords, i * obstacleCoordsPerVertex);
        }

        lock.lock();

        for (int i = 0; i < verticesPerAzimuth; i++) {
            for (int j = 0; j < obstacleCoordsPerVertex; j++) {
                obstacleVertexBuffer.put(azimuth * obstacleCoordsPerVertex * verticesPerAzimuth + i * obstacleCoordsPerVertex + j, positionnedCoords[i * obstacleCoordsPerVertex + j]);
            }
        }

        lock.unlock();
    }

    public void clear() {

        lock.lock();

        for (int i = 0; i < obstacleVertexCount; i++) {
            for (int j = 0; j < obstacleCoordsPerVertex; j++) {
                obstacleVertexBuffer.put(i * obstacleCoordsPerVertex + j, 0.0f);
            }
        }
        obstacleVertexBuffer.position(0);

        lock.unlock();
    }

    public void draw(float[] mvpMatrix) {

        GLES20.glUseProgram(eglProgram);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture  (GLES20.GL_TEXTURE_2D, Renderer.textures[1]);
        GLES20.glUniform1i    (texUnitHandle, 1);

        lock.lock();

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer    (positionHandle, obstacleCoordsPerVertex, GLES20.GL_FLOAT, false, 0, obstacleVertexBuffer);

        GLES20.glEnableVertexAttribArray(texPositionHandle);
        GLES20.glVertexAttribPointer    (texPositionHandle, textureCoordsPerVertex, GLES20.GL_FLOAT, false, 0, textureVertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, obstacleVertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texPositionHandle);

        lock.unlock();
    }
}