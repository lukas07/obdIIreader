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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.lukas.bluetoothtest.io.BluetoothConnector;
import com.example.lukas.bluetoothtest.R;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_01_20;
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

/**
 * Author: Lukas Breit
 *
 * Description: The MainActivity is the entry point of the application. From there you are able to open the overview of the trips
 *              and start a new trip recording. For starting a new one, bluetooth and GPS must be enabled. Furthermore the OBD Adapter
 *              has to be selected. Each of the last three aspects can be initiated from the MainActivity.
 *
 *
 */


public class MainActivity extends AppCompatActivity {
    // Constants
    private static final String CLASS = MainActivity.class.getName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_DEVICE = 2;
    private static final int REQUEST_CHECK_SETTINGS = 10;

    // Attributes
    private ImageButton bt_activateBt;
    private ImageButton bt_selectDev;
    private ImageButton bt_activateGps;
    private ImageButton bt_startTrip;
    private ImageButton bt_showTrips;

    private ImageView iv_info_bt;
    private ImageView iv_info_device;
    private ImageView iv_info_gps;

    // Only if both are set to true a recording can be started
    private boolean btEnabled = false;
    private boolean gpsEnabled = false;

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();;
    public static BluetoothDevice btdevice;
    public static BluetoothSocket socket;

    // Receiver that recognizes changes of the bluetooth state
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        bt_activateBt.setEnabled(true);
                        iv_info_bt.setBackgroundResource(R.drawable.close_circle_red);
                        bt_selectDev.setEnabled(false);
                        iv_info_device.setBackgroundResource(R.drawable.close_circle_red);
                        bt_startTrip.setEnabled(false);
                        socket = null;
                        btEnabled = false;
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        bt_activateBt.setEnabled(false);
                        iv_info_bt.setBackgroundResource(R.drawable.check_circle_green);
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

    // Receiver that recognizes changes of the GPS state
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
                    iv_info_gps.setBackgroundResource(R.drawable.close_circle_red);
                    bt_startTrip.setEnabled(false);
                    showToast(getResources().getString(R.string.main_gps_manual_disabled), Toast.LENGTH_LONG);
                } else {
                    gpsEnabled = true;
                    bt_activateGps.setEnabled(false);
                    iv_info_gps.setBackgroundResource(R.drawable.check_circle_green);
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


        // Receiver that recognizes changes of the bluetooth state
        IntentFilter filterBt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBt);
        // Receiver that recognizes changes of the GPS state
        IntentFilter filterGps = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsReceiver, filterGps);

        bt_activateBt = (ImageButton) findViewById(R.id.bt_activateBt);
        bt_selectDev = (ImageButton) findViewById(R.id.bt_selectDev);
        bt_activateGps = (ImageButton) findViewById(R.id.bt_activateGps);
        bt_startTrip = (ImageButton) findViewById(R.id.bt_startTrip);
        bt_showTrips = (ImageButton) findViewById(R.id.bt_showTrips);
        iv_info_bt = (ImageView) findViewById(R.id.iv_info_bt);
        iv_info_device = (ImageView) findViewById(R.id.iv_info_device);
        iv_info_gps = (ImageView) findViewById(R.id.iv_info_gps);

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

    }


    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }


    // Bluetooth connection gets established
    private void activateBt () {
        // Check wheather the device supports bluetooth
        if(btAdapter == null) {
            showToast(getResources().getString(R.string.main_bt_support), Toast.LENGTH_SHORT);
            Log.e(CLASS, "No Bluetooth support");
        } else {
            // Enable Bluetooth
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Log.e(CLASS, "Bluetooth already enabled");
                bt_activateBt.setEnabled(false);
            }
        }
    }


    // Show List of bounded devices, search for new ones and select a device to connect
    private void selectDevice() {
        Intent btIntent = new Intent(MainActivity.this, DeviceListActivity.class);
        startActivityForResult(btIntent, REQUEST_BLUETOOTH_DEVICE);
    }

    // Enables GPS on the device
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
                    // Bluetooth successfully enabled
                    Log.e(CLASS, "Bluetooth enabled");
                } else if (resultCode == RESULT_CANCELED) {
                    Log.e(CLASS, "Error while enabling Bluetooth");
                }
                break;
            case REQUEST_BLUETOOTH_DEVICE:
                if(resultCode == RESULT_OK) {
                    String deviceAddress = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Address of the bluetooth device
                    btdevice = btAdapter.getRemoteDevice(deviceAddress);
                    // If the pairing request has not been accepted --> wait 2 seconds and afterwards try to establish the connection
                    if(btdevice.getBondState() == BluetoothDevice.BOND_BONDING)
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    Log.e(CLASS, "Selected device:" + btdevice.getAddress() + "; " + btdevice.getAddress());
                    try{
                        //Establish connecntion
                        socket = BluetoothConnector.connectDevice(btdevice);
                        showToast(getResources().getString(R.string.main_sel_dev) + btdevice.getName(), Toast.LENGTH_SHORT);
                        btEnabled = true;
                        checkStartTrip();
                        bt_selectDev.setEnabled(false);
                        iv_info_device.setBackgroundResource(R.drawable.check_circle_green);
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
                    iv_info_gps.setBackgroundResource(R.drawable.check_circle_green);
                    checkStartTrip();
                }
        }
    }

    // Checks the connections and sets up the Buttons, etc.
    private void checkConnections () {
        // Falls Bluetooth bereits eingeschaltet ist, Buttons setzen
        if (btAdapter.isEnabled()) {
            Log.e(CLASS, "Bluetooth already enabled");
            bt_activateBt.setEnabled(false);
            bt_selectDev.setEnabled(true);
            iv_info_bt.setBackgroundResource(R.drawable.check_circle_green);
        } else {
            bt_activateBt.setEnabled(true);
            bt_selectDev.setEnabled(false);
            iv_info_bt.setBackgroundResource(R.drawable.close_circle_red);
            socket = null;

        }
        if (socket != null) {
            btEnabled = true;
            bt_selectDev.setEnabled(false);
            iv_info_device.setBackgroundResource(R.drawable.check_circle_green);
        } else {
            bt_startTrip.setEnabled(false);
            iv_info_device.setBackgroundResource(R.drawable.close_circle_red);
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsEnabled = true;
            bt_activateGps.setEnabled(false);
            iv_info_gps.setBackgroundResource(R.drawable.check_circle_green);
            checkStartTrip();
        } else {
            gpsEnabled = false;
            bt_activateGps.setEnabled(true);
            iv_info_gps.setBackgroundResource(R.drawable.close_circle_red);
        }
    }

    // If GPS and bluetooth is enabled and a connection is established to a device --> allow to start a trip recording
    private void checkStartTrip() {
        if(btEnabled && gpsEnabled)
            bt_startTrip.setEnabled(true);
    }

    // Open activity where additional information are processed
    private void startTrip() {
        Intent intent = new Intent(this, StartTripActivity.class);
        startActivity(intent);
    }

    // Opens the overview of all recorded trips
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