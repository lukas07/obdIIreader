package com.example.lukas.bluetoothtest.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.lukas.bluetoothtest.io.BluetoothConnector;
import com.example.lukas.bluetoothtest.R;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    // Konstanten
    private static final String CLASS = MainActivity.class.getName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_DEVICE = 2;
    private static final int REQUEST_CHECK_SETTINGS = 10;

    // Attribute
    private Button bt_activateBt;
    private Button bt_selectDev;
    private Button bt_activateGps;
    private Button bt_startTrip;
    private Button bt_showTrips;

    // Nur wenn beide auf true gesetzt sind, kann die Tripaufzeichnung gestartet werden
    private boolean btEnabled = false;
    private boolean gpsEnabled = false;
    public static boolean obd_initialized = false;
    //private SharedPref sharedPreferences;


    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();;
    public static BluetoothDevice btdevice;
    public static BluetoothSocket socket;

    // Receiver, der Veränderungen des Bluetooth-Status registriert
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        bt_activateBt.setEnabled(true);
                        bt_activateBt.setText(getResources().getString(R.string.main_bt_disabled));
                        bt_selectDev.setEnabled(false);
                        bt_selectDev.setText(R.string.main_dev);
                        bt_startTrip.setEnabled(false);
                        socket = null;
                        btEnabled = false;
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        bt_activateBt.setEnabled(false);
                        bt_activateBt.setText(getResources().getString(R.string.main_bt_enabled));
                        bt_selectDev.setEnabled(true);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        showToast(getResources().getString(R.string.main_bt_manual_enabled), Toast.LENGTH_LONG);
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        showToast(getResources().getString(R.string.main_bt_manual_disabled), Toast.LENGTH_LONG);
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
                    gpsEnabled = false;
                    bt_activateGps.setEnabled(true);
                    bt_activateGps.setText(R.string.main_bt_disabled);
                    bt_startTrip.setEnabled(false);
                    showToast(getResources().getString(R.string.main_gps_manual_disabled), Toast.LENGTH_LONG);
                } else {
                    gpsEnabled = true;
                    bt_activateGps.setEnabled(false);
                    bt_activateGps.setText(R.string.main_bt_enabled);
                    checkStartTrip();
                    showToast(getResources().getString(R.string.main_gps_manual_enabled), Toast.LENGTH_LONG);
                }
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //sharedPreferences = new SharedPref(this);


        // Broadcast-Recevier, um Änderungen des Bluetooth-Status zu registrieren
        IntentFilter filterBt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBt);
        // Broadcast-Recevier, um Änderungen des GPS-Status zu registrieren
        IntentFilter filterGps = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsReceiver, filterGps);

        // Referenzvariablen zu den Feldern deklarieren
        bt_activateBt = (Button) findViewById(R.id.bt_activateBt);
        bt_selectDev = (Button) findViewById(R.id.bt_selectDev);
        bt_activateGps = (Button) findViewById(R.id.bt_activateGps);
        bt_startTrip = (Button) findViewById(R.id.bt_startTrip);
        bt_showTrips = (Button) findViewById(R.id.bt_showTrips);


        // Clicklistener für die Buttons
        bt_activateBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateBt();
            }
        });
        bt_selectDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDevice();
            }
        });
        bt_activateGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateGps();
            }
        });
        bt_startTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTrip();
            }
        });
        bt_showTrips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTrips();
            }
        });

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        checkConnections();
        //sharedPreferences.setObdInitialized(false);

    }


    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }


    // Bluetooth-Verbindung wird aktiviert
    private void activateBt () {
        // Prüfen, ob das Gerät Bluetooth unterstützt
        if(btAdapter == null) {
            showToast(getResources().getString(R.string.main_bt_support), Toast.LENGTH_SHORT);
            Log.e(CLASS, "No Bluetooth support");
        } else {
            // Bluetooth ggf. aktivieren
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Log.e(CLASS, "Bluetooth already enabled");
                bt_activateBt.setText(getResources().getString(R.string.main_bt_enabled));
                bt_activateBt.setEnabled(false);
            }
        }
    }


    // Liste der gekoppelten Geräte anzeigen und Verbindung zu ausgewähltem Gerät aufbauen
    private void selectDevice() {
        // Nachdem Bluetooth erfolgreich aktiviert wurde, das Dialogfenster zur Geräteauswahl anzeigen
        Intent btIntent = new Intent(MainActivity.this, DeviceListActivity.class);
        startActivityForResult(btIntent, REQUEST_BLUETOOTH_DEVICE);
    }

    private void activateGps() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                gpsEnabled = true;
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) {
                    // Bluetooth erfolgreich aktiviert
                    Log.e(CLASS, "Bluetooth enabled");
                    /*bt_activateBt.setEnabled(false);
                    bt_activateBt.setText(getResources().getString(R.string.main_bt_enabled));
                    bt_selectDev.setEnabled(true);*/
                } else if (resultCode == RESULT_CANCELED) {
                    //showToast(getResources().getString(R.string.main_bt_disabled), Toast.LENGTH_SHORT);
                    Log.e(CLASS, "Error while enabling Bluetooth");
                }
                break;
            case REQUEST_BLUETOOTH_DEVICE:
                if(resultCode == RESULT_OK) {
                    String deviceAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Adresse des zu verbindenden Gerätes
                    btdevice = btAdapter.getRemoteDevice(deviceAddress);
                    Log.e(CLASS, "Selected device:" + btdevice.getAddress() + "; " + btdevice.getAddress());
                    try{
                        // Verbindung aufbauen
                        socket = BluetoothConnector.connectDevice(btdevice);
                        showToast(getResources().getString(R.string.main_sel_dev) + btdevice.getName(), Toast.LENGTH_SHORT);
                        btEnabled = true;
                        checkStartTrip();
                        bt_selectDev.setText(getResources().getString(R.string.main_sel));
                        bt_selectDev.setEnabled(false);
                    } catch (IOException ioe) {
                        showToast(getResources().getString(R.string.main_con_err), Toast.LENGTH_LONG);
                    }
                } else {
                    Log.e(CLASS, "No device selected");
                }
                break;
            case REQUEST_CHECK_SETTINGS:
                if(resultCode == RESULT_OK) {
                    Log.e(CLASS, "GPS enabled");
                    gpsEnabled = true;
                    bt_activateGps.setEnabled(false);
                    bt_activateGps.setText(R.string.main_bt_enabled);
                    checkStartTrip();
                }
        }
    }

    private void checkConnections () {
        // Falls Bluetooth bereits eingeschaltet ist, Buttons setzen
        if (btAdapter.isEnabled()) {
            Log.e(CLASS, "Bluetooth already enabled");
            bt_activateBt.setText(getResources().getString(R.string.main_bt_enabled));
            bt_activateBt.setEnabled(false);
            bt_selectDev.setEnabled(true);
        } else {
            bt_activateBt.setText(getResources().getString(R.string.main_bt_disabled));
            bt_activateBt.setEnabled(true);
            bt_selectDev.setText(getResources().getString(R.string.main_sel));
            bt_selectDev.setEnabled(false);
            socket = null;
            //sharedPreferences.setObdInitialized(false);

        }
        if (socket != null) {
            btEnabled = true;
            //obd_initialized = true;

            bt_selectDev.setText(getResources().getString(R.string.main_sel));
            bt_selectDev.setEnabled(false);
        } else {
            bt_startTrip.setEnabled(false);
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsEnabled = true;
            bt_activateGps.setEnabled(false);
            bt_activateGps.setText(R.string.main_bt_enabled);
            checkStartTrip();
        } else {
            gpsEnabled = false;
            bt_activateGps.setEnabled(true);
            bt_activateGps.setText(R.string.main_bt_disabled);
        }
    }

    // Falls sowohl GPS als auch Bluetooth eingeschaltet sind und eine Bt-Verbindung zu einem Gerät besteht,
    // wird der StartYourTrip-Button aktiviert
    private void checkStartTrip() {
        if(btEnabled && gpsEnabled)
            bt_startTrip.setEnabled(true);
    }

    // Activity starten, in der der Nutzer zusätzliche Angaben machen muss
    private void startTrip() {
        Intent intent = new Intent(this, StartTripActivity.class);
        startActivity(intent);
    }

    private void showTrips() {
        Intent intent = new Intent(this, TripListActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(bluetoothReceiver);
        unregisterReceiver(gpsReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkConnections();
        // Broadcast-Recevier, um Änderungen des Bluetooth-Status zu registrieren
        IntentFilter filterBt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBt);
        // Broadcast-Recevier, um Änderungen des GPS-Status zu registrieren
        IntentFilter filterGps = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsReceiver, filterGps);
    }
}