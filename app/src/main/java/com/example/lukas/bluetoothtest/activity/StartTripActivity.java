package com.example.lukas.bluetoothtest.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.lukas.bluetoothtest.R;
import com.example.lukas.bluetoothtest.trip.TripOpenHelper;
import com.example.lukas.bluetoothtest.trip.TripProvider;
import com.example.lukas.bluetoothtest.trip.TripRecord;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Author: Lukas Breit
 *
 * Description:  The StartTripAcitivity captures some necessary information that is requested for a valid driver's log.
 *
 */

public class StartTripActivity extends AppCompatActivity {
    private static final String CLASS = StartTripActivity.class.getName();
    private static final int REQUEST_LOCATION = 1;

    private EditText et_driverName;
    private EditText et_reason;
    private EditText et_mileageStart;
    private Spinner sp_tripMode;
    private ImageButton bt_Go;

    private TripRecord record;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private Geocoder geocoder;

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
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
                if (mode == Settings.Secure.LOCATION_MODE_OFF)
                    showToast(getResources().getString(R.string.main_gps_manual_disabled), Toast.LENGTH_LONG);
                finish();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_your_trip_acitivity);

        IntentFilter filterBt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBt);

        IntentFilter filterGps = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsReceiver, filterGps);

        // Used to determine the start address
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        et_driverName = (EditText) findViewById(R.id.et_driverName);
        et_reason = (EditText) findViewById(R.id.et_reason);
        et_mileageStart = (EditText) findViewById(R.id.et_mileageStart);
        sp_tripMode = (Spinner) findViewById(R.id.sp_tripMode);
        bt_Go = (ImageButton) findViewById(R.id.bt_Go);

        // Adapter for the spinner of the trip mode
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.start_tripMode, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_tripMode.setAdapter(spinnerAdapter);

        bt_Go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTripRecording();
            }
        });

        // Take the end mileage of the last saved trip as the start value of this one
        final Cursor cursor;
        cursor = this.getContentResolver().query(TripProvider.CONTENT_URI, null, null, null, null);
        if(cursor.getCount() > 0) {
            if (!cursor.moveToLast() || cursor.isLast()) {
                et_mileageStart.setText(cursor.getString(TripOpenHelper.COL_ID_ENDMIL));
            }
        }
    }


    private void startTripRecording() {
        if (checkInput()) {

            // Instance of the trip record
            record = record.getTripRecord();
            record.resetRecord();

            // Save input values into record
            int mileage = Integer.parseInt(et_mileageStart.getText().toString());
            record.setStartMileage(mileage);
            record.setDriver(et_driverName.getText().toString());
            record.setReason(et_reason.getText().toString());
            record.setDriveMode(sp_tripMode.getSelectedItem().toString());


            SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            Long timestamp = Long.parseLong(timestampFormat.format(new Date()));
            record.setStartTimestamp(timestamp);


            // Determine and save the start address
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION);
                Log.e(CLASS, "GPS-Permission requested");
                //return;
            } else {
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
                                            record.setStartAddress("NODATA");
                                        else
                                            record.setStartAddress(addresses.get(0).getAddressLine(0));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Log.e(CLASS, "Geocoder: " + e.toString());
                                    }
                                }
                            }
                        });
            }

            if(record.getStartAddress() == null)
                record.setStartAddress("Keine Informationen");

            Intent intent = new Intent(StartTripActivity.this, RunningTripActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // Checks weather all fields are filled with data by the user
    private boolean checkInput() {
        // Name is missing
        if (et_driverName.getText().length() == 0) {
            Toast.makeText(this, R.string.start_driver_missing, Toast.LENGTH_SHORT).show();
            return false;
        // Reason of the trip is missing
        } else if (et_reason.getText().length() == 0) {
            Toast.makeText(this, R.string.start_reason_missing, Toast.LENGTH_SHORT).show();
            return false;
        // Mileage is missing
        } else if (et_mileageStart.getText().length() == 0) {
            Toast.makeText(this, R.string.start_odometer_missing, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
                                            record.setStartAddress("NODATA");
                                        else
                                            record.setStartAddress(addresses.get(0).getAddressLine(0));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Log.e(CLASS, "Geocoder: " + e.toString());
                                    }
                                }
                            }
                        });
            }
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
