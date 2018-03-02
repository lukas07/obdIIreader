package com.example.lukas.trackyourtrips.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.example.lukas.trackyourtrips.fragment.GoogleMapFragment;
import com.example.lukas.trackyourtrips.R;
import com.example.lukas.trackyourtrips.fragment.TripDetailFragment;

/**
 * Created by Lukas on 30.12.2017.
 */

public class TripDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        //Back-Button hinzufügen
        ActionBar bar = getSupportActionBar();
        //bar.setDisplayHomeAsUpEnabled(true);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            finish();
            return;
        }

        TripDetailFragment detailFragment = null;
        GoogleMapFragment mapFragment = null;
        // Wird die Acitivity wiederhergestellt muss kein neues Fragment erzeugt werden
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
