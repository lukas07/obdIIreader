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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class StartYourTripActivity extends AppCompatActivity {
    private EditText et_driverName;
    private EditText et_kmDriven;
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

        et_driverName = (EditText) findViewById(R.id.et_driverName);
        et_kmDriven = (EditText) findViewById(R.id.et_kmDriven);

        // Adapter für das DropDown Menü der Trip-Art hinzufügen
        sp_tripMode = (Spinner) findViewById(R.id.sp_tripMode);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.start_tripMode, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_tripMode.setAdapter(spinnerAdapter);

        // Go-Button, der die Trip-Aufzeichnung startet
        bt_Go = (Button) findViewById(R.id.bt_Go);
        bt_Go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTripRecording();
            }
        });

    }


    private void startTripRecording() {
        // Instanz des Triprecords
        record = record.getTripRecord();

        // Eingabewerte im Recordobjekt speichern
        String mil = et_kmDriven.getText().toString();
        int mileage = Integer.parseInt(et_kmDriven.getText().toString());
        record.setMileage(mileage);
        record.setDriver(et_driverName.getText().toString());
        record.setDriveMode(sp_tripMode.getSelectedItem().toString());


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String datestring  = dateFormat.format(new Date());
        long date = Long.parseLong(datestring);//Calendar.getInstance().getTime().toString());
        record.setStartTimestamp(date);

        Intent intent = new Intent(StartYourTripActivity.this, RunningTripActivity.class);
        startActivity(intent);

    }


}
