package com.example.lukas.bluetoothtest;



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

public class TripListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, FragmentManager.OnBackStackChangedListener {
    private ListView lv_trips;

    private TripOpenHelper helper;
    private TripsAdapter adapter;
    private Cursor cursor;

    private FragmentManager fm;
    private int curCheckPosition = 0;
    private long curCheckRowid = 0;
    private boolean dualPane;
    private boolean uselessStackState = true;

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

        fm = getFragmentManager();
        fm.addOnBackStackChangedListener(this);

        adapter = new TripsAdapter(getActivity(), null);
        setListAdapter(adapter);
        getLoaderManager().initLoader(0, null, this);

        // Im Landscape-Modus den Bildschirm geteilt anzeigen
        dualPane = getActivity().findViewById(R.id.details) != null;
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
        return new CursorLoader(getActivity(),
                TripProvider.CONTENT_URI, null, null, null, TripOpenHelper.COL_TS_START + " DESC");
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
