package com.example.lukas.bluetoothtest.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.example.lukas.bluetoothtest.fragment.GoogleMapFragment;
import com.example.lukas.bluetoothtest.R;
import com.example.lukas.bluetoothtest.fragment.TripDetailFragment;

/**
 * Author: Lukas Breit
 *
 * Description:  The TripDetailActivity is just a kind of wrapper activity that contains the Detail Fragment and the Google Map Fragment
 *
 */

public class TripDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            finish();
            return;
        }

        TripDetailFragment detailFragment = null;
        GoogleMapFragment mapFragment = null;
        // If the activity is recreated the same fragments can be used
        if(savedInstanceState == null) {
            mapFragment = GoogleMapFragment.newInstance(getApplicationContext(), getIntent().getExtras().getLong("rowid"), GoogleMapFragment.MAP_MODE_DISPLAY);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.detail_map_container, mapFragment, "map").commit();

            detailFragment = TripDetailFragment.newInstance(getApplicationContext(), getIntent().getExtras().getInt("position"), getIntent().getExtras().getLong("rowid"));
            transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.detail_text_container, detailFragment, "defrag");
            transaction.commit();
        } else {
            mapFragment = (GoogleMapFragment) getSupportFragmentManager().findFragmentByTag("map");
            detailFragment = (TripDetailFragment) getSupportFragmentManager().findFragmentByTag("defrag");
        }
        setTitle(R.string.detail_title);


    }

}
