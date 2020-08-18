package com.e.rpist;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.TextureView;
import android.opengl.EGLSurface;

class Renderer extends Thread implements TextureView.SurfaceTextureListener {

    private static final float clearColor2d   [] = {0.20f, 0.20f, 0.20f, 1.0f};
    private static final float clearColor3d   [] = {0.31f, 0.40f, 0.49f, 1.0f};
    private static final float clearColorLive [] = {0.60f, 0.78f, 0.95f, 1.0f};
    private static final float obstacleColor2d[] = {0.37f, 0.37f, 0.29f, 1.0f};
    private static final float obstacleColor3d[] = {1.00f, 1.00f, 1.00f, 1.0f};
    private static final float xArrowColor    [] = {1.00f, 0.00f, 0.00f, 1.0f};
    private static final float yArrowColor    [] = {0.00f, 1.00f, 0.00f, 1.0f};
    private static final float zArrowColor    [] = {0.00f, 0.00f, 1.00f, 1.0f};
    private static final float tankBodyColor  [] = {0.00f, 0.00f, 1.00f, 1.0f};
    private static final float groundColor    [] = {0.37f, 0.37f, 0.29f, 1.0f};

    private static final float obstacleFov = 5.0f;

    private Object                   lock = new Object();
    private SurfaceTexture           surfaceTexture;
    private EglCore                  eglCore;
    private ControlRobot.DisplayMode mode;

