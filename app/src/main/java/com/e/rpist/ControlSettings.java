package com.e.rpist;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class ControlSettings extends AppCompatActivity {

    public  static final String EXTRA_SETTINGS_RESULT_ROBOT_ADDRESS = "com.e.rpist.settings.result.robot.address";
    public  static final String EXTRA_SETTINGS_RESULT_CAMERA_PORT   = "com.e.rpist.settings.result.camera.port";
    public  static final String EXTRA_SETTINGS_RESULT_COMMANDS_PORT = "com.e.rpist.settings.result.commands.port";
    public  static final String EXTRA_SETTINGS_RESULT_DATA_PORT     = "com.e.rpist.settings.result.data.port";

    private Button ok, cancel;

    public static String inputRobotAddress, inputCameraPort, inputCommandsPort, inputDataPort;

    EditText robotAddress;
    EditText cameraPort;
    EditText commandsPort;
    EditText dataPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        ok     = findViewById(R.id.okButton    );
        cancel = findViewById(R.id.cancelButton);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        inputRobotAddress = bundle.getString(ControlRobot.EXTRA_ROBOT_ADDRESS);
        inputCameraPort   = bundle.getString(ControlRobot.EXTRA_CAMERA_PORT  );
        inputCommandsPort = bundle.getString(ControlRobot.EXTRA_COMMANDS_PORT);
        inputDataPort     = bundle.getString(ControlRobot.EXTRA_DATA_PORT    );

        robotAddress = findViewById(R.id.robot_address);
        cameraPort   = findViewById(R.id.camera_port  );
        commandsPort = findViewById(R.id.commands_port);
        dataPort     = findViewById(R.id.data_port    );

        robotAddress.setText(inputRobotAddress);
        cameraPort.setText  (inputCameraPort  );
        commandsPort.setText(inputCommandsPort);
        dataPort.setText    (inputDataPort    );

        ok.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SETTINGS_RESULT_ROBOT_ADDRESS, robotAddress.getText().toString());
                resultIntent.putExtra(EXTRA_SETTINGS_RESULT_CAMERA_PORT  , cameraPort.getText().toString()  );
                resultIntent.putExtra(EXTRA_SETTINGS_RESULT_COMMANDS_PORT, commandsPort.getText().toString());
                resultIntent.putExtra(EXTRA_SETTINGS_RESULT_DATA_PORT    , dataPort.getText().toString()    );
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SETTINGS_RESULT_ROBOT_ADDRESS, inputRobotAddress);
                resultIntent.putExtra(EXTRA_SETTINGS_RESULT_CAMERA_PORT  , inputCameraPort  );
                resultIntent.putExtra(EXTRA_SETTINGS_RESULT_COMMANDS_PORT, inputCommandsPort);
                resultIntent.putExtra(EXTRA_SETTINGS_RESULT_DATA_PORT    , inputDataPort    );
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }
}
