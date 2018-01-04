package com.example.lukas.bluetoothtest;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Lukas on 30.12.2017.
 */

public class TripDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Back-Button hinzufügen
        ActionBar bar = getSupportActionBar();
        //bar.setDisplayHomeAsUpEnabled(true);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            finish();
            return;
        }

        TripDetailFragment detailFragment = null;
        // Wird die Acitivity wiederhergestellt muss kein neues Fragment erzeugt werden
        if(savedInstanceState == null) {
            detailFragment = TripDetailFragment.newInstance(getApplicationContext(), getIntent().getExtras().getInt("position"), getIntent().getExtras().getLong("rowid"));
            getFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, detailFragment, "defrag")
                    .commit();
        } else
            detailFragment = (TripDetailFragment) getFragmentManager().findFragmentByTag("defrag");
        setTitle(R.string.detail_title);
    }

}
