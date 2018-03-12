package com.example.lukas.bluetoothtest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StartTripActivity extends AppCompatActivity {
    private static final String CLASS = StartTripActivity.class.getName();

    private EditText et_driverName;
    private EditText et_mileageStart;
    private Spinner sp_tripMode;
    private Button bt_Go;

    private TripRecord record;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private Geocoder geocoder;

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        showToast(getResources().getString(R.string.main_bt_manual_disabled), Toast.LENGTH_LONG);
                        finish();
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
                if(mode == Settings.Secure.LOCATION_MODE_OFF)
                    showToast(getResources().getString(R.string.main_gps_manual_disabled), Toast.LENGTH_LONG);
                finish();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_your_trip_acitivity);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        // Back-Button hinzufügen
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        // Broadcast-Recevier, um Änderungen des Bluetooth-Status zu registrieren
        IntentFilter filterBt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBt);
        // Broadcast-Recevier, um Änderungen des GPS-Status zu registrieren
        IntentFilter filterGps = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsReceiver, filterGps);

        // Werden zur Ermittlung der Startadresse benötigt, wenn der Trip gestartet wird
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        // Referenzvariablen zu den Feldern deklarieren
        et_driverName = (EditText) findViewById(R.id.et_driverName);
        et_mileageStart = (EditText) findViewById(R.id.et_mileageStart);
        sp_tripMode = (Spinner) findViewById(R.id.sp_tripMode);
        bt_Go = (Button) findViewById(R.id.bt_Go);

        // Adapter für das DropDown Menü der Trip-Art hinzufügen
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.start_tripMode, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_tripMode.setAdapter(spinnerAdapter);

        // Go-Button, der die Trip-Aufzeichnung startet
        bt_Go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTripRecording();
            }
        });

    }


    private void startTripRecording() {
        if (checkInput()) {

            // Instanz des Triprecords
            record = record.getTripRecord();

            // Eingabewerte im Recordobjekt speichern
            String mil = et_mileageStart.getText().toString();
            int mileage = Integer.parseInt(et_mileageStart.getText().toString());
            record.setStartMileage(mileage);
            record.setDriver(et_driverName.getText().toString());
            record.setDriveMode(sp_tripMode.getSelectedItem().toString());


            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            Long timestamp = Long.parseLong(timestampFormat.format(new Date()));
            record.setStartTimestamp(timestamp);


            // Die Startadresse ermitteln und im Record speichern
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(CLASS, "Test");
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
                                    record.setStartAddress(addresses.get(0).getAddressLine(0));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.e(CLASS, "Geocoder: " + e.toString());
                                }
                            }
                        }
                    });
/*
            try {
                String address = addressDetecter.getCurrentAddress();
                record.setStartAddress(address);
            } catch (Exception e) {
                e.printStackTrace();
                record.setStartAddress("NO DATA");
            }
*/
            Intent intent = new Intent(StartTripActivity.this, RunningTripActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private boolean checkInput() {
        // Fahrername fehlt
        if(et_driverName.getText().length() == 0)  {
            Toast.makeText(this, R.string.start_driver_missing, Toast.LENGTH_SHORT).show();
            return false;
        // Kilometerstand fehlt
        } else if(et_mileageStart.getText().length() == 0) {
            Toast.makeText(this, R.string.start_odometer_missing, Toast.LENGTH_SHORT).show();
            return false;
        }  else {
            return true;
        }
    }

    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(bluetoothReceiver);
        unregisterReceiver(gpsReceiver);
    }
}
