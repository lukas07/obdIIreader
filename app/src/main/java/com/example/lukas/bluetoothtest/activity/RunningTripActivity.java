package com.example.lukas.bluetoothtest.activity;

import android.Manifest;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lukas.bluetoothtest.io.BluetoothConnector;
import com.example.lukas.bluetoothtest.fragment.GoogleMapFragment;
import com.example.lukas.bluetoothtest.io.ObdService;
import com.example.lukas.bluetoothtest.R;
import com.example.lukas.bluetoothtest.io.Stopwatch;
import com.example.lukas.bluetoothtest.trip.TripRecord;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.example.lukas.bluetoothtest.activity.MainActivity.btdevice;
import static com.example.lukas.bluetoothtest.activity.MainActivity.socket;

public class RunningTripActivity extends AppCompatActivity {

    private static final String CLASS = RunningTripActivity.class.getName();
    // Konstanten: SaveInstance
    private static final String STATE_TIMER = "timerValue";
    private static final String STATE_MARKERPOINTS = "marker";
    // Konstanten: obdHandler
        // Steuerung des Ladebalken
    private static final int INIT_STARTED = 1;
    private static final int INIT_SUCCESS = 2;
    private static final int INIT_STOPPED = 3;
        // Exception-Meldungen für UI-Activity
    private static final int NODATA_EXCEPTION = 10;
    private static final int CONNECT_EXCEPTION = 11;

    private TextView tv_speed;
    private TextView tv_fuel;
    private TextView tv_consumption;
    private TextView tv_timer;
    private TextView tv_internet;
    private ImageButton bt_stop;
    private ProgressBar pb_init;

    private Context context = this;

    private ObdService service;
    private boolean serviceBound = false;
    private Thread initThread;
    private Thread sendThread;
    private int orientation;
    private TripRecord record = TripRecord.getTripRecord();
    //private SharedPref sharedPref;


    // Google Map und GPS-Daten
    private GoogleMapFragment mapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Geocoder geocoder;

    // Fahrtzeit
    final int MSG_START_TIMER = 0;
    final int MSG_STOP_TIMER = 1;
    final int MSG_UPDATE_TIMER = 2;
    final int MSG_SET_TIMER = 3;
    Stopwatch timer = new Stopwatch();
    final int REFRESH_RATE = 1000;
    private long timerValue = 0;
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

                case MSG_SET_TIMER:
                    timer.setTimer(timerValue);
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

    // Handler, um die Daten des OBDII-Adapters zu empfangen und Aktualisierung der UI durchzuführen
    private Handler obdHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case INIT_STARTED:
                    pb_init.setVisibility(View.VISIBLE);
                    break;
                case INIT_STOPPED:
                    pb_init.setVisibility(View.GONE);
                    Log.e(CLASS, "Toast");
                    Toast.makeText(RunningTripActivity.this, getResources().getString(R.string.run_init_fail), Toast.LENGTH_LONG).show();
                    // Sonst wird der Timer noch kurz angezeigt
                    //tv_timer.setVisibility(View.INVISIBLE);
                    break;
                case INIT_SUCCESS:
                    pb_init.setVisibility(View.GONE);
                    bt_stop.setEnabled(true);
                    // Timer zur Anzeige der Fahrzeit starten
                    startTimer();
                    break;
                case NODATA_EXCEPTION:
                    Log.e(CLASS, "NODATA_EXCEPTION thrown");
                    showErrorDialog(NODATA_EXCEPTION);
                    break;
                case CONNECT_EXCEPTION:
                    Log.e(CLASS, "CONNECT_EXCEPTION thrown");
                    showErrorDialog(CONNECT_EXCEPTION);
                    break;
                default:
                    Bundle bundle = message.getData();
                    String speed = bundle.getString(AvailableCommandNames.SPEED.getValue());
                    String fuel = bundle.getString(AvailableCommandNames.MAF.getValue());
                    //String consumption = bundle.getString(AvailableCommandNames.FUEL_CONSUMPTION_RATE.getValue());

                    if(speed != null)
                        tv_speed.setText(speed);
                    if(fuel != null)
                        tv_fuel.setText(fuel);

