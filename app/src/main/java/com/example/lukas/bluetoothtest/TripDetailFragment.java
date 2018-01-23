package com.example.lukas.bluetoothtest;


import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class TripDetailFragment extends Fragment  {
    private static final int MAP_CAM_ZOOM = 15;
    private GoogleMap map;

    private TextView tv_detail_driver, tv_detail_mode, tv_detail_startTs,
            tv_detail_endTs, tv_detail_startMil, tv_detail_endMil,
            tv_detail_startAdd, tv_detail_endAdd, tv_detail_route;


    public static TripDetailFragment newInstance(Context context, int position, long rowid) {
        TripDetailFragment detailFragment = new TripDetailFragment();
        Bundle args = new Bundle();
        args.putInt("position", position);
        args.putLong("rowid", rowid);
        // Daten des Trips aus DB lesen
        if(rowid != 0) {
            final Cursor cursor;
            cursor = context.getContentResolver().query(TripProvider.CONTENT_URI, null, "_id=?", new String[]{Long.toString(rowid)}, null);
            if (!cursor.moveToFirst()) {
                Log.e(TripDetailFragment.class.getName(), "Result set is empty");
                return null;
            }
            args.putString(TripOpenHelper.COL_DRIVER_NAME, cursor.getString(TripOpenHelper.COL_ID_DRIVER));
            args.putString(TripOpenHelper.COL_DRIVE_MODE, cursor.getString(TripOpenHelper.COL_ID_MODE));
            args.putString(TripOpenHelper.COL_TS_START, TripsAdapter.convertDate(cursor.getLong(TripOpenHelper.COL_ID_STARTTS)));
            args.putString(TripOpenHelper.COL_TS_END, TripsAdapter.convertDate(cursor.getLong(TripOpenHelper.COL_ID_ENDTS)));
            args.putString(TripOpenHelper.COL_MILEAGE_START, cursor.getString(TripOpenHelper.COL_ID_STARTMIL));
            args.putString(TripOpenHelper.COL_MILEAGE_END, cursor.getString(TripOpenHelper.COL_ID_ENDMIL));
            args.putString(TripOpenHelper.COL_ADDRESS_START, cursor.getString(TripOpenHelper.COL_ID_STARTADD));
            args.putString(TripOpenHelper.COL_ADDRESS_END, cursor.getString(TripOpenHelper.COL_ID_ENDADD));
            args.putString(TripOpenHelper.COL_ROUTE_POINTS, cursor.getString(TripOpenHelper.COL_ID_ROUTE));
        }

        detailFragment.setArguments(args);
        return detailFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(container == null) {
            return null;
        }
        View v = inflater.inflate(R.layout.activity_trip_detail, container, false);

        // Referenzvariablen zu den Feldern deklarieren
        tv_detail_driver = (TextView) v.findViewById(R.id.tv_detail_driver);
        tv_detail_mode = (TextView) v.findViewById(R.id.tv_detail_mode);
        tv_detail_startTs = (TextView) v.findViewById(R.id.tv_detail_startTs);
        tv_detail_endTs = (TextView) v.findViewById(R.id.tv_detail_endTs);
        tv_detail_startMil = (TextView) v.findViewById(R.id.tv_detail_startMil);
        tv_detail_endMil = (TextView) v.findViewById(R.id.tv_detail_endMil);
        tv_detail_startAdd = (TextView) v.findViewById(R.id.tv_detail_startAdd);
        tv_detail_endAdd = (TextView) v.findViewById(R.id.tv_detail_endAdd);
        //tv_detail_route = (TextView) v.findViewById(R.id.tv_detail_route);
        // Karte initialisieren
        /*MapView mapView = (MapView) v.findViewById(R.id.map_detail);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);*/


        // TextViews mit den Werten füllen
        tv_detail_driver.setText(getArguments().getString(TripOpenHelper.COL_DRIVER_NAME));
        tv_detail_mode.setText(getArguments().getString(TripOpenHelper.COL_DRIVE_MODE));
        tv_detail_startTs.setText(getArguments().getString(TripOpenHelper.COL_TS_START));
        tv_detail_endTs.setText(getArguments().getString(TripOpenHelper.COL_TS_END));
        tv_detail_startMil.setText(getArguments().getString(TripOpenHelper.COL_MILEAGE_START));
        tv_detail_endMil.setText(getArguments().getString(TripOpenHelper.COL_MILEAGE_END));
        tv_detail_startAdd.setText(getArguments().getString(TripOpenHelper.COL_ADDRESS_START));
        tv_detail_endAdd.setText(getArguments().getString(TripOpenHelper.COL_ADDRESS_END));
        //tv_detail_route.setText(getArguments().getString(TripOpenHelper.COL_ROUTE_POINTS));

        return v;
    }
/*
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
/*
        List<Fragment> fragmentManager = getChildFragmentManager().getFragments();
        FragmentManager fragmentManager2 = getFragmentManager();
        FragmentManager fragmentManager3 = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(new GoogleMapFragment(), "map").commit();

// Die Route als String auslesen und in eine Arraylist konvertieren --> auf Map anzeigen
        String routeString = getArguments().getString(KEY_ROUTE);
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<LatLng>>() {}.getType();
        ArrayList<LatLng> routePoints = gson.fromJson(routeString, type);

        GoogleMapFragment googleMapFragment = (GoogleMapFragment) getFragmentManager().findFragmentByTag("map");
        googleMapFragment.drawRouteOnMap(routePoints);

    }
*/
    public int getShownIndex() {
        return getArguments().getInt("position", 0);
    }
/*
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;

        // Die Route als String auslesen und in eine Arraylist konvertieren --> auf Map anzeigen
        String routeString = getArguments().getString(KEY_ROUTE);
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<LatLng>>() {}.getType();
        ArrayList<LatLng> routePoints = gson.fromJson(routeString, type);

        // Route der Karte hinzufügen
        PolylineOptions lineOptions = new PolylineOptions();

        lineOptions.addAll(routePoints);
        lineOptions.width(12);
        lineOptions.color(Color.RED);

        map.addPolyline(lineOptions);

        Log.e(GoogleMapFragment.class.getName(), "Polyline added to map");

        map.moveCamera(CameraUpdateFactory.newLatLng(routePoints.get(0)));
        map.animateCamera(CameraUpdateFactory.zoomTo(MAP_CAM_ZOOM));
    }
*/
}
