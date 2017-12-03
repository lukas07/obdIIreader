package com.example.lukas.bluetoothtest;

import android.database.Cursor;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class TripListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
    private ListView lv_trips;

    private TripOpenHelper helper;
    private TripsAdapter adapter;
    private Cursor cursor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_list);

        //callbacks = this;

        // Back-Button hinzufügen
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        // Referenzvariablen zu den Feldern deklarieren
        lv_trips = (ListView) findViewById(R.id.lv_trips);

        /*lv_trips.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showDetails(id);
            }
        });*/
        //registerForContextMenu(lv_trips);

        adapter = new TripsAdapter(this, null);
        lv_trips.setAdapter(adapter);
        getLoaderManager().initLoader(0, null, this);
    }

    private void showDetails(long rowid) {
        // TODO Detailansicht aufrufen
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                TripProvider.CONTENT_URI, null, null, null, TripOpenHelper.COL_TS_START + " DESC");
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        showDetails(id);
    }
}