    private final float[] modelMatrix      = new float[16];
    private final float[] viewMatrix       = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] vpMatrix         = new float[16];
    private final float[] mvpMatrix        = new float[16];

    private Obstacles2D   obstacles2D;
    private Obstacles3D   obstacles3D;
    private ObstaclesLive obstaclesLive;
    private Arrow         xArrow;
    private Arrow         yArrow;
    private Arrow         zArrow;
    private Tank          tank;
    private Ground        ground;
    private SkyBox        skyBox;

    public volatile float xPos          = 0.0f;
    public volatile float yPos          = 0.0f;
    public volatile float scalingFactor = 1.0f;

    public static int[] textures = new int[3];

    Context context;

    public Renderer(Context applicationContext) {
        super("TextureView Renderer");
        context = applicationContext;
    }

    public void setMode(ControlRobot.DisplayMode displayMode) {
        mode = displayMode;
    }

    public float getXPos() {
        return xPos;
    }

    public void setXPos(float xPosIn) {
        xPos = xPosIn;
    }

    public float getYPos() {
        return yPos;
    }

    public void setYPos(float yPosIn) {
        yPos = yPosIn;
    }

    public void setScalingFactor(float factor) {
        scalingFactor = factor;
    }

    public void setData(int elevation, int azimuth, float distance) {

        if (mode == ControlRobot.DisplayMode.SCAN_2D) {
            if (obstacles2D != null) {
                obstacles2D.setPosition(azimuth, distance);
            }
        } else if (mode == ControlRobot.DisplayMode.SCAN_3D) {
            if (obstacles3D != null) {
                obstacles3D.setPosition(elevation, azimuth, distance);
            }
        } else {
            if (obstaclesLive != null) {
                obstaclesLive.setPosition(azimuth, distance);
            }
        }
    }

    public void clearData() {

        if (obstacles2D != null) {
          obstacles2D.clear();
        }

        if (obstacles3D != null) {
            obstacles3D.clear();
        }

        if (obstaclesLive != null) {
            obstaclesLive.clear();
        }

        if (mode == ControlRobot.DisplayMode.SCAN_2D) {
            xPos          = 0.0f;
            yPos          = 0.5f;
            scalingFactor = 0.6f;
        } else if (mode == ControlRobot.DisplayMode.SCAN_3D){
            xPos          =  0.0f;
            yPos          = -1.15f;
            scalingFactor =  0.9f;
        } else {
            xPos          =  0.0f;
            yPos          = -0.6f;
            scalingFactor =  1.1f;
        }
    }

    public static int loadShader(int type, String shaderCode){

        int shaderId = GLES20.glCreateShader(type);

        GLES20.glShaderSource (shaderId, shaderCode);
        GLES20.glCompileShader(shaderId);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            String info = GLES20.glGetShaderInfoLog(shaderId);
            GLES20.glDeleteShader(shaderId);
            throw new RuntimeException("Could not compile shader " + type + ": " + info);
        }

        return shaderId;
    }

    @Override
    public void run() {
        while (true) {

            synchronized(lock) {
                while (surfaceTexture == null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                }
            }

            eglCore = new EglCore(null, EglCore.FLAG_TRY_GLES3);

            EGLSurface windowSurface = eglCore.createWindowSurface(surfaceTexture);

            eglCore.makeCurrent(windowSurface);

            GLES20.glGenTextures(3 ,textures, 0);

            xArrow = new Arrow(xArrowColor);
            yArrow = new Arrow(yArrowColor);
            zArrow = new Arrow(zArrowColor);

            obstacles2D   = new Obstacles2D  (obstacleColor2d, obstacleFov);
            obstacles3D   = new Obstacles3D  (obstacleColor3d);
            obstaclesLive = new ObstaclesLive(context, obstacleFov);

            tank   = new Tank  (context, tankBodyColor);
            ground = new Ground(groundColor);
            skyBox = new SkyBox(context);

            draw(windowSurface);

            eglCore.releaseSurface(windowSurface);
            eglCore.release();

            if (surfaceTexture != null) {
                surfaceTexture.release();
            }
        }
    }

    private void draw(EGLSurface eglSurface) {

        GLES20.glEnable   (GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL    );
        GLES20.glDepthMask(true);

        while (true) {
            synchronized(lock) {
                if (surfaceTexture == null) {
                    return;
                }
            }

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            float clearColor[];

            if (mode == ControlRobot.DisplayMode.SCAN_2D) {
                clearColor = clearColor2d;
            } else if (mode == ControlRobot.DisplayMode.SCAN_3D){
                clearColor = clearColor3d;
            } else {
                clearColor = clearColorLive;
            }

            GLES20.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);

            // Set the camera position (View matrix)
            if (mode == ControlRobot.DisplayMode.SCAN_2D) {
                Matrix.setLookAtM(viewMatrix, 0, xPos, yPos, scalingFactor, xPos, yPos, 0f, 0f, 1.0f, 0.0f);
            } else if (mode == ControlRobot.DisplayMode.SCAN_3D){
                Matrix.setLookAtM(viewMatrix, 0, xPos, yPos, scalingFactor, 0f, 2.0f, 2f, 0f, 0.0f, 1.0f);
            } else {
                Matrix.setLookAtM(viewMatrix, 0, xPos, yPos, scalingFactor, 0f, 2.0f, 0f, 0f, 0.0f, 1.0f);
            }

            // Log.d("RPIST", "X: " + xPos + " Y: " + yPos + " Z: " + scalingFactor);

            // Calculate the projection and view transformation
            Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

            if (mode == ControlRobot.DisplayMode.SCAN_2D) {
                obstacles2D.draw(vpMatrix);
            } else if (mode == ControlRobot.DisplayMode.SCAN_3D){
                obstacles3D.draw(vpMatrix);
            } else {
                obstaclesLive.draw(vpMatrix);
            }

            if (mode == ControlRobot.DisplayMode.SCAN_2D) {
                tank.draw(vpMatrix, false);
            } else {
                tank.draw  (vpMatrix, false);
                ground.draw(vpMatrix);
                skyBox.draw(viewMatrix);
            }

            xArrow.draw(vpMatrix);

            Matrix.setRotateM(modelMatrix, 0, 90.0f, 0.0f, 0.0f, 1.0f);
            Matrix.multiplyMM(mvpMatrix  , 0, vpMatrix, 0, modelMatrix, 0);

            yArrow.draw(mvpMatrix);

            if (mode != ControlRobot.DisplayMode.SCAN_2D) {
                Matrix.setRotateM(modelMatrix, 0, -90.0f, 0.0f, 1.0f, 0.0f);
                Matrix.multiplyMM(mvpMatrix  , 0, vpMatrix, 0, modelMatrix, 0);

                zArrow.draw(mvpMatrix);
            }

            eglCore.swapBuffers(eglSurface);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
        synchronized(lock) {
            surfaceTexture = st;
            lock.notify();

            float ratio = (float) width / height;

            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 0.5f, 100.0f);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
        synchronized(lock) {
            surfaceTexture = null;
        }

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture st) {
        // Nothing to do
    }
}
