package com.example.lukas.bluetoothtest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import static com.example.lukas.bluetoothtest.MainActivity.socket;

public class RunningTripActivity extends AppCompatActivity {
    private static final String CLASS = RunningTripActivity.class.getName();

    private TextView tv_speed;
    private TextView tv_fuel;
    private Button bt_stop;

    private ObdService service;


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            updateUI(message);
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            Log.e(CLASS, "Service connected");
            ObdService.ObdServiceBinder binder = (ObdService.ObdServiceBinder) serviceBinder;
            service = binder.getService();
            service.setHandler(handler);
            try {
                service.initObdConnection();
                //btn_selectDev.setText(getResources().getString(R.string.dev_connected));
            } catch (IOException ioe) {
                unbindService(serviceConnection);
                Log.e(CLASS, "Service disconnected");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(CLASS, "Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_trip);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tv_speed = (TextView) findViewById(R.id.tv_speed);
        tv_fuel = (TextView) findViewById(R.id.tv_fuel);

        bt_stop = (Button) findViewById(R.id.bt_stop);
        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrip();
            }
        });

        // OBD-Service zum Auslesen der Daten des Adapters starten
        Log.e(CLASS, "Bind OBD-Service");
        Intent serviceIntent = new Intent(this, ObdService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);


    }


    private void updateUI(Message message) {
        Bundle bundle = message.getData();
        String speed = bundle.getString("speed");
        String fuel = bundle.getString("fuel");

        tv_speed.setText(speed + " km/h");
        tv_fuel.setText(fuel + " l/100km");
    }


    private void stopTrip() {
        Log.e(CLASS, "Unbind Service");
        //unbindService(serviceConnection);
        //btn_startTrip.setEnabled(true);
        try {
            if (socket != null) {
                service.closeSocket();
            } else {
                Log.e(CLASS, "No socket available");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(CLASS, "Error while closing socket");
        }
        Intent intent = new Intent(RunningTripActivity.this, StoppedTripActivity.class);
        startActivity(intent);
    }

}
