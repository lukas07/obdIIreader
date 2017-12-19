package com.example.lukas.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    // Konstanten
    private static final String CLASS = MainActivity.class.getName();
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final int REQUEST_BLUETOOTH_DEVICE = 1;

    // Attribute
    private Button bt_activateBt;
    private Button bt_selectDev;
    private Button bt_activateGps;
    private Button bt_startTrip;
    private Button bt_showTrips;


    private boolean bluetoothEnabled = false;
    private static BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();;
    public BluetoothDevice btdevice;
    public static BluetoothSocket socket;

    public static boolean obd_initialized = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Referenzvariablen zu den Feldern deklarieren
        bt_activateBt = (Button) findViewById(R.id.bt_activateBt);
        bt_selectDev = (Button) findViewById(R.id.bt_selectDev);
        bt_startTrip = (Button) findViewById(R.id.bt_startTrip);
        bt_showTrips = (Button) findViewById(R.id.bt_showTrips);


        // Clicklistener für die Buttons
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

        // Falls Bluetooth bereits eingeschaltet ist, Buttons setzen
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Log.e(CLASS, "Bluetooth already enabled");
            bt_activateBt.setText(getResources().getString(R.string.main_bt_enabled));
            bt_activateBt.setEnabled(false);
            bt_selectDev.setEnabled(true);
        }
        if (socket != null) {
            bt_startTrip.setEnabled(true);
            bt_selectDev.setText(getResources().getString(R.string.main_sel));
            bt_selectDev.setEnabled(false);

            obd_initialized = true;
        }


  /*      bt_activateGps = (Button) findViewById(R.id.bt_activateGps);
        bt_activateGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateGps();
            }
        });*/
    }


    private void showToast(String message, int duration) {
        Toast.makeText(MainActivity.this, message, duration).show();
    }


    // Bluetooth-Verbindung wird aktiviert
    private void activateBt () {
        // Prüfen, ob das Gerät Bluetooth unterstützt
        if(btAdapter == null) {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.main_bt_support), Toast.LENGTH_SHORT).show();
            Log.e(CLASS, "No Bluetooth support");
        } else {
            // Bluetooth ggf. aktivieren
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Log.e(CLASS, "Bluetooth already enabled");
                bluetoothEnabled = true;
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

/*    private void activateGps() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }*/


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) {
                    // Bluetooth erfolgreich aktiviert
                    //Toast.makeText(MainActivity.this, getResources().getString(R.string.bt_enabled), Toast.LENGTH_SHORT).show();
                    Log.e(CLASS, "Bluetooth enabled");
                    bt_activateBt.setText(getResources().getString(R.string.main_bt_enabled));
                    bt_activateBt.setEnabled(false);
                    bt_selectDev.setEnabled(true);
                    bluetoothEnabled = true;
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.main_bt_disabled), Toast.LENGTH_SHORT).show();
                    Log.e(CLASS, "Error while enabling Bluetooth");
                    bluetoothEnabled = false;
                }
                break;
            case REQUEST_BLUETOOTH_DEVICE:
                if(resultCode == RESULT_OK) {
                    String deviceAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Adresse des zu verbindenden Gerätes
                    btdevice = btAdapter.getRemoteDevice(deviceAddress);
                    showToast(getResources().getString(R.string.main_sel_dev) + btdevice.getName(), Toast.LENGTH_SHORT);
                    Log.e(CLASS, "Selected device:" + btdevice.getAddress() + "; " + btdevice.getAddress());
                    try{
                        // Verbindung aufbauen
                        socket = BluetoothConnector.connectDevice(btdevice);
                        bt_startTrip.setEnabled(true);
                        bt_selectDev.setText(getResources().getString(R.string.main_sel));
                        bt_selectDev.setEnabled(false);
                    } catch (IOException ioe) {
                        showToast(getResources().getString(R.string.main_con_err), Toast.LENGTH_LONG);
                    }
                } else {
                    Log.e(CLASS, "No device selected");
                }
                break;
        }
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

}