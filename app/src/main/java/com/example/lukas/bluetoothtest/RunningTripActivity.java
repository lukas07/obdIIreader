package com.example.lukas.bluetoothtest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.lukas.bluetoothtest.MainActivity.socket;

public class RunningTripActivity extends AppCompatActivity {
    private static final String CLASS = RunningTripActivity.class.getName();

    private TextView tv_speed;
    private TextView tv_fuel;
    private TextView tv_timer;
    private Button bt_stop;
    private ProgressBar pb_init;


    private ObdService service;

    private TripRecord record;

    // Fahrtzeit
    final int MSG_START_TIMER = 0;
    final int MSG_STOP_TIMER = 1;
    final int MSG_UPDATE_TIMER = 2;
    Stopwatch timer = new Stopwatch();
    final int REFRESH_RATE = 1000;


    // Handler, um den Timer auf der Oberfläche zu aktualisieren
    private Handler clockHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_START_TIMER:
                    timer.start(); //start timer
                    clockHandler.sendEmptyMessage(MSG_UPDATE_TIMER);
                    break;

                case MSG_UPDATE_TIMER:
                    tv_timer.setText(""+ timer.toString());
                    clockHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIMER,REFRESH_RATE); //text view is updated every second,
                    break;                                  //though the timer is still running
                case MSG_STOP_TIMER:
                    timer.stop();//stop timer
                    break;

                default:
                    break;
            }
        }
    };

    // Handler, um die Daten des OBDII-Adapters zu empfangen und Aktualisierung aufzurufen
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            updateUI(message);
        }
    };

    // Stellt Verbindung mit dem OBD-Service her und startet diesen
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            Log.e(CLASS, "Service connected");
            ObdService.ObdServiceBinder binder = (ObdService.ObdServiceBinder) serviceBinder;
            service = binder.getService();
            service.setHandler(handler);
            try {
                service.initObdConnection();
                bt_stop.setEnabled(true);
                pb_init.setVisibility(View.GONE);
                // Timer zur Anzeige der Fahrzeit starten
                clockHandler.sendEmptyMessage(MSG_START_TIMER);
            } catch (IOException ioe) {
                pb_init.setVisibility(View.GONE);
                unbindService(serviceConnection);
                Log.e(CLASS, "Service disconnected");
                Toast.makeText(RunningTripActivity.this, getResources().getString(R.string.run_init_fail), Toast.LENGTH_LONG).show();
                // Sonst wird der Timer noch kurz angezeigt
                tv_timer.setVisibility(View.INVISIBLE);
                finish();
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

        // Referenzvariablen zu den Feldern deklarieren
        tv_speed = (TextView) findViewById(R.id.tv_speed);
        tv_fuel = (TextView) findViewById(R.id.tv_fuel);
        tv_timer = (TextView) findViewById(R.id.tv_timer);
        bt_stop = (Button) findViewById(R.id.bt_stop);
        pb_init = (ProgressBar) findViewById(R.id.pb_init);


        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrip();
            }
        });

        // OBD-Service zum Auslesen der Daten des Adapters starten
        Log.e(CLASS, "Bind OBD-Service");
        Intent serviceIntent = new Intent(getApplicationContext(), ObdService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);


    }

    // Aktualisiert die Anzeige der OBD-Daten
    private void updateUI(Message message) {
        Bundle bundle = message.getData();
        String speed = bundle.getString("speed");
        String fuel = bundle.getString("consumption");

        tv_speed.setText(speed);
        tv_fuel.setText(fuel);
    }


    private void stopTrip() {
        // Timer der Fahrzeit stoppen
        clockHandler.sendEmptyMessage(MSG_STOP_TIMER);

        Log.e(CLASS, "Unbind Service");
        service.stopSendOBDCommands();
        unbindService(serviceConnection);
        /*try {
            if (socket != null) {
                service.closeSocket();
            } else {
                Log.e(CLASS, "No socket available");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(CLASS, "Error while closing socket");
        }*/
        // Daten in Triprecord schreiben (Stopzeitpunkt, Endadresse,...)
        record = record.getTripRecord();

        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Long timestamp = Long.parseLong(timestampFormat.format(new Date()));
        record.setEndTimestamp(timestamp);


        Intent intent = new Intent(RunningTripActivity.this, StoppedTripActivity.class);
        startActivity(intent);
        finish();
    }

}
