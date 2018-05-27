package com.example.lukas.bluetoothtest.activity;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.lukas.bluetoothtest.fragment.GoogleMapFragment;
import com.example.lukas.bluetoothtest.R;
import com.example.lukas.bluetoothtest.trip.TripOpenHelper;
import com.example.lukas.bluetoothtest.trip.TripProvider;
import com.example.lukas.bluetoothtest.trip.TripRecord;
import com.example.lukas.bluetoothtest.trip.TripsAdapter;

/**
 * Author: Lukas Breit, Berit Grasemann
 *
 * Description:  The StoppedTripActivity is used after a trip recording is finished. The user has to add some additional mandatory
 *               information. Furthermore a Google Map is displayed with the recorded route. Here the trip record is saved in the
 *               database.
 *
 */

public class StoppedTripActivity extends AppCompatActivity {
    private static final String CLASS = StoppedTripActivity.class.getName();

    private EditText et_mileageEnd;
    private ImageButton bt_save;

    private TripRecord record;

    private boolean bt_change_menu = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopped_trip);


        et_mileageEnd = (EditText) findViewById(R.id.et_mileageEnd);
        bt_save = (ImageButton) findViewById(R.id.bt_save);

        bt_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bt_change_menu) {
                    // Back to main menu
                    finish();
                    bt_change_menu = false;
                } else {
                    // Save trip and stay on the screen
                    saveRecord();
                }
            }
        });



        // Add Google Map
        GoogleMapFragment mapFragment = GoogleMapFragment.newInstance(getApplicationContext(), -1, GoogleMapFragment.MAP_MODE_DISPLAY);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.stopped_container, mapFragment);
        fragmentTransaction.commit();

    }

    // Check weather the input is valid and save the record into the database
    private void saveRecord() {
        record = TripRecord.getTripRecord();
        String mileageEnd = et_mileageEnd.getText().toString();
        // end mileage is missing
        if(mileageEnd.length() == 0) {
            showToast(R.string.stop_odometer_missing, Toast.LENGTH_SHORT);
        // The end mileage must be greater than start mileage
        } else if(Integer.parseInt(mileageEnd) <= record.getStartMileage()) {
            showToast(R.string.stop_odometer_low, Toast.LENGTH_SHORT);
        }

        else {
            record.setEndMileage(Integer.parseInt(mileageEnd));
            // Save trip record into database
            ContentValues values = new ContentValues();
            values.put(TripOpenHelper.COL_DRIVER_NAME, record.getDriver());
            values.put(TripOpenHelper.COL_MILEAGE_START, record.getStartMileage());
            values.put(TripOpenHelper.COL_MILEAGE_END, record.getEndMileage());
            values.put(TripOpenHelper.COL_DRIVE_MODE, record.getDriveMode());
            values.put(TripOpenHelper.COL_TS_START, record.getStartTimestamp());
            values.put(TripOpenHelper.COL_TS_END, record.getEndTimestamp());
            values.put(TripOpenHelper.COL_ADDRESS_START, record.getStartAddress());
            values.put(TripOpenHelper.COL_ADDRESS_END, record.getEndAddress());
            values.put(TripOpenHelper.COL_ROUTE_POINTS, record.getRoutePoints());
            values.put(TripOpenHelper.COL_REASON, record.getReason());
            getContentResolver().insert(TripProvider.CONTENT_URI, values);

            // Flag to change the funtion of the button --> after a trip is saved you get back to main menu
            bt_change_menu = true;
            et_mileageEnd.setEnabled(false);
        }
       if(bt_change_menu == true){
           bt_save.setBackgroundResource(R.drawable.back_custom);
       }
    }

    @Override
    public void onBackPressed() {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(R.string.stop_back_msg);
        alertDialogBuilder.setPositiveButton(R.string.stop_back_save,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Log.e(CLASS, "back pressed: Save trip");
                        saveRecord();
                    }
                });

        alertDialogBuilder.setNegativeButton(R.string.stop_back_menu,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e(CLASS, "back pressed: go to menu without save");
                        finish();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showToast (int message, int duration) {
        Toast.makeText(StoppedTripActivity.this, message, duration).show();
    }
}
