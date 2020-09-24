package com.e.rpist;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class ControlRobot extends AppCompatActivity implements SensorEventListener {

    public final  static String EXTRA_ROBOT_ADDRESS = "com.e.rpist.robot.address";
    public final  static String EXTRA_CAMERA_PORT   = "com.e.rpist.camera.port"  ;
    public final  static String EXTRA_COMMANDS_PORT = "com.e.rpist.commands.port";
    public final  static String EXTRA_DATA_PORT     = "com.e.rpist.data.port"    ;

    private final static String forward1String            = "F1";
    private final static String forward2String            = "F2";
    private final static String backward1String           = "B1";
    private final static String backward2String           = "B2";
    private final static String left1String               = "L1";
    private final static String left2String                = "L2";
    private final static String right1String              = "R1";
    private final static String right2String              = "R2";
    private final static String stopString                = "S";
    private final static String modeUserControlString     = "MU";
    private final static String modeAvoidObstaclesString  = "MO";
    private final static String modeFollowLineString      = "ML";
    private final static String modeFollowCorridorString  = "MC";
    private final static String modeAlongObstacleString   = "MA";
    private final static String controlDirectionsString   = "CD";
    private final static String controlJoystickString     = "CJ";
    private final static String controlSensorsString      = "CS";
    private final static String displayOffString          = "DO";
    private final static String displayCameraString       = "DC";
    private final static String display2dString           = "D2";
    private final static String display3dString           = "D3";
    private final static String live3dString              = "L3";
    private final static String followCorridorUTurnString = "UT";

    private enum Mode {
        USER_CONTROL,
        AVOID_OBSTACLES,
        FOLLOW_LINE,
        FOLLOW_CORRIDOR,
        ALONG_OBSTACLE,
    };

    private enum UserControl {
        DIRECTIONS,
        JOYSTICK,
        SENSORS,
    };

    public enum DisplayMode {
        OFF,
        CAMERA,
        SCAN_2D,
        SCAN_3D,
        LIVE_3D
    };

    private Socket                commandsServerSocket;
    private ServerSocket          dataReceiverServerSocket;
    private Socket                dataReceiverSocket;
    private SensorManager         sensorManager;
    private long                  lastEventUpdate;
    private boolean               isCommandsServerConnected = false;
    private boolean               isDataReceiverStarted     = false;
    private boolean               isDataReceiverConnected   = false;
    private String                robotAddress              = "192.168.0.17";
    private String                cameraPort                = "8000";
    private String                commandsPort              = "12345";
    private String                dataPort                  = "54321";
    private Queue<String>         commandsQueue             = new LinkedList<>();
    private Semaphore             commandsQueueLock         = new Semaphore(1);
    private ConnectCommandsServer connectTask;
    private SendCommandsToServer  sendingTask;
    private DisconnectSockets     disconnectTask;
    private DataReceiver          receiverTask;
    private int                   joystickLayoutWidth, joystickLayoutHeight, joystickXRef, joystickYRef, joystickWidth, joystickHeight;
    private float                 joystickXRatio, joystickYRatio;
    private ConstraintLayout      directionsLayout, joystickLayout;
    private SeekBar               modeSwitch, userControlSwitch, displaySwitch;
    private ImageButton           forward1, forward2, backward1, backward2, stop, left1, left2, right1, right2, joystick;
    private ImageView             imageView;
    private WebView               cameraView;
    private TextureView           textureView;
    private float                 previousX;
    private float                 previousY;
    private ScaleGestureDetector  scaleDetector;
    private Renderer              renderer;
    private float                 scalingFactor  = 1.0f;
    private Mode                  currentMode    = Mode.USER_CONTROL;
    private UserControl           currentControl = UserControl.DIRECTIONS;
    private DisplayMode           currentDisplay = DisplayMode.OFF;

    private void enableAll() {
        modeSwitch.setEnabled       (true);
        userControlSwitch.setEnabled(true);
        displaySwitch.setEnabled    (true);
        joystickLayout.setEnabled   (true);
        directionsLayout.setEnabled (true);
        stop.setEnabled             (true);
    }

    private void disableAll() {
        modeSwitch.setEnabled       (false);
        userControlSwitch.setEnabled(false);
        displaySwitch.setEnabled    (false);
        directionsLayout.setEnabled (false);
        joystickLayout.setEnabled   (false);
        stop.setEnabled             (false);
        stop.setEnabled             (false);
    }

    private void setDefaultConfiguration() {

        queueCommand(stopString);

        queueCommand(modeUserControlString);
        modeSwitch.setProgress(0);
        currentMode = Mode.USER_CONTROL;

        queueCommand(controlDirectionsString);
        directionsLayout.setVisibility(View.VISIBLE  );
        joystickLayout.setVisibility  (View.INVISIBLE);
        joystick.setX(joystickXRef);
        joystick.setY(joystickYRef);
        userControlSwitch.setProgress(2);
        currentControl = UserControl.DIRECTIONS;

        queueCommand(displayOffString);
        displaySwitch.setProgress(4);
        currentDisplay = DisplayMode.OFF;
        cameraView.loadUrl("about:blank");
        imageView.setVisibility  (View.VISIBLE  );
        cameraView.setVisibility (View.INVISIBLE);
        textureView.setVisibility(View.INVISIBLE);
    }

    private void showMsgOnUi(final String str) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void restartControl() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isCommandsServerConnected = false;
                isDataReceiverStarted     = false;
                isDataReceiverConnected   = false;
                connectTask.cancel (true);
                sendingTask.cancel (true);
                receiverTask.cancel(true);
                recreate();
            }
        });
    }

    private void queueCommand(String command) {

        if (isCommandsServerConnected == true) {

            try {
                commandsQueueLock.acquire();
            } catch (InterruptedException e) {
                // Nothing to do
            }
            commandsQueue.add(command);
            commandsQueueLock.release();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            scalingFactor /= detector.getScaleFactor();
            scalingFactor  = Math.max(0.55f, Math.min(scalingFactor, 10.0f));

            renderer.setScalingFactor(scalingFactor);

            return true;
        }
    }

    private void startCameraDisplayMode() {

        queueCommand(displayCameraString);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Nothing to do
        }
        cameraView.loadUrl("http://" + robotAddress + ":" + cameraPort);
        imageView.setVisibility  (View.INVISIBLE);
        cameraView.setVisibility (View.VISIBLE  );
        textureView.setVisibility(View.INVISIBLE);
    }

    private void startLive3dDisplayMode() {

        queueCommand(stopString  );
        queueCommand(live3dString);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // Nothing to do
        }
        cameraView.loadUrl("about:blank");
        imageView.setVisibility  (View.INVISIBLE);
        cameraView.setVisibility (View.INVISIBLE);
        textureView.setVisibility(View.VISIBLE  );
        renderer.setMode(DisplayMode.LIVE_3D);
        renderer.clearData();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_robot);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);

        directionsLayout  = findViewById(R.id.directionsLayout   );
        joystickLayout    = findViewById(R.id.joystickLayout     );
        modeSwitch        = findViewById(R.id.mode_switch        );
        userControlSwitch = findViewById(R.id.user_control_switch);
        displaySwitch     = findViewById(R.id.display_mode_switch);
        joystick          = findViewById(R.id.joystick           );
        forward1          = findViewById(R.id.forward1           );
        forward2          = findViewById(R.id.forward2           );
        backward1         = findViewById(R.id.backward1          );
        backward2         = findViewById(R.id.backward2          );
        left1             = findViewById(R.id.left1              );
        left2             = findViewById(R.id.left2              );
        right1            = findViewById(R.id.right1             );
        right2            = findViewById(R.id.right2             );
        stop              = findViewById(R.id.stop               );
        imageView         = findViewById(R.id.image_view         );
        cameraView        = findViewById(R.id.web_view           );
        textureView       = findViewById(R.id.texture_view       );

        scaleDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleListener());

        renderer = new Renderer(getApplicationContext());
        renderer.start();

        cameraView.setWebViewClient(new WebViewClient());
        textureView.setSurfaceTextureListener(renderer);

        joystickLayoutWidth  = getResources().getDimensionPixelSize(R.dimen.control_area_width );
        joystickLayoutHeight = getResources().getDimensionPixelSize(R.dimen.control_area_height);

        joystickWidth  = ((BitmapDrawable)getResources().getDrawable(R.drawable.joystick)).getBitmap().getWidth ();
        joystickHeight = ((BitmapDrawable)getResources().getDrawable(R.drawable.joystick)).getBitmap().getHeight();

        joystickXRef = joystickLayoutWidth  / 2 - joystickWidth  / 2;
        joystickYRef = joystickLayoutHeight / 2 - joystickHeight / 2;

        joystickXRatio = joystickLayoutWidth  / 180;
        joystickYRatio = joystickLayoutHeight / 180;

        joystickLayout.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent event) {

                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        joystick.setVisibility(View.INVISIBLE);
                        lastEventUpdate = System.currentTimeMillis();
                        break;

                    case DragEvent.ACTION_DRAG_LOCATION:
                        long   actualTime = System.currentTimeMillis();
                        String roll;
                        String pitch;

                        int xPos = (int)event.getX() - joystickWidth  / 2 - joystickXRef;
                        int yPos = (int)event.getY() - joystickHeight / 2 - joystickYRef;

                        xPos /= joystickXRatio;
                        yPos /= joystickYRatio;

                        if (actualTime - lastEventUpdate > 50) {
                            lastEventUpdate = actualTime;

                            roll  = Integer.toString(-xPos);
                            pitch = Integer.toString(-yPos);

                            queueCommand("0:" + roll + ":" + pitch);
                        }

                        break;

                    case DragEvent.ACTION_DRAG_ENDED:
                        joystick.setVisibility(View.VISIBLE);
                        queueCommand(stopString);
                        break;

                    case DragEvent.ACTION_DRAG_ENTERED:
                    case DragEvent.ACTION_DRAG_EXITED :
                    case DragEvent.ACTION_DROP:
                    default:
                        // Nothing to do
                        break;
                }
                return true;
            }
        });

        joystick.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (currentMode       == Mode.USER_CONTROL
                 && currentControl    == UserControl.JOYSTICK
                 && event.getAction() == MotionEvent.ACTION_DOWN) {

                    ClipData data = ClipData.newPlainText("", "");
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(joystick);

                    joystick.startDrag(data, shadowBuilder, joystick, 0);
                    return true;
                } else {
                    return false;
                }
            }
        });

        modeSwitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Nothing to do
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Nothing to do
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                int  progress = seekBar.getProgress();
                Mode requestedMode;

                switch (progress) {
                    case 0:
                        requestedMode = Mode.USER_CONTROL;
                        break;
                    case 1:
                        requestedMode = Mode.AVOID_OBSTACLES;
                        break;
                    case 2:
                        requestedMode = Mode.ALONG_OBSTACLE;
                        break;
                    case 3:
                        requestedMode = Mode.FOLLOW_CORRIDOR;
                        break;
                    case 4:
                        requestedMode = Mode.FOLLOW_LINE;
                        break;
                    default:
                        requestedMode = currentMode;
                        break;
                }

                if (requestedMode != currentMode) {

                    queueCommand(stopString);

                    switch (requestedMode) {
                        case USER_CONTROL:
                            queueCommand(modeUserControlString);
                            userControlSwitch.setEnabled(true);
                            directionsLayout.setEnabled (true);
                            joystickLayout.setEnabled   (true);
                            displaySwitch.setEnabled    (true);
                             break;
                        case AVOID_OBSTACLES:
                            queueCommand(modeAvoidObstaclesString);
                            userControlSwitch.setEnabled(false);
                            directionsLayout.setEnabled (false);
                            joystickLayout.setEnabled   (false);
                            displaySwitch.setEnabled    (true );
                            displaySwitch.setProgress   (2);
                            startLive3dDisplayMode();
                            currentDisplay = DisplayMode.LIVE_3D;
                            break;
                        case ALONG_OBSTACLE:
                            queueCommand(modeAlongObstacleString);
                            userControlSwitch.setEnabled(false);
                            directionsLayout.setEnabled (false);
                            joystickLayout.setEnabled   (false);
                            displaySwitch.setEnabled    (true );
                            displaySwitch.setProgress   (2);
                            startLive3dDisplayMode();
                            currentDisplay = DisplayMode.LIVE_3D;
                            break;
                        case FOLLOW_CORRIDOR:
                            queueCommand(modeFollowCorridorString);
                            userControlSwitch.setEnabled(false);
                            directionsLayout.setEnabled (false);
                            joystickLayout.setEnabled   (false);
                            displaySwitch.setEnabled    (true );
                            displaySwitch.setProgress   (2);
                            startLive3dDisplayMode();
                            currentDisplay = DisplayMode.LIVE_3D;
                            break;
                        case FOLLOW_LINE:
                            queueCommand(modeFollowLineString);
                            userControlSwitch.setEnabled(false);
                            directionsLayout.setEnabled (false);
                            joystickLayout.setEnabled   (false);
                            displaySwitch.setEnabled    (false);
                            displaySwitch.setProgress   (3);
                            startCameraDisplayMode();
                            currentDisplay = DisplayMode.CAMERA;
                            break;

                        default:
                            // Nothing to do
                            break;
                    }

                    currentMode = requestedMode;
                } else if (requestedMode == Mode.FOLLOW_CORRIDOR) {
                    queueCommand(followCorridorUTurnString);
                }
            }
        });

        userControlSwitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Nothing to do
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Nothing to do
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                int         progress = seekBar.getProgress();
                UserControl requestedControl;

                switch (progress) {
                    case 0:
                        requestedControl = UserControl.SENSORS;
                        break;
                    case 1:
                        requestedControl = UserControl.JOYSTICK;
                        break;
                    case 2:
                        requestedControl = UserControl.DIRECTIONS;
                        break;
                    default:
                        requestedControl = currentControl;
                        break;
                }

                if (requestedControl != currentControl) {

                    queueCommand(stopString);

                    switch (requestedControl) {
                        case DIRECTIONS:
                            queueCommand(controlDirectionsString);
                            directionsLayout.setVisibility(View.VISIBLE  );
                            joystickLayout.setVisibility  (View.INVISIBLE);
                            break;
                        case JOYSTICK:
                            queueCommand(controlJoystickString);
                            directionsLayout.setVisibility(View.INVISIBLE);
                            joystickLayout.setVisibility  (View.VISIBLE  );
                            joystick.setX                 (joystickXRef  );
                            joystick.setY                 (joystickYRef  );
                            break;
                        case SENSORS:
                            queueCommand(controlSensorsString);
                            directionsLayout.setVisibility(View.INVISIBLE);
                            joystickLayout.setVisibility  (View.VISIBLE  );
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            break;
                        default:
                            // Nothing to do
                            break;
                    }

                    currentControl = requestedControl;
                }
            }
        });

        displaySwitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Nothing to do
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Nothing to do
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                int         progress = seekBar.getProgress();
                DisplayMode requestedDisplay;

                switch (progress) {
                    case 0:
                        requestedDisplay = DisplayMode.SCAN_3D ;
                        break;
                    case 1:
                        requestedDisplay = DisplayMode.SCAN_2D;
                        break;
                    case 2:
                        requestedDisplay = DisplayMode.LIVE_3D;
                        break;
                    case 3:
                        requestedDisplay = DisplayMode.CAMERA;
                        break;
                    case 4:
                        requestedDisplay = DisplayMode.OFF;
                        break;
                    default:
                        requestedDisplay = currentDisplay;
                        break;
                }

                if (requestedDisplay != currentDisplay) {

                    switch (requestedDisplay) {
                        case OFF:
                            queueCommand(displayOffString);
                            cameraView.loadUrl("about:blank");
                            imageView.setVisibility  (View.VISIBLE  );
                            cameraView.setVisibility (View.INVISIBLE);
                            textureView.setVisibility(View.INVISIBLE);
                            break;
                        case CAMERA:
                            startCameraDisplayMode();
                            break;
                        case LIVE_3D:
                            startLive3dDisplayMode();
                            break;
                        case SCAN_2D:
                            queueCommand(stopString     );
                            queueCommand(display2dString);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                // Nothing to do
                            }
                            cameraView.loadUrl("about:blank");
                            imageView.setVisibility  (View.INVISIBLE);
                            cameraView.setVisibility (View.INVISIBLE);
                            textureView.setVisibility(View.VISIBLE  );
                            renderer.setMode(DisplayMode.SCAN_2D);
                            renderer.clearData();
                            break;
                        case SCAN_3D:
                            queueCommand(stopString     );
                            queueCommand(display3dString);
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                // Nothing to do
                            }
                            cameraView.loadUrl("about:blank");
                            imageView.setVisibility  (View.INVISIBLE);
                            cameraView.setVisibility (View.INVISIBLE);
                            textureView.setVisibility(View.VISIBLE  );
                            renderer.setMode(DisplayMode.SCAN_3D);
                            renderer.clearData();
                            break;
                        default:
                            // Nothing to do
                            break;
                    }

                    currentDisplay = requestedDisplay;
                }
            }
        });

        forward1.setOnClickListener(new View.OnClickListener()
           {
               @Override
               public void onClick(View view) {
                   queueCommand(forward1String);
           }
        }
        );

        forward2.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view) {
                    queueCommand(forward2String);
            }
        }
        );

        backward1.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view) {
                    queueCommand(backward1String);
            }
        }
        );

        backward2.setOnClickListener(new View.OnClickListener()
             {
                 @Override
                 public void onClick(View view) {
                     queueCommand(backward2String);
             }
        }
        );

        left1.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view) {
                    queueCommand(left1String);
            }
        }
        );

        left2.setOnClickListener(new View.OnClickListener()
             {
                 @Override
                 public void onClick(View view) {
                     queueCommand(left2String);
             }
         }
        );

        right1.setOnClickListener(new View.OnClickListener()
             {
                 @Override
                 public void onClick(View view) {
                     queueCommand(right1String);
             }
         }
        );

        right2.setOnClickListener(new View.OnClickListener()
              {
                  @Override
                  public void onClick(View view) {
                      queueCommand(right2String);
              }
          }
        );

        stop.setOnClickListener(new View.OnClickListener()
             {
                 @Override
                 public void onClick(View view) {
                     enableAll();
                     queueCommand(stopString);
             }
         }
        );

        textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                scaleDetector.onTouchEvent(event);

                float x = event.getX();
                float y = event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:

                        float dx = x - previousX;
                        float dy = y - previousY;

                        renderer.setXPos(renderer.getXPos() - 2 * dx / view.getWidth ());
                        renderer.setYPos(renderer.getYPos() + 2 * dy / view.getHeight());
                }

                previousX = x;
                previousY = y;
                return true;
            }
        });


        sensorManager   = (SensorManager)getSystemService(SENSOR_SERVICE);
        lastEventUpdate = System.currentTimeMillis();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {

            Intent intent = new Intent(getApplicationContext(), ControlSettings.class);
            intent.putExtra(EXTRA_ROBOT_ADDRESS, robotAddress);
            intent.putExtra(EXTRA_CAMERA_PORT  , cameraPort  );
            intent.putExtra(EXTRA_COMMANDS_PORT, commandsPort);
            intent.putExtra(EXTRA_DATA_PORT    , dataPort    );
            startActivityForResult(intent, 0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (currentMode    == Mode.USER_CONTROL
         && currentControl == UserControl.SENSORS) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                getRotation(event);
            }
        }
        return;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nothing to do
        return;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getRotation(SensorEvent event) {

        long   actualTime = event.timestamp;
        byte   outputBuffer[] = new byte[3];
        String yaw;
        String roll;
        String pitch;

        float[] rotationMatrix = new float[16];
        SensorManager.getRotationMatrixFromVector(
                rotationMatrix,
                event.values);

        float[] orientations = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientations);

        for(int i = 0; i < 3; i++) {
            orientations[i] = (float)(Math.toDegrees(orientations[i]) * 2.0);
        }

        joystick.setX(joystickXRef - orientations[1] * joystickXRatio);
        joystick.setY(joystickYRef - orientations[2] * joystickYRatio);

        if (actualTime - lastEventUpdate > 50) {
            lastEventUpdate = actualTime;

            yaw   = Byte.toString((byte)orientations[0]);
            roll  = Byte.toString((byte)orientations[1]);
            pitch = Byte.toString((byte)orientations[2]);

            queueCommand(yaw + ":" + roll + ":" + pitch);
        }
    }

    @Override
    protected void onPause() {

        disableAll();

        setDefaultConfiguration();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Nothing to do
        }

        connectTask.cancel (true);
        sendingTask.cancel (true);
        receiverTask.cancel(true);
        disconnectTask = new DisconnectSockets();
        disconnectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        sensorManager.unregisterListener(this);

        super.onPause();
    }

    @Override
    protected void onResume() {

        if (isCommandsServerConnected == false) {
            connectTask = new ConnectCommandsServer();
            connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            sendingTask = new SendCommandsToServer();
            sendingTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        if (isDataReceiverStarted == false) {
            receiverTask = new DataReceiver();
            receiverTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Nothing to do
        }

        disableAll();

        setDefaultConfiguration();

        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {

                String resultRobotAddress = data.getStringExtra(ControlSettings.EXTRA_SETTINGS_RESULT_ROBOT_ADDRESS);
                String resultCameraPort   = data.getStringExtra(ControlSettings.EXTRA_SETTINGS_RESULT_CAMERA_PORT  );
                String resultCommandsPort = data.getStringExtra(ControlSettings.EXTRA_SETTINGS_RESULT_COMMANDS_PORT);
                String resultDataPort     = data.getStringExtra(ControlSettings.EXTRA_SETTINGS_RESULT_DATA_PORT    );

                robotAddress = resultRobotAddress;
                cameraPort   = resultCameraPort  ;
                commandsPort = resultCommandsPort;
                dataPort     = resultDataPort    ;
                cameraView.loadUrl("http://" + robotAddress + ":" + cameraPort);
            }
        }
    }

    private class ConnectCommandsServer extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {

            while (isCommandsServerConnected == false) {

                if (isCancelled() == true) {
                    break;
                }

                try {
                    commandsServerSocket = new Socket(robotAddress, Integer.parseInt(commandsPort));
                    isCommandsServerConnected = true;
                } catch (IOException e) {
                    // Nothing to do
                }

                if (isCommandsServerConnected == false) {
                    showMsgOnUi("Could not connect.\n\nCheck robot or settings.\n\nRetrying in a second.");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // Nothing to do
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            enableAll();
            super.onPostExecute(result);
        }
    }

    private class SendCommandsToServer extends AsyncTask<Void, Void, Void> {

        String       command;
        OutputStream outputStream;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected Void doInBackground(Void... params) {

            while (true) {

                if (isCancelled() == true) {
                    break;
                }

                if (isCommandsServerConnected == true) {
                    try {

                        outputStream = commandsServerSocket.getOutputStream();

                        if (commandsQueue.size() != 0) {

                            commandsQueueLock.acquire();

                            Iterator<String> commandsQueueIterator = commandsQueue.iterator();

                            while (commandsQueueIterator.hasNext() == true) {
                                command = commandsQueueIterator.next();
                                outputStream.write(command.getBytes());
                                outputStream.flush();
                                Thread.sleep(50);
                                commandsQueueIterator.remove();
                            }

                            commandsQueueLock.release();

                        } else {
                            Thread.sleep(50);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        restartControl();
                    } catch (InterruptedException e) {
                        // Nothing to do
                    }
                } else {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // Nothing to do
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    private class DisconnectSockets extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (isCommandsServerConnected == true) {
                isCommandsServerConnected = false;
                try {
                    commandsServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (isDataReceiverStarted == true) {
                isDataReceiverStarted = false;
                try {
                    dataReceiverServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (isDataReceiverConnected == true) {
                isDataReceiverConnected = false;
                try {
                    dataReceiverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    private class DataReceiver extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                dataReceiverServerSocket = new ServerSocket();
                dataReceiverServerSocket.setReuseAddress(true);
                dataReceiverServerSocket.bind(new InetSocketAddress(Integer.parseInt(dataPort)));

                isDataReceiverStarted = true;

                while (true) {

                    if (isCancelled() == true) {
                        break;
                    }

                    dataReceiverSocket = dataReceiverServerSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(dataReceiverSocket.getInputStream()));

                    isDataReceiverConnected = true;

                    while (true) {

                        if (isCancelled() == true) {
                            break;
                        }

                        String str = in.readLine();

                        if (str != null) {
                            String[] parts = str.split(":");
                            if (parts.length == 3) {
                                renderer.setData((int)Float.parseFloat(parts[0]), (int)Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
                            } else {
                                // Nothing to do
                            }
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }
}
