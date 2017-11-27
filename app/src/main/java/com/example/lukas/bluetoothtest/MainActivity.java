package com.example.lukas.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    // Konstanten
    private static final String CLASS = MainActivity.class.getName();
    private static final int REQUEST_ENABLE_BT = 1234;
    private static final int REQUEST_BLUETOOTH_DEVICE = 1;

    // Attribute
    private Button btn_activateBt;
    private Button btn_selectDev;
    private Button btn_activateGps;
    private Button btn_startTrip;


    private boolean bluetoothEnabled = false;
    private BluetoothAdapter btAdapter;
    public BluetoothDevice btdevice;
    public static BluetoothSocket socket;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_activateBt = (Button) findViewById(R.id.btn_activateBt);
        btn_activateBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateBt();
            }
        });


        btn_selectDev = (Button) findViewById(R.id.btn_selectDev);
        btn_selectDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDevice();
            }
        });

  /*      btn_activateGps = (Button) findViewById(R.id.btn_activateGps);
        btn_activateGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateGps();
            }
        });*/

        btn_startTrip = (Button) findViewById(R.id.btn_startTrip);
        btn_startTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTrip();
            }
        });


    }


    private void showToast(String message, int duration) {
        Toast.makeText(MainActivity.this, message, duration).show();
    }


    // Bluetooth-Verbindung wird aktiviert
    private void activateBt () {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
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
                btn_activateBt.setText(getResources().getString(R.string.main_bt_enabled));
                btn_activateBt.setEnabled(false);
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
                    btn_activateBt.setText(getResources().getString(R.string.main_bt_enabled));
                    btn_activateBt.setEnabled(false);
                    btn_selectDev.setEnabled(true);
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
                    } catch (IOException ioe) {
                    }
                    btn_selectDev.setText(getResources().getString(R.string.main_sel));
                    btn_selectDev.setEnabled(false);
                } else {
                    Log.e(CLASS, "No device selected");
                }
                break;
        }
    }

    // Activity starten, in der der Nutzer zusätzliche Angaben machen muss
    private void startTrip() {
        Intent intent = new Intent(this, StartYourTripActivity.class);
        startActivity(intent);
    }


}