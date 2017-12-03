package com.example.lukas.bluetoothtest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StartTripActivity extends AppCompatActivity {
    private EditText et_driverName;
    private EditText et_mileageStart;
    private Spinner sp_tripMode;
    private Button bt_Go;

    private TripRecord record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_your_trip_acitivity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Back-Button hinzufügen
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

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

}