                    //if(consumption != null)
                    if (tv_internet.getVisibility() == View.VISIBLE)
                        tv_internet.setVisibility(View.GONE);
                    //  tv_consumption.setText(consumption);
            }
        }
    };

    // Stellt Verbindung mit dem OBD-Service her und startet diesen
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            orientation = getRequestedOrientation();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

            Log.e(CLASS, "Service connected");
            ObdService.ObdServiceBinder binder = (ObdService.ObdServiceBinder) serviceBinder;
            service = binder.getService();
            serviceBound = true;
            service.setHandler(obdHandler);
            // Auslagerung der Initialisierung in eigenen Thread, um UI gleichzeitig updaten zu können
            initThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        service.startObdConnection();
                    } catch (IOException ioe) {
                        //unbindService(serviceConnection);
                        serviceBound = false;
                        Log.e(CLASS, "Service disconnected");
                        finish();
                    }
                }
            });
            initThread.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(CLASS, "Service disconnected");
            serviceBound = false;
        }
    };

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                        alertDialogBuilder.setCancelable(false);
                        alertDialogBuilder.setMessage(R.string.run_bt_off);
                        alertDialogBuilder.setPositiveButton(R.string.run_reconnect,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        Log.e(CLASS, "Reconnect...");
                                        BluetoothAdapter.getDefaultAdapter().enable();
                                        try {
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        try {
                                            socket = BluetoothConnector.connectDevice(btdevice);

                                            sendThread = new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    service.startSendOBDCommands();
                                                    Log.e(CLASS, "Test");
                                                }
                                            });
                                            sendThread.start();
                                            Log.e(CLASS, "Send thread started...");
                                            //service.startSendOBDCommands();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Log.e(CLASS, "Could not connect to socket");
                                        }
                                    }
                                });

                        alertDialogBuilder.setNegativeButton(R.string.run_bt_stop,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.e(CLASS, "Stop tracking...");
                                        stopTrip();
                                    }
                                });

                        alertDialogBuilder.setNeutralButton(R.string.run_bt_menu,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Log.e(CLASS, "Go back to menu...");
                                        service.stopSendOBDCommands();
                                        mapFragment.onDestroy();
                                        finish();
                                    }
                                });


                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();

