package com.example.lukas.bluetoothtest.fragment;



import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.example.lukas.bluetoothtest.R;
import com.example.lukas.bluetoothtest.activity.TripDetailActivity;
import com.example.lukas.bluetoothtest.trip.TripOpenHelper;
import com.example.lukas.bluetoothtest.trip.TripProvider;
import com.example.lukas.bluetoothtest.trip.TripsAdapter;

public class TripListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, FragmentManager.OnBackStackChangedListener {
    private static final String KEY_MODE_MAIN_ACTIVITY = "MODE";

    private ListView lv_trips;

    private TripOpenHelper helper;
    private TripsAdapter adapter;
    private Cursor cursor;

    private FragmentManager fm;
    private int curCheckPosition = 0;
    private long curCheckRowid = 0;
    private boolean dualPane;
    private boolean uselessStackState = true;


    private boolean modeMainActivity = false;


    public static TripListFragment newInstance (boolean modeMainActivity) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_MODE_MAIN_ACTIVITY, modeMainActivity);

        TripListFragment tripListFragment = new TripListFragment();
        tripListFragment.setArguments(bundle);

        return tripListFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            curCheckPosition = savedInstanceState.getInt("curPos", 0);
            curCheckRowid = savedInstanceState.getLong("curRow", 0);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Falls Fragment in der MainActivity angezeigt wird --> nur ersten 4 Sätze anzeigen; wird über boolean gesteuert
        if (getArguments() != null)
            modeMainActivity = getArguments().getBoolean(KEY_MODE_MAIN_ACTIVITY);

        fm = getFragmentManager();
        fm.addOnBackStackChangedListener(this);

        adapter = new TripsAdapter(getActivity(), null);
        setListAdapter(adapter);
        getLoaderManager().initLoader(0, null, this);

        // Wenn nicht in der MainActivity angezeigt wird
        if (!modeMainActivity)
            // Im Landscape-Modus den Bildschirm geteilt anzeigen
            dualPane = getActivity().findViewById(R.id.details) != null;
        else
            dualPane = false;
        if(dualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            showDetails(curCheckPosition, curCheckRowid);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curPos", curCheckPosition);
        outState.putLong("curRow", curCheckRowid);
    }

    @Override
    public void onBackStackChanged() {
        TripDetailFragment details = (TripDetailFragment) fm.findFragmentById(R.id.details);
        if (details != null)
            getListView().setItemChecked(details.getShownIndex(), true);
    }


    private void showDetails(int position, long rowid) {
        curCheckPosition = position;
        curCheckRowid = rowid;
        if(dualPane) {
            getListView().setItemChecked(position, true);
            TripDetailFragment detailFragment = (TripDetailFragment) fm.findFragmentById(R.id.details);
            if((detailFragment == null || detailFragment.getShownIndex() != position) && rowid != 0) {
                detailFragment = TripDetailFragment.newInstance(getActivity().getApplicationContext(), curCheckPosition, curCheckRowid);
                FragmentTransaction ft = fm.beginTransaction();
                ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                ft.replace(R.id.details, detailFragment);
                //if(!uselessStackState)
                  //   ft.addToBackStack(null);
                ft.commit();

                GoogleMapFragment mapFragment = GoogleMapFragment.newInstance(getActivity().getApplicationContext(), rowid, GoogleMapFragment.MAP_MODE_DISPLAY );
                ft = fm.beginTransaction();
                ft.replace(R.id.map_container, mapFragment);
                ft.commit();
            }
        } else if (rowid != 0) {
            Intent intent = new Intent(getActivity(), TripDetailActivity.class);
            intent.putExtra("rowid", rowid);
            intent.putExtra("position", position);
            startActivity(intent);
        }
        uselessStackState = false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader;
        // In der MainActivity nur die ersten 5 Sätze anzeigen
        if (modeMainActivity) {
             loader = new CursorLoader(getActivity(),
                    TripProvider.CONTENT_URI, null, null, null, TripOpenHelper.COL_TS_START + " DESC, " + "ROWID LIMIT 5");
        } else {
            loader = new CursorLoader(getActivity(),
                    TripProvider.CONTENT_URI, null, null, null, TripOpenHelper.COL_TS_START + " DESC");
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onListItemClick(ListView list, View v, int position, long id) {
        showDetails(position, id);
    }

}