//                        showToast(getResources().getString(R.string.main_bt_manual_disabled), Toast.LENGTH_LONG);
//                        finish();
                        break;
                }

            }
        }
    };

    private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                ContentResolver contentResolver = getApplicationContext().getContentResolver();
                int mode = Settings.Secure.getInt(
                        contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
                if(mode == Settings.Secure.LOCATION_MODE_OFF) {
                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                    alertDialogBuilder.setCancelable(false);
                    alertDialogBuilder.setMessage(R.string.run_gps_off);
                    alertDialogBuilder.setPositiveButton(R.string.run_reconnect,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    // TODO
                                    mapFragment.buildGoogleApiClient();
                                }
                            });

                    alertDialogBuilder.setNegativeButton(R.string.run_bt_stop,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.e(CLASS, "Stop tracking...");
                                    stopTrip();
                                }
                            });

                    alertDialogBuilder.setNeutralButton(R.string.run_bt_menu,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.e(CLASS, "Go back to menu...");
                                    service.stopSendOBDCommands();
                                    mapFragment.onDestroy();
                                    finish();
                                }
                            });


                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
                    //showToast(getResources().getString(R.string.main_gps_manual_disabled), Toast.LENGTH_LONG);
                //finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_trip);

        //sharedPref = new SharedPref(this);

        // Referenzvariablen zu den Feldern deklarieren
        tv_speed = (TextView) findViewById(R.id.tv_speed);
        tv_fuel = (TextView) findViewById(R.id.tv_fuel);
        tv_consumption = (TextView) findViewById(R.id.tv_consumption);
        tv_timer = (TextView) findViewById(R.id.tv_timer);
        tv_internet = (TextView) findViewById(R.id.tv_internet);
        bt_stop = (ImageButton) findViewById(R.id.bt_stop);
        pb_init = (ProgressBar) findViewById(R.id.pb_init);


        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrip();
            }
        });

        // Broadcast-Recevier, um Änderungen des Bluetooth-Status zu registrieren
        IntentFilter filterBt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBt);
        // Broadcast-Recevier, um Änderungen des GPS-Status zu registrieren
        IntentFilter filterGps = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsReceiver, filterGps);

        // Werden zur Ermittlung der Endadresse benötigt, wenn der Trip gestoppt wird
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        // OBD-Service zum Auslesen der Daten des Adapters starten
        Log.e(CLASS, "Bind OBD-Service");
        Intent serviceIntent = new Intent(getApplicationContext(), ObdService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        if(savedInstanceState != null) {
            // Falls der Timer bereits gelaufen ist, den Wert übernehmen
            timerValue = savedInstanceState.getLong(STATE_TIMER);
        }

        // Google Map hinzufügen
        mapFragment = GoogleMapFragment.newInstance(getApplicationContext(), 0, GoogleMapFragment.MAP_MODE_LIVE);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.running_container, mapFragment);
        fragmentTransaction.commit();

    }

    // Wenn der Adapter einen Fehler zurück gibt, wird ein Dialogfenster geöffnet, in dem der User entscheidet,
    // ob er den Trip regulär stoppen will oder abbrechen
    private void showErrorDialog(int errorType) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCancelable(false);
        if (errorType == NODATA_EXCEPTION)
            alertDialogBuilder.setMessage(R.string.run_no_data);
        else if (errorType == CONNECT_EXCEPTION)
            alertDialogBuilder.setMessage(R.string.run_connect);
        alertDialogBuilder.setPositiveButton(R.string.run_bt_stop,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        stopTrip();
                    }
        });

        alertDialogBuilder.setNegativeButton(R.string.run_bt_menu,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        service.stopSendOBDCommands();
                        mapFragment.onDestroy();
                        finish();
                    }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        // Adapter muss bei nächster Aufzeichnung neu initialisiert werden ??
        //sharedPref.setObdInitialized(false);
    }

    private void stopTrip() {
        // Timer der Fahrzeit stoppen
        clockHandler.sendEmptyMessage(MSG_STOP_TIMER);

        Log.e(CLASS, "Unbind Service");
        //OBD-Service stoppen
        service.stopSendOBDCommands();
        try {
            unbindService(serviceConnection);
            serviceBound = false;
        } catch (Exception e) {
            Log.e(CLASS, "Couldnt unbind");
        }

        // Updaten der GPS-Daten stoppen
        mapFragment.onDestroy();

        // Daten in Triprecord schreiben (Stopzeitpunkt, Endadresse,...)
        record = record.getTripRecord();

        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Long timestamp = Long.parseLong(timestampFormat.format(new Date()));
        record.setEndTimestamp(timestamp);

        // Die Startadresse ermitteln und im Record speichern
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            try {
                                // Die Adresse über den Geocoder ermitteln
                                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                Log.e(CLASS, "Geocoder: " + addresses.toString());
                                if (addresses == null || addresses.size() == 0)
                                    record.setEndAddress("NODATA");
                                else
                                    record.setEndAddress(addresses.get(0).getAddressLine(0));
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(CLASS, "Geocoder: " + e.toString());
                            }
                        }
                    }
                });
        if(record.getEndAddress() == null)
            record.setEndAddress("Keine Informationen");


        // Arraylist der Locations wird in einen String gepackt, um diesen in die DB zu schreiben
        Gson gsonRoutePoints = new Gson();
        String routeString = gsonRoutePoints.toJson(mapFragment.getRoutePoints());
        Log.e(CLASS, "Route points: " + routeString);
        record.setRoutePoints(routeString);


        Intent intent = new Intent(RunningTripActivity.this, StoppedTripActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(R.string.run_back_msg);
        alertDialogBuilder.setPositiveButton(R.string.run_yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Log.e(CLASS, "Stop tracking...");
                        service.stopSendOBDCommands();
                        mapFragment.onDestroy();
                        finish();
                    }
                });

        alertDialogBuilder.setNegativeButton(R.string.run_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e(CLASS, "Continue tracking...");
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void startTimer() {
        if(timerValue == 0)
            clockHandler.sendEmptyMessage(MSG_START_TIMER);
        else
            clockHandler.sendEmptyMessage(MSG_SET_TIMER);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setRequestedOrientation(orientation);
        if(serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        // Falls die Initliasisierung noch läuft abbrechen
       if(initThread.isAlive())
            initThread.interrupt();

        if (sendThread != null) {
            if (sendThread.isAlive())
                sendThread.interrupt();
        }


        unregisterReceiver(bluetoothReceiver);
        unregisterReceiver(gpsReceiver);
    }

    @Override
    public void onSaveInstanceState (Bundle savedInstanceState) {
        // Den aktuellen Wert des Timers speichern
        savedInstanceState.putLong(STATE_TIMER, timer.getStartTime());

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mapFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }

}
